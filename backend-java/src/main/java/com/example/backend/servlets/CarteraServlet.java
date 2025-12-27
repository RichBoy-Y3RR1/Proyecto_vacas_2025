package com.example.backend.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.sql.Connection;
import com.example.backend.db.DBConnection;
import java.math.BigDecimal;

@WebServlet(name = "CarteraServlet", urlPatterns = {"/api/cartera/*"})
public class CarteraServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (Connection conn = DBConnection.getConnection()){
            Integer usuarioId = null;
            String path = req.getPathInfo();
            if (path != null && path.length() > 1){ try { usuarioId = Integer.valueOf(path.substring(1)); } catch(Exception ex){} }
            if (usuarioId == null){ String q = req.getParameter("usuario_id"); if (q!=null) try { usuarioId = Integer.valueOf(q); } catch(Exception ex){} }
            if (usuarioId == null){ resp.setStatus(400); writeJson(resp, java.util.Map.of("error","missing_usuario_id")); return; }

            var ps = conn.prepareStatement("SELECT id, saldo FROM Cartera WHERE usuario_id = ?"); ps.setInt(1, usuarioId);
            var rs = ps.executeQuery();
            if (rs.next()){
                java.util.Map<String,Object> out = new java.util.HashMap<>();
                out.put("id", rs.getInt("id")); out.put("usuario_id", usuarioId); out.put("saldo", rs.getBigDecimal("saldo"));
                writeJson(resp, out);
            } else {
                // no wallet yet
                writeJson(resp, java.util.Map.of("id", null, "usuario_id", usuarioId, "saldo", new BigDecimal("0.00")));
            }
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try (Connection conn = DBConnection.getConnection()){
            var body = gson.fromJson(req.getReader(), java.util.Map.class);
            Integer usuarioId = ((Number)body.get("usuario_id")).intValue();
            Object amtObj = body.get("amount");
            if (usuarioId == null || amtObj == null){ resp.setStatus(400); writeJson(resp, java.util.Map.of("error","usuario_id and amount required")); return; }
            // require token user matches usuarioId or admin
            Integer tokenUser = (Integer) req.getAttribute("userId");
            String tokenRole = (String) req.getAttribute("role");
            if (tokenUser == null) { writeError(resp, 401, "login_required", "Authentication required"); return; }
            if (!tokenUser.equals(usuarioId) && (tokenRole == null || !tokenRole.equalsIgnoreCase("ADMIN"))){ writeError(resp, 403, "forbidden", "Not allowed to top-up this wallet"); return; }
            BigDecimal amount = new BigDecimal(String.valueOf(amtObj));
            if (amount.compareTo(BigDecimal.ZERO) <= 0){ resp.setStatus(400); writeJson(resp, java.util.Map.of("error","amount_must_be_positive")); return; }

            // check existing wallet
            var ps = conn.prepareStatement("SELECT id, saldo FROM Cartera WHERE usuario_id = ? FOR UPDATE"); ps.setInt(1, usuarioId);
            var rs = ps.executeQuery();
            if (rs.next()){
                int id = rs.getInt("id"); java.math.BigDecimal saldo = rs.getBigDecimal("saldo");
                var upd = conn.prepareStatement("UPDATE Cartera SET saldo = saldo + ? WHERE id = ?"); upd.setBigDecimal(1, amount); upd.setInt(2, id); upd.executeUpdate();
                var ps2 = conn.prepareStatement("SELECT saldo FROM Cartera WHERE id = ?"); ps2.setInt(1, id); var rs2 = ps2.executeQuery(); if (rs2.next()){ writeJson(resp, java.util.Map.of("id", id, "usuario_id", usuarioId, "saldo", rs2.getBigDecimal("saldo"))); } else { writeJson(resp, java.util.Map.of("error","unexpected")); }
            } else {
                var ins = conn.prepareStatement("INSERT INTO Cartera (usuario_id, saldo) VALUES (?,?)", java.sql.PreparedStatement.RETURN_GENERATED_KEYS);
                ins.setInt(1, usuarioId); ins.setBigDecimal(2, amount); ins.executeUpdate(); var rki = ins.getGeneratedKeys(); Integer id = null; if (rki.next()) id = rki.getInt(1);
                writeJson(resp, java.util.Map.of("id", id, "usuario_id", usuarioId, "saldo", amount));
            }
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }
}
