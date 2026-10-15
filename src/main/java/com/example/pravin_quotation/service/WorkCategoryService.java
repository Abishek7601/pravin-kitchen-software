package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        this.workCategoryRepository = workCategoryRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.quotationRepository = quotationRepository;
        this.divisionRepository = divisionRepository;
        this.itemRepository = itemRepository;
        this.materialRepository = materialRepository;
        this.materialOptionRepository = materialOptionRepository;
        this.pricingRepository = pricingRepository;
        this.materialSpecificationRepository = materialSpecificationRepository;
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

        category.setIcon(
                cleanValue(icon)
        );

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

        category.setIcon(
                cleanValue(icon)
        );

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