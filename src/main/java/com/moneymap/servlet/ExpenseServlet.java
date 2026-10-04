package com.moneymap.servlet;

import com.moneymap.dao.ExpenseDAO;
import com.moneymap.model.Expense;
import com.moneymap.service.BudgetService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/expenses/*")
public class ExpenseServlet extends BaseServlet {
    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final BudgetService budgetService = new BudgetService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);

        String startDate = req.getParameter("startDate");
        String endDate = req.getParameter("endDate");
        String catIdParam = req.getParameter("categoryId");
        Integer categoryId = (catIdParam != null && !catIdParam.isEmpty()) ? Integer.parseInt(catIdParam) : null;

        try {
            List<Expense> list = expenseDAO.findByUserId(userId, startDate, endDate, categoryId);
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
            Expense exp = readJson(req, Expense.class);
            if (exp == null || exp.getAmount() == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid expense data.");
                return;
            }
            exp.setUserId(userId);
            Expense created = expenseDAO.create(exp);

            // Check budget thresholds and send alerts
            budgetService.checkBudgetThresholds(userId);

            sendSuccess(resp, "Expense added successfully", created);
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);

        String pathInfo = req.getPathInfo();
        String idParam = req.getParameter("id");
        int expenseId = -1;

        if (pathInfo != null && pathInfo.length() > 1) {
            try {
                expenseId = Integer.parseInt(pathInfo.substring(1));
            } catch (Exception ignored) {}
        }
        if (expenseId == -1 && idParam != null && !idParam.isEmpty()) {
            try {
                expenseId = Integer.parseInt(idParam);
            } catch (Exception ignored) {}
        }

        if (expenseId == -1) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Expense ID is required.");
            return;
        }

        try {
            boolean ok = expenseDAO.delete(expenseId, userId);
            if (ok) {
                sendSuccess(resp, "Expense deleted successfully", null);
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Expense not found or not owned by user.");
            }
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
