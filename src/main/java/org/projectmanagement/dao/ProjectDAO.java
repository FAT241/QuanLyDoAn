package org.projectmanagement.dao;

import org.projectmanagement.models.Project;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProjectDAO {
    private final Connection connection;
    private static final List<String> VALID_STATUSES = Arrays.asList("CHO_DUYET", "DUYET", "TU_CHOI", "DA_NOP");

    public ProjectDAO(Connection connection) {
        this.connection = connection;
    }

    public boolean addProject(Project project) throws SQLException {
        if (!VALID_STATUSES.contains(project.getStatus())) {
            throw new SQLException("Trạng thái không hợp lệ: " + project.getStatus());
        }

        String sql = "INSERT INTO projects (title, description, ngay_bat_dau, ngay_ket_thuc, ngay_nop, status, tep_bao_cao, student_id, teacher_id, comment) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, project.getTitle());
            stmt.setString(2, project.getDescription());
            stmt.setDate(3, new java.sql.Date(project.getNgayBatDau().getTime()));
            stmt.setDate(4, new java.sql.Date(project.getNgayKetThuc().getTime()));
            if (project.getNgayNop() != null) {
                stmt.setDate(5, new java.sql.Date(project.getNgayNop().getTime()));
            } else {
                stmt.setNull(5, Types.DATE);
            }
            stmt.setString(6, project.getStatus());
            stmt.setString(7, project.getTepBaoCao());
            stmt.setInt(8, project.getStudentId());
            stmt.setInt(9, project.getTeacherId());
            stmt.setString(10, project.getComment()); // Thêm comment
            return stmt.executeUpdate() > 0;
        }
    }

    public Project findById(int projectId) throws SQLException {
        String sql = "SELECT p.*, s.full_name AS student_name, t.full_name AS teacher_name " +
                "FROM projects p " +
                "LEFT JOIN students s ON p.student_id = s.student_id " +
                "LEFT JOIN teachers t ON p.teacher_id = t.teacher_id " +
                "WHERE p.project_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractProjectFromResultSet(rs);
                }
            }
        }
        return null;
    }

    public boolean updateProject(Project project) throws SQLException {
        if (!VALID_STATUSES.contains(project.getStatus())) {
            throw new SQLException("Trạng thái không hợp lệ: " + project.getStatus());
        }

        Project existingProject = findById(project.getProjectId());
        if (existingProject != null && existingProject.getStatus().equals("CHO_DUYET") &&
                (project.getStatus().equals("DUYET") || project.getStatus().equals("TU_CHOI"))) {
            // Giả định logic kiểm tra quyền admin được thực hiện ở tầng giao diện
        }

        String sql = "UPDATE projects SET title=?, description=?, ngay_bat_dau=?, ngay_ket_thuc=?, ngay_nop=?, status=?, tep_bao_cao=?, student_id=?, teacher_id=?, comment=? " +
                "WHERE project_id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, project.getTitle());
            stmt.setString(2, project.getDescription());
            stmt.setDate(3, new java.sql.Date(project.getNgayBatDau().getTime()));
            stmt.setDate(4, new java.sql.Date(project.getNgayKetThuc().getTime()));
            if (project.getNgayNop() != null) {
                stmt.setDate(5, new java.sql.Date(project.getNgayNop().getTime()));
            } else {
                stmt.setNull(5, Types.DATE);
            }
            stmt.setString(6, project.getStatus());
            stmt.setString(7, project.getTepBaoCao());
            stmt.setInt(8, project.getStudentId());
            stmt.setInt(9, project.getTeacherId());
            stmt.setString(10, project.getComment()); // Thêm comment
            stmt.setInt(11, project.getProjectId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteProject(int projectId) throws SQLException {
        String sql = "DELETE FROM projects WHERE project_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Project> findAll() throws SQLException {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT p.*, s.full_name AS student_name, t.full_name AS teacher_name " +
                "FROM projects p " +
                "LEFT JOIN students s ON p.student_id = s.student_id " +
                "LEFT JOIN teachers t ON p.teacher_id = t.teacher_id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                projects.add(extractProjectFromResultSet(rs));
            }
        }
        return projects;
    }

    public List<Project> getProjectsByStudentId(int studentId) throws SQLException {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT p.*, s.full_name AS student_name, t.full_name AS teacher_name " +
                "FROM projects p " +
                "LEFT JOIN students s ON p.student_id = s.student_id " +
                "LEFT JOIN teachers t ON p.teacher_id = t.teacher_id " +
                "WHERE p.student_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    projects.add(extractProjectFromResultSet(rs));
                }
            }
        }
        return projects;
    }

    public List<Project> findProjectsNopDungHan() throws SQLException {
        return findProjectsByCondition("ngay_nop IS NOT NULL AND ngay_nop <= ngay_ket_thuc");
    }

    public List<Project> findProjectsNopTreHan() throws SQLException {
        return findProjectsByCondition("ngay_nop IS NOT NULL AND ngay_nop > ngay_ket_thuc");
    }

    public List<Project> findProjectsChuaNop() throws SQLException {
        return findProjectsByCondition("ngay_nop IS NULL");
    }

    public List<Project> searchByTitleOrStudentId(String keyword) throws SQLException {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT p.*, s.full_name AS student_name, t.full_name AS teacher_name " +
                "FROM projects p " +
                "LEFT JOIN students s ON p.student_id = s.student_id " +
                "LEFT JOIN teachers t ON p.teacher_id = t.teacher_id " +
                "WHERE p.title LIKE ? OR p.student_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");
            try {
                pstmt.setInt(2, Integer.parseInt(keyword));
            } catch (NumberFormatException e) {
                pstmt.setInt(2, -1);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    projects.add(extractProjectFromResultSet(rs));
                }
            }
        }
        return projects;
    }

    private Project extractProjectFromResultSet(ResultSet rs) throws SQLException {
        Project project = new Project(
                rs.getInt("project_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getDate("ngay_bat_dau"),
                rs.getDate("ngay_ket_thuc"),
                rs.getDate("ngay_nop"),
                rs.getString("tep_bao_cao"),
                rs.getInt("student_id"),
                rs.getInt("teacher_id")
        );
        project.setStatus(rs.getString("status"));
        project.setStudentName(rs.getString("student_name"));
        project.setTeacherName(rs.getString("teacher_name"));
        project.setComment(rs.getString("comment")); // Thêm comment
        return project;
    }

    private List<Project> findProjectsByCondition(String condition) throws SQLException {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT p.*, s.full_name AS student_name, t.full_name AS teacher_name " +
                "FROM projects p " +
                "LEFT JOIN students s ON p.student_id = s.student_id " +
                "LEFT JOIN teachers t ON p.teacher_id = t.teacher_id " +
                "WHERE " + condition;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                projects.add(extractProjectFromResultSet(rs));
            }
        }
        return projects;
    }
}