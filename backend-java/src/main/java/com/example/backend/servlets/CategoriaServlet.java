package com.example.backend.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.sql.Connection;
import com.example.backend.db.DBConnection;

@WebServlet(name = "CategoriaServlet", urlPatterns = {"/api/categorias/*"})
public class CategoriaServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (Connection conn = DBConnection.getConnection()){
            var rs = conn.createStatement().executeQuery("SELECT id_categoria, nombre, descripcion FROM Categoria");
            java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>();
            while (rs.next()){
                java.util.Map<String,Object> m = new java.util.HashMap<>();
                m.put("id", rs.getInt("id_categoria")); m.put("nombre", rs.getString("nombre")); m.put("descripcion", rs.getString("descripcion"));
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
            String nombre = (String) body.get("nombre"); String descripcion = (String) body.getOrDefault("descripcion", null);
            var ps = conn.prepareStatement("INSERT INTO Categoria (nombre, descripcion) VALUES (?,?)", java.sql.PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1,nombre); ps.setString(2,descripcion); ps.executeUpdate(); var rs = ps.getGeneratedKeys(); Integer id = null; if (rs.next()) id = rs.getInt(1);
            writeJson(resp, java.util.Map.of("id", id));
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }
}
