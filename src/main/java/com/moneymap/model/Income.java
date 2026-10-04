package com.moneymap.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

public class Income {
    private int incomeId;
    private int userId;
    private Integer accountId;
    private Integer categoryId;
    private BigDecimal amount;
    private String source;
    private Date date;
    private String description;
    private Timestamp createdAt;

    // Joined view fields
    private String categoryName;
    private String categoryIcon;
    private String bankName;

    public Income() {}

    public Income(int incomeId, int userId, Integer accountId, Integer categoryId, BigDecimal amount, String source, Date date, String description, Timestamp createdAt) {
        this.incomeId = incomeId;
        this.userId = userId;
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.source = source;
        this.date = date;
        this.description = description;
        this.createdAt = createdAt;
    }

    public int getIncomeId() { return incomeId; }
    public void setIncomeId(int incomeId) { this.incomeId = incomeId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
}
