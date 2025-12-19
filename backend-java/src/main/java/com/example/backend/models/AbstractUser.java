package com.example.backend.models;

public abstract class AbstractUser {
    protected Integer id;
    protected String email;
    protected String passwordHash;
    protected String role;

    public AbstractUser(){}

    public AbstractUser(Integer id, String email, String passwordHash, String role){
        this.id = id; this.email = email; this.passwordHash = passwordHash; this.role = role;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

   
    public abstract Object publicProfile();
}
