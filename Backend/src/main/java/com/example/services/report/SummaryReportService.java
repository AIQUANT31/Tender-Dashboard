package com.example.services.report;

import com.example.dto.SummaryReport;
import com.example.dto.ValidationResult;
import com.example.entity.ActivityLog;
import com.example.entity.Bid;
import com.example.entity.Bidder;
import com.example.entity.Tender;

import com.example.repository.BidRepository;
import com.example.repository.BidderRepository;
import com.example.repository.TenderRepository;
import com.example.services.activity.ActivityLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SummaryReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(SummaryReportService.class);
    
    @Autowired
    private BidRepository bidRepository;
    
    @Autowired
    private TenderRepository tenderRepository;
    
    @Autowired
    private BidderRepository bidderRepository;
    
    @Autowired
    private ActivityLogService activityLogService;
    
   
    public SummaryReport generateBidSummaryReport(Long bidId) {
        SummaryReport report = new SummaryReport();
        
        try {
            Bid bid = bidRepository.findById(bidId).orElse(null);
            if (bid == null) {
                report.setValid(false);
                report.setValidationMessage("Bid not found with ID: " + bidId);
                return report;
            }
            
            
            report.setBidId(bid.getId());
            
            
            if (bid.getTenderId() != null) {
                Tender tender = tenderRepository.findById(bid.getTenderId()).orElse(null);
                if (tender != null) {
                    report.setTenderId(tender.getId());
                    report.setTenderName(tender.getName());
                }
            }
            
            
            if (bid.getBidderId() != null) {
                Bidder bidder = bidderRepository.findById(bid.getBidderId()).orElse(null);
                if (bidder != null) {
                    report.setBidderName(bidder.getCompanyName());
                }
            }
            
        
            List<ActivityLog> bidActivities = activityLogService.getEntityActivityLogs("BID", bidId);
            if (bidActivities.isEmpty()) {
                // Also check tender activities if no bid activities found
                bidActivities = activityLogService.getEntityActivityLogs("TENDER", bidId);
            }
            report.setManualChangesCount(bidActivities.size());
            
            List<String> recentChanges = new ArrayList<>();
            for (ActivityLog activity : bidActivities) {
                recentChanges.add(activity.getTimestamp() + ": " + activity.getDescription());
                if (recentChanges.size() >= 10) break;
            }
            report.setRecentManualChanges(recentChanges);
            
            report.setValid(true);
            report.setValidationMessage("Summary report generated successfully");
            
        } catch (Exception e) {
            logger.error("Error generating summary report for bid {}: ", bidId, e);
            report.setValid(false);
            report.setValidationMessage("Error generating report: " + e.getMessage());
        }
        
        return report;
    }
    
    
    public SummaryReport generateValidationSummaryReport(Long bidId, ValidationResult validationResult) {
        SummaryReport report = generateBidSummaryReport(bidId);
        
        if (validationResult != null) {
        
            report.setDocumentsFound(new ArrayList<>(validationResult.getMatchedDocuments()));
            
            
            report.setMissingDocuments(new ArrayList<>(validationResult.getMissingDocuments()));
            
            
            report.setDuplicateDocuments(new ArrayList<>(validationResult.getDuplicateDocuments()));
            
    
            report.calculateTotals();
            
            
            report.setValid(validationResult.isValid());
            report.setValidationMessage(validationResult.getMessage() != null ? 
                validationResult.getMessage() : "Validation completed");
            
        
            int totalRequired = report.getTotalDocumentsFound() + report.getTotalDocumentsMissing();
            if (totalRequired > 0) {
                report.setValidationScore((report.getTotalDocumentsFound() * 100) / totalRequired);
            }
        }
        
        return report;
    }
    
    public List<SummaryReport> generateBatchSummaryReport(List<Long> bidIds) {
        List<SummaryReport> reports = new ArrayList<>();
        
        for (Long bidId : bidIds) {
            try {
                SummaryReport report = generateBidSummaryReport(bidId);
                reports.add(report);
            } catch (Exception e) {
                logger.error("Error generating report for bid {}: ", bidId, e);
                SummaryReport errorReport = new SummaryReport();
                errorReport.setBidId(bidId);
                errorReport.setValid(false);
                errorReport.setValidationMessage("Error: " + e.getMessage());
                reports.add(errorReport);
            }
        }
        
        return reports;
    }
    
   
    public SummaryReport generateTenderSummaryReport(Long tenderId) {
        SummaryReport report = new SummaryReport();
        
        try {
            Tender tender = tenderRepository.findById(tenderId).orElse(null);
            if (tender == null) {
                report.setValid(false);
                report.setValidationMessage("Tender not found with ID: " + tenderId);
                return report;
            }
            
            report.setTenderId(tender.getId());
            report.setTenderName(tender.getName());
            
        
            List<Bid> bids = bidRepository.findByTenderId(tenderId);
            
            int totalDocsFound = 0;
            int totalDocsMissing = 0;
            int totalDocsDuplicate = 0;
            
            List<String> allFoundDocs = new ArrayList<>();
            List<String> allMissingDocs = new ArrayList<>();
            List<String> allDuplicateDocs = new ArrayList<>();
            
            for (Bid bid : bids) {
                List<ActivityLog> activities = activityLogService.getEntityActivityLogs("BID", bid.getId());
                if (activities.isEmpty()) {
                    // Also check tender activities if no bid activities found
                    activities = activityLogService.getEntityActivityLogs("TENDER", bid.getId());
                }
                totalDocsFound += activities.size(); 
            }
            
            report.setTotalDocumentsFound(totalDocsFound);
            report.setTotalDocumentsMissing(totalDocsMissing);
            report.setTotalDocumentsDuplicate(totalDocsDuplicate);
            report.setTotalDocumentsProcessed(totalDocsFound + totalDocsMissing);
            
            report.setDocumentsFound(allFoundDocs);
            report.setMissingDocuments(allMissingDocs);
            report.setDuplicateDocuments(allDuplicateDocs);
            
            report.setValid(true);
            report.setValidationMessage("Tender summary generated for " + bids.size() + " bids");
            
        } catch (Exception e) {
            logger.error("Error generating tender summary report: ", e);
            report.setValid(false);
            report.setValidationMessage("Error: " + e.getMessage());
        }
        
        return report;
    }
}
