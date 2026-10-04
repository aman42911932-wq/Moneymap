package com.moneymap.servlet;

import com.google.gson.JsonObject;
import com.moneymap.dao.CategoryDAO;
import com.moneymap.dao.ImportDAO;
import com.moneymap.model.Category;
import com.moneymap.model.Import;
import com.moneymap.model.ImportTransaction;
import com.moneymap.service.BudgetService;
import com.moneymap.service.StatementParserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/imports/*")
public class ImportServlet extends BaseServlet {
    private final ImportDAO importDAO = new ImportDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final StatementParserService parserService = new StatementParserService();
    private final BudgetService budgetService = new BudgetService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        // Format: /{id}/transactions
        if (pathInfo.contains("/transactions")) {
            String idStr = pathInfo.replace("/transactions", "").replace("/", "").trim();
            try {
                int importId = Integer.parseInt(idStr);
                List<ImportTransaction> list = importDAO.getTransactionsByImportId(importId);
                sendSuccess(resp, list);
                return;
            } catch (Exception e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid import ID.");
                return;
            }
        }
        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        try {
            JsonObject body = readJson(req, JsonObject.class);

            if (pathInfo.equals("/upload") || pathInfo.isEmpty()) {
                String fileName = body != null && body.has("fileName") ? body.get("fileName").getAsString() : "statement.csv";
                String fileContent = body != null && body.has("fileContent") ? body.get("fileContent").getAsString() : "";
                Integer accountId = (body != null && body.has("accountId") && !body.get("accountId").isJsonNull()) ? body.get("accountId").getAsInt() : null;

                if (fileContent.trim().isEmpty()) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "File content cannot be empty.");
                    return;
                }

                Import imp = new Import();
                imp.setUserId(userId);
                imp.setAccountId(accountId);
                imp.setFileName(fileName);
                imp.setFileType("CSV");
                imp.setStatus("pending");

                Import created = importDAO.createImport(imp);

                List<ImportTransaction> parsedTxns = parserService.parseCsv(fileContent, userId);
                for (ImportTransaction tx : parsedTxns) {
                    tx.setImportId(created.getImportId());
                }

                if (!parsedTxns.isEmpty()) {
                    importDAO.createTransactionsBatch(parsedTxns);
                }

                List<ImportTransaction> staged = importDAO.getTransactionsByImportId(created.getImportId());

                Map<String, Object> data = new HashMap<>();
                data.put("importId", created.getImportId());
                data.put("transactions", staged);
                sendSuccess(resp, "Statement parsed and staged for review", data);

            } else if (pathInfo.contains("/confirm")) {
                int importId = -1;
                String clean = pathInfo.replace("/confirm", "").replace("/", "").trim();
                try {
                    importId = Integer.parseInt(clean);
                } catch (Exception ignored) {}

                if (importId == -1 && body != null && body.has("importId")) {
                    importId = body.get("importId").getAsInt();
                }

                if (importId == -1) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Import ID is required.");
                    return;
                }

                boolean ok = importDAO.confirmImport(importId, userId);
                if (ok) {
                    // Check budget thresholds after confirming imports
                    budgetService.checkBudgetThresholds(userId);
                    sendSuccess(resp, "Statement imported and converted into transactions successfully", null);
                } else {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Failed to confirm import (already completed, invalid or unauthorized).");
                }
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Import endpoint not found.");
            }
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        // Format: /transactions/{id}/category
        if (pathInfo.contains("/transactions/") && pathInfo.contains("/category")) {
            String[] parts = pathInfo.split("/");
            int txnId = -1;
            for (int i = 0; i < parts.length; i++) {
                if ("transactions".equals(parts[i]) && i + 1 < parts.length) {
                    try {
                        txnId = Integer.parseInt(parts[i + 1]);
                    } catch (Exception ignored) {}
                }
            }

            try {
                JsonObject body = readJson(req, JsonObject.class);
                int categoryId = -1;
                if (body != null && body.has("categoryId")) {
                    categoryId = body.get("categoryId").getAsInt();
                } else if (body != null && body.has("categoryName")) {
                    String catName = body.get("categoryName").getAsString();
                    String type = body.has("type") ? body.get("type").getAsString() : "expense";
                    Category c = categoryDAO.findByNameAndType(catName, type);
                    if (c != null) categoryId = c.getCategoryId();
                }

                if (txnId == -1 || categoryId == -1) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid transactionId and categoryId are required.");
                    return;
                }

                boolean ok = importDAO.updateCategory(txnId, categoryId, userId);
                if (ok) {
                    sendSuccess(resp, "Transaction category updated", null);
                } else {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Staged transaction not found or unauthorized.");
                }
            } catch (Exception e) {
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
            return;
        }

        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Import PUT endpoint not found.");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        // Format: /transactions/{id}
        if (pathInfo.contains("/transactions/")) {
            String[] parts = pathInfo.split("/");
            int txnId = -1;
            for (int i = 0; i < parts.length; i++) {
                if ("transactions".equals(parts[i]) && i + 1 < parts.length) {
                    try {
                        txnId = Integer.parseInt(parts[i + 1]);
                    } catch (Exception ignored) {}
                }
            }

            if (txnId == -1) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid transaction ID is required.");
                return;
            }

            try {
                boolean ok = importDAO.deleteTransaction(txnId, userId);
                if (ok) {
                    sendSuccess(resp, "Staged transaction removed", null);
                } else {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Staged transaction not found or unauthorized.");
                }
            } catch (Exception e) {
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
            return;
        }

        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Import DELETE endpoint not found.");
    }
}
