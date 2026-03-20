package com.example.services.report;

import com.example.dto.ScrutinyResultSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class IpfsService {
    
    private static final Logger logger = LoggerFactory.getLogger(IpfsService.class);
    
    @Value("${pinata.api.key:}")
    private String pinataApiKey;
    
    @Value("${pinata.api.secret:}")
    private String pinataApiSecret;
    
    private final String PINATA_API_URL = "https://api.pinata.cloud/pinning/pinJSONToIPFS";
    private final RestTemplate restTemplate = new RestTemplate();  // to easy communication
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    
    public Map<String, Object> storeSummaryToIpfs(List<ScrutinyResultSummary> summaries) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Prepare JSON data
            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("scrutinySummaries", summaries);
            jsonData.put("timestamp", java.time.LocalDateTime.now().toString());
            jsonData.put("totalRecords", summaries.size());
            
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Pinata v3 uses JWT token in Authorization header
            headers.set("Authorization", "Bearer " + pinataApiSecret);
            
            logger.info("IPFS Pinata request - using JWT auth");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(jsonData, headers);
            
            // Make API call
            ResponseEntity<Map> apiResponse = restTemplate.exchange(
                PINATA_API_URL,
                HttpMethod.POST,
                request,
                Map.class
            );
            
            logger.info("IPFS response status: {}", apiResponse.getStatusCode());
            
            if (apiResponse.getStatusCode() == HttpStatus.OK || apiResponse.getStatusCode() == HttpStatus.CREATED) {
                Map<String, Object> body = apiResponse.getBody();
                if (body != null) {
                    String ipfsHash = (String) body.get("IpfsHash");
                    response.put("success", true);
                    response.put("ipfsHash", ipfsHash);
                    response.put("pinataUrl", "https://gateway.pinata.cloud/ipfs/" + ipfsHash);
                    response.put("message", "Summary stored to IPFS successfully");
                    logger.info("Summary stored to IPFS with hash: {}", ipfsHash);
                }
            } else {
                response.put("success", false);
                response.put("message", "Failed to store to IPFS: " + apiResponse.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error storing to IPFS: ", e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return response;
    }
    
 
    public Map<String, Object> storeSingleSummaryToIpfs(ScrutinyResultSummary summary) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Prepare JSON data
            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("id", summary.getId());
            jsonData.put("tenderId", summary.getTenderId());
            jsonData.put("bidId", summary.getBidId());
            jsonData.put("bidderName", summary.getBidderName());
            jsonData.put("documentMissing", summary.getDocumentMissing());
            jsonData.put("documentValidate", summary.getDocumentValidate());
            jsonData.put("duplicateDoc", summary.getDuplicateDoc());
            jsonData.put("duplicateCount", summary.getDuplicateCount());
            jsonData.put("changeCount", summary.getChangeCount());
            jsonData.put("totalDocumentsFound", summary.getTotalDocumentsFound());
            jsonData.put("totalDocumentsMissing", summary.getTotalDocumentsMissing());
            jsonData.put("totalDocumentsDuplicate", summary.getTotalDocumentsDuplicate());
            jsonData.put("validationStatus", summary.getValidationStatus());
            jsonData.put("remarks", summary.getRemarks());
            jsonData.put("timestamp", summary.getTimestamp() != null ? summary.getTimestamp().toString() : null);
            jsonData.put("changedBy", summary.getChangedBy());
            jsonData.put("storedAt", java.time.LocalDateTime.now().toString());
            
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Pinata v3 uses JWT token in Authorization header
            headers.set("Authorization", "Bearer " + pinataApiSecret);
            
            logger.info("IPFS single summary request - API Key present: {}, using JWT auth", 
                pinataApiKey != null && !pinataApiKey.isEmpty() ? "yes" : "no");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(jsonData, headers);
            
            // Make API call
            ResponseEntity<Map> apiResponse = restTemplate.exchange(
                PINATA_API_URL,
                HttpMethod.POST,
                request,
                Map.class
            );
            
            logger.info("IPFS single summary response status: {}", apiResponse.getStatusCode());
            
            if (apiResponse.getStatusCode() == HttpStatus.OK || apiResponse.getStatusCode() == HttpStatus.CREATED) {
                Map<String, Object> body = apiResponse.getBody();
                if (body != null) {
                    String ipfsHash = (String) body.get("IpfsHash");
                    response.put("success", true);
                    response.put("ipfsHash", ipfsHash);
                    response.put("pinataUrl", "https://gateway.pinata.cloud/ipfs/" + ipfsHash);
                    response.put("message", "Summary stored to IPFS successfully");
                    logger.info("Summary stored to IPFS with hash: {}", ipfsHash);
                }
            } else {
                response.put("success", false);
                response.put("message", "Failed to store to IPFS: " + apiResponse.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error storing to IPFS: ", e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return response;
    }
    
   
    public Map<String, Object> getSummaryFromIpfs(String ipfsHash) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String gatewayUrl = "https://gateway.pinata.cloud/ipfs/" + ipfsHash;
            
            ResponseEntity<Map> apiResponse = restTemplate.getForEntity(gatewayUrl, Map.class);
            
            if (apiResponse.getStatusCode() == HttpStatus.OK) {
                response.put("success", true);
                response.put("data", apiResponse.getBody());
            } else {
                response.put("success", false);
                response.put("message", "Failed to get from IPFS");
            }
            
        } catch (Exception e) {
            logger.error("Error getting from IPFS: ", e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return response;
    }
}
