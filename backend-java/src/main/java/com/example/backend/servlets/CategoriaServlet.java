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

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (Connection conn = DBConnection.getConnection()){
            // allow path /api/categorias/{id} or JSON with id
            Integer id = null;
            String path = req.getPathInfo();
            if (path != null && path.length() > 1){ try { id = Integer.valueOf(path.substring(1)); } catch(Exception ex){} }
            var body = gson.fromJson(req.getReader(), java.util.Map.class);
            if (id == null) { Object o = body.get("id"); if (o instanceof Number) id = ((Number)o).intValue(); else if (o instanceof String) try { id = Integer.valueOf((String)o); } catch(Exception ex){} }
            if (id == null) { resp.setStatus(400); writeJson(resp, java.util.Map.of("error","missing id")); return; }
            String nombre = (String) body.get("nombre"); String descripcion = (String) body.getOrDefault("descripcion", null);
            var ps = conn.prepareStatement("UPDATE Categoria SET nombre = ?, descripcion = ? WHERE id_categoria = ?");
            ps.setString(1, nombre); ps.setString(2, descripcion); ps.setInt(3, id);
            int updated = ps.executeUpdate();
            writeJson(resp, java.util.Map.of("updated", updated));
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (Connection conn = DBConnection.getConnection()){
            Integer id = null;
            String path = req.getPathInfo();
            if (path != null && path.length() > 1){ try { id = Integer.valueOf(path.substring(1)); } catch(Exception ex){} }
            if (id == null){ String pid = req.getParameter("id"); if (pid != null) try { id = Integer.valueOf(pid); } catch(Exception ex){} }
            if (id == null) { resp.setStatus(400); writeJson(resp, java.util.Map.of("error","missing id")); return; }
            var ps = conn.prepareStatement("DELETE FROM Categoria WHERE id_categoria = ?"); ps.setInt(1, id);
            int deleted = ps.executeUpdate(); writeJson(resp, java.util.Map.of("deleted", deleted));
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }
}
