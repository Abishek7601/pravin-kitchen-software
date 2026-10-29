package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.repository.QuotationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service responsible for automatic retention cleanup of quotations.
 * Automatically deletes quotations that have reached or exceeded 365 days from creation.
 */
@Service
public class QuotationCleanupService {

    private static final Logger log = LoggerFactory.getLogger(QuotationCleanupService.class);

    private final QuotationRepository quotationRepository;
    private final QuotationService quotationService;

    public QuotationCleanupService(QuotationRepository quotationRepository, QuotationService quotationService) {
        this.quotationRepository = quotationRepository;
        this.quotationService = quotationService;
    }

    /**
     * Daily scheduled job to permanently delete quotations that are 365 days or older.
     * By default, runs every day at 02:00 AM.
     */
    @Scheduled(cron = "${quotation.cleanup.cron:0 0 2 * * ?}")
    public int cleanUpExpiredQuotations() {
        return cleanUpQuotationsOlderThan(LocalDate.now(), 365);
    }

    /**
     * Finds and permanently deletes quotations aged >= retentionDays from baseDate.
     * Cascade-deletes all associated sizes, items, rooms, communications, status history, and workflows.
     * Master data (customers, employees, branches, materials, etc.) are never deleted.
     *
     * @param baseDate      The reference date (typically today).
     * @param retentionDays The retention duration threshold in days (e.g. 365).
     * @return Number of quotations successfully deleted.
     */
    @Transactional
    public int cleanUpQuotationsOlderThan(LocalDate baseDate, int retentionDays) {
        if (baseDate == null) {
            baseDate = LocalDate.now();
        }
        LocalDate cutoffDate = baseDate.minusDays(retentionDays);
        LocalDateTime cutoffDateTime = cutoffDate.atTime(LocalTime.MAX);

        log.info("Starting quotation retention cleanup: baseDate={}, retentionDays={}, cutoffDate={}",
                baseDate, retentionDays, cutoffDate);

        List<Quotation> eligibleQuotations = quotationRepository.findQuotationsEligibleForDeletion(cutoffDate, cutoffDateTime);
        int deletedCount = 0;

        for (Quotation quotation : eligibleQuotations) {
            try {
                Long qId = quotation.getId();
                String qNumber = quotation.getQuotationNumber();
                LocalDate qDate = quotation.getQuotationDate();

                log.info("Deleting expired quotation ID: {} ({}) created on {} (exceeded {} days)",
                        qId, qNumber, qDate, retentionDays);
                quotationService.delete(qId);
                deletedCount++;
            } catch (Exception e) {
                log.error("Failed to delete expired quotation ID: {} ({})",
                        quotation.getId(), quotation.getQuotationNumber(), e);
            }
        }

        log.info("Quotation retention cleanup finished: {} expired quotation(s) deleted.", deletedCount);
        return deletedCount;
    }
}

