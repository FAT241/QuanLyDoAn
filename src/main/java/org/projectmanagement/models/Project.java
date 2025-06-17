package org.projectmanagement.models;

import java.util.Date;

public class Project {
    private int projectId;
    private String title;
    private String description;
    private Date ngayBatDau;
    private Date ngayKetThuc;
    private Date ngayNop;
    private String tepBaoCao;
    private int studentId;
    private int teacherId;
    private String status;
    private String studentName; // New field for student full name
    private String teacherName; // New field for teacher full name

    // Constructor
    public Project(int projectId, String title, String description, Date ngayBatDau, Date ngayKetThuc,
                   Date ngayNop, String tepBaoCao, int studentId, int teacherId) {
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
        this.ngayNop = ngayNop;
        this.tepBaoCao = tepBaoCao;
        this.studentId = studentId;
        this.teacherId = teacherId;
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
    public String getTepBaoCao() { return tepBaoCao; }
    public void setTepBaoCao(String tepBaoCao) { this.tepBaoCao = tepBaoCao; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public int getTeacherId() { return teacherId; }
    public void setTeacherId(int teacherId) { this.teacherId = teacherId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
}