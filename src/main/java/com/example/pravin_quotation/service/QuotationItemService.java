package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.QuotationItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class QuotationItemService {

    private final QuotationItemRepository quotationItemRepository;
    private final QuotationWorkflowService quotationWorkflowService;

    public QuotationItemService(
            QuotationItemRepository quotationItemRepository,
            QuotationWorkflowService quotationWorkflowService
    ) {
        this.quotationItemRepository = quotationItemRepository;
        this.quotationWorkflowService = quotationWorkflowService;
    }


    // =========================================================
    // GET ALL
    // =========================================================

    public List<QuotationItem> getAllItems() {

        return quotationItemRepository.findAll();
    }


    // =========================================================
    // GET BY ID
    // =========================================================

    public QuotationItem getById(Long id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Quotation item ID is required."
            );
        }

        return quotationItemRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Quotation item not found."
                        )
                );
    }


    // =========================================================
    // GET BY ROOM
    // =========================================================

    public List<QuotationItem> getByQuotationRoomId(
            Long quotationRoomId
    ) {

        if (quotationRoomId == null) {
            throw new IllegalArgumentException(
                    "Quotation room ID is required."
            );
        }

        return quotationItemRepository
                .findByQuotationRoomIdOrderByIdAsc(
                        quotationRoomId
                );
    }


    // =========================================================
    // GET ACTIVE ITEMS BY ROOM
    // =========================================================

    public List<QuotationItem> getActiveByQuotationRoomId(
            Long quotationRoomId
    ) {

        if (quotationRoomId == null) {
            throw new IllegalArgumentException(
                    "Quotation room ID is required."
            );
        }

        return quotationItemRepository
                .findByQuotationRoomIdAndActiveTrueOrderByIdAsc(
                        quotationRoomId
                );
    }


    // =========================================================
    // COUNT ITEMS
    // =========================================================

    public long countByQuotationRoomId(
            Long quotationRoomId
    ) {

        if (quotationRoomId == null) {
            return 0;
        }

        return quotationItemRepository
                .countByQuotationRoomId(
                        quotationRoomId
                );
    }


    // =========================================================
    // COUNT ACTIVE ITEMS
    // =========================================================

    public long countActiveByQuotationRoomId(
            Long quotationRoomId
    ) {

        if (quotationRoomId == null) {
            return 0;
        }

        return quotationItemRepository
                .countByQuotationRoomIdAndActiveTrue(
                        quotationRoomId
                );
    }


    // =========================================================
    // CREATE ITEM
    // =========================================================

    @Transactional
    public QuotationItem create(
            QuotationRoom quotationRoom,
            WorkCategory workCategory,
            Division division,
            Item item,
            Material material,
            MaterialOption materialOption,
            String itemDescription,
            BigDecimal lengthValue,
            BigDecimal widthValue,
            BigDecimal heightValue,
            BigDecimal calculatedSqft,
            BigDecimal rate,
            BigDecimal amount,
            BigDecimal offerPrice,
            String formula,
            String specification
    ) {

        if (quotationRoom == null) {
            throw new IllegalArgumentException(
                    "Quotation room is required."
            );
        }

        validateNonNegative(
                lengthValue,
                "Length"
        );

        validateNonNegative(
                widthValue,
                "Width"
        );

        validateNonNegative(
                heightValue,
                "Height"
        );

        validateNonNegative(
                calculatedSqft,
                "Calculated Sq.Ft"
        );

        validateNonNegative(
                rate,
                "Rate"
        );

        validateNonNegative(
                amount,
                "Amount"
        );

        validateNonNegative(
                offerPrice,
                "Offer price"
        );

        QuotationItem quotationItem =
                new QuotationItem();

        quotationItem.setQuotationRoom(
                quotationRoom
        );

        quotationItem.setWorkCategory(
                workCategory
        );

        quotationItem.setDivision(
                division
        );

        quotationItem.setItem(
                item
        );

        quotationItem.setMaterial(
                material
        );

        quotationItem.setMaterialOption(
                materialOption
        );

        quotationItem.setItemDescription(
                cleanText(itemDescription)
        );

        quotationItem.setLengthValue(
                lengthValue
        );

        quotationItem.setWidthValue(
                widthValue
        );

        quotationItem.setHeightValue(
                heightValue
        );

        quotationItem.setCalculatedSqft(
                defaultZero(calculatedSqft)
        );

        quotationItem.setRate(
                defaultZero(rate)
        );

        quotationItem.setAmount(
                defaultZero(amount)
        );

        quotationItem.setOfferPrice(
                defaultZero(offerPrice)
        );

        quotationItem.setFormula(
                cleanText(formula)
        );

        quotationItem.setSpecification(
                cleanText(specification)
        );

        quotationItem.setActive(true);

        // -----------------------------------------------------
        // Save item
        // -----------------------------------------------------

        QuotationItem savedItem =
                quotationItemRepository.save(
                        quotationItem
                );

        // -----------------------------------------------------
        // WORKFLOW TRACKING
        // -----------------------------------------------------

        quotationWorkflowService.record(
                quotationRoom.getQuotation(),
                QuotationWorkflow.WorkflowAction.CREATED,
                quotationRoom.getQuotation().getEmployee(),
                "Quotation item added: "
                        + getItemName(savedItem),
                null,
                buildItemValue(savedItem)
        );

        return savedItem;
    }


    // =========================================================
    // UPDATE ITEM
    // =========================================================

    @Transactional
    public QuotationItem update(
            Long id,
            WorkCategory workCategory,
            Division division,
            Item item,
            Material material,
            MaterialOption materialOption,
            String itemDescription,
            BigDecimal lengthValue,
            BigDecimal widthValue,
            BigDecimal heightValue,
            BigDecimal calculatedSqft,
            BigDecimal rate,
            BigDecimal amount,
            BigDecimal offerPrice,
            String formula,
            String specification
    ) {

        QuotationItem quotationItem =
                getById(id);

        validateNonNegative(
                lengthValue,
                "Length"
        );

        validateNonNegative(
                widthValue,
                "Width"
        );

        validateNonNegative(
                heightValue,
                "Height"
        );

        validateNonNegative(
                calculatedSqft,
                "Calculated Sq.Ft"
        );

        validateNonNegative(
                rate,
                "Rate"
        );

        validateNonNegative(
                amount,
                "Amount"
        );

        validateNonNegative(
                offerPrice,
                "Offer price"
        );

        // -----------------------------------------------------
        // Store old values
        // -----------------------------------------------------

        String oldValue =
                buildItemValue(quotationItem);

        // -----------------------------------------------------
        // Update item
        // -----------------------------------------------------

        quotationItem.setWorkCategory(
                workCategory
        );

        quotationItem.setDivision(
                division
        );

        quotationItem.setItem(
                item
        );

        quotationItem.setMaterial(
                material
        );

        quotationItem.setMaterialOption(
                materialOption
        );

        quotationItem.setItemDescription(
                cleanText(itemDescription)
        );

        quotationItem.setLengthValue(
                lengthValue
        );

        quotationItem.setWidthValue(
                widthValue
        );

        quotationItem.setHeightValue(
                heightValue
        );

        quotationItem.setCalculatedSqft(
                defaultZero(calculatedSqft)
        );

        quotationItem.setRate(
                defaultZero(rate)
        );

        quotationItem.setAmount(
                defaultZero(amount)
        );

        quotationItem.setOfferPrice(
                defaultZero(offerPrice)
        );

        quotationItem.setFormula(
                cleanText(formula)
        );

        quotationItem.setSpecification(
                cleanText(specification)
        );

        QuotationItem updatedItem =
                quotationItemRepository.save(
                        quotationItem
                );

        // -----------------------------------------------------
        // WORKFLOW TRACKING
        // -----------------------------------------------------

        quotationWorkflowService.record(
                updatedItem
                        .getQuotationRoom()
                        .getQuotation(),
                QuotationWorkflow.WorkflowAction.UPDATED,
                updatedItem
                        .getQuotationRoom()
                        .getQuotation()
                        .getEmployee(),
                "Quotation item updated: "
                        + getItemName(updatedItem),
                oldValue,
                buildItemValue(updatedItem)
        );

        return updatedItem;
    }


    // =========================================================
    // CALCULATE SQ.FT
    //
    // Default formula:
    // (Length + Width) × Height / 144
    //
    // Dimensions are expected in inches.
    // =========================================================

    public BigDecimal calculateSqft(
            BigDecimal lengthValue,
            BigDecimal widthValue,
            BigDecimal heightValue
    ) {

        validatePositive(
                lengthValue,
                "Length"
        );

        validatePositive(
                widthValue,
                "Width"
        );

        validatePositive(
                heightValue,
                "Height"
        );

        BigDecimal result =
                lengthValue
                        .add(widthValue)
                        .multiply(heightValue)
                        .divide(
                                new BigDecimal("144"),
                                2,
                                RoundingMode.HALF_UP
                        );

        return result.max(
                BigDecimal.ZERO
        );
    }


    // =========================================================
    // CALCULATE AMOUNT
    // =========================================================

    public BigDecimal calculateAmount(
            BigDecimal calculatedSqft,
            BigDecimal rate
    ) {

        validateNonNegative(
                calculatedSqft,
                "Calculated Sq.Ft"
        );

        validateNonNegative(
                rate,
                "Rate"
        );

        return calculatedSqft
                .multiply(rate)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }

    /**
     * If offer price is null or zero, use the calculated amount.
     * A positive offer price is retained.
     */
    public BigDecimal calculateOfferPrice(
            BigDecimal amount,
            BigDecimal offerPrice) {

        validateNonNegative(amount, "Amount");

        if (amount == null) {
            throw new IllegalArgumentException(
                    "Amount is required.");
        }

        if (offerPrice == null
                || offerPrice.compareTo(BigDecimal.ZERO) == 0) {

            return amount.setScale(
                    2,
                    RoundingMode.HALF_UP);
        }

        validateNonNegative(offerPrice, "Offer price");

        return offerPrice.setScale(
                2,
                RoundingMode.HALF_UP);
    }


    // =========================================================
    // UPDATE CALCULATED VALUES
    // =========================================================

    @Transactional
    public QuotationItem updateCalculatedValues(
            Long id,
            BigDecimal calculatedSqft,
            BigDecimal rate,
            BigDecimal amount,
            BigDecimal offerPrice
    ) {

        QuotationItem quotationItem =
                getById(id);

        validateNonNegative(
                calculatedSqft,
                "Calculated Sq.Ft"
        );

        validateNonNegative(
                rate,
                "Rate"
        );

        validateNonNegative(
                amount,
                "Amount"
        );

        validateNonNegative(
                offerPrice,
                "Offer price"
        );

        // -----------------------------------------------------
        // Store old calculated values
        // -----------------------------------------------------

        String oldValue =
                buildCalculatedValue(
                        quotationItem
                );

        // -----------------------------------------------------
        // Update calculated values
        // -----------------------------------------------------

        quotationItem.setCalculatedSqft(
                defaultZero(calculatedSqft)
        );

        quotationItem.setRate(
                defaultZero(rate)
        );

        quotationItem.setAmount(
                defaultZero(amount)
        );

        quotationItem.setOfferPrice(
                defaultZero(offerPrice)
        );

        QuotationItem updatedItem =
                quotationItemRepository.save(
                        quotationItem
                );

        // -----------------------------------------------------
        // Workflow tracking
        // -----------------------------------------------------

        String newValue =
                buildCalculatedValue(
                        updatedItem
                );

        quotationWorkflowService.record(
                updatedItem
                        .getQuotationRoom()
                        .getQuotation(),
                QuotationWorkflow.WorkflowAction.UPDATED,
                updatedItem
                        .getQuotationRoom()
                        .getQuotation()
                        .getEmployee(),
                "Quotation item pricing/calculation updated: "
                        + getItemName(updatedItem),
                oldValue,
                newValue
        );

        return updatedItem;
    }


    // =========================================================
    // ACTIVATE
    // =========================================================

    @Transactional
    public QuotationItem activate(
            Long id
    ) {

        QuotationItem quotationItem =
                getById(id);

        quotationItem.setActive(true);

        QuotationItem updatedItem =
                quotationItemRepository.save(
                        quotationItem
                );

        // -----------------------------------------------------
        // Workflow tracking
        // -----------------------------------------------------

        quotationWorkflowService.record(
                updatedItem
                        .getQuotationRoom()
                        .getQuotation(),
                QuotationWorkflow.WorkflowAction.UPDATED,
                updatedItem
                        .getQuotationRoom()
                        .getQuotation()
                        .getEmployee(),
                "Quotation item activated: "
                        + getItemName(updatedItem),
                "ACTIVE = false",
                "ACTIVE = true"
        );

        return updatedItem;
    }


    // =========================================================
    // DEACTIVATE
    // =========================================================

    @Transactional
    public QuotationItem deactivate(
            Long id
    ) {

        QuotationItem quotationItem =
                getById(id);

        quotationItem.setActive(false);

        QuotationItem updatedItem =
                quotationItemRepository.save(
                        quotationItem
                );

        // -----------------------------------------------------
        // Workflow tracking
        // -----------------------------------------------------

        quotationWorkflowService.record(
                updatedItem
                        .getQuotationRoom()
                        .getQuotation(),
                QuotationWorkflow.WorkflowAction.UPDATED,
                updatedItem
                        .getQuotationRoom()
                        .getQuotation()
                        .getEmployee(),
                "Quotation item deactivated: "
                        + getItemName(updatedItem),
                "ACTIVE = true",
                "ACTIVE = false"
        );

        return updatedItem;
    }


// =========================================================
// DELETE ITEM
// =========================================================

    @Transactional
    public void delete(
            Long id
    ) {

        QuotationItem quotationItem =
                getById(id);

        // -----------------------------------------------------
        // Store quotation before changing item
        // -----------------------------------------------------

        Quotation quotation =
                quotationItem
                        .getQuotationRoom()
                        .getQuotation();

        // -----------------------------------------------------
        // Store old item information
        // -----------------------------------------------------

        String itemValue =
                buildItemValue(
                        quotationItem
                );

        String itemName =
                getItemName(
                        quotationItem
                );

        // -----------------------------------------------------
        // SOFT DELETE
        // -----------------------------------------------------

        quotationItem.setActive(false);

        QuotationItem deletedItem =
                quotationItemRepository.save(
                        quotationItem
                );

        // -----------------------------------------------------
        // WORKFLOW TRACKING
        // -----------------------------------------------------

        quotationWorkflowService.record(
                quotation,
                QuotationWorkflow.WorkflowAction.ITEM_DELETED,
                quotation.getEmployee(),
                "Quotation item deleted: "
                        + itemName,
                itemValue,
                null
        );
    }



    // =========================================================
    // ITEM NAME
    // =========================================================

    private String getItemName(
            QuotationItem quotationItem
    ) {

        if (quotationItem == null) {
            return "Unknown item";
        }

        if (quotationItem.getItem() != null
                && quotationItem
                .getItem()
                .getName() != null) {

            return quotationItem
                    .getItem()
                    .getName();
        }

        if (quotationItem.getItemDescription() != null
                && !quotationItem
                .getItemDescription()
                .isBlank()) {

            return quotationItem
                    .getItemDescription();
        }

        return "Unnamed item";
    }


    // =========================================================
    // BUILD ITEM VALUE
    // =========================================================

    private String buildItemValue(
            QuotationItem quotationItem
    ) {

        if (quotationItem == null) {
            return null;
        }

        StringBuilder value =
                new StringBuilder();

        value.append("Item: ")
                .append(
                        getItemName(
                                quotationItem
                        )
                );

        if (quotationItem.getWorkCategory() != null) {

            value.append(", Category: ")
                    .append(
                            quotationItem
                                    .getWorkCategory()
                                    .getName()
                    );
        }

        if (quotationItem.getDivision() != null) {

            value.append(", Division: ")
                    .append(
                            quotationItem
                                    .getDivision()
                                    .getName()
                    );
        }

        if (quotationItem.getMaterial() != null) {

            value.append(", Material: ")
                    .append(
                            quotationItem
                                    .getMaterial()
                                    .getName()
                    );
        }

        if (quotationItem.getMaterialOption() != null) {

            value.append(", Option: ")
                    .append(
                            quotationItem
                                    .getMaterialOption()
                                    .getName()
                    );
        }

        if (quotationItem.getLengthValue() != null
                || quotationItem.getWidthValue() != null
                || quotationItem.getHeightValue() != null) {

            value.append(", Size: ")
                    .append(
                            safeDecimal(
                                    quotationItem
                                            .getLengthValue()
                            )
                    )
                    .append(" × ")
                    .append(
                            safeDecimal(
                                    quotationItem
                                            .getWidthValue()
                            )
                    )
                    .append(" × ")
                    .append(
                            safeDecimal(
                                    quotationItem
                                            .getHeightValue()
                            )
                    );
        }

        value.append(", Sq.Ft: ")
                .append(
                        safeDecimal(
                                quotationItem
                                        .getCalculatedSqft()
                        )
                );

        value.append(", Rate: ₹")
                .append(
                        safeDecimal(
                                quotationItem.getRate()
                        )
                );

        value.append(", Amount: ₹")
                .append(
                        safeDecimal(
                                quotationItem.getAmount()
                        )
                );

        value.append(", Offer Price: ₹")
                .append(
                        safeDecimal(
                                quotationItem
                                        .getOfferPrice()
                        )
                );

        if (quotationItem.getSpecification() != null
                && !quotationItem
                .getSpecification()
                .isBlank()) {

            value.append(", Specification: ")
                    .append(
                            quotationItem
                                    .getSpecification()
                    );
        }

        if (quotationItem.getFormula() != null
                && !quotationItem
                .getFormula()
                .isBlank()) {

            value.append(", Formula: ")
                    .append(
                            quotationItem.getFormula()
                    );
        }

        return value.toString();
    }


    // =========================================================
    // BUILD CALCULATED VALUE
    // =========================================================

    private String buildCalculatedValue(
            QuotationItem quotationItem
    ) {

        if (quotationItem == null) {
            return null;
        }

        return "Sq.Ft: "
                + safeDecimal(
                quotationItem
                        .getCalculatedSqft()
        )
                + ", Rate: ₹"
                + safeDecimal(
                quotationItem.getRate()
        )
                + ", Amount: ₹"
                + safeDecimal(
                quotationItem.getAmount()
        )
                + ", Offer Price: ₹"
                + safeDecimal(
                quotationItem.getOfferPrice()
        );
    }


    // =========================================================
    // SAFE DECIMAL
    // =========================================================

    private BigDecimal safeDecimal(
            BigDecimal value
    ) {

        return value != null
                ? value
                : BigDecimal.ZERO;
    }


    // =========================================================
    // VALIDATION HELPERS
    // =========================================================

    private void validateNonNegative(
            BigDecimal value,
            String fieldName
    ) {

        if (value != null
                && value.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            throw new IllegalArgumentException(
                    fieldName
                            + " cannot be negative."
            );
        }
    }


    private void validatePositive(
            BigDecimal value,
            String fieldName
    ) {

        if (value == null
                || value.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalArgumentException(
                    fieldName
                            + " must be greater than zero."
            );
        }
    }


    private BigDecimal defaultZero(
            BigDecimal value
    ) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }


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