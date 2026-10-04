package com.moneymap.servlet;

import com.moneymap.dao.BudgetDAO;
import com.moneymap.dao.CategoryDAO;
import com.moneymap.model.Budget;
import com.moneymap.model.Category;
import com.moneymap.service.BudgetService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/api/budgets/*")
public class BudgetServlet extends BaseServlet {
    private final BudgetDAO budgetDAO = new BudgetDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final BudgetService budgetService = new BudgetService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);

        try {
            List<Budget> list = budgetDAO.findByUserId(userId);
            sendSuccess(resp, list);
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);

        try {
            Budget budget = readJson(req, Budget.class);
            if (budget == null || budget.getAmount() == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Budget amount is required.");
                return;
            }

            // Resolve category by name if provided and categoryId is null
            if (budget.getCategoryId() == null && budget.getCategoryName() != null && !budget.getCategoryName().isEmpty() && !budget.getCategoryName().equalsIgnoreCase("Overall Budget")) {
                Category c = categoryDAO.findByNameAndType(budget.getCategoryName(), "expense");
                if (c != null) {
                    budget.setCategoryId(c.getCategoryId());
                }
            }

            // If start/end date missing, default to current month
            if (budget.getStartDate() == null) {
                LocalDate start = LocalDate.now().withDayOfMonth(1);
                budget.setStartDate(Date.valueOf(start));
            }
            if (budget.getEndDate() == null) {
                LocalDate now = LocalDate.now();
                LocalDate end = now.withDayOfMonth(now.lengthOfMonth());
                budget.setEndDate(Date.valueOf(end));
            }

            budget.setUserId(userId);
            Budget saved = budgetDAO.upsert(budget);

            // Re-fetch to populate computed fields
            List<Budget> allBudgets = budgetDAO.findByUserId(userId);
            Budget populated = saved;
            for (Budget b : allBudgets) {
                if (b.getBudgetId() == saved.getBudgetId()) {
                    populated = b;
                    break;
                }
            }

            // Check thresholds and notify if already exceeded/approaching
            budgetService.checkBudgetThresholds(userId);

            sendSuccess(resp, "Budget saved successfully", populated);
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
