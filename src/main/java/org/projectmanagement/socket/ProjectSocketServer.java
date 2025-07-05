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

        // Tạo thư mục upload nếu chưa tồn tại và kiểm tra quyền
        initializeUploadDirectory();
    }
// Phương thức khởi tạo thư mục upload
    //Kiểm tra quyền ghi vào thư mục upload trong initializeUploadDirectory().
    private void initializeUploadDirectory() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);

            // Kiểm tra thư mục đã được tạo và có quyền ghi
            if (!Files.exists(uploadPath)) {
                throw new IOException("Không thể tạo thư mục upload: " + UPLOAD_DIR);
            }

            if (!Files.isWritable(uploadPath)) {
                throw new IOException("Không có quyền ghi vào thư mục upload: " + UPLOAD_DIR);
            }

            System.out.println("Thư mục upload đã được khởi tạo thành công: " + UPLOAD_DIR);

        } catch (IOException e) {
            System.err.println("Lỗi khởi tạo thư mục upload: " + e.getMessage());
            throw new RuntimeException("Không thể khởi tạo server do lỗi thư mục upload", e);
        }
    }
// Phương thức khởi động server
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
//Sử dụng ExecutorService với Executors.newFixedThreadPool(10) để quản lý nhiều thread xử lý các kết nối client đồng thời.
//Mỗi ClientHandler được chạy trong một thread riêng (qua threadPool.submit(clientHandler)), cho phép server xử lý nhiều client cùng lúc.
//Thread listener trong start() để chấp nhận kết nối mới từ serverSocket.accept().
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
// Xử lý các tin nhắn từ client
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
                e.printStackTrace(); // Log chi tiết lỗi
            }
        }
// Xử lý kết nối từ client
        private void handleConnect(JsonObject request) {
            try {
                if (!request.has("userId") || request.get("userId").isJsonNull()) {
                    sendErrorResponse("User ID không được để trống");
                    return;
                }

                userId = request.get("userId").getAsString();
                if (userId == null || userId.trim().isEmpty()) {
                    sendErrorResponse("User ID không hợp lệ");
                    return;
                }

                connectedClients.put(userId, this);

                JsonObject response = new JsonObject();
                response.addProperty("action", "connect_response");
                response.addProperty("status", "success");
                response.addProperty("message", "Kết nối thành công");
                sendMessage(gson.toJson(response));

                System.out.println("User " + userId + " đã kết nối");
            } catch (Exception e) {
                sendErrorResponse("Lỗi kết nối: " + e.getMessage());
            }
        }

        private void handleFileUpload(JsonObject request) {
            try {
                // Kiểm tra các tham số bắt buộc
                if (!request.has("fileName") || request.get("fileName").isJsonNull()) {
                    sendErrorResponse("Tên file không được để trống");
                    return;
                }

                if (!request.has("fileData") || request.get("fileData").isJsonNull()) {
                    sendErrorResponse("Dữ liệu file không được để trống");
                    return;
                }

                if (!request.has("projectId") || request.get("projectId").isJsonNull()) {
                    sendErrorResponse("ID đồ án không được để trống");
                    return;
                }

                String fileName = request.get("fileName").getAsString();
                String fileData = request.get("fileData").getAsString();
                int projectId = request.get("projectId").getAsInt();

                // Validate dữ liệu đầu vào
                if (fileName == null || fileName.trim().isEmpty()) {
                    sendErrorResponse("Tên file không hợp lệ");
                    return;
                }

                if (fileData == null || fileData.trim().isEmpty()) {
                    sendErrorResponse("Dữ liệu file không hợp lệ");
                    return;
                }

                if (projectId <= 0) {
                    sendErrorResponse("ID đồ án không hợp lệ: " + projectId);
                    return;
                }

                // Kiểm tra project tồn tại
                Project project = projectDAO.findById(projectId);
                if (project == null) {
                    sendErrorResponse("Không tìm thấy đồ án với ID: " + projectId);
                    return;
                }

                // Decode base64 file data
                byte[] decodedData;
                try {
                    decodedData = java.util.Base64.getDecoder().decode(fileData);
                } catch (IllegalArgumentException e) {
                    sendErrorResponse("Dữ liệu file không đúng định dạng Base64");
                    return;
                }

                // Tạo tên file unique với xử lý extension an toàn
                String timestamp = String.valueOf(System.currentTimeMillis());
                String extension = "";
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                    extension = fileName.substring(dotIndex);
                }

                String uniqueFileName = "project_" + projectId + "_" + timestamp + extension;
                String filePath = UPLOAD_DIR + uniqueFileName;

                // Validate file path
                if (filePath == null || filePath.trim().isEmpty()) {
                    sendErrorResponse("Đường dẫn file tải lên không hợp lệ: null");
                    return;
                }

                // Kiểm tra đường dẫn file hợp lệ
                Path targetPath = Paths.get(filePath);
                if (!targetPath.startsWith(Paths.get(UPLOAD_DIR))) {
                    sendErrorResponse("Đường dẫn file không an toàn");
                    return;
                }

                // Lưu file
                try {
                    Files.write(targetPath, decodedData);
                } catch (IOException e) {
                    sendErrorResponse("Không thể lưu file: " + e.getMessage());
                    return;
                }

                // Kiểm tra file đã được lưu thành công
                if (!Files.exists(targetPath)) {
                    sendErrorResponse("File không được lưu thành công");
                    return;
                }

                // Cập nhật database với file mới
                try {
                    synchronized (projectDAO) {
                        // Kiểm tra lại project trước khi cập nhật
                        Project updatedProject = projectDAO.findById(projectId);
                        if (updatedProject == null) {
                            // Xóa file đã tạo nếu project không tồn tại
                            Files.deleteIfExists(targetPath);
                            sendErrorResponse("Đồ án không tồn tại khi cập nhật database");
                            return;
                        }

                        // Thêm file path vào database
                        projectDAO.addFile(projectId, filePath);

                        // Cập nhật ngày nộp
                        updatedProject.setNgayNop(new java.util.Date());
                        projectDAO.updateProject(updatedProject);

                        // Refresh project data
                        project = projectDAO.findById(projectId);
                    }
                } catch (SQLException e) {
                    // Xóa file nếu cập nhật database thất bại
                    try {
                        Files.deleteIfExists(targetPath);
                    } catch (IOException ioException) {
                        System.err.println("Không thể xóa file sau khi lỗi database: " + ioException.getMessage());
                    }
                    sendErrorResponse("Lỗi cập nhật database: " + e.getMessage());
                    return;
                }

                // Gửi response thành công
                JsonObject response = new JsonObject();
                response.addProperty("action", "upload_response");
                response.addProperty("status", "success");
                response.addProperty("message", "Upload file thành công");
                response.addProperty("filePath", filePath);
                response.addProperty("fileName", uniqueFileName);
                response.addProperty("fileSize", decodedData.length);
                sendMessage(gson.toJson(response));

                // Gửi thông báo đến tất cả client
                if (project != null) {
                    broadcastNotification("Đồ án '" + project.getTitle() + "' đã được nộp", "project_submitted");
                }

                System.out.println("File uploaded successfully: " + filePath);

            } catch (Exception e) {
                sendErrorResponse("Lỗi upload file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void handleFileDownload(JsonObject request) {
            try {
                if (!request.has("projectId") || request.get("projectId").isJsonNull()) {
                    sendErrorResponse("ID đồ án không được để trống");
                    return;
                }

                int projectId = request.get("projectId").getAsInt();
                if (projectId <= 0) {
                    sendErrorResponse("ID đồ án không hợp lệ: " + projectId);
                    return;
                }

                Project project = projectDAO.findById(projectId);
                if (project == null) {
                    sendErrorResponse("Không tìm thấy đồ án với ID: " + projectId);
                    return;
                }

                String filePath = project.getLatestFilePath();
                if (filePath == null || filePath.trim().isEmpty()) {
                    sendErrorResponse("Không có file để tải xuống cho đồ án ID: " + projectId);
                    return;
                }

                File file = new File(filePath);
                if (!file.exists()) {
                    sendErrorResponse("File không tồn tại: " + filePath);
                    return;
                }

                if (!file.canRead()) {
                    sendErrorResponse("Không có quyền đọc file: " + filePath);
                    return;
                }

                // Đọc file và encode base64
                byte[] fileData;
                try {
                    fileData = Files.readAllBytes(file.toPath());
                } catch (IOException e) {
                    sendErrorResponse("Không thể đọc file: " + e.getMessage());
                    return;
                }

                String encodedData = java.util.Base64.getEncoder().encodeToString(fileData);

                JsonObject response = new JsonObject();
                response.addProperty("action", "download_response");
                response.addProperty("status", "success");
                response.addProperty("fileName", file.getName());
                response.addProperty("fileData", encodedData);
                response.addProperty("fileSize", file.length());
                sendMessage(gson.toJson(response));

                System.out.println("File downloaded: " + filePath);

            } catch (Exception e) {
                sendErrorResponse("Lỗi download file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void handleProjectSubmission(JsonObject request) {
            try {
                if (!request.has("projectId") || request.get("projectId").isJsonNull()) {
                    sendErrorResponse("ID đồ án không được để trống");
                    return;
                }

                if (!request.has("status") || request.get("status").isJsonNull()) {
                    sendErrorResponse("Trạng thái không được để trống");
                    return;
                }

                int projectId = request.get("projectId").getAsInt();
                String status = request.get("status").getAsString();

                if (projectId <= 0) {
                    sendErrorResponse("ID đồ án không hợp lệ: " + projectId);
                    return;
                }

                if (status == null || status.trim().isEmpty()) {
                    sendErrorResponse("Trạng thái không hợp lệ");
                    return;
                }

                Project project = projectDAO.findById(projectId);
                if (project == null) {
                    sendErrorResponse("Không tìm thấy đồ án với ID: " + projectId);
                    return;
                }

                // Cập nhật trạng thái
                project.setStatus(status);
                if ("DA_NOP".equals(status)) {
                    project.setNgayNop(new java.util.Date());
                }

                try {
                    projectDAO.updateProject(project);
                } catch (SQLException e) {
                    sendErrorResponse("Lỗi cập nhật database: " + e.getMessage());
                    return;
                }

                JsonObject response = new JsonObject();
                response.addProperty("action", "submit_response");
                response.addProperty("status", "success");
                response.addProperty("message", "Cập nhật trạng thái đồ án thành công");
                sendMessage(gson.toJson(response));

                // Thông báo cho các client khác
                String message = "Đồ án '" + project.getTitle() + "' đã được cập nhật trạng thái: " + status;
                broadcastNotification(message, "project_status_updated");

                System.out.println("Project status updated: " + projectId + " -> " + status);

            } catch (Exception e) {
                sendErrorResponse("Lỗi cập nhật đồ án: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void handleGetProjectStatus(JsonObject request) {
            try {
                if (!request.has("projectId") || request.get("projectId").isJsonNull()) {
                    sendErrorResponse("ID đồ án không được để trống");
                    return;
                }

                int projectId = request.get("projectId").getAsInt();
                if (projectId <= 0) {
                    sendErrorResponse("ID đồ án không hợp lệ: " + projectId);
                    return;
                }

                Project project = projectDAO.findById(projectId);
                if (project == null) {
                    sendErrorResponse("Không tìm thấy đồ án với ID: " + projectId);
                    return;
                }

                JsonObject response = new JsonObject();
                response.addProperty("action", "project_status_response");
                response.addProperty("status", "success");
                response.addProperty("projectId", project.getProjectId());
                response.addProperty("projectStatus", project.getStatus());
                response.addProperty("title", project.getTitle());
                response.addProperty("submissionDate", project.getNgayNop() != null ?
                        project.getNgayNop().toString() : null);
                sendMessage(gson.toJson(response));

            } catch (Exception e) {
                sendErrorResponse("Lỗi lấy trạng thái đồ án: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void sendErrorResponse(String errorMessage) {
            JsonObject response = new JsonObject();
            response.addProperty("action", "error");
            response.addProperty("status", "error");
            response.addProperty("message", errorMessage);
            response.addProperty("timestamp", System.currentTimeMillis());
            sendMessage(gson.toJson(response));

            System.err.println("Error sent to client: " + errorMessage);
        }

        public void sendMessage(String message) {
            if (writer != null && !socket.isClosed()) {
                try {
                    writer.println(message);
                    writer.flush();
                } catch (Exception e) {
                    System.err.println("Lỗi gửi message: " + e.getMessage());
                }
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

            // Thêm shutdown hook để đóng server một cách graceful
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Đang tắt server...");
                server.stop();
            }));

            server.start();
            System.out.println("Server đang chạy trên port 8888...");
        } catch (SQLException e) {
            System.err.println("Lỗi khởi động server: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Lỗi không mong muốn: " + e.getMessage());
            e.printStackTrace();
        }
    }
}