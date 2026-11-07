package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.WorkCategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/quotations")
public class AdminQuotationBuilderController {

    private final BranchRepository branchRepository;
    private final CustomerRepository customerRepository;
    private final WorkCategoryRepository workCategoryRepository;
    private final DivisionRepository divisionRepository;
    private final ItemRepository itemRepository;
    private final MaterialRepository materialRepository;
    private final MaterialOptionRepository materialOptionRepository;
    private final MaterialSpecificationRepository materialSpecificationRepository;
    private final AdminQuotationBuilderService builderService;
    private final QuotationRepository quotationRepository;
    private final QuotationRoomRepository quotationRoomRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final QuotationItemSizeRepository quotationItemSizeRepository;
    private final PricingRepository pricingRepository;
    private final com.example.pravin_quotation.service.FileUploadService fileUploadService;

    public AdminQuotationBuilderController(
            BranchRepository branchRepository,
            CustomerRepository customerRepository,
            WorkCategoryRepository workCategoryRepository,
            DivisionRepository divisionRepository,
            ItemRepository itemRepository,
            MaterialRepository materialRepository,
            MaterialOptionRepository materialOptionRepository,
            MaterialSpecificationRepository materialSpecificationRepository,
            AdminQuotationBuilderService builderService,
            QuotationRepository quotationRepository,
            QuotationRoomRepository quotationRoomRepository,
            QuotationItemRepository quotationItemRepository,
            QuotationItemSizeRepository quotationItemSizeRepository,
            PricingRepository pricingRepository,
            com.example.pravin_quotation.service.FileUploadService fileUploadService) {
        this.branchRepository = branchRepository;
        this.customerRepository = customerRepository;
        this.workCategoryRepository = workCategoryRepository;
        this.divisionRepository = divisionRepository;
        this.itemRepository = itemRepository;
        this.materialRepository = materialRepository;
        this.materialOptionRepository = materialOptionRepository;
        this.materialSpecificationRepository = materialSpecificationRepository;
        this.builderService = builderService;
        this.quotationRepository = quotationRepository;
        this.quotationRoomRepository = quotationRoomRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.quotationItemSizeRepository = quotationItemSizeRepository;
        this.pricingRepository = pricingRepository;
        this.fileUploadService = fileUploadService;
    }

    @GetMapping({"/builder", "/builder/{quotationId:[0-9]+}"})
    public String builder(
            @PathVariable(required = false) Long quotationId,
            @RequestParam(value = "quotationId", required = false) Long paramQuotationId,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "parentId", required = false) Long parentId,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(required = false) Long branchId,
            Model model) {
        Long effectiveQuotationId = quotationId != null ? quotationId : (paramQuotationId != null ? paramQuotationId : id);
        return builderInternal(effectiveQuotationId, parentId, grade, branchId, model);
    }

    public String builder(Long quotationId, Long branchId, Model model) {
        return builderInternal(quotationId, null, null, branchId, model);
    }

    public String builder(Long branchId, Model model) {
        return builderInternal(null, null, null, branchId, model);
    }

    private String builderInternal(Long effectiveQuotationId, Long parentId, String grade, Long branchId, Model model) {
        model.addAttribute("branches", branchRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("customers", branchId != null
                ? customerRepository.findByBranchIdAndActiveTrueOrderByNameAsc(branchId)
                : customerRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("workCategories", workCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc());
        model.addAttribute("pricingModes", PricingMode.values());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("editQuotationId", effectiveQuotationId != null ? effectiveQuotationId : "");
        model.addAttribute("parentId", parentId != null ? parentId : "");
        model.addAttribute("targetGrade", grade != null ? grade.toUpperCase() : "");
        model.addAttribute("selectedBranchId", branchId != null ? branchId : "");
        return "admin/quotation-builder";
    }

    @GetMapping("/builder/customers")
    @ResponseBody
    public List<Map<String, Object>> customers(@RequestParam(required = false) Long branchId) {
        List<Customer> list;
        if (branchId != null) {
            list = customerRepository.findByBranchIdAndActiveTrueOrderByNameAsc(branchId);
        } else {
            list = customerRepository.findByActiveTrueOrderByNameAsc();
        }
        return list.stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            map.put("phone", c.getPhone());
            map.put("email", c.getEmail());
            map.put("address", c.getAddress());
            map.put("branchId", c.getBranch() != null ? c.getBranch().getId() : null);
            map.put("branchName", c.getBranch() != null ? c.getBranch().getName() : "");
            return map;
        }).toList();
    }

    @GetMapping("/builder/divisions")
    @ResponseBody
    public List<Map<String, Object>> divisions(@RequestParam Long workCategoryId) {
        return divisionRepository.findByWorkCategoryIdAndActiveTrueOrderByDisplayOrderAsc(workCategoryId)
                .stream().map(d -> {
                    String emoji = d.getIcon() != null && !d.getIcon().isBlank()
                            ? d.getIcon().trim()
                            : WorkCategoryService.resolveWorkTypeEmoji(d.getName(), "📁");
                    return map("id", d.getId(),
                            "name", d.getName() != null ? d.getName().toUpperCase() : "",
                            "emoji", emoji,
                            "icon", emoji,
                            "description", d.getDescription());
                })
                .toList();
    }

    @GetMapping("/builder/items")
    @ResponseBody
    public List<Map<String, Object>> items(@RequestParam Long divisionId) {
        return itemRepository.findByDivisionIdAndActiveTrueOrderByDisplayOrderAsc(divisionId)
                .stream().map(i -> map("id", i.getId(), "name", i.getName() != null ? i.getName().toUpperCase() : "", "description", i.getDescription()))
                .toList();
    }

    @GetMapping("/builder/materials")
    @ResponseBody
    public List<Map<String, Object>> materials(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) String pricingGrade,
            @RequestParam(required = false) Boolean onlyAvailable) {
        PricingMode grade = null;
        if (pricingGrade != null && !pricingGrade.isBlank()) {
            String g = pricingGrade.trim().toUpperCase();
            if (g.contains("PREMIUM")) grade = PricingMode.PREMIUM;
            else if (g.contains("ECONOMY")) grade = PricingMode.ECONOMY;
            else grade = PricingMode.MIDDLE;
        }

        List<Material> list = null;
        if (divisionId != null) {
            if (grade != null) {
                List<Material> allInDiv = materialRepository.findActiveByDivisionId(divisionId);
                final PricingMode filterGrade = grade;
                final boolean requireAvailable = Boolean.TRUE.equals(onlyAvailable);

                list = allInDiv.stream()
                        .filter(m -> {
                            if (m.getPricingGrade() == filterGrade) {
                                return true;
                            }
                            if (m.getPricingGrade() == null) {
                                return !requireAvailable || builderService.hasPositiveRateForGrade(branchId, m, filterGrade);
                            }
                            return false;
                        })
                        .toList();
            } else {
                list = materialRepository.findActiveByDivisionId(divisionId);
            }
        } else if (itemId != null) {
            List<Material> itemMats = materialRepository.findByItemIdAndActiveTrueOrderByDisplayOrderAsc(itemId);
            if (grade != null) {
                final PricingMode filterGrade = grade;
                final boolean requireAvailable = Boolean.TRUE.equals(onlyAvailable);
                list = itemMats.stream()
                        .filter(m -> {
                            if (m.getPricingGrade() == filterGrade) {
                                return true;
                            }
                            if (m.getPricingGrade() == null) {
                                return !requireAvailable || builderService.hasPositiveRateForGrade(branchId, m, filterGrade);
                            }
                            return false;
                        })
                        .toList();
            } else {
                list = itemMats;
            }
        } else {
            List<Material> allMats = materialRepository.findByActiveTrueOrderByDisplayOrderAsc();
            if (grade != null) {
                final PricingMode filterGrade = grade;
                final boolean requireAvailable = Boolean.TRUE.equals(onlyAvailable);
                list = allMats.stream()
                        .filter(m -> {
                            if (m.getPricingGrade() == filterGrade) {
                                return true;
                            }
                            if (m.getPricingGrade() == null) {
                                return !requireAvailable || builderService.hasPositiveRateForGrade(branchId, m, filterGrade);
                            }
                            return false;
                        })
                        .toList();
            } else {
                list = allMats;
            }
        }
        if (list != null) {
            list = list.stream().filter(m -> Boolean.TRUE.equals(m.getActive())).toList();
        } else {
            list = Collections.emptyList();
        }
        final PricingMode targetGrade = grade;
        List<Map<String, Object>> result = list.stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("name", m.getName() != null ? m.getName().toUpperCase() : "");
            map.put("description", m.getDescription());
            map.put("specification", m.getSpecification() != null ? m.getSpecification().trim() : "");
            map.put("unit", m.getUnit() != null ? m.getUnit() : "Sq.ft");
            boolean isMaterialActive = Boolean.TRUE.equals(m.getActive());
            map.put("active", isMaterialActive);

            AdminQuotationBuilderService.ResolvedMaterialRate res = null;
            if (targetGrade != null && isMaterialActive) {
                try {
                    Long effectiveDivId = divisionId != null ? divisionId : (m.getDivision() != null ? m.getDivision().getId() : null);
                    res = builderService.resolveMaterialAndRate(branchId, effectiveDivId, m.getId(), null, targetGrade);
                } catch (Exception ignored) {}
            }

            boolean isAvailable = isMaterialActive && (res != null && res.available() && res.rate() != null && res.rate().compareTo(BigDecimal.ZERO) > 0);
            BigDecimal resolvedRate = isAvailable ? res.rate() : null;
            if (isMaterialActive && targetGrade == null && resolvedRate == null) {
                resolvedRate = m.getRate();
                isAvailable = (resolvedRate != null && resolvedRate.compareTo(BigDecimal.ZERO) > 0);
            }

            String gradeLabel = targetGrade == PricingMode.PREMIUM ? "Premium" : (targetGrade == PricingMode.ECONOMY ? "Economy" : "Middle");
            String unavailableMsg = !isMaterialActive
                    ? "Material is inactive"
                    : ((res != null && res.message() != null) ? res.message() : ("No material available in " + gradeLabel + " Grade"));

            map.put("available", isAvailable);
            map.put("rate", resolvedRate);
            map.put("unavailableMessage", isAvailable ? null : unavailableMsg);
            map.put("message", isAvailable ? null : unavailableMsg);
            map.put("pricingGrade", targetGrade != null ? targetGrade.name() : (m.getPricingGrade() != null ? m.getPricingGrade().name() : ""));
            map.put("formula", (res != null && res.formula() != null) ? res.formula() : m.getFormula());
            map.put("measurementType", (res != null && res.measurementType() != null) ? res.measurementType().name() : (m.getMeasurementType() != null ? m.getMeasurementType().name() : "SQFT"));
            map.put("coveragePerRoll", m.getCoveragePerRoll());
            map.put("imageUrl", isAvailable ? m.getImageUrl() : null);
            return map;
        }).toList();

        if (Boolean.TRUE.equals(onlyAvailable)) {
            result = result.stream()
                    .filter(m -> Boolean.TRUE.equals(m.get("active")) && Boolean.TRUE.equals(m.get("available")) && m.get("rate") != null)
                    .toList();
        }
        return result;
    }

    public List<Map<String, Object>> materials(
            Long branchId,
            Long divisionId,
            Long itemId,
            String pricingGrade) {
        return materials(branchId, divisionId, itemId, pricingGrade, false);
    }

    public List<Map<String, Object>> materials(Long itemId) {
        return materials(null, null, itemId, (String) null);
    }

    public List<Map<String, Object>> materials(Long divisionId, Long itemId) {
        return materials(null, divisionId, itemId, (String) null);
    }

    public List<Map<String, Object>> materials(Long divisionId, Long itemId, String pricingGrade) {
        return materials(null, divisionId, itemId, pricingGrade);
    }

    public ResponseEntity<?> rate(Long branchId, Long materialOptionId, PricingMode pricingMode) {
        return rate(branchId, null, materialOptionId, null, pricingMode);
    }

    public ResponseEntity<?> rate(Long branchId, Long materialOptionId, Long materialId, PricingMode pricingMode) {
        return rate(branchId, null, materialOptionId, materialId, pricingMode);
    }

    @GetMapping("/builder/material-options")
    @ResponseBody
    public List<Map<String, Object>> materialOptions(@RequestParam Long materialId) {
        return materialOptionRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(materialId)
                .stream().map(o -> map("id", o.getId(), "name", o.getName() != null ? o.getName().toUpperCase() : "", "description", o.getDescription()))
                .toList();
    }

    @GetMapping("/builder/material-specifications")
    @ResponseBody
    public List<Map<String, Object>> materialSpecifications(
            @RequestParam(required = false) Long materialOptionId,
            @RequestParam(required = false) Long materialId) {
        List<MaterialSpecification> specs;
        if (materialOptionId != null) {
            specs = materialSpecificationRepository.findByMaterialOptionIdAndActiveTrueOrderByDisplayOrderAsc(materialOptionId);
        } else if (materialId != null) {
            specs = materialSpecificationRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(materialId);
        } else {
            specs = List.of();
        }
        return specs.stream().map(s -> map(
                "id", s.getId(),
                "name", s.getName() != null ? s.getName().toUpperCase() : "",
                "description", s.getDescription()
        )).toList();
    }

    @GetMapping("/builder/rate")
    @ResponseBody
    public ResponseEntity<?> rate(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long materialOptionId,
            @RequestParam(required = false) Long materialId,
            @RequestParam PricingMode pricingMode) {
        try {
            AdminQuotationBuilderService.ResolvedMaterialRate res =
                    builderService.resolveMaterialAndRate(branchId, divisionId, materialId, materialOptionId, pricingMode);
            if (!res.available() || res.rate() == null || res.rate().compareTo(BigDecimal.ZERO) <= 0) {
                String gradeLabel = pricingMode == PricingMode.PREMIUM ? "Premium" : (pricingMode == PricingMode.ECONOMY ? "Economy" : "Middle");
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("available", false);
                resp.put("message", res.message() != null ? res.message() : ("No material available in " + gradeLabel + " Grade"));
                resp.put("rate", null);
                resp.put("materialId", materialId);
                return ResponseEntity.ok(resp);
            }

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("available", true);
            resp.put("rate", res.rate());
            resp.put("pricingMode", pricingMode.name());
            resp.put("materialId", res.materialId());
            resp.put("materialOptionId", res.materialOptionId());
            resp.put("materialName", res.materialName());
            resp.put("formula", res.formula());
            resp.put("unit", res.unit());
            resp.put("measurementUnit", res.unit());
            resp.put("pricingUnit", "₹ / " + res.unit());
            resp.put("measurementType", res.measurementType() != null ? res.measurementType().name() : "SQFT");
            resp.put("imageUrl", res.imageUrl());
            resp.put("coveragePerRoll", res.coveragePerRoll());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "available", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to resolve rate for this branch and grade."
            ));
        }
    }

    @PostMapping("/builder/save")
    public Object save(
            @RequestBody AdminQuotationBuilderService.BuilderRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        boolean acceptsJson = httpRequest != null
                && httpRequest.getHeader("Accept") != null
                && httpRequest.getHeader("Accept").contains(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
        try {
            Quotation quotation = builderService.save(request, authentication.getName());
            Long redirectId = (quotation.getParent() != null) ? quotation.getParent().getId() : quotation.getId();
            String redirectUrl = "/admin/quotations/view/" + redirectId;
            if (acceptsJson) {
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("redirectUrl", redirectUrl);
                resp.put("quotationId", quotation.getId());
                resp.put("parentId", quotation.getParent() != null ? quotation.getParent().getId() : redirectId);
                resp.put("quotationNumber", quotation.getQuotationNumber());
                return ResponseEntity.ok(resp);
            }
            redirectAttributes.addFlashAttribute("successMessage",
                    "Quotation " + quotation.getQuotationNumber() + " saved successfully.");
            return "redirect:" + redirectUrl;
        } catch (Exception e) {
            if (acceptsJson) {
                Map<String, Object> errResp = new LinkedHashMap<>();
                errResp.put("success", false);
                errResp.put("message", e.getMessage() != null ? e.getMessage() : "Unable to save quotation.");
                return ResponseEntity.badRequest().body(errResp);
            }
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/quotations/builder";
        }
    }

    @PostMapping("/builder/auto-save")
    @ResponseBody
    public ResponseEntity<?> autoSave(
            @RequestBody AdminQuotationBuilderService.BuilderRequest request,
            Authentication authentication) {
        try {
            Quotation quotation = builderService.autoSave(request, authentication.getName());
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("quotationId", quotation.getId());
            resp.put("parentId", quotation.getParent() != null ? quotation.getParent().getId() : null);
            resp.put("quotationNumber", quotation.getQuotationNumber());
            resp.put("status", quotation.getStatus() != null ? quotation.getStatus().name() : "PENDING");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> errResp = new LinkedHashMap<>();
            errResp.put("success", false);
            errResp.put("message", e.getMessage() != null ? e.getMessage() : "Unable to auto-save quotation draft.");
            return ResponseEntity.badRequest().body(errResp);
        }
    }

    public ResponseEntity<?> loadQuotation(Long id) {
        return loadQuotation(id, null);
    }

    @GetMapping("/builder/load/{id}")
    @ResponseBody
    public ResponseEntity<?> loadQuotation(
            @PathVariable Long id,
            @RequestParam(required = false) String grade) {
        Quotation requested = quotationRepository.findById(id).orElse(null);
        if (requested == null) {
            return ResponseEntity.notFound().build();
        }

        Quotation parent = requested.getParent() != null ? requested.getParent() : requested;

        PricingMode targetMode = null;
        if (grade != null && !grade.isBlank()) {
            try {
                targetMode = PricingMode.valueOf(grade.trim().toUpperCase());
            } catch (Exception ignored) {}
        }

        Quotation q;
        boolean isPrepared = true;
        if (targetMode != null) {
            Quotation child = null;
            if (parent.getId() != null) {
                child = quotationRepository.findByParentIdAndPricingMode(parent.getId(), targetMode).orElse(null);
                if (child == null) {
                    String expectedNo = (parent.getRawQuotationNumber() != null ? parent.getRawQuotationNumber() : parent.getQuotationNumber()) + "-" + targetMode.name();
                    child = quotationRepository.findByQuotationNumber(expectedNo).orElse(null);
                }
            }
            if (child == null) {
                child = parent.getChildForGrade(targetMode);
            }
            if (child != null) {
                q = child;
                isPrepared = true;
            } else if (!parent.isParentQuotation() && parent.getPricingMode() == targetMode) {
                q = parent;
                isPrepared = true;
            } else {
                q = new Quotation();
                q.setParent(parent);
                q.setQuotationNumber(parent.getQuotationNumber() + "-" + targetMode.name());
                q.setCustomer(parent.getCustomer());
                q.setBranch(parent.getBranch());
                q.setEmployee(parent.getEmployee());
                q.setQuotationDate(parent.getQuotationDate());
                q.setValidUntil(parent.getValidUntil());
                q.setPricingMode(targetMode);
                q.setStatus(QuotationStatus.PENDING);
                isPrepared = false;
            }
        } else if (requested.isParentQuotation()) {
            PricingMode pick = parent.getSelectedGrade() != null
                    ? parent.getSelectedGrade()
                    : (parent.getChildQuotations() != null && !parent.getChildQuotations().isEmpty()
                            ? parent.getChildQuotations().get(0).getPricingMode()
                            : (PricingMode.values().length > 0 ? PricingMode.values()[0] : PricingMode.PREMIUM));
            q = parent.getChildForGrade(pick);
            if (q == null) q = requested;
        } else {
            q = requested;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", isPrepared ? q.getId() : null);
        data.put("parentId", parent != null && parent != q ? parent.getId() : (q.getParent() != null ? q.getParent().getId() : null));
        data.put("isPrepared", isPrepared);
        data.put("quotationNumber", parent != null ? parent.getQuotationNumber() : q.getQuotationNumber());
        data.put("selectedGrade", parent != null ? (parent.getSelectedGrade() != null ? parent.getSelectedGrade().name() : null) : null);

        List<String> preparedGrades = new ArrayList<>();
        if (parent != null && parent.getChildQuotations() != null) {
            for (Quotation c : parent.getChildQuotations()) {
                if (c.getPricingMode() != null) {
                    preparedGrades.add(c.getPricingMode().name());
                }
            }
        }
        data.put("preparedGrades", preparedGrades);

        data.put("status", q.getStatus() != null ? q.getStatus().name() : "PENDING");
        data.put("customerId", q.getCustomer() != null ? q.getCustomer().getId() : null);
        data.put("customerName", q.getCustomer() != null ? q.getCustomer().getName() : "");
        data.put("phone", q.getCustomer() != null ? q.getCustomer().getPhone() : "");
        data.put("email", q.getCustomer() != null ? q.getCustomer().getEmail() : "");
        data.put("address", q.getCustomer() != null ? q.getCustomer().getAddress() : "");
        data.put("branchId", q.getBranch() != null ? q.getBranch().getId() : null);
        data.put("quotationDate", q.getQuotationDate() != null ? q.getQuotationDate().toString() : "");
        data.put("validUntil", q.getValidUntil() != null ? q.getValidUntil().toString() : "");
        data.put("pricingMode", q.getPricingMode() != null ? q.getPricingMode().name() : (PricingMode.values().length > 0 ? PricingMode.values()[0].name() : "PREMIUM"));
        data.put("customerRequirements", q.getCustomerRequirements() != null ? q.getCustomerRequirements() : "");
        data.put("notes", q.getNotes() != null ? q.getNotes() : "");
        data.put("subtotal", q.getSubtotal() != null ? q.getSubtotal() : BigDecimal.ZERO);
        data.put("accessoriesAmount", q.getAccessoriesAmount() != null ? q.getAccessoriesAmount() : BigDecimal.ZERO);
        data.put("travelCharge", q.getTravelCharge() != null ? q.getTravelCharge() : BigDecimal.ZERO);
        data.put("otherCharges", q.getOtherCharges() != null ? q.getOtherCharges() : BigDecimal.ZERO);
        data.put("otherChargesDescription", q.getOtherChargesDescription() != null ? q.getOtherChargesDescription() : "");
        data.put("discountAmount", q.getDiscountAmount() != null ? q.getDiscountAmount() : BigDecimal.ZERO);
        data.put("taxableAmount", q.getTaxableAmount() != null ? q.getTaxableAmount() : BigDecimal.ZERO);
        data.put("gstPercentage", q.getGstPercentage() != null ? q.getGstPercentage() : BigDecimal.ZERO);
        data.put("gstAmount", q.getGstAmount() != null ? q.getGstAmount() : BigDecimal.ZERO);
        data.put("totalAmount", q.getTotalAmount() != null ? q.getTotalAmount() : q.getGrandTotal());
        data.put("offerPercentage", q.getOfferPercentage() != null ? q.getOfferPercentage() : BigDecimal.ZERO);
        data.put("offerAmount", q.getOfferAmount() != null ? q.getOfferAmount() : BigDecimal.ZERO);
        data.put("grandTotal", q.getGrandTotal() != null ? q.getGrandTotal() : BigDecimal.ZERO);
        data.put("showSqftSize", q.getShowSqftSize() != null ? q.getShowSqftSize() : true);

        data.put("lastActiveStep", q.getLastActiveStep());
        data.put("lastActiveFloor", q.getLastActiveFloor());
        data.put("lastActiveRoom", q.getLastActiveRoom());
        data.put("lastActiveCategoryId", q.getLastActiveCategoryId());
        data.put("lastActiveDivisionId", q.getLastActiveDivisionId());
        data.put("builderState", q.getBuilderState());

        Long branchId = q.getBranch() != null ? q.getBranch().getId() : null;
        PricingMode pricingGrade = q.getPricingMode() != null ? q.getPricingMode() : (PricingMode.values().length > 0 ? PricingMode.values()[0] : PricingMode.PREMIUM);

        List<QuotationRoom> rooms = (isPrepared && q.getId() != null)
                ? quotationRoomRepository.findByQuotationIdAndActiveTrueOrderByIdAsc(q.getId())
                : java.util.Collections.emptyList();
            List<Map<String, Object>> roomList = new ArrayList<>();
            for (QuotationRoom room : rooms) {
                Map<String, Object> rMap = new LinkedHashMap<>();
                rMap.put("floor", room.getFloor());
                rMap.put("room", room.getRoom());
                rMap.put("workDescription", room.getWorkDescription());
                List<QuotationItem> items = quotationItemRepository.findByQuotationRoomIdAndActiveTrueOrderByIdAsc(room.getId());
                List<Map<String, Object>> itemList = new ArrayList<>();
                for (QuotationItem item : items) {
                    Map<String, Object> iMap = new LinkedHashMap<>();
                    iMap.put("categoryId", item.getWorkCategory() != null ? item.getWorkCategory().getId() : null);
                    iMap.put("categoryName", item.getWorkCategory() != null && item.getWorkCategory().getName() != null ? item.getWorkCategory().getName().toUpperCase() : "");

                    Long divId = item.getDivision() != null ? item.getDivision().getId()
                            : (item.getMaterial() != null && item.getMaterial().getDivision() != null ? item.getMaterial().getDivision().getId() : null);
                    String divName = item.getDivision() != null && item.getDivision().getName() != null ? item.getDivision().getName().toUpperCase()
                            : (item.getMaterial() != null && item.getMaterial().getDivision() != null && item.getMaterial().getDivision().getName() != null ? item.getMaterial().getDivision().getName().toUpperCase() : "");
                    iMap.put("divisionId", divId);
                    iMap.put("divisionName", divName);

                    iMap.put("itemId", item.getItem() != null ? item.getItem().getId() : null);
                    iMap.put("itemName", item.getItem() != null && item.getItem().getName() != null ? item.getItem().getName().toUpperCase() : "");
                    iMap.put("materialId", item.getMaterial() != null ? item.getMaterial().getId() : null);
                    iMap.put("materialName", item.getMaterial() != null && item.getMaterial().getName() != null ? item.getMaterial().getName().toUpperCase() : "");
                    iMap.put("materialOptionId", item.getMaterialOption() != null ? item.getMaterialOption().getId() : null);
                    iMap.put("materialOptionName", item.getMaterialOption() != null && item.getMaterialOption().getName() != null ? item.getMaterialOption().getName().toUpperCase() : "");
                    iMap.put("materialSpecificationId", item.getMaterialSpecification() != null ? item.getMaterialSpecification().getId() : null);
                    iMap.put("materialSpecificationName", item.getMaterialSpecification() != null && item.getMaterialSpecification().getName() != null ? item.getMaterialSpecification().getName().toUpperCase() : "");

                    // Validate rate & availability against current Pricing Mode and Branch
                    boolean rateChanged = false;
                    boolean materialAvailable = true;
                    String availabilityMessage = null;
                    BigDecimal currentRate = null;

                    if (item.getMaterial() != null) {
                        Material m = item.getMaterial();
                        if (!Boolean.TRUE.equals(m.getActive())) {
                            materialAvailable = false;
                            availabilityMessage = "Material '" + m.getName() + "' is inactive";
                        } else {
                            try {
                                Long optId = item.getMaterialOption() != null ? item.getMaterialOption().getId() : null;
                                AdminQuotationBuilderService.ResolvedMaterialRate res =
                                        builderService.resolveMaterialAndRate(branchId, divId, m.getId(), optId, pricingGrade);
                                if (!res.available() || res.rate() == null || res.rate().compareTo(BigDecimal.ZERO) <= 0) {
                                    materialAvailable = false;
                                    availabilityMessage = res.message() != null ? res.message() : "Rate unavailable for selected branch and grade";
                                } else {
                                    currentRate = res.rate();
                                    if (item.getRate() != null && currentRate.compareTo(item.getRate()) != 0) {
                                        rateChanged = true;
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }

                    iMap.put("materialAvailable", materialAvailable);
                    iMap.put("availabilityMessage", availabilityMessage);
                    iMap.put("currentRate", currentRate);
                    iMap.put("rateChanged", rateChanged);

                    iMap.put("rate", (rateChanged && currentRate != null) ? currentRate : item.getRate());
                    iMap.put("unit", (item.getMaterial() != null && item.getMaterial().getUnit() != null) ? item.getMaterial().getUnit() : (item.getMeasurementType() == MeasurementType.FILES ? "Files" : (item.getMeasurementType() == MeasurementType.QUANTITY ? "Quantity" : "Sq.ft")));
                    iMap.put("itemDescription", item.getItemDescription());
                    iMap.put("specification", item.getSpecification());
                    iMap.put("measurementType", item.getMeasurementType() != null ? item.getMeasurementType().name() : "SQFT");
                    iMap.put("quantity", item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE);
                    iMap.put("imageUrl", item.getImageUrl());
                    iMap.put("coveragePerRoll", item.getCoveragePerRoll() != null ? item.getCoveragePerRoll() : (item.getMaterial() != null ? item.getMaterial().getCoveragePerRoll() : null));
                    iMap.put("calculatedSqft", item.getCalculatedSqft());
                    iMap.put("amount", item.getAmount());

                    String itemFormula = (item.getFormula() != null && !item.getFormula().isBlank())
                            ? item.getFormula()
                            : ((item.getMaterial() != null && item.getMaterial().getFormula() != null && !item.getMaterial().getFormula().isBlank())
                                    ? item.getMaterial().getFormula()
                                    : "(L + W) × H");
                    iMap.put("formula", itemFormula);

                    List<QuotationItemSize> sizes = quotationItemSizeRepository.findByQuotationItemIdAndActiveTrueOrderByDisplayOrderAsc(item.getId());
                    List<Map<String, Object>> sizeList = new ArrayList<>();
                    for (QuotationItemSize s : sizes) {
                        Map<String, Object> sMap = new LinkedHashMap<>();
                        sMap.put("length", s.getLengthValue());
                        sMap.put("width", s.getWidthValue());
                        sMap.put("height", s.getHeightValue());
                        sMap.put("offerPrice", s.getOfferPrice());
                        sizeList.add(sMap);
                    }
                    if (sizeList.isEmpty() && (item.getMeasurementType() == MeasurementType.QUANTITY || item.getMeasurementType() == MeasurementType.FILES)) {
                        Map<String, Object> sMap = new LinkedHashMap<>();
                        sMap.put("length", BigDecimal.ZERO);
                        sMap.put("width", BigDecimal.ZERO);
                        sMap.put("height", BigDecimal.ZERO);
                        sMap.put("offerPrice", item.getOfferPrice() != null ? item.getOfferPrice() : item.getAmount());
                        sizeList.add(sMap);
                    }
                    iMap.put("sizes", sizeList);
                    itemList.add(iMap);
                }
                rMap.put("items", itemList);
                roomList.add(rMap);
            }
            data.put("rooms", roomList);
            return ResponseEntity.ok(data);
    }

    @PostMapping("/builder/upload-image")
    @ResponseBody
    public ResponseEntity<?> uploadItemImage(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String url = fileUploadService.storeFile(file, "quotation-items");
            return ResponseEntity.ok(Map.of("success", true, "url", url));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage() != null ? e.getMessage() : "Failed to upload image."));
        }
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}