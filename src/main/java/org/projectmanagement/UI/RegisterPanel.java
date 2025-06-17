package org.projectmanagement.UI;

import org.projectmanagement.dao.UserDAO;
import org.projectmanagement.models.User;
import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;

public class RegisterPanel extends JPanel {
    private JTextField txtUsername, txtFullName, txtEmail, txtPhone, txtStudentCode;
    private JPasswordField txtPassword, txtConfirmPassword;
    private JButton btnRegister, btnBack;
    private JLabel lblMessage;
    private Connection connection;

    public RegisterPanel(Connection connection) {
        this.connection = connection;
        initComponents();
    }

    private void initComponents() {
        this.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        txtUsername = new JTextField(20);
        txtPassword = new JPasswordField(20);
        txtConfirmPassword = new JPasswordField(20);
        txtFullName = new JTextField(20);
        txtEmail = new JTextField(20);
        txtPhone = new JTextField(20);
        txtStudentCode = new JTextField(20);

        lblMessage = new JLabel(" ");
        lblMessage.setForeground(Color.RED);

        btnRegister = new JButton("Register");
        btnBack = new JButton("Back to Login");

        int y = 0;
        formPanel.add(new JLabel("Username:"), gbc(0, y));
        formPanel.add(txtUsername, gbc(1, y++));

        formPanel.add(new JLabel("Password:"), gbc(0, y));
        formPanel.add(txtPassword, gbc(1, y++));

        formPanel.add(new JLabel("Confirm Password:"), gbc(0, y));
        formPanel.add(txtConfirmPassword, gbc(1, y++));

        formPanel.add(new JLabel("Full Name:"), gbc(0, y));
        formPanel.add(txtFullName, gbc(1, y++));

        formPanel.add(new JLabel("Email:"), gbc(0, y));
        formPanel.add(txtEmail, gbc(1, y++));

        formPanel.add(new JLabel("Phone:"), gbc(0, y));
        formPanel.add(txtPhone, gbc(1, y++));

        formPanel.add(new JLabel("Student Code:"), gbc(0, y));
        formPanel.add(txtStudentCode, gbc(1, y++));

        formPanel.add(btnRegister, gbc(1, y++));
        formPanel.add(btnBack, gbc(1, y++));
        formPanel.add(lblMessage, gbc(1, y));

        this.add(formPanel, BorderLayout.CENTER);

        btnRegister.addActionListener(this::handleRegister);
        btnBack.addActionListener(e -> firePropertyChange("backToLogin", null, null));
    }

    private GridBagConstraints gbc(int x, int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        return gbc;
    }

    private void handleRegister(ActionEvent e) {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        String confirm = new String(txtConfirmPassword.getPassword());
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String studentCode = txtStudentCode.getText().trim();

        // Validate username
        if (username.isEmpty() || username.length() < 4 || username.length() > 20) {
            lblMessage.setText("Username must be 4-20 characters.");
            return;
        }

        // Validate password
        if (password.isEmpty() || password.length() < 6) {
            lblMessage.setText("Password must be at least 6 characters.");
            return;
        }

        // Check password confirm
        if (!password.equals(confirm)) {
            lblMessage.setText("Passwords do not match.");
            return;
        }

        // Validate email bằng regex đơn giản
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,6}$")) {
            lblMessage.setText("Invalid email format.");
            return;
        }

        // Validate phone (ví dụ chỉ chấp nhận số, từ 9 đến 12 số)
        if (!phone.matches("\\d{9,12}")) {
            lblMessage.setText("Invalid phone number.");
            return;
        }

        // Có thể thêm validate studentCode nếu cần, ví dụ không rỗng
        if (studentCode.isEmpty()) {
            lblMessage.setText("Student code cannot be empty.");
            return;
        }

        // Nếu qua hết validate, tiếp tục tạo User và đăng ký
        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole("user");
        user.setPhoneNumber(phone);
        user.setStudentCode(studentCode);
        user.setAvatarPath("images/default.png");

        UserDAO dao = new UserDAO(connection);
        try {
            if (dao.registerUser(user)) {
                lblMessage.setForeground(new Color(0, 128, 0));
                lblMessage.setText("Registration successful!");
            } else {
                lblMessage.setForeground(Color.RED);
                lblMessage.setText("Registration failed!");
            }
        } catch (Exception ex) {
            lblMessage.setForeground(Color.RED);
            lblMessage.setText("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

}
