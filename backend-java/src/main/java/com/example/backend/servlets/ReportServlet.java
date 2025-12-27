package com.example.backend.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.sql.Connection;
import com.example.backend.db.DBConnection;

@WebServlet(name = "ReportServlet", urlPatterns = {"/api/reportes/*"})
public class ReportServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (Connection conn = DBConnection.getConnection()){
            String format = req.getParameter("format");
            if (format != null && format.equalsIgnoreCase("pdf")) {
                // Generate Jasper PDF using the JDBC connection and JRXML in resources
                try (java.io.InputStream jr = getClass().getClassLoader().getResourceAsStream("reports/sales_report.jrxml")){
                    if (jr == null) throw new RuntimeException("Report template not found");
                    net.sf.jasperreports.engine.JasperReport jasper = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jr);
                    java.util.Map<String,Object> params = new java.util.HashMap<>();
                    net.sf.jasperreports.engine.JasperPrint jasperPrint = net.sf.jasperreports.engine.JasperFillManager.fillReport(jasper, params, conn);
                    resp.setContentType("application/pdf");
                    resp.setHeader("Content-Disposition", "inline; filename=report.pdf");
                    try (java.io.OutputStream out = resp.getOutputStream()) {
                        net.sf.jasperreports.engine.JasperExportManager.exportReportToPdfStream(jasperPrint, out);
                        out.flush();
                    }
                    return;
                }
            }

            // default JSON report (existing behavior)
            var rs = conn.createStatement().executeQuery("SELECT SUM(total) AS total_revenue, SUM(platform_commission) AS total_commission FROM Compra");
            java.util.Map<String,Object> out = new java.util.HashMap<>();
            if (rs.next()){
                out.put("total_revenue", rs.getBigDecimal("total_revenue"));
                out.put("total_commission", rs.getBigDecimal("total_commission"));
            }
            // por empresa
            var per = conn.createStatement().executeQuery("SELECT e.id, e.nombre, SUM(c.total) AS ventas, SUM(c.platform_commission) AS comision FROM Compra c JOIN Videojuego v ON c.videojuego_id = v.id JOIN Empresa e ON v.empresa_id = e.id GROUP BY e.id, e.nombre");
            java.util.List<java.util.Map<String,Object>> companies = new java.util.ArrayList<>();
            while (per.next()){
                java.util.Map<String,Object> m = new java.util.HashMap<>();
                m.put("company_id", per.getInt("id")); m.put("company_name", per.getString("nombre")); m.put("ventas", per.getBigDecimal("ventas")); m.put("comision", per.getBigDecimal("comision"));
                companies.add(m);
            }
            out.put("by_company", companies);
            writeJson(resp, out);
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }
}
