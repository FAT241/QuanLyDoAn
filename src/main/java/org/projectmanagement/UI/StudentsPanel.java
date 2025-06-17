package org.projectmanagement.UI;

import org.mindrot.jbcrypt.BCrypt;
import org.projectmanagement.dao.StudentDAO;
import org.projectmanagement.dao.UserDAO;
import org.projectmanagement.models.Student;
import org.projectmanagement.models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class StudentsPanel extends JPanel {
    private User loggedUser;
    private Connection connection;
    private StudentDAO studentDAO;
    private JTable studentTable;
    private JButton btnAdd, btnEdit, btnDelete, btnSearch, btnReset;
    private JTextField txtSearch;

    public StudentsPanel(User user, Connection connection) {
        this.loggedUser = user;
        this.connection = connection;
        this.studentDAO = new StudentDAO(connection);
        initComponents();
        loadStudentsAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Tiêu đề
        JLabel title = new JLabel("Quản lý sinh viên", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setBorder(new EmptyBorder(10, 0, 10, 0));
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
        btnSearch.addActionListener(e -> searchStudents());
        btnReset.addActionListener(e -> loadStudentsAsync());
        searchPanel.add(new JLabel("Tìm kiếm:"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnReset);
        add(searchPanel, BorderLayout.NORTH);

        // Bảng sinh viên
        studentTable = new JTable();
        studentTable.setRowHeight(30);
        studentTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        studentTable.setGridColor(new Color(200, 200, 200));
        studentTable.setShowGrid(true);
        studentTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        studentTable.getTableHeader().setBackground(new Color(230, 230, 230));
        studentTable.getTableHeader().setReorderingAllowed(false);
        studentTable.setDefaultRenderer(Object.class, new CustomTableCellRenderer());
        JScrollPane scrollPane = new JScrollPane(studentTable);
        add(scrollPane, BorderLayout.CENTER);

        // Nút hành động
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        btnAdd = new JButton("Thêm sinh viên");
        btnEdit = new JButton("Sửa sinh viên");
        btnDelete = new JButton("Xóa sinh viên");
        btnAdd.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnEdit.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnDelete.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnAdd.setBackground(new Color(0, 123, 255));
        btnEdit.setBackground(new Color(0, 123, 255));
        btnDelete.setBackground(new Color(255, 0, 0));
        btnAdd.setForeground(Color.WHITE);
        btnEdit.setForeground(Color.WHITE);
        btnDelete.setForeground(Color.WHITE);
        btnAdd.addActionListener(e -> showAddStudentDialog());
        btnEdit.addActionListener(e -> showEditStudentDialog());
        btnDelete.addActionListener(e -> deleteSelectedStudent());
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadStudentsAsync() {
        SwingWorker<List<Student>, Void> worker = new SwingWorker<List<Student>, Void>() {
            @Override
            protected List<Student> doInBackground() throws Exception {
                synchronized (studentDAO) {
                    return studentDAO.findAll();
                }
            }

            @Override
            protected void done() {
                try {
                    List<Student> students = get();
                    updateTable(students);
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(StudentsPanel.this, "Lỗi tải dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void searchStudents() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập từ khóa tìm kiếm.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SwingWorker<List<Student>, Void> worker = new SwingWorker<List<Student>, Void>() {
            @Override
            protected List<Student> doInBackground() throws Exception {
                synchronized (studentDAO) {
                    return studentDAO.searchByNameOrEmail(keyword);
                }
            }

            @Override
            protected void done() {
                try {
                    List<Student> students = get();
                    updateTable(students);
                    if (students.isEmpty()) {
                        JOptionPane.showMessageDialog(StudentsPanel.this, "Không tìm thấy sinh viên nào.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(StudentsPanel.this, "Lỗi tìm kiếm: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateTable(List<Student> students) {
        String[] columns = {"ID", "Tên", "Email", "Số điện thoại", "Chuyên ngành", "Mã lớp"};
        Object[][] data = new Object[students.size()][6];
        for (int i = 0; i < students.size(); i++) {
            Student s = students.get(i);
            data[i] = new Object[]{
                    s.getStudentId(),
                    s.getFullName(),
                    s.getEmail(),
                    s.getPhoneNumber(),
                    s.getMajor(),
                    s.getClassCode()
            };
        }
        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        studentTable.setModel(model);
    }

    private void showAddStudentDialog() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Thêm sinh viên");
        dialog.setSize(400, 450); // Tăng kích thước để chứa thêm trường
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField txtFullName = new JTextField(20);
        JTextField txtEmail = new JTextField(20);
        JTextField txtPhoneNumber = new JTextField(20);
        JTextField txtMajor = new JTextField(20);
        JTextField txtClassCode = new JTextField(20);
        JTextField txtStudentCode = new JTextField(20); // Trường mã sinh viên
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
        dialog.add(new JLabel("Chuyên ngành:"), gbc);
        gbc.gridx = 1;
        dialog.add(txtMajor, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        dialog.add(new JLabel("Mã lớp:"), gbc);
        gbc.gridx = 1;
        dialog.add(txtClassCode, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        dialog.add(new JLabel("Mã sinh viên:"), gbc);
        gbc.gridx = 1;
        dialog.add(txtStudentCode, gbc);

        gbc.gridx = 1; gbc.gridy = 6;
        dialog.add(btnSave, gbc);

        btnSave.addActionListener(e -> {
            try {
                // Validate input
                String fullName = txtFullName.getText().trim();
                String email = txtEmail.getText().trim();
                String phoneNumber = txtPhoneNumber.getText().trim();
                String major = txtMajor.getText().trim();
                String classCode = txtClassCode.getText().trim();
                String studentCode = txtStudentCode.getText().trim();

                if (fullName.isEmpty() || email.isEmpty() || phoneNumber.isEmpty() || major.isEmpty() || classCode.isEmpty() || studentCode.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Validate email format
                if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,6}$")) {
                    JOptionPane.showMessageDialog(this, "Định dạng email không hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Validate phone number
                if (!phoneNumber.matches("\\d{9,12}")) {
                    JOptionPane.showMessageDialog(this, "Số điện thoại không hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Tạo tài khoản người dùng
                User user = new User();
                user.setUsername(email); // Dùng email làm username
                user.setPassword(BCrypt.hashpw("pass123", BCrypt.gensalt())); // Mật khẩu mặc định
                user.setEmail(email);
                user.setFullName(fullName);
                user.setRole("user");
                user.setPhoneNumber(phoneNumber);
                user.setStudentCode(studentCode);
                user.setAvatarPath("images/default.png");

                UserDAO userDAO = new UserDAO(connection);

                synchronized (userDAO) {
                    // Kiểm tra email trùng lặp
                    if (userDAO.findByEmail(email) != null) {
                        JOptionPane.showMessageDialog(this, "Email đã được sử dụng.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // Thêm tài khoản người dùng
                    userDAO.registerUser(user);

                    // Lấy user_id vừa tạo
                    User createdUser = userDAO.findByEmail(email);
                    if (createdUser == null) {
                        throw new SQLException("Không thể lấy thông tin tài khoản vừa tạo.");
                    }

                    // Tạo sinh viên
                    Student student = new Student(
                            0,
                            fullName,
                            email,
                            phoneNumber,
                            major,
                            classCode,
                            createdUser.getUserId() // bổ sung userId
                    );

                    synchronized (studentDAO) {
                        // Thêm sinh viên với user_id
                        studentDAO.addStudent(student, createdUser.getUserId());
                    }
                }

                loadStudentsAsync();
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Thêm sinh viên và tài khoản thành công! Mật khẩu mặc định: pass123", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        dialog.setVisible(true);
    }

    private void showEditStudentDialog() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một sinh viên để sửa.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int studentId = (int) studentTable.getValueAt(selectedRow, 0);
        try {
            Student student = studentDAO.findById(studentId);
            if (student == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy sinh viên.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JDialog dialog = new JDialog();
            dialog.setTitle("Sửa sinh viên");
            dialog.setSize(400, 450);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);

            JTextField txtFullName = new JTextField(student.getFullName(), 20);
            JTextField txtEmail = new JTextField(student.getEmail(), 20);
            JTextField txtPhoneNumber = new JTextField(student.getPhoneNumber(), 20);
            JTextField txtMajor = new JTextField(student.getMajor(), 20);
            JTextField txtClassCode = new JTextField(student.getClassCode(), 20);
            JTextField txtStudentCode = new JTextField(20); // Trường mã sinh viên
            JButton btnSave = new JButton("Lưu");

            // Tìm user_id tương ứng
            UserDAO userDAO = new UserDAO(connection);
            User user = userDAO.findByEmail(student.getEmail());
            if (user != null) {
                txtStudentCode.setText(user.getStudentCode());
            }

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
            dialog.add(new JLabel("Chuyên ngành:"), gbc);
            gbc.gridx = 1;
            dialog.add(txtMajor, gbc);

            gbc.gridx = 0; gbc.gridy = 4;
            dialog.add(new JLabel("Mã lớp:"), gbc);
            gbc.gridx = 1;
            dialog.add(txtClassCode, gbc);

            gbc.gridx = 0; gbc.gridy = 5;
            dialog.add(new JLabel("Mã sinh viên:"), gbc);
            gbc.gridx = 1;
            dialog.add(txtStudentCode, gbc);

            gbc.gridx = 1; gbc.gridy = 6;
            dialog.add(btnSave, gbc);

            btnSave.addActionListener(e -> {
                try {
                    // Validate input
                    String fullName = txtFullName.getText().trim();
                    String email = txtEmail.getText().trim();
                    String phoneNumber = txtPhoneNumber.getText().trim();
                    String major = txtMajor.getText().trim();
                    String classCode = txtClassCode.getText().trim();
                    String studentCode = txtStudentCode.getText().trim();

                    if (fullName.isEmpty() || email.isEmpty() || phoneNumber.isEmpty() || major.isEmpty() || classCode.isEmpty() || studentCode.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // Validate email format
                    if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,6}$")) {
                        JOptionPane.showMessageDialog(this, "Định dạng email không hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // Validate phone number
                    if (!phoneNumber.matches("\\d{9,12}")) {
                        JOptionPane.showMessageDialog(this, "Số điện thoại không hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // Kiểm tra email trùng lặp với user khác
                    User existingUser = userDAO.findByEmail(email);
                    if (existingUser != null && existingUser.getUserId() != user.getUserId()) {
                        JOptionPane.showMessageDialog(this, "Email đã được sử dụng bởi tài khoản khác.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // Cập nhật sinh viên
                    student.setFullName(fullName);
                    student.setEmail(email);
                    student.setPhoneNumber(phoneNumber);
                    student.setMajor(major);
                    student.setClassCode(classCode);

                    // Cập nhật tài khoản người dùng
                    if (user != null) {
                        user.setEmail(email);
                        user.setFullName(fullName);
                        user.setPhoneNumber(phoneNumber);
                        user.setStudentCode(studentCode);

                        synchronized (userDAO) {
                            userDAO.updateUser(user);
                        }
                    }

                    synchronized (studentDAO) {
                        studentDAO.updateStudent(student);
                    }

                    loadStudentsAsync();
                    dialog.dispose();
                    JOptionPane.showMessageDialog(this, "Sửa sinh viên thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            });

            dialog.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedStudent() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một sinh viên để xóa.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int studentId = (int) studentTable.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa sinh viên này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                synchronized (studentDAO) {
                    studentDAO.deleteStudent(studentId);
                }
                loadStudentsAsync();
                JOptionPane.showMessageDialog(this, "Xóa sinh viên thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}