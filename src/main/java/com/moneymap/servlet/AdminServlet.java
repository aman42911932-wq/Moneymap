package com.moneymap.servlet;

import com.google.gson.JsonObject;
import com.moneymap.config.DBConnection;
import com.moneymap.dao.CategoryDAO;
import com.moneymap.dao.UserDAO;
import com.moneymap.model.Category;
import com.moneymap.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/admin/*")
public class AdminServlet extends BaseServlet {
    private final UserDAO userDAO = new UserDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAdmin(req, resp)) return;

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        try {
            if (pathInfo.equals("/users")) {
                List<User> users = userDAO.findAll();
                sendSuccess(resp, users);

            } else if (pathInfo.equals("/categories")) {
                List<Category> categories = categoryDAO.findAll();
                sendSuccess(resp, categories);

            } else if (pathInfo.equals("/stats")) {
                int totalUsers = 0;
                int totalTransactions = 0;
                int totalCategories = 0;

                try (Connection conn = DBConnection.getConnection()) {
                    try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM \"user\"");
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) totalUsers = rs.getInt(1);
                    }
                    try (PreparedStatement ps = conn.prepareStatement("SELECT (SELECT COUNT(*) FROM expense) + (SELECT COUNT(*) FROM income)");
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) totalTransactions = rs.getInt(1);
                    }
                    try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM category");
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) totalCategories = rs.getInt(1);
                    }
                }

                Map<String, Object> stats = new HashMap<>();
                stats.put("totalUsers", totalUsers);
                stats.put("totalTransactions", totalTransactions);
                stats.put("totalCategories", totalCategories);
                sendSuccess(resp, stats);

            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Admin endpoint not found.");
            }
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAdmin(req, resp)) return;

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        try {
            if (pathInfo.equals("/categories")) {
                Category cat = readJson(req, Category.class);
                if (cat == null || cat.getName() == null || cat.getType() == null) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Category name and type are required.");
                    return;
                }
                Category created = categoryDAO.create(cat);
                sendSuccess(resp, "Category created successfully", created);
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Admin POST endpoint not found.");
            }
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAdmin(req, resp)) return;

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        try {
            JsonObject body = readJson(req, JsonObject.class);

            if (pathInfo.contains("/users/") && pathInfo.contains("/status")) {
                String[] parts = pathInfo.split("/");
                int targetUserId = -1;
                for (int i = 0; i < parts.length; i++) {
                    if ("users".equals(parts[i]) && i + 1 < parts.length) {
                        try { targetUserId = Integer.parseInt(parts[i + 1]); } catch (Exception ignored) {}
                    }
                }

                boolean isActive = body != null && body.has("isActive") && body.get("isActive").getAsBoolean();
                boolean ok = userDAO.updateStatus(targetUserId, isActive);
                if (ok) {
                    sendSuccess(resp, "User status updated", null);
                } else {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found.");
                }

            } else if (pathInfo.contains("/categories/") && pathInfo.contains("/status")) {
                String[] parts = pathInfo.split("/");
                int targetCatId = -1;
                for (int i = 0; i < parts.length; i++) {
                    if ("categories".equals(parts[i]) && i + 1 < parts.length) {
                        try { targetCatId = Integer.parseInt(parts[i + 1]); } catch (Exception ignored) {}
                    }
                }

                boolean isActive = body != null && body.has("isActive") && body.get("isActive").getAsBoolean();
                boolean ok = categoryDAO.updateStatus(targetCatId, isActive);
                if (ok) {
                    sendSuccess(resp, "Category status updated", null);
                } else {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Category not found.");
                }

            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Admin PUT endpoint not found.");
            }
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
