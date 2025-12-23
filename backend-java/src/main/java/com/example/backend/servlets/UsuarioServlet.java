package com.example.backend.servlets;

import com.example.backend.models.Admin;
import com.example.backend.models.CompanyUser;
import com.example.backend.models.Gamer;
import com.example.backend.models.AbstractUser;
import com.example.backend.services.UsuarioService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class UsuarioServlet extends BaseServlet {
    private final UsuarioService service = new UsuarioService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Integer id = parseId(req);
            if (id == null) {
                writeJson(resp, service.listAll());
            } else {
                AbstractUser u = service.getById(id);
                if (u == null) { resp.setStatus(HttpServletResponse.SC_NOT_FOUND); return; }
                writeJson(resp, u);
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJson(resp, Map.of("error", e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Map body = gson.fromJson(req.getReader(), Map.class);
            String role = (String) body.getOrDefault("role","USUARIO");
            AbstractUser u;
            if ("EMPRESA".equalsIgnoreCase(role)){
                CompanyUser cu = new CompanyUser(); cu.setEmail((String)body.get("email")); cu.setPasswordHash((String)body.get("password")); cu.setName((String)body.get("name")); u = cu;
            } else if ("ADMIN".equalsIgnoreCase(role)){
                Admin a = new Admin(); a.setEmail((String)body.get("email")); a.setPasswordHash((String)body.get("password")); u = a;
            } else {
                Gamer g = new Gamer(); g.setEmail((String)body.get("email")); g.setPasswordHash((String)body.get("password")); g.setNickname((String)body.get("nickname")); u = g;
            }
            Integer id = service.create(u);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            writeJson(resp, Map.of("id", id));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, Map.of("error", e.getMessage()));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Integer id = parseId(req);
            if (id == null) { resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); return; }
            Map body = gson.fromJson(req.getReader(), Map.class);
            AbstractUser existing = service.getById(id);
            if (existing == null) { resp.setStatus(HttpServletResponse.SC_NOT_FOUND); return; }
            // simple field updates
            if (body.containsKey("email")) existing.setEmail((String)body.get("email"));
            if (body.containsKey("password")) existing.setPasswordHash((String)body.get("password"));
            if (existing instanceof Gamer){ Gamer g = (Gamer) existing; if (body.containsKey("nickname")) g.setNickname((String)body.get("nickname")); }
            if (existing instanceof CompanyUser){ CompanyUser cu = (CompanyUser) existing; if (body.containsKey("name")) cu.setName((String)body.get("name")); }
            boolean ok = service.update(existing);
            resp.setStatus(ok ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, Map.of("error", e.getMessage()));
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
            writeJson(resp, Map.of("error", e.getMessage()));
        }
    }
}

