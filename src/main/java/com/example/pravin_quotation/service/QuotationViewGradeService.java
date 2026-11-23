package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.util.CalculationFormula;
import com.example.pravin_quotation.util.IndianCurrencyFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class QuotationViewGradeService {

    private final QuotationRepository quotationRepository;
    private final QuotationRoomRepository quotationRoomRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final QuotationItemSizeService quotationItemSizeService;
    private final AdminQuotationBuilderService adminQuotationBuilderService;
    private final PricingRepository pricingRepository;

    @Autowired
    public QuotationViewGradeService(
            QuotationRepository quotationRepository,
            QuotationRoomRepository quotationRoomRepository,
            QuotationItemRepository quotationItemRepository,
            QuotationItemSizeService quotationItemSizeService,
            AdminQuotationBuilderService adminQuotationBuilderService,
            PricingRepository pricingRepository) {
        this.quotationRepository = quotationRepository;
        this.quotationRoomRepository = quotationRoomRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.quotationItemSizeService = quotationItemSizeService;
        this.adminQuotationBuilderService = adminQuotationBuilderService;
        this.pricingRepository = pricingRepository;
    }

    public record PaymentTermsDto(
            String token10,
            String advance50,
            String part35,
            String final5
    ) {}

    public record GradeViewItemDto(
            String sno,
            String floor,
            String room,
            String categoryName,
            String workTypeName,
            String materialName,
            String materialOptionName,
            boolean materialAvailable,
            String availabilityMessage,
            String specification,
            String formula,
            String dimensions,
            String sqft,
            String unit,
            String unitRate,
            String amount,
            String offerPrice,
            String imageUrl
    ) {}

    public record GradeViewRoomDto(
            Long roomId,
            String floor,
            String room,
            String workDescription,
            String roomSubtotal,
            BigDecimal roomSubtotalRaw,
            List<GradeViewItemDto> items
    ) {}

    public record GradeViewFinancialsDto(
            String premiumSubtotal,
            String middleSubtotal,
            String economySubtotal,
            String subtotal,
            String accessoriesAmount,
            String travelCharge,
            String otherCharges,
            String otherChargesDescription,
            String discountAmount,
            String offerPercentage,
            String offerAmount,
            String gstPercentage,
            String gstAmount,
            String grandTotal
    ) {}

    public record GradeViewResponse(
            boolean success,
            String message,
            Long quotationId,
            String quotationNumber,
            String activeGrade,
            String activeGradeLabel,
            String quotedProjectValue,
            BigDecimal grandTotalRaw,
            String gradeBadgeClass,
            String gradeBadgeText,
            PaymentTermsDto paymentTerms,
            List<GradeViewRoomDto> rooms,
            GradeViewFinancialsDto financials,
            Boolean showSize,
            Boolean showSqft,
            Boolean showSqftSize
    ) {
        public GradeViewResponse(
                boolean success,
                String message,
                Long quotationId,
                String quotationNumber,
                String activeGrade,
                String activeGradeLabel,
                String quotedProjectValue,
                BigDecimal grandTotalRaw,
                String gradeBadgeClass,
                String gradeBadgeText,
                PaymentTermsDto paymentTerms,
                List<GradeViewRoomDto> rooms,
                GradeViewFinancialsDto financials
        ) {
            this(success, message, quotationId, quotationNumber, activeGrade, activeGradeLabel,
                    quotedProjectValue, grandTotalRaw, gradeBadgeClass, gradeBadgeText,
                    paymentTerms, rooms, financials, true, true, true);
        }
    }

    @Transactional(readOnly = true)
    public GradeViewResponse getGradeViewData(Long quotationId, PricingMode targetGrade) {
        if (quotationId == null) {
            return new GradeViewResponse(false, "Quotation ID is required", null, null, "PREMIUM", "Premium", "₹ 0.00", BigDecimal.ZERO, "grade-badge-premium", "★ Premium: ₹ 0.00", null, Collections.emptyList(), null);
        }

        Quotation quotation = quotationRepository.findById(quotationId).orElse(null);
        if (quotation == null) {
            return new GradeViewResponse(false, "Quotation not found: " + quotationId, quotationId, null, "PREMIUM", "Premium", "₹ 0.00", BigDecimal.ZERO, "grade-badge-premium", "★ Premium: ₹ 0.00", null, Collections.emptyList(), null);
        }

        Quotation parent = quotation.getParent() != null ? quotation.getParent() : quotation;
        Branch branch = quotation.getBranch() != null ? quotation.getBranch() : parent.getBranch();
        Long branchId = branch != null ? branch.getId() : null;

        if (targetGrade == null) {
            if (quotation.getSelectedGrade() != null) {
                targetGrade = quotation.getSelectedGrade();
            } else if (parent.getSelectedGrade() != null) {
                targetGrade = parent.getSelectedGrade();
            } else if (quotation.getPricingMode() != null) {
                targetGrade = quotation.getPricingMode();
            } else {
                targetGrade = PricingMode.PREMIUM;
            }
        }

        final PricingMode effectiveGrade = targetGrade;

        // Check if there is a child quotation for this grade with active rooms
        Quotation childQuote = parent.getChildForGrade(effectiveGrade);
        boolean useChildQuote = false;
        if (childQuote != null && childQuote.getId() != null) {
            List<QuotationRoom> chRooms = quotationRoomRepository.findByQuotationIdAndActiveTrueOrderByIdAsc(childQuote.getId());
            if (!chRooms.isEmpty()) {
                useChildQuote = true;
            }
        }

        List<GradeViewRoomDto> roomDtos = new ArrayList<>();
        BigDecimal totalRoomsSubtotal = BigDecimal.ZERO;

        if (useChildQuote) {
            List<QuotationRoom> childRooms = quotationRoomRepository.findByQuotationIdAndActiveTrueOrderByIdAsc(childQuote.getId());
            int roomIndex = 1;
            for (QuotationRoom room : childRooms) {
                List<QuotationItem> items = quotationItemRepository.findByQuotationRoomIdAndActiveTrueOrderByIdAsc(room.getId());
                List<GradeViewItemDto> itemDtos = new ArrayList<>();
                int itemIndex = 1;
                BigDecimal roomSubtotal = BigDecimal.ZERO;

                for (QuotationItem item : items) {
                    String sno = roomIndex + "." + itemIndex++;
                    String catName = item.getWorkCategory() != null ? item.getWorkCategory().getName().toUpperCase() : "-";
                    String workTypeName = resolveWorkTypeName(item, catName);

                    String matName = item.getMaterial() != null ? item.getMaterial().getName() : "-";
                    String matOptionName = (item.getMaterialOption() != null
                            && item.getMaterial() != null
                            && !item.getMaterialOption().getName().equalsIgnoreCase(item.getMaterial().getName()))
                            ? item.getMaterialOption().getName() : null;

                    String specText = item.getSpecification() != null && !item.getSpecification().isBlank()
                            ? item.getSpecification()
                            : (item.getMaterialSpecification() != null ? item.getMaterialSpecification().getName() : "");

                    String rawFormula = item.getFormula() != null && !item.getFormula().isBlank()
                            ? item.getFormula()
                            : ((item.getMaterial() != null && item.getMaterial().getFormula() != null && !item.getMaterial().getFormula().isBlank())
                                    ? item.getMaterial().getFormula()
                                    : ((item.getMaterialOption() != null && item.getMaterialOption().getFormula() != null)
                                            ? item.getMaterialOption().getFormula() : "(L + W) × H"));

                    String formula = rawFormula != null && !rawFormula.isBlank()
                            ? "Formula: " + rawFormula : "";

                    String dims = buildDimensionsString(item, rawFormula);
                    String sqftStr = (item.getMeasurementType() != null && !item.getMeasurementType().isSqft() && !item.getMeasurementType().isRoll())
                            ? "-" : formatDecimal(item.getCalculatedSqft() != null ? item.getCalculatedSqft() : BigDecimal.ZERO);

                    String unitStr = resolveItemUnit(item, effectiveGrade);
                    BigDecimal rate = item.getRate() != null ? item.getRate() : BigDecimal.ZERO;
                    String rateStr = "₹ " + IndianCurrencyFormatter.formatAmount(rate)
                            + (item.getMeasurementType() == MeasurementType.ROLL ? " / roll" : "");

                    BigDecimal itemAmt = item.getOfferPrice() != null && item.getOfferPrice().compareTo(BigDecimal.ZERO) > 0
                            ? item.getOfferPrice() : (item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
                    String amtStr = "₹ " + IndianCurrencyFormatter.formatAmount(itemAmt);
                    String offerStr = (item.getOfferPrice() != null && item.getOfferPrice().compareTo(BigDecimal.ZERO) > 0 && item.getAmount() != null && item.getOfferPrice().compareTo(item.getAmount()) < 0)
                            ? "₹ " + IndianCurrencyFormatter.formatAmount(item.getOfferPrice()) : null;

                    roomSubtotal = roomSubtotal.add(itemAmt);

                    itemDtos.add(new GradeViewItemDto(
                            sno,
                            room.getFloor() != null ? room.getFloor() : "-",
                            room.getRoom() != null ? room.getRoom() : "-",
                            catName,
                            workTypeName,
                            matName + " (" + formatGrade(effectiveGrade) + ")",
                            matOptionName,
                            true,
                            null,
                            specText,
                            formula,
                            dims,
                            sqftStr,
                            unitStr,
                            rateStr,
                            amtStr,
                            offerStr,
                            item.getImageUrl()
                    ));
                }

                totalRoomsSubtotal = totalRoomsSubtotal.add(roomSubtotal);
                roomDtos.add(new GradeViewRoomDto(
                        room.getId(),
                        room.getFloor(),
                        room.getRoom(),
                        room.getWorkDescription(),
                        "₹ " + IndianCurrencyFormatter.formatAmount(roomSubtotal),
                        roomSubtotal,
                        itemDtos
                ));
                roomIndex++;
            }
        } else {
            // Single quotation or un-prepared grade: dynamically resolve materials using position-based mapping
            Quotation baseQuote = (quotation.getId() != null && !quotationRoomRepository.findByQuotationIdAndActiveTrueOrderByIdAsc(quotation.getId()).isEmpty())
                    ? quotation : parent;

            List<QuotationRoom> baseRooms = quotationRoomRepository.findByQuotationIdAndActiveTrueOrderByIdAsc(baseQuote.getId());
            int roomIndex = 1;
            Map<Long, Integer> divisionItemCounter = new HashMap<>();

            for (QuotationRoom room : baseRooms) {
                divisionItemCounter.clear();
                List<QuotationItem> items = quotationItemRepository.findByQuotationRoomIdAndActiveTrueOrderByIdAsc(room.getId());
                List<GradeViewItemDto> itemDtos = new ArrayList<>();
                int itemIndex = 1;
                BigDecimal roomSubtotal = BigDecimal.ZERO;

                for (QuotationItem item : items) {
                    String sno = roomIndex + "." + itemIndex++;
                    Long catId = item.getWorkCategory() != null ? item.getWorkCategory().getId()
                            : (item.getDivision() != null && item.getDivision().getWorkCategory() != null ? item.getDivision().getWorkCategory().getId() : null);
                    String catName = item.getWorkCategory() != null ? item.getWorkCategory().getName().toUpperCase() : "-";

                    Long divId = item.getDivision() != null ? item.getDivision().getId()
                            : (item.getMaterial() != null && item.getMaterial().getDivision() != null ? item.getMaterial().getDivision().getId() : null);
                    String workTypeName = resolveWorkTypeName(item, catName);

                    int posInDivision = 0;
                    if (divId != null) {
                        posInDivision = divisionItemCounter.getOrDefault(divId, 0);
                        divisionItemCounter.put(divId, posInDivision + 1);
                    }

                    PricingMode itemOriginGrade = item.getEffectivePricingMode() != null ? item.getEffectivePricingMode() : baseQuote.getPricingMode();
                    if (itemOriginGrade == null) {
                        itemOriginGrade = PricingMode.PREMIUM;
                    }

                    Integer position = null;
                    if (item.getMaterial() != null && divId != null && itemOriginGrade != null) {
                        List<Material> originMats = adminQuotationBuilderService.getActiveMaterialsForDivisionAndGrade(divId, itemOriginGrade, branchId);
                        if (originMats != null) {
                            for (int mIdx = 0; mIdx < originMats.size(); mIdx++) {
                                if (item.getMaterial().getId().equals(originMats.get(mIdx).getId())) {
                                    position = mIdx;
                                    break;
                                }
                            }
                        }
                    }
                    if (position == null) {
                        position = posInDivision;
                    }

                    boolean isAlreadyTargetGrade = (itemOriginGrade == effectiveGrade && item.getMaterial() != null
                            && item.getMaterial().getPricingGrade() == effectiveGrade);

                    String matName;
                    String matOptionName = null;
                    boolean materialAvailable;
                    String availabilityMsg = null;
                    String specText = "";
                    String formula = "";
                    BigDecimal rate = BigDecimal.ZERO;
                    MeasurementType measurementType = item.getMeasurementType() != null ? item.getMeasurementType() : MeasurementType.SQFT;
                    String unitStr;
                    BigDecimal sqft = BigDecimal.ZERO;
                    BigDecimal itemAmt = BigDecimal.ZERO;
                    String imageUrl = item.getImageUrl();

                    if (isAlreadyTargetGrade) {
                        Material m = item.getMaterial();
                        materialAvailable = Boolean.TRUE.equals(m.getActive());
                        matName = m.getName() + " (" + formatGrade(effectiveGrade) + ")";
                        if (item.getMaterialOption() != null && !item.getMaterialOption().getName().equalsIgnoreCase(m.getName())) {
                            matOptionName = item.getMaterialOption().getName();
                        }
                        specText = item.getSpecification() != null && !item.getSpecification().isBlank()
                                ? item.getSpecification()
                                : (item.getMaterialSpecification() != null ? item.getMaterialSpecification().getName() : "");
                        formula = item.getFormula() != null && !item.getFormula().isBlank() ? item.getFormula() : "(L + W) × H";
                        rate = item.getRate() != null ? item.getRate() : BigDecimal.ZERO;
                        measurementType = item.getMeasurementType() != null ? item.getMeasurementType() : MeasurementType.SQFT;
                        unitStr = resolveItemUnit(item, effectiveGrade);
                        sqft = item.getCalculatedSqft() != null ? item.getCalculatedSqft() : BigDecimal.ZERO;
                        itemAmt = item.getOfferPrice() != null && item.getOfferPrice().compareTo(BigDecimal.ZERO) > 0
                                ? item.getOfferPrice() : (item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
                    } else {
                        Long lookupMatId = item.getMaterial() != null ? item.getMaterial().getId() : null;
                        Long lookupOptId = item.getMaterialOption() != null ? item.getMaterialOption().getId() : null;

                        AdminQuotationBuilderService.ResolvedMaterialRate res = adminQuotationBuilderService.resolveMaterialAndRate(
                                branchId,
                                divId,
                                lookupMatId,
                                lookupOptId,
                                effectiveGrade,
                                position
                        );

                        if (res != null && res.available() && res.rate() != null && res.rate().compareTo(BigDecimal.ZERO) > 0) {
                            materialAvailable = true;
                            matName = res.materialName() + " (" + formatGrade(effectiveGrade) + ")";
                            specText = res.specification() != null ? res.specification() : "";
                            formula = res.formula() != null && !res.formula().isBlank() ? res.formula() : "(L + W) × H";
                            rate = res.rate();
                            measurementType = res.measurementType() != null ? res.measurementType() : MeasurementType.SQFT;
                            unitStr = (res.unit() != null && !res.unit().isBlank()) ? res.unit().toUpperCase()
                                    : (measurementType == MeasurementType.ROLL ? "ROLL" : (measurementType == MeasurementType.QUANTITY ? "QTY" : (measurementType == MeasurementType.FILES ? "FILES" : "SQ.FT")));
                            if (res.imageUrl() != null && !res.imageUrl().isBlank()) {
                                imageUrl = res.imageUrl();
                            }

                            // Recalculate dimensions & sqft & amount
                            BigDecimal[] calcRes = calculateItemSqftAndAmount(item, formula, rate, measurementType, res.coveragePerRoll());
                            sqft = calcRes[0];
                            itemAmt = calcRes[1];
                        } else {
                            materialAvailable = false;
                            String gradeLabel = formatGrade(effectiveGrade);
                            matName = "Material Unavailable (" + gradeLabel + " Grade)";
                            availabilityMsg = (res != null && res.message() != null && !res.message().isBlank())
                                    ? res.message()
                                    : "No material available in " + gradeLabel + " Grade for this item.";
                            specText = "";
                            formula = "";
                            rate = BigDecimal.ZERO;
                            sqft = BigDecimal.ZERO;
                            itemAmt = BigDecimal.ZERO;
                            unitStr = "-";
                        }
                    }

                    String dims = buildDimensionsString(item, formula);
                    String sqftStr = (!materialAvailable || (!measurementType.isSqft() && !measurementType.isRoll()))
                            ? "-" : formatDecimal(sqft);
                    String rateStr = materialAvailable
                            ? ("₹ " + IndianCurrencyFormatter.formatAmount(rate) + (measurementType == MeasurementType.ROLL ? " / roll" : ""))
                            : "₹ 0.00";
                    String amtStr = "₹ " + IndianCurrencyFormatter.formatAmount(itemAmt);

                    roomSubtotal = roomSubtotal.add(itemAmt);

                    itemDtos.add(new GradeViewItemDto(
                            sno,
                            room.getFloor() != null ? room.getFloor() : "-",
                            room.getRoom() != null ? room.getRoom() : "-",
                            catName,
                            workTypeName,
                            matName,
                            matOptionName,
                            materialAvailable,
                            availabilityMsg,
                            specText,
                            formula.isBlank() ? "" : "Formula: " + formula,
                            dims,
                            sqftStr,
                            unitStr,
                            rateStr,
                            amtStr,
                            null,
                            imageUrl
                    ));
                }

                totalRoomsSubtotal = totalRoomsSubtotal.add(roomSubtotal);
                roomDtos.add(new GradeViewRoomDto(
                        room.getId(),
                        room.getFloor(),
                        room.getRoom(),
                        room.getWorkDescription(),
                        "₹ " + IndianCurrencyFormatter.formatAmount(roomSubtotal),
                        roomSubtotal,
                        itemDtos
                ));
                roomIndex++;
            }
        }

        // Financial calculations
        Quotation calcBase = useChildQuote ? childQuote : (quotation.getParent() != null ? quotation.getParent() : quotation);

        BigDecimal accessories = zeroIfNull(calcBase.getAccessoriesAmount());
        BigDecimal travel = zeroIfNull(calcBase.getTravelCharge());
        BigDecimal otherCharges = zeroIfNull(calcBase.getOtherCharges());
        String otherChargesDesc = calcBase.getOtherChargesDescription() != null ? calcBase.getOtherChargesDescription().trim() : "";
        BigDecimal discount = zeroIfNull(calcBase.getDiscountAmount());
        BigDecimal offerPct = zeroIfNull(calcBase.getOfferPercentage());
        BigDecimal gstPct = zeroIfNull(calcBase.getGstPercentage());

        BigDecimal amountBeforeDiscount = totalRoomsSubtotal.add(accessories).add(travel).add(otherCharges);
        BigDecimal amountAfterDiscount = amountBeforeDiscount.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal offerAmount = amountAfterDiscount.multiply(offerPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal taxableAmount = amountAfterDiscount.subtract(offerAmount).max(BigDecimal.ZERO);
        BigDecimal gstAmount = taxableAmount.multiply(gstPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = taxableAmount.add(gstAmount);

        // Payment terms
        BigDecimal token10 = grandTotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal advance50 = grandTotal.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal part35 = grandTotal.multiply(new BigDecimal("0.35")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal final5 = grandTotal.subtract(token10).subtract(advance50).subtract(part35).max(BigDecimal.ZERO);

        PaymentTermsDto paymentTerms = new PaymentTermsDto(
                "₹ " + IndianCurrencyFormatter.formatAmount(token10),
                "₹ " + IndianCurrencyFormatter.formatAmount(advance50),
                "₹ " + IndianCurrencyFormatter.formatAmount(part35),
                "₹ " + IndianCurrencyFormatter.formatAmount(final5)
        );

        String activeGradeLabel = effectiveGrade == PricingMode.MIDDLE ? "Middle / Standard" : formatGrade(effectiveGrade);
        String badgeSymbol = effectiveGrade == PricingMode.PREMIUM ? "★" : (effectiveGrade == PricingMode.ECONOMY ? "☘" : "✦");
        String badgeClass = "grade-badge grade-badge-" + effectiveGrade.name().toLowerCase();
        String badgeText = badgeSymbol + " " + formatGrade(effectiveGrade) + ": ₹ " + IndianCurrencyFormatter.formatAmount(grandTotal);

        GradeViewFinancialsDto financials = new GradeViewFinancialsDto(
                effectiveGrade == PricingMode.PREMIUM ? ("₹ " + IndianCurrencyFormatter.formatAmount(totalRoomsSubtotal)) : null,
                effectiveGrade == PricingMode.MIDDLE ? ("₹ " + IndianCurrencyFormatter.formatAmount(totalRoomsSubtotal)) : null,
                effectiveGrade == PricingMode.ECONOMY ? ("₹ " + IndianCurrencyFormatter.formatAmount(totalRoomsSubtotal)) : null,
                "₹ " + IndianCurrencyFormatter.formatAmount(totalRoomsSubtotal),
                "₹ " + IndianCurrencyFormatter.formatAmount(accessories),
                "₹ " + IndianCurrencyFormatter.formatAmount(travel),
                "₹ " + IndianCurrencyFormatter.formatAmount(otherCharges),
                otherChargesDesc,
                "- ₹ " + IndianCurrencyFormatter.formatAmount(discount),
                offerPct.stripTrailingZeros().toPlainString(),
                "- ₹ " + IndianCurrencyFormatter.formatAmount(offerAmount),
                gstPct.stripTrailingZeros().toPlainString(),
                "₹ " + IndianCurrencyFormatter.formatAmount(gstAmount),
                "₹ " + IndianCurrencyFormatter.formatAmount(grandTotal)
        );

        return new GradeViewResponse(
                true,
                "Quotation details loaded for " + activeGradeLabel + " Grade",
                quotation.getId(),
                quotation.getQuotationNumber(),
                effectiveGrade.name(),
                activeGradeLabel,
                "₹ " + IndianCurrencyFormatter.formatAmount(grandTotal),
                grandTotal,
                badgeClass,
                badgeText,
                paymentTerms,
                roomDtos,
                financials,
                quotation.getShowSize() != null ? quotation.getShowSize() : true,
                quotation.getShowSqft() != null ? quotation.getShowSqft() : true,
                quotation.getShowSqftSize() != null ? quotation.getShowSqftSize() : true
        );
    }

    private BigDecimal[] calculateItemSqftAndAmount(QuotationItem item, String formula, BigDecimal rate, MeasurementType mType, BigDecimal coveragePerRoll) {
        if (rate == null) rate = BigDecimal.ZERO;
        List<QuotationItemSize> sizes = quotationItemSizeService.getActiveSizes(item.getId());

        BigDecimal totalSqft = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (mType == null || mType.isSqft()) {
            if (sizes != null && !sizes.isEmpty()) {
                for (QuotationItemSize s : sizes) {
                    if (s.getActive() != null && !s.getActive()) continue;
                    BigDecimal l = s.getLengthValue() != null ? s.getLengthValue() : BigDecimal.ZERO;
                    BigDecimal w = s.getWidthValue() != null ? s.getWidthValue() : BigDecimal.ZERO;
                    BigDecimal h = s.getHeightValue() != null ? s.getHeightValue() : BigDecimal.ZERO;
                    BigDecimal sqft = quotationItemSizeService.calculateSqft(formula, l, w, h, "in");
                    totalSqft = totalSqft.add(sqft);
                    totalAmount = totalAmount.add(sqft.multiply(rate));
                }
            } else {
                BigDecimal l = item.getLengthValue() != null ? item.getLengthValue() : BigDecimal.ZERO;
                BigDecimal w = item.getWidthValue() != null ? item.getWidthValue() : BigDecimal.ZERO;
                BigDecimal h = item.getHeightValue() != null ? item.getHeightValue() : BigDecimal.ZERO;
                BigDecimal sqft = quotationItemSizeService.calculateSqft(formula, l, w, h, "in");
                totalSqft = sqft;
                totalAmount = sqft.multiply(rate);
            }
        } else if (mType.isRoll()) {
            if (sizes != null && !sizes.isEmpty()) {
                for (QuotationItemSize s : sizes) {
                    if (s.getActive() != null && !s.getActive()) continue;
                    BigDecimal l = s.getLengthValue() != null ? s.getLengthValue() : BigDecimal.ZERO;
                    BigDecimal w = s.getWidthValue() != null ? s.getWidthValue() : BigDecimal.ZERO;
                    BigDecimal h = s.getHeightValue() != null ? s.getHeightValue() : BigDecimal.ZERO;
                    BigDecimal sqft = quotationItemSizeService.calculateSqft(formula, l, w, h, "in");
                    totalSqft = totalSqft.add(sqft);
                }
            } else {
                BigDecimal l = item.getLengthValue() != null ? item.getLengthValue() : BigDecimal.ZERO;
                BigDecimal w = item.getWidthValue() != null ? item.getWidthValue() : BigDecimal.ZERO;
                BigDecimal h = item.getHeightValue() != null ? item.getHeightValue() : BigDecimal.ZERO;
                totalSqft = quotationItemSizeService.calculateSqft(formula, l, w, h, "in");
            }
            BigDecimal cov = coveragePerRoll != null && coveragePerRoll.compareTo(BigDecimal.ZERO) > 0
                    ? coveragePerRoll : (item.getCoveragePerRoll() != null ? item.getCoveragePerRoll() : new BigDecimal("50"));
            BigDecimal rolls = totalSqft.compareTo(BigDecimal.ZERO) > 0 ? totalSqft.divide(cov, 0, RoundingMode.CEILING) : BigDecimal.ONE;
            totalAmount = rolls.multiply(rate);
        } else {
            BigDecimal qty = (item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                    ? item.getQuantity() : BigDecimal.ONE;
            totalAmount = qty.multiply(rate);
            totalSqft = BigDecimal.ZERO;
        }

        totalSqft = totalSqft.setScale(2, RoundingMode.HALF_UP);
        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal[]{totalSqft, totalAmount};
    }

    /**
     * Resolves the canonical Work Type (Division) name for a quotation item.
     * Prevents repeating the Work Category name when a specific Work Type is present.
     */
    public static String resolveWorkTypeName(QuotationItem item, String categoryName) {
        if (item == null) return "-";
        String candidate = null;

        // 1. item.division (the canonical Work Type entity in this system)
        if (item.getDivision() != null && item.getDivision().getName() != null && !item.getDivision().getName().isBlank()) {
            candidate = item.getDivision().getName().trim();
        }

        // 2. item.item.division
        if ((candidate == null || (categoryName != null && candidate.equalsIgnoreCase(categoryName)))
                && item.getItem() != null && item.getItem().getDivision() != null && item.getItem().getDivision().getName() != null
                && !item.getItem().getDivision().getName().isBlank()) {
            candidate = item.getItem().getDivision().getName().trim();
        }

        // 3. item.material.division
        if ((candidate == null || (categoryName != null && candidate.equalsIgnoreCase(categoryName)))
                && item.getMaterial() != null && item.getMaterial().getDivision() != null && item.getMaterial().getDivision().getName() != null
                && !item.getMaterial().getDivision().getName().isBlank()) {
            candidate = item.getMaterial().getDivision().getName().trim();
        }

        // 4. item.material.divisions
        if ((candidate == null || (categoryName != null && candidate.equalsIgnoreCase(categoryName)))
                && item.getMaterial() != null && item.getMaterial().getDivisions() != null) {
            for (Division div : item.getMaterial().getDivisions()) {
                if (div != null && div.getName() != null && !div.getName().isBlank()) {
                    if (categoryName == null || !div.getName().equalsIgnoreCase(categoryName)) {
                        candidate = div.getName().trim();
                        break;
                    } else if (candidate == null) {
                        candidate = div.getName().trim();
                    }
                }
            }
        }

        // 5. item.item.name
        if ((candidate == null || (categoryName != null && candidate.equalsIgnoreCase(categoryName)))
                && item.getItem() != null && item.getItem().getName() != null && !item.getItem().getName().isBlank()) {
            String iname = item.getItem().getName().trim();
            if (categoryName == null || !iname.equalsIgnoreCase(categoryName)) {
                candidate = iname;
            } else if (candidate == null) {
                candidate = iname;
            }
        }

        // 6. item.itemDescription
        if ((candidate == null || (categoryName != null && candidate.equalsIgnoreCase(categoryName)))
                && item.getItemDescription() != null && !item.getItemDescription().isBlank()) {
            String idesc = item.getItemDescription().trim();
            if (categoryName == null || !idesc.equalsIgnoreCase(categoryName)) {
                candidate = idesc;
            } else if (candidate == null) {
                candidate = idesc;
            }
        }

        return (candidate != null && !candidate.isBlank()) ? candidate : "-";
    }

    public String buildDimensionsString(QuotationItem item) {
        String formula = (item.getFormula() != null && !item.getFormula().isBlank())
                ? item.getFormula()
                : ((item.getMaterial() != null && item.getMaterial().getFormula() != null && !item.getMaterial().getFormula().isBlank())
                        ? item.getMaterial().getFormula()
                        : ((item.getMaterialOption() != null && item.getMaterialOption().getFormula() != null)
                                ? item.getMaterialOption().getFormula() : "(L + W) × H"));
        return buildDimensionsString(item, formula);
    }

    public String buildDimensionsString(QuotationItem item, String formula) {
        if (item.getMeasurementType() != null && !item.getMeasurementType().isSqft() && !item.getMeasurementType().isRoll()) {
            BigDecimal qty = (item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                    ? item.getQuantity() : BigDecimal.ONE;
            return "Qty: " + qty.stripTrailingZeros().toPlainString();
        }

        List<QuotationItemSize> sizes = quotationItemSizeService.getActiveSizes(item.getId());
        if (sizes != null && !sizes.isEmpty()) {
            List<String> lines = new ArrayList<>();
            int idx = 1;
            for (QuotationItemSize s : sizes) {
                if (s.getActive() != null && !s.getActive()) continue;
                String prefix = sizes.size() > 1 ? ("#" + idx + ": ") : "";
                lines.add(prefix + formatFormulaDimension(formula, s.getLengthValue(), s.getWidthValue(), s.getHeightValue()));
                idx++;
            }
            if (!lines.isEmpty()) {
                return String.join("<br>", lines);
            }
        }

        return formatFormulaDimension(formula, item.getLengthValue(), item.getWidthValue(), item.getHeightValue());
    }

    public static String formatFormulaDimension(String formula, BigDecimal length, BigDecimal width, BigDecimal height) {
        String norm = CalculationFormula.normalizeFormula(formula);
        String l = formatDim(length);
        String w = formatDim(width);
        String h = formatDim(height);

        if (CalculationFormula.FORMULA_L_X_H.equals(norm)) {
            return "L: " + l + ", H: " + h;
        } else if (CalculationFormula.FORMULA_L_PLUS_W_X_H.equals(norm)) {
            return "L: " + l + ", W: " + w + ", H: " + h;
        } else {
            return "L: " + l + ", W: " + w;
        }
    }

    public static String formatDim(BigDecimal val) {
        if (val == null) return "0";
        BigDecimal scaled = val.setScale(2, RoundingMode.HALF_UP);
        if (scaled.stripTrailingZeros().scale() <= 0) {
            return scaled.setScale(0, RoundingMode.HALF_UP).toPlainString();
        }
        return scaled.stripTrailingZeros().toPlainString();
    }

    private String resolveItemUnit(QuotationItem item, PricingMode pricingMode) {
        if (item.getMeasurementType() == MeasurementType.ROLL || (item.getMaterial() != null && item.getMaterial().getMeasurementType() == MeasurementType.ROLL)) {
            return "ROLL";
        }
        if (item.getMeasurementType() == MeasurementType.QUANTITY) {
            return "QTY";
        }
        if (item.getMeasurementType() == MeasurementType.FILES) {
            return "FILES";
        }
        if (item.getMaterial() != null && item.getMaterial().getMeasurementType() == MeasurementType.QUANTITY) {
            return "QTY";
        }
        if (item.getMaterial() != null && item.getMaterial().getMeasurementType() == MeasurementType.FILES) {
            return "FILES";
        }
        if (item.getMaterialOption() != null && pricingMode != null) {
            Optional<Pricing> pricingOpt = pricingRepository.findByMaterialOptionIdAndPricingMode(
                    item.getMaterialOption().getId(),
                    pricingMode
            );
            if (pricingOpt.isPresent() && pricingOpt.get().getUnit() != null) {
                String u = pricingOpt.get().getUnit().trim().toUpperCase();
                switch (u) {
                    case "SQFT":
                    case "SQ.FT":
                    case "SQ_FT":
                    case "SFT":
                        return "SQ.FT";
                    case "RUNNING_FEET":
                    case "RFT":
                    case "R.FT":
                        return "R.FT";
                    case "NOS":
                    case "NO":
                    case "QUANTITY":
                        return "QTY";
                    case "FILES":
                        return "FILES";
                    case "ROLL":
                    case "ROLLS":
                        return "ROLL";
                    case "LUMP_SUM":
                    case "LS":
                        return "LS";
                    default:
                        return u;
                }
            }
        }
        return "SQ.FT";
    }

    private String formatGrade(PricingMode mode) {
        if (mode == null) return "Premium";
        switch (mode) {
            case PREMIUM: return "Premium";
            case MIDDLE: return "Middle";
            case ECONOMY: return "Economy";
            default: return mode.name();
        }
    }

    private String formatDecimal(BigDecimal val) {
        if (val == null) return "0.00";
        return val.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal zeroIfNull(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
