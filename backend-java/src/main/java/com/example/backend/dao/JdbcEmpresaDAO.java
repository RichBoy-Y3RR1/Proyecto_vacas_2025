package com.example.backend.dao;

import com.example.backend.db.DBConnection;
import com.example.backend.models.CompanyUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class JdbcEmpresaDAO implements EmpresaDAO {
    @Override
    public Integer createCompany(String name, String email) throws Exception {
        try (Connection conn = DBConnection.getConnection()){
            PreparedStatement ps = conn.prepareStatement("INSERT INTO Empresa (nombre, correo, telefono, estado) VALUES (?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, name); ps.setString(2, email); ps.setString(3, null); ps.setString(4, "ACTIVA");
            ps.executeUpdate(); ResultSet rs = ps.getGeneratedKeys(); if (rs.next()) return rs.getInt(1); return null;
        }
    }

    @Override
    public CompanyUser findById(Integer id) throws Exception {
        try (Connection conn = DBConnection.getConnection()){
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM Empresa WHERE id = ?"); ps.setInt(1,id); ResultSet rs = ps.executeQuery();
            if (rs.next()){
                CompanyUser cu = new CompanyUser(); cu.setCompanyId(rs.getInt("id")); cu.setName(rs.getString("nombre")); cu.setEmail(rs.getString("correo")); return cu;
            }
            return null;
        }
    }

    @Override
    public List<CompanyUser> listAll() throws Exception {
        List<CompanyUser> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()){
            ResultSet rs = conn.createStatement().executeQuery("SELECT id FROM Empresa");
            while (rs.next()) list.add(findById(rs.getInt("id")));
        }
        return list;
    }
}
