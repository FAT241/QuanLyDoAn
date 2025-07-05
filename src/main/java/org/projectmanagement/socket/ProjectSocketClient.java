package org.projectmanagement.socket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ProjectSocketClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Gson gson;
    private boolean isConnected = false;
    private String userId;

    // Callback cho các sự kiện
    private Consumer<String> onNotificationReceived;
    private Consumer<String> onFileUploadProgress;
    private Consumer<String> onFileDownloadProgress;

    public ProjectSocketClient() {
        this.gson = new Gson();
    }
// Phương thức connect để kết nối đến server
    public CompletableFuture<Boolean> connect(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.userId = userId;
                socket = new Socket(SERVER_HOST, SERVER_PORT);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);

                // Gửi yêu cầu kết nối
                JsonObject connectRequest = new JsonObject();
                connectRequest.addProperty("action", "connect");
                connectRequest.addProperty("userId", userId);
                writer.println(gson.toJson(connectRequest));

                // Khởi động thread để lắng nghe tin nhắn từ server
                startMessageListener();

                isConnected = true;
                return true;

            } catch (IOException e) {
                System.err.println("Lỗi kết nối socket: " + e.getMessage());
                return false;
            }
        });
    }
// Bắt đầu lắng nghe tin nhắn từ server
    private void startMessageListener() {
        Thread listenerThread = new Thread(() -> {
            try {
                String message;
                while (isConnected && (message = reader.readLine()) != null) {
                    handleServerMessage(message);
                }
            } catch (IOException e) {
                if (isConnected) {
                    System.err.println("Lỗi đọc tin nhắn từ server: " + e.getMessage());
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
// Phương thức xử lý tin nhắn từ server
    // Đây là nơi nhận các thông báo, phản hồi upload/download, và lỗi từ server
    private void handleServerMessage(String message) {
        try {
            JsonObject response = gson.fromJson(message, JsonObject.class);
            String action = response.get("action").getAsString();

            switch (action) {
                case "notification":
                    if (onNotificationReceived != null) {
                        String notificationMessage = response.get("message").getAsString();
                        String type = response.get("type").getAsString();
                        SwingUtilities.invokeLater(() ->
                                onNotificationReceived.accept(type + ": " + notificationMessage));
                    }
                    break;
                case "upload_response":
                    handleUploadResponse(response);
                    break;
                case "download_response":
                    handleDownloadResponse(response);
                    break;
                case "error":
                    String errorMessage = response.get("message").getAsString();
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(null, errorMessage, "Lỗi Socket", JOptionPane.ERROR_MESSAGE));
                    break;
            }
        } catch (Exception e) {
            System.err.println("Lỗi xử lý tin nhắn từ server: " + e.getMessage());
        }
    }
// Phương thức uploadFile để tải file lên server
    // Đây là nơi gửi file từ client lên server, bao gồm việc đọc file, mã hóa base64 và gửi yêu cầu upload
    public CompletableFuture<UploadResult> uploadFile(String filePath, int projectId,
                                                      Consumer<Integer> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    return new UploadResult(false, "File không tồn tại", null);
                }

                // Đọc file và encode base64
                byte[] fileData = Files.readAllBytes(file.toPath());
                String encodedData = java.util.Base64.getEncoder().encodeToString(fileData);

                // Gửi yêu cầu upload
                JsonObject uploadRequest = new JsonObject();
                uploadRequest.addProperty("action", "upload_file");
                uploadRequest.addProperty("fileName", file.getName());
                uploadRequest.addProperty("fileData", encodedData);
                uploadRequest.addProperty("projectId", projectId);
                uploadRequest.addProperty("userId", userId);

                writer.println(gson.toJson(uploadRequest));

                // Báo cáo tiến độ (giả lập)
                if (progressCallback != null) {
                    for (int i = 0; i <= 100; i += 10) {
                        final int progress = i;
                        SwingUtilities.invokeLater(() -> progressCallback.accept(progress));
                        Thread.sleep(100);
                    }
                }

                return new UploadResult(true, "Upload thành công", null);

            } catch (Exception e) {
                return new UploadResult(false, "Lỗi upload: " + e.getMessage(), null);
            }
        });
    }
// Phương thức downloadFile để tải file từ server về
    public CompletableFuture<DownloadResult> downloadFile(String serverFilePath, String localFilePath,
                                                          Consumer<Integer> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Gửi yêu cầu download
                JsonObject downloadRequest = new JsonObject();
                downloadRequest.addProperty("action", "download_file");
                downloadRequest.addProperty("filePath", serverFilePath);
                downloadRequest.addProperty("userId", userId);

                writer.println(gson.toJson(downloadRequest));

                // Báo cáo tiến độ (giả lập)
                if (progressCallback != null) {
                    for (int i = 0; i <= 100; i += 10) {
                        final int progress = i;
                        SwingUtilities.invokeLater(() -> progressCallback.accept(progress));
                        Thread.sleep(100);
                    }
                }

                return new DownloadResult(true, "Download thành công", localFilePath);

            } catch (Exception e) {
                return new DownloadResult(false, "Lỗi download: " + e.getMessage(), null);
            }
        });
    }

    public void submitProject(int projectId, String status) {
        JsonObject submitRequest = new JsonObject();
        submitRequest.addProperty("action", "submit_project");
        submitRequest.addProperty("projectId", projectId);
        submitRequest.addProperty("status", status);
        submitRequest.addProperty("userId", userId);

        writer.println(gson.toJson(submitRequest));
    }

    public void getProjectStatus(int projectId) {
        JsonObject statusRequest = new JsonObject();
        statusRequest.addProperty("action", "get_project_status");
        statusRequest.addProperty("projectId", projectId);
        statusRequest.addProperty("userId", userId);

        writer.println(gson.toJson(statusRequest));
    }
// Phương thức xử lý phản hồi upload
    // Đây là nơi nhận phản hồi từ server sau khi upload file, hiển thị thông báo thành công hoặc lỗi
    private void handleUploadResponse(JsonObject response) {
        String status = response.get("status").getAsString();
        String message = response.get("message").getAsString();

        SwingUtilities.invokeLater(() -> {
            if ("success".equals(status)) {
                JOptionPane.showMessageDialog(null, message, "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void handleDownloadResponse(JsonObject response) {
        try {
            String status = response.get("status").getAsString();
            if ("success".equals(status)) {
                String fileName = response.get("fileName").getAsString();
                String fileData = response.get("fileData").getAsString();

                // Decode và lưu file
                byte[] decodedData = java.util.Base64.getDecoder().decode(fileData);

                // Hiển thị dialog chọn nơi lưu
                SwingUtilities.invokeLater(() -> {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setSelectedFile(new File(fileName));
                    if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                        try {
                            Files.write(fileChooser.getSelectedFile().toPath(), decodedData);
                            JOptionPane.showMessageDialog(null, "Download thành công!",
                                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(null, "Lỗi lưu file: " + e.getMessage(),
                                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null, "Lỗi xử lý download: " + e.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE));
        }
    }

    public void disconnect() {
        isConnected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("Lỗi ngắt kết nối: " + e.getMessage());
        }
    }

    // Setter cho callbacks
    public void setOnNotificationReceived(Consumer<String> callback) {
        this.onNotificationReceived = callback;
    }

    public void setOnFileUploadProgress(Consumer<String> callback) {
        this.onFileUploadProgress = callback;
    }

    public void setOnFileDownloadProgress(Consumer<String> callback) {
        this.onFileDownloadProgress = callback;
    }

    public boolean isConnected() {
        return isConnected;
    }

    // Các class kết quả
    public static class UploadResult {
        private final boolean success;
        private final String message;
        private final String filePath;

        public UploadResult(boolean success, String message, String filePath) {
            this.success = success;
            this.message = message;
            this.filePath = filePath;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getFilePath() { return filePath; }
    }

    public static class DownloadResult {
        private final boolean success;
        private final String message;
        private final String filePath;

        public DownloadResult(boolean success, String message, String filePath) {
            this.success = success;
            this.message = message;
            this.filePath = filePath;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getFilePath() { return filePath; }
    }
}