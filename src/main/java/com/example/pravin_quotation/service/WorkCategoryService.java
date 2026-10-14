package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.WorkCategory;
import com.example.pravin_quotation.repository.QuotationItemRepository;
import com.example.pravin_quotation.repository.WorkCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkCategoryService {

    private final WorkCategoryRepository workCategoryRepository;
    private final QuotationItemRepository quotationItemRepository;

    public WorkCategoryService(
            WorkCategoryRepository workCategoryRepository,
            QuotationItemRepository quotationItemRepository) {

        this.workCategoryRepository = workCategoryRepository;
        this.quotationItemRepository = quotationItemRepository;
    }


    public List<WorkCategory> getAllCategories() {

        return workCategoryRepository
                .findAllByOrderByDisplayOrderAsc();
    }


    public List<WorkCategory> getActiveCategories() {

        return workCategoryRepository
                .findByActiveTrueOrderByDisplayOrderAsc();
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


    public void deleteCategory(Long id) {
        WorkCategory category = getCategoryById(id);
        long quotationCount = quotationItemRepository.countByWorkCategoryId(id);
        if (quotationCount > 0) {
            category.setActive(false);
            workCategoryRepository.save(category);
            return;
        }
        try {
            workCategoryRepository.delete(category);
        } catch (Exception e) {
            category.setActive(false);
            workCategoryRepository.save(category);
        }
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