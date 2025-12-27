package com.example.backend.servlets;

import com.example.backend.models.Videojuego;
import com.example.backend.services.VideojuegoService;
import jakarta.servlet.annotation.WebServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "VideojuegoServlet", urlPatterns = {"/api/videojuegos/*"})
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
            // require authenticated user (company or admin)
            Integer tokenUser = (Integer) req.getAttribute("userId");
            String tokenRole = (String) req.getAttribute("role");
            Integer companyIdAttr = (Integer) req.getAttribute("companyId");
            if (tokenUser == null) { resp.setStatus(401); writeJson(resp, java.util.Map.of("error","login_required")); return; }
            if (!("EMPRESA".equalsIgnoreCase(tokenRole) || "ADMIN".equalsIgnoreCase(tokenRole))){ resp.setStatus(403); writeJson(resp, java.util.Map.of("error","forbidden")); return; }
            Videojuego v = gson.fromJson(req.getReader(), Videojuego.class);
            // Basic validation
            if (v.getNombre() == null || v.getNombre().trim().isEmpty()){ resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); writeJson(resp, java.util.Map.of("error","nombre_required")); return; }
            if (v.getPrecio() == null) v.setPrecio(new java.math.BigDecimal("0"));
            // If user is company and no empresa specified, set empresa_id via companyId claim in token
            if ("EMPRESA".equalsIgnoreCase(tokenRole)){
                Integer tokenCompany = (Integer) req.getAttribute("companyId");
                if (tokenCompany != null) v.setEmpresaId(tokenCompany);
            }
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
            Videojuego existing = service.getById(id);
            if (existing == null) { resp.setStatus(HttpServletResponse.SC_NOT_FOUND); return; }
            // permission: only ADMIN or company owner can update
            String tokenRole = (String) req.getAttribute("role");
            Integer tokenCompany = (Integer) req.getAttribute("companyId");
            if (!"ADMIN".equalsIgnoreCase(tokenRole)){
                if (tokenCompany == null || existing.getEmpresaId() == null || !tokenCompany.equals(existing.getEmpresaId())){ resp.setStatus(HttpServletResponse.SC_FORBIDDEN); writeJson(resp, java.util.Map.of("error","forbidden")); return; }
            }
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
            Videojuego existing = service.getById(id);
            if (existing == null) { resp.setStatus(HttpServletResponse.SC_NOT_FOUND); return; }
            String tokenRole = (String) req.getAttribute("role");
            Integer tokenCompany = (Integer) req.getAttribute("companyId");
            if (!"ADMIN".equalsIgnoreCase(tokenRole)){
                if (tokenCompany == null || existing.getEmpresaId() == null || !tokenCompany.equals(existing.getEmpresaId())){ resp.setStatus(HttpServletResponse.SC_FORBIDDEN); writeJson(resp, java.util.Map.of("error","forbidden")); return; }
            }
            boolean ok = service.delete(id);
            resp.setStatus(ok ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJson(resp, java.util.Map.of("error", e.getMessage()));
        }
    }
}


