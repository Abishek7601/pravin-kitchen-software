package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationRoom;
import com.example.pravin_quotation.model.QuotationWorkflow;
import com.example.pravin_quotation.repository.QuotationRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class QuotationRoomService {

    private final QuotationRoomRepository quotationRoomRepository;
    private final QuotationWorkflowService quotationWorkflowService;

    public QuotationRoomService(
            QuotationRoomRepository quotationRoomRepository,
            QuotationWorkflowService quotationWorkflowService
    ) {
        this.quotationRoomRepository = quotationRoomRepository;
        this.quotationWorkflowService = quotationWorkflowService;
    }


    // =========================================================
    // GET ALL ROOMS
    // =========================================================

    public List<QuotationRoom> getAllRooms() {

        return quotationRoomRepository.findAll();
    }


    // =========================================================
    // GET ROOM BY ID
    // =========================================================

    public QuotationRoom getById(Long id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Quotation room ID is required."
            );
        }

        return quotationRoomRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Quotation room not found."
                        )
                );
    }


    // =========================================================
    // GET ROOMS BY QUOTATION
    // =========================================================

    public List<QuotationRoom> getByQuotationId(
            Long quotationId
    ) {

        if (quotationId == null) {
            throw new IllegalArgumentException(
                    "Quotation ID is required."
            );
        }

        return quotationRoomRepository
                .findByQuotationIdOrderByIdAsc(
                        quotationId
                );
    }


    // =========================================================
    // GET ACTIVE ROOMS BY QUOTATION
    // =========================================================

    public List<QuotationRoom> getActiveByQuotationId(
            Long quotationId
    ) {

        if (quotationId == null) {
            throw new IllegalArgumentException(
                    "Quotation ID is required."
            );
        }

        return quotationRoomRepository
                .findByQuotationIdAndActiveTrueOrderByIdAsc(
                        quotationId
                );
    }


    // =========================================================
    // COUNT ROOMS
    // =========================================================

    public long countByQuotationId(
            Long quotationId
    ) {

        if (quotationId == null) {
            return 0;
        }

        return quotationRoomRepository
                .countByQuotationId(
                        quotationId
                );
    }


    // =========================================================
    // COUNT ACTIVE ROOMS
    // =========================================================

    public long countActiveByQuotationId(
            Long quotationId
    ) {

        if (quotationId == null) {
            return 0;
        }

        return quotationRoomRepository
                .countByQuotationIdAndActiveTrue(
                        quotationId
                );
    }


    // =========================================================
    // CREATE ROOM
    // =========================================================

    @Transactional
    public QuotationRoom create(
            Quotation quotation,
            String floor,
            String room,
            String workDescription
    ) {

        if (quotation == null) {
            throw new IllegalArgumentException(
                    "Quotation is required."
            );
        }

        if (floor == null
                || floor.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Floor is required."
            );
        }

        if (room == null
                || room.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Room is required."
            );
        }

        QuotationRoom quotationRoom =
                new QuotationRoom();

        quotationRoom.setQuotation(
                quotation
        );

        quotationRoom.setFloor(
                floor.trim()
        );

        quotationRoom.setRoom(
                room.trim()
        );

        quotationRoom.setWorkDescription(
                cleanText(workDescription)
        );

        quotationRoom.setSubtotal(
                BigDecimal.ZERO
        );

        quotationRoom.setOfferPrice(
                BigDecimal.ZERO
        );

        quotationRoom.setActive(true);

        // -----------------------------------------------------
        // Save room
        // -----------------------------------------------------

        QuotationRoom savedRoom =
                quotationRoomRepository.save(
                        quotationRoom
                );

        // -----------------------------------------------------
        // WORKFLOW TRACKING
        // -----------------------------------------------------

        quotationWorkflowService.record(
                quotation,
                QuotationWorkflow.WorkflowAction.CREATED,
                quotation.getEmployee(),
                "Quotation room added: "
                        + savedRoom.getRoom(),
                null,
                buildRoomValue(savedRoom)
        );

        return savedRoom;
    }


    // =========================================================
    // UPDATE ROOM
    // =========================================================

    @Transactional
    public QuotationRoom update(
            Long id,
            String floor,
            String room,
            String workDescription
    ) {

        QuotationRoom quotationRoom =
                getById(id);

        if (floor == null
                || floor.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Floor is required."
            );
        }

        if (room == null
                || room.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Room is required."
            );
        }

        // -----------------------------------------------------
        // Store old values
        // -----------------------------------------------------

        String oldValue =
                buildRoomValue(
                        quotationRoom
                );

        // -----------------------------------------------------
        // Update room
        // -----------------------------------------------------

        quotationRoom.setFloor(
                floor.trim()
        );

        quotationRoom.setRoom(
                room.trim()
        );

        quotationRoom.setWorkDescription(
                cleanText(workDescription)
        );

        QuotationRoom updatedRoom =
                quotationRoomRepository.save(
                        quotationRoom
                );

        // -----------------------------------------------------
        // WORKFLOW TRACKING
        // -----------------------------------------------------

        quotationWorkflowService.record(
                updatedRoom.getQuotation(),
                QuotationWorkflow.WorkflowAction.UPDATED,
                updatedRoom.getQuotation().getEmployee(),
                "Quotation room updated: "
                        + updatedRoom.getRoom(),
                oldValue,
                buildRoomValue(updatedRoom)
        );

        return updatedRoom;
    }


    // =========================================================
    // UPDATE ROOM TOTAL
    // =========================================================

    @Transactional
    public QuotationRoom updateTotals(
            Long id,
            BigDecimal subtotal,
            BigDecimal offerPrice
    ) {

        QuotationRoom quotationRoom =
                getById(id);

        if (subtotal == null
                || subtotal.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            throw new IllegalArgumentException(
                    "Room subtotal cannot be negative."
            );
        }

        if (offerPrice == null
                || offerPrice.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            throw new IllegalArgumentException(
                    "Room offer price cannot be negative."
            );
        }

        // -----------------------------------------------------
        // Store old pricing
        // -----------------------------------------------------

        String oldValue =
                "Subtotal: ₹"
                        + safeAmount(
                        quotationRoom.getSubtotal()
                )
                        + ", Offer Price: ₹"
                        + safeAmount(
                        quotationRoom.getOfferPrice()
                );

        // -----------------------------------------------------
        // Update pricing
        // -----------------------------------------------------

        quotationRoom.setSubtotal(
                subtotal
        );

        quotationRoom.setOfferPrice(
                offerPrice
        );

        QuotationRoom updatedRoom =
                quotationRoomRepository.save(
                        quotationRoom
                );

        // -----------------------------------------------------
        // Workflow tracking
        // -----------------------------------------------------

        String newValue =
                "Subtotal: ₹"
                        + safeAmount(subtotal)
                        + ", Offer Price: ₹"
                        + safeAmount(offerPrice);

        quotationWorkflowService.record(
                updatedRoom.getQuotation(),
                QuotationWorkflow.WorkflowAction.UPDATED,
                updatedRoom.getQuotation().getEmployee(),
                "Quotation room pricing updated: "
                        + updatedRoom.getRoom(),
                oldValue,
                newValue
        );

        return updatedRoom;
    }


    // =========================================================
    // ACTIVATE
    // =========================================================

    @Transactional
    public QuotationRoom activate(
            Long id
    ) {

        QuotationRoom quotationRoom =
                getById(id);

        quotationRoom.setActive(true);

        QuotationRoom updatedRoom =
                quotationRoomRepository.save(
                        quotationRoom
                );

        // -----------------------------------------------------
        // WORKFLOW TRACKING
        // -----------------------------------------------------

        quotationWorkflowService.record(
                updatedRoom.getQuotation(),
                QuotationWorkflow.WorkflowAction.UPDATED,
                updatedRoom.getQuotation().getEmployee(),
                "Quotation room activated: "
                        + updatedRoom.getRoom(),
                "ACTIVE = false",
                "ACTIVE = true"
        );

        return updatedRoom;
    }


    // =========================================================
    // DEACTIVATE
    // =========================================================

    @Transactional
    public QuotationRoom deactivate(
            Long id
    ) {

        QuotationRoom quotationRoom =
                getById(id);

        quotationRoom.setActive(false);

        QuotationRoom updatedRoom =
                quotationRoomRepository.save(
                        quotationRoom
                );

        // -----------------------------------------------------
        // WORKFLOW TRACKING
        // -----------------------------------------------------

        quotationWorkflowService.record(
                updatedRoom.getQuotation(),
                QuotationWorkflow.WorkflowAction.UPDATED,
                updatedRoom.getQuotation().getEmployee(),
                "Quotation room deactivated: "
                        + updatedRoom.getRoom(),
                "ACTIVE = true",
                "ACTIVE = false"
        );

        return updatedRoom;
    }


    // =========================================================
    // DELETE
    // =========================================================
    @Transactional
    public void delete(Long id) {

        QuotationRoom quotationRoom = getById(id);

        String roomName =
                quotationRoom.getRoom();

        String oldValue =
                "Floor: "
                        + quotationRoom.getFloor()
                        + ", Room: "
                        + roomName;

        quotationRoom.setActive(false);

        quotationRoomRepository.save(quotationRoom);

        quotationWorkflowService.record(
                quotationRoom.getQuotation(),
                QuotationWorkflow.WorkflowAction.ROOM_DELETED,
                quotationRoom.getQuotation().getEmployee(),
                "Quotation room deleted.",
                oldValue,
                null
        );
    }


    // =========================================================
    // BUILD ROOM VALUE
    // =========================================================

    private String buildRoomValue(
            QuotationRoom quotationRoom
    ) {

        if (quotationRoom == null) {
            return null;
        }

        StringBuilder value =
                new StringBuilder();

        value.append("Floor: ")
                .append(
                        quotationRoom.getFloor() != null
                                ? quotationRoom.getFloor()
                                : "-"
                );

        value.append(", Room: ")
                .append(
                        quotationRoom.getRoom() != null
                                ? quotationRoom.getRoom()
                                : "-"
                );

        if (quotationRoom.getWorkDescription() != null
                && !quotationRoom
                .getWorkDescription()
                .isBlank()) {

            value.append(", Work: ")
                    .append(
                            quotationRoom
                                    .getWorkDescription()
                    );
        }

        return value.toString();
    }


    // =========================================================
    // SAFE AMOUNT
    // =========================================================

    private BigDecimal safeAmount(
            BigDecimal amount
    ) {

        return amount != null
                ? amount
                : BigDecimal.ZERO;
    }


    // =========================================================
    // TEXT CLEANING
    // =========================================================

    private String cleanText(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String cleaned =
                value.trim();

        return cleaned.isEmpty()
                ? null
                : cleaned;
    }
}