package com.example.backend.models;

public class Admin extends AbstractUser {
    private String fullName;

    public Admin(){ this.role = "ADMIN"; }

    public Admin(Integer id, String email, String passwordHash, String fullName){
        super(id,email,passwordHash,"ADMIN");
        this.fullName = fullName;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    @Override
    public Object publicProfile() {
        return new java.util.HashMap<String,Object>(){{ put("id", id); put("email", email); put("role", role); put("fullName", fullName);} };
    }
}
