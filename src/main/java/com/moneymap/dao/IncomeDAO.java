package com.moneymap.dao;

import com.moneymap.config.DBConnection;
import com.moneymap.model.Category;
import com.moneymap.model.Income;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IncomeDAO {
    private final CategoryDAO categoryDAO = new CategoryDAO();

    public List<Income> findByUserId(int userId, String startDate, String endDate, Integer categoryId) throws SQLException {
        List<Income> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT i.income_id, i.user_id, i.account_id, i.category_id, i.amount, i.source, i.date, i.description, i.created_at, " +
                "COALESCE(c.name, 'Other Income') as category_name, COALESCE(c.icon, 'wallet') as category_icon, b.bank_name " +
                "FROM income i " +
                "LEFT JOIN category c ON i.category_id = c.category_id " +
                "LEFT JOIN bank_account b ON i.account_id = b.account_id " +
                "WHERE i.user_id = ? "
        );

        if (startDate != null && !startDate.isEmpty()) sql.append("AND i.date >= ?::date ");
        if (endDate != null && !endDate.isEmpty()) sql.append("AND i.date <= ?::date ");
        if (categoryId != null && categoryId > 0) sql.append("AND i.category_id = ? ");
        sql.append("ORDER BY i.date DESC, i.income_id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            ps.setInt(paramIndex++, userId);
            if (startDate != null && !startDate.isEmpty()) ps.setString(paramIndex++, startDate);
            if (endDate != null && !endDate.isEmpty()) ps.setString(paramIndex++, endDate);
            if (categoryId != null && categoryId > 0) ps.setInt(paramIndex++, categoryId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Income inc = new Income(
                            rs.getInt("income_id"),
                            rs.getInt("user_id"),
                            (Integer) rs.getObject("account_id"),
                            (Integer) rs.getObject("category_id"),
                            rs.getBigDecimal("amount"),
                            rs.getString("source"),
                            rs.getDate("date"),
                            rs.getString("description"),
                            rs.getTimestamp("created_at")
                    );
                    inc.setCategoryName(rs.getString("category_name"));
                    inc.setCategoryIcon(rs.getString("category_icon"));
                    inc.setBankName(rs.getString("bank_name"));
                    list.add(inc);
                }
            }
        }
        return list;
    }

    public Income create(Income inc) throws SQLException {
        // Resolve categoryId if null but categoryName is provided
        if (inc.getCategoryId() == null && inc.getCategoryName() != null && !inc.getCategoryName().isEmpty()) {
            Category c = categoryDAO.findByNameAndType(inc.getCategoryName(), "income");
            if (c != null) {
                inc.setCategoryId(c.getCategoryId());
            }
        }

        String sql = "INSERT INTO income (user_id, account_id, category_id, amount, source, date, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING income_id, created_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, inc.getUserId());
            if (inc.getAccountId() != null) ps.setInt(2, inc.getAccountId()); else ps.setNull(2, Types.INTEGER);
            if (inc.getCategoryId() != null) ps.setInt(3, inc.getCategoryId()); else ps.setNull(3, Types.INTEGER);
            ps.setBigDecimal(4, inc.getAmount());
            ps.setString(5, inc.getSource() != null ? inc.getSource() : "Salary");
            ps.setDate(6, inc.getDate() != null ? inc.getDate() : new Date(System.currentTimeMillis()));
            ps.setString(7, inc.getDescription() != null ? inc.getDescription() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    inc.setIncomeId(rs.getInt("income_id"));
                    inc.setCreatedAt(rs.getTimestamp("created_at"));
                    return inc;
                }
            }
        }
        return inc;
    }

    public boolean delete(int incomeId, int userId) throws SQLException {
        String sql = "DELETE FROM income WHERE income_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, incomeId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }
}
