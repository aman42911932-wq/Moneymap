package com.moneymap.servlet;

import com.moneymap.config.DBConnection;
import com.moneymap.dao.BudgetDAO;
import com.moneymap.dao.ExpenseDAO;
import com.moneymap.dao.IncomeDAO;
import com.moneymap.dao.SavingsGoalDAO;
import com.moneymap.model.Budget;
import com.moneymap.model.Expense;
import com.moneymap.model.Income;
import com.moneymap.model.SavingsGoal;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@WebServlet("/api/reports/*")
public class ReportServlet extends BaseServlet {
    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final IncomeDAO incomeDAO = new IncomeDAO();
    private final BudgetDAO budgetDAO = new BudgetDAO();
    private final SavingsGoalDAO goalDAO = new SavingsGoalDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAuth(req, resp)) return;
        int userId = getUserId(req);

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        String startDate = req.getParameter("startDate");
        String endDate = req.getParameter("endDate");

        try {
            if (pathInfo.equals("/summary")) {
                List<Expense> expenses = expenseDAO.findByUserId(userId, startDate, endDate, null);
                List<Income> incomes = incomeDAO.findByUserId(userId, startDate, endDate, null);
                List<Budget> budgets = budgetDAO.findByUserId(userId);
                List<SavingsGoal> goals = goalDAO.findByUserId(userId);

                BigDecimal totalIncome = BigDecimal.ZERO;
                for (Income i : incomes) totalIncome = totalIncome.add(i.getAmount());

                BigDecimal totalExpense = BigDecimal.ZERO;
                for (Expense e : expenses) totalExpense = totalExpense.add(e.getAmount());

                BigDecimal netSavings = totalIncome.subtract(totalExpense);
                double savingsRate = totalIncome.compareTo(BigDecimal.ZERO) > 0 ?
                        (netSavings.doubleValue() / totalIncome.doubleValue() * 100.0) : 0.0;

                Map<String, Object> data = new HashMap<>();
                data.put("totalIncome", totalIncome);
                data.put("totalExpense", totalExpense);
                data.put("netSavings", netSavings);
                data.put("savingsRate", Math.max(0.0, Math.round(savingsRate * 10.0) / 10.0));
                data.put("activeBudgetsCount", budgets.size());
                data.put("totalGoalsCount", goals.size());
                data.put("recentExpenses", expenses.subList(0, Math.min(5, expenses.size())));
                data.put("recentIncomes", incomes.subList(0, Math.min(5, incomes.size())));

                sendSuccess(resp, data);

            } else if (pathInfo.equals("/by-category")) {
                List<Expense> expenses = expenseDAO.findByUserId(userId, startDate, endDate, null);
                BigDecimal totalExpense = BigDecimal.ZERO;
                Map<String, BigDecimal> catTotals = new LinkedHashMap<>();
                Map<String, String> catIcons = new HashMap<>();

                for (Expense e : expenses) {
                    totalExpense = totalExpense.add(e.getAmount());
                    String name = e.getCategoryName() != null ? e.getCategoryName() : "Miscellaneous";
                    catTotals.put(name, catTotals.getOrDefault(name, BigDecimal.ZERO).add(e.getAmount()));
                    catIcons.put(name, e.getCategoryIcon() != null ? e.getCategoryIcon() : "tag");
                }

                List<Map<String, Object>> list = new ArrayList<>();
                for (Map.Entry<String, BigDecimal> entry : catTotals.entrySet()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("categoryName", entry.getKey());
                    item.put("amount", entry.getValue());
                    item.put("icon", catIcons.get(entry.getKey()));
                    double pct = totalExpense.compareTo(BigDecimal.ZERO) > 0 ?
                            (entry.getValue().doubleValue() / totalExpense.doubleValue() * 100.0) : 0.0;
                    item.put("percentage", Math.round(pct * 10.0) / 10.0);
                    list.add(item);
                }

                sendSuccess(resp, list);

            } else if (pathInfo.equals("/monthly-comparison")) {
                // Generate last 6 months comparison
                List<Map<String, Object>> comparison = new ArrayList<>();
                LocalDate now = LocalDate.now();

                for (int i = 5; i >= 0; i--) {
                    LocalDate monthDate = now.minusMonths(i);
                    LocalDate firstDay = monthDate.withDayOfMonth(1);
                    LocalDate lastDay = monthDate.withDayOfMonth(monthDate.lengthOfMonth());

                    String start = firstDay.toString();
                    String end = lastDay.toString();
                    String label = monthDate.format(DateTimeFormatter.ofPattern("MMM yyyy"));

                    List<Expense> exp = expenseDAO.findByUserId(userId, start, end, null);
                    List<Income> inc = incomeDAO.findByUserId(userId, start, end, null);

                    BigDecimal expSum = BigDecimal.ZERO;
                    for (Expense e : exp) expSum = expSum.add(e.getAmount());

                    BigDecimal incSum = BigDecimal.ZERO;
                    for (Income in : inc) incSum = incSum.add(in.getAmount());

                    Map<String, Object> monthData = new HashMap<>();
                    monthData.put("month", label);
                    monthData.put("income", incSum);
                    monthData.put("expense", expSum);
                    comparison.add(monthData);
                }

                sendSuccess(resp, comparison);

            } else if (pathInfo.equals("/insights")) {
                List<Expense> expenses = expenseDAO.findByUserId(userId, startDate, endDate, null);
                List<Income> incomes = incomeDAO.findByUserId(userId, startDate, endDate, null);
                List<Budget> budgets = budgetDAO.findByUserId(userId);

                List<String> insights = new ArrayList<>();
                String topCategory = "None";
                BigDecimal maxCatAmount = BigDecimal.ZERO;
                Map<String, BigDecimal> catTotals = new HashMap<>();

                for (Expense e : expenses) {
                    String cat = e.getCategoryName() != null ? e.getCategoryName() : "Miscellaneous";
                    BigDecimal updated = catTotals.getOrDefault(cat, BigDecimal.ZERO).add(e.getAmount());
                    catTotals.put(cat, updated);
                    if (updated.compareTo(maxCatAmount) > 0) {
                        maxCatAmount = updated;
                        topCategory = cat;
                    }
                }

                if (!"None".equals(topCategory)) {
                    insights.add(String.format("Your largest spending category is %s (₹%.2f).", topCategory, maxCatAmount.doubleValue()));
                }

                for (Budget b : budgets) {
                    if (b.getPercentageSpent() >= 100.0) {
                        insights.add(String.format("⚠️ You have exceeded your budget for %s (%.1f%% spent).", b.getCategoryName(), b.getPercentageSpent()));
                    } else if (b.getPercentageSpent() >= 80.0) {
                        insights.add(String.format("⚡ You are approaching your budget limit for %s (%.1f%% spent).", b.getCategoryName(), b.getPercentageSpent()));
                    }
                }

                if (insights.isEmpty()) {
                    insights.add("Your finances are looking healthy with no budget overruns!");
                }

                Map<String, Object> data = new HashMap<>();
                data.put("insights", insights);
                data.put("topCategory", topCategory);
                sendSuccess(resp, data);

            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Report endpoint not found.");
            }
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
