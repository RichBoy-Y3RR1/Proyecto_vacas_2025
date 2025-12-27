package com.example.backend.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.sql.Connection;
import com.example.backend.db.DBConnection;

@WebServlet(name = "ComentarioServlet", urlPatterns = {"/api/comentarios/*"})
public class ComentarioServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (Connection conn = DBConnection.getConnection()){
            var rs = conn.createStatement().executeQuery("SELECT id, usuario_id, videojuego_id, texto, puntuacion, fecha, visible FROM Comentario");
            java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>();
            while (rs.next()){
                java.util.Map<String,Object> m = new java.util.HashMap<>();
                m.put("id", rs.getInt("id")); m.put("usuario_id", rs.getInt("usuario_id")); m.put("videojuego_id", rs.getInt("videojuego_id"));
                m.put("texto", rs.getString("texto")); m.put("puntuacion", rs.getInt("puntuacion")); m.put("fecha", rs.getTimestamp("fecha")); m.put("visible", rs.getBoolean("visible"));
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
            Integer usuarioId = ((Number)body.get("usuario_id")).intValue(); Integer videojuegoId = ((Number)body.get("videojuego_id")).intValue();
            // Verify that the user purchased the game before allowing a comment (business rule)
            var check = conn.prepareStatement("SELECT 1 FROM Compra WHERE usuario_id = ? AND videojuego_id = ? LIMIT 1");
            check.setInt(1, usuarioId); check.setInt(2, videojuegoId);
            var rcheck = check.executeQuery();
            if (!rcheck.next()) { resp.setStatus(403); writeError(resp, 403, "purchase_required", "User must have purchased the game to comment"); return; }
            // ensure token user matches usuarioId (or admin)
            Integer tokenUser = (Integer) req.getAttribute("userId");
            String tokenRole = (String) req.getAttribute("role");
            if (tokenUser == null) { writeError(resp, 401, "login_required", "Authentication required"); return; }
            if (!tokenUser.equals(usuarioId) && (tokenRole == null || !tokenRole.equalsIgnoreCase("ADMIN"))){ writeError(resp, 403, "forbidden", "Not allowed to create comment for this user"); return; }

            String texto = (String) body.getOrDefault("texto", null); Integer puntuacion = ((Number)body.getOrDefault("puntuacion", 0)).intValue();
            var ps = conn.prepareStatement("INSERT INTO Comentario (usuario_id, videojuego_id, texto, puntuacion) VALUES (?,?,?,?)", java.sql.PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setInt(1, usuarioId); ps.setInt(2, videojuegoId); ps.setString(3, texto); ps.setInt(4, puntuacion);
            ps.executeUpdate(); var rs = ps.getGeneratedKeys(); Integer id = null; if (rs.next()) id = rs.getInt(1);
            writeJson(resp, java.util.Map.of("id", id));
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }
}
