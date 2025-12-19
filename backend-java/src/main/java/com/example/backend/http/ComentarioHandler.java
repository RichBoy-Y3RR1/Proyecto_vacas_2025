package com.example.backend.http;

import com.example.backend.db.DBConnection;
import com.example.backend.models.Comentario;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ComentarioHandler implements HttpHandler {
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange ex) {
        try {
            ex.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            String base = "/backend/api/comentarios";
            if ("GET".equalsIgnoreCase(method) && path.equals(base)){
                try (Connection conn = DBConnection.getConnection()){
                    ResultSet rs = conn.createStatement().executeQuery("SELECT id, usuario_id, videojuego_id, texto, puntuacion, fecha, visible FROM Comentario");
                    List<Comentario> list = new ArrayList<>();
                    while (rs.next()){
                        Comentario c = new Comentario();
                        c.setId(rs.getInt("id")); c.setUsuario_id(rs.getInt("usuario_id")); c.setVideojuego_id(rs.getInt("videojuego_id"));
                        c.setTexto(rs.getString("texto")); c.setPuntuacion(rs.getInt("puntuacion"));
                        java.sql.Timestamp ts = rs.getTimestamp("fecha"); if (ts!=null) c.setFecha(ts.toInstant()); c.setVisible(rs.getBoolean("visible"));
                        list.add(c);
                    }
                    write(ex,200,gson.toJson(list)); return;
                }
            }

            if ("POST".equalsIgnoreCase(method) && path.equals(base)){
                Comentario body = gson.fromJson(new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8), Comentario.class);
                try (Connection conn = DBConnection.getConnection()){
                    PreparedStatement ps = conn.prepareStatement("INSERT INTO Comentario (usuario_id, videojuego_id, texto, puntuacion) VALUES (?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
                    ps.setInt(1, body.getUsuario_id()); ps.setInt(2, body.getVideojuego_id()); ps.setString(3, body.getTexto()); ps.setInt(4, body.getPuntuacion() != null ? body.getPuntuacion() : 0);
                    ps.executeUpdate(); ResultSet rs = ps.getGeneratedKeys(); Integer id = null; if (rs.next()) id = rs.getInt(1);
                    write(ex,200,gson.toJson(java.util.Collections.singletonMap("id", id))); return;
                }
            }

            write(ex,405,gson.toJson(java.util.Collections.singletonMap("error","method not allowed")));
        } catch (Exception e){ try { write(ex,500,gson.toJson(java.util.Collections.singletonMap("error", e.getMessage()))); } catch (Exception exx){} }
    }

    private void write(HttpExchange ex, int status, String body) throws Exception{
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream os = ex.getResponseBody()){ os.write(b); }
    }
}
