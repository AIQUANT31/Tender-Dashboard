package com.example.dto;

import java.time.LocalDateTime;

public class ScrutinyResultSummary {
    
    private Long id;
    private Long tenderId;
    private Long bidId;
    private String bidderName;
    
    // Document details
    private String documentMissing;
    private String documentValidate;
    private String duplicateDoc;
    private Integer duplicateCount;
    
    // Validation status
    private String validationStatus;  // VALID, INVALID, PENDING
    private String remarks;
    
    // Change tracking
    private Integer changeCount;  // Number of changes made by user
    private LocalDateTime timestamp;
    private String changedBy;
    
    // Summary counts
    private Integer totalDocumentsFound;
    private Integer totalDocumentsMissing;
    private Integer totalDocumentsDuplicate;
    
    public ScrutinyResultSummary() {
        this.timestamp = LocalDateTime.now();
        this.changeCount = 0;
        this.validationStatus = "PENDING";
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getTenderId() {
        return tenderId;
    }
    
    public void setTenderId(Long tenderId) {
        this.tenderId = tenderId;
    }
    
    public Long getBidId() {
        return bidId;
    }
    
    public void setBidId(Long bidId) {
        this.bidId = bidId;
    }
    
    public String getBidderName() {
        return bidderName;
    }
    
    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }
    
    public String getDocumentMissing() {
        return documentMissing;
    }
    
    public void setDocumentMissing(String documentMissing) {
        this.documentMissing = documentMissing;
    }
    
    public String getDocumentValidate() {
        return documentValidate;
    }
    
    public void setDocumentValidate(String documentValidate) {
        this.documentValidate = documentValidate;
    }
    
    public String getDuplicateDoc() {
        return duplicateDoc;
    }
    
    public void setDuplicateDoc(String duplicateDoc) {
        this.duplicateDoc = duplicateDoc;
    }
    
    public Integer getDuplicateCount() {
        return duplicateCount;
    }
    
    public void setDuplicateCount(Integer duplicateCount) {
        this.duplicateCount = duplicateCount;
    }
    
    public String getValidationStatus() {
        return validationStatus;
    }
    
    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }
    
    public String getRemarks() {
        return remarks;
    }
    
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
    
    public Integer getChangeCount() {
        return changeCount;
    }
    
    public void setChangeCount(Integer changeCount) {
        this.changeCount = changeCount;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getChangedBy() {
        return changedBy;
    }
    
    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
    
    public Integer getTotalDocumentsFound() {
        return totalDocumentsFound;
    }
    
    public void setTotalDocumentsFound(Integer totalDocumentsFound) {
        this.totalDocumentsFound = totalDocumentsFound;
    }
    
    public Integer getTotalDocumentsMissing() {
        return totalDocumentsMissing;
    }
    
    public void setTotalDocumentsMissing(Integer totalDocumentsMissing) {
        this.totalDocumentsMissing = totalDocumentsMissing;
    }
    
    public Integer getTotalDocumentsDuplicate() {
        return totalDocumentsDuplicate;
    }
    
    public void setTotalDocumentsDuplicate(Integer totalDocumentsDuplicate) {
        this.totalDocumentsDuplicate = totalDocumentsDuplicate;
    }
    
    /* Helper method to increment change count */
    public void incrementChangeCount() {
        this.changeCount = (this.changeCount != null ? this.changeCount : 0) + 1;
    }
    
    /*  Check if document is valid */
    public boolean isValid() {
        return "VALID".equals(this.validationStatus) && 
               (this.documentMissing == null || this.documentMissing.isEmpty());
    }
}
