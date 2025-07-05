package org.projectmanagement.dao;

import org.projectmanagement.models.Teacher;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TeacherDAO {
    private final Connection connection;

    public TeacherDAO(Connection connection) {
        this.connection = connection;
    }
// Thêm giáo viên vào cơ sở dữ liệu
    public void addTeacher(Teacher teacher) throws SQLException {
        // Kiểm tra email trùng lặp trong bảng users
        UserDAO userDAO = new UserDAO(connection);
        if (userDAO.findByEmail(teacher.getEmail()) != null) {
            throw new SQLException("Email đã tồn tại: " + teacher.getEmail());
        }

        // Thêm giáo viên vào bảng teachers và lấy teacher_id
        String sqlTeacher = "INSERT INTO teachers (full_name, email, phone_number, position) VALUES (?, ?, ?, ?)";
        int teacherId;
        try (PreparedStatement pstmt = connection.prepareStatement(sqlTeacher, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, teacher.getFullName());
            pstmt.setString(2, teacher.getEmail());
            pstmt.setString(3, teacher.getPhoneNumber());
            pstmt.setString(4, teacher.getPosition());
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Thêm giáo viên thất bại.");
            }

            // Lấy teacher_id vừa tạo
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    teacherId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Không lấy được teacher_id.");
                }
            }
        }

        // Tạo tài khoản người dùng trong bảng users
        String username = generateUsername(teacher.getEmail(), userDAO);
        String hashedPassword = BCrypt.hashpw("pass123", BCrypt.gensalt());
        String sqlUser = "INSERT INTO users (username, password, email, full_name, role, avatar_path, phone_number) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlUser)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, teacher.getEmail());
            pstmt.setString(4, teacher.getFullName());
            pstmt.setString(5, "teacher");
            pstmt.setString(6, "images/default.png");
            pstmt.setString(7, teacher.getPhoneNumber());
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Tạo tài khoản người dùng thất bại.");
            }
        }
    }
// Phương thức tạo username từ email
    private String generateUsername(String email, UserDAO userDAO) throws SQLException {
        // Tạo username từ email, ví dụ: teacher01@vku.edu.vn -> teacher01
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int suffix = 1;
        while (userDAO.findByUsername(username) != null) {
            username = baseUsername + suffix;
            suffix++;
        }
        return username;
    }
// Phương thức cập nhật thông tin giáo viên
    public void updateTeacher(Teacher teacher) throws SQLException {
        String sql = "UPDATE teachers SET full_name = ?, email = ?, phone_number = ?, position = ? WHERE teacher_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, teacher.getFullName());
            pstmt.setString(2, teacher.getEmail());
            pstmt.setString(3, teacher.getPhoneNumber());
            pstmt.setString(4, teacher.getPosition());
            pstmt.setInt(5, teacher.getTeacherId());
            pstmt.executeUpdate();
        }
    }

    public void deleteTeacher(int id) throws SQLException {
        String sql = "DELETE FROM teachers WHERE teacher_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public Teacher findById(int id) throws SQLException {
        String sql = "SELECT * FROM teachers WHERE teacher_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Teacher(
                            rs.getInt("teacher_id"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("phone_number"),
                            rs.getString("position")
                    );
                }
            }
        }
        return null;
    }
//
    public List<Teacher> findAll() throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT * FROM teachers";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                teachers.add(new Teacher(
                        rs.getInt("teacher_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        rs.getString("position")
                ));
            }
        }
        return teachers;
    }

    public List<Teacher> searchByNameOrEmail(String keyword) throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT * FROM teachers WHERE full_name LIKE ? OR email LIKE ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    teachers.add(new Teacher(
                            rs.getInt("teacher_id"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("phone_number"),
                            rs.getString("position")
                    ));
                }
            }
        }
        return teachers;
    }
}