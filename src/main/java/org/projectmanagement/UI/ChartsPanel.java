package org.projectmanagement.UI;

import org.projectmanagement.dao.ProjectDAO;
import org.projectmanagement.models.Project;
import org.projectmanagement.models.User;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ChartsPanel extends JPanel {
    private User loggedUser;
    private Connection connection;
    private ProjectDAO projectDAO;
    private JTextField txtSearch;
    private JButton btnSearch, btnReset;
    private ChartPanel pieChartPanel;
    private ChartPanel barChartPanel;

    public ChartsPanel(User user, Connection connection) {
        this.loggedUser = user;
        this.connection = connection;
        this.projectDAO = new ProjectDAO(connection);
        initComponents();
        loadChartAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Tiêu đề
        JLabel title = new JLabel("Thống kê đồ án", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setBorder(new EmptyBorder(10, 0, 10, 0));
        add(title, BorderLayout.NORTH);

        // Thanh tìm kiếm
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        txtSearch = new JTextField(20);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnSearch = new JButton("Tìm kiếm");
        btnSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnSearch.setBackground(new Color(0, 123, 255));
        btnSearch.setForeground(Color.WHITE);
        btnReset = new JButton("Reset");
        btnReset.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnReset.setBackground(new Color(108, 117, 125));
        btnReset.setForeground(Color.WHITE);
        btnSearch.addActionListener(e -> searchAndUpdateChart());
        btnReset.addActionListener(e -> loadChartAsync());
        searchPanel.add(new JLabel("Tìm kiếm:"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnReset);
        add(searchPanel, BorderLayout.NORTH);

        // Panel chứa hai biểu đồ (xếp dọc)
        JPanel chartsContainer = new JPanel();
        chartsContainer.setLayout(new BoxLayout(chartsContainer, BoxLayout.Y_AXIS));
        chartsContainer.setBackground(Color.WHITE);

        pieChartPanel = new ChartPanel(null);
        pieChartPanel.setBackground(Color.WHITE);
        pieChartPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Trạng thái đồ án",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 16)
        ));
        pieChartPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        barChartPanel = new ChartPanel(null);
        barChartPanel.setBackground(Color.WHITE);
        barChartPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Tình trạng nộp đồ án",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 16)
        ));
        barChartPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        chartsContainer.add(Box.createVerticalStrut(10)); // Khoảng cách
        chartsContainer.add(pieChartPanel);
        chartsContainer.add(Box.createVerticalStrut(20)); // Khoảng cách giữa biểu đồ
        chartsContainer.add(barChartPanel);
        chartsContainer.add(Box.createVerticalStrut(10));

        // Thêm vào JScrollPane để cuộn
        JScrollPane scrollPane = new JScrollPane(chartsContainer);
        scrollPane.setBackground(Color.WHITE);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadChartAsync() {
        SwingWorker<List<Project>, Void> worker = new SwingWorker<List<Project>, Void>() {
            @Override
            protected List<Project> doInBackground() throws Exception {
                synchronized (projectDAO) {
                    return projectDAO.findAll();
                }
            }

            @Override
            protected void done() {
                try {
                    List<Project> projects = get();
                    updateCharts(projects);
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(ChartsPanel.this, "Lỗi tải dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void searchAndUpdateChart() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập từ khóa tìm kiếm.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SwingWorker<List<Project>, Void> worker = new SwingWorker<List<Project>, Void>() {
            @Override
            protected List<Project> doInBackground() throws Exception {
                synchronized (projectDAO) {
                    return projectDAO.searchByTitleOrStudentId(keyword);
                }
            }

            @Override
            protected void done() {
                try {
                    List<Project> projects = get();
                    updateCharts(projects);
                    if (projects.isEmpty()) {
                        JOptionPane.showMessageDialog(ChartsPanel.this, "Không tìm thấy đồ án nào.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(ChartsPanel.this, "Lỗi tìm kiếm: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateCharts(List<Project> projects) {
        // Lọc theo role
        if ("user".equals(loggedUser.getRole())) {
            projects = projects.stream()
                    .filter(p -> p.getStudentId() == loggedUser.getUserId())
                    .collect(Collectors.toList());
        }

        // --- Pie Chart: Thống kê trạng thái ---
        Map<String, Long> statusCount = projects.stream()
                .collect(Collectors.groupingBy(
                        Project::getStatus,
                        Collectors.counting()
                ));

        DefaultPieDataset pieDataset = new DefaultPieDataset();
        String[] statuses = {"CHO_DUYET", "DUYET", "TU_CHOI", "DA_NOP"};
        String[] pieLabels = {"Chờ duyệt", "Đánh giá", "Từ chối", "Đã nộp"};
        for (int i = 0; i < statuses.length; i++) {
            pieDataset.setValue(pieLabels[i], statusCount.getOrDefault(statuses[i], 0L));
        }

        JFreeChart pieChart = ChartFactory.createPieChart3D(
                "",
                pieDataset,
                true,
                true,
                false
        );

        PiePlot3D piePlot = (PiePlot3D) pieChart.getPlot();
        piePlot.setBackgroundPaint(Color.WHITE);
        piePlot.setOutlinePaint(null);
        piePlot.setLabelFont(new Font("Segoe UI", Font.PLAIN, 14));
        piePlot.setLabelBackgroundPaint(new Color(240, 240, 240));
        piePlot.setLabelOutlinePaint(new Color(200, 200, 200));
        piePlot.setLabelShadowPaint(null);
        piePlot.setSectionPaint("Chờ duyệt", new Color(54, 162, 235));
        piePlot.setSectionPaint("Đánh giá", new Color(75, 192, 192));
        piePlot.setSectionPaint("Từ chối", new Color(255, 99, 132));
        piePlot.setSectionPaint("Đã nộp", new Color(153, 102, 255));
        piePlot.setLabelGenerator(new StandardPieSectionLabelGenerator(
                "{0}: {1} ({2})",
                new DecimalFormat("0"),
                new DecimalFormat("0%")
        ));

        pieChart.getLegend().setItemFont(new Font("Segoe UI", Font.PLAIN, 14));
        pieChartPanel.setChart(pieChart);
        pieChartPanel.setPreferredSize(new Dimension(450, 350));

        // --- Bar Chart: Đã nộp vs Chưa nộp ---
        long submittedCount = statusCount.getOrDefault("DA_NOP", 0L);
        long notSubmittedCount = projects.size() - submittedCount;

        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
        barDataset.addValue(submittedCount, "Số lượng", "Đã nộp");
        barDataset.addValue(notSubmittedCount, "Số lượng", "Chưa nộp");

        JFreeChart barChart = ChartFactory.createBarChart(
                "",
                "",
                "Số lượng",
                barDataset
        );

        CategoryPlot barPlot = barChart.getCategoryPlot();
        barPlot.setBackgroundPaint(Color.WHITE);
        barPlot.setDomainGridlinePaint(new Color(200, 200, 200));
        barPlot.setRangeGridlinePaint(new Color(200, 200, 200));

        BarRenderer renderer = (BarRenderer) barPlot.getRenderer();
        renderer.setSeriesPaint(0, new Color(75, 192, 192)); // Đã nộp: xanh lá
        renderer.setSeriesPaint(1, new Color(108, 117, 125)); // Chưa nộp: xám
        renderer.setDrawBarOutline(true);
        renderer.setItemMargin(0.2);

        barChart.getLegend().setItemFont(new Font("Segoe UI", Font.PLAIN, 14));
        barChart.getCategoryPlot().getDomainAxis().setLabelFont(new Font("Segoe UI", Font.PLAIN, 14));
        barChart.getCategoryPlot().getRangeAxis().setLabelFont(new Font("Segoe UI", Font.PLAIN, 14));
        barChart.getCategoryPlot().getDomainAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
        barChart.getCategoryPlot().getRangeAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 12));

        barChartPanel.setChart(barChart);
        barChartPanel.setPreferredSize(new Dimension(450, 350));
    }
}