package com.example.backend.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CorsWrapper implements HttpHandler {
    private final HttpHandler delegate;

    public CorsWrapper(HttpHandler delegate){ this.delegate = delegate; }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            // Use set(...) to avoid creating duplicate header entries when other
            // components also set CORS headers. set(...) replaces existing values.
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept");
            exchange.getResponseHeaders().set("Access-Control-Max-Age", "3600");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())){
                byte[] empty = new byte[0];
                exchange.sendResponseHeaders(204, 0);
                try (OutputStream os = exchange.getResponseBody()){ os.write(empty); }
                return;
            }
            delegate.handle(exchange);
        } catch (Exception e){
            try { String err = "{\"error\":\""+ e.getMessage() +"\"}"; byte[] b = err.getBytes(StandardCharsets.UTF_8); exchange.getResponseHeaders().add("Content-Type","application/json;charset=UTF-8"); exchange.sendResponseHeaders(500, b.length); try (OutputStream os = exchange.getResponseBody()){ os.write(b); } } catch(Exception ex){}
        }
    }
}
