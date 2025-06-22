package org.projectmanagement.dao;

import org.projectmanagement.models.Project;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectDAO {
    private Connection connection;

    public ProjectDAO(Connection connection) {
        this.connection = connection;
    }

    public List<Project> findAll() throws SQLException {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT p.project_id, p.title, p.description, p.ngay_bat_dau, p.ngay_ket_thuc, p.ngay_nop, p.status, " +
                "s.full_name AS student_name, t.full_name AS teacher_name, s.student_id, t.teacher_id " +
                "FROM projects p " +
                "LEFT JOIN students s ON p.student_id = s.student_id " +
                "LEFT JOIN teachers t ON p.teacher_id = t.teacher_id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Project p = new Project(
                        rs.getInt("project_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getDate("ngay_bat_dau"),
                        rs.getDate("ngay_ket_thuc"),
                        rs.getDate("ngay_nop"),
                        loadFilePaths(rs.getInt("project_id")),
                        loadComments(rs.getInt("project_id")),
                        rs.getInt("student_id"),
                        rs.getInt("teacher_id")
                );
                p.setStudentName(rs.getString("student_name"));
                p.setTeacherName(rs.getString("teacher_name"));
                p.setStatus(rs.getString("status"));
                projects.add(p);
            }
        }
        return projects;
    }

    public Project findById(int projectId) throws SQLException {
        String sql = "SELECT p.project_id, p.title, p.description, p.ngay_bat_dau, p.ngay_ket_thuc, p.ngay_nop, p.status, " +
                "s.full_name AS student_name, t.full_name AS teacher_name, s.student_id, t.teacher_id " +
                "FROM projects p " +
                "LEFT JOIN students s ON p.student_id = s.student_id " +
                "LEFT JOIN teachers t ON p.teacher_id = t.teacher_id " +
                "WHERE p.project_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Project p = new Project(
                        rs.getInt("project_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getDate("ngay_bat_dau"),
                        rs.getDate("ngay_ket_thuc"),
                        rs.getDate("ngay_nop"),
                        loadFilePaths(projectId),
                        loadComments(projectId),
                        rs.getInt("student_id"),
                        rs.getInt("teacher_id")
                );
                p.setStudentName(rs.getString("student_name"));
                p.setTeacherName(rs.getString("teacher_name"));
                p.setStatus(rs.getString("status"));
                return p;
            }
        }
        return null;
    }

    public List<Project> searchByTitleOrStudentId(String keyword) throws SQLException {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT p.project_id, p.title, p.description, p.ngay_bat_dau, p.ngay_ket_thuc, p.ngay_nop, p.status, " +
                "s.full_name AS student_name, t.full_name AS teacher_name, s.student_id, t.teacher_id " +
                "FROM projects p " +
                "LEFT JOIN students s ON p.student_id = s.student_id " +
                "LEFT JOIN teachers t ON p.teacher_id = t.teacher_id " +
                "WHERE p.title LIKE ? OR CAST(s.student_id AS TEXT) LIKE ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Project p = new Project(
                        rs.getInt("project_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getDate("ngay_bat_dau"),
                        rs.getDate("ngay_ket_thuc"),
                        rs.getDate("ngay_nop"),
                        loadFilePaths(rs.getInt("project_id")),
                        loadComments(rs.getInt("project_id")),
                        rs.getInt("student_id"),
                        rs.getInt("teacher_id")
                );
                p.setStudentName(rs.getString("student_name"));
                p.setTeacherName(rs.getString("teacher_name"));
                p.setStatus(rs.getString("status"));
                projects.add(p);
            }
        }
        return projects;
    }

    public boolean addProject(Project project) throws SQLException {
        String sql = "INSERT INTO projects (title, description, ngay_bat_dau, ngay_ket_thuc, status, student_id, teacher_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, project.getTitle());
            pstmt.setString(2, project.getDescription());
            pstmt.setDate(3, new java.sql.Date(project.getNgayBatDau().getTime()));
            pstmt.setDate(4, new java.sql.Date(project.getNgayKetThuc().getTime()));
            pstmt.setString(5, project.getStatus());
            pstmt.setInt(6, project.getStudentId());
            pstmt.setInt(7, project.getTeacherId());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        project.setProjectId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean updateProject(Project project) throws SQLException {
        String sql = "UPDATE projects SET title = ?, description = ?, ngay_bat_dau = ?, ngay_ket_thuc = ?, status = ?, student_id = ?, teacher_id = ?, ngay_nop = ? WHERE project_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, project.getTitle());
            pstmt.setString(2, project.getDescription());
            pstmt.setDate(3, new java.sql.Date(project.getNgayBatDau().getTime()));
            pstmt.setDate(4, new java.sql.Date(project.getNgayKetThuc().getTime()));
            pstmt.setString(5, project.getStatus());
            pstmt.setInt(6, project.getStudentId());
            pstmt.setInt(7, project.getTeacherId());
            pstmt.setDate(8, project.getNgayNop() != null ? new java.sql.Date(project.getNgayNop().getTime()) : null);
            pstmt.setInt(9, project.getProjectId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteProject(int projectId) throws SQLException {
        String sql = "DELETE FROM projects WHERE project_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean addFile(int projectId, String filePath) throws SQLException {
        String sql = "INSERT INTO project_files (project_id, file_path) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            pstmt.setString(2, filePath);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean addComment(int projectId, int teacherId, String comment) throws SQLException {
        String sql = "INSERT INTO project_comments (project_id, teacher_id, comment) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            pstmt.setInt(2, teacherId);
            pstmt.setString(3, comment);
            return pstmt.executeUpdate() > 0;
        }
    }

    private List<String> loadFilePaths(int projectId) throws SQLException {
        List<String> filePaths = new ArrayList<>();
        String sql = "SELECT file_path FROM project_files WHERE project_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                filePaths.add(rs.getString("file_path"));
            }
        }
        return filePaths;
    }

    private List<String> loadComments(int projectId) throws SQLException {
        List<String> comments = new ArrayList<>();
        String sql = "SELECT comment FROM project_comments WHERE project_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                comments.add(rs.getString("comment"));
            }
        }
        return comments;
    }
}