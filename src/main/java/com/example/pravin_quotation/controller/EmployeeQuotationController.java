package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Customer;
import com.example.pravin_quotation.model.Division;
import com.example.pravin_quotation.model.Item;
import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.model.MaterialOption;
import com.example.pravin_quotation.model.PricingMode;
import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationItem;
import com.example.pravin_quotation.model.QuotationRoom;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.model.WorkCategory;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.CustomerRepository;
import com.example.pravin_quotation.repository.DivisionRepository;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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

    private final QuotationService quotationService;
    private final QuotationRoomService quotationRoomService;
    private final QuotationItemService quotationItemService;
    private final QuotationCalculationService quotationCalculationService;

    private final WorkCategoryService workCategoryService;
    private final DivisionService divisionService;
    private final ItemService itemService;
    private final MaterialService materialService;
    private final MaterialOptionService materialOptionService;
    private final QuotationPdfService quotationPdfService;
    private final QuotationEmailService quotationEmailService;
    private final QuotationCommunicationService quotationCommunicationService;

    public EmployeeQuotationController(
            QuotationRepository quotationRepository,
            UserRepository userRepository,
            CustomerRepository customerRepository,
            BranchRepository branchRepository,
            DivisionRepository divisionRepository,
            QuotationService quotationService,
            QuotationRoomService quotationRoomService,
            QuotationItemService quotationItemService,
            QuotationCalculationService quotationCalculationService,
            WorkCategoryService workCategoryService,
            DivisionService divisionService,
            ItemService itemService,
            MaterialService materialService,
            MaterialOptionService materialOptionService,
            QuotationPdfService quotationPdfService,
            QuotationEmailService quotationEmailService,
            QuotationCommunicationService quotationCommunicationService
    ) {

        this.quotationRepository = quotationRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.divisionRepository = divisionRepository;

        this.quotationService = quotationService;
        this.quotationRoomService = quotationRoomService;
        this.quotationItemService = quotationItemService;
        this.quotationCalculationService = quotationCalculationService;

        this.workCategoryService = workCategoryService;
        this.divisionService = divisionService;
        this.itemService = itemService;
        this.materialService = materialService;
        this.materialOptionService = materialOptionService;
        this.quotationPdfService = quotationPdfService;
        this.quotationEmailService = quotationEmailService;
        this.quotationCommunicationService = quotationCommunicationService;
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
    // CREATE NEW QUOTATION PAGE
    // ============================================================

    @GetMapping("/new")
    public String newQuotation(
            Authentication authentication,
            Model model
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        if (employee.getBranch() == null) {

            throw new IllegalStateException(
                    "Employee is not assigned to a branch"
            );
        }

        Long branchId =
                employee.getBranch().getId();

        List<Customer> customers =
                customerRepository.findByBranchId(
                        branchId
                );

        var branches =
                branchRepository.findAll()
                        .stream()
                        .filter(branch ->
                                Boolean.TRUE.equals(
                                        branch.getActive()
                                )
                        )
                        .toList();

        List<Division> divisions =
                divisionRepository.findAll()
                        .stream()
                        .filter(division ->
                                Boolean.TRUE.equals(
                                        division.getActive()
                                )
                        )
                        .toList();

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "customers",
                customers
        );

        model.addAttribute(
                "branches",
                branches
        );

        model.addAttribute(
                "divisions",
                divisions
        );

        model.addAttribute(
                "pricingModes",
                PricingMode.values()
        );

        model.addAttribute(
                "quotationDate",
                LocalDate.now()
        );

        return "employee/quotation-new";
    }


    // ============================================================
    // SAVE NEW QUOTATION
    // ============================================================

    @PostMapping("/save")
    public String saveQuotation(

            @RequestParam Long customerId,

            @RequestParam Long branchId,

            @RequestParam Long divisionId,

            @RequestParam String quotationDate,

            @RequestParam PricingMode pricingMode,

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

        LocalDate date =
                LocalDate.parse(
                        quotationDate,
                        DateTimeFormatter.ISO_LOCAL_DATE
                );

        String quotationNumber =
                generateQuotationNumber();

        Quotation quotation =
                quotationService.create(
                        quotationNumber,
                        customerId,
                        employee.getId(),
                        branchId,
                        date,
                        pricingMode,
                        customerRequirements,
                        notes
                );

        quotation.setDivision(
                divisionRepository.findById(
                        divisionId
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Division not found"
                        )
                )
        );

        quotationRepository.save(quotation);

        quotationCalculationService.initializeTravelCharge(
                quotation.getId());

        return "redirect:/employee/quotations/view/"
                + quotation.getId();
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
                quotationRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found"
                                )
                        );

        checkOwnership(
                quotation,
                employee
        );

        // Recalculate before displaying
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

        return "employee/quotation-workspace";
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
                quotationRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found"
                                )
                        );

        checkOwnership(
                quotation,
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
                quotationRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found"
                                )
                        );

        checkOwnership(
                quotation,
                employee
        );

        quotationRoomService.create(
                quotation,
                floor,
                room,
                workDescription
        );

        return "redirect:/employee/quotations/view/"
                + id;
    }


    // ============================================================
    // NEW ITEM PAGE
    // ============================================================

    @GetMapping("/view/{id}/room/{roomId}/item/new")
    public String newItem(

            @PathVariable Long id,

            @PathVariable Long roomId,

            Authentication authentication,

            Model model
    ) {

        // Get the logged-in employee
        User employee =
                getLoggedInEmployee(authentication);

        // Get quotation and verify ownership
        Quotation quotation =
                quotationRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Quotation not found"
                                )
                        );

        checkOwnership(
                quotation,
                employee
        );

        // Get the room and verify it belongs to this quotation
        QuotationRoom quotationRoom =
                getQuotationRoomForQuotation(
                        roomId,
                        quotation
                );

        // IMPORTANT: Create a new item for the form
        QuotationItem quotationItem =
                new QuotationItem();

        // Connect the new item to the selected room
        quotationItem.setQuotationRoom(
                quotationRoom
        );

        // Load employee, quotation, room and dropdown data
        addItemFormMasterData(
                model,
                employee,
                quotation,
                quotationRoom
        );

        // IMPORTANT: Add quotationItem to the model
        model.addAttribute(
                "quotationItem",
                quotationItem
        );

        // Mark this as the new-item form
        model.addAttribute(
                "editMode",
                false
        );

        return "employee/quotation-item-form";
    }


    // ============================================================
    // SAVE NEW ITEM
    // ============================================================

    @PostMapping("/view/{id}/room/{roomId}/item/save")
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

            @RequestParam BigDecimal rate,

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

        BigDecimal calculatedSqft =
                quotationItemService.calculateSqft(
                        lengthValue,
                        widthValue,
                        heightValue
                );

        BigDecimal resolvedRate =
                quotationCalculationService.resolveItemRate(
                        quotation.getId(),
                        materialOptionId);

        BigDecimal amount =
                quotationItemService.calculateAmount(
                        calculatedSqft,
                        resolvedRate);

        BigDecimal finalOfferPrice =
                quotationItemService.calculateOfferPrice(
                        amount,
                        offerPrice
                );

        String formula =
                "(L + W) × H / 144";

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
                resolvedRate, // Use the database rate
                amount,
                finalOfferPrice,
                formula,
                specification
        );

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

    @GetMapping("/view/{quotationId}/room/{roomId}/item/{itemId}/edit")
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

        // Make sure item belongs to this room
        if (quotationItem.getQuotationRoom() == null
                || quotationItem
                .getQuotationRoom()
                .getId() == null
                || !quotationItem
                .getQuotationRoom()
                .getId()
                .equals(roomId)) {

            throw new IllegalStateException(
                    "Item does not belong to this room"
            );
        }

        // Make sure item is active
        if (!Boolean.TRUE.equals(
                quotationItem.getActive()
        )) {

            throw new IllegalStateException(
                    "This item is no longer active"
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

        return "employee/quotation-item-edit";
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

            @RequestParam BigDecimal rate,

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



        // --------------------------------------------------------
        // ITEM OWNERSHIP CHECK
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // LOAD MASTER DATA
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // CALCULATE SQ.FT
        // --------------------------------------------------------

        BigDecimal calculatedSqft =
                quotationItemService.calculateSqft(
                        lengthValue,
                        widthValue,
                        heightValue
                );

        // --------------------------------------------------------
        // CALCULATE AMOUNT
        // --------------------------------------------------------
        BigDecimal resolvedRate =
                quotationCalculationService.resolveItemRate(
                        quotation.getId(),
                        materialOptionId
                );

        BigDecimal amount =
                quotationItemService.calculateAmount(
                        calculatedSqft,
                        resolvedRate
                );

        // --------------------------------------------------------
        // CALCULATE OFFER PRICE
        // --------------------------------------------------------

        BigDecimal finalOfferPrice =
                quotationItemService.calculateOfferPrice(
                        amount,
                        offerPrice
                );

        // --------------------------------------------------------
        // FORMULA
        // --------------------------------------------------------

        String formula =
                "(L + W) × H / 144";

        // --------------------------------------------------------
        // UPDATE ITEM
        // --------------------------------------------------------

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
                resolvedRate, // Use resolved database rate
                amount,
                finalOfferPrice,
                formula,
                specification
        );

        // --------------------------------------------------------
        // RECALCULATE QUOTATION
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // ITEM OWNERSHIP CHECK
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // SOFT DELETE
        // --------------------------------------------------------

        quotationItemService.delete(
                itemId
        );

        // --------------------------------------------------------
        // RECALCULATE QUOTATION
        // --------------------------------------------------------

        quotationCalculationService
                .calculateQuotation(
                        quotation.getId()
                );

        return "redirect:/employee/quotations/view/"
                + quotation.getId();
    }


    // ============================================================
    // LOGGED-IN EMPLOYEE
    // ============================================================

    private User getLoggedInEmployee(
            Authentication authentication
    ) {

        if (authentication == null
                || authentication.getName() == null) {

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
    // LOAD QUOTATION + OWNERSHIP CHECK
    // ============================================================

    private Quotation getQuotationAndCheckOwnership(
            Long quotationId,
            User employee
    ) {

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
    // GET ROOM AND VERIFY QUOTATION
    // ============================================================

    private QuotationRoom getQuotationRoomForQuotation(
            Long roomId,
            Quotation quotation
    ) {

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
    }


    // ============================================================
    // OWNERSHIP CHECK
    // ============================================================

    private void checkOwnership(
            Quotation quotation,
            User employee
    ) {

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
    // QUOTATION NUMBER
    // ============================================================

    private String generateQuotationNumber() {

        String date =
                LocalDate.now()
                        .format(
                                DateTimeFormatter
                                        .ofPattern("yyyyMMdd")
                        );

        long count =
                quotationRepository.count();

        return String.format(
                "QT-%s-%04d",
                date,
                count + 1
        );
    }

    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> downloadQuotationPdf(
            @PathVariable Long id,
            Authentication authentication) {

        // Get the logged-in employee
        User employee = getLoggedInEmployee(authentication);

        // Load quotation and verify ownership
        Quotation quotation = quotationService.getById(id);
        checkOwnership(quotation, employee);

        // Generate PDF only after ownership verification
        byte[] pdf = quotationPdfService.generateQuotationPdf(id);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=quotation-" + id + ".pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }


    // ============================================================
    // SEND QUOTATION EMAIL
    // ============================================================

    @PostMapping("/email/{id}")
    public String sendQuotationEmail(
            @PathVariable Long id,
            Authentication authentication,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        try {
            User employee = getLoggedInEmployee(authentication);

            Quotation quotation =
                    getQuotationAndCheckOwnership(id, employee);

            quotationEmailService.sendQuotationEmail(quotation);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Quotation PDF emailed successfully."
            );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Failed to send quotation email: " + e.getMessage()
            );
        }

        return "redirect:/employee/quotations/view/" + id;
    }


    // ============================================================
    // WHATSAPP COMMUNICATION TRACKING
    // ============================================================


    @GetMapping("/whatsapp/{id}")
    public String trackWhatsApp(
            @PathVariable Long id,
            Authentication authentication,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        try {
            User employee = getLoggedInEmployee(authentication);
            Quotation quotation = getQuotationAndCheckOwnership(id, employee);

            if (quotation.getCustomer() == null) {
                throw new IllegalArgumentException("Customer not found.");
            }

            String phone = quotation.getCustomer().getPhone();

            if (phone == null || phone.isBlank()) {
                throw new IllegalArgumentException(
                        "Customer phone number is not available.");
            }

            // Normalize Indian phone number
            phone = phone.replaceAll("[^0-9]", "");

            if (phone.startsWith("0") && phone.length() == 11) {
                phone = phone.substring(1);
            }

            if (phone.length() == 10) {
                phone = "91" + phone;
            }

            if (!phone.matches("[1-9][0-9]{7,14}")) {
                throw new IllegalArgumentException(
                        "Customer phone number format is invalid.");
            }

            String quotationNumber = quotation.getQuotationNumber();

            String message = "Hello "
                    + quotation.getCustomer().getName()
                    + ", your quotation "
                    + quotationNumber
                    + " from Pravin Kitchens & Interiors is ready. "
                    + "Please find your quotation details attached via email. "
                    + "Thank you.";

            String encodedMessage = java.net.URLEncoder.encode(
                    message,
                    java.nio.charset.StandardCharsets.UTF_8);

            String whatsappUrl = "https://wa.me/"
                    + phone
                    + "?text="
                    + encodedMessage;

            return "redirect:" + whatsappUrl;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to open WhatsApp: " + e.getMessage());

            return "redirect:/employee/quotations/view/" + id;
        }
    }


}