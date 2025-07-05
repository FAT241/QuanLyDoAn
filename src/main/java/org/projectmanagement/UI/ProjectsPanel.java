package org.projectmanagement.UI;

import org.projectmanagement.dao.ProjectDAO;
import org.projectmanagement.dao.StudentDAO;
import org.projectmanagement.dao.TeacherDAO;
import org.projectmanagement.models.Project;
import org.projectmanagement.models.Student;
import org.projectmanagement.models.Teacher;
import org.projectmanagement.models.User;
import org.projectmanagement.socket.ProjectSocketClient;
import org.projectmanagement.socket.SocketEventListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ProjectsPanel extends JPanel implements SocketEventListener {
    private ProjectDAO projectDAO;
    private User loggedUser;
    private Connection connection;
    private JTable projectTable;
    private JButton btnAdd, btnEdit, btnDelete, btnSearch, btnReset;
    private JTextField txtSearch;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private ProjectSocketClient socketClient;
    private JLabel statusLabel;
    private boolean isSocketConnected = false;

    public ProjectsPanel(User user, Connection connection) {
        this.loggedUser = user;
        this.connection = connection;
        this.projectDAO = new ProjectDAO(connection);
        initSocketClient();
        initComponents();
        loadProjectsAsync();
    }
// Khởi tạo socket client để lắng nghe các sự kiện từ server
    private void initSocketClient() {
        try {
            socketClient = new ProjectSocketClient();
            socketClient.setOnNotificationReceived(message -> onNotificationReceived(message.split(": ", 2)[1], message.split(": ", 2)[0]));
            socketClient.setOnFileUploadProgress(progress -> onFileUploadProgress(Integer.parseInt(progress)));
            socketClient.setOnFileDownloadProgress(progress -> onFileDownloadProgress(Integer.parseInt(progress)));
            socketClient.connect(String.valueOf(loggedUser.getUserId())).thenAccept(connected -> {
                SwingUtilities.invokeLater(() -> {
                    if (connected) {
                        isSocketConnected = true;
                        onConnected();
                    } else {
                        onDisconnected();
                    }
                });
            }).exceptionally(throwable -> {
                SwingUtilities.invokeLater(() -> {
                    System.err.println("Lỗi kết nối socket: " + throwable.getMessage());
                    onDisconnected();
                });
                return null;
            });
        } catch (Exception e) {
            System.err.println("Không thể khởi tạo socket client: " + e.getMessage());
            isSocketConnected = false;
            onDisconnected();
        }
    }
// Phương thức khởi tạo giao diện người dùng
    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Header panel với title và status
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Quản lý đồ án", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.BLACK);
        title.setBorder(new EmptyBorder(10, 20, 10, 0));

        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.setBackground(Color.WHITE);
        statusLabel = new JLabel(isSocketConnected ? "Kết nối" : "Mất kết nối");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(isSocketConnected ? Color.GREEN : Color.RED);
        statusPanel.add(statusLabel);

        headerPanel.add(title, BorderLayout.CENTER);
        headerPanel.add(statusPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JLabel searchLabel = new JLabel("Tìm kiếm:");
        searchLabel.setForeground(Color.BLACK);
        txtSearch = new JTextField(20);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSearch.setForeground(Color.BLACK);
        btnSearch = new JButton("Tìm kiếm");
        btnSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnSearch.setBackground(new Color(0, 123, 255));
        btnSearch.setForeground(Color.BLACK);
        btnReset = new JButton("Reset");
        btnReset.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnReset.setBackground(new Color(108, 117, 125));
        btnReset.setForeground(Color.BLACK);

        btnSearch.addActionListener(e -> searchProjects());
        btnReset.addActionListener(e -> loadProjectsAsync());

        searchPanel.add(searchLabel);
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnReset);

        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(searchPanel, BorderLayout.NORTH);

        // Project table
        projectTable = new JTable();
        projectTable.setRowHeight(30);
        projectTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        projectTable.setForeground(Color.BLACK);
        projectTable.setGridColor(new Color(200, 200, 200));
        projectTable.setShowGrid(true);
        projectTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        projectTable.getTableHeader().setBackground(new Color(230, 230, 230));
        projectTable.getTableHeader().setForeground(Color.BLACK);
        projectTable.getTableHeader().setReorderingAllowed(false);
        projectTable.setDefaultRenderer(Object.class, new CustomTableCellRenderer());

        JScrollPane scrollPane = new JScrollPane(projectTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);

        if ("user".equals(loggedUser.getRole())) {
            btnAdd = new JButton("Thêm đồ án");
            btnDelete = new JButton("Xóa đồ án"); // Thêm nút xóa cho role user

            btnAdd.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnDelete.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            btnAdd.setBackground(new Color(0, 123, 255)); // Blue for primary action
            btnDelete.setBackground(new Color(255, 0, 0)); // Red for delete action

            btnAdd.setForeground(Color.BLACK);
            btnDelete.setForeground(Color.BLACK);

            btnAdd.addActionListener(e -> showAddProjectDialog());
            btnDelete.addActionListener(e -> deleteSelectedProject());

            buttonPanel.add(btnAdd);
            buttonPanel.add(btnDelete);
        } else if ("admin".equals(loggedUser.getRole())) {
            btnAdd = new JButton("Thêm đồ án");
            btnEdit = new JButton("Sửa đồ án");
            btnDelete = new JButton("Xóa đồ án");

            btnAdd.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnEdit.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnDelete.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            btnAdd.setBackground(new Color(0, 123, 255));
            btnEdit.setBackground(new Color(0, 123, 255));
            btnDelete.setBackground(new Color(255, 0, 0));

            btnAdd.setForeground(Color.BLACK);
            btnEdit.setForeground(Color.BLACK);
            btnDelete.setForeground(Color.BLACK);

            btnAdd.addActionListener(e -> showAddProjectDialog());
            btnEdit.addActionListener(e -> showEditProjectDialog());
            btnDelete.addActionListener(e -> deleteSelectedProject());

            buttonPanel.add(btnAdd);
            buttonPanel.add(btnEdit);
            buttonPanel.add(btnDelete);
        } else if ("teacher".equals(loggedUser.getRole())) {
            btnEdit = new JButton("Sửa đồ án");
            btnEdit.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnEdit.setBackground(new Color(0, 123, 255));
            btnEdit.setForeground(Color.BLACK);
            btnEdit.addActionListener(e -> showEditProjectDialog());
            buttonPanel.add(btnEdit);
        }
        add(buttonPanel, BorderLayout.SOUTH);
    }

    @Override
    public void onConnected() {
        SwingUtilities.invokeLater(() -> {
            isSocketConnected = true;
            statusLabel.setText("Kết nối");
            statusLabel.setForeground(Color.GREEN);
        });
    }

    @Override
    public void onDisconnected() {
        SwingUtilities.invokeLater(() -> {
            isSocketConnected = false;
            statusLabel.setText("Mất kết nối");
            statusLabel.setForeground(Color.RED);
            showNotification("Mất kết nối với server", "error");
        });
    }

    @Override
    public void onNotificationReceived(String message, String type) {
        SwingUtilities.invokeLater(() -> {
            showNotification(message, type);
            if (type.contains("project")) {
                loadProjectsAsync();
            }
        });
    }

    @Override
    public void onFileUploadProgress(int progress) {
        SwingUtilities.invokeLater(() -> {
            // Update progress UI if needed
        });
    }

    @Override
    public void onFileDownloadProgress(int progress) {
        SwingUtilities.invokeLater(() -> {
            // Update progress UI if needed
        });
    }

    @Override
    public void onError(String error) {
        SwingUtilities.invokeLater(() -> {
            showNotification("Lỗi: " + error, "error");
        });
    }

    private void showNotification(String message, String type) {
        Color bgColor;
        switch (type) {
            case "success":
                bgColor = new Color(212, 237, 218);
                break;
            case "error":
                bgColor = new Color(248, 215, 218);
                break;
            case "warning":
                bgColor = new Color(255, 243, 205);
                break;
            default:
                bgColor = new Color(217, 237, 247);
        }

        JOptionPane optionPane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);
        optionPane.setBackground(bgColor);
        JDialog dialog = optionPane.createDialog(this, "Thông báo");
        Timer timer = new Timer(3000, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();
        dialog.setVisible(true);
    }
//Sử dụng SwingWorker để thực hiện truy vấn cơ sở dữ liệu trong thread nền, đảm bảo UI vẫn phản hồi.
    private void loadProjectsAsync() {
        SwingWorker<List<Project>, Void> worker = new SwingWorker<List<Project>, Void>() {
            @Override
            protected List<Project> doInBackground() throws SQLException {
                synchronized (projectDAO) {
                    List<Project> projects = projectDAO.findAll();
                    if ("user".equals(loggedUser.getRole())) {
                        StudentDAO studentDAO = new StudentDAO(connection);
                        List<Student> students = studentDAO.findAll();
                        Student currentStudent = students.stream()
                                .filter(s -> s.getUserId() == loggedUser.getUserId())
                                .findFirst()
                                .orElse(null);
                        if (currentStudent != null) {
                            final int studentId = currentStudent.getStudentId();
                            projects = projects.stream()
                                    .filter(p -> p.getStudentId() == studentId)
                                    .collect(Collectors.toList());
                        } else {
                            projects = new ArrayList<>();
                        }
                    } else if ("teacher".equals(loggedUser.getRole())) {
                        TeacherDAO teacherDAO = new TeacherDAO(connection);
                        List<Teacher> teachers = teacherDAO.findAll();
                        Teacher currentTeacher = teachers.stream()
                                .filter(t -> t.getEmail().equals(loggedUser.getEmail()))
                                .findFirst()
                                .orElse(null);
                        if (currentTeacher != null) {
                            final int teacherId = currentTeacher.getTeacherId();
                            projects = projects.stream()
                                    .filter(p -> p.getTeacherId() == teacherId)
                                    .collect(Collectors.toList());
                        } else {
                            projects = new ArrayList<>();
                        }
                    }
                    return projects;
                }
            }

            @Override
            protected void done() {
                try {
                    List<Project> projects = get();
                    updateTable(projects);
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(ProjectsPanel.this,
                            "Lỗi tải dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void searchProjects() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập từ khóa tìm kiếm.",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SwingWorker<List<Project>, Void> worker = new SwingWorker<List<Project>, Void>() {
            @Override
            protected List<Project> doInBackground() throws Exception {
                synchronized (projectDAO) {
                    List<Project> projects = projectDAO.searchByTitleOrStudentId(keyword);
                    if ("user".equals(loggedUser.getRole())) {
                        StudentDAO studentDAO = new StudentDAO(connection);
                        List<Student> students = studentDAO.findAll();
                        Student currentStudent = students.stream()
                                .filter(s -> s.getUserId() == loggedUser.getUserId())
                                .findFirst()
                                .orElse(null);
                        if (currentStudent != null) {
                            final int studentId = currentStudent.getStudentId();
                            projects = projects.stream()
                                    .filter(p -> p.getStudentId() == studentId)
                                    .collect(Collectors.toList());
                        } else {
                            projects = new ArrayList<>();
                        }
                    } else if ("teacher".equals(loggedUser.getRole())) {
                        TeacherDAO teacherDAO = new TeacherDAO(connection);
                        List<Teacher> teachers = teacherDAO.findAll();
                        Teacher currentTeacher = teachers.stream()
                                .filter(t -> t.getEmail().equals(loggedUser.getEmail()))
                                .findFirst()
                                .orElse(null);
                        if (currentTeacher != null) {
                            final int teacherId = currentTeacher.getTeacherId();
                            projects = projects.stream()
                                    .filter(p -> p.getTeacherId() == teacherId)
                                    .collect(Collectors.toList());
                        } else {
                            projects = new ArrayList<>();
                        }
                    }
                    return projects;
                }
            }

            @Override
            protected void done() {
                try {
                    List<Project> projects = get();
                    updateTable(projects);
                    if (projects.isEmpty()) {
                        JOptionPane.showMessageDialog(ProjectsPanel.this,
                                "Không tìm thấy đồ án nào.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(ProjectsPanel.this,
                            "Lỗi tìm kiếm: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateTable(List<Project> projects) {
        String[] columns = {"ID", "Tiêu đề", "Mô tả", "Ngày bắt đầu", "Ngày kết thúc",
                "Ngày nộp", "Trạng thái", "File báo cáo", "Sinh viên", "Giảng viên", "Nhận xét", "Hành động"};
        Object[][] data = new Object[projects.size()][12];

        for (int i = 0; i < projects.size(); i++) {
            Project p = projects.get(i);
            JButton btnDownload = new JButton("Tải xuống");
            btnDownload.setForeground(Color.BLACK);
            btnDownload.setEnabled(p.getLatestFilePath() != null && !p.getLatestFilePath().isEmpty());
            btnDownload.addActionListener(e -> downloadFileViaSocket(p.getLatestFilePath()));

            data[i] = new Object[]{
                    p.getProjectId(),
                    p.getTitle(),
                    p.getDescription(),
                    p.getNgayBatDau() != null ? DATE_FORMAT.format(p.getNgayBatDau()) : "Chưa đặt",
                    p.getNgayKetThuc() != null ? DATE_FORMAT.format(p.getNgayKetThuc()) : "Chưa đặt",
                    p.getNgayNop() != null ? DATE_FORMAT.format(p.getNgayNop()) : "Chưa nộp",
                    p.getStatus(),
                    p.getLatestFilePath(),
                    p.getStudentName() != null ? p.getStudentName() : "N/A",
                    p.getTeacherName() != null ? p.getTeacherName() : "N/A",
                    p.getLatestComment() != null ? p.getLatestComment() : "Chưa có nhận xét",
                    btnDownload
            };
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 11;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 11 ? JButton.class : Object.class;
            }
        };

        projectTable.setModel(model);
        projectTable.getColumnModel().getColumn(11).setCellRenderer(new ButtonRenderer());
        projectTable.getColumnModel().getColumn(11).setCellEditor(new ButtonEditor(new JCheckBox()));
        projectTable.getColumnModel().getColumn(10).setPreferredWidth(200);
    }

    private void downloadFileViaSocket(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có file để tải xuống!",
                    "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!checkSocketConnection()) {
            downloadFile(filePath);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(filePath.substring(filePath.lastIndexOf("/") + 1)));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String destFilePath = fileChooser.getSelectedFile().getAbsolutePath();

            JProgressBar progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            JDialog progressDialog = new JDialog();
            progressDialog.setTitle("Đang tải xuống...");
            progressDialog.add(progressBar);
            progressDialog.setSize(300, 100);
            progressDialog.setLocationRelativeTo(this);
            progressDialog.setVisible(true);

            socketClient.downloadFile(filePath, destFilePath, progress -> {
                SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
            }).thenAccept(result -> {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    if (result.isSuccess()) {
                        showNotification("Tải xuống thành công!", "success");
                    } else {
                        showNotification("Lỗi tải xuống: Upload failed", "error");
                    }
                });
            });
        }
    }

    private void downloadFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có file để tải xuống!",
                    "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(filePath.substring(filePath.lastIndexOf("/") + 1)));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String destFilePath = fileChooser.getSelectedFile().getAbsolutePath();
            UIFileHandler.downloadFileWithProgress(this, filePath, destFilePath, () -> {
                showNotification("Tải xuống thành công!", "success");
            });
        }
    }

    private void showAddProjectDialog() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Thêm đồ án");
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());
        dialog.setBackground(Color.WHITE);
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtTitle = new JTextField(30);
        txtTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtTitle.setForeground(Color.BLACK);
        JTextArea txtDescription = new JTextArea(4, 30);
        txtDescription.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtDescription.setForeground(Color.BLACK);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        JScrollPane scrollDescription = new JScrollPane(txtDescription);
        JTextField txtStartDate = new JTextField(30);
        txtStartDate.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtStartDate.setForeground(Color.BLACK);
        JTextField txtEndDate = new JTextField(30);
        txtEndDate.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtEndDate.setForeground(Color.BLACK);
        JTextField txtReportFile = new JTextField(20);
        txtReportFile.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtReportFile.setForeground(Color.BLACK);
        txtReportFile.setEditable(false);

        JComboBox<String> cbStudent = new JComboBox<>();
        cbStudent.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbStudent.setForeground(Color.BLACK);
        StudentDAO studentDAO = new StudentDAO(connection);
        try {
            List<Student> students = studentDAO.findAll();
            if (students.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không có sinh viên nào trong cơ sở dữ liệu.",
                        "Lỗi", JOptionPane.WARNING_MESSAGE);
                cbStudent.addItem("Không có sinh viên");
            } else {
                for (Student s : students) {
                    cbStudent.addItem(s.getFullName() + " (ID: " + s.getStudentId() + ")");
                }
                if ("user".equals(loggedUser.getRole())) {
                    Student matchingStudent = students.stream()
                            .filter(s -> s.getUserId() == loggedUser.getUserId())
                            .findFirst()
                            .orElse(null);
                    if (matchingStudent != null) {
                        cbStudent.setSelectedItem(matchingStudent.getFullName() + " (ID: " + matchingStudent.getStudentId() + ")");
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Không tìm thấy thông tin sinh viên của bạn trong cơ sở dữ liệu. (user_id: " + loggedUser.getUserId() + ")",
                                "Lỗi", JOptionPane.WARNING_MESSAGE);
                    }
                    cbStudent.setEnabled(false);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách sinh viên: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }

        JComboBox<String> cbTeacher = new JComboBox<>();
        cbTeacher.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbTeacher.setForeground(Color.BLACK);
        TeacherDAO teacherDAO = new TeacherDAO(connection);
        try {
            List<Teacher> teachers = teacherDAO.findAll();
            for (Teacher t : teachers) {
                cbTeacher.addItem(t.getFullName() + " (ID: " + t.getTeacherId() + ")");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách giảng viên: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }

        JLabel lblStatus = new JLabel("Trạng thái:");
        lblStatus.setForeground(Color.BLACK);
        JTextField txtStatus = new JTextField("CHO_DUYET", 30);
        txtStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtStatus.setForeground(Color.BLACK);
        txtStatus.setEditable(false);
        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"CHO_DUYET", "DUYET", "TU_CHOI", "DA_NOP"});
        cbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbStatus.setForeground(Color.BLACK);
        cbStatus.setSelectedItem("CHO_DUYET");

        JButton btnChooseFile = new JButton("Chọn file");
        btnChooseFile.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnChooseFile.setBackground(new Color(108, 117, 125));
        btnChooseFile.setForeground(Color.BLACK);

        JButton btnSave = new JButton("Lưu");
        btnSave.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnSave.setBackground(new Color(0, 123, 255));
        btnSave.setForeground(Color.BLACK);

        JLabel lblTitle = new JLabel("Tiêu đề:");
        lblTitle.setForeground(Color.BLACK);
        JLabel lblDescription = new JLabel("Mô tả:");
        lblDescription.setForeground(Color.BLACK);
        JLabel lblStartDate = new JLabel("Ngày bắt đầu (yyyy-MM-dd):");
        lblStartDate.setForeground(Color.BLACK);
        JLabel lblEndDate = new JLabel("Ngày kết thúc (yyyy-MM-dd):");
        lblEndDate.setForeground(Color.BLACK);
        JLabel lblReportFile = new JLabel("File báo cáo:");
        lblReportFile.setForeground(Color.BLACK);
        JLabel lblStudent = new JLabel("Sinh viên:");
        lblStudent.setForeground(Color.BLACK);
        JLabel lblTeacher = new JLabel("Giảng viên:");
        lblTeacher.setForeground(Color.BLACK);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        dialog.add(lblTitle, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(txtTitle, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        dialog.add(lblDescription, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(scrollDescription, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        dialog.add(lblStartDate, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(txtStartDate, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        dialog.add(lblEndDate, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(txtEndDate, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.0;
        dialog.add(lblReportFile, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(txtReportFile, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        dialog.add(btnChooseFile, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.0;
        dialog.add(lblStudent, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(cbStudent, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 0.0;
        dialog.add(lblTeacher, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(cbTeacher, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weightx = 0.0;
        dialog.add(lblStatus, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        if ("admin".equals(loggedUser.getRole())) {
            dialog.add(cbStatus, gbc);
        } else {
            dialog.add(txtStatus, gbc);
        }

        gbc.gridx = 1;
        gbc.gridy = 8;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        dialog.add(btnSave, gbc);

        btnChooseFile.addActionListener(e -> {
            String selectedPath = UIFileHandler.showFileChooserDialog(dialog, "Chọn file báo cáo");
            if (selectedPath != null) {
                txtReportFile.setText(selectedPath);
            }
        });

        btnSave.addActionListener(e -> {
            try {
                // Validate input
                String title = txtTitle.getText().trim();
                String description = txtDescription.getText().trim();

                if (title.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng nhập tiêu đề đồ án.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                if (description.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng nhập mô tả đồ án.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String startDateText = txtStartDate.getText().trim();
                String endDateText = txtEndDate.getText().trim();

                if (startDateText.isEmpty() || endDateText.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng nhập đầy đủ ngày bắt đầu và kết thúc.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                if (!startDateText.matches("\\d{4}-\\d{2}-\\d{2}") || !endDateText.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    JOptionPane.showMessageDialog(dialog, "Ngày phải có định dạng yyyy-MM-dd.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                Date startDate = DATE_FORMAT.parse(startDateText);
                Date endDate = DATE_FORMAT.parse(endDateText);

                if (startDate.after(endDate)) {
                    JOptionPane.showMessageDialog(dialog, "Ngày bắt đầu không thể sau ngày kết thúc.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String studentSelection = (String) cbStudent.getSelectedItem();
                String teacherSelection = (String) cbTeacher.getSelectedItem();

                if (studentSelection == null || teacherSelection == null || studentSelection.contains("Không có sinh viên")) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng chọn sinh viên và giảng viên.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int studentId = Integer.parseInt(studentSelection.substring(studentSelection.lastIndexOf(": ") + 2, studentSelection.lastIndexOf(")")));
                int teacherId = Integer.parseInt(teacherSelection.substring(teacherSelection.lastIndexOf(": ") + 2, teacherSelection.lastIndexOf(")")));

                String sourceFilePath = txtReportFile.getText().trim();

                // Tạo project mới với trạng thái mặc định là CHO_DUYET cho học sinh
                Project project = new Project(0, title, description, startDate, endDate, null, new ArrayList<>(), new ArrayList<>(), studentId, teacherId);
                String status = "admin".equals(loggedUser.getRole()) ? (String) cbStatus.getSelectedItem() : "CHO_DUYET";
                project.setStatus(status);

                // Lưu project vào database trước
                boolean success;
                int newProjectId;
                synchronized (projectDAO) {
                    success = projectDAO.addProject(project);
                    if (success) {
                        List<Project> projects = projectDAO.searchByTitleOrStudentId(title);
                        Project addedProject = projects.stream()
                                .filter(p -> p.getTitle().equals(title) && p.getStudentId() == studentId)
                                .findFirst()
                                .orElse(null);
                        if (addedProject != null) {
                            newProjectId = addedProject.getProjectId();
                            project.setProjectId(newProjectId);
                        } else {
                            throw new SQLException("Không tìm thấy dự án vừa thêm trong cơ sở dữ liệu.");
                        }
                    } else {
                        throw new SQLException("Thêm đồ án thất bại.");
                    }
                }

                // Nếu có file để upload
                if (!sourceFilePath.isEmpty()) {
                    File sourceFile = new File(sourceFilePath);
                    if (!sourceFile.exists() || !sourceFile.isFile()) {
                        JOptionPane.showMessageDialog(dialog, "File được chọn không tồn tại: " + sourceFilePath, "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    btnSave.setEnabled(false);
                    btnSave.setText("Đang xử lý...");

                    if (checkSocketConnection()) {
                        uploadFileViaSocket(newProjectId, sourceFilePath, dialog, project, btnSave);
                    } else {
                        UIFileHandler.uploadFileWithProgress(dialog, sourceFilePath, result -> {
                            SwingUtilities.invokeLater(() -> {
                                if (result.isSuccess()) {
                                    try {
                                        project.getFilePaths().add(result.getFilePath());
                                        synchronized (projectDAO) {
                                            projectDAO.addFile(newProjectId, result.getFilePath());
                                            project.setNgayNop(new Date());
                                            projectDAO.updateProject(project);
                                        }
                                        loadProjectsAsync();
                                        dialog.dispose();
                                        showNotification("Thêm đồ án thành công!", "success");
                                    } catch (Exception ex) {
                                        showNotification("Lỗi cập nhật sau khi tải lên: " + ex.getMessage(), "error");
                                    }
                                } else {
                                    showNotification("Lỗi tải lên file: Upload failed", "error");
                                }
                                btnSave.setEnabled(true);
                                btnSave.setText("Lưu");
                            });
                        });
                    }
                } else {
                    loadProjectsAsync();
                    dialog.dispose();
                    showNotification("Thêm đồ án thành công!", "success");
                }
            } catch (ParseException ex) {
                showNotification("Ngày không hợp lệ: " + ex.getMessage(), "error");
                btnSave.setEnabled(true);
                btnSave.setText("Lưu");
            } catch (Exception ex) {
                showNotification("Lỗi: " + ex.getMessage(), "error");
                btnSave.setEnabled(true);
                btnSave.setText("Lưu");
            }
        });

        dialog.setVisible(true);
    }

    private void uploadFileViaSocket(int projectId, String filePath, JDialog parentDialog, Project project, JButton btnSave) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            showNotification("File không tồn tại: " + filePath, "error");
            btnSave.setEnabled(true);
            btnSave.setText("Lưu");
            return;
        }

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        progressBar.setString("Đang chuẩn bị...");

        JDialog progressDialog = new JDialog(parentDialog, "Đang tải lên...", true);
        progressDialog.add(progressBar);
        progressDialog.setSize(300, 100);
        progressDialog.setLocationRelativeTo(parentDialog);

        SwingWorker<Void, Integer> uploadWorker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    if (!isSocketConnected || socketClient == null) {
                        throw new Exception("Không có kết nối socket");
                    }

                    socketClient.uploadFile(filePath, projectId, progress -> {
                        publish(progress);
                    }).thenAccept(result -> {
                        SwingUtilities.invokeLater(() -> {
                            progressDialog.dispose();

                            if (result.isSuccess()) {
                                try {
                                    // Kiểm tra kỹ lưỡng result trước khi xử lý
                                    String uploadedFilePath = result.getFilePath();

                                    // Debug logging
                                    System.out.println("Upload result: " + result);
                                    System.out.println("File path from result: " + uploadedFilePath);

                                    // Xử lý trường hợp filePath null hoặc empty
                                    if (uploadedFilePath == null || uploadedFilePath.trim().isEmpty()) {
                                        // Thử lấy từ các field khác của result nếu có
                                        String alternativePath = tryGetAlternativeFilePath(result, filePath);

                                        if (alternativePath != null && !alternativePath.trim().isEmpty()) {
                                            uploadedFilePath = alternativePath;
                                            System.out.println("Using alternative path: " + uploadedFilePath);
                                        } else {
                                            // Log chi tiết để debug
                                            System.err.println("Upload result details:");
                                            System.err.println("- Success: " + result.isSuccess());
                                            System.err.println("- Message: " + result.getMessage());
                                            System.err.println("- FilePath: " + result.getFilePath());

                                            throw new SQLException("Server không trả về đường dẫn file hợp lệ. " +
                                                    "Vui lòng kiểm tra cấu hình server hoặc thử lại.");
                                        }
                                    }

                                    // Thêm file vào project
                                    project.getFilePaths().add(uploadedFilePath);

                                    synchronized (projectDAO) {
                                        boolean fileAdded = projectDAO.addFile(projectId, uploadedFilePath);
                                        if (fileAdded) {
                                            project.setNgayNop(new Date());
                                            projectDAO.updateProject(project);
                                            showNotification("Tải lên file và thêm đồ án thành công!", "success");
                                        } else {
                                            showNotification("Đồ án đã được tạo nhưng có lỗi khi liên kết file.", "warning");
                                        }
                                    }

                                    loadProjectsAsync();
                                    parentDialog.dispose();

                                } catch (SQLException ex) {
                                    System.err.println("SQL Exception in upload process: " + ex.getMessage());
                                    ex.printStackTrace();
                                    showNotification("Lỗi xử lý file: " + ex.getMessage(), "error");
                                } catch (Exception ex) {
                                    System.err.println("General Exception in upload process: " + ex.getMessage());
                                    ex.printStackTrace();
                                    showNotification("Lỗi không xác định: " + ex.getMessage(), "error");
                                }
                            } else {
                                String errorMsg = result.getMessage() != null ? result.getMessage() : "Upload failed";
                                System.err.println("Upload failed: " + errorMsg);
                                showNotification("Lỗi tải lên file: " + errorMsg, "error");
                            }

                            // Reset button state
                            btnSave.setEnabled(true);
                            btnSave.setText("Lưu");
                        });
                    }).exceptionally(throwable -> {
                        SwingUtilities.invokeLater(() -> {
                            progressDialog.dispose();
                            System.err.println("Upload exception: " + throwable.getMessage());
                            throwable.printStackTrace();

                            String errorMsg = throwable.getCause() != null ?
                                    throwable.getCause().getMessage() : throwable.getMessage();
                            showNotification("Lỗi kết nối: " + errorMsg, "error");

                            btnSave.setEnabled(true);
                            btnSave.setText("Lưu");
                        });
                        return null;
                    });

                } catch (Exception e) {
                    throw new Exception("Lỗi trong quá trình upload: " + e.getMessage(), e);
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    int latestProgress = chunks.get(chunks.size() - 1);
                    progressBar.setValue(latestProgress);
                    progressBar.setString(latestProgress + "%");
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        showNotification("Upload bị gián đoạn", "error");
                        btnSave.setEnabled(true);
                        btnSave.setText("Lưu");
                    });
                } catch (ExecutionException e) {
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        Throwable cause = e.getCause();
                        String errorMsg = cause != null ? cause.getMessage() : e.getMessage();
                        showNotification("Lỗi upload: " + errorMsg, "error");
                        System.err.println("ExecutionException in upload: " + errorMsg);
                        e.printStackTrace();
                        btnSave.setEnabled(true);
                        btnSave.setText("Lưu");
                    });
                }
            }
        };

        SwingUtilities.invokeLater(() -> {
            progressDialog.setVisible(true);
        });

        uploadWorker.execute();
    }

    private String tryGetAlternativeFilePath(Object result, String originalFilePath) {
        try {
            // Thử reflection để lấy các field khác của result object
            Class<?> resultClass = result.getClass();

            // Thử các tên field có thể có
            String[] possibleFields = {"path", "fileName", "uploadPath", "serverPath", "relativePath"};

            for (String fieldName : possibleFields) {
                try {
                    java.lang.reflect.Field field = resultClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(result);
                    if (value != null && !value.toString().trim().isEmpty()) {
                        return value.toString();
                    }
                } catch (NoSuchFieldException | IllegalAccessException ignored) {
                    // Tiếp tục thử field khác
                }
            }

            // Nếu không có field nào, thử sử dụng tên file gốc
            if (originalFilePath != null) {
                File file = new File(originalFilePath);
                return file.getName(); // Chỉ lấy tên file, không có path
            }

        } catch (Exception e) {
            System.err.println("Error trying to get alternative file path: " + e.getMessage());
        }

        return null;
    }


    private void showEditProjectDialog() {
        int selectedRow = projectTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đồ án để sửa.",
                    "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int projectId = (int) projectTable.getValueAt(selectedRow, 0);
        try {
            Project project = projectDAO.findById(projectId);
            JDialog dialog = new JDialog();
            dialog.setTitle("Sửa đồ án");
            dialog.setSize(600, 600);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new GridBagLayout());
            dialog.setBackground(Color.WHITE);
            ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JTextField txtTitle = new JTextField(project.getTitle(), 30);
            txtTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtTitle.setForeground(Color.BLACK);
            JTextArea txtDescription = new JTextArea(project.getDescription(), 4, 30);
            txtDescription.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtDescription.setForeground(Color.BLACK);
            txtDescription.setLineWrap(true);
            txtDescription.setWrapStyleWord(true);
            JScrollPane scrollDescription = new JScrollPane(txtDescription);
            JTextField txtStartDate = new JTextField(project.getNgayBatDau() != null ? DATE_FORMAT.format(project.getNgayBatDau()) : "", 30);
            txtStartDate.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtStartDate.setForeground(Color.BLACK);
            JTextField txtEndDate = new JTextField(project.getNgayKetThuc() != null ? DATE_FORMAT.format(project.getNgayKetThuc()) : "", 30);
            txtEndDate.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtEndDate.setForeground(Color.BLACK);
            JTextField txtReportFile = new JTextField(project.getLatestFilePath() != null ? project.getLatestFilePath() : "", 20);
            txtReportFile.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtReportFile.setForeground(Color.BLACK);
            txtReportFile.setEditable(false);

            JTextArea txtComment = new JTextArea(project.getLatestComment() != null ? project.getLatestComment() : "", 4, 30);
            txtComment.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtComment.setForeground(Color.BLACK);
            txtComment.setLineWrap(true);
            txtComment.setWrapStyleWord(true);
            JScrollPane scrollComment = new JScrollPane(txtComment);

            JComboBox<String> cbStudentComboBox = new JComboBox<>();
            cbStudentComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            cbStudentComboBox.setForeground(Color.BLACK);
            StudentDAO studentDAO = new StudentDAO(connection);
            try {
                List<Student> students = studentDAO.findAll();
                for (Student s : students) {
                    cbStudentComboBox.addItem(s.getFullName() + " (ID: " + s.getStudentId() + ")");
                }
                cbStudentComboBox.setSelectedItem(project.getStudentName() + " (ID: " + project.getStudentId() + ")");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi tải danh sách sinh viên: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }

            JComboBox<String> cbTeacherComboBox = new JComboBox<>();
            cbTeacherComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            cbTeacherComboBox.setForeground(Color.BLACK);
            TeacherDAO teacherDAO = new TeacherDAO(connection);
            try {
                List<Teacher> teachers = teacherDAO.findAll();
                for (Teacher t : teachers) {
                    cbTeacherComboBox.addItem(t.getFullName() + " (ID: " + t.getTeacherId() + ")");
                }
                cbTeacherComboBox.setSelectedItem(project.getTeacherName() + " (ID: " + project.getTeacherId() + ")");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi tải danh sách giáo viên: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }

            JTextField txtStatus = new JTextField(project.getStatus(), 30);
            txtStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtStatus.setForeground(Color.BLACK);
            txtStatus.setEditable(false);
            JComboBox<String> cbStatusComboBox = new JComboBox<>(new String[]{"CHO_DUYET", "DUYET", "TU_CHOI", "DA_NOP"});
            cbStatusComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            cbStatusComboBox.setForeground(Color.BLACK);
            cbStatusComboBox.setSelectedItem(project.getStatus());

            JButton btnChooseFile = new JButton("Chọn file");
            btnChooseFile.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnChooseFile.setBackground(new Color(108, 117, 125));
            btnChooseFile.setForeground(Color.BLACK);
            JButton btnSaveButton = new JButton("Lưu");
            btnSaveButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnSaveButton.setBackground(new Color(0, 123, 255));
            btnSaveButton.setForeground(Color.BLACK);

            // Hạn chế chỉnh sửa dựa trên vai trò
            if ("user".equals(loggedUser.getRole())) {
                txtTitle.setEnabled(false);
                txtDescription.setEnabled(false);
                txtStartDate.setEnabled(false);
                txtEndDate.setEnabled(false);
                txtReportFile.setEnabled(false);
                btnChooseFile.setEnabled(false);
                cbStudentComboBox.setEnabled(false);
                cbTeacherComboBox.setEnabled(false);
                txtStatus.setEnabled(false);
                cbStatusComboBox.setEnabled(false);
                txtComment.setEnabled(false);
                JOptionPane.showMessageDialog(this, "Bạn không có quyền chỉnh sửa đồ án.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                dialog.dispose();
                return;
            } else if ("teacher".equals(loggedUser.getRole())) {
                txtTitle.setEnabled(false);
                txtDescription.setEnabled(false);
                txtStartDate.setEnabled(false);
                txtEndDate.setEnabled(false);
                txtReportFile.setEnabled(false);
                btnChooseFile.setEnabled(false);
                cbStudentComboBox.setEnabled(false);
                cbTeacherComboBox.setEnabled(false);
                cbStatusComboBox.setEnabled(true);
                txtComment.setEnabled(true);
            } else if ("admin".equals(loggedUser.getRole())) {
                txtComment.setEnabled(false); // Admin chỉ xem nhận xét
            }

            JLabel lblTitle = new JLabel("Tiêu đề:");
            lblTitle.setForeground(Color.BLACK);
            JLabel lblDescription = new JLabel("Mô tả:");
            lblDescription.setForeground(Color.BLACK);
            JLabel lblStartDate = new JLabel("Ngày bắt đầu (yyyy-MM-dd):");
            lblStartDate.setForeground(Color.BLACK);
            JLabel lblEndDate = new JLabel("Ngày kết thúc (yyyy-MM-dd):");
            lblEndDate.setForeground(Color.BLACK);
            JLabel lblReportFile = new JLabel("File báo cáo:");
            lblReportFile.setForeground(Color.BLACK);
            JLabel lblStudent = new JLabel("Sinh viên:");
            lblStudent.setForeground(Color.BLACK);
            JLabel lblTeacher = new JLabel("Giảng viên:");
            lblTeacher.setForeground(Color.BLACK);
            JLabel lblStatus = new JLabel("Trạng thái:");
            lblStatus.setForeground(Color.BLACK);
            JLabel lblComment = new JLabel("Nhận xét:");
            lblComment.setForeground(Color.BLACK);

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0.0;
            dialog.add(lblTitle, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtTitle, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0.0;
            dialog.add(lblDescription, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(scrollDescription, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0.0;
            dialog.add(lblStartDate, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtStartDate, gbc);

            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.weightx = 0.0;
            dialog.add(lblEndDate, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtEndDate, gbc);

            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.weightx = 0.0;
            dialog.add(lblReportFile, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtReportFile, gbc);
            gbc.gridx = 2;
            gbc.weightx = 0.0;
            dialog.add(btnChooseFile, gbc);

            gbc.gridx = 0;
            gbc.gridy = 5;
            gbc.weightx = 0.0;
            dialog.add(lblStudent, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(cbStudentComboBox, gbc);

            gbc.gridx = 0;
            gbc.gridy = 6;
            gbc.weightx = 0.0;
            dialog.add(lblTeacher, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(cbTeacherComboBox, gbc);

            gbc.gridx = 0;
            gbc.gridy = 7;
            gbc.weightx = 0.0;
            dialog.add(lblStatus, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            if ("admin".equals(loggedUser.getRole()) || "teacher".equals(loggedUser.getRole())) {
                dialog.add(cbStatusComboBox, gbc);
            } else {
                dialog.add(txtStatus, gbc);
            }

            gbc.gridx = 0;
            gbc.gridy = 8;
            gbc.weightx = 0.0;
            dialog.add(lblComment, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(scrollComment, gbc);

            gbc.gridx = 1;
            gbc.gridy = 9;
            gbc.weightx = 0.0;
            gbc.anchor = GridBagConstraints.CENTER;
            dialog.add(btnSaveButton, gbc);

            btnChooseFile.addActionListener(e -> {
                String selectedPath = UIFileHandler.showFileChooserDialog(dialog, "Chọn file báo cáo");
                if (selectedPath != null) {
                    txtReportFile.setText(selectedPath);
                }
            });

            btnSaveButton.addActionListener(e -> {
                try {
                    String startDateText = txtStartDate.getText().trim();
                    String endDateText = txtEndDate.getText().trim();
                    if (!startDateText.matches("\\d{4}-\\d{2}-\\d{2}") || !endDateText.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        JOptionPane.showMessageDialog(dialog, "Ngày phải có định dạng yyyy-MM-dd.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    Date startDate = DATE_FORMAT.parse(startDateText);
                    Date endDate = DATE_FORMAT.parse(endDateText);

                    String studentSelection = (String) cbStudentComboBox.getSelectedItem();
                    String teacherSelection = (String) cbTeacherComboBox.getSelectedItem();
                    int studentId = Integer.parseInt(studentSelection.substring(studentSelection.lastIndexOf(": ") + 2, studentSelection.lastIndexOf(")")));
                    int teacherId = Integer.parseInt(teacherSelection.substring(teacherSelection.lastIndexOf(": ") + 2, teacherSelection.lastIndexOf(")")));

                    String sourceFilePath = txtReportFile.getText().trim();
                    String comment = txtComment.getText().trim();

                    project.setTitle(txtTitle.getText().trim());
                    project.setDescription(txtDescription.getText().trim());
                    project.setNgayBatDau(startDate);
                    project.setNgayKetThuc(endDate);
                    project.setStudentId(studentId);
                    project.setTeacherId(teacherId);
                    if ("admin".equals(loggedUser.getRole()) || "teacher".equals(loggedUser.getRole())) {
                        project.setStatus((String) cbStatusComboBox.getSelectedItem());
                    } else {
                        project.setStatus(project.getStatus()); // Giữ nguyên trạng thái
                    }

                    if (!comment.isEmpty() && "teacher".equals(loggedUser.getRole())) {
                        synchronized (projectDAO) {
                            projectDAO.addComment(projectId, teacherId, comment);
                            project.getComments().add(comment);
                        }
                    }

                    if (!sourceFilePath.isEmpty() && !sourceFilePath.equals(project.getLatestFilePath())) {
                        if (checkSocketConnection()) {
                            uploadFileViaSocket(project.getProjectId(), sourceFilePath, dialog, project, btnSaveButton);
                        } else {
                            UIFileHandler.uploadFileWithProgress(dialog, sourceFilePath, result -> {
                                if (result.isSuccess()) {
                                    try {
                                        project.getFilePaths().add(result.getFilePath());
                                        synchronized (projectDAO) {
                                            projectDAO.addFile(projectId, result.getFilePath());
                                            project.setNgayNop(new Date());
                                            projectDAO.updateProject(project);
                                        }
                                        loadProjectsAsync();
                                        dialog.dispose();
                                        showNotification("Sửa đồ án thành công!", "success");
                                    } catch (Exception ex) {
                                        showNotification("Lỗi: " + ex.getMessage(), "error");
                                    }
                                }
                            });
                        }
                    } else {
                        synchronized (projectDAO) {
                            projectDAO.updateProject(project);
                        }
                        loadProjectsAsync();
                        dialog.dispose();
                        showNotification("Sửa đồ án thành công!", "success");
                    }
                } catch (ParseException ex) {
                    showNotification("Ngày không hợp lệ: " + ex.getMessage(), "error");
                } catch (Exception ex) {
                    showNotification("Lỗi: " + ex.getMessage(), "error");
                }
            });

            dialog.setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedProject() {
        int selectedRow = projectTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đồ án để xóa.",
                    "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int projectId = (int) projectTable.getValueAt(selectedRow, 0);
        String filePath = (String) projectTable.getValueAt(selectedRow, 7);
        String projectStatus = (String) projectTable.getValueAt(selectedRow, 6);

        try {
            Project project = projectDAO.findById(projectId);

            // Kiểm tra quyền xóa cho role user
            if ("user".equals(loggedUser.getRole())) {
                StudentDAO studentDAO = new StudentDAO(connection);
                Student currentStudent = studentDAO.findAll().stream()
                        .filter(s -> s.getUserId() == loggedUser.getUserId())
                        .findFirst()
                        .orElse(null);
                if (currentStudent == null || project.getStudentId() != currentStudent.getStudentId()) {
                    JOptionPane.showMessageDialog(this, "Bạn không có quyền xóa đồ án này.",
                            "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                // Chỉ cho phép xóa khi trạng thái là CHO_DUYET
                if (!"CHO_DUYET".equals(projectStatus)) {
                    JOptionPane.showMessageDialog(this, "Chỉ có thể xóa đồ án ở trạng thái 'Chờ duyệt'.",
                            "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn xóa đồ án này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (filePath != null && !filePath.isEmpty()) {
                    try {
                        EnhancedFileManager.safeDeleteFile(filePath);
                    } catch (IOException ex) {
                        showNotification("Lỗi khi xóa file: " + ex.getMessage(), "error");
                    }
                }
                synchronized (projectDAO) {
                    projectDAO.deleteProject(projectId);
                }
                loadProjectsAsync();
                showNotification("Xóa đồ án thành công!", "success");
            }
        } catch (SQLException ex) {
            showNotification("Lỗi: " + ex.getMessage(), "error");
        }
    }

    private boolean checkSocketConnection() {
        if (!isSocketConnected || socketClient == null) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Kết nối socket bị mất. Bạn có muốn thử kết nối lại không?",
                    "Mất kết nối", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                initSocketClient();
                return isSocketConnected;
            }
            return false;
        }
        return true;
    }

    public static class EnhancedFileManager {
        public static void safeDeleteFile(String filePath) throws IOException {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        }
    }

    static class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setForeground(Color.BLACK);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "Tải xuống" : value.toString());
            setEnabled(value != null && !value.toString().isEmpty());
            return this;
        }
    }

    static class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String filePath;
        private int row;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setForeground(Color.BLACK);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            this.row = row;
            filePath = (String) table.getValueAt(row, 7);
            button.setText("Tải xuống");
            button.setEnabled(filePath != null && !filePath.isEmpty());
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (filePath != null && !filePath.isEmpty()) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new File(filePath.substring(filePath.lastIndexOf("/") + 1)));
                if (fileChooser.showSaveDialog(button) == JFileChooser.APPROVE_OPTION) {
                    String destFilePath = fileChooser.getSelectedFile().getAbsolutePath();
                    UIFileHandler.downloadFileWithProgress(button, filePath, destFilePath, () -> {
                    });
                }
            }
            return filePath;
        }
    }

    static class CustomTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            c.setForeground(Color.BLACK);
            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
            } else {
                c.setBackground(Color.WHITE);
                c.setForeground(Color.BLACK);
            }
            return c;
        }
    }
}