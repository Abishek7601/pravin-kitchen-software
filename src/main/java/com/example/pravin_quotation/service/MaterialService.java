package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Division;
import com.example.pravin_quotation.model.Item;
import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.repository.DivisionRepository;
import com.example.pravin_quotation.repository.ItemRepository;
import com.example.pravin_quotation.repository.MaterialRepository;
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
    private final com.example.pravin_quotation.repository.QuotationItemRepository quotationItemRepository;

    public MaterialService(
            MaterialRepository materialRepository,
            ItemRepository itemRepository,
            DivisionRepository divisionRepository,
            com.example.pravin_quotation.repository.QuotationItemRepository quotationItemRepository
    ) {
        this.materialRepository = materialRepository;
        this.itemRepository = itemRepository;
        this.divisionRepository = divisionRepository;
        this.quotationItemRepository = quotationItemRepository;
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
        String materialName = name.trim();

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

        String materialName = name.trim();
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
    public void deleteMaterial(Long id) {
        Material material = getMaterialById(id);
        if (!quotationItemRepository.findByMaterialIdOrderByCreatedAtDesc(id).isEmpty()) {
            material.setActive(false);
            materialRepository.save(material);
            return;
        }
        try {
            materialRepository.delete(material);
        } catch (Exception e) {
            material.setActive(false);
            materialRepository.save(material);
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
        return cleaned.isEmpty() ? null : cleaned;
    }
}