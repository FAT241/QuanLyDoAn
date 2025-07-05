package org.projectmanagement.UI;

import org.projectmanagement.models.User;
import org.projectmanagement.util.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.SQLException;

public class MainFrame extends JFrame {
    private RegisterPanel registerPanel;
    private Connection connection;
    private LoginPanel loginPanel;
    private MainPanel mainPanel;
    private JPanel mainContainer;

    // Modern color scheme
    private static final Color BACKGROUND_COLOR = new Color(245, 247, 250);
    private static final Color ACCENT_COLOR = new Color(79, 172, 254);

    public MainFrame(Connection connection) {
        this.connection = connection;
        initUI();
        setupWindowProperties();
    }
    private class FadePanel extends JPanel {
        private float alpha = 1.0f;
        private JPanel contentPanel;

        public FadePanel(JPanel content) {
            super(new BorderLayout());
            this.contentPanel = content;
            setOpaque(false);
            add(content, BorderLayout.CENTER);
        }

        public void setAlpha(float alpha) {
            this.alpha = Math.max(0.0f, Math.min(1.0f, alpha)); // Clamp to valid range
            repaint();
        }

        public float getAlpha() {
            return alpha;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            super.paintComponent(g2d);
            g2d.dispose();
        }
    }

    private void initUI() {
        setTitle("Project Management System - VKU");
        setMinimumSize(new Dimension(1200, 800));
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Set modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Modern window styling
        setBackground(BACKGROUND_COLOR);

        // Main container with modern styling
        mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(BACKGROUND_COLOR);
        mainContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        loginPanel = new LoginPanel(connection);
        registerPanel = new RegisterPanel(connection);

        // Add smooth transition effect
        mainContainer.add(createTransitionPanel(loginPanel), BorderLayout.CENTER);
        add(mainContainer);

        setupEventListeners();
    }

    private void setupWindowProperties() {
        // Add window resize listener for responsive design
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Ensure minimum size is maintained
                Dimension size = getSize();
                Dimension minSize = getMinimumSize();
                if (size.width < minSize.width || size.height < minSize.height) {
                    setSize(Math.max(size.width, minSize.width),
                            Math.max(size.height, minSize.height));
                }
            }
        });

        // Set window icon if available
        try {
            setIconImage(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource("/logo_vku.png")));
        } catch (Exception e) {
            // Use default icon if custom icon not found
        }
    }

    private void setupEventListeners() {
        // Login thành công với animation
        loginPanel.addPropertyChangeListener("loginSuccess", evt -> {
            User user = (User) evt.getNewValue();
            SwingUtilities.invokeLater(() -> onLoginSuccess(user));
        });

        // Chuyển sang giao diện đăng ký với transition
        loginPanel.addPropertyChangeListener("goToRegister", evt -> {
            SwingUtilities.invokeLater(() -> switchPanelWithTransition(registerPanel));
        });

        // Quay lại đăng nhập từ đăng ký với transition
        registerPanel.addPropertyChangeListener("backToLogin", evt -> {
            SwingUtilities.invokeLater(() -> switchPanelWithTransition(loginPanel));
        });
    }

    private JPanel createTransitionPanel(JPanel content) {
        JPanel transitionPanel = new JPanel(new BorderLayout());
        transitionPanel.setBackground(BACKGROUND_COLOR);
        transitionPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Add subtle shadow effect
        transitionPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20),
                BorderFactory.createLineBorder(new Color(0, 0, 0, 10), 1, true)
        ));

        transitionPanel.add(content, BorderLayout.CENTER);
        return transitionPanel;
    }

    private void switchPanelWithTransition(JPanel newPanel) {
        // Get current panel
        Component currentComponent = mainContainer.getComponent(0);

        // Create fade panels
        FadePanel fadeOutPanel = new FadePanel((JPanel) ((JPanel) currentComponent).getComponent(0));
        FadePanel fadeInPanel = new FadePanel(newPanel);
        fadeInPanel.setAlpha(0.0f);

        // Setup transition
        Timer transitionTimer = new Timer(20, null);
        final int[] step = {0};
        final int totalSteps = 20;

        transitionTimer.addActionListener(e -> {
            step[0]++;
            float progress = (float) step[0] / totalSteps;

            // Fade out current panel
            fadeOutPanel.setAlpha(1.0f - progress);

            // Fade in new panel
            fadeInPanel.setAlpha(progress);

            if (step[0] >= totalSteps) {
                transitionTimer.stop();
                // Final cleanup - replace with the actual new panel without fade wrapper
                mainContainer.removeAll();
                mainContainer.add(createTransitionPanel(newPanel), BorderLayout.CENTER);
                mainContainer.revalidate();
                mainContainer.repaint();
            }
        });

        // Start transition
        mainContainer.removeAll();

        // Create layered panel for transition
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(mainContainer.getSize());

        // Add both panels to the layered pane
        fadeOutPanel.setBounds(0, 0, mainContainer.getWidth(), mainContainer.getHeight());
        fadeInPanel.setBounds(0, 0, mainContainer.getWidth(), mainContainer.getHeight());

        layeredPane.add(fadeOutPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(fadeInPanel, JLayeredPane.PALETTE_LAYER);

        mainContainer.add(layeredPane, BorderLayout.CENTER);
        mainContainer.revalidate();
        mainContainer.repaint();

        transitionTimer.start();
    }

    private void onLoginSuccess(User user) {
        // Create main panel with modern styling
        mainPanel = new MainPanel(user, connection);

        // Setup logout listener with smooth transition
        mainPanel.setLogoutListener(() -> {
            SwingUtilities.invokeLater(() -> {
                // Clear user session data
                clearUserSession();

                // Smooth transition back to login
                switchPanelWithTransition(loginPanel);

                // Reset login panel
                if (loginPanel instanceof LoginPanel) {
                    // Clear login form if method exists
                    try {
                        loginPanel.getClass().getMethod("clearForm").invoke(loginPanel);
                    } catch (Exception e) {
                        // Method might not exist, that's ok
                    }
                }
            });
        });

        // Smooth transition to main panel
        Timer transitionTimer = new Timer(300, e -> {
            mainContainer.removeAll();
            mainContainer.add(mainPanel, BorderLayout.CENTER);
            mainContainer.revalidate();
            mainContainer.repaint();
            ((Timer) e.getSource()).stop();
        });
        transitionTimer.setRepeats(false);
        transitionTimer.start();
    }

    private void clearUserSession() {
        // Clear any cached user data or session information
        if (mainPanel != null) {
            mainPanel = null;
        }
        System.gc(); // Suggest garbage collection
    }

    public interface LogoutListener {
        void onLogout();
    }

    // Enhanced error handling and connection management
    public static void main(String[] args) {
        // Set system properties for better UI rendering
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Enable anti-aliasing for better text rendering
        System.setProperty("sun.java2d.opengl", "true");

        try {
            Connection conn = DBConnection.getConnection();

            SwingUtilities.invokeLater(() -> {
                try {
                    // Create splash screen effect
                    JWindow splash = createSplashScreen();
                    splash.setVisible(true);

                    // Simulate loading time
                    Timer splashTimer = new Timer(2000, e -> {
                        splash.dispose();

                        // Create and show main frame
                        MainFrame frame = new MainFrame(conn);

                        // Show frame without opacity animation (since it causes issues with decorated frames)
                        frame.setVisible(true);

                        // Alternative: Use a simple slide-in animation instead
                        Point originalLocation = frame.getLocation();
                        frame.setLocation(originalLocation.x, originalLocation.y - 100);

                        Timer slideIn = new Timer(20, null);
                        final int[] currentY = {originalLocation.y - 100};

                        slideIn.addActionListener(slideEvent -> {
                            currentY[0] += 5;
                            if (currentY[0] >= originalLocation.y) {
                                currentY[0] = originalLocation.y;
                                slideIn.stop();
                            }
                            frame.setLocation(originalLocation.x, currentY[0]);
                        });
                        slideIn.start();

                        ((Timer) e.getSource()).stop();
                    });
                    splashTimer.setRepeats(false);
                    splashTimer.start();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    showErrorDialog("Lỗi khởi tạo ứng dụng: " + ex.getMessage());
                }
            });

        } catch (SQLException ex) {
            ex.printStackTrace();
            showErrorDialog("Không thể kết nối cơ sở dữ liệu!\nChi tiết: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static JWindow createSplashScreen() {
        JWindow splash = new JWindow();
        splash.setSize(400, 250);
        splash.setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createLineBorder(new Color(79, 172, 254), 2));

        // Logo and title
        JLabel titleLabel = new JLabel("Project Management System", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(79, 172, 254));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(40, 20, 20, 20));

        JLabel subtitleLabel = new JLabel("Vietnam-Korea University", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.GRAY);

        // Loading animation
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(false);
        progressBar.setBorder(BorderFactory.createEmptyBorder(20, 40, 40, 40));

        content.add(titleLabel, BorderLayout.NORTH);
        content.add(subtitleLabel, BorderLayout.CENTER);
        content.add(progressBar, BorderLayout.SOUTH);

        splash.setContentPane(content);
        return splash;
    }

    private static void showErrorDialog(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message, "Lỗi hệ thống",
                    JOptionPane.ERROR_MESSAGE);
        });
    }
}