package com.example.repository;

import com.example.entity.BlockchainTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockchainTransactionRepository extends JpaRepository<BlockchainTransaction, Long> {
    
    List<BlockchainTransaction> findByTenderIdOrderByCreatedAtDesc(Long tenderId);
    
    List<BlockchainTransaction> findByBidIdOrderByCreatedAtDesc(Long bidId);
    
    List<BlockchainTransaction> findAllByOrderByCreatedAtDesc();
    
    BlockchainTransaction findTopByTenderIdOrderByCreatedAtDesc(Long tenderId);
}
