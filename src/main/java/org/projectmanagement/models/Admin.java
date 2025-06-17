package org.projectmanagement.models;

public class Admin extends User {
    public Admin(int userId, String username, String password, String email,
                 String fullName, String avatarPath, String phoneNumber) {
        super(userId, username, password, email, "admin", fullName, avatarPath, null, phoneNumber);
    }
}
