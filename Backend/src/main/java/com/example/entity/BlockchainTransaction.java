package com.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "blockchain_transactions")
public class BlockchainTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tender_id")
    private Long tenderId;
    
    @Column(name = "bid_id")
    private Long bidId;
    
    @Column(name = "summary_hash", length = 1000)
    private String summaryHash;
    
    @Column(name = "ipfs_hash", length = 500)
    private String ipfsHash;
    
    @Column(name = "ipfs_url", length = 1000)
    private String ipfsUrl;
    
    @Column(name = "cardano_transaction_id", length = 200)
    private String cardanoTransactionId;
    
    @Column(name = "blockchain_url", length = 1000)
    private String blockchainUrl;
    
    @Column(name = "blockchain_network")
    private String blockchainNetwork;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "total_records")
    private Integer totalRecords;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    public BlockchainTransaction() {
        this.createdAt = LocalDateTime.now();
        this.status = "COMPLETED";
        this.blockchainNetwork = "Cardano Preprod Testnet";
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getTenderId() {
        return tenderId;
    }
    
    public void setTenderId(Long tenderId) {
        this.tenderId = tenderId;
    }
    
    public Long getBidId() {
        return bidId;
    }
    
    public void setBidId(Long bidId) {
        this.bidId = bidId;
    }
    
    public String getSummaryHash() {
        return summaryHash;
    }
    
    public void setSummaryHash(String summaryHash) {
        this.summaryHash = summaryHash;
    }
    
    public String getIpfsHash() {
        return ipfsHash;
    }
    
    public void setIpfsHash(String ipfsHash) {
        this.ipfsHash = ipfsHash;
    }
    
    public String getIpfsUrl() {
        return ipfsUrl;
    }
    
    public void setIpfsUrl(String ipfsUrl) {
        this.ipfsUrl = ipfsUrl;
    }
    
    public String getCardanoTransactionId() {
        return cardanoTransactionId;
    }
    
    public void setCardanoTransactionId(String cardanoTransactionId) {
        this.cardanoTransactionId = cardanoTransactionId;
    }
    
    public String getBlockchainUrl() {
        return blockchainUrl;
    }
    
    public void setBlockchainUrl(String blockchainUrl) {
        this.blockchainUrl = blockchainUrl;
    }
    
    public String getBlockchainNetwork() {
        return blockchainNetwork;
    }
    
    public void setBlockchainNetwork(String blockchainNetwork) {
        this.blockchainNetwork = blockchainNetwork;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Integer getTotalRecords() {
        return totalRecords;
    }
    
    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
