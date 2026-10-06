package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationWorkflow;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.QuotationWorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuotationWorkflowService {

    private final QuotationWorkflowRepository repository;

    public QuotationWorkflowService(
            QuotationWorkflowRepository repository) {
        this.repository = repository;
    }

    // ---------------------------------------------------------
    // Record workflow action
    // ---------------------------------------------------------

    @Transactional
    public QuotationWorkflow record(
            Quotation quotation,
            QuotationWorkflow.WorkflowAction action,
            User performedBy,
            String description,
            String oldValue,
            String newValue
    ) {

        if (quotation == null) {
            throw new IllegalArgumentException(
                    "Quotation is required."
            );
        }

        if (action == null) {
            throw new IllegalArgumentException(
                    "Workflow action is required."
            );
        }

        QuotationWorkflow workflow =
                new QuotationWorkflow();

        workflow.setQuotation(quotation);
        workflow.setAction(action);
        workflow.setPerformedBy(performedBy);
        workflow.setDescription(description);
        workflow.setOldValue(oldValue);
        workflow.setNewValue(newValue);
        workflow.setCreatedAt(LocalDateTime.now());

        return repository.save(workflow);
    }

    // ---------------------------------------------------------
    // Get quotation workflow history
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public List<QuotationWorkflow> getByQuotationId(
            Long quotationId) {

        return repository
                .findByQuotationIdOrderByCreatedAtDesc(
                        quotationId
                );
    }
}