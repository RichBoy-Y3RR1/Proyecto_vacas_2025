package com.example.backend.dao;

import com.example.backend.db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import com.example.backend.models.Videojuego;

public class JdbcVideojuegoDAO implements VideojuegoDAO {
    @Override
    public List<Videojuego> listAll() throws Exception {
        List<Videojuego> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()){
            ResultSet rs = conn.createStatement().executeQuery("SELECT v.id, v.nombre, v.descripcion, v.precio, v.estado, e.nombre AS empresa, v.edad_clasificacion, v.empresa_id FROM Videojuego v JOIN Empresa e ON v.empresa_id = e.id");
            while (rs.next()){
                Videojuego v = new Videojuego();
                v.setId(rs.getInt("id"));
                v.setNombre(rs.getString("nombre"));
                v.setDescripcion(rs.getString("descripcion"));
                v.setPrecio(rs.getBigDecimal("precio"));
                v.setEstado(rs.getString("estado"));
                v.setEmpresa(rs.getString("empresa"));
                v.setEmpresaId(rs.getInt("empresa_id"));
                v.setEdad_clasificacion(rs.getString("edad_clasificacion"));
                list.add(v);
            }
        }
        return list;
    }

    @Override
    public Integer create(com.example.backend.models.Videojuego data) throws Exception {
        try (Connection conn = DBConnection.getConnection()){
            PreparedStatement ps = conn.prepareStatement("INSERT INTO Videojuego (nombre, descripcion, empresa_id, precio, estado, fecha_lanzamiento, edad_clasificacion) VALUES (?,?,?,?,?,NULL,?)", PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, data.getNombre());
            ps.setString(2, data.getDescripcion());
            // use empresaId from payload when provided, otherwise default to 1
            ps.setInt(3, data.getEmpresaId() != null ? data.getEmpresaId() : 1);
            ps.setBigDecimal(4, data.getPrecio() != null ? data.getPrecio() : new java.math.BigDecimal("0"));
            ps.setString(5, "PUBLICADO");
            ps.setString(6, data.getEdad_clasificacion());
            ps.executeUpdate(); ResultSet rs = ps.getGeneratedKeys(); if (rs.next()) return rs.getInt(1); return null;
        }
    }

    @Override
    public boolean update(Integer id, com.example.backend.models.Videojuego data) throws Exception {
        try (Connection conn = DBConnection.getConnection()){
            PreparedStatement ps = conn.prepareStatement("UPDATE Videojuego SET nombre=?, descripcion=?, precio=?, edad_clasificacion=? WHERE id=?");
            ps.setString(1, data.getNombre());
            ps.setString(2, data.getDescripcion());
            ps.setBigDecimal(3, data.getPrecio() != null ? data.getPrecio() : new java.math.BigDecimal("0"));
            ps.setString(4, data.getEdad_clasificacion());
            ps.setInt(5, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean setForSale(Integer id, boolean forSale) throws Exception {
        try (Connection conn = DBConnection.getConnection()){
            PreparedStatement ps = conn.prepareStatement("UPDATE Videojuego SET estado = ? WHERE id = ?");
            ps.setString(1, forSale ? "PUBLICADO" : "SUSPENDIDO"); ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public Videojuego findById(Integer id) throws Exception {
        try (Connection conn = DBConnection.getConnection()){
            PreparedStatement ps = conn.prepareStatement("SELECT v.id, v.nombre, v.descripcion, v.precio, v.estado, e.nombre AS empresa, v.edad_clasificacion, v.empresa_id FROM Videojuego v JOIN Empresa e ON v.empresa_id = e.id WHERE v.id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                Videojuego v = new Videojuego();
                v.setId(rs.getInt("id"));
                v.setNombre(rs.getString("nombre"));
                v.setDescripcion(rs.getString("descripcion"));
                v.setPrecio(rs.getBigDecimal("precio"));
                v.setEstado(rs.getString("estado"));
                v.setEmpresa(rs.getString("empresa"));
                v.setEmpresaId(rs.getInt("empresa_id"));
                v.setEdad_clasificacion(rs.getString("edad_clasificacion"));
                return v;
            }
            return null;
        }
    }

    @Override
    public boolean delete(Integer id) throws Exception {
        try (Connection conn = DBConnection.getConnection()){
            PreparedStatement ps = conn.prepareStatement("DELETE FROM Videojuego WHERE id = ?");
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
