package com.example.backend.models;

import java.time.LocalDate;

public class Gamer extends AbstractUser {
    private String nickname;
    private LocalDate birthDate;
    private String country;

    public Gamer(){ this.role = "USUARIO"; }

    public Gamer(Integer id, String email, String passwordHash, String nickname, LocalDate birthDate, String country){
        super(id,email,passwordHash,"USUARIO");
        this.nickname = nickname; this.birthDate = birthDate; this.country = country;
    }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    @Override
    public Object publicProfile() {
        return new java.util.HashMap<String,Object>(){{ put("id", id); put("nickname", nickname); put("country", country);} };
    }
}
