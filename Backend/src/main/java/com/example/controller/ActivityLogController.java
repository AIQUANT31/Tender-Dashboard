package com.example.controller;

import com.example.dto.SummaryReport;
import com.example.entity.ActivityLog;
import com.example.repository.ActivityLogRepository;
import com.example.services.activity.ActivityLogService;
import com.example.services.report.IpfsService;
import com.example.services.report.ScrutinySummaryService;
import com.example.services.report.SummaryReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.support.Repositories;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
@CrossOrigin(origins = "*")
public class ActivityLogController {
    
    private final ActivityLogRepository activityLogRepository;

    private static final Logger logger = LoggerFactory.getLogger(ActivityLogController.class);
    
    @Autowired
    private ActivityLogService activityLogService;
    
    @Autowired
    private SummaryReportService summaryReportService;
    
    @Autowired
    private ScrutinySummaryService scrutinySummaryService;
    
    @Autowired
    private IpfsService ipfsService;


    ActivityLogController(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }
    
    
    @PostMapping("/log")
    public ResponseEntity<Map<String, Object>> logActivity(
            @RequestParam String username,
            @RequestParam String action,
            @RequestParam String description,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String oldValue,
            @RequestParam(required = false) String newValue) {
        
        try {
            ActivityLog log = activityLogService.logManualChange(
                username, action, description, entityType, entityId, oldValue, newValue
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Activity logged successfully");
            response.put("activityId", log.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error logging activity: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error logging activity: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
   
    @PostMapping("/document-override")
    public ResponseEntity<Map<String, Object>> logDocumentOverride(
            @RequestParam String username,
            @RequestParam Long bidId,
            @RequestParam String documentName,
            @RequestParam boolean previousStatus,
            @RequestParam boolean newStatus) {
        
        try {
            ActivityLog log = activityLogService.logDocumentValidationOverride(
                username, bidId, documentName, previousStatus, newStatus
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document override logged");
            response.put("activityId", log.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error logging document override: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
 
   
    @PostMapping("/document-accept")
    public ResponseEntity<Map<String, Object>> logDocumentAcceptance(
            @RequestParam String username,
            @RequestParam Long bidId,
            @RequestParam String documentName,
            @RequestParam String reason) {
        
        try {
            ActivityLog log = activityLogService.logManualDocumentAcceptance(
                username, bidId, documentName, reason
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document acceptance logged");
            response.put("activityId", log.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error logging document acceptance: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    
    @PostMapping("/document-reject")
    public ResponseEntity<Map<String, Object>> logDocumentRejection(
            @RequestParam String username,
            @RequestParam Long bidId,
            @RequestParam String documentName,
            @RequestParam String reason) {
        
        try {
            ActivityLog log = activityLogService.logManualDocumentRejection(
                username, bidId, documentName, reason
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document rejection logged");
            response.put("activityId", log.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error logging document rejection: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    
    @GetMapping("/user/{username}")
    public ResponseEntity<Map<String, Object>> getUserActivityLogs(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Page<ActivityLog> logs = activityLogService.getUserActivityLogs(username, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("activities", logs.getContent());
            response.put("totalPages", logs.getTotalPages());
            response.put("totalElements", logs.getTotalElements());
            response.put("currentPage", logs.getNumber());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting user activity logs: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<Map<String, Object>> getEntityActivityLogs(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        
        try {
            List<ActivityLog> logs = activityLogService.getEntityActivityLogs(entityType, entityId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("activities", logs);
            response.put("count", logs.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting entity activity logs: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    
    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentActivities() {
        try {
            List<ActivityLog> logs = activityLogService.getRecentActivityLogs();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("activities", logs);
            response.put("count", logs.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting recent activities: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    
    @GetMapping("/report/bid/{bidId}")
    public ResponseEntity<Map<String, Object>> getBidSummaryReport(@PathVariable Long bidId) {
        try {
            SummaryReport report = summaryReportService.generateBidSummaryReport(bidId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("report", report);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating bid summary report: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
   
    @GetMapping("/report/tender/{tenderId}")
    public ResponseEntity<Map<String, Object>> getTenderSummaryReport(@PathVariable Long tenderId) {
        try {
            SummaryReport report = summaryReportService.generateTenderSummaryReport(tenderId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("report", report);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating tender summary report: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    
    @PostMapping("/validation-summary")
    public ResponseEntity<Map<String, Object>> logValidationSummary(
            @RequestParam String username,
            @RequestParam Long bidId,
            @RequestParam int foundCount,
            @RequestParam int missingCount,
            @RequestParam int duplicateCount,
            @RequestParam(required = false, defaultValue = "BID") String entityType,
            @RequestParam(required = false, defaultValue = "") String foundDocs,
            @RequestParam(required = false, defaultValue = "") String missingDocs,
            @RequestParam(required = false, defaultValue = "") String duplicateDocs) {
        
        try {
            // Parse comma-separated document names into lists
            java.util.List<String> foundDocsList = foundDocs.isEmpty() ? new java.util.ArrayList<>() : java.util.Arrays.asList(foundDocs.split(","));
            java.util.List<String> missingDocsList = missingDocs.isEmpty() ? new java.util.ArrayList<>() : java.util.Arrays.asList(missingDocs.split(","));
            java.util.List<String> duplicateDocsList = duplicateDocs.isEmpty() ? new java.util.ArrayList<>() : java.util.Arrays.asList(duplicateDocs.split(","));
            
            ActivityLog log = activityLogService.logValidationSummary(
                username, entityType, bidId, foundCount, missingCount, duplicateCount,
                foundDocsList, missingDocsList, duplicateDocsList
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Validation summary logged successfully");
            response.put("activityId", log.getId());
            response.put("description", log.getDescription());
            response.put("uploadCount", log.getUploadCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error logging validation summary: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    

     
    @PostMapping("/store-to-ipfs")
    public ResponseEntity<Map<String, Object>> storeValidationToIpfs(
            @RequestParam Long bidId) {
        
        try {
            // Generate scrutiny summary for this bid
            com.example.dto.ScrutinyResultSummary summary = scrutinySummaryService.generateBidScrutinySummary(bidId);
            
            if (summary == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Bid not found: " + bidId);
                return ResponseEntity.ok(response);
            }
            
            // Store to IPFS
            Map<String, Object> ipfsResponse = ipfsService.storeSingleSummaryToIpfs(summary);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", ipfsResponse.get("success"));
            response.put("ipfsHash", ipfsResponse.get("ipfsHash"));
            response.put("pinataUrl", ipfsResponse.get("pinataUrl"));
            response.put("message", ipfsResponse.get("message"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error storing validation to IPFS: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
   


    @PostMapping("/log-and-store")
    public ResponseEntity<Map<String, Object>> logAndStoreValidation(
            @RequestParam String username,
            @RequestParam Long bidId,
            @RequestParam int foundCount,
            @RequestParam int missingCount,
            @RequestParam int duplicateCount,
            @RequestParam(required = false, defaultValue = "BID") String entityType,
            @RequestParam(required = false, defaultValue = "") String foundDocs,
            @RequestParam(required = false, defaultValue = "") String missingDocs,
            @RequestParam(required = false, defaultValue = "") String duplicateDocs,
            @RequestParam(required = false, defaultValue = "false") boolean storeToIpfs) {
        
        try {
            // Parse comma-separated document names into lists
            java.util.List<String> foundDocsList = foundDocs.isEmpty() ? new java.util.ArrayList<>() : java.util.Arrays.asList(foundDocs.split(","));
            java.util.List<String> missingDocsList = missingDocs.isEmpty() ? new java.util.ArrayList<>() : java.util.Arrays.asList(missingDocs.split(","));
            java.util.List<String> duplicateDocsList = duplicateDocs.isEmpty() ? new java.util.ArrayList<>() : java.util.Arrays.asList(duplicateDocs.split(","));
            
            // Log validation summary
            ActivityLog log = activityLogService.logValidationSummary(
                username, entityType, bidId, foundCount, missingCount, duplicateCount,
                foundDocsList, missingDocsList, duplicateDocsList
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Validation summary logged successfully");
            response.put("activityId", log.getId());
            response.put("description", log.getDescription());
            response.put("uploadCount", log.getUploadCount());
            
            // Store to IPFS if requested
            if (storeToIpfs) {
                com.example.dto.ScrutinyResultSummary summary = scrutinySummaryService.generateBidScrutinySummary(bidId);
                if (summary != null) {
                    Map<String, Object> ipfsResponse = ipfsService.storeSingleSummaryToIpfs(summary);
                    response.put("ipfsHash", ipfsResponse.get("ipfsHash"));
                    response.put("pinataUrl", ipfsResponse.get("pinataUrl"));
                    response.put("ipfsStored", ipfsResponse.get("success"));
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error logging and storing validation: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    
    @PostMapping("/update-tender-to-bid")
    public ResponseEntity<Map<String, Object>> updateTenderToBid(
            @RequestParam Long tenderId,
            @RequestParam Long bidId,
            @RequestParam String username) {
        
        try {
            activityLogService.updateTenderActivitiesToBid(tenderId, bidId, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tender activities updated to bid successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating tender activities to bid: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    
    @PostMapping("/store-activity-ipfs/{activityId}")
    public ResponseEntity<Map<String, Object>> storeActivityToIpfs(
            @PathVariable Long activityId) {
        
        try {
            Map<String, Object> result = activityLogService.storeActivityToIpfs(activityId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error storing activity to IPFS: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
   
    @PostMapping("/store-all-validations-ipfs/{bidId}")
    public ResponseEntity<Map<String, Object>> storeAllValidationsToIpfs(
            @PathVariable Long bidId) {
        
        try {
            Map<String, Object> result = activityLogService.storeAllValidationsToIpfs(bidId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error storing all validations to IPFS: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
