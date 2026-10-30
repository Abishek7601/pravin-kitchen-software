package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Division;
import com.example.pravin_quotation.model.Item;
import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.model.MaterialOption;
import com.example.pravin_quotation.model.MaterialSpecification;
import com.example.pravin_quotation.model.Pricing;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.util.CalculationFormula;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final ItemRepository itemRepository;
    private final DivisionRepository divisionRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final MaterialOptionRepository materialOptionRepository;
    private final PricingRepository pricingRepository;
    private final MaterialSpecificationRepository materialSpecificationRepository;

    public MaterialService(
            MaterialRepository materialRepository,
            ItemRepository itemRepository,
            DivisionRepository divisionRepository,
            QuotationItemRepository quotationItemRepository,
            MaterialOptionRepository materialOptionRepository,
            PricingRepository pricingRepository,
            MaterialSpecificationRepository materialSpecificationRepository
    ) {
        this.materialRepository = materialRepository;
        this.itemRepository = itemRepository;
        this.divisionRepository = divisionRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.materialOptionRepository = materialOptionRepository;
        this.pricingRepository = pricingRepository;
        this.materialSpecificationRepository = materialSpecificationRepository;
    }

    public List<Material> getAllMaterials() {
        return materialRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<Material> getActiveMaterials() {
        return materialRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public Material getMaterialById(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Material not found"
                ));
    }

    public List<Material> getMaterialsByItem(Long itemId) {
        return materialRepository
                .findByItemIdOrderByDisplayOrderAsc(itemId);
    }

    public List<Material> getActiveMaterialsByItem(Long itemId) {
        return materialRepository
                .findByItemIdAndActiveTrueOrderByDisplayOrderAsc(itemId);
    }

    public List<Material> getMaterialsByDivision(Long divisionId) {
        return materialRepository.findAllByDivisionId(divisionId);
    }

    public List<Material> getActiveMaterialsByDivision(Long divisionId) {
        return materialRepository.findActiveByDivisionId(divisionId);
    }

    @Transactional
    public Material createMaterial(
            String name,
            String description,
            Long itemId,
            List<Long> divisionIds,
            String formula,
            Integer displayOrder
    ) {
        validateName(name);
        String materialName = name.trim().toUpperCase();

        Item item = null;
        if (itemId != null) {
            item = itemRepository.findById(itemId).orElse(null);
        }

        if (itemId != null && materialRepository.existsByNameIgnoreCaseAndItemId(materialName, itemId)) {
            throw new IllegalArgumentException(
                    "Material already exists for this item"
            );
        }

        Material material = new Material();
        material.setName(materialName);
        material.setDescription(cleanValue(description));
        material.setItem(item);
        material.setActive(true);
        material.setDisplayOrder(displayOrder == null ? 0 : displayOrder);

        String normFormula = (formula != null && !formula.isBlank())
                ? CalculationFormula.normalizeFormula(formula)
                : "(L + W) × H";
        material.setFormula(normFormula);

        Set<Division> divisions = new HashSet<>();
        if (divisionIds != null && !divisionIds.isEmpty()) {
            for (Long divId : divisionIds) {
                divisionRepository.findById(divId).ifPresent(divisions::add);
            }
        }
        if (item != null && item.getDivision() != null) {
            divisions.add(item.getDivision());
        }
        material.setDivisions(divisions);

        return materialRepository.save(material);
    }

    public Material createMaterial(
            String name,
            String description,
            Integer displayOrder
    ) {
        return createMaterial(name, description, (Long) null, null, "(L + W) × H", displayOrder);
    }

    public Material createMaterial(
            String name,
            String description,
            String formula,
            Long itemId,
            List<Long> divisionIds
    ) {
        return createMaterial(name, description, itemId, divisionIds, formula, 0);
    }

    public Material updateMaterial(
            Long id,
            String name,
            String description,
            String formula,
            Long itemId,
            List<Long> divisionIds
    ) {
        return updateMaterial(id, name, description, itemId, divisionIds, formula, 0);
    }

    @Transactional
    public Material updateMaterial(
            Long id,
            String name,
            String description,
            Long itemId,
            List<Long> divisionIds,
            String formula,
            Integer displayOrder
    ) {
        Material material = getMaterialById(id);
        validateName(name);

        String materialName = name.trim().toUpperCase();
        if (itemId != null && materialRepository.existsByNameIgnoreCaseAndItemIdAndIdNot(materialName, itemId, id)) {
            throw new IllegalArgumentException(
                    "Another material with this name already exists for this item"
            );
        }

        Item item = null;
        if (itemId != null) {
            item = itemRepository.findById(itemId).orElse(null);
        }

        material.setName(materialName);
        material.setDescription(cleanValue(description));
        if (item != null) {
            material.setItem(item);
        }
        material.setDisplayOrder(displayOrder == null ? 0 : displayOrder);

        if (formula != null && !formula.isBlank()) {
            material.setFormula(CalculationFormula.normalizeFormula(formula));
        }

        if (divisionIds != null) {
            Set<Division> divisions = new HashSet<>();
            for (Long divId : divisionIds) {
                divisionRepository.findById(divId).ifPresent(divisions::add);
            }
            if (item != null && item.getDivision() != null) {
                divisions.add(item.getDivision());
            } else if (material.getItem() != null && material.getItem().getDivision() != null) {
                divisions.add(material.getItem().getDivision());
            }
            material.setDivisions(divisions);
        }

        return materialRepository.save(material);
    }

    public Material updateMaterial(
            Long id,
            String name,
            String description,
            Long itemId,
            Integer displayOrder
    ) {
        return updateMaterial(id, name, description, itemId, null, null, displayOrder);
    }

    @Transactional
    public void mapDivision(Long materialId, Long divisionId) {
        Material material = getMaterialById(materialId);
        Division division = divisionRepository.findById(divisionId)
                .orElseThrow(() -> new IllegalArgumentException("Division not found"));
        material.addDivision(division);
        materialRepository.save(material);
    }

    @Transactional
    public void unmapDivision(Long materialId, Long divisionId) {
        Material material = getMaterialById(materialId);
        Division division = divisionRepository.findById(divisionId)
                .orElseThrow(() -> new IllegalArgumentException("Division not found"));
        material.removeDivision(division);
        materialRepository.save(material);
    }

    public void toggleMaterialStatus(Long id) {
        Material material = getMaterialById(id);
        material.setActive(!Boolean.TRUE.equals(material.getActive()));
        materialRepository.save(material);
    }

    @Transactional
    public String deleteMaterial(Long id) {
        Material material = getMaterialById(id);
        String matName = material.getName();

        // 1. Check if material or any of its options/specifications are used in quotations
        boolean usedInQuotations = quotationItemRepository.countByMaterialId(id) > 0;

        List<MaterialOption> options = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(id);
        if (!usedInQuotations) {
            for (MaterialOption opt : options) {
                if (quotationItemRepository.countByMaterialOptionId(opt.getId()) > 0) {
                    usedInQuotations = true;
                    break;
                }
            }
        }

        List<MaterialSpecification> specs = materialSpecificationRepository.findByMaterialIdOrderByDisplayOrderAsc(id);
        if (!usedInQuotations) {
            for (MaterialSpecification spec : specs) {
                if (quotationItemRepository.countByMaterialSpecificationId(spec.getId()) > 0) {
                    usedInQuotations = true;
                    break;
                }
            }
        }

        if (usedInQuotations) {
            // Unsafe to permanently delete because historical quotations depend on it
            material.setActive(false);
            materialRepository.saveAndFlush(material);
            for (MaterialOption opt : options) {
                opt.setActive(false);
                materialOptionRepository.saveAndFlush(opt);
            }
            for (MaterialSpecification spec : specs) {
                spec.setActive(false);
                materialSpecificationRepository.saveAndFlush(spec);
            }
            return "Material '" + matName + "' is associated with existing quotations. It has been safely archived and deactivated to protect quotation history.";
        }

        // 2. Safe to permanently delete from database
        try {
            // Unmap divisions from join table
            material.getDivisions().clear();
            materialRepository.save(material);

            // Delete pricing and specifications under options, then options
            for (MaterialOption opt : options) {
                List<Pricing> pricing = pricingRepository.findByMaterialOptionIdOrderByPricingModeAsc(opt.getId());
                if (!pricing.isEmpty()) {
                    pricingRepository.deleteAll(pricing);
                }
                List<MaterialSpecification> optSpecs = materialSpecificationRepository.findByMaterialOptionIdOrderByDisplayOrderAsc(opt.getId());
                if (!optSpecs.isEmpty()) {
                    materialSpecificationRepository.deleteAll(optSpecs);
                }
                materialOptionRepository.delete(opt);
            }

            // Delete direct specifications
            if (!specs.isEmpty()) {
                materialSpecificationRepository.deleteAll(specs);
            }

            // Delete material
            materialRepository.delete(material);
            return "Material '" + matName + "' deleted successfully.";
        } catch (Exception e) {
            // Fallback soft-delete if any unexpected constraint arises
            material.setActive(false);
            materialRepository.saveAndFlush(material);
            return "Material '" + matName + "' could not be permanently removed due to database constraints, but has been safely deactivated.";
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Material name is required"
            );
        }
    }

    private String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : com.example.pravin_quotation.util.TitleCaseUtils.toTitleCase(cleaned);
    }
}