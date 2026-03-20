package com.example.services.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for cleaning up uploaded documents after ZIP download
 * Ensures bidder documents are not retained in the system
 * Only summary logs and blockchain references remain
 */
@Service
public class DocumentCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentCleanupService.class);
    private static final String UPLOAD_DIR = "./bid-documents/";
    private static final String BACKEND_UPLOAD_DIR = "Backend/bid-documents/";

    /**
     * Delete individual PDF documents after ZIP is downloaded
     * Only deletes .pdf files, keeps .zip files for blockchain reference
     * 
     * @param documentPaths List of document paths to delete
     * @return CleanupResult with success status and details
     */
    public CleanupResult cleanupDocuments(List<String> documentPaths) {
        CleanupResult result = new CleanupResult();
        
        if (documentPaths == null || documentPaths.isEmpty()) {
            logger.info("No documents to cleanup");
            result.setSuccess(true);
            result.setMessage("No documents to cleanup");
            return result;
        }

        List<String> deletedFiles = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        logger.info("Starting cleanup for {} document paths", documentPaths.size());

        for (String docPath : documentPaths) {
            try {
                // Extract filename from path
                String fileName = docPath;
                if (docPath.contains("/")) {
                    fileName = docPath.substring(docPath.lastIndexOf("/") + 1);
                }

                // Build full file path - try multiple possible locations
                Path filePath = Paths.get(UPLOAD_DIR, fileName);
                
                // Check multiple possible paths
                if (!Files.exists(filePath)) {
                    filePath = Paths.get(BACKEND_UPLOAD_DIR, fileName);
                    logger.info("Trying Backend path: {}", filePath);
                }
                
                if (!Files.exists(filePath)) {
                    filePath = Paths.get("." + docPath);
                    logger.info("Trying relative path: {}", filePath);
                }
                
                if (!Files.exists(filePath)) {
                    filePath = Paths.get("Backend/bid-documents", fileName);
                    logger.info("Trying Backend/bid-documents: {}", filePath);
                }

                // Delete the file if it exists
                if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                    Files.delete(filePath);
                    deletedFiles.add(fileName);
                    logger.info("Deleted document: {}", fileName);
                } else {
                    logger.warn("File not found for deletion: {}", fileName);
                }

            } catch (IOException e) {
                logger.error("Failed to delete document: {}", docPath, e);
                failedFiles.add(docPath);
            }
        }

        result.setDeletedFiles(deletedFiles);
        result.setFailedFiles(failedFiles);
        result.setTotalDeleted(deletedFiles.size());
        result.setTotalFailed(failedFiles.size());
        
        if (failedFiles.isEmpty()) {
            result.setSuccess(true);
            result.setMessage("Successfully deleted " + deletedFiles.size() + " document(s)");
        } else {
            result.setSuccess(true); // Partial success
            result.setMessage("Deleted " + deletedFiles.size() + " document(s), " + failedFiles.size() + " failed");
        }

        logger.info("Cleanup completed: {} deleted, {} failed", deletedFiles.size(), failedFiles.size());
        return result;
    }

    /**
     * Delete all PDF and ZIP files for a specific bid
     * 
     * @param bidId The bid ID whose documents should be deleted
     * @return CleanupResult with success status
     */
    public CleanupResult cleanupBidDocuments(Long bidId) {
        logger.info("Cleaning up documents for bid: {}", bidId);
        
        List<String> pdfFiles = new ArrayList<>();
        
        // Search in both possible directories
        String[] dirs = {UPLOAD_DIR, BACKEND_UPLOAD_DIR};
        
        for (String dir : dirs) {
            try {
                Path uploadPath = Paths.get(dir);
                if (Files.exists(uploadPath)) {
                    Files.list(uploadPath)
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".pdf") || path.toString().endsWith(".zip"))
                        .filter(path -> path.toString().contains("bid_" + bidId + "_") || path.toString().contains("bid_" + bidId + "."))
                        .forEach(path -> {
                            String fileName = path.getFileName().toString();
                            pdfFiles.add("/bid-documents/" + fileName);
                            logger.info("Found file to delete: {}", fileName);
                        });
                }
            } catch (IOException e) {
                logger.error("Error listing files for bid {}: {}", bidId, e);
            }
        }

        return cleanupDocuments(pdfFiles);
    }

    /**
     * Delete specific file by filename
     * Can delete both PDF and ZIP files
     * 
     * @param fileName The filename to delete
     * @return true if deleted successfully
     */
    public boolean deleteFile(String fileName) {
        try {
            if (fileName == null || fileName.isEmpty()) {
                logger.warn("Empty filename provided for deletion");
                return false;
            }

            // Try multiple possible paths - be more aggressive
            Path filePath = null;
            
            // Direct path attempts
            String[] possiblePaths = {
                "./bid-documents/" + fileName,
                "Backend/bid-documents/" + fileName,
                fileName
            };
            
            for (String path : possiblePaths) {
                filePath = Paths.get(path);
                logger.info("Trying to delete from: {}", filePath.toAbsolutePath());
                if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                    logger.info("Found file at: {}", filePath.toAbsolutePath());
                    Files.delete(filePath);
                    logger.info("Successfully deleted: {}", fileName);
                    return true;
                }
            }
            
            logger.warn("File not found for deletion: {} (tried {} paths)", fileName, possiblePaths.length);
            return false;
            
        } catch (IOException e) {
            logger.error("Failed to delete file: {}", fileName, e);
            return false;
        }
    }

    /**
     * Result class for cleanup operations
     */
    public static class CleanupResult {
        private boolean success;
        private String message;
        private List<String> deletedFiles;
        private List<String> failedFiles;
        private int totalDeleted;
        private int totalFailed;

        public CleanupResult() {
            this.deletedFiles = new ArrayList<>();
            this.failedFiles = new ArrayList<>();
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public List<String> getDeletedFiles() { return deletedFiles; }
        public void setDeletedFiles(List<String> deletedFiles) { this.deletedFiles = deletedFiles; }
        
        public List<String> getFailedFiles() { return failedFiles; }
        public void setFailedFiles(List<String> failedFiles) { this.failedFiles = failedFiles; }
        
        public int getTotalDeleted() { return totalDeleted; }
        public void setTotalDeleted(int totalDeleted) { this.totalDeleted = totalDeleted; }
        
        public int getTotalFailed() { return totalFailed; }
        public void setTotalFailed(int totalFailed) { this.totalFailed = totalFailed; }
    }
}

