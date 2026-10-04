package com.moneymap.servlet;

import com.moneymap.dao.CategoryDAO;
import com.moneymap.model.Category;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/categories/*")
public class CategoryServlet extends BaseServlet {
    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String type = req.getParameter("type");
        try {
            List<Category> list;
            if (type != null && !type.trim().isEmpty()) {
                list = categoryDAO.findByType(type.trim());
            } else {
                list = categoryDAO.findAll();
            }
            sendSuccess(resp, list);
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;

        try {
            Category cat = readJson(req, Category.class);
            if (cat == null || cat.getName() == null || cat.getType() == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Category name and type are required.");
                return;
            }
            Category created = categoryDAO.create(cat);
            sendSuccess(resp, "Category created successfully", created);
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
