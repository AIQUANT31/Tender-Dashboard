package com.example.controller;

import com.example.entity.Bid;
import com.example.entity.BlockchainTransaction;
import com.example.repository.BlockchainTransactionRepository;
import com.example.services.bid.BidService;
import com.example.services.bid.BidDocumentZipService;
import com.example.services.document.DocumentCleanupService;
import com.example.services.document.ZipFileService;
import com.example.services.report.CardanoService;
import com.example.services.report.IpfsService;
import com.example.services.report.ScrutinySummaryService;
import com.example.services.report.BlockchainTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.net.MalformedURLException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bids")
@CrossOrigin(origins = "*")
public class BidDocumentController {

    private static final Logger logger = LoggerFactory.getLogger(BidDocumentController.class);
    private static final String UPLOAD_DIR = "./bid-documents/";

    @Autowired
    private BidService bidService;
    
    @Autowired
    private ZipFileService zipFileService;
    
    @Autowired
    private BidDocumentZipService bidDocumentZipService;
    
    @Autowired
    private DocumentCleanupService documentCleanupService;
    
    @Autowired
    private CardanoService cardanoService;
    
    @Autowired
    private IpfsService ipfsService;
    
    @Autowired
    private ScrutinySummaryService scrutinySummaryService;
    
    @Autowired
    private BlockchainTransactionService blockchainTransactionService;
    
    @Autowired
    private BlockchainTransactionRepository blockchainTransactionRepository;
    
    @PostMapping(value = "/create-with-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createBidWithDocument(
            @RequestParam("tenderId") Long tenderId,
            @RequestParam("bidderId") Long bidderId,
            @RequestParam("bidAmount") java.math.BigDecimal bidAmount,
            @RequestParam(value = "proposalText", required = false) String proposalText,
                        @RequestParam(value = "contactNumber", required = false) String contactNumber,
            @RequestParam(value = "status", defaultValue = "PENDING") String status,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            
            @RequestParam(value = "file", required = false) MultipartFile file) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
         
            com.example.dto.BidRequest request = new com.example.dto.BidRequest();
            request.setTenderId(tenderId);
            request.setBidderId(bidderId);
            request.setBidAmount(bidAmount);
            request.setProposalText(proposalText);
            request.setContactNumber(contactNumber);
            request.setStatus(status);
            
            
            Map<String, Object> bidResponse = bidService.createBid(request);
            
            if (!(Boolean) bidResponse.get("success")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(bidResponse);
            }
            
            Bid savedBid = (Bid) bidResponse.get("bid");
            
            
            List<MultipartFile> allFiles = new ArrayList<>();
            if (files != null) {
                for (MultipartFile f : files) {
                    if (f != null && !f.isEmpty()) allFiles.add(f);
                }
            }
            if (file != null && !file.isEmpty()) {
                allFiles.add(file);
            }

            if (!allFiles.isEmpty()) {
                List<String> savedDocPaths = new ArrayList<>();

                for (MultipartFile f : allFiles) {
                    // Use validateAndSaveFile to eliminate duplicate validation and saving code
                    String docPath = validateAndSaveFile(f, savedBid.getId());
                    if (docPath == null) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("success", false, "message", "Only PDF files under 50MB are allowed"));
                    }
                    savedDocPaths.add(docPath);
                }

                // Save document paths first
                String jsonPaths = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(savedDocPaths);
                savedBid.setDocumentPaths(jsonPaths);
                
                // Create ZIP file automatically using BidDocumentZipService
                String zipFilePath = bidDocumentZipService.createZipForBid(savedBid, savedDocPaths);
                
                // Update bid with ZIP file info if available, otherwise set first document path
                if (zipFilePath != null) {
                    bidDocumentZipService.updateBidWithZip(savedBid, zipFilePath, bidService);
                } else if (!savedDocPaths.isEmpty()) {
                    savedBid.setDocumentPath(savedDocPaths.get(0));
                    bidService.saveBid(savedBid);
                }

                bidResponse.put("documentPath", savedBid.getDocumentPath());
                bidResponse.put("documentPaths", jsonPaths);
                if (zipFilePath != null) {
                    bidResponse.put("zipFilePath", zipFilePath);
                }
            }
            
            try {
                // Generate scrutiny summary for this bid
                List<com.example.dto.ScrutinyResultSummary> summaries = new ArrayList<>();
                com.example.dto.ScrutinyResultSummary summary = scrutinySummaryService.generateBidScrutinySummary(savedBid.getId());
                if (summary != null) {
                    summaries.add(summary);
                }
                
                // If no summary exists, create a basic one from bid data
                if (summaries.isEmpty()) {
                    com.example.dto.ScrutinyResultSummary basicSummary = new com.example.dto.ScrutinyResultSummary();
                    basicSummary.setBidId(savedBid.getId());
                    basicSummary.setTenderId(savedBid.getTenderId());
                    basicSummary.setValidationStatus("PENDING");
                    basicSummary.setRemarks("Bid submitted - awaiting scrutiny");
                    summaries.add(basicSummary);
                }
                
                // Generate SHA-256 hash of the bid summary
                String summaryHash = cardanoService.generateHash(summaries);
                
                // Store to IPFS
                Map<String, Object> ipfsResponse = ipfsService.storeSingleSummaryToIpfs(summaries.get(0));
                String ipfsHash = (String) ipfsResponse.get("ipfsHash");
                
                // Write hash to Cardano testnet using Lucid
                Map<String, Object> cardanoResponse = cardanoService.writeHashToBlockchain(
                    ipfsHash != null ? ipfsHash : "",
                    summaryHash,
                    summaries
                );
                
                String transactionId = (String) cardanoResponse.get("transactionId");
                String blockchainUrl = (String) cardanoResponse.get("metadataUrl");
                Boolean cardanoSuccess = (Boolean) cardanoResponse.get("success");
                String cardanoMessage = (String) cardanoResponse.get("message");
                
                logger.info("Cardano response (create-with-document) - success: {}, transactionId: {}, message: {}", 
                    cardanoSuccess, transactionId, cardanoMessage);
                
                // Only save blockchain transaction if it was successful and we have a valid transactionId
                if (cardanoSuccess != null && cardanoSuccess && transactionId != null && !transactionId.isEmpty()) {
                    // Save transaction to database
                    BlockchainTransaction savedTx = blockchainTransactionService.saveTransaction(
                        savedBid.getTenderId(),
                        savedBid.getId(),
                        summaryHash,
                        ipfsHash,
                        (String) ipfsResponse.get("pinataUrl"),
                        transactionId,
                        blockchainUrl,
                        summaries.size(),
                        "bidder"
                    );
                    
                    if (savedTx != null) {
                        logger.info("Blockchain transaction saved to DB (create-with-document) - TX ID: {}, DB ID: {}", transactionId, savedTx.getId());
                        
                        // Fetch fresh bid from database and update with blockchain transaction hash
                        Bid bidFromDb = bidService.getBidById(savedBid.getId());
                        if (bidFromDb != null) {
                            bidFromDb.setBlockchainTxHash(transactionId);
                            bidFromDb.setIpfsHash(ipfsHash);
                            bidFromDb.setBlockchainTimestamp(java.time.LocalDateTime.now());
                            bidService.saveBid(bidFromDb);
                            logger.info("Bid {} updated with blockchain tx hash (create-with-document): {}", bidFromDb.getId(), transactionId);
                        }
                    }
                    
                    // Add blockchain info to response for popup display
                    bidResponse.put("blockchainTransactionId", transactionId);
                    bidResponse.put("blockchainUrl", blockchainUrl);
                    bidResponse.put("summaryHash", summaryHash);
                    bidResponse.put("ipfsHash", ipfsHash);
                    bidResponse.put("blockchainNetwork", "Cardano Preprod Testnet");
                    bidResponse.put("message", "Bid placed successfully! Hash written to Cardano blockchain.");
                    
                    logger.info("Bid {} submitted with document - Transaction ID: {}", savedBid.getId(), transactionId);
                } else {
                    // Blockchain transaction failed or returned no transactionId
                    logger.error("Blockchain transaction failed (create-with-document) - success: {}, transactionId: {}, message: {}", 
                        cardanoSuccess, transactionId, cardanoMessage);
                    
                    // Still save the transaction attempt with FAILED status for tracking
                    BlockchainTransaction savedTx = blockchainTransactionService.saveTransaction(
                        savedBid.getTenderId(),
                        savedBid.getId(),
                        summaryHash,
                        ipfsHash,
                        (String) ipfsResponse.get("pinataUrl"),
                        transactionId != null ? transactionId : "FAILED",
                        blockchainUrl,
                        summaries.size(),
                        "bidder"
                    );
                    
                    // Update transaction status to FAILED
                    if (savedTx != null) {
                        savedTx.setStatus("FAILED");
                        savedTx.setCardanoTransactionId(cardanoMessage);
                        blockchainTransactionRepository.save(savedTx);
                    }
                    
                    // Add warning to response but don't fail the bid creation
                    bidResponse.put("blockchainWarning", "Bid created but blockchain recording failed: " + cardanoMessage);
                    bidResponse.put("message", "Bid placed successfully! (Blockchain recording failed)");
                }
                
            } catch (Exception e) {
                logger.error("Error writing to blockchain (create-with-document): ", e);
                // Don't fail the bid creation if blockchain write fails
                bidResponse.put("blockchainWarning", "Bid created but blockchain recording failed: " + e.getMessage());
            }
            
            return ResponseEntity.ok(bidResponse);
            
        } catch (Exception e) {
            logger.error("Error creating bid with document: ", e);
            response.put("success", false);
            response.put("message", "Error creating bid: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping(value = "/add-documents/{bidId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> addDocumentsToBid(
            @PathVariable Long bidId,
            @RequestParam("files") MultipartFile[] files) {
        
        Map<String, Object> response = new HashMap<>();
        List<String> savedFileNames = new ArrayList<>();
        
        try {
            Bid bid = bidService.getBidById(bidId);
            if (bid == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Bid not found"));
            }
            
        
            List<String> allPaths = parseDocumentPaths(bid.getDocumentPaths());

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                
                // Use validateAndSaveFile to eliminate duplicate validation and saving code
                String docPath = validateAndSaveFile(file, bidId);
                if (docPath == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Only PDF files under 50MB are allowed"));
                }
                if (docPath != null) {
                    savedFileNames.add(docPath);
                    allPaths.add(docPath);
                }
            }
            
            // Create ZIP file automatically using BidDocumentZipService
            String zipFilePath = bidDocumentZipService.createZipForBid(bid, allPaths);
            
            // Update bid with ZIP file info
            if (zipFilePath != null) {
                bidDocumentZipService.updateBidWithZip(bid, zipFilePath, bidService);
            }
            
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonPaths = mapper.writeValueAsString(allPaths);
            bid.setDocumentPaths(jsonPaths);
            bidService.saveBid(bid);
            
            response.put("success", true);
            response.put("message", "Documents uploaded successfully");
            response.put("fileNames", savedFileNames);
            response.put("documentPaths", jsonPaths);
            if (zipFilePath != null) {
                response.put("zipFilePath", zipFilePath);
            }
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error uploading documents: ", e);
            response.put("success", false);
            response.put("message", "Error uploading documents: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

 
    @PostMapping(value = "/upload-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDocuments(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "bidId", required = false) Long bidId) {
        
        Map<String, Object> response = new HashMap<>();
        List<String> savedFileNames = new ArrayList<>();
        
        logger.info("Uploading {} files", files != null ? files.length : 0);
        
        if (files == null || files.length == 0) {
            response.put("success", false);
            response.put("message", "No files provided");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                
                
                if (!isValidPdfFile(file, 50 * 1024 * 1024)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Only PDF files under 50MB are allowed"));
                }

                
                String docPath = saveFile(file, "bid_" + (bidId != null ? bidId : "temp"));
                if (docPath != null) {
                    savedFileNames.add(docPath);
                    logger.info("File saved: {}", file.getOriginalFilename());
                }
            }

            
            if (bidId != null) {
                Bid bid = bidService.getBidById(bidId);
                if (bid != null) {
                    List<String> allPaths = parseDocumentPaths(bid.getDocumentPaths());
                    allPaths.addAll(savedFileNames);
                    
                    
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String jsonPaths = mapper.writeValueAsString(allPaths);
                    bid.setDocumentPaths(jsonPaths);
                    bidService.saveBid(bid);
                }
            }
            
            response.put("success", true);
            response.put("message", "Documents uploaded successfully");
            response.put("fileNames", savedFileNames);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error uploading documents: ", e);
            response.put("success", false);
            response.put("message", "Error uploading documents: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

   
    @PostMapping("/upload-document")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bidId") Long bidId) {
        
        try {
            
            if (!isValidPdfFile(file, 10 * 1024 * 1024)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Only PDF files under 10MB are allowed"));
            }

            
            String docPath = saveFile(file, "bid_" + bidId);
            if (docPath == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error saving file"));
            }

            
            Bid bid = bidService.getBidById(bidId);
            if (bid == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Bid not found"));
            }

            bid.setDocumentPath(docPath);
            bidService.saveBid(bid);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document uploaded successfully",
                "documentPath", docPath
            ));

        } catch (IOException e) {
            logger.error("Error uploading document: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error uploading document"));
        }
    }


    @GetMapping("/download-document/{bidId}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long bidId, 
            @RequestParam(value = "fileName", required = false) String fileName) {
        try {
            Bid bid = bidService.getBidById(bidId);
            if (bid == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath;
            String downloadFileName;

            // First try to get path from database
            if (fileName != null && !fileName.isEmpty()) {
                String docPaths = bid.getDocumentPaths();
                if (docPaths != null && docPaths.contains(fileName)) {
                    filePath = Paths.get("Backend/bid-documents/" + fileName);
                    downloadFileName = fileName;
                } else if (bid.getDocumentPath() != null && bid.getDocumentPath().contains(fileName)) {
                    String cleanPath = bid.getDocumentPath().startsWith("./") ? 
                        bid.getDocumentPath().substring(2) : bid.getDocumentPath();
                    filePath = Paths.get("Backend" + cleanPath);
                    downloadFileName = bid.getDocumentPath().substring(bid.getDocumentPath().lastIndexOf("/") + 1);
                } else {
                    // Fallback: try to find file directly in folder
                    filePath = Paths.get("Backend/bid-documents/" + fileName);
                    downloadFileName = fileName;
                    if (!Files.exists(filePath)) {
                        return ResponseEntity.notFound().build();
                    }
                }
            } else {
                
                if (bid.getDocumentPath() == null) {
                    return ResponseEntity.notFound().build();
                }
                String cleanPath = bid.getDocumentPath().startsWith("./") ? 
                    bid.getDocumentPath().substring(2) : bid.getDocumentPath();
                filePath = Paths.get("Backend" + cleanPath);
                downloadFileName = bid.getDocumentPath().substring(bid.getDocumentPath().lastIndexOf("/") + 1);
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Determine content type based on file extension
                MediaType contentType = MediaType.APPLICATION_PDF;
                boolean isZipFile = downloadFileName.toLowerCase().endsWith(".zip");
                
                if (isZipFile) {
                    contentType = MediaType.APPLICATION_OCTET_STREAM;
                }
                
                // Schedule cleanup AFTER download completes (delete after user downloads)
                final Long bidIdForCleanup = bidId;
                final String zipFileNameForCleanup = downloadFileName;
                
                if (isZipFile) {
                    logger.info("Scheduling cleanup for bid {} after download completes...", bidId);
                    
                    // Run cleanup in background after a delay to allow download to complete
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000); // Wait 3 seconds for download to finish
                            
                            logger.info("Starting cleanup for bid {}...", bidIdForCleanup);
                            
                            // Delete all PDF files for this bid
                            documentCleanupService.cleanupBidDocuments(bidIdForCleanup);
                            
                            // Delete the ZIP file too
                            documentCleanupService.deleteFile(zipFileNameForCleanup);
                            
                            logger.info("Cleanup completed - all documents deleted for bid {}", bidIdForCleanup);
                        } catch (Exception e) {
                            logger.error("Cleanup error for bid {}: {}", bidIdForCleanup, e.getMessage());
                        }
                    }).start();
                }
                
                return ResponseEntity.ok()
                    .contentType(contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + downloadFileName + "\"")
                    .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String validateAndSaveFile(MultipartFile file, Long bidId) throws IOException {
        // Use 50MB limit consistent with other methods in this controller
        if (!isValidPdfFile(file, 50 * 1024 * 1024)) {
            return null;
        }

        return saveFile(file, "bid_" + bidId);
    }

    private boolean isValidPdfFile(MultipartFile file, long maxSize) {
        if (file == null || file.isEmpty()) return false;
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        boolean isPdfByContentType = "application/pdf".equalsIgnoreCase(contentType);
        boolean isPdfByExtension = originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf");
        return (isPdfByContentType || isPdfByExtension) && file.getSize() <= maxSize;
    }

    private String saveFile(MultipartFile file, String prefix) throws IOException {
        
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "document.pdf";
        }
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = prefix + "_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000) + fileExtension;

        
        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/bid-documents/" + newFilename;
    }

    private List<String> parseDocumentPaths(String existingPaths) {
        List<String> allPaths = new ArrayList<>();
        if (existingPaths != null && !existingPaths.isEmpty()) {
            try {
                
                List<?> parsed = new com.fasterxml.jackson.databind.ObjectMapper().readValue(existingPaths, List.class);
                for (Object p : parsed) {
                    allPaths.add(p.toString());
                }
            } catch (Exception e) {
                
                String clean = existingPaths.replace("[", "").replace("\"", "").replace("]", "");
                for (String p : clean.split(",")) {
                    if (!p.trim().isEmpty()) allPaths.add(p.trim());
                }
            }
        }
        return allPaths;
    }

   
    @PostMapping(value = "/upload-as-zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFilesAsZip(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "bidId", required = false) Long bidId,
            @RequestParam(value = "prefix", defaultValue = "bidder_docs") String prefix) {
        
        Map<String, Object> response = new HashMap<>();
        
        logger.info("Received {} files for ZIP conversion", files != null ? files.length : 0);
        
        if (files == null || files.length == 0) {
            response.put("success", false);
            response.put("message", "No files provided");
            return ResponseEntity.badRequest().body(response);
        }
        
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                if (!isValidPdfFile(file, 50 * 1024 * 1024)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", 
                                "Only PDF files under 50MB are allowed: " + file.getOriginalFilename()));
                }
            }
        }
        
        try {
            // Create ZIP from uploaded files
            String actualPrefix = prefix + (bidId != null ? "_" + bidId : "");
            Map<String, Object> zipResult = zipFileService.createZipFromFiles(files, actualPrefix);
            
            if ((Boolean) zipResult.get("success")) {
               
                if (bidId != null) {
                    Bid bid = bidService.getBidById(bidId);
                    if (bid != null) {
                        List<String> savedDocPaths = new ArrayList<>();
                        
                        for (MultipartFile f : files) {
                            if (f != null && !f.isEmpty()) {
                                
                                String docPath = validateAndSaveFile(f, bidId);
                                if (docPath != null) {
                                    savedDocPaths.add(docPath);
                                }
                            }
                        }
                        
                        // Update bid with document paths
                        List<String> allPaths = parseDocumentPaths(bid.getDocumentPaths());
                        allPaths.addAll(savedDocPaths);
                        allPaths.add((String) zipResult.get("zipFilePath"));
                        
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        bid.setDocumentPaths(mapper.writeValueAsString(allPaths));
                        bidService.saveBid(bid);
                        
                        response.put("bidId", bidId);
                        response.put("individualFileCount", savedDocPaths.size());
                    }
                }
                
                response.put("success", true);
                response.put("message", zipResult.get("message"));
                response.put("zipFileName", zipResult.get("zipFileName"));
                response.put("zipFilePath", zipResult.get("zipFilePath"));
                response.put("originalFileCount", zipResult.get("originalFileCount"));
                response.put("totalSize", zipResult.get("totalSize"));
                response.put("zipFileSize", zipResult.get("zipFileSize"));
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(zipResult);
            }
            
        } catch (Exception e) {
            logger.error("Error creating ZIP from uploaded files: ", e);
            response.put("success", false);
            response.put("message", "Error processing files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    
    @PostMapping("/create-zip/{bidId}")
    public ResponseEntity<Map<String, Object>> createZipFromExistingDocuments(
            @PathVariable Long bidId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Bid bid = bidService.getBidById(bidId);
            if (bid == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Bid not found"));
            }
            
            List<String> filePaths = parseDocumentPaths(bid.getDocumentPaths());
            
            if (filePaths.isEmpty()) {
                response.put("success", false);
                response.put("message", "No documents found for this bid");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> zipResult = zipFileService.createZipFromExistingFiles(filePaths, "bid_" + bidId);
            
            if ((Boolean) zipResult.get("success")) {
                response.put("success", true);
                response.put("message", zipResult.get("message"));
                response.put("zipFileName", zipResult.get("zipFileName"));
                response.put("zipFilePath", zipResult.get("zipFilePath"));
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(zipResult);
            }
            
        } catch (Exception e) {
            logger.error("Error creating ZIP from existing documents: ", e);
            response.put("success", false);
            response.put("message", "Error creating ZIP: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

//    download zip file 
    @GetMapping("/download-zip")
    public ResponseEntity<Resource> downloadZip(
            @RequestParam("fileName") String fileName) {
        try {
            // If fileName is provided, try to find matching file
            Path zipPath = null;
            
            if (fileName != null && !fileName.isEmpty()) {
                // Clean the filename
                String cleanName = fileName.replace("./", "");
                
                // Try direct path first
                zipPath = Paths.get("Backend/bid-documents/" + cleanName);
                if (!Files.exists(zipPath)) {
                    zipPath = Paths.get("./bid-documents/" + cleanName);
                }
                
                // If still not found, search for matching file by prefix
                if (!Files.exists(zipPath)) {
                    String prefix = cleanName.contains(".zip") ? 
                        cleanName.substring(0, cleanName.indexOf(".zip")) : cleanName;
                    
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                            Paths.get("Backend/bid-documents"), 
                            prefix + "*.zip")) {
                        for (Path entry : stream) {
                            zipPath = entry;
                            break;
                        }
                    } catch (Exception e) {
                        // Try alternate directory
                    }
                }
            }
            
            if (zipPath == null || !Files.exists(zipPath)) {
                return ResponseEntity.notFound().build();
            }
            
            if (!Files.exists(zipPath) || !zipPath.toString().endsWith(".zip")) {
                return ResponseEntity.notFound().build();
            }
            
            // Extract bidId from filename (format: bid_{bidId}_{timestamp}_{random}.zip)
            Long bidId = null;
            if (fileName.startsWith("bid_")) {
                try {
                    String[] parts = fileName.split("_");
                    if (parts.length >= 2) {
                        bidId = Long.parseLong(parts[1]);
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Could not extract bidId from filename: {}", fileName);
                }
            }
            
            Resource resource = new UrlResource(zipPath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                // Schedule cleanup AFTER download completes (delete after user downloads)
                final Long bidIdForCleanup = bidId;
                final String zipFileNameForCleanup = fileName;
                
                if (bidId != null) {
                    logger.info("Scheduling cleanup for bid {} after download completes...", bidId);
                    
                    // Run cleanup in background after a delay to allow download to complete
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000); // Wait 3 seconds for download to finish
                            
                            logger.info("Starting cleanup for bid {}...", bidIdForCleanup);
                            
                            // Delete all PDF files for this bid
                            documentCleanupService.cleanupBidDocuments(bidIdForCleanup);
                            
                            // Delete the ZIP file too
                            documentCleanupService.deleteFile(zipFileNameForCleanup);
                            
                            logger.info("Cleanup completed - all documents deleted for bid {}", bidIdForCleanup);
                        } catch (Exception e) {
                            logger.error("Cleanup error for bid {}: {}", bidIdForCleanup, e.getMessage());
                        }
                    }).start();
                }
                
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

// Test endpoint for cleanup
@RestController
@RequestMapping("/api/test")
class TestController {
    
    @Autowired
    private DocumentCleanupService documentCleanupService;
    
    @Autowired
    private BidService bidService;
    
    @GetMapping("/cleanup/{bidId}")
    public ResponseEntity<Map<String, Object>> testCleanup(@PathVariable Long bidId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            DocumentCleanupService.CleanupResult result = 
                documentCleanupService.cleanupBidDocuments(bidId);
            
            Bid bid = bidService.getBidById(bidId);
            if (bid != null) {
                bid.setDocumentPaths("[]");
                bidService.saveBid(bid);
            }
            
            response.put("success", true);
            response.put("message", result.getMessage());
            response.put("deletedFiles", result.getDeletedFiles());
            response.put("totalDeleted", result.getTotalDeleted());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
