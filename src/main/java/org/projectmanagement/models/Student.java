package org.projectmanagement.models;

public class Student {
    private int studentId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String major;
    private String classCode;
    private int userId; // Thêm trường userId

    public Student(int studentId, String fullName, String email, String phoneNumber, String major, String classCode, int userId) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.major = major;
        this.classCode = classCode;
        this.userId = userId;
    }

    // Cập nhật getter và setter cho userId
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    // Giữ nguyên các getter/setter khác
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }
}