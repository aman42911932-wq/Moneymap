package com.moneymap.dao;

import com.moneymap.config.DBConnection;
import com.moneymap.model.SavingsGoal;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SavingsGoalDAO {

    public List<SavingsGoal> findByUserId(int userId) throws SQLException {
        List<SavingsGoal> list = new ArrayList<>();
        String sql = "SELECT goal_id, user_id, goal_name, target_amount, current_amount, target_date, status, created_at " +
                     "FROM savings_goal WHERE user_id = ? ORDER BY goal_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SavingsGoal g = new SavingsGoal(
                            rs.getInt("goal_id"),
                            rs.getInt("user_id"),
                            rs.getString("goal_name"),
                            rs.getBigDecimal("target_amount"),
                            rs.getBigDecimal("current_amount"),
                            rs.getDate("target_date"),
                            rs.getString("status"),
                            rs.getTimestamp("created_at")
                    );
                    if (g.getTargetAmount() != null && g.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
                        g.setPercentage(Math.min(100.0, (g.getCurrentAmount().doubleValue() / g.getTargetAmount().doubleValue()) * 100.0));
                    }
                    list.add(g);
                }
            }
        }
        return list;
    }

    public SavingsGoal create(SavingsGoal goal) throws SQLException {
        String sql = "INSERT INTO savings_goal (user_id, goal_name, target_amount, current_amount, target_date, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?) RETURNING goal_id, created_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, goal.getUserId());
            ps.setString(2, goal.getGoalName());
            ps.setBigDecimal(3, goal.getTargetAmount());
            ps.setBigDecimal(4, goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO);
            ps.setDate(5, goal.getTargetDate());
            ps.setString(6, goal.getStatus() != null ? goal.getStatus() : "In Progress");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    goal.setGoalId(rs.getInt("goal_id"));
                    goal.setCreatedAt(rs.getTimestamp("created_at"));
                    return goal;
                }
            }
        }
        return goal;
    }

    public boolean addContribution(int goalId, BigDecimal amount) throws SQLException {
        String sql = "UPDATE savings_goal SET current_amount = current_amount + ?, " +
                     "status = CASE WHEN current_amount + ? >= target_amount THEN 'Completed' ELSE 'In Progress' END " +
                     "WHERE goal_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setBigDecimal(2, amount);
            ps.setInt(3, goalId);
            return ps.executeUpdate() > 0;
        }
    }
}
