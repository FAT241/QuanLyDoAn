package org.projectmanagement.socket;
//onConnected: Cập nhật trạng thái UI khi kết nối thành công.
//onDisconnected: Hiển thị thông báo mất kết nối và cập nhật trạng thái UI.
//onNotificationReceived: Hiển thị thông báo từ server (thành công, lỗi, cảnh báo).
//onFileUploadProgress/onFileDownloadProgress: Cập nhật thanh tiến độ (progress bar) khi tải lên/tải xuống file.
//onError: Hiển thị thông báo lỗi từ socket.
public interface SocketEventListener {
    void onConnected();
    void onDisconnected();
    void onNotificationReceived(String message, String type);
    void onFileUploadProgress(int progress);
    void onFileDownloadProgress(int progress);
    void onError(String error);
}