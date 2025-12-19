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

public class VideojuegoHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final VideojuegoDAO dao = new JdbcVideojuegoDAO();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            // path: /backend/api/videojuegos or /backend/api/videojuegos/{id}
            String base = "/backend/api/videojuegos";
            if ("GET".equalsIgnoreCase(method) && path.equals(base)) {
                List<Videojuego> list = dao.listAll();
                write(exchange, 200, gson.toJson(list));
                return;
            }

            if ("POST".equalsIgnoreCase(method) && path.equals(base)) {
                Videojuego body = gson.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), Videojuego.class);
                Integer id = dao.create(body);
                write(exchange, 200, gson.toJson(java.util.Collections.singletonMap("id", id)));
                return;
            }

            if (("PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) && path.startsWith(base + "/")) {
                String idStr = path.substring((base + "/").length()).split("/")[0];
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
