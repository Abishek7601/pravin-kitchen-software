package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.util.CalculationFormula;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PricingService {

    private final PricingRepository pricingRepository;
    private final MaterialOptionRepository materialOptionRepository;
    private final DistrictPricingRepository districtPricingRepository;
    private final WorkCategoryRepository workCategoryRepository;
    private final DivisionRepository divisionRepository;
    private final ItemRepository itemRepository;
    private final MaterialRepository materialRepository;

    public PricingService(
            PricingRepository pricingRepository,
            MaterialOptionRepository materialOptionRepository,
            DistrictPricingRepository districtPricingRepository,
            WorkCategoryRepository workCategoryRepository,
            DivisionRepository divisionRepository,
            ItemRepository itemRepository,
            MaterialRepository materialRepository
    ) {
        this.pricingRepository = pricingRepository;
        this.materialOptionRepository = materialOptionRepository;
        this.districtPricingRepository = districtPricingRepository;
        this.workCategoryRepository = workCategoryRepository;
        this.divisionRepository = divisionRepository;
        this.itemRepository = itemRepository;
        this.materialRepository = materialRepository;
    }

    public List<Pricing> getAllPricing() {

        return pricingRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Pricing> getActivePricing() {

        return pricingRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    public Pricing getPricingById(Long id) {

        return pricingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pricing record not found"
                ));
    }

    public List<Pricing> getPricingByMaterialOption(Long materialOptionId) {

        return pricingRepository
                .findByMaterialOptionIdOrderByPricingModeAsc(
                        materialOptionId
                );
    }

    public Pricing getPricing(
            Long materialOptionId,
            PricingMode pricingMode
    ) {

        return pricingRepository
                .findByMaterialOptionIdAndPricingMode(
                        materialOptionId,
                        pricingMode
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pricing not found for the selected material option and pricing mode"
                ));
    }

    public Pricing createPricing(
            Long materialOptionId,
            PricingMode pricingMode,
            BigDecimal rate,
            String unit,
            String description
    ) {
        return createPricing(materialOptionId, pricingMode, rate, unit, CalculationFormula.FORMULA_L_X_W, description);
    }

    public Pricing createPricing(
            Long materialOptionId,
            PricingMode pricingMode,
            BigDecimal rate,
            String unit,
            String formula,
            String description
    ) {

        validateMaterialOption(materialOptionId);
        validatePricingMode(pricingMode);
        validateRate(rate);
        validateUnit(unit);

        MaterialOption materialOption =
                materialOptionRepository.findById(materialOptionId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Material option not found"
                        ));

        if (pricingRepository
                .existsByMaterialOptionIdAndPricingMode(
                        materialOptionId,
                        pricingMode
                )) {

            throw new IllegalArgumentException(
                    "Pricing already exists for this material option and pricing mode"
            );
        }

        String normFormula = resolveFormula(formula);
        materialOption.setFormula(normFormula);
        materialOptionRepository.save(materialOption);

        Pricing pricing = new Pricing();

        pricing.setMaterialOption(materialOption);
        pricing.setPricingMode(pricingMode);
        pricing.setRate(rate);
        pricing.setUnit(unit.trim().toUpperCase());
        pricing.setFormula(normFormula);
        pricing.setDescription(cleanValue(description));
        pricing.setActive(true);

        return pricingRepository.save(pricing);
    }

    public Pricing updatePricing(
            Long id,
            Long materialOptionId,
            PricingMode pricingMode,
            BigDecimal rate,
            String unit,
            String description
    ) {
        return updatePricing(id, materialOptionId, pricingMode, rate, unit, CalculationFormula.FORMULA_L_X_W, description);
    }

    public Pricing updatePricing(
            Long id,
            Long materialOptionId,
            PricingMode pricingMode,
            BigDecimal rate,
            String unit,
            String formula,
            String description
    ) {

        Pricing pricing = getPricingById(id);

        validateMaterialOption(materialOptionId);
        validatePricingMode(pricingMode);
        validateRate(rate);
        validateUnit(unit);

        MaterialOption materialOption =
                materialOptionRepository.findById(materialOptionId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Material option not found"
                        ));

        if (pricingRepository
                .existsByMaterialOptionIdAndPricingModeAndIdNot(
                        materialOptionId,
                        pricingMode,
                        id
                )) {

            throw new IllegalArgumentException(
                    "Another pricing record already exists for this material option and pricing mode"
            );
        }

        String normFormula = resolveFormula(formula);
        materialOption.setFormula(normFormula);
        materialOptionRepository.save(materialOption);

        pricing.setMaterialOption(materialOption);
        pricing.setPricingMode(pricingMode);
        pricing.setRate(rate);
        pricing.setUnit(unit.trim().toUpperCase());
        pricing.setFormula(normFormula);
        pricing.setDescription(cleanValue(description));

        return pricingRepository.save(pricing);
    }

    /**
     * Unified Master Data Configuration:
     * Configures Work Category, Division, Material, Material Option, Pricing Mode, Price, and Formula in one workflow.
     * Prevents duplicates by reusing existing records when names match (case-insensitive).
     */
    public Pricing saveUnifiedMasterData(
            Long pricingId,
            Long workCategoryId,
            String newWorkCategoryName,
            Long divisionId,
            String newDivisionName,
            Long materialId,
            String newMaterialName,
            Long materialOptionId,
            String newMaterialOptionName,
            PricingMode pricingMode,
            BigDecimal rate,
            String unit,
            String formula,
            String description
    ) {
        // 1. Work Category
        WorkCategory workCategory;
        if (newWorkCategoryName != null && !newWorkCategoryName.trim().isEmpty()) {
            String name = newWorkCategoryName.trim();
            workCategory = workCategoryRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> {
                        WorkCategory wc = new WorkCategory();
                        wc.setName(name);
                        wc.setActive(true);
                        return workCategoryRepository.save(wc);
                    });
        } else if (workCategoryId != null) {
            workCategory = workCategoryRepository.findById(workCategoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Selected Work Category not found"));
        } else {
            throw new IllegalArgumentException("Work Category is required");
        }

        // 2. Division
        Division division;
        if (newDivisionName != null && !newDivisionName.trim().isEmpty()) {
            String divName = newDivisionName.trim();
            division = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(workCategory.getId())
                    .stream()
                    .filter(d -> d.getName().equalsIgnoreCase(divName))
                    .findFirst()
                    .orElseGet(() -> {
                        Division d = new Division();
                        d.setName(divName);
                        d.setWorkCategory(workCategory);
                        d.setActive(true);
                        return divisionRepository.save(d);
                    });
        } else if (divisionId != null) {
            division = divisionRepository.findById(divisionId)
                    .orElseThrow(() -> new IllegalArgumentException("Selected Division not found"));
        } else {
            throw new IllegalArgumentException("Division is required");
        }

        // 3. Item & Material
        Material material;
        if (newMaterialName != null && !newMaterialName.trim().isEmpty()) {
            String matName = newMaterialName.trim();
            List<Item> items = itemRepository.findByDivisionIdOrderByDisplayOrderAsc(division.getId());
            Item item = items.stream()
                    .filter(i -> i.getName().equalsIgnoreCase(matName))
                    .findFirst()
                    .orElseGet(() -> {
                        if (!items.isEmpty()) {
                            return items.get(0);
                        }
                        Item i = new Item();
                        i.setName(matName);
                        i.setDivision(division);
                        i.setActive(true);
                        return itemRepository.save(i);
                    });

            List<Material> mats = materialRepository.findByItemIdOrderByDisplayOrderAsc(item.getId());
            material = mats.stream()
                    .filter(m -> m.getName().equalsIgnoreCase(matName))
                    .findFirst()
                    .orElseGet(() -> {
                        Material m = new Material();
                        m.setName(matName);
                        m.setItem(item);
                        m.setActive(true);
                        return materialRepository.save(m);
                    });
        } else if (materialId != null) {
            material = materialRepository.findById(materialId)
                    .orElseThrow(() -> new IllegalArgumentException("Selected Material not found"));
        } else {
            throw new IllegalArgumentException("Material is required");
        }

        // 4. Material Option (OPTIONAL)
        String normFormula = resolveFormula(formula);
        MaterialOption materialOption;
        if (newMaterialOptionName != null && !newMaterialOptionName.trim().isEmpty()) {
            String optName = newMaterialOptionName.trim();
            List<MaterialOption> options = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(material.getId());
            materialOption = options.stream()
                    .filter(o -> o.getName().equalsIgnoreCase(optName))
                    .findFirst()
                    .orElseGet(() -> {
                        MaterialOption o = new MaterialOption();
                        o.setName(optName);
                        o.setMaterial(material);
                        o.setFormula(normFormula);
                        o.setActive(true);
                        return materialOptionRepository.save(o);
                    });
            materialOption.setFormula(normFormula);
            materialOption = materialOptionRepository.save(materialOption);
        } else if (materialOptionId != null) {
            materialOption = materialOptionRepository.findById(materialOptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Selected Material Option not found"));
            materialOption.setFormula(normFormula);
            materialOption = materialOptionRepository.save(materialOption);
        } else {
            // Material Option / Specification is optional:
            // Find existing "Standard" or material-named option, or create a default "Standard" option
            List<MaterialOption> options = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(material.getId());
            materialOption = options.stream()
                    .filter(o -> o.getName().equalsIgnoreCase("Standard") || o.getName().equalsIgnoreCase(material.getName()))
                    .findFirst()
                    .orElseGet(() -> {
                        MaterialOption o = new MaterialOption();
                        o.setName("Standard");
                        o.setMaterial(material);
                        o.setFormula(normFormula);
                        o.setActive(true);
                        return materialOptionRepository.save(o);
                    });
            materialOption.setFormula(normFormula);
            materialOption = materialOptionRepository.save(materialOption);
        }

        // 5. Rate & Unit & Pricing Mode validation
        validateRate(rate);
        if (pricingMode == null) {
            pricingMode = PricingMode.MIDDLE;
        }
        String cleanUnit = (unit == null || unit.isBlank()) ? "SQFT" : unit.trim().toUpperCase();
        validateUnit(cleanUnit);

        // 6. Pricing record create or update
        Pricing pricing;
        if (pricingId != null) {
            pricing = getPricingById(pricingId);
            if (pricingRepository.existsByMaterialOptionIdAndPricingModeAndIdNot(
                    materialOption.getId(), pricingMode, pricingId)) {
                throw new IllegalArgumentException(
                        "Another pricing record already exists for this material option and pricing mode"
                );
            }
        } else {
            pricing = pricingRepository.findByMaterialOptionIdAndPricingMode(materialOption.getId(), pricingMode).orElse(null);
            if (pricing == null) {
                pricing = new Pricing();
                pricing.setActive(true);
            }
        }

        pricing.setMaterialOption(materialOption);
        pricing.setPricingMode(pricingMode);
        pricing.setRate(rate);
        pricing.setUnit(cleanUnit);
        pricing.setFormula(normFormula);
        pricing.setDescription(cleanValue(description));

        return pricingRepository.save(pricing);
    }

    public void togglePricingStatus(Long id) {

        Pricing pricing = getPricingById(id);

        pricing.setActive(
                !Boolean.TRUE.equals(pricing.getActive())
        );

        pricingRepository.save(pricing);
    }

    @Transactional
    public void deletePricing(Long id) {

        Pricing pricing = getPricingById(id);

        // Safely remove any legacy district pricing records pointing to this pricing
        districtPricingRepository.deleteByPricingId(id);

        try {

            pricingRepository.delete(pricing);

        } catch (DataIntegrityViolationException e) {

            /*
             * Safety fallback in case another foreign-key relationship
             * is using this Pricing record.
             */

            throw new IllegalStateException(
                    "This pricing cannot be deleted because it is being used by another record. "
                            + "Please deactivate it instead.",
                    e
            );
        }
    }

    private void validateMaterialOption(Long materialOptionId) {

        if (materialOptionId == null) {

            throw new IllegalArgumentException(
                    "Material option is required"
            );
        }
    }

    private void validatePricingMode(PricingMode pricingMode) {

        if (pricingMode == null) {

            throw new IllegalArgumentException(
                    "Pricing mode is required"
            );
        }
    }

    private void validateRate(BigDecimal rate) {

        if (rate == null) {

            throw new IllegalArgumentException(
                    "Rate is required"
            );
        }

        if (rate.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Rate must be greater than zero"
            );
        }
    }

    private void validateUnit(String unit) {
        if (unit == null || unit.trim().isEmpty()) {
            throw new IllegalArgumentException("Unit is required");
        }
        String u = unit.trim().toUpperCase();
        if ("INCHES".equals(u) || "INCH".equals(u) || "IN".equals(u)) {
            throw new IllegalArgumentException("Inches is not a permitted calculation unit. Only Sq Ft is allowed.");
        }
    }

    private String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String resolveFormula(String formula) {
        return CalculationFormula.normalizeFormula(formula);
    }
}