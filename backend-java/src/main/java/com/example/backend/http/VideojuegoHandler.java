package com.example.backend.http;

import com.example.backend.dao.JdbcVideojuegoDAO;
import com.example.backend.dao.VideojuegoDAO;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.example.backend.models.Videojuego;
import com.example.backend.db.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.math.BigDecimal;

public class VideojuegoHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final VideojuegoDAO dao = new JdbcVideojuegoDAO();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/") && path.length()>1) path = path.substring(0, path.length()-1);
            // path may end with /api/videojuegos or /api/videojuegos/{id}
            String suffix = "/api/videojuegos";
            if ("GET".equalsIgnoreCase(method) && path.endsWith(suffix)) {
                // support optional query ?published=true to return only PUBLICADO games
                String query = exchange.getRequestURI().getQuery();
                boolean onlyPublished = false;
                if (query != null) {
                    for (String p : query.split("&")){
                        String[] kv = p.split("="); if (kv.length>0 && "published".equals(kv[0]) && kv.length>1 && "true".equalsIgnoreCase(kv[1])) onlyPublished = true;
                    }
                }
                List<Videojuego> list = dao.listAll();
                if (onlyPublished){ java.util.List<Videojuego> filtered = new java.util.ArrayList<>(); for(Videojuego v: list){ if ("PUBLICADO".equalsIgnoreCase(v.getEstado())) filtered.add(v); } write(exchange,200,gson.toJson(filtered)); return; }
                write(exchange, 200, gson.toJson(list));
                return;
            }

            if ("POST".equalsIgnoreCase(method) && path.endsWith(suffix)) {
                Videojuego body = gson.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), Videojuego.class);
                Integer id = dao.create(body);
                write(exchange, 200, gson.toJson(java.util.Collections.singletonMap("id", id)));
                return;
            }
            if (("PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) && path.contains(suffix + "/")) {
                String idStr = path.substring(path.lastIndexOf('/') + 1).split("/")[0];
                Integer id = Integer.parseInt(idStr);
                if ("PUT".equalsIgnoreCase(method)) {
                    Videojuego body = gson.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), Videojuego.class);
                    boolean ok = dao.update(id, body);
                    write(exchange, ok ? 200 : 404, gson.toJson(java.util.Collections.singletonMap("id", id)));
                    return;
                } else { // PATCH: toggle for_sale if provided
                    java.util.Map body = gson.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), java.util.Map.class);
                    Boolean forSale = (Boolean) body.getOrDefault("for_sale", Boolean.TRUE);
                    boolean ok = dao.setForSale(id, forSale);
                    write(exchange, ok ? 200 : 404, gson.toJson(java.util.Map.of("id", id, "for_sale", forSale)));
                    return;
                }
            }

            // admin actions that don't target a single resource by conventional methods
            // POST /api/videojuegos/{id}/publish  -> publish game
            // POST /api/videojuegos/{id}/suspend  -> suspend game
            if ("POST".equalsIgnoreCase(method)){
                // support approve-all via several path shapes
                if (path.endsWith(suffix + "/approve-all") || path.endsWith(suffix + "/approve-all/") || path.contains(suffix + "/approve-all")){
                    boolean ok = dao.approveAllPending();
                    write(exchange, ok?200:500, gson.toJson(java.util.Map.of("approved", ok)));
                    return;
                }

                // actions on a specific videogame: use lastIndexOf to avoid matching earlier occurrences
                int idx = path.lastIndexOf(suffix);
                if (idx >= 0 && path.length() > idx + suffix.length()){
                    String tail = path.substring(idx + suffix.length() + 1); // {id}/action or {id}/action/...
                    String[] seg = tail.split("/");
                    if (seg.length >= 2){
                        try {
                            Integer id = Integer.parseInt(seg[0]);
                            String action = seg[1];
                            if ("publish".equalsIgnoreCase(action)){
                                boolean ok = dao.setForSale(id, true);
                                write(exchange, ok?200:404, gson.toJson(java.util.Map.of("id", id, "published", ok)));
                                return;
                            } else if ("suspend".equalsIgnoreCase(action)){
                                boolean ok = dao.setForSale(id, false);
                                write(exchange, ok?200:404, gson.toJson(java.util.Map.of("id", id, "suspended", ok)));
                                return;
                            } else if ("send".equalsIgnoreCase(action) || "gift".equalsIgnoreCase(action)){
                                try (Connection conn = DBConnection.getConnection()){
                                    var body = gson.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), java.util.Map.class);
                                    if (body == null || !body.containsKey("usuario_id")) { write(exchange,400,gson.toJson(java.util.Map.of("error","usuario_id required"))); return; }
                                    Integer usuarioId = ((Number)body.get("usuario_id")).intValue();
                                    // check existing ownership
                                    PreparedStatement pst = conn.prepareStatement("SELECT id FROM Compra WHERE usuario_id = ? AND videojuego_id = ?"); pst.setInt(1, usuarioId); pst.setInt(2, id); ResultSet rsu = pst.executeQuery(); if (rsu.next()){ write(exchange,200,gson.toJson(java.util.Map.of("id", rsu.getInt("id"), "notice","already_owned"))); return; }
                                    // insert zero-cost compra as gift
                                    PreparedStatement ins = conn.prepareStatement("INSERT INTO Compra (usuario_id, videojuego_id, total, platform_commission, company_amount) VALUES (?,?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
                                    ins.setInt(1, usuarioId); ins.setInt(2, id); ins.setBigDecimal(3, BigDecimal.ZERO); ins.setBigDecimal(4, BigDecimal.ZERO); ins.setBigDecimal(5, BigDecimal.ZERO); ins.executeUpdate(); ResultSet rki = ins.getGeneratedKeys(); Integer cid = null; if (rki.next()) cid = rki.getInt(1);
                                    write(exchange,201,gson.toJson(java.util.Map.of("id", cid, "gifted_to", usuarioId)));
                                    return;
                                } catch(Exception ex){ write(exchange,500,gson.toJson(java.util.Map.of("error", ex.getMessage()))); return; }
                            }
                        } catch (NumberFormatException nfe){
                            // tail did not start with an integer id, ignore and fallthrough
                        } catch(Exception e){ /* fallthrough */ }
                    }
                }
            }

            write(exchange, 405, gson.toJson(java.util.Collections.singletonMap("error","method not allowed")));
        } catch (Exception e) {
            try { write(exchange, 500, gson.toJson(java.util.Collections.singletonMap("error", e.getMessage()))); } catch (Exception ex) { /* ignore */ }
        }
    }

    private void write(HttpExchange ex, int status, String body) throws Exception {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()){ os.write(bytes); }
    }
}
