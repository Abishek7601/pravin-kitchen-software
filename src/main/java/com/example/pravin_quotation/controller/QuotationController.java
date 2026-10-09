package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.CustomerRepository;
import com.example.pravin_quotation.repository.DivisionRepository;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/quotations")
public class QuotationController {

    private final QuotationService quotationService;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final DivisionRepository divisionRepository;

    private final QuotationRoomService quotationRoomService;
    private final QuotationItemService quotationItemService;
    private final QuotationItemSizeService quotationItemSizeService;

    private final QuotationStatusHistoryService quotationStatusHistoryService;
    private final QuotationEmailService quotationEmailService;
    private final QuotationCommunicationService quotationCommunicationService;
    private final QuotationCalculationService quotationCalculationService;


    public QuotationController(
            QuotationService quotationService,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            BranchRepository branchRepository,
            DivisionRepository divisionRepository,
            QuotationRoomService quotationRoomService,
            QuotationItemService quotationItemService,
            QuotationStatusHistoryService quotationStatusHistoryService,
            QuotationEmailService quotationEmailService,
            QuotationItemSizeService quotationItemSizeService,
            QuotationCommunicationService quotationCommunicationService,
            QuotationCalculationService quotationCalculationService

    ) {

        this.quotationService = quotationService;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.divisionRepository = divisionRepository;

        this.quotationRoomService = quotationRoomService;
        this.quotationItemService = quotationItemService;

        this.quotationStatusHistoryService =
                quotationStatusHistoryService;

        this.quotationEmailService =
                quotationEmailService;

        this.quotationItemSizeService = quotationItemSizeService;

        this.quotationCommunicationService =
                quotationCommunicationService;

        this.quotationCalculationService = quotationCalculationService;
    }


    // =========================================================
    // QUOTATION LIST
    // =========================================================

    @GetMapping
    public String quotations(Model model) {

        model.addAttribute(
                "quotationList",
                quotationService.getAllQuotations()
        );

        model.addAttribute(
                "quotationStatuses",
                QuotationStatus.values()
        );

        return "admin/quotations";
    }


    // =========================================================
    // NEW QUOTATION
    // =========================================================

    @GetMapping("/new")
    public String newQuotation(Model model) {

        Quotation quotation =
                new Quotation();

        quotation.setQuotationDate(
                LocalDate.now()
        );

        quotation.setStatus(
                QuotationStatus.DRAFT
        );

        model.addAttribute(
                "quotation",
                quotation
        );

        model.addAttribute(
                "customers",
                customerRepository.findByActiveTrue()
        );

        model.addAttribute(
                "employees",
                userRepository.findAll()
                        .stream()
                        .filter(user ->
                                Boolean.TRUE.equals(
                                        user.getActive()
                                )
                        )
                        .toList()
        );

        model.addAttribute(
                "branches",
                branchRepository.findAll()
                        .stream()
                        .filter(branch ->
                                Boolean.TRUE.equals(
                                        branch.getActive()
                                )
                        )
                        .toList()
        );

        model.addAttribute(
                "divisions",
                divisionRepository.findAll()
                        .stream()
                        .filter(division ->
                                Boolean.TRUE.equals(
                                        division.getActive()
                                )
                        )
                        .toList()
        );

        model.addAttribute(
                "pricingModes",
                PricingMode.values()
        );

        return "admin/quotation-form";
    }


    // =========================================================
    // SAVE QUOTATION
    // =========================================================

    @PostMapping("/save")
    public String saveQuotation(
            @RequestParam String quotationNumber,
            @RequestParam Long customerId,
            @RequestParam Long employeeId,
            @RequestParam Long branchId,
            @RequestParam Long divisionId,
            @RequestParam(required = false) String quotationDate,
            @RequestParam PricingMode pricingMode,
            @RequestParam(required = false) String customerRequirements,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes
    ) {

        try {

            LocalDate date = null;

            if (quotationDate != null
                    && !quotationDate.isBlank()) {

                date = LocalDate.parse(
                        quotationDate
                );
            }


            Quotation quotation =
                    quotationService.create(
                            quotationNumber,
                            customerId,
                            employeeId,
                            branchId,
                            date,
                            pricingMode,
                            customerRequirements,
                            notes
                    );


            // -------------------------------------------------
            // SET DIVISION
            // -------------------------------------------------

            quotation.setDivision(
                    divisionRepository
                            .findById(divisionId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Division not found."
                                    )
                            )
            );


            quotationService.save(
                    quotation
            );


            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Quotation created successfully."
            );

            return "redirect:/admin/quotations";

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/admin/quotations/new";
        }
    }


    // =========================================================
    // VIEW QUOTATION
    // =========================================================

    @GetMapping("/view/{id}")
    public String viewQuotation(
            @PathVariable Long id,
            Model model
    ) {

        // -----------------------------------------------------
        // QUOTATION
        // -----------------------------------------------------

        Quotation quotation =
                quotationService.getById(id);

        model.addAttribute(
                "quotation",
                quotation
        );


        // -----------------------------------------------------
        // ACTIVE ROOMS
        // -----------------------------------------------------

        List<QuotationRoom> quotationRooms =
                quotationRoomService
                        .getActiveByQuotationId(id);

        model.addAttribute(
                "quotationRooms",
                quotationRooms
        );


// -----------------------------------------------------
// ACTIVE ITEMS BY ROOM
// -----------------------------------------------------

        Map<Long, List<QuotationItem>> itemsByRoom =
                new HashMap<>();

// -----------------------------------------------------
// ACTIVE SIZES BY ITEM
// -----------------------------------------------------

        Map<Long, List<QuotationItemSize>> sizesByItem =
                new HashMap<>();

        for (QuotationRoom room : quotationRooms) {

            List<QuotationItem> items =
                    quotationItemService
                            .getActiveByQuotationRoomId(
                                    room.getId()
                            );

            itemsByRoom.put(
                    room.getId(),
                    items
            );

            // ---------------------------------------------
            // Get all sizes for every item
            // ---------------------------------------------

            for (QuotationItem item : items) {

                List<QuotationItemSize> sizes =
                        quotationItemSizeService
                                .getActiveSizes(
                                        item.getId()
                                );

                sizesByItem.put(
                        item.getId(),
                        sizes
                );
            }
        }

        model.addAttribute(
                "itemsByRoom",
                itemsByRoom
        );

        model.addAttribute(
                "sizesByItem",
                sizesByItem
        );


        // -----------------------------------------------------
        // STATUS HISTORY
        // -----------------------------------------------------

        model.addAttribute(
                "statusHistory",
                quotationStatusHistoryService
                        .getByQuotationId(id)
        );


        // -----------------------------------------------------
        // COMMUNICATION HISTORY
        // -----------------------------------------------------

        model.addAttribute(
                "communicationHistory",
                quotationCommunicationService
                        .getByQuotationId(id)
        );


        return "admin/quotation-view";
    }


    // =========================================================
    // EDIT QUOTATION
    // =========================================================

    @GetMapping("/edit/{id}")
    public String editQuotation(
            @PathVariable Long id,
            Model model
    ) {

        model.addAttribute(
                "quotation",
                quotationService.getById(id)
        );

        model.addAttribute(
                "customers",
                customerRepository.findByActiveTrue()
        );

        model.addAttribute(
                "employees",
                userRepository.findAll()
                        .stream()
                        .filter(user ->
                                Boolean.TRUE.equals(
                                        user.getActive()
                                )
                        )
                        .toList()
        );

        model.addAttribute(
                "branches",
                branchRepository.findAll()
                        .stream()
                        .filter(branch ->
                                Boolean.TRUE.equals(
                                        branch.getActive()
                                )
                        )
                        .toList()
        );

        model.addAttribute(
                "divisions",
                divisionRepository.findAll()
                        .stream()
                        .filter(division ->
                                Boolean.TRUE.equals(
                                        division.getActive()
                                )
                        )
                        .toList()
        );

        model.addAttribute(
                "pricingModes",
                PricingMode.values()
        );

        return "admin/quotation-form";
    }


    // =========================================================
    // UPDATE QUOTATION
    // =========================================================

    @PostMapping("/update/{id}")
    public String updateQuotation(
            @PathVariable Long id,
            @RequestParam Long customerId,
            @RequestParam Long employeeId,
            @RequestParam Long branchId,
            @RequestParam Long divisionId,
            @RequestParam(required = false) String quotationDate,
            @RequestParam PricingMode pricingMode,
            @RequestParam(required = false) String customerRequirements,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes
    ) {

        try {

            LocalDate date = null;

            if (quotationDate != null
                    && !quotationDate.isBlank()) {

                date = LocalDate.parse(
                        quotationDate
                );
            }


            quotationService.updateBasicDetails(
                    id,
                    customerId,
                    employeeId,
                    branchId,
                    date,
                    pricingMode,
                    customerRequirements,
                    notes
            );


            // -------------------------------------------------
            // UPDATE DIVISION
            // -------------------------------------------------

            Quotation quotation =
                    quotationService.getById(id);

            quotation.setDivision(
                    divisionRepository
                            .findById(divisionId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Division not found."
                                    )
                            )
            );


            quotationService.save(
                    quotation
            );


            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Quotation updated successfully."
            );

            return "redirect:/admin/quotations";

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/admin/quotations/edit/" + id;
        }
    }


    // =========================================================
    // CHANGE STATUS
    // =========================================================

    @PostMapping("/status/{id}")
    public String changeStatus(
            @PathVariable Long id,
            @RequestParam QuotationStatus status
    ) {

        Quotation quotation =
                quotationService.getById(id);

        QuotationStatus oldStatus =
                quotation.getStatus();


        quotationService.changeStatus(
                id,
                status
        );


        Quotation updatedQuotation =
                quotationService.getById(id);


        quotationStatusHistoryService.create(
                updatedQuotation,
                oldStatus,
                status,
                updatedQuotation.getEmployee(),
                null
        );


        return "redirect:/admin/quotations/view/" + id;
    }


    // =========================================================
    // SEND EMAIL
    // =========================================================

    @PostMapping("/email/{id}")
    public String sendQuotationEmail(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Quotation quotation =
                    quotationService.getById(id);

            quotationEmailService.sendQuotationEmail(
                    quotation
            );


            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Quotation email sent successfully."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Failed to send quotation email: "
                            + e.getMessage()
            );
        }

        return "redirect:/admin/quotations/view/" + id;
    }


    // =========================================================
    // WHATSAPP TRACKING
    // =========================================================

    @GetMapping("/whatsapp/{id}")
    public String trackWhatsApp(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Quotation quotation =
                    quotationService.getById(id);


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


            quotationCommunicationService.record(
                    quotation,
                    QuotationCommunication.CommunicationType.WHATSAPP,
                    phone,
                    QuotationCommunication.CommunicationStatus.SENT,
                    "Quotation WhatsApp message initiated."
            );


            return "redirect:/admin/quotations/view/" + id;

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "WhatsApp tracking failed: "
                            + e.getMessage()
            );

            return "redirect:/admin/quotations/view/" + id;
        }
    }


    // =========================================================
    // DELETE QUOTATION
    // =========================================================

    @PostMapping("/delete/{id}")
    public String deleteQuotation(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            quotationService.delete(
                    id
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Quotation deleted successfully."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/quotations";
    }


    // =========================================================
    // QUOTATIONS BY STATUS
    // =========================================================

    @GetMapping("/status/{status}")
    public String quotationsByStatus(
            @PathVariable QuotationStatus status,
            Model model
    ) {

        model.addAttribute(
                "quotationList",
                quotationService.getByStatus(status)
        );

        model.addAttribute(
                "quotationStatuses",
                QuotationStatus.values()
        );

        model.addAttribute(
                "selectedStatus",
                status
        );

        return "admin/quotations";
    }


    // =========================================================
    // LAST 60 DAYS QUOTATIONS
    // =========================================================

    @GetMapping("/last-60-days")
    public String last60DaysQuotations(
            Model model
    ) {

        model.addAttribute(
                "quotationList",
                quotationService.getLast60DaysQuotations()
        );

        model.addAttribute(
                "quotationStatuses",
                QuotationStatus.values()
        );

        model.addAttribute(
                "isLast60Days",
                true
        );

        return "admin/quotations";
    }


    // =========================================================
    // CUSTOMER QUOTATIONS
    // =========================================================

    @GetMapping("/customer/{customerId}")
    public String customerQuotations(
            @PathVariable Long customerId,
            Model model
    ) {

        model.addAttribute(
                "quotationList",
                quotationService.getByCustomerId(
                        customerId
                )
        );

        model.addAttribute(
                "quotationStatuses",
                QuotationStatus.values()
        );

        model.addAttribute(
                "selectedCustomerId",
                customerId
        );

        return "admin/quotations";
    }


    // =========================================================
    // EMPLOYEE QUOTATIONS
    // =========================================================

    @GetMapping("/employee/{employeeId}")
    public String employeeQuotations(
            @PathVariable Long employeeId,
            Model model
    ) {

        model.addAttribute(
                "quotationList",
                quotationService.getByEmployeeId(
                        employeeId
                )
        );

        model.addAttribute(
                "quotationStatuses",
                QuotationStatus.values()
        );

        model.addAttribute(
                "selectedEmployeeId",
                employeeId
        );

        return "admin/quotations";
    }


    // =========================================================
    // BRANCH QUOTATIONS
    // =========================================================

    @GetMapping("/branch/{branchId}")
    public String branchQuotations(
            @PathVariable Long branchId,
            Model model
    ) {

        model.addAttribute(
                "quotationList",
                quotationService.getByBranchId(
                        branchId
                )
        );

        model.addAttribute(
                "quotationStatuses",
                QuotationStatus.values()
        );

        model.addAttribute(
                "selectedBranchId",
                branchId
        );

        return "admin/quotations";
    }

    @PostMapping("/charges/{id}")
    public String updateCharges(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal accessoriesAmount,
            @RequestParam(required = false) BigDecimal travelCharge,
            @RequestParam(required = false) BigDecimal otherCharges,
            @RequestParam(required = false) BigDecimal discountAmount,
            @RequestParam(required = false) BigDecimal gstPercentage,
            RedirectAttributes redirectAttributes) {

        try {

            quotationCalculationService.updateCharges(
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

        return "redirect:/admin/quotations/view/" + id;
    }
}