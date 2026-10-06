package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Division;
import com.example.pravin_quotation.model.WorkCategory;
import com.example.pravin_quotation.repository.DivisionRepository;
import com.example.pravin_quotation.repository.WorkCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DivisionService {

    private final DivisionRepository divisionRepository;
    private final WorkCategoryRepository workCategoryRepository;

    public DivisionService(
            DivisionRepository divisionRepository,
            WorkCategoryRepository workCategoryRepository
    ) {
        this.divisionRepository = divisionRepository;
        this.workCategoryRepository = workCategoryRepository;
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

        String divisionName = name.trim();

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

        String divisionName = name.trim();

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

    public void deleteDivision(Long id) {
        Division division = getDivisionById(id);

        divisionRepository.delete(division);
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