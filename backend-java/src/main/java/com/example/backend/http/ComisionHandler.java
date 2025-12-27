package com.example.backend.http;

import com.example.backend.db.DBConnection;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ComisionHandler implements HttpHandler {
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange ex) {
        try (java.sql.Connection conn = DBConnection.getConnection()){
            ex.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if (path.endsWith("/") && path.length()>1) path = path.substring(0, path.length()-1);
            String base = "/backend/api/comision";

            if ("GET".equalsIgnoreCase(method) && path.equals(base)){
                var rs = conn.createStatement().executeQuery("SELECT id, porcentaje FROM Comision");
                java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>();
                while (rs.next()){ var m = new java.util.HashMap<String,Object>(); m.put("id", rs.getInt("id")); m.put("porcentaje", rs.getBigDecimal("porcentaje")); list.add(m); }
                write(ex,200,gson.toJson(list)); return;
            }

            if ("POST".equalsIgnoreCase(method) && path.equals(base)){
                var body = gson.fromJson(new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8), java.util.Map.class);
                java.math.BigDecimal pct = body.get("porcentaje") instanceof Number ? new java.math.BigDecimal(((Number)body.get("porcentaje")).toString()) : new java.math.BigDecimal("0");
                var ps = conn.prepareStatement("INSERT INTO Comision (porcentaje) VALUES (?)", java.sql.PreparedStatement.RETURN_GENERATED_KEYS);
                ps.setBigDecimal(1, pct); ps.executeUpdate(); var rs = ps.getGeneratedKeys(); Integer id = null; if (rs.next()) id = rs.getInt(1);
                write(ex,200,gson.toJson(java.util.Map.of("id", id))); return;
            }

            write(ex,405,gson.toJson(java.util.Map.of("error","method not allowed")));
        } catch (Exception e){ try { write(ex,500,gson.toJson(java.util.Map.of("error", e.getMessage()))); } catch(Exception exx){} }
    }

    private void write(HttpExchange ex, int status, String body) throws Exception { byte[] b = body.getBytes(StandardCharsets.UTF_8); ex.sendResponseHeaders(status, b.length); try (OutputStream os = ex.getResponseBody()){ os.write(b); } }
}
