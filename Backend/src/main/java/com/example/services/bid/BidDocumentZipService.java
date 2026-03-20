package com.example.services.bid;

import com.example.entity.Bid;
import com.example.services.document.ZipFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
public class BidDocumentZipService {

    private static final Logger logger = LoggerFactory.getLogger(BidDocumentZipService.class);

    @Autowired
    private ZipFileService zipFileService;

   
    public String createZipForBid(Bid bid, List<String> savedDocPaths) {
        logger.info("Creating ZIP for bid {} with {} documents", bid.getId(), savedDocPaths != null ? savedDocPaths.size() : 0);
        
        if (savedDocPaths == null || savedDocPaths.isEmpty()) {
            logger.debug("No documents to zip for bid {}", bid.getId());
            return null;
        }

        try {
            String prefix = "bid_" + bid.getId();
            logger.info("Calling createZipFromExistingFiles with prefix: {}", prefix);
            Map<String, Object> zipResult = zipFileService.createZipFromExistingFiles(savedDocPaths, prefix);
            
            logger.info("ZIP creation result: {}", zipResult);
            
            if ((Boolean) zipResult.get("success")) {
                String zipFilePath = (String) zipResult.get("zipFilePath");
                logger.info("ZIP file created successfully for bid {}: {}", bid.getId(), zipFilePath);
                return zipFilePath;
            } else {
                logger.warn("Failed to create ZIP for bid {}: {}", bid.getId(), zipResult.get("message"));
                return null;
            }
        } catch (Exception e) {
            logger.error("Error creating ZIP file for bid {}: ", bid.getId(), e);
            return null;
        }
    }

 
    public void updateBidWithZip(Bid bid, String zipFilePath, BidService bidService) {
        if (zipFilePath != null) {
            bid.setZipFilePath(zipFilePath);
            bid.setDocumentPath(zipFilePath);
            bidService.saveBid(bid);
            logger.info("Bid {} updated with ZIP file path: {}", bid.getId(), zipFilePath);
        }
    }
}
