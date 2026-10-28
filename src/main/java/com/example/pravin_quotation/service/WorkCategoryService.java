package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.util.CalculationFormula;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class WorkCategoryService {

    private final WorkCategoryRepository workCategoryRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final QuotationRepository quotationRepository;
    private final DivisionRepository divisionRepository;
    private final ItemRepository itemRepository;
    private final MaterialRepository materialRepository;
    private final MaterialOptionRepository materialOptionRepository;
    private final PricingRepository pricingRepository;
    private final MaterialSpecificationRepository materialSpecificationRepository;
    private final BranchRepository branchRepository;
    private final DistrictPricingRepository districtPricingRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public WorkCategoryService(
            WorkCategoryRepository workCategoryRepository,
            QuotationItemRepository quotationItemRepository,
            QuotationRepository quotationRepository,
            DivisionRepository divisionRepository,
            ItemRepository itemRepository,
            MaterialRepository materialRepository,
            MaterialOptionRepository materialOptionRepository,
            PricingRepository pricingRepository,
            MaterialSpecificationRepository materialSpecificationRepository,
            BranchRepository branchRepository,
            DistrictPricingRepository districtPricingRepository) {

        this.workCategoryRepository = workCategoryRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.quotationRepository = quotationRepository;
        this.divisionRepository = divisionRepository;
        this.itemRepository = itemRepository;
        this.materialRepository = materialRepository;
        this.materialOptionRepository = materialOptionRepository;
        this.pricingRepository = pricingRepository;
        this.materialSpecificationRepository = materialSpecificationRepository;
        this.branchRepository = branchRepository;
        this.districtPricingRepository = districtPricingRepository;
    }

    public WorkCategoryService(
            WorkCategoryRepository workCategoryRepository,
            QuotationItemRepository quotationItemRepository,
            QuotationRepository quotationRepository,
            DivisionRepository divisionRepository,
            ItemRepository itemRepository,
            MaterialRepository materialRepository,
            MaterialOptionRepository materialOptionRepository,
            PricingRepository pricingRepository,
            MaterialSpecificationRepository materialSpecificationRepository) {

        this(workCategoryRepository, quotationItemRepository, quotationRepository,
                divisionRepository, itemRepository, materialRepository,
                materialOptionRepository, pricingRepository, materialSpecificationRepository,
                null, null);
    }


    public List<WorkCategory> getAllCategories() {

        return workCategoryRepository
                .findNormalCategories();
    }


    public List<WorkCategory> getActiveCategories() {

        return workCategoryRepository
                .findByActiveTrueOrderByDisplayOrderAsc();
    }


    public List<WorkCategory> getArchivedCategories() {

        return workCategoryRepository
                .findArchivedCategories();
    }


    public long getArchivedCategoryCount() {

        return workCategoryRepository
                .countArchivedCategories();
    }


    public WorkCategory getCategoryById(Long id) {

        return workCategoryRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Work category not found"
                        )
                );
    }


    public WorkCategory createCategory(
            String name,
            String description,
            String icon,
            Integer displayOrder) {

        validateName(name);

        String categoryName = name.trim().toUpperCase();

        if (workCategoryRepository
                .existsByNameIgnoreCase(categoryName)) {

            throw new IllegalArgumentException(
                    "Work category already exists"
            );
        }

        WorkCategory category = new WorkCategory();

        category.setName(categoryName);

        category.setDescription(
                cleanValue(description)
        );

        String iconVal = cleanValue(icon);
        if (iconVal == null) {
            iconVal = extractLeadingEmoji(categoryName);
        }
        category.setIcon(iconVal);

        category.setActive(true);

        category.setDisplayOrder(
                displayOrder == null ? 0 : displayOrder
        );

        return workCategoryRepository.save(category);
    }


    public WorkCategory updateCategory(
            Long id,
            String name,
            String description,
            String icon,
            Integer displayOrder) {

        WorkCategory category =
                getCategoryById(id);

        validateName(name);

        String categoryName = name.trim().toUpperCase();

        if (workCategoryRepository
                .existsByNameIgnoreCaseAndIdNot(
                        categoryName,
                        id)) {

            throw new IllegalArgumentException(
                    "Another work category with this name already exists"
            );
        }

        category.setName(categoryName);

        category.setDescription(
                cleanValue(description)
        );

        String updateIconVal = cleanValue(icon);
        if (updateIconVal == null) {
            updateIconVal = extractLeadingEmoji(categoryName);
        }
        category.setIcon(updateIconVal);

        category.setDisplayOrder(
                displayOrder == null ? 0 : displayOrder
        );

        return workCategoryRepository.save(category);
    }


    public void toggleCategoryStatus(Long id) {

        WorkCategory category =
                getCategoryById(id);

        category.setActive(
                !Boolean.TRUE.equals(
                        category.getActive()
                )
        );

        workCategoryRepository.save(category);
    }


    @Transactional
    public String deleteCategory(Long id) {
        WorkCategory category = getCategoryById(id);
        String catName = category.getName();

        // 1. Check if category or any child division, item, material, option, or spec is linked to quotations
        boolean usedInQuotations = quotationItemRepository.countByWorkCategoryId(id) > 0;

        List<Division> divisions = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(id);
        if (!usedInQuotations) {
            for (Division div : divisions) {
                if (quotationItemRepository.countByDivisionId(div.getId()) > 0
                        || quotationRepository.countByDivisionId(div.getId()) > 0) {
                    usedInQuotations = true;
                    break;
                }
                List<Item> items = itemRepository.findByDivisionIdOrderByDisplayOrderAsc(div.getId());
                for (Item item : items) {
                    if (!quotationItemRepository.findByItemIdOrderByCreatedAtDesc(item.getId()).isEmpty()) {
                        usedInQuotations = true;
                        break;
                    }
                }
                if (usedInQuotations) break;

                List<Material> materials = materialRepository.findAllByDivisionId(div.getId());
                for (Material mat : materials) {
                    if (quotationItemRepository.countByMaterialId(mat.getId()) > 0) {
                        usedInQuotations = true;
                        break;
                    }
                    for (MaterialOption opt : materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(mat.getId())) {
                        if (quotationItemRepository.countByMaterialOptionId(opt.getId()) > 0) {
                            usedInQuotations = true;
                            break;
                        }
                        for (MaterialSpecification spec : materialSpecificationRepository.findByMaterialOptionIdOrderByDisplayOrderAsc(opt.getId())) {
                            if (quotationItemRepository.countByMaterialSpecificationId(spec.getId()) > 0) {
                                usedInQuotations = true;
                                break;
                            }
                        }
                        if (usedInQuotations) break;
                    }
                    if (usedInQuotations) break;

                    for (MaterialSpecification spec : materialSpecificationRepository.findByMaterialIdOrderByDisplayOrderAsc(mat.getId())) {
                        if (quotationItemRepository.countByMaterialSpecificationId(spec.getId()) > 0) {
                            usedInQuotations = true;
                            break;
                        }
                    }
                    if (usedInQuotations) break;
                }
                if (usedInQuotations) break;
            }
        }

        if (usedInQuotations) {
            category.setArchived(true);
            category.setActive(false);
            workCategoryRepository.saveAndFlush(category);
            for (Division div : divisions) {
                div.setActive(false);
                divisionRepository.saveAndFlush(div);
            }
            return "Work category '" + catName + "' is referenced by existing quotations and cannot be permanently deleted. It has been archived and removed from the active work categories list to protect quotation history.";
        }

        // 2. Safe to permanently delete from database
        try {
            for (Division div : divisions) {
                List<Material> materials = materialRepository.findAllByDivisionId(div.getId());
                for (Material mat : materials) {
                    mat.getDivisions().remove(div);
                    if (mat.getDivisions().isEmpty() && (mat.getItem() == null || div.getId().equals(mat.getItem().getDivision().getId()))) {
                        for (MaterialOption opt : materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(mat.getId())) {
                            pricingRepository.deleteAll(pricingRepository.findByMaterialOptionIdOrderByPricingModeAsc(opt.getId()));
                            materialSpecificationRepository.deleteAll(materialSpecificationRepository.findByMaterialOptionIdOrderByDisplayOrderAsc(opt.getId()));
                            materialOptionRepository.delete(opt);
                        }
                        materialSpecificationRepository.deleteAll(materialSpecificationRepository.findByMaterialIdOrderByDisplayOrderAsc(mat.getId()));
                        materialRepository.delete(mat);
                    } else {
                        materialRepository.save(mat);
                    }
                }

                List<Item> items = itemRepository.findByDivisionIdOrderByDisplayOrderAsc(div.getId());
                if (!items.isEmpty()) {
                    itemRepository.deleteAll(items);
                }

                divisionRepository.delete(div);
            }

            workCategoryRepository.delete(category);
            workCategoryRepository.flush();
            return "Work category '" + catName + "' has been permanently deleted from the database.";
        } catch (Exception e) {
            category.setArchived(true);
            category.setActive(false);
            workCategoryRepository.saveAndFlush(category);
            return "Work category '" + catName + "' could not be permanently removed due to database constraints, but has been safely archived.";
        }
    }


    @Transactional
    public String unarchiveCategory(Long id) {
        WorkCategory category = getCategoryById(id);
        String catName = category.getName();

        if (!category.isArchived()) {
            return "Work category '" + catName + "' is already active and not archived.";
        }

        category.setArchived(false);
        category.setActive(true);
        workCategoryRepository.saveAndFlush(category);

        List<Division> divisions = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(id);
        for (Division div : divisions) {
            div.setActive(true);
            divisionRepository.saveAndFlush(div);
        }

        return "Work category '" + catName + "' has been successfully unarchived and restored to active work categories.";
    }

    public java.util.Map<String, Object> getCategoryWorkTypes(Long categoryId) {
        WorkCategory category = getCategoryById(categoryId);
        String catName = category.getName() != null ? category.getName().trim().toUpperCase() : "";
        String catEmoji = category.getIcon() != null && !category.getIcon().isBlank() ? category.getIcon().trim() : "";
        if (catEmoji.isEmpty()) {
            String extracted = extractLeadingEmoji(catName);
            if (extracted != null && !extracted.isBlank()) {
                catEmoji = extracted.trim();
            }
        }

        List<Division> divisions = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(categoryId);

        List<java.util.Map<String, Object>> workTypes = new java.util.ArrayList<>();
        for (Division div : divisions) {
            String name = div.getName() != null ? div.getName().trim() : "";
            if (!name.isEmpty()) {
                java.util.Map<String, Object> divMap = new java.util.LinkedHashMap<>();
                divMap.put("id", div.getId());
                divMap.put("name", name.toUpperCase());
                String emoji = (div.getIcon() != null && !div.getIcon().isBlank())
                        ? div.getIcon().trim()
                        : resolveWorkTypeEmoji(name, (!catEmoji.isEmpty() ? catEmoji : "📁"));
                divMap.put("emoji", emoji);
                divMap.put("icon", emoji);
                divMap.put("description", div.getDescription() != null ? div.getDescription() : "");
                divMap.put("type", "Division");
                divMap.put("active", div.getActive() != null ? div.getActive() : true);
                workTypes.add(divMap);
            }
        }

        String fullCatName = (!catEmoji.isEmpty() && !catName.contains(catEmoji))
                ? (catEmoji + " " + catName)
                : catName;

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("success", true);
        result.put("categoryId", category.getId());
        result.put("categoryName", fullCatName);
        result.put("categoryEmoji", catEmoji);
        result.put("categoryDescription", category.getDescription() != null ? category.getDescription() : "");
        result.put("totalCount", workTypes.size());
        result.put("workTypes", workTypes);
        return result;
    }

    @Transactional
    public Division createWorkType(Long categoryId, String name, String emoji, String description) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category is required");
        }
        WorkCategory category = getCategoryById(categoryId);

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Work type name is required");
        }
        String cleanName = name.trim().toUpperCase();

        if (divisionRepository.existsByNameIgnoreCaseAndWorkCategoryId(cleanName, categoryId)) {
            throw new IllegalArgumentException("Work type '" + cleanName + "' already exists in this category");
        }

        Division division = new Division();
        division.setName(cleanName);
        division.setWorkCategory(category);
        division.setDescription(cleanValue(description));

        String effectiveEmoji = cleanValue(emoji);
        if (effectiveEmoji == null || effectiveEmoji.isBlank()) {
            effectiveEmoji = resolveWorkTypeEmoji(cleanName, category.getIcon());
        }
        division.setIcon(effectiveEmoji);
        division.setActive(true);
        division.setDisplayOrder(0);

        return divisionRepository.save(division);
    }

    @Transactional
    public Division updateWorkType(Long categoryId, Long workTypeId, String name, String emoji, String description) {
        if (workTypeId == null) {
            throw new IllegalArgumentException("Work type ID is required");
        }
        Division division = divisionRepository.findById(workTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Work type not found with ID: " + workTypeId));

        if (categoryId != null && division.getWorkCategory() != null && !categoryId.equals(division.getWorkCategory().getId())) {
            throw new IllegalArgumentException("Work type does not belong to the specified category");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Work type name is required");
        }
        String cleanName = name.trim().toUpperCase();

        Long catId = division.getWorkCategory() != null ? division.getWorkCategory().getId() : categoryId;
        if (catId != null && divisionRepository.existsByNameIgnoreCaseAndWorkCategoryIdAndIdNot(cleanName, catId, workTypeId)) {
            throw new IllegalArgumentException("Another work type with name '" + cleanName + "' already exists in this category");
        }

        division.setName(cleanName);
        division.setDescription(cleanValue(description));

        String effectiveEmoji = cleanValue(emoji);
        if (effectiveEmoji == null || effectiveEmoji.isBlank()) {
            effectiveEmoji = resolveWorkTypeEmoji(cleanName, division.getWorkCategory() != null ? division.getWorkCategory().getIcon() : null);
        }
        division.setIcon(effectiveEmoji);

        return divisionRepository.save(division);
    }

    @Transactional
    public String deleteWorkType(Long categoryId, Long workTypeId) {
        if (workTypeId == null) {
            throw new IllegalArgumentException("Work type ID is required");
        }
        Division division = divisionRepository.findById(workTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Work type not found with ID: " + workTypeId));

        if (categoryId != null && division.getWorkCategory() != null && !categoryId.equals(division.getWorkCategory().getId())) {
            throw new IllegalArgumentException("Work type does not belong to the specified category");
        }

        String workTypeName = division.getName();

        // 1. Check if division is referenced by existing quotation items or quotations
        boolean usedInQuotations = quotationItemRepository.countByDivisionId(workTypeId) > 0
                || quotationRepository.countByDivisionId(workTypeId) > 0;

        if (usedInQuotations) {
            division.setActive(false);
            divisionRepository.saveAndFlush(division);
            return "Work type '" + workTypeName + "' is referenced by existing quotations. It has been safely deactivated to preserve quotation history.";
        }

        // 2. Also check if any child items are used in quotations
        List<Item> items = itemRepository.findByDivisionIdOrderByDisplayOrderAsc(workTypeId);
        for (Item item : items) {
            if (!quotationItemRepository.findByItemIdOrderByCreatedAtDesc(item.getId()).isEmpty()) {
                usedInQuotations = true;
                break;
            }
        }
        if (usedInQuotations) {
            division.setActive(false);
            divisionRepository.saveAndFlush(division);
            return "Work type '" + workTypeName + "' is referenced by existing quotations. It has been safely deactivated to preserve quotation history.";
        }

        try {
            // Unlink materials
            List<Material> materials = materialRepository.findAllByDivisionId(workTypeId);
            for (Material mat : materials) {
                mat.getDivisions().remove(division);
                materialRepository.save(mat);
            }
            if (!items.isEmpty()) {
                itemRepository.deleteAll(items);
            }
            divisionRepository.delete(division);
            divisionRepository.flush();
            return "Work type '" + workTypeName + "' deleted successfully.";
        } catch (Exception e) {
            division.setActive(false);
            divisionRepository.saveAndFlush(division);
            return "Work type '" + workTypeName + "' has associated data and has been safely deactivated to protect system integrity.";
        }
    }

    @Transactional
    public Division toggleWorkTypeStatus(Long categoryId, Long workTypeId) {
        if (workTypeId == null) {
            throw new IllegalArgumentException("Work type ID is required");
        }
        Division division = divisionRepository.findById(workTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Work type not found with ID: " + workTypeId));

        if (categoryId != null && division.getWorkCategory() != null && !categoryId.equals(division.getWorkCategory().getId())) {
            throw new IllegalArgumentException("Work type does not belong to the specified category");
        }

        division.setActive(!Boolean.TRUE.equals(division.getActive()));
        return divisionRepository.saveAndFlush(division);
    }

    public static String extractLeadingEmoji(String text) {
        if (text == null || text.isBlank()) return null;
        text = text.trim();
        StringBuilder emoji = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            int type = Character.getType(codePoint);
            if (type == Character.SURROGATE || type == Character.OTHER_SYMBOL || type == Character.MODIFIER_SYMBOL
                    || type == Character.FORMAT
                    || codePoint == 0x200D
                    || (codePoint >= 0x1F000 && codePoint <= 0x1FAFF)
                    || (codePoint >= 0x2000 && codePoint <= 0x2BFF)
                    || (codePoint >= 0xFE00 && codePoint <= 0xFE0F)) {
                emoji.append(Character.toChars(codePoint));
                i += Character.charCount(codePoint);
            } else if (Character.isWhitespace(codePoint) && emoji.length() > 0) {
                break;
            } else {
                break;
            }
        }
        return emoji.length() > 0 ? emoji.toString().trim() : null;
    }

    public static String resolveWorkTypeEmoji(String name, String fallbackCategoryEmoji) {
        if (name == null || name.isBlank()) {
            return (fallbackCategoryEmoji != null && !fallbackCategoryEmoji.isBlank() && !"?".equals(fallbackCategoryEmoji)) ? fallbackCategoryEmoji : "📋";
        }
        String lower = name.toLowerCase();
        if (lower.contains("bottom") || lower.contains("base") || lower.contains("overhead") 
                || lower.contains("cabinet") || lower.contains("storage") || lower.contains("shutter") 
                || lower.contains("drawer")) {
            return "🗄️";
        }
        if (lower.contains("table") || lower.contains("breakfast") || lower.contains("dining") || lower.contains("desk")) {
            return "🪑";
        }
        if (lower.contains("island") || lower.contains("i-land")) {
            return "🏝️";
        }
        if (lower.contains("loft") || lower.contains("roof") || lower.contains("ceiling")) {
            return "🏠";
        }
        if (lower.contains("rack") || lower.contains("spice") || lower.contains("shelf") || lower.contains("ledge")) {
            return "🪜";
        }
        if (lower.contains("shoe")) {
            return "👞";
        }
        if (lower.contains("wardrobe") || lower.contains("almirah") || lower.contains("closet") || lower.contains("door")) {
            return "🚪";
        }
        if (lower.contains("box") || lower.contains("ply") || lower.contains("unit") || lower.contains("partition")) {
            return "📦";
        }
        if (lower.contains("panel") || lower.contains("beam") || lower.contains("wood") || lower.contains("board")) {
            return "🪵";
        }
        if (lower.contains("kitchen") || lower.contains("cook") || lower.contains("hob")) {
            return "🍳";
        }
        if (lower.contains("bed") || lower.contains("bedroom") || lower.contains("cot")) {
            return "🛏️";
        }
        if (lower.contains("foyer") || lower.contains("entrance") || lower.contains("living")) {
            return "🛋️";
        }
        if (fallbackCategoryEmoji != null && !fallbackCategoryEmoji.isBlank() && !"?".equals(fallbackCategoryEmoji)) {
            return fallbackCategoryEmoji;
        }
        return "🛠️";
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getWorkTypeMaterials(Long workTypeId) {
        if (workTypeId == null) {
            throw new IllegalArgumentException("Work type ID is required.");
        }
        Division division = divisionRepository.findById(workTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Work type not found with ID: " + workTypeId));

        WorkCategory category = division.getWorkCategory();
        String catName = category != null ? category.getName() : "";
        String emoji = division.getIcon() != null && !division.getIcon().isBlank()
                ? division.getIcon().trim()
                : resolveWorkTypeEmoji(division.getName(), category != null ? category.getIcon() : "📋");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("workTypeId", division.getId());
        response.put("workTypeName", division.getName());
        response.put("workTypeEmoji", emoji);
        response.put("categoryId", category != null ? category.getId() : null);
        response.put("categoryName", catName);

        // Dynamic branches loaded from MySQL
        List<Branch> activeBranches = (branchRepository != null)
                ? branchRepository.findByActiveTrueOrderByNameAsc()
                : Collections.emptyList();
        List<Map<String, Object>> branchList = new ArrayList<>();
        for (Branch b : activeBranches) {
            Map<String, Object> bm = new LinkedHashMap<>();
            bm.put("id", b.getId());
            bm.put("name", b.getName());
            branchList.add(bm);
        }
        response.put("branches", branchList);

        Map<String, List<Map<String, Object>>> materialsByGrade = new LinkedHashMap<>();
        materialsByGrade.put("PREMIUM", new ArrayList<>());
        materialsByGrade.put("MIDDLE", new ArrayList<>());
        materialsByGrade.put("ECONOMY", new ArrayList<>());

        List<Material> allMaterials = materialRepository.findAllByDivisionIdUnified(workTypeId);
        for (Material m : allMaterials) {
            Map<String, Object> matMap = new LinkedHashMap<>();
            matMap.put("id", m.getId());
            matMap.put("name", m.getName());
            matMap.put("specification", m.getSpecification() != null ? m.getSpecification() : (m.getDescription() != null ? m.getDescription() : ""));
            matMap.put("unit", m.getUnit() != null ? m.getUnit() : "Sq.ft");
            matMap.put("rate", m.getRate() != null ? m.getRate() : BigDecimal.ZERO);
            matMap.put("pricingGrade", m.getPricingGrade() != null ? m.getPricingGrade().name() : "MIDDLE");
            matMap.put("formula", m.getFormula());
            matMap.put("active", m.getActive());

            // Fetch branch-specific rates for this material
            Map<Long, Map<String, BigDecimal>> branchRates = new LinkedHashMap<>();
            Map<String, Map<Long, BigDecimal>> gradeBranchRates = new LinkedHashMap<>();
            gradeBranchRates.put("PREMIUM", new LinkedHashMap<>());
            gradeBranchRates.put("MIDDLE", new LinkedHashMap<>());
            gradeBranchRates.put("ECONOMY", new LinkedHashMap<>());

            List<MaterialOption> options = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(m.getId());
            if (!options.isEmpty()) {
                MaterialOption option = options.get(0);
                for (PricingMode mode : PricingMode.values()) {
                    Optional<Pricing> pOpt = pricingRepository.findByMaterialOptionIdAndPricingMode(option.getId(), mode);
                    if (pOpt.isPresent()) {
                        Pricing pricing = pOpt.get();
                        if (districtPricingRepository != null) {
                            List<DistrictPricing> dpList = districtPricingRepository.findByPricingIdOrderByCreatedAtDesc(pricing.getId());
                            for (DistrictPricing dp : dpList) {
                                if (dp.getBranch() != null && dp.getRate() != null && Boolean.TRUE.equals(dp.getActive())) {
                                    Long bId = dp.getBranch().getId();
                                    branchRates.computeIfAbsent(bId, k -> new LinkedHashMap<>()).put(mode.name(), dp.getRate());
                                    gradeBranchRates.get(mode.name()).put(bId, dp.getRate());
                                }
                            }
                        }
                    }
                }
            }

            // Fallback for legacy materials if branch rates table is empty
            if (branchRates.isEmpty() && m.getRate() != null) {
                PricingMode mGrade = m.getPricingGrade() != null ? m.getPricingGrade() : PricingMode.MIDDLE;
                for (Branch b : activeBranches) {
                    branchRates.computeIfAbsent(b.getId(), k -> new LinkedHashMap<>()).put(mGrade.name(), m.getRate());
                    gradeBranchRates.get(mGrade.name()).put(b.getId(), m.getRate());
                }
            }

            List<Map<String, Object>> branchRatesList = new ArrayList<>();
            for (Branch b : activeBranches) {
                Map<String, Object> brItem = new LinkedHashMap<>();
                brItem.put("branchId", b.getId());
                brItem.put("branchName", b.getName());
                Map<String, BigDecimal> ratesForBranch = branchRates.getOrDefault(b.getId(), Collections.emptyMap());
                brItem.put("premiumRate", ratesForBranch.get("PREMIUM"));
                brItem.put("middleRate", ratesForBranch.get("MIDDLE"));
                brItem.put("economyRate", ratesForBranch.get("ECONOMY"));
                branchRatesList.add(brItem);
            }

            matMap.put("branchRates", branchRates);
            matMap.put("branchRatesList", branchRatesList);

            boolean hasPremium = !gradeBranchRates.get("PREMIUM").isEmpty() || m.getPricingGrade() == PricingMode.PREMIUM;
            boolean hasMiddle = !gradeBranchRates.get("MIDDLE").isEmpty() || m.getPricingGrade() == PricingMode.MIDDLE || m.getPricingGrade() == null;
            boolean hasEconomy = !gradeBranchRates.get("ECONOMY").isEmpty() || m.getPricingGrade() == PricingMode.ECONOMY;

            if (hasPremium) {
                Map<String, Object> copy = new LinkedHashMap<>(matMap);
                copy.put("pricingGrade", "PREMIUM");
                BigDecimal rep = gradeBranchRates.get("PREMIUM").values().stream().findFirst().orElse(m.getRate() != null ? m.getRate() : BigDecimal.ZERO);
                copy.put("rate", rep);
                materialsByGrade.get("PREMIUM").add(copy);
            }
            if (hasMiddle) {
                Map<String, Object> copy = new LinkedHashMap<>(matMap);
                copy.put("pricingGrade", "MIDDLE");
                BigDecimal rep = gradeBranchRates.get("MIDDLE").values().stream().findFirst().orElse(m.getRate() != null ? m.getRate() : BigDecimal.ZERO);
                copy.put("rate", rep);
                materialsByGrade.get("MIDDLE").add(copy);
            }
            if (hasEconomy) {
                Map<String, Object> copy = new LinkedHashMap<>(matMap);
                copy.put("pricingGrade", "ECONOMY");
                BigDecimal rep = gradeBranchRates.get("ECONOMY").values().stream().findFirst().orElse(m.getRate() != null ? m.getRate() : BigDecimal.ZERO);
                copy.put("rate", rep);
                materialsByGrade.get("ECONOMY").add(copy);
            }
        }

        response.put("materials", materialsByGrade);
        response.put("premium", materialsByGrade.get("PREMIUM"));
        response.put("middle", materialsByGrade.get("MIDDLE"));
        response.put("economy", materialsByGrade.get("ECONOMY"));
        response.put("premiumCount", materialsByGrade.get("PREMIUM").size());
        response.put("middleCount", materialsByGrade.get("MIDDLE").size());
        response.put("economyCount", materialsByGrade.get("ECONOMY").size());
        response.put("totalCount", allMaterials.size());

        return response;
    }

    public Map<Long, Map<PricingMode, BigDecimal>> parseBranchRates(String json, Map<String, String> formParams) {
        Map<Long, Map<PricingMode, BigDecimal>> result = new LinkedHashMap<>();
        if (json != null && !json.isBlank()) {
            java.util.regex.Pattern branchPattern = java.util.regex.Pattern.compile("\"?(\\d+)\"?\\s*:\\s*\\{([^}]+)\\}");
            java.util.regex.Matcher branchMatcher = branchPattern.matcher(json);
            while (branchMatcher.find()) {
                try {
                    Long branchId = Long.parseLong(branchMatcher.group(1));
                    String inner = branchMatcher.group(2);
                    java.util.regex.Pattern gradePattern = java.util.regex.Pattern.compile("\"?(PREMIUM|MIDDLE|ECONOMY)\"?\\s*:\\s*\"?([0-9.]+)\"?");
                    java.util.regex.Matcher gradeMatcher = gradePattern.matcher(inner);
                    while (gradeMatcher.find()) {
                        PricingMode mode = PricingMode.valueOf(gradeMatcher.group(1).trim().toUpperCase());
                        BigDecimal rate = new BigDecimal(gradeMatcher.group(2).trim());
                        if (rate.compareTo(BigDecimal.ZERO) < 0) {
                            throw new IllegalArgumentException("Rates must be non-negative.");
                        }
                        result.computeIfAbsent(branchId, k -> new LinkedHashMap<>()).put(mode, rate);
                    }
                } catch (IllegalArgumentException e) {
                    throw e;
                } catch (Exception ignored) {}
            }
        }

        if (formParams != null && !formParams.isEmpty()) {
            for (Map.Entry<String, String> entry : formParams.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                if (val != null && !val.isBlank() && (key.startsWith("rate_branch_") || key.startsWith("rate_"))) {
                    try {
                        String[] parts = key.split("_");
                        Long branchId = null;
                        PricingMode mode = null;
                        if (parts.length >= 4 && "branch".equalsIgnoreCase(parts[1])) {
                            branchId = Long.parseLong(parts[2]);
                            mode = PricingMode.valueOf(parts[3].toUpperCase());
                        } else if (parts.length >= 3) {
                            branchId = Long.parseLong(parts[1]);
                            mode = PricingMode.valueOf(parts[2].toUpperCase());
                        }
                        if (branchId != null && mode != null) {
                            BigDecimal rate = new BigDecimal(val.trim());
                            if (rate.compareTo(BigDecimal.ZERO) < 0) {
                                throw new IllegalArgumentException("Rates must be non-negative.");
                            }
                            result.computeIfAbsent(branchId, k -> new LinkedHashMap<>()).put(mode, rate);
                        }
                    } catch (IllegalArgumentException e) {
                        throw e;
                    } catch (Exception ignored) {}
                }
            }
        }
        return result;
    }

    @Transactional
    public Material saveWorkTypeMaterialWithBranchRates(
            Long materialId,
            Long workTypeId,
            String name,
            String specification,
            String unit,
            String formula,
            Map<Long, Map<PricingMode, BigDecimal>> branchRates,
            PricingMode fallbackGrade,
            BigDecimal fallbackRate) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Material name is required.");
        }
        String cleanName = name.trim();

        // Find representative rate
        BigDecimal representativeRate = fallbackRate;
        PricingMode representativeGrade = fallbackGrade != null ? fallbackGrade : PricingMode.MIDDLE;
        if (branchRates != null && !branchRates.isEmpty()) {
            for (Map<PricingMode, BigDecimal> gm : branchRates.values()) {
                if (gm.containsKey(PricingMode.PREMIUM)) {
                    representativeRate = gm.get(PricingMode.PREMIUM);
                    representativeGrade = PricingMode.PREMIUM;
                    break;
                } else if (gm.containsKey(PricingMode.MIDDLE)) {
                    representativeRate = gm.get(PricingMode.MIDDLE);
                    representativeGrade = PricingMode.MIDDLE;
                    break;
                } else if (gm.containsKey(PricingMode.ECONOMY)) {
                    representativeRate = gm.get(PricingMode.ECONOMY);
                    representativeGrade = PricingMode.ECONOMY;
                    break;
                }
            }
        }

        if (branchRates != null && !branchRates.isEmpty()) {
            for (Map<PricingMode, BigDecimal> gm : branchRates.values()) {
                for (BigDecimal r : gm.values()) {
                    if (r != null && r.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("Material rate must be non-negative.");
                    }
                }
            }
        }
        if (fallbackRate != null && fallbackRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Material rate must be non-negative.");
        }

        Material material;
        Division division;

        if (materialId != null) {
            material = materialRepository.findById(materialId)
                    .orElseThrow(() -> new IllegalArgumentException("Material not found with ID: " + materialId));
            division = (workTypeId != null)
                    ? divisionRepository.findById(workTypeId).orElse(material.getDivision())
                    : material.getDivision();
            if (division != null && materialRepository.existsByDivisionIdAndPricingGradeAndNameIgnoreCaseAndIdNot(division.getId(), representativeGrade, cleanName, materialId)) {
                throw new IllegalArgumentException("Another material with name '" + cleanName + "' already exists in this work type under " + representativeGrade.name() + " grade.");
            }
        } else {
            if (workTypeId == null) {
                throw new IllegalArgumentException("Work type ID is required.");
            }
            division = divisionRepository.findById(workTypeId)
                    .orElseThrow(() -> new IllegalArgumentException("Work type not found with ID: " + workTypeId));
            if (materialRepository.existsByDivisionIdAndPricingGradeAndNameIgnoreCase(division.getId(), representativeGrade, cleanName)) {
                throw new IllegalArgumentException("Material '" + cleanName + "' already exists in this work type under " + representativeGrade.name() + " grade.");
            }
            material = new Material();
        }
        String cleanUnit = (unit != null && !unit.trim().isEmpty() && (unit.trim().equalsIgnoreCase("Sq.ft") || unit.trim().equalsIgnoreCase("Sq.Ft"))) ? "Sq.ft" : "Sq.ft";
        String cleanSpec = cleanValue(specification);
        String cleanFormula = CalculationFormula.normalizeFormula(formula);

        if (representativeRate == null) {
            representativeRate = BigDecimal.ZERO;
        }

        material.setName(cleanName);
        material.setDivision(division);
        material.setPricingGrade(representativeGrade);
        material.setRate(representativeRate);
        material.setUnit(cleanUnit);
        material.setFormula(cleanFormula);
        material.setSpecification(cleanSpec);
        material.setDescription(cleanSpec);
        material.setActive(true);

        Material savedMaterial = materialRepository.saveAndFlush(material);

        // Sync MaterialOption
        List<MaterialOption> existingOpts = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(savedMaterial.getId());
        MaterialOption option;
        if (!existingOpts.isEmpty()) {
            option = existingOpts.get(0);
        } else {
            option = new MaterialOption();
            option.setMaterial(savedMaterial);
        }
        option.setName(cleanName);
        option.setDescription(cleanSpec);
        option.setFormula(cleanFormula);
        option.setActive(true);
        MaterialOption savedOption = materialOptionRepository.saveAndFlush(option);

        // Save Pricing and DistrictPricing across branches
        List<Branch> activeBranches = (branchRepository != null)
                ? branchRepository.findByActiveTrueOrderByNameAsc()
                : Collections.emptyList();

        for (PricingMode mode : PricingMode.values()) {
            Pricing pricing = pricingRepository.findByMaterialOptionIdAndPricingMode(option.getId(), mode)
                    .orElseGet(() -> {
                        Pricing p = new Pricing();
                        p.setMaterialOption(option);
                        p.setPricingMode(mode);
                        p.setUnit(cleanUnit);
                        p.setFormula(cleanFormula);
                        p.setActive(true);
                        return p;
                    });
            pricing.setUnit(cleanUnit);
            pricing.setFormula(cleanFormula);
            pricing.setDescription(cleanSpec);
            pricing.setActive(true);

            // Compute default/base rate for pricing record
            BigDecimal modeBaseRate = BigDecimal.ZERO;
            if (branchRates != null && !branchRates.isEmpty()) {
                for (Map<PricingMode, BigDecimal> gm : branchRates.values()) {
                    if (gm.containsKey(mode) && gm.get(mode) != null) {
                        modeBaseRate = gm.get(mode);
                        break;
                    }
                }
            } else if (fallbackRate != null && (fallbackGrade == null || fallbackGrade == mode)) {
                modeBaseRate = fallbackRate;
            }
            pricing.setRate(modeBaseRate);
            Pricing savedPricing = pricingRepository.saveAndFlush(pricing);

            if (districtPricingRepository != null && !activeBranches.isEmpty()) {
                for (Branch branch : activeBranches) {
                    BigDecimal branchRate = null;
                    if (branchRates != null) {
                        if (branchRates.containsKey(branch.getId()) && branchRates.get(branch.getId()) != null) {
                            branchRate = branchRates.get(branch.getId()).get(mode);
                        }
                    } else if (fallbackRate != null && (fallbackGrade == null || fallbackGrade == mode)) {
                        branchRate = fallbackRate;
                    }

                    if (branchRate != null) {
                        DistrictPricing dp = districtPricingRepository.findByBranchIdAndPricingId(branch.getId(), savedPricing.getId())
                                .orElseGet(() -> {
                                    DistrictPricing newDp = new DistrictPricing();
                                    newDp.setBranch(branch);
                                    newDp.setPricing(savedPricing);
                                    return newDp;
                                });
                        dp.setBranch(branch);
                        dp.setPricing(savedPricing);
                        dp.setRate(branchRate);
                        dp.setActive(true);
                        districtPricingRepository.save(dp);
                    } else {
                        // Rate was unconfigured or cleared for this branch/grade
                        Optional<DistrictPricing> existing = districtPricingRepository.findByBranchIdAndPricingId(branch.getId(), savedPricing.getId());
                        existing.ifPresent(dp -> {
                            dp.setActive(false);
                            districtPricingRepository.save(dp);
                        });
                    }
                }
            }
        }

        if (districtPricingRepository != null) {
            districtPricingRepository.flush();
        }
        pricingRepository.flush();

        return savedMaterial;
    }

    @Transactional
    public Material createWorkTypeMaterial(
            Long workTypeId,
            PricingMode grade,
            String name,
            String specification,
            String unit,
            BigDecimal rate,
            String formula,
            Map<Long, Map<PricingMode, BigDecimal>> branchRates) {
        return saveWorkTypeMaterialWithBranchRates(null, workTypeId, name, specification, unit, formula, branchRates, grade, rate);
    }

    @Transactional
    public Material createWorkTypeMaterial(
            Long workTypeId,
            PricingMode grade,
            String name,
            String specification,
            String unit,
            BigDecimal rate) {
        return createWorkTypeMaterial(workTypeId, grade, name, specification, unit, rate, null, null);
    }

    @Transactional
    public Material createWorkTypeMaterial(
            Long workTypeId,
            PricingMode grade,
            String name,
            String specification,
            String unit,
            BigDecimal rate,
            String formula) {
        return createWorkTypeMaterial(workTypeId, grade, name, specification, unit, rate, formula, null);
    }

    @Transactional
    public Material updateWorkTypeMaterial(
            Long materialId,
            String name,
            String specification,
            String unit,
            BigDecimal rate,
            String formula,
            Map<Long, Map<PricingMode, BigDecimal>> branchRates) {
        return saveWorkTypeMaterialWithBranchRates(materialId, null, name, specification, unit, formula, branchRates, null, rate);
    }

    @Transactional
    public Material updateWorkTypeMaterial(
            Long materialId,
            String name,
            String specification,
            String unit,
            BigDecimal rate) {
        return updateWorkTypeMaterial(materialId, name, specification, unit, rate, null, null);
    }

    @Transactional
    public Material updateWorkTypeMaterial(
            Long materialId,
            String name,
            String specification,
            String unit,
            BigDecimal rate,
            String formula) {
        return updateWorkTypeMaterial(materialId, name, specification, unit, rate, formula, null);
    }

    @Transactional
    public String deleteWorkTypeMaterial(Long materialId) {
        if (materialId == null) {
            throw new IllegalArgumentException("Material ID is required.");
        }
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found with ID: " + materialId));

        String materialName = material.getName();
        long quotationCount = quotationItemRepository.countByMaterialId(materialId);
        if (quotationCount > 0) {
            material.setActive(false);
            materialRepository.saveAndFlush(material);
            return "Material '" + materialName + "' is associated with " + quotationCount + " quotation item(s) and has been safely deactivated to preserve historical quotation data.";
        }

        try {
            List<MaterialOption> options = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(materialId);
            for (MaterialOption opt : options) {
                List<Pricing> pricings = pricingRepository.findByMaterialOptionIdOrderByPricingModeAsc(opt.getId());
                if (!pricings.isEmpty()) {
                    for (Pricing p : pricings) {
                        if (districtPricingRepository != null) {
                            districtPricingRepository.deleteByPricingId(p.getId());
                        }
                    }
                    pricingRepository.deleteAll(pricings);
                }
            }
            if (!options.isEmpty()) {
                materialOptionRepository.deleteAll(options);
            }
            materialRepository.delete(material);
            materialRepository.flush();
            return "Material '" + materialName + "' deleted successfully.";
        } catch (Exception e) {
            material.setActive(false);
            materialRepository.saveAndFlush(material);
            return "Material '" + materialName + "' has existing references and has been safely deactivated.";
        }
    }

    private void validateName(String name) {

        if (name == null || name.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Work category name is required"
            );
        }
    }


    private String cleanValue(String value) {

        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        return cleaned.isEmpty() ? null : cleaned;
    }
}