package com.moneymap.dao;

import com.moneymap.config.DBConnection;
import com.moneymap.model.Budget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BudgetDAO {

    public List<Budget> findByUserId(int userId) throws SQLException {
        List<Budget> list = new ArrayList<>();
        String sql = "SELECT b.budget_id, b.user_id, b.category_id, b.amount, b.start_date, b.end_date, b.created_at, " +
                     "COALESCE(c.name, 'Overall Budget') as category_name, " +
                     "COALESCE(c.icon, 'gauge-high') as category_icon, " +
                     "COALESCE(( " +
                     "   SELECT SUM(e.amount) FROM expense e " +
                     "   WHERE e.user_id = b.user_id " +
                     "     AND (b.category_id IS NULL OR e.category_id = b.category_id) " +
                     "     AND e.date >= b.start_date AND e.date <= b.end_date " +
                     "), 0.00) as spent_amount " +
                     "FROM budget b " +
                     "LEFT JOIN category c ON b.category_id = c.category_id " +
                     "WHERE b.user_id = ? " +
                     "ORDER BY b.start_date DESC, b.budget_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Budget b = new Budget(
                            rs.getInt("budget_id"),
                            rs.getInt("user_id"),
                            (Integer) rs.getObject("category_id"),
                            rs.getBigDecimal("amount"),
                            rs.getDate("start_date"),
                            rs.getDate("end_date"),
                            rs.getTimestamp("created_at")
                    );
                    b.setCategoryName(rs.getString("category_name"));
                    b.setCategoryIcon(rs.getString("category_icon"));
                    BigDecimal spent = rs.getBigDecimal("spent_amount");
                    b.setCurrentSpent(spent != null ? spent : BigDecimal.ZERO);
                    if (b.getAmount() != null && b.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                        double pct = (b.getCurrentSpent().doubleValue() / b.getAmount().doubleValue()) * 100.0;
                        b.setPercentageSpent(Math.round(pct * 10.0) / 10.0);
                    } else {
                        b.setPercentageSpent(0.0);
                    }
                    list.add(b);
                }
            }
        }
        return list;
    }

    public Budget upsert(Budget budget) throws SQLException {
        // Check if an existing budget exists for this user + category + dates
        String findSql = "SELECT budget_id FROM budget WHERE user_id = ? AND " +
                         "(category_id = ? OR (category_id IS NULL AND ? IS NULL)) AND " +
                         "start_date = ? AND end_date = ?";
        try (Connection conn = DBConnection.getConnection()) {
            Integer existingId = null;
            try (PreparedStatement psFind = conn.prepareStatement(findSql)) {
                psFind.setInt(1, budget.getUserId());
                if (budget.getCategoryId() != null) psFind.setInt(2, budget.getCategoryId()); else psFind.setNull(2, Types.INTEGER);
                if (budget.getCategoryId() != null) psFind.setInt(3, budget.getCategoryId()); else psFind.setNull(3, Types.INTEGER);
                psFind.setDate(4, budget.getStartDate());
                psFind.setDate(5, budget.getEndDate());
                try (ResultSet rs = psFind.executeQuery()) {
                    if (rs.next()) {
                        existingId = rs.getInt("budget_id");
                    }
                }
            }

            if (existingId != null) {
                String updateSql = "UPDATE budget SET amount = ? WHERE budget_id = ? RETURNING created_at";
                try (PreparedStatement psUp = conn.prepareStatement(updateSql)) {
                    psUp.setBigDecimal(1, budget.getAmount());
                    psUp.setInt(2, existingId);
                    try (ResultSet rs = psUp.executeQuery()) {
                        if (rs.next()) {
                            budget.setBudgetId(existingId);
                            budget.setCreatedAt(rs.getTimestamp("created_at"));
                            return budget;
                        }
                    }
                }
            } else {
                String insertSql = "INSERT INTO budget (user_id, category_id, amount, start_date, end_date) " +
                                   "VALUES (?, ?, ?, ?, ?) RETURNING budget_id, created_at";
                try (PreparedStatement psIn = conn.prepareStatement(insertSql)) {
                    psIn.setInt(1, budget.getUserId());
                    if (budget.getCategoryId() != null) psIn.setInt(2, budget.getCategoryId()); else psIn.setNull(2, Types.INTEGER);
                    psIn.setBigDecimal(3, budget.getAmount());
                    psIn.setDate(4, budget.getStartDate());
                    psIn.setDate(5, budget.getEndDate());
                    try (ResultSet rs = psIn.executeQuery()) {
                        if (rs.next()) {
                            budget.setBudgetId(rs.getInt("budget_id"));
                            budget.setCreatedAt(rs.getTimestamp("created_at"));
                            return budget;
                        }
                    }
                }
            }
        }
        return budget;
    }
}
