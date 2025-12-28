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
            // include 'estado' and omit empresa_id to match init_mysql.sql schema (created_at present)
            String sql = "INSERT INTO Usuario (correo, password, role, estado, nickname, fecha_nacimiento, telefono, pais) VALUES (?,?,?,?,?,?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole());
            // default new users to active status
            ps.setString(4, "ACTIVA");
            if (user instanceof Gamer){
                Gamer g = (Gamer) user;
                ps.setString(5, g.getNickname());
                if (g.getBirthDate() != null) ps.setDate(6, java.sql.Date.valueOf(g.getBirthDate())); else ps.setNull(6, java.sql.Types.DATE);
                ps.setString(7, null);
                ps.setString(8, g.getCountry());
            } else if (user instanceof CompanyUser){
                CompanyUser cu = (CompanyUser) user;
                ps.setString(5, cu.getName());
                ps.setNull(6, java.sql.Types.DATE);
                ps.setString(7, null);
                ps.setString(8, null);
            } else {
                ps.setString(5, null); ps.setNull(6, java.sql.Types.DATE); ps.setString(7,null); ps.setString(8,null);
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
                    // empresa_id column may not exist in schema; guard retrieve
                    try {
                        Object compObj = rs.getObject("empresa_id");
                        if (compObj != null) cu.setCompanyId(rs.getInt("empresa_id"));
                    } catch (Exception _e) { /* ignore if column missing */ }
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
            PreparedStatement ps = conn.prepareStatement("SELECT id FROM Usuario WHERE LOWER(correo) = ?");
            ps.setString(1, email == null ? null : email.toLowerCase().trim());
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
    public boolean update(AbstractUser user) throws Exception {
        try (Connection conn = DBConnection.getConnection()){
            PreparedStatement ps = conn.prepareStatement("UPDATE Usuario SET correo=?, password=?, role=?, nickname=?, fecha_nacimiento=?, telefono=?, pais=? WHERE id=?");
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole());
            String nickname = null; java.sql.Date birth = null; String telefono = null; String pais = null;
            if (user instanceof Gamer){
                Gamer g = (Gamer) user; nickname = g.getNickname(); if (g.getBirthDate()!=null) birth = java.sql.Date.valueOf(g.getBirthDate()); pais = g.getCountry();
            } else if (user instanceof CompanyUser){
                CompanyUser cu = (CompanyUser) user; nickname = cu.getName();
            }
            ps.setString(4, nickname);
            if (birth != null) ps.setDate(5, birth); else ps.setNull(5, java.sql.Types.DATE);
            ps.setString(6, telefono);
            ps.setString(7, pais);
            ps.setInt(8, user.getId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(Integer id) throws Exception {
        try (Connection conn = DBConnection.getConnection()){
            PreparedStatement ps = conn.prepareStatement("DELETE FROM Usuario WHERE id = ?");
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
