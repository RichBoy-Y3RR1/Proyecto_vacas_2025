package com.example.backend.http;

import com.example.backend.services.UserService;
import com.example.backend.services.UserServiceImpl;
import com.example.backend.models.AbstractUser;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class UsuarioHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final UserService userService = new UserServiceImpl();

    @Override
    public void handle(HttpExchange ex) {
        try {
            ex.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if (path.endsWith("/") && path.length()>1) path = path.substring(0, path.length()-1);
            String suffix = "/api/usuarios";
            if ("GET".equalsIgnoreCase(method) && path.endsWith(suffix)){
                List<AbstractUser> all = userService.listAll();
                var profiles = all.stream().map(AbstractUser::publicProfile).toList();
                write(ex,200,gson.toJson(profiles)); return;
            }
            // create/register user
            if ("POST".equalsIgnoreCase(method) && path.endsWith(suffix)){
                // read body
                java.io.InputStream is = ex.getRequestBody();
                String body = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                try {
                    java.util.Map map = gson.fromJson(body, java.util.Map.class);
                    String email = map.containsKey("email") ? String.valueOf(map.get("email")) : null;
                    String password = map.containsKey("password") ? String.valueOf(map.get("password")) : null;
                    String nickname = map.containsKey("nickname") ? String.valueOf(map.get("nickname")) : null;
                    if (email == null || password == null) { write(ex,400,gson.toJson(java.util.Collections.singletonMap("error","email and password required"))); return; }
                    com.example.backend.models.Gamer g = new com.example.backend.models.Gamer();
                    g.setEmail(email); g.setPasswordHash(password); g.setNickname(nickname);
                    Integer id = userService.register(g);
                    if (id != null) { g.setId(id); write(ex,201,gson.toJson(g.publicProfile())); } else { write(ex,500,gson.toJson(java.util.Collections.singletonMap("error","could_not_create"))); }
                } catch(Exception e){ write(ex,500,gson.toJson(java.util.Collections.singletonMap("error", e.getMessage()))); }
                return;
            }
            // GET by id: path may end with /api/usuarios/{id}
            if ("GET".equalsIgnoreCase(method) && path.contains(suffix + "/")){
                String idStr = path.substring(path.lastIndexOf('/') + 1);
                try { Integer id = Integer.parseInt(idStr); var u = userService.getById(id); if (u == null) { write(ex,404,gson.toJson(java.util.Collections.singletonMap("error","not_found"))); } else { write(ex,200,gson.toJson(u.publicProfile())); } return; } catch(Exception exx){ /* fallthrough */ }
            }
            write(ex,405,gson.toJson(java.util.Collections.singletonMap("error","method not allowed")));
        } catch (Exception e){ try { write(ex,500,gson.toJson(java.util.Collections.singletonMap("error", e.getMessage()))); } catch (Exception exx){} }
    }

    private void write(HttpExchange ex, int status, String body) throws Exception{
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream os = ex.getResponseBody()){ os.write(b); }
    }
}
