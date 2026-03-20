package com.example.services.report;

import com.example.dto.ScrutinyResultSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.util.*;


@Service
public class CardanoService {
    
    private static final Logger logger = LoggerFactory.getLogger(CardanoService.class);
    
    // Node.js Lucid service URL   From cardano-lucid-service
    @Value("${lucid.service.url:http://localhost:3001}")
    private String lucidServiceUrl;
    
    // Network (preprod, preview, mainnet)   default : preprod
    @Value("${cardano.network:preprod}")
    private String network;
    
    
    @Value("${cardano.wallet.address:addr_test1vzr47p0lheukvq48lkcfyp4zmpazms9vm2xgn3w7kqn8cyg0texev}")
    private String walletAddress;
    
    private final RestTemplate restTemplate;
    
    public CardanoService() {
        this.restTemplate = new RestTemplate();
        logger.info("CardanoService initialized with Lucid via Node.js service");
    }
    
    
    public String getWalletAddress() {
        return walletAddress;
    }
    
    
    public BigInteger getBalance() {
        try {
            String url = lucidServiceUrl + "/wallet/balance";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Boolean success = (Boolean) response.getBody().get("success");
                if (success != null && success) {
                    String balance = (String) response.getBody().get("balance");
                    return new BigInteger(balance);
                }
            }
        } catch (Exception e) {
            logger.error("Error getting balance: ", e);
        }
        
        return BigInteger.ZERO;
    }
    
   
    public String getBalanceInAda() {
        BigInteger balance = getBalance();
        // Convert from lovelace to ADA (1 ADA = 1,000,000 lovelace)
        return balance.divide(BigInteger.valueOf(1000000)).toString() + " ADA";
    }
    
    
    public String generateHash(List<ScrutinyResultSummary> summaries) {
        try {
            StringBuilder sb = new StringBuilder();
            for (ScrutinyResultSummary summary : summaries) {
                sb.append(summary.getBidId());
                sb.append(summary.getTenderId());
                sb.append(summary.getBidderName());
                sb.append(summary.getValidationStatus());
                sb.append(summary.getTimestamp() != null ? summary.getTimestamp().toString() : "");
            }
            
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            logger.error("Error generating hash: ", e);
            throw new RuntimeException("Failed to generate hash", e);
        }
    }
    
   
    public Map<String, Object> writeHashToBlockchain(String ipfsHash, String summaryHash, List<ScrutinyResultSummary> summaries) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (ipfsHash == null || ipfsHash.isEmpty()) {
                response.put("success", false);
                response.put("message", "IPFS hash is required");
                return response;
            }
            
            logger.info("Writing to Cardano blockchain - IPFS Hash: {}, Summary Hash: {}", ipfsHash, summaryHash);
            
            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("ipfsHash", ipfsHash);
            requestBody.put("summaryHash", summaryHash);
            
            // Convert summaries to map list
            List<Map<String, Object>> summariesList = new ArrayList<>();
            for (ScrutinyResultSummary summary : summaries) {
                Map<String, Object> summaryMap = new HashMap<>();
                summaryMap.put("bidId", summary.getBidId());
                summaryMap.put("tenderId", summary.getTenderId());
                summaryMap.put("bidderName", summary.getBidderName());
                summaryMap.put("validationStatus", summary.getValidationStatus());
                summariesList.add(summaryMap);
            }
            requestBody.put("summaries", summariesList);
            
            // Call Node.js Lucid service
            String url = lucidServiceUrl + "/transaction/submit";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            logger.info("Calling Lucid service at: {}", url);
            ResponseEntity<Map> apiResponse = restTemplate.postForEntity(url, request, Map.class);
            
            if (apiResponse.getStatusCode() == HttpStatus.OK && apiResponse.getBody() != null) {
                Map<String, Object> body = apiResponse.getBody();
                response.put("success", body.get("success"));
                response.put("transactionId", body.get("transactionId"));
                response.put("metadataUrl", body.get("metadataUrl"));
                response.put("message", body.get("message"));
                response.put("metadata", body.get("metadata"));
                
                logger.info("Transaction submitted successfully - TX Hash: {}", body.get("transactionId"));
            } else {
                response.put("success", false);
                response.put("message", "Failed to submit transaction to blockchain");
            }
            
        } catch (Exception e) {
            logger.error("Error writing to Cardano blockchain: ", e);
            response.put("success", false);
            response.put("message", "Error writing to blockchain: " + e.getMessage());
        }
        
        return response;
    }
  
    public Map<String, Object> verifyTransaction(String txHash) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String url = lucidServiceUrl + "/transaction/verify/" + txHash;
            ResponseEntity<Map> apiResponse = restTemplate.getForEntity(url, Map.class);
            
            if (apiResponse.getStatusCode() == HttpStatus.OK && apiResponse.getBody() != null) {
                Map<String, Object> body = apiResponse.getBody();
                response.put("success", body.get("success"));
                response.put("found", body.get("found"));
                response.put("message", body.get("message"));
            } else {
                response.put("success", false);
                response.put("message", "Failed to verify transaction");
            }
            
        } catch (Exception e) {
            logger.error("Error verifying transaction: ", e);
            response.put("success", false);
            response.put("message", "Error verifying transaction: " + e.getMessage());
        }
        
        return response;
    }
    
   
    public Map<String, Object> getTransaction(String txHash) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String url = lucidServiceUrl + "/transaction/" + txHash;
            ResponseEntity<Map> apiResponse = restTemplate.getForEntity(url, Map.class);
            
            if (apiResponse.getStatusCode() == HttpStatus.OK && apiResponse.getBody() != null) {
                return apiResponse.getBody();
            }
            
        } catch (Exception e) {
            logger.error("Error getting transaction: ", e);
            response.put("success", false);
            response.put("message", "Error getting transaction: " + e.getMessage());
        }
        
        return response;
    }
    
   
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
