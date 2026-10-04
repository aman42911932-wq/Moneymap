package com.moneymap.servlet;

import com.google.gson.JsonObject;
import com.moneymap.dao.SavingsGoalDAO;
import com.moneymap.model.SavingsGoal;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@WebServlet("/api/goals/*")
public class GoalServlet extends BaseServlet {
    private final SavingsGoalDAO goalDAO = new SavingsGoalDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);

        try {
            List<SavingsGoal> list = goalDAO.findByUserId(userId);
            sendSuccess(resp, list);
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);

        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.contains("contribute")) {
            handleContribute(req, resp, userId);
            return;
        }

        try {
            SavingsGoal goal = readJson(req, SavingsGoal.class);
            if (goal == null || goal.getGoalName() == null || goal.getTargetAmount() == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Goal name and target amount are required.");
                return;
            }
            goal.setUserId(userId);
            SavingsGoal created = goalDAO.create(goal);
            sendSuccess(resp, "Savings goal created successfully", created);
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void handleContribute(HttpServletRequest req, HttpServletResponse resp, int userId) throws IOException {
        String pathInfo = req.getPathInfo();
        int goalId = -1;

        if (pathInfo != null && pathInfo.contains("/contribute")) {
            String clean = pathInfo.replace("/contribute", "").replace("/", "");
            try {
                goalId = Integer.parseInt(clean);
            } catch (Exception ignored) {}
        }

        try {
            JsonObject body = readJson(req, JsonObject.class);
            if (goalId == -1 && body != null && body.has("goalId")) {
                goalId = body.get("goalId").getAsInt();
            }

            BigDecimal amount = BigDecimal.ZERO;
            if (body != null && body.has("amount")) {
                amount = body.get("amount").getAsBigDecimal();
            }

            if (goalId == -1 || amount.compareTo(BigDecimal.ZERO) <= 0) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid goalId and positive contribution amount are required.");
                return;
            }

            boolean ok = goalDAO.addContribution(goalId, amount);
            if (ok) {
                sendSuccess(resp, "Contribution added successfully", null);
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Goal not found.");
            }
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
