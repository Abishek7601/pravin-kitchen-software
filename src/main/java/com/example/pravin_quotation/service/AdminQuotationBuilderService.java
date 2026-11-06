package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.util.TitleCaseUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class AdminQuotationBuilderService {

    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final WorkCategoryRepository workCategoryRepository;
    private final DivisionRepository divisionRepository;
    private final ItemRepository itemRepository;
    private final MaterialRepository materialRepository;
    private final MaterialOptionRepository materialOptionRepository;
    private final MaterialSpecificationRepository materialSpecificationRepository;
    private final PricingRepository pricingRepository;
    private final DistrictPricingRepository districtPricingRepository;
    private final QuotationRoomService quotationRoomService;
    private final QuotationItemService quotationItemService;
    private final QuotationItemSizeService quotationItemSizeService;
    private final QuotationCalculationService quotationCalculationService;

    @org.springframework.beans.factory.annotation.Autowired
    public AdminQuotationBuilderService(
            QuotationRepository quotationRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            BranchRepository branchRepository,
            WorkCategoryRepository workCategoryRepository,
            DivisionRepository divisionRepository,
            ItemRepository itemRepository,
            MaterialRepository materialRepository,
            MaterialOptionRepository materialOptionRepository,
            MaterialSpecificationRepository materialSpecificationRepository,
            PricingRepository pricingRepository,
            DistrictPricingRepository districtPricingRepository,
            QuotationRoomService quotationRoomService,
            QuotationItemService quotationItemService,
            QuotationItemSizeService quotationItemSizeService,
            QuotationCalculationService quotationCalculationService) {

        this.quotationRepository = quotationRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.workCategoryRepository = workCategoryRepository;
        this.divisionRepository = divisionRepository;
        this.itemRepository = itemRepository;
        this.materialRepository = materialRepository;
        this.materialOptionRepository = materialOptionRepository;
        this.materialSpecificationRepository = materialSpecificationRepository;
        this.pricingRepository = pricingRepository;
        this.districtPricingRepository = districtPricingRepository;
        this.quotationRoomService = quotationRoomService;
        this.quotationItemService = quotationItemService;
        this.quotationItemSizeService = quotationItemSizeService;
        this.quotationCalculationService = quotationCalculationService;
    }

    public AdminQuotationBuilderService(
            QuotationRepository quotationRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            BranchRepository branchRepository,
            WorkCategoryRepository workCategoryRepository,
            DivisionRepository divisionRepository,
            ItemRepository itemRepository,
            MaterialRepository materialRepository,
            MaterialOptionRepository materialOptionRepository,
            MaterialSpecificationRepository materialSpecificationRepository,
            PricingRepository pricingRepository,
            QuotationRoomService quotationRoomService,
            QuotationItemService quotationItemService,
            QuotationItemSizeService quotationItemSizeService,
            QuotationCalculationService quotationCalculationService) {

        this(quotationRepository, customerRepository, userRepository,
                branchRepository, workCategoryRepository, divisionRepository,
                itemRepository, materialRepository, materialOptionRepository,
                materialSpecificationRepository, pricingRepository, null,
                quotationRoomService, quotationItemService, quotationItemSizeService,
                quotationCalculationService);
    }

    // =========================================================
    // RESOLVE RATE
    // =========================================================

    public record ResolvedMaterialRate(
            Long materialId,
            Long materialOptionId,
            String materialName,
            BigDecimal rate,
            String formula,
            String unit,
            MeasurementType measurementType,
            String imageUrl,
            BigDecimal coveragePerRoll,
            boolean available,
            String message
    ) {
        public static ResolvedMaterialRate unavailable(String message) {
            return new ResolvedMaterialRate(null, null, null, null, null, null, null, null, null, false, message);
        }

        public static ResolvedMaterialRate unavailable(Long materialId, String materialName, String message) {
            return new ResolvedMaterialRate(materialId, null, materialName, null, null, null, null, null, null, false, message);
        }
    }

    @Transactional(readOnly = true)
    public ResolvedMaterialRate resolveMaterialAndRate(
            Long branchId,
            Long divisionId,
            Long materialId,
            Long materialOptionId,
            PricingMode targetGrade) {

        if (targetGrade == null) {
            return ResolvedMaterialRate.unavailable("Pricing grade is required.");
        }

        if (branchId == null && branchRepository != null) {
            Branch defBranch = branchRepository.findAll().stream().filter(b -> Boolean.TRUE.equals(b.getActive())).findFirst().orElse(null);
            if (defBranch != null) {
                branchId = defBranch.getId();
            }
        }

        Material mat = (materialId != null) ? materialRepository.findById(materialId).orElse(null) : null;
        MaterialOption opt = (materialOptionId != null) ? materialOptionRepository.findById(materialOptionId).orElse(null) : null;
        if (mat == null && opt != null && opt.getMaterial() != null) {
            mat = opt.getMaterial();
            materialId = mat.getId();
        }

        if (mat == null) {
            return ResolvedMaterialRate.unavailable("Selected material not found.");
        }

        if (!Boolean.TRUE.equals(mat.getActive())) {
            return ResolvedMaterialRate.unavailable(mat.getId(), mat.getName(), "Material '" + mat.getName() + "' is inactive.");
        }

        if (divisionId == null && mat != null) {
            if (mat.getDivision() != null) {
                divisionId = mat.getDivision().getId();
            } else if (mat.getItem() != null && mat.getItem().getDivision() != null) {
                divisionId = mat.getItem().getDivision().getId();
            } else if (mat.getDivisions() != null && !mat.getDivisions().isEmpty()) {
                divisionId = mat.getDivisions().iterator().next().getId();
            }
        }

        Branch branch = null;
        if (branchId != null) {
            branch = branchRepository.findById(branchId).orElse(null);
        }

        BigDecimal matchedRate = null;

        // 1. Direct option match if materialOption is provided
        if (opt != null) {
            Pricing p = pricingRepository.findByMaterialOptionIdAndPricingMode(opt.getId(), targetGrade).orElse(null);
            if (p != null && Boolean.TRUE.equals(p.getActive())) {
                BigDecimal r = resolveRateFromPricing(branchId, p);
                if (r != null && r.compareTo(BigDecimal.ZERO) > 0) {
                    matchedRate = r;
                }
            }
        }

        // 2. Direct material match across all options belonging to this material
        if (matchedRate == null && mat != null) {
            List<MaterialOption> opts = materialOptionRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(mat.getId());
            if (opts.isEmpty()) {
                opts = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(mat.getId());
            }
            for (MaterialOption o : opts) {
                Pricing p = pricingRepository.findByMaterialOptionIdAndPricingMode(o.getId(), targetGrade).orElse(null);
                if (p != null && Boolean.TRUE.equals(p.getActive())) {
                    BigDecimal r = resolveRateFromPricing(branchId, p);
                    if (r != null && r.compareTo(BigDecimal.ZERO) > 0) {
                        matchedRate = r;
                        break;
                    }
                }
            }
        }

        // 3. Direct rate on mat itself if mat's pricingGrade matches targetGrade
        if (matchedRate == null && mat != null && mat.getPricingGrade() == targetGrade && mat.getRate() != null && mat.getRate().compareTo(BigDecimal.ZERO) > 0) {
            matchedRate = money(mat.getRate());
        }

        Material counterpart = null;
        // 4. Exact counterpart match in the same division (Work Type) for targetGrade
        if (matchedRate == null && divisionId != null && mat != null) {
            List<Material> candidates = materialRepository.findActiveByDivisionIdAndPricingGrade(divisionId, targetGrade);
            if (candidates.isEmpty()) {
                candidates = materialRepository.findAllByDivisionIdAndPricingGrade(divisionId, targetGrade);
            }
            if (candidates.isEmpty()) {
                List<Material> allInDiv = materialRepository.findActiveByDivisionId(divisionId);
                candidates = allInDiv.stream()
                        .filter(m -> m.getPricingGrade() == targetGrade || (m.getPricingGrade() == null && hasPositiveRateForGrade(m, targetGrade)))
                        .toList();
            }
            candidates = candidates.stream().filter(m -> Boolean.TRUE.equals(m.getActive())).toList();

            counterpart = findExactCounterpart(mat, candidates);
            if (counterpart != null) {
                List<MaterialOption> cpOpts = materialOptionRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(counterpart.getId());
                if (cpOpts.isEmpty()) {
                    cpOpts = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(counterpart.getId());
                }
                MaterialOption bestCpOpt = null;
                if (!cpOpts.isEmpty()) {
                    if (opt != null && opt.getName() != null) {
                        bestCpOpt = cpOpts.stream()
                                .filter(o -> o.getName() != null && o.getName().trim().equalsIgnoreCase(opt.getName().trim()))
                                .findFirst()
                                .orElse(cpOpts.get(0));
                    } else {
                        bestCpOpt = cpOpts.get(0);
                    }
                }

                if (bestCpOpt != null) {
                    Pricing p = pricingRepository.findByMaterialOptionIdAndPricingMode(bestCpOpt.getId(), targetGrade).orElse(null);
                    if (p != null && Boolean.TRUE.equals(p.getActive())) {
                        BigDecimal r = resolveRateFromPricing(branchId, p);
                        if (r != null && r.compareTo(BigDecimal.ZERO) > 0) {
                            matchedRate = r;
                        }
                    }
                }

                if (matchedRate == null && counterpart.getRate() != null && counterpart.getRate().compareTo(BigDecimal.ZERO) > 0) {
                    matchedRate = money(counterpart.getRate());
                }
            }
        }

        // 5. Fallback check on mat's options even if mat had a different pricingGrade tag
        if (matchedRate == null && mat != null) {
            List<MaterialOption> opts = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(mat.getId());
            for (MaterialOption o : opts) {
                Pricing p = pricingRepository.findByMaterialOptionIdAndPricingMode(o.getId(), targetGrade).orElse(null);
                if (p != null && Boolean.TRUE.equals(p.getActive())) {
                    BigDecimal r = resolveRateFromPricing(branchId, p);
                    if (r != null && r.compareTo(BigDecimal.ZERO) > 0) {
                        matchedRate = r;
                        break;
                    }
                }
            }
        }

        String matName = mat != null ? mat.getName() : "selected material";
        if (matchedRate == null) {
            String gradeLabel = targetGrade == PricingMode.PREMIUM ? "Premium" : (targetGrade == PricingMode.ECONOMY ? "Economy" : "Middle");
            return ResolvedMaterialRate.unavailable(
                    mat != null ? mat.getId() : null,
                    matName,
                    "No material available in " + gradeLabel + " Grade"
            );
        }

        // ALWAYS preserve the original material entity and ID! Only rate is updated for the target grade.
        Material effectiveMat = mat;
        MaterialOption effectiveOpt = opt;

        String formula = (effectiveMat != null && effectiveMat.getFormula() != null && !effectiveMat.getFormula().isBlank() && !"(L + W) × H".equals(effectiveMat.getFormula()))
                ? effectiveMat.getFormula()
                : ((effectiveOpt != null && effectiveOpt.getFormula() != null && !effectiveOpt.getFormula().isBlank())
                        ? effectiveOpt.getFormula()
                        : "(L + W) × H");

        MeasurementType measurementType = resolveEffectiveMeasurementType(effectiveMat);
        String unit = (effectiveMat != null && effectiveMat.getUnit() != null && !effectiveMat.getUnit().isBlank())
                ? effectiveMat.getUnit()
                : (measurementType == MeasurementType.ROLL ? "Roll" : (measurementType == MeasurementType.QUANTITY ? "Quantity" : (measurementType == MeasurementType.FILES ? "Files" : "Sq.ft")));

        BigDecimal coverage = (effectiveMat != null && effectiveMat.getCoveragePerRoll() != null)
                ? effectiveMat.getCoveragePerRoll()
                : (counterpart != null ? counterpart.getCoveragePerRoll() : null);

        String imageUrl = effectiveMat != null ? effectiveMat.getImageUrl() : null;

        return new ResolvedMaterialRate(
                effectiveMat != null ? effectiveMat.getId() : null,
                effectiveOpt != null ? effectiveOpt.getId() : null,
                matName,
                matchedRate,
                formula,
                unit,
                measurementType,
                imageUrl,
                coverage,
                true,
                null
        );
    }

    public MeasurementType resolveEffectiveMeasurementType(Material m) {
        if (m == null) return MeasurementType.SQFT;
        if (m.getMeasurementType() != null) return m.getMeasurementType();
        if ("Quantity".equalsIgnoreCase(m.getUnit())) return MeasurementType.QUANTITY;
        if ("Files".equalsIgnoreCase(m.getUnit())) return MeasurementType.FILES;
        if ("Roll".equalsIgnoreCase(m.getUnit())) return MeasurementType.ROLL;
        return MeasurementType.SQFT;
    }

    public boolean hasPositiveRateForGrade(Material m, PricingMode grade) {
        if (m == null) return false;
        if (m.getPricingGrade() == grade && m.getRate() != null && m.getRate().compareTo(BigDecimal.ZERO) > 0) {
            return true;
        }
        List<MaterialOption> opts = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(m.getId());
        for (MaterialOption opt : opts) {
            Pricing p = pricingRepository.findByMaterialOptionIdAndPricingMode(opt.getId(), grade).orElse(null);
            if (p != null && p.getRate() != null && p.getRate().compareTo(BigDecimal.ZERO) > 0) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal resolveRateFromPricing(Long branchId, Pricing pricing) {
        if (pricing == null || !Boolean.TRUE.equals(pricing.getActive())) {
            return null;
        }
        if (branchId != null && districtPricingRepository != null) {
            DistrictPricing dp = districtPricingRepository.findByBranchIdAndPricingId(branchId, pricing.getId()).orElse(null);
            if (dp != null) {
                if (Boolean.TRUE.equals(dp.getActive()) && dp.getRate() != null && dp.getRate().compareTo(BigDecimal.ZERO) > 0) {
                    return money(dp.getRate());
                }
                return null;
            }
        }
        if (pricing.getRate() != null && pricing.getRate().compareTo(BigDecimal.ZERO) > 0) {
            return money(pricing.getRate());
        }
        return null;
    }

    private Material findExactCounterpart(Material source, List<Material> candidates) {
        if (source == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        String sourceName = source.getName() != null ? source.getName().trim() : "";
        if (sourceName.isEmpty()) {
            return null;
        }

        MeasurementType sourceType = resolveEffectiveMeasurementType(source);

        // 1. Exact name match (case-insensitive) AND matching measurement type
        for (Material c : candidates) {
            if (resolveEffectiveMeasurementType(c) == sourceType
                    && c.getName() != null && c.getName().trim().equalsIgnoreCase(sourceName)) {
                return c;
            }
        }

        // 2. Normalized name match (removes redundant grade tokens if present, and common spelling typos) AND matching measurement type
        String normSource = normalizeMaterialName(sourceName);
        if (!normSource.isEmpty()) {
            for (Material c : candidates) {
                if (resolveEffectiveMeasurementType(c) == sourceType
                        && c.getName() != null && normalizeMaterialName(c.getName()).equalsIgnoreCase(normSource)) {
                    return c;
                }
            }
        }

        return null;
    }

    private String normalizeMaterialName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("\\b(premium|middle|economy)\\b", "")
                .replace("laminmation", "lamination")
                .replace("laminates", "lamination")
                .replace("laminate", "lamination")
                .replace("plywood", "ply")
                .replaceAll("[^a-z0-9]", "");
    }

    public BigDecimal resolveRate(
            Long branchId,
            Long materialId,
            Long materialOptionId,
            PricingMode mode) {
        return resolveRate(branchId, null, materialId, materialOptionId, mode);
    }

    @Transactional(readOnly = true)
    public BigDecimal resolveRate(
            Long branchId,
            Long divisionId,
            Long materialId,
            Long materialOptionId,
            PricingMode mode) {

        ResolvedMaterialRate res = resolveMaterialAndRate(branchId, divisionId, materialId, materialOptionId, mode);
        if (!res.available() || res.rate() == null) {
            throw new IllegalArgumentException(res.message() != null ? res.message() : "No pricing configured for material under " + mode.name() + " grade.");
        }
        return res.rate();
    }

    @Transactional(readOnly = true)
    public BigDecimal resolveRate(
            Long branchId,
            Long materialOptionId,
            PricingMode mode) {
        return resolveRate(branchId, null, null, materialOptionId, mode);
    }

    // =========================================================
    // SAVE COMPLETE QUOTATION
    // =========================================================

    @Transactional
    public Quotation save(
            BuilderRequest request,
            String loggedInEmail) {

        // -----------------------------------------------------
        // Logged-in user (Admin or Employee)
        // -----------------------------------------------------

        User loggedInUser =
                userRepository.findByEmail(loggedInEmail)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Logged-in user was not found."
                                )
                        );

        if (!Boolean.TRUE.equals(loggedInUser.getActive())) {
            throw new IllegalArgumentException(
                    "User account is inactive."
            );
        }

        if (request != null && loggedInUser.getRole() == Role.EMPLOYEE && loggedInUser.getBranch() != null && (request.branchId() == null || request.branchId() == 0L)) {
            request = new BuilderRequest(
                    request.quotationId(),
                    request.customer(),
                    loggedInUser.getBranch().getId(),
                    request.quotationDate(),
                    request.validUntil(),
                    request.pricingMode(),
                    request.customerRequirements(),
                    request.notes(),
                    request.rooms(),
                    request.accessoriesAmount(),
                    request.travelCharge(),
                    request.otherCharges(),
                    request.otherChargesDescription(),
                    request.discountAmount(),
                    request.gstPercentage(),
                    request.offerPercentage(),
                    request.showSqftSize(),
                    request.lastActiveStep(),
                    request.lastActiveFloor(),
                    request.lastActiveRoom(),
                    request.lastActiveCategoryId(),
                    request.lastActiveDivisionId(),
                    request.builderState()
            );
        }

        validateRequest(request);

        // -----------------------------------------------------
        // Branch resolution & authorization
        // -----------------------------------------------------

        Branch branch;
        if (loggedInUser.getRole() == Role.EMPLOYEE) {
            if (loggedInUser.getBranch() == null) {
                throw new IllegalStateException("Employee is not assigned to a branch.");
            }
            branch = loggedInUser.getBranch();
            if (request.branchId() != null && !request.branchId().equals(branch.getId())) {
                throw new AccessDeniedException("Unauthorized: Cannot create quotations for another branch.");
            }
        } else {
            if (request.branchId() == null) {
                throw new IllegalArgumentException("Branch is required.");
            }
            branch = branchRepository.findById(request.branchId())
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found."));
        }

        // -----------------------------------------------------
        // Quotation lookup or creation
        // -----------------------------------------------------

        Quotation quotation;
        boolean isEdit = false;
        java.util.Set<Long> existingMaterialIds = new java.util.HashSet<>();
        if (request.quotationId() != null) {
            quotation = quotationRepository.findById(request.quotationId())
                    .orElseGet(Quotation::new);
            if (quotation.getId() != null) {
                isEdit = true;
                List<QuotationRoom> oldRooms =
                        quotationRoomService.getActiveByQuotationId(quotation.getId());
                for (QuotationRoom oldRoom : oldRooms) {
                    List<QuotationItem> oldItems = quotationItemService.getActiveByQuotationRoomId(oldRoom.getId());
                    if (oldItems != null) {
                        for (QuotationItem oi : oldItems) {
                            if (oi.getMaterial() != null && oi.getMaterial().getId() != null) {
                                existingMaterialIds.add(oi.getMaterial().getId());
                            }
                        }
                    }
                    quotationRoomService.deactivate(oldRoom.getId());
                }
            } else {
                quotation.setQuotationNumber(nextQuotationNumber());
            }
        } else {
            quotation = new Quotation();
            quotation.setQuotationNumber(nextQuotationNumber());
        }

        // -----------------------------------------------------
        // Customer resolution & authorization
        // -----------------------------------------------------

        Customer customer =
                resolveCustomer(
                        request.customer(),
                        branch,
                        loggedInUser,
                        quotation,
                        isEdit
                );

        quotation.setCustomer(
                customer
        );

        quotation.setEmployee(
                loggedInUser
        );

        quotation.setBranch(
                branch
        );

        LocalDate effectiveQuotationDate;
        if (quotation.getId() != null) {
            // Existing quotation being edited: preserve original quotationDate and validUntil
            if (quotation.getQuotationDate() == null) {
                quotation.setQuotationDate(
                        request.quotationDate() == null ? LocalDate.now() : request.quotationDate()
                );
            }
            effectiveQuotationDate = quotation.getQuotationDate();

            if (request.validUntil() != null) {
                if (request.validUntil().isBefore(effectiveQuotationDate)) {
                    throw new IllegalArgumentException(
                            "Valid until date cannot be earlier than quotation date."
                    );
                }
                quotation.setValidUntil(request.validUntil());
            } else if (quotation.getValidUntil() == null) {
                quotation.setValidUntil(effectiveQuotationDate.plusDays(365));
            }
        } else {
            // New quotation creation
            LocalDate qDate = request.quotationDate() == null ? LocalDate.now() : request.quotationDate();
            quotation.setQuotationDate(qDate);
            effectiveQuotationDate = qDate;

            if (request.validUntil() != null) {
                if (request.validUntil().isBefore(effectiveQuotationDate)) {
                    throw new IllegalArgumentException(
                            "Valid until date cannot be earlier than quotation date."
                    );
                }
                quotation.setValidUntil(request.validUntil());
            } else {
                quotation.setValidUntil(qDate.plusDays(365));
            }
        }

        quotation.setPricingMode(
                request.pricingMode()
        );

        // Completing and saving a quotation promotes PENDING to WORKING.
        // A previously saved WORKING quotation stays WORKING.
        // If a SENT quotation is edited and saved, preserve SENT.
        if (quotation.getStatus() == null || quotation.getStatus() == QuotationStatus.PENDING) {
            quotation.setStatus(QuotationStatus.WORKING);
        }

        quotation.setCustomerRequirements(
                clean(request.customerRequirements())
        );

        quotation.setNotes(
                clean(request.notes())
        );

        if (request.accessoriesAmount() != null) {
            quotation.setAccessoriesAmount(request.accessoriesAmount());
        }
        if (request.travelCharge() != null) {
            quotation.setTravelCharge(request.travelCharge());
        }
        if (request.otherCharges() != null) {
            quotation.setOtherCharges(request.otherCharges());
        }
        if (request.otherChargesDescription() != null) {
            quotation.setOtherChargesDescription(clean(request.otherChargesDescription()));
        }
        if (request.discountAmount() != null) {
            quotation.setDiscountAmount(request.discountAmount());
        }
        if (request.gstPercentage() != null) {
            quotation.setGstPercentage(request.gstPercentage());
        }
        if (request.offerPercentage() != null) {
            quotation.setOfferPercentage(request.offerPercentage());
        }
        if (request.showSqftSize() != null) {
            quotation.setShowSqftSize(request.showSqftSize());
        } else if (quotation.getShowSqftSize() == null) {
            quotation.setShowSqftSize(true);
        }

        quotation.setLastActiveStep(request.lastActiveStep());
        quotation.setLastActiveFloor(request.lastActiveFloor());
        quotation.setLastActiveRoom(request.lastActiveRoom());
        quotation.setLastActiveCategoryId(request.lastActiveCategoryId());
        quotation.setLastActiveDivisionId(request.lastActiveDivisionId());
        quotation.setBuilderState(request.builderState());

        Quotation savedQuotation =
                quotationRepository.save(
                        quotation
                );

        // =====================================================
        // ROOMS & ITEMS
        // =====================================================

        saveRoomsAndItems(savedQuotation, request.rooms(), branch.getId(), request.pricingMode(), existingMaterialIds, isEdit, false);

        // -----------------------------------------------------
        // Final quotation calculation
        // -----------------------------------------------------

        return quotationCalculationService
                .calculateQuotation(
                        savedQuotation.getId()
                );
    }



    // =========================================================
    // PROGRESSIVE AUTO-SAVE (DRAFT QUOTATIONS)
    // =========================================================

    @Transactional
    public Quotation autoSave(
            BuilderRequest request,
            String loggedInEmail) {

        if (request == null) {
            throw new IllegalArgumentException("Draft quotation request cannot be null.");
        }

        User loggedInUser = userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new IllegalArgumentException("Logged-in user was not found."));

        if (!Boolean.TRUE.equals(loggedInUser.getActive())) {
            throw new IllegalArgumentException("User account is inactive.");
        }

        Branch branch = null;
        if (loggedInUser.getRole() == Role.EMPLOYEE) {
            if (loggedInUser.getBranch() == null) {
                throw new IllegalStateException("Employee is not assigned to a branch.");
            }
            branch = loggedInUser.getBranch();
            if (request.branchId() != null && !request.branchId().equals(branch.getId())) {
                throw new AccessDeniedException("Unauthorized: Cannot create quotations for another branch.");
            }
        } else {
            if (request.branchId() != null) {
                branch = branchRepository.findById(request.branchId()).orElse(null);
            }
            if (branch == null && branchRepository != null) {
                branch = branchRepository.findAll().stream().filter(b -> Boolean.TRUE.equals(b.getActive())).findFirst().orElse(null);
            }
        }

        Quotation quotation;
        boolean isEdit = false;
        java.util.Set<Long> existingMaterialIds = new java.util.HashSet<>();
        if (request.quotationId() != null) {
            quotation = quotationRepository.findById(request.quotationId()).orElseGet(Quotation::new);
            if (quotation.getId() != null) {
                isEdit = true;
                if (loggedInUser.getRole() == Role.EMPLOYEE) {
                    if (quotation.getBranch() != null && loggedInUser.getBranch() != null
                            && !quotation.getBranch().getId().equals(loggedInUser.getBranch().getId())) {
                        throw new AccessDeniedException("Unauthorized: Cannot edit quotations for another branch.");
                    }
                }
                List<QuotationRoom> oldRooms = quotationRoomService.getActiveByQuotationId(quotation.getId());
                for (QuotationRoom oldRoom : oldRooms) {
                    List<QuotationItem> oldItems = quotationItemService.getActiveByQuotationRoomId(oldRoom.getId());
                    if (oldItems != null) {
                        for (QuotationItem oi : oldItems) {
                            if (oi.getMaterial() != null && oi.getMaterial().getId() != null) {
                                existingMaterialIds.add(oi.getMaterial().getId());
                            }
                        }
                    }
                    quotationRoomService.deactivate(oldRoom.getId());
                }
            } else {
                quotation.setQuotationNumber(nextQuotationNumber());
            }
        } else {
            quotation = new Quotation();
            quotation.setQuotationNumber(nextQuotationNumber());
        }

        // Customer resolution (permissive for drafts)
        Customer customer = null;
        if (request.customer() != null) {
            try {
                if (request.customer().id() != null) {
                    customer = customerRepository.findById(request.customer().id()).orElse(null);
                } else if (request.customer().name() != null && !request.customer().name().isBlank()
                        && request.customer().phone() != null && !request.customer().phone().isBlank()) {
                    customer = resolveCustomer(request.customer(), branch, loggedInUser, quotation, isEdit);
                }
            } catch (Exception ignored) {
                // Draft errors during customer resolution should not block progressive saving
            }
        }
        if (customer == null && quotation.getCustomer() != null) {
            customer = quotation.getCustomer();
        }
        quotation.setCustomer(customer);

        if (quotation.getEmployee() == null) {
            quotation.setEmployee(loggedInUser);
        }
        if (branch != null) {
            quotation.setBranch(branch);
        }

        LocalDate effectiveDate = request.quotationDate() != null
                ? request.quotationDate()
                : (quotation.getQuotationDate() != null ? quotation.getQuotationDate() : LocalDate.now());
        quotation.setQuotationDate(effectiveDate);

        if (request.validUntil() != null) {
            quotation.setValidUntil(request.validUntil());
        } else if (quotation.getValidUntil() == null) {
            quotation.setValidUntil(effectiveDate.plusDays(365));
        }

        quotation.setPricingMode(request.pricingMode() != null
                ? request.pricingMode()
                : (quotation.getPricingMode() != null ? quotation.getPricingMode() : PricingMode.MIDDLE));

        // Incomplete / auto-saved quotations MUST remain in PENDING status
        if (quotation.getStatus() == null || quotation.getStatus() == QuotationStatus.PENDING) {
            quotation.setStatus(QuotationStatus.PENDING);
        }

        quotation.setCustomerRequirements(clean(request.customerRequirements()));
        quotation.setNotes(clean(request.notes()));
        if (request.accessoriesAmount() != null) quotation.setAccessoriesAmount(request.accessoriesAmount());
        if (request.travelCharge() != null) quotation.setTravelCharge(request.travelCharge());
        if (request.otherCharges() != null) quotation.setOtherCharges(request.otherCharges());
        if (request.otherChargesDescription() != null) quotation.setOtherChargesDescription(clean(request.otherChargesDescription()));
        if (request.discountAmount() != null) quotation.setDiscountAmount(request.discountAmount());
        if (request.gstPercentage() != null) quotation.setGstPercentage(request.gstPercentage());
        if (request.offerPercentage() != null) quotation.setOfferPercentage(request.offerPercentage());
        if (request.showSqftSize() != null) quotation.setShowSqftSize(request.showSqftSize());

        // Resume navigation fields
        quotation.setLastActiveStep(request.lastActiveStep());
        quotation.setLastActiveFloor(request.lastActiveFloor());
        quotation.setLastActiveRoom(request.lastActiveRoom());
        quotation.setLastActiveCategoryId(request.lastActiveCategoryId());
        quotation.setLastActiveDivisionId(request.lastActiveDivisionId());
        quotation.setBuilderState(request.builderState());

        Quotation savedQuotation = quotationRepository.save(quotation);

        if (request.rooms() != null && !request.rooms().isEmpty()) {
            saveRoomsAndItems(savedQuotation, request.rooms(), branch != null ? branch.getId() : null, savedQuotation.getPricingMode(), existingMaterialIds, isEdit, true);
        }

        Quotation calculated = quotationCalculationService.calculateQuotation(savedQuotation.getId());
        if (savedQuotation.getStatus() == QuotationStatus.PENDING && calculated.getStatus() != QuotationStatus.PENDING) {
            calculated.setStatus(QuotationStatus.PENDING);
            calculated = quotationRepository.save(calculated);
        }
        return calculated;
    }

    private void saveRoomsAndItems(
            Quotation savedQuotation,
            List<RoomRequest> rooms,
            Long branchId,
            PricingMode pricingMode,
            java.util.Set<Long> existingMaterialIds,
            boolean isEdit,
            boolean isAutoSave) {

        if (rooms == null || rooms.isEmpty()) {
            return;
        }

        for (RoomRequest roomRequest : rooms) {
            if (roomRequest == null) continue;
            String floor = roomRequest.floor() != null ? roomRequest.floor().trim() : "";
            String roomName = roomRequest.room() != null ? roomRequest.room().trim() : "";
            if (floor.isEmpty() || roomName.isEmpty()) {
                if (isAutoSave) {
                    if (floor.isEmpty()) floor = "Ground Floor";
                    if (roomName.isEmpty()) roomName = "Room 1";
                } else {
                    floor = required(roomRequest.floor(), "Floor");
                    roomName = required(roomRequest.room(), "Room");
                }
            }

            QuotationRoom room = quotationRoomService.create(
                    savedQuotation,
                    floor,
                    roomName,
                    roomRequest.workDescription()
            );

            if (roomRequest.items() == null || roomRequest.items().isEmpty()) {
                continue;
            }

            for (ItemRequest itemRequest : roomRequest.items()) {
                if (itemRequest == null) continue;
                if (itemRequest.workCategoryId() == null || itemRequest.divisionId() == null || itemRequest.materialId() == null) {
                    if (isAutoSave) continue;
                }

                WorkCategory category = workCategoryRepository.findById(itemRequest.workCategoryId()).orElse(null);
                if (category == null) {
                    if (isAutoSave) continue;
                    throw new IllegalArgumentException("Work category not found.");
                }

                Division division = divisionRepository.findById(itemRequest.divisionId()).orElse(null);
                if (division == null) {
                    if (isAutoSave) continue;
                    throw new IllegalArgumentException("Division not found.");
                }

                Material material = materialRepository.findById(itemRequest.materialId()).orElse(null);
                if (material == null) {
                    if (isAutoSave) continue;
                    throw new IllegalArgumentException("Material not found.");
                }

                if (!Boolean.TRUE.equals(material.getActive())) {
                    boolean isExistingItemInQuotation = isEdit && existingMaterialIds.contains(material.getId());
                    if (!isExistingItemInQuotation) {
                        if (isAutoSave) continue;
                        throw new IllegalArgumentException("Material '" + material.getName() + "' is inactive and cannot be added to quotations.");
                    }
                }

                Item item = null;
                if (itemRequest.itemId() != null) {
                    item = itemRepository.findById(itemRequest.itemId()).orElse(null);
                }
                if (item == null && material.getItem() != null) {
                    item = material.getItem();
                }
                if (item == null) {
                    item = itemRepository.findByDivisionIdAndActiveTrueOrderByDisplayOrderAsc(division.getId())
                            .stream().findFirst().orElse(null);
                    if (item == null) {
                        Item defaultItem = new Item();
                        defaultItem.setName(division.getName());
                        defaultItem.setDivision(division);
                        defaultItem.setActive(true);
                        item = itemRepository.save(defaultItem);
                    }
                }

                MaterialOption option = null;
                if (itemRequest.materialOptionId() != null) {
                    option = materialOptionRepository.findById(itemRequest.materialOptionId()).orElse(null);
                }
                if (option == null) {
                    List<MaterialOption> existingOpts = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(material.getId());
                    if (!existingOpts.isEmpty()) {
                        option = existingOpts.get(0);
                    } else {
                        MaterialOption opt = new MaterialOption();
                        opt.setName(material.getName());
                        opt.setMaterial(material);
                        opt.setDescription(material.getDescription());
                        opt.setActive(true);
                        option = materialOptionRepository.save(opt);
                    }
                }

                if (option != null && option.getMaterial() != null && !option.getMaterial().getId().equals(material.getId())) {
                    if (!isAutoSave) {
                        throw new IllegalArgumentException("Material option does not belong to the selected material.");
                    }
                }

                if (division.getWorkCategory() != null && !division.getWorkCategory().getId().equals(category.getId())) {
                    if (!isAutoSave) {
                        throw new IllegalArgumentException("Division does not belong to the selected work category.");
                    }
                }

                MaterialSpecification materialSpecification = null;
                if (itemRequest.materialSpecificationId() != null) {
                    materialSpecification = materialSpecificationRepository.findById(itemRequest.materialSpecificationId()).orElse(null);
                    if (materialSpecification != null) {
                        if (materialSpecification.getMaterial() != null && !materialSpecification.getMaterial().getId().equals(material.getId())) {
                            materialSpecification = null;
                        }
                        if (materialSpecification != null && materialSpecification.getMaterialOption() != null
                                && option != null && !materialSpecification.getMaterialOption().getId().equals(option.getId())) {
                            materialSpecification = null;
                        }
                    }
                }

                MeasurementType effectiveType = itemRequest.measurementType();
                if (effectiveType == null && material != null) {
                    effectiveType = material.getMeasurementType();
                }
                if (effectiveType == null && material != null && "Files".equalsIgnoreCase(material.getUnit())) {
                    effectiveType = MeasurementType.FILES;
                }
                if (effectiveType == null) {
                    effectiveType = MeasurementType.SQFT;
                }

                ResolvedMaterialRate resolved = resolveMaterialAndRate(
                        branchId,
                        division.getId(),
                        material.getId(),
                        option != null ? option.getId() : null,
                        pricingMode
                );
                boolean isExistingItemInQuotation = isEdit && existingMaterialIds.contains(material.getId());

                if (!resolved.available() || resolved.rate() == null || resolved.rate().compareTo(BigDecimal.ZERO) <= 0) {
                    if (isExistingItemInQuotation || isAutoSave) {
                        BigDecimal fallbackRate = (itemRequest.rate() != null && itemRequest.rate().compareTo(BigDecimal.ZERO) > 0)
                                ? money(itemRequest.rate())
                                : (material.getRate() != null && material.getRate().compareTo(BigDecimal.ZERO) > 0 ? money(material.getRate()) : BigDecimal.ONE);
                        resolved = new ResolvedMaterialRate(
                                material.getId(),
                                option != null ? option.getId() : null,
                                material.getName(),
                                fallbackRate,
                                material.getFormula(),
                                material.getUnit(),
                                effectiveType,
                                material.getImageUrl(),
                                material.getCoveragePerRoll(),
                                true,
                                null
                        );
                    } else {
                        String gradeLabel = pricingMode == PricingMode.PREMIUM ? "Premium" : (pricingMode == PricingMode.ECONOMY ? "Economy" : "Middle");
                        throw new IllegalArgumentException(resolved.message() != null ? resolved.message() : ("No material available in " + gradeLabel + " Grade"));
                    }
                }

                BigDecimal rate;
                if (effectiveType.isFiles()) {
                    if (itemRequest.rate() != null) {
                        if (itemRequest.rate().compareTo(BigDecimal.ZERO) < 0 && !isAutoSave) {
                            throw new IllegalArgumentException("Rate for Files unit must be a valid, non-negative value.");
                        }
                        rate = money(itemRequest.rate().max(BigDecimal.ZERO));
                    } else {
                        rate = resolved.rate();
                    }
                } else {
                    rate = resolved.rate();
                }

                Pricing itemPricing = (option != null)
                        ? pricingRepository.findByMaterialOptionIdAndPricingMode(option.getId(), pricingMode).orElse(null)
                        : null;

                String itemFormula = (material.getFormula() != null && !material.getFormula().isBlank())
                        ? material.getFormula()
                        : ((itemPricing != null && itemPricing.getFormula() != null && !itemPricing.getFormula().isBlank())
                                ? itemPricing.getFormula()
                                : (option != null && option.getFormula() != null && !option.getFormula().isBlank() ? option.getFormula() : "(L + W) × H"));

                BigDecimal totalSqft = BigDecimal.ZERO;
                BigDecimal totalAmount = BigDecimal.ZERO;
                BigDecimal totalOfferPrice = BigDecimal.ZERO;
                BigDecimal firstLength = null;
                BigDecimal firstWidth = null;
                BigDecimal firstHeight = null;
                BigDecimal effectiveQuantity = (itemRequest.quantity() != null && itemRequest.quantity().compareTo(BigDecimal.ZERO) > 0)
                        ? itemRequest.quantity()
                        : BigDecimal.ONE;

                String effectiveImageUrl = (itemRequest.imageUrl() != null && !itemRequest.imageUrl().isBlank())
                        ? itemRequest.imageUrl()
                        : (material != null ? material.getImageUrl() : null);

                BigDecimal coverage = null;
                if (effectiveType.isSqft()) {
                    if (itemRequest.sizes() != null) {
                        for (int sizeIndex = 0; sizeIndex < itemRequest.sizes().size(); sizeIndex++) {
                            SizeRequest size = itemRequest.sizes().get(sizeIndex);
                            if (size == null) continue;
                            String normFormula = com.example.pravin_quotation.util.CalculationFormula.normalizeFormula(itemFormula);
                            boolean requiresW = !normFormula.equals(com.example.pravin_quotation.util.CalculationFormula.FORMULA_L_X_H);
                            boolean requiresH = !normFormula.equals(com.example.pravin_quotation.util.CalculationFormula.FORMULA_L_X_W);

                            BigDecimal length = isAutoSave ? (size.length() != null ? size.length() : BigDecimal.ZERO) : positive(size.length(), "Length");
                            BigDecimal width = requiresW ? (isAutoSave ? (size.width() != null ? size.width() : BigDecimal.ZERO) : positive(size.width(), "Width")) : (size.width() != null ? size.width() : BigDecimal.ZERO);
                            BigDecimal height = requiresH ? (isAutoSave ? (size.height() != null ? size.height() : BigDecimal.ZERO) : positive(size.height(), "Height")) : (size.height() != null ? size.height() : BigDecimal.ZERO);
                            if (!isAutoSave) {
                                com.example.pravin_quotation.util.CalculationFormula.validateDimensions(normFormula, length, width, height);
                            }

                            if (sizeIndex == 0) {
                                firstLength = length;
                                firstWidth = width;
                                firstHeight = height;
                            }

                            BigDecimal sqft = quotationItemSizeService.calculateSqft(itemFormula, length, width, height, "in");
                            BigDecimal amount = money(sqft.multiply(rate));
                            BigDecimal offerPrice;
                            if (size.offerPrice() == null) {
                                offerPrice = amount;
                            } else {
                                offerPrice = isAutoSave ? size.offerPrice().max(BigDecimal.ZERO) : nonNegative(size.offerPrice(), "Offer price");
                            }

                            totalSqft = totalSqft.add(sqft);
                            totalAmount = totalAmount.add(amount);
                            totalOfferPrice = totalOfferPrice.add(offerPrice);
                        }
                    }
                } else if (effectiveType.isRoll()) {
                    coverage = (material != null && material.getCoveragePerRoll() != null && material.getCoveragePerRoll().compareTo(BigDecimal.ZERO) > 0)
                            ? material.getCoveragePerRoll()
                            : (itemRequest.coveragePerRoll() != null && itemRequest.coveragePerRoll().compareTo(BigDecimal.ZERO) > 0
                                    ? itemRequest.coveragePerRoll()
                                    : null);
                    if (!isAutoSave && (coverage == null || coverage.compareTo(BigDecimal.ZERO) <= 0)) {
                        throw new IllegalArgumentException("Coverage Per Roll is required and must be greater than zero for Roll material.");
                    }

                    if (itemRequest.sizes() != null) {
                        for (int sizeIndex = 0; sizeIndex < itemRequest.sizes().size(); sizeIndex++) {
                            SizeRequest size = itemRequest.sizes().get(sizeIndex);
                            if (size == null) continue;
                            String normFormula = com.example.pravin_quotation.util.CalculationFormula.normalizeFormula(itemFormula);
                            boolean requiresW = !normFormula.equals(com.example.pravin_quotation.util.CalculationFormula.FORMULA_L_X_H);
                            boolean requiresH = !normFormula.equals(com.example.pravin_quotation.util.CalculationFormula.FORMULA_L_X_W);

                            BigDecimal length = isAutoSave ? (size.length() != null ? size.length() : BigDecimal.ZERO) : positive(size.length(), "Length");
                            BigDecimal width = requiresW ? (isAutoSave ? (size.width() != null ? size.width() : BigDecimal.ZERO) : positive(size.width(), "Width")) : (size.width() != null ? size.width() : BigDecimal.ZERO);
                            BigDecimal height = requiresH ? (isAutoSave ? (size.height() != null ? size.height() : BigDecimal.ZERO) : positive(size.height(), "Height")) : (size.height() != null ? size.height() : BigDecimal.ZERO);
                            if (!isAutoSave) {
                                com.example.pravin_quotation.util.CalculationFormula.validateDimensions(normFormula, length, width, height);
                            }

                            if (sizeIndex == 0) {
                                firstLength = length;
                                firstWidth = width;
                                firstHeight = height;
                            }

                            BigDecimal sqft = quotationItemSizeService.calculateSqft(itemFormula, length, width, height, "in");
                            totalSqft = totalSqft.add(sqft);
                        }
                    }

                    BigDecimal requiredRolls = (totalSqft.compareTo(BigDecimal.ZERO) > 0 && coverage != null && coverage.compareTo(BigDecimal.ZERO) > 0)
                            ? totalSqft.divide(coverage, 0, RoundingMode.CEILING)
                            : BigDecimal.ZERO;
                    effectiveQuantity = requiredRolls;

                    totalAmount = money(requiredRolls.multiply(rate));
                    BigDecimal offerPrice = totalAmount;
                    if (itemRequest.sizes() != null && !itemRequest.sizes().isEmpty() && itemRequest.sizes().get(0).offerPrice() != null) {
                        offerPrice = isAutoSave ? itemRequest.sizes().get(0).offerPrice().max(BigDecimal.ZERO) : nonNegative(itemRequest.sizes().get(0).offerPrice(), "Offer price");
                    }
                    totalOfferPrice = offerPrice;
                } else {
                    totalAmount = money(effectiveQuantity.multiply(rate));
                    BigDecimal offerPrice = totalAmount;
                    if (itemRequest.sizes() != null && !itemRequest.sizes().isEmpty() && itemRequest.sizes().get(0).offerPrice() != null) {
                        offerPrice = isAutoSave ? itemRequest.sizes().get(0).offerPrice().max(BigDecimal.ZERO) : nonNegative(itemRequest.sizes().get(0).offerPrice(), "Offer price");
                    }
                    totalOfferPrice = offerPrice;
                    totalSqft = BigDecimal.ZERO;
                }

                totalSqft = money(totalSqft);
                totalAmount = money(totalAmount);
                totalOfferPrice = money(totalOfferPrice);

                String specText = itemRequest.specification();
                if ((specText == null || specText.isBlank()) && materialSpecification != null) {
                    specText = materialSpecification.getName();
                }

                QuotationItem quotationItem = quotationItemService.create(
                        room,
                        category,
                        division,
                        item,
                        material,
                        option,
                        materialSpecification,
                        itemRequest.itemDescription(),
                        firstLength,
                        firstWidth,
                        firstHeight,
                        totalSqft,
                        rate,
                        totalAmount,
                        totalOfferPrice,
                        (effectiveType.isSqft() || effectiveType.isRoll()) ? itemFormula : null,
                        specText,
                        effectiveType,
                        effectiveQuantity,
                        effectiveImageUrl,
                        coverage
                );

                if ((effectiveType.isSqft() || effectiveType.isRoll()) && itemRequest.sizes() != null) {
                    for (int sizeIndex = 0; sizeIndex < itemRequest.sizes().size(); sizeIndex++) {
                        SizeRequest size = itemRequest.sizes().get(sizeIndex);
                        if (size == null) continue;
                        String normFormula = com.example.pravin_quotation.util.CalculationFormula.normalizeFormula(itemFormula);
                        boolean requiresW = !normFormula.equals(com.example.pravin_quotation.util.CalculationFormula.FORMULA_L_X_H);
                        boolean requiresH = !normFormula.equals(com.example.pravin_quotation.util.CalculationFormula.FORMULA_L_X_W);

                        BigDecimal length = isAutoSave ? (size.length() != null ? size.length() : BigDecimal.ZERO) : positive(size.length(), "Length");
                        BigDecimal width = requiresW ? (isAutoSave ? (size.width() != null ? size.width() : BigDecimal.ZERO) : positive(size.width(), "Width")) : (size.width() != null ? size.width() : BigDecimal.ZERO);
                        BigDecimal height = requiresH ? (isAutoSave ? (size.height() != null ? size.height() : BigDecimal.ZERO) : positive(size.height(), "Height")) : (size.height() != null ? size.height() : BigDecimal.ZERO);

                        BigDecimal sqft = quotationItemSizeService.calculateSqft(itemFormula, length, width, height, "in");
                        BigDecimal sizeRolls = (effectiveType.isRoll() && coverage != null && coverage.compareTo(BigDecimal.ZERO) > 0 && sqft.compareTo(BigDecimal.ZERO) > 0)
                                ? sqft.divide(coverage, 0, RoundingMode.CEILING)
                                : BigDecimal.ZERO;
                        BigDecimal amount = effectiveType.isRoll() ? money(sizeRolls.multiply(rate)) : money(sqft.multiply(rate));
                        BigDecimal offerPrice;
                        if (size.offerPrice() == null) {
                            offerPrice = amount;
                        } else {
                            offerPrice = isAutoSave ? size.offerPrice().max(BigDecimal.ZERO) : nonNegative(size.offerPrice(), "Offer price");
                        }

                        QuotationItemSize createdSize = quotationItemSizeService.create(
                                quotationItem,
                                length,
                                width,
                                height,
                                offerPrice,
                                sizeIndex + 1,
                                "in"
                        );
                        if (quotationItem.getSizes() != null) {
                            quotationItem.getSizes().add(createdSize);
                        }
                    }
                }
            }
        }
    }
    // =========================================================
    // CUSTOMER RESOLUTION & AUTHORIZATION
    // =========================================================

    private Customer resolveCustomer(
            CustomerRequest request,
            Branch branch,
            User loggedInUser) {
        return resolveCustomer(request, branch, loggedInUser, null, false);
    }

    private Customer resolveCustomer(
            CustomerRequest request,
            Branch branch,
            User loggedInUser,
            Quotation quotation,
            boolean isEdit) {

        if (request == null) {
            throw new IllegalArgumentException("Customer details are required.");
        }

        // Case 1: Editing customer details in Edit Builder
        if (isEdit) {
            Customer existingCustomer = null;
            if (request.id() != null) {
                existingCustomer = customerRepository.findById(request.id())
                        .orElse(null);
            }
            if (existingCustomer == null && quotation != null && quotation.getCustomer() != null) {
                existingCustomer = quotation.getCustomer();
            }

            if (existingCustomer != null) {
                // Enforce branch security for employees
                if (loggedInUser.getRole() == Role.EMPLOYEE) {
                    if (existingCustomer.getBranch() == null
                            || !existingCustomer.getBranch().getId().equals(loggedInUser.getBranch().getId())) {
                        throw new AccessDeniedException("Unauthorized: Customer does not belong to your assigned branch.");
                    }
                }

                String name = TitleCaseUtils.toTitleCase(required(request.name(), "Customer name").trim());
                String phone = required(request.phone(), "Phone number").trim();
                String email = clean(request.email());
                String address = TitleCaseUtils.toTitleCase(clean(request.address()));

                // Check duplicate phone across other customers
                java.util.Optional<Customer> existingByPhone = customerRepository.findByPhone(phone);
                if (existingByPhone.isPresent() && !existingByPhone.get().getId().equals(existingCustomer.getId())) {
                    throw new IllegalArgumentException("Phone number already exists: A customer with phone number " + phone + " is already registered.");
                }

                // Check duplicate email across other customers if provided
                if (email != null && !email.isBlank()) {
                    String lowerEmail = email.toLowerCase().trim();
                    java.util.Optional<Customer> existingByEmail = customerRepository.findByEmail(lowerEmail);
                    if (existingByEmail.isPresent() && !existingByEmail.get().getId().equals(existingCustomer.getId())) {
                        throw new IllegalArgumentException("Email address already exists: A customer with email " + lowerEmail + " is already registered.");
                    }
                }

                existingCustomer.setName(name);
                existingCustomer.setPhone(phone);
                existingCustomer.setEmail(email != null && !email.isBlank() ? email.toLowerCase().trim() : null);
                existingCustomer.setAddress(address);
                if (existingCustomer.getBranch() == null) {
                    existingCustomer.setBranch(branch);
                }
                existingCustomer.setActive(true);

                return customerRepository.save(existingCustomer);
            }
        }

        // Case 2: Existing Customer by ID for new quotation (reusing profile without mutating)
        if (request.id() != null) {
            Customer existingCustomer = customerRepository.findById(request.id())
                    .orElseThrow(() -> new IllegalArgumentException("Selected customer not found."));

            // Enforce branch security for employees
            if (loggedInUser.getRole() == Role.EMPLOYEE) {
                if (existingCustomer.getBranch() == null
                        || !existingCustomer.getBranch().getId().equals(loggedInUser.getBranch().getId())) {
                    throw new AccessDeniedException("Unauthorized: Customer does not belong to your assigned branch.");
                }
            }
            return existingCustomer;
        }

        // Case 3: New Customer creation
        String name = TitleCaseUtils.toTitleCase(required(request.name(), "Customer name").trim());
        String phone = required(request.phone(), "Phone number").trim();
        String email = clean(request.email());
        String address = TitleCaseUtils.toTitleCase(clean(request.address()));

        // Check if phone already exists
        java.util.Optional<Customer> existingByPhone = customerRepository.findByPhone(phone);
        if (existingByPhone.isPresent()) {
            Customer existing = existingByPhone.get();
            if (loggedInUser.getRole() == Role.EMPLOYEE) {
                if (existing.getBranch() != null && !existing.getBranch().getId().equals(loggedInUser.getBranch().getId())) {
                    throw new IllegalArgumentException("A customer with this phone number already belongs to another branch.");
                }
            }
            // Existing customer found in this branch - reuse to avoid duplicate!
            return existing;
        }

        // Email duplicate check if provided
        if (email != null && !email.isBlank()) {
            String lowerEmail = email.toLowerCase();
            java.util.Optional<Customer> existingByEmail = customerRepository.findByEmail(lowerEmail);
            if (existingByEmail.isPresent()) {
                Customer existing = existingByEmail.get();
                if (loggedInUser.getRole() == Role.EMPLOYEE) {
                    if (existing.getBranch() != null && !existing.getBranch().getId().equals(loggedInUser.getBranch().getId())) {
                        throw new IllegalArgumentException("A customer with this email already belongs to another branch.");
                    }
                }
                return existing;
            }
        }

        Customer newCustomer = new Customer();
        newCustomer.setName(name);
        newCustomer.setPhone(phone);
        newCustomer.setEmail(email != null && !email.isBlank() ? email.toLowerCase() : null);
        newCustomer.setAddress(address);
        newCustomer.setBranch(branch);
        newCustomer.setActive(true);

        return customerRepository.save(newCustomer);
    }

    // =========================================================
    // QUOTATION NUMBER
    // =========================================================

    private String nextQuotationNumber() {

        long next =
                quotationRepository.count() + 1;

        String number;

        do {

            number =
                    String.format(
                            "QT-%05d",
                            next++
                    );

        } while (
                quotationRepository
                        .existsByQuotationNumber(number)
        );

        return number;
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    private void validateRequest(
            BuilderRequest request) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Quotation data is required."
            );
        }

        if (request.branchId() == null) {

            throw new IllegalArgumentException(
                    "Branch is required."
            );
        }

        if (request.pricingMode() == null) {

            throw new IllegalArgumentException(
                    "Pricing mode is required."
            );
        }

        if (request.customer() == null) {

            throw new IllegalArgumentException(
                    "Customer details are required."
            );
        }

        if (request.customer().id() == null) {
            required(request.customer().name(), "Customer name");
            required(request.customer().phone(), "Phone number");
        }

        if (request.validUntil() != null && request.quotationDate() != null) {
            if (request.validUntil().isBefore(request.quotationDate())) {
                throw new IllegalArgumentException(
                        "Valid until date cannot be earlier than quotation date."
                );
            }
        }

        if (request.otherCharges() != null) {
            nonNegative(request.otherCharges(), "Other charges");
            if (request.otherCharges().compareTo(BigDecimal.ZERO) > 0) {
                if (request.otherChargesDescription() == null || request.otherChargesDescription().trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Other charges description is required when other charges amount is entered."
                    );
                }
            }
        }

        if (request.rooms() == null
                || request.rooms().isEmpty()) {

            throw new IllegalArgumentException(
                    "Add at least one room."
            );
        }

        for (RoomRequest room :
                request.rooms()) {

            if (room.items() == null
                    || room.items().isEmpty()) {

                throw new IllegalArgumentException(
                        "Each room must contain at least one item."
                );
            }

            for (ItemRequest item : room.items()) {
                MeasurementType type = item.measurementType();
                if (type == null && item.materialId() != null) {
                    Material mat = materialRepository.findById(item.materialId()).orElse(null);
                    if (mat != null) {
                        type = mat.getMeasurementType();
                    }
                }
                if (type == null) {
                    type = MeasurementType.SQFT;
                }

                if (type.isSqft() || type.isRoll()) {
                    if (item.sizes() == null || item.sizes().isEmpty()) {
                        throw new IllegalArgumentException(
                                "Each " + (type.isRoll() ? "Roll" : "Sq.ft") + " item must contain at least one size."
                        );
                    }
                } else {
                    if (item.quantity() != null && item.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException(
                                "Quantity must be greater than zero."
                        );
                    }
                    if (type.isFiles() && item.rate() != null) {
                        if (item.rate().compareTo(BigDecimal.ZERO) < 0) {
                            throw new IllegalArgumentException(
                                    "Rate for Files unit must be a valid, non-negative value."
                            );
                        }
                    }
                }
            }
        }
    }

    // =========================================================
    // POSITIVE VALUE
    // =========================================================

    private BigDecimal positive(
            BigDecimal value,
            String name) {

        if (value == null
                || value.compareTo(
                BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    name + " must be greater than zero."
            );
        }

        return value;
    }

    // =========================================================
    // NON NEGATIVE
    // =========================================================

    private BigDecimal nonNegative(
            BigDecimal value,
            String name) {

        if (value != null
                && value.compareTo(
                BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    name + " cannot be negative."
            );
        }

        return value == null
                ? BigDecimal.ZERO
                : money(value);
    }

    // =========================================================
    // MONEY
    // =========================================================

    private BigDecimal money(
            BigDecimal value) {

        return (
                value == null
                        ? BigDecimal.ZERO
                        : value
        ).setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    // =========================================================
    // REQUIRED STRING
    // =========================================================

    private String required(
            String value,
            String name) {

        String cleaned =
                clean(value);

        if (cleaned == null) {

            throw new IllegalArgumentException(
                    name + " is required."
            );
        }

        return cleaned;
    }

    // =========================================================
    // CLEAN STRING
    // =========================================================

    private String clean(
            String value) {

        if (value == null) {
            return null;
        }

        String cleaned =
                value.trim();

        return cleaned.isEmpty()
                ? null
                : cleaned;
    }

    // =========================================================
    // REQUEST RECORDS
    // =========================================================

    public record BuilderRequest(
            Long quotationId,
            CustomerRequest customer,
            Long branchId,
            LocalDate quotationDate,
            LocalDate validUntil,
            PricingMode pricingMode,
            String customerRequirements,
            String notes,
            List<RoomRequest> rooms,
            BigDecimal accessoriesAmount,
            BigDecimal travelCharge,
            BigDecimal otherCharges,
            String otherChargesDescription,
            BigDecimal discountAmount,
            BigDecimal gstPercentage,
            BigDecimal offerPercentage,
            Boolean showSqftSize,
            String lastActiveStep,
            String lastActiveFloor,
            String lastActiveRoom,
            Long lastActiveCategoryId,
            Long lastActiveDivisionId,
            String builderState) {

        public BuilderRequest(
                Long quotationId,
                CustomerRequest customer,
                Long branchId,
                LocalDate quotationDate,
                LocalDate validUntil,
                PricingMode pricingMode,
                String customerRequirements,
                String notes,
                List<RoomRequest> rooms,
                BigDecimal accessoriesAmount,
                BigDecimal travelCharge,
                BigDecimal otherCharges,
                String otherChargesDescription,
                BigDecimal discountAmount,
                BigDecimal gstPercentage,
                BigDecimal offerPercentage,
                Boolean showSqftSize) {
            this(quotationId, customer, branchId, quotationDate, validUntil, pricingMode, customerRequirements, notes, rooms,
                    accessoriesAmount, travelCharge, otherCharges, otherChargesDescription,
                    discountAmount, gstPercentage, offerPercentage, showSqftSize,
                    null, null, null, null, null, null);
        }

        public BuilderRequest(
                Long quotationId,
                CustomerRequest customer,
                Long branchId,
                LocalDate quotationDate,
                LocalDate validUntil,
                PricingMode pricingMode,
                String customerRequirements,
                String notes,
                List<RoomRequest> rooms,
                BigDecimal accessoriesAmount,
                BigDecimal travelCharge,
                BigDecimal otherCharges,
                BigDecimal discountAmount,
                BigDecimal gstPercentage,
                BigDecimal offerPercentage,
                Boolean showSqftSize) {
            this(quotationId, customer, branchId, quotationDate, validUntil, pricingMode, customerRequirements, notes, rooms,
                    accessoriesAmount, travelCharge, otherCharges,
                    (otherCharges != null && otherCharges.compareTo(BigDecimal.ZERO) > 0) ? "Other Charges" : null,
                    discountAmount, gstPercentage, offerPercentage, showSqftSize,
                    null, null, null, null, null, null);
        }

        public BuilderRequest(
                Long quotationId,
                CustomerRequest customer,
                Long branchId,
                LocalDate quotationDate,
                PricingMode pricingMode,
                String customerRequirements,
                String notes,
                List<RoomRequest> rooms,
                BigDecimal accessoriesAmount,
                BigDecimal travelCharge,
                BigDecimal otherCharges,
                BigDecimal discountAmount,
                BigDecimal gstPercentage,
                BigDecimal offerPercentage,
                Boolean showSqftSize) {
            this(quotationId, customer, branchId, quotationDate, null, pricingMode, customerRequirements, notes, rooms,
                    accessoriesAmount, travelCharge, otherCharges,
                    (otherCharges != null && otherCharges.compareTo(BigDecimal.ZERO) > 0) ? "Other Charges" : null,
                    discountAmount, gstPercentage, offerPercentage, showSqftSize,
                    null, null, null, null, null, null);
        }

        public BuilderRequest(
                Long quotationId,
                CustomerRequest customer,
                Long branchId,
                LocalDate quotationDate,
                PricingMode pricingMode,
                String customerRequirements,
                String notes,
                List<RoomRequest> rooms,
                BigDecimal accessoriesAmount,
                BigDecimal travelCharge,
                BigDecimal otherCharges,
                BigDecimal discountAmount,
                BigDecimal gstPercentage,
                BigDecimal offerPercentage) {
            this(quotationId, customer, branchId, quotationDate, null, pricingMode, customerRequirements, notes, rooms,
                    accessoriesAmount, travelCharge, otherCharges,
                    (otherCharges != null && otherCharges.compareTo(BigDecimal.ZERO) > 0) ? "Other Charges" : null,
                    discountAmount, gstPercentage, offerPercentage, true,
                    null, null, null, null, null, null);
        }

        public BuilderRequest(
                Long quotationId,
                CustomerRequest customer,
                Long branchId,
                LocalDate quotationDate,
                PricingMode pricingMode,
                String customerRequirements,
                String notes,
                List<RoomRequest> rooms) {
            this(quotationId, customer, branchId, quotationDate, null, pricingMode, customerRequirements, notes, rooms,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                    null, null, null, null, null, null);
        }
    }

    public record CustomerRequest(
            Long id,
            String name,
            String phone,
            String email,
            String address) {

        public CustomerRequest(
                String name,
                String phone,
                String email,
                String address) {
            this(null, name, phone, email, address);
        }
    }

    public record RoomRequest(
            String floor,
            String room,
            String workDescription,
            List<ItemRequest> items) {
    }

    public record ItemRequest(
            Long workCategoryId,
            Long divisionId,
            Long itemId,
            Long materialId,
            Long materialOptionId,
            Long materialSpecificationId,
            String itemDescription,
            String specification,
            List<SizeRequest> sizes,
            MeasurementType measurementType,
            BigDecimal quantity,
            String imageUrl,
            BigDecimal rate,
            BigDecimal coveragePerRoll) {

        public ItemRequest(
                Long workCategoryId,
                Long divisionId,
                Long itemId,
                Long materialId,
                Long materialOptionId,
                Long materialSpecificationId,
                String itemDescription,
                String specification,
                List<SizeRequest> sizes,
                MeasurementType measurementType,
                BigDecimal quantity,
                String imageUrl,
                BigDecimal rate) {
            this(workCategoryId, divisionId, itemId, materialId, materialOptionId, materialSpecificationId, itemDescription, specification, sizes, measurementType, quantity, imageUrl, rate, null);
        }

        public ItemRequest(
                Long workCategoryId,
                Long divisionId,
                Long itemId,
                Long materialId,
                Long materialOptionId,
                Long materialSpecificationId,
                String itemDescription,
                String specification,
                List<SizeRequest> sizes,
                MeasurementType measurementType,
                BigDecimal quantity,
                String imageUrl) {
            this(workCategoryId, divisionId, itemId, materialId, materialOptionId, materialSpecificationId, itemDescription, specification, sizes, measurementType, quantity, imageUrl, null, null);
        }

        public ItemRequest(
                Long workCategoryId,
                Long divisionId,
                Long itemId,
                Long materialId,
                Long materialOptionId,
                Long materialSpecificationId,
                String itemDescription,
                String specification,
                List<SizeRequest> sizes) {
            this(workCategoryId, divisionId, itemId, materialId, materialOptionId, materialSpecificationId, itemDescription, specification, sizes, null, null, null, null, null);
        }

        public ItemRequest(
                Long workCategoryId,
                Long divisionId,
                Long itemId,
                Long materialId,
                Long materialOptionId,
                String itemDescription,
                String specification,
                List<SizeRequest> sizes) {
            this(workCategoryId, divisionId, itemId, materialId, materialOptionId, null, itemDescription, specification, sizes, null, null, null, null, null);
        }
    }

    public record SizeRequest(
            BigDecimal length,
            BigDecimal width,
            BigDecimal height,
            BigDecimal offerPrice) {
    }
}
