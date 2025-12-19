package com.example.backend.dao;

import com.example.backend.db.DBConnection;
import com.example.backend.models.Gamer;
import com.example.backend.models.CompanyUser;
import com.example.backend.models.Admin;
import com.example.backend.models.AbstractUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class JdbcUserDAO implements UserDAO {
    @Override
    public Integer create(AbstractUser user) throws Exception {
        try (Connection conn = DBConnection.getConnection()){
            String sql = "INSERT INTO Usuario (correo, password, role, nickname, fecha_nacimiento, telefono, pais) VALUES (?,?,?,?,?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole());
            if (user instanceof Gamer){
                Gamer g = (Gamer) user;
                ps.setString(4, g.getNickname());
                ps.setDate(5, java.sql.Date.valueOf(g.getBirthDate()));
                ps.setString(6, null);
                ps.setString(7, g.getCountry());
            } else if (user instanceof CompanyUser){
                CompanyUser cu = (CompanyUser) user;
                ps.setString(4, cu.getName());
                ps.setDate(5, null);
                ps.setString(6, null);
                ps.setString(7, null);
            } else {
                ps.setString(4, null); ps.setDate(5,null); ps.setString(6,null); ps.setString(7,null);
            }
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
            return null;
        }
    }

    @Override
    public AbstractUser findById(Integer id) throws Exception {
        try (Connection conn = DBConnection.getConnection()){
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM Usuario WHERE id = ?");
            ps.setInt(1,id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                String role = rs.getString("role");
                if ("USUARIO".equals(role)){
                    Gamer g = new Gamer();
                    g.setId(rs.getInt("id"));
                    g.setEmail(rs.getString("correo"));
                    g.setPasswordHash(rs.getString("password"));
                    g.setNickname(rs.getString("nickname"));
                    java.sql.Date d = rs.getDate("fecha_nacimiento"); if (d!=null) g.setBirthDate(d.toLocalDate());
                    g.setRole(role);
                    return g;
                } else if ("EMPRESA".equals(role)){
                    CompanyUser cu = new CompanyUser();
                    cu.setId(rs.getInt("id")); cu.setEmail(rs.getString("correo")); cu.setPasswordHash(rs.getString("password")); cu.setName(rs.getString("nickname")); cu.setRole(role);
                    return cu;
                } else {
                    Admin a = new Admin(); a.setId(rs.getInt("id")); a.setEmail(rs.getString("correo")); a.setPasswordHash(rs.getString("password")); a.setRole(role);
                    return a;
                }
            }
            return null;
        }
    }

    @Override
    public AbstractUser findByEmail(String email) throws Exception {
        try (Connection conn = DBConnection.getConnection()){
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM Usuario WHERE correo = ?");
            ps.setString(1,email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return findById(rs.getInt("id"));
            return null;
        }
    }

    @Override
    public List<AbstractUser> listAll() throws Exception {
        List<AbstractUser> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()){
            ResultSet rs = conn.createStatement().executeQuery("SELECT id FROM Usuario");
            while (rs.next()) list.add(findById(rs.getInt("id")));
        }
        return list;
    }

    @Override
    public boolean update(AbstractUser user) throws Exception { return false; }

    @Override
    public boolean delete(Integer id) throws Exception { return false; }
}
