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
            ex.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            String base = "/backend/api/usuarios";
            if ("GET".equalsIgnoreCase(method) && path.equals(base)){
                List<AbstractUser> all = userService.listAll();
            
                var profiles = all.stream().map(AbstractUser::publicProfile).toList();
                write(ex,200,gson.toJson(profiles)); return;
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
