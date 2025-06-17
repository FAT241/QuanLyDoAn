package org.projectmanagement.UI;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Enhanced FileManager với nhiều tính năng cải tiến
 */
public class EnhancedFileManager {

    // Cấu hình file
    public static class FileConfig {
        public static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
        public static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf"); // Chỉ chấp nhận .pdf
        public static final String REPORTS_DIR = "reports";
        public static final String TEMP_DIR = "temp";
        public static final String BACKUP_DIR = "backup";
        public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");
    }

    // Kết quả upload file
    public static class FileUploadResult {
        private final boolean success;
        private final String filePath;
        private final String fileName;
        private final long fileSize;
        private final String fileHash;
        private final String error;

        private FileUploadResult(boolean success, String filePath, String fileName,
                                 long fileSize, String fileHash, String error) {
            this.success = success;
            this.filePath = filePath;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.fileHash = fileHash;
            this.error = error;
        }

        public static FileUploadResult success(String filePath, String fileName, long fileSize, String hash) {
            return new FileUploadResult(true, filePath, fileName, fileSize, hash, null);
        }

        public static FileUploadResult failure(String error) {
            return new FileUploadResult(false, null, null, 0, null, error);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getFilePath() { return filePath; }
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public String getFileHash() { return fileHash; }
        public String getError() { return error; }
    }

    // Progress callback interface
    public interface ProgressCallback {
        void onProgress(int percentage);
        void onComplete();
        void onError(String error);
    }

    static {
        // Tạo các thư mục cần thiết khi khởi tạo
        createDirectoriesIfNotExist();
    }

    /**
     * Tạo các thư mục cần thiết
     */
    private static void createDirectoriesIfNotExist() {
        try {
            Files.createDirectories(Paths.get(FileConfig.REPORTS_DIR));
            Files.createDirectories(Paths.get(FileConfig.TEMP_DIR));
            Files.createDirectories(Paths.get(FileConfig.BACKUP_DIR));
        } catch (IOException e) {
            System.err.println("Error creating directories: " + e.getMessage());
        }
    }

    /**
     * Upload file với validation và progress tracking
     */
    public static CompletableFuture<FileUploadResult> uploadFileAsync(
            String sourceFilePath,
            ProgressCallback callback) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validation
                FileUploadResult validationResult = validateFile(sourceFilePath);
                if (!validationResult.isSuccess()) {
                    if (callback != null) {
                        callback.onError(validationResult.getError());
                    }
                    return validationResult;
                }

                File sourceFile = new File(sourceFilePath);
                String fileName = generateUniqueFileName(sourceFile.getName());
                Path destPath = Paths.get(FileConfig.REPORTS_DIR, fileName);

                // Copy file với progress tracking
                copyFileWithProgress(sourceFile.toPath(), destPath, callback);

                // Tính hash để verify integrity
                String fileHash = calculateFileHash(destPath);

                if (callback != null) {
                    callback.onComplete();
                }
                return FileUploadResult.success(
                        destPath.toString(),
                        fileName,
                        sourceFile.length(),
                        fileHash
                );

            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
                return FileUploadResult.failure(e.getMessage());
            }
        });
    }

    /**
     * Validate file trước khi upload
     */
    public static FileUploadResult validateFile(String filePath) {
        try {
            if (filePath == null || filePath.trim().isEmpty()) {
                return FileUploadResult.failure("Đường dẫn file không hợp lệ!");
            }

            File file = new File(filePath);

            // Kiểm tra file tồn tại
            if (!file.exists()) {
                return FileUploadResult.failure("File không tồn tại!");
            }

            // Kiểm tra có phải là file không
            if (!file.isFile()) {
                return FileUploadResult.failure("Đây không phải là file!");
            }

            // Kiểm tra file có thể đọc được không
            if (!file.canRead()) {
                return FileUploadResult.failure("Không thể đọc file!");
            }

            // Kiểm tra kích thước
            if (file.length() > FileConfig.MAX_FILE_SIZE) {
                return FileUploadResult.failure(
                        String.format("File quá lớn! Kích thước tối đa: %.1fMB",
                                FileConfig.MAX_FILE_SIZE / (1024.0 * 1024.0))
                );
            }

            // Kiểm tra file rỗng
            if (file.length() == 0) {
                return FileUploadResult.failure("File rỗng!");
            }

            // Kiểm tra extension
            String extension = getFileExtension(file.getName()).toLowerCase();
            if (extension.isEmpty()) {
                return FileUploadResult.failure("File không có phần mở rộng!");
            }

            if (!FileConfig.ALLOWED_EXTENSIONS.contains(extension)) {
                return FileUploadResult.failure(
                        "Định dạng file không được hỗ trợ! Chỉ cho phép: " +
                                String.join(", ", FileConfig.ALLOWED_EXTENSIONS)
                );
            }

            // Kiểm tra file có bị corrupt không
            if (!isFileValid(file)) {
                return FileUploadResult.failure("File bị lỗi hoặc không thể đọc!");
            }

            return FileUploadResult.success(filePath, file.getName(), file.length(), null);

        } catch (Exception e) {
            return FileUploadResult.failure("Lỗi validate file: " + e.getMessage());
        }
    }

    /**
     * Download file với resume capability
     */
    public static CompletableFuture<Boolean> downloadFileAsync(
            String sourceFilePath,
            String destFilePath,
            ProgressCallback callback) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (sourceFilePath == null || sourceFilePath.trim().isEmpty()) {
                    if (callback != null) {
                        callback.onError("Đường dẫn file nguồn không hợp lệ!");
                    }
                    return false;
                }

                if (destFilePath == null || destFilePath.trim().isEmpty()) {
                    if (callback != null) {
                        callback.onError("Đường dẫn file đích không hợp lệ!");
                    }
                    return false;
                }

                Path sourcePath = Paths.get(sourceFilePath);
                Path destPath = Paths.get(destFilePath);

                if (!Files.exists(sourcePath)) {
                    if (callback != null) {
                        callback.onError("File nguồn không tồn tại!");
                    }
                    return false;
                }

                // Tạo thư mục đích nếu chưa có
                if (destPath.getParent() != null) {
                    Files.createDirectories(destPath.getParent());
                }

                // Copy với progress
                copyFileWithProgress(sourcePath, destPath, callback);

                // Verify integrity
                String sourceHash = calculateFileHash(sourcePath);
                String destHash = calculateFileHash(destPath);

                if (!sourceHash.equals(destHash)) {
                    Files.deleteIfExists(destPath);
                    if (callback != null) {
                        callback.onError("File integrity check failed!");
                    }
                    return false;
                }

                if (callback != null) {
                    callback.onComplete();
                }
                return true;

            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
                return false;
            }
        });
    }

    /**
     * Backup file trước khi xóa/sửa
     */
    public static String backupFile(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            return null;
        }

        Path sourcePath = Paths.get(filePath);
        if (!Files.exists(sourcePath)) {
            return null;
        }

        String timestamp = FileConfig.DATE_FORMAT.format(new Date());
        String fileName = sourcePath.getFileName().toString();
        String backupFileName = getFileBaseName(fileName)
                + "_backup_" + timestamp
                + getFileExtension(fileName);

        Path backupPath = Paths.get(FileConfig.BACKUP_DIR, backupFileName);
        Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);

        return backupPath.toString();
    }

    /**
     * Xóa file an toàn (backup trước khi xóa)
     */
    public static boolean safeDeleteFile(String filePath) {
        try {
            if (filePath == null || filePath.trim().isEmpty()) {
                return false;
            }

            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return false;
            }

            // Kiểm tra quyền xóa
            if (!Files.isWritable(path)) {
                throw new IOException("Không có quyền xóa file: " + filePath);
            }

            // Kiểm tra không gian lưu trữ
            Path backupDir = Paths.get(FileConfig.BACKUP_DIR);
            if (!hasEnoughSpace(backupDir, Files.size(path))) {
                throw new IOException("Không đủ dung lượng để sao lưu file!");
            }

            // Sao lưu trước khi xóa
            String backupPath = backupFile(filePath);
            if (backupPath == null) {
                throw new IOException("Sao lưu file thất bại!");
            }

            // Xóa file gốc
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                System.out.println("File đã được backup tại: " + backupPath);
            }

            return deleted;

        } catch (IOException e) {
            System.err.println("Lỗi xóa file: " + e.getMessage());
            return false;
        }
    }

    // Kiểm tra không gian lưu trữ
    private static boolean hasEnoughSpace(Path dir, long requiredSpace) throws IOException {
        return Files.getFileStore(dir).getUsableSpace() >= requiredSpace;
    }

    /**
     * Nén file/folder thành ZIP
     */
    public static CompletableFuture<String> compressToZipAsync(
            String sourcePath,
            String zipFileName,
            ProgressCallback callback) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (sourcePath == null || sourcePath.trim().isEmpty()) {
                    if (callback != null) {
                        callback.onError("Đường dẫn nguồn không hợp lệ!");
                    }
                    return null;
                }

                if (zipFileName == null || zipFileName.trim().isEmpty()) {
                    if (callback != null) {
                        callback.onError("Tên file ZIP không hợp lệ!");
                    }
                    return null;
                }

                Path source = Paths.get(sourcePath);
                if (!Files.exists(source)) {
                    if (callback != null) {
                        callback.onError("File/thư mục nguồn không tồn tại!");
                    }
                    return null;
                }

                String zipPath = Paths.get(FileConfig.TEMP_DIR, zipFileName).toString();

                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath))) {
                    if (Files.isDirectory(source)) {
                        compressDirectory(source, source, zos, callback);
                    } else {
                        compressFile(source, source.getFileName().toString(), zos);
                    }
                }

                if (callback != null) {
                    callback.onComplete();
                }
                return zipPath;

            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
                return null;
            }
        });
    }

    /**
     * Giải nén ZIP file
     */
    public static CompletableFuture<String> extractZipAsync(
            String zipFilePath,
            String destDir,
            ProgressCallback callback) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (zipFilePath == null || zipFilePath.trim().isEmpty()) {
                    if (callback != null) {
                        callback.onError("Đường dẫn file ZIP không hợp lệ!");
                    }
                    return null;
                }

                if (destDir == null || destDir.trim().isEmpty()) {
                    if (callback != null) {
                        callback.onError("Thư mục đích không hợp lệ!");
                    }
                    return null;
                }

                Path zipPath = Paths.get(zipFilePath);
                if (!Files.exists(zipPath)) {
                    if (callback != null) {
                        callback.onError("File ZIP không tồn tại!");
                    }
                    return null;
                }

                Path destPath = Paths.get(destDir);
                Files.createDirectories(destPath);

                try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        Path entryPath = destPath.resolve(entry.getName()).normalize();

                        // Security check - prevent path traversal
                        if (!entryPath.startsWith(destPath)) {
                            throw new IOException("Entry is outside of the target dir: " + entry.getName());
                        }

                        if (entry.isDirectory()) {
                            Files.createDirectories(entryPath);
                        } else {
                            if (entryPath.getParent() != null) {
                                Files.createDirectories(entryPath.getParent());
                            }
                            Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                        zis.closeEntry();
                    }
                }

                if (callback != null) {
                    callback.onComplete();
                }
                return destDir;

            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
                return null;
            }
        });
    }

    /**
     * Lấy thông tin file chi tiết
     */
    public static Map<String, Object> getFileInfo(String filePath) {
        Map<String, Object> info = new HashMap<>();

        try {
            if (filePath == null || filePath.trim().isEmpty()) {
                info.put("error", "Đường dẫn file không hợp lệ");
                return info;
            }

            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                info.put("error", "File không tồn tại");
                return info;
            }

            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

            info.put("fileName", path.getFileName().toString());
            info.put("fileSize", attrs.size());
            info.put("creationTime", attrs.creationTime().toString());
            info.put("lastModified", attrs.lastModifiedTime().toString());
            info.put("isDirectory", attrs.isDirectory());
            info.put("isRegularFile", attrs.isRegularFile());
            info.put("fileExtension", getFileExtension(path.getFileName().toString()));

            // Only calculate hash for regular files, not directories
            if (attrs.isRegularFile()) {
                info.put("fileHash", calculateFileHash(path));
            } else {
                info.put("fileHash", "N/A");
            }

        } catch (Exception e) {
            info.put("error", e.getMessage());
        }

        return info;
    }

    /**
     * Dọn dẹp file tạm và backup cũ
     */
    public static void cleanupOldFiles(int daysOld) {
        if (daysOld <= 0) {
            System.err.println("Số ngày phải lớn hơn 0");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);

                // Dọn dẹp thư mục temp
                cleanupDirectory(Paths.get(FileConfig.TEMP_DIR), cutoffTime);

                // Dọn dẹp thư mục backup
                cleanupDirectory(Paths.get(FileConfig.BACKUP_DIR), cutoffTime);

                System.out.println("Dọn dẹp hoàn tất");

            } catch (Exception e) {
                System.err.println("Lỗi khi dọn dẹp: " + e.getMessage());
            }
        });
    }

    private static void cleanupDirectory(Path directory, long cutoffTime) throws IOException {
        if (!Files.exists(directory)) return;

        Files.walk(directory)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffTime && Files.isWritable(path);
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        System.out.println("Đã xóa file cũ: " + path);
                    } catch (IOException e) {
                        System.err.println("Không thể xóa: " + path + " - " + e.getMessage());
                    }
                });
    }

    // =============== PUBLIC UTILITY METHODS ===============

    /**
     * Get file extension (made public and static for UIFileHandler)
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }

    // =============== PRIVATE UTILITY METHODS ===============

    private static void copyFileWithProgress(Path source, Path dest, ProgressCallback callback) throws IOException {
        // Kiểm tra quyền ghi
        if (dest.getParent() != null && !Files.isWritable(dest.getParent())) {
            throw new IOException("Không có quyền ghi vào thư mục đích: " + dest.getParent());
        }

        // Kiểm tra file đích tồn tại
        if (Files.exists(dest)) {
            dest = resolveUniquePath(dest); // Tạo tên file mới nếu cần
        }

        long fileSize = Files.size(source);
        if (fileSize == 0) {
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            if (callback != null) {
                callback.onProgress(100);
            }
            return;
        }

        try (InputStream in = Files.newInputStream(source);
             OutputStream out = Files.newOutputStream(dest, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[8192];
            long totalBytesRead = 0;
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                if (callback != null) {
                    int percentage = (int) ((totalBytesRead * 100) / fileSize);
                    callback.onProgress(Math.min(percentage, 100));
                }
            }
        }
    }

    // Tạo tên file mới nếu file đích đã tồn tại
    private static Path resolveUniquePath(Path dest) {
        String baseName = getFileBaseName(dest.getFileName().toString());
        String extension = getFileExtension(dest.getFileName().toString());
        Path parent = dest.getParent();
        int counter = 1;

        while (Files.exists(dest)) {
            String newFileName = baseName + "_" + counter + extension;
            dest = parent.resolve(newFileName);
            counter++;
        }
        return dest;
    }

    private static String calculateFileHash(Path filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }

            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException | IOException e) {
            return "hash_error";
        }
    }

    private static String generateUniqueFileName(String originalName) {
        if (originalName == null || originalName.trim().isEmpty()) {
            return "unnamed_" + FileConfig.DATE_FORMAT.format(new Date());
        }

        String baseName = getFileBaseName(originalName);
        String extension = getFileExtension(originalName);
        String timestamp = FileConfig.DATE_FORMAT.format(new Date());

        return baseName + "_" + timestamp + extension;
    }

    private static String getFileBaseName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "unnamed";
        }
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }

    private static boolean isFileValid(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            // Đọc vài byte đầu để kiểm tra file có bị corrupt không
            byte[] buffer = new byte[Math.min(1024, (int)file.length())];
            fis.read(buffer);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void compressDirectory(Path sourceDir, Path rootDir, ZipOutputStream zos, ProgressCallback callback) throws IOException {
        Files.walk(sourceDir)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        String relativePath = rootDir.relativize(path).toString().replace('\\', '/'); // Normalize path separators
                        compressFile(path, relativePath, zos);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static void compressFile(Path filePath, String entryName, ZipOutputStream zos) throws IOException {
        ZipEntry zipEntry = new ZipEntry(entryName);
        zos.putNextEntry(zipEntry);
        Files.copy(filePath, zos);
        zos.closeEntry();
    }
}