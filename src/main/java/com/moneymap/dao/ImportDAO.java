package com.moneymap.dao;

import com.moneymap.config.DBConnection;
import com.moneymap.model.Category;
import com.moneymap.model.Import;
import com.moneymap.model.ImportTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ImportDAO {
    private final CategoryDAO categoryDAO = new CategoryDAO();

    public Import createImport(Import imp) throws SQLException {
        String sql = "INSERT INTO import (user_id, account_id, file_name, file_type, status) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING import_id, upload_date";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, imp.getUserId());
            if (imp.getAccountId() != null) ps.setInt(2, imp.getAccountId()); else ps.setNull(2, Types.INTEGER);
            ps.setString(3, imp.getFileName());
            ps.setString(4, imp.getFileType() != null ? imp.getFileType() : "CSV");
            ps.setString(5, "pending");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    imp.setImportId(rs.getInt("import_id"));
                    imp.setUploadDate(rs.getTimestamp("upload_date"));
                    return imp;
                }
            }
        }
        return imp;
    }

    public void createTransactionsBatch(List<ImportTransaction> txns) throws SQLException {
        String sql = "INSERT INTO import_transaction (import_id, user_id, date, description, amount, txn_type, suggested_category, final_category_id, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (ImportTransaction tx : txns) {
                ps.setInt(1, tx.getImportId());
                ps.setInt(2, tx.getUserId());
                ps.setDate(3, tx.getDate());
                ps.setString(4, tx.getDescription());
                ps.setBigDecimal(5, tx.getAmount());
                ps.setString(6, tx.getTxnType());
                ps.setString(7, tx.getSuggestedCategory());
                if (tx.getFinalCategoryId() != null) ps.setInt(8, tx.getFinalCategoryId()); else ps.setNull(8, Types.INTEGER);
                ps.setString(9, "pending");
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<ImportTransaction> getTransactionsByImportId(int importId) throws SQLException {
        List<ImportTransaction> list = new ArrayList<>();
        String sql = "SELECT t.transaction_id, t.import_id, t.user_id, t.date, t.description, t.amount, t.txn_type, " +
                     "COALESCE(c.name, t.suggested_category) as suggested_category, t.final_category_id, t.status " +
                     "FROM import_transaction t " +
                     "LEFT JOIN category c ON t.final_category_id = c.category_id " +
                     "WHERE t.import_id = ? ORDER BY t.transaction_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, importId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ImportTransaction(
                            rs.getInt("transaction_id"),
                            rs.getInt("import_id"),
                            rs.getInt("user_id"),
                            rs.getDate("date"),
                            rs.getString("description"),
                            rs.getBigDecimal("amount"),
                            rs.getString("txn_type"),
                            rs.getString("suggested_category"),
                            (Integer) rs.getObject("final_category_id"),
                            rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    public boolean updateCategory(int transactionId, int categoryId, int userId) throws SQLException {
        String sql = "UPDATE import_transaction SET final_category_id = ? WHERE transaction_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ps.setInt(2, transactionId);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteTransaction(int transactionId, int userId) throws SQLException {
        String sql = "DELETE FROM import_transaction WHERE transaction_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean confirmImport(int importId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Verify import belongs to user and is currently pending
                Integer accountId = null;
                String status = null;
                String accSql = "SELECT account_id, status FROM import WHERE import_id = ? AND user_id = ?";
                try (PreparedStatement psAcc = conn.prepareStatement(accSql)) {
                    psAcc.setInt(1, importId);
                    psAcc.setInt(2, userId);
                    try (ResultSet rsAcc = psAcc.executeQuery()) {
                        if (rsAcc.next()) {
                            accountId = (Integer) rsAcc.getObject("account_id");
                            status = rsAcc.getString("status");
                        } else {
                            conn.rollback();
                            return false; // Not found or unauthorized
                        }
                    }
                }

                if ("completed".equalsIgnoreCase(status)) {
                    conn.rollback();
                    return false; // Already confirmed
                }

                // Transfer surviving pending rows to expense or income
                String txSql = "SELECT * FROM import_transaction WHERE import_id = ? AND status = 'pending'";
                try (PreparedStatement psTx = conn.prepareStatement(txSql)) {
                    psTx.setInt(1, importId);
                    try (ResultSet rsTx = psTx.executeQuery()) {
                        while (rsTx.next()) {
                            String type = rsTx.getString("txn_type");
                            Date date = rsTx.getDate("date");
                            String desc = rsTx.getString("description");
                            java.math.BigDecimal amount = rsTx.getBigDecimal("amount");
                            Integer catId = (Integer) rsTx.getObject("final_category_id");
                            String suggested = rsTx.getString("suggested_category");

                            // Look up category by name and type if catId is null
                            if (catId == null && suggested != null && !suggested.isEmpty()) {
                                String expectedType = "credit".equalsIgnoreCase(type) ? "income" : "expense";
                                Category c = categoryDAO.findByNameAndType(suggested, expectedType);
                                if (c != null) catId = c.getCategoryId();
                            }

                            if ("debit".equalsIgnoreCase(type)) {
                                String expSql = "INSERT INTO expense (user_id, account_id, category_id, amount, date, description) VALUES (?, ?, ?, ?, ?, ?)";
                                try (PreparedStatement psExp = conn.prepareStatement(expSql)) {
                                    psExp.setInt(1, userId);
                                    if (accountId != null) psExp.setInt(2, accountId); else psExp.setNull(2, Types.INTEGER);
                                    if (catId != null) psExp.setInt(3, catId); else psExp.setNull(3, Types.INTEGER);
                                    psExp.setBigDecimal(4, amount);
                                    psExp.setDate(5, date);
                                    psExp.setString(6, desc);
                                    psExp.executeUpdate();
                                }
                            } else {
                                String incSql = "INSERT INTO income (user_id, account_id, category_id, amount, source, date, description) VALUES (?, ?, ?, ?, ?, ?, ?)";
                                try (PreparedStatement psInc = conn.prepareStatement(incSql)) {
                                    psInc.setInt(1, userId);
                                    if (accountId != null) psInc.setInt(2, accountId); else psInc.setNull(2, Types.INTEGER);
                                    if (catId != null) psInc.setInt(3, catId); else psInc.setNull(3, Types.INTEGER);
                                    psInc.setBigDecimal(4, amount);
                                    psInc.setString(5, desc);
                                    psInc.setDate(6, date);
                                    psInc.setString(7, desc);
                                    psInc.executeUpdate();
                                }
                            }
                        }
                    }
                }

                // Mark import and import_transaction as completed
                String updateImp = "UPDATE import SET status = 'completed' WHERE import_id = ?";
                try (PreparedStatement psU = conn.prepareStatement(updateImp)) {
                    psU.setInt(1, importId);
                    psU.executeUpdate();
                }

                String updateTx = "UPDATE import_transaction SET status = 'completed' WHERE import_id = ?";
                try (PreparedStatement psU2 = conn.prepareStatement(updateTx)) {
                    psU2.setInt(1, importId);
                    psU2.executeUpdate();
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
}
