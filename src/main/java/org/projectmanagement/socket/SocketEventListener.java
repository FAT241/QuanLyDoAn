package org.projectmanagement.socket;

public interface SocketEventListener {
    void onConnected();
    void onDisconnected();
    void onNotificationReceived(String message, String type);
    void onFileUploadProgress(int progress);
    void onFileDownloadProgress(int progress);
    void onError(String error);
}