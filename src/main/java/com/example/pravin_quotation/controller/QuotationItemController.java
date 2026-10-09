package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Division;
import com.example.pravin_quotation.model.Item;
import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.model.MaterialOption;
import com.example.pravin_quotation.model.QuotationItem;
import com.example.pravin_quotation.model.QuotationItemSize;
import com.example.pravin_quotation.model.QuotationRoom;
import com.example.pravin_quotation.model.WorkCategory;
import com.example.pravin_quotation.repository.DivisionRepository;
import com.example.pravin_quotation.repository.ItemRepository;
import com.example.pravin_quotation.repository.MaterialOptionRepository;
import com.example.pravin_quotation.repository.MaterialRepository;
import com.example.pravin_quotation.repository.WorkCategoryRepository;
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

    public QuotationItemController(
            QuotationItemService quotationItemService,
            QuotationItemSizeService quotationItemSizeService,
            QuotationRoomService quotationRoomService,
            QuotationCalculationService quotationCalculationService,
            WorkCategoryRepository workCategoryRepository,
            DivisionRepository divisionRepository,
            ItemRepository itemRepository,
            MaterialRepository materialRepository,
            MaterialOptionRepository materialOptionRepository) {

        this.quotationItemService = quotationItemService;
        this.quotationItemSizeService = quotationItemSizeService;
        this.quotationRoomService = quotationRoomService;
        this.quotationCalculationService = quotationCalculationService;

        this.workCategoryRepository = workCategoryRepository;
        this.divisionRepository = divisionRepository;
        this.itemRepository = itemRepository;
        this.materialRepository = materialRepository;
        this.materialOptionRepository = materialOptionRepository;
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
            // Validate size lists
            // -------------------------------------------------

            validateSizeLists(
                    lengthValues,
                    widthValues,
                    heightValues
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
            // Calculate total SQ.FT
            // -------------------------------------------------

            BigDecimal totalSqft =
                    calculateTotalSqft(
                            lengthValues,
                            widthValues,
                            heightValues
                    );

            // -------------------------------------------------
            // Resolve server-side rate
            // -------------------------------------------------

            Long quotationId =
                    room.getQuotation().getId();

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
            // Parent item keeps first size for backward
            // compatibility.
            // -------------------------------------------------

            BigDecimal firstLength =
                    lengthValues.get(0);

            BigDecimal firstWidth =
                    widthValues.get(0);

            BigDecimal firstHeight =
                    heightValues.get(0);

            String formula =
                    "(Length + Width) × Height / 144";

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

                                // Offer price remains at parent
                                // quotation item level.
                                null,

                                i + 1
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
            // Validate size lists
            // -------------------------------------------------

            validateSizeLists(
                    lengthValues,
                    widthValues,
                    heightValues
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
            // Calculate total SQ.FT
            // -------------------------------------------------

            BigDecimal totalSqft =
                    calculateTotalSqft(
                            lengthValues,
                            widthValues,
                            heightValues
                    );

            // -------------------------------------------------
            // Resolve current server-side rate
            // -------------------------------------------------

            Long quotationId =
                    room.getQuotation().getId();

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

            String formula =
                    "(Length + Width) × Height / 144";

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

                                i + 1
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
            @RequestParam BigDecimal length,
            @RequestParam BigDecimal width,
            @RequestParam BigDecimal height) {

        return quotationItemSizeService.calculateSqft(
                length,
                width,
                height
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
            List<BigDecimal> lengths,
            List<BigDecimal> widths,
            List<BigDecimal> heights) {

        if (lengths == null || lengths.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one size is required."
            );
        }

        if (widths == null || widths.isEmpty()) {
            throw new IllegalArgumentException(
                    "Width is required."
            );
        }

        if (heights == null || heights.isEmpty()) {
            throw new IllegalArgumentException(
                    "Height is required."
            );
        }

        if (lengths.size() != widths.size()
                || lengths.size() != heights.size()) {

            throw new IllegalArgumentException(
                    "Size information is incomplete."
            );
        }

        for (int i = 0; i < lengths.size(); i++) {

            BigDecimal length = lengths.get(i);
            BigDecimal width = widths.get(i);
            BigDecimal height = heights.get(i);

            if (!hasValidDimension(length)
                    || !hasValidDimension(width)
                    || !hasValidDimension(height)) {

                throw new IllegalArgumentException(
                        "Length, Width and Height must be greater than zero for Size "
                                + (i + 1)
                                + "."
                );
            }
        }
    }

    // =========================================================
    // CALCULATE TOTAL SQ.FT
    // =========================================================

    private BigDecimal calculateTotalSqft(
            List<BigDecimal> lengths,
            List<BigDecimal> widths,
            List<BigDecimal> heights) {

        BigDecimal total =
                BigDecimal.ZERO;

        for (int i = 0; i < lengths.size(); i++) {

            BigDecimal sqft =
                    quotationItemSizeService.calculateSqft(
                            lengths.get(i),
                            widths.get(i),
                            heights.get(i)
                    );

            total =
                    total.add(sqft);
        }

        return total.setScale(
                2,
                RoundingMode.HALF_UP
        );
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