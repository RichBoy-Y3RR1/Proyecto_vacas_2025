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
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpContext;
import java.util.ArrayList;
import java.util.List;
import java.io.OutputStream;
import java.io.IOException;

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
        // By default bind only to the preferred port (8080). To allow trying a range
        // set environment variable ALLOW_PORT_RANGE=1 (for compatibility during dev).
        boolean allowRange = "1".equals(System.getenv().getOrDefault("ALLOW_PORT_RANGE","0"));
        if (allowRange) {
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
                return;
            }
        } else {
            try {
                srv = HttpServer.create(new InetSocketAddress(preferred), 0);
                port = preferred;
            } catch (java.net.BindException be) {
                System.err.println("Preferred port " + preferred + " unavailable. Exiting because ALLOW_PORT_RANGE is not enabled.");
                return;
            }
        }
        // initialize DB schema for embedded mode (if using H2 fallback)
        try { com.example.backend.db.EmbeddedDbInitializer.init(); } catch (Exception ex) { System.err.println("Embedded DB init failed: " + ex.getMessage()); }

        // abrir la api de juegos y otros endpoints
        List<HttpContext> createdContexts = new ArrayList<>();
        createdContexts.add(srv.createContext("/backend/api/videojuegos", new com.example.backend.http.CorsWrapper(new VideojuegoHandler())));
        createdContexts.add(srv.createContext("/backend/api/comentarios", new com.example.backend.http.CorsWrapper(new ComentarioHandler())));
        createdContexts.add(srv.createContext("/backend/api/usuarios", new com.example.backend.http.CorsWrapper(new UsuarioHandler())));
        createdContexts.add(srv.createContext("/backend/api/compras", new com.example.backend.http.CorsWrapper(new CompraHandler())));
        createdContexts.add(srv.createContext("/backend/api/auth", new com.example.backend.http.CorsWrapper(new AuthHandler())));
        createdContexts.add(srv.createContext("/backend/api/categorias", new com.example.backend.http.CorsWrapper(new CategoriaHandler())));
        createdContexts.add(srv.createContext("/backend/api/banner", new com.example.backend.http.CorsWrapper(new BannerHandler())));
        createdContexts.add(srv.createContext("/backend/api/cartera", new com.example.backend.http.CorsWrapper(new CarteraHandler())));
        createdContexts.add(srv.createContext("/backend/api/comision", new com.example.backend.http.CorsWrapper(new com.example.backend.http.ComisionHandler())));
        createdContexts.add(srv.createContext("/backend/api/empresa", new com.example.backend.http.CorsWrapper(new com.example.backend.http.EmpresaHandler())));
        // reports endpoint (plural 'empresas' to match frontend requests)
        try {
            Class<?> rhClass = Class.forName("com.example.backend.http.ReportHandler");
            Object rh = rhClass.getDeclaredConstructor().newInstance();
            srv.createContext("/backend/api/empresas", new com.example.backend.http.CorsWrapper((com.sun.net.httpserver.HttpHandler) rh));
        } catch (ClassNotFoundException cnf) {
            System.err.println("ReportHandler (JasperReports) not available on classpath — report endpoints disabled.");
        } catch (Throwable t) {
            System.err.println("Failed to initialize ReportHandler: " + t.getMessage());
        }

        // Simple diagnostic probe to verify routing and CORS behavior
            createdContexts.add(srv.createContext("/backend/api/probe", new com.example.backend.http.CorsWrapper(new com.sun.net.httpserver.HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) {
                    try {
                        java.util.Map<String,Object> out = new java.util.HashMap<>();
                        out.put("path", exchange.getRequestURI().getPath());
                        out.put("method", exchange.getRequestMethod());
                        out.put("remote", String.valueOf(exchange.getRemoteAddress()));
                        java.util.Map<String, String> hdrs = new java.util.HashMap<>();
                        exchange.getRequestHeaders().forEach((k,v)-> hdrs.put(k, String.join(";", v)));
                        out.put("requestHeaders", hdrs);
                        byte[] b = new com.google.gson.Gson().toJson(out).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
                        exchange.sendResponseHeaders(200, b.length);
                        try (java.io.OutputStream os = exchange.getResponseBody()){ os.write(b); }
                    } catch(Exception e){ try { exchange.sendResponseHeaders(500, -1); } catch(Exception ex){} }
                }
            })));
        // Add a catch-all API dispatcher to ensure any /backend/api/* path is routed
        createdContexts.add(srv.createContext("/backend/api", new com.example.backend.http.CorsWrapper(new com.sun.net.httpserver.HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) {
                String path = exchange.getRequestURI().getPath();
                // route to specific handlers based on path prefix
                try {
                    if (path.startsWith("/backend/api/auth")) { new com.example.backend.http.AuthHandler().handle(exchange); return; }
                    if (path.startsWith("/backend/api/videojuegos")) { new com.example.backend.http.VideojuegoHandler().handle(exchange); return; }
                    if (path.startsWith("/backend/api/comentarios")) { new com.example.backend.http.ComentarioHandler().handle(exchange); return; }
                    if (path.startsWith("/backend/api/usuarios")) { new com.example.backend.http.UsuarioHandler().handle(exchange); return; }
                    if (path.startsWith("/backend/api/compras")) { new com.example.backend.http.CompraHandler().handle(exchange); return; }
                    if (path.startsWith("/backend/api/categorias")) { new com.example.backend.http.CategoriaHandler().handle(exchange); return; }
                    if (path.startsWith("/backend/api/banner")) { new com.example.backend.http.BannerHandler().handle(exchange); return; }
                    if (path.startsWith("/backend/api/cartera")) { new com.example.backend.http.CarteraHandler().handle(exchange); return; }
                    if (path.startsWith("/backend/api/comision")) { new com.example.backend.http.ComisionHandler().handle(exchange); return; }
                    if (path.startsWith("/backend/api/empresa")) { new com.example.backend.http.EmpresaHandler().handle(exchange); return; }
                    if (path.startsWith("/backend/api/empresas")) { 
                        try { Class<?> rhClass = Class.forName("com.example.backend.http.ReportHandler"); Object rh = rhClass.getDeclaredConstructor().newInstance(); ((com.sun.net.httpserver.HttpHandler)rh).handle(exchange); return; } catch (ClassNotFoundException cnf) { }
                    }
                    if (path.startsWith("/backend/api/gamer")) {
                        try { Class<?> grh = Class.forName("com.example.backend.http.GamerReportHandler"); Object gh = grh.getDeclaredConstructor().newInstance(); ((com.sun.net.httpserver.HttpHandler)gh).handle(exchange); return; } catch (ClassNotFoundException cnf) { }
                    }
                    // fallback to 404 for unknown API routes
                    String notFound = "{\"error\":\"not_found\"}";
                    exchange.getResponseHeaders().set("Content-Type","application/json;charset=utf-8");
                    byte[] nf = notFound.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(404, nf.length);
                    try (java.io.OutputStream os = exchange.getResponseBody()){ os.write(nf); }
                } catch (Exception e) {
                    try { exchange.sendResponseHeaders(500, -1); } catch(Exception ex){}
                }
            }
        })));
        // register same API under the WAR context path some clients expect
        String warPrefix = "/tienda-backend-1.0.0/api";
        createdContexts.add(srv.createContext(warPrefix + "/videojuegos", new com.example.backend.http.CorsWrapper(new VideojuegoHandler())));
        createdContexts.add(srv.createContext(warPrefix + "/comentarios", new com.example.backend.http.CorsWrapper(new ComentarioHandler())));
        createdContexts.add(srv.createContext(warPrefix + "/usuarios", new com.example.backend.http.CorsWrapper(new UsuarioHandler())));
        createdContexts.add(srv.createContext(warPrefix + "/compras", new com.example.backend.http.CorsWrapper(new CompraHandler())));
        createdContexts.add(srv.createContext(warPrefix + "/auth", new com.example.backend.http.CorsWrapper(new AuthHandler())));
        createdContexts.add(srv.createContext(warPrefix + "/categorias", new com.example.backend.http.CorsWrapper(new CategoriaHandler())));
        createdContexts.add(srv.createContext(warPrefix + "/banner", new com.example.backend.http.CorsWrapper(new BannerHandler())));
        createdContexts.add(srv.createContext(warPrefix + "/cartera", new com.example.backend.http.CorsWrapper(new CarteraHandler())));
        createdContexts.add(srv.createContext(warPrefix + "/comision", new com.example.backend.http.CorsWrapper(new com.example.backend.http.ComisionHandler())));
        createdContexts.add(srv.createContext(warPrefix + "/empresa", new com.example.backend.http.CorsWrapper(new com.example.backend.http.EmpresaHandler())));

        // gamer reports handler (provides preview JSON and fallback PDF generation)
        try {
            Class<?> grh = Class.forName("com.example.backend.http.GamerReportHandler");
            Object gr = grh.getDeclaredConstructor().newInstance();
            srv.createContext("/backend/api/gamer", new com.example.backend.http.CorsWrapper((com.sun.net.httpserver.HttpHandler) gr));
        } catch (ClassNotFoundException cnf) {
            System.err.println("GamerReportHandler not found, gamer report endpoints disabled.");
        } catch (Throwable t) {
            System.err.println("Failed to initialize GamerReportHandler: " + t.getMessage());
        }

        // servir archivos estáticos para la UI (Backend.html, openapi.yaml, assets...)
        // use absolute project dir to avoid issues when process wd differs
        String proj = System.getProperty("user.dir");
        java.io.File webRoot = new java.io.File(proj, "target/backend");
        if (!webRoot.exists()) webRoot = new java.io.File(proj, "src/main/webapp");
        final java.io.File finalRoot = webRoot;
            createdContexts.add(srv.createContext("/backend", new com.sun.net.httpserver.HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) {
                    String path = exchange.getRequestURI().getPath();
                    // remove the '/backend' prefix
                    String rel = path.length() > 8 ? path.substring(8) : "/";
                    if (rel.equals("") || rel.equals("/")) rel = "/Backend.html";
                    java.io.File f = new java.io.File(finalRoot, rel.replaceFirst("^/+",""));
                    System.out.println("Static request -> root="+finalRoot.getAbsolutePath()+" rel='"+rel+"' file='"+f.getAbsolutePath()+"'");
                    // Ensure CORS headers for static responses (helpful when API contexts are not matched)
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept");
                    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())){
                        try { exchange.sendResponseHeaders(204, 0); } catch(Exception ex){}
                        try (java.io.OutputStream os = exchange.getResponseBody()){ } catch(Exception ex){}
                        return;
                    }
                    if (!f.exists() || f.isDirectory()) {
                        String notFound = "404 Not Found\nNo context found for request";
                        exchange.getResponseHeaders().set("Content-Type","text/plain;charset=utf-8");
                        byte[] nf = notFound.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        try { exchange.sendResponseHeaders(404, nf.length); try (java.io.OutputStream os = exchange.getResponseBody()) { os.write(nf); } } catch(Exception ex){}
                        return;
                    }
                    String contentType = null;
                    try { contentType = java.nio.file.Files.probeContentType(f.toPath()); } catch(Exception ex){}
                    if (contentType == null) contentType = "application/octet-stream";
                    exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
                    try { exchange.sendResponseHeaders(200, f.length()); try (java.io.OutputStream os = exchange.getResponseBody(); java.io.InputStream is = new java.io.FileInputStream(f)) { byte[] buf = new byte[8192]; int r; while ((r = is.read(buf)) != -1) os.write(buf,0,r); } } catch(Exception ex){}
                }
            }));
        srv.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        // Add a CORS filter to every context to ensure preflight and error responses
        try {
            Filter corsFilter = new Filter() {
                @Override
                public String description() { return "Ensure CORS headers and handle OPTIONS"; }

                @Override
                public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
                    String reqPath = exchange.getRequestURI() != null ? exchange.getRequestURI().getPath() : "<no-path>";
                    String method = exchange.getRequestMethod();
                    System.out.println("CORS Filter: incoming " + method + " " + reqPath + " from " + exchange.getRemoteAddress());
                    var headers = exchange.getResponseHeaders();
                    headers.set("Access-Control-Allow-Origin", "*");
                    headers.set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH");
                    headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept");
                    headers.set("Access-Control-Max-Age", "3600");
                    if ("OPTIONS".equalsIgnoreCase(method)){
                        System.out.println("CORS Filter: handling preflight for " + reqPath);
                        exchange.sendResponseHeaders(204, 0);
                        try (OutputStream os = exchange.getResponseBody()) { }
                        return;
                    }
                    chain.doFilter(exchange);
                }
            };

            for (com.sun.net.httpserver.HttpContext ctx : createdContexts) {
                // add filter at the front so it runs before handlers
                ctx.getFilters().add(0, corsFilter);
                System.out.println("Registered context: '" + ctx.getPath() + "' -> handler=" + (ctx.getHandler() != null ? ctx.getHandler().getClass().getName() : "<null>"));
            }
        } catch (Throwable t) { System.err.println("Could not add CORS filters: " + t.getMessage()); }
        try{
            srv.start();
            System.out.println("LauncherMain HTTP server started on http://localhost:"+port+"/backend/api/ and static /backend/");
        } catch (Throwable t){
            System.err.println("Failed to start HTTP server: " + t.getMessage());
            // attempt graceful shutdown if partially initialised
            try{ srv.stop(0); } catch(Exception ex){}
            return;
        }
    }
}
