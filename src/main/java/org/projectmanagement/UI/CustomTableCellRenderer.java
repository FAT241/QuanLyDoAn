package org.projectmanagement.UI;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class CustomTableCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // Căn chỉnh
        if (value instanceof Number) {
            setHorizontalAlignment(CENTER);
        } else {
            setHorizontalAlignment(LEFT);
        }

        // Màu nền và chữ
        if (!isSelected) {
            if (row % 2 == 0) {
                cell.setBackground(new Color(245, 245, 245)); // Xám nhạt
            } else {
                cell.setBackground(Color.WHITE);
            }
            cell.setForeground(Color.BLACK); // Explicitly set foreground for unselected rows
        } else {
            cell.setBackground(new Color(0, 123, 255)); // Màu xanh khi chọn
            cell.setForeground(Color.WHITE); // Foreground for selected rows
        }

        // Font và padding
        cell.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        return cell;
    }
}