package com.example.backend.models;

public class CompanyUser extends AbstractUser {
    private Integer companyId;
    private String name;

    public CompanyUser(){ this.role = "EMPRESA"; }

    public CompanyUser(Integer id, String email, String passwordHash, Integer companyId, String name){
        super(id,email,passwordHash,"EMPRESA");
        this.companyId = companyId; this.name = name;
    }

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public Object publicProfile() {
        return new java.util.HashMap<String,Object>(){{ put("id", id); put("name", name); put("companyId", companyId);} };
    }
}
