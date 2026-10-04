package com.moneymap.dao;

import com.moneymap.config.DBConnection;
import com.moneymap.model.RecurringExpense;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecurringExpenseDAO {

    public List<RecurringExpense> findByUserId(int userId) throws SQLException {
        List<RecurringExpense> list = new ArrayList<>();
        String sql = "SELECT r.recurring_id, r.user_id, r.category_id, r.title, r.amount, r.frequency, r.start_date, r.next_due_date, r.status, r.created_at, " +
                     "c.name as category_name, c.icon as category_icon " +
                     "FROM recurring_expense r " +
                     "LEFT JOIN category c ON r.category_id = c.category_id " +
                     "WHERE r.user_id = ? ORDER BY r.next_due_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RecurringExpense r = new RecurringExpense(
                            rs.getInt("recurring_id"),
                            rs.getInt("user_id"),
                            (Integer) rs.getObject("category_id"),
                            rs.getString("title"),
                            rs.getBigDecimal("amount"),
                            rs.getString("frequency"),
                            rs.getDate("start_date"),
                            rs.getDate("next_due_date"),
                            rs.getString("status"),
                            rs.getTimestamp("created_at")
                    );
                    r.setCategoryName(rs.getString("category_name"));
                    r.setCategoryIcon(rs.getString("category_icon"));
                    list.add(r);
                }
            }
        }
        return list;
    }

    public RecurringExpense create(RecurringExpense rec) throws SQLException {
        String sql = "INSERT INTO recurring_expense (user_id, category_id, title, amount, frequency, start_date, next_due_date, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING recurring_id, created_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rec.getUserId());
            if (rec.getCategoryId() != null) ps.setInt(2, rec.getCategoryId()); else ps.setNull(2, Types.INTEGER);
            ps.setString(3, rec.getTitle());
            ps.setBigDecimal(4, rec.getAmount());
            ps.setString(5, rec.getFrequency() != null ? rec.getFrequency() : "Monthly");
            ps.setDate(6, rec.getStartDate());
            ps.setDate(7, rec.getNextDueDate());
            ps.setString(8, rec.getStatus() != null ? rec.getStatus() : "Active");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    rec.setRecurringId(rs.getInt("recurring_id"));
                    rec.setCreatedAt(rs.getTimestamp("created_at"));
                    return rec;
                }
            }
        }
        return rec;
    }
}
