package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Item;
import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.repository.ItemRepository;
import com.example.pravin_quotation.repository.MaterialRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final ItemRepository itemRepository;

    public MaterialService(
            MaterialRepository materialRepository,
            ItemRepository itemRepository
    ) {
        this.materialRepository = materialRepository;
        this.itemRepository = itemRepository;
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

    public Material createMaterial(
            String name,
            String description,
            Long itemId,
            Integer displayOrder
    ) {

        validateName(name);

        if (itemId == null) {
            throw new IllegalArgumentException(
                    "Item is required"
            );
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item not found"
                ));

        String materialName = name.trim();

        if (materialRepository
                .existsByNameIgnoreCaseAndItemId(
                        materialName,
                        itemId
                )) {

            throw new IllegalArgumentException(
                    "Material already exists for this item"
            );
        }

        Material material = new Material();

        material.setName(materialName);
        material.setDescription(cleanValue(description));
        material.setItem(item);
        material.setActive(true);
        material.setDisplayOrder(
                displayOrder == null ? 0 : displayOrder
        );

        return materialRepository.save(material);
    }

    public Material updateMaterial(
            Long id,
            String name,
            String description,
            Long itemId,
            Integer displayOrder
    ) {

        Material material = getMaterialById(id);

        validateName(name);

        if (itemId == null) {
            throw new IllegalArgumentException(
                    "Item is required"
            );
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item not found"
                ));

        String materialName = name.trim();

        if (materialRepository
                .existsByNameIgnoreCaseAndItemIdAndIdNot(
                        materialName,
                        itemId,
                        id
                )) {

            throw new IllegalArgumentException(
                    "Another material with this name already exists for this item"
            );
        }

        material.setName(materialName);
        material.setDescription(cleanValue(description));
        material.setItem(item);
        material.setDisplayOrder(
                displayOrder == null ? 0 : displayOrder
        );

        return materialRepository.save(material);
    }

    public void toggleMaterialStatus(Long id) {

        Material material = getMaterialById(id);

        material.setActive(
                !Boolean.TRUE.equals(material.getActive())
        );

        materialRepository.save(material);
    }

    public void deleteMaterial(Long id) {

        Material material = getMaterialById(id);

        materialRepository.delete(material);
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