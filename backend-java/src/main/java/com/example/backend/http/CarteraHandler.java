package com.example.backend.http;

import com.example.backend.db.DBConnection;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CarteraHandler implements HttpHandler {
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange ex) {
        try (java.sql.Connection conn = DBConnection.getConnection()){
            ex.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if (path.endsWith("/") && path.length()>1) path = path.substring(0, path.length()-1);
            String base = "/backend/api/cartera";

            if ("GET".equalsIgnoreCase(method) && path.equals(base)){
                var rs = conn.createStatement().executeQuery("SELECT id_cartera, usuario_id, saldo FROM Cartera");
                java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>();
                while (rs.next()){ var m = new java.util.HashMap<String,Object>(); m.put("id", rs.getInt("id_cartera")); m.put("usuario_id", rs.getInt("usuario_id")); m.put("saldo", rs.getBigDecimal("saldo")); list.add(m); }
                write(ex,200,gson.toJson(list)); return;
            }

            if ("POST".equalsIgnoreCase(method) && path.equals(base)){
                var body = gson.fromJson(new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8), java.util.Map.class);
                Integer usuarioId = body.get("usuario_id") instanceof Number ? ((Number)body.get("usuario_id")).intValue() : null; java.math.BigDecimal saldo = body.get("saldo") instanceof Number ? new java.math.BigDecimal(((Number)body.get("saldo")).toString()) : new java.math.BigDecimal("0");
                var ps = conn.prepareStatement("INSERT INTO Cartera (usuario_id, saldo) VALUES (?,?)", java.sql.PreparedStatement.RETURN_GENERATED_KEYS);
                ps.setInt(1, usuarioId); ps.setBigDecimal(2, saldo); ps.executeUpdate(); var rs = ps.getGeneratedKeys(); Integer id = null; if (rs.next()) id = rs.getInt(1);
                write(ex,200,gson.toJson(java.util.Map.of("id", id))); return;
            }

            write(ex,405,gson.toJson(java.util.Map.of("error","method not allowed")));
        } catch (Exception e){ try { write(ex,500,gson.toJson(java.util.Map.of("error", e.getMessage()))); } catch(Exception exx){} }
    }

    private void write(HttpExchange ex, int status, String body) throws Exception { byte[] b = body.getBytes(StandardCharsets.UTF_8); ex.sendResponseHeaders(status, b.length); try (OutputStream os = ex.getResponseBody()){ os.write(b); } }
}
