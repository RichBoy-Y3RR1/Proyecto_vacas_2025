package com.example.backend.http;

import com.example.backend.db.DBConnection;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ComisionHandler implements HttpHandler {
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange ex) {
        try (java.sql.Connection conn = DBConnection.getConnection()){
            ex.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if (path.endsWith("/") && path.length()>1) path = path.substring(0, path.length()-1);
            String[] suffixes = new String[]{"/api/comision","/api/comisiones"};

            if ("OPTIONS".equalsIgnoreCase(method)) { ex.getResponseHeaders().set("Access-Control-Allow-Methods","GET,POST,PUT,DELETE,OPTIONS"); ex.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type,Authorization"); write(ex,204,""); return; }

            // GET -> return global percent and per-company percents
            if ("GET".equalsIgnoreCase(method) && (path.endsWith(suffixes[0]) || path.endsWith(suffixes[1]))){
                java.math.BigDecimal global = new java.math.BigDecimal("0");
                try (var rs = conn.createStatement().executeQuery("SELECT percent FROM Comision_Global WHERE id = 1")){ if (rs.next()) global = rs.getBigDecimal("percent"); }
                var rs2 = conn.createStatement().executeQuery("SELECT empresa_id, percent FROM Comision_Empresa");
                java.util.List<java.util.Map<String,Object>> empresas = new java.util.ArrayList<>();
                while (rs2.next()){ var m = new java.util.HashMap<String,Object>(); m.put("empresaId", rs2.getInt("empresa_id")); m.put("percent", rs2.getBigDecimal("percent")); empresas.add(m); }
                write(ex,200,gson.toJson(java.util.Map.of("global", global, "empresas", empresas))); return;
            }

            // PUT -> update global or company-specific commission
            if ("PUT".equalsIgnoreCase(method) && (path.endsWith(suffixes[0]) || path.endsWith(suffixes[1]) || path.contains(suffixes[0] + "/empresa/") || path.contains(suffixes[1] + "/empresa/"))){
                var body = gson.fromJson(new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8), java.util.Map.class);
                // obtain current global
                java.math.BigDecimal currentGlobal = new java.math.BigDecimal("0");
                try (var rs = conn.createStatement().executeQuery("SELECT percent FROM Comision_Global WHERE id = 1")){ if (rs.next()) currentGlobal = rs.getBigDecimal("percent"); }

                // If path contains /empresa/{id} treat as company update
                Integer pathEmpresaId = null;
                if (path.contains("/empresa/")){
                    String idStr = path.substring(path.indexOf("/empresa/") + "/empresa/".length());
                    try { pathEmpresaId = Integer.valueOf(idStr.split("/")[0]); } catch(Exception exx){ pathEmpresaId = null; }
                }

                // prefer company id from path, otherwise from body (accept empresa_id or empresaId)
                Integer empresaId = pathEmpresaId != null ? pathEmpresaId : (body.get("empresaId") instanceof Number ? ((Number)body.get("empresaId")).intValue() : (body.get("empresa_id") instanceof Number ? ((Number)body.get("empresa_id")).intValue() : null));

                // support frontend keys: globalPercent for global, empresa_id for company id
                boolean hasGlobalKey = body.containsKey("global") || body.containsKey("globalPercent") || (body.containsKey("percent") && empresaId==null);
                if (hasGlobalKey){
                    // update global percent
                    String val = body.containsKey("global") ? String.valueOf(body.get("global")) : (body.containsKey("globalPercent") ? String.valueOf(body.get("globalPercent")) : String.valueOf(body.get("percent")));
                    java.math.BigDecimal newGlobal = new java.math.BigDecimal(val);
                    var ps = conn.prepareStatement("UPDATE Comision_Global SET percent = ? WHERE id = 1"); ps.setBigDecimal(1, newGlobal); int updated = ps.executeUpdate();
                    // if decreased, clamp company percents
                    if (newGlobal.compareTo(currentGlobal) < 0){ var ps2 = conn.prepareStatement("UPDATE Comision_Empresa SET percent = ? WHERE percent > ?"); ps2.setBigDecimal(1, newGlobal); ps2.setBigDecimal(2, newGlobal); ps2.executeUpdate(); }
                    write(ex,200,gson.toJson(java.util.Map.of("updated", updated))); return;
                }

                if (empresaId != null){
                    // validate empresa exists
                    try (var prs = conn.prepareStatement("SELECT id FROM Empresa WHERE id = ?")){ prs.setInt(1, empresaId); var r = prs.executeQuery(); if (!r.next()){ write(ex,404,gson.toJson(java.util.Map.of("error","company_not_found"))); return; } }
                    java.math.BigDecimal newPct = body.get("percent") instanceof Number ? new java.math.BigDecimal(((Number)body.get("percent")).toString()) : new java.math.BigDecimal(String.valueOf(body.getOrDefault("percent","0")));
                    // enforce not exceeding global
                    java.math.BigDecimal currentG = currentGlobal;
                    if (newPct.compareTo(currentG) > 0){ write(ex,400,gson.toJson(java.util.Map.of("error","percent_exceeds_global","global", currentG))); return; }
                    // try update, else insert
                    var ups = conn.prepareStatement("UPDATE Comision_Empresa SET percent = ? WHERE empresa_id = ?"); ups.setBigDecimal(1, newPct); ups.setInt(2, empresaId); int u = ups.executeUpdate(); if (u==0){ var ins = conn.prepareStatement("INSERT INTO Comision_Empresa (empresa_id, percent) VALUES (?,?)"); ins.setInt(1, empresaId); ins.setBigDecimal(2, newPct); ins.executeUpdate(); }
                    write(ex,200,gson.toJson(java.util.Map.of("empresaId", empresaId, "percent", newPct))); return;
                }

                write(ex,400,gson.toJson(java.util.Map.of("error","missing_parameters")));
                return;
            }

            write(ex,405,gson.toJson(java.util.Map.of("error","method not allowed")));
        } catch (Exception e){ try { write(ex,500,gson.toJson(java.util.Map.of("error", e.getMessage()))); } catch(Exception exx){} }
    }

    private void write(HttpExchange ex, int status, String body) throws Exception { byte[] b = body.getBytes(StandardCharsets.UTF_8); ex.sendResponseHeaders(status, b.length); try (OutputStream os = ex.getResponseBody()){ os.write(b); } }
}
