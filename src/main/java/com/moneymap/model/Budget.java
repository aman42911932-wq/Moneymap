package com.moneymap.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

public class Budget {
    private int budgetId;
    private int userId;
    private Integer categoryId;
    private BigDecimal amount;
    private Date startDate;
    private Date endDate;
    private Timestamp createdAt;

    // View fields
    private String categoryName;
    private String categoryIcon;
    private BigDecimal currentSpent;
    private double percentageSpent;

    public Budget() {}

    public Budget(int budgetId, int userId, Integer categoryId, BigDecimal amount, Date startDate, Date endDate, Timestamp createdAt) {
        this.budgetId = budgetId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
    }

    public int getBudgetId() { return budgetId; }
    public void setBudgetId(int budgetId) { this.budgetId = budgetId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }

    public BigDecimal getCurrentSpent() { return currentSpent; }
    public void setCurrentSpent(BigDecimal currentSpent) { this.currentSpent = currentSpent; }

    public double getPercentageSpent() { return percentageSpent; }
    public void setPercentageSpent(double percentageSpent) { this.percentageSpent = percentageSpent; }
}
