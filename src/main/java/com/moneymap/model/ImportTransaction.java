package com.moneymap.model;

import java.math.BigDecimal;
import java.sql.Date;

public class ImportTransaction {
    private int transactionId;
    private int importId;
    private int userId;
    private Date date;
    private String description;
    private BigDecimal amount;
    private String txnType; // 'debit' or 'credit'
    private String suggestedCategory;
    private Integer finalCategoryId;
    private String status;

    public ImportTransaction() {}

    public ImportTransaction(int transactionId, int importId, int userId, Date date, String description, BigDecimal amount, String txnType, String suggestedCategory, Integer finalCategoryId, String status) {
        this.transactionId = transactionId;
        this.importId = importId;
        this.userId = userId;
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.txnType = txnType;
        this.suggestedCategory = suggestedCategory;
        this.finalCategoryId = finalCategoryId;
        this.status = status;
    }

    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public int getImportId() { return importId; }
    public void setImportId(int importId) { this.importId = importId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getTxnType() { return txnType; }
    public void setTxnType(String txnType) { this.txnType = txnType; }

    public String getSuggestedCategory() { return suggestedCategory; }
    public void setSuggestedCategory(String suggestedCategory) { this.suggestedCategory = suggestedCategory; }

    public Integer getFinalCategoryId() { return finalCategoryId; }
    public void setFinalCategoryId(Integer finalCategoryId) { this.finalCategoryId = finalCategoryId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
