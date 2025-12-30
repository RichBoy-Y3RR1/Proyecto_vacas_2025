package com.example.backend.http;

import com.example.backend.services.UserService;
import com.example.backend.services.UserServiceImpl;
import com.example.backend.db.DBConnection;
import com.example.backend.security.JwtUtil;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AuthHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final UserService userService = new UserServiceImpl();

    @Override
    public void handle(HttpExchange ex) {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            System.out.println("AuthHandler: " + method + " " + path + " from " + ex.getRemoteAddress());
            ex.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            if ("OPTIONS".equalsIgnoreCase(method)) { ex.sendResponseHeaders(204, -1); return; }
            if (path.endsWith("/") && path.length()>1) path = path.substring(0,path.length()-1);
            String suffix = "/api/auth";

            if ("POST".equalsIgnoreCase(method) && (path.endsWith(suffix) || path.endsWith(suffix + "/register"))){
                Map body = gson.fromJson(new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8), Map.class);
                String email = (String) body.get("email"); String password = (String) body.get("password");
                if (email != null) email = email.toLowerCase().trim();
                String role = (String) body.getOrDefault("role","USUARIO");
                com.example.backend.models.AbstractUser user;
                if ("EMPRESA".equalsIgnoreCase(role)){
                    com.example.backend.models.CompanyUser cu = new com.example.backend.models.CompanyUser(); cu.setEmail(email); cu.setPasswordHash(password); cu.setRole("EMPRESA"); cu.setName((String)body.getOrDefault("name", null)); user = cu;
                } else if ("ADMIN".equalsIgnoreCase(role)){
                    com.example.backend.models.Admin a = new com.example.backend.models.Admin(); a.setEmail(email); a.setPasswordHash(password); a.setRole("ADMIN"); a.setFullName((String)body.getOrDefault("fullName", null)); user = a;
                } else {
                    com.example.backend.models.Gamer g = new com.example.backend.models.Gamer(); g.setEmail(email); g.setPasswordHash(password); g.setRole("USUARIO"); g.setNickname((String)body.getOrDefault("nickname", null));
                    // optional fields
                    try { Object bd = body.get("fecha_nacimiento"); if (bd!=null) g.setBirthDate(java.time.LocalDate.parse(String.valueOf(bd))); } catch(Exception _e){}
                    try { Object cty = body.get("pais"); if (cty!=null) g.setCountry(String.valueOf(cty)); } catch(Exception _e){}
                    user = g;
                }
                Integer id = userService.register(user);
                // if created, respond like login: token + user object
                if (id != null) {
                    // use the user object we just created (avoid extra DB lookup which may fail in some DB configs)
                    try {
                        user.setId(id);
                        java.util.Map<String,Object> claims = new java.util.HashMap<>(); claims.put("userId", id); claims.put("role", user.getRole());
                        if (user instanceof com.example.backend.models.CompanyUser){ Integer cid = ((com.example.backend.models.CompanyUser)user).getCompanyId(); if (cid != null) claims.put("companyId", cid); }
                        String token = JwtUtil.createToken(claims);
                        java.util.Map<String,Object> out = new java.util.HashMap<>(); out.put("token", token);
                        java.util.Map<String,Object> userMap = new java.util.HashMap<>(); userMap.put("id", user.getId()); userMap.put("email", user.getEmail()); userMap.put("role", user.getRole());
                        if (user instanceof com.example.backend.models.CompanyUser){ Integer cid = ((com.example.backend.models.CompanyUser)user).getCompanyId(); if (cid != null){ userMap.put("companyId", cid); userMap.put("empresaId", cid); userMap.put("empresa_id", cid); } }
                        out.put("user", userMap);
                        write(ex,200,gson.toJson(out));
                        return;
                    } catch (Exception _e) {
                        _e.printStackTrace();
                        write(ex,500,gson.toJson(Map.of("error","created_but_error")));
                        return;
                    }
                }
                write(ex,500,gson.toJson(Map.of("error","could_not_create")));
                return;
            }

            if ("POST".equalsIgnoreCase(method) && path.endsWith(suffix + "/login")){
                System.out.println("AuthHandler: login attempt for path="+path);
                Map body = gson.fromJson(new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8), Map.class);
                String email = (String) body.get("email"); String password = (String) body.get("password");
                if (email != null) email = email.toLowerCase().trim();
                var found = userService.findByEmail(email);
                if (found==null || !password.equals(found.getPasswordHash())){ write(ex,401,gson.toJson(Map.of("error","invalid"))); return; }
                java.util.Map<String,Object> claims = new java.util.HashMap<>(); claims.put("userId", found.getId()); claims.put("role", found.getRole());
                if (found instanceof com.example.backend.models.CompanyUser){ claims.put("companyId", ((com.example.backend.models.CompanyUser)found).getCompanyId()); }
                String token = JwtUtil.createToken(claims);
                java.util.Map<String,Object> out = new java.util.HashMap<>(); out.put("token", token);
                java.util.Map<String,Object> userMap = new java.util.HashMap<>(); userMap.put("id", found.getId()); userMap.put("email", found.getEmail()); userMap.put("role", found.getRole());
                // if company user, expose company id under multiple keys for frontend compatibility
                if (found instanceof com.example.backend.models.CompanyUser){ Integer cid = ((com.example.backend.models.CompanyUser)found).getCompanyId(); if (cid != null){ userMap.put("companyId", cid); userMap.put("empresaId", cid); userMap.put("empresa_id", cid); } }
                out.put("user", userMap);
                write(ex,200,gson.toJson(out)); return;
            }

            if ("POST".equalsIgnoreCase(method) && path.endsWith(suffix + "/logout")){
                String auth = ex.getRequestHeaders().getFirst("Authorization"); if (auth == null || !auth.startsWith("Bearer ")){ write(ex,401,gson.toJson(Map.of("error","missing_token"))); return; }
                String token = auth.substring("Bearer ".length());
                try { var dj = JwtUtil.verify(token); new com.example.backend.dao.RevokedTokenDAO().revoke(dj.getId()); write(ex,200,gson.toJson(Map.of("ok", true))); } catch(Exception e){ write(ex,400,gson.toJson(Map.of("error","invalid_token"))); }
                return;
            }

            if ("POST".equalsIgnoreCase(method) && path.endsWith(suffix + "/refresh")){
                String auth = ex.getRequestHeaders().getFirst("Authorization"); if (auth == null || !auth.startsWith("Bearer ")){ write(ex,401,gson.toJson(Map.of("error","missing_token"))); return; }
                String token = auth.substring("Bearer ".length());
                try { var dj = JwtUtil.verify(token); new com.example.backend.dao.RevokedTokenDAO().revoke(dj.getId()); java.util.Map<String,Object> claims = new java.util.HashMap<>(); var rc = dj.getClaim("role"); if (!rc.isNull()) claims.put("role", rc.asString()); var uid = dj.getClaim("userId"); if (!uid.isNull()) claims.put("userId", uid.asInt()); String newTok = JwtUtil.createToken(claims); write(ex,200,gson.toJson(Map.of("token", newTok))); } catch(Exception e){ write(ex,400,gson.toJson(Map.of("error","invalid_token"))); }
                return;
            }

            write(ex,404,gson.toJson(Map.of("error","not_found")));
        } catch (Exception e){
            // Log full stacktrace so we can see root cause in server console
            e.printStackTrace();
            try{ write(ex,500,gson.toJson(Map.of("error", e.getMessage()))); } catch(Exception exx){}
        }
    }

        private void write(HttpExchange ex, int status, String body) throws Exception {
            // Ensure CORS headers are always present even if other code paths didn't set them
            var headers = ex.getResponseHeaders();
            if (headers.getFirst("Access-Control-Allow-Origin") == null) headers.set("Access-Control-Allow-Origin","*");
            if (headers.getFirst("Access-Control-Allow-Methods") == null) headers.set("Access-Control-Allow-Methods","GET,POST,PUT,DELETE,OPTIONS");
            if (headers.getFirst("Access-Control-Allow-Headers") == null) headers.set("Access-Control-Allow-Headers","Content-Type, Authorization");
            if (headers.getFirst("Content-Type") == null) headers.set("Content-Type","application/json;charset=UTF-8");
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, b.length);
            try (OutputStream os = ex.getResponseBody()){ os.write(b); }
        }
}
