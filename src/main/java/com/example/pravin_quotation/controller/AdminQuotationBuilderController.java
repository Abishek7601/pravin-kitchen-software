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
            PricingRepository pricingRepository) {
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
    }

    @GetMapping("/builder")
    public String builder(
            @RequestParam(required = false) Long quotationId,
            @RequestParam(required = false) Long branchId,
            Model model) {
        model.addAttribute("branches", branchRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("customers", branchId != null
                ? customerRepository.findByBranchIdAndActiveTrueOrderByNameAsc(branchId)
                : customerRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("workCategories", workCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc());
        model.addAttribute("pricingModes", PricingMode.values());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("editQuotationId", quotationId != null ? quotationId : "");
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
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) String pricingGrade) {
        PricingMode grade = null;
        if (pricingGrade != null && !pricingGrade.isBlank()) {
            String g = pricingGrade.trim().toUpperCase();
            if (g.contains("PREMIUM")) grade = PricingMode.PREMIUM;
            else if (g.contains("ECONOMY")) grade = PricingMode.ECONOMY;
            else grade = PricingMode.MIDDLE;
        }

        List<Material> list = null;
        if (divisionId != null && grade != null) {
            list = materialRepository.findActiveByDivisionIdAndPricingGrade(divisionId, grade);
        }
        if (list == null || list.isEmpty()) {
            if (divisionId != null) {
                list = materialRepository.findActiveByDivisionId(divisionId);
            } else if (itemId != null) {
                list = materialRepository.findByItemIdAndActiveTrueOrderByDisplayOrderAsc(itemId);
            } else {
                list = materialRepository.findByActiveTrueOrderByDisplayOrderAsc();
            }
        }
        return list.stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("name", m.getName() != null ? m.getName().toUpperCase() : "");
            map.put("description", m.getDescription());
            map.put("specification", m.getSpecification() != null ? m.getSpecification() : (m.getDescription() != null ? m.getDescription() : ""));
            map.put("unit", m.getUnit() != null ? m.getUnit() : "Sq.ft");
            map.put("rate", m.getRate());
            map.put("pricingGrade", m.getPricingGrade() != null ? m.getPricingGrade().name() : "");
            map.put("formula", m.getFormula());
            return map;
        }).toList();
    }

    public List<Map<String, Object>> materials(Long itemId) {
        return materials(null, itemId, (String) null);
    }

    public List<Map<String, Object>> materials(Long divisionId, Long itemId) {
        return materials(divisionId, itemId, (String) null);
    }

    public ResponseEntity<?> rate(Long branchId, Long materialOptionId, PricingMode pricingMode) {
        return rate(branchId, materialOptionId, null, pricingMode);
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
            @RequestParam(required = false) Long materialOptionId,
            @RequestParam(required = false) Long materialId,
            @RequestParam PricingMode pricingMode) {
        try {
            BigDecimal rate = null;
            Material material = materialId != null ? materialRepository.findById(materialId).orElse(null) : null;
            MaterialOption option = materialOptionId != null ? materialOptionRepository.findById(materialOptionId).orElse(null) : null;

            if (materialOptionId != null) {
                try {
                    rate = builderService.resolveRate(branchId, materialOptionId, pricingMode);
                } catch (Exception ignored) {}
            }

            if (rate == null && material != null && material.getRate() != null) {
                rate = material.getRate();
            }

            if (rate == null && materialOptionId != null) {
                rate = builderService.resolveRate(branchId, materialOptionId, pricingMode);
            }

            if (rate == null) {
                rate = BigDecimal.ZERO;
            }

            Pricing pricing = materialOptionId != null ? pricingRepository.findByMaterialOptionIdAndPricingMode(materialOptionId, pricingMode).orElse(null) : null;

            String formula = (material != null && material.getFormula() != null && !material.getFormula().isBlank() && !"(L + W) × H".equals(material.getFormula()))
                    ? material.getFormula()
                    : ((pricing != null && pricing.getFormula() != null && !pricing.getFormula().isBlank())
                    ? pricing.getFormula()
                    : ((option != null && option.getMaterial() != null && option.getMaterial().getFormula() != null)
                    ? option.getMaterial().getFormula()
                    : "(L + W) × H"));

            String unit = (material != null && material.getUnit() != null) ? material.getUnit() : "SQFT";
            String measurementUnit = unit;
            String pricingUnit = "₹ / " + unit;

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("rate", rate);
            resp.put("pricingMode", pricingMode.name());
            resp.put("formula", formula);
            resp.put("unit", unit);
            resp.put("measurementUnit", measurementUnit);
            resp.put("pricingUnit", pricingUnit);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(map("message", e.getMessage()));
        }
    }

    @PostMapping("/builder/save")
    public String save(
            @RequestBody AdminQuotationBuilderService.BuilderRequest request,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Quotation quotation = builderService.save(request, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Quotation " + quotation.getQuotationNumber() + " created successfully.");
            return "redirect:/admin/quotations/view/" + quotation.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/quotations/builder";
        }
    }

    @GetMapping("/builder/load/{id}")
    @ResponseBody
    public ResponseEntity<?> loadQuotation(@PathVariable Long id) {
        return quotationRepository.findById(id).map(q -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", q.getId());
            data.put("quotationNumber", q.getQuotationNumber());
            data.put("customerId", q.getCustomer() != null ? q.getCustomer().getId() : null);
            data.put("customerName", q.getCustomer() != null ? q.getCustomer().getName() : "");
            data.put("phone", q.getCustomer() != null ? q.getCustomer().getPhone() : "");
            data.put("email", q.getCustomer() != null ? q.getCustomer().getEmail() : "");
            data.put("address", q.getCustomer() != null ? q.getCustomer().getAddress() : "");
            data.put("branchId", q.getBranch() != null ? q.getBranch().getId() : null);
            data.put("quotationDate", q.getQuotationDate() != null ? q.getQuotationDate().toString() : "");
            data.put("pricingMode", q.getPricingMode() != null ? q.getPricingMode().name() : "MIDDLE");
            data.put("customerRequirements", q.getCustomerRequirements() != null ? q.getCustomerRequirements() : "");
            data.put("notes", q.getNotes() != null ? q.getNotes() : "");
            data.put("totalAmount", q.getTotalAmount() != null ? q.getTotalAmount() : q.getGrandTotal());
            data.put("offerPercentage", q.getOfferPercentage() != null ? q.getOfferPercentage() : BigDecimal.ZERO);
            data.put("offerAmount", q.getOfferAmount() != null ? q.getOfferAmount() : BigDecimal.ZERO);
            data.put("grandTotal", q.getGrandTotal() != null ? q.getGrandTotal() : BigDecimal.ZERO);

            List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationIdAndActiveTrueOrderByIdAsc(id);
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
                    iMap.put("divisionId", item.getDivision() != null ? item.getDivision().getId() : null);
                    iMap.put("divisionName", item.getDivision() != null && item.getDivision().getName() != null ? item.getDivision().getName().toUpperCase() : "");
                    iMap.put("itemId", item.getItem() != null ? item.getItem().getId() : null);
                    iMap.put("itemName", item.getItem() != null && item.getItem().getName() != null ? item.getItem().getName().toUpperCase() : "");
                    iMap.put("materialId", item.getMaterial() != null ? item.getMaterial().getId() : null);
                    iMap.put("materialName", item.getMaterial() != null && item.getMaterial().getName() != null ? item.getMaterial().getName().toUpperCase() : "");
                    iMap.put("materialOptionId", item.getMaterialOption() != null ? item.getMaterialOption().getId() : null);
                    iMap.put("materialOptionName", item.getMaterialOption() != null && item.getMaterialOption().getName() != null ? item.getMaterialOption().getName().toUpperCase() : "");
                    iMap.put("materialSpecificationId", item.getMaterialSpecification() != null ? item.getMaterialSpecification().getId() : null);
                    iMap.put("materialSpecificationName", item.getMaterialSpecification() != null && item.getMaterialSpecification().getName() != null ? item.getMaterialSpecification().getName().toUpperCase() : "");
                    iMap.put("rate", item.getRate());
                    iMap.put("itemDescription", item.getItemDescription());
                    iMap.put("specification", item.getSpecification());

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
                    iMap.put("sizes", sizeList);
                    itemList.add(iMap);
                }
                rMap.put("items", itemList);
                roomList.add(rMap);
            }
            data.put("rooms", roomList);
            return ResponseEntity.ok(data);
        }).orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}