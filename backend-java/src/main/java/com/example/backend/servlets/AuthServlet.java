package com.example.backend.servlets;

import com.example.backend.models.AbstractUser;
import com.example.backend.models.Gamer;
import com.example.backend.models.Admin;
import com.example.backend.services.UserService;
import com.example.backend.services.UserServiceImpl;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "AuthServlet", urlPatterns = {"/api/auth/*"})
public class AuthServlet extends BaseServlet {
    private final UserService userService = new UserServiceImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        String path = req.getPathInfo();
        try {
            if (path == null || path.equals("/") || path.equals("/register")){
                // simple registro
                Map body = gson.fromJson(req.getReader(), Map.class);
                String email = (String) body.get("email");
                String password = (String) body.get("password");
                String role = (String) body.getOrDefault("role","USUARIO");
                if (email==null || password==null){ resp.setStatus(400); writeJson(resp, Map.of("error","email and password required")); return; }
                AbstractUser u;
                if ("USUARIO".equalsIgnoreCase(role)){
                    Gamer g = new Gamer(); g.setEmail(email); g.setPasswordHash(password); g.setNickname((String)body.getOrDefault("nickname","user"));
                    u = g;
                } else if ("EMPRESA".equalsIgnoreCase(role)){
                    com.example.backend.models.CompanyUser cu = new com.example.backend.models.CompanyUser(); cu.setEmail(email); cu.setPasswordHash(password); cu.setName((String)body.getOrDefault("name","empresa")); u = cu;
                } else {
                    Admin a = new Admin(); a.setEmail(email); a.setPasswordHash(password); u = a;
                }
                Integer id = userService.register(u);
                writeJson(resp, Map.of("id", id));
                return;
            }
            // login
            if (path.equals("/login")){
                Map body = gson.fromJson(req.getReader(), Map.class);
                String email = (String) body.get("email");
                String password = (String) body.get("password");
                var found = userService.findByEmail(email);
                if (found==null || !password.equals(found.getPasswordHash())){ resp.setStatus(401); writeJson(resp, Map.of("error","invalid")); return; }
              
                Map<String,Object> out = new HashMap<>(); out.put("token","demo-token"); out.put("user", found.publicProfile());
                writeJson(resp, out);
                return;
            }
            resp.setStatus(404);
        } catch (Exception e){
            resp.setStatus(500); writeJson(resp, Map.of("error", e.getMessage()));
        }
    }
}
