package com.moneymap.servlet;

import com.google.gson.JsonObject;
import com.moneymap.model.Admin;
import com.moneymap.model.User;
import com.moneymap.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/auth/*", "/api/me"})
public class AuthServlet extends BaseServlet {
    private final AuthService authService = new AuthService();

    private final com.moneymap.dao.UserDAO userDAO = new com.moneymap.dao.UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            int uid = (Integer) session.getAttribute("userId");
            String role = (String) session.getAttribute("role");
            if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
                try {
                    User u = userDAO.findById(uid);
                    if (u == null || !u.isActive()) {
                        session.invalidate();
                        sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Account has been deactivated.");
                        return;
                    }
                } catch (Exception ignored) {}
            }

            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", session.getAttribute("userId"));
            userData.put("name", session.getAttribute("name"));
            userData.put("email", session.getAttribute("email"));
            userData.put("role", session.getAttribute("role"));
            sendSuccess(resp, userData);
        } else {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "No active session.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        JsonObject body;
        try {
            body = readJson(req, JsonObject.class);
        } catch (Exception e) {
            body = new JsonObject();
        }

        try {
            if (pathInfo.equals("/login")) {
                String email = body != null && body.has("email") ? body.get("email").getAsString().trim() : "";
                String password = body != null && body.has("password") ? body.get("password").getAsString() : "";

                if (email.isEmpty() || password.isEmpty()) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Email and password are required.");
                    return;
                }

                // If admin logging in via general form or admin-login
                if (email.toLowerCase().contains("admin")) {
                    Admin admin = authService.authenticateAdmin(email, password);
                    if (admin != null) {
                        HttpSession session = req.getSession(true);
                        session.setAttribute("userId", admin.getAdminId());
                        session.setAttribute("name", admin.getName());
                        session.setAttribute("email", admin.getEmail());
                        session.setAttribute("role", admin.getRole());

                        Map<String, Object> data = new HashMap<>();
                        data.put("userId", admin.getAdminId());
                        data.put("name", admin.getName());
                        data.put("email", admin.getEmail());
                        data.put("role", admin.getRole());
                        sendSuccess(resp, "Admin login successful", data);
                        return;
                    }
                }

                User user = authService.authenticateUser(email, password);
                if (user != null) {
                    if (!user.isActive()) {
                        sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Account has been deactivated. Please contact support.");
                        return;
                    }

                    HttpSession session = req.getSession(true);
                    session.setAttribute("userId", user.getUserId());
                    session.setAttribute("name", user.getName());
                    session.setAttribute("email", user.getEmail());
                    session.setAttribute("role", "USER");

                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", user.getUserId());
                    data.put("name", user.getName());
                    data.put("email", user.getEmail());
                    data.put("phone", user.getPhone());
                    data.put("role", "USER");
                    sendSuccess(resp, "Login successful", data);
                } else {
                    sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid email or password.");
                }

            } else if (pathInfo.equals("/admin-login")) {
                String email = body != null && body.has("email") ? body.get("email").getAsString().trim() : "";
                String password = body != null && body.has("password") ? body.get("password").getAsString() : "";

                Admin admin = authService.authenticateAdmin(email, password);
                if (admin != null) {
                    HttpSession session = req.getSession(true);
                    session.setAttribute("userId", admin.getAdminId());
                    session.setAttribute("name", admin.getName());
                    session.setAttribute("email", admin.getEmail());
                    session.setAttribute("role", admin.getRole());

                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", admin.getAdminId());
                    data.put("name", admin.getName());
                    data.put("email", admin.getEmail());
                    data.put("role", admin.getRole());
                    sendSuccess(resp, "Admin login successful", data);
                } else {
                    sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid administrator credentials.");
                }

            } else if (pathInfo.equals("/register")) {
                String name = body != null && body.has("name") ? body.get("name").getAsString().trim() : "";
                String email = body != null && body.has("email") ? body.get("email").getAsString().trim() : "";
                String phone = body != null && body.has("phone") ? body.get("phone").getAsString().trim() : "";
                String password = body != null && body.has("password") ? body.get("password").getAsString() : "";

                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Name, email, and password are required.");
                    return;
                }

                User created = authService.registerUser(name, email, phone, password);
                HttpSession session = req.getSession(true);
                session.setAttribute("userId", created.getUserId());
                session.setAttribute("name", created.getName());
                session.setAttribute("email", created.getEmail());
                session.setAttribute("role", "USER");

                Map<String, Object> data = new HashMap<>();
                data.put("userId", created.getUserId());
                data.put("name", created.getName());
                data.put("email", created.getEmail());
                data.put("phone", created.getPhone());
                data.put("role", "USER");
                sendSuccess(resp, "Registration successful", data);

            } else if (pathInfo.equals("/logout")) {
                HttpSession session = req.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                sendSuccess(resp, "Logged out successfully", null);

            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Auth endpoint not found.");
            }
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
