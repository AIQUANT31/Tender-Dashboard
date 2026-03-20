package com.example.services.report;

import com.example.entity.BlockchainTransaction;
import com.example.repository.BlockchainTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlockchainTransactionService {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockchainTransactionService.class);
    
    @Autowired
    private BlockchainTransactionRepository blockchainTransactionRepository;
    
   
    public BlockchainTransaction saveTransaction(
            Long tenderId,
            Long bidId,
            String summaryHash,
            String ipfsHash,
            String ipfsUrl,
            String cardanoTransactionId,
            String blockchainUrl,
            Integer totalRecords,
            String createdBy) {
        
        try {
            logger.info("Attempting to save blockchain transaction - tenderId: {}, bidId: {}, txId: {}", 
                tenderId, bidId, cardanoTransactionId);
            
            BlockchainTransaction transaction = new BlockchainTransaction();
            transaction.setTenderId(tenderId);
            transaction.setBidId(bidId);
            transaction.setSummaryHash(summaryHash);
            transaction.setIpfsHash(ipfsHash);
            transaction.setIpfsUrl(ipfsUrl);
            transaction.setCardanoTransactionId(cardanoTransactionId);
            transaction.setBlockchainUrl(blockchainUrl);
            transaction.setTotalRecords(totalRecords);
            transaction.setCreatedBy(createdBy);
            transaction.setStatus("COMPLETED");
            transaction.setBlockchainNetwork("Cardano Preprod Testnet");
            
            BlockchainTransaction saved = blockchainTransactionRepository.save(transaction);
            
            logger.info("Blockchain transaction saved successfully to database with ID: {} for txId: {}", 
                saved.getId(), cardanoTransactionId);
            
            return saved;
            
        } catch (Exception e) {
            logger.error("Error saving blockchain transaction to database: ", e);
            logger.error("Exception details: tenderId={}, bidId={}, txId={}, ipfsHash={}", 
                tenderId, bidId, cardanoTransactionId, ipfsHash);
            return null;
        }
    }
    
   
    public List<BlockchainTransaction> getAllTransactions() {
        return blockchainTransactionRepository.findAllByOrderByCreatedAtDesc();
    }
    
  
    public List<BlockchainTransaction> getTransactionsByTender(Long tenderId) {
        return blockchainTransactionRepository.findByTenderIdOrderByCreatedAtDesc(tenderId);
    }
    
   
    public List<BlockchainTransaction> getTransactionsByBid(Long bidId) {
        return blockchainTransactionRepository.findByBidIdOrderByCreatedAtDesc(bidId);
    }
    
    public BlockchainTransaction getLatestTransactionForTender(Long tenderId) {
        return blockchainTransactionRepository.findTopByTenderIdOrderByCreatedAtDesc(tenderId);
    }
}
