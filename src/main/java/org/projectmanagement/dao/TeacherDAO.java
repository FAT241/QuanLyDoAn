package org.projectmanagement.dao;

import org.projectmanagement.models.Teacher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TeacherDAO {
    private final Connection connection;

    public TeacherDAO(Connection connection) {
        this.connection = connection;
    }

    public void addTeacher(Teacher teacher) throws SQLException {
        String sql = "INSERT INTO teachers (full_name, email, phone_number, position) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, teacher.getFullName());
            pstmt.setString(2, teacher.getEmail());
            pstmt.setString(3, teacher.getPhoneNumber());
            pstmt.setString(4, teacher.getPosition());
            pstmt.executeUpdate();
        }
    }

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

    // Thêm phương thức tìm kiếm
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