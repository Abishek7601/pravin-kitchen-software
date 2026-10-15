package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DivisionService {

    private final DivisionRepository divisionRepository;
    private final WorkCategoryRepository workCategoryRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final QuotationRepository quotationRepository;
    private final ItemRepository itemRepository;
    private final MaterialRepository materialRepository;
    private final MaterialOptionRepository materialOptionRepository;
    private final PricingRepository pricingRepository;
    private final MaterialSpecificationRepository materialSpecificationRepository;

    public DivisionService(
            DivisionRepository divisionRepository,
            WorkCategoryRepository workCategoryRepository,
            QuotationItemRepository quotationItemRepository,
            QuotationRepository quotationRepository,
            ItemRepository itemRepository,
            MaterialRepository materialRepository,
            MaterialOptionRepository materialOptionRepository,
            PricingRepository pricingRepository,
            MaterialSpecificationRepository materialSpecificationRepository
    ) {
        this.divisionRepository = divisionRepository;
        this.workCategoryRepository = workCategoryRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.quotationRepository = quotationRepository;
        this.itemRepository = itemRepository;
        this.materialRepository = materialRepository;
        this.materialOptionRepository = materialOptionRepository;
        this.pricingRepository = pricingRepository;
        this.materialSpecificationRepository = materialSpecificationRepository;
    }

    public List<Division> getAllDivisions() {
        return divisionRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<Division> getActiveDivisions() {
        return divisionRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public Division getDivisionById(Long id) {
        return divisionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Division not found"
                ));
    }

    public List<Division> getDivisionsByCategory(Long categoryId) {
        return divisionRepository
                .findByWorkCategoryIdOrderByDisplayOrderAsc(categoryId);
    }

    public List<Division> getActiveDivisionsByCategory(Long categoryId) {
        return divisionRepository
                .findByWorkCategoryIdAndActiveTrueOrderByDisplayOrderAsc(
                        categoryId
                );
    }

    public Division createDivision(
            String name,
            String description,
            Long workCategoryId,
            Integer displayOrder
    ) {
        validateName(name);

        if (workCategoryId == null) {
            throw new IllegalArgumentException(
                    "Work category is required"
            );
        }

        WorkCategory workCategory = workCategoryRepository.findById(
                workCategoryId
        ).orElseThrow(() -> new IllegalArgumentException(
                "Work category not found"
        ));

        String divisionName = name.trim().toUpperCase();

        if (divisionRepository
                .existsByNameIgnoreCaseAndWorkCategoryId(
                        divisionName,
                        workCategoryId
                )) {

            throw new IllegalArgumentException(
                    "Division already exists in this work category"
            );
        }

        Division division = new Division();

        division.setName(divisionName);
        division.setDescription(cleanValue(description));
        division.setWorkCategory(workCategory);
        division.setActive(true);
        division.setDisplayOrder(
                displayOrder == null ? 0 : displayOrder
        );

        return divisionRepository.save(division);
    }

    public Division updateDivision(
            Long id,
            String name,
            String description,
            Long workCategoryId,
            Integer displayOrder
    ) {
        Division division = getDivisionById(id);

        validateName(name);

        if (workCategoryId == null) {
            throw new IllegalArgumentException(
                    "Work category is required"
            );
        }

        WorkCategory workCategory = workCategoryRepository.findById(
                workCategoryId
        ).orElseThrow(() -> new IllegalArgumentException(
                "Work category not found"
        ));

        String divisionName = name.trim().toUpperCase();

        if (divisionRepository
                .existsByNameIgnoreCaseAndWorkCategoryIdAndIdNot(
                        divisionName,
                        workCategoryId,
                        id
                )) {

            throw new IllegalArgumentException(
                    "Another division with this name already exists in this work category"
            );
        }

        division.setName(divisionName);
        division.setDescription(cleanValue(description));
        division.setWorkCategory(workCategory);
        division.setDisplayOrder(
                displayOrder == null ? 0 : displayOrder
        );

        return divisionRepository.save(division);
    }

    public void toggleDivisionStatus(Long id) {
        Division division = getDivisionById(id);

        division.setActive(
                !Boolean.TRUE.equals(division.getActive())
        );

        divisionRepository.save(division);
    }

    @Transactional
    public String deleteDivision(Long id) {
        Division division = getDivisionById(id);
        String divName = division.getName();

        // 1. Check if division or any items/materials under it are used in quotations
        boolean usedInQuotations = quotationItemRepository.countByDivisionId(id) > 0
                || quotationRepository.countByDivisionId(id) > 0;

        List<Item> items = itemRepository.findByDivisionIdOrderByDisplayOrderAsc(id);
        if (!usedInQuotations) {
            for (Item item : items) {
                if (!quotationItemRepository.findByItemIdOrderByCreatedAtDesc(item.getId()).isEmpty()) {
                    usedInQuotations = true;
                    break;
                }
            }
        }

        List<Material> materials = materialRepository.findAllByDivisionId(id);
        if (!usedInQuotations) {
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
                }
                if (usedInQuotations) break;
            }
        }

        if (usedInQuotations) {
            division.setActive(false);
            divisionRepository.saveAndFlush(division);
            return "Division '" + divName + "' is associated with existing quotations. It has been safely archived and deactivated to protect quotation history.";
        }

        // 2. Safe to permanently delete
        try {
            // Remove division from material associations
            for (Material mat : materials) {
                mat.getDivisions().remove(division);
                if (mat.getDivisions().isEmpty() && (mat.getItem() == null || id.equals(mat.getItem().getDivision().getId()))) {
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

            // Remove items under division
            if (!items.isEmpty()) {
                itemRepository.deleteAll(items);
            }

            // Delete division
            divisionRepository.delete(division);
            divisionRepository.flush();
            return "Division '" + divName + "' deleted successfully.";
        } catch (Exception e) {
            division.setActive(false);
            divisionRepository.saveAndFlush(division);
            return "Division '" + divName + "' could not be permanently removed due to database constraints, but has been safely deactivated.";
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Division name is required"
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