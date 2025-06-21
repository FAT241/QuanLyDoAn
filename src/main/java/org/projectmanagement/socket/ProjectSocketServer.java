package org.projectmanagement.socket;

import org.projectmanagement.dao.ProjectDAO;
import org.projectmanagement.models.Project;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProjectSocketServer {
    private static final int PORT = 8888;
    private static final String UPLOAD_DIR = "uploads/projects/";
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private ExecutorService threadPool;
    private Connection dbConnection;
    private ProjectDAO projectDAO;
    private Gson gson;

    // Lưu trữ các client đang kết nối
    private ConcurrentHashMap<String, ClientHandler> connectedClients;

    public ProjectSocketServer(Connection dbConnection) {
        this.dbConnection = dbConnection;
        this.projectDAO = new ProjectDAO(dbConnection);
        this.gson = new Gson();
        this.connectedClients = new ConcurrentHashMap<>();
        this.threadPool = Executors.newFixedThreadPool(10);

        // Tạo thư mục upload nếu chưa tồn tại
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            System.err.println("Không thể tạo thư mục upload: " + e.getMessage());
        }
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            isRunning = true;
            System.out.println("Socket Server đã khởi động trên port " + PORT);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                threadPool.submit(clientHandler);
            }
        } catch (IOException e) {
            if (isRunning) {
                System.err.println("Lỗi server socket: " + e.getMessage());
            }
        }
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
        } catch (IOException e) {
            System.err.println("Lỗi khi đóng server: " + e.getMessage());
        }
    }

    // Gửi thông báo đến tất cả client
    public void broadcastNotification(String message, String type) {
        JsonObject notification = new JsonObject();
        notification.addProperty("action", "notification");
        notification.addProperty("type", type);
        notification.addProperty("message", message);
        notification.addProperty("timestamp", System.currentTimeMillis());

        String jsonMessage = gson.toJson(notification);
        connectedClients.values().forEach(client -> client.sendMessage(jsonMessage));
    }

    // Gửi thông báo đến client cụ thể
    public void sendNotificationToUser(String userId, String message, String type) {
        ClientHandler client = connectedClients.get(userId);
        if (client != null) {
            JsonObject notification = new JsonObject();
            notification.addProperty("action", "notification");
            notification.addProperty("type", type);
            notification.addProperty("message", message);
            notification.addProperty("timestamp", System.currentTimeMillis());

            client.sendMessage(gson.toJson(notification));
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String userId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.err.println("Lỗi khởi tạo client handler: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                String inputLine;
                while ((inputLine = reader.readLine()) != null) {
                    handleClientMessage(inputLine);
                }
            } catch (IOException e) {
                System.err.println("Lỗi xử lý client: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void handleClientMessage(String message) {
            try {
                JsonObject request = gson.fromJson(message, JsonObject.class);
                String action = request.get("action").getAsString();

                switch (action) {
                    case "connect":
                        handleConnect(request);
                        break;
                    case "upload_file":
                        handleFileUpload(request);
                        break;
                    case "download_file":
                        handleFileDownload(request);
                        break;
                    case "submit_project":
                        handleProjectSubmission(request);
                        break;
                    case "get_project_status":
                        handleGetProjectStatus(request);
                        break;
                    default:
                        sendErrorResponse("Hành động không được hỗ trợ: " + action);
                }
            } catch (Exception e) {
                sendErrorResponse("Lỗi xử lý yêu cầu: " + e.getMessage());
            }
        }

        private void handleConnect(JsonObject request) {
            userId = request.get("userId").getAsString();
            connectedClients.put(userId, this);

            JsonObject response = new JsonObject();
            response.addProperty("action", "connect_response");
            response.addProperty("status", "success");
            response.addProperty("message", "Kết nối thành công");
            sendMessage(gson.toJson(response));

            System.out.println("User " + userId + " đã kết nối");
        }

        private void handleFileUpload(JsonObject request) {
            try {
                String fileName = request.get("fileName").getAsString();
                String fileData = request.get("fileData").getAsString();
                int projectId = request.get("projectId").getAsInt();

                // Decode base64 file data
                byte[] decodedData = java.util.Base64.getDecoder().decode(fileData);

                // Tạo tên file unique
                String timestamp = String.valueOf(System.currentTimeMillis());
                String extension = fileName.substring(fileName.lastIndexOf('.'));
                String uniqueFileName = "project_" + projectId + "_" + timestamp + extension;
                String filePath = UPLOAD_DIR + uniqueFileName;

                // Lưu file
                Files.write(Paths.get(filePath), decodedData);

                // Cập nhật database
                Project project = projectDAO.findById(projectId);
                if (project != null) {
                    project.setTepBaoCao(filePath);
                    project.setNgayNop(new java.util.Date());
                    projectDAO.updateProject(project);

                    // Gửi response thành công
                    JsonObject response = new JsonObject();
                    response.addProperty("action", "upload_response");
                    response.addProperty("status", "success");
                    response.addProperty("message", "Upload file thành công");
                    response.addProperty("filePath", filePath);
                    sendMessage(gson.toJson(response));

                    // Gửi thông báo đến tất cả client
                    broadcastNotification("Đồ án '" + project.getTitle() + "' đã được nộp", "project_submitted");

                } else {
                    sendErrorResponse("Không tìm thấy đồ án với ID: " + projectId);
                }

            } catch (Exception e) {
                sendErrorResponse("Lỗi upload file: " + e.getMessage());
            }
        }

        private void handleFileDownload(JsonObject request) {
            try {
                String filePath = request.get("filePath").getAsString();
                File file = new File(filePath);

                if (!file.exists()) {
                    sendErrorResponse("File không tồn tại: " + filePath);
                    return;
                }

                // Đọc file và encode base64
                byte[] fileData = Files.readAllBytes(file.toPath());
                String encodedData = java.util.Base64.getEncoder().encodeToString(fileData);

                JsonObject response = new JsonObject();
                response.addProperty("action", "download_response");
                response.addProperty("status", "success");
                response.addProperty("fileName", file.getName());
                response.addProperty("fileData", encodedData);
                response.addProperty("fileSize", file.length());
                sendMessage(gson.toJson(response));

            } catch (Exception e) {
                sendErrorResponse("Lỗi download file: " + e.getMessage());
            }
        }

        private void handleProjectSubmission(JsonObject request) {
            try {
                int projectId = request.get("projectId").getAsInt();
                String status = request.get("status").getAsString();

                Project project = projectDAO.findById(projectId);
                if (project != null) {
                    project.setStatus(status);
                    if ("DA_NOP".equals(status)) {
                        project.setNgayNop(new java.util.Date());
                    }
                    projectDAO.updateProject(project);

                    JsonObject response = new JsonObject();
                    response.addProperty("action", "submit_response");
                    response.addProperty("status", "success");
                    response.addProperty("message", "Cập nhật trạng thái đồ án thành công");
                    sendMessage(gson.toJson(response));

                    // Thông báo cho các client khác
                    String message = "Đồ án '" + project.getTitle() + "' đã được cập nhật trạng thái: " + status;
                    broadcastNotification(message, "project_status_updated");

                } else {
                    sendErrorResponse("Không tìm thấy đồ án với ID: " + projectId);
                }

            } catch (Exception e) {
                sendErrorResponse("Lỗi cập nhật đồ án: " + e.getMessage());
            }
        }

        private void handleGetProjectStatus(JsonObject request) {
            try {
                int projectId = request.get("projectId").getAsInt();
                Project project = projectDAO.findById(projectId);

                if (project != null) {
                    JsonObject response = new JsonObject();
                    response.addProperty("action", "project_status_response");
                    response.addProperty("status", "success");
                    response.addProperty("projectId", project.getProjectId());
                    response.addProperty("projectStatus", project.getStatus());
                    response.addProperty("title", project.getTitle());
                    response.addProperty("submissionDate", project.getNgayNop() != null ?
                            project.getNgayNop().toString() : null);
                    sendMessage(gson.toJson(response));
                } else {
                    sendErrorResponse("Không tìm thấy đồ án với ID: " + projectId);
                }

            } catch (Exception e) {
                sendErrorResponse("Lỗi lấy trạng thái đồ án: " + e.getMessage());
            }
        }

        private void sendErrorResponse(String errorMessage) {
            JsonObject response = new JsonObject();
            response.addProperty("action", "error");
            response.addProperty("status", "error");
            response.addProperty("message", errorMessage);
            sendMessage(gson.toJson(response));
        }

        public void sendMessage(String message) {
            if (writer != null) {
                writer.println(message);
            }
        }

        private void cleanup() {
            try {
                if (userId != null) {
                    connectedClients.remove(userId);
                    System.out.println("User " + userId + " đã ngắt kết nối");
                }
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
                System.err.println("Lỗi cleanup client: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        // Khởi động server với kết nối database
        try {
            Connection dbConnection = org.projectmanagement.util.DBConnection.getConnection();
            ProjectSocketServer server = new ProjectSocketServer(dbConnection);
            server.start();
            System.out.println("Server đang chạy trên port 8888...");
        } catch (SQLException e) {
            System.err.println("Lỗi khởi động server: " + e.getMessage());
        }
    }
}