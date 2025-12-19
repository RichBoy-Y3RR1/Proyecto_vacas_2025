package com.example.backend.integration;

import com.example.backend.services.PurchaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class PurchaseFlowIntegrationTest {
    private Connection conn;

    @BeforeEach
    public void setup() throws Exception {
        conn = DriverManager.getConnection("jdbc:h2:mem:tienda;DB_CLOSE_DELAY=-1");
        try (Statement st = conn.createStatement()){
           
            st.execute("CREATE TABLE Usuario (id INT PRIMARY KEY AUTO_INCREMENT, correo VARCHAR(150), password VARCHAR(255), role VARCHAR(20), estado VARCHAR(20), fecha_nacimiento DATE);");
            st.execute("CREATE TABLE Empresa (id INT PRIMARY KEY AUTO_INCREMENT, nombre VARCHAR(150));");
            st.execute("CREATE TABLE Videojuego (id INT PRIMARY KEY AUTO_INCREMENT, nombre VARCHAR(150), empresa_id INT, precio DECIMAL(10,2), edad_clasificacion VARCHAR(10));");
            st.execute("CREATE TABLE Cartera (id INT PRIMARY KEY AUTO_INCREMENT, usuario_id INT, saldo DECIMAL(12,2));");
            st.execute("CREATE TABLE Compra (id INT PRIMARY KEY AUTO_INCREMENT, usuario_id INT, videojuego_id INT, total DECIMAL(10,2), platform_commission DECIMAL(10,2), company_amount DECIMAL(10,2));");
            st.execute("CREATE TABLE Comision_Global (id INT PRIMARY KEY, percent DECIMAL(5,2));");
            st.execute("CREATE TABLE Comision_Empresa (empresa_id INT PRIMARY KEY, percent DECIMAL(5,2));");

            
            st.execute("INSERT INTO Usuario (id, correo, password, role, estado, fecha_nacimiento) VALUES (1,'user@cliente.com','pwd','USUARIO','ACTIVA','2000-01-01');");
            st.execute("INSERT INTO Empresa (id, nombre) VALUES (1,'Acme Games');");
            st.execute("INSERT INTO Videojuego (id, nombre, empresa_id, precio, edad_clasificacion) VALUES (1,'Space Shooter',1,9.99,'E');");
            st.execute("INSERT INTO Cartera (id, usuario_id, saldo) VALUES (1,1,100.00);");
            st.execute("INSERT INTO Comision_Global (id, percent) VALUES (1,15.00);");
        }
    }

    @AfterEach
    public void teardown() throws Exception { conn.close(); }

    @Test
    public void testFullPurchaseFlow() throws Exception {
       
        PurchaseService svc = new PurchaseService();
        BigDecimal price = new BigDecimal("9.99");
        BigDecimal global = new BigDecimal("15.00");
        PurchaseService.Fees fees = svc.computeFees(price, global, null);
        assertEquals(new BigDecimal("1.4985").setScale(4), fees.platformFee().setScale(4));
        assertEquals(new BigDecimal("8.4915").setScale(4), fees.companyAmount().setScale(4));

        //inserts iniciales
        conn.setAutoCommit(false);
        try {
            try (java.sql.PreparedStatement ps = conn.prepareStatement("INSERT INTO Compra (usuario_id, videojuego_id, total, platform_commission, company_amount) VALUES (?,?,?,?,?)", java.sql.PreparedStatement.RETURN_GENERATED_KEYS)){
                ps.setInt(1,1); ps.setInt(2,1); ps.setBigDecimal(3, price); ps.setBigDecimal(4, fees.platformFee()); ps.setBigDecimal(5, fees.companyAmount());
                ps.executeUpdate();
            }
            try (java.sql.PreparedStatement upd = conn.prepareStatement("UPDATE Cartera SET saldo = saldo - ? WHERE usuario_id = ?")){
                upd.setBigDecimal(1, price); upd.setInt(2,1); upd.executeUpdate();
            }
            conn.commit();
        } finally { conn.setAutoCommit(true); }
       
        try (java.sql.ResultSet rs = conn.createStatement().executeQuery("SELECT saldo FROM Cartera WHERE usuario_id = 1")){
            assertTrue(rs.next()); assertEquals(new BigDecimal("90.01").setScale(2), rs.getBigDecimal(1).setScale(2));
        }
        try (java.sql.ResultSet rs2 = conn.createStatement().executeQuery("SELECT COUNT(*) FROM Compra")){
            assertTrue(rs2.next()); assertEquals(1, rs2.getInt(1));
        }
    }
}
