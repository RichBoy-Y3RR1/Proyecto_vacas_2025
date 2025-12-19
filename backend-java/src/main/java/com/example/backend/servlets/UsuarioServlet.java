package com.example.backend.servlets;

import com.example.backend.models.AbstractUser;
import com.example.backend.services.UserService;
import com.example.backend.services.UserServiceImpl;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet(name = "UsuarioServlet", urlPatterns = {"/api/usuarios/*"})
public class UsuarioServlet extends BaseServlet {
    private final UserService userService = new UserServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try {
            Integer id = parseId(req);
            if (id == null){
                List<AbstractUser> all = userService.listAll();
                writeJson(resp, all.stream().map(AbstractUser::publicProfile).collect(Collectors.toList()));
                return;
            }
            AbstractUser u = userService.find(id);
            if (u==null){ resp.setStatus(404); writeJson(resp, Map.of("error","not found")); return; }
            writeJson(resp, u.publicProfile());
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, Map.of("error", e.getMessage())); }
    }
}
