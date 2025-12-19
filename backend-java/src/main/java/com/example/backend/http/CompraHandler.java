package com.example.backend.http;

import com.example.backend.db.DBConnection;
import com.example.backend.services.PurchaseService;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CompraHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final PurchaseService purchaseService = new PurchaseService();

    @Override
    public void handle(HttpExchange ex) {
        try {
            ex.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            String base = "/backend/api/compras";
            if ("POST".equalsIgnoreCase(method) && path.equals(base)){
                var body = gson.fromJson(new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8), java.util.Map.class);
                Integer usuarioId = ((Number)body.get("usuario_id")).intValue();
                Integer videojuegoId = ((Number)body.get("videojuego_id")).intValue();
                try (Connection conn = DBConnection.getConnection()){
                    conn.setAutoCommit(false);
                    try {
                        PreparedStatement ps = conn.prepareStatement("SELECT precio, empresa_id FROM Videojuego WHERE id = ? FOR UPDATE");
                        ps.setInt(1, videojuegoId); ResultSet rs = ps.executeQuery(); if (!rs.next()){ conn.rollback(); write(ex,400,gson.toJson(java.util.Collections.singletonMap("error","game not found"))); return; }
                        BigDecimal price = rs.getBigDecimal("precio"); Integer companyId = rs.getInt("empresa_id");
                        PreparedStatement psWallet = conn.prepareStatement("SELECT id, saldo FROM Cartera WHERE usuario_id = ? FOR UPDATE"); psWallet.setInt(1, usuarioId); ResultSet rsw = psWallet.executeQuery(); Integer walletId = null; BigDecimal saldo = BigDecimal.ZERO; if (rsw.next()){ walletId = rsw.getInt("id"); saldo = rsw.getBigDecimal("saldo"); }
                        if (saldo.compareTo(price) < 0){ conn.rollback(); write(ex,402,gson.toJson(java.util.Collections.singletonMap("error","insufficient_funds"))); return; }
                        BigDecimal globalPercent = new BigDecimal("15.00"); ResultSet rs2 = conn.createStatement().executeQuery("SELECT percent FROM Comision_Global LIMIT 1"); if (rs2.next()) globalPercent = rs2.getBigDecimal("percent");
                        PreparedStatement ps3 = conn.prepareStatement("SELECT percent FROM Comision_Empresa WHERE empresa_id = ?"); ps3.setInt(1, companyId); ResultSet rs3 = ps3.executeQuery(); BigDecimal companyPercent = null; if (rs3.next()) companyPercent = rs3.getBigDecimal("percent");
                        var fees = purchaseService.computeFees(price, globalPercent, companyPercent);
                        PreparedStatement ins = conn.prepareStatement("INSERT INTO Compra (usuario_id, videojuego_id, total, platform_commission, company_amount) VALUES (?,?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
                        ins.setInt(1, usuarioId); ins.setInt(2, videojuegoId); ins.setBigDecimal(3, price); ins.setBigDecimal(4, fees.platformFee()); ins.setBigDecimal(5, fees.companyAmount()); ins.executeUpdate(); ResultSet rki = ins.getGeneratedKeys(); Integer id = null; if (rki.next()) id = rki.getInt(1);
                        PreparedStatement upd = conn.prepareStatement("UPDATE Cartera SET saldo = saldo - ? WHERE id = ?"); upd.setBigDecimal(1, price); upd.setInt(2, walletId); upd.executeUpdate();
                        conn.commit(); write(ex,200,gson.toJson(java.util.Map.of("id", id, "total", price, "platform_fee", fees.platformFee(), "company_amount", fees.companyAmount()))); return;
                    } catch (Exception exx){ conn.rollback(); throw exx; } finally { conn.setAutoCommit(true); }
                }
            }
            write(ex,405,gson.toJson(java.util.Collections.singletonMap("error","method not allowed")));
        } catch (Exception e){ try { write(ex,500,gson.toJson(java.util.Collections.singletonMap("error", e.getMessage()))); } catch (Exception exx){} }
    }

    private void write(HttpExchange ex, int status, String body) throws Exception { byte[] b = body.getBytes(StandardCharsets.UTF_8); ex.sendResponseHeaders(status, b.length); try (OutputStream os = ex.getResponseBody()){ os.write(b); } }
}
