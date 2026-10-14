package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.model.MaterialOption;
import com.example.pravin_quotation.model.MaterialSpecification;
import com.example.pravin_quotation.repository.MaterialOptionRepository;
import com.example.pravin_quotation.repository.MaterialRepository;
import com.example.pravin_quotation.repository.MaterialSpecificationRepository;
import com.example.pravin_quotation.repository.QuotationItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MaterialSpecificationService {

    private final MaterialSpecificationRepository materialSpecificationRepository;
    private final MaterialRepository materialRepository;
    private final MaterialOptionRepository materialOptionRepository;
    private final QuotationItemRepository quotationItemRepository;

    public MaterialSpecificationService(
            MaterialSpecificationRepository materialSpecificationRepository,
            MaterialRepository materialRepository,
            MaterialOptionRepository materialOptionRepository,
            QuotationItemRepository quotationItemRepository
    ) {
        this.materialSpecificationRepository = materialSpecificationRepository;
        this.materialRepository = materialRepository;
        this.materialOptionRepository = materialOptionRepository;
        this.quotationItemRepository = quotationItemRepository;
    }

    public List<MaterialSpecification> getAllSpecifications() {
        return materialSpecificationRepository.findAll();
    }

    public List<MaterialSpecification> getActiveSpecifications() {
        return materialSpecificationRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public List<MaterialSpecification> getByMaterialOptionId(Long materialOptionId) {
        if (materialOptionId == null) return List.of();
        return materialSpecificationRepository.findByMaterialOptionIdAndActiveTrueOrderByDisplayOrderAsc(materialOptionId);
    }

    public List<MaterialSpecification> getByMaterialId(Long materialId) {
        if (materialId == null) return List.of();
        return materialSpecificationRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(materialId);
    }

    public Optional<MaterialSpecification> getById(Long id) {
        if (id == null) return Optional.empty();
        return materialSpecificationRepository.findById(id);
    }

    @Transactional
    public MaterialSpecification saveSpecification(
            Long materialId,
            Long materialOptionId,
            String name,
            String description,
            Integer displayOrder
    ) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Specification name is required");
        }
        String cleanName = name.trim().toUpperCase();

        Material material = null;
        if (materialId != null) {
            material = materialRepository.findById(materialId)
                    .orElseThrow(() -> new IllegalArgumentException("Material not found"));
        }

        MaterialOption materialOption = null;
        if (materialOptionId != null) {
            materialOption = materialOptionRepository.findById(materialOptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Material option not found"));
            if (material == null && materialOption.getMaterial() != null) {
                material = materialOption.getMaterial();
            }
        }

        MaterialSpecification spec = new MaterialSpecification();
        spec.setName(cleanName);
        spec.setDescription(description != null ? description.trim() : null);
        spec.setMaterial(material);
        spec.setMaterialOption(materialOption);
        spec.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        spec.setActive(true);

        return materialSpecificationRepository.save(spec);
    }

    @Transactional
    public MaterialSpecification updateSpecification(
            Long id,
            Long materialId,
            Long materialOptionId,
            String name,
            String description,
            Integer displayOrder,
            Boolean active
    ) {
        MaterialSpecification spec = materialSpecificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Specification not found"));

        if (name != null && !name.trim().isEmpty()) {
            spec.setName(name.trim().toUpperCase());
        }
        spec.setDescription(description != null ? description.trim() : null);

        if (materialId != null) {
            Material material = materialRepository.findById(materialId)
                    .orElseThrow(() -> new IllegalArgumentException("Material not found"));
            spec.setMaterial(material);
        }

        if (materialOptionId != null) {
            MaterialOption option = materialOptionRepository.findById(materialOptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Material option not found"));
            spec.setMaterialOption(option);
            if (spec.getMaterial() == null) {
                spec.setMaterial(option.getMaterial());
            }
        }

        if (displayOrder != null) {
            spec.setDisplayOrder(displayOrder);
        }
        if (active != null) {
            spec.setActive(active);
        }

        return materialSpecificationRepository.save(spec);
    }

    @Transactional
    public void toggleStatus(Long id) {
        MaterialSpecification spec = materialSpecificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Specification not found"));
        spec.setActive(!Boolean.TRUE.equals(spec.getActive()));
        materialSpecificationRepository.save(spec);
    }

    @Transactional
    public String deleteSpecification(Long id) {
        MaterialSpecification spec = materialSpecificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Specification not found"));

        long usageCount = quotationItemRepository.countByMaterialSpecificationId(id);
        if (usageCount > 0) {
            spec.setActive(false);
            materialSpecificationRepository.save(spec);
            return "Specification is referenced by " + usageCount + " quotation item(s). It has been safely deactivated instead of deleted.";
        }

        materialSpecificationRepository.delete(spec);
        return "Specification deleted successfully.";
    }

    @Transactional
    public MaterialSpecification findOrCreateSpecification(Material material, MaterialOption materialOption, String specName) {
        if (specName == null || specName.trim().isEmpty()) {
            return null;
        }
        String cleanName = specName.trim().toUpperCase();

        if (materialOption != null) {
            Optional<MaterialSpecification> existing = materialSpecificationRepository.findByMaterialOptionIdAndNameIgnoreCase(materialOption.getId(), cleanName);
            if (existing.isPresent()) {
                return existing.get();
            }
        } else if (material != null) {
            Optional<MaterialSpecification> existing = materialSpecificationRepository.findByMaterialIdAndNameIgnoreCase(material.getId(), cleanName);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        MaterialSpecification spec = new MaterialSpecification();
        spec.setName(cleanName);
        spec.setMaterial(material);
        spec.setMaterialOption(materialOption);
        spec.setActive(true);
        spec.setDisplayOrder(0);
        return materialSpecificationRepository.save(spec);
    }
}
