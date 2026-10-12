package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.DivisionRepository;
import com.example.pravin_quotation.repository.ItemRepository;
import com.example.pravin_quotation.repository.MaterialOptionRepository;
import com.example.pravin_quotation.repository.MaterialRepository;
import com.example.pravin_quotation.repository.PricingRepository;
import com.example.pravin_quotation.repository.WorkCategoryRepository;
import com.example.pravin_quotation.util.CalculationFormula;
import com.example.pravin_quotation.service.QuotationCalculationService;
import com.example.pravin_quotation.service.QuotationItemService;
import com.example.pravin_quotation.service.QuotationItemSizeService;
import com.example.pravin_quotation.service.QuotationRoomService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/quotation-items")
public class QuotationItemController {

    private final QuotationItemService quotationItemService;
    private final QuotationItemSizeService quotationItemSizeService;
    private final QuotationRoomService quotationRoomService;
    private final QuotationCalculationService quotationCalculationService;

    private final WorkCategoryRepository workCategoryRepository;
    private final DivisionRepository divisionRepository;
    private final ItemRepository itemRepository;
    private final MaterialRepository materialRepository;
    private final MaterialOptionRepository materialOptionRepository;
    private final PricingRepository pricingRepository;

    public QuotationItemController(
            QuotationItemService quotationItemService,
            QuotationItemSizeService quotationItemSizeService,
            QuotationRoomService quotationRoomService,
            QuotationCalculationService quotationCalculationService,
            WorkCategoryRepository workCategoryRepository,
            DivisionRepository divisionRepository,
            ItemRepository itemRepository,
            MaterialRepository materialRepository,
            MaterialOptionRepository materialOptionRepository,
            PricingRepository pricingRepository) {

        this.quotationItemService = quotationItemService;
        this.quotationItemSizeService = quotationItemSizeService;
        this.quotationRoomService = quotationRoomService;
        this.quotationCalculationService = quotationCalculationService;

        this.workCategoryRepository = workCategoryRepository;
        this.divisionRepository = divisionRepository;
        this.itemRepository = itemRepository;
        this.materialRepository = materialRepository;
        this.materialOptionRepository = materialOptionRepository;
        this.pricingRepository = pricingRepository;
    }

    // =========================================================
    // LIST ITEMS
    // =========================================================

    @GetMapping("/room/{roomId}")
    public String listItems(
            @PathVariable Long roomId,
            Model model) {

        QuotationRoom room = quotationRoomService.getById(roomId);

        if (room == null) {
            throw new IllegalArgumentException(
                    "Quotation room not found: " + roomId
            );
        }

        List<QuotationItem> items =
                quotationItemService.getActiveByQuotationRoomId(roomId);

        model.addAttribute("quotationRoom", room);
        model.addAttribute("quotation", room.getQuotation());
        model.addAttribute("items", items);

        return "admin/quotation-items";
    }

    // =========================================================
    // NEW ITEM FORM
    // =========================================================
    @GetMapping("/new/{roomId}")
    public String showItemForm(
            @PathVariable Long roomId,
            Model model) {

        QuotationRoom room =
                quotationRoomService.getById(roomId);

        if (room == null) {
            throw new IllegalArgumentException(
                    "Quotation room not found: " + roomId
            );
        }

        // Load categories, divisions, items and materials
        loadFormData(model);

        // Add room and quotation details
        model.addAttribute("quotationRoom", room);
        model.addAttribute("quotation", room.getQuotation());

        // Create an empty form object
        QuotationItem quotationItem = new QuotationItem();
        quotationItem.setQuotationRoom(room);

        model.addAttribute("quotationItem", quotationItem);

        return "admin/quotation-item-form";
    }

    // =========================================================
    // SAVE ITEM
    // =========================================================

    @PostMapping("/save")
    @Transactional
    public String saveItem(
            @RequestParam Long quotationRoomId,

            @RequestParam Long workCategoryId,
            @RequestParam Long divisionId,
            @RequestParam Long itemId,
            @RequestParam Long materialId,
            @RequestParam Long materialOptionId,

            @RequestParam(required = false)
            String itemDescription,

            @RequestParam(required = false)
            List<BigDecimal> lengthValues,

            @RequestParam(required = false)
            List<BigDecimal> widthValues,

            @RequestParam(required = false)
            List<BigDecimal> heightValues,

            @RequestParam(required = false)
            BigDecimal offerPrice,

            @RequestParam(required = false)
            String specification,

            @RequestParam(required = false)
            String formula,

            @RequestParam(required = false, defaultValue = "ft")
            String dimensionUnit,

            Model model) {

        try {

            // -------------------------------------------------
            // Load room
            // -------------------------------------------------

            QuotationRoom room =
                    quotationRoomService.getById(quotationRoomId);

            if (room == null) {
                throw new IllegalArgumentException(
                        "Quotation room not found.");
            }

            // -------------------------------------------------
            // Load master data
            // -------------------------------------------------

            WorkCategory workCategory =
                    workCategoryRepository.findById(workCategoryId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Work category not found."));

            Division division =
                    divisionRepository.findById(divisionId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Division not found."));

            Item item =
                    itemRepository.findById(itemId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Item not found."));

            Material material =
                    materialRepository.findById(materialId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Material not found."));

            MaterialOption materialOption =
                    materialOptionRepository.findById(materialOptionId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Material option not found."));

            // -------------------------------------------------
            // Resolve formula
            // -------------------------------------------------

            Quotation quotation = room.getQuotation();
            Long quotationId = quotation.getId();

            if (formula == null || formula.trim().isEmpty()) {
                Pricing pricing = pricingRepository
                        .findByMaterialOptionIdAndPricingMode(
                                materialOptionId,
                                quotation.getPricingMode()
                        ).orElse(null);
                if (pricing != null && pricing.getFormula() != null && !pricing.getFormula().trim().isEmpty()) {
                    formula = pricing.getFormula();
                } else if (materialOption.getFormula() != null && !materialOption.getFormula().trim().isEmpty()) {
                    formula = materialOption.getFormula();
                } else {
                    formula = CalculationFormula.FORMULA_L_X_W;
                }
            }
            formula = CalculationFormula.normalizeFormula(formula);

            // Pad width and height lists if needed
            int sizeCount = lengthValues != null ? lengthValues.size() : 0;
            if (widthValues == null) {
                widthValues = new ArrayList<>();
            }
            while (widthValues.size() < sizeCount) {
                widthValues.add(BigDecimal.ZERO);
            }
            if (heightValues == null) {
                heightValues = new ArrayList<>();
            }
            while (heightValues.size() < sizeCount) {
                heightValues.add(BigDecimal.ZERO);
            }

            // -------------------------------------------------
            // Validate size lists
            // -------------------------------------------------

            validateSizeLists(
                    formula,
                    lengthValues,
                    widthValues,
                    heightValues
            );

            // -------------------------------------------------
            // Calculate total SQ.FT
            // -------------------------------------------------

            BigDecimal totalSqft =
                    calculateTotalSqft(
                            formula,
                            dimensionUnit,
                            lengthValues,
                            widthValues,
                            heightValues
                    );

            // -------------------------------------------------
            // Resolve server-side rate
            // -------------------------------------------------

            BigDecimal rate =
                    quotationCalculationService.resolveItemRate(
                            quotationId,
                            materialOptionId
                    );

            // -------------------------------------------------
            // Calculate amount
            // -------------------------------------------------

            BigDecimal amount =
                    quotationItemService.calculateAmount(
                            totalSqft,
                            rate
                    );

            // -------------------------------------------------
            // Calculate offer price
            // -------------------------------------------------

            BigDecimal finalOfferPrice =
                    quotationItemService.calculateOfferPrice(
                            amount,
                            offerPrice
                    );

            // -------------------------------------------------
            // Parent item keeps first size for backward compatibility
            // -------------------------------------------------

            BigDecimal firstLength =
                    lengthValues.get(0);

            BigDecimal firstWidth =
                    widthValues.get(0);

            BigDecimal firstHeight =
                    heightValues.get(0);

            // -------------------------------------------------
            // Create parent quotation item
            // -------------------------------------------------

            QuotationItem savedItem =
                    quotationItemService.create(
                            room,
                            workCategory,
                            division,
                            item,
                            material,
                            materialOption,
                            itemDescription,

                            firstLength,
                            firstWidth,
                            firstHeight,

                            totalSqft,
                            rate,
                            amount,
                            finalOfferPrice,

                            formula,
                            specification
                    );

            // -------------------------------------------------
            // Create all size rows
            // -------------------------------------------------

            for (int i = 0; i < lengthValues.size(); i++) {

                QuotationItemSize savedSize =
                        quotationItemSizeService.create(
                                savedItem,
                                lengthValues.get(i),
                                widthValues.get(i),
                                heightValues.get(i),

                                null,

                                i + 1,
                                dimensionUnit
                        );

                savedItem.addSize(savedSize);
            }

            // -------------------------------------------------
            // Recalculate quotation
            // -------------------------------------------------

            quotationCalculationService.calculateQuotation(
                    quotationId
            );

            return "redirect:/admin/quotation-items/room/"
                    + quotationRoomId;

        } catch (Exception e) {

            loadFormData(model);

            QuotationRoom room =
                    quotationRoomService.getById(quotationRoomId);

            QuotationItem quotationItem =
                    new QuotationItem();

            quotationItem.setQuotationRoom(room);

            model.addAttribute(
                    "quotationItem",
                    quotationItem
            );

            model.addAttribute("room", room);

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "admin/quotation-item-form";
        }
    }

    // =========================================================
    // EDIT ITEM
    // =========================================================

    @GetMapping("/edit/{id}")
    public String editItem(
            @PathVariable Long id,
            Model model) {

        QuotationItem quotationItem =
                quotationItemService.getById(id);

        if (quotationItem == null) {
            throw new IllegalArgumentException(
                    "Quotation item not found.");
        }

        /*
         * Force-load the size collection so Thymeleaf can
         * safely display the existing size rows.
         */
        quotationItem.getSizes().size();

        QuotationRoom room =
                quotationItem.getQuotationRoom();

        loadFormData(model);

        model.addAttribute(
                "quotationItem",
                quotationItem
        );

        model.addAttribute(
                "room",
                room
        );

        return "admin/quotation-item-form";
    }

    // =========================================================
    // UPDATE ITEM
    // =========================================================

    @PostMapping("/update")
    @Transactional
    public String updateItem(
            @RequestParam Long id,
            @RequestParam Long quotationRoomId,

            @RequestParam Long workCategoryId,
            @RequestParam Long divisionId,
            @RequestParam Long itemId,
            @RequestParam Long materialId,
            @RequestParam Long materialOptionId,

            @RequestParam(required = false)
            String itemDescription,

            @RequestParam(required = false)
            List<BigDecimal> lengthValues,

            @RequestParam(required = false)
            List<BigDecimal> widthValues,

            @RequestParam(required = false)
            List<BigDecimal> heightValues,

            @RequestParam(required = false)
            BigDecimal offerPrice,

            @RequestParam(required = false)
            String specification,

            @RequestParam(required = false)
            String formula,

            @RequestParam(required = false, defaultValue = "ft")
            String dimensionUnit,

            Model model) {

        try {

            // -------------------------------------------------
            // Existing item
            // -------------------------------------------------

            QuotationItem existingItem =
                    quotationItemService.getById(id);

            if (existingItem == null) {
                throw new IllegalArgumentException(
                        "Quotation item not found.");
            }

            QuotationRoom room =
                    quotationRoomService.getById(
                            quotationRoomId
                    );

            // -------------------------------------------------
            // Load master data
            // -------------------------------------------------

            WorkCategory workCategory =
                    workCategoryRepository.findById(workCategoryId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Work category not found."));

            Division division =
                    divisionRepository.findById(divisionId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Division not found."));

            Item item =
                    itemRepository.findById(itemId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Item not found."));

            Material material =
                    materialRepository.findById(materialId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Material not found."));

            MaterialOption materialOption =
                    materialOptionRepository.findById(materialOptionId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Material option not found."));

            // -------------------------------------------------
            // Resolve formula
            // -------------------------------------------------

            Quotation quotation = room.getQuotation();
            Long quotationId = quotation.getId();

            if (formula == null || formula.trim().isEmpty()) {
                Pricing pricing = pricingRepository
                        .findByMaterialOptionIdAndPricingMode(
                                materialOptionId,
                                quotation.getPricingMode()
                        ).orElse(null);
                if (pricing != null && pricing.getFormula() != null && !pricing.getFormula().trim().isEmpty()) {
                    formula = pricing.getFormula();
                } else if (materialOption.getFormula() != null && !materialOption.getFormula().trim().isEmpty()) {
                    formula = materialOption.getFormula();
                } else if (existingItem.getFormula() != null && !existingItem.getFormula().trim().isEmpty()) {
                    formula = existingItem.getFormula();
                } else {
                    formula = CalculationFormula.FORMULA_L_X_W;
                }
            }
            formula = CalculationFormula.normalizeFormula(formula);

            // Pad width and height lists if needed
            int sizeCount = lengthValues != null ? lengthValues.size() : 0;
            if (widthValues == null) {
                widthValues = new ArrayList<>();
            }
            while (widthValues.size() < sizeCount) {
                widthValues.add(BigDecimal.ZERO);
            }
            if (heightValues == null) {
                heightValues = new ArrayList<>();
            }
            while (heightValues.size() < sizeCount) {
                heightValues.add(BigDecimal.ZERO);
            }

            // -------------------------------------------------
            // Validate size lists
            // -------------------------------------------------

            validateSizeLists(
                    formula,
                    lengthValues,
                    widthValues,
                    heightValues
            );

            // -------------------------------------------------
            // Calculate total SQ.FT
            // -------------------------------------------------

            BigDecimal totalSqft =
                    calculateTotalSqft(
                            formula,
                            dimensionUnit,
                            lengthValues,
                            widthValues,
                            heightValues
                    );

            // -------------------------------------------------
            // Resolve current server-side rate
            // -------------------------------------------------

            BigDecimal rate =
                    quotationCalculationService.resolveItemRate(
                            quotationId,
                            materialOptionId
                    );

            // -------------------------------------------------
            // Calculate amount
            // -------------------------------------------------

            BigDecimal amount =
                    quotationItemService.calculateAmount(
                            totalSqft,
                            rate
                    );

            // -------------------------------------------------
            // Calculate offer price
            // -------------------------------------------------

            BigDecimal finalOfferPrice =
                    quotationItemService.calculateOfferPrice(
                            amount,
                            offerPrice
                    );

            // -------------------------------------------------
            // First size for backward compatibility
            // -------------------------------------------------

            BigDecimal firstLength =
                    lengthValues.get(0);

            BigDecimal firstWidth =
                    widthValues.get(0);

            BigDecimal firstHeight =
                    heightValues.get(0);

            // -------------------------------------------------
            // Update parent quotation item
            // -------------------------------------------------

            QuotationItem updatedItem =
                    quotationItemService.update(
                            id,
                            workCategory,
                            division,
                            item,
                            material,
                            materialOption,
                            itemDescription,

                            firstLength,
                            firstWidth,
                            firstHeight,

                            totalSqft,
                            rate,
                            amount,
                            finalOfferPrice,

                            formula,
                            specification
                    );

            // -------------------------------------------------
            // Remove old child size rows from parent collection
            // -------------------------------------------------

            List<QuotationItemSize> oldSizes =
                    new ArrayList<>(
                            updatedItem.getSizes()
                    );

            for (QuotationItemSize oldSize : oldSizes) {
                updatedItem.removeSize(oldSize);
            }

            // -------------------------------------------------
            // Create new child size rows
            // -------------------------------------------------

            for (int i = 0; i < lengthValues.size(); i++) {

                QuotationItemSize savedSize =
                        quotationItemSizeService.create(
                                updatedItem,
                                lengthValues.get(i),
                                widthValues.get(i),
                                heightValues.get(i),

                                null,

                                i + 1,
                                dimensionUnit
                        );

                updatedItem.addSize(savedSize);
            }

            // -------------------------------------------------
            // Recalculate quotation
            // -------------------------------------------------

            quotationCalculationService.calculateQuotation(
                    quotationId
            );

            return "redirect:/admin/quotation-items/room/"
                    + quotationRoomId;

        } catch (Exception e) {

            loadFormData(model);

            QuotationRoom room =
                    quotationRoomService.getById(
                            quotationRoomId
                    );

            QuotationItem quotationItem =
                    quotationItemService.getById(id);

            model.addAttribute(
                    "quotationItem",
                    quotationItem
            );

            model.addAttribute(
                    "room",
                    room
            );

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "admin/quotation-item-form";
        }
    }

    // =========================================================
    // ACTIVATE ITEM
    // =========================================================

    @PostMapping("/activate/{id}")
    public String activateItem(
            @PathVariable Long id) {

        QuotationItem item =
                quotationItemService.activate(id);

        quotationCalculationService.calculateQuotation(
                item.getQuotationRoom()
                        .getQuotation()
                        .getId()
        );

        return "redirect:/admin/quotation-items/room/"
                + item.getQuotationRoom().getId();
    }

    // =========================================================
    // DEACTIVATE ITEM
    // =========================================================

    @PostMapping("/deactivate/{id}")
    public String deactivateItem(
            @PathVariable Long id) {

        QuotationItem item =
                quotationItemService.deactivate(id);

        quotationCalculationService.calculateQuotation(
                item.getQuotationRoom()
                        .getQuotation()
                        .getId()
        );

        return "redirect:/admin/quotation-items/room/"
                + item.getQuotationRoom().getId();
    }

    // =========================================================
    // DELETE ITEM
    // =========================================================

    @PostMapping("/delete/{id}")
    public String deleteItem(
            @PathVariable Long id) {

        QuotationItem item =
                quotationItemService.getById(id);

        Long roomId =
                item.getQuotationRoom().getId();

        Long quotationId =
                item.getQuotationRoom()
                        .getQuotation()
                        .getId();

        quotationItemService.delete(id);

        quotationCalculationService.calculateQuotation(
                quotationId
        );

        return "redirect:/admin/quotation-items/room/"
                + roomId;
    }

    // =========================================================
    // AJAX - CALCULATE SQ.FT
    // =========================================================

    @GetMapping("/calculate-sqft")
    @ResponseBody
    public BigDecimal calculateSqft(
            @RequestParam(required = false) String formula,
            @RequestParam BigDecimal length,
            @RequestParam(required = false) BigDecimal width,
            @RequestParam(required = false) BigDecimal height,
            @RequestParam(required = false, defaultValue = "ft") String unit) {

        return quotationItemSizeService.calculateSqft(
                formula,
                length,
                width != null ? width : BigDecimal.ZERO,
                height != null ? height : BigDecimal.ZERO,
                unit
        );
    }

    // =========================================================
    // AJAX - CALCULATE AMOUNT
    // =========================================================

    @GetMapping("/calculate-amount")
    @ResponseBody
    public BigDecimal calculateAmount(
            @RequestParam BigDecimal sqft,
            @RequestParam BigDecimal rate) {

        return quotationItemService.calculateAmount(
                sqft,
                rate
        );
    }

    // =========================================================
    // AJAX - RESOLVE CONFIGURED RATE
    // =========================================================

    @GetMapping("/resolve-rate")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> resolveRate(
            @RequestParam Long quotationRoomId,
            @RequestParam Long materialOptionId) {

        try {
            QuotationRoom room = quotationRoomService.getById(quotationRoomId);

            if (room == null || room.getQuotation() == null) {
                return org.springframework.http.ResponseEntity.badRequest()
                        .body(java.util.Map.of("success", false, "message", "Quotation room not found."));
            }

            Quotation quotation = room.getQuotation();
            BigDecimal rate = quotationCalculationService.resolveItemRate(
                    quotation.getId(),
                    materialOptionId
            );

            // Resolve formula from Pricing or MaterialOption
            Pricing pricing = pricingRepository
                    .findByMaterialOptionIdAndPricingMode(
                            materialOptionId,
                            quotation.getPricingMode()
                    ).orElse(null);

            String formula = null;
            if (pricing != null && pricing.getFormula() != null && !pricing.getFormula().trim().isEmpty()) {
                formula = CalculationFormula.normalizeFormula(pricing.getFormula());
            } else {
                MaterialOption mo = materialOptionRepository.findById(materialOptionId).orElse(null);
                if (mo != null && mo.getFormula() != null && !mo.getFormula().trim().isEmpty()) {
                    formula = CalculationFormula.normalizeFormula(mo.getFormula());
                }
            }
            if (formula == null || formula.trim().isEmpty()) {
                formula = CalculationFormula.FORMULA_L_X_W;
            }

            return org.springframework.http.ResponseEntity.ok(
                    java.util.Map.of(
                            "success", true,
                            "rate", rate,
                            "formula", formula,
                            "pricingMode", quotation.getPricingMode() != null ? quotation.getPricingMode().name() : ""
                    )
            );

        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.ok(
                    java.util.Map.of("success", false, "message", e.getMessage())
            );
        }
    }

    // =========================================================
    // LOAD FORM DATA
    // =========================================================

    private void loadFormData(Model model) {

        List<WorkCategory> workCategories =
                workCategoryRepository.findAll();

        List<Division> divisions =
                divisionRepository.findAll();

        List<Item> items =
                itemRepository.findAll();

        List<Material> materials =
                materialRepository.findAll();

        List<MaterialOption> materialOptions =
                materialOptionRepository.findAll();

        model.addAttribute(
                "workCategories",
                workCategories
        );

        model.addAttribute(
                "divisions",
                divisions
        );

        model.addAttribute(
                "items",
                items
        );

        model.addAttribute(
                "materials",
                materials
        );

        model.addAttribute(
                "materialOptions",
                materialOptions
        );
    }

    // =========================================================
    // VALIDATE SIZE LISTS
    // =========================================================

    private void validateSizeLists(
            String formula,
            List<BigDecimal> lengths,
            List<BigDecimal> widths,
            List<BigDecimal> heights) {

        if (lengths == null || lengths.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one size is required."
            );
        }

        for (int i = 0; i < lengths.size(); i++) {
            BigDecimal length = lengths.get(i);
            BigDecimal width = (widths != null && i < widths.size()) ? widths.get(i) : null;
            BigDecimal height = (heights != null && i < heights.size()) ? heights.get(i) : null;

            try {
                CalculationFormula.validateDimensions(formula, length, width, height);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(ex.getMessage() + " (for Size " + (i + 1) + ")");
            }
        }
    }

    private void validateSizeLists(
            List<BigDecimal> lengths,
            List<BigDecimal> widths,
            List<BigDecimal> heights) {
        validateSizeLists(null, lengths, widths, heights);
    }

    // =========================================================
    // CALCULATE TOTAL SQ.FT
    // =========================================================

    private BigDecimal calculateTotalSqft(
            String formula,
            String unit,
            List<BigDecimal> lengths,
            List<BigDecimal> widths,
            List<BigDecimal> heights) {

        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < lengths.size(); i++) {
            BigDecimal l = lengths.get(i);
            BigDecimal w = (widths != null && i < widths.size()) ? widths.get(i) : BigDecimal.ZERO;
            BigDecimal h = (heights != null && i < heights.size()) ? heights.get(i) : BigDecimal.ZERO;

            BigDecimal sqft = quotationItemSizeService.calculateSqft(
                    formula,
                    l,
                    w,
                    h,
                    unit
            );

            total = total.add(sqft);
        }

        return total.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal calculateTotalSqft(
            List<BigDecimal> lengths,
            List<BigDecimal> widths,
            List<BigDecimal> heights) {
        return calculateTotalSqft(null, "ft", lengths, widths, heights);
    }

    // =========================================================
    // DIMENSION VALIDATION
    // =========================================================

    private boolean hasValidDimension(
            BigDecimal value) {

        return value != null
                && value.compareTo(BigDecimal.ZERO) > 0;
    }
}