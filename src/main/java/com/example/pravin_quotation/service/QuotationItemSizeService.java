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
            BigDecimal length,
            BigDecimal width,
            BigDecimal height) {

        validateDimensions(
                length,
                width,
                height
        );

        /*
         * Existing project formula:
         *
         * (Length + Width) × Height / 144
         */

        BigDecimal total =
                length
                        .add(width)
                        .multiply(height)
                        .divide(
                                BigDecimal.valueOf(144),
                                2,
                                RoundingMode.HALF_UP
                        );

        return total;
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
            Integer displayOrder) {

        if (quotationItem == null) {
            throw new IllegalArgumentException(
                    "Quotation item is required."
            );
        }

        validateDimensions(
                length,
                width,
                height
        );

        BigDecimal sqft =
                calculateSqft(
                        length,
                        width,
                        height
                );

        QuotationItemSize size =
                new QuotationItemSize();

        size.setQuotationItem(quotationItem);

        size.setLengthValue(length);

        size.setWidthValue(width);

        size.setHeightValue(height);

        size.setCalculatedSqft(sqft);

        size.setOfferPrice(
                offerPrice != null
                        ? offerPrice
                        : BigDecimal.ZERO
        );

        size.setDisplayOrder(
                displayOrder != null
                        ? displayOrder
                        : 0
        );

        size.setActive(true);

        return quotationItemSizeRepository.save(size);
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

        validateDimensions(
                length,
                width,
                height
        );

        BigDecimal sqft =
                calculateSqft(
                        length,
                        width,
                        height
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

    private void validateDimensions(
            BigDecimal length,
            BigDecimal width,
            BigDecimal height) {

        if (length == null
                || width == null
                || height == null) {

            throw new IllegalArgumentException(
                    "Length, width and height are required."
            );
        }

        if (length.compareTo(BigDecimal.ZERO) <= 0
                || width.compareTo(BigDecimal.ZERO) <= 0
                || height.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Length, width and height must be greater than zero."
            );
        }
    }
}