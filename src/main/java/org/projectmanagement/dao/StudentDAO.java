package org.projectmanagement.dao;

import org.projectmanagement.models.Student;
import org.projectmanagement.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {
    private final Connection connection;

    public StudentDAO(Connection connection) {
        this.connection = connection;
    }
// Phương thức thêm sinh viên
    public void addStudent(Student student, int userId) throws SQLException {
        String sql = "INSERT INTO students (full_name, email, phone_number, major, class_code, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, student.getFullName());
            pstmt.setString(2, student.getEmail());
            pstmt.setString(3, student.getPhoneNumber());
            pstmt.setString(4, student.getMajor());
            pstmt.setString(5, student.getClassCode());
            pstmt.setInt(6, userId);
            pstmt.executeUpdate();

            // Lấy student_id vừa tạo
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    student.setStudentId(rs.getInt(1));
                }
            }
        }
    }
// Phương thức cập nhật thông tin sinh viên
    public void updateStudent(Student student) throws SQLException {
        String sql = "UPDATE students SET full_name = ?, email = ?, phone_number = ?, major = ?, class_code = ?, user_id = ? WHERE student_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, student.getFullName());
            pstmt.setString(2, student.getEmail());
            pstmt.setString(3, student.getPhoneNumber());
            pstmt.setString(4, student.getMajor());
            pstmt.setString(5, student.getClassCode());
            pstmt.setInt(6, student.getUserId());
            pstmt.setInt(7, student.getStudentId());
            pstmt.executeUpdate();
        }
    }
// Phương thức xóa sinh viên
    public void deleteStudent(int id) throws SQLException {
        String sql = "DELETE FROM students WHERE student_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
// Phương thức tìm kiếm sinh viên theo ID
    public Student findById(int id) throws SQLException {
        String sql = "SELECT * FROM students WHERE student_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Student(
                            rs.getInt("student_id"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("phone_number"),
                            rs.getString("major"),
                            rs.getString("class_code"),
                            rs.getInt("user_id")
                    );
                }
            }
        }
        return null;
    }
// Phương thức lấy danh sách tất cả sinh viên
    public List<Student> findAll() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                students.add(new Student(
                        rs.getInt("student_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        rs.getString("major"),
                        rs.getString("class_code"),
                        rs.getInt("user_id")
                ));
            }
        }
        return students;
    }

    // Thêm phương thức tìm kiếm
    public List<Student> searchByNameOrEmail(String keyword) throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students WHERE full_name LIKE ? OR email LIKE ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    students.add(new Student(
                            rs.getInt("student_id"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("phone_number"),
                            rs.getString("major"),
                            rs.getString("class_code"),
                            rs.getInt("user_id")
                    ));
                }
            }
        }
        return students;
    }
}