package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
public class MasterDataHierarchyController {

    private final WorkCategoryRepository workCategoryRepository;
    private final DivisionRepository divisionRepository;
    private final MaterialRepository materialRepository;
    private final MaterialOptionRepository materialOptionRepository;
    private final MaterialSpecificationRepository materialSpecificationRepository;
    private final PricingRepository pricingRepository;

    public MasterDataHierarchyController(
            WorkCategoryRepository workCategoryRepository,
            DivisionRepository divisionRepository,
            MaterialRepository materialRepository,
            MaterialOptionRepository materialOptionRepository,
            MaterialSpecificationRepository materialSpecificationRepository,
            PricingRepository pricingRepository
    ) {
        this.workCategoryRepository = workCategoryRepository;
        this.divisionRepository = divisionRepository;
        this.materialRepository = materialRepository;
        this.materialOptionRepository = materialOptionRepository;
        this.materialSpecificationRepository = materialSpecificationRepository;
        this.pricingRepository = pricingRepository;
    }

    public static class HierarchyCategoryDto {
        private WorkCategory category;
        private List<HierarchyDivisionDto> divisions = new ArrayList<>();

        public WorkCategory getCategory() { return category; }
        public void setCategory(WorkCategory category) { this.category = category; }
        public List<HierarchyDivisionDto> getDivisions() { return divisions; }
        public void setDivisions(List<HierarchyDivisionDto> divisions) { this.divisions = divisions; }
    }

    public static class HierarchyDivisionDto {
        private Division division;
        private List<HierarchyMaterialDto> materials = new ArrayList<>();

        public Division getDivision() { return division; }
        public void setDivision(Division division) { this.division = division; }
        public List<HierarchyMaterialDto> getMaterials() { return materials; }
        public void setMaterials(List<HierarchyMaterialDto> materials) { this.materials = materials; }
    }

    public static class HierarchyMaterialDto {
        private Material material;
        private List<HierarchyOptionDto> options = new ArrayList<>();
        private List<MaterialSpecification> directSpecifications = new ArrayList<>();

        public Material getMaterial() { return material; }
        public void setMaterial(Material material) { this.material = material; }
        public List<HierarchyOptionDto> getOptions() { return options; }
        public void setOptions(List<HierarchyOptionDto> options) { this.options = options; }
        public List<MaterialSpecification> getDirectSpecifications() { return directSpecifications; }
        public void setDirectSpecifications(List<MaterialSpecification> directSpecifications) { this.directSpecifications = directSpecifications; }
    }

    public static class HierarchyOptionDto {
        private MaterialOption option;
        private List<Pricing> pricingList = new ArrayList<>();
        private List<MaterialSpecification> specifications = new ArrayList<>();

        public MaterialOption getOption() { return option; }
        public void setOption(MaterialOption option) { this.option = option; }
        public List<Pricing> getPricingList() { return pricingList; }
        public void setPricingList(List<Pricing> pricingList) { this.pricingList = pricingList; }
        public List<MaterialSpecification> getSpecifications() { return specifications; }
        public void setSpecifications(List<MaterialSpecification> specifications) { this.specifications = specifications; }
    }

    public HierarchyCategoryDto buildHierarchyForCategory(WorkCategory cat) {
        HierarchyCategoryDto catDto = new HierarchyCategoryDto();
        catDto.setCategory(cat);

        List<Division> divisions = divisionRepository.findByWorkCategoryIdAndActiveTrueOrderByDisplayOrderAsc(cat.getId());
        for (Division div : divisions) {
            HierarchyDivisionDto divDto = new HierarchyDivisionDto();
            divDto.setDivision(div);

            List<Material> materials = materialRepository.findActiveByDivisionId(div.getId());
            for (Material mat : materials) {
                HierarchyMaterialDto matDto = new HierarchyMaterialDto();
                matDto.setMaterial(mat);

                List<MaterialOption> options = materialOptionRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(mat.getId());
                for (MaterialOption opt : options) {
                    HierarchyOptionDto optDto = new HierarchyOptionDto();
                    optDto.setOption(opt);
                    optDto.setPricingList(pricingRepository.findByMaterialOptionIdOrderByPricingModeAsc(opt.getId()));

                    List<MaterialSpecification> specs = materialSpecificationRepository.findByMaterialOptionIdAndActiveTrueOrderByDisplayOrderAsc(opt.getId());
                    if (specs.isEmpty()) {
                        specs = materialSpecificationRepository.findByMaterialOptionIdOrderByDisplayOrderAsc(opt.getId());
                    }
                    if (specs.isEmpty() && opt.getDescription() != null && !opt.getDescription().isBlank()) {
                        String cleanName = opt.getDescription().trim().toUpperCase();
                        Optional<MaterialSpecification> existing = materialSpecificationRepository.findByMaterialOptionIdAndNameIgnoreCase(opt.getId(), cleanName);
                        MaterialSpecification spec = existing.orElseGet(() -> {
                            MaterialSpecification s = new MaterialSpecification();
                            s.setName(cleanName);
                            s.setDescription(opt.getDescription().trim());
                            s.setMaterial(mat);
                            s.setMaterialOption(opt);
                            s.setActive(true);
                            s.setDisplayOrder(0);
                            return materialSpecificationRepository.save(s);
                        });
                        specs = List.of(spec);
                    }
                    optDto.setSpecifications(specs);
                    matDto.getOptions().add(optDto);
                }

                List<MaterialSpecification> directSpecs = materialSpecificationRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(mat.getId())
                        .stream()
                        .filter(s -> s.getMaterialOption() == null)
                        .toList();
                matDto.setDirectSpecifications(directSpecs);
                divDto.getMaterials().add(matDto);
            }

            catDto.getDivisions().add(divDto);
        }
        return catDto;
    }

    public String viewCategoryDetails(Long id, Model model) {
        WorkCategory cat = workCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Work category not found: " + id));

        HierarchyCategoryDto catDto = buildHierarchyForCategory(cat);

        model.addAttribute("category", cat);
        model.addAttribute("catDto", catDto);
        model.addAttribute("categoryDto", catDto);
        return "admin/work-category-view";
    }

    @GetMapping({"/admin/master-data/hierarchy", "/admin/work-categories/hierarchy"})
    public String showHierarchy(
            @RequestParam(required = false) Long categoryId,
            Model model
    ) {
        if (model != null) {
            List<WorkCategory> categories = workCategoryRepository.findAllByOrderByDisplayOrderAsc();
            List<HierarchyCategoryDto> hierarchyList = new ArrayList<>();

            for (WorkCategory cat : categories) {
                if (categoryId != null && !cat.getId().equals(categoryId)) {
                    continue;
                }
                hierarchyList.add(buildHierarchyForCategory(cat));
            }

            model.addAttribute("hierarchyList", hierarchyList);
            model.addAttribute("categories", hierarchyList);
            model.addAttribute("allCategories", categories);
            model.addAttribute("selectedCategoryId", categoryId != null ? categoryId : "");
        }

        return "admin/master-data-hierarchy";
    }

    public String showHierarchy(Long categoryId) {
        return showHierarchy(categoryId, (Model) null);
    }
}
