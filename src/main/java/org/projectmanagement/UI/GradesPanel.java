package org.projectmanagement.UI;

import org.projectmanagement.dao.ProjectDAO;
import org.projectmanagement.dao.StudentDAO;
import org.projectmanagement.models.Project;
import org.projectmanagement.models.Student;
import org.projectmanagement.models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class GradesPanel extends JPanel {
    private ProjectDAO projectDAO;
    private User loggedUser;
    private Connection connection;
    private JTable gradesTable;
    private JButton btnAddScore, btnEditScore;
    private JTextField txtSearch;
    private JButton btnSearch, btnReset;
    private List<Project> currentProjects; // Cache hiện tại

    public GradesPanel(User user, Connection connection) {
        this.loggedUser = user;
        this.connection = connection;
        this.projectDAO = new ProjectDAO(connection);
        this.currentProjects = new ArrayList<>();
        initComponents();
        loadGradesAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        JLabel title = new JLabel("Quản lý điểm số", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.BLACK);
        title.setBorder(new EmptyBorder(10, 20, 10, 0));
        headerPanel.add(title, BorderLayout.CENTER);
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

        btnSearch.addActionListener(e -> searchGrades());
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            loadGradesAsync();
        });

        searchPanel.add(searchLabel);
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnReset);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.add(searchPanel, BorderLayout.NORTH);

        // Grades table
        gradesTable = new JTable();
        gradesTable.setRowHeight(30);
        gradesTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gradesTable.setForeground(Color.BLACK);
        gradesTable.setGridColor(new Color(200, 200, 200));
        gradesTable.setShowGrid(true);
        gradesTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        gradesTable.getTableHeader().setBackground(new Color(230, 230, 230));
        gradesTable.getTableHeader().setForeground(Color.BLACK);
        gradesTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(gradesTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);

        if ("admin".equals(loggedUser.getRole()) || "teacher".equals(loggedUser.getRole())) {
            btnAddScore = new JButton("Thêm điểm");
            btnEditScore = new JButton("Sửa điểm");
            btnAddScore.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnEditScore.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnAddScore.setBackground(new Color(0, 123, 255));
            btnEditScore.setBackground(new Color(0, 123, 255));
            btnAddScore.setForeground(Color.BLACK);
            btnEditScore.setForeground(Color.BLACK);

            btnAddScore.addActionListener(e -> showAddScoreDialog());
            btnEditScore.addActionListener(e -> showEditScoreDialog());

            buttonPanel.add(btnAddScore);
            buttonPanel.add(btnEditScore);
        }

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadGradesAsync() {
        // Disable buttons to prevent multiple operations
        setButtonsEnabled(false);

        SwingWorker<List<Project>, Void> worker = new SwingWorker<List<Project>, Void>() {
            @Override
            protected List<Project> doInBackground() throws SQLException {
                // Force refresh from database
                List<Project> projects = projectDAO.findAll();
                List<Project> filteredProjects = new ArrayList<>();

                if ("teacher".equals(loggedUser.getRole())) {
                    int teacherId = getTeacherId();
                    for (Project p : projects) {
                        if (p.getTeacherId() == teacherId) {
                            filteredProjects.add(p);
                        }
                    }
                } else if ("user".equals(loggedUser.getRole())) {
                    StudentDAO studentDAO = new StudentDAO(connection);
                    List<Student> students = studentDAO.findAll();
                    Student currentStudent = null;
                    for (Student s : students) {
                        if (s.getUserId() == loggedUser.getUserId()) {
                            currentStudent = s;
                            break;
                        }
                    }
                    if (currentStudent != null) {
                        int studentId = currentStudent.getStudentId();
                        for (Project p : projects) {
                            if (p.getStudentId() == studentId) {
                                filteredProjects.add(p);
                            }
                        }
                    }
                } else {
                    filteredProjects.addAll(projects);
                }
                return filteredProjects;
            }

            @Override
            protected void done() {
                try {
                    List<Project> projects = get();
                    currentProjects.clear();
                    currentProjects.addAll(projects);
                    updateTable(projects);
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(GradesPanel.this,
                            "Lỗi tải dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setButtonsEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void searchGrades() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập từ khóa tìm kiếm.",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setButtonsEnabled(false);

        SwingWorker<List<Project>, Void> worker = new SwingWorker<List<Project>, Void>() {
            @Override
            protected List<Project> doInBackground() throws SQLException {
                List<Project> projects = projectDAO.searchByTitleOrStudentId(keyword);
                List<Project> filteredProjects = new ArrayList<>();

                if ("teacher".equals(loggedUser.getRole())) {
                    int teacherId = getTeacherId();
                    for (Project p : projects) {
                        if (p.getTeacherId() == teacherId) {
                            filteredProjects.add(p);
                        }
                    }
                } else if ("user".equals(loggedUser.getRole())) {
                    StudentDAO studentDAO = new StudentDAO(connection);
                    List<Student> students = studentDAO.findAll();
                    Student currentStudent = null;
                    for (Student s : students) {
                        if (s.getUserId() == loggedUser.getUserId()) {
                            currentStudent = s;
                            break;
                        }
                    }
                    if (currentStudent != null) {
                        int studentId = currentStudent.getStudentId();
                        for (Project p : projects) {
                            if (p.getStudentId() == studentId) {
                                filteredProjects.add(p);
                            }
                        }
                    }
                } else {
                    filteredProjects.addAll(projects);
                }
                return filteredProjects;
            }

            @Override
            protected void done() {
                try {
                    List<Project> projects = get();
                    currentProjects.clear();
                    currentProjects.addAll(projects);
                    updateTable(projects);
                    if (projects.isEmpty()) {
                        JOptionPane.showMessageDialog(GradesPanel.this,
                                "Không tìm thấy đồ án nào.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(GradesPanel.this,
                            "Lỗi tìm kiếm: " + e.getMessage(), "Lỗi", JOptionPane.WARNING_MESSAGE);
                } finally {
                    setButtonsEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void updateTable(List<Project> projects) {
        String[] columns = {"ID", "Tên Đồ án", "Điểm Quá Trình", "Điểm Bảo Vệ", "Điểm Tổng Kết", "Xếp Loại"};
        Object[][] data = new Object[projects.size()][6];

        for (int i = 0; i < projects.size(); i++) {
            Project p = projects.get(i);
            data[i] = new Object[]{
                    p.getProjectId(),
                    p.getTitle(),
                    p.getProcessScore() != null ? String.format("%.2f", p.getProcessScore()) : "Chưa có",
                    p.getDefenseScore() != null ? String.format("%.2f", p.getDefenseScore()) : "Chưa có",
                    p.getFinalScore() != null ? String.format("%.2f", p.getFinalScore()) : "Chưa có",
                    p.getGrade() != null ? p.getGrade() : "Chưa có"
            };
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        gradesTable.setModel(model);

        // Hide ID column but keep it for reference
        gradesTable.getColumnModel().getColumn(0).setMinWidth(0);
        gradesTable.getColumnModel().getColumn(0).setMaxWidth(0);
        gradesTable.getColumnModel().getColumn(0).setWidth(0);
    }

    private void showAddScoreDialog() {
        // Check if there are any projects displayed
        if (currentProjects.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Không có đồ án nào để thêm điểm. Vui lòng tải lại dữ liệu.",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog();
        dialog.setTitle("Thêm điểm số");
        dialog.setSize(600, 400); // Increased size to match ProjectsPanel
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());
        dialog.setBackground(Color.WHITE);
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Added padding

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Consistent insets
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<String> cbProject = new JComboBox<>();
        cbProject.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbProject.setForeground(Color.BLACK);

        // Only add projects that are currently displayed in the table
        for (Project p : currentProjects) {
            // Additional security check for teachers
            if ("teacher".equals(loggedUser.getRole())) {
                int teacherId = getTeacherId();
                if (p.getTeacherId() != teacherId) {
                    continue; // Skip projects not owned by this teacher
                }
            }
            cbProject.addItem(p.getTitle() + " (ID: " + p.getProjectId() + ")");
        }

        // Check if any projects are available after filtering
        if (cbProject.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Không có đồ án nào để thêm điểm.",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JTextField txtProcess = new JTextField(20);
        txtProcess.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtProcess.setForeground(Color.BLACK);
        JTextField txtDefense = new JTextField(20);
        txtDefense.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtDefense.setForeground(Color.BLACK);
        JButton btnSave = new JButton("Lưu");
        btnSave.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnSave.setBackground(new Color(0, 123, 255));
        btnSave.setForeground(Color.BLACK);

        // Separate labels with black foreground
        JLabel lblProject = new JLabel("Đồ án:");
        lblProject.setForeground(Color.BLACK);
        JLabel lblProcess = new JLabel("Điểm Quá Trình:");
        lblProcess.setForeground(Color.BLACK);
        JLabel lblDefense = new JLabel("Điểm Bảo Vệ:");
        lblDefense.setForeground(Color.BLACK);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        dialog.add(lblProject, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(cbProject, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        dialog.add(lblProcess, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(txtProcess, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        dialog.add(lblDefense, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(txtDefense, gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        dialog.add(btnSave, gbc);

        btnSave.addActionListener(e -> {
            try {
                String projectSelection = (String) cbProject.getSelectedItem();
                if (projectSelection == null) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng chọn đồ án.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String idStr = projectSelection.substring(projectSelection.lastIndexOf(": ") + 2, projectSelection.lastIndexOf(")"));
                int projectId;
                try {
                    projectId = Integer.parseInt(idStr);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "ID đồ án không hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Find the selected project from current projects to verify ownership
                Project selectedProject = null;
                for (Project p : currentProjects) {
                    if (p.getProjectId() == projectId) {
                        selectedProject = p;
                        break;
                    }
                }

                if (selectedProject == null) {
                    JOptionPane.showMessageDialog(dialog, "Đồ án được chọn không tồn tại trong danh sách hiện tại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Additional security check for teachers
                if ("teacher".equals(loggedUser.getRole())) {
                    int teacherId = getTeacherId();
                    if (selectedProject.getTeacherId() != teacherId) {
                        JOptionPane.showMessageDialog(dialog,
                                "Bạn không có quyền chấm điểm đồ án này.",
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                Double processScore = txtProcess.getText().trim().isEmpty() ? null : Double.parseDouble(txtProcess.getText().trim());
                Double defenseScore = txtDefense.getText().trim().isEmpty() ? null : Double.parseDouble(txtDefense.getText().trim());

                if ((processScore != null && (processScore < 0 || processScore > 10)) || (defenseScore != null && (defenseScore < 0 || defenseScore > 10))) {
                    JOptionPane.showMessageDialog(dialog, "Điểm phải nằm trong khoảng từ 0 đến 10.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Update in database
                boolean success = projectDAO.updateScores(projectId, processScore, defenseScore);

                if (success) {
                    dialog.dispose();
                    showNotification("Thêm điểm số thành công!", "success");

                    // Force refresh data after successful update
                    SwingUtilities.invokeLater(() -> {
                        loadGradesAsync();
                    });
                } else {
                    JOptionPane.showMessageDialog(dialog, "Cập nhật điểm thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Điểm phải là số hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    private void showEditScoreDialog() {
        int selectedRow = gradesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đồ án để sửa điểm.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get project ID from hidden column
        int projectId = (Integer) gradesTable.getValueAt(selectedRow, 0);

        try {
            // Get fresh data from database
            Project selectedProject = projectDAO.findById(projectId);

            if (selectedProject == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy đồ án.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Security check: verify teacher owns this project
            if ("teacher".equals(loggedUser.getRole())) {
                int teacherId = getTeacherId();
                if (selectedProject.getTeacherId() != teacherId) {
                    JOptionPane.showMessageDialog(this,
                            "Bạn không có quyền chấm điểm đồ án này.",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            JDialog dialog = new JDialog();
            dialog.setTitle("Sửa điểm số");
            dialog.setSize(600, 400); // Increased size to match ProjectsPanel
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new GridBagLayout());
            dialog.setBackground(Color.WHITE);
            ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Added padding

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10); // Consistent insets
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JTextField txtProject = new JTextField(selectedProject.getTitle(), 20);
            txtProject.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtProject.setForeground(Color.BLACK);
            txtProject.setEditable(false);
            JTextField txtProcess = new JTextField(selectedProject.getProcessScore() != null ? String.format("%.2f", selectedProject.getProcessScore()) : "", 20);
            txtProcess.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtProcess.setForeground(Color.BLACK);
            JTextField txtDefense = new JTextField(selectedProject.getDefenseScore() != null ? String.format("%.2f", selectedProject.getDefenseScore()) : "", 20);
            txtDefense.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtDefense.setForeground(Color.BLACK);
            JButton btnSave = new JButton("Lưu");
            btnSave.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnSave.setBackground(new Color(0, 123, 255));
            btnSave.setForeground(Color.BLACK);

            // Separate labels with black foreground
            JLabel lblProject = new JLabel("Đồ án:");
            lblProject.setForeground(Color.BLACK);
            JLabel lblProcess = new JLabel("Điểm Quá Trình:");
            lblProcess.setForeground(Color.BLACK);
            JLabel lblDefense = new JLabel("Điểm Bảo Vệ:");
            lblDefense.setForeground(Color.BLACK);

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0.0;
            dialog.add(lblProject, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtProject, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0.0;
            dialog.add(lblProcess, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtProcess, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0.0;
            dialog.add(lblDefense, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            dialog.add(txtDefense, gbc);

            gbc.gridx = 1;
            gbc.gridy = 3;
            gbc.weightx = 0.0;
            gbc.anchor = GridBagConstraints.CENTER;
            dialog.add(btnSave, gbc);

            btnSave.addActionListener(e -> {
                try {
                    // Additional security check before saving
                    if ("teacher".equals(loggedUser.getRole())) {
                        int teacherId = getTeacherId();
                        if (selectedProject.getTeacherId() != teacherId) {
                            JOptionPane.showMessageDialog(dialog,
                                    "Bạn không có quyền chấm điểm đồ án này.",
                                    "Lỗi",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }

                    Double processScore = txtProcess.getText().trim().isEmpty() ? null : Double.parseDouble(txtProcess.getText().trim());
                    Double defenseScore = txtDefense.getText().trim().isEmpty() ? null : Double.parseDouble(txtDefense.getText().trim());

                    if ((processScore != null && (processScore < 0 || processScore > 10)) || (defenseScore != null && (defenseScore < 0 || defenseScore > 10))) {
                        JOptionPane.showMessageDialog(dialog, "Điểm phải nằm trong khoảng từ 0 đến 10.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    boolean success = projectDAO.updateScores(selectedProject.getProjectId(), processScore, defenseScore);

                    if (success) {
                        dialog.dispose();
                        showNotification("Sửa điểm số thành công!", "success");

                        // Force refresh data after successful update
                        SwingUtilities.invokeLater(() -> {
                            loadGradesAsync();
                        });
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Cập nhật điểm thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "Điểm phải là số hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.WARNING_MESSAGE);
                }
            });

            dialog.setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int getTeacherId() {
        try {
            String sql = "SELECT teacher_id FROM teachers WHERE email = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, loggedUser.getEmail());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("teacher_id");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi lấy teacher_id: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
        return -1;
    }

    private void setButtonsEnabled(boolean enabled) {
        if (btnAddScore != null) btnAddScore.setEnabled(enabled);
        if (btnEditScore != null) btnEditScore.setEnabled(enabled);
        btnSearch.setEnabled(enabled);
        btnReset.setEnabled(enabled);
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
}