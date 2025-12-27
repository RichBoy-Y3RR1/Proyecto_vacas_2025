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
        } catch (Exception e){
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.toLowerCase().contains("table \"banner\" not found") || msg.toLowerCase().contains("table 'banner' not found") || msg.toLowerCase().contains("table banner not found")){
                writeJson(resp, java.util.List.of());
            } else {
                resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage()));
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (Connection conn = DBConnection.getConnection()){
            // Only ADMIN may create banners
            Integer tokenUser = (Integer) req.getAttribute("userId");
            String tokenRole = (String) req.getAttribute("role");
            if (tokenUser == null) { writeError(resp, 401, "login_required", "Authentication required"); return; }
            if (tokenRole == null || !tokenRole.equalsIgnoreCase("ADMIN")) { writeError(resp, 403, "forbidden", "Only admin can create banners"); return; }
            var body = gson.fromJson(req.getReader(), java.util.Map.class);
            String url = (String) body.get("url_imagen"); String inicio = (String) body.getOrDefault("fecha_inicio", null); String fin = (String) body.getOrDefault("fecha_fin", null);
            var ps = conn.prepareStatement("INSERT INTO Banner (url_imagen, fecha_inicio, fecha_fin) VALUES (?,?,?)", java.sql.PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, url);
            if (inicio!=null) ps.setString(2,inicio); else ps.setNull(2, java.sql.Types.VARCHAR);
            if (fin!=null) ps.setString(3,fin); else ps.setNull(3, java.sql.Types.VARCHAR);
            ps.executeUpdate(); var rs = ps.getGeneratedKeys(); Integer id = null; if (rs.next()) id = rs.getInt(1);
            writeJson(resp, java.util.Map.of("id", id));
        } catch (Exception e){ writeError(resp, 500, "internal_error", e.getMessage()); }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (Connection conn = DBConnection.getConnection()){
            // Only ADMIN may update banners
            Integer tokenUser = (Integer) req.getAttribute("userId");
            String tokenRole = (String) req.getAttribute("role");
            if (tokenUser == null) { writeError(resp, 401, "login_required", "Authentication required"); return; }
            if (tokenRole == null || !tokenRole.equalsIgnoreCase("ADMIN")) { writeError(resp, 403, "forbidden", "Only admin can update banners"); return; }
            Integer id = null;
            String path = req.getPathInfo();
            if (path != null && path.length() > 1){ try { id = Integer.valueOf(path.substring(1)); } catch(Exception ex){} }
            var body = gson.fromJson(req.getReader(), java.util.Map.class);
            if (id == null) { Object o = body.get("id"); if (o instanceof Number) id = ((Number)o).intValue(); else if (o instanceof String) try { id = Integer.valueOf((String)o); } catch(Exception ex){} }
            if (id == null) { resp.setStatus(400); writeJson(resp, java.util.Map.of("error","missing id")); return; }
            String url = (String) body.get("url_imagen"); String inicio = (String) body.getOrDefault("fecha_inicio", null); String fin = (String) body.getOrDefault("fecha_fin", null);
            var ps = conn.prepareStatement("UPDATE Banner SET url_imagen = ?, fecha_inicio = ?, fecha_fin = ? WHERE id = ?");
            ps.setString(1, url);
            if (inicio!=null) ps.setString(2,inicio); else ps.setNull(2, java.sql.Types.VARCHAR);
            if (fin!=null) ps.setString(3,fin); else ps.setNull(3, java.sql.Types.VARCHAR);
            ps.setInt(4, id);
            int updated = ps.executeUpdate(); writeJson(resp, java.util.Map.of("updated", updated));
        } catch (Exception e){ writeError(resp, 500, "internal_error", e.getMessage()); }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (Connection conn = DBConnection.getConnection()){
            // Only ADMIN may delete banners
            Integer tokenUser = (Integer) req.getAttribute("userId");
            String tokenRole = (String) req.getAttribute("role");
            if (tokenUser == null) { writeError(resp, 401, "login_required", "Authentication required"); return; }
            if (tokenRole == null || !tokenRole.equalsIgnoreCase("ADMIN")) { writeError(resp, 403, "forbidden", "Only admin can delete banners"); return; }
            Integer id = null;
            String path = req.getPathInfo();
            if (path != null && path.length() > 1){ try { id = Integer.valueOf(path.substring(1)); } catch(Exception ex){} }
            if (id == null){ String pid = req.getParameter("id"); if (pid != null) try { id = Integer.valueOf(pid); } catch(Exception ex){} }
            if (id == null) { resp.setStatus(400); writeJson(resp, java.util.Map.of("error","missing id")); return; }
            var ps = conn.prepareStatement("DELETE FROM Banner WHERE id = ?"); ps.setInt(1, id);
            int deleted = ps.executeUpdate(); writeJson(resp, java.util.Map.of("deleted", deleted));
        } catch (Exception e){ writeError(resp, 500, "internal_error", e.getMessage()); }
    }
}
