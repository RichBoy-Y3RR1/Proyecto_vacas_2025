package com.example.backend.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.sql.Connection;
import com.example.backend.db.DBConnection;

@WebServlet(name = "ComisionServlet", urlPatterns = {"/api/comisiones/*"})
public class ComisionServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (Connection conn = DBConnection.getConnection()){
            java.util.Map<String,Object> out = new java.util.HashMap<>();
            // global
            var rs = conn.createStatement().executeQuery("SELECT percent FROM Comision_Global LIMIT 1");
            if (rs.next()) out.put("global", rs.getBigDecimal("percent")); else out.put("global", null);
            // per-company
            var per = conn.createStatement().executeQuery("SELECT empresa_id, percent FROM Comision_Empresa");
            java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>();
            while (per.next()){
                java.util.Map<String,Object> m = new java.util.HashMap<>();
                m.put("empresa_id", per.getInt("empresa_id")); m.put("percent", per.getBigDecimal("percent"));
                list.add(m);
            }
            out.put("companies", list);
            writeJson(resp, out);
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        // Only ADMIN can change commissions
        Integer tokenUser = (Integer) req.getAttribute("userId");
        String tokenRole = (String) req.getAttribute("role");
        if (tokenUser == null) { resp.setStatus(401); writeJson(resp, java.util.Map.of("error","login_required")); return; }
        if (tokenRole == null || !tokenRole.equalsIgnoreCase("ADMIN")){ resp.setStatus(403); writeJson(resp, java.util.Map.of("error","forbidden")); return; }
        try (Connection conn = DBConnection.getConnection()){
            var body = gson.fromJson(req.getReader(), java.util.Map.class);
            // set global: { global: 12.5 } or { globalPercent: 12.5 }
            Object g = body.get("global"); if (g==null) g = body.get("globalPercent");
            if (g != null){ java.math.BigDecimal p = new java.math.BigDecimal(String.valueOf(g));
                // upsert into Comision_Global with id=1
                var ps = conn.prepareStatement("REPLACE INTO Comision_Global (id, percent, updated_at) VALUES (1, ?, CURRENT_TIMESTAMP)");
                ps.setBigDecimal(1, p); int u = ps.executeUpdate(); writeJson(resp, java.util.Map.of("updated_global", u)); return;
            }
            // set company percent: { empresa_id: 1, percent: 10.0 }
            Object eid = body.get("empresa_id"); Object pc = body.get("percent");
            if (eid != null && pc != null){ int empresaId = (int) ((Number) eid).intValue(); java.math.BigDecimal p = new java.math.BigDecimal(String.valueOf(pc));
                var ps2 = conn.prepareStatement("REPLACE INTO Comision_Empresa (empresa_id, percent) VALUES (?,?)"); ps2.setInt(1, empresaId); ps2.setBigDecimal(2, p); int u = ps2.executeUpdate(); writeJson(resp, java.util.Map.of("updated_company", u)); return;
            }
            resp.setStatus(400); writeJson(resp, java.util.Map.of("error","invalid payload"));
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }
}
