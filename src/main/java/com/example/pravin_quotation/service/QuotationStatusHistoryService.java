package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationStatus;
import com.example.pravin_quotation.model.QuotationStatusHistory;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.QuotationStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuotationStatusHistoryService {

    private final QuotationStatusHistoryRepository historyRepository;

    public QuotationStatusHistoryService(
            QuotationStatusHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public List<QuotationStatusHistory> getByQuotationId(Long quotationId) {

        if (quotationId == null) {
            throw new IllegalArgumentException(
                    "Quotation ID is required.");
        }

        return historyRepository
                .findByQuotationIdOrderByChangedAtDesc(quotationId);
    }

    public long countByQuotationId(Long quotationId) {

        if (quotationId == null) {
            throw new IllegalArgumentException(
                    "Quotation ID is required.");
        }

        return historyRepository.countByQuotationId(quotationId);
    }

    @Transactional
    public QuotationStatusHistory create(
            Quotation quotation,
            QuotationStatus oldStatus,
            QuotationStatus newStatus,
            User changedBy,
            String remarks) {

        if (quotation == null) {
            throw new IllegalArgumentException(
                    "Quotation is required.");
        }

        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "New quotation status is required.");
        }

        QuotationStatusHistory history =
                new QuotationStatusHistory();

        history.setQuotation(quotation);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setRemarks(cleanText(remarks));

        return historyRepository.save(history);
    }

    private String cleanText(String value) {

        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        return cleaned.isEmpty() ? null : cleaned;
    }
}