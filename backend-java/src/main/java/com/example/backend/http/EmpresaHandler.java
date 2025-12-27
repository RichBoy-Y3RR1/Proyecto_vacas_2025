package com.example.backend.http;

import com.example.backend.db.DBConnection;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class EmpresaHandler implements HttpHandler {
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange ex) {
        try (java.sql.Connection conn = DBConnection.getConnection()){
            ex.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if (path.endsWith("/") && path.length()>1) path = path.substring(0, path.length()-1);
            String suffix = "/api/empresa";

            if ("OPTIONS".equalsIgnoreCase(method)) { ex.getResponseHeaders().set("Access-Control-Allow-Methods","GET,POST,PUT,DELETE,OPTIONS"); ex.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type,Authorization"); write(ex,204,""); return; }

            // list companies
            if ("GET".equalsIgnoreCase(method) && path.endsWith(suffix)){
                var rs = conn.createStatement().executeQuery("SELECT id, nombre, correo, telefono, estado FROM Empresa");
                java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>();
                while (rs.next()){ var m = new java.util.HashMap<String,Object>(); m.put("id", rs.getInt("id")); m.put("nombre", rs.getString("nombre")); m.put("correo", rs.getString("correo")); m.put("telefono", rs.getString("telefono")); m.put("estado", rs.getString("estado")); list.add(m); }
                write(ex,200,gson.toJson(list)); return;
            }

            // create company, optional initial user in payload under 'initialUser'
            if ("POST".equalsIgnoreCase(method) && path.endsWith(suffix)){
                var body = gson.fromJson(new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8), java.util.Map.class);
                String nombre = (String) body.get("nombre"); String correo = (String) body.getOrDefault("correo", null); String telefono = (String) body.getOrDefault("telefono", null); String estado = (String) body.getOrDefault("estado", "ACTIVA");
                var ps = conn.prepareStatement("INSERT INTO Empresa (nombre, correo, telefono, estado) VALUES (?,?,?,?)", java.sql.PreparedStatement.RETURN_GENERATED_KEYS);
                ps.setString(1,nombre); ps.setString(2,correo); ps.setString(3,telefono); ps.setString(4,estado); ps.executeUpdate(); var rs = ps.getGeneratedKeys(); Integer id = null; if (rs.next()) id = rs.getInt(1);
                // optional initial user
                try {
                    Object iu = body.get("initialUser");
                    if (iu instanceof java.util.Map && id != null){ var um = (java.util.Map) iu; String uemail = String.valueOf(um.get("email")); String upass = um.containsKey("password") ? String.valueOf(um.get("password")) : "pass"; String uname = um.containsKey("name") ? String.valueOf(um.get("name")) : null; java.sql.PreparedStatement pus = conn.prepareStatement("INSERT INTO Usuario (correo,password,role,nickname,fecha_nacimiento,empresa_id) VALUES (?,?,?,?,?,?)", java.sql.PreparedStatement.RETURN_GENERATED_KEYS); pus.setString(1,uemail); pus.setString(2,upass); pus.setString(3,"EMPRESA"); pus.setString(4,uname); if (um.containsKey("birthDate")) { pus.setString(5,String.valueOf(um.get("birthDate"))); } else { pus.setNull(5, java.sql.Types.VARCHAR); } pus.setInt(6,id); pus.executeUpdate(); }
                } catch(Exception exx){ System.out.println("EmpresaHandler: could not create initial user: " + exx.getMessage()); }
                write(ex,201,gson.toJson(java.util.Map.of("id", id))); return;
            }

            // GET company by id with catalog
            if ("GET".equalsIgnoreCase(method) && path.contains(suffix + "/")){
                String idStr = path.substring(path.lastIndexOf('/')+1).split("/")[0]; Integer id = Integer.parseInt(idStr);
                var ps2 = conn.prepareStatement("SELECT id, nombre, correo, telefono, estado FROM Empresa WHERE id = ?"); ps2.setInt(1,id); var rs2 = ps2.executeQuery(); if (!rs2.next()){ write(ex,404,gson.toJson(java.util.Map.of("error","not_found"))); return; }
                var comp = new java.util.HashMap<String,Object>(); comp.put("id", rs2.getInt("id")); comp.put("nombre", rs2.getString("nombre")); comp.put("correo", rs2.getString("correo")); comp.put("telefono", rs2.getString("telefono")); comp.put("estado", rs2.getString("estado"));
                // catalog
                var rs3 = conn.createStatement().executeQuery("SELECT v.id, v.nombre, v.descripcion, v.precio, v.estado, v.edad_clasificacion FROM Videojuego v WHERE v.empresa_id = " + id);
                java.util.List<java.util.Map<String,Object>> catalog = new java.util.ArrayList<>(); while (rs3.next()){ var m = new java.util.HashMap<String,Object>(); m.put("id", rs3.getInt("id")); m.put("nombre", rs3.getString("nombre")); m.put("descripcion", rs3.getString("descripcion")); m.put("precio", rs3.getBigDecimal("precio")); m.put("estado", rs3.getString("estado")); m.put("edad_clasificacion", rs3.getString("edad_clasificacion")); catalog.add(m); }
                comp.put("catalog", catalog);
                write(ex,200,gson.toJson(comp)); return;
            }

            // manage company users: /api/empresa/{id}/usuarios
            if (path.contains(suffix + "/") && path.contains("/usuarios")){
                // extract first numeric segment after the suffix to be the empresa id
                String rem = path.substring(path.indexOf(suffix) + suffix.length()); // like /{id}/usuarios or /{id}/usuarios/{uid}
                String[] parts = rem.split("/");
                Integer eid = null;
                for (String p : parts) {
                    if (p == null || p.isEmpty()) continue;
                    try { eid = Integer.parseInt(p); break; } catch(NumberFormatException nfe) { /* not numeric - continue */ }
                }
                if (eid == null) { write(ex,400,gson.toJson(java.util.Map.of("error","company id missing or invalid"))); return; }

                if ("GET".equalsIgnoreCase(method)){
                    var rsu = conn.createStatement().executeQuery("SELECT id, correo, nickname, fecha_nacimiento, role FROM Usuario WHERE empresa_id = " + eid);
                    java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>(); while (rsu.next()){ var m = new java.util.HashMap<String,Object>(); m.put("id", rsu.getInt("id")); m.put("correo", rsu.getString("correo")); m.put("nickname", rsu.getString("nickname")); m.put("fecha_nacimiento", rsu.getString("fecha_nacimiento")); m.put("role", rsu.getString("role")); list.add(m); }
                    write(ex,200,gson.toJson(list)); return;
                }

                if ("POST".equalsIgnoreCase(method)){
                    var body = gson.fromJson(new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8), java.util.Map.class);
                    String uemail = String.valueOf(body.get("email")); String uname = body.containsKey("name") ? String.valueOf(body.get("name")) : null; String dob = body.containsKey("birthDate") ? String.valueOf(body.get("birthDate")) : null;
                    var pus = conn.prepareStatement("INSERT INTO Usuario (correo,password,role,nickname,fecha_nacimiento,empresa_id) VALUES (?,?,?,?,?,?)", java.sql.PreparedStatement.RETURN_GENERATED_KEYS);
                    pus.setString(1,uemail); pus.setString(2,"pass"); pus.setString(3,"EMPRESA"); pus.setString(4,uname); if (dob!=null) pus.setString(5,dob); else pus.setNull(5, java.sql.Types.VARCHAR); pus.setInt(6,eid); pus.executeUpdate(); var rsg = pus.getGeneratedKeys(); Integer uid = null; if (rsg.next()) uid = rsg.getInt(1);
                    write(ex,201,gson.toJson(java.util.Map.of("id", uid))); return;
                }

                // DELETE: expect URL like .../{id}/usuarios/{uid}
                if ("DELETE".equalsIgnoreCase(method)){
                    // find numeric uid after the empresa id
                    Integer uid = null;
                    boolean foundEid = false;
                    for (String p : parts) {
                        if (p == null || p.isEmpty()) continue;
                        if (!foundEid) {
                            try {
                                // parse numeric candidate and compare to the empresa id
                                Integer candidate = Integer.valueOf(p);
                                if (candidate.equals(eid)) { foundEid = true; }
                            } catch(NumberFormatException nfe) { }
                        } else {
                            try { uid = Integer.valueOf(p); break; } catch(NumberFormatException nfe) { }
                        }
                    }
                    if (uid == null) { write(ex,400,gson.toJson(java.util.Map.of("error","user id missing or invalid"))); return; }
                    var pdel = conn.prepareStatement("DELETE FROM Usuario WHERE id = ? AND empresa_id = ?"); pdel.setInt(1, uid); pdel.setInt(2, eid); int del = pdel.executeUpdate(); write(ex,200,gson.toJson(java.util.Map.of("deleted", del))); return;
                }
            }

            write(ex,405,gson.toJson(java.util.Map.of("error","method not allowed")));
        } catch (Exception e){ try { write(ex,500,gson.toJson(java.util.Map.of("error", e.getMessage()))); } catch(Exception exx){} }
    }

    private void write(HttpExchange ex, int status, String body) throws Exception { byte[] b = body.getBytes(StandardCharsets.UTF_8); ex.sendResponseHeaders(status, b.length); try (OutputStream os = ex.getResponseBody()){ os.write(b); } }
}
