package com.example.backend.servlets;

import com.example.backend.db.DBConnection;
import com.example.backend.services.PurchaseService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "CompraServlet", urlPatterns = {"/api/compras/*"})
public class CompraServlet extends BaseServlet {
    private final PurchaseService purchaseService = new PurchaseService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCors(resp);
        try {
            var body = gson.fromJson(req.getReader(), java.util.Map.class);
            Integer usuarioId = ((Number)body.get("usuario_id")).intValue();
            Integer videojuegoId = ((Number)body.get("videojuego_id")).intValue();
            try (Connection conn = DBConnection.getConnection()){
                conn.setAutoCommit(false);
                try {
                    PreparedStatement ps = conn.prepareStatement("SELECT precio, empresa_id FROM Videojuego WHERE id = ? FOR UPDATE");
                    ps.setInt(1, videojuegoId);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()){ conn.rollback(); resp.setStatus(400); writeJson(resp, java.util.Map.of("error","game not found")); return; }
                    BigDecimal price = rs.getBigDecimal("precio");
                    Integer companyId = rs.getInt("empresa_id");

                    // cargar usuario con fecha de nacimiento y cartera
                    PreparedStatement psUser = conn.prepareStatement("SELECT fecha_nacimiento FROM Usuario WHERE id = ?"); psUser.setInt(1, usuarioId);
                    ResultSet rsu = psUser.executeQuery(); java.sql.Date birth = null; if (rsu.next()) birth = rsu.getDate("fecha_nacimiento");

                    PreparedStatement psWallet = conn.prepareStatement("SELECT id, saldo FROM Cartera WHERE usuario_id = ? FOR UPDATE"); psWallet.setInt(1, usuarioId);
                    ResultSet rsw = psWallet.executeQuery(); Integer walletId = null; BigDecimal saldo = BigDecimal.ZERO; if (rsw.next()){ walletId = rsw.getInt("id"); saldo = rsw.getBigDecimal("saldo"); }

                    // validar edad si se proporciona fecha de nacimiento; mapa de clasificaciones a edad mínima
                    PreparedStatement psRating = conn.prepareStatement("SELECT edad_clasificacion FROM Videojuego WHERE id = ?"); psRating.setInt(1, videojuegoId); ResultSet rsr = psRating.executeQuery(); String rating = null; if (rsr.next()) rating = rsr.getString("edad_clasificacion");
                    int requiredAge = 0;
                    if (rating != null){
                        switch (rating) {
                            case "T": requiredAge = 13; break;
                            case "M": requiredAge = 18; break;
                            default: requiredAge = 0; break;
                        }
                    }
                    if (birth != null && requiredAge > 0){
                        java.time.Period p = java.time.Period.between(birth.toLocalDate(), java.time.LocalDate.now());
                        if (p.getYears() < requiredAge){ conn.rollback(); resp.setStatus(403); writeJson(resp, java.util.Map.of("error","age_restriction")); return; }
                    }

                    // cargar porcentajes de comisión
                    BigDecimal globalPercent = new BigDecimal("15.00");
                    ResultSet rs2 = conn.createStatement().executeQuery("SELECT percent FROM Comision_Global LIMIT 1"); if (rs2.next()) globalPercent = rs2.getBigDecimal("percent");
                    PreparedStatement ps3 = conn.prepareStatement("SELECT percent FROM Comision_Empresa WHERE empresa_id = ?"); ps3.setInt(1, companyId); ResultSet rs3 = ps3.executeQuery(); BigDecimal companyPercent = null; if (rs3.next()) companyPercent = rs3.getBigDecimal("percent");

                    // calcular comisiones
                    var fees = purchaseService.computeFees(price, globalPercent, companyPercent);

                    // verificar saldo de cartera
                    if (saldo.compareTo(price) < 0){ conn.rollback(); resp.setStatus(402); writeJson(resp, java.util.Map.of("error","insufficient_funds")); return; }

                    // insertar compra
                    PreparedStatement ins = conn.prepareStatement("INSERT INTO Compra (usuario_id, videojuego_id, total, platform_commission, company_amount) VALUES (?,?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
                    ins.setInt(1, usuarioId); ins.setInt(2, videojuegoId); ins.setBigDecimal(3, price); ins.setBigDecimal(4, fees.platformFee()); ins.setBigDecimal(5, fees.companyAmount());
                    ins.executeUpdate(); ResultSet rki = ins.getGeneratedKeys(); Integer id = null; if (rki.next()) id = rki.getInt(1);

                    // actualizar cartera
                    PreparedStatement upd = conn.prepareStatement("UPDATE Cartera SET saldo = saldo - ? WHERE id = ?"); upd.setBigDecimal(1, price); upd.setInt(2, walletId); upd.executeUpdate();

                    conn.commit();
                    writeJson(resp, java.util.Map.of("id", id, "total", price, "platform_fee", fees.platformFee(), "company_amount", fees.companyAmount()));
                    return;
                } catch (Exception ex){ conn.rollback(); throw ex; }
                finally { conn.setAutoCommit(true); }
            }
        } catch (Exception e){ resp.setStatus(500); writeJson(resp, java.util.Map.of("error", e.getMessage())); }
    }
}
