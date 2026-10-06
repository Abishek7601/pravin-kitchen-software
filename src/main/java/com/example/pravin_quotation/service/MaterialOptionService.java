package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.model.MaterialOption;
import com.example.pravin_quotation.repository.MaterialOptionRepository;
import com.example.pravin_quotation.repository.MaterialRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaterialOptionService {

    private final MaterialOptionRepository materialOptionRepository;
    private final MaterialRepository materialRepository;

    public MaterialOptionService(
            MaterialOptionRepository materialOptionRepository,
            MaterialRepository materialRepository
    ) {
        this.materialOptionRepository = materialOptionRepository;
        this.materialRepository = materialRepository;
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

        String optionName = name.trim();

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

        return materialOptionRepository.save(option);
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

        if (materialId == null) {
            throw new IllegalArgumentException(
                    "Material is required"
            );
        }

        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Material not found"
                ));

        String optionName = name.trim();

        if (materialOptionRepository
                .existsByNameIgnoreCaseAndMaterialIdAndIdNot(
                        optionName,
                        materialId,
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

        return materialOptionRepository.save(option);
    }

    public void toggleOptionStatus(Long id) {

        MaterialOption option = getOptionById(id);

        option.setActive(
                !Boolean.TRUE.equals(option.getActive())
        );

        materialOptionRepository.save(option);
    }

    public void deleteOption(Long id) {

        MaterialOption option = getOptionById(id);

        materialOptionRepository.delete(option);
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