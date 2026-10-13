package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
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
    private final WorkCategoryRepository workCategoryRepository;
    private final DivisionRepository divisionRepository;
    private final ItemRepository itemRepository;
    private final MaterialRepository materialRepository;
    private final MaterialOptionRepository materialOptionRepository;
    private final AdminQuotationBuilderService builderService;
    private final QuotationRepository quotationRepository;
    private final QuotationRoomRepository quotationRoomRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final QuotationItemSizeRepository quotationItemSizeRepository;
    private final PricingRepository pricingRepository;

    public AdminQuotationBuilderController(
            BranchRepository branchRepository,
            WorkCategoryRepository workCategoryRepository,
            DivisionRepository divisionRepository,
            ItemRepository itemRepository,
            MaterialRepository materialRepository,
            MaterialOptionRepository materialOptionRepository,
            AdminQuotationBuilderService builderService,
            QuotationRepository quotationRepository,
            QuotationRoomRepository quotationRoomRepository,
            QuotationItemRepository quotationItemRepository,
            QuotationItemSizeRepository quotationItemSizeRepository,
            PricingRepository pricingRepository) {
        this.branchRepository = branchRepository;
        this.workCategoryRepository = workCategoryRepository;
        this.divisionRepository = divisionRepository;
        this.itemRepository = itemRepository;
        this.materialRepository = materialRepository;
        this.materialOptionRepository = materialOptionRepository;
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
        model.addAttribute("workCategories", workCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc());
        model.addAttribute("pricingModes", PricingMode.values());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("editQuotationId", quotationId != null ? quotationId : "");
        model.addAttribute("selectedBranchId", branchId != null ? branchId : "");
        return "admin/quotation-builder";
    }

    @GetMapping("/builder/divisions")
    @ResponseBody
    public List<Map<String, Object>> divisions(@RequestParam Long workCategoryId) {
        return divisionRepository.findByWorkCategoryIdAndActiveTrueOrderByDisplayOrderAsc(workCategoryId)
                .stream().map(d -> map("id", d.getId(), "name", d.getName(), "description", d.getDescription()))
                .toList();
    }

    @GetMapping("/builder/items")
    @ResponseBody
    public List<Map<String, Object>> items(@RequestParam Long divisionId) {
        return itemRepository.findByDivisionIdAndActiveTrueOrderByDisplayOrderAsc(divisionId)
                .stream().map(i -> map("id", i.getId(), "name", i.getName(), "description", i.getDescription()))
                .toList();
    }

    @GetMapping("/builder/materials")
    @ResponseBody
    public List<Map<String, Object>> materials(@RequestParam Long itemId) {
        return materialRepository.findByItemIdAndActiveTrueOrderByDisplayOrderAsc(itemId)
                .stream().map(m -> map("id", m.getId(), "name", m.getName(), "description", m.getDescription()))
                .toList();
    }

    @GetMapping("/builder/material-options")
    @ResponseBody
    public List<Map<String, Object>> materialOptions(@RequestParam Long materialId) {
        return materialOptionRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(materialId)
                .stream().map(o -> map("id", o.getId(), "name", o.getName(), "description", o.getDescription()))
                .toList();
    }

    @GetMapping("/builder/rate")
    @ResponseBody
    public ResponseEntity<?> rate(
            @RequestParam(required = false) Long branchId,
            @RequestParam Long materialOptionId,
            @RequestParam PricingMode pricingMode) {
        try {
            BigDecimal rate = builderService.resolveRate(branchId, materialOptionId, pricingMode);
            Pricing pricing = pricingRepository.findByMaterialOptionIdAndPricingMode(materialOptionId, pricingMode).orElse(null);
            String formula = (pricing != null && pricing.getFormula() != null && !pricing.getFormula().isBlank())
                    ? pricing.getFormula() : "(L + W) × H";
            String unit = (pricing != null && pricing.getUnit() != null) ? pricing.getUnit() : "SQFT";
            String measurementUnit = ("INCH".equalsIgnoreCase(unit) || "INCHES".equalsIgnoreCase(unit) || "SQIN".equalsIgnoreCase(unit)) ? "SQIN" : "SQFT";
            String pricingUnit = "₹ / " + measurementUnit;

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
            data.put("customerName", q.getCustomer() != null ? q.getCustomer().getName() : "");
            data.put("phone", q.getCustomer() != null ? q.getCustomer().getPhone() : "");
            data.put("email", q.getCustomer() != null ? q.getCustomer().getEmail() : "");
            data.put("address", q.getCustomer() != null ? q.getCustomer().getAddress() : "");
            data.put("branchId", q.getBranch() != null ? q.getBranch().getId() : null);
            data.put("quotationDate", q.getQuotationDate() != null ? q.getQuotationDate().toString() : "");
            data.put("pricingMode", q.getPricingMode() != null ? q.getPricingMode().name() : "MIDDLE");
            data.put("customerRequirements", q.getCustomerRequirements() != null ? q.getCustomerRequirements() : "");
            data.put("notes", q.getNotes() != null ? q.getNotes() : "");

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
                    iMap.put("categoryName", item.getWorkCategory() != null ? item.getWorkCategory().getName() : "");
                    iMap.put("divisionId", item.getDivision() != null ? item.getDivision().getId() : null);
                    iMap.put("divisionName", item.getDivision() != null ? item.getDivision().getName() : "");
                    iMap.put("itemId", item.getItem() != null ? item.getItem().getId() : null);
                    iMap.put("itemName", item.getItem() != null ? item.getItem().getName() : "");
                    iMap.put("materialId", item.getMaterial() != null ? item.getMaterial().getId() : null);
                    iMap.put("materialName", item.getMaterial() != null ? item.getMaterial().getName() : "");
                    iMap.put("materialOptionId", item.getMaterialOption() != null ? item.getMaterialOption().getId() : null);
                    iMap.put("materialOptionName", item.getMaterialOption() != null ? item.getMaterialOption().getName() : "");
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