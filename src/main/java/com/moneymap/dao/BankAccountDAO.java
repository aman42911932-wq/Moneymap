package com.moneymap.dao;

import com.moneymap.config.DBConnection;
import com.moneymap.model.BankAccount;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BankAccountDAO {

    public List<BankAccount> findByUserId(int userId) throws SQLException {
        List<BankAccount> list = new ArrayList<>();
        String sql = "SELECT b.account_id, b.user_id, b.bank_name, b.account_holder_name, b.account_last4, b.ifsc_code, " +
                     "b.account_type, b.opening_balance, b.is_primary, b.is_active, b.created_at, " +
                     "(b.opening_balance + " +
                     " COALESCE((SELECT SUM(i.amount) FROM income i WHERE i.account_id = b.account_id), 0.00) - " +
                     " COALESCE((SELECT SUM(e.amount) FROM expense e WHERE e.account_id = b.account_id), 0.00)" +
                     ") AS current_balance " +
                     "FROM bank_account b WHERE b.user_id = ? AND b.is_active = TRUE ORDER BY b.is_primary DESC, b.account_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BankAccount acc = mapRow(rs);
                    BigDecimal curBal = rs.getBigDecimal("current_balance");
                    acc.setCurrentBalance(curBal != null ? curBal : acc.getOpeningBalance());
                    list.add(acc);
                }
            }
        }
        return list;
    }

    public BankAccount findById(int accountId) throws SQLException {
        String sql = "SELECT b.account_id, b.user_id, b.bank_name, b.account_holder_name, b.account_last4, b.ifsc_code, " +
                     "b.account_type, b.opening_balance, b.is_primary, b.is_active, b.created_at, " +
                     "(b.opening_balance + " +
                     " COALESCE((SELECT SUM(i.amount) FROM income i WHERE i.account_id = b.account_id), 0.00) - " +
                     " COALESCE((SELECT SUM(e.amount) FROM expense e WHERE e.account_id = b.account_id), 0.00)" +
                     ") AS current_balance " +
                     "FROM bank_account b WHERE b.account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BankAccount acc = mapRow(rs);
                    BigDecimal curBal = rs.getBigDecimal("current_balance");
                    acc.setCurrentBalance(curBal != null ? curBal : acc.getOpeningBalance());
                    return acc;
                }
            }
        }
        return null;
    }

    public BankAccount create(BankAccount account) throws SQLException {
        String sql = "INSERT INTO bank_account (user_id, bank_name, account_holder_name, account_last4, ifsc_code, account_type, opening_balance, is_primary, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING account_id, created_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, account.getUserId());
            ps.setString(2, account.getBankName());
            ps.setString(3, account.getAccountHolderName());
            ps.setString(4, account.getAccountLast4());
            ps.setString(5, account.getIfscCode());
            ps.setString(6, account.getAccountType() != null ? account.getAccountType() : "Savings");
            ps.setBigDecimal(7, account.getOpeningBalance() != null ? account.getOpeningBalance() : BigDecimal.ZERO);
            ps.setBoolean(8, account.isPrimary());
            ps.setBoolean(9, true);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    account.setAccountId(rs.getInt("account_id"));
                    account.setCreatedAt(rs.getTimestamp("created_at"));
                    account.setCurrentBalance(account.getOpeningBalance());
                    return account;
                }
            }
        }
        return account;
    }

    public boolean setPrimary(int userId, int accountId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String resetSql = "UPDATE bank_account SET is_primary = FALSE WHERE user_id = ?";
                try (PreparedStatement ps1 = conn.prepareStatement(resetSql)) {
                    ps1.setInt(1, userId);
                    ps1.executeUpdate();
                }

                String setSql = "UPDATE bank_account SET is_primary = TRUE WHERE user_id = ? AND account_id = ?";
                try (PreparedStatement ps2 = conn.prepareStatement(setSql)) {
                    ps2.setInt(1, userId);
                    ps2.setInt(2, accountId);
                    ps2.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private BankAccount mapRow(ResultSet rs) throws SQLException {
        return new BankAccount(
                rs.getInt("account_id"),
                rs.getInt("user_id"),
                rs.getString("bank_name"),
                rs.getString("account_holder_name"),
                rs.getString("account_last4"),
                rs.getString("ifsc_code"),
                rs.getString("account_type"),
                rs.getBigDecimal("opening_balance"),
                rs.getBoolean("is_primary"),
                rs.getBoolean("is_active"),
                rs.getTimestamp("created_at")
        );
    }
}
