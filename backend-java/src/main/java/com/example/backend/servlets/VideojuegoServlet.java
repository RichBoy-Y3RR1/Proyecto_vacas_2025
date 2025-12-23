package com.example.backend.servlets;

import com.example.backend.models.Videojuego;
import com.example.backend.services.VideojuegoService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class VideojuegoServlet extends BaseServlet {
    private final VideojuegoService service = new VideojuegoService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Integer id = parseId(req);
            if (id == null) {
                List<Videojuego> list = service.listAll();
                writeJson(resp, list);
            } else {
                Videojuego v = service.getById(id);
                if (v == null) { resp.setStatus(HttpServletResponse.SC_NOT_FOUND); return; }
                writeJson(resp, v);
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJson(resp, java.util.Map.of("error", e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Videojuego v = gson.fromJson(req.getReader(), Videojuego.class);
            Integer id = service.create(v);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            writeJson(resp, java.util.Map.of("id", id));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, java.util.Map.of("error", e.getMessage()));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Integer id = parseId(req);
            if (id == null) { resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); return; }
            Videojuego v = gson.fromJson(req.getReader(), Videojuego.class);
            boolean ok = service.update(id, v);
            resp.setStatus(ok ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, java.util.Map.of("error", e.getMessage()));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Integer id = parseId(req);
            if (id == null) { resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); return; }
            boolean ok = service.delete(id);
            resp.setStatus(ok ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJson(resp, java.util.Map.of("error", e.getMessage()));
        }
    }
}


