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
            boolean available,
            String message
    ) {
        public static ResolvedMaterialRate unavailable(String message) {
            return new ResolvedMaterialRate(null, null, null, null, null, null, null, null, false, message);
        }

        public static ResolvedMaterialRate unavailable(Long materialId, String materialName, String message) {
            return new ResolvedMaterialRate(materialId, null, materialName, null, null, null, null, null, false, message);
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

        Branch branch = null;
        if (branchId != null) {
            branch = branchRepository.findById(branchId).orElse(null);
        }

        Material matchedMat = null;
        MaterialOption matchedOpt = null;
        Pricing matchedPricing = null;
        BigDecimal matchedRate = null;

        boolean matIsDifferentGrade = (mat != null && mat.getPricingGrade() != null && mat.getPricingGrade() != targetGrade);

        // 1. Direct option match if material is not explicitly tagged with a different grade
        if (!matIsDifferentGrade && opt != null) {
            Pricing p = pricingRepository.findByMaterialOptionIdAndPricingMode(opt.getId(), targetGrade).orElse(null);
            if (p != null && Boolean.TRUE.equals(p.getActive())) {
                BigDecimal r = resolveRateFromPricing(branchId, p);
                if (r != null) {
                    matchedMat = mat;
                    matchedOpt = opt;
                    matchedPricing = p;
                    matchedRate = r;
                }
            }
        }

        // 2. Direct material match across all options or material rate
        if (matchedRate == null && !matIsDifferentGrade && mat != null) {
            List<MaterialOption> opts = materialOptionRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(mat.getId());
            if (opts.isEmpty()) {
                opts = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(mat.getId());
            }
            for (MaterialOption o : opts) {
                Pricing p = pricingRepository.findByMaterialOptionIdAndPricingMode(o.getId(), targetGrade).orElse(null);
                if (p != null && Boolean.TRUE.equals(p.getActive())) {
                    BigDecimal r = resolveRateFromPricing(branchId, p);
                    if (r != null) {
                        matchedMat = mat;
                        matchedOpt = o;
                        matchedPricing = p;
                        matchedRate = r;
                        break;
                    }
                }
            }
            if (matchedRate == null && mat.getPricingGrade() == targetGrade && mat.getRate() != null) {
                matchedMat = mat;
                matchedRate = money(mat.getRate());
            }
        }

        // 3. Cross-grade counterpart match in the same division
        if (matchedRate == null && divisionId != null && mat != null) {
            List<Material> candidates = materialRepository.findActiveByDivisionIdAndPricingGrade(divisionId, targetGrade);
            if (candidates.isEmpty()) {
                candidates = materialRepository.findAllByDivisionIdAndPricingGrade(divisionId, targetGrade);
            }

            Material counterpart = findMatchingCounterpart(mat, candidates);
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
                        if (r != null) {
                            matchedMat = counterpart;
                            matchedOpt = bestCpOpt;
                            matchedPricing = p;
                            matchedRate = r;
                        }
                    }
                }

                if (matchedRate == null && counterpart.getRate() != null) {
                    matchedMat = counterpart;
                    matchedOpt = bestCpOpt;
                    matchedRate = money(counterpart.getRate());
                }
            }
        }

        // 4. Fallback check on mat's options even if mat had a different pricingGrade tag
        if (matchedRate == null && mat != null) {
            List<MaterialOption> opts = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(mat.getId());
            for (MaterialOption o : opts) {
                Pricing p = pricingRepository.findByMaterialOptionIdAndPricingMode(o.getId(), targetGrade).orElse(null);
                if (p != null && Boolean.TRUE.equals(p.getActive())) {
                    BigDecimal r = resolveRateFromPricing(branchId, p);
                    if (r != null) {
                        matchedMat = mat;
                        matchedOpt = o;
                        matchedPricing = p;
                        matchedRate = r;
                        break;
                    }
                }
            }
        }

        if (matchedRate == null) {
            String matName = mat != null ? mat.getName() : "selected material";
            String branchName = branch != null ? branch.getName() : "the selected branch";
            return ResolvedMaterialRate.unavailable(
                    mat != null ? mat.getId() : null,
                    matName,
                    "No rate has been configured for branch '" + branchName + "' for material '" + matName + "' under " + targetGrade.name() + " pricing grade."
            );
        }

        Material effectiveMat = (matchedMat != null) ? matchedMat : mat;
        MaterialOption effectiveOpt = (matchedOpt != null) ? matchedOpt : opt;
        Pricing effectivePricing = matchedPricing;

        String formula = (effectiveMat != null && effectiveMat.getFormula() != null && !effectiveMat.getFormula().isBlank() && !"(L + W) × H".equals(effectiveMat.getFormula()))
                ? effectiveMat.getFormula()
                : ((effectivePricing != null && effectivePricing.getFormula() != null && !effectivePricing.getFormula().isBlank())
                        ? effectivePricing.getFormula()
                        : ((effectiveOpt != null && effectiveOpt.getFormula() != null && !effectiveOpt.getFormula().isBlank())
                                ? effectiveOpt.getFormula()
                                : "(L + W) × H"));

        String unit = (effectivePricing != null && effectivePricing.getUnit() != null && !effectivePricing.getUnit().isBlank())
                ? effectivePricing.getUnit()
                : ((effectiveMat != null && effectiveMat.getUnit() != null) ? effectiveMat.getUnit() : "Sq.ft");

        MeasurementType measurementType = (effectiveMat != null && effectiveMat.getMeasurementType() != null)
                ? effectiveMat.getMeasurementType()
                : MeasurementType.SQFT;

        String imageUrl = effectiveMat != null ? effectiveMat.getImageUrl() : null;
        String matName = effectiveMat != null ? effectiveMat.getName() : "";

        return new ResolvedMaterialRate(
                effectiveMat != null ? effectiveMat.getId() : null,
                effectiveOpt != null ? effectiveOpt.getId() : null,
                matName,
                matchedRate,
                formula,
                unit,
                measurementType,
                imageUrl,
                true,
                null
        );
    }

    private BigDecimal resolveRateFromPricing(Long branchId, Pricing pricing) {
        if (pricing == null || !Boolean.TRUE.equals(pricing.getActive())) {
            return null;
        }
        if (branchId != null && districtPricingRepository != null) {
            DistrictPricing dp = districtPricingRepository.findByBranchIdAndPricingId(branchId, pricing.getId()).orElse(null);
            if (dp != null && dp.getRate() != null && Boolean.TRUE.equals(dp.getActive())) {
                return money(dp.getRate());
            }
            boolean hasBranchRates = districtPricingRepository.existsByPricingIdAndActiveTrue(pricing.getId());
            if (hasBranchRates) {
                return null;
            }
        }
        if (pricing.getRate() != null) {
            return money(pricing.getRate());
        }
        return null;
    }

    private Material findMatchingCounterpart(Material source, List<Material> candidates) {
        if (source == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        String sourceName = source.getName() != null ? source.getName().trim() : "";

        // 1. Exact name match (case-insensitive)
        for (Material c : candidates) {
            if (c.getName() != null && c.getName().trim().equalsIgnoreCase(sourceName)) {
                return c;
            }
        }

        // 2. Normalized name match
        String normSource = normalizeMaterialName(sourceName);
        for (Material c : candidates) {
            if (c.getName() != null && normalizeMaterialName(c.getName()).equalsIgnoreCase(normSource)) {
                return c;
            }
        }

        // 3. Substring match
        for (Material c : candidates) {
            if (c.getName() != null) {
                String normC = normalizeMaterialName(c.getName());
                if (!normC.isBlank() && !normSource.isBlank() && (normC.contains(normSource) || normSource.contains(normC))) {
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
                .replaceAll("[^a-z0-9]", "");
    }

    @Transactional(readOnly = true)
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
                    request.pricingMode(),
                    request.customerRequirements(),
                    request.notes(),
                    request.rooms(),
                    request.accessoriesAmount(),
                    request.travelCharge(),
                    request.otherCharges(),
                    request.discountAmount(),
                    request.gstPercentage(),
                    request.offerPercentage(),
                    request.showSqftSize()
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
        // Customer resolution & authorization
        // -----------------------------------------------------

        Customer customer =
                resolveCustomer(
                        request.customer(),
                        branch,
                        loggedInUser
                );

        // -----------------------------------------------------
        // Quotation lookup or creation
        // -----------------------------------------------------

        Quotation quotation;
        if (request.quotationId() != null) {
            quotation = quotationRepository.findById(request.quotationId())
                    .orElseGet(Quotation::new);
            if (quotation.getId() != null) {
                List<QuotationRoom> oldRooms =
                        quotationRoomService.getActiveByQuotationId(quotation.getId());
                for (QuotationRoom oldRoom : oldRooms) {
                    quotationRoomService.deactivate(oldRoom.getId());
                }
            } else {
                quotation.setQuotationNumber(nextQuotationNumber());
            }
        } else {
            quotation = new Quotation();
            quotation.setQuotationNumber(nextQuotationNumber());
        }

        quotation.setCustomer(
                customer
        );

        quotation.setEmployee(
                loggedInUser
        );

        quotation.setBranch(
                branch
        );

        if (quotation.getId() != null) {
            // Existing quotation being edited: preserve original quotationDate and validUntil
            if (quotation.getQuotationDate() == null) {
                quotation.setQuotationDate(
                        request.quotationDate() == null ? LocalDate.now() : request.quotationDate()
                );
            }
            if (quotation.getValidUntil() == null && quotation.getQuotationDate() != null) {
                quotation.setValidUntil(quotation.getQuotationDate().plusDays(365));
            }
        } else {
            // New quotation creation
            LocalDate qDate = request.quotationDate() == null ? LocalDate.now() : request.quotationDate();
            quotation.setQuotationDate(qDate);
            quotation.setValidUntil(qDate.plusDays(365));
        }

        quotation.setPricingMode(
                request.pricingMode()
        );

        boolean hasRooms = request.rooms() != null && !request.rooms().isEmpty();
        quotation.setStatus(
                hasRooms ? QuotationStatus.WORKING : QuotationStatus.PENDING
        );

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

        Quotation savedQuotation =
                quotationRepository.save(
                        quotation
                );

        // =====================================================
        // ROOMS
        // =====================================================

        for (RoomRequest roomRequest :
                request.rooms()) {

            QuotationRoom room =
                    quotationRoomService.create(
                            savedQuotation,
                            required(
                                    roomRequest.floor(),
                                    "Floor"
                            ),
                            required(
                                    roomRequest.room(),
                                    "Room"
                            ),
                            roomRequest.workDescription()
                    );

            // =================================================
            // ITEMS
            // =================================================

            for (ItemRequest itemRequest :
                    roomRequest.items()) {

                WorkCategory category =
                        workCategoryRepository
                                .findById(
                                        itemRequest.workCategoryId()
                                )
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Work category not found."
                                        )
                                );

                Division division =
                        divisionRepository
                                .findById(
                                        itemRequest.divisionId()
                                )
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Division not found."
                                        )
                                );

                Material material =
                        materialRepository
                                .findById(
                                        itemRequest.materialId()
                                )
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Material not found."
                                        )
                                );

                Item item = null;
                if (itemRequest.itemId() != null) {
                    item = itemRepository
                            .findById(itemRequest.itemId())
                            .orElse(null);
                }
                if (item == null && material.getItem() != null) {
                    item = material.getItem();
                }
                if (item == null) {
                    item = itemRepository
                            .findByDivisionIdAndActiveTrueOrderByDisplayOrderAsc(division.getId())
                            .stream()
                            .findFirst()
                            .orElse(null);
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

                // -------------------------------------------------
                // Validate hierarchy
                // -------------------------------------------------

                if (!option.getMaterial()
                        .getId()
                        .equals(material.getId())) {

                    throw new IllegalArgumentException(
                            "Material option does not belong to the selected material."
                    );
                }

                if (!division.getWorkCategory()
                        .getId()
                        .equals(category.getId())) {

                    throw new IllegalArgumentException(
                            "Division does not belong to the selected work category."
                    );
                }

                MaterialSpecification materialSpecification = null;
                if (itemRequest.materialSpecificationId() != null) {
                    materialSpecification = materialSpecificationRepository
                            .findById(itemRequest.materialSpecificationId())
                            .orElse(null);
                    if (materialSpecification != null) {
                        if (materialSpecification.getMaterial() != null
                                && !materialSpecification.getMaterial().getId().equals(material.getId())) {
                            materialSpecification = null;
                        }
                        if (materialSpecification != null && materialSpecification.getMaterialOption() != null
                                && option != null && !materialSpecification.getMaterialOption().getId().equals(option.getId())) {
                            materialSpecification = null;
                        }
                    }
                }

                // -------------------------------------------------
                // Resolve measurement type & effective values
                // -------------------------------------------------

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

                // -------------------------------------------------
                // Resolve rate for this item:
                // For FILES unit, allow quotation-specific edited rate.
                // For other units, resolve from pricing configuration.
                // -------------------------------------------------

                BigDecimal rate;
                if (effectiveType.isFiles()) {
                    if (itemRequest.rate() != null) {
                        if (itemRequest.rate().compareTo(BigDecimal.ZERO) < 0) {
                            throw new IllegalArgumentException("Rate for Files unit must be a valid, non-negative value.");
                        }
                        rate = money(itemRequest.rate());
                    } else {
                        ResolvedMaterialRate resolved = resolveMaterialAndRate(
                                request.branchId(),
                                division.getId(),
                                material.getId(),
                                option != null ? option.getId() : null,
                                request.pricingMode()
                        );
                        if (resolved.available() && resolved.rate() != null) {
                            rate = resolved.rate();
                            if (resolved.materialId() != null) {
                                material = materialRepository.findById(resolved.materialId()).orElse(material);
                            }
                            if (resolved.materialOptionId() != null) {
                                option = materialOptionRepository.findById(resolved.materialOptionId()).orElse(option);
                            }
                        } else if (material.getRate() != null && material.getRate().compareTo(BigDecimal.ZERO) >= 0) {
                            rate = money(material.getRate());
                        } else {
                            throw new IllegalArgumentException(resolved.message());
                        }
                    }
                } else {
                    ResolvedMaterialRate resolved = resolveMaterialAndRate(
                            request.branchId(),
                            division.getId(),
                            material.getId(),
                            option != null ? option.getId() : null,
                            request.pricingMode()
                    );
                    if (!resolved.available() || resolved.rate() == null) {
                        throw new IllegalArgumentException(resolved.message());
                    }
                    rate = resolved.rate();
                    if (resolved.materialId() != null) {
                        material = materialRepository.findById(resolved.materialId()).orElse(material);
                    }
                    if (resolved.materialOptionId() != null) {
                        option = materialOptionRepository.findById(resolved.materialOptionId()).orElse(option);
                    }
                }

                Pricing itemPricing = (option != null)
                        ? pricingRepository.findByMaterialOptionIdAndPricingMode(option.getId(), request.pricingMode()).orElse(null)
                        : null;

                String itemFormula = (material.getFormula() != null && !material.getFormula().isBlank())
                        ? material.getFormula()
                        : ((itemPricing != null && itemPricing.getFormula() != null && !itemPricing.getFormula().isBlank())
                                ? itemPricing.getFormula()
                                : (option.getFormula() != null && !option.getFormula().isBlank() ? option.getFormula() : "(L + W) × H"));

                String itemUnit = (itemPricing != null && itemPricing.getUnit() != null) ? itemPricing.getUnit() : "SQFT";

                // =================================================
                // ONE QuotationItem
                // MANY QuotationItemSize
                // =================================================

                BigDecimal totalSqft =
                        BigDecimal.ZERO;

                BigDecimal totalAmount =
                        BigDecimal.ZERO;

                BigDecimal totalOfferPrice =
                        BigDecimal.ZERO;

                BigDecimal firstLength = null;
                BigDecimal firstWidth = null;
                BigDecimal firstHeight = null;

                BigDecimal effectiveQuantity = (itemRequest.quantity() != null && itemRequest.quantity().compareTo(BigDecimal.ZERO) > 0)
                        ? itemRequest.quantity()
                        : BigDecimal.ONE;

                String effectiveImageUrl = (itemRequest.imageUrl() != null && !itemRequest.imageUrl().isBlank())
                        ? itemRequest.imageUrl()
                        : (material != null ? material.getImageUrl() : null);

                // -------------------------------------------------
                // First pass:
                // Calculate all sizes and totals
                // -------------------------------------------------

                if (effectiveType.isSqft()) {
                    if (itemRequest.sizes() != null) {
                        for (int sizeIndex = 0;
                             sizeIndex < itemRequest.sizes().size();
                             sizeIndex++) {

                            SizeRequest size =
                                    itemRequest.sizes()
                                            .get(sizeIndex);

                            String normFormula = com.example.pravin_quotation.util.CalculationFormula.normalizeFormula(itemFormula);
                            boolean requiresW = !normFormula.equals(com.example.pravin_quotation.util.CalculationFormula.FORMULA_L_X_H);
                            boolean requiresH = !normFormula.equals(com.example.pravin_quotation.util.CalculationFormula.FORMULA_L_X_W);

                            BigDecimal length = positive(size.length(), "Length");
                            BigDecimal width = requiresW ? positive(size.width(), "Width") : (size.width() != null ? size.width() : BigDecimal.ZERO);
                            BigDecimal height = requiresH ? positive(size.height(), "Height") : (size.height() != null ? size.height() : BigDecimal.ZERO);
                            com.example.pravin_quotation.util.CalculationFormula.validateDimensions(normFormula, length, width, height);

                            // Save first size in parent item
                            // for backward compatibility.
                            if (sizeIndex == 0) {
                                firstLength = length;
                                firstWidth = width;
                                firstHeight = height;
                            }

                            BigDecimal sqft =
                                    quotationItemSizeService
                                            .calculateSqft(
                                                    itemFormula,
                                                    length,
                                                    width,
                                                    height,
                                                    "in"
                                            );

                            BigDecimal amount =
                                    money(
                                            sqft.multiply(rate)
                                    );

                            BigDecimal offerPrice;

                            if (size.offerPrice() == null) {
                                offerPrice = amount;
                            } else {
                                offerPrice =
                                        nonNegative(
                                                size.offerPrice(),
                                                "Offer price"
                                        );
                            }

                            totalSqft =
                                    totalSqft.add(sqft);

                            totalAmount =
                                    totalAmount.add(amount);

                            totalOfferPrice =
                                    totalOfferPrice.add(
                                            offerPrice
                                    );
                        }
                    }
                } else {
                    // QUANTITY or FILES based item:
                    totalAmount = money(effectiveQuantity.multiply(rate));
                    BigDecimal offerPrice = totalAmount;
                    if (itemRequest.sizes() != null && !itemRequest.sizes().isEmpty() && itemRequest.sizes().get(0).offerPrice() != null) {
                        offerPrice = nonNegative(itemRequest.sizes().get(0).offerPrice(), "Offer price");
                    }
                    totalOfferPrice = offerPrice;
                    totalSqft = BigDecimal.ZERO;
                }

                totalSqft =
                        money(totalSqft);

                totalAmount =
                        money(totalAmount);

                totalOfferPrice =
                        money(totalOfferPrice);

                // =================================================
                // Create ONE parent QuotationItem
                // =================================================

                String specText = itemRequest.specification();
                if ((specText == null || specText.isBlank()) && materialSpecification != null) {
                    specText = materialSpecification.getName();
                }

                QuotationItem quotationItem =
                        quotationItemService.create(
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

                                effectiveType.isSqft() ? itemFormula : null,

                                specText,
                                effectiveType,
                                effectiveQuantity,
                                effectiveImageUrl
                        );

                // =================================================
                // Create child QuotationItemSize rows (only for Sq.ft)
                // =================================================

                if (effectiveType.isSqft() && itemRequest.sizes() != null) {
                    for (int sizeIndex = 0;
                         sizeIndex < itemRequest.sizes().size();
                         sizeIndex++) {

                        SizeRequest size =
                                itemRequest.sizes()
                                        .get(sizeIndex);

                        String normFormula = com.example.pravin_quotation.util.CalculationFormula.normalizeFormula(itemFormula);
                        boolean requiresW = !normFormula.equals(com.example.pravin_quotation.util.CalculationFormula.FORMULA_L_X_H);
                        boolean requiresH = !normFormula.equals(com.example.pravin_quotation.util.CalculationFormula.FORMULA_L_X_W);

                        BigDecimal length = positive(size.length(), "Length");
                        BigDecimal width = requiresW ? positive(size.width(), "Width") : (size.width() != null ? size.width() : BigDecimal.ZERO);
                        BigDecimal height = requiresH ? positive(size.height(), "Height") : (size.height() != null ? size.height() : BigDecimal.ZERO);
                        com.example.pravin_quotation.util.CalculationFormula.validateDimensions(normFormula, length, width, height);

                        BigDecimal sqft =
                                quotationItemSizeService
                                        .calculateSqft(
                                                itemFormula,
                                                length,
                                                width,
                                                height,
                                                "in"
                                        );

                        BigDecimal amount =
                                money(
                                        sqft.multiply(rate)
                                );

                        BigDecimal offerPrice;

                        if (size.offerPrice() == null) {
                            offerPrice = amount;
                        } else {
                            offerPrice =
                                    nonNegative(
                                            size.offerPrice(),
                                            "Offer price"
                                    );
                        }

                        quotationItemSizeService.create(
                                quotationItem,
                                length,
                                width,
                                height,
                                offerPrice,
                                sizeIndex + 1,
                                "in"
                        );
                    }
                }
            }
        }

        // -----------------------------------------------------
        // Final quotation calculation
        // -----------------------------------------------------

        return quotationCalculationService
                .calculateQuotation(
                        savedQuotation.getId()
                );
    }

    // =========================================================
    // CUSTOMER RESOLUTION & AUTHORIZATION
    // =========================================================

    private Customer resolveCustomer(
            CustomerRequest request,
            Branch branch,
            User loggedInUser) {

        if (request == null) {
            throw new IllegalArgumentException("Customer details are required.");
        }

        // Case 1: Existing Customer by ID
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

        // Case 2: New Customer creation
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

                if (type.isSqft()) {
                    if (item.sizes() == null || item.sizes().isEmpty()) {
                        throw new IllegalArgumentException(
                                "Each Sq.ft item must contain at least one size."
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
            this(quotationId, customer, branchId, quotationDate, pricingMode, customerRequirements, notes, rooms,
                    accessoriesAmount, travelCharge, otherCharges, discountAmount, gstPercentage, offerPercentage, true);
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
            this(quotationId, customer, branchId, quotationDate, pricingMode, customerRequirements, notes, rooms,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true);
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
            BigDecimal rate) {

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
            this(workCategoryId, divisionId, itemId, materialId, materialOptionId, materialSpecificationId, itemDescription, specification, sizes, measurementType, quantity, imageUrl, null);
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
            this(workCategoryId, divisionId, itemId, materialId, materialOptionId, materialSpecificationId, itemDescription, specification, sizes, null, null, null, null);
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
            this(workCategoryId, divisionId, itemId, materialId, materialOptionId, null, itemDescription, specification, sizes, null, null, null, null);
        }
    }

    public record SizeRequest(
            BigDecimal length,
            BigDecimal width,
            BigDecimal height,
            BigDecimal offerPrice) {
    }
}