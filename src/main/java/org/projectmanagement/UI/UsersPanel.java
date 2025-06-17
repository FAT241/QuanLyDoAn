package org.projectmanagement.UI;

import org.projectmanagement.models.User;

import java.awt.*;
import java.sql.Connection;

import javax.swing.*;

public class UsersPanel extends JPanel {
    public UsersPanel(User user, Connection connection) {
        setLayout(new BorderLayout());
        add(new JLabel("Quản lý người dùng - Chưa triển khai", JLabel.CENTER), BorderLayout.CENTER);
    }
}