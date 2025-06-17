package org.projectmanagement.UI;

import org.projectmanagement.dao.UserDAO;
import org.projectmanagement.models.User;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException; // Thêm import này
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
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setBorder(new EmptyBorder(10, 0, 10, 0));
        add(title, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel lblOldPassword = new JLabel("Mật khẩu cũ:");
        txtOldPassword = new JPasswordField(20);

        JLabel lblNewPassword = new JLabel("Mật khẩu mới:");
        txtNewPassword = new JPasswordField(20);

        JLabel lblConfirmPassword = new JLabel("Xác nhận mật khẩu:");
        txtConfirmPassword = new JPasswordField(20);

        btnChange = new JButton("Đổi mật khẩu");
        lblMessage = new JLabel(" ");
        lblMessage.setForeground(Color.RED);

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
        gbc.gridx = 1;
        gbc.gridy = 3;
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