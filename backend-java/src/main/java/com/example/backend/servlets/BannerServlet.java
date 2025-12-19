package com.example.backend.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.sql.Connection;
import com.example.backend.db.DBConnection;

@WebServlet(name = "BannerServlet", urlPatterns = {"/api/banner/*"})
public class BannerServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (Connection conn = DBConnection.getConnection()){
            var rs = conn.createStatement().executeQuery("SELECT id, url_imagen, fecha_inicio, fecha_fin FROM Banner");
            java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>();
            while (rs.next()){
                java.util.Map<String,Object> m = new java.util.HashMap<>();
                m.put("id", rs.getInt("id")); m.put("url_imagen", rs.getString("url_imagen")); m.put("fecha_inicio", rs.getTimestamp("fecha_inicio")); m.put("fecha_fin", rs.getTimestamp("fecha_fin"));
                list.add(m);
            }
            writeJson(resp, list);
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (Connection conn = DBConnection.getConnection()){
            var body = gson.fromJson(req.getReader(), java.util.Map.class);
            String url = (String) body.get("url_imagen"); String inicio = (String) body.getOrDefault("fecha_inicio", null); String fin = (String) body.getOrDefault("fecha_fin", null);
            var ps = conn.prepareStatement("INSERT INTO Banner (url_imagen, fecha_inicio, fecha_fin) VALUES (?,?,?)", java.sql.PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, url);
            if (inicio!=null) ps.setString(2,inicio); else ps.setNull(2, java.sql.Types.VARCHAR);
            if (fin!=null) ps.setString(3,fin); else ps.setNull(3, java.sql.Types.VARCHAR);
            ps.executeUpdate(); var rs = ps.getGeneratedKeys(); Integer id = null; if (rs.next()) id = rs.getInt(1);
            writeJson(resp, java.util.Map.of("id", id));
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }
}
