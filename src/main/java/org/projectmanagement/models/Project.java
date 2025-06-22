package org.projectmanagement.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Project {
    private int projectId;
    private String title;
    private String description;
    private Date ngayBatDau;
    private Date ngayKetThuc;
    private Date ngayNop;
    private String status;
    private int studentId;
    private int teacherId;
    private String studentName;
    private String teacherName;
    private List<String> filePaths;
    private List<String> comments;

    public Project(int projectId, String title, String description, Date ngayBatDau, Date ngayKetThuc, Date ngayNop,
                   List<String> filePaths, List<String> comments, int studentId, int teacherId) {
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
        this.ngayNop = ngayNop;
        this.status = "CHO_DUYET";
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.filePaths = filePaths != null ? new ArrayList<>(filePaths) : new ArrayList<>();
        this.comments = comments != null ? new ArrayList<>(comments) : new ArrayList<>();
    }

    // Getters and Setters
    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Date getNgayBatDau() { return ngayBatDau; }
    public void setNgayBatDau(Date ngayBatDau) { this.ngayBatDau = ngayBatDau; }
    public Date getNgayKetThuc() { return ngayKetThuc; }
    public void setNgayKetThuc(Date ngayKetThuc) { this.ngayKetThuc = ngayKetThuc; }
    public Date getNgayNop() { return ngayNop; }
    public void setNgayNop(Date ngayNop) { this.ngayNop = ngayNop; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public int getTeacherId() { return teacherId; }
    public void setTeacherId(int teacherId) { this.teacherId = teacherId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public List<String> getFilePaths() { return new ArrayList<>(filePaths); }
    public void setFilePaths(List<String> filePaths) { this.filePaths = new ArrayList<>(filePaths); }
    public List<String> getComments() { return new ArrayList<>(comments); }
    public void setComments(List<String> comments) { this.comments = new ArrayList<>(comments); }

    public String getLatestFilePath() {
        return filePaths.isEmpty() ? null : filePaths.get(filePaths.size() - 1);
    }

    public String getLatestComment() {
        return comments.isEmpty() ? null : comments.get(comments.size() - 1);
    }
}