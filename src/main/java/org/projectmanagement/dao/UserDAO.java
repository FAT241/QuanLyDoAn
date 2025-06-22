package org.projectmanagement.dao;

import org.projectmanagement.models.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private Connection connection;

    public UserDAO(Connection connection) {
        this.connection = connection;
    }

    // Đăng ký (Sign up)
    public boolean registerUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, email, role, full_name, avatar_path, student_code, phone_number) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword()); // phải là mật khẩu đã được BCrypt hash
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getRole());
            stmt.setString(5, user.getFullName());
            stmt.setString(6, user.getAvatarPath());
            stmt.setString(7, user.getStudentCode());
            stmt.setString(8, user.getPhoneNumber());
            return stmt.executeUpdate() > 0;
        }
    }

    // Đăng nhập bằng username (giữ nguyên)
    public User login(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password");

                // Dùng BCrypt để kiểm tra
                if (BCrypt.checkpw(password, hashedPassword)) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            hashedPassword,
                            rs.getString("email"),
                            rs.getString("role"),
                            rs.getString("full_name"),
                            rs.getString("avatar_path"),
                            rs.getString("student_code"),
                            rs.getString("phone_number")
                    );
                }
            }
        }
        return null;
    }

    // Đăng nhập bằng email (mới)
    public User loginByEmail(String email, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                if (BCrypt.checkpw(password, hashedPassword)) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            hashedPassword,
                            rs.getString("email"),
                            rs.getString("role"),
                            rs.getString("full_name"),
                            rs.getString("avatar_path"),
                            rs.getString("student_code"),
                            rs.getString("phone_number")
                    );
                }
            }
        }
        return null;
    }

    // Đổi mật khẩu (mới)
    public boolean changePassword(int userId, String oldPassword, String newPassword) throws SQLException {
        // Kiểm tra mật khẩu cũ
        String sql = "SELECT password FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                if (!BCrypt.checkpw(oldPassword, hashedPassword)) {
                    return false; // Mật khẩu cũ không đúng
                }
            } else {
                return false; // Không tìm thấy user
            }
        }

        // Cập nhật mật khẩu mới
        String hashedNewPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        sql = "UPDATE users SET password = ? WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, hashedNewPassword);
            stmt.setInt(2, userId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    // Tìm theo ID
    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("full_name"),
                        rs.getString("avatar_path"),
                        rs.getString("student_code"),
                        rs.getString("phone_number")
                );
            }
        }
        return null;
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email"),
                            rs.getString("role"),
                            rs.getString("full_name"),
                            rs.getString("avatar_path"),
                            rs.getString("student_code"),
                            rs.getString("phone_number")
                    );
                }
            }
        }
        return null;
    }
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email"),
                            rs.getString("role"),
                            rs.getString("full_name"),
                            rs.getString("avatar_path"),
                            rs.getString("student_code"),
                            rs.getString("phone_number")
                    );
                }
            }
        }
        return null;
    }

    // Cập nhật thông tin
    public boolean updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET email = ?, full_name = ?, avatar_path = ?, student_code = ?, phone_number = ? WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getAvatarPath());
            stmt.setString(4, user.getStudentCode());
            stmt.setString(5, user.getPhoneNumber());
            stmt.setInt(6, user.getUserId());
            return stmt.executeUpdate() > 0;
        }
    }

    // Xóa người dùng
    public boolean deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    // Lấy danh sách tất cả người dùng
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("full_name"),
                        rs.getString("avatar_path"),
                        rs.getString("student_code"),
                        rs.getString("phone_number")
                ));
            }
        }
        return users;
    }
}