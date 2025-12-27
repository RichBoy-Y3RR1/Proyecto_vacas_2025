package com.example.backend.servlets;

import com.example.backend.models.CompanyUser;
import com.example.backend.services.EmpresaService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class EmpresaServlet extends BaseServlet {
    private final EmpresaService service = new EmpresaService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Integer id = parseId(req);
            if (id == null) {
                writeJson(resp, service.listAll());
            } else {
                CompanyUser c = service.getById(id);
                if (c == null) { resp.setStatus(HttpServletResponse.SC_NOT_FOUND); return; }
                writeJson(resp, c);
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJson(resp, Map.of("error", e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Only ADMIN may create companies
        Integer tokenUser = (Integer) req.getAttribute("userId");
        String tokenRole = (String) req.getAttribute("role");
        if (tokenUser == null) { writeError(resp, 401, "login_required", "Authentication required"); return; }
        if (tokenRole == null || !tokenRole.equalsIgnoreCase("ADMIN")) { writeError(resp, 403, "forbidden", "Only admin can create companies"); return; }
        try {
            CompanyUser c = gson.fromJson(req.getReader(), CompanyUser.class);
            Integer id = service.createCompany(c.getName(), c.getEmail());
            resp.setStatus(HttpServletResponse.SC_CREATED);
            writeJson(resp, Map.of("id", id));
        } catch (Exception e) {
            writeError(resp, 400, "bad_request", e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Integer id = parseId(req);
            if (id == null) { resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); return; }
            CompanyUser c = gson.fromJson(req.getReader(), CompanyUser.class);
            // permission: ADMIN or company owner
            Integer tokenCompany = (Integer) req.getAttribute("companyId");
            String tokenRole = (String) req.getAttribute("role");
            if (tokenRole == null || (!tokenRole.equalsIgnoreCase("ADMIN") && (tokenCompany == null || !tokenCompany.equals(id)))){
                writeError(resp, 403, "forbidden", "Not allowed to update this company"); return;
            }
            boolean ok = service.update(id, c);
            resp.setStatus(ok ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            writeError(resp, 400, "bad_request", e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Integer id = parseId(req);
            if (id == null) { resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); return; }
            // permission: ADMIN or company owner
            Integer tokenCompany = (Integer) req.getAttribute("companyId");
            String tokenRole = (String) req.getAttribute("role");
            if (tokenRole == null || (!tokenRole.equalsIgnoreCase("ADMIN") && (tokenCompany == null || !tokenCompany.equals(id)))){
                writeError(resp, 403, "forbidden", "Not allowed to delete this company"); return;
            }
            boolean ok = service.delete(id);
            resp.setStatus(ok ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            writeError(resp, 500, "internal_error", e.getMessage());
        }
    }
}

