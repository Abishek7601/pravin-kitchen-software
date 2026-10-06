package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Division;
import com.example.pravin_quotation.model.Item;
import com.example.pravin_quotation.repository.DivisionRepository;
import com.example.pravin_quotation.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final DivisionRepository divisionRepository;

    public ItemService(
            ItemRepository itemRepository,
            DivisionRepository divisionRepository
    ) {
        this.itemRepository = itemRepository;
        this.divisionRepository = divisionRepository;
    }

    public List<Item> getAllItems() {
        return itemRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<Item> getActiveItems() {
        return itemRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public Item getItemById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item not found"
                ));
    }

    public List<Item> getItemsByDivision(Long divisionId) {
        return itemRepository
                .findByDivisionIdOrderByDisplayOrderAsc(divisionId);
    }

    public List<Item> getActiveItemsByDivision(Long divisionId) {
        return itemRepository
                .findByDivisionIdAndActiveTrueOrderByDisplayOrderAsc(
                        divisionId
                );
    }

    public Item createItem(
            String name,
            String description,
            Long divisionId,
            Integer displayOrder
    ) {

        validateName(name);

        if (divisionId == null) {
            throw new IllegalArgumentException(
                    "Division is required"
            );
        }

        Division division = divisionRepository.findById(divisionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Division not found"
                ));

        String itemName = name.trim();

        if (itemRepository
                .existsByNameIgnoreCaseAndDivisionId(
                        itemName,
                        divisionId
                )) {

            throw new IllegalArgumentException(
                    "Item already exists in this division"
            );
        }

        Item item = new Item();

        item.setName(itemName);
        item.setDescription(cleanValue(description));
        item.setDivision(division);
        item.setActive(true);
        item.setDisplayOrder(
                displayOrder == null ? 0 : displayOrder
        );

        return itemRepository.save(item);
    }

    public Item updateItem(
            Long id,
            String name,
            String description,
            Long divisionId,
            Integer displayOrder
    ) {

        Item item = getItemById(id);

        validateName(name);

        if (divisionId == null) {
            throw new IllegalArgumentException(
                    "Division is required"
            );
        }

        Division division = divisionRepository.findById(divisionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Division not found"
                ));

        String itemName = name.trim();

        if (itemRepository
                .existsByNameIgnoreCaseAndDivisionIdAndIdNot(
                        itemName,
                        divisionId,
                        id
                )) {

            throw new IllegalArgumentException(
                    "Another item with this name already exists in this division"
            );
        }

        item.setName(itemName);
        item.setDescription(cleanValue(description));
        item.setDivision(division);
        item.setDisplayOrder(
                displayOrder == null ? 0 : displayOrder
        );

        return itemRepository.save(item);
    }

    public void toggleItemStatus(Long id) {

        Item item = getItemById(id);

        item.setActive(
                !Boolean.TRUE.equals(item.getActive())
        );

        itemRepository.save(item);
    }

    public void deleteItem(Long id) {

        Item item = getItemById(id);

        itemRepository.delete(item);
    }

    private void validateName(String name) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Item name is required"
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