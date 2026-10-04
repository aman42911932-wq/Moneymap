package com.moneymap.dao;

import com.moneymap.config.DBConnection;
import com.moneymap.model.Category;
import com.moneymap.model.Expense;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDAO {
    private final CategoryDAO categoryDAO = new CategoryDAO();

    public List<Expense> findByUserId(int userId, String startDate, String endDate, Integer categoryId) throws SQLException {
        List<Expense> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT e.expense_id, e.user_id, e.account_id, e.category_id, e.amount, e.date, e.time, e.merchant_place, " +
                "e.payment_method, e.description, e.created_at, " +
                "COALESCE(c.name, 'Miscellaneous') as category_name, COALESCE(c.icon, 'tag') as category_icon, b.bank_name " +
                "FROM expense e " +
                "LEFT JOIN category c ON e.category_id = c.category_id " +
                "LEFT JOIN bank_account b ON e.account_id = b.account_id " +
                "WHERE e.user_id = ? "
        );

        if (startDate != null && !startDate.isEmpty()) sql.append("AND e.date >= ?::date ");
        if (endDate != null && !endDate.isEmpty()) sql.append("AND e.date <= ?::date ");
        if (categoryId != null && categoryId > 0) sql.append("AND e.category_id = ? ");
        sql.append("ORDER BY e.date DESC, e.expense_id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            ps.setInt(paramIndex++, userId);
            if (startDate != null && !startDate.isEmpty()) ps.setString(paramIndex++, startDate);
            if (endDate != null && !endDate.isEmpty()) ps.setString(paramIndex++, endDate);
            if (categoryId != null && categoryId > 0) ps.setInt(paramIndex++, categoryId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Expense exp = new Expense(
                            rs.getInt("expense_id"),
                            rs.getInt("user_id"),
                            (Integer) rs.getObject("account_id"),
                            (Integer) rs.getObject("category_id"),
                            rs.getBigDecimal("amount"),
                            rs.getDate("date"),
                            rs.getTime("time"),
                            rs.getString("merchant_place"),
                            rs.getString("payment_method"),
                            rs.getString("description"),
                            rs.getTimestamp("created_at")
                    );
                    exp.setCategoryName(rs.getString("category_name"));
                    exp.setCategoryIcon(rs.getString("category_icon"));
                    exp.setBankName(rs.getString("bank_name"));
                    list.add(exp);
                }
            }
        }
        return list;
    }

    public Expense create(Expense exp) throws SQLException {
        // Resolve categoryId if null but categoryName is provided
        if (exp.getCategoryId() == null && exp.getCategoryName() != null && !exp.getCategoryName().isEmpty()) {
            Category c = categoryDAO.findByNameAndType(exp.getCategoryName(), "expense");
            if (c != null) {
                exp.setCategoryId(c.getCategoryId());
            }
        }

        String sql = "INSERT INTO expense (user_id, account_id, category_id, amount, date, time, merchant_place, payment_method, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING expense_id, created_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, exp.getUserId());
            if (exp.getAccountId() != null) ps.setInt(2, exp.getAccountId()); else ps.setNull(2, Types.INTEGER);
            if (exp.getCategoryId() != null) ps.setInt(3, exp.getCategoryId()); else ps.setNull(3, Types.INTEGER);
            ps.setBigDecimal(4, exp.getAmount());
            ps.setDate(5, exp.getDate() != null ? exp.getDate() : new Date(System.currentTimeMillis()));
            ps.setTime(6, exp.getTime() != null ? exp.getTime() : new Time(System.currentTimeMillis()));
            ps.setString(7, exp.getMerchantPlace() != null ? exp.getMerchantPlace() : "");
            ps.setString(8, exp.getPaymentMethod() != null ? exp.getPaymentMethod() : "UPI");
            ps.setString(9, exp.getDescription() != null ? exp.getDescription() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    exp.setExpenseId(rs.getInt("expense_id"));
                    exp.setCreatedAt(rs.getTimestamp("created_at"));
                    return exp;
                }
            }
        }
        return exp;
    }

    public boolean delete(int expenseId, int userId) throws SQLException {
        String sql = "DELETE FROM expense WHERE expense_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, expenseId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }
}
