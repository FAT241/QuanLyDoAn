package org.projectmanagement.models;

import java.util.Date;
import java.util.List;

public class Project {
    private int projectId;
    private String title;
    private String description;
    private Date ngayBatDau;
    private Date ngayKetThuc;
    private Date ngayNop;
    private List<String> filePaths;
    private List<String> comments;
    private int studentId;
    private int teacherId;
    private String studentName;
    private String teacherName;
    private String status;
    private Double processScore;
    private Double defenseScore;
    private Double finalScore;
    private String grade;

    public Project(int projectId, String title, String description, Date ngayBatDau, Date ngayKetThuc, Date ngayNop,
                   List<String> filePaths, List<String> comments, int studentId, int teacherId) {
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
        this.ngayNop = ngayNop;
        this.filePaths = filePaths;
        this.comments = comments;
        this.studentId = studentId;
        this.teacherId = teacherId;
    }

    // Getters and Setters

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getNgayBatDau() {
        return ngayBatDau;
    }

    public void setNgayBatDau(Date ngayBatDau) {
        this.ngayBatDau = ngayBatDau;
    }

    public Date getNgayKetThuc() {
        return ngayKetThuc;
    }

    public void setNgayKetThuc(Date ngayKetThuc) {
        this.ngayKetThuc = ngayKetThuc;
    }

    public Date getNgayNop() {
        return ngayNop;
    }

    public void setNgayNop(Date ngayNop) {
        this.ngayNop = ngayNop;
    }

    public List<String> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
    }

    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getTeacherId() {
        return teacherId;  // Fixed: removed "get" and added "()"
    }

    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getProcessScore() {
        return processScore;
    }

    public void setProcessScore(Double processScore) {
        this.processScore = processScore;
    }

    public Double getDefenseScore() {
        return defenseScore;
    }

    public void setDefenseScore(Double defenseScore) {
        this.defenseScore = defenseScore;
    }

    public Double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Double finalScore) {
        this.finalScore = finalScore;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getLatestFilePath() {
        if (filePaths != null && !filePaths.isEmpty()) {
            return filePaths.get(filePaths.size() - 1);
        }
        return null;
    }

    public String getLatestComment() {
        if (comments != null && !comments.isEmpty()) {
            return comments.get(comments.size() - 1);
        }
        return null;
    }

}