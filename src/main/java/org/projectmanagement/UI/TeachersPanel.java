package org.projectmanagement.UI;

import org.projectmanagement.dao.TeacherDAO;
import org.projectmanagement.models.Teacher;
import org.projectmanagement.models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TeachersPanel extends JPanel {
    private User loggedUser;
    private Connection connection;
    private TeacherDAO teacherDAO;
    private JTable teacherTable;
    private JButton btnAdd, btnEdit, btnDelete, btnSearch, btnReset;
    private JTextField txtSearch;

    public TeachersPanel(User user, Connection connection) {
        this.loggedUser = user;
        this.connection = connection;
        this.teacherDAO = new TeacherDAO(connection);
        initComponents();
        loadTeachersAsync();
    }
//
    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Tiêu đề
        JLabel title = new JLabel("Quản lý giảng viên", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.BLACK);
        title.setBorder(new EmptyBorder(10, 0, 10, 0));

        // Thanh tìm kiếm
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

        btnSearch.addActionListener(e -> searchTeachers());
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            loadTeachersAsync();
        });

        searchPanel.add(searchLabel);
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnReset);

        // Container for title and search panel
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(Color.WHITE);
        northPanel.add(title, BorderLayout.NORTH);
        northPanel.add(searchPanel, BorderLayout.CENTER);
        add(northPanel, BorderLayout.NORTH);

        // Bảng giảng viên
        teacherTable = new JTable();
        teacherTable.setRowHeight(30);
        teacherTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        teacherTable.setForeground(Color.BLACK);
        teacherTable.setGridColor(new Color(200, 200, 200));
        teacherTable.setShowGrid(true);
        teacherTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        teacherTable.getTableHeader().setForeground(Color.BLACK);
        teacherTable.getTableHeader().setBackground(new Color(230, 230, 230));
        teacherTable.getTableHeader().setReorderingAllowed(false);
        teacherTable.setDefaultRenderer(Object.class, new CustomTableCellRenderer());
        JScrollPane scrollPane = new JScrollPane(teacherTable);
        add(scrollPane, BorderLayout.CENTER);

        // Nút hành động
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);

        btnAdd = new JButton("Thêm giảng viên");
        btnEdit = new JButton("Sửa giảng viên");
        btnDelete = new JButton("Xóa giảng viên");

        btnAdd.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnEdit.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnDelete.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        btnAdd.setBackground(new Color(0, 123, 255));
        btnEdit.setBackground(new Color(0, 123, 255));
        btnDelete.setBackground(new Color(255, 0, 0));

        btnAdd.setForeground(Color.BLACK);
        btnEdit.setForeground(Color.BLACK);
        btnDelete.setForeground(Color.BLACK);

        btnAdd.addActionListener(e -> showAddTeacherDialog());
        btnEdit.addActionListener(e -> showEditTeacherDialog());
        btnDelete.addActionListener(e -> deleteSelectedTeacher());

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadTeachersAsync() {
        SwingWorker<List<Teacher>, Void> worker = new SwingWorker<List<Teacher>, Void>() {
            @Override
            protected List<Teacher> doInBackground() throws Exception {
                synchronized (teacherDAO) {
                    return teacherDAO.findAll();
                }
            }

            @Override
            protected void done() {
                try {
                    List<Teacher> teachers = get();
                    updateTable(teachers);
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(TeachersPanel.this, "Lỗi tải dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void searchTeachers() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập từ khóa tìm kiếm.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SwingWorker<List<Teacher>, Void> worker = new SwingWorker<List<Teacher>, Void>() {
            @Override
            protected List<Teacher> doInBackground() throws Exception {
                synchronized (teacherDAO) {
                    return teacherDAO.searchByNameOrEmail(keyword);
                }
            }

            @Override
            protected void done() {
                try {
                    List<Teacher> teachers = get();
                    updateTable(teachers);
                    if (teachers.isEmpty()) {
                        JOptionPane.showMessageDialog(TeachersPanel.this, "Không tìm thấy giảng viên nào.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(TeachersPanel.this, "Lỗi tìm kiếm: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateTable(List<Teacher> teachers) {
        String[] columns = {"ID", "Tên", "Email", "Số điện thoại", "Chức vụ"};
        Object[][] data = new Object[teachers.size()][5];
        for (int i = 0; i < teachers.size(); i++) {
            Teacher t = teachers.get(i);
            data[i] = new Object[]{
                    t.getTeacherId(),
                    t.getFullName(),
                    t.getEmail(),
                    t.getPhoneNumber(),
                    t.getPosition()
            };
        }
        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        teacherTable.setModel(model);
    }

    private void showAddTeacherDialog() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Thêm giảng viên");
        dialog.setSize(600, 400); // Increased size to match ProjectsPanel
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());
        dialog.setBackground(Color.WHITE);
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Added padding

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Consistent insets
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtFullName = new JTextField(20);
        txtFullName.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtFullName.setForeground(Color.BLACK);
        JTextField txtEmail = new JTextField(20);
        txtEmail.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtEmail.setForeground(Color.BLACK);
        JTextField txtPhoneNumber = new JTextField(20);
        txtPhoneNumber.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPhoneNumber.setForeground(Color.BLACK);
        JTextField txtPosition = new JTextField(20);
        txtPosition.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPosition.setForeground(Color.BLACK);
        JButton btnSave = new JButton("Lưu");
        btnSave.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnSave.setBackground(new Color(0, 123, 255));
        btnSave.setForeground(Color.BLACK);

        JLabel lblFullName = new JLabel("Tên:");
        lblFullName.setForeground(Color.BLACK);
        JLabel lblEmail = new JLabel("Email:");
        lblEmail.setForeground(Color.BLACK);
        JLabel lblPhoneNumber = new JLabel("Số điện thoại:");
        lblPhoneNumber.setForeground(Color.BLACK);
        JLabel lblPosition = new JLabel("Chức vụ:");
        lblPosition.setForeground(Color.BLACK);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        dialog.add(lblFullName, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        dialog.add(txtFullName, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        dialog.add(lblEmail, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        dialog.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        dialog.add(lblPhoneNumber, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        dialog.add(txtPhoneNumber, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
        dialog.add(lblPosition, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        dialog.add(txtPosition, gbc);

        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        dialog.add(btnSave, gbc);

        btnSave.addActionListener(e -> {
            try {
                Teacher teacher = new Teacher(
                        0,
                        txtFullName.getText(),
                        txtEmail.getText(),
                        txtPhoneNumber.getText(),
                        txtPosition.getText()
                );
                synchronized (teacherDAO) {
                    teacherDAO.addTeacher(teacher);
                }
                loadTeachersAsync();
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Thêm giảng viên thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    private void showEditTeacherDialog() {
        int selectedRow = teacherTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một giảng viên để sửa.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int teacherId = (int) teacherTable.getValueAt(selectedRow, 0);
        try {
            Teacher teacher = teacherDAO.findById(teacherId);
            if (teacher == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy giảng viên.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JDialog dialog = new JDialog();
            dialog.setTitle("Sửa giảng viên");
            dialog.setSize(600, 400); // Increased size to match ProjectsPanel
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new GridBagLayout());
            dialog.setBackground(Color.WHITE);
            ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Added padding

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10); // Consistent insets
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JTextField txtFullName = new JTextField(teacher.getFullName(), 20);
            txtFullName.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtFullName.setForeground(Color.BLACK);
            JTextField txtEmail = new JTextField(teacher.getEmail(), 20);
            txtEmail.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtEmail.setForeground(Color.BLACK);
            JTextField txtPhoneNumber = new JTextField(teacher.getPhoneNumber(), 20);
            txtPhoneNumber.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtPhoneNumber.setForeground(Color.BLACK);
            JTextField txtPosition = new JTextField(teacher.getPosition(), 20);
            txtPosition.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtPosition.setForeground(Color.BLACK);
            JButton btnSave = new JButton("Lưu");
            btnSave.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnSave.setBackground(new Color(0, 123, 255));
            btnSave.setForeground(Color.BLACK);

            JLabel lblFullName = new JLabel("Tên:");
            lblFullName.setForeground(Color.BLACK);
            JLabel lblEmail = new JLabel("Email:");
            lblEmail.setForeground(Color.BLACK);
            JLabel lblPhoneNumber = new JLabel("Số điện thoại:");
            lblPhoneNumber.setForeground(Color.BLACK);
            JLabel lblPosition = new JLabel("Chức vụ:");
            lblPosition.setForeground(Color.BLACK);

            gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
            dialog.add(lblFullName, gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            dialog.add(txtFullName, gbc);

            gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
            dialog.add(lblEmail, gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            dialog.add(txtEmail, gbc);

            gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
            dialog.add(lblPhoneNumber, gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            dialog.add(txtPhoneNumber, gbc);

            gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
            dialog.add(lblPosition, gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            dialog.add(txtPosition, gbc);

            gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 0.0;
            gbc.anchor = GridBagConstraints.CENTER;
            dialog.add(btnSave, gbc);

            btnSave.addActionListener(e -> {
                try {
                    teacher.setFullName(txtFullName.getText());
                    teacher.setEmail(txtEmail.getText());
                    teacher.setPhoneNumber(txtPhoneNumber.getText());
                    teacher.setPosition(txtPosition.getText());
                    synchronized (teacherDAO) {
                        teacherDAO.updateTeacher(teacher);
                    }
                    loadTeachersAsync();
                    dialog.dispose();
                    JOptionPane.showMessageDialog(this, "Sửa giảng viên thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            });

            dialog.setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedTeacher() {
        int selectedRow = teacherTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một giảng viên để xóa.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int teacherId = (int) teacherTable.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa giảng viên này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                synchronized (teacherDAO) {
                    teacherDAO.deleteTeacher(teacherId);
                }
                loadTeachersAsync();
                JOptionPane.showMessageDialog(this, "Xóa giảng viên thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}