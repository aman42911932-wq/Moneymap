package com.moneymap.service;

import com.moneymap.dao.BudgetDAO;
import com.moneymap.dao.NotificationDAO;
import com.moneymap.model.Budget;
import com.moneymap.model.Notification;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class BudgetService {
    private final BudgetDAO budgetDAO = new BudgetDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    public void checkBudgetThresholds(int userId) {
        try {
            List<Budget> budgets = budgetDAO.findByUserId(userId);
            List<Notification> existingNotifs = notificationDAO.findByUserId(userId);

            for (Budget b : budgets) {
                if (b.getAmount() == null || b.getAmount().compareTo(BigDecimal.ZERO) <= 0) continue;
                double pct = b.getPercentageSpent();
                String catName = b.getCategoryName() != null ? b.getCategoryName() : "Overall";

                if (pct >= 100.0) {
                    String msg = String.format("🚨 Budget Alert: You have exceeded 100%% of your %s budget! (Spent: ₹%.2f / Limit: ₹%.2f)", catName, b.getCurrentSpent().doubleValue(), b.getAmount().doubleValue());
                    if (!hasRecentNotification(existingNotifs, msg)) {
                        Notification n = new Notification();
                        n.setUserId(userId);
                        n.setMessage(msg);
                        n.setType("budget_exceeded");
                        notificationDAO.create(n);
                    }
                } else if (pct >= 80.0) {
                    String msg = String.format("⚠️ Budget Warning: You have reached %.1f%% of your %s budget. (Spent: ₹%.2f / Limit: ₹%.2f)", pct, catName, b.getCurrentSpent().doubleValue(), b.getAmount().doubleValue());
                    if (!hasRecentNotification(existingNotifs, msg)) {
                        Notification n = new Notification();
                        n.setUserId(userId);
                        n.setMessage(msg);
                        n.setType("budget_warning");
                        notificationDAO.create(n);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[BudgetService] Error checking budget thresholds: " + e.getMessage());
        }
    }

    private boolean hasRecentNotification(List<Notification> notifs, String prefix) {
        for (Notification n : notifs) {
            if (n.getMessage() != null && n.getMessage().startsWith(prefix.substring(0, Math.min(25, prefix.length())))) {
                return true;
            }
        }
        return false;
    }
}
