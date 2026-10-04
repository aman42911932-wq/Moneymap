package com.moneymap.servlet;

import com.moneymap.dao.CategoryDAO;
import com.moneymap.dao.RecurringExpenseDAO;
import com.moneymap.model.Category;
import com.moneymap.model.RecurringExpense;
import com.moneymap.service.BudgetService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/api/recurring/*")
public class RecurringServlet extends BaseServlet {
    private final RecurringExpenseDAO recurringDAO = new RecurringExpenseDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final BudgetService budgetService = new BudgetService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);

        try {
            List<RecurringExpense> list = recurringDAO.findByUserId(userId);
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
            RecurringExpense rec = readJson(req, RecurringExpense.class);
            if (rec == null || rec.getTitle() == null || rec.getAmount() == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Title and amount are required.");
                return;
            }

            // Resolve category by name if provided and categoryId is null
            if (rec.getCategoryId() == null && rec.getCategoryName() != null && !rec.getCategoryName().isEmpty()) {
                Category c = categoryDAO.findByNameAndType(rec.getCategoryName(), "expense");
                if (c != null) {
                    rec.setCategoryId(c.getCategoryId());
                }
            }

            if (rec.getStartDate() == null) {
                rec.setStartDate(Date.valueOf(LocalDate.now()));
            }
            if (rec.getNextDueDate() == null) {
                rec.setNextDueDate(Date.valueOf(LocalDate.now().plusMonths(1)));
            }

            rec.setUserId(userId);
            RecurringExpense created = recurringDAO.create(rec);

            // Trigger budget threshold notifications
            budgetService.checkBudgetThresholds(userId);

            sendSuccess(resp, "Recurring commitment added successfully", created);
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
