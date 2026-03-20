package com.example.controller;

import com.example.dto.ScrutinyResultSummary;
import com.example.entity.BlockchainTransaction;
import com.example.services.report.BlockchainTransactionService;
import com.example.services.report.CardanoService;
import com.example.services.report.IpfsService;
import com.example.services.report.ScrutinySummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/scrutiny")
@CrossOrigin(origins = "*")
public class ScrutinyController {
    
    private static final Logger logger = LoggerFactory.getLogger(ScrutinyController.class);
    
    @Autowired
    private ScrutinySummaryService scrutinySummaryService;
    
    @Autowired
    private IpfsService ipfsService;
    
    @Autowired
    private CardanoService cardanoService;
    
    @Autowired
    private BlockchainTransactionService blockchainTransactionService;
    
   
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllScrutinySummaries() {
        try {
            List<ScrutinyResultSummary> summaries = scrutinySummaryService.generateAllScrutinySummary();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", summaries);
            response.put("total", summaries.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting all scrutiny summaries: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    
    @GetMapping("/tender/{tenderId}")
    public ResponseEntity<Map<String, Object>> getScrutinyByTender(@PathVariable Long tenderId) {
        try {
            List<ScrutinyResultSummary> summaries = scrutinySummaryService.generateTenderScrutinySummary(tenderId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", summaries);
            response.put("total", summaries.size());
            response.put("tenderId", tenderId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting scrutiny summary for tender {}: ", tenderId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    
    @GetMapping("/bid/{bidId}")
    public ResponseEntity<Map<String, Object>> getScrutinyByBid(@PathVariable Long bidId) {
        try {
            ScrutinyResultSummary summary = scrutinySummaryService.generateBidScrutinySummary(bidId);
            
            Map<String, Object> response = new HashMap<>();
            if (summary != null) {
                response.put("success", true);
                response.put("data", summary);
            } else {
                response.put("success", false);
                response.put("message", "Bid not found: " + bidId);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting scrutiny summary for bid {}: ", bidId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
   
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getScrutinyByUser(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long tenderId) {
        try {
            List<ScrutinyResultSummary> summaries = scrutinySummaryService.generateUserChangeSummary(tenderId, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", summaries);
            response.put("total", summaries.size());
            
            if (username != null) {
                response.put("username", username);
            }
            if (tenderId != null) {
                response.put("tenderId", tenderId);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting user change summary: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
   
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getScrutinyStats() {
        try {
            List<ScrutinyResultSummary> summaries = scrutinySummaryService.generateAllScrutinySummary();
            
            int totalBids = summaries.size();
            int validBids = 0;
            int invalidBids = 0;
            int pendingBids = 0;
            int totalDocumentsFound = 0;
            int totalDocumentsMissing = 0;
            int totalDocumentsDuplicate = 0;
            
            for (ScrutinyResultSummary summary : summaries) {
                if ("VALID".equals(summary.getValidationStatus())) {
                    validBids++;
                } else if ("INVALID".equals(summary.getValidationStatus())) {
                    invalidBids++;
                } else {
                    pendingBids++;
                }
                
                totalDocumentsFound += summary.getTotalDocumentsFound() != null ? summary.getTotalDocumentsFound() : 0;
                totalDocumentsMissing += summary.getTotalDocumentsMissing() != null ? summary.getTotalDocumentsMissing() : 0;
                totalDocumentsDuplicate += summary.getTotalDocumentsDuplicate() != null ? summary.getTotalDocumentsDuplicate() : 0;
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalBids", totalBids);
            response.put("validBids", validBids);
            response.put("invalidBids", invalidBids);
            response.put("pendingBids", pendingBids);
            response.put("totalDocumentsFound", totalDocumentsFound);
            response.put("totalDocumentsMissing", totalDocumentsMissing);
            response.put("totalDocumentsDuplicate", totalDocumentsDuplicate);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting scrutiny stats: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
   
    @PostMapping("/store-ipfs")
    public ResponseEntity<Map<String, Object>> storeToIpfs() {
        try {
            List<ScrutinyResultSummary> summaries = scrutinySummaryService.generateAllScrutinySummary();
            
            Map<String, Object> ipfsResponse = ipfsService.storeSummaryToIpfs(summaries);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", ipfsResponse.get("success"));
            response.put("ipfsHash", ipfsResponse.get("ipfsHash"));
            response.put("pinataUrl", ipfsResponse.get("pinataUrl"));
            response.put("message", ipfsResponse.get("message"));
            response.put("totalRecords", summaries.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error storing to IPFS: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
   
    @PostMapping("/bid/{bidId}/store-ipfs")
    public ResponseEntity<Map<String, Object>> storeBidToIpfs(@PathVariable Long bidId) {
        try {
            ScrutinyResultSummary summary = scrutinySummaryService.generateBidScrutinySummary(bidId);
            
            if (summary == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Bid not found: " + bidId);
                return ResponseEntity.ok(response);
            }
            
            Map<String, Object> ipfsResponse = ipfsService.storeSingleSummaryToIpfs(summary);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", ipfsResponse.get("success"));
            response.put("ipfsHash", ipfsResponse.get("ipfsHash"));
            response.put("pinataUrl", ipfsResponse.get("pinataUrl"));
            response.put("message", ipfsResponse.get("message"));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error storing bid to IPFS: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
   
    @PostMapping("/final-review")
    public ResponseEntity<Map<String, Object>> finalReview() {
        try {
            // Step 1: Generate all scrutiny summaries
            List<ScrutinyResultSummary> summaries = scrutinySummaryService.generateAllScrutinySummary();
            
            if (summaries.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No summaries found to review");
                return ResponseEntity.ok(response);
            }
            
            // Step 2: Generate SHA-256 cryptographic hash of the summary
            String summaryHash = cardanoService.generateHash(summaries);
            
            // Step 3: Store to IPFS via Pinata
            Map<String, Object> ipfsResponse = ipfsService.storeSummaryToIpfs(summaries);
            String ipfsHash = (String) ipfsResponse.get("ipfsHash");
            
            // Step 4: Write hash to Cardano Testnet
            Map<String, Object> cardanoResponse = cardanoService.writeHashToBlockchain(
                ipfsHash != null ? ipfsHash : "", 
                summaryHash, 
                summaries
            );
            
            // Step 5: Prepare final response
            boolean cardanoSuccess = cardanoResponse != null && Boolean.TRUE.equals(cardanoResponse.get("success"));
            
            // Extract transaction details
            String txId = cardanoResponse != null ? (String) cardanoResponse.get("transactionId") : null;
            String blockchainUrl = cardanoResponse != null ? (String) cardanoResponse.get("metadataUrl") : null;
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", cardanoSuccess);
            response.put("message", cardanoSuccess ? "Final review completed - Summary hash written to Cardano Testnet" : "Review completed but blockchain write failed");
            response.put("totalRecords", summaries.size());
            response.put("summaryHash", summaryHash);
            response.put("ipfsHash", ipfsHash);
            response.put("ipfsUrl", ipfsResponse.get("pinataUrl"));
            
            // Blockchain transaction details
            if (txId != null && !txId.startsWith("pending_")) {
                response.put("transactionId", txId);
                response.put("blockchainUrl", blockchainUrl);
                response.put("blockchainNetwork", "Cardano Preprod Testnet");
                response.put("blockchainStatus", "CONFIRMED");
            } else {
                response.put("transactionId", txId);
                response.put("blockchainUrl", null);
                response.put("blockchainNetwork", "Cardano Preprod Testnet");
                response.put("blockchainStatus", "PENDING");
            }
            
            response.put("timestamp", new java.util.Date().toString());
            
            // Step 6: Save transaction to database (always save for debugging)
            String txStatus = cardanoSuccess ? "COMPLETED" : "FAILED";
            String errorMsg = cardanoResponse != null ? (String) cardanoResponse.get("message") : "No response from Cardano service";
            
            try {
                BlockchainTransaction savedTx = blockchainTransactionService.saveTransaction(
                    null, // tenderId
                    null, // bidId
                    summaryHash,
                    ipfsHash,
                    (String) ipfsResponse.get("pinataUrl"),
                    txId,
                    blockchainUrl,
                    summaries.size(),
                    "system"
                );
                
                if (savedTx != null) {
                    logger.info("Blockchain transaction saved to database with ID: {}, Status: {}", savedTx.getId(), txStatus);
                    response.put("dbTransactionId", savedTx.getId());
                    response.put("dbStatus", savedTx.getStatus());
                } else {
                    logger.warn("Blockchain transaction save returned null");
                    response.put("dbStatus", "SAVE_FAILED");
                }
            } catch (Exception e) {
                logger.error("Error saving blockchain transaction to database: ", e);
                response.put("dbStatus", "ERROR: " + e.getMessage());
            }
            
            logger.info("Final review completed - TX ID: {}, Status: {}, DB Saved: true", txId, txStatus);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error in final review: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    
    @GetMapping("/hash")
    public ResponseEntity<Map<String, Object>> getHash() {
        try {
            List<ScrutinyResultSummary> summaries = scrutinySummaryService.generateAllScrutinySummary();
            String hash = cardanoService.generateHash(summaries);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("summaryHash", hash);
            response.put("totalRecords", summaries.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating hash: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
   
    @GetMapping("/blockchain/transactions")
    public ResponseEntity<Map<String, Object>> getAllBlockchainTransactions() {
        try {
            List<com.example.entity.BlockchainTransaction> transactions = 
                blockchainTransactionService.getAllTransactions();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transactions);
            response.put("total", transactions.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting blockchain transactions: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    
    @GetMapping("/blockchain/tender/{tenderId}")
    public ResponseEntity<Map<String, Object>> getBlockchainTransactionByTender(@PathVariable Long tenderId) {
        try {
            List<com.example.entity.BlockchainTransaction> transactions = 
                blockchainTransactionService.getTransactionsByTender(tenderId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transactions);
            response.put("total", transactions.size());
            response.put("tenderId", tenderId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting blockchain transactions for tender: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
