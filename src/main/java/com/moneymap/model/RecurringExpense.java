package com.moneymap.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

public class RecurringExpense {
    private int recurringId;
    private int userId;
    private Integer categoryId;
    private String title;
    private BigDecimal amount;
    private String frequency;
    private Date startDate;
    private Date nextDueDate;
    private String status;
    private Timestamp createdAt;

    // View fields
    private String categoryName;
    private String categoryIcon;

    public RecurringExpense() {}

    public RecurringExpense(int recurringId, int userId, Integer categoryId, String title, BigDecimal amount, String frequency, Date startDate, Date nextDueDate, String status, Timestamp createdAt) {
        this.recurringId = recurringId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.title = title;
        this.amount = amount;
        this.frequency = frequency;
        this.startDate = startDate;
        this.nextDueDate = nextDueDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getRecurringId() { return recurringId; }
    public void setRecurringId(int recurringId) { this.recurringId = recurringId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(Date nextDueDate) { this.nextDueDate = nextDueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }
}
