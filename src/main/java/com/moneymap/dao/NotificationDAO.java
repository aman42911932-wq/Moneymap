package com.moneymap.dao;

import com.moneymap.config.DBConnection;
import com.moneymap.model.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public List<Notification> findByUserId(int userId) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT notification_id, user_id, message, type, is_read, created_at " +
                     "FROM notification WHERE user_id = ? ORDER BY created_at DESC LIMIT 50";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public int getUnreadCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public Notification create(Notification notif) throws SQLException {
        String sql = "INSERT INTO notification (user_id, message, type, is_read) VALUES (?, ?, ?, ?) RETURNING notification_id, created_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notif.getUserId());
            ps.setString(2, notif.getMessage());
            ps.setString(3, notif.getType() != null ? notif.getType() : "budget_alert");
            ps.setBoolean(4, false);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    notif.setNotificationId(rs.getInt("notification_id"));
                    notif.setCreatedAt(rs.getTimestamp("created_at"));
                    return notif;
                }
            }
        }
        return notif;
    }

    public boolean markRead(int notificationId, int userId) throws SQLException {
        String sql = "UPDATE notification SET is_read = TRUE WHERE notification_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        return new Notification(
                rs.getInt("notification_id"),
                rs.getInt("user_id"),
                rs.getString("message"),
                rs.getString("type"),
                rs.getBoolean("is_read"),
                rs.getTimestamp("created_at")
        );
    }
}
