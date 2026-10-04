package com.moneymap.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class BankAccount {
    private int accountId;
    private int userId;
    private String bankName;
    private String accountHolderName;
    private String accountLast4;
    private String ifscCode;
    private String accountType;
    private BigDecimal openingBalance;
    private BigDecimal currentBalance;
    private boolean isPrimary;
    private boolean isActive;
    private Timestamp createdAt;

    public BankAccount() {}

    public BankAccount(int accountId, int userId, String bankName, String accountHolderName, String accountLast4, String ifscCode, String accountType, BigDecimal openingBalance, boolean isPrimary, boolean isActive, Timestamp createdAt) {
        this.accountId = accountId;
        this.userId = userId;
        this.bankName = bankName;
        this.accountHolderName = accountHolderName;
        this.accountLast4 = accountLast4;
        this.ifscCode = ifscCode;
        this.accountType = accountType;
        this.openingBalance = openingBalance;
        this.currentBalance = openingBalance;
        this.isPrimary = isPrimary;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public String getAccountLast4() { return accountLast4; }
    public void setAccountLast4(String accountLast4) { this.accountLast4 = accountLast4; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
