package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.QuotationItem;
import com.example.pravin_quotation.model.QuotationItemSize;
import com.example.pravin_quotation.repository.QuotationItemSizeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class QuotationItemSizeService {

    private final QuotationItemSizeRepository quotationItemSizeRepository;


    public QuotationItemSizeService(
            QuotationItemSizeRepository quotationItemSizeRepository) {

        this.quotationItemSizeRepository =
                quotationItemSizeRepository;
    }


    // =========================================================
    // GET ACTIVE SIZES
    // =========================================================

    public List<QuotationItemSize> getActiveSizes(
            Long quotationItemId) {

        return quotationItemSizeRepository
                .findByQuotationItemIdAndActiveTrueOrderByDisplayOrderAsc(
                        quotationItemId
                );
    }


    // =========================================================
    // GET ALL SIZES
    // =========================================================

    public List<QuotationItemSize> getAllSizes(
            Long quotationItemId) {

        return quotationItemSizeRepository
                .findByQuotationItemIdOrderByDisplayOrderAsc(
                        quotationItemId
                );
    }


    // =========================================================
    // GET SIZE BY ID
    // =========================================================

    public QuotationItemSize getById(Long id) {

        return quotationItemSizeRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Quotation item size not found."
                        )
                );
    }


    // =========================================================
    // CALCULATE SQ.FT
    // =========================================================

    public BigDecimal calculateSqft(
            String formula,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height,
            String unit) {

        return com.example.pravin_quotation.util.CalculationFormula.calculateArea(
                formula,
                length,
                width,
                height,
                unit
        );
    }

    public BigDecimal calculateSqft(
            String formula,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height) {

        return calculateSqft(formula, length, width, height, "ft");
    }

    public BigDecimal calculateSqft(
            BigDecimal length,
            BigDecimal width,
            BigDecimal height) {

        return calculateSqft("(Length + Width) × Height / 144", length, width, height, "in");
    }


    // =========================================================
    // CALCULATE AMOUNT
    // =========================================================

    public BigDecimal calculateAmount(
            BigDecimal sqft,
            BigDecimal rate) {

        if (sqft == null) {
            sqft = BigDecimal.ZERO;
        }

        if (rate == null) {
            rate = BigDecimal.ZERO;
        }

        if (sqft.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "SQ.FT cannot be negative."
            );
        }

        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Rate cannot be negative."
            );
        }

        return sqft
                .multiply(rate)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }


    // =========================================================
    // CREATE SIZE
    // =========================================================

    public QuotationItemSize create(
            QuotationItem quotationItem,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height,
            BigDecimal offerPrice,
            Integer displayOrder,
            String unit) {

        if (quotationItem == null) {
            throw new IllegalArgumentException(
                    "Quotation item is required."
            );
        }

        String formula = quotationItem.getFormula();
        validateDimensions(
                formula,
                length,
                width,
                height
        );

        BigDecimal sqft =
                calculateSqft(
                        formula,
                        length,
                        width != null ? width : BigDecimal.ZERO,
                        height != null ? height : BigDecimal.ZERO,
                        unit != null ? unit : "ft"
                );

        QuotationItemSize size =
                new QuotationItemSize();

        size.setQuotationItem(quotationItem);

        size.setLengthValue(length != null ? length : BigDecimal.ZERO);

        size.setWidthValue(width != null ? width : BigDecimal.ZERO);

        size.setHeightValue(height != null ? height : BigDecimal.ZERO);

        size.setCalculatedSqft(sqft);

        size.setOfferPrice(
                offerPrice != null
                        ? offerPrice
                        : BigDecimal.ZERO
        );

        size.setDisplayOrder(
                displayOrder != null
                        ? displayOrder
                        : 1
        );
        size.setActive(true);

        return quotationItemSizeRepository.save(size);
    }

    public QuotationItemSize create(
            QuotationItem quotationItem,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height,
            BigDecimal offerPrice,
            Integer displayOrder) {

        return create(
                quotationItem,
                length,
                width,
                height,
                offerPrice,
                displayOrder,
                "in"
        );
    }


    // =========================================================
    // UPDATE SIZE
    // =========================================================

    public QuotationItemSize update(
            Long id,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height,
            BigDecimal offerPrice,
            Integer displayOrder) {

        QuotationItemSize size =
                getById(id);

        String formula = size.getQuotationItem() != null
                ? size.getQuotationItem().getFormula()
                : null;

        validateDimensions(
                formula,
                length,
                width,
                height
        );

        BigDecimal sqft =
                calculateSqft(
                        formula,
                        length,
                        width,
                        height,
                        "ft"
                );

        size.setLengthValue(length);

        size.setWidthValue(width);

        size.setHeightValue(height);

        size.setCalculatedSqft(sqft);

        size.setOfferPrice(
                offerPrice != null
                        ? offerPrice
                        : BigDecimal.ZERO
        );

        if (displayOrder != null) {
            size.setDisplayOrder(displayOrder);
        }

        return quotationItemSizeRepository.save(size);
    }


    // =========================================================
    // DEACTIVATE SIZE
    // =========================================================

    public QuotationItemSize deactivate(Long id) {

        QuotationItemSize size =
                getById(id);

        size.setActive(false);

        return quotationItemSizeRepository.save(size);
    }


    // =========================================================
    // ACTIVATE SIZE
    // =========================================================

    public QuotationItemSize activate(Long id) {

        QuotationItemSize size =
                getById(id);

        size.setActive(true);

        return quotationItemSizeRepository.save(size);
    }


    // =========================================================
    // DELETE SIZE
    // =========================================================

    public void delete(Long id) {

        QuotationItemSize size =
                getById(id);

        quotationItemSizeRepository.delete(size);
    }


    // =========================================================
    // TOTAL SQ.FT
    // =========================================================

    public BigDecimal calculateTotalSqft(
            Long quotationItemId) {

        List<QuotationItemSize> sizes =
                getActiveSizes(quotationItemId);

        return sizes.stream()
                .map(QuotationItemSize::getCalculatedSqft)
                .filter(value -> value != null)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                )
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }


    // =========================================================
    // TOTAL AMOUNT
    // =========================================================

    public BigDecimal calculateTotalAmount(
            Long quotationItemId,
            BigDecimal rate) {

        BigDecimal totalSqft =
                calculateTotalSqft(
                        quotationItemId
                );

        return calculateAmount(
                totalSqft,
                rate
        );
    }


    // =========================================================
    // VALIDATE DIMENSIONS
    // =========================================================

    public void validateDimensions(
            String formula,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height) {

        com.example.pravin_quotation.util.CalculationFormula.validateDimensions(
                formula,
                length,
                width,
                height
        );
    }

    public void validateDimensions(
            BigDecimal length,
            BigDecimal width,
            BigDecimal height) {

        validateDimensions(
                null,
                length,
                width,
                height
        );
    }
}