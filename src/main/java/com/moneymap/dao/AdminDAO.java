package com.moneymap.dao;

import com.moneymap.config.DBConnection;
import com.moneymap.model.Admin;

import java.sql.*;

public class AdminDAO {

    public Admin findByEmail(String email) throws SQLException {
        String sql = "SELECT admin_id, name, email, password_hash, role, created_at FROM admin WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    private Admin mapRow(ResultSet rs) throws SQLException {
        return new Admin(
                rs.getInt("admin_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("role"),
                rs.getTimestamp("created_at")
        );
    }
}
