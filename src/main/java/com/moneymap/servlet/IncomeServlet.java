package com.moneymap.servlet;

import com.moneymap.dao.IncomeDAO;
import com.moneymap.model.Income;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/income/*")
public class IncomeServlet extends BaseServlet {
    private final IncomeDAO incomeDAO = new IncomeDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);

        String startDate = req.getParameter("startDate");
        String endDate = req.getParameter("endDate");
        String catIdParam = req.getParameter("categoryId");
        Integer categoryId = (catIdParam != null && !catIdParam.isEmpty()) ? Integer.parseInt(catIdParam) : null;

        try {
            List<Income> list = incomeDAO.findByUserId(userId, startDate, endDate, categoryId);
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
            Income inc = readJson(req, Income.class);
            if (inc == null || inc.getAmount() == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid income data.");
                return;
            }
            inc.setUserId(userId);
            Income created = incomeDAO.create(inc);
            sendSuccess(resp, "Income added successfully", created);
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
        int incomeId = -1;

        if (pathInfo != null && pathInfo.length() > 1) {
            try {
                incomeId = Integer.parseInt(pathInfo.substring(1));
            } catch (Exception ignored) {}
        }
        if (incomeId == -1 && idParam != null && !idParam.isEmpty()) {
            try {
                incomeId = Integer.parseInt(idParam);
            } catch (Exception ignored) {}
        }

        if (incomeId == -1) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Income ID is required.");
            return;
        }

        try {
            boolean ok = incomeDAO.delete(incomeId, userId);
            if (ok) {
                sendSuccess(resp, "Income deleted successfully", null);
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Income not found or not owned by user.");
            }
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
