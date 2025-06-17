package org.projectmanagement.UI;

import org.projectmanagement.models.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.Connection;

public class MainPanel extends JPanel {
    private User loggedUser;
    private JPanel contentPanel;
    private JLabel lblFullName;
    private JLabel lblRole;
    private JLabel lblAvatar;
    private JButton btnLogout;
    private Connection connection;
    private MainFrame.LogoutListener logoutListener;

    public MainPanel(User user, Connection connection) {
        this.loggedUser = user;
        this.connection = connection;
        initComponents();
    }

    private void initComponents() {
        this.setLayout(new BorderLayout());

        // Sidebar
        JPanel sidebar = createSidebar();
        this.add(sidebar, BorderLayout.WEST);

        // Header
        JPanel header = createHeader();
        this.add(header, BorderLayout.NORTH);

        // Content panel (nội dung chính)
        contentPanel = new JPanel();
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setLayout(new BorderLayout());
        // Mặc định hiển thị Projects khi mở
        onMenuSelected("Projects");
        this.add(contentPanel, BorderLayout.CENTER);
    }

    public void setLogoutListener(MainFrame.LogoutListener listener) {
        this.logoutListener = listener;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(new Color(41, 53, 65));
        sidebar.setLayout(new BorderLayout());

        JPanel logoPanel = new JPanel();
        logoPanel.setBackground(new Color(30, 39, 46));
        logoPanel.setPreferredSize(new Dimension(220, 100));
        logoPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 20));

        String logoPath = "/logo_vku.png";
        try {
            java.net.URL logoURL = getClass().getResource(logoPath);
            if (logoURL != null) {
                Image logo = ImageIO.read(logoURL);
                if (logo != null) {
                    logo = logo.getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                    JLabel lblLogo = new JLabel(new ImageIcon(logo));
                    logoPanel.add(lblLogo);
                } else {
                    System.err.println("Không thể tải ảnh: " + logoPath);
                }
            } else {
                System.err.println("Không tìm thấy tài nguyên: " + logoPath);
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi tải ảnh: " + e.getMessage());
            e.printStackTrace();
        }

        if (logoPanel.getComponentCount() == 0) {
            JLabel placeholder = new JLabel("Không có logo");
            placeholder.setForeground(Color.WHITE);
            logoPanel.add(placeholder);
        }

        sidebar.add(logoPanel, BorderLayout.NORTH);

        JPanel menuPanel = new JPanel();
        menuPanel.setBackground(new Color(41, 53, 65));
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // Thêm các mục menu
        menuPanel.add(createMenuButton("Projects"));
        menuPanel.add(createMenuButton("Students"));
        menuPanel.add(createMenuButton("Teachers"));
        menuPanel.add(createMenuButton("Charts"));
        menuPanel.add(createMenuButton("Change Password"));

        sidebar.add(menuPanel, BorderLayout.CENTER);
        return sidebar;
    }

    private JButton createMenuButton(String title) {
        JButton button = new JButton(title);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(220, 40));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(41, 53, 65));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(65, 85, 105));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(41, 53, 65));
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                onMenuSelected(title);
            }
        });

        return button;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setPreferredSize(new Dimension(0, 60));
        header.setBackground(new Color(238, 238, 238));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));

        JPanel left = new JPanel();
        left.setBackground(new Color(238, 238, 238));
        header.add(left, BorderLayout.WEST);

        JPanel userPanel = new JPanel();
        userPanel.setBackground(new Color(238, 238, 238));
        userPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 10));

        lblAvatar = new JLabel();
        setUserAvatar(loggedUser.getAvatarPath());
        userPanel.add(lblAvatar);

        JPanel userInfo = new JPanel();
        userInfo.setBackground(new Color(238, 238, 238));
        userInfo.setLayout(new BoxLayout(userInfo, BoxLayout.Y_AXIS));
        lblFullName = new JLabel(loggedUser.getFullName());
        lblFullName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblRole = new JLabel("Role: " + loggedUser.getRole());
        lblRole.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblRole.setForeground(Color.DARK_GRAY);

        userInfo.add(lblFullName);
        userInfo.add(lblRole);

        userPanel.add(userInfo);

        btnLogout = new JButton("Logout");
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> onLogout());
        userPanel.add(btnLogout);

        header.add(userPanel, BorderLayout.EAST);

        return header;
    }

    private void setUserAvatar(String avatarPath) {
        try {
            Image avatarImg;
            if (avatarPath != null && !avatarPath.isEmpty()) {
                avatarImg = ImageIO.read(getClass().getResource(avatarPath)).getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            } else {
                Icon defaultIcon = UIManager.getIcon("OptionPane.informationIcon");
                if (defaultIcon instanceof ImageIcon) {
                    avatarImg = ((ImageIcon) defaultIcon).getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                } else {
                    avatarImg = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
                }
            }
            lblAvatar.setIcon(new ImageIcon(avatarImg));
        } catch (Exception e) {
            lblAvatar.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
        }
    }

    private void onMenuSelected(String menu) {
        contentPanel.removeAll();

        switch (menu) {
            case "Projects":
                contentPanel.add(new ProjectsPanel(loggedUser, connection), BorderLayout.CENTER);
                break;
            case "Students":
                if ("admin".equals(loggedUser.getRole())) {
                    contentPanel.add(new StudentsPanel(loggedUser, connection), BorderLayout.CENTER);
                } else {
                    contentPanel.add(new JLabel("Chỉ admin mới có quyền truy cập.", JLabel.CENTER), BorderLayout.CENTER);
                }
                break;
            case "Teachers":
                if ("admin".equals(loggedUser.getRole())) {
                    contentPanel.add(new TeachersPanel(loggedUser, connection), BorderLayout.CENTER);
                } else {
                    contentPanel.add(new JLabel("Chỉ admin mới có quyền truy cập.", JLabel.CENTER), BorderLayout.CENTER);
                }
                break;
            case "Charts":
                contentPanel.add(new ChartsPanel(loggedUser, connection), BorderLayout.CENTER);
                break;
            case "Change Password":
                contentPanel.add(new ChangePasswordPanel(loggedUser, connection), BorderLayout.CENTER);
                break;
            default:
                contentPanel.add(new JLabel("Welcome", JLabel.CENTER), BorderLayout.CENTER);
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void onLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn đăng xuất?",
                "Xác nhận",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (logoutListener != null) {
                logoutListener.onLogout();
            } else {
                System.exit(0);
            }
        }
    }
}