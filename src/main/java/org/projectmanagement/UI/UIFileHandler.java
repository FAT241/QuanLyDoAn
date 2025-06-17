package org.projectmanagement.UI;

import org.projectmanagement.UI.EnhancedFileManager.FileUploadResult;
import org.projectmanagement.UI.EnhancedFileManager.ProgressCallback;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.projectmanagement.UI.EnhancedFileManager.getFileExtension;

/**
 * UI File Handler để tích hợp với Swing components
 */
public class UIFileHandler {

    /**
     * Tạo file chooser với filter và preview
     */
    public static JFileChooser createEnhancedFileChooser() {
        JFileChooser fileChooser = new JFileChooser();

        // File filters
        fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String ext = getFileExtension(f.getName()).toLowerCase();
                return ext.equals(".pdf"); // Chỉ chấp nhận .pdf
            }

            @Override
            public String getDescription() {
                return "PDF Files (*.pdf)";
            }
        });

        // Set default filter
        fileChooser.setFileFilter(fileChooser.getChoosableFileFilters()[1]);

        // Enable file preview (optional)
        fileChooser.setAccessory(createFilePreviewPanel());

        return fileChooser;
    }

    /**
     * Upload file với progress dialog
     */
    public static void uploadFileWithProgress(Component parent, String sourceFilePath,
                                              Consumer<FileUploadResult> onComplete) {
        // Kiểm tra quyền đọc
        if (!new File(sourceFilePath).canRead()) {
            JOptionPane.showMessageDialog(parent, "Không có quyền đọc file!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JDialog progressDialog = new JDialog();
        progressDialog.setTitle("Đang tải file...");
        progressDialog.setSize(400, 120);
        progressDialog.setLocationRelativeTo(parent);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setString("Đang chuẩn bị...");
        progressBar.setStringPainted(true);

        JLabel statusLabel = new JLabel("Đang kiểm tra file...");
        JButton cancelButton = new JButton("Hủy");

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(statusLabel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        panel.add(cancelButton, BorderLayout.SOUTH);

        progressDialog.add(panel);

        ProgressCallback callback = new ProgressCallback() {
            @Override
            public void onProgress(int percentage) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(percentage);
                    progressBar.setString(percentage + "%");
                    statusLabel.setText("Đang tải file... " + percentage + "%");
                });
            }

            @Override
            public void onComplete() {
                SwingUtilities.invokeLater(() -> progressDialog.dispose());
            }

            @Override
            public void onError(String error) {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(parent, "Lỗi upload file: " + error, "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        };

        CompletableFuture<FileUploadResult> uploadFuture = EnhancedFileManager.uploadFileAsync(sourceFilePath, callback);

        cancelButton.addActionListener(e -> {
            uploadFuture.cancel(true);
            progressDialog.dispose();
        });

        uploadFuture.thenAccept(result -> SwingUtilities.invokeLater(() -> {
            if (result.isSuccess()) {
                onComplete.accept(result);
                showUploadSuccessDialog(parent, result);
            } else if (!uploadFuture.isCancelled()) {
                JOptionPane.showMessageDialog(parent, "Upload thất bại: " + result.getError(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }));

        progressDialog.setVisible(true);
    }

    /**
     * Download file với progress dialog
     */
    public static void downloadFileWithProgress(Component parent, String sourceFilePath,
                                                String destFilePath, Runnable onComplete) {

        // Tạo progress dialog
        JDialog progressDialog = new JDialog();
        progressDialog.setTitle("Đang tải xuống...");
        progressDialog.setSize(400, 120);
        progressDialog.setLocationRelativeTo(parent);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setString("Đang chuẩn bị...");
        progressBar.setStringPainted(true);

        JLabel statusLabel = new JLabel("Đang kiểm tra file...");
        JButton cancelButton = new JButton("Hủy");

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(statusLabel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        panel.add(cancelButton, BorderLayout.SOUTH);

        progressDialog.add(panel);

        // Progress callback
        ProgressCallback callback = new ProgressCallback() {
            @Override
            public void onProgress(int percentage) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(percentage);
                    progressBar.setString(percentage + "%");
                    statusLabel.setText("Đang tải xuống... " + percentage + "%");
                });
            }

            @Override
            public void onComplete() {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(parent,
                            "Tải file thành công!",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    if (onComplete != null) onComplete.run();
                });
            }

            @Override
            public void onError(String error) {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(parent,
                            "Lỗi tải file: " + error,
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        };

        // Start download
        CompletableFuture<Boolean> downloadFuture =
                EnhancedFileManager.downloadFileAsync(sourceFilePath, destFilePath, callback);

        // Handle cancel
        cancelButton.addActionListener(e -> {
            downloadFuture.cancel(true);
            progressDialog.dispose();
        });

        progressDialog.setVisible(true);
    }

    /**
     * Hiển thị dialog chọn file với validation
     */
    public static String showFileChooserDialog(Component parent, String title) {
        JFileChooser fileChooser = createEnhancedFileChooser();
        fileChooser.setDialogTitle(title);

        int result = fileChooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            String selectedPath = fileChooser.getSelectedFile().getAbsolutePath();

            // Validate file trước khi return
            FileUploadResult validation = EnhancedFileManager.validateFile(selectedPath);
            if (!validation.isSuccess()) {
                JOptionPane.showMessageDialog(parent,
                        "File không hợp lệ: " + validation.getError(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return null;
            }

            return selectedPath;
        }

        return null;
    }

    /**
     * Tạo enhanced button với file operations
     */
    public static JButton createFileOperationButton(String text, String iconPath,
                                                    ActionListener actionListener) {
        JButton button = new JButton(text);

        // Set icon if provided
        if (iconPath != null) {
            try {
                ImageIcon icon = new ImageIcon(iconPath);
                Image img = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                button.setIcon(new ImageIcon(img));
            } catch (Exception e) {
                // Icon loading failed, continue without icon
            }
        }

        // Styling
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);

        // Add hover effects
        Color originalBg = button.getBackground();
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(originalBg.brighter());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(originalBg);
            }
        });

        if (actionListener != null) {
            button.addActionListener(actionListener);
        }

        return button;
    }

    /**
     * Tạo file info panel
     */
    public static JPanel createFileInfoPanel(String filePath) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Thông tin file"));

        if (filePath == null || filePath.isEmpty()) {
            panel.add(new JLabel("Chưa có file"));
            return panel;
        }

        Map<String, Object> fileInfo = EnhancedFileManager.getFileInfo(filePath);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        for (Map.Entry<String, Object> entry : fileInfo.entrySet()) {
            gbc.gridx = 0; gbc.gridy = row;
            panel.add(new JLabel(entry.getKey() + ":"), gbc);

            gbc.gridx = 1;
            String value = entry.getValue().toString();
            if ("fileSize".equals(entry.getKey())) {
                long size = (Long) entry.getValue();
                value = formatFileSize(size);
            }
            panel.add(new JLabel(value), gbc);
            row++;
        }

        return panel;
    }

    // =============== PRIVATE HELPER METHODS ===============

    private static JPanel createFilePreviewPanel() {
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setPreferredSize(new Dimension(200, 100));
        previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));

        JTextArea previewArea = new JTextArea();
        previewArea.setEditable(false);
        previewArea.setBackground(Color.LIGHT_GRAY);
        previewArea.setText("File preview\nwill appear here");

        previewPanel.add(new JScrollPane(previewArea), BorderLayout.CENTER);
        return previewPanel;
    }

    /**
     * Hiển thị dialog khi upload thành công
     */
    private static void showUploadSuccessDialog(Component parent, FileUploadResult result) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setText("Upload thành công!\n\n" +
                "Tên file: " + result.getFileName() + "\n" +
                "Kích thước: " + formatFileSize(result.getFileSize()) + "\n" +
                "Vị trí lưu: " + result.getFilePath() + "\n" +
                "SHA-256: " + result.getFileHash()
        );

        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        JOptionPane.showMessageDialog(parent, panel, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Định dạng kích thước file
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}