package com.example.pravin_quotation.service;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.util.TitleCaseUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

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
            String message,
            String specification,
            Long materialSpecificationId
    ) {
        public ResolvedMaterialRate(
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
            this(materialId, materialOptionId, materialName, rate, formula, unit, measurementType, imageUrl, coveragePerRoll, available, message, null, null);
        }

        public static ResolvedMaterialRate unavailable(String message) {
            return new ResolvedMaterialRate(null, null, null, null, null, null, null, null, null, false, message, null, null);
        }

        public static ResolvedMaterialRate unavailable(Long materialId, String materialName, String message) {
            return new ResolvedMaterialRate(materialId, null, materialName, null, null, null, null, null, null, false, message, null, null);
        }
    }

    public List<Material> getActiveMaterialsForDivisionAndGrade(Long divisionId, PricingMode targetGrade, Long branchId) {
        if (divisionId == null || targetGrade == null || materialRepository == null) {
            return Collections.emptyList();
        }
        List<Material> result = new ArrayList<>();
        List<Material> gradeMats = null;
        try {
            gradeMats = materialRepository.findActiveByDivisionIdAndPricingGrade(divisionId, targetGrade);
        } catch (Exception ignored) {}
        if (gradeMats != null) {
            for (Material m : gradeMats) {
                if (m != null && Boolean.TRUE.equals(m.getActive()) && !result.contains(m)) {
                    result.add(m);
                }
            }
        }
        List<Material> allInDiv = null;
        try {
            allInDiv = materialRepository.findActiveByDivisionId(divisionId);
        } catch (Exception ignored) {}
        if (allInDiv != null) {
            for (Material m : allInDiv) {
                if (m != null && Boolean.TRUE.equals(m.getActive()) && !result.contains(m)) {
                    if (m.getPricingGrade() == targetGrade) {
                        result.add(m);
                    } else if (m.getPricingGrade() == null && hasPositiveRateForGrade(branchId, m, targetGrade)) {
                        result.add(m);
                    }
                }
            }
        }
        if (result.isEmpty()) {
            List<Material> fallback = materialRepository.findAllByDivisionIdAndPricingGrade(divisionId, targetGrade);
            if (fallback != null) {
                for (Material m : fallback) {
                    if (m != null && Boolean.TRUE.equals(m.getActive()) && !result.contains(m)) {
                        result.add(m);
                    }
                }
            }
        }
        // Explicit sort by displayOrder ASC, id ASC to guarantee deterministic position order
        result.sort(Comparator.comparing((Material m) -> m.getDisplayOrder() != null ? m.getDisplayOrder() : 0)
                .thenComparing((Material m) -> m.getId() != null ? m.getId() : 0L));
        return result;
    }

    public Material findMaterialByPosition(List<Material> materials, Integer position) {
        if (materials == null || materials.isEmpty() || position == null || position < 0) {
            return null;
        }
        if (position < materials.size()) {
            return materials.get(position);
        }
        return null; // Position does not exist in target grade! Never fallback or reuse!
    }

    @Transactional(readOnly = true)
    public ResolvedMaterialRate resolveMaterialAndRate(
            Long branchId,
            Long divisionId,
            Long materialId,
            Long materialOptionId,
            PricingMode targetGrade) {
        return resolveMaterialAndRate(branchId, divisionId, materialId, materialOptionId, targetGrade, null);
    }

    @Transactional(readOnly = true)
    public ResolvedMaterialRate resolveMaterialAndRate(
            Long branchId,
            Long divisionId,
            Long materialId,
            Long materialOptionId,
            PricingMode targetGrade,
            Integer position) {

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

        if (divisionId == null && mat != null) {
            if (mat.getDivision() != null) {
                divisionId = mat.getDivision().getId();
            } else if (mat.getItem() != null && mat.getItem().getDivision() != null) {
                divisionId = mat.getItem().getDivision().getId();
            } else if (mat.getDivisions() != null && !mat.getDivisions().isEmpty()) {
                divisionId = mat.getDivisions().iterator().next().getId();
            }
        }

        String gradeLabel = targetGrade == PricingMode.PREMIUM ? "Premium" : (targetGrade == PricingMode.ECONOMY ? "Economy" : "Middle");
        String divName = "";
        if (divisionId != null && divisionRepository != null) {
            divName = divisionRepository.findById(divisionId).map(Division::getName).orElse("");
        }

        List<Material> targetCandidates = getActiveMaterialsForDivisionAndGrade(divisionId, targetGrade, branchId);

        // If position is not explicitly provided, determine position from source material in its source grade
        if (position == null && mat != null && divisionId != null) {
            PricingMode sourceGrade = mat.getPricingGrade() != null ? mat.getPricingGrade() : PricingMode.PREMIUM;
            List<Material> sourceMats = getActiveMaterialsForDivisionAndGrade(divisionId, sourceGrade, branchId);
            for (int i = 0; i < sourceMats.size(); i++) {
                if (mat.getId().equals(sourceMats.get(i).getId())) {
                    position = i;
                    break;
                }
            }
        }

        Material effectiveMat = null;
        if (position != null && position >= 0) {
            effectiveMat = findMaterialByPosition(targetCandidates, position);
        } else if (mat != null && (mat.getPricingGrade() == targetGrade || (mat.getPricingGrade() == null && targetCandidates.contains(mat)))) {
            effectiveMat = mat;
        }

        if (effectiveMat == null) {
            String posHint = (position != null) ? " (Position " + (position + 1) + ")" : "";
            String msg = "No material available in " + gradeLabel + " Grade" + (!divName.isBlank() ? (" for " + divName) : "") + posHint + ".";
            return ResolvedMaterialRate.unavailable(null, "Material Unavailable (" + gradeLabel + " Grade)", msg);
        }

        if (!Boolean.TRUE.equals(effectiveMat.getActive())) {
            return ResolvedMaterialRate.unavailable(effectiveMat.getId(), effectiveMat.getName(), "Material '" + effectiveMat.getName() + "' is inactive.");
        }

        MaterialOption effectiveOpt = null;
        List<MaterialOption> cpOpts = materialOptionRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(effectiveMat.getId());
        if (cpOpts.isEmpty()) {
            cpOpts = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(effectiveMat.getId());
        }
        if (!cpOpts.isEmpty()) {
            if (opt != null && opt.getName() != null) {
                effectiveOpt = cpOpts.stream()
                        .filter(o -> o.getName() != null && o.getName().trim().equalsIgnoreCase(opt.getName().trim()))
                        .findFirst()
                        .orElse(cpOpts.get(0));
            } else {
                effectiveOpt = cpOpts.get(0);
            }
        }

        BigDecimal matchedRate = null;
        if (effectiveOpt != null) {
            Pricing p = pricingRepository.findByMaterialOptionIdAndPricingMode(effectiveOpt.getId(), targetGrade).orElse(null);
            if (p != null && Boolean.TRUE.equals(p.getActive())) {
                matchedRate = resolveRateFromPricing(branchId, p);
            }
        }
        if (matchedRate == null) {
            for (MaterialOption o : cpOpts) {
                Pricing p = pricingRepository.findByMaterialOptionIdAndPricingMode(o.getId(), targetGrade).orElse(null);
                if (p != null && Boolean.TRUE.equals(p.getActive())) {
                    matchedRate = resolveRateFromPricing(branchId, p);
                    if (matchedRate != null) {
                        effectiveOpt = o;
                        break;
                    }
                }
            }
        }
        if (matchedRate == null && effectiveMat.getRate() != null && effectiveMat.getRate().compareTo(BigDecimal.ZERO) > 0) {
            matchedRate = money(effectiveMat.getRate());
        }

        if (matchedRate == null) {
            String msg = "No branch rate configured for " + effectiveMat.getName() + " in " + gradeLabel + " Grade. Please configure a rate.";
            return ResolvedMaterialRate.unavailable(effectiveMat.getId(), effectiveMat.getName(), msg);
        }

        String effectiveMatName = effectiveMat.getName();
        String formula = (effectiveMat.getFormula() != null && !effectiveMat.getFormula().isBlank() && !"(L + W) × H".equals(effectiveMat.getFormula()))
                ? effectiveMat.getFormula()
                : ((effectiveOpt != null && effectiveOpt.getFormula() != null && !effectiveOpt.getFormula().isBlank())
                        ? effectiveOpt.getFormula()
                        : "(L + W) × H");

        MeasurementType measurementType = resolveEffectiveMeasurementType(effectiveMat);
        String unit = (effectiveMat.getUnit() != null && !effectiveMat.getUnit().isBlank())
                ? effectiveMat.getUnit()
                : (measurementType == MeasurementType.ROLL ? "Roll" : (measurementType == MeasurementType.QUANTITY ? "Quantity" : (measurementType == MeasurementType.FILES ? "Files" : "Sq.ft")));

        BigDecimal coverage = effectiveMat.getCoveragePerRoll();
        String imageUrl = effectiveMat.getImageUrl();

        String effectiveSpec = null;
        Long effectiveSpecId = null;

        if (effectiveOpt != null && materialSpecificationRepository != null) {
            List<MaterialSpecification> optSpecs = materialSpecificationRepository.findByMaterialOptionIdAndActiveTrueOrderByDisplayOrderAsc(effectiveOpt.getId());
            if (optSpecs != null && !optSpecs.isEmpty()) {
                MaterialSpecification s = optSpecs.get(0);
                effectiveSpec = s.getName() != null && !s.getName().isBlank() ? s.getName().trim() : s.getDescription();
                effectiveSpecId = s.getId();
            }
        }
        if ((effectiveSpec == null || effectiveSpec.isBlank()) && materialSpecificationRepository != null) {
            List<MaterialSpecification> matSpecs = materialSpecificationRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(effectiveMat.getId());
            if (matSpecs != null && !matSpecs.isEmpty()) {
                MaterialSpecification s = matSpecs.get(0);
                effectiveSpec = s.getName() != null && !s.getName().isBlank() ? s.getName().trim() : s.getDescription();
                effectiveSpecId = s.getId();
            }
        }
        if ((effectiveSpec == null || effectiveSpec.isBlank()) && effectiveMat.getSpecification() != null && !effectiveMat.getSpecification().isBlank()) {
            effectiveSpec = effectiveMat.getSpecification().trim();
        }
        if ((effectiveSpec == null || effectiveSpec.isBlank()) && effectiveOpt != null && effectiveOpt.getDescription() != null && !effectiveOpt.getDescription().isBlank()) {
            effectiveSpec = effectiveOpt.getDescription().trim();
        }
        if ((effectiveSpec == null || effectiveSpec.isBlank()) && effectiveMat.getDescription() != null && !effectiveMat.getDescription().isBlank()) {
            effectiveSpec = effectiveMat.getDescription().trim();
        }
        if (effectiveSpec == null) effectiveSpec = "";

        return new ResolvedMaterialRate(
                effectiveMat.getId(),
                effectiveOpt != null ? effectiveOpt.getId() : null,
                effectiveMatName,
                matchedRate,
                formula,
                unit,
                measurementType,
                imageUrl,
                coverage,
                true,
                null,
                effectiveSpec,
                effectiveSpecId
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
        return hasPositiveRateForGrade(null, m, grade);
    }

    public boolean hasPositiveRateForGrade(Long branchId, Material m, PricingMode grade) {
        if (m == null) return false;
        if (m.getPricingGrade() == grade && m.getRate() != null && m.getRate().compareTo(BigDecimal.ZERO) > 0) {
            return true;
        }
        List<MaterialOption> opts = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(m.getId());
        for (MaterialOption opt : opts) {
            Pricing p = pricingRepository.findByMaterialOptionIdAndPricingMode(opt.getId(), grade).orElse(null);
            if (p != null && Boolean.TRUE.equals(p.getActive())) {
                BigDecimal r = resolveRateFromPricing(branchId, p);
                if (r != null && r.compareTo(BigDecimal.ZERO) > 0) {
                    return true;
                }
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

    public String extractSubtypeToken(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        if (lower.contains("partition")) return "partition";
        if (lower.contains("box")) return "box";
        if (lower.contains("carcass")) return "carcass";
        if (lower.contains("drawer")) return "drawer";
        if (lower.contains("fluted") || lower.contains("louver")) return "fluted";
        if (lower.contains("cove")) return "cove";
        if (lower.contains("cornice")) return "cornice";
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

        // 3. Shared item match (if both materials belong to the same parent Item)
        if (source.getItem() != null && source.getItem().getId() != null) {
            for (Material c : candidates) {
                if (resolveEffectiveMeasurementType(c) == sourceType
                        && c.getItem() != null && source.getItem().getId().equals(c.getItem().getId())) {
                    return c;
                }
            }
        }

        // 4. Subtype / structural keyword match (e.g. "box type" / "box" vs "partition type" / "partition")
        String sourceSubtype = extractSubtypeToken(sourceName);
        if (sourceSubtype != null) {
            for (Material c : candidates) {
                if (resolveEffectiveMeasurementType(c) == sourceType && c.getName() != null) {
                    String candidateSubtype = extractSubtypeToken(c.getName());
                    if (sourceSubtype.equals(candidateSubtype)) {
                        return c;
                    }
                }
            }
            // If source has a distinct subtype (like "partition"), but NO candidate shares it,
            // strictly DO NOT match! It has no counterpart in this target grade.
            return null;
        }

        // 5. If division has only 1 active material in sourceGrade AND only 1 candidate in targetGrade,
        // and neither has a conflicting subtype token:
        if (candidates.size() == 1) {
            Material singleCandidate = candidates.get(0);
            String candidateSubtype = extractSubtypeToken(singleCandidate.getName());
            if (candidateSubtype == null && resolveEffectiveMeasurementType(singleCandidate) == sourceType) {
                Long divId = (source.getDivision() != null) ? source.getDivision().getId() : null;
                if (divId != null && source.getPricingGrade() != null) {
                    List<Material> sourceDivMats = materialRepository.findActiveByDivisionIdAndPricingGrade(divId, source.getPricingGrade());
                    if (sourceDivMats != null && sourceDivMats.size() == 1) {
                        return singleCandidate;
                    }
                } else {
                    return singleCandidate;
                }
            }
        }

        return null;
    }

    private String normalizeMaterialName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("\\b(premium|middle|economy|standard)\\b", "")
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

    // =========================================================
    // SAVE / AUTO-SAVE
    // =========================================================

    @Transactional
    public Quotation save(
            BuilderRequest request,
            String loggedInEmail) {
        return processQuotationSave(request, loggedInEmail, false);
    }

    @Transactional
    public Quotation autoSave(
            BuilderRequest request,
            String loggedInEmail) {
        return processQuotationSave(request, loggedInEmail, true);
    }

    private Quotation processQuotationSave(
            BuilderRequest request,
            String loggedInEmail,
            boolean isAutoSave) {

        if (request == null) {
            throw new IllegalArgumentException("Quotation request cannot be null.");
        }

        User loggedInUser = userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new IllegalArgumentException("Logged-in user was not found."));

        if (!Boolean.TRUE.equals(loggedInUser.getActive())) {
            throw new IllegalArgumentException("User account is inactive.");
        }

        if (loggedInUser.getRole() == Role.EMPLOYEE && loggedInUser.getBranch() != null && (request.branchId() == null || request.branchId() == 0L)) {
            request = new BuilderRequest(
                    request.quotationId(),
                    request.parentId(),
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

        if (!isAutoSave) {
            validateRequest(request);
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
            if (branch == null && !isAutoSave) {
                throw new IllegalArgumentException("Branch is required.");
            }
        }

        PricingMode requestedMode = request.pricingMode() != null ? request.pricingMode() : (PricingMode.values().length > 0 ? PricingMode.values()[0] : PricingMode.PREMIUM);

        Quotation targetQuotation = null;
        boolean isEdit = false;
        java.util.Set<Long> existingMaterialIds = new java.util.HashSet<>();

        // 1. Resolve existing quotation for single mixed-grade quotation
        if (request.quotationId() != null) {
            targetQuotation = quotationRepository.findById(request.quotationId()).orElse(null);
            if (targetQuotation != null) {
                isEdit = true;
            }
        }
        if (targetQuotation == null && request.parentId() != null) {
            targetQuotation = quotationRepository.findById(request.parentId()).orElse(null);
            if (targetQuotation != null) {
                isEdit = true;
            }
        }

        if (targetQuotation != null && loggedInUser.getRole() == Role.EMPLOYEE && targetQuotation.getBranch() != null && loggedInUser.getBranch() != null
                && !targetQuotation.getBranch().getId().equals(loggedInUser.getBranch().getId())) {
            throw new AccessDeniedException("Unauthorized: Cannot modify quotation for another branch.");
        }

        // 2. Brand new Quotation (single quotation number)
        if (targetQuotation == null) {
            targetQuotation = new Quotation();
            targetQuotation.setQuotationNumber(nextQuotationNumber());
            targetQuotation.setPricingMode(requestedMode);
            targetQuotation.setStatus(isAutoSave ? QuotationStatus.PENDING : QuotationStatus.WORKING);
            targetQuotation.setEmployee(loggedInUser);
            if (branch != null) targetQuotation.setBranch(branch);
            isEdit = false;
        }

        Quotation parentQuotation = targetQuotation.getParent();

        if (isEdit && targetQuotation.getId() != null) {
            if (loggedInUser.getRole() == Role.EMPLOYEE && targetQuotation.getBranch() != null && loggedInUser.getBranch() != null
                    && !targetQuotation.getBranch().getId().equals(loggedInUser.getBranch().getId())) {
                throw new AccessDeniedException("Unauthorized: Cannot edit quotations for another branch.");
            }
            List<QuotationRoom> oldRooms = quotationRoomService.getActiveByQuotationId(targetQuotation.getId());
            for (QuotationRoom oldRoom : oldRooms) {
                List<QuotationItem> oldItems = quotationItemService.getActiveByQuotationRoomId(oldRoom.getId());
                if (oldItems != null) {
                    for (QuotationItem oi : oldItems) {
                        if (oi.getMaterial() != null && oi.getMaterial().getId() != null) {
                            existingMaterialIds.add(oi.getMaterial().getId());
                        }
                        if (quotationItemSizeService != null) {
                            List<QuotationItemSize> oldSizes = quotationItemSizeService.getActiveSizes(oi.getId());
                            if (oldSizes != null) {
                                for (QuotationItemSize os : oldSizes) {
                                    try {
                                        quotationItemSizeService.deactivate(os.getId());
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                        quotationItemService.deactivate(oi.getId());
                    }
                }
                quotationRoomService.deactivate(oldRoom.getId());
            }
        }

        // Customer resolution
        Customer customer = null;
        if (request.customer() != null) {
            try {
                if (isEdit || (request.customer().name() != null && !request.customer().name().isBlank()
                        && request.customer().phone() != null && !request.customer().phone().isBlank())) {
                    customer = resolveCustomer(request.customer(), branch, loggedInUser, parentQuotation != null ? parentQuotation : targetQuotation, isEdit);
                } else if (request.customer().id() != null) {
                    customer = customerRepository.findById(request.customer().id()).orElse(null);
                }
            } catch (Exception ex) {
                if (!isAutoSave) {
                    throw ex;
                }
            }
        }
        if (customer == null) {
            if (parentQuotation != null && parentQuotation.getCustomer() != null) {
                customer = parentQuotation.getCustomer();
            } else if (targetQuotation.getCustomer() != null) {
                customer = targetQuotation.getCustomer();
            }
        }

        LocalDate effectiveQuotationDate = request.quotationDate() != null
                ? request.quotationDate()
                : (parentQuotation != null && parentQuotation.getQuotationDate() != null ? parentQuotation.getQuotationDate() : LocalDate.now());

        LocalDate validUntil = request.validUntil();
        if (validUntil == null) {
            validUntil = effectiveQuotationDate.plusDays(365);
        }

        if (parentQuotation != null) {
            if (customer != null) parentQuotation.setCustomer(customer);
            if (branch != null) parentQuotation.setBranch(branch);
            if (parentQuotation.getEmployee() == null) parentQuotation.setEmployee(loggedInUser);
            if (parentQuotation.getQuotationDate() == null) parentQuotation.setQuotationDate(effectiveQuotationDate);
            if (parentQuotation.getValidUntil() == null) parentQuotation.setValidUntil(validUntil);
            parentQuotation.setCustomerRequirements(clean(request.customerRequirements()));
            parentQuotation.setNotes(clean(request.notes()));
            if (!isAutoSave && (parentQuotation.getStatus() == null || parentQuotation.getStatus() == QuotationStatus.PENDING)) {
                parentQuotation.setStatus(QuotationStatus.WORKING);
            }
            parentQuotation = quotationRepository.save(parentQuotation);
            targetQuotation.setParent(parentQuotation);
        }

        targetQuotation.setCustomer(customer);
        if (branch != null) targetQuotation.setBranch(branch);
        if (targetQuotation.getEmployee() == null) targetQuotation.setEmployee(loggedInUser);
        targetQuotation.setQuotationDate(effectiveQuotationDate);
        targetQuotation.setValidUntil(validUntil);
        targetQuotation.setPricingMode(requestedMode);

        if (isAutoSave) {
            if (targetQuotation.getStatus() == null || targetQuotation.getStatus() == QuotationStatus.PENDING) {
                targetQuotation.setStatus(QuotationStatus.PENDING);
            }
        } else {
            if (targetQuotation.getStatus() == null || targetQuotation.getStatus() == QuotationStatus.PENDING) {
                targetQuotation.setStatus(QuotationStatus.WORKING);
            }
        }

        targetQuotation.setCustomerRequirements(clean(request.customerRequirements()));
        targetQuotation.setNotes(clean(request.notes()));
        if (request.accessoriesAmount() != null) targetQuotation.setAccessoriesAmount(request.accessoriesAmount());
        if (request.travelCharge() != null) targetQuotation.setTravelCharge(request.travelCharge());
        if (request.otherCharges() != null) targetQuotation.setOtherCharges(request.otherCharges());
        if (request.otherChargesDescription() != null) targetQuotation.setOtherChargesDescription(clean(request.otherChargesDescription()));
        if (request.discountAmount() != null) targetQuotation.setDiscountAmount(request.discountAmount());
        if (request.gstPercentage() != null) targetQuotation.setGstPercentage(request.gstPercentage());
        if (request.offerPercentage() != null) targetQuotation.setOfferPercentage(request.offerPercentage());
        if (request.showSize() != null) {
            targetQuotation.setShowSize(request.showSize());
        } else if (request.showSqftSize() != null) {
            targetQuotation.setShowSize(request.showSqftSize());
        } else if (targetQuotation.getShowSize() == null) {
            targetQuotation.setShowSize(true);
        }

        if (request.showSqft() != null) {
            targetQuotation.setShowSqft(request.showSqft());
        } else if (request.showSqftSize() != null) {
            targetQuotation.setShowSqft(request.showSqftSize());
        } else if (targetQuotation.getShowSqft() == null) {
            targetQuotation.setShowSqft(true);
        }

        if (request.showSqftSize() != null) {
            targetQuotation.setShowSqftSize(request.showSqftSize());
        } else {
            targetQuotation.setShowSqftSize(Boolean.TRUE.equals(targetQuotation.getShowSize()) && Boolean.TRUE.equals(targetQuotation.getShowSqft()));
        }

        targetQuotation.setLastActiveStep(request.lastActiveStep());
        targetQuotation.setLastActiveFloor(request.lastActiveFloor());
        targetQuotation.setLastActiveRoom(request.lastActiveRoom());
        targetQuotation.setLastActiveCategoryId(request.lastActiveCategoryId());
        targetQuotation.setLastActiveDivisionId(request.lastActiveDivisionId());
        targetQuotation.setBuilderState(request.builderState());

        Quotation savedQuotation;
        try {
            savedQuotation = quotationRepository.save(targetQuotation);
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            String expectedChildNumber = (parentQuotation != null && parentQuotation.getRawQuotationNumber() != null)
                    ? parentQuotation.getRawQuotationNumber() + "-" + requestedMode.name()
                    : targetQuotation.getRawQuotationNumber();
            Quotation existingChild = null;
            if (parentQuotation != null && parentQuotation.getId() != null) {
                existingChild = quotationRepository.findByParentIdAndPricingMode(parentQuotation.getId(), requestedMode).orElse(null);
            }
            if (existingChild == null && expectedChildNumber != null) {
                existingChild = quotationRepository.findByQuotationNumber(expectedChildNumber).orElse(null);
            }
            if (existingChild != null) {
                targetQuotation = existingChild;
                targetQuotation.setCustomer(customer);
                if (branch != null) targetQuotation.setBranch(branch);
                targetQuotation.setQuotationDate(effectiveQuotationDate);
                targetQuotation.setValidUntil(validUntil);
                targetQuotation.setPricingMode(requestedMode);
                if (isAutoSave) {
                    if (targetQuotation.getStatus() == null || targetQuotation.getStatus() == QuotationStatus.PENDING) {
                        targetQuotation.setStatus(QuotationStatus.PENDING);
                    }
                } else {
                    targetQuotation.setStatus(QuotationStatus.WORKING);
                }
                targetQuotation.setCustomerRequirements(clean(request.customerRequirements()));
                targetQuotation.setNotes(clean(request.notes()));
                if (request.accessoriesAmount() != null) targetQuotation.setAccessoriesAmount(request.accessoriesAmount());
                if (request.travelCharge() != null) targetQuotation.setTravelCharge(request.travelCharge());
                if (request.otherCharges() != null) targetQuotation.setOtherCharges(request.otherCharges());
                if (request.otherChargesDescription() != null) targetQuotation.setOtherChargesDescription(clean(request.otherChargesDescription()));
                if (request.discountAmount() != null) targetQuotation.setDiscountAmount(request.discountAmount());
                if (request.gstPercentage() != null) targetQuotation.setGstPercentage(request.gstPercentage());
                if (request.offerPercentage() != null) targetQuotation.setOfferPercentage(request.offerPercentage());
                if (request.showSize() != null) targetQuotation.setShowSize(request.showSize());
                if (request.showSqft() != null) targetQuotation.setShowSqft(request.showSqft());
                if (request.showSqftSize() != null) targetQuotation.setShowSqftSize(request.showSqftSize());
                targetQuotation.setLastActiveStep(request.lastActiveStep());
                targetQuotation.setLastActiveFloor(request.lastActiveFloor());
                targetQuotation.setLastActiveRoom(request.lastActiveRoom());
                targetQuotation.setLastActiveCategoryId(request.lastActiveCategoryId());
                targetQuotation.setLastActiveDivisionId(request.lastActiveDivisionId());
                targetQuotation.setBuilderState(request.builderState());
                if (parentQuotation != null) targetQuotation.setParent(parentQuotation);
                savedQuotation = quotationRepository.save(targetQuotation);
                isEdit = true;
            } else {
                throw dive;
            }
        }

        if (request.rooms() != null && !request.rooms().isEmpty()) {
            saveRoomsAndItems(savedQuotation, request.rooms(), branch != null ? branch.getId() : null, requestedMode, existingMaterialIds, isEdit, isAutoSave);
        }

        Quotation calculated = quotationCalculationService.calculateQuotation(savedQuotation.getId());
        if (isAutoSave && savedQuotation.getStatus() == QuotationStatus.PENDING && calculated.getStatus() != QuotationStatus.PENDING) {
            calculated.setStatus(QuotationStatus.PENDING);
            calculated = quotationRepository.save(calculated);
        }

        if (!isAutoSave && parentQuotation != null && parentQuotation.getStatus() == QuotationStatus.PENDING) {
            parentQuotation.setStatus(QuotationStatus.WORKING);
            parentQuotation = quotationRepository.save(parentQuotation);
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

                Material material = itemRequest.materialId() != null ? materialRepository.findById(itemRequest.materialId()).orElse(null) : null;
                if (material == null && itemRequest.originalMaterialId() != null) {
                    material = materialRepository.findById(itemRequest.originalMaterialId()).orElse(null);
                }
                if (material == null && itemRequest.divisionId() != null) {
                    List<Material> divMats = materialRepository.findActiveByDivisionId(itemRequest.divisionId());
                    if (divMats != null && !divMats.isEmpty()) {
                        material = divMats.get(0);
                    }
                }
                if (material == null) {
                    if (isAutoSave) continue;
                    throw new IllegalArgumentException("Material not found.");
                }

                Long divId = itemRequest.divisionId();
                if (divId == null && material.getDivision() != null) {
                    divId = material.getDivision().getId();
                }
                Division division = divId != null ? divisionRepository.findById(divId).orElse(null) : null;
                if (division == null) {
                    if (isAutoSave) continue;
                    throw new IllegalArgumentException("Division not found.");
                }

                Long catId = itemRequest.workCategoryId();
                if (catId == null && division.getWorkCategory() != null) {
                    catId = division.getWorkCategory().getId();
                }
                WorkCategory category = catId != null ? workCategoryRepository.findById(catId).orElse(null) : null;
                if (category == null && division.getWorkCategory() != null) {
                    category = division.getWorkCategory();
                }
                if (category == null) {
                    if (isAutoSave) continue;
                    throw new IllegalArgumentException("Work category not found.");
                }

                if (division.getWorkCategory() != null && !division.getWorkCategory().getId().equals(category.getId())) {
                    category = division.getWorkCategory();
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

                PricingMode itemGrade = itemRequest.pricingMode() != null ? itemRequest.pricingMode() : pricingMode;

                ResolvedMaterialRate resolved = resolveMaterialAndRate(
                        branchId,
                        division.getId(),
                        material.getId(),
                        option != null ? option.getId() : null,
                        itemGrade
                );
                boolean isExistingItemInQuotation = isEdit && existingMaterialIds.contains(material.getId());

                if (!resolved.available() || resolved.rate() == null || resolved.rate().compareTo(BigDecimal.ZERO) <= 0) {
                    if (isExistingItemInQuotation || isAutoSave || (itemRequest.rate() != null && itemRequest.rate().compareTo(BigDecimal.ZERO) > 0)) {
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
                        String gradeLabel = itemGrade == PricingMode.PREMIUM ? "Premium" : (itemGrade == PricingMode.ECONOMY ? "Economy" : "Middle");
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
                        ? pricingRepository.findByMaterialOptionIdAndPricingMode(option.getId(), itemGrade).orElse(null)
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
                        coverage,
                        itemGrade
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
                    if (request.id() == null) {
                        existingCustomer = existingByPhone.get();
                    } else {
                        throw new IllegalArgumentException("Phone number already exists: A customer with phone number " + phone + " is already registered.");
                    }
                }

                // Check duplicate email across other customers if provided
                if (email != null && !email.isBlank()) {
                    String lowerEmail = email.toLowerCase().trim();
                    java.util.Optional<Customer> existingByEmail = customerRepository.findByEmail(lowerEmail);
                    if (existingByEmail.isPresent() && !existingByEmail.get().getId().equals(existingCustomer.getId())) {
                        if (request.id() == null) {
                            existingCustomer = existingByEmail.get();
                        } else {
                            throw new IllegalArgumentException("Email address already exists: A customer with email " + lowerEmail + " is already registered.");
                        }
                    }
                }

                existingCustomer.setName(name);
                existingCustomer.setPhone(phone);
                existingCustomer.setEmail(email != null && !email.isBlank() ? email.toLowerCase().trim() : null);
                existingCustomer.setAddress(address);
                if (branch != null) {
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
            Long parentId,
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
            String builderState,
            Boolean showSize,
            Boolean showSqft) {

        public BuilderRequest(
                Long quotationId,
                Long parentId,
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
            this(quotationId, parentId, customer, branchId, quotationDate, validUntil, pricingMode, customerRequirements, notes, rooms,
                    accessoriesAmount, travelCharge, otherCharges, otherChargesDescription,
                    discountAmount, gstPercentage, offerPercentage, showSqftSize,
                    lastActiveStep, lastActiveFloor, lastActiveRoom, lastActiveCategoryId, lastActiveDivisionId, builderState,
                    showSqftSize, showSqftSize);
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
            this(quotationId, null, customer, branchId, quotationDate, validUntil, pricingMode, customerRequirements, notes, rooms,
                    accessoriesAmount, travelCharge, otherCharges, otherChargesDescription,
                    discountAmount, gstPercentage, offerPercentage, showSqftSize,
                    lastActiveStep, lastActiveFloor, lastActiveRoom, lastActiveCategoryId, lastActiveDivisionId, builderState);
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
                String otherChargesDescription,
                BigDecimal discountAmount,
                BigDecimal gstPercentage,
                BigDecimal offerPercentage,
                Boolean showSqftSize) {
            this(quotationId, null, customer, branchId, quotationDate, validUntil, pricingMode, customerRequirements, notes, rooms,
                    accessoriesAmount, travelCharge, otherCharges, otherChargesDescription,
                    discountAmount, gstPercentage, offerPercentage, showSqftSize,
                    null, null, null, null, null, null);
        }

        public BuilderRequest(
                Long quotationId,
                Long parentId,
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
            this(quotationId, parentId, customer, branchId, quotationDate, validUntil, pricingMode, customerRequirements, notes, rooms,
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
            this(quotationId, null, customer, branchId, quotationDate, validUntil, pricingMode, customerRequirements, notes, rooms,
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
            this(quotationId, null, customer, branchId, quotationDate, null, pricingMode, customerRequirements, notes, rooms,
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
            this(quotationId, null, customer, branchId, quotationDate, null, pricingMode, customerRequirements, notes, rooms,
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RoomRequest(
            String floor,
            String room,
            String workDescription,
            List<ItemRequest> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemRequest(
            @JsonAlias({"categoryId", "workCategoryId"})
            Long workCategoryId,
            Long divisionId,
            Long itemId,
            Long materialId,
            String materialName,
            Long materialOptionId,
            Long materialSpecificationId,
            String itemDescription,
            String specification,
            List<SizeRequest> sizes,
            MeasurementType measurementType,
            BigDecimal quantity,
            String imageUrl,
            BigDecimal rate,
            BigDecimal coveragePerRoll,
            PricingMode pricingMode,
            Long originalMaterialId,
            PricingMode originalPricingMode,
            String originalMaterialName,
            String originalSpecification,
            @JsonAlias({"position", "materialPosition", "itemPosition"})
            Integer originalPosition) {

        public ItemRequest(
                Long workCategoryId,
                Long divisionId,
                Long itemId,
                Long materialId,
                String materialName,
                Long materialOptionId,
                Long materialSpecificationId,
                String itemDescription,
                String specification,
                List<SizeRequest> sizes,
                MeasurementType measurementType,
                BigDecimal quantity,
                String imageUrl,
                BigDecimal rate,
                BigDecimal coveragePerRoll,
                PricingMode pricingMode,
                Long originalMaterialId,
                PricingMode originalPricingMode,
                String originalMaterialName,
                String originalSpecification) {
            this(workCategoryId, divisionId, itemId, materialId, materialName, materialOptionId, materialSpecificationId, itemDescription, specification, sizes, measurementType, quantity, imageUrl, rate, coveragePerRoll, pricingMode, originalMaterialId, originalPricingMode, originalMaterialName, originalSpecification, null);
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
                String imageUrl,
                BigDecimal rate,
                BigDecimal coveragePerRoll,
                PricingMode pricingMode) {
            this(workCategoryId, divisionId, itemId, materialId, null, materialOptionId, materialSpecificationId, itemDescription, specification, sizes, measurementType, quantity, imageUrl, rate, coveragePerRoll, pricingMode, null, null, null, null, null);
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
                String imageUrl,
                BigDecimal rate,
                BigDecimal coveragePerRoll) {
            this(workCategoryId, divisionId, itemId, materialId, null, materialOptionId, materialSpecificationId, itemDescription, specification, sizes, measurementType, quantity, imageUrl, rate, coveragePerRoll, null, null, null, null, null, null);
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
                String imageUrl,
                BigDecimal rate) {
            this(workCategoryId, divisionId, itemId, materialId, null, materialOptionId, materialSpecificationId, itemDescription, specification, sizes, measurementType, quantity, imageUrl, rate, null, null, null, null, null, null, null);
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
            this(workCategoryId, divisionId, itemId, materialId, null, materialOptionId, materialSpecificationId, itemDescription, specification, sizes, measurementType, quantity, imageUrl, null, null, null, null, null, null, null, null);
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
            this(workCategoryId, divisionId, itemId, materialId, null, materialOptionId, materialSpecificationId, itemDescription, specification, sizes, null, null, null, null, null, null, null, null, null, null, null);
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
            this(workCategoryId, divisionId, itemId, materialId, null, materialOptionId, null, itemDescription, specification, sizes, null, null, null, null, null, null, null, null, null, null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SizeRequest(
            BigDecimal length,
            BigDecimal width,
            BigDecimal height,
            BigDecimal offerPrice) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SwitchGradeRequest(
            Long branchId,
            PricingMode targetGrade,
            List<RoomRequest> rooms,
            Long activeDivisionId,
            String activeFloor,
            String activeRoom) {

        public SwitchGradeRequest(Long branchId, PricingMode targetGrade, List<RoomRequest> rooms) {
            this(branchId, targetGrade, rooms, null, null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SwitchGradeItemResponse(
            Long workCategoryId,
            String categoryName,
            Long divisionId,
            String divisionName,
            Long itemId,
            Long materialId,
            String materialName,
            Long materialOptionId,
            Long materialSpecificationId,
            String specification,
            String itemDescription,
            String formula,
            String unit,
            String measurementType,
            BigDecimal coveragePerRoll,
            String imageUrl,
            BigDecimal rate,
            PricingMode pricingMode,
            boolean rateUnavailable,
            String unavailableMessage,
            BigDecimal quantity,
            BigDecimal calculatedSqft,
            BigDecimal lineAmount,
            List<SizeRequest> sizes,
            Long originalMaterialId,
            PricingMode originalPricingMode,
            String originalMaterialName,
            String originalSpecification,
            Integer originalPosition) {

        public SwitchGradeItemResponse(
                Long workCategoryId,
                String categoryName,
                Long divisionId,
                String divisionName,
                Long itemId,
                Long materialId,
                String materialName,
                Long materialOptionId,
                Long materialSpecificationId,
                String specification,
                String itemDescription,
                String formula,
                String unit,
                String measurementType,
                BigDecimal coveragePerRoll,
                String imageUrl,
                BigDecimal rate,
                PricingMode pricingMode,
                boolean rateUnavailable,
                String unavailableMessage,
                BigDecimal quantity,
                BigDecimal calculatedSqft,
                BigDecimal lineAmount,
                List<SizeRequest> sizes,
                Long originalMaterialId,
                PricingMode originalPricingMode,
                String originalMaterialName,
                String originalSpecification) {
            this(workCategoryId, categoryName, divisionId, divisionName, itemId, materialId, materialName, materialOptionId, materialSpecificationId, specification, itemDescription, formula, unit, measurementType, coveragePerRoll, imageUrl, rate, pricingMode, rateUnavailable, unavailableMessage, quantity, calculatedSqft, lineAmount, sizes, originalMaterialId, originalPricingMode, originalMaterialName, originalSpecification, null);
        }

        public SwitchGradeItemResponse(
                Long workCategoryId,
                String categoryName,
                Long divisionId,
                String divisionName,
                Long itemId,
                Long materialId,
                String materialName,
                Long materialOptionId,
                Long materialSpecificationId,
                String specification,
                String itemDescription,
                String formula,
                String unit,
                String measurementType,
                BigDecimal coveragePerRoll,
                String imageUrl,
                BigDecimal rate,
                PricingMode pricingMode,
                boolean rateUnavailable,
                String unavailableMessage,
                BigDecimal quantity,
                BigDecimal calculatedSqft,
                BigDecimal lineAmount,
                List<SizeRequest> sizes) {
            this(workCategoryId, categoryName, divisionId, divisionName, itemId, materialId, materialName, materialOptionId, materialSpecificationId, specification, itemDescription, formula, unit, measurementType, coveragePerRoll, imageUrl, rate, pricingMode, rateUnavailable, unavailableMessage, quantity, calculatedSqft, lineAmount, sizes, null, null, null, null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SwitchGradeRoomResponse(
            String key,
            String floor,
            String room,
            String workDescription,
            List<SwitchGradeItemResponse> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SwitchGradeResponse(
            boolean success,
            PricingMode targetGrade,
            List<SwitchGradeRoomResponse> rooms,
            Map<String, BigDecimal> gradeSubtotals,
            BigDecimal grandTotal,
            List<String> notices) {
    }

    public SwitchGradeResponse switchGrade(SwitchGradeRequest request) {
        if (request == null) return switchGrade(null, PricingMode.PREMIUM, Collections.emptyList());
        return switchGrade(request.branchId(), request.targetGrade(), request.rooms(),
                request.activeDivisionId(), request.activeFloor(), request.activeRoom());
    }

    public SwitchGradeResponse switchGrade(Long branchId, PricingMode targetGrade, List<RoomRequest> rooms) {
        return switchGrade(branchId, targetGrade, rooms, null, null, null);
    }

    public SwitchGradeResponse switchGrade(Long branchId, PricingMode targetGrade, List<RoomRequest> rooms,
                                           Long activeDivisionId, String activeFloor, String activeRoom) {
        if (targetGrade == null) {
            targetGrade = PricingMode.PREMIUM;
        }
        if (rooms == null) {
            rooms = Collections.emptyList();
        }

        Set<PricingMode> existingGrades = new HashSet<>();
        for (RoomRequest r : rooms) {
            if (r != null && r.items() != null) {
                for (ItemRequest it : r.items()) {
                    if (it != null && it.pricingMode() != null) {
                        existingGrades.add(it.pricingMode());
                    }
                }
            }
        }
        boolean isMultiGradeQuote = existingGrades.size() > 1;

        List<SwitchGradeRoomResponse> updatedRooms = new ArrayList<>();
        Map<String, BigDecimal> gradeSubtotals = new LinkedHashMap<>();
        gradeSubtotals.put("PREMIUM", BigDecimal.ZERO);
        gradeSubtotals.put("MIDDLE", BigDecimal.ZERO);
        gradeSubtotals.put("ECONOMY", BigDecimal.ZERO);
        BigDecimal grandTotal = BigDecimal.ZERO;
        List<String> notices = new ArrayList<>();

        Map<Long, Integer> divisionItemCounter = new HashMap<>();

        for (RoomRequest room : rooms) {
            if (room == null) continue;
            List<SwitchGradeItemResponse> updatedItems = new ArrayList<>();
            divisionItemCounter.clear();

            boolean isSameRoom = (activeFloor == null || activeFloor.equalsIgnoreCase(room.floor()))
                    && (activeRoom == null || activeRoom.equalsIgnoreCase(room.room()));

            if (room.items() != null) {
                for (ItemRequest item : room.items()) {
                    if (item == null) continue;

                    boolean isSameDivision = (activeDivisionId != null && activeDivisionId.equals(item.divisionId()));

                    boolean shouldConvert = true;
                    PricingMode itemTargetGrade = targetGrade;

                    Long divId = item.divisionId();
                    if (divId == null && item.materialId() != null && materialRepository != null) {
                        divId = materialRepository.findById(item.materialId())
                                .map(m -> m.getDivision() != null ? m.getDivision().getId() : null)
                                .orElse(null);
                    }

                    int posInDivision = 0;
                    if (divId != null) {
                        posInDivision = divisionItemCounter.getOrDefault(divId, 0);
                        divisionItemCounter.put(divId, posInDivision + 1);
                    }

                    Long lookupMatId = (item.originalMaterialId() != null) ? item.originalMaterialId() : item.materialId();
                    PricingMode originGrade = (item.originalPricingMode() != null) ? item.originalPricingMode() : item.pricingMode();
                    if (originGrade == null) {
                        originGrade = PricingMode.PREMIUM;
                    }

                    String origMatName = item.originalMaterialName();
                    if ((origMatName == null || origMatName.isBlank()) && lookupMatId != null && materialRepository != null) {
                        origMatName = materialRepository.findById(lookupMatId).map(Material::getName).orElse(item.materialName());
                    }
                    if (origMatName == null || origMatName.isBlank()) {
                        origMatName = item.materialName();
                    }
                    String origSpec = item.originalSpecification() != null ? item.originalSpecification() : item.specification();

                    // Determine the item's original 0-based position in its Work Type (Division)
                    Integer position = item.originalPosition();
                    if (position == null && divId != null && originGrade != null && lookupMatId != null) {
                        List<Material> originMats = getActiveMaterialsForDivisionAndGrade(divId, originGrade, branchId);
                        if (originMats != null) {
                            for (int mIdx = 0; mIdx < originMats.size(); mIdx++) {
                                if (lookupMatId.equals(originMats.get(mIdx).getId())) {
                                    position = mIdx;
                                    break;
                                }
                            }
                        }
                    }
                    if (position == null) {
                        position = posInDivision;
                    }

                    ResolvedMaterialRate res = resolveMaterialAndRate(
                            branchId,
                            divId,
                            lookupMatId,
                            item.materialOptionId(),
                            targetGrade,
                            position
                    );

                    boolean available = (res != null && res.available() && res.rate() != null && res.rate().compareTo(BigDecimal.ZERO) > 0);
                    Long matId = available ? res.materialId() : null;
                    Long optId = available ? res.materialOptionId() : null;
                    String gradeLabel = targetGrade == PricingMode.PREMIUM ? "Premium" : (targetGrade == PricingMode.ECONOMY ? "Economy" : "Middle");
                    String matName = available
                            ? res.materialName()
                            : ("Material Unavailable (" + gradeLabel + " Grade)");
                    String spec = (available && res.specification() != null) ? res.specification() : "";
                    Long specId = available ? res.materialSpecificationId() : null;
                    BigDecimal rate = available ? res.rate() : null;
                    String formula = (available && res.formula() != null && !res.formula().isBlank())
                            ? res.formula()
                            : "(L + W) × H";
                    MeasurementType mType = (available && res.measurementType() != null)
                            ? res.measurementType()
                            : (item.measurementType() != null ? item.measurementType() : MeasurementType.SQFT);
                    BigDecimal coverage = available ? res.coveragePerRoll() : item.coveragePerRoll();
                    String imageUrl = available ? res.imageUrl() : (item.imageUrl() != null ? item.imageUrl() : null);
                    BigDecimal quantity = item.quantity() != null ? item.quantity() : BigDecimal.ONE;
                    String unit = available ? res.unit() : (mType.isRoll() ? "Roll" : (mType == MeasurementType.QUANTITY ? "Quantity" : (mType == MeasurementType.FILES ? "Files" : "Sq.ft")));

                    String catName = "";
                    if (item.workCategoryId() != null && workCategoryRepository != null) {
                        catName = workCategoryRepository.findById(item.workCategoryId()).map(WorkCategory::getName).orElse("");
                    }
                    String divName = "";
                    if (item.divisionId() != null && divisionRepository != null) {
                        Division d = divisionRepository.findById(item.divisionId()).orElse(null);
                        if (d != null) {
                            divName = d.getName() != null ? d.getName() : "";
                            if ((catName == null || catName.isBlank()) && d.getWorkCategory() != null) {
                                catName = d.getWorkCategory().getName() != null ? d.getWorkCategory().getName() : "";
                            }
                        }
                    }

                    List<SizeRequest> newSizes = new ArrayList<>();
                    BigDecimal itemSqft = BigDecimal.ZERO;
                    BigDecimal lineAmount = BigDecimal.ZERO;

                    if (mType.isSqft()) {
                        if (item.sizes() != null) {
                            for (SizeRequest s : item.sizes()) {
                                if (s == null) continue;
                                BigDecimal l = s.length() != null ? s.length() : BigDecimal.ZERO;
                                BigDecimal w = s.width() != null ? s.width() : BigDecimal.ZERO;
                                BigDecimal h = s.height() != null ? s.height() : BigDecimal.ZERO;
                                BigDecimal sqft = quotationItemSizeService != null
                                        ? quotationItemSizeService.calculateSqft(formula, l, w, h, "in")
                                        : BigDecimal.ZERO;
                                BigDecimal rowOffer = (rate != null) ? money(sqft.multiply(rate)) : BigDecimal.ZERO;
                                itemSqft = itemSqft.add(sqft);
                                lineAmount = lineAmount.add(rowOffer);
                                newSizes.add(new SizeRequest(l, w, h, rowOffer));
                            }
                        }
                    } else if (mType.isRoll()) {
                        BigDecimal totalWallArea = BigDecimal.ZERO;
                        if (item.sizes() != null) {
                            for (SizeRequest s : item.sizes()) {
                                if (s == null) continue;
                                BigDecimal l = s.length() != null ? s.length() : BigDecimal.ZERO;
                                BigDecimal w = s.width() != null ? s.width() : BigDecimal.ZERO;
                                BigDecimal h = s.height() != null ? s.height() : BigDecimal.ZERO;
                                BigDecimal sqft = quotationItemSizeService != null
                                        ? quotationItemSizeService.calculateSqft(formula, l, w, h, "in")
                                        : BigDecimal.ZERO;
                                totalWallArea = totalWallArea.add(sqft);
                                newSizes.add(new SizeRequest(l, w, h, BigDecimal.ZERO));
                            }
                        }
                        BigDecimal effCoverage = (coverage != null && coverage.compareTo(BigDecimal.ZERO) > 0)
                                ? coverage : new BigDecimal("50.00");
                        long rollCount = 0;
                        if (totalWallArea.compareTo(BigDecimal.ZERO) > 0) {
                            rollCount = (long) Math.ceil(totalWallArea.doubleValue() / effCoverage.doubleValue());
                        }
                        quantity = BigDecimal.valueOf(Math.max(1, rollCount));
                        itemSqft = totalWallArea;
                        lineAmount = (rate != null) ? money(quantity.multiply(rate)) : BigDecimal.ZERO;
                    } else {
                        // QUANTITY or FILES
                        quantity = (item.quantity() != null && item.quantity().compareTo(BigDecimal.ZERO) > 0)
                                ? item.quantity() : BigDecimal.ONE;
                        lineAmount = (rate != null) ? money(quantity.multiply(rate)) : BigDecimal.ZERO;
                    }

                    String unavailMsg = null;
                    if (shouldConvert && !available) {
                        unavailMsg = (res != null && res.message() != null)
                                ? res.message()
                                : "Rate unavailable for " + targetGrade + " Grade.";
                        notices.add(unavailMsg);
                    }

                    String gradeKey = itemTargetGrade.name();
                    gradeSubtotals.put(gradeKey, gradeSubtotals.getOrDefault(gradeKey, BigDecimal.ZERO).add(lineAmount));
                    grandTotal = grandTotal.add(lineAmount);

                    updatedItems.add(new SwitchGradeItemResponse(
                            item.workCategoryId(),
                            catName,
                            item.divisionId(),
                            divName,
                            item.itemId(),
                            matId,
                            matName,
                            optId,
                            specId,
                            spec,
                            item.itemDescription(),
                            formula,
                            unit,
                            mType.name(),
                            coverage,
                            imageUrl,
                            rate,
                            itemTargetGrade,
                            !available,
                            unavailMsg,
                            quantity,
                            itemSqft,
                            lineAmount,
                            newSizes,
                            lookupMatId,
                            originGrade,
                            origMatName,
                            origSpec,
                            position
                    ));
                }
            }

            String roomKey = (room.floor() != null ? room.floor() : "Ground Floor") + "||" + (room.room() != null ? room.room() : "1");
            updatedRooms.add(new SwitchGradeRoomResponse(
                    roomKey,
                    room.floor(),
                    room.room(),
                    room.workDescription(),
                    updatedItems
            ));
        }

        return new SwitchGradeResponse(
                true,
                targetGrade,
                updatedRooms,
                gradeSubtotals,
                grandTotal,
                notices
        );
    }
}
