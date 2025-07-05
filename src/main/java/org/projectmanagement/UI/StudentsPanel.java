package org.projectmanagement.UI;

import org.mindrot.jbcrypt.BCrypt;
import org.projectmanagement.dao.StudentDAO;
import org.projectmanagement.dao.UserDAO;
import org.projectmanagement.models.Student;
import org.projectmanagement.models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
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
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
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

        btnSearch.addActionListener(e -> searchStudents());
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            loadStudentsAsync(); // Reload all data instead of just clearing filter
        });

        searchPanel.add(searchLabel);
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnReset);

        // Container for title and search panel (giống TeachersPanel)
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(Color.WHITE);
        northPanel.add(title, BorderLayout.NORTH);
        northPanel.add(searchPanel, BorderLayout.CENTER);
        add(northPanel, BorderLayout.NORTH);

        // Bảng sinh viên
        String[] columns = {"ID", "Tên", "Email", "Số điện thoại", "Chuyên ngành", "Mã lớp"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        studentTable = new JTable(tableModel);
        studentTable.setRowHeight(30);
        studentTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        studentTable.setForeground(Color.BLACK);
        studentTable.setGridColor(new Color(200, 200, 200));
        studentTable.setShowGrid(true);
        studentTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        studentTable.getTableHeader().setForeground(Color.BLACK);
        studentTable.getTableHeader().setBackground(new Color(230, 230, 230));
        studentTable.getTableHeader().setReorderingAllowed(false);
        studentTable.setDefaultRenderer(Object.class, new CustomTableCellRenderer());

        // Initialize sorter for real-time filtering
        sorter = new TableRowSorter<>(tableModel);
        studentTable.setRowSorter(sorter);

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

        btnAdd.setForeground(Color.BLACK);
        btnEdit.setForeground(Color.BLACK);
        btnDelete.setForeground(Color.BLACK);

        btnAdd.addActionListener(e -> showAddStudentDialog());
        btnEdit.addActionListener(e -> showEditStudentDialog());
        btnDelete.addActionListener(e -> deleteSelectedStudent());

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add real-time search listener
        addSearchListener();
    }

    private void addSearchListener() {
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTable();
            }
        });
    }

    private void filterTable() {
        String text = txtSearch.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 1, 2)); // Search in "Tên" and "Email" columns
        }
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

    // Cải thiện phương thức searchStudents giống TeachersPanel
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

    // Cải thiện phương thức updateTable giống TeachersPanel
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
        tableModel = model;
        studentTable.setModel(model);

        // Reinitialize sorter after updating table model
        sorter = new TableRowSorter<>(tableModel);
        studentTable.setRowSorter(sorter);
    }

    private void showAddStudentDialog() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Thêm sinh viên");
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());
        dialog.setBackground(Color.WHITE);
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
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
        JTextField txtMajor = new JTextField(20);
        txtMajor.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtMajor.setForeground(Color.BLACK);
        JTextField txtClassCode = new JTextField(20);
        txtClassCode.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtClassCode.setForeground(Color.BLACK);
        JTextField txtStudentCode = new JTextField(20);
        txtStudentCode.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtStudentCode.setForeground(Color.BLACK);
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
        JLabel lblMajor = new JLabel("Chuyên ngành:");
        lblMajor.setForeground(Color.BLACK);
        JLabel lblClassCode = new JLabel("Mã lớp:");
        lblClassCode.setForeground(Color.BLACK);
        JLabel lblStudentCode = new JLabel("Mã sinh viên:");
        lblStudentCode.setForeground(Color.BLACK);

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
        dialog.add(lblMajor, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        dialog.add(txtMajor, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0;
        dialog.add(lblClassCode, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        dialog.add(txtClassCode, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.0;
        dialog.add(lblStudentCode, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        dialog.add(txtStudentCode, gbc);

        gbc.gridx = 1; gbc.gridy = 6; gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        dialog.add(btnSave, gbc);

        btnSave.addActionListener(e -> {
            try {
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

                if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,6}$")) {
                    JOptionPane.showMessageDialog(this, "Định dạng email không hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                if (!phoneNumber.matches("\\d{10,12}$")) {
                    JOptionPane.showMessageDialog(this, "Số điện thoại không hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                // Tạo tai khoan dang nhap voi mat khau mac dinh là pass123
                User user = new User();
                user.setUsername(email);
                user.setPassword(BCrypt.hashpw("pass123", BCrypt.gensalt())); //// Hàm băm BCrypt
                user.setEmail(email);
                user.setFullName(fullName);
                user.setRole("user");
                user.setPhoneNumber(phoneNumber);
                user.setStudentCode(studentCode);
                user.setAvatarPath("images/default.png");

                UserDAO userDAO = new UserDAO(connection);

                synchronized (userDAO) {
                    if (userDAO.findByEmail(email) != null) {
                        JOptionPane.showMessageDialog(this, "Email đã được sử dụng.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    userDAO.registerUser(user);

                    User createdUser = userDAO.findByEmail(email);
                    if (createdUser == null) {
                        throw new SQLException("Không thể lấy thông tin tài khoản vừa tạo.");
                    }

                    Student student = new Student(
                            0,
                            fullName,
                            email,
                            phoneNumber,
                            major,
                            classCode,
                            createdUser.getUserId()
                    );

                    synchronized (studentDAO) {
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
            dialog.setSize(600, 500);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new GridBagLayout());
            dialog.setBackground(Color.WHITE);
            ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JTextField txtFullName = new JTextField(student.getFullName(), 20);
            txtFullName.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtFullName.setForeground(Color.BLACK);
            JTextField txtEmail = new JTextField(student.getEmail(), 20);
            txtEmail.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtEmail.setForeground(Color.BLACK);
            JTextField txtPhoneNumber = new JTextField(student.getPhoneNumber(), 20);
            txtPhoneNumber.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtPhoneNumber.setForeground(Color.BLACK);
            JTextField txtMajor = new JTextField(student.getMajor(), 20);
            txtMajor.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtMajor.setForeground(Color.BLACK);
            JTextField txtClassCode = new JTextField(student.getClassCode(), 20);
            txtClassCode.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtClassCode.setForeground(Color.BLACK);
            JTextField txtStudentCode = new JTextField(20);
            txtStudentCode.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtStudentCode.setForeground(Color.BLACK);
            JButton btnSave = new JButton("Lưu");
            btnSave.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnSave.setBackground(new Color(0, 123, 255));
            btnSave.setForeground(Color.BLACK);

            UserDAO userDAO = new UserDAO(connection);
            User user = userDAO.findByEmail(student.getEmail());
            if (user != null) {
                txtStudentCode.setText(user.getStudentCode());
            }

            JLabel lblFullName = new JLabel("Tên:");
            lblFullName.setForeground(Color.BLACK);
            JLabel lblEmail = new JLabel("Email:");
            lblEmail.setForeground(Color.BLACK);
            JLabel lblPhoneNumber = new JLabel("Số điện thoại:");
            lblPhoneNumber.setForeground(Color.BLACK);
            JLabel lblMajor = new JLabel("Chuyên ngành:");
            lblMajor.setForeground(Color.BLACK);
            JLabel lblClassCode = new JLabel("Mã lớp:");
            lblClassCode.setForeground(Color.BLACK);
            JLabel lblStudentCode = new JLabel("Mã sinh viên:");
            lblStudentCode.setForeground(Color.BLACK);

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
            dialog.add(lblMajor, gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            dialog.add(txtMajor, gbc);

            gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0;
            dialog.add(lblClassCode, gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            dialog.add(txtClassCode, gbc);

            gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.0;
            dialog.add(lblStudentCode, gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            dialog.add(txtStudentCode, gbc);

            gbc.gridx = 1; gbc.gridy = 6; gbc.weightx = 0.0;
            gbc.anchor = GridBagConstraints.CENTER;
            dialog.add(btnSave, gbc);

            btnSave.addActionListener(e -> {
                try {
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

                    if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,6}$")) {
                        JOptionPane.showMessageDialog(this, "Định dạng email không hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    if (!phoneNumber.matches("\\d{10,12}$")) {
                        JOptionPane.showMessageDialog(this, "Số điện thoại không hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    User existingUser = userDAO.findByEmail(email);
                    if (existingUser != null && existingUser.getUserId() != user.getUserId()) {
                        JOptionPane.showMessageDialog(this, "Email đã được sử dụng bởi tài khoản khác.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    student.setFullName(fullName);
                    student.setEmail(email);
                    student.setPhoneNumber(phoneNumber);
                    student.setMajor(major);
                    student.setClassCode(classCode);

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
        } catch (SQLException ex) {
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