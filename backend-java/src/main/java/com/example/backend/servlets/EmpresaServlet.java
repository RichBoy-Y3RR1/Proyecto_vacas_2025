package com.example.backend.servlets;

import com.example.backend.dao.EmpresaDAO;
import com.example.backend.dao.JdbcEmpresaDAO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;

@WebServlet(name = "EmpresaServlet", urlPatterns = {"/api/empresas/*"})
public class EmpresaServlet extends BaseServlet {
    private final EmpresaDAO dao = new JdbcEmpresaDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try {
            var body = gson.fromJson(req.getReader(), java.util.Map.class);
            String name = (String) body.get("name"); String email = (String) body.get("email");
            Integer id = dao.createCompany(name,email);
            writeJson(resp, java.util.Map.of("id", id));
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try {
            Integer id = parseId(req);
            if (id==null) writeJson(resp, dao.listAll()); else writeJson(resp, dao.findById(id));
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }
}
