package org.projectmanagement.UI;

import org.projectmanagement.dao.ProjectDAO;
import org.projectmanagement.dao.StudentDAO;
import org.projectmanagement.dao.TeacherDAO;
import org.projectmanagement.models.Project;
import org.projectmanagement.models.Student;
import org.projectmanagement.models.Teacher;
import org.projectmanagement.models.User;

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ProjectsPanel extends JPanel {
    private ProjectDAO projectDAO;
    private User loggedUser;
    private Connection connection;
    private JTable projectTable;
    private JButton btnAdd, btnEdit, btnDelete, btnSearch, btnReset;
    private JTextField txtSearch;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public ProjectsPanel(User user, Connection connection) {
        this.loggedUser = user;
        this.connection = connection;
        this.projectDAO = new ProjectDAO(connection);
        initComponents();
        loadProjectsAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Tiêu đề
        JLabel title = new JLabel("Quản lý đồ án", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setBorder(new EmptyBorder(10, 20, 10, 0));
        add(title, BorderLayout.NORTH);

        // Thanh tìm kiếm
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
        add(searchPanel, BorderLayout.NORTH);

        // Bảng đồ án
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
        add(scrollPane, BorderLayout.CENTER);

        // Nút hành động
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
                    JOptionPane.showMessageDialog(ProjectsPanel.this, "Lỗi tải dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void searchProjects() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập từ khóa tìm kiếm.", "Thông báo", JOptionPane.WARNING_MESSAGE);
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
                        JOptionPane.showMessageDialog(ProjectsPanel.this, "Không tìm thấy đồ án nào.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(ProjectsPanel.this, "Lỗi tìm kiếm: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateTable(List<Project> projects) {
        String[] columns = {"ID", "Tiêu đề", "Mô tả", "Ngày bắt đầu", "Ngày kết thúc", "Ngày nộp", "Trạng thái", "File báo cáo", "Sinh viên", "Giảng viên", "Hành động"};
        Object[][] data = new Object[projects.size()][11];
        for (int i = 0; i < projects.size(); i++) {
            Project p = projects.get(i);
            JButton btnDownload = new JButton("Tải xuống");
            btnDownload.setEnabled(p.getTepBaoCao() != null && !p.getTepBaoCao().isEmpty());
            btnDownload.addActionListener(e -> downloadFile(p.getTepBaoCao()));
            data[i] = new Object[]{
                    p.getProjectId(),
                    p.getTitle(),
                    p.getDescription(),
                    p.getNgayBatDau() != null ? DATE_FORMAT.format(p.getNgayBatDau()) : "",
                    p.getNgayKetThuc() != null ? DATE_FORMAT.format(p.getNgayKetThuc()) : "",
                    p.getNgayNop() != null ? DATE_FORMAT.format(p.getNgayNop()) : "",
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

    private void downloadFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có file để tải xuống!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(filePath.substring(filePath.lastIndexOf("/") + 1)));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String destFilePath = fileChooser.getSelectedFile().getAbsolutePath();
            UIFileHandler.downloadFileWithProgress(this, filePath, destFilePath, () -> {
                // Optional: Additional actions after download completes
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
        ((JComponent)dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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

        // Populate student combo box
        JComboBox<String> cbStudent = new JComboBox<>();
        cbStudent.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        StudentDAO studentDAO = new StudentDAO(connection);
        try {
            List<Student> students = studentDAO.findAll();
            System.out.println("Danh sách sinh viên từ DB: " + students); // Debug log
            if (students.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không có sinh viên nào trong cơ sở dữ liệu.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                cbStudent.addItem("Không có sinh viên");
            } else {
                for (Student s : students) {
                    cbStudent.addItem(s.getFullName() + " (ID: " + s.getStudentId() + ")");
                    System.out.println("Sinh viên: " + s.getFullName() + ", student_id: " + s.getStudentId() + ", user_id: " + s.getUserId()); // Debug log
                }
                if ("user".equals(loggedUser.getRole())) {
                    System.out.println("loggedUser.getUserId(): " + loggedUser.getUserId() + ", Type: " + loggedUser.getUserId()); // Debug log
                    Student matchingStudent = students.stream()
                            .filter(s -> s.getUserId() == loggedUser.getUserId()) // So sánh user_id
                            .findFirst()
                            .orElse(null);
                    if (matchingStudent != null) {
                        cbStudent.setSelectedItem(matchingStudent.getFullName() + " (ID: " + matchingStudent.getStudentId() + ")");
                    } else {
                        JOptionPane.showMessageDialog(this, "Không tìm thấy thông tin sinh viên của bạn trong cơ sở dữ liệu. (user_id: " + loggedUser.getUserId() + ")", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    }
                    cbStudent.setEnabled(false);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách sinh viên: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }

        // Populate teacher combo box
        JComboBox<String> cbTeacher = new JComboBox<>();
        cbTeacher.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        TeacherDAO teacherDAO = new TeacherDAO(connection);
        try {
            List<Teacher> teachers = teacherDAO.findAll();
            for (Teacher t : teachers) {
                cbTeacher.addItem(t.getFullName() + " (ID: " + t.getTeacherId() + ")");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách giảng viên: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Tiêu đề:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(txtTitle, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Mô tả:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(scrollDescription, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Ngày bắt đầu (yyyy-MM-dd):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(txtStartDate, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Ngày kết thúc (yyyy-MM-dd):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(txtEndDate, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("File báo cáo:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(txtReportFile, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        dialog.add(btnChooseFile, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Sinh viên:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(cbStudent, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Giảng viên:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(cbTeacher, gbc);

        gbc.gridx = 0; gbc.gridy = 7;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Trạng thái:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(cbStatus, gbc);

        gbc.gridx = 1; gbc.gridy = 8;
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
                String startDateText = txtStartDate.getText().trim();
                String endDateText = txtEndDate.getText().trim();
                if (!startDateText.matches("\\d{4}-\\d{2}-\\d{2}") || !endDateText.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    JOptionPane.showMessageDialog(dialog, "Ngày phải có định dạng yyyy-MM-dd.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Extract studentId and teacherId from selected combo box items
                String studentSelection = (String) cbStudent.getSelectedItem();
                String teacherSelection = (String) cbTeacher.getSelectedItem();
                int studentId = Integer.parseInt(studentSelection.substring(studentSelection.lastIndexOf(": ") + 2, studentSelection.lastIndexOf(")")));
                int teacherId = Integer.parseInt(teacherSelection.substring(teacherSelection.lastIndexOf(": ") + 2, teacherSelection.lastIndexOf(")")));

                String sourceFilePath = txtReportFile.getText();
                final String[] savedFilePath = {null};

                if (!sourceFilePath.isEmpty()) {
                    UIFileHandler.uploadFileWithProgress(dialog, sourceFilePath, result -> {
                        if (result.isSuccess()) {
                            savedFilePath[0] = result.getFilePath();
                            try {
                                Date startDate = DATE_FORMAT.parse(startDateText);
                                Date endDate = DATE_FORMAT.parse(endDateText);
                                Project project = new Project(
                                        0,
                                        txtTitle.getText(),
                                        txtDescription.getText(),
                                        startDate,
                                        endDate,
                                        null,
                                        savedFilePath[0],
                                        studentId,
                                        teacherId
                                );
                                project.setStatus((String) cbStatus.getSelectedItem());
                                synchronized (projectDAO) {
                                    projectDAO.addProject(project);
                                }
                                loadProjectsAsync();
                                dialog.dispose();
                                JOptionPane.showMessageDialog(ProjectsPanel.this, "Thêm đồ án thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(ProjectsPanel.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });
                } else {
                    Date startDate = DATE_FORMAT.parse(startDateText);
                    Date endDate = DATE_FORMAT.parse(endDateText);
                    Project project = new Project(
                            0,
                            txtTitle.getText(),
                            txtDescription.getText(),
                            startDate,
                            endDate,
                            null,
                            null,
                            studentId,
                            teacherId
                    );
                    project.setStatus((String) cbStatus.getSelectedItem());
                    synchronized (projectDAO) {
                        projectDAO.addProject(project);
                    }
                    loadProjectsAsync();
                    dialog.dispose();
                    JOptionPane.showMessageDialog(this, "Thêm đồ án thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    private void showEditProjectDialog() {
        int selectedRow = projectTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đồ án để sửa.", "Lỗi", JOptionPane.WARNING_MESSAGE);
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
            ((JComponent)dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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
            JTextField txtStudentId = new JTextField(String.valueOf(project.getStudentId()), 30);
            txtStudentId.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            JTextField txtTeacherId = new JTextField(String.valueOf(project.getTeacherId()), 30);
            txtTeacherId.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            JComboBox<String> cbStatus = new JComboBox<>(new String[]{"CHO_DUYET", "DUYET", "TU_CHOI", "DA_NOP"});
            cbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            cbStatus.setSelectedItem(project.getStatus());
            JButton btnChooseFile = new JButton("Chọn file");
            btnChooseFile.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnChooseFile.setBackground(new Color(108, 117, 125));
            btnChooseFile.setForeground(Color.WHITE);
            JButton btnSave = new JButton("Lưu");
            btnSave.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnSave.setBackground(new Color(0, 123, 255));
            btnSave.setForeground(Color.WHITE);

            gbc.gridx = 0; gbc.gridy = 0;
            gbc.weightx = 0.0;
            dialog.add(new JLabel("Tiêu đề:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtTitle, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            gbc.weightx = 0.0;
            dialog.add(new JLabel("Mô tả:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(scrollDescription, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            gbc.weightx = 0.0;
            dialog.add(new JLabel("Ngày bắt đầu (yyyy-MM-dd):"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtStartDate, gbc);

            gbc.gridx = 0; gbc.gridy = 3;
            gbc.weightx = 0.0;
            dialog.add(new JLabel("Ngày kết thúc (yyyy-MM-dd):"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtEndDate, gbc);

            gbc.gridx = 0; gbc.gridy = 4;
            gbc.weightx = 0.0;
            dialog.add(new JLabel("File báo cáo:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtReportFile, gbc);
            gbc.gridx = 2;
            gbc.weightx = 0.0;
            dialog.add(btnChooseFile, gbc);

            gbc.gridx = 0; gbc.gridy = 5;
            gbc.weightx = 0.0;
            dialog.add(new JLabel("ID Sinh viên:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtStudentId, gbc);

            gbc.gridx = 0; gbc.gridy = 6;
            gbc.weightx = 0.0;
            dialog.add(new JLabel("ID Giảng viên:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtTeacherId, gbc);

            gbc.gridx = 0; gbc.gridy = 7;
            gbc.weightx = 0.0;
            dialog.add(new JLabel("Trạng thái:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(cbStatus, gbc);

            gbc.gridx = 1; gbc.gridy = 8;
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
                    String startDateText = txtStartDate.getText().trim();
                    String endDateText = txtEndDate.getText().trim();
                    if (!startDateText.matches("\\d{4}-\\d{2}-\\d{2}") || !endDateText.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        JOptionPane.showMessageDialog(dialog, "Ngày phải có định dạng yyyy-MM-dd.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    String sourceFilePath = txtReportFile.getText();
                    final String[] savedFilePath = {project.getTepBaoCao()};

                    if (!sourceFilePath.equals(project.getTepBaoCao()) && !sourceFilePath.isEmpty()) {
                        UIFileHandler.uploadFileWithProgress(dialog, sourceFilePath, result -> {
                            if (result.isSuccess()) {
                                savedFilePath[0] = result.getFilePath();
                                try {
                                    Date startDate = DATE_FORMAT.parse(startDateText);
                                    Date endDate = DATE_FORMAT.parse(endDateText);
                                    project.setTitle(txtTitle.getText());
                                    project.setDescription(txtDescription.getText());
                                    project.setNgayBatDau(startDate);
                                    project.setNgayKetThuc(endDate);
                                    project.setTepBaoCao(savedFilePath[0]);
                                    project.setStudentId(Integer.parseInt(txtStudentId.getText()));
                                    project.setTeacherId(Integer.parseInt(txtTeacherId.getText()));
                                    project.setStatus((String) cbStatus.getSelectedItem());
                                    synchronized (projectDAO) {
                                        projectDAO.updateProject(project);
                                    }
                                    loadProjectsAsync();
                                    dialog.dispose();
                                    JOptionPane.showMessageDialog(ProjectsPanel.this, "Sửa đồ án thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(ProjectsPanel.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        });
                    } else {
                        Date startDate = DATE_FORMAT.parse(startDateText);
                        Date endDate = DATE_FORMAT.parse(endDateText);
                        project.setTitle(txtTitle.getText());
                        project.setDescription(txtDescription.getText());
                        project.setNgayBatDau(startDate);
                        project.setNgayKetThuc(endDate);
                        project.setTepBaoCao(savedFilePath[0]);
                        project.setStudentId(Integer.parseInt(txtStudentId.getText()));
                        project.setTeacherId(Integer.parseInt(txtTeacherId.getText()));
                        project.setStatus((String) cbStatus.getSelectedItem());
                        synchronized (projectDAO) {
                            projectDAO.updateProject(project);
                        }
                        loadProjectsAsync();
                        dialog.dispose();
                        JOptionPane.showMessageDialog(ProjectsPanel.this, "Sửa đồ án thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProjectsPanel.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đồ án để xóa.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int projectId = (int) projectTable.getValueAt(selectedRow, 0);
        String filePath = (String) projectTable.getValueAt(selectedRow, 7);
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa đồ án này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (filePath != null && !filePath.isEmpty()) {
                    try {
                        EnhancedFileManager.safeDeleteFile(filePath);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this, "Lỗi khi xóa file: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
                synchronized (projectDAO) {
                    projectDAO.deleteProject(projectId);
                }
                loadProjectsAsync();
                JOptionPane.showMessageDialog(this, "Xóa đồ án thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
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
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
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
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
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
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
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