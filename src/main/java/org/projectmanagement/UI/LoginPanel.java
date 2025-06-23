package org.projectmanagement.UI;

import org.projectmanagement.dao.UserDAO;
import org.projectmanagement.models.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
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
        // Set layout with padding
        this.setLayout(new BorderLayout(20, 20));
        this.setBackground(new Color(245, 247, 250));
        this.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Logo panel with proper scaling
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        logoPanel.setBackground(new Color(245, 247, 250));
        logoPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        try {
            // Load and scale logo properly
            Image originalLogo = ImageIO.read(getClass().getResource("/logo_vku.png"));
            // Scale to maintain aspect ratio, max width 250px, max height 150px
            int logoWidth = 250;
            int logoHeight = 150;
            Image scaledLogo = originalLogo.getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH);

            JLabel lblLogo = new JLabel(new ImageIcon(scaledLogo));
            lblLogo.setHorizontalAlignment(JLabel.CENTER);
            logoPanel.add(lblLogo);
        } catch (Exception e) {
            // Add placeholder text if logo not found
            JLabel lblTitle = new JLabel("PROJECT MANAGEMENT SYSTEM");
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
            lblTitle.setForeground(new Color(52, 58, 64));
            lblTitle.setHorizontalAlignment(JLabel.CENTER);
            logoPanel.add(lblTitle);
        }

        this.add(logoPanel, BorderLayout.NORTH);

        // Center panel with improved card style
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                new ShadowBorder(),
                BorderFactory.createEmptyBorder(30, 50, 30, 50)
        ));
        formPanel.setPreferredSize(new Dimension(650, 320));
        formPanel.setMaximumSize(new Dimension(650, 320));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel lblTitle = new JLabel("Đăng nhập hệ thống");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(52, 58, 64));
        lblTitle.setHorizontalAlignment(JLabel.CENTER);

        // Labels and fields with improved styling
        JLabel lblEmail = new JLabel("Email:");
        lblEmail.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblEmail.setForeground(new Color(73, 80, 87));

        txtEmail = new JTextField();
        txtEmail.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtEmail.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 2),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        txtEmail.setPreferredSize(new Dimension(400, 40));
        txtEmail.setBackground(new Color(248, 249, 250));

        // Add focus effect to email field
        txtEmail.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                txtEmail.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0, 123, 255), 2),
                        BorderFactory.createEmptyBorder(12, 15, 12, 15)
                ));
                txtEmail.setBackground(Color.WHITE);
            }

            @Override
            public void focusLost(FocusEvent e) {
                txtEmail.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(206, 212, 218), 2),
                        BorderFactory.createEmptyBorder(12, 15, 12, 15)
                ));
                txtEmail.setBackground(new Color(248, 249, 250));
            }
        });

        JLabel lblPassword = new JLabel("Mật khẩu:");
        lblPassword.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblPassword.setForeground(new Color(73, 80, 87));

        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 2),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        txtPassword.setPreferredSize(new Dimension(400, 40));
        txtPassword.setBackground(new Color(248, 249, 250));

        // Add focus effect to password field
        txtPassword.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                txtPassword.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0, 123, 255), 2),
                        BorderFactory.createEmptyBorder(12, 15, 12, 15)
                ));
                txtPassword.setBackground(Color.WHITE);
            }

            @Override
            public void focusLost(FocusEvent e) {
                txtPassword.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(206, 212, 218), 2),
                        BorderFactory.createEmptyBorder(12, 15, 12, 15)
                ));
                txtPassword.setBackground(new Color(248, 249, 250));
            }
        });

        // Improved buttons
        btnLogin = new JButton("Đăng nhập");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnLogin.setBackground(new Color(40, 167, 69));
        btnLogin.setForeground(Color.BLACK); // Changed to black
        btnLogin.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.setPreferredSize(new Dimension(180, 45));

        // Add hover effect to login button
        btnLogin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnLogin.setBackground(new Color(33, 136, 56));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnLogin.setBackground(new Color(40, 167, 69));
            }
        });

        btnRegister = new JButton("Tạo tài khoản mới");
        btnRegister.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRegister.setBackground(new Color(108, 117, 125));
        btnRegister.setForeground(Color.BLACK); // Changed to black
        btnRegister.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        btnRegister.setFocusPainted(false);
        btnRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRegister.setPreferredSize(new Dimension(180, 40));

        // Add hover effect to register button
        btnRegister.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnRegister.setBackground(new Color(90, 98, 104));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnRegister.setBackground(new Color(108, 117, 125));
            }
        });

        lblMessage = new JLabel(" ");
        lblMessage.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblMessage.setForeground(new Color(220, 53, 69));
        lblMessage.setHorizontalAlignment(JLabel.CENTER);

        // Layout components with better spacing
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 20, 0);
        formPanel.add(lblTitle, gbc);

        // Email section
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 5, 15);
        formPanel.add(lblEmail, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 5, 0);
        formPanel.add(txtEmail, gbc);

        // Password section
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 5, 15);
        formPanel.add(lblPassword, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 15, 0);
        formPanel.add(txtPassword, gbc);

        // Buttons section
        gbc.gridwidth = 1;
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(10, 0, 10, 10);
        formPanel.add(btnLogin, gbc);

        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 10, 0);
        formPanel.add(btnRegister, gbc);

        // Message
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(5, 0, 0, 0);
        formPanel.add(lblMessage, gbc);

        // Wrap formPanel in another panel for centering
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(new Color(245, 247, 250));
        centerWrapper.add(formPanel, new GridBagConstraints());

        this.add(centerWrapper, BorderLayout.CENTER);

        // Event listeners
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
            showMessage("Vui lòng nhập email.", true);
            txtEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showMessage("Vui lòng nhập mật khẩu.", true);
            txtPassword.requestFocus();
            return;
        }

        try {
            User user = userDAO.loginByEmail(email, password);
            if (user != null) {
                showMessage("Đăng nhập thành công! Chào mừng " + user.getFullName(), false);
                firePropertyChange("loginSuccess", null, user);
            } else {
                showMessage("Email hoặc mật khẩu không đúng.", true);
                txtPassword.setText("");
                txtEmail.requestFocus();
            }
        } catch (Exception ex) {
            showMessage("Lỗi: " + ex.getMessage(), true);
            ex.printStackTrace();
        }
    }

    private void showMessage(String message, boolean isError) {
        lblMessage.setText(message);
        if (isError) {
            lblMessage.setForeground(new Color(220, 53, 69));
        } else {
            lblMessage.setForeground(new Color(40, 167, 69));
        }
    }

    // Enhanced shadow border
    private static class ShadowBorder extends AbstractBorder {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int shadowSize = 8;
            int arc = 15;

            // Draw multiple shadow layers for smoother effect
            for (int i = 0; i < shadowSize; i++) {
                int alpha = Math.max(5, 25 - (i * 3));
                g2.setColor(new Color(0, 0, 0, alpha));
                g2.fillRoundRect(x + i, y + i, width - i, height - i, arc, arc);
            }

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(8, 8, 8, 8);
        }
    }
}