package com.moneymap.servlet;

import com.moneymap.dao.BankAccountDAO;
import com.moneymap.model.BankAccount;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/accounts/*")
public class AccountServlet extends BaseServlet {
    private final BankAccountDAO bankAccountDAO = new BankAccountDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);

        try {
            List<BankAccount> list = bankAccountDAO.findByUserId(userId);
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
        if (pathInfo != null && pathInfo.contains("set-primary")) {
            handleSetPrimary(req, resp, userId);
            return;
        }

        try {
            BankAccount account = readJson(req, BankAccount.class);
            if (account == null || account.getBankName() == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid bank account details.");
                return;
            }
            account.setUserId(userId);
            BankAccount created = bankAccountDAO.create(account);
            sendSuccess(resp, "Bank account added successfully", created);
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);
        handleSetPrimary(req, resp, userId);
    }

    private void handleSetPrimary(HttpServletRequest req, HttpServletResponse resp, int userId) throws IOException {
        String pathInfo = req.getPathInfo();
        String idParam = req.getParameter("id");
        int accountId = -1;

        if (pathInfo != null && pathInfo.contains("/primary")) {
            String clean = pathInfo.replace("/primary", "").replace("/", "");
            try {
                accountId = Integer.parseInt(clean);
            } catch (Exception ignored) {}
        }
        if (accountId == -1 && idParam != null && !idParam.isEmpty()) {
            try {
                accountId = Integer.parseInt(idParam);
            } catch (Exception ignored) {}
        }

        if (accountId == -1) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Account ID is required.");
            return;
        }

        try {
            boolean ok = bankAccountDAO.setPrimary(userId, accountId);
            if (ok) {
                sendSuccess(resp, "Primary account updated successfully", null);
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Account not found or update failed.");
            }
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
