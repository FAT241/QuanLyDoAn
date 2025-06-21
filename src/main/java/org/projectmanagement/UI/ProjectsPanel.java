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

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Header panel với title và status
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Quản lý đồ án", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setBorder(new EmptyBorder(10, 20, 10, 0));

        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.setBackground(Color.WHITE);
        statusLabel = new JLabel(isSocketConnected ? "🟢 Kết nối" : "🔴 Mất kết nối");
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
        txtSearch = new JTextField(20);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnSearch = new JButton("Tìm kiếm");
        btnSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnSearch.setBackground(new Color(0, 123, 255));
        btnSearch.setForeground(Color.WHITE);
        btnReset = new JButton("Reset");
        btnReset.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnReset.setBackground(new Color(108, 117, 125));
        btnReset.setForeground(Color.WHITE);

        btnSearch.addActionListener(e -> searchProjects());
        btnReset.addActionListener(e -> loadProjectsAsync());

        searchPanel.add(new JLabel("Tìm kiếm:"));
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
        projectTable.setGridColor(new Color(200, 200, 200));
        projectTable.setShowGrid(true);
        projectTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        projectTable.getTableHeader().setBackground(new Color(230, 230, 230));
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
            btnAdd.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnAdd.setBackground(new Color(0, 123, 255));
            btnAdd.setForeground(Color.WHITE);
            btnAdd.addActionListener(e -> showAddProjectDialog());
            buttonPanel.add(btnAdd);
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

            btnAdd.setForeground(Color.WHITE);
            btnEdit.setForeground(Color.WHITE);
            btnDelete.setForeground(Color.WHITE);

            btnAdd.addActionListener(e -> showAddProjectDialog());
            btnEdit.addActionListener(e -> showEditProjectDialog());
            btnDelete.addActionListener(e -> deleteSelectedProject());

            buttonPanel.add(btnAdd);
            buttonPanel.add(btnEdit);
            buttonPanel.add(btnDelete);
        }
        add(buttonPanel, BorderLayout.SOUTH);
    }

    @Override
    public void onConnected() {
        SwingUtilities.invokeLater(() -> {
            isSocketConnected = true;
            statusLabel.setText("🟢 Kết nối");
            statusLabel.setForeground(Color.GREEN);
            showNotification("Kết nối thành công với server", "success");
        });
    }

    @Override
    public void onDisconnected() {
        SwingUtilities.invokeLater(() -> {
            isSocketConnected = false;
            statusLabel.setText("🔴 Mất kết nối");
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

    private void loadProjectsAsync() {
        SwingWorker<List<Project>, Void> worker = new SwingWorker<List<Project>, Void>() {
            @Override
            protected List<Project> doInBackground() throws SQLException {
                synchronized (projectDAO) {
                    return projectDAO.findAll();
                }
            }

            @Override
            protected void done() {
                try {
                    List<Project> projects = get();
                    if ("user".equals(loggedUser.getRole())) {
                        projects = projects.stream()
                                .filter(p -> p.getStudentId() == loggedUser.getUserId())
                                .collect(Collectors.toList());
                    }
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
                    return projectDAO.searchByTitleOrStudentId(keyword);
                }
            }

            @Override
            protected void done() {
                try {
                    List<Project> projects = get();
                    if ("user".equals(loggedUser.getRole())) {
                        projects = projects.stream()
                                .filter(p -> p.getStudentId() == loggedUser.getUserId())
                                .collect(Collectors.toList());
                    }
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
                "Ngày nộp", "Trạng thái", "File báo cáo", "Sinh viên", "Giảng viên", "Hành động"};
        Object[][] data = new Object[projects.size()][11];

        for (int i = 0; i < projects.size(); i++) {
            Project p = projects.get(i);
            JButton btnDownload = new JButton("Tải xuống");
            btnDownload.setEnabled(p.getTepBaoCao() != null && !p.getTepBaoCao().isEmpty());
            btnDownload.addActionListener(e -> downloadFileViaSocket(p.getTepBaoCao()));

            data[i] = new Object[]{
                    p.getProjectId(),
                    p.getTitle(),
                    p.getDescription(),
                    p.getNgayBatDau() != null ? DATE_FORMAT.format(p.getNgayBatDau()) : "Chưa đặt",
                    p.getNgayKetThuc() != null ? DATE_FORMAT.format(p.getNgayKetThuc()) : "Chưa đặt",
                    p.getNgayNop() != null ? DATE_FORMAT.format(p.getNgayNop()) : "Chưa nộp",
                    p.getStatus(),
                    p.getTepBaoCao(),
                    p.getStudentName() != null ? p.getStudentName() : "N/A",
                    p.getTeacherName() != null ? p.getTeacherName() : "N/A",
                    btnDownload
            };
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 10;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 10 ? JButton.class : Object.class;
            }
        };

        projectTable.setModel(model);
        projectTable.getColumnModel().getColumn(10).setCellRenderer(new ButtonRenderer());
        projectTable.getColumnModel().getColumn(10).setCellEditor(new ButtonEditor(new JCheckBox()));
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
        JTextArea txtDescription = new JTextArea(4, 30);
        txtDescription.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        JScrollPane scrollDescription = new JScrollPane(txtDescription);
        JTextField txtStartDate = new JTextField(30);
        txtStartDate.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JTextField txtEndDate = new JTextField(30);
        txtEndDate.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JTextField txtReportFile = new JTextField(20);
        txtReportFile.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtReportFile.setEditable(false);

        JComboBox<String> cbStudent = new JComboBox<>();
        cbStudent.setFont(new Font("Segoe UI", Font.PLAIN, 14));
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

        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"CHO_DUYET", "DUYET", "TU_CHOI", "DA_NOP"});
        cbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbStatus.setSelectedItem("CHO_DUYET");

        JButton btnChooseFile = new JButton("Chọn file");
        btnChooseFile.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnChooseFile.setBackground(new Color(108, 117, 125));
        btnChooseFile.setForeground(Color.WHITE);

        JButton btnSave = new JButton("Lưu");
        btnSave.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnSave.setBackground(new Color(0, 123, 255));
        btnSave.setForeground(Color.WHITE);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Tiêu đề:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(txtTitle, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Mô tả:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(scrollDescription, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Ngày bắt đầu (yyyy-MM-dd):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(txtStartDate, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Ngày kết thúc (yyyy-MM-dd):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(txtEndDate, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("File báo cáo:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(txtReportFile, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        dialog.add(btnChooseFile, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Sinh viên:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(cbStudent, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Giảng viên:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(cbTeacher, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Trạng thái:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(cbStatus, gbc);

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

                // Tạo project mới với đầy đủ 9 tham số
                Project project = new Project(0, title, description, startDate, endDate, null, sourceFilePath.isEmpty() ? null : sourceFilePath, studentId, teacherId);
                project.setStatus((String) cbStatus.getSelectedItem());

                // Lưu project vào database trước
                boolean success;
                int newProjectId;
                synchronized (projectDAO) {
                    success = projectDAO.addProject(project);
                    if (success) {
                        // Tìm project vừa thêm để lấy project_id
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

                    // Vô hiệu hóa nút Save để tránh click nhiều lần
                    btnSave.setEnabled(false);
                    btnSave.setText("Đang xử lý...");

                    if (checkSocketConnection()) {
                        uploadFileViaSocket(newProjectId, sourceFilePath, dialog, project, btnSave);
                    } else {
                        // Fallback to regular file upload
                        UIFileHandler.uploadFileWithProgress(dialog, sourceFilePath, result -> {
                            SwingUtilities.invokeLater(() -> {
                                if (result.isSuccess()) {
                                    try {
                                        project.setTepBaoCao(result.getFilePath());
                                        project.setNgayNop(new Date());
                                        synchronized (projectDAO) {
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
                                // Khôi phục nút Save
                                btnSave.setEnabled(true);
                                btnSave.setText("Lưu");
                            });
                        });
                    }
                } else {
                    // Không có file để tải lên
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
        // Kiểm tra file tồn tại trước khi upload
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

        // Tạo SwingWorker để xử lý upload không đồng bộ
        SwingWorker<Void, Integer> uploadWorker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // Kiểm tra kết nối socket
                    if (!isSocketConnected || socketClient == null) {
                        throw new Exception("Không có kết nối socket");
                    }

                    // Thực hiện upload với callback progress
                    socketClient.uploadFile(filePath, projectId, progress -> {
                        publish(progress); // Publish progress để cập nhật UI
                    }).thenAccept(result -> {
                        SwingUtilities.invokeLater(() -> {
                            progressDialog.dispose();
                            if (result.isSuccess()) {
                                try {
                                    String uploadedFilePath = result.getFilePath();
                                    if (uploadedFilePath == null || uploadedFilePath.isEmpty()) {
                                        throw new SQLException("Đường dẫn file tải lên không hợp lệ: " + uploadedFilePath);
                                    }
                                    System.out.println("Đường dẫn file tải lên: " + uploadedFilePath); // Debug log
                                    project.setTepBaoCao(uploadedFilePath);
                                    project.setNgayNop(new Date());
                                    synchronized (projectDAO) {
                                        boolean updateSuccess = projectDAO.updateProject(project);
                                        System.out.println("Cập nhật database: " + (updateSuccess ? "Thành công" : "Thất bại")); // Debug log
                                        if (!updateSuccess) {
                                            throw new SQLException("Cập nhật đường dẫn file vào database thất bại.");
                                        }
                                    }
                                    loadProjectsAsync();
                                    showNotification("Tải lên file và thêm đồ án thành công!", "success");
                                    parentDialog.dispose();
                                } catch (SQLException ex) {
                                    showNotification("Lỗi cập nhật database: " + ex.getMessage(), "error");
                                    System.err.println("Lỗi SQL khi cập nhật project: " + ex.getMessage()); // Debug log
                                }
                            } else {
                                showNotification("Lỗi tải lên file: " + (result.getMessage() != null ? result.getMessage() : "Upload failed"), "error");
                                System.err.println("Upload thất bại: " + result.getMessage()); // Debug log
                            }
                            btnSave.setEnabled(true);
                            btnSave.setText("Lưu");
                        });
                    }).exceptionally(throwable -> {
                        SwingUtilities.invokeLater(() -> {
                            progressDialog.dispose();
                            showNotification("Lỗi khi tải lên: " + throwable.getMessage(), "error");
                            System.err.println("Lỗi upload exception: " + throwable.getMessage()); // Debug log
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
                // Cập nhật progress bar với giá trị mới nhất
                if (!chunks.isEmpty()) {
                    int latestProgress = chunks.get(chunks.size() - 1);
                    progressBar.setValue(latestProgress);
                    progressBar.setString(latestProgress + "%");
                }
            }

            @Override
            protected void done() {
                try {
                    get(); // Kiểm tra exception
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
                        showNotification("Lỗi upload: " + e.getCause().getMessage(), "error");
                        btnSave.setEnabled(true);
                        btnSave.setText("Lưu");
                    });
                }
            }
        };

        // Hiển thị dialog trước khi bắt đầu upload
        SwingUtilities.invokeLater(() -> {
            progressDialog.setVisible(true);
        });

        // Bắt đầu tải lên
        uploadWorker.execute();
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
            dialog.setSize(600, 500);
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
            JTextArea txtDescription = new JTextArea(project.getDescription(), 4, 30);
            txtDescription.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtDescription.setLineWrap(true);
            txtDescription.setWrapStyleWord(true);
            JScrollPane scrollDescription = new JScrollPane(txtDescription);
            JTextField txtStartDate = new JTextField(project.getNgayBatDau() != null ? DATE_FORMAT.format(project.getNgayBatDau()) : "", 30);
            txtStartDate.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            JTextField txtEndDate = new JTextField(project.getNgayKetThuc() != null ? DATE_FORMAT.format(project.getNgayKetThuc()) : "", 30);
            txtEndDate.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            JTextField txtReportFile = new JTextField(project.getTepBaoCao() != null ? project.getTepBaoCao() : "", 20);
            txtReportFile.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtReportFile.setEditable(false);

            JComboBox<String> cbStudentComboBox = new JComboBox<>();
            cbStudentComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
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

            JComboBox<String> cbStatusComboBox = new JComboBox<>(new String[]{"CHO_DUYET", "DUYET", "TU_CHOI", "DA_NOP"});
            cbStatusComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            cbStatusComboBox.setSelectedItem(project.getStatus());

            JButton btnChooseFile = new JButton("Chọn file");
            btnChooseFile.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnChooseFile.setBackground(new Color(108, 117, 125));
            btnChooseFile.setForeground(Color.WHITE);
            JButton btnSaveButton = new JButton("Lưu");
            btnSaveButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnSaveButton.setBackground(new Color(0, 123, 255));
            btnSaveButton.setForeground(Color.WHITE);

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0.0;
            dialog.add(new JLabel("Tiêu đề:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtTitle, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0.0;
            dialog.add(new JLabel("Mô tả:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(scrollDescription, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0.0;
            dialog.add(new JLabel("Ngày bắt đầu (yyyy-MM-dd):"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtStartDate, gbc);

            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.weightx = 0.0;
            dialog.add(new JLabel("Ngày kết thúc (yyyy-MM-dd):"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtEndDate, gbc);

            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.weightx = 0.0;
            dialog.add(new JLabel("File báo cáo:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtReportFile, gbc);
            gbc.gridx = 2;
            gbc.weightx = 0.0;
            dialog.add(btnChooseFile, gbc);

            gbc.gridx = 0;
            gbc.gridy = 5;
            gbc.weightx = 0.0;
            dialog.add(new JLabel("Sinh viên:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(cbStudentComboBox, gbc);

            gbc.gridx = 0;
            gbc.gridy = 6;
            gbc.weightx = 0.0;
            dialog.add(new JLabel("Giảng viên:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(cbTeacherComboBox, gbc);

            gbc.gridx = 0;
            gbc.gridy = 7;
            gbc.weightx = 0.0;
            dialog.add(new JLabel("Trạng thái:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(cbStatusComboBox, gbc);

            gbc.gridx = 1;
            gbc.gridy = 8;
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

                    project.setTitle(txtTitle.getText().trim());
                    project.setDescription(txtDescription.getText().trim());
                    project.setNgayBatDau(startDate);
                    project.setNgayKetThuc(endDate);
                    project.setStudentId(studentId);
                    project.setTeacherId(teacherId);
                    project.setStatus((String) cbStatusComboBox.getSelectedItem());

                    if (!sourceFilePath.equals(project.getTepBaoCao()) && !sourceFilePath.isEmpty()) {
                        if (checkSocketConnection()) {
                            uploadFileViaSocket(project.getProjectId(), sourceFilePath, dialog, project, btnSaveButton);
                        } else {
                            UIFileHandler.uploadFileWithProgress(dialog, sourceFilePath, result -> {
                                if (result.isSuccess()) {
                                    try {
                                        project.setTepBaoCao(result.getFilePath());
                                        project.setNgayNop(new Date());
                                        synchronized (projectDAO) {
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
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa đồ án này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
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
            } catch (SQLException ex) {
                showNotification("Lỗi: " + ex.getMessage(), "error");
            }
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
                        // Optional: Additional actions after download completes
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