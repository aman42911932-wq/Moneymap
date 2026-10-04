package com.moneymap.servlet;

import com.google.gson.JsonObject;
import com.moneymap.model.User;
import com.moneymap.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;

public abstract class BaseServlet extends HttpServlet {

    protected int getUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            return (Integer) session.getAttribute("userId");
        }
        return -1;
    }

    protected String getUserRole(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("role") != null) {
            return (String) session.getAttribute("role");
        }
        return null;
    }

    private static final com.moneymap.dao.UserDAO BASE_USER_DAO = new com.moneymap.dao.UserDAO();

    protected boolean requireAuth(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int uid = getUserId(req);
        if (uid == -1) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized. Please log in.");
            return false;
        }
        String role = getUserRole(req);
        if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
            try {
                com.moneymap.model.User u = BASE_USER_DAO.findById(uid);
                if (u == null || !u.isActive()) {
                    HttpSession session = req.getSession(false);
                    if (session != null) session.invalidate();
                    sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Account is deactivated or invalid.");
                    return false;
                }
            } catch (Exception ignored) {}
        }
        return true;
    }

    protected boolean requireAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String role = getUserRole(req);
        if (role == null || (!role.equals("ADMIN") && !role.equals("SUPER_ADMIN"))) {
            sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Forbidden. Administrator access required.");
            return false;
        }
        return true;
    }

    protected void sendSuccess(HttpServletResponse resp, Object data) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        JsonObject res = new JsonObject();
        res.addProperty("success", true);
        if (data != null) {
            res.add("data", JsonUtil.getGson().toJsonTree(data));
        }
        resp.getWriter().write(JsonUtil.toJson(res));
    }

    protected void sendSuccess(HttpServletResponse resp, String message, Object data) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        JsonObject res = new JsonObject();
        res.addProperty("success", true);
        if (message != null) res.addProperty("message", message);
        if (data != null) {
            res.add("data", JsonUtil.getGson().toJsonTree(data));
        }
        resp.getWriter().write(JsonUtil.toJson(res));
    }

    protected void sendError(HttpServletResponse resp, int statusCode, String errorMsg) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json;charset=UTF-8");
        JsonObject res = new JsonObject();
        res.addProperty("success", false);
        res.addProperty("error", errorMsg != null ? errorMsg : "An error occurred");
        resp.getWriter().write(JsonUtil.toJson(res));
    }

    protected <T> T readJson(HttpServletRequest req, Class<T> clazz) throws IOException {
        BufferedReader reader = req.getReader();
        return JsonUtil.fromJson(reader.lines().reduce("", (a, b) -> a + b), clazz);
    }
}
