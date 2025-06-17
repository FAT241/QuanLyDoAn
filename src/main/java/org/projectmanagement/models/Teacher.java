package org.projectmanagement.models;

public class Teacher {
    private int teacherId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String position;

    public Teacher(int teacherId, String fullName, String email, String phoneNumber, String position) {
        this.teacherId = teacherId;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.position = position;
    }
    public int getTeacherId() {
        return teacherId;
    }
    public String getFullName() {
        return fullName;
    }
    public String getEmail() {
        return email;
    }
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public String getPosition() {
        return position;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public void setPosition(String position) {
        this.position = position;
    }
}
