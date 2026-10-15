package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.model.MaterialOption;
import com.example.pravin_quotation.model.MaterialSpecification;
import com.example.pravin_quotation.model.Pricing;
import com.example.pravin_quotation.repository.MaterialOptionRepository;
import com.example.pravin_quotation.repository.MaterialRepository;
import com.example.pravin_quotation.repository.MaterialSpecificationRepository;
import com.example.pravin_quotation.repository.PricingRepository;
import com.example.pravin_quotation.repository.QuotationItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MaterialOptionService {

    private final MaterialOptionRepository materialOptionRepository;
    private final MaterialRepository materialRepository;
    private final MaterialSpecificationRepository materialSpecificationRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final PricingRepository pricingRepository;

    public MaterialOptionService(
            MaterialOptionRepository materialOptionRepository,
            MaterialRepository materialRepository,
            MaterialSpecificationRepository materialSpecificationRepository,
            QuotationItemRepository quotationItemRepository,
            PricingRepository pricingRepository
    ) {
        this.materialOptionRepository = materialOptionRepository;
        this.materialRepository = materialRepository;
        this.materialSpecificationRepository = materialSpecificationRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.pricingRepository = pricingRepository;
    }

    public List<MaterialOption> getAllOptions() {
        return materialOptionRepository
                .findAllByOrderByDisplayOrderAsc();
    }

    public List<MaterialOption> getActiveOptions() {
        return materialOptionRepository
                .findByActiveTrueOrderByDisplayOrderAsc();
    }

    public MaterialOption getOptionById(Long id) {
        return materialOptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Material option not found"
                ));
    }

    public List<MaterialOption> getOptionsByMaterial(Long materialId) {
        return materialOptionRepository
                .findByMaterialIdOrderByDisplayOrderAsc(materialId);
    }

    public List<MaterialOption> getActiveOptionsByMaterial(Long materialId) {
        return materialOptionRepository
                .findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(
                        materialId
                );
    }

    public MaterialOption createOption(
            String name,
            String description,
            Long materialId,
            Integer displayOrder
    ) {

        validateName(name);

        if (materialId == null) {
            throw new IllegalArgumentException(
                    "Material is required"
            );
        }

        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Material not found"
                ));

        String optionName = name.trim().toUpperCase();

        if (materialOptionRepository
                .existsByNameIgnoreCaseAndMaterialId(
                        optionName,
                        materialId
                )) {

            throw new IllegalArgumentException(
                    "Material option already exists for this material"
            );
        }

        MaterialOption option = new MaterialOption();

        option.setName(optionName);
        option.setDescription(cleanValue(description));
        option.setMaterial(material);
        option.setActive(true);
        option.setDisplayOrder(
                displayOrder == null ? 0 : displayOrder
        );

        MaterialOption savedOption = materialOptionRepository.save(option);
        syncSpecifications(savedOption, description, material);
        return savedOption;
    }

    public MaterialOption updateOption(
            Long id,
            String name,
            String description,
            Long materialId,
            Integer displayOrder
    ) {

        MaterialOption option = getOptionById(id);

        validateName(name);

        Material material;
        if (materialId != null) {
            material = materialRepository.findById(materialId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Material not found"
                    ));
        } else {
            material = option.getMaterial();
        }

        if (material == null) {
            throw new IllegalArgumentException(
                    "Material is required"
            );
        }

        String optionName = name.trim().toUpperCase();

        if (materialOptionRepository
                .existsByNameIgnoreCaseAndMaterialIdAndIdNot(
                        optionName,
                        material.getId(),
                        id
                )) {

            throw new IllegalArgumentException(
                    "Another material option with this name already exists for this material"
            );
        }

        option.setName(optionName);
        option.setDescription(cleanValue(description));
        option.setMaterial(material);
        option.setDisplayOrder(
                displayOrder == null ? 0 : displayOrder
        );

        MaterialOption updatedOption = materialOptionRepository.save(option);
        syncSpecifications(updatedOption, description, material);
        return updatedOption;
    }

    public void toggleOptionStatus(Long id) {

        MaterialOption option = getOptionById(id);

        boolean newActive = !Boolean.TRUE.equals(option.getActive());
        option.setActive(newActive);

        materialOptionRepository.save(option);

        // Synchronize active status on linked specifications
        List<MaterialSpecification> specs =
                materialSpecificationRepository.findByMaterialOptionIdOrderByDisplayOrderAsc(id);
        for (MaterialSpecification s : specs) {
            s.setActive(newActive);
            materialSpecificationRepository.save(s);
        }
    }

    @Transactional
    public String deleteOption(Long id) {
        MaterialOption option = getOptionById(id);
        String optName = option.getName();

        // 1. Check if option or any specifications under it are used in quotations
        boolean usedInQuotations = quotationItemRepository.countByMaterialOptionId(id) > 0;

        List<MaterialSpecification> specs =
                materialSpecificationRepository.findByMaterialOptionIdOrderByDisplayOrderAsc(id);
        if (!usedInQuotations) {
            for (MaterialSpecification s : specs) {
                if (quotationItemRepository.countByMaterialSpecificationId(s.getId()) > 0) {
                    usedInQuotations = true;
                    break;
                }
            }
        }

        if (usedInQuotations) {
            option.setActive(false);
            materialOptionRepository.saveAndFlush(option);
            for (MaterialSpecification s : specs) {
                s.setActive(false);
                materialSpecificationRepository.saveAndFlush(s);
            }
            return "Material option '" + optName + "' is associated with existing quotations. It has been safely archived and deactivated to protect quotation history.";
        }

        // 2. Safe to permanently delete
        try {
            // Delete pricing under this option
            List<Pricing> pricing = pricingRepository.findByMaterialOptionIdOrderByPricingModeAsc(id);
            if (!pricing.isEmpty()) {
                pricingRepository.deleteAll(pricing);
            }

            // Delete specifications under this option
            if (!specs.isEmpty()) {
                materialSpecificationRepository.deleteAll(specs);
            }

            // Delete option
            materialOptionRepository.delete(option);
            materialOptionRepository.flush();
            return "Material option '" + optName + "' deleted successfully.";
        } catch (Exception e) {
            option.setActive(false);
            materialOptionRepository.saveAndFlush(option);
            return "Material option '" + optName + "' could not be permanently removed due to database constraints, but has been safely deactivated.";
        }
    }

    private void syncSpecifications(MaterialOption option, String description, Material material) {
        if (option == null || option.getId() == null) {
            return;
        }

        List<MaterialSpecification> existingSpecs =
                materialSpecificationRepository.findByMaterialOptionIdOrderByDisplayOrderAsc(option.getId());

        String cleanDesc = cleanValue(description);

        if (cleanDesc != null && !cleanDesc.isBlank()) {
            String specName = cleanDesc.toUpperCase();
            if (!existingSpecs.isEmpty()) {
                // Update the primary existing specification in place
                MaterialSpecification primary = existingSpecs.get(0);
                primary.setName(specName);
                primary.setDescription(cleanDesc);
                primary.setMaterial(material != null ? material : option.getMaterial());
                primary.setMaterialOption(option);
                primary.setActive(true);
                materialSpecificationRepository.save(primary);

                // Ensure any secondary specs remain associated with the correct material
                for (int i = 1; i < existingSpecs.size(); i++) {
                    MaterialSpecification extra = existingSpecs.get(i);
                    extra.setMaterial(material != null ? material : option.getMaterial());
                    materialSpecificationRepository.save(extra);
                }
            } else {
                // Create and link a new MaterialSpecification
                MaterialSpecification newSpec = new MaterialSpecification();
                newSpec.setName(specName);
                newSpec.setDescription(cleanDesc);
                newSpec.setMaterial(material != null ? material : option.getMaterial());
                newSpec.setMaterialOption(option);
                newSpec.setActive(true);
                newSpec.setDisplayOrder(0);
                materialSpecificationRepository.save(newSpec);
            }
        } else {
            // Description was cleared; keep existing specs associated with material
            for (MaterialSpecification spec : existingSpecs) {
                spec.setDescription(null);
                if (material != null) {
                    spec.setMaterial(material);
                }
                materialSpecificationRepository.save(spec);
            }
        }
    }

    private void validateName(String name) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Material option name is required"
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