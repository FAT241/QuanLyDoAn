package org.projectmanagement.UI;

import org.projectmanagement.dao.ProjectDAO;
import org.projectmanagement.models.Project;
import org.projectmanagement.models.User;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
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
    private JTable summaryTable;

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
        title.setForeground(Color.BLACK);
        title.setBorder(new EmptyBorder(10, 0, 10, 0));

        // Thanh tìm kiếm
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JLabel searchLabel = new JLabel("Tìm kiếm:");
        searchLabel.setForeground(Color.BLACK);
        txtSearch = new JTextField(20);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSearch.setForeground(Color.BLACK);
        btnSearch = new JButton("Tìm kiếm");
        btnSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnSearch.setBackground(new Color(0, 123, 255));
        btnSearch.setForeground(Color.BLACK);
        btnReset = new JButton("Reset");
        btnReset.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnReset.setBackground(new Color(108, 117, 125));
        btnReset.setForeground(Color.BLACK);
        btnSearch.addActionListener(e -> searchAndUpdateChart());
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            loadChartAsync();
        });
        searchPanel.add(searchLabel);
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnReset);

        // Container for title and search panel
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(Color.WHITE);
        northPanel.add(title, BorderLayout.NORTH);
        northPanel.add(searchPanel, BorderLayout.CENTER);
        add(northPanel, BorderLayout.NORTH);

        // Panel chứa biểu đồ và bảng tóm tắt
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

        // Bảng tóm tắt
        summaryTable = new JTable();
        summaryTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        summaryTable.setRowHeight(25);
        summaryTable.setGridColor(new Color(200, 200, 200));
        JScrollPane tableScrollPane = new JScrollPane(summaryTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Tóm tắt trạng thái",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 16)
        ));

        chartsContainer.add(Box.createVerticalStrut(10));
        chartsContainer.add(pieChartPanel);
        chartsContainer.add(Box.createVerticalStrut(20));
        chartsContainer.add(tableScrollPane);
        chartsContainer.add(Box.createVerticalStrut(10));

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
        String Nathan = txtSearch.getText().trim();
        if (Nathan.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập từ khóa tìm kiếm.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SwingWorker<List<Project>, Void> worker = new SwingWorker<List<Project>, Void>() {
            @Override
            protected List<Project> doInBackground() throws Exception {
                synchronized (projectDAO) {
                    return projectDAO.searchByTitleOrStudentId(Nathan);
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

        JFreeChart pieChart = ChartFactory.createPieChart(
                "",
                pieDataset,
                true,
                true,
                false
        );

        PiePlot piePlot = (PiePlot) pieChart.getPlot();
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

        // --- Bảng tóm tắt ---
        DefaultTableModel tableModel = new DefaultTableModel(
                new String[]{"Trạng thái", "Số lượng", "Tỷ lệ"}, 0
        );
        long total = projects.size();
        for (int i = 0; i < statuses.length; i++) {
            long count = statusCount.getOrDefault(statuses[i], 0L);
            double percentage = total > 0 ? (count * 100.0 / total) : 0;
            tableModel.addRow(new Object[]{
                    pieLabels[i],
                    count,
                    String.format("%.1f%%", percentage)
            });
        }
        summaryTable.setModel(tableModel);
    }
}