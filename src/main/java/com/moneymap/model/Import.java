package com.moneymap.model;

import java.sql.Timestamp;

public class Import {
    private int importId;
    private int userId;
    private Integer accountId;
    private String fileName;
    private String fileType;
    private Timestamp uploadDate;
    private String status;

    public Import() {}

    public Import(int importId, int userId, Integer accountId, String fileName, String fileType, Timestamp uploadDate, String status) {
        this.importId = importId;
        this.userId = userId;
        this.accountId = accountId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.uploadDate = uploadDate;
        this.status = status;
    }

    public int getImportId() { return importId; }
    public void setImportId(int importId) { this.importId = importId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Timestamp getUploadDate() { return uploadDate; }
    public void setUploadDate(Timestamp uploadDate) { this.uploadDate = uploadDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
