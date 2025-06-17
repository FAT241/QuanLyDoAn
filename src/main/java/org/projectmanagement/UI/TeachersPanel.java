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

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Tiêu đề
        JLabel title = new JLabel("Quản lý giảng viên", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setBorder(new EmptyBorder(10, 0, 30, 0));
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
        btnSearch.addActionListener(e -> searchTeachers());
        btnReset.addActionListener(e -> loadTeachersAsync());
        searchPanel.add(new JLabel("Tìm kiếm:"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnReset);
        add(searchPanel, BorderLayout.NORTH);

        // Bảng giảng viên
        teacherTable = new JTable();
        teacherTable.setRowHeight(30);
        teacherTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        teacherTable.setGridColor(new Color(200, 200, 200));
        teacherTable.setShowGrid(true);
        teacherTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
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
        btnAdd.setForeground(Color.WHITE);
        btnEdit.setForeground(Color.WHITE);
        btnDelete.setForeground(Color.WHITE);
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
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField txtFullName = new JTextField(20);
        JTextField txtEmail = new JTextField(20);
        JTextField txtPhoneNumber = new JTextField(20);
        JTextField txtPosition = new JTextField(20);
        JButton btnSave = new JButton("Lưu");

        gbc.gridx = 0; gbc.gridy = 0;
        dialog.add(new JLabel("Tên:"), gbc);
        gbc.gridx = 1;
        dialog.add(txtFullName, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        dialog.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        dialog.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        dialog.add(new JLabel("Số điện thoại:"), gbc);
        gbc.gridx = 1;
        dialog.add(txtPhoneNumber, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        dialog.add(new JLabel("Chức vụ:"), gbc);
        gbc.gridx = 1;
        dialog.add(txtPosition, gbc);

        gbc.gridx = 1; gbc.gridy = 4;
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
            dialog.setSize(400, 350);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);

            JTextField txtFullName = new JTextField(teacher.getFullName(), 20);
            JTextField txtEmail = new JTextField(teacher.getEmail(), 20);
            JTextField txtPhoneNumber = new JTextField(teacher.getPhoneNumber(), 20);
            JTextField txtPosition = new JTextField(teacher.getPosition(), 20);
            JButton btnSave = new JButton("Lưu");

            gbc.gridx = 0; gbc.gridy = 0;
            dialog.add(new JLabel("Tên:"), gbc);
            gbc.gridx = 1;
            dialog.add(txtFullName, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            dialog.add(new JLabel("Email:"), gbc);
            gbc.gridx = 1;
            dialog.add(txtEmail, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            dialog.add(new JLabel("Số điện thoại:"), gbc);
            gbc.gridx = 1;
            dialog.add(txtPhoneNumber, gbc);

            gbc.gridx = 0; gbc.gridy = 3;
            dialog.add(new JLabel("Chức vụ:"), gbc);
            gbc.gridx = 1;
            dialog.add(txtPosition, gbc);

            gbc.gridx = 1; gbc.gridy = 4;
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