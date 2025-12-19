package com.example.backend.servlets;

import com.example.backend.db.DBConnection;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;

@WebServlet(name = "VideojuegoServlet", urlPatterns = {"/api/videojuegos/*"})
public class VideojuegoServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (var conn = DBConnection.getConnection()){
            var rs = conn.createStatement().executeQuery("SELECT v.id, v.nombre, v.descripcion, v.precio, v.estado, e.nombre AS empresa, v.edad_clasificacion FROM Videojuego v JOIN Empresa e ON v.empresa_id = e.id");
            java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>();
            while (rs.next()){
                java.util.Map<String,Object> m = new java.util.HashMap<>();
                m.put("id", rs.getInt("id")); m.put("nombre", rs.getString("nombre")); m.put("descripcion", rs.getString("descripcion"));
                m.put("precio", rs.getBigDecimal("precio")); m.put("estado", rs.getString("estado")); m.put("empresa", rs.getString("empresa")); m.put("edad_clasificacion", rs.getString("edad_clasificacion"));
                list.add(m);
            }
            writeJson(resp, list);
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (var conn = DBConnection.getConnection()){
            var body = gson.fromJson(req.getReader(), java.util.Map.class);
            String nombre = (String) body.get("nombre"); String descripcion = (String) body.getOrDefault("descripcion", null);
            Integer empresaId = ((Number)body.get("empresa_id")).intValue(); java.math.BigDecimal precio = new java.math.BigDecimal(String.valueOf(body.getOrDefault("precio","0")));
            String edad = (String) body.getOrDefault("edad_clasificacion", null);
            var ps = conn.prepareStatement("INSERT INTO Videojuego (nombre, descripcion, empresa_id, precio, estado, fecha_lanzamiento, edad_clasificacion) VALUES (?,?,?,?,?,NULL,?)", java.sql.PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1,nombre); ps.setString(2,descripcion); ps.setInt(3, empresaId); ps.setBigDecimal(4, precio); ps.setString(5, "PUBLICADO"); ps.setString(6, edad);
            ps.executeUpdate(); var rs = ps.getGeneratedKeys(); Integer id = null; if (rs.next()) id = rs.getInt(1);
            writeJson(resp, java.util.Map.of("id", id));
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        Integer id = parseId(req);
        if (id==null){ resp.setStatus(400); writeJson(resp, java.util.Map.of("error","id required")); return; }
        try (var conn = DBConnection.getConnection()){
            var body = gson.fromJson(req.getReader(), java.util.Map.class);
            var ps = conn.prepareStatement("UPDATE Videojuego SET nombre=?, descripcion=?, precio=?, edad_clasificacion=? WHERE id=?");
            ps.setString(1, (String)body.getOrDefault("nombre", null)); ps.setString(2, (String)body.getOrDefault("descripcion", null));
            ps.setBigDecimal(3, new java.math.BigDecimal(String.valueOf(body.getOrDefault("precio","0")))); ps.setString(4, (String)body.getOrDefault("edad_clasificacion", null)); ps.setInt(5, id);
            ps.executeUpdate(); writeJson(resp, java.util.Map.of("id", id));
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }

    
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        Integer id = parseId(req);
        if (id==null){ resp.setStatus(400); writeJson(resp, java.util.Map.of("error","id required")); return; }
        try (var conn = DBConnection.getConnection()){
            var body = gson.fromJson(req.getReader(), java.util.Map.class);
            Boolean forSale = (Boolean) body.getOrDefault("for_sale", Boolean.TRUE);
            var ps = conn.prepareStatement("UPDATE Videojuego SET estado = ? WHERE id = ?");
            ps.setString(1, forSale ? "PUBLICADO" : "SUSPENDIDO"); ps.setInt(2, id); ps.executeUpdate();
            writeJson(resp, java.util.Map.of("id", id, "for_sale", forSale));
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }
}

