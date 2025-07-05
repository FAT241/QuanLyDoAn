package org.projectmanagement.UI;

import org.projectmanagement.models.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public class MainPanel extends JPanel {
    private User loggedUser;
    private JPanel contentPanel;
    private JLabel lblFullName;
    private JLabel lblRole;
    private JLabel lblAvatar;
    private JButton btnLogout;
    private Connection connection;
    private MainFrame.LogoutListener logoutListener;

    // Modern color scheme - Enhanced contrast
    private static final Color SIDEBAR_COLOR = new Color(30, 39, 55);
    private static final Color SIDEBAR_HOVER = new Color(45, 56, 75);
    private static final Color SIDEBAR_ACTIVE = new Color(79, 172, 254);
    private static final Color HEADER_COLOR = new Color(255, 255, 255);
    private static final Color CONTENT_COLOR = new Color(248, 250, 252);
    private static final Color TEXT_PRIMARY = new Color(33, 41, 60);
    private static final Color TEXT_SECONDARY = new Color(107, 124, 147);
    private static final Color ACCENT_COLOR = new Color(79, 172, 254); // Màu avatar và button chính
    private static final Color BUTTON_PRIMARY = new Color(79, 172, 254);
    private static final Color BUTTON_SUCCESS = new Color(40, 167, 69);
    private static final Color BUTTON_WARNING = new Color(255, 193, 7);
    private static final Color BUTTON_DANGER = new Color(220, 53, 69);
    private static final Color CARD_SHADOW = new Color(0, 0, 0, 15);

    private String activeMenu = "Projects";
    private Map<String, JButton> menuButtons = new HashMap<>();

    public MainPanel(User user, Connection connection) {
        this.loggedUser = user;
        this.connection = connection;
        initComponents();
        setupResponsiveLayout();
    }

    private void initComponents() {
        this.setLayout(new BorderLayout());
        this.setBackground(CONTENT_COLOR);

        // Create components with modern styling
        JPanel sidebar = createModernSidebar();
        JPanel header = createModernHeader();

        // Content panel with modern styling
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(CONTENT_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 8, 20, 20));

        // Add initial content
        onMenuSelected("Projects");

        // Layout components
        this.add(sidebar, BorderLayout.WEST);
        this.add(header, BorderLayout.NORTH);
        this.add(contentPanel, BorderLayout.CENTER);
    }

    private void setupResponsiveLayout() {
        // Add component listener for responsive behavior
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                // Adjust layout based on window size
                Dimension size = getSize();
                if (size.width < 1000) {
                    // Compact mode for smaller screens
                    // Could implement sidebar collapse here
                }
            }
        });
    }

    public void setLogoutListener(MainFrame.LogoutListener listener) {
        this.logoutListener = listener;
    }

    private JPanel createModernSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(320, 0)); // Increased from 300 to 320
        sidebar.setBackground(SIDEBAR_COLOR);
        sidebar.setLayout(new BorderLayout());

        // Add subtle shadow effect
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1,
                new Color(0, 0, 0, 20)));

        // Logo panel with modern styling
        JPanel logoPanel = createLogoPanel();
        sidebar.add(logoPanel, BorderLayout.NORTH);

        // Menu panel with modern styling
        JPanel menuPanel = createMenuPanel();
        sidebar.add(menuPanel, BorderLayout.CENTER);

        // Footer panel
        JPanel footerPanel = createSidebarFooter();
        sidebar.add(footerPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel createLogoPanel() {
        JPanel logoPanel = new JPanel();
        logoPanel.setBackground(SIDEBAR_COLOR);
        logoPanel.setPreferredSize(new Dimension(320, 160)); // Tăng height để có không gian cho logo
        logoPanel.setLayout(new BorderLayout());
        logoPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Logo container với layout tốt hơn
        JPanel logoContainer = new JPanel(new BorderLayout());
        logoContainer.setBackground(SIDEBAR_COLOR);
        logoContainer.setPreferredSize(new Dimension(290, 100));

        // Try to load logo
        String logoPath = "/logo_vku.png";
        try {
            java.net.URL logoURL = getClass().getResource(logoPath);
            if (logoURL != null) {
                Image logo = ImageIO.read(logoURL);
                if (logo != null) {
                    // Tính toán kích thước logo mới với tỷ lệ phù hợp
                    int originalWidth = logo.getWidth(null);
                    int originalHeight = logo.getHeight(null);

                    // Đặt kích thước tối đa cho logo (tăng chiều ngang)
                    int maxWidth = 240;  // Doubled from 120
                    int maxHeight = 160;  // Doubled from 80

                    // Tính tỷ lệ scale để giữ nguyên aspect ratio
                    double scaleX = (double) maxWidth / originalWidth;
                    double scaleY = (double) maxHeight / originalHeight;
                    double scale = Math.min(scaleX, scaleY);

                    int newWidth = (int) (originalWidth * scale);
                    int newHeight = (int) (originalHeight * scale);

                    logo = logo.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                    JLabel lblLogo = new JLabel(new ImageIcon(logo));
                    lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
                    logoContainer.add(lblLogo, BorderLayout.CENTER);
                }
            }
        } catch (IOException e) {
            // Use text logo if image not found với kích thước lớn hơn
            JLabel textLogo = new JLabel("VKU", SwingConstants.CENTER);
            textLogo.setForeground(ACCENT_COLOR);
            textLogo.setFont(new Font("Segoe UI", Font.BOLD, 36)); // Tăng từ 32 lên 36
            logoContainer.add(textLogo, BorderLayout.CENTER);
        }

        logoPanel.add(logoContainer, BorderLayout.NORTH);

        // Title panel với spacing tốt hơn
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(SIDEBAR_COLOR);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel appTitle = new JLabel("VKU Project");
        appTitle.setForeground(Color.WHITE);
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        appTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel appSubtitle = new JLabel("Management System");
        appSubtitle.setForeground(TEXT_SECONDARY);
        appSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        appSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        titlePanel.add(Box.createVerticalStrut(10)); // Spacing trước title
        titlePanel.add(appTitle);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(appSubtitle);

        logoPanel.add(titlePanel, BorderLayout.CENTER);

        return logoPanel;
    }

    private JPanel createMenuPanel() {
        JPanel menuPanel = new JPanel();
        menuPanel.setBackground(SIDEBAR_COLOR);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // Menu items
        String[] menuItems = {"Projects", "Students", "Teachers", "Grades", "Charts", "Change Password"};

        for (String menuItem : menuItems) {
            JButton menuButton = createModernMenuButton(menuItem);
            menuButtons.put(menuItem, menuButton);
            menuPanel.add(menuButton);
            menuPanel.add(Box.createVerticalStrut(5));
        }

        return menuPanel;
    }

    private JPanel createSidebarFooter() {
        JPanel footer = new JPanel();
        footer.setBackground(SIDEBAR_COLOR);
        footer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        footer.setLayout(new BorderLayout());

        JLabel versionLabel = new JLabel("Version 1.0");
        versionLabel.setForeground(TEXT_SECONDARY);
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        versionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        footer.add(versionLabel, BorderLayout.CENTER);
        return footer;
    }

    private JButton createModernMenuButton(String title) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background
                if (activeMenu.equals(title)) {
                    g2.setColor(SIDEBAR_ACTIVE);
                } else if (getModel().isRollover()) {
                    g2.setColor(SIDEBAR_HOVER);
                } else {
                    g2.setColor(SIDEBAR_COLOR);
                }

                g2.fillRoundRect(10, 2, getWidth() - 20, getHeight() - 4, 8, 8);

                // Active indicator
                if (activeMenu.equals(title)) {
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(5, getHeight() / 2 - 10, 3, 20, 2, 2);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };

        button.setText(title);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(300, 45)); // Adjusted to match sidebar width
        button.setPreferredSize(new Dimension(300, 45)); // Adjusted to match sidebar width
        button.setForeground(Color.WHITE);
        button.setBackground(SIDEBAR_COLOR);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onMenuSelected(title);
            }
        });

        return button;
    }

    private JPanel createModernHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setPreferredSize(new Dimension(0, 80));
        header.setBackground(HEADER_COLOR);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0, 0, 0, 10)),
                BorderFactory.createEmptyBorder(15, 30, 15, 30)
        ));

        // Left side - Welcome message
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(HEADER_COLOR);

        JLabel welcomeLabel = new JLabel("Welcome back, " + loggedUser.getFullName() + "!");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        welcomeLabel.setForeground(TEXT_PRIMARY);
        leftPanel.add(welcomeLabel);

        // Right side - User info and logout
        JPanel rightPanel = createUserPanel();

        header.add(leftPanel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createUserPanel() {
        JPanel userPanel = new JPanel();
        userPanel.setBackground(HEADER_COLOR);
        userPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 0));

        // User avatar with modern styling
        lblAvatar = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw circular background
                g2.setColor(ACCENT_COLOR);
                g2.fillOval(0, 0, getWidth(), getHeight());

                // Draw image if available
                Icon icon = getIcon();
                if (icon != null) {
                    // Create circular clip
                    g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, getWidth(), getHeight()));
                    icon.paintIcon(this, g2, 0, 0);
                }

                g2.dispose();
            }
        };

        lblAvatar.setPreferredSize(new Dimension(50, 50));
        setUserAvatar(loggedUser.getAvatarPath());
        userPanel.add(lblAvatar);

        // User info panel
        JPanel userInfo = new JPanel();
        userInfo.setBackground(HEADER_COLOR);
        userInfo.setLayout(new BoxLayout(userInfo, BoxLayout.Y_AXIS));

        lblFullName = new JLabel(loggedUser.getFullName());
        lblFullName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblFullName.setForeground(TEXT_PRIMARY);

        lblRole = new JLabel(loggedUser.getRole().toUpperCase());
        lblRole.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblRole.setForeground(TEXT_SECONDARY);

        userInfo.add(lblFullName);
        userInfo.add(lblRole);
        userPanel.add(userInfo);

        // Modern logout button
        btnLogout = new JButton("Logout") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background
                if (getModel().isPressed()) {
                    g2.setColor(new Color(220, 53, 69, 180));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(220, 53, 69, 150));
                } else {
                    g2.setColor(new Color(220, 53, 69, 120));
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogout.setFocusPainted(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setContentAreaFilled(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.setPreferredSize(new Dimension(80, 35));
        btnLogout.addActionListener(e -> onLogout());

        userPanel.add(btnLogout);
        return userPanel;
    }
// Tạo avatar cho người dùng
    private void setUserAvatar(String avatarPath) {
        try {
            Image avatarImg;
            if (avatarPath != null && !avatarPath.isEmpty()) {
                avatarImg = ImageIO.read(getClass().getResource(avatarPath))
                        .getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            } else {
                // Create default avatar
                BufferedImage defaultAvatar = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = defaultAvatar.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT_COLOR); // Use accent color for default avatar
                g2.fillOval(0, 0, 50, 50);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
                String initial = loggedUser.getFullName().substring(0, 1).toUpperCase();
                FontMetrics fm = g2.getFontMetrics();
                int x = (50 - fm.stringWidth(initial)) / 2;
                int y = ((50 - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(initial, x, y);
                g2.dispose();
                avatarImg = defaultAvatar;
            }
            lblAvatar.setIcon(new ImageIcon(avatarImg));
        } catch (Exception e) {
            // Create simple text avatar
            lblAvatar.setText(loggedUser.getFullName().substring(0, 1).toUpperCase());
            lblAvatar.setHorizontalAlignment(SwingConstants.CENTER);
            lblAvatar.setForeground(Color.WHITE);
            lblAvatar.setFont(new Font("Segoe UI", Font.BOLD, 20));
        }
    }

    private void onMenuSelected(String menu) {
        // Update active menu
        activeMenu = menu;

        // Update button states
        for (Map.Entry<String, JButton> entry : menuButtons.entrySet()) {
            entry.getValue().repaint();
        }

        // Clear content with fade effect
        contentPanel.removeAll();

        // Create content wrapper with modern styling
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setBackground(Color.WHITE);
        contentWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0, 10), 1, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        switch (menu) {
            case "Projects":
                contentWrapper.add(new ProjectsPanel(loggedUser, connection), BorderLayout.CENTER);
                break;
            case "Students":
                if ("admin".equals(loggedUser.getRole())) {
                    contentWrapper.add(new StudentsPanel(loggedUser, connection), BorderLayout.CENTER);
                } else {
                    contentWrapper.add(createAccessDeniedPanel("Students"), BorderLayout.CENTER);
                }
                break;
            case "Teachers":
                if ("admin".equals(loggedUser.getRole())) {
                    contentWrapper.add(new TeachersPanel(loggedUser, connection), BorderLayout.CENTER);
                } else {
                    contentWrapper.add(createAccessDeniedPanel("Teachers"), BorderLayout.CENTER);
                }
                break;
            case "Grades":
                contentWrapper.add(new GradesPanel(loggedUser, connection), BorderLayout.CENTER);
                break;
            case "Charts":
                contentWrapper.add(new ChartsPanel(loggedUser, connection), BorderLayout.CENTER);
                break;
            case "Change Password":
                contentWrapper.add(new ChangePasswordPanel(loggedUser, connection), BorderLayout.CENTER);
                break;
            default:
                contentWrapper.add(createWelcomePanel(), BorderLayout.CENTER);
        }

        contentPanel.add(contentWrapper, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JPanel createAccessDeniedPanel(String feature) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Title
        JLabel titleLabel = new JLabel("Access Denied");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Message
        JLabel messageLabel = new JLabel("You don't have permission to access " + feature);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageLabel.setForeground(TEXT_SECONDARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(messageLabel);
        centerPanel.add(Box.createVerticalGlue());

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Welcome message
        JLabel welcomeLabel = new JLabel("Welcome to VKU Project Management System");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        welcomeLabel.setForeground(TEXT_PRIMARY);
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Choose a menu item from the sidebar to get started");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Quick stats or features panel
        JPanel statsPanel = createQuickStatsPanel();

        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(welcomeLabel);
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(subtitleLabel);
        centerPanel.add(Box.createVerticalStrut(40));
        centerPanel.add(statsPanel);
        centerPanel.add(Box.createVerticalGlue());

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createQuickStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setMaximumSize(new Dimension(800, 120));

        // Sample stats cards
        statsPanel.add(createStatCard("Projects", "12", ACCENT_COLOR));
        statsPanel.add(createStatCard("Students", "156", new Color(40, 167, 69)));
        statsPanel.add(createStatCard("Teachers", "23", new Color(255, 193, 7)));
        statsPanel.add(createStatCard("Grades", "1,204", new Color(220, 53, 69)));

        return statsPanel;
    }

    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background with gradient
                GradientPaint gradient = new GradientPaint(
                        0,
                        0,
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 20),
                        0,
                        getHeight(),
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 40)
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                // Border
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        card.setLayout(new BorderLayout());
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(180, 120));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Content
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleLabel.setForeground(TEXT_SECONDARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentPanel.add(valueLabel);
        contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(titleLabel);

        card.add(contentPanel, BorderLayout.CENTER);

        return card;
    }

    private void onLogout() {
        // Create modern confirmation dialog
        int result = showModernConfirmDialog(
                "Are you sure you want to logout?",
                "Confirm Logout"
        );

        if (result == JOptionPane.YES_OPTION) {
            if (logoutListener != null) {
                logoutListener.onLogout();
            } else {
                System.exit(0);
            }
        }
    }

    private int showModernConfirmDialog(String message, String title) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(30, 30, 20, 30));

        // Message
        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageLabel.setForeground(TEXT_PRIMARY);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBackground(Color.WHITE);

        JButton yesButton = createModernButton("Yes", ACCENT_COLOR, Color.WHITE);
        JButton noButton = createModernButton("No", new Color(108, 117, 125), Color.WHITE);

        final int[] result = {JOptionPane.NO_OPTION};

        yesButton.addActionListener(e -> {
            result[0] = JOptionPane.YES_OPTION;
            dialog.dispose();
        });

        noButton.addActionListener(e -> {
            result[0] = JOptionPane.NO_OPTION;
            dialog.dispose();
        });

        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);

        content.add(messageLabel, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);

        return result[0];
    }

    private JButton createModernButton(String text, Color bgColor, Color textColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color color = bgColor;
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

        button.setForeground(textColor);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(80, 35));

        return button;
    }
}