package com.example.services.activity;

import com.example.entity.ActivityLog;
import com.example.entity.Bid;
import com.example.repository.ActivityLogRepository;
import com.example.repository.BidRepository;
import com.example.repository.BidderRepository;
import com.example.services.report.IpfsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActivityLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(ActivityLogService.class);
    
    @Autowired
    private ActivityLogRepository activityLogRepository;
    
    public ActivityLog logManualChange(String username, String action, String description,
                                       String entityType, Long entityId,
                                       String oldValue, String newValue) {
        ActivityLog activityLog = new ActivityLog(username, action, description);
        activityLog.setEntityType(entityType);
        activityLog.setEntityId(entityId);
        activityLog.setOldValue(oldValue);
        activityLog.setNewValue(newValue);
        
        ActivityLog saved = activityLogRepository.save(activityLog);
        logger.info("Activity logged: {} - {} by {}", action, description, username);
        return saved;
    }
    
    
    @Autowired
    private IpfsService ipfsService;
    
    @Autowired
    private com.example.repository.BidRepository bidRepository;
    
    @Autowired
    private com.example.repository.BidderRepository bidderRepository;
    
    public ActivityLog logValidationSummary(String username, String entityType, Long bidId, 
                                          int foundCount, int missingCount, int duplicateCount,
                                          List<String> foundDocs, List<String> missingDocs, List<String> duplicateDocs) {
        // Always create a NEW activity log entry for each validation (don't update existing)
        
        String action = "VALIDATION_SUMMARY";
        String description;
        String newValue;
        
        // Convert document lists to comma-separated strings
        String foundDocsStr = foundDocs != null ? String.join(", ", foundDocs) : "";
        String missingDocsStr = missingDocs != null ? String.join(", ", missingDocs) : "";
        String duplicateDocsStr = duplicateDocs != null ? String.join(", ", duplicateDocs) : "";
        
        // Get existing count for this entity to set upload number
        List<ActivityLog> existingLogs = activityLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, bidId);
        int uploadNum = 1;
        for (ActivityLog log : existingLogs) {
            if ("VALIDATION_SUMMARY".equals(log.getAction())) {
                uploadNum++;
            }
        }
        
        description = String.format("Upload %d: Found=%d, Missing=%d, Duplicate=%d", 
            uploadNum, foundCount, missingCount, duplicateCount);
        newValue = String.format("Found:%d|Missing:%d|Duplicate:%d", 
            foundCount, missingCount, duplicateCount);
        
        // Always create a new entry
        ActivityLog activityLog = new ActivityLog(username, action, description);
        activityLog.setEntityType(entityType);
        activityLog.setEntityId(bidId);
        activityLog.setNewValue(newValue);
        activityLog.setUploadCount(uploadNum);
        activityLog.setFoundDocuments(foundDocsStr);
        activityLog.setMissingDocuments(missingDocsStr);
        activityLog.setDuplicateDocuments(duplicateDocsStr);
        
        ActivityLog saved = activityLogRepository.save(activityLog);
        logger.info("Activity logged: {} - {} by {}", action, description, username);
        
        // Store this validation to IPFS immediately
        storeValidationToIpfs(activityLog, bidId);
        
        return saved;
    }
  
    
    // Store individual validation to IPFS
   
    private void storeValidationToIpfs(ActivityLog activityLog, Long bidId) {
        try {
            // Get bid info for the summary
            com.example.dto.ScrutinyResultSummary summary = new com.example.dto.ScrutinyResultSummary();
            summary.setId(activityLog.getId());
            summary.setBidId(bidId);
            summary.setTotalDocumentsFound(activityLog.getFoundDocuments() != null ? 
                activityLog.getFoundDocuments().split(",").length : 0);
            summary.setTotalDocumentsMissing(activityLog.getMissingDocuments() != null ? 
                activityLog.getMissingDocuments().split(",").length : 0);
            summary.setTotalDocumentsDuplicate(activityLog.getDuplicateDocuments() != null ? 
                activityLog.getDuplicateDocuments().split(",").length : 0);
            summary.setDocumentValidate(activityLog.getFoundDocuments());
            summary.setDocumentMissing(activityLog.getMissingDocuments());
            summary.setDuplicateDoc(activityLog.getDuplicateDocuments());
            summary.setChangeCount(activityLog.getUploadCount());
            summary.setTimestamp(activityLog.getTimestamp());
            summary.setChangedBy(activityLog.getUsername());
            summary.setValidationStatus("PENDING");
            summary.setRemarks("Upload " + activityLog.getUploadCount() + ": " + activityLog.getDescription());
            
            // Get tender and bidder info
            java.util.Optional<Bid> bidOpt = bidRepository.findById(bidId);
            if (bidOpt.isPresent()) {
                com.example.entity.Bid bidEntity = bidOpt.get();
                summary.setTenderId(bidEntity.getTenderId());
                if (bidEntity.getBidderId() != null) {
                    bidderRepository.findById(bidEntity.getBidderId()).ifPresent(b -> summary.setBidderName(b.getCompanyName()));
                }
            }
            
            Map<String, Object> ipfsResponse = ipfsService.storeSingleSummaryToIpfs(summary);
            logger.info("Validation {} stored to IPFS: {}", activityLog.getId(), ipfsResponse.get("ipfsHash"));
        } catch (Exception e) {
            logger.error("Error storing validation to IPFS: ", e);
        }
    }
    
// Store a single activity to IPFS

    
    public Map<String, Object> storeActivityToIpfs(Long activityId) {
        Map<String, Object> result = new HashMap<>();
        try {
            ActivityLog activityLog = activityLogRepository.findById(activityId).orElse(null);
            if (activityLog == null) {
                result.put("success", false);
                result.put("message", "Activity not found: " + activityId);
                return result;
            }
            
            // Get bid info
            Long bidId = activityLog.getEntityId();
            
            com.example.dto.ScrutinyResultSummary summary = new com.example.dto.ScrutinyResultSummary();
            summary.setId(activityLog.getId());
            summary.setBidId(bidId);
            summary.setTotalDocumentsFound(activityLog.getFoundDocuments() != null ? 
                activityLog.getFoundDocuments().split(",").length : 0);
            summary.setTotalDocumentsMissing(activityLog.getMissingDocuments() != null ? 
                activityLog.getMissingDocuments().split(",").length : 0);
            summary.setTotalDocumentsDuplicate(activityLog.getDuplicateDocuments() != null ? 
                activityLog.getDuplicateDocuments().split(",").length : 0);
            summary.setDocumentValidate(activityLog.getFoundDocuments());
            summary.setDocumentMissing(activityLog.getMissingDocuments());
            summary.setDuplicateDoc(activityLog.getDuplicateDocuments());
            summary.setChangeCount(activityLog.getUploadCount());
            summary.setTimestamp(activityLog.getTimestamp());
            summary.setChangedBy(activityLog.getUsername());
            summary.setValidationStatus("PENDING");
            summary.setRemarks("Upload " + activityLog.getUploadCount() + ": " + activityLog.getDescription());
            
            // Get tender and bidder info
            if (bidId != null) {
                java.util.Optional<Bid> bidOpt = bidRepository.findById(bidId);
                if (bidOpt.isPresent()) {
                    Bid bidEntity = bidOpt.get();
                    summary.setTenderId(bidEntity.getTenderId());
                    if (bidEntity.getBidderId() != null) {
                        bidderRepository.findById(bidEntity.getBidderId()).ifPresent(bidder -> summary.setBidderName(bidder.getCompanyName()));
                    }
                }
            }
            
            Map<String, Object> ipfsResponse = ipfsService.storeSingleSummaryToIpfs(summary);
            result.put("success", ipfsResponse.get("success"));
            result.put("ipfsHash", ipfsResponse.get("ipfsHash"));
            result.put("pinataUrl", ipfsResponse.get("pinataUrl"));
            result.put("message", "Activity " + activityId + " stored to IPFS: " + ipfsResponse.get("ipfsHash"));
            logger.info("Activity {} stored to IPFS: {}", activityId, ipfsResponse.get("ipfsHash"));
        } catch (Exception e) {
            logger.error("Error storing activity to IPFS: ", e);
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        return result;
    }
    
 
    public Map<String, Object> storeAllValidationsToIpfs(Long bidId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // Get bid to find tenderId
            Bid bid = bidRepository.findById(bidId).orElse(null);
            
            // Get all BID activities for this bid
            List<ActivityLog> bidActivities = activityLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("BID", bidId);
            
            // Also get TENDER activities using tenderId
            List<ActivityLog> tenderActivities = null;
            Long tenderId = null;
            if (bid != null && bid.getTenderId() != null) {
                tenderId = bid.getTenderId();
                tenderActivities = activityLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("TENDER", tenderId);
                
                
                List<ActivityLog> wrongBidActivities = activityLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("BID", tenderId);
                if (wrongBidActivities != null && !wrongBidActivities.isEmpty()) {
                    if (bidActivities == null) bidActivities = new java.util.ArrayList<>();
                    bidActivities.addAll(wrongBidActivities);
                }
            }
            
            // Merge both lists
            List<ActivityLog> activities = new java.util.ArrayList<>();
            if (bidActivities != null) activities.addAll(bidActivities);
            if (tenderActivities != null) activities.addAll(tenderActivities);
            
            // Sort by timestamp
            activities.sort((a, b) -> {
                if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
                if (a.getTimestamp() == null) return 1;
                if (b.getTimestamp() == null) return -1;
                return a.getTimestamp().compareTo(b.getTimestamp());
            });
            
            if (activities == null || activities.isEmpty()) {
                result.put("success", false);
                result.put("message", "No activities found for bid: " + bidId);
                return result;
            }
            
            // Create a combined summary with all validations
            com.example.dto.ScrutinyResultSummary summary = new com.example.dto.ScrutinyResultSummary();
            summary.setBidId(bidId);
            
            // Get bid info from earlier fetched bid
            if (bid != null) {
                summary.setTenderId(bid.getTenderId());
                if (bid.getBidderId() != null) {
                    bidderRepository.findById(bid.getBidderId()).ifPresent(b -> summary.setBidderName(b.getCompanyName()));
                }
            }
            
            // Build history string showing all validation attempts
            StringBuilder historyBuilder = new StringBuilder();
            StringBuilder foundDocsBuilder = new StringBuilder();
            StringBuilder missingDocsBuilder = new StringBuilder();
            StringBuilder duplicateDocsBuilder = new StringBuilder();
            
            int totalFound = 0;
            int totalMissing = 0;
            int totalDuplicate = 0;
            
            for (ActivityLog log : activities) {
                if ("VALIDATION_SUMMARY".equals(log.getAction())) {
                    if (historyBuilder.length() > 0) {
                        historyBuilder.append(" | ");
                    }
                    
                    int found = log.getFoundDocuments() != null ? log.getFoundDocuments().split(",").length : 0;
                    int missing = log.getMissingDocuments() != null && !log.getMissingDocuments().isEmpty() ? 
                        log.getMissingDocuments().split(",").length : 0;
                    int duplicate = log.getDuplicateDocuments() != null && !log.getDuplicateDocuments().isEmpty() ? 
                        log.getDuplicateDocuments().split(",").length : 0;
                    
                    historyBuilder.append("Upload ").append(log.getUploadCount())
                        .append(": Found=").append(found)
                        .append(", Missing=").append(missing)
                        .append(", Duplicate=").append(duplicate);
                    
                    totalFound = found;
                    totalMissing = missing;
                    totalDuplicate = duplicate;
                    
                    // Use latest validation data
                    if (log.getFoundDocuments() != null) {
                        foundDocsBuilder.append(log.getFoundDocuments());
                    }
                    if (log.getMissingDocuments() != null) {
                        missingDocsBuilder.append(log.getMissingDocuments());
                    }
                    if (log.getDuplicateDocuments() != null) {
                        duplicateDocsBuilder.append(log.getDuplicateDocuments());
                    }
                }
            }
            
            summary.setTotalDocumentsFound(totalFound);
            summary.setTotalDocumentsMissing(totalMissing);
            summary.setTotalDocumentsDuplicate(totalDuplicate);
            summary.setDocumentValidate(foundDocsBuilder.toString());
            summary.setDocumentMissing(missingDocsBuilder.toString());
            summary.setDuplicateDoc(duplicateDocsBuilder.toString());
            summary.setChangeCount(activities.size());
            
            if (!activities.isEmpty()) {
                ActivityLog latest = activities.get(activities.size() - 1);
                summary.setTimestamp(latest.getTimestamp());
                summary.setChangedBy(latest.getUsername());
            }
            
            summary.setValidationStatus(totalMissing > 0 ? "PENDING" : "VALID");
            summary.setRemarks(historyBuilder.toString());
            
            // Store to IPFS
            Map<String, Object> ipfsResponse = ipfsService.storeSingleSummaryToIpfs(summary);
            result.put("success", ipfsResponse.get("success"));
            result.put("ipfsHash", ipfsResponse.get("ipfsHash"));
            result.put("pinataUrl", ipfsResponse.get("pinataUrl"));
            result.put("message", "All validations stored to IPFS: " + ipfsResponse.get("ipfsHash"));
            result.put("totalValidations", activities.size());
            logger.info("All validations for bid {} stored to IPFS: {}", bidId, ipfsResponse.get("ipfsHash"));
        } catch (Exception e) {
            logger.error("Error storing all validations to IPFS: ", e);
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        return result;
    }
    
   
    public void updateTenderActivitiesToBid(Long tenderId, Long bidId, String username) {
        try {
            // Find all TENDER activities for this tender
            List<ActivityLog> tenderActivities = activityLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("TENDER", tenderId);
            
            for (ActivityLog log : tenderActivities) {
                // Update entity type and ID to BID
                log.setEntityType("BID");
                log.setEntityId(bidId);
                activityLogRepository.save(log);
                logger.info("Updated activity {} from TENDER to BID for bidId: {}", log.getId(), bidId);
            }
            
            logger.info("Updated {} activities from TENDER to BID for tender: {}, bid: {}", 
                tenderActivities.size(), tenderId, bidId);
        } catch (Exception e) {
            logger.error("Error updating tender activities to bid: ", e);
        }
    }
    
    
    public ActivityLog logDocumentValidationOverride(String username, Long bidId, 
                                                     String documentName, boolean previousStatus, boolean newStatus) {
        String action = "MANUAL_VALIDATION_OVERRIDE";
        String description = String.format("Manual override for document '%s': from %s to %s", 
                                           documentName, 
                                           previousStatus ? "INVALID" : "VALID",
                                           newStatus ? "VALID" : "INVALID");
        
        return logManualChange(username, action, description, "BID_DOCUMENT", bidId,
                             String.valueOf(previousStatus), String.valueOf(newStatus));
    }
    
   
    public ActivityLog logBidStatusChange(String username, Long bidId, 
                                          String oldStatus, String newStatus) {
        String action = "BID_STATUS_CHANGE";
        String description = String.format("Bid status changed from %s to %s", oldStatus, newStatus);
        
        return logManualChange(username, action, description, "BID", bidId, oldStatus, newStatus);
    }
   
    public ActivityLog logTenderDocumentChange(String username, Long tenderId,
                                               String documentName, String oldRequirement, String newRequirement) {
        String action = "TENDER_DOCUMENT_REQUIREMENT_CHANGE";
        String description = String.format("Document requirement '%s' changed from '%s' to '%s'",
                                           documentName, oldRequirement, newRequirement);
        
        return logManualChange(username, action, description, "TENDER", tenderId, oldRequirement, newRequirement);
    }
    
    
    public ActivityLog logManualDocumentAcceptance(String username, Long bidId, String documentName, String reason) {
        String action = "MANUAL_DOCUMENT_ACCEPTANCE";
        String description = String.format("Manually accepted document '%s'. Reason: %s", documentName, reason);
        
        return logManualChange(username, action, description, "BID_DOCUMENT", bidId, "REJECTED", "ACCEPTED");
    }
    
    
    public ActivityLog logManualDocumentRejection(String username, Long bidId, String documentName, String reason) {
        String action = "MANUAL_DOCUMENT_REJECTION";
        String description = String.format("Manually rejected document '%s'. Reason: %s", documentName, reason);
        
        return logManualChange(username, action, description, "BID_DOCUMENT", bidId, "ACCEPTED", "REJECTED");
    }
    
   
    public List<ActivityLog> getUserActivityLogs(String username) {
        return activityLogRepository.findByUsernameOrderByTimestampDesc(username);
    }
    
   
    public Page<ActivityLog> getUserActivityLogs(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return activityLogRepository.findByUsernameOrderByTimestampDesc(username, pageable);
    }
    
   
    public List<ActivityLog> getEntityActivityLogs(String entityType, Long entityId) {
        return activityLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }
    
   
    public List<ActivityLog> getActivityLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return activityLogRepository.findByDateRange(startDate, endDate);
    }
    
    
    public List<ActivityLog> getRecentActivityLogs() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<ActivityLog> page = activityLogRepository.findAll(pageable);
        return page.getContent();
    }
    
    public List<ActivityLog> searchByAction(String action) {
        return activityLogRepository.findByActionContainingIgnoreCaseOrderByTimestampDesc(action);
    }
    
    
    public long getUserActivityCount(String username) {
        return activityLogRepository.countByUsername(username);
    }
}
