package com.example.backend;

import com.example.backend.http.VideojuegoHandler;
import com.example.backend.http.ComentarioHandler;
import com.example.backend.http.UsuarioHandler;
import com.example.backend.http.CompraHandler;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class LauncherMain {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer srv = HttpServer.create(new InetSocketAddress(port), 0);
        // abrir la api de juegos y otros endpoints
        srv.createContext("/backend/api/videojuegos", new VideojuegoHandler());
        srv.createContext("/backend/api/comentarios", new ComentarioHandler());
        srv.createContext("/backend/api/usuarios", new UsuarioHandler());
        srv.createContext("/backend/api/compras", new CompraHandler());
        srv.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        srv.start();
        System.out.println("LauncherMain HTTP server started on http://localhost:"+port+"/backend/api/");
    }
}
