package org.projectmanagement.UI;

import org.projectmanagement.dao.UserDAO;
import org.projectmanagement.models.User;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.border.EmptyBorder;

public class ChangePasswordPanel extends JPanel {
    private User loggedUser;
    private Connection connection;
    private UserDAO userDAO;
    private JPasswordField txtOldPassword;
    private JPasswordField txtNewPassword;
    private JPasswordField txtConfirmPassword;
    private JButton btnChange;
    private JLabel lblMessage;

    // Colors from MainPanel.java for consistency
    private static final Color TEXT_PRIMARY = new Color(33, 41, 60);
    private static final Color TEXT_SECONDARY = new Color(107, 124, 147);
    private static final Color ACCENT_COLOR = new Color(79, 172, 254);

    public ChangePasswordPanel(User user, Connection connection) {
        this.loggedUser = user;
        this.connection = connection;
        this.userDAO = new UserDAO(connection);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JLabel title = new JLabel("Đổi Mật Khẩu", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(20, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);

        JLabel lblOldPassword = new JLabel("Mật khẩu cũ:");
        lblOldPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblOldPassword.setForeground(TEXT_PRIMARY);
        txtOldPassword = new JPasswordField(20);
        txtOldPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel lblNewPassword = new JLabel("Mật khẩu mới:");
        lblNewPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblNewPassword.setForeground(TEXT_PRIMARY);
        txtNewPassword = new JPasswordField(20);
        txtNewPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel lblConfirmPassword = new JLabel("Xác nhận mật khẩu:");
        lblConfirmPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblConfirmPassword.setForeground(TEXT_PRIMARY);
        txtConfirmPassword = new JPasswordField(20);
        txtConfirmPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        btnChange = new JButton("Đổi mật khẩu") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color color = ACCENT_COLOR;
                if (getModel().isPressed()) {
                    color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 180);
                } else if (getModel().isRollover()) {
                    color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 220);
                }

                g2.setColor(color);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnChange.setForeground(Color.WHITE);
        btnChange.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnChange.setFocusPainted(false);
        btnChange.setBorderPainted(false);
        btnChange.setContentAreaFilled(false);
        btnChange.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnChange.setPreferredSize(new Dimension(160, 40));

        lblMessage = new JLabel(" ");
        lblMessage.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMessage.setForeground(Color.RED);
        lblMessage.setHorizontalAlignment(SwingConstants.CENTER);

        // Mật khẩu cũ
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(lblOldPassword, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(txtOldPassword, gbc);

        // Mật khẩu mới
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(lblNewPassword, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(txtNewPassword, gbc);

        // Xác nhận mật khẩu
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(lblConfirmPassword, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(txtConfirmPassword, gbc);

        // Nút đổi
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(btnChange, gbc);

        // Thông báo
        gbc.gridy = 4;
        formPanel.add(lblMessage, gbc);

        add(formPanel, BorderLayout.CENTER);

        btnChange.addActionListener(e -> handleChangePassword());
    }

    private void handleChangePassword() {
        String oldPassword = new String(txtOldPassword.getPassword()).trim();
        String newPassword = new String(txtNewPassword.getPassword()).trim();
        String confirmPassword = new String(txtConfirmPassword.getPassword()).trim();

        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            lblMessage.setText("Vui lòng điền đầy đủ thông tin.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            lblMessage.setText("Mật khẩu xác nhận không khớp.");
            return;
        }

        if (newPassword.length() < 6) {
            lblMessage.setText("Mật khẩu mới phải có ít nhất 6 ký tự.");
            return;
        }

        try {
            boolean success = userDAO.changePassword(loggedUser.getUserId(), oldPassword, newPassword);
            if (success) {
                lblMessage.setForeground(new Color(0, 128, 0));
                lblMessage.setText("Đổi mật khẩu thành công!");
                txtOldPassword.setText("");
                txtNewPassword.setText("");
                txtConfirmPassword.setText("");
            } else {
                lblMessage.setForeground(Color.RED);
                lblMessage.setText("Mật khẩu cũ không đúng.");
            }
        } catch (SQLException ex) {
            lblMessage.setForeground(Color.RED);
            lblMessage.setText("Lỗi: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}