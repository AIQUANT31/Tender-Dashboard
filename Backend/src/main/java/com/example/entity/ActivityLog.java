package com.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String action;
    
    @Column(length = 1000)
    private String description;
    
    @Column(name = "entity_type", length = 50)
    private String entityType;
    
    @Column(name = "entity_id")
    private Long entityId;
    
    @Column(name = "old_value", length = 1000)
    private String oldValue;
    
    @Column(name = "new_value", length = 1000)
    private String newValue;
    
    @Column(name = "upload_count")
    private Integer uploadCount;
    
    @Column(name = "found_documents", length = 1000)
    private String foundDocuments;
    
    @Column(name = "missing_documents", length = 1000)
    private String missingDocuments;
    
    @Column(name = "duplicate_documents", length = 1000)
    private String duplicateDocuments;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    public ActivityLog() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ActivityLog(String username, String action, String description) {
        this.username = username;
        this.action = action;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Long getEntityId() {
        return entityId;
    }
    
    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public String getOldValue() {
        return oldValue;
    }
    
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }
    
    public String getNewValue() {
        return newValue;
    }
    
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Integer getUploadCount() {
        return uploadCount;
    }
    
    public void setUploadCount(Integer uploadCount) {
        this.uploadCount = uploadCount;
    }
    
    public String getFoundDocuments() {
        return foundDocuments;
    }
    
    public void setFoundDocuments(String foundDocuments) {
        this.foundDocuments = foundDocuments;
    }
    
    public String getMissingDocuments() {
        return missingDocuments;
    }
    
    public void setMissingDocuments(String missingDocuments) {
        this.missingDocuments = missingDocuments;
    }
    
    public String getDuplicateDocuments() {
        return duplicateDocuments;
    }
    
    public void setDuplicateDocuments(String duplicateDocuments) {
        this.duplicateDocuments = duplicateDocuments;
    }
}
