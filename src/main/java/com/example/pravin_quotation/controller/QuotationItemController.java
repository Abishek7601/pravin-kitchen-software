package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Division;
import com.example.pravin_quotation.model.Item;
import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.model.MaterialOption;
import com.example.pravin_quotation.model.QuotationItem;
import com.example.pravin_quotation.model.QuotationRoom;
import com.example.pravin_quotation.model.WorkCategory;
import com.example.pravin_quotation.repository.DivisionRepository;
import com.example.pravin_quotation.repository.ItemRepository;
import com.example.pravin_quotation.repository.MaterialOptionRepository;
import com.example.pravin_quotation.repository.MaterialRepository;
import com.example.pravin_quotation.repository.WorkCategoryRepository;
import com.example.pravin_quotation.service.QuotationCalculationService;
import com.example.pravin_quotation.service.QuotationItemService;
import com.example.pravin_quotation.service.QuotationRoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/quotation-items")
public class QuotationItemController {

    private final QuotationItemService quotationItemService;
    private final QuotationRoomService quotationRoomService;
    private final QuotationCalculationService quotationCalculationService;

    private final WorkCategoryRepository workCategoryRepository;
    private final DivisionRepository divisionRepository;
    private final ItemRepository itemRepository;
    private final MaterialRepository materialRepository;
    private final MaterialOptionRepository materialOptionRepository;

    public QuotationItemController(
            QuotationItemService quotationItemService,
            QuotationRoomService quotationRoomService,
            QuotationCalculationService quotationCalculationService,
            WorkCategoryRepository workCategoryRepository,
            DivisionRepository divisionRepository,
            ItemRepository itemRepository,
            MaterialRepository materialRepository,
            MaterialOptionRepository materialOptionRepository) {

        this.quotationItemService = quotationItemService;
        this.quotationRoomService = quotationRoomService;
        this.quotationCalculationService = quotationCalculationService;
        this.workCategoryRepository = workCategoryRepository;
        this.divisionRepository = divisionRepository;
        this.itemRepository = itemRepository;
        this.materialRepository = materialRepository;
        this.materialOptionRepository = materialOptionRepository;
    }


    // =========================================================
    // LIST ITEMS BY ROOM
    // =========================================================

    @GetMapping("/room/{roomId}")
    public String listItems(
            @PathVariable Long roomId,
            Model model) {

        QuotationRoom quotationRoom =
                quotationRoomService.getById(roomId);

        List<QuotationItem> items =
                quotationItemService.getActiveByQuotationRoomId(roomId);

        model.addAttribute("quotationRoom", quotationRoom);
        model.addAttribute("quotation", quotationRoom.getQuotation());
        model.addAttribute("items", items);

        return "admin/quotation-items";
    }


    // =========================================================
    // NEW ITEM FORM
    // =========================================================

    @GetMapping("/new/{roomId}")
    public String newItem(
            @PathVariable Long roomId,
            Model model) {

        QuotationRoom quotationRoom =
                quotationRoomService.getById(roomId);

        QuotationItem quotationItem = new QuotationItem();
        quotationItem.setQuotationRoom(quotationRoom);

        loadFormData(model);

        model.addAttribute("quotationItem", quotationItem);
        model.addAttribute("quotationRoom", quotationRoom);
        model.addAttribute("quotation", quotationRoom.getQuotation());

        return "admin/quotation-item-form";
    }


    // =========================================================
    // SAVE ITEM
    // =========================================================

    @PostMapping("/save")
    public String saveItem(

            @RequestParam Long quotationRoomId,

            @RequestParam(required = false) Long workCategoryId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) Long materialOptionId,

            @RequestParam(required = false) String itemDescription,

            @RequestParam(required = false) BigDecimal lengthValue,
            @RequestParam(required = false) BigDecimal widthValue,
            @RequestParam(required = false) BigDecimal heightValue,

            @RequestParam(required = false) BigDecimal calculatedSqft,
            @RequestParam(required = false) BigDecimal rate,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) BigDecimal offerPrice,

            @RequestParam(required = false) String formula,
            @RequestParam(required = false) String specification) {


        QuotationRoom quotationRoom =
                quotationRoomService.getById(quotationRoomId);


        WorkCategory workCategory =
                getWorkCategory(workCategoryId);

        Division division =
                getDivision(divisionId);

        Item item =
                getItem(itemId);

        Material material =
                getMaterial(materialId);

        MaterialOption materialOption =
                getMaterialOption(materialOptionId);

// Dimensions are mandatory for this Sq.Ft calculation.
        if (!hasDimensions(lengthValue, widthValue, heightValue)) {
            throw new IllegalArgumentException(
                    "Length, width and height must all be greater than zero.");
        }

        if (materialOption == null) {
            throw new IllegalArgumentException(
                    "Please select a material option.");
        }

// Always calculate Sq.Ft on the server.
// Do not trust calculatedSqft submitted by the browser.
        calculatedSqft = quotationItemService.calculateSqft(
                lengthValue,
                widthValue,
                heightValue);

// Always resolve the district-specific rate on the server.
// Do not trust the rate submitted by the browser.
        rate = quotationCalculationService.resolveItemRate(
                quotationRoom.getQuotation().getId(),
                materialOption.getId());

// Calculate the amount using the server-calculated values.
        amount = quotationItemService.calculateAmount(
                calculatedSqft,
                rate);

// If offer price is null or zero, use the calculated amount.
        offerPrice = quotationItemService.calculateOfferPrice(
                amount,
                offerPrice);

// Store the formula used for the calculation.
        formula = "(Length + Width) × Height / 144";


        quotationItemService.create(

                quotationRoom,
                workCategory,
                division,
                item,
                material,
                materialOption,
                itemDescription,
                lengthValue,
                widthValue,
                heightValue,
                calculatedSqft,
                rate,
                amount,
                offerPrice,
                formula,
                specification
        );

        quotationCalculationService.calculateQuotation(
                quotationRoom.getQuotation().getId()
        );


        return "redirect:/admin/quotation-items/room/"
                + quotationRoomId;
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

        QuotationRoom quotationRoom =
                quotationItem.getQuotationRoom();

        loadFormData(model);

        model.addAttribute(
                "quotationItem",
                quotationItem);

        model.addAttribute(
                "quotationRoom",
                quotationRoom);

        model.addAttribute(
                "quotation",
                quotationRoom.getQuotation());

        return "admin/quotation-item-form";
    }


// =========================================================
// UPDATE ITEM
// =========================================================

    @PostMapping("/update")
    public String updateItem(

            @RequestParam Long id,

            @RequestParam(required = false) Long workCategoryId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) Long materialOptionId,

            @RequestParam(required = false) String itemDescription,

            @RequestParam(required = false) BigDecimal lengthValue,
            @RequestParam(required = false) BigDecimal widthValue,
            @RequestParam(required = false) BigDecimal heightValue,

            @RequestParam(required = false) BigDecimal calculatedSqft,
            @RequestParam(required = false) BigDecimal rate,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) BigDecimal offerPrice,

            @RequestParam(required = false) String formula,
            @RequestParam(required = false) String specification) {

        // Get the existing item and its quotation room
        QuotationItem quotationItem =
                quotationItemService.getById(id);

        QuotationRoom quotationRoom =
                quotationItem.getQuotationRoom();

        // Load selected entities
        WorkCategory workCategory =
                getWorkCategory(workCategoryId);

        Division division =
                getDivision(divisionId);

        Item item =
                getItem(itemId);

        Material material =
                getMaterial(materialId);

        MaterialOption materialOption =
                getMaterialOption(materialOptionId);

        // Dimensions are mandatory
        if (!hasDimensions(lengthValue, widthValue, heightValue)) {
            throw new IllegalArgumentException(
                    "Length, width and height must all be greater than zero.");
        }

        // Material option is mandatory for district pricing
        if (materialOption == null) {
            throw new IllegalArgumentException(
                    "Please select a material option.");
        }

        // Always calculate Sq.Ft on the server
        calculatedSqft = quotationItemService.calculateSqft(
                lengthValue,
                widthValue,
                heightValue
        );

        // Always resolve the rate from configured district pricing
        rate = quotationCalculationService.resolveItemRate(
                quotationRoom.getQuotation().getId(),
                materialOption.getId()
        );

        // Calculate amount on the server
        amount = quotationItemService.calculateAmount(
                calculatedSqft,
                rate
        );

        // If offer price is null or zero, use calculated amount
        offerPrice = quotationItemService.calculateOfferPrice(
                amount,
                offerPrice
        );

        // Keep the calculation formula consistent
        formula = "(Length + Width) × Height / 144";

        quotationItemService.update(
                id,
                workCategory,
                division,
                item,
                material,
                materialOption,
                itemDescription,
                lengthValue,
                widthValue,
                heightValue,
                calculatedSqft,
                rate,
                amount,
                offerPrice,
                formula,
                specification
        );

        // Recalculate quotation totals
        quotationCalculationService.calculateQuotation(
                quotationRoom.getQuotation().getId()
        );

        return "redirect:/admin/quotation-items/room/"
                + quotationRoom.getId();
    }




    // =========================================================
    // ACTIVATE
    // =========================================================

    @PostMapping("/activate/{id}")
    public String activateItem(
            @PathVariable Long id) {

        QuotationItem quotationItem =
                quotationItemService.activate(id);

        quotationCalculationService.calculateQuotation(
                quotationItem.getQuotationRoom()
                        .getQuotation()
                        .getId()
        );

        return redirectToRoom(quotationItem);
    }


    // =========================================================
    // DEACTIVATE
    // =========================================================

    @PostMapping("/deactivate/{id}")
    public String deactivateItem(
            @PathVariable Long id) {

        QuotationItem quotationItem =
                quotationItemService.deactivate(id);

        quotationCalculationService.calculateQuotation(
                quotationItem.getQuotationRoom()
                        .getQuotation()
                        .getId()
        );

        return redirectToRoom(quotationItem);
    }


    // =========================================================
    // DELETE
    // =========================================================

    @PostMapping("/delete/{id}")
    public String deleteItem(
            @PathVariable Long id) {

        QuotationItem quotationItem =
                quotationItemService.getById(id);

        Long roomId =
                quotationItem.getQuotationRoom().getId();

        Long quotationId =
                quotationItem.getQuotationRoom()
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
    // CALCULATE SQ.FT - AJAX
    // =========================================================

    @GetMapping("/calculate-sqft")
    @ResponseBody
    public BigDecimal calculateSqft(

            @RequestParam BigDecimal length,
            @RequestParam BigDecimal width,
            @RequestParam BigDecimal height) {

        return quotationItemService.calculateSqft(
                length,
                width,
                height);
    }


    // =========================================================
    // CALCULATE AMOUNT - AJAX
    // =========================================================

    @GetMapping("/calculate-amount")
    @ResponseBody
    public BigDecimal calculateAmount(

            @RequestParam BigDecimal sqft,
            @RequestParam BigDecimal rate) {

        return quotationItemService.calculateAmount(
                sqft,
                rate);
    }


    // =========================================================
    // LOAD FORM DATA
    // =========================================================

    private void loadFormData(Model model) {

        model.addAttribute(
                "workCategories",
                workCategoryRepository
                        .findAllByOrderByDisplayOrderAsc());

        model.addAttribute(
                "divisions",
                divisionRepository
                        .findAllByOrderByDisplayOrderAsc());

        model.addAttribute(
                "itemsList",
                itemRepository
                        .findAllByOrderByDisplayOrderAsc());

        model.addAttribute(
                "materials",
                materialRepository
                        .findAllByOrderByDisplayOrderAsc());

        model.addAttribute(
                "materialOptions",
                materialOptionRepository
                        .findAllByOrderByDisplayOrderAsc());
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private WorkCategory getWorkCategory(Long id) {

        if (id == null) {
            return null;
        }

        return workCategoryRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Work category not found."));
    }


    private Division getDivision(Long id) {

        if (id == null) {
            return null;
        }

        return divisionRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Division not found."));
    }


    private Item getItem(Long id) {

        if (id == null) {
            return null;
        }

        return itemRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Item not found."));
    }


    private Material getMaterial(Long id) {

        if (id == null) {
            return null;
        }

        return materialRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Material not found."));
    }


    private MaterialOption getMaterialOption(Long id) {

        if (id == null) {
            return null;
        }

        return materialOptionRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Material option not found."));
    }


    private boolean hasDimensions(
            BigDecimal length,
            BigDecimal width,
            BigDecimal height) {

        return length != null
                && width != null
                && height != null
                && length.compareTo(BigDecimal.ZERO) > 0
                && width.compareTo(BigDecimal.ZERO) > 0
                && height.compareTo(BigDecimal.ZERO) > 0;
    }


    private String redirectToRoom(
            QuotationItem quotationItem) {

        return "redirect:/admin/quotation-items/room/"
                + quotationItem
                .getQuotationRoom()
                .getId();
    }
}