package com.example.backend.dao;

import com.example.backend.db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RevokedTokenDAO {
    public void revoke(String jti) throws Exception {
        try (Connection conn = DBConnection.getConnection()){
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Revoked_Token (jti VARCHAR(128) PRIMARY KEY, revoked_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            PreparedStatement ps = conn.prepareStatement("REPLACE INTO Revoked_Token (jti, revoked_at) VALUES (?, CURRENT_TIMESTAMP)");
            ps.setString(1, jti);
            ps.executeUpdate();
        }
    }

    public boolean isRevoked(String jti) throws Exception {
        try (Connection conn = DBConnection.getConnection()){
            var ps = conn.prepareStatement("SELECT jti FROM Revoked_Token WHERE jti = ? LIMIT 1");
            ps.setString(1, jti);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }
}
