package com.example.backend.http;

import com.example.backend.db.DBConnection;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportHandler implements HttpHandler {
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange ex) {
        try (Connection conn = DBConnection.getConnection()){
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            System.out.println("ReportHandler: " + method + " " + path + " from " + ex.getRemoteAddress());
            ex.getResponseHeaders().set("Access-Control-Allow-Origin","*");
            ex.getResponseHeaders().set("Access-Control-Allow-Methods","GET,OPTIONS");
            ex.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type,Authorization");
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())){ sendEmpty(ex,204); return; }

            String path = ex.getRequestURI().getPath();
            String query = ex.getRequestURI().getQuery();
            if (path == null || !path.contains("/empresas/")) { sendJson(ex,400, Map.of("error","invalid_path")); return; }
            String rem = path.substring(path.indexOf("/empresas/") + "/empresas/".length());
            String[] parts = rem.split("/");
            Integer empresaId = null; try { empresaId = Integer.parseInt(parts[0]); } catch(Exception e){ sendJson(ex,400, Map.of("error","invalid_company_id")); return; }

            if (rem.contains("/reportes/ventas")){
                String from = param(query, "from"); String to = param(query, "to");
                try {
                    List<Map<String,Object>> rows = ventasByGame(conn, empresaId, from, to);
                    if (rows == null || rows.isEmpty()) { sendJson(ex,400, Map.of("error","no_purchases","msg","No hay ventas registradas para esta empresa")); return; }
                    try (InputStream is = ReportHandler.class.getResourceAsStream("/reports/sales_by_game.jrxml")){
                        if (is == null) { sendJson(ex,500, Map.of("error","report_template_missing")); return; }
                        JasperReport jr = JasperCompileManager.compileReport(is);
                        JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(rows);
                        Map<String,Object> params = new HashMap<>(); params.put("REPORT_TITLE", "Empresa ID: " + empresaId);
                        JasperPrint jp = JasperFillManager.fillReport(jr, params, ds);
                        byte[] pdf = JasperExportManager.exportReportToPdf(jp);
                        ex.getResponseHeaders().set("Content-Type","application/pdf"); ex.sendResponseHeaders(200, pdf.length);
                        try (OutputStream os = ex.getResponseBody()){ os.write(pdf); }
                        return;
                    }
                } catch(Exception e){ e.printStackTrace(); sendJson(ex,500, Map.of("error","report_error","msg", e.getMessage())); return; }
            }

            if (rem.contains("/reportes/feedback")){
                try{
                    List<Map<String,Object>> rows = feedbackReportData(conn, empresaId);
                    if (rows == null || rows.isEmpty()) { sendJson(ex,400, Map.of("error","no_feedback","msg","No hay comentarios/feedback para esta empresa")); return; }
                    try (InputStream is = ReportHandler.class.getResourceAsStream("/reports/feedback_report.jrxml")){
                        if (is == null) { sendJson(ex,500, Map.of("error","report_template_missing")); return; }
                        JasperReport jr = JasperCompileManager.compileReport(is);
                        JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(rows);
                        Map<String,Object> params = new HashMap<>(); params.put("REPORT_TITLE","Feedback — Empresa: " + empresaId);
                        JasperPrint jp = JasperFillManager.fillReport(jr, params, ds);
                        byte[] pdf = JasperExportManager.exportReportToPdf(jp);
                        ex.getResponseHeaders().set("Content-Type","application/pdf"); ex.sendResponseHeaders(200, pdf.length);
                        try (OutputStream os = ex.getResponseBody()){ os.write(pdf); }
                        return;
                    }
                }catch(Exception e){ e.printStackTrace(); sendJson(ex,500, Map.of("error","report_error","msg", e.getMessage())); return; }
            }

            if (rem.contains("/reportes/top5")){
                try{
                    List<Map<String,Object>> rows = ventasByGame(conn, empresaId, null, null);
                    if (rows == null || rows.isEmpty()) { sendJson(ex,400, Map.of("error","no_purchases","msg","No hay ventas registradas para esta empresa")); return; }
                    List<Map<String,Object>> top = rows.size() > 5 ? rows.subList(0, Math.min(5, rows.size())) : rows;
                    try (InputStream is = ReportHandler.class.getResourceAsStream("/reports/top5.jrxml")){
                        if (is == null) { sendJson(ex,500, Map.of("error","report_template_missing")); return; }
                        JasperReport jr = JasperCompileManager.compileReport(is);
                        JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(top);
                        Map<String,Object> params = new HashMap<>(); params.put("REPORT_TITLE","Top5 — Empresa: " + empresaId);
                        JasperPrint jp = JasperFillManager.fillReport(jr, params, ds);
                        byte[] pdf = JasperExportManager.exportReportToPdf(jp);
                        ex.getResponseHeaders().set("Content-Type","application/pdf"); ex.sendResponseHeaders(200, pdf.length);
                        try (OutputStream os = ex.getResponseBody()){ os.write(pdf); }
                        return;
                    }
                }catch(Exception e){ e.printStackTrace(); sendJson(ex,500, Map.of("error","report_error","msg", e.getMessage())); return; }
            }

            sendJson(ex,404, Map.of("error","not_found"));
        } catch (Exception e){ e.printStackTrace(); try { sendJson(ex,500, Map.of("error", e.getMessage())); } catch(Exception exx){} }
    }

    private static String param(String q, String name){ if(q==null) return null; for(String p: q.split("&")){ String[] kv = p.split("="); if(kv.length>0 && kv[0].equals(name)) return kv.length>1? kv[1] : null; } return null; }

    private List<Map<String,Object>> ventasByGame(Connection conn, int empresaId, String from, String to) throws Exception{
        String sql = "SELECT v.id, v.nombre AS nombre, COUNT(c.id) AS ventas, SUM(c.total) AS total, SUM(c.platform_commission) AS commission FROM Compra c JOIN Videojuego v ON c.videojuego_id = v.id WHERE v.empresa_id = ?";
        if (from != null && to != null) sql += " AND c.fecha BETWEEN ? AND ?";
        sql += " GROUP BY v.id, v.nombre ORDER BY SUM(c.total) DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, empresaId);
        if (from != null && to != null){ ps.setString(2, from); ps.setString(3, to); }
        ResultSet rs = ps.executeQuery();
        List<Map<String,Object>> out = new ArrayList<>();
        while (rs.next()){
            Map<String,Object> m = new HashMap<>();
            m.put("nombre", rs.getString("nombre"));
            m.put("ventas", rs.getInt("ventas"));
            BigDecimal total = rs.getBigDecimal("total") != null ? rs.getBigDecimal("total") : BigDecimal.ZERO;
            BigDecimal commission = rs.getBigDecimal("commission") != null ? rs.getBigDecimal("commission") : BigDecimal.ZERO;
            m.put("total", total);
            m.put("commission", commission.doubleValue());
            m.put("net", total.subtract(commission).doubleValue());
            out.add(m);
        }
        return out;
    }

    private List<Map<String,Object>> feedbackReportData(Connection conn, int empresaId) throws Exception{
        List<Map<String,Object>> rows = new ArrayList<>();
        String sqlAvg = "SELECT v.nombre AS nombre, AVG(c.puntuacion) AS avgRating, COUNT(c.id) AS cnt FROM Comentario c JOIN Videojuego v ON c.videojuego_id = v.id WHERE v.empresa_id = ? GROUP BY v.id, v.nombre ORDER BY AVG(c.puntuacion) DESC";
        PreparedStatement psAvg = conn.prepareStatement(sqlAvg);
        psAvg.setInt(1, empresaId);
        ResultSet rsa = psAvg.executeQuery();
        while (rsa.next()){
            Map<String,Object> m = new HashMap<>();
            m.put("section","AVG");
            m.put("nombre", rsa.getString("nombre"));
            m.put("avgRating", rsa.getDouble("avgRating"));
            m.put("count", rsa.getInt("cnt"));
            m.put("commentText", null);
            m.put("puntuacion", null);
            m.put("usuario", null);
            rows.add(m);
        }

        String sqlBest = "SELECT c.texto AS texto, c.puntuacion AS puntuacion, COALESCE(u.nickname, u.correo) AS usuario, v.nombre AS nombre FROM Comentario c JOIN Usuario u ON c.usuario_id = u.id JOIN Videojuego v ON c.videojuego_id = v.id WHERE v.empresa_id = ? ORDER BY c.puntuacion DESC, c.fecha DESC LIMIT 5";
        PreparedStatement psBest = conn.prepareStatement(sqlBest);
        psBest.setInt(1, empresaId);
        ResultSet rsb = psBest.executeQuery();
        while (rsb.next()){
            Map<String,Object> m = new HashMap<>();
            m.put("section","BEST");
            m.put("nombre", rsb.getString("nombre"));
            m.put("avgRating", null);
            m.put("count", null);
            m.put("commentText", rsb.getString("texto"));
            m.put("puntuacion", rsb.getInt("puntuacion"));
            m.put("usuario", rsb.getString("usuario"));
            rows.add(m);
        }

        String sqlWorst = "SELECT c.texto AS texto, c.puntuacion AS puntuacion, COALESCE(u.nickname, u.correo) AS usuario, v.nombre AS nombre FROM Comentario c JOIN Usuario u ON c.usuario_id = u.id JOIN Videojuego v ON c.videojuego_id = v.id WHERE v.empresa_id = ? ORDER BY c.puntuacion ASC, c.fecha ASC LIMIT 5";
        PreparedStatement psWorst = conn.prepareStatement(sqlWorst);
        psWorst.setInt(1, empresaId);
        ResultSet rsw = psWorst.executeQuery();
        while (rsw.next()){
            Map<String,Object> m = new HashMap<>();
            m.put("section","WORST");
            m.put("nombre", rsw.getString("nombre"));
            m.put("avgRating", null);
            m.put("count", null);
            m.put("commentText", rsw.getString("texto"));
            m.put("puntuacion", rsw.getInt("puntuacion"));
            m.put("usuario", rsw.getString("usuario"));
            rows.add(m);
        }
        return rows;
    }

    private void sendJson(HttpExchange ex, int code, Object obj) throws Exception{
        String s = gson.toJson(obj);
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type","application/json;charset=UTF-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()){ os.write(b); }
    }

    private void sendEmpty(HttpExchange ex, int code) throws Exception{ ex.sendResponseHeaders(code, -1); }
}
}
package com.example.backend.http;

import com.example.backend.db.DBConnection;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class ReportHandler implements HttpHandler {
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange ex) {
        // simple CORS + content negotiation
        try (Connection conn = DBConnection.getConnection()){
            ex.getResponseHeaders().set("Access-Control-Allow-Origin","*");
            ex.getResponseHeaders().set("Access-Control-Allow-Methods","GET,OPTIONS");
            ex.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type,Authorization");
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())){ sendEmpty(ex,204); return; }

            String path = ex.getRequestURI().getPath();
            String query = ex.getRequestURI().getQuery();
            // expected path like /backend/api/empresas/{id}/reportes/ventas
            if (path == null || !path.contains("/empresas/")) { sendJson(ex,400, Map.of("error","invalid_path")); return; }
            String rem = path.substring(path.indexOf("/empresas/") + "/empresas/".length()); // {id}/reportes/...
            String[] parts = rem.split("/");
            Integer empresaId = null; try { empresaId = Integer.parseInt(parts[0]); } catch(Exception e){ sendJson(ex,400, Map.of("error","invalid_company_id")); return; }

            // find report type
            if (rem.contains("/reportes/ventas")){
                // parse optional from/to from query
                String from = param(query, "from"); String to = param(query, "to");
                try {
                    List<Map<String,Object>> rows = ventasByGame(conn, empresaId, from, to);
                    if (rows == null || rows.isEmpty()) { sendJson(ex,400, Map.of("error","no_purchases","msg","No hay ventas registradas para esta empresa")); return; }
                    // compile jrxml from resources (per-game report)
                    InputStream is = ReportHandler.class.getResourceAsStream("/reports/sales_by_game.jrxml");
                    if (is == null) { sendJson(ex,500, Map.of("error","report_template_missing")); return; }
                    JasperReport jr = JasperCompileManager.compileReport(is);
                    JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(rows);
                    Map<String,Object> params = new HashMap<>(); params.put("REPORT_TITLE", "Empresa ID: " + empresaId);
                    JasperPrint jp = JasperFillManager.fillReport(jr, params, ds);
                    byte[] pdf = JasperExportManager.exportReportToPdf(jp);
                    ex.getResponseHeaders().set("Content-Type","application/pdf");
                    ex.sendResponseHeaders(200, pdf.length);
                    try (OutputStream os = ex.getResponseBody()){ os.write(pdf); }
                    return;
                } catch(Exception e){ e.printStackTrace(); sendJson(ex,500, Map.of("error","report_error","msg", e.getMessage())); return; }
            }

            if (rem.contains("/reportes/feedback")){
                try{
                    List<Map<String,Object>> rows = feedbackReportData(conn, empresaId);
                    if (rows == null || rows.isEmpty()) { sendJson(ex,400, Map.of("error","no_feedback","msg","No hay comentarios/feedback para esta empresa")); return; }
                    InputStream is = ReportHandler.class.getResourceAsStream("/reports/feedback_report.jrxml");
                    if (is == null) { sendJson(ex,500, Map.of("error","report_template_missing")); return; }
                    JasperReport jr = JasperCompileManager.compileReport(is);
                    JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(rows);
                    Map<String,Object> params = new HashMap<>(); params.put("REPORT_TITLE","Feedback — Empresa: " + empresaId);
                    JasperPrint jp = JasperFillManager.fillReport(jr, params, ds);
                    byte[] pdf = JasperExportManager.exportReportToPdf(jp);
                    ex.getResponseHeaders().set("Content-Type","application/pdf");
                    ex.sendResponseHeaders(200, pdf.length);
                    try (OutputStream os = ex.getResponseBody()){ os.write(pdf); }
                    return;
                }catch(Exception e){ e.printStackTrace(); sendJson(ex,500, Map.of("error","report_error","msg", e.getMessage())); return; }
            }

            if (rem.contains("/reportes/top5")){
                try{
                    List<Map<String,Object>> rows = ventasByGame(conn, empresaId, null, null);
                    if (rows == null || rows.isEmpty()) { sendJson(ex,400, Map.of("error","no_purchases","msg","No hay ventas registradas para esta empresa")); return; }
                    // take top 5
                    List<Map<String,Object>> top = rows.size() > 5 ? rows.subList(0, Math.min(5, rows.size())) : rows;
                    InputStream is = ReportHandler.class.getResourceAsStream("/reports/top5_report.jrxml");
                    if (is == null) { sendJson(ex,500, Map.of("error","report_template_missing")); return; }
                    JasperReport jr = JasperCompileManager.compileReport(is);
                    JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(top);
                    Map<String,Object> params = new HashMap<>(); params.put("REPORT_TITLE","Top5 — Empresa: " + empresaId);
                    JasperPrint jp = JasperFillManager.fillReport(jr, params, ds);
                    byte[] pdf = JasperExportManager.exportReportToPdf(jp);
                    ex.getResponseHeaders().set("Content-Type","application/pdf");
                    ex.sendResponseHeaders(200, pdf.length);
                    try (OutputStream os = ex.getResponseBody()){ os.write(pdf); }
                    return;
                }catch(Exception e){ e.printStackTrace(); sendJson(ex,500, Map.of("error","report_error","msg", e.getMessage())); return; }
            }

            sendJson(ex,404, Map.of("error","not_found"));
        } catch (Exception e){ e.printStackTrace(); try { sendJson(ex,500, Map.of("error", e.getMessage())); } catch(Exception exx){} }
    }

    private static String param(String q, String name){ if(q==null) return null; for(String p: q.split("&")){ String[] kv = p.split("="); if(kv.length>0 && kv[0].equals(name)) return kv.length>1? kv[1] : null; } return null; }

    private List<Map<String,Object>> ventasByGame(Connection conn, int empresaId, String from, String to) throws Exception{
        String sql = "SELECT v.id, v.nombre AS nombre, COUNT(c.id) AS qty, SUM(c.total) AS gross, SUM(c.platform_commission) AS commission FROM Compra c JOIN Videojuego v ON c.videojuego_id = v.id WHERE v.empresa_id = ?";
        if (from != null && to != null) sql += " AND c.fecha BETWEEN ? AND ?";
        sql += " GROUP BY v.id, v.nombre ORDER BY SUM(c.total) DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, empresaId);
        if (from != null && to != null){ ps.setString(2, from); ps.setString(3, to); }
        ResultSet rs = ps.executeQuery();
        List<Map<String,Object>> out = new ArrayList<>();
        while (rs.next()){
            Map<String,Object> m = new HashMap<>();
            m.put("nombre", rs.getString("nombre"));
            m.put("qty", rs.getInt("qty"));
            BigDecimal gross = rs.getBigDecimal("gross") != null ? rs.getBigDecimal("gross") : BigDecimal.ZERO;
            BigDecimal commission = rs.getBigDecimal("commission") != null ? rs.getBigDecimal("commission") : BigDecimal.ZERO;
            m.put("gross", gross.doubleValue());
            m.put("commission", commission.doubleValue());
            m.put("net", gross.subtract(commission).doubleValue());
            out.add(m);
        }
        return out;
    }

    private List<Map<String,Object>> feedbackReportData(Connection conn, int empresaId) throws Exception{
        List<Map<String,Object>> rows = new ArrayList<>();
        // 1) Avg rating per game
        String sqlAvg = "SELECT v.nombre AS nombre, AVG(c.puntuacion) AS avgRating, COUNT(c.id) AS cnt FROM Comentario c JOIN Videojuego v ON c.videojuego_id = v.id WHERE v.empresa_id = ? GROUP BY v.id, v.nombre ORDER BY AVG(c.puntuacion) DESC";
        PreparedStatement psAvg = conn.prepareStatement(sqlAvg);
        psAvg.setInt(1, empresaId);
        ResultSet rsa = psAvg.executeQuery();
        while (rsa.next()){
            Map<String,Object> m = new HashMap<>();
            m.put("section","AVG");
            m.put("nombre", rsa.getString("nombre"));
            m.put("avgRating", rsa.getDouble("avgRating"));
            m.put("count", rsa.getInt("cnt"));
            m.put("commentText", null);
            m.put("puntuacion", null);
            m.put("usuario", null);
            rows.add(m);
        }

        // 2) Best comments (top 5 by puntuacion)
        String sqlBest = "SELECT c.texto AS texto, c.puntuacion AS puntuacion, u.name AS usuario, v.nombre AS nombre FROM Comentario c JOIN Usuario u ON c.usuario_id = u.id JOIN Videojuego v ON c.videojuego_id = v.id WHERE v.empresa_id = ? ORDER BY c.puntuacion DESC, c.fecha DESC LIMIT 5";
        PreparedStatement psBest = conn.prepareStatement(sqlBest);
        psBest.setInt(1, empresaId);
        ResultSet rsb = psBest.executeQuery();
        while (rsb.next()){
            Map<String,Object> m = new HashMap<>();
            m.put("section","BEST");
            m.put("nombre", rsb.getString("nombre"));
            m.put("avgRating", null);
            m.put("count", null);
            m.put("commentText", rsb.getString("texto"));
            m.put("puntuacion", rsb.getInt("puntuacion"));
            m.put("usuario", rsb.getString("usuario"));
            rows.add(m);
        }

        // 3) Worst comments (bottom 5 by puntuacion)
        String sqlWorst = "SELECT c.texto AS texto, c.puntuacion AS puntuacion, u.name AS usuario, v.nombre AS nombre FROM Comentario c JOIN Usuario u ON c.usuario_id = u.id JOIN Videojuego v ON c.videojuego_id = v.id WHERE v.empresa_id = ? ORDER BY c.puntuacion ASC, c.fecha ASC LIMIT 5";
        PreparedStatement psWorst = conn.prepareStatement(sqlWorst);
        psWorst.setInt(1, empresaId);
        ResultSet rsw = psWorst.executeQuery();
        while (rsw.next()){
            Map<String,Object> m = new HashMap<>();
            m.put("section","WORST");
            m.put("nombre", rsw.getString("nombre"));
            m.put("avgRating", null);
            m.put("count", null);
            m.put("commentText", rsw.getString("texto"));
            m.put("puntuacion", rsw.getInt("puntuacion"));
            m.put("usuario", rsw.getString("usuario"));
            rows.add(m);
        }
        return rows;
    }

    private void sendJson(HttpExchange ex, int code, Object obj) throws Exception{
        String s = gson.toJson(obj);
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type","application/json;charset=UTF-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()){ os.write(b); }
    }

    private void sendEmpty(HttpExchange ex, int code) throws Exception{ ex.sendResponseHeaders(code, -1); }
}
