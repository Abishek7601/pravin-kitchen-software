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
    private final DistrictPricingRepository districtPricingRepository;
    private final PricingRepository pricingRepository;
    private final TravelChargeRepository travelChargeRepository;

    public QuotationCalculationService(
            QuotationRepository quotationRepository,
            QuotationRoomRepository quotationRoomRepository,
            QuotationItemRepository quotationItemRepository,
            DistrictPricingRepository districtPricingRepository,
            PricingRepository pricingRepository,
            TravelChargeRepository travelChargeRepository) {

        this.quotationRepository = quotationRepository;
        this.quotationRoomRepository = quotationRoomRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.districtPricingRepository = districtPricingRepository;
        this.pricingRepository = pricingRepository;
        this.travelChargeRepository = travelChargeRepository;
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
                    quotationSubtotal.add(roomSubtotal);
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

        BigDecimal taxableAmount =
                quotationSubtotal
                        .add(accessories)
                        .add(travel)
                        .add(otherCharges)
                        .subtract(discount);

        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) {
            taxableAmount = BigDecimal.ZERO;
        }

        quotation.setTaxableAmount(
                money(taxableAmount));

        BigDecimal gstPercentage =
                zeroIfNull(quotation.getGstPercentage());

        BigDecimal gstAmount =
                taxableAmount
                        .multiply(gstPercentage)
                        .divide(
                                new BigDecimal("100"),
                                2,
                                RoundingMode.HALF_UP);

        quotation.setGstAmount(
                money(gstAmount));

        BigDecimal grandTotal =
                taxableAmount.add(gstAmount);

        quotation.setGrandTotal(
                money(grandTotal));

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

            if (itemOfferPrice == null
                   || itemOfferPrice.compareTo(BigDecimal.ZERO)<=0){

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
            BigDecimal discountAmount,
            BigDecimal gstPercentage) {

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

        validateNonNegative(
                discountAmount,
                "Discount amount");

        validateGstPercentage(gstPercentage);

        quotation.setAccessoriesAmount(
                zeroIfNull(accessoriesAmount));

        quotation.setTravelCharge(
                zeroIfNull(travelCharge));

        quotation.setOtherCharges(
                zeroIfNull(otherCharges));

        quotation.setDiscountAmount(
                zeroIfNull(discountAmount));

        quotation.setGstPercentage(
                zeroIfNull(gstPercentage));

        return calculateQuotation(quotationId);
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

        if (quotationId == null || materialOptionId == null) {
            throw new IllegalArgumentException(
                    "Quotation and material option are required.");
        }

        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Quotation not found."));

        Branch branch = quotation.getBranch();

        if (branch == null || branch.getId() == null) {
            throw new IllegalArgumentException(
                    "Please assign a district to this quotation.");
        }

        PricingMode pricingMode = quotation.getPricingMode();

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

        DistrictPricing districtPricing =
                districtPricingRepository
                        .findByBranchIdAndPricingId(
                                branch.getId(),
                                pricing.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "District-specific pricing is not configured "
                                                + "for this item. Please configure "
                                                + "the district rate before saving."));

        if (!Boolean.TRUE.equals(districtPricing.getActive())) {
            throw new IllegalArgumentException(
                    "District-specific pricing is inactive. "
                            + "Activate the district rate before saving.");
        }

        if (districtPricing.getRate() == null
                || districtPricing.getRate()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "The configured district rate must be greater than zero.");
        }

        return money(districtPricing.getRate());
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
}