package com.example.backend;

import com.example.backend.http.VideojuegoHandler;
import com.example.backend.http.ComentarioHandler;
import com.example.backend.http.UsuarioHandler;
import com.example.backend.http.CompraHandler;
import com.example.backend.http.AuthHandler;
import com.example.backend.http.CategoriaHandler;
import com.example.backend.http.BannerHandler;
import com.example.backend.http.CarteraHandler;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import com.example.backend.db.DbInitializer;

public class LauncherMain {
    public static void main(String[] args) throws Exception {
        // Try to start embedded HTTP server on preferred port (default 8080).
        int preferred = 8080;
        if (args != null && args.length > 0) {
            try { preferred = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        } else {
            String env = System.getenv("PORT");
            if (env != null && !env.isBlank()) {
                try { preferred = Integer.parseInt(env); } catch (Exception ignored) {}
            }
        }

        HttpServer srv = null;
        int port = -1;
        for (int p = preferred; p <= preferred + 10; p++) {
            try {
                srv = HttpServer.create(new InetSocketAddress(p), 0);
                port = p;
                break;
            } catch (java.net.BindException be) {
                System.err.println("Port " + p + " unavailable, trying next...");
            }
        }
        if (srv == null) {
            System.err.println("No available port found in range " + preferred + "-" + (preferred+10));
        
        }
        // initialize DB schema for embedded mode (if using H2 fallback)
        try { com.example.backend.db.EmbeddedDbInitializer.init(); } catch (Exception ex) { System.err.println("Embedded DB init failed: " + ex.getMessage()); }

        // abrir la api de juegos y otros endpoints
        srv.createContext("/backend/api/videojuegos", new com.example.backend.http.CorsWrapper(new VideojuegoHandler()));
        srv.createContext("/backend/api/comentarios", new com.example.backend.http.CorsWrapper(new ComentarioHandler()));
        srv.createContext("/backend/api/usuarios", new com.example.backend.http.CorsWrapper(new UsuarioHandler()));
        srv.createContext("/backend/api/compras", new com.example.backend.http.CorsWrapper(new CompraHandler()));
        srv.createContext("/backend/api/auth", new com.example.backend.http.CorsWrapper(new AuthHandler()));
        srv.createContext("/backend/api/categorias", new com.example.backend.http.CorsWrapper(new CategoriaHandler()));
        srv.createContext("/backend/api/banner", new com.example.backend.http.CorsWrapper(new BannerHandler()));
        srv.createContext("/backend/api/cartera", new com.example.backend.http.CorsWrapper(new CarteraHandler()));
        srv.createContext("/backend/api/comision", new com.example.backend.http.CorsWrapper(new com.example.backend.http.ComisionHandler()));
        srv.createContext("/backend/api/empresa", new com.example.backend.http.CorsWrapper(new com.example.backend.http.EmpresaHandler()));

        // register same API under the WAR context path some clients expect
        String warPrefix = "/tienda-backend-1.0.0/api";
        srv.createContext(warPrefix + "/videojuegos", new com.example.backend.http.CorsWrapper(new VideojuegoHandler()));
        srv.createContext(warPrefix + "/comentarios", new com.example.backend.http.CorsWrapper(new ComentarioHandler()));
        srv.createContext(warPrefix + "/usuarios", new com.example.backend.http.CorsWrapper(new UsuarioHandler()));
        srv.createContext(warPrefix + "/compras", new com.example.backend.http.CorsWrapper(new CompraHandler()));
        srv.createContext(warPrefix + "/auth", new com.example.backend.http.CorsWrapper(new AuthHandler()));
        srv.createContext(warPrefix + "/categorias", new com.example.backend.http.CorsWrapper(new CategoriaHandler()));
        srv.createContext(warPrefix + "/banner", new com.example.backend.http.CorsWrapper(new BannerHandler()));
        srv.createContext(warPrefix + "/cartera", new com.example.backend.http.CorsWrapper(new CarteraHandler()));
        srv.createContext(warPrefix + "/comision", new com.example.backend.http.CorsWrapper(new com.example.backend.http.ComisionHandler()));
        srv.createContext(warPrefix + "/empresa", new com.example.backend.http.CorsWrapper(new com.example.backend.http.EmpresaHandler()));

        // servir archivos estáticos para la UI (Backend.html, openapi.yaml, assets...)
        // use absolute project dir to avoid issues when process wd differs
        String proj = System.getProperty("user.dir");
        java.io.File webRoot = new java.io.File(proj, "target/backend");
        if (!webRoot.exists()) webRoot = new java.io.File(proj, "src/main/webapp");
        final java.io.File finalRoot = webRoot;
        srv.createContext("/backend", exchange -> {
            String path = exchange.getRequestURI().getPath();
            // remove the '/backend' prefix
            String rel = path.length() > 8 ? path.substring(8) : "/";
            if (rel.equals("") || rel.equals("/")) rel = "/Backend.html";
            java.io.File f = new java.io.File(finalRoot, rel.replaceFirst("^/+",""));
            System.out.println("Static request -> root="+finalRoot.getAbsolutePath()+" rel='"+rel+"' file='"+f.getAbsolutePath()+"'");
            if (!f.exists() || f.isDirectory()) {
                String notFound = "404 Not Found\nNo context found for request";
                exchange.sendResponseHeaders(404, notFound.getBytes().length);
                try (java.io.OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
                return;
            }
            String contentType = java.nio.file.Files.probeContentType(f.toPath());
            if (contentType == null) contentType = "application/octet-stream";
            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
            exchange.sendResponseHeaders(200, f.length());
            try (java.io.OutputStream os = exchange.getResponseBody(); java.io.InputStream is = new java.io.FileInputStream(f)) {
                byte[] buf = new byte[8192]; int r;
                while ((r = is.read(buf)) != -1) os.write(buf,0,r);
            }
        });
        srv.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        srv.start();
        System.out.println("LauncherMain HTTP server started on http://localhost:"+port+"/backend/api/ and static /backend/");
    }
}
