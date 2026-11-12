package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class QuotationCalculationService {

    private final QuotationRepository quotationRepository;
    private final QuotationRoomRepository quotationRoomRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final PricingRepository pricingRepository;
    private final DistrictPricingRepository districtPricingRepository;
    private final TravelChargeRepository travelChargeRepository;
    private final QuotationItemSizeService quotationItemSizeService;

    @org.springframework.beans.factory.annotation.Autowired
    public QuotationCalculationService(
            QuotationRepository quotationRepository,
            QuotationRoomRepository quotationRoomRepository,
            QuotationItemRepository quotationItemRepository,
            PricingRepository pricingRepository,
            DistrictPricingRepository districtPricingRepository,
            TravelChargeRepository travelChargeRepository,
            QuotationItemSizeService quotationItemSizeService) {

        this.quotationRepository = quotationRepository;
        this.quotationRoomRepository = quotationRoomRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.pricingRepository = pricingRepository;
        this.districtPricingRepository = districtPricingRepository;
        this.travelChargeRepository = travelChargeRepository;
        this.quotationItemSizeService = quotationItemSizeService;
    }

    public QuotationCalculationService(
            QuotationRepository quotationRepository,
            QuotationRoomRepository quotationRoomRepository,
            QuotationItemRepository quotationItemRepository,
            PricingRepository pricingRepository,
            TravelChargeRepository travelChargeRepository,
            QuotationItemSizeService quotationItemSizeService) {

        this(quotationRepository, quotationRoomRepository, quotationItemRepository,
                pricingRepository, null, travelChargeRepository, quotationItemSizeService);
    }


    // =========================================================
    // CALCULATE COMPLETE QUOTATION
    // =========================================================

    @Transactional
    public Quotation calculateQuotation(Long quotationId) {

        if (quotationId == null) {
            throw new IllegalArgumentException(
                    "Quotation ID is required.");
        }

        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Quotation not found."));

        List<QuotationRoom> rooms =
                quotationRoomRepository
                        .findByQuotationIdAndActiveTrueOrderByIdAsc(
                                quotationId);

        BigDecimal quotationSubtotal = BigDecimal.ZERO;

        for (QuotationRoom room : rooms) {

            BigDecimal roomSubtotal =
                    calculateRoomSubtotal(room);

            BigDecimal roomOfferPrice =
                    calculateRoomOfferPrice(room);

            room.setSubtotal(roomSubtotal);
            room.setOfferPrice(roomOfferPrice);

            quotationRoomRepository.save(room);

            quotationSubtotal =
                    quotationSubtotal.add(roomOfferPrice);
        }

        quotation.setSubtotal(
                money(quotationSubtotal));

        BigDecimal accessories =
                zeroIfNull(quotation.getAccessoriesAmount());

        BigDecimal travel =
                zeroIfNull(quotation.getTravelCharge());

        BigDecimal otherCharges =
                zeroIfNull(quotation.getOtherCharges());

        BigDecimal discount =
                zeroIfNull(quotation.getDiscountAmount());

        // Step 2: Amount Before Discount = Subtotal + Accessories + Travel Charges + Other Charges
        BigDecimal amountBeforeDiscount =
                quotationSubtotal
                        .add(accessories)
                        .add(travel)
                        .add(otherCharges);

        // Step 3: Amount After Discount = Amount Before Discount - Discount
        BigDecimal amountAfterDiscount =
                amountBeforeDiscount.subtract(discount);

        if (amountAfterDiscount.compareTo(BigDecimal.ZERO) < 0) {
            amountAfterDiscount = BigDecimal.ZERO;
        }

        // Step 4: Offer Amount = Amount After Discount * Offer Percentage / 100
        BigDecimal offerPercentage =
                zeroIfNull(quotation.getOfferPercentage());

        if (offerPercentage.compareTo(BigDecimal.ZERO) < 0) {
            offerPercentage = BigDecimal.ZERO;
        } else if (offerPercentage.compareTo(new BigDecimal("100")) > 0) {
            offerPercentage = new BigDecimal("100");
        }

        quotation.setOfferPercentage(
                money(offerPercentage));

        BigDecimal offerAmount =
                amountAfterDiscount
                        .multiply(offerPercentage)
                        .divide(
                                new BigDecimal("100"),
                                2,
                                RoundingMode.HALF_UP);

        quotation.setOfferAmount(
                money(offerAmount));

        // Amount After Offer (Taxable Amount for GST) = Amount After Discount - Offer Amount
        BigDecimal amountAfterOffer =
                amountAfterDiscount.subtract(offerAmount);

        if (amountAfterOffer.compareTo(BigDecimal.ZERO) < 0) {
            amountAfterOffer = BigDecimal.ZERO;
        }

        quotation.setTaxableAmount(
                money(amountAfterOffer));

        // Step 5: GST Amount = Amount After Offer * GST Percentage / 100
        BigDecimal gstPercentage =
                zeroIfNull(quotation.getGstPercentage());

        if (gstPercentage.compareTo(BigDecimal.ZERO) < 0) {
            gstPercentage = BigDecimal.ZERO;
        } else if (gstPercentage.compareTo(new BigDecimal("100")) > 0) {
            gstPercentage = new BigDecimal("100");
        }

        quotation.setGstPercentage(
                money(gstPercentage));

        BigDecimal gstAmount =
                amountAfterOffer
                        .multiply(gstPercentage)
                        .divide(
                                new BigDecimal("100"),
                                2,
                                RoundingMode.HALF_UP);

        quotation.setGstAmount(
                money(gstAmount));

        // Step 6: Grand Total = Amount After Offer + GST Amount
        BigDecimal grandTotal =
                amountAfterOffer.add(gstAmount);

        quotation.setTotalAmount(
                money(amountAfterDiscount));

        quotation.setGrandTotal(
                money(grandTotal));

        quotation.setPricingModeDisplay(resolvePricingModeDisplay(quotationId));

        return quotationRepository.save(quotation);
    }


    // =========================================================
    // CALCULATE ROOM SUBTOTAL
    // =========================================================

    public BigDecimal calculateRoomSubtotal(
            QuotationRoom room) {

        if (room == null || room.getId() == null) {
            return BigDecimal.ZERO;
        }

        List<QuotationItem> items =
                quotationItemRepository
                        .findByQuotationRoomIdAndActiveTrueOrderByIdAsc(
                                room.getId());

        BigDecimal subtotal = BigDecimal.ZERO;

        for (QuotationItem item : items) {

            BigDecimal amount =
                    zeroIfNull(item.getAmount());

            subtotal = subtotal.add(amount);
        }

        return money(subtotal);
    }


    // =========================================================
    // CALCULATE ROOM OFFER PRICE
    // =========================================================

    public BigDecimal calculateRoomOfferPrice(
            QuotationRoom room) {

        if (room == null || room.getId() == null) {
            return BigDecimal.ZERO;
        }

        List<QuotationItem> items =
                quotationItemRepository
                        .findByQuotationRoomIdAndActiveTrueOrderByIdAsc(
                                room.getId());

        BigDecimal offerPrice = BigDecimal.ZERO;

        for (QuotationItem item : items) {

            BigDecimal itemOfferPrice =
                    item.getOfferPrice();

            if (quotationItemSizeService != null && item.getId() != null) {
                List<QuotationItemSize> sizes = quotationItemSizeService.getActiveSizes(item.getId());
                if (sizes != null && !sizes.isEmpty()) {
                    BigDecimal sizesOfferTotal = BigDecimal.ZERO;
                    boolean hasCustomSizeOffer = false;
                    for (QuotationItemSize sz : sizes) {
                        BigDecimal szOffer = sz.getOfferPrice();
                        BigDecimal szSqft = zeroIfNull(sz.getCalculatedSqft());
                        BigDecimal szBase;
                        if (item.getMeasurementType() == com.example.pravin_quotation.model.MeasurementType.ROLL) {
                            BigDecimal cov = item.getCoveragePerRoll() != null ? item.getCoveragePerRoll()
                                    : (item.getMaterial() != null ? item.getMaterial().getCoveragePerRoll() : null);
                            BigDecimal rolls = (cov != null && cov.compareTo(BigDecimal.ZERO) > 0 && szSqft.compareTo(BigDecimal.ZERO) > 0)
                                    ? szSqft.divide(cov, 0, java.math.RoundingMode.CEILING)
                                    : (item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO);
                            szBase = money(rolls.multiply(zeroIfNull(item.getRate())));
                        } else {
                            szBase = money(szSqft.multiply(zeroIfNull(item.getRate())));
                        }
                        if (szOffer != null && szOffer.compareTo(BigDecimal.ZERO) > 0) {
                            sizesOfferTotal = sizesOfferTotal.add(szOffer);
                            if (szOffer.compareTo(szBase) != 0) {
                                hasCustomSizeOffer = true;
                            }
                        } else {
                            sizesOfferTotal = sizesOfferTotal.add(szBase);
                        }
                    }
                    if (hasCustomSizeOffer && sizesOfferTotal.compareTo(BigDecimal.ZERO) > 0) {
                        itemOfferPrice = sizesOfferTotal;
                    }
                }
            }

            if (itemOfferPrice == null
                    || itemOfferPrice.compareTo(BigDecimal.ZERO) <= 0) {

                itemOfferPrice = zeroIfNull(item.getAmount());
            }

            offerPrice =
                    offerPrice.add(itemOfferPrice);
        }

        return money(offerPrice);
    }


    // =========================================================
    // UPDATE QUOTATION EXTRA CHARGES
    // =========================================================

    @Transactional
    public Quotation updateCharges(
            Long quotationId,
            BigDecimal accessoriesAmount,
            BigDecimal travelCharge,
            BigDecimal otherCharges,
            String otherChargesDescription,
            BigDecimal discountAmount,
            BigDecimal gstPercentage,
            BigDecimal offerPercentage) {

        if (quotationId == null) {
            throw new IllegalArgumentException(
                    "Quotation ID is required.");
        }

        Quotation quotation =
                quotationRepository.findById(quotationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found."));

        validateNonNegative(
                accessoriesAmount,
                "Accessories amount");

        validateNonNegative(
                travelCharge,
                "Travel charge");

        validateNonNegative(
                otherCharges,
                "Other charges");

        if (otherCharges != null && otherCharges.compareTo(BigDecimal.ZERO) > 0) {
            if (otherChargesDescription == null || otherChargesDescription.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Other charges description is required when other charges amount is entered.");
            }
        }

        validateNonNegative(
                discountAmount,
                "Discount amount");

        validateGstPercentage(gstPercentage);

        validateOfferPercentage(offerPercentage);

        quotation.setAccessoriesAmount(
                zeroIfNull(accessoriesAmount));

        quotation.setTravelCharge(
                zeroIfNull(travelCharge));

        quotation.setOtherCharges(
                zeroIfNull(otherCharges));

        quotation.setOtherChargesDescription(
                otherChargesDescription != null ? otherChargesDescription.trim() : null);

        quotation.setDiscountAmount(
                zeroIfNull(discountAmount));

        quotation.setGstPercentage(
                zeroIfNull(gstPercentage));

        if (offerPercentage != null) {
            quotation.setOfferPercentage(
                    money(offerPercentage));
        }

        return calculateQuotation(quotationId);
    }

    @Transactional
    public Quotation updateCharges(
            Long quotationId,
            BigDecimal accessoriesAmount,
            BigDecimal travelCharge,
            BigDecimal otherCharges,
            BigDecimal discountAmount,
            BigDecimal gstPercentage,
            BigDecimal offerPercentage) {

        Quotation quotation =
                quotationRepository.findById(quotationId)
                        .orElse(null);
        String existingDesc = quotation != null ? quotation.getOtherChargesDescription() : null;
        if ((existingDesc == null || existingDesc.trim().isEmpty()) && otherCharges != null && otherCharges.compareTo(BigDecimal.ZERO) > 0) {
            existingDesc = "Other Charges";
        }

        return updateCharges(
                quotationId,
                accessoriesAmount,
                travelCharge,
                otherCharges,
                existingDesc,
                discountAmount,
                gstPercentage,
                offerPercentage);
    }

    @Transactional
    public Quotation updateCharges(
            Long quotationId,
            BigDecimal accessoriesAmount,
            BigDecimal travelCharge,
            BigDecimal otherCharges,
            BigDecimal discountAmount,
            BigDecimal gstPercentage) {

        Quotation quotation =
                quotationRepository.findById(quotationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found."));

        return updateCharges(
                quotationId,
                accessoriesAmount,
                travelCharge,
                otherCharges,
                discountAmount,
                gstPercentage,
                quotation.getOfferPercentage());
    }


    // =========================================================
    // UPDATE GST
    // =========================================================

    @Transactional
    public Quotation updateGst(
            Long quotationId,
            BigDecimal gstPercentage) {

        if (quotationId == null) {
            throw new IllegalArgumentException(
                    "Quotation ID is required.");
        }

        validateGstPercentage(gstPercentage);

        Quotation quotation =
                quotationRepository.findById(quotationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found."));

        quotation.setGstPercentage(
                zeroIfNull(gstPercentage));

        return calculateQuotation(quotationId);
    }


    // =========================================================
    // UPDATE DISCOUNT
    // =========================================================

    @Transactional
    public Quotation updateDiscount(
            Long quotationId,
            BigDecimal discountAmount) {

        if (quotationId == null) {
            throw new IllegalArgumentException(
                    "Quotation ID is required.");
        }

        validateNonNegative(
                discountAmount,
                "Discount amount");

        Quotation quotation =
                quotationRepository.findById(quotationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found."));

        quotation.setDiscountAmount(
                zeroIfNull(discountAmount));

        return calculateQuotation(quotationId);
    }


    // =========================================================
    // UPDATE ACCESSORIES
    // =========================================================

    @Transactional
    public Quotation updateAccessories(
            Long quotationId,
            BigDecimal accessoriesAmount) {

        if (quotationId == null) {
            throw new IllegalArgumentException(
                    "Quotation ID is required.");
        }

        validateNonNegative(
                accessoriesAmount,
                "Accessories amount");

        Quotation quotation =
                quotationRepository.findById(quotationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found."));

        quotation.setAccessoriesAmount(
                zeroIfNull(accessoriesAmount));

        return calculateQuotation(quotationId);
    }


    // =========================================================
    // UPDATE TRAVEL CHARGE
    // =========================================================

    @Transactional
    public Quotation updateTravelCharge(
            Long quotationId,
            BigDecimal travelCharge) {

        if (quotationId == null) {
            throw new IllegalArgumentException(
                    "Quotation ID is required.");
        }

        validateNonNegative(
                travelCharge,
                "Travel charge");

        Quotation quotation =
                quotationRepository.findById(quotationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found."));

        quotation.setTravelCharge(
                zeroIfNull(travelCharge));

        return calculateQuotation(quotationId);
    }


    // =========================================================
    // UPDATE OTHER CHARGES
    // =========================================================

    @Transactional
    public Quotation updateOtherCharges(
            Long quotationId,
            BigDecimal otherCharges) {

        if (quotationId == null) {
            throw new IllegalArgumentException(
                    "Quotation ID is required.");
        }

        validateNonNegative(
                otherCharges,
                "Other charges");

        Quotation quotation =
                quotationRepository.findById(quotationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found."));

        quotation.setOtherCharges(
                zeroIfNull(otherCharges));

        return calculateQuotation(quotationId);
    }


    // =========================================================
    // GET FINAL GRAND TOTAL
    // =========================================================

    public BigDecimal getGrandTotal(Long quotationId) {

        if (quotationId == null) {
            return BigDecimal.ZERO;
        }

        Quotation quotation =
                quotationRepository.findById(quotationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found."));

        return zeroIfNull(
                quotation.getGrandTotal());
    }


    // =========================================================
    // GET TAXABLE AMOUNT
    // =========================================================

    public BigDecimal getTaxableAmount(Long quotationId) {

        if (quotationId == null) {
            return BigDecimal.ZERO;
        }

        Quotation quotation =
                quotationRepository.findById(quotationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found."));

        return zeroIfNull(
                quotation.getTaxableAmount());
    }


    // =========================================================
    // GET GST AMOUNT
    // =========================================================

    public BigDecimal getGstAmount(Long quotationId) {

        if (quotationId == null) {
            return BigDecimal.ZERO;
        }

        Quotation quotation =
                quotationRepository.findById(quotationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found."));

        return zeroIfNull(
                quotation.getGstAmount());
    }


    // =========================================================
    // MONEY ROUNDING
    // =========================================================

    private BigDecimal money(BigDecimal value) {

        if (value == null) {
            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP);
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP);
    }


    // =========================================================
    // NULL → ZERO
    // =========================================================

    private BigDecimal zeroIfNull(BigDecimal value) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateNonNegative(
            BigDecimal value,
            String fieldName) {

        if (value != null &&
                value.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    fieldName + " cannot be negative.");
        }
    }


    private void validateGstPercentage(
            BigDecimal gstPercentage) {

        if (gstPercentage == null) {
            throw new IllegalArgumentException(
                    "GST percentage is required.");
        }

        if (gstPercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "GST percentage cannot be negative.");
        }

        if (gstPercentage.compareTo(
                new BigDecimal("100")) > 0) {

            throw new IllegalArgumentException(
                    "GST percentage cannot be greater than 100.");
        }
    }

    public void validateOfferPercentage(
            BigDecimal offerPercentage) {

        if (offerPercentage != null) {
            if (offerPercentage.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Offer percentage cannot be negative.");
            }
            if (offerPercentage.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException(
                        "Offer percentage cannot be greater than 100.");
            }
        }
    }

    /**
     * Resolves the rate for a quotation item.
     *
     * Rules:
     * 1. The quotation must have a district/branch.
     * 2. Standard pricing must exist and be active.
     * 3. Active district-specific pricing is mandatory.
     * 4. Missing or inactive district pricing blocks item saving.
     */
    @Transactional(readOnly = true)
    public BigDecimal resolveItemRate(
            Long quotationId,
            Long materialOptionId) {

        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Quotation not found."));

        return resolveItemRate(
                quotationId,
                materialOptionId,
                quotation.getPricingMode()
        );
    }


    /**
     * Resolves the district-specific rate for a material option and
     * an explicitly supplied pricing mode.
     *
     * This overload is used when an employee changes:
     * ECONOMY -> MIDDLE -> PREMIUM.
     */
    @Transactional(readOnly = true)
    public BigDecimal resolveItemRate(
            Long quotationId,
            Long materialOptionId,
            PricingMode pricingMode) {

        if (quotationId == null || materialOptionId == null) {
            throw new IllegalArgumentException(
                    "Quotation and material option are required.");
        }

        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Quotation not found."));

        if (pricingMode == null) {
            throw new IllegalArgumentException(
                    "Quotation pricing mode is required.");
        }

        Pricing pricing = pricingRepository
                .findByMaterialOptionIdAndPricingMode(
                        materialOptionId,
                        pricingMode)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No standard pricing exists for the selected "
                                        + "material option and pricing mode."));

        if (!Boolean.TRUE.equals(pricing.getActive())) {
            throw new IllegalArgumentException(
                    "The selected standard pricing is inactive.");
        }

        if (quotation.getBranch() != null && districtPricingRepository != null) {
            DistrictPricing dp = districtPricingRepository.findByBranchIdAndPricingId(quotation.getBranch().getId(), pricing.getId())
                    .orElse(null);
            if (dp != null && dp.getRate() != null && Boolean.TRUE.equals(dp.getActive())) {
                return money(dp.getRate());
            }

            boolean hasBranchRates = districtPricingRepository.existsByPricingIdAndActiveTrue(pricing.getId());
            if (!hasBranchRates && pricing.getRate() != null) {
                return money(pricing.getRate());
            }

            throw new IllegalArgumentException("No rate has been configured for branch '" + quotation.getBranch().getName() + "' under " + pricingMode.name() + " pricing grade.");
        }

        return money(pricing.getRate());
    }


    /**
     * Changes the quotation pricing mode and recalculates every active
     * quotation item using the new mode. Dimensions, materials and items
     * are preserved.
     */
    @Transactional
    public Quotation changePricingMode(
            Long quotationId,
            PricingMode newMode) {

        if (quotationId == null) {
            throw new IllegalArgumentException(
                    "Quotation ID is required.");
        }

        if (newMode == null) {
            throw new IllegalArgumentException(
                    "Pricing mode is required.");
        }

        Quotation quotation =
                quotationRepository.findById(quotationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found."));

        PricingMode oldMode =
                quotation.getPricingMode();

        quotation.setPricingMode(newMode);

        quotationRepository.save(quotation);

        List<QuotationRoom> rooms =
                quotationRoomRepository
                        .findByQuotationIdAndActiveTrueOrderByIdAsc(
                                quotationId);

        for (QuotationRoom room : rooms) {

            List<QuotationItem> items =
                    quotationItemRepository
                            .findByQuotationRoomIdAndActiveTrueOrderByIdAsc(
                                    room.getId());

            for (QuotationItem item : items) {

                if (item.getMaterialOption() == null
                        || item.getMaterialOption().getId() == null) {

                    throw new IllegalArgumentException(
                            "Material option is missing for quotation item "
                                    + item.getId() + ".");
                }

                // Save old rate BEFORE changing it.
                BigDecimal oldRate =
                        zeroIfNull(item.getRate());

                // -------------------------------------------------
                // Get new rate
                // -------------------------------------------------

                BigDecimal newRate =
                        resolveItemRate(
                                quotationId,
                                item.getMaterialOption().getId(),
                                newMode
                        );

                // -------------------------------------------------
                // Total SQ.FT from all child sizes
                // -------------------------------------------------

                BigDecimal totalSqft =
                        quotationItemSizeService
                                .calculateTotalSqft(
                                        item.getId()
                                );

                totalSqft =
                        money(totalSqft);

                // -------------------------------------------------
                // New total amount
                // -------------------------------------------------

                BigDecimal newAmount;
                if (item.getMeasurementType() == com.example.pravin_quotation.model.MeasurementType.ROLL) {
                    BigDecimal cov = item.getCoveragePerRoll() != null ? item.getCoveragePerRoll()
                            : (item.getMaterial() != null ? item.getMaterial().getCoveragePerRoll() : null);
                    BigDecimal rolls = (cov != null && cov.compareTo(BigDecimal.ZERO) > 0 && totalSqft.compareTo(BigDecimal.ZERO) > 0)
                            ? totalSqft.divide(cov, 0, java.math.RoundingMode.CEILING)
                            : (item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO);
                    item.setQuantity(rolls);
                    newAmount = money(rolls.multiply(newRate));
                } else {
                    newAmount = money(totalSqft.multiply(newRate));
                }

                // -------------------------------------------------
                // Child sizes
                // -------------------------------------------------

                List<QuotationItemSize> sizes =
                        quotationItemSizeService
                                .getActiveSizes(
                                        item.getId()
                                );

                BigDecimal totalOfferPrice =
                        BigDecimal.ZERO;

                for (QuotationItemSize size : sizes) {

                    BigDecimal sizeSqft =
                            zeroIfNull(
                                    size.getCalculatedSqft()
                            );

                    BigDecimal oldSizeAmount;
                    BigDecimal newSizeAmount;
                    if (item.getMeasurementType() == com.example.pravin_quotation.model.MeasurementType.ROLL) {
                        BigDecimal cov = item.getCoveragePerRoll() != null ? item.getCoveragePerRoll()
                                : (item.getMaterial() != null ? item.getMaterial().getCoveragePerRoll() : null);
                        BigDecimal sizeRolls = (cov != null && cov.compareTo(BigDecimal.ZERO) > 0 && sizeSqft.compareTo(BigDecimal.ZERO) > 0)
                                ? sizeSqft.divide(cov, 0, java.math.RoundingMode.CEILING)
                                : (item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO);
                        oldSizeAmount = money(sizeRolls.multiply(oldRate));
                        newSizeAmount = money(sizeRolls.multiply(newRate));
                    } else {
                        oldSizeAmount = money(sizeSqft.multiply(oldRate));
                        newSizeAmount = money(sizeSqft.multiply(newRate));
                    }

                    BigDecimal offerPrice =
                            size.getOfferPrice();

                    // -------------------------------------------------
                    // Normal price → update to new pricing mode
                    // -------------------------------------------------

                    if (offerPrice == null
                            || offerPrice.compareTo(
                            BigDecimal.ZERO) <= 0
                            || offerPrice.compareTo(
                            oldSizeAmount) == 0) {

                        offerPrice =
                                newSizeAmount;
                    }

                    // -------------------------------------------------
                    // Manual discounted price → preserve it
                    // -------------------------------------------------

                    size.setOfferPrice(
                            money(offerPrice)
                    );

                    totalOfferPrice =
                            totalOfferPrice.add(
                                    offerPrice
                            );
                }

                // -------------------------------------------------
                // Update parent quotation item
                // -------------------------------------------------

                item.setCalculatedSqft(
                        totalSqft
                );

                item.setRate(
                        newRate
                );

                item.setAmount(
                        newAmount
                );

                item.setOfferPrice(
                        money(totalOfferPrice)
                );

                quotationItemRepository.save(item);
            }
        }

        // ---------------------------------------------------------
        // Recalculate subtotal + GST + final total
        // ---------------------------------------------------------

        return calculateQuotation(
                quotationId
        );
    }


    /**
     * Applies the configured travel charge when a new quotation is created.
     * Call this once during quotation creation, not on every page refresh.
     */
    @Transactional
    public Quotation initializeTravelCharge(Long quotationId) {

        if (quotationId == null) {
            throw new IllegalArgumentException(
                    "Quotation ID is required.");
        }

        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Quotation not found."));

        if (quotation.getBranch() == null
                || quotation.getBranch().getId() == null) {
            throw new IllegalArgumentException(
                    "Please assign a district to this quotation.");
        }

        List<TravelCharge> charges =
                travelChargeRepository
                        .findByBranchIdAndActiveTrueOrderByCreatedAtDesc(
                                quotation.getBranch().getId());

        BigDecimal travelCharge = charges.isEmpty()
                ? BigDecimal.ZERO
                : zeroIfNull(charges.get(0).getAmount());

        validateNonNegative(travelCharge, "Travel charge");

        quotation.setTravelCharge(money(travelCharge));

        quotationRepository.save(quotation);

        return calculateQuotation(quotationId);
    }

    @Transactional(readOnly = true)
    public java.util.Map<PricingMode, BigDecimal> calculateGradeSubtotals(Long quotationId) {
        java.util.Map<PricingMode, BigDecimal> map = new java.util.LinkedHashMap<>();
        map.put(PricingMode.PREMIUM, BigDecimal.ZERO);
        map.put(PricingMode.MIDDLE, BigDecimal.ZERO);
        map.put(PricingMode.ECONOMY, BigDecimal.ZERO);

        if (quotationId == null) {
            return map;
        }

        List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationIdAndActiveTrueOrderByIdAsc(quotationId);
        for (QuotationRoom room : rooms) {
            List<QuotationItem> items = quotationItemRepository.findByQuotationRoomIdAndActiveTrueOrderByIdAsc(room.getId());
            for (QuotationItem item : items) {
                PricingMode mode = item.getEffectivePricingMode();
                if (mode == null) mode = PricingMode.MIDDLE;

                BigDecimal itemAmt;
                if (item.getOfferPrice() != null && item.getOfferPrice().compareTo(BigDecimal.ZERO) > 0) {
                    itemAmt = item.getOfferPrice();
                } else if (item.getAmount() != null) {
                    itemAmt = item.getAmount();
                } else {
                    itemAmt = BigDecimal.ZERO;
                }

                map.put(mode, money(map.getOrDefault(mode, BigDecimal.ZERO).add(itemAmt)));
            }
        }
        return map;
    }

    @Transactional(readOnly = true)
    public String resolvePricingModeDisplay(Long quotationId) {
        if (quotationId == null) {
            return "Middle / Standard";
        }
        Quotation quotation = quotationRepository.findById(quotationId).orElse(null);
        PricingMode fallback = quotation != null ? quotation.getPricingMode() : PricingMode.MIDDLE;

        java.util.Set<PricingMode> usedGrades = new java.util.LinkedHashSet<>();
        List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationIdAndActiveTrueOrderByIdAsc(quotationId);
        for (QuotationRoom room : rooms) {
            List<QuotationItem> items = quotationItemRepository.findByQuotationRoomIdAndActiveTrueOrderByIdAsc(room.getId());
            for (QuotationItem item : items) {
                PricingMode mode = item.getEffectivePricingMode();
                if (mode != null) {
                    usedGrades.add(mode);
                }
            }
        }
        return formatPricingModeDisplay(usedGrades, fallback);
    }


    public String formatPricingColumnDisplay(java.util.Set<PricingMode> usedGrades, PricingMode confirmedGrade) {
        if (usedGrades == null || usedGrades.isEmpty()) {
            return "Not Prepared";
        }
        List<String> parts = new java.util.ArrayList<>();
        if (usedGrades.contains(PricingMode.PREMIUM)) {
            parts.add((confirmedGrade == PricingMode.PREMIUM ? "✓ " : "") + "PREMIUM");
        }
        if (usedGrades.contains(PricingMode.MIDDLE)) {
            parts.add((confirmedGrade == PricingMode.MIDDLE ? "✓ " : "") + "MIDDLE");
        }
        if (usedGrades.contains(PricingMode.ECONOMY)) {
            parts.add((confirmedGrade == PricingMode.ECONOMY ? "✓ " : "") + "ECONOMY");
        }
        if (parts.isEmpty()) {
            return "Not Prepared";
        }
        return String.join(" + ", parts);
    }

    @Transactional(readOnly = true)
    public void populatePricingModeDisplays(List<Quotation> quotations) {
        if (quotations == null || quotations.isEmpty()) {
            return;
        }
        java.util.Set<Long> quoteIds = quotations.stream()
                .filter(java.util.Objects::nonNull)
                .map(Quotation::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (quoteIds.isEmpty()) {
            return;
        }

        List<Object[]> rows = quotationItemRepository.findDistinctUsedGradesForQuotationIds(quoteIds);
        java.util.Map<Long, java.util.Set<PricingMode>> quoteGradesMap = new java.util.HashMap<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 2) continue;
            Long quoteId = (Long) row[0];
            PricingMode mode = (row[1] instanceof PricingMode) ? (PricingMode) row[1] : (row[1] != null ? PricingMode.valueOf(row[1].toString()) : null);
            if (mode == null && row.length > 2) mode = (row[2] instanceof PricingMode) ? (PricingMode) row[2] : (row[2] != null ? PricingMode.valueOf(row[2].toString()) : null);
            if (mode == null && row.length > 3) mode = (row[3] instanceof PricingMode) ? (PricingMode) row[3] : (row[3] != null ? PricingMode.valueOf(row[3].toString()) : null);
            if (quoteId != null && mode != null) {
                quoteGradesMap.computeIfAbsent(quoteId, k -> new java.util.LinkedHashSet<>()).add(mode);
            }
        }

        for (Quotation q : quotations) {
            if (q == null) continue;
            java.util.Set<PricingMode> used = quoteGradesMap.get(q.getId());
            PricingMode confirmed = q.getSelectedGrade();
            q.setPricingModeDisplay(formatPricingColumnDisplay(used, confirmed));
        }
    }

 public String formatPricingModeDisplay(java.util.Set<PricingMode> usedGrades, PricingMode fallback) {
        if (usedGrades == null || usedGrades.isEmpty()) {
            if (fallback == PricingMode.PREMIUM) return "Premium";
            if (fallback == PricingMode.ECONOMY) return "Economy";
            return "Middle / Standard";
        }
        boolean hasPremium = usedGrades.contains(PricingMode.PREMIUM);
        boolean hasMiddle = usedGrades.contains(PricingMode.MIDDLE);
        boolean hasEconomy = usedGrades.contains(PricingMode.ECONOMY);

        List<String> parts = new java.util.ArrayList<>();
        if (hasPremium) parts.add("Premium");
        if (hasMiddle) {
            if (hasPremium || hasEconomy) {
                parts.add("Middle");
            } else {
                parts.add("Middle / Standard");
            }
        }
        if (hasEconomy) parts.add("Economy");

        if (parts.isEmpty()) {
            return "Middle / Standard";
        }
        return String.join(" + ", parts);
    }
}