package com.example.backend.http;

import com.example.backend.db.DBConnection;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

public class CategoriaHandler implements HttpHandler {
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange ex) {
        try (Connection conn = DBConnection.getConnection()){
            ex.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if (path.endsWith("/") && path.length()>1) path = path.substring(0, path.length()-1);
            String base = "/backend/api/categorias";

            if ("GET".equalsIgnoreCase(method) && path.equals(base)){
                var rs = conn.createStatement().executeQuery("SELECT id_categoria, nombre, descripcion FROM Categoria");
                java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>();
                while (rs.next()){ java.util.Map<String,Object> m = new java.util.HashMap<>(); m.put("id", rs.getInt("id_categoria")); m.put("nombre", rs.getString("nombre")); m.put("descripcion", rs.getString("descripcion")); list.add(m); }
                write(ex,200,gson.toJson(list)); return;
            }

            if ("POST".equalsIgnoreCase(method) && path.equals(base)){
                var body = gson.fromJson(new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8), java.util.Map.class);
                String nombre = (String) body.get("nombre"); String descripcion = (String) body.getOrDefault("descripcion", null);
                var ps = conn.prepareStatement("INSERT INTO Categoria (nombre, descripcion) VALUES (?,?)", java.sql.PreparedStatement.RETURN_GENERATED_KEYS);
                ps.setString(1,nombre); ps.setString(2,descripcion); ps.executeUpdate(); var rs = ps.getGeneratedKeys(); Integer id = null; if (rs.next()) id = rs.getInt(1);
                write(ex,200,gson.toJson(java.util.Map.of("id", id))); return;
            }

            if ("PUT".equalsIgnoreCase(method) && path.startsWith(base + "/")){
                String idStr = path.substring((base + "/").length()).split("/")[0]; Integer id = Integer.parseInt(idStr);
                var body = gson.fromJson(new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8), java.util.Map.class);
                String nombre = (String) body.get("nombre"); String descripcion = (String) body.getOrDefault("descripcion", null);
                var ps = conn.prepareStatement("UPDATE Categoria SET nombre = ?, descripcion = ? WHERE id_categoria = ?"); ps.setString(1,nombre); ps.setString(2,descripcion); ps.setInt(3,id); int updated = ps.executeUpdate(); write(ex,200,gson.toJson(java.util.Map.of("updated", updated))); return;
            }

            if ("DELETE".equalsIgnoreCase(method) && path.startsWith(base + "/")){
                String idStr = path.substring((base + "/").length()).split("/")[0]; Integer id = Integer.parseInt(idStr);
                var ps = conn.prepareStatement("DELETE FROM Categoria WHERE id_categoria = ?"); ps.setInt(1,id); int deleted = ps.executeUpdate(); write(ex,200,gson.toJson(java.util.Map.of("deleted", deleted))); return;
            }

            write(ex,405,gson.toJson(java.util.Map.of("error","method not allowed")));
        } catch (Exception e){ try { write(ex,500,gson.toJson(java.util.Map.of("error", e.getMessage()))); } catch(Exception exx){} }
    }

    private void write(HttpExchange ex, int status, String body) throws Exception { byte[] b = body.getBytes(StandardCharsets.UTF_8); ex.sendResponseHeaders(status, b.length); try (OutputStream os = ex.getResponseBody()){ os.write(b); } }
}
