package com.example.backend.http;

import com.example.backend.db.DBConnection;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class BannerHandler implements HttpHandler {
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange ex) {
        try (java.sql.Connection conn = DBConnection.getConnection()){
            ex.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if (path.endsWith("/") && path.length()>1) path = path.substring(0, path.length()-1);
            String base = "/backend/api/banner";

            if ("GET".equalsIgnoreCase(method) && path.equals(base)){
                var rs = conn.createStatement().executeQuery("SELECT id, url_imagen, fecha_inicio, fecha_fin FROM Banner");
                java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>();
                while (rs.next()){ var m = new java.util.HashMap<String,Object>(); m.put("id", rs.getInt("id")); m.put("url_imagen", rs.getString("url_imagen")); m.put("fecha_inicio", rs.getTimestamp("fecha_inicio")); m.put("fecha_fin", rs.getTimestamp("fecha_fin")); list.add(m); }
                write(ex,200,gson.toJson(list)); return;
            }

            if ("POST".equalsIgnoreCase(method) && path.equals(base)){
                var body = gson.fromJson(new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8), java.util.Map.class);
                String url = (String) body.get("url_imagen"); String inicio = (String) body.getOrDefault("fecha_inicio", null); String fin = (String) body.getOrDefault("fecha_fin", null);
                var ps = conn.prepareStatement("INSERT INTO Banner (url_imagen, fecha_inicio, fecha_fin) VALUES (?,?,?)", java.sql.PreparedStatement.RETURN_GENERATED_KEYS);
                ps.setString(1, url);
                if (inicio!=null) ps.setString(2,inicio); else ps.setNull(2, java.sql.Types.VARCHAR);
                if (fin!=null) ps.setString(3,fin); else ps.setNull(3, java.sql.Types.VARCHAR);
                ps.executeUpdate(); var rs = ps.getGeneratedKeys(); Integer id = null; if (rs.next()) id = rs.getInt(1);
                write(ex,200,gson.toJson(java.util.Map.of("id", id))); return;
            }

            write(ex,405,gson.toJson(java.util.Map.of("error","method not allowed")));
        } catch (Exception e){
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("table \"banner\" not found") || msg.contains("table 'banner' not found") || msg.contains("table banner not found")){
                try { write(ex,200,gson.toJson(java.util.List.of())); } catch(Exception exx){}
            } else {
                try { write(ex,500,gson.toJson(java.util.Map.of("error", e.getMessage()))); } catch(Exception exx){}
            }
        }
    }

    private void write(HttpExchange ex, int status, String body) throws Exception { byte[] b = body.getBytes(StandardCharsets.UTF_8); ex.sendResponseHeaders(status, b.length); try (OutputStream os = ex.getResponseBody()){ os.write(b); } }
}
