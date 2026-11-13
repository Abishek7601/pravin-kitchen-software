package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.CustomerRepository;
import com.example.pravin_quotation.repository.DivisionRepository;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
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
    private final WhatsAppService whatsAppService;


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
            QuotationCalculationService quotationCalculationService,
            WhatsAppService whatsAppService

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
        this.whatsAppService = whatsAppService;
    }


    // =========================================================
    // QUOTATION LIST
    // =========================================================

    @GetMapping
    public String quotations(
            @RequestParam(name = "period", required = false) String period,
            Model model
    ) {

        List<Quotation> list;
        if ("60".equals(period)) {
            list = quotationService.getLast60DaysQuotations();
            model.addAttribute("isLast60Days", true);
        } else if ("365".equals(period) || "year".equalsIgnoreCase(period) || "1year".equalsIgnoreCase(period)) {
            list = quotationService.getOneYearQuotations();
            model.addAttribute("isOneYear", true);
        } else {
            list = quotationService.getAllQuotations();
        }

        model.addAttribute(
                "quotationList",
                withPricingDisplays(list)
        );

        model.addAttribute(
                "allQuotationList",
                quotationService.getAllQuotations()
        );

        model.addAttribute(
                "quotationStatuses",
                QuotationStatus.values()
        );

        model.addAttribute("selectedPeriod", period);

        return "admin/quotations";
    }


    // =========================================================
    // NEW QUOTATION
    // =========================================================

    @GetMapping("/new")
    public String newQuotation() {
        return "redirect:/admin/quotations/builder";
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

    public String viewQuotation(Long id, Model model) {
        return viewQuotation(id, null, model);
    }

    @GetMapping("/view/{id}")
    public String viewQuotation(
            @PathVariable Long id,
            @RequestParam(required = false) PricingMode grade,
            Model model
    ) {

        // -----------------------------------------------------
        // QUOTATION
        // -----------------------------------------------------

        Quotation quotation =
                quotationService.getById(id);

        if (quotation.getParent() != null) {
            return "redirect:/admin/quotations/view/" + quotation.getParent().getId()
                    + (quotation.getPricingMode() != null ? "?grade=" + quotation.getPricingMode().name() : "");
        }

        model.addAttribute(
                "quotation",
                quotation
        );

        // Multi-grade child quotations
        Quotation premiumGrade = quotation.getChildForGrade(PricingMode.PREMIUM);
        Quotation middleGrade = quotation.getChildForGrade(PricingMode.MIDDLE);
        Quotation economyGrade = quotation.getChildForGrade(PricingMode.ECONOMY);

        model.addAttribute("premiumGrade", premiumGrade);
        model.addAttribute("middleGrade", middleGrade);
        model.addAttribute("economyGrade", economyGrade);

        // Grade breakdowns
        Map<String, List<QuotationRoom>> gradeRooms = new HashMap<>();
        Map<String, Map<Long, List<QuotationItem>>> gradeItemsByRoom = new HashMap<>();
        Map<String, Map<Long, List<QuotationItemSize>>> gradeSizesByItem = new HashMap<>();

        for (PricingMode mode : PricingMode.values()) {
            Quotation child = quotation.getChildForGrade(mode);
            if (child != null) {
                List<QuotationRoom> rooms = quotationRoomService.getActiveByQuotationId(child.getId());
                gradeRooms.put(mode.name(), rooms);
                Map<Long, List<QuotationItem>> itemsMap = new HashMap<>();
                Map<Long, List<QuotationItemSize>> sizesMap = new HashMap<>();
                for (QuotationRoom r : rooms) {
                    List<QuotationItem> items = quotationItemService.getActiveByQuotationRoomId(r.getId());
                    itemsMap.put(r.getId(), items);
                    for (QuotationItem it : items) {
                        sizesMap.put(it.getId(), quotationItemSizeService.getActiveSizes(it.getId()));
                    }
                }
                gradeItemsByRoom.put(mode.name(), itemsMap);
                gradeSizesByItem.put(mode.name(), sizesMap);
            }
        }

        model.addAttribute("gradeRooms", gradeRooms);
        model.addAttribute("gradeItemsByRoom", gradeItemsByRoom);
        model.addAttribute("gradeSizesByItem", gradeSizesByItem);

        String activeGradeTab = "MIDDLE";
        if (grade != null && quotation.getChildForGrade(grade) != null) {
            activeGradeTab = grade.name();
        } else if (quotation.getSelectedGrade() != null) {
            activeGradeTab = quotation.getSelectedGrade().name();
        } else if (premiumGrade != null) {
            activeGradeTab = "PREMIUM";
        } else if (middleGrade != null) {
            activeGradeTab = "MIDDLE";
        } else if (economyGrade != null) {
            activeGradeTab = "ECONOMY";
        }
        model.addAttribute("activeGradeTab", activeGradeTab);

        // -----------------------------------------------------
        // ACTIVE ROOMS (Legacy & active grade fallback)
        // -----------------------------------------------------

        Quotation displayTarget = quotation.isParentQuotation()
                ? (quotation.getChildForGrade(PricingMode.valueOf(activeGradeTab)) != null ? quotation.getChildForGrade(PricingMode.valueOf(activeGradeTab)) : quotation)
                : quotation;

        List<QuotationRoom> quotationRooms =
                quotationRoomService
                        .getActiveByQuotationId(displayTarget.getId());

        model.addAttribute(
                "quotationRooms",
                quotationRooms
        );

        Map<Long, List<QuotationItem>> itemsByRoom =
                new HashMap<>();

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

        if (quotationCalculationService != null && quotation.getId() != null) {
            java.util.Map<PricingMode, BigDecimal> gradeSubtotals = quotationCalculationService.calculateGradeSubtotals(quotation.getId());
            model.addAttribute("gradeSubtotals", gradeSubtotals);
            model.addAttribute("premiumSubtotal", gradeSubtotals.get(PricingMode.PREMIUM));
            model.addAttribute("middleSubtotal", gradeSubtotals.get(PricingMode.MIDDLE));
            model.addAttribute("economySubtotal", gradeSubtotals.get(PricingMode.ECONOMY));
            String pricingModeDisplay = quotationCalculationService.resolvePricingModeDisplay(quotation.getId());
            quotation.setPricingModeDisplay(pricingModeDisplay);
            model.addAttribute("pricingModeDisplay", pricingModeDisplay);
        } else {
            model.addAttribute("pricingModeDisplay", quotation.getPricingModeDisplay());
        }

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
    // CONFIRM CUSTOMER SELECTED GRADE
    // =========================================================

    @PostMapping("/{id}/confirm-grade")
    public String confirmGrade(
            @PathVariable Long id,
            @RequestParam PricingMode grade,
            org.springframework.security.core.Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String loggedInEmail = authentication != null ? authentication.getName() : "ADMIN";
            quotationService.confirmCustomerGrade(id, grade, loggedInEmail);
            redirectAttributes.addFlashAttribute("successMessage",
                    grade.name() + " Grade confirmed as Customer's Selected Grade successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/quotations/view/" + id;
    }


    // =========================================================
    // EDIT QUOTATION
    // =========================================================

    @GetMapping("/edit/{id}")
    public String editQuotation(
            @PathVariable Long id
    ) {
        return "redirect:/admin/quotations/builder?quotationId=" + id;
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

    public Object sendQuotationEmail(Long id, RedirectAttributes redirectAttributes) {
        return sendQuotationEmail(id, null, null, redirectAttributes);
    }

    private List<Quotation> withPricingDisplays(List<Quotation> list) {
        if (quotationCalculationService != null && list != null && !list.isEmpty()) {
            quotationCalculationService.populatePricingModeDisplays(list);
        }
        return list;
    }

    @PostMapping("/email/{id}")
    public Object sendQuotationEmail(
            @PathVariable Long id,
            @RequestParam(required = false) PricingMode grade,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        String accept = request != null ? request.getHeader("Accept") : null;
        String requestedWith = request != null ? request.getHeader("X-Requested-With") : null;
        boolean wantsJson = (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        try {
            Quotation quotation = quotationService.getById(id);
            Quotation targetQuotation = quotation;
            if (grade != null) {
                Quotation child = quotation.getChildForGrade(grade);
                if (child != null) {
                    targetQuotation = child;
                }
            }

            quotationEmailService.sendQuotationEmail(targetQuotation);

            String successMsg = (grade != null ? grade.name() + " Grade " : "") + "Quotation email sent successfully.";
            if (wantsJson) {
                return ResponseEntity.ok(java.util.Map.of(
                        "success", true,
                        "message", successMsg,
                        "status", "SENT"
                ));
            }

            redirectAttributes.addFlashAttribute("successMessage", successMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to send quotation email: " + e.getMessage();
            if (wantsJson) {
                return ResponseEntity.status(500).body(java.util.Map.of(
                        "success", false,
                        "message", errorMsg
                ));
            }
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
        }

        return "redirect:/admin/quotations/view/" + id + (grade != null ? "?grade=" + grade.name() : "");
    }


    // =========================================================
    // WHATSAPP TRACKING
    // =========================================================

    public Object trackWhatsApp(Long id, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        return trackWhatsApp(id, null, request, redirectAttributes);
    }

    @GetMapping("/whatsapp/{id}")
    public Object trackWhatsApp(
            @PathVariable Long id,
            @RequestParam(required = false) PricingMode grade,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {

        String accept = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        boolean wantsJson = (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        try {

            Quotation quotation =
                    quotationService.getById(id);

            Quotation targetQuotation = quotation;
            if (grade != null) {
                Quotation child = quotation.getChildForGrade(grade);
                if (child != null) {
                    targetQuotation = child;
                }
            }

            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            WhatsAppResult result = whatsAppService.sendOrPrepareQuotationPdf(targetQuotation, baseUrl);

            if (wantsJson) {
                return ResponseEntity.ok(result);
            }

            if ("API".equalsIgnoreCase(result.getMethod())) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        result.getMessage()
                );
                return "redirect:/admin/quotations/view/" + id + (grade != null ? "?grade=" + grade.name() : "");
            }

            return "redirect:" + result.getWhatsappUrl();

        } catch (Exception e) {

            if (wantsJson) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "configured", whatsAppService.isApiProviderConfigured(),
                        "provider", whatsAppService.getApiProviderName(),
                        "message", e.getMessage()
                ));
            }

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "WhatsApp document delivery failed: "
                            + e.getMessage()
            );

            return "redirect:/admin/quotations/view/" + id;
        }
    }


    // =========================================================
    // DELETE QUOTATION
    // =========================================================

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QuotationController.class);

    @PostMapping("/delete/{id}")
    public String deleteQuotation(
            @PathVariable Long id,
            @RequestParam(required = false) String returnUrl,
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
            log.error("Failed to delete quotation ID {}: {}", id, e.getMessage(), e);

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to delete quotation: " + e.getMessage()
            );
        }

        if (returnUrl != null && !returnUrl.isBlank() && returnUrl.startsWith("/admin")) {
            return "redirect:" + returnUrl;
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
                withPricingDisplays(quotationService.getByStatus(status))
        );

        model.addAttribute(
                "allQuotationList",
                quotationService.getAllQuotations()
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
                withPricingDisplays(quotationService.getLast60DaysQuotations())
        );

        model.addAttribute(
                "allQuotationList",
                quotationService.getAllQuotations()
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
                withPricingDisplays(quotationService.getByCustomerId(customerId))
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
                withPricingDisplays(quotationService.getByEmployeeId(employeeId))
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
                withPricingDisplays(quotationService.getByBranchId(branchId))
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

    @GetMapping("/branch/{branchId}/status/{status}")
    public String branchQuotationsByStatus(
            @PathVariable Long branchId,
            @PathVariable QuotationStatus status
    ) {
        return "redirect:/admin/branches/" + branchId + "/quotations/status/" + status;
    }

    @PostMapping("/charges/{id}")
    public String updateCharges(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal accessoriesAmount,
            @RequestParam(required = false) BigDecimal travelCharge,
            @RequestParam(required = false) BigDecimal otherCharges,
            @RequestParam(required = false) String otherChargesDescription,
            @RequestParam(required = false) BigDecimal discountAmount,
            @RequestParam(required = false) BigDecimal gstPercentage,
            @RequestParam(required = false) BigDecimal offerPercentage,
            RedirectAttributes redirectAttributes) {

        try {

            quotationCalculationService.updateCharges(
                    id,
                    accessoriesAmount,
                    travelCharge,
                    otherCharges,
                    otherChargesDescription,
                    discountAmount,
                    gstPercentage,
                    offerPercentage
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