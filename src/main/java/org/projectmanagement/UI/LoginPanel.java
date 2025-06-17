package org.projectmanagement.UI;

import org.projectmanagement.dao.UserDAO;
import org.projectmanagement.models.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;

public class LoginPanel extends JPanel {
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnRegister;
    private JLabel lblMessage;
    private UserDAO userDAO;
    private Connection connection;

    public LoginPanel(Connection connection) {
        this.connection = connection;
        this.userDAO = new UserDAO(connection);
        initComponents();
    }

    private void initComponents() {
        this.setLayout(new BorderLayout());
        this.setBackground(Color.WHITE);

        try {
            Image logo = ImageIO.read(getClass().getResource("/logo_vku.png")).getScaledInstance(120, 120, Image.SCALE_SMOOTH);
            JLabel lblLogo = new JLabel(new ImageIcon(logo));
            lblLogo.setHorizontalAlignment(JLabel.CENTER);
            this.add(lblLogo, BorderLayout.NORTH);
        } catch (Exception e) {
            // Không có logo thì thôi
        }

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel lblEmail = new JLabel("Email:");
        txtEmail = new JTextField(20);

        JLabel lblPassword = new JLabel("Password:");
        txtPassword = new JPasswordField(20);

        btnLogin = new JButton("Login");
        btnRegister = new JButton("Register");
        lblMessage = new JLabel(" ");
        lblMessage.setForeground(Color.RED);

        // Email
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(lblEmail, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(txtEmail, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(lblPassword, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(txtPassword, gbc);

        // Login button
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(btnLogin, gbc);

        // Register button
        gbc.gridy = 3;
        formPanel.add(btnRegister, gbc);

        // Message
        gbc.gridy = 4;
        formPanel.add(lblMessage, gbc);

        this.add(formPanel, BorderLayout.CENTER);

        btnLogin.addActionListener(e -> handleLogin());

        txtPassword.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        });

        btnRegister.addActionListener(e -> firePropertyChange("goToRegister", null, null));
    }

    private void handleLogin() {
        String email = txtEmail.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (email.isEmpty()) {
            lblMessage.setText("Vui lòng nhập email.");
            return;
        }
        if (password.isEmpty()) {
            lblMessage.setText("Vui lòng nhập mật khẩu.");
            return;
        }

        try {
            User user = userDAO.loginByEmail(email, password);
            if (user != null) {
                lblMessage.setForeground(new Color(0, 128, 0));
                lblMessage.setText("Đăng nhập thành công! Chào mừng " + user.getFullName());
                firePropertyChange("loginSuccess", null, user);
            } else {
                lblMessage.setForeground(Color.RED);
                lblMessage.setText("Email hoặc mật khẩu không đúng.");
            }
        } catch (Exception ex) {
            lblMessage.setForeground(Color.RED);
            lblMessage.setText("Lỗi: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}