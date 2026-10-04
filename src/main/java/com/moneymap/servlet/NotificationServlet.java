package com.moneymap.servlet;

import com.moneymap.dao.NotificationDAO;
import com.moneymap.model.Notification;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/notifications/*")
public class NotificationServlet extends BaseServlet {
    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        try {
            if (pathInfo.equals("/count")) {
                int count = notificationDAO.getUnreadCount(userId);
                Map<String, Object> data = new HashMap<>();
                data.put("unreadCount", count);
                sendSuccess(resp, data);
            } else {
                List<Notification> list = notificationDAO.findByUserId(userId);
                sendSuccess(resp, list);
            }
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);
        handleMarkRead(req, resp, userId);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);
        handleMarkRead(req, resp, userId);
    }

    private void handleMarkRead(HttpServletRequest req, HttpServletResponse resp, int userId) throws IOException {
        String pathInfo = req.getPathInfo();
        String idParam = req.getParameter("id");
        int notifId = -1;

        if (pathInfo != null && pathInfo.contains("/read")) {
            String clean = pathInfo.replace("/read", "").replace("/", "");
            try {
                notifId = Integer.parseInt(clean);
            } catch (Exception ignored) {}
        }
        if (notifId == -1 && idParam != null && !idParam.isEmpty()) {
            try {
                notifId = Integer.parseInt(idParam);
            } catch (Exception ignored) {}
        }

        if (notifId == -1) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Notification ID is required.");
            return;
        }

        try {
            boolean ok = notificationDAO.markRead(notifId, userId);
            if (ok) {
                sendSuccess(resp, "Notification marked as read", null);
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Notification not found.");
            }
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
