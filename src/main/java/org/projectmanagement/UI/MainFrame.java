package org.projectmanagement.UI;

import org.projectmanagement.models.User;
import org.projectmanagement.util.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

public class MainFrame extends JFrame {
    private RegisterPanel registerPanel;
    private Connection connection;
    private LoginPanel loginPanel;
    private MainPanel mainPanel;

    public MainFrame(Connection connection) {
        this.connection = connection;
        initUI();
    }

    private void initUI() {
        setTitle("Project Management System");
        setSize(900, 600); // tăng kích thước để UI rộng hơn
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        loginPanel = new LoginPanel(connection);
        registerPanel = new RegisterPanel(connection);

        add(loginPanel, BorderLayout.CENTER);

        // Login thành công
        loginPanel.addPropertyChangeListener("loginSuccess", evt -> {
            User user = (User) evt.getNewValue();
            onLoginSuccess(user);
        });

        // Chuyển sang giao diện đăng ký
        loginPanel.addPropertyChangeListener("goToRegister", evt -> {
            switchPanel(registerPanel);
        });

        // Quay lại đăng nhập từ đăng ký
        registerPanel.addPropertyChangeListener("backToLogin", evt -> {
            switchPanel(loginPanel);
        });
    }

    private void switchPanel(JPanel panel) {
        getContentPane().removeAll();
        add(panel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void onLoginSuccess(User user) {
        remove(loginPanel);

        mainPanel = new MainPanel(user, connection);

        // Đăng ký listener logout từ mainPanel
        mainPanel.setLogoutListener(() -> {
            // Quay lại màn hình login khi logout
            switchPanel(loginPanel);
        });

        add(mainPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    public interface LogoutListener {
        void onLogout();
    }

    public static void main(String[] args) {
        try {
            Connection conn = DBConnection.getConnection();
            SwingUtilities.invokeLater(() -> {
                MainFrame frame = new MainFrame(conn);
                frame.setVisible(true);
            });
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Không thể kết nối cơ sở dữ liệu!",
                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
