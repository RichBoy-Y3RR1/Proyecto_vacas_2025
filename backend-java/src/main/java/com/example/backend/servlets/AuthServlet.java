package com.example.backend.servlets;

import com.example.backend.models.AbstractUser;
import com.example.backend.models.Gamer;
import com.example.backend.models.Admin;
import com.example.backend.services.UserService;
import com.example.backend.services.UserServiceImpl;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "AuthServlet", urlPatterns = {"*/api/auth/"})
public class AuthServlet extends BaseServlet {
    private final UserService userService = new UserServiceImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        String path = req.getPathInfo();
        try {
            if (path == null || path.equals("/") || path.equals("/register")){
                // simple registro
                java.io.BufferedReader reader = req.getReader();
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) { sb.append(line); }
                String bodyStr = sb.toString();
                System.out.println("REQUEST BODY (register): " + bodyStr);
                Map body = gson.fromJson(bodyStr, Map.class);
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
                java.io.BufferedReader reader = req.getReader();
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) { sb.append(line); }
                String bodyStr = sb.toString();
                System.out.println("REQUEST BODY (login): " + bodyStr);
                Map body = gson.fromJson(bodyStr, Map.class);
                String email = (String) body.get("email");
                String password = (String) body.get("password");
                var found = userService.findByEmail(email);
                if (found==null || !password.equals(found.getPasswordHash())){ resp.setStatus(401); writeJson(resp, Map.of("error","invalid")); return; }
              
                Map<String,Object> out = new HashMap<>();
                // create JWT with claims
                java.util.Map<String,Object> claims = new java.util.HashMap<>();
                claims.put("userId", found.getId());
                claims.put("role", found.getRole());
                if (found instanceof com.example.backend.models.CompanyUser){
                    com.example.backend.models.CompanyUser cu = (com.example.backend.models.CompanyUser) found;
                    claims.put("companyId", cu.getCompanyId());
                }
                String token = com.example.backend.security.JwtUtil.createToken(claims);
                out.put("token", token);
                Map<String,Object> userMap = new HashMap<>();
                userMap.put("id", found.getId());
                userMap.put("email", found.getEmail());
                userMap.put("role", found.getRole());
                if (found instanceof com.example.backend.models.CompanyUser){
                    com.example.backend.models.CompanyUser cu = (com.example.backend.models.CompanyUser) found;
                    userMap.put("name", cu.getName());
                    userMap.put("companyId", cu.getCompanyId());
                } else if (found instanceof Gamer){
                    Gamer g = (Gamer) found;
                    userMap.put("nickname", g.getNickname());
                    userMap.put("country", g.getCountry());
                }
                out.put("user", userMap);
                writeJson(resp, out);
                return;
            }
            if (path.equals("/logout")){
                // require Authorization header
                String authh = req.getHeader("Authorization");
                if (authh == null || !authh.startsWith("Bearer ")){ resp.setStatus(401); writeJson(resp, Map.of("error","missing_token")); return; }
                String token = authh.substring("Bearer ".length());
                try {
                    var dj = com.example.backend.security.JwtUtil.verify(token);
                    String jti = dj.getId();
                    new com.example.backend.dao.RevokedTokenDAO().revoke(jti);
                    writeJson(resp, Map.of("ok", true));
                } catch (Exception ex){ resp.setStatus(400); writeJson(resp, Map.of("error","invalid_token")); }
                return;
            }
            if (path.equals("/refresh")){
                String authh = req.getHeader("Authorization");
                if (authh == null || !authh.startsWith("Bearer ")){ resp.setStatus(401); writeJson(resp, Map.of("error","missing_token")); return; }
                String token = authh.substring("Bearer ".length());
                try {
                    var dj = com.example.backend.security.JwtUtil.verify(token);
                    // revoke old token
                    String oldjti = dj.getId();
                    new com.example.backend.dao.RevokedTokenDAO().revoke(oldjti);
                    // extract claims and issue new token
                    java.util.Map<String,Object> claims = new java.util.HashMap<>();
                    var roleClaim = dj.getClaim("role"); if (!roleClaim.isNull()) claims.put("role", roleClaim.asString());
                    var uidClaim = dj.getClaim("userId"); if (!uidClaim.isNull()) claims.put("userId", uidClaim.asInt());
                    var comp = dj.getClaim("companyId"); if (comp != null && !comp.isNull()) claims.put("companyId", comp.asInt());
                    String newToken = com.example.backend.security.JwtUtil.createToken(claims);
                    writeJson(resp, Map.of("token", newToken));
                } catch (Exception ex){ resp.setStatus(400); writeJson(resp, Map.of("error","invalid_token")); }
                return;
            }
            resp.setStatus(404);
        } catch (Exception e){
            resp.setStatus(500); writeJson(resp, Map.of("error", e.getMessage()));
        }
    }
}
