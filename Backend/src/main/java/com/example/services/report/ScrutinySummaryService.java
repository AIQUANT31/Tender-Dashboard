package com.example.services.report;

import com.example.dto.ScrutinyResultSummary;
import com.example.entity.ActivityLog;
import com.example.entity.Bid;
import com.example.entity.Bidder;
import com.example.entity.Tender;
import com.example.repository.ActivityLogRepository;
import com.example.repository.BidRepository;
import com.example.repository.BidderRepository;
import com.example.repository.TenderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class ScrutinySummaryService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScrutinySummaryService.class);
    
    @Autowired
    private ActivityLogRepository activityLogRepository;
    
    @Autowired
    private BidRepository bidRepository;
    
    @Autowired
    private TenderRepository tenderRepository;
    
    @Autowired
    private BidderRepository bidderRepository;
  
    public List<ScrutinyResultSummary> generateTenderScrutinySummary(Long tenderId) {
        List<ScrutinyResultSummary> summaries = new ArrayList<>();
        
        try {
            // Get all bids for this tender
            List<Bid> bids = bidRepository.findByTenderId(tenderId);
            
            for (Bid bid : bids) {
                ScrutinyResultSummary summary = generateBidScrutinySummary(bid.getId());
                if (summary != null) {
                    summaries.add(summary);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error generating scrutiny summary for tender {}: ", tenderId, e);
        }
        
        return summaries;
    }
  
    public ScrutinyResultSummary generateBidScrutinySummary(Long bidId) {
        ScrutinyResultSummary summary = new ScrutinyResultSummary();
        
        try {
            Bid bid = bidRepository.findById(bidId).orElse(null);
            if (bid == null) {
                logger.warn("Bid not found with id: {}", bidId);
                return null;
            }
            
            summary.setBidId(bid.getId());
            
            // Get tender info
            if (bid.getTenderId() != null) {
                Tender tender = tenderRepository.findById(bid.getTenderId()).orElse(null);
                if (tender != null) {
                    summary.setTenderId(tender.getId());
                }
            }
            
            // Get bidder info
            if (bid.getBidderId() != null) {
                Bidder bidder = bidderRepository.findById(bid.getBidderId()).orElse(null);
                if (bidder != null) {
                    summary.setBidderName(bidder.getCompanyName());
                }
            }
            
          
            List<ActivityLog> bidActivities = activityLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("BID", bidId);
            
            // Get TENDER activities using tenderId (since TENDER activities have tenderId as entityId)
            Long tenderId = bid.getTenderId();
            List<ActivityLog> tenderActivities = null;
            if (tenderId != null) {
                tenderActivities = activityLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("TENDER", tenderId);
                
                
                List<ActivityLog> wrongBidActivities = activityLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("BID", tenderId);
                if (wrongBidActivities != null && !wrongBidActivities.isEmpty()) {
                    if (bidActivities == null) bidActivities = new java.util.ArrayList<>();
                    bidActivities.addAll(wrongBidActivities);
                }
            }
            
            // Merge both lists
            List<ActivityLog> allActivities = new java.util.ArrayList<>();
            if (bidActivities != null) allActivities.addAll(bidActivities);
            if (tenderActivities != null) allActivities.addAll(tenderActivities);
            
            // Sort by timestamp
            allActivities.sort((a, b) -> {
                if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
                if (a.getTimestamp() == null) return 1;
                if (b.getTimestamp() == null) return -1;
                return a.getTimestamp().compareTo(b.getTimestamp());
            });
            
            if (allActivities != null && !allActivities.isEmpty()) {
                // Get the LATEST activity for current status
                ActivityLog latestActivity = allActivities.get(allActivities.size() - 1);
                
                // Set document details from latest activity
                summary.setDocumentValidate(latestActivity.getFoundDocuments());
                summary.setDocumentMissing(latestActivity.getMissingDocuments());
                summary.setDuplicateDoc(latestActivity.getDuplicateDocuments());
                
                // Calculate duplicate count
                if (latestActivity.getDuplicateDocuments() != null && 
                    !latestActivity.getDuplicateDocuments().isEmpty()) {
                    String[] duplicates = latestActivity.getDuplicateDocuments().split(",");
                    summary.setDuplicateCount(duplicates.length);
                }
                
                // Set change count (number of validation activities)
                int validationCount = 0;
                for (ActivityLog log : allActivities) {
                    if ("VALIDATION_SUMMARY".equals(log.getAction())) {
                        validationCount++;
                    }
                }
                summary.setChangeCount(validationCount);
                
                // Set timestamp from latest activity
                summary.setTimestamp(latestActivity.getTimestamp());
                
                // Set who changed
                summary.setChangedBy(latestActivity.getUsername());
                
                // Calculate totals from latest activity
                calculateTotals(summary, latestActivity);
                
                // Set validation status
                setValidationStatus(summary);
                
                // Build history string showing all validation attempts
                StringBuilder historyBuilder = new StringBuilder();
                for (int i = 0; i < allActivities.size(); i++) {
                    ActivityLog log = allActivities.get(i);
                    if ("VALIDATION_SUMMARY".equals(log.getAction())) {
                        if (historyBuilder.length() > 0) {
                            historyBuilder.append(" | ");
                        }
                        historyBuilder.append("Upload ")
                            .append(log.getUploadCount() != null ? log.getUploadCount() : (i + 1))
                            .append(": Found=")
                            .append(log.getFoundDocuments() != null ? log.getFoundDocuments().split(",").length : 0)
                            .append(", Missing=")
                            .append(log.getMissingDocuments() != null ? log.getMissingDocuments().split(",").length : 0)
                            .append(", Duplicate=")
                            .append(log.getDuplicateDocuments() != null ? log.getDuplicateDocuments().split(",").length : 0);
                    }
                }
                summary.setRemarks(historyBuilder.toString());
            }
            
            summary.setId(bidId);
            
        } catch (Exception e) {
            logger.error("Error generating scrutiny summary for bid {}: ", bidId, e);
        }
        
        return summary;
    }
    
    
    public List<ScrutinyResultSummary> generateAllScrutinySummary() {
        List<ScrutinyResultSummary> summaries = new ArrayList<>();
        
        try {
            List<Bid> allBids = bidRepository.findAll();
            
            for (Bid bid : allBids) {
                ScrutinyResultSummary summary = generateBidScrutinySummary(bid.getId());
                if (summary != null) {
                    summaries.add(summary);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error generating all scrutiny summaries: ", e);
        }
        
        return summaries;
    }
    
    
    public List<ScrutinyResultSummary> generateUserChangeSummary(Long tenderId, String username) {
        List<ScrutinyResultSummary> summaries = new ArrayList<>();
        
        try {
            List<ActivityLog> activities;
            
            if (username != null && !username.isEmpty()) {
                // Get activities for specific user
                activities = activityLogRepository.findAll().stream()
                    .filter(a -> username.equals(a.getUsername()))
                    .collect(Collectors.toList());
            } else {
                activities = activityLogRepository.findAll();
            }
            
            // Group activities by bid and process
            Map<Long, List<ActivityLog>> activitiesByBid = new HashMap<>();
            for (ActivityLog activity : activities) {
                if (activity.getEntityId() != null) {
                    activitiesByBid.computeIfAbsent(activity.getEntityId(), k -> new ArrayList<>())
                        .add(activity);
                }
            }
            
            // Generate summaries for each bid
            for (Map.Entry<Long, List<ActivityLog>> entry : activitiesByBid.entrySet()) {
                Bid bid = bidRepository.findById(entry.getKey()).orElse(null);
                if (bid != null) {
                    if (tenderId != null && !tenderId.equals(bid.getTenderId())) {
                        continue;
                    }
                    
                    ScrutinyResultSummary summary = new ScrutinyResultSummary();
                    summary.setBidId(bid.getId());
                    
                    if (bid.getTenderId() != null) {
                        summary.setTenderId(bid.getTenderId());
                    }
                    
                    if (bid.getBidderId() != null) {
                        Bidder bidder = bidderRepository.findById(bid.getBidderId()).orElse(null);
                        if (bidder != null) {
                            summary.setBidderName(bidder.getCompanyName());
                        }
                    }
                    
                    List<ActivityLog> bidActivities = entry.getValue();
                    ActivityLog latestActivity = bidActivities.get(bidActivities.size() - 1);
                    
                    summary.setDocumentValidate(latestActivity.getFoundDocuments());
                    summary.setDocumentMissing(latestActivity.getMissingDocuments());
                    summary.setDuplicateDoc(latestActivity.getDuplicateDocuments());
                    
                    if (latestActivity.getDuplicateDocuments() != null && 
                        !latestActivity.getDuplicateDocuments().isEmpty()) {
                        String[] duplicates = latestActivity.getDuplicateDocuments().split(",");
                        summary.setDuplicateCount(duplicates.length);
                    }
                    
                    summary.setChangeCount(bidActivities.size());
                    summary.setTimestamp(latestActivity.getTimestamp());
                    summary.setChangedBy(latestActivity.getUsername());
                    
                    calculateTotals(summary, latestActivity);
                    setValidationStatus(summary);
                    
                    summary.setId(bid.getId());
                    summaries.add(summary);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error generating user change summary: ", e);
        }
        
        return summaries;
    }
    
  
    private void calculateTotals(ScrutinyResultSummary summary, ActivityLog activity) {
        // Total documents found
        if (activity.getFoundDocuments() != null && !activity.getFoundDocuments().isEmpty()) {
            String[] found = activity.getFoundDocuments().split(",");
            summary.setTotalDocumentsFound(found.length);
        } else {
            summary.setTotalDocumentsFound(0);
        }
        
        // Total documents missing
        if (activity.getMissingDocuments() != null && !activity.getMissingDocuments().isEmpty()) {
            String[] missing = activity.getMissingDocuments().split(",");
            summary.setTotalDocumentsMissing(missing.length);
        } else {
            summary.setTotalDocumentsMissing(0);
        }
        
        // Total documents duplicate
        if (activity.getDuplicateDocuments() != null && !activity.getDuplicateDocuments().isEmpty()) {
            String[] duplicate = activity.getDuplicateDocuments().split(",");
            summary.setTotalDocumentsDuplicate(duplicate.length);
        } else {
            summary.setTotalDocumentsDuplicate(0);
        }
    }
    
    
    private void setValidationStatus(ScrutinyResultSummary summary) {
        if (summary.getTotalDocumentsMissing() != null && summary.getTotalDocumentsMissing() > 0) {
            summary.setValidationStatus("INVALID");
            summary.setRemarks("Missing required documents");
        } else if (summary.getTotalDocumentsDuplicate() != null && summary.getTotalDocumentsDuplicate() > 0) {
            summary.setValidationStatus("PENDING");
            summary.setRemarks("Duplicate documents found");
        } else {
            summary.setValidationStatus("VALID");
            summary.setRemarks("All documents valid");
        }
    }
}
