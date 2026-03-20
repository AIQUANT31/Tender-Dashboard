package com.example.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SummaryReport {
    
    private Long tenderId;
    private String tenderName;
    private Long bidId;
    private String bidderName;
    private LocalDateTime generatedAt;
    
    // Document counts
    private int totalDocumentsFound;
    private int totalDocumentsMissing;
    private int totalDocumentsDuplicate;
    private int totalDocumentsProcessed;
    
    // Document lists
    private List<String> documentsFound = new ArrayList<>();
    private List<String> missingDocuments = new ArrayList<>();
    private List<String> duplicateDocuments = new ArrayList<>();
    
    // Validation summary
    private boolean isValid;
    private String validationMessage;
    private int validationScore;
    
    // Activity log summary
    private int manualChangesCount;
    private List<String> recentManualChanges = new ArrayList<>();
    
    public SummaryReport() {
        this.generatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getTenderId() {
        return tenderId;
    }
    
    public void setTenderId(Long tenderId) {
        this.tenderId = tenderId;
    }
    
    public String getTenderName() {
        return tenderName;
    }
    
    public void setTenderName(String tenderName) {
        this.tenderName = tenderName;
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
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
    
    public int getTotalDocumentsFound() {
        return totalDocumentsFound;
    }
    
    public void setTotalDocumentsFound(int totalDocumentsFound) {
        this.totalDocumentsFound = totalDocumentsFound;
    }
    
    public int getTotalDocumentsMissing() {
        return totalDocumentsMissing;
    }
    
    public void setTotalDocumentsMissing(int totalDocumentsMissing) {
        this.totalDocumentsMissing = totalDocumentsMissing;
    }
    
    public int getTotalDocumentsDuplicate() {
        return totalDocumentsDuplicate;
    }
    
    public void setTotalDocumentsDuplicate(int totalDocumentsDuplicate) {
        this.totalDocumentsDuplicate = totalDocumentsDuplicate;
    }
    
    public int getTotalDocumentsProcessed() {
        return totalDocumentsProcessed;
    }
    
    public void setTotalDocumentsProcessed(int totalDocumentsProcessed) {
        this.totalDocumentsProcessed = totalDocumentsProcessed;
    }
    
    public List<String> getDocumentsFound() {
        return documentsFound;
    }
    
    public void setDocumentsFound(List<String> documentsFound) {
        this.documentsFound = documentsFound;
    }
    
    public List<String> getMissingDocuments() {
        return missingDocuments;
    }
    
    public void setMissingDocuments(List<String> missingDocuments) {
        this.missingDocuments = missingDocuments;
    }
    
    public List<String> getDuplicateDocuments() {
        return duplicateDocuments;
    }
    
    public void setDuplicateDocuments(List<String> duplicateDocuments) {
        this.duplicateDocuments = duplicateDocuments;
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    public void setValid(boolean valid) {
        isValid = valid;
    }
    
    public String getValidationMessage() {
        return validationMessage;
    }
    
    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }
    
    public int getValidationScore() {
        return validationScore;
    }
    
    public void setValidationScore(int validationScore) {
        this.validationScore = validationScore;
    }
    
    public int getManualChangesCount() {
        return manualChangesCount;
    }
    
    public void setManualChangesCount(int manualChangesCount) {
        this.manualChangesCount = manualChangesCount;
    }
    
    public List<String> getRecentManualChanges() {
        return recentManualChanges;
    }
    
    public void setRecentManualChanges(List<String> recentManualChanges) {
        this.recentManualChanges = recentManualChanges;
    }
    
    /**
     * Helper method to calculate totals
     */
    public void calculateTotals() {
        this.totalDocumentsFound = documentsFound.size();
        this.totalDocumentsMissing = missingDocuments.size();
        this.totalDocumentsDuplicate = duplicateDocuments.size();
        this.totalDocumentsProcessed = totalDocumentsFound + totalDocumentsMissing;
    }
}
