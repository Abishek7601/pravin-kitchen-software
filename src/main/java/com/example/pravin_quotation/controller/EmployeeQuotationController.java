package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Customer;
import com.example.pravin_quotation.model.Division;
import com.example.pravin_quotation.model.Item;
import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.model.MaterialOption;
import com.example.pravin_quotation.model.PricingMode;
import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationItem;
import com.example.pravin_quotation.model.QuotationItemSize;
import com.example.pravin_quotation.model.QuotationRoom;
import com.example.pravin_quotation.model.QuotationStatus;
import com.example.pravin_quotation.model.QuotationStatusHistory;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.model.WorkCategory;

import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.CustomerRepository;
import com.example.pravin_quotation.repository.DivisionRepository;
import com.example.pravin_quotation.repository.DistrictPricingRepository;
import com.example.pravin_quotation.repository.ItemRepository;
import com.example.pravin_quotation.repository.MaterialOptionRepository;
import com.example.pravin_quotation.repository.MaterialRepository;
import com.example.pravin_quotation.repository.PricingRepository;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.repository.WorkCategoryRepository;

import com.example.pravin_quotation.service.*;

import java.util.Collections;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/employee/quotations")
public class EmployeeQuotationController {

    private final QuotationRepository quotationRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final DivisionRepository divisionRepository;
    private final PricingRepository pricingRepository;
    private final DistrictPricingRepository districtPricingRepository;
    private final WorkCategoryRepository workCategoryRepository;
    private final ItemRepository itemRepository;
    private final MaterialRepository materialRepository;
    private final MaterialOptionRepository materialOptionRepository;
    private final AdminQuotationBuilderService builderService;
    private final CustomerService customerService;

    private final QuotationService quotationService;
    private final QuotationRoomService quotationRoomService;
    private final QuotationItemService quotationItemService;
    private final QuotationItemSizeService quotationItemSizeService;
    private final QuotationCalculationService quotationCalculationService;

    private final WorkCategoryService workCategoryService;
    private final DivisionService divisionService;
    private final ItemService itemService;
    private final MaterialService materialService;
    private final MaterialOptionService materialOptionService;

    private final QuotationPdfService quotationPdfService;
    private final QuotationEmailService quotationEmailService;
    private final QuotationCommunicationService quotationCommunicationService;

    private final QuotationStatusHistoryService quotationStatusHistoryService;


    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public EmployeeQuotationController(

            QuotationRepository quotationRepository,

            UserRepository userRepository,

            CustomerRepository customerRepository,

            BranchRepository branchRepository,

            DivisionRepository divisionRepository,

            PricingRepository pricingRepository,

            DistrictPricingRepository districtPricingRepository,

            WorkCategoryRepository workCategoryRepository,

            ItemRepository itemRepository,

            MaterialRepository materialRepository,

            MaterialOptionRepository materialOptionRepository,

            AdminQuotationBuilderService builderService,

            CustomerService customerService,

            QuotationService quotationService,

            QuotationRoomService quotationRoomService,

            QuotationItemService quotationItemService,

            QuotationItemSizeService quotationItemSizeService,

            QuotationCalculationService quotationCalculationService,

            WorkCategoryService workCategoryService,

            DivisionService divisionService,

            ItemService itemService,

            MaterialService materialService,

            MaterialOptionService materialOptionService,

            QuotationPdfService quotationPdfService,

            QuotationEmailService quotationEmailService,

            QuotationCommunicationService quotationCommunicationService,

            QuotationStatusHistoryService quotationStatusHistoryService
    ) {

        this.quotationRepository =
                quotationRepository;

        this.userRepository =
                userRepository;

        this.customerRepository =
                customerRepository;

        this.branchRepository =
                branchRepository;

        this.divisionRepository =
                divisionRepository;

        this.pricingRepository =
                pricingRepository;

        this.districtPricingRepository =
                districtPricingRepository;

        this.workCategoryRepository =
                workCategoryRepository;

        this.itemRepository =
                itemRepository;

        this.materialRepository =
                materialRepository;

        this.materialOptionRepository =
                materialOptionRepository;

        this.builderService =
                builderService;

        this.customerService =
                customerService;

        this.quotationService =
                quotationService;

        this.quotationRoomService =
                quotationRoomService;

        this.quotationItemService =
                quotationItemService;

        this.quotationItemSizeService =
                quotationItemSizeService;

        this.quotationCalculationService =
                quotationCalculationService;

        this.workCategoryService =
                workCategoryService;

        this.divisionService =
                divisionService;

        this.itemService =
                itemService;

        this.materialService =
                materialService;

        this.materialOptionService =
                materialOptionService;

        this.quotationPdfService =
                quotationPdfService;

        this.quotationEmailService =
                quotationEmailService;

        this.quotationCommunicationService =
                quotationCommunicationService;

        this.quotationStatusHistoryService =
                quotationStatusHistoryService;
    }


    // ============================================================
    // EMPLOYEE QUOTATION LIST
    // ============================================================

    @GetMapping
    public String quotations(
            Authentication authentication,
            Model model
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        List<Quotation> quotations =
                quotationRepository
                        .findByEmployeeIdOrderByCreatedAtDesc(
                                employee.getId()
                        );

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "quotations",
                quotations
        );

        return "employee/quotations";
    }


    // ============================================================
    // UNIFIED QUOTATION BUILDER (CREATION)
    // ============================================================

    @GetMapping({"/new", "/builder"})
    public String builder(
            Authentication authentication,
            Model model
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        Quotation quotation = new Quotation();
        quotation.setQuotationNumber(previewQuotationNumber());
        quotation.setQuotationDate(LocalDate.now());
        quotation.setPricingMode(PricingMode.ECONOMY);
        quotation.setBranch(employee.getBranch());
        quotation.setSubtotal(BigDecimal.ZERO);
        quotation.setGstAmount(BigDecimal.ZERO);
        quotation.setTravelCharge(BigDecimal.ZERO);
        quotation.setGrandTotal(BigDecimal.ZERO);

        model.addAttribute("employee", employee);
        model.addAttribute("quotation", quotation);
        model.addAttribute("nextQuotationNumber", quotation.getQuotationNumber());
        model.addAttribute("rooms", Collections.emptyList());
        model.addAttribute("roomItems", Collections.emptyMap());
        model.addAttribute("branches", branchRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("workCategories", workCategoryService.getActiveCategories());
        model.addAttribute("pricingModes", PricingMode.values());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("employeeBranchId", employee.getBranch() != null ? employee.getBranch().getId() : null);

        return "employee/quotation-builder";
    }

    @GetMapping("/builder/divisions")
    @ResponseBody
    public List<Map<String, Object>> builderDivisions(@RequestParam Long workCategoryId) {
        return divisionRepository.findByWorkCategoryIdAndActiveTrueOrderByDisplayOrderAsc(workCategoryId)
                .stream().map(d -> mapData("id", d.getId(), "name", d.getName(), "description", d.getDescription()))
                .toList();
    }

    @GetMapping("/builder/items")
    @ResponseBody
    public List<Map<String, Object>> builderItems(@RequestParam Long divisionId) {
        return itemRepository.findByDivisionIdAndActiveTrueOrderByDisplayOrderAsc(divisionId)
                .stream().map(i -> mapData("id", i.getId(), "name", i.getName(), "description", i.getDescription()))
                .toList();
    }

    @GetMapping("/builder/materials")
    @ResponseBody
    public List<Map<String, Object>> builderMaterials(@RequestParam Long itemId) {
        return materialRepository.findByItemIdAndActiveTrueOrderByDisplayOrderAsc(itemId)
                .stream().map(m -> mapData("id", m.getId(), "name", m.getName(), "description", m.getDescription()))
                .toList();
    }

    @GetMapping("/builder/material-options")
    @ResponseBody
    public List<Map<String, Object>> builderMaterialOptions(@RequestParam Long materialId) {
        return materialOptionRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(materialId)
                .stream().map(o -> mapData("id", o.getId(), "name", o.getName(), "description", o.getDescription()))
                .toList();
    }

    @GetMapping("/builder/rate")
    @ResponseBody
    public ResponseEntity<?> builderRate(
            @RequestParam Long branchId,
            @RequestParam Long materialOptionId,
            @RequestParam PricingMode pricingMode) {
        try {
            BigDecimal rate = builderService.resolveRate(branchId, materialOptionId, pricingMode);
            return ResponseEntity.ok(mapData("rate", rate, "pricingMode", pricingMode.name()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(mapData("message", e.getMessage()));
        }
    }

    @PostMapping("/builder/save")
    public String saveBuilderQuotation(
            @RequestBody AdminQuotationBuilderService.BuilderRequest request,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Quotation quotation = builderService.save(request, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Quotation " + quotation.getQuotationNumber() + " created successfully.");
            return "redirect:/employee/quotations/view/" + quotation.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/employee/quotations/builder";
        }
    }

    private Map<String, Object> mapData(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    // ============================================================
    // AJAX - RESOLVE CONFIGURED RATE FOR ITEM FORM
    // ============================================================

    @GetMapping("/resolve-rate")
    @ResponseBody
    public ResponseEntity<?> resolveItemRate(
            @RequestParam Long quotationId,
            @RequestParam Long materialOptionId) {

        try {
            BigDecimal rate = quotationCalculationService.resolveItemRate(
                    quotationId,
                    materialOptionId
            );

            return ResponseEntity.ok(
                    mapData("success", true, "rate", rate)
            );

        } catch (Exception e) {
            return ResponseEntity.ok(
                    mapData("success", false, "message", e.getMessage())
            );
        }
    }


    // ============================================================
    @PostMapping("/save")
    public String saveQuotation(

            @RequestParam(required = false)
            String customerName,

            @RequestParam(required = false)
            String phone,

            @RequestParam(required = false)
            String email,

            @RequestParam(required = false)
            String address,

            @RequestParam(required = false)
            String quotationDate,

            @RequestParam(required = false)
            PricingMode pricingMode,

            @RequestParam(required = false)
            Long divisionId,

            @RequestParam(required = false)
            String customerRequirements,

            @RequestParam(required = false)
            String notes,

            Authentication authentication
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        if (employee.getBranch() == null) {

            throw new IllegalStateException(
                    "Employee is not assigned to a branch"
            );
        }

        String finalCustomerName = (customerName == null || customerName.isBlank()) ? "Draft Customer" : customerName.trim();
        String finalPhone = (phone == null || phone.isBlank()) ? "0000000000" : phone.trim();
        LocalDate date = (quotationDate == null || quotationDate.isBlank())
                ? LocalDate.now()
                : LocalDate.parse(quotationDate, DateTimeFormatter.ISO_LOCAL_DATE);
        PricingMode mode = (pricingMode == null) ? PricingMode.ECONOMY : pricingMode;

        Customer customer =
                customerService.createOrUpdateForQuotation(
                        finalCustomerName,
                        email,
                        finalPhone,
                        address,
                        employee.getBranch().getId()
                );

        String quotationNumber =
                generateQuotationNumber();

        Quotation quotation =
                quotationService.create(
                        quotationNumber,
                        customer.getId(),
                        employee.getId(),
                        employee.getBranch().getId(),
                        date,
                        mode,
                        customerRequirements,
                        notes
                );

        if (divisionId != null) {

            divisionRepository.findById(divisionId)
                    .ifPresent(
                            quotation::setDivision
                    );

            quotationRepository.save(
                    quotation
            );
        }

        quotationCalculationService
                .initializeTravelCharge(
                        quotation.getId()
                );

        return "redirect:/employee/quotations/builder/"
                + quotation.getId();
    }


    // ============================================================
    // QUOTATION BUILDER
    // ============================================================

    @GetMapping("/builder/{id}")
    public String quotationBuilder(
            @PathVariable Long id,
            Authentication authentication,
            Model model
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        Quotation quotation =
                getQuotationAndCheckOwnership(
                        id,
                        employee
                );

        quotationCalculationService
                .calculateQuotation(id);

        quotation =
                quotationRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found"
                                )
                        );

        List<QuotationRoom> rooms =
                quotationRoomService
                        .getActiveByQuotationId(id);

        Map<Long, List<QuotationItem>> roomItems =
                new HashMap<>();

        for (QuotationRoom room : rooms) {

            roomItems.put(
                    room.getId(),
                    quotationItemService
                            .getActiveByQuotationRoomId(
                                    room.getId()
                            )
            );
        }

        model.addAttribute("employee", employee);
        model.addAttribute("quotation", quotation);
        model.addAttribute("nextQuotationNumber", quotation.getQuotationNumber());
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomItems", roomItems);
        model.addAttribute("branches", branchRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("workCategories", workCategoryService.getActiveCategories());
        model.addAttribute("pricingModes", PricingMode.values());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("employeeBranchId", employee.getBranch() != null ? employee.getBranch().getId() : null);

        return "employee/quotation-builder";
    }


    // ============================================================
    // UPDATE CUSTOMER DETAILS FROM BUILDER
    // ============================================================

    @PostMapping("/builder/{id}/customer")
    public String updateBuilderCustomer(

            @PathVariable Long id,

            @RequestParam String customerName,

            @RequestParam String phone,

            @RequestParam(required = false)
            String email,

            @RequestParam(required = false)
            String address,

            @RequestParam String quotationDate,

            @RequestParam(required = false)
            String customerRequirements,

            @RequestParam(required = false)
            String notes,

            Authentication authentication
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        Quotation quotation =
                getQuotationAndCheckOwnership(
                        id,
                        employee
                );

        Customer customer =
                customerService.createOrUpdateForQuotation(
                        customerName,
                        email,
                        phone,
                        address,
                        employee.getBranch().getId()
                );

        quotation.setCustomer(customer);

        quotation.setQuotationDate(
                LocalDate.parse(
                        quotationDate,
                        DateTimeFormatter.ISO_LOCAL_DATE
                )
        );

        quotation.setCustomerRequirements(
                customerRequirements
        );

        quotation.setNotes(notes);

        quotationRepository.save(
                quotation
        );

        return "redirect:/employee/quotations/builder/"
                + id
                + "?saved=customer";
    }


    // ============================================================
    // CHANGE PRICING MODE
    // ============================================================

    @PostMapping("/builder/{id}/pricing-mode")
    public String changeBuilderPricingMode(

            @PathVariable Long id,

            @RequestParam PricingMode pricingMode,

            Authentication authentication
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        Quotation quotation =
                getQuotationAndCheckOwnership(
                        id,
                        employee
                );

        quotationCalculationService
                .changePricingMode(
                        quotation.getId(),
                        pricingMode
                );

        return "redirect:/employee/quotations/builder/"
                + id
                + "?saved=pricing";
    }


    // ============================================================
    // ADD ITEMS DIRECTLY FROM BUILDER
    // ============================================================

    @PostMapping("/builder/{id}/items")
    public String addBuilderItems(

            @PathVariable Long id,

            @RequestParam String floor,

            @RequestParam String room,

            @RequestParam Long workCategoryId,

            @RequestParam Long itemId,

            @RequestParam Long materialId,

            @RequestParam Long materialOptionId,

            @RequestParam List<BigDecimal> lengthValue,

            @RequestParam List<BigDecimal> widthValue,

            @RequestParam List<BigDecimal> heightValue,

            @RequestParam(required = false)
            List<String> offerPrice,

            @RequestParam(required = false)
            String specification,

            Authentication authentication
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        Quotation quotation =
                getQuotationAndCheckOwnership(
                        id,
                        employee
                );

        validateSizeLists(
                lengthValue,
                widthValue,
                heightValue
        );

        WorkCategory workCategory =
                workCategoryService
                        .getCategoryById(
                                workCategoryId
                        );

        Item item =
                itemService.getItemById(
                        itemId
                );

        Division division =
                item.getDivision();

        validateItemRelationship(
                workCategory,
                division,
                item
        );

        Material material =
                materialService.getMaterialById(
                        materialId
                );

        validateMaterialRelationship(
                item,
                material
        );

        MaterialOption materialOption =
                materialOptionService
                        .getOptionById(
                                materialOptionId
                        );

        validateMaterialOptionRelationship(
                material,
                materialOption
        );

        QuotationRoom quotationRoom =
                quotationRoomService
                        .getActiveByQuotationId(id)
                        .stream()
                        .filter(existing ->
                                existing.getFloor() != null
                                        && existing.getRoom() != null
                                        && existing.getFloor()
                                        .equalsIgnoreCase(
                                                floor.trim()
                                        )
                                        && existing.getRoom()
                                        .equalsIgnoreCase(
                                                room.trim()
                                        )
                        )
                        .findFirst()
                        .orElseGet(() ->
                                quotationRoomService.create(
                                        quotation,
                                        floor.trim(),
                                        room.trim(),
                                        workCategory.getName()
                                )
                        );

        /*
         * IMPORTANT:
         *
         * Do NOT trust the rate coming from the browser.
         *
         * Rate is resolved from:
         *
         * Quotation Branch
         *       +
         * Material Option
         *       +
         * Quotation Pricing Mode
         *       ↓
         * District Pricing
         */

        BigDecimal rate =
                quotationCalculationService
                        .resolveItemRate(
                                id,
                                materialOptionId
                        );

        validateRate(rate);

        String formula =
                "(L + W) × H / 144";

        BigDecimal totalSqft =
                BigDecimal.ZERO;

        BigDecimal totalAmount =
                BigDecimal.ZERO;

        BigDecimal totalOfferPrice =
                BigDecimal.ZERO;

        BigDecimal firstLength =
                lengthValue.get(0);

        BigDecimal firstWidth =
                widthValue.get(0);

        BigDecimal firstHeight =
                heightValue.get(0);

        validateDimensions(
                firstLength,
                firstWidth,
                firstHeight
        );

        BigDecimal firstSqft =
                quotationItemService.calculateSqft(
                        firstLength,
                        firstWidth,
                        firstHeight
                );

        BigDecimal firstAmount =
                quotationItemService.calculateAmount(
                        firstSqft,
                        rate
                );

        BigDecimal firstRequestedOffer =
                parseOfferPrice(
                        offerPrice,
                        0
                );

        BigDecimal firstOfferPrice =
                quotationItemService.calculateOfferPrice(
                        firstAmount,
                        firstRequestedOffer
                );

        totalSqft =
                totalSqft.add(firstSqft);

        totalAmount =
                totalAmount.add(firstAmount);

        totalOfferPrice =
                totalOfferPrice.add(firstOfferPrice);

        String finalSpecification =
                specification != null
                        && !specification.isBlank()
                        ? specification.trim()
                        : materialOption.getDescription();

        QuotationItem quotationItem =
                quotationItemService.create(
                        quotationRoom,
                        workCategory,
                        division,
                        item,
                        material,
                        materialOption,
                        item.getDescription(),
                        firstLength,
                        firstWidth,
                        firstHeight,
                        firstSqft,
                        rate,
                        firstAmount,
                        firstOfferPrice,
                        formula,
                        finalSpecification
                );

        QuotationItemSize firstSize =
                quotationItemSizeService.create(
                        quotationItem,
                        firstLength,
                        firstWidth,
                        firstHeight,
                        firstOfferPrice,
                        1
                );

        quotationItem.addSize(
                firstSize
        );

        for (int i = 1;
             i < lengthValue.size();
             i++) {

            BigDecimal length =
                    lengthValue.get(i);

            BigDecimal width =
                    widthValue.get(i);

            BigDecimal height =
                    heightValue.get(i);

            validateDimensions(
                    length,
                    width,
                    height
            );

            BigDecimal sqft =
                    quotationItemService.calculateSqft(
                            length,
                            width,
                            height
                    );

            BigDecimal amount =
                    quotationItemService.calculateAmount(
                            sqft,
                            rate
                    );

            BigDecimal requestedOffer =
                    parseOfferPrice(
                            offerPrice,
                            i
                    );

            BigDecimal finalOffer =
                    quotationItemService.calculateOfferPrice(
                            amount,
                            requestedOffer
                    );

            totalSqft =
                    totalSqft.add(sqft);

            totalAmount =
                    totalAmount.add(amount);

            totalOfferPrice =
                    totalOfferPrice.add(finalOffer);

            QuotationItemSize quotationItemSize =
                    quotationItemSizeService.create(
                            quotationItem,
                            length,
                            width,
                            height,
                            finalOffer,
                            i + 1
                    );

            quotationItem.addSize(
                    quotationItemSize
            );
        }

        quotationItem.setCalculatedSqft(
                totalSqft
        );

        quotationItem.setAmount(
                totalAmount
        );

        quotationItem.setOfferPrice(
                totalOfferPrice
        );

        quotationItem.setRate(
                rate
        );

        quotationItem.setLengthValue(
                firstLength
        );

        quotationItem.setWidthValue(
                firstWidth
        );

        quotationItem.setHeightValue(
                firstHeight
        );

        quotationCalculationService
                .calculateQuotation(
                        quotation.getId()
                );

        return "redirect:/employee/quotations/builder/"
                + quotation.getId()
                + "?saved=item";
    }


    // ============================================================
    // BUILDER API - ITEMS BY CATEGORY
    // ============================================================

    @GetMapping(
            "/builder/{id}/items-by-category/{categoryId}"
    )
    @ResponseBody
    public List<Map<String, Object>> builderItemsByCategory(

            @PathVariable Long id,

            @PathVariable Long categoryId,

            Authentication authentication
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        if (id != null && id > 0) {
            getQuotationAndCheckOwnership(
                    id,
                    employee
            );
        }

        List<Division> divisions =
                divisionService
                        .getActiveDivisionsByCategory(
                                categoryId
                        );

        List<Map<String, Object>> result =
                new ArrayList<>();

        for (Division division : divisions) {

            List<Item> items =
                    itemService
                            .getActiveItemsByDivision(
                                    division.getId()
                            );

            for (Item item : items) {

                Map<String, Object> row =
                        new LinkedHashMap<>();

                row.put(
                        "id",
                        item.getId()
                );

                row.put(
                        "name",
                        item.getName()
                );

                row.put(
                        "description",
                        item.getDescription()
                );

                row.put(
                        "divisionId",
                        division.getId()
                );

                result.add(row);
            }
        }

        return result;
    }


    // ============================================================
    // BUILDER API - MATERIALS BY ITEM
    // ============================================================

    @GetMapping(
            "/builder/{id}/materials/{itemId}"
    )
    @ResponseBody
    public List<Map<String, Object>> builderMaterials(

            @PathVariable Long id,

            @PathVariable Long itemId,

            Authentication authentication
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        if (id != null && id > 0) {
            getQuotationAndCheckOwnership(
                    id,
                    employee
            );
        }

        List<Material> materials =
                materialService
                        .getActiveMaterialsByItem(
                                itemId
                        );

        List<Map<String, Object>> result =
                new ArrayList<>();

        for (Material material : materials) {

            Map<String, Object> row =
                    new LinkedHashMap<>();

            row.put(
                    "id",
                    material.getId()
            );

            row.put(
                    "name",
                    material.getName()
            );

            row.put(
                    "description",
                    material.getDescription()
            );

            result.add(row);
        }

        return result;
    }


    // ============================================================
    // BUILDER API - OPTIONS BY MATERIAL
    // ============================================================

    @GetMapping(
            "/builder/{id}/options/{materialId}"
    )
    @ResponseBody
    public List<Map<String, Object>> builderMaterialOptions(

            @PathVariable Long id,

            @PathVariable Long materialId,

            Authentication authentication
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        if (id != null && id > 0) {
            getQuotationAndCheckOwnership(
                    id,
                    employee
            );
        }

        List<MaterialOption> options =
                materialOptionService
                        .getActiveOptionsByMaterial(
                                materialId
                        );

        List<Map<String, Object>> result =
                new ArrayList<>();

        for (MaterialOption option : options) {

            Map<String, Object> row =
                    new LinkedHashMap<>();

            row.put(
                    "id",
                    option.getId()
            );

            row.put(
                    "name",
                    option.getName()
            );

            row.put(
                    "description",
                    option.getDescription()
            );

            result.add(row);
        }

        return result;
    }


    // ============================================================
    // BUILDER API - CURRENT MODE RATE
    // ============================================================

    @GetMapping("/builder/{id}/price")
    @ResponseBody
    public Map<String, Object> builderPrice(

            @PathVariable Long id,

            @RequestParam Long materialOptionId,

            @RequestParam PricingMode mode,

            Authentication authentication
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        BigDecimal rate;
        String unit = "SQFT";
        String description = "";

        if (id != null && id > 0) {
            Quotation quotation =
                    getQuotationAndCheckOwnership(
                            id,
                            employee
                    );

            if (quotation.getBranch() == null) {
                throw new IllegalStateException(
                        "Quotation branch is not assigned."
                );
            }

            rate = quotationCalculationService.resolveItemRate(id, materialOptionId, mode);
        } else {
            Long branchId = employee.getBranch() != null ? employee.getBranch().getId() : null;
            if (branchId == null) {
                throw new IllegalStateException("Employee is not assigned to a branch.");
            }
            rate = builderService.resolveRate(branchId, materialOptionId, mode);
        }

        var pricing =
                pricingRepository
                        .findByMaterialOptionIdAndPricingMode(
                                materialOptionId,
                                mode
                        )
                        .orElse(null);

        if (pricing != null) {
            if (pricing.getUnit() != null) {
                unit = pricing.getUnit();
            }
            if (pricing.getDescription() != null) {
                description = pricing.getDescription();
            }
        }

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put(
                "rate",
                rate
        );

        result.put(
                "unit",
                unit
        );

        result.put(
                "description",
                description
        );

        result.put(
                "mode",
                mode.name()
        );

        result.put(
                "quotationBranch",
                employee.getBranch() != null ? employee.getBranch().getName() : "Not Assigned"
        );

        return result;
    }


    // ============================================================
    // VIEW QUOTATION WORKSPACE
    // ============================================================

    @GetMapping("/view/{id}")
    public String viewQuotation(

            @PathVariable Long id,

            Authentication authentication,

            Model model
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        Quotation quotation =
                getQuotationAndCheckOwnership(
                        id,
                        employee
                );

        quotationCalculationService
                .calculateQuotation(id);

        quotation =
                quotationRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found"
                                )
                        );

        List<QuotationRoom> rooms =
                quotationRoomService
                        .getActiveByQuotationId(id);

        Map<Long, List<QuotationItem>> roomItems =
                new HashMap<>();

        for (QuotationRoom room : rooms) {

            List<QuotationItem> items =
                    quotationItemService
                            .getActiveByQuotationRoomId(
                                    room.getId()
                            );

            roomItems.put(
                    room.getId(),
                    items
            );
        }

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "quotation",
                quotation
        );

        model.addAttribute(
                "rooms",
                rooms
        );

        model.addAttribute(
                "roomItems",
                roomItems
        );

        // ========================================================
        // STATUS HISTORY
        // ========================================================

        List<QuotationStatusHistory> statusHistory =
                quotationStatusHistoryService
                        .getByQuotationId(id);

        model.addAttribute(
                "statusHistory",
                statusHistory
        );

        // ========================================================
        // AVAILABLE STATUSES
        // ========================================================

        model.addAttribute(
                "quotationStatuses",
                QuotationStatus.values()
        );

        return "employee/quotation-workspace";
    }


    // ============================================================
    // CHANGE QUOTATION STATUS
    // ============================================================

    @PostMapping("/status/{id}")
    public String changeStatus(

            @PathVariable Long id,

            @RequestParam QuotationStatus status,

            Authentication authentication,

            RedirectAttributes redirectAttributes
    ) {

        try {

            User employee =
                    getLoggedInEmployee(
                            authentication
                    );

            Quotation quotation =
                    getQuotationAndCheckOwnership(
                            id,
                            employee
                    );

            if (status == null) {

                throw new IllegalArgumentException(
                        "Quotation status is required."
                );
            }

            QuotationStatus oldStatus =
                    quotation.getStatus();

            /*
             * Current supported statuses:
             *
             * DRAFT
             * PENDING
             * SENT
             * APPROVED
             * CANCELLED
             * COMPLETED
             *
             * REJECTED and EXPIRED are no longer part
             * of the QuotationStatus enum.
             */

            quotationService.changeStatus(
                    id,
                    status
            );

            Quotation updatedQuotation =
                    quotationService.getById(id);

            /*
             * Save status history.
             */
            quotationStatusHistoryService.create(
                    updatedQuotation,
                    oldStatus,
                    status,
                    employee,
                    null
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Quotation status updated successfully."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to update quotation status: "
                            + e.getMessage()
            );
        }

        return "redirect:/employee/quotations/view/"
                + id;
    }


    // ============================================================
    // NEW ROOM PAGE
    // ============================================================

    @GetMapping("/view/{id}/room/new")
    public String newRoom(

            @PathVariable Long id,

            Authentication authentication,

            Model model
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        Quotation quotation =
                getQuotationAndCheckOwnership(
                        id,
                        employee
                );

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "quotation",
                quotation
        );

        return "employee/quotation-room-form";
    }


    // ============================================================
    // SAVE ROOM
    // ============================================================

    @PostMapping("/view/{id}/room/save")
    public String saveRoom(

            @PathVariable Long id,

            @RequestParam String floor,

            @RequestParam String room,

            @RequestParam(required = false)
            String workDescription,

            Authentication authentication
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        Quotation quotation =
                getQuotationAndCheckOwnership(
                        id,
                        employee
                );

        if (floor == null
                || floor.isBlank()
                || room == null
                || room.isBlank()) {

            throw new IllegalArgumentException(
                    "Floor and room are required."
            );
        }

        quotationRoomService.create(
                quotation,
                floor.trim(),
                room.trim(),
                workDescription
        );

        return "redirect:/employee/quotations/view/"
                + id;
    }


    // ============================================================
    // NEW ITEM PAGE
    // ============================================================

    @GetMapping(
            "/view/{id}/room/{roomId}/item/new"
    )
    public String newItem(

            @PathVariable Long id,

            @PathVariable Long roomId,

            Authentication authentication,

            Model model
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        Quotation quotation =
                getQuotationAndCheckOwnership(
                        id,
                        employee
                );

        QuotationRoom quotationRoom =
                getQuotationRoomForQuotation(
                        roomId,
                        quotation
                );

        QuotationItem quotationItem =
                new QuotationItem();

        quotationItem.setQuotationRoom(
                quotationRoom
        );

        addItemFormMasterData(
                model,
                employee,
                quotation,
                quotationRoom
        );

        model.addAttribute(
                "quotationItem",
                quotationItem
        );

        model.addAttribute(
                "editMode",
                false
        );

        return "employee/quotation-item-form";
    }


    // ============================================================
    // SAVE NEW ITEM
    // ============================================================

    @PostMapping(
            "/view/{id}/room/{roomId}/item/save"
    )
    public String saveItem(

            @PathVariable Long id,

            @PathVariable Long roomId,

            @RequestParam Long workCategoryId,

            @RequestParam Long divisionId,

            @RequestParam Long itemId,

            @RequestParam Long materialId,

            @RequestParam Long materialOptionId,

            @RequestParam(required = false)
            String itemDescription,

            @RequestParam BigDecimal lengthValue,

            @RequestParam BigDecimal widthValue,

            @RequestParam BigDecimal heightValue,

            /*
             * This is kept so the existing HTML form
             * can continue submitting rate.
             *
             * IMPORTANT:
             * This value is intentionally NOT used.
             *
             * Server-side district pricing is always used.
             */
            @RequestParam(required = false)
            BigDecimal rate,

            @RequestParam(required = false)
            BigDecimal offerPrice,

            @RequestParam(required = false)
            String specification,

            Authentication authentication
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        Quotation quotation =
                getQuotationAndCheckOwnership(
                        id,
                        employee
                );

        QuotationRoom quotationRoom =
                getQuotationRoomForQuotation(
                        roomId,
                        quotation
                );

        validateDimensions(
                lengthValue,
                widthValue,
                heightValue
        );

        WorkCategory workCategory =
                workCategoryService
                        .getCategoryById(
                                workCategoryId
                        );

        Division division =
                divisionService
                        .getDivisionById(
                                divisionId
                        );

        Item item =
                itemService
                        .getItemById(
                                itemId
                        );

        Material material =
                materialService
                        .getMaterialById(
                                materialId
                        );

        MaterialOption materialOption =
                materialOptionService
                        .getOptionById(
                                materialOptionId
                        );

        /*
         * Validate master-data relationships.
         */

        validateItemRelationship(
                workCategory,
                division,
                item
        );

        validateMaterialRelationship(
                item,
                material
        );

        validateMaterialOptionRelationship(
                material,
                materialOption
        );

        /*
         * IMPORTANT:
         *
         * Rate comes from Admin/District Pricing.
         *
         * Browser rate is ignored.
         */

        BigDecimal resolvedRate =
                quotationCalculationService
                        .resolveItemRate(
                                quotation.getId(),
                                materialOptionId
                        );

        validateRate(
                resolvedRate
        );

        BigDecimal calculatedSqft =
                quotationItemService.calculateSqft(
                        lengthValue,
                        widthValue,
                        heightValue
                );

        BigDecimal amount =
                quotationItemService.calculateAmount(
                        calculatedSqft,
                        resolvedRate
                );

        BigDecimal finalOfferPrice =
                quotationItemService.calculateOfferPrice(
                        amount,
                        offerPrice
                );

        String formula =
                "(L + W) × H / 144";

        String finalSpecification =
                specification != null
                        && !specification.isBlank()
                        ? specification.trim()
                        : materialOption.getDescription();

        QuotationItem quotationItem =
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
                        resolvedRate,
                        amount,
                        finalOfferPrice,
                        formula,
                        finalSpecification
                );

        /*
         * Create size row for the item.
         */

        QuotationItemSize quotationItemSize =
                quotationItemSizeService.create(
                        quotationItem,
                        lengthValue,
                        widthValue,
                        heightValue,
                        finalOfferPrice,
                        1
                );

        quotationItem.addSize(
                quotationItemSize
        );

        /*
         * Recalculate entire quotation.
         */

        quotationCalculationService
                .calculateQuotation(
                        quotation.getId()
                );

        return "redirect:/employee/quotations/view/"
                + quotation.getId();
    }


    // ============================================================
    // EDIT ITEM PAGE
    // ============================================================

    @GetMapping(
            "/view/{quotationId}/room/{roomId}/item/{itemId}/edit"
    )
    public String editItem(

            @PathVariable Long quotationId,

            @PathVariable Long roomId,

            @PathVariable Long itemId,

            Authentication authentication,

            Model model
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        Quotation quotation =
                getQuotationAndCheckOwnership(
                        quotationId,
                        employee
                );

        QuotationRoom quotationRoom =
                getQuotationRoomForQuotation(
                        roomId,
                        quotation
                );

        QuotationItem quotationItem =
                quotationItemService.getById(
                        itemId
                );

        validateQuotationItemBelongsToRoom(
                quotationItem,
                quotationRoom
        );

        if (!Boolean.TRUE.equals(
                quotationItem.getActive()
        )) {

            throw new IllegalStateException(
                    "This item is no longer active."
            );
        }

        addItemFormMasterData(
                model,
                employee,
                quotation,
                quotationRoom
        );

        model.addAttribute(
                "quotationItem",
                quotationItem
        );

        model.addAttribute(
                "editMode",
                true
        );

        /*
         * IMPORTANT:
         *
         * Use the same employee quotation-item-form.html
         * for both NEW and EDIT.
         */

        return "employee/quotation-item-form";
    }


    // ============================================================
    // UPDATE ITEM
    // ============================================================

    @PostMapping(
            "/view/{quotationId}/room/{roomId}/item/{itemId}/update"
    )
    public String updateItem(

            @PathVariable Long quotationId,

            @PathVariable Long roomId,

            @PathVariable Long itemId,

            @RequestParam Long workCategoryId,

            @RequestParam Long divisionId,

            @RequestParam Long itemIdValue,

            @RequestParam Long materialId,

            @RequestParam Long materialOptionId,

            @RequestParam(required = false)
            String itemDescription,

            @RequestParam BigDecimal lengthValue,

            @RequestParam BigDecimal widthValue,

            @RequestParam BigDecimal heightValue,

            /*
             * Kept for compatibility with current HTML.
             * Never trusted for calculation.
             */
            @RequestParam(required = false)
            BigDecimal rate,

            @RequestParam(required = false)
            BigDecimal offerPrice,

            @RequestParam(required = false)
            String specification,

            Authentication authentication
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        Quotation quotation =
                getQuotationAndCheckOwnership(
                        quotationId,
                        employee
                );

        QuotationRoom quotationRoom =
                getQuotationRoomForQuotation(
                        roomId,
                        quotation
                );

        QuotationItem quotationItem =
                quotationItemService.getById(
                        itemId
                );

        validateQuotationItemBelongsToRoom(
                quotationItem,
                quotationRoom
        );

        if (!Boolean.TRUE.equals(
                quotationItem.getActive()
        )) {

            throw new IllegalStateException(
                    "This item is no longer active."
            );
        }

        validateDimensions(
                lengthValue,
                widthValue,
                heightValue
        );

        WorkCategory workCategory =
                workCategoryService
                        .getCategoryById(
                                workCategoryId
                        );

        Division division =
                divisionService
                        .getDivisionById(
                                divisionId
                        );

        Item item =
                itemService
                        .getItemById(
                                itemIdValue
                        );

        Material material =
                materialService
                        .getMaterialById(
                                materialId
                        );

        MaterialOption materialOption =
                materialOptionService
                        .getOptionById(
                                materialOptionId
                        );

        /*
         * Validate master-data relationships.
         */

        validateItemRelationship(
                workCategory,
                division,
                item
        );

        validateMaterialRelationship(
                item,
                material
        );

        validateMaterialOptionRelationship(
                material,
                materialOption
        );

        /*
         * Always resolve the correct Admin/District rate.
         */

        BigDecimal resolvedRate =
                quotationCalculationService
                        .resolveItemRate(
                                quotation.getId(),
                                materialOptionId
                        );

        validateRate(
                resolvedRate
        );

        BigDecimal calculatedSqft =
                quotationItemService.calculateSqft(
                        lengthValue,
                        widthValue,
                        heightValue
                );

        BigDecimal amount =
                quotationItemService.calculateAmount(
                        calculatedSqft,
                        resolvedRate
                );

        BigDecimal finalOfferPrice =
                quotationItemService.calculateOfferPrice(
                        amount,
                        offerPrice
                );

        String formula =
                "(L + W) × H / 144";

        String finalSpecification =
                specification != null
                        && !specification.isBlank()
                        ? specification.trim()
                        : materialOption.getDescription();

        quotationItemService.update(
                itemId,
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
                resolvedRate,
                amount,
                finalOfferPrice,
                formula,
                finalSpecification
        );

        quotationCalculationService
                .calculateQuotation(
                        quotation.getId()
                );

        return "redirect:/employee/quotations/view/"
                + quotation.getId();
    }


    // ============================================================
    // DELETE ITEM
    // ============================================================

    @PostMapping(
            "/view/{quotationId}/room/{roomId}/item/{itemId}/delete"
    )
    public String deleteItem(

            @PathVariable Long quotationId,

            @PathVariable Long roomId,

            @PathVariable Long itemId,

            Authentication authentication
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        Quotation quotation =
                getQuotationAndCheckOwnership(
                        quotationId,
                        employee
                );

        QuotationRoom quotationRoom =
                getQuotationRoomForQuotation(
                        roomId,
                        quotation
                );

        QuotationItem quotationItem =
                quotationItemService.getById(
                        itemId
                );

        validateQuotationItemBelongsToRoom(
                quotationItem,
                quotationRoom
        );

        if (!Boolean.TRUE.equals(
                quotationItem.getActive()
        )) {

            throw new IllegalStateException(
                    "This item is already inactive."
            );
        }

        quotationItemService.delete(
                itemId
        );

        quotationCalculationService
                .calculateQuotation(
                        quotation.getId()
                );

        return "redirect:/employee/quotations/view/"
                + quotation.getId();
    }


    // ============================================================
    // DOWNLOAD QUOTATION PDF
    // ============================================================

    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> downloadQuotationPdf(

            @PathVariable Long id,

            Authentication authentication
    ) {

        User employee =
                getLoggedInEmployee(
                        authentication
                );

        Quotation quotation =
                getQuotationAndCheckOwnership(
                        id,
                        employee
                );

        byte[] pdf =
                quotationPdfService
                        .generateQuotationPdf(id);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=quotation-"
                                + quotation.getId()
                                + ".pdf"
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .body(pdf);
    }


    // ============================================================
    // SEND QUOTATION EMAIL
    // ============================================================

    @PostMapping("/email/{id}")
    public String sendQuotationEmail(

            @PathVariable Long id,

            Authentication authentication,

            RedirectAttributes redirectAttributes
    ) {

        try {

            User employee =
                    getLoggedInEmployee(
                            authentication
                    );

            Quotation quotation =
                    getQuotationAndCheckOwnership(
                            id,
                            employee
                    );

            quotationEmailService
                    .sendQuotationEmail(
                            quotation
                    );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Quotation PDF emailed successfully."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Failed to send quotation email: "
                            + e.getMessage()
            );
        }

        return "redirect:/employee/quotations/view/"
                + id;
    }


    // ============================================================
    // WHATSAPP COMMUNICATION
    // ============================================================

    @GetMapping("/whatsapp/{id}")
    public String trackWhatsApp(

            @PathVariable Long id,

            Authentication authentication,

            RedirectAttributes redirectAttributes
    ) {

        try {

            User employee =
                    getLoggedInEmployee(
                            authentication
                    );

            Quotation quotation =
                    getQuotationAndCheckOwnership(
                            id,
                            employee
                    );

            if (quotation.getCustomer() == null) {

                throw new IllegalArgumentException(
                        "Customer not found."
                );
            }

            String phone =
                    quotation
                            .getCustomer()
                            .getPhone();

            if (phone == null
                    || phone.isBlank()) {

                throw new IllegalArgumentException(
                        "Customer phone number is not available."
                );
            }

            phone =
                    phone.replaceAll(
                            "[^0-9]",
                            ""
                    );

            if (phone.startsWith("0")
                    && phone.length() == 11) {

                phone =
                        phone.substring(1);
            }

            if (phone.length() == 10) {

                phone =
                        "91" + phone;
            }

            if (!phone.matches(
                    "[1-9][0-9]{7,14}"
            )) {

                throw new IllegalArgumentException(
                        "Customer phone number format is invalid."
                );
            }

            String quotationNumber =
                    quotation.getQuotationNumber();

            String message =
                    "Hello "
                            + quotation
                            .getCustomer()
                            .getName()
                            + ",\n\n"
                            + "Your quotation has been prepared.\n\n"
                            + "Quotation No: "
                            + quotationNumber
                            + "\n"
                            + "Quotation Amount: ₹"
                            + quotation.getGrandTotal()
                            + "\n\n"
                            + "Thank you,\n"
                            + "Pravin KITCHENS & INTERIORSS & INTERIORS";

            String encodedMessage =
                    java.net.URLEncoder.encode(
                            message,
                            java.nio.charset.StandardCharsets.UTF_8
                    );

            String whatsappUrl =
                    "https://wa.me/"
                            + phone
                            + "?text="
                            + encodedMessage;

            return "redirect:" + whatsappUrl;

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to open WhatsApp: "
                            + e.getMessage()
            );

            return "redirect:/employee/quotations/view/"
                    + id;
        }
    }


    // ============================================================
    // UPDATE CHARGES
    // ============================================================

    @PostMapping("/charges/{id}")
    public String updateCharges(

            @PathVariable Long id,

            @RequestParam(required = false)
            BigDecimal accessoriesAmount,

            @RequestParam(required = false)
            BigDecimal travelCharge,

            @RequestParam(required = false)
            BigDecimal otherCharges,

            @RequestParam(required = false)
            BigDecimal discountAmount,

            @RequestParam(required = false)
            BigDecimal gstPercentage,

            Authentication authentication,

            RedirectAttributes redirectAttributes
    ) {

        try {

            User employee =
                    getLoggedInEmployee(
                            authentication
                    );

            /*
             * IMPORTANT:
             *
             * Before changing quotation charges,
             * verify that this quotation belongs
             * to the logged-in employee.
             */

            getQuotationAndCheckOwnership(
                    id,
                    employee
            );

            quotationCalculationService
                    .updateCharges(
                            id,
                            accessoriesAmount,
                            travelCharge,
                            otherCharges,
                            discountAmount,
                            gstPercentage
                    );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Quotation charges updated successfully."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to update quotation charges: "
                            + e.getMessage()
            );
        }

        return "redirect:/employee/quotations/view/"
                + id;
    }


    // ============================================================
    // LOGGED-IN EMPLOYEE
    // ============================================================

    private User getLoggedInEmployee(
            Authentication authentication
    ) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new IllegalStateException(
                    "Employee authentication not found"
            );
        }

        return userRepository
                .findByEmail(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Employee not found"
                        )
                );
    }


    // ============================================================
    // GET QUOTATION + OWNERSHIP CHECK
    // ============================================================

    private Quotation getQuotationAndCheckOwnership(

            Long quotationId,

            User employee
    ) {

        if (quotationId == null) {

            throw new IllegalArgumentException(
                    "Quotation ID is required."
            );
        }

        if (employee == null
                || employee.getId() == null) {

            throw new IllegalStateException(
                    "Employee information is not available."
            );
        }

        Quotation quotation =
                quotationRepository.findById(
                                quotationId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found"
                                )
                        );

        checkOwnership(
                quotation,
                employee
        );

        return quotation;
    }


    // ============================================================
    // GET ROOM + VERIFY QUOTATION
    // ============================================================

    private QuotationRoom getQuotationRoomForQuotation(

            Long roomId,

            Quotation quotation
    ) {

        if (roomId == null) {

            throw new IllegalArgumentException(
                    "Room ID is required."
            );
        }

        if (quotation == null
                || quotation.getId() == null) {

            throw new IllegalArgumentException(
                    "Quotation is required."
            );
        }

        QuotationRoom quotationRoom =
                quotationRoomService.getById(
                        roomId
                );

        if (quotationRoom.getQuotation() == null
                || quotationRoom
                .getQuotation()
                .getId() == null
                || !quotationRoom
                .getQuotation()
                .getId()
                .equals(quotation.getId())) {

            throw new IllegalStateException(
                    "Room does not belong to this quotation"
            );
        }

        return quotationRoom;
    }


    // ============================================================
    // ADD ITEM FORM MASTER DATA
    // ============================================================

    private void addItemFormMasterData(

            Model model,

            User employee,

            Quotation quotation,

            QuotationRoom quotationRoom
    ) {

        List<WorkCategory> workCategories =
                workCategoryService
                        .getActiveCategories();

        List<Division> divisions =
                divisionService
                        .getActiveDivisions();

        List<Item> items =
                itemService
                        .getActiveItems();

        List<Material> materials =
                materialService
                        .getActiveMaterials();

        List<MaterialOption> materialOptions =
                materialOptionService
                        .getActiveOptions();

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "quotation",
                quotation
        );

        model.addAttribute(
                "quotationRoom",
                quotationRoom
        );

        model.addAttribute(
                "workCategories",
                workCategories
        );

        model.addAttribute(
                "divisions",
                divisions
        );

        /*
         * IMPORTANT:
         *
         * Employee quotation-item-form.html
         * expects ${itemsList}.
         */

        model.addAttribute(
                "itemsList",
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

        /*
         * Send current quotation pricing mode
         * to the employee form.
         */

        model.addAttribute(
                "pricingMode",
                quotation.getPricingMode()
        );

        model.addAttribute(
                "pricingModes",
                PricingMode.values()
        );
    }


    // ============================================================
    // OWNERSHIP CHECK
    // ============================================================

    private void checkOwnership(

            Quotation quotation,

            User employee
    ) {

        if (quotation == null) {

            throw new IllegalArgumentException(
                    "Quotation is required."
            );
        }

        if (employee == null
                || employee.getId() == null) {

            throw new IllegalStateException(
                    "Employee is not available."
            );
        }

        if (quotation.getEmployee() == null
                || quotation
                .getEmployee()
                .getId() == null
                || !quotation
                .getEmployee()
                .getId()
                .equals(employee.getId())) {

            throw new IllegalStateException(
                    "You are not authorized to modify this quotation"
            );
        }
    }


    // ============================================================
    // VALIDATE QUOTATION ITEM
    // ============================================================

    private void validateQuotationItemBelongsToRoom(

            QuotationItem quotationItem,

            QuotationRoom quotationRoom
    ) {

        if (quotationItem == null) {

            throw new IllegalArgumentException(
                    "Quotation item not found."
            );
        }

        if (quotationRoom == null
                || quotationRoom.getId() == null) {

            throw new IllegalArgumentException(
                    "Quotation room not found."
            );
        }

        if (quotationItem.getQuotationRoom() == null
                || quotationItem
                .getQuotationRoom()
                .getId() == null
                || !quotationItem
                .getQuotationRoom()
                .getId()
                .equals(quotationRoom.getId())) {

            throw new IllegalStateException(
                    "Item does not belong to this room"
            );
        }
    }


    // ============================================================
    // VALIDATE WORK CATEGORY → DIVISION → ITEM
    // ============================================================

    private void validateItemRelationship(

            WorkCategory workCategory,

            Division division,

            Item item
    ) {

        if (workCategory == null) {

            throw new IllegalArgumentException(
                    "Work category not found."
            );
        }

        if (division == null) {

            throw new IllegalArgumentException(
                    "Division not found."
            );
        }

        if (item == null) {

            throw new IllegalArgumentException(
                    "Item not found."
            );
        }

        if (division.getWorkCategory() == null
                || division
                .getWorkCategory()
                .getId() == null
                || !division
                .getWorkCategory()
                .getId()
                .equals(workCategory.getId())) {

            throw new IllegalArgumentException(
                    "Selected division does not belong to the selected work category."
            );
        }

        if (item.getDivision() == null
                || item
                .getDivision()
                .getId() == null
                || !item
                .getDivision()
                .getId()
                .equals(division.getId())) {

            throw new IllegalArgumentException(
                    "Selected item does not belong to the selected division."
            );
        }
    }


    // ============================================================
    // VALIDATE ITEM → MATERIAL
    // ============================================================

    private void validateMaterialRelationship(

            Item item,

            Material material
    ) {

        if (item == null) {

            throw new IllegalArgumentException(
                    "Item is required."
            );
        }

        if (material == null) {

            throw new IllegalArgumentException(
                    "Material not found."
            );
        }

        /*
         * Material must belong to selected item.
         */

        if (material.getItem() == null
                || material
                .getItem()
                .getId() == null
                || !material
                .getItem()
                .getId()
                .equals(item.getId())) {

            throw new IllegalArgumentException(
                    "Selected material does not belong to the selected item."
            );
        }
    }


    // ============================================================
    // VALIDATE MATERIAL → MATERIAL OPTION
    // ============================================================

    private void validateMaterialOptionRelationship(

            Material material,

            MaterialOption materialOption
    ) {

        if (material == null) {

            throw new IllegalArgumentException(
                    "Material is required."
            );
        }

        if (materialOption == null) {

            throw new IllegalArgumentException(
                    "Material option not found."
            );
        }

        if (materialOption.getMaterial() == null
                || materialOption
                .getMaterial()
                .getId() == null
                || !materialOption
                .getMaterial()
                .getId()
                .equals(material.getId())) {

            throw new IllegalArgumentException(
                    "Selected material option does not belong to the selected material."
            );
        }
    }


    // ============================================================
    // VALIDATE DIMENSIONS
    // ============================================================

    private void validateDimensions(

            BigDecimal length,

            BigDecimal width,

            BigDecimal height
    ) {

        if (length == null
                || width == null
                || height == null) {

            throw new IllegalArgumentException(
                    "Length, Width and Height are required."
            );
        }

        if (length.compareTo(
                BigDecimal.ZERO
        ) <= 0
                || width.compareTo(
                BigDecimal.ZERO
        ) <= 0
                || height.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalArgumentException(
                    "Length, Width and Height must be greater than zero."
            );
        }
    }


    // ============================================================
    // VALIDATE SIZE LISTS
    // ============================================================

    private void validateSizeLists(

            List<BigDecimal> lengthValue,

            List<BigDecimal> widthValue,

            List<BigDecimal> heightValue
    ) {

        if (lengthValue == null
                || widthValue == null
                || heightValue == null
                || lengthValue.isEmpty()
                || widthValue.isEmpty()
                || heightValue.isEmpty()) {

            throw new IllegalArgumentException(
                    "Please add at least one size."
            );
        }

        if (lengthValue.size()
                != widthValue.size()
                || lengthValue.size()
                != heightValue.size()) {

            throw new IllegalArgumentException(
                    "Invalid size information."
            );
        }
    }


    // ============================================================
    // VALIDATE RATE
    // ============================================================

    private void validateRate(
            BigDecimal rate
    ) {

        if (rate == null
                || rate.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalArgumentException(
                    "A valid admin district rate is required for the selected material option."
            );
        }
    }


    // ============================================================
    // PARSE OFFER PRICE
    // ============================================================

    private BigDecimal parseOfferPrice(

            List<String> offerPrice,

            int index
    ) {

        if (offerPrice == null
                || index < 0
                || index >= offerPrice.size()) {

            return null;
        }

        String value =
                offerPrice.get(index);

        if (value == null
                || value.isBlank()) {

            return null;
        }

        try {

            BigDecimal parsed =
                    new BigDecimal(
                            value.trim()
                    );

            if (parsed.compareTo(
                    BigDecimal.ZERO
            ) < 0) {

                throw new IllegalArgumentException(
                        "Offer price cannot be negative."
                );
            }

            return parsed;

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Invalid offer price."
            );
        }
    }


    // ============================================================
    // QUOTATION NUMBER
    // ============================================================

    private String generateQuotationNumber() {

        long next =
                quotationRepository.count() + 1;

        String candidate;

        do {

            candidate =
                    String.format(
                            "QT-%05d",
                            next
                    );

            next++;

        } while (
                quotationRepository
                        .existsByQuotationNumber(
                                candidate
                        )
        );

        return candidate;
    }


    private String previewQuotationNumber() {

        long next =
                quotationRepository.count() + 1;

        String candidate;

        do {

            candidate =
                    String.format(
                            "QT-%05d",
                            next
                    );

            next++;

        } while (
                quotationRepository
                        .existsByQuotationNumber(
                                candidate
                        )
        );

        return candidate;
    }
}