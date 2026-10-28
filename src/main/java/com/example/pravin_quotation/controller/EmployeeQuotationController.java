package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Customer;
import com.example.pravin_quotation.model.Division;
import com.example.pravin_quotation.model.Item;
import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.model.MaterialOption;
import com.example.pravin_quotation.model.Pricing;
import com.example.pravin_quotation.model.PricingMode;
import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationCommunication;
import com.example.pravin_quotation.model.QuotationItem;
import com.example.pravin_quotation.model.QuotationItemSize;
import com.example.pravin_quotation.model.QuotationRoom;
import com.example.pravin_quotation.model.QuotationStatus;
import com.example.pravin_quotation.model.QuotationStatusHistory;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.model.WorkCategory;
import com.example.pravin_quotation.model.MaterialSpecification;

import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.CustomerRepository;
import com.example.pravin_quotation.repository.DivisionRepository;
import com.example.pravin_quotation.repository.ItemRepository;
import com.example.pravin_quotation.repository.MaterialOptionRepository;
import com.example.pravin_quotation.repository.MaterialRepository;
import com.example.pravin_quotation.repository.MaterialSpecificationRepository;
import com.example.pravin_quotation.repository.PricingRepository;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.repository.WorkCategoryRepository;

import com.example.pravin_quotation.model.WhatsAppResult;
import com.example.pravin_quotation.service.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
    private final WorkCategoryRepository workCategoryRepository;
    private final ItemRepository itemRepository;
    private final MaterialRepository materialRepository;
    private final MaterialOptionRepository materialOptionRepository;
    private final MaterialSpecificationRepository materialSpecificationRepository;
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
    private final WhatsAppService whatsAppService;


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

            WorkCategoryRepository workCategoryRepository,

            ItemRepository itemRepository,

            MaterialRepository materialRepository,

            MaterialOptionRepository materialOptionRepository,

            MaterialSpecificationRepository materialSpecificationRepository,

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

            QuotationStatusHistoryService quotationStatusHistoryService,

            WhatsAppService whatsAppService
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

        this.workCategoryRepository =
                workCategoryRepository;

        this.itemRepository =
                itemRepository;

        this.materialRepository =
                materialRepository;

        this.materialOptionRepository =
                materialOptionRepository;

        this.materialSpecificationRepository =
                materialSpecificationRepository;

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

        this.whatsAppService =
                whatsAppService;
    }


    // ============================================================
    // EMPLOYEE QUOTATION LIST
    // ============================================================

    @GetMapping({"", "/status/{status}"})
    public String quotations(
            @PathVariable(required = false) String status,
            @RequestParam(name = "status", required = false) String paramStatus,
            @RequestParam(name = "period", required = false) String period,
            Authentication authentication,
            Model model
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        String rawStatus = (status != null && !status.trim().isEmpty()) ? status : paramStatus;
        QuotationStatus activeStatus = parseStatus(rawStatus);

        List<Quotation> quotations;
        String pageTitle = "My Quotations";
        String pageSubtitle = "View and manage your customer quotations.";
        String emptyTitle = "No quotations found";
        String emptyMsg = "No quotations found. Create your first quotation to get started.";

        if (activeStatus != null) {
            quotations = quotationRepository
                    .findByEmployeeIdAndStatusOrderByCreatedAtDesc(
                            employee.getId(),
                            activeStatus
                    );

            switch (activeStatus) {
                case PENDING -> {
                    pageTitle = "Pending Quotations";
                    pageSubtitle = "Quotations awaiting review, approval, or client response.";
                    emptyTitle = "No pending quotations";
                    emptyMsg = "You currently have no quotations with Pending status.";
                }
                case WORKING -> {
                    pageTitle = "Working Quotations";
                    pageSubtitle = "Quotations currently in progress or being worked on.";
                    emptyTitle = "No working quotations";
                    emptyMsg = "You currently have no quotations with Working status.";
                }
                case SENT -> {
                    pageTitle = "Sent Quotations";
                    pageSubtitle = "Quotations that have been successfully sent to customers.";
                    emptyTitle = "No sent quotations";
                    emptyMsg = "You currently have no quotations with Sent status.";
                }
            }
        } else if ("60".equals(period)) {
            LocalDate today = LocalDate.now();
            LocalDate sixtyDaysAgo = today.minusDays(59);
            quotations = quotationRepository
                    .findByEmployeeIdAndQuotationDateBetweenOrderByQuotationDateDesc(
                            employee.getId(),
                            sixtyDaysAgo,
                            today
                    );
            pageTitle = "60 Days Quotations";
            pageSubtitle = "Quotations created in the last 60 days.";
            emptyTitle = "No quotations in the last 60 days";
            emptyMsg = "You have not created any quotations in the last 60 days.";
        } else if ("365".equals(period) || "year".equalsIgnoreCase(period) || "1year".equalsIgnoreCase(period)) {
            LocalDate today = LocalDate.now();
            LocalDate oneYearAgo = today.minusYears(1);
            quotations = quotationRepository
                    .findByEmployeeIdAndQuotationDateBetweenOrderByQuotationDateDesc(
                            employee.getId(),
                            oneYearAgo,
                            today
                    );
            pageTitle = "Total One Year Quotations";
            pageSubtitle = "Quotations created during the last one year.";
            emptyTitle = "No quotations in the last one year";
            emptyMsg = "You have not created any quotations in the last one year.";
        } else {
            quotations = quotationRepository
                    .findByEmployeeIdOrderByCreatedAtDesc(
                            employee.getId()
                    );
        }

        long totalCount = quotationRepository.countByEmployeeId(employee.getId());
        long pendingCount = quotationRepository.countByEmployeeIdAndStatus(employee.getId(), QuotationStatus.PENDING);
        long workingCount = quotationRepository.countByEmployeeIdAndStatus(employee.getId(), QuotationStatus.WORKING);
        long sentCount = quotationRepository.countByEmployeeIdAndStatus(employee.getId(), QuotationStatus.SENT);

        model.addAttribute("employee", employee);
        model.addAttribute("quotations", quotations);
        model.addAttribute("selectedStatus", activeStatus != null ? activeStatus.name() : null);
        model.addAttribute("selectedPeriod", period);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageSubtitle", pageSubtitle);
        model.addAttribute("emptyMessageTitle", emptyTitle);
        model.addAttribute("emptyMessage", emptyMsg);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("workingCount", workingCount);
        model.addAttribute("sentCount", sentCount);

        return "employee/quotations";
    }

    private QuotationStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return null;
        }
        String clean = statusStr.trim().toUpperCase();
        try {
            return QuotationStatus.valueOf(clean);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }


    // ============================================================
    // UNIFIED QUOTATION BUILDER (CREATION)
    // ============================================================

    @GetMapping({"/new", "/builder"})
    public String builder(
            @RequestParam(required = false) Long quotationId,
            @RequestParam(required = false) Long branchId,
            Authentication authentication,
            Model model
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        if (quotationId != null) {
            getQuotationAndCheckOwnership(quotationId, employee);
        }

        Long effectiveBranchId = (employee.getBranch() != null) ? employee.getBranch().getId() : branchId;

        Quotation quotation = new Quotation();
        quotation.setQuotationNumber(previewQuotationNumber());
        quotation.setQuotationDate(LocalDate.now());
        quotation.setPricingMode(PricingMode.MIDDLE);
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
        List<Customer> branchCustomers = Collections.emptyList();
        if (employee.getBranch() != null) {
            branchCustomers = customerRepository.findByBranchIdAndActiveTrueOrderByNameAsc(employee.getBranch().getId());
        }
        model.addAttribute("customers", branchCustomers);
        model.addAttribute("workCategories", workCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc());
        model.addAttribute("pricingModes", PricingMode.values());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("editQuotationId", quotationId != null ? quotationId : "");
        model.addAttribute("selectedBranchId", effectiveBranchId != null ? effectiveBranchId : "");
        model.addAttribute("employeeBranchId", employee.getBranch() != null ? employee.getBranch().getId() : "");

        return "employee/quotation-builder";
    }

    public String builder(Authentication authentication, Model model) {
        return builder(null, null, authentication, model);
    }

    @GetMapping("/builder/customers")
    @ResponseBody
    public List<Map<String, Object>> builderCustomers(Authentication authentication) {
        User employee = getLoggedInEmployee(authentication);
        if (employee == null || employee.getBranch() == null) {
            return Collections.emptyList();
        }
        return customerRepository.findByBranchIdAndActiveTrueOrderByNameAsc(employee.getBranch().getId())
                .stream().map(c -> {
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
    public List<Map<String, Object>> builderDivisions(@RequestParam Long workCategoryId) {
        return divisionRepository.findByWorkCategoryIdAndActiveTrueOrderByDisplayOrderAsc(workCategoryId)
                .stream().map(d -> {
                    String emoji = d.getIcon() != null && !d.getIcon().isBlank()
                            ? d.getIcon().trim()
                            : WorkCategoryService.resolveWorkTypeEmoji(d.getName(), "📁");
                    return mapData("id", d.getId(),
                            "name", d.getName() != null ? d.getName().toUpperCase() : "",
                            "emoji", emoji,
                            "icon", emoji,
                            "description", d.getDescription());
                })
                .toList();
    }

    @GetMapping("/builder/items")
    @ResponseBody
    public List<Map<String, Object>> builderItems(@RequestParam Long divisionId) {
        return itemRepository.findByDivisionIdAndActiveTrueOrderByDisplayOrderAsc(divisionId)
                .stream().map(i -> mapData("id", i.getId(), "name", i.getName() != null ? i.getName().toUpperCase() : "", "description", i.getDescription()))
                .toList();
    }

    @GetMapping("/builder/materials")
    @ResponseBody
    public List<Map<String, Object>> builderMaterials(
            @RequestParam(required = false) Long branchId,
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
        final PricingMode targetGrade = grade;
        return list.stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("name", m.getName() != null ? m.getName().toUpperCase() : "");
            map.put("description", m.getDescription());
            map.put("specification", m.getSpecification() != null ? m.getSpecification() : (m.getDescription() != null ? m.getDescription() : ""));
            map.put("unit", m.getUnit() != null ? m.getUnit() : "Sq.ft");

            BigDecimal resolvedRate = null;
            if (branchId != null && targetGrade != null) {
                try {
                    resolvedRate = builderService.resolveRate(branchId, m.getId(), null, targetGrade);
                } catch (Exception ignored) {}
            }
            if (resolvedRate == null && branchId == null) {
                resolvedRate = m.getRate();
            }

            map.put("rate", resolvedRate);
            map.put("pricingGrade", m.getPricingGrade() != null ? m.getPricingGrade().name() : "");
            map.put("formula", m.getFormula());
            return map;
        }).toList();
    }

    public List<Map<String, Object>> builderMaterials(Long itemId) {
        return builderMaterials(null, null, itemId, (String) null);
    }

    public List<Map<String, Object>> builderMaterials(Long divisionId, Long itemId) {
        return builderMaterials(null, divisionId, itemId, (String) null);
    }

    public List<Map<String, Object>> builderMaterials(Long divisionId, Long itemId, String pricingGrade) {
        return builderMaterials(null, divisionId, itemId, pricingGrade);
    }

    public ResponseEntity<?> builderRate(Long branchId, Long materialOptionId, PricingMode pricingMode) {
        return builderRate(branchId, materialOptionId, null, pricingMode);
    }

    @GetMapping("/builder/material-options")
    @ResponseBody
    public List<Map<String, Object>> builderMaterialOptions(@RequestParam Long materialId) {
        return materialOptionRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(materialId)
                .stream().map(o -> mapData("id", o.getId(), "name", o.getName() != null ? o.getName().toUpperCase() : "", "description", o.getDescription()))
                .toList();
    }

    @GetMapping("/builder/material-specifications")
    @ResponseBody
    public List<Map<String, Object>> builderMaterialSpecifications(
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
        return specs.stream().map(s -> mapData(
                "id", s.getId(),
                "name", s.getName() != null ? s.getName().toUpperCase() : "",
                "description", s.getDescription()
        )).toList();
    }

    @GetMapping("/builder/rate")
    @ResponseBody
    public ResponseEntity<?> builderRate(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long materialOptionId,
            @RequestParam(required = false) Long materialId,
            @RequestParam PricingMode pricingMode) {
        try {
            if (branchId == null) {
                return ResponseEntity.badRequest().body(mapData(
                        "success", false,
                        "message", "Please select a branch first to retrieve material rates."
                ));
            }

            BigDecimal rate = builderService.resolveRate(branchId, materialId, materialOptionId, pricingMode);
            Material material = materialId != null ? materialRepository.findById(materialId).orElse(null) : null;
            Pricing pricing = materialOptionId != null ? pricingRepository.findByMaterialOptionIdAndPricingMode(materialOptionId, pricingMode).orElse(null) : null;
            MaterialOption option = materialOptionId != null ? materialOptionRepository.findById(materialOptionId).orElse(null) : null;

            String formula = (material != null && material.getFormula() != null && !material.getFormula().isBlank() && !"(L + W) × H".equals(material.getFormula()))
                    ? material.getFormula()
                    : ((pricing != null && pricing.getFormula() != null && !pricing.getFormula().isBlank())
                    ? pricing.getFormula()
                    : ((option != null && option.getMaterial() != null && option.getMaterial().getFormula() != null)
                    ? option.getMaterial().getFormula()
                    : "(L + W) × H"));

            String unit = (pricing != null && pricing.getUnit() != null)
                    ? pricing.getUnit()
                    : ((material != null && material.getUnit() != null) ? material.getUnit() : "Sq.ft");

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("rate", rate);
            resp.put("pricingMode", pricingMode.name());
            resp.put("formula", formula);
            resp.put("unit", unit);
            resp.put("measurementUnit", unit);
            resp.put("pricingUnit", "₹ / " + unit);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(mapData(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to resolve rate for this branch and grade."
            ));
        }
    }

    @PostMapping("/builder/save")
    public Object saveBuilderQuotation(
            @RequestBody AdminQuotationBuilderService.BuilderRequest request,
            HttpServletRequest httpRequest,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        boolean acceptsJson = httpRequest != null
                && httpRequest.getHeader("Accept") != null
                && httpRequest.getHeader("Accept").contains(MediaType.APPLICATION_JSON_VALUE);
        try {
            User employee = getLoggedInEmployee(authentication);
            if (employee.getBranch() == null) {
                throw new IllegalStateException("Employee is not assigned to a branch.");
            }
            if (request.quotationId() != null) {
                getQuotationAndCheckOwnership(request.quotationId(), employee);
            }

            Long effectiveBranchId = request.branchId();
            if (effectiveBranchId == null || effectiveBranchId == 0L) {
                effectiveBranchId = employee.getBranch().getId();
            }
            if (!effectiveBranchId.equals(employee.getBranch().getId())) {
                throw new AccessDeniedException("Unauthorized: Cannot create quotations for another branch.");
            }

            AdminQuotationBuilderService.BuilderRequest effectiveRequest = new AdminQuotationBuilderService.BuilderRequest(
                    request.quotationId(),
                    request.customer(),
                    effectiveBranchId,
                    request.quotationDate() != null ? request.quotationDate() : LocalDate.now(),
                    request.pricingMode() != null ? request.pricingMode() : PricingMode.MIDDLE,
                    request.customerRequirements(),
                    request.notes(),
                    request.rooms(),
                    request.accessoriesAmount(),
                    request.travelCharge(),
                    request.otherCharges(),
                    request.discountAmount(),
                    request.gstPercentage(),
                    request.offerPercentage()
            );

            Quotation quotation = builderService.save(effectiveRequest, authentication.getName());
            String redirectUrl = "/employee/quotations/view/" + quotation.getId();

            if (acceptsJson) {
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("redirectUrl", redirectUrl);
                resp.put("quotationId", quotation.getId());
                resp.put("quotationNumber", quotation.getQuotationNumber());
                return ResponseEntity.ok(resp);
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Quotation " + quotation.getQuotationNumber() + " created successfully.");
            return "redirect:" + redirectUrl;
        } catch (Exception e) {
            if (acceptsJson) {
                Map<String, Object> errResp = new LinkedHashMap<>();
                errResp.put("success", false);
                errResp.put("message", e.getMessage() != null ? e.getMessage() : "Unable to save quotation.");
                return ResponseEntity.badRequest().body(errResp);
            }
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/employee/quotations/builder";
        }
    }

    public String saveBuilderQuotation(
            AdminQuotationBuilderService.BuilderRequest request,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Object res = saveBuilderQuotation(request, null, authentication, redirectAttributes);
        return (String) res;
    }

    @GetMapping("/builder/load/{id}")
    @ResponseBody
    public ResponseEntity<?> builderLoadQuotation(@PathVariable Long id, Authentication authentication) {
        User employee = getLoggedInEmployee(authentication);
        Quotation q = getQuotationAndCheckOwnership(id, employee);
        if (q == null) {
            return ResponseEntity.notFound().build();
        }

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
        data.put("subtotal", q.getSubtotal() != null ? q.getSubtotal() : BigDecimal.ZERO);
        data.put("accessoriesAmount", q.getAccessoriesAmount() != null ? q.getAccessoriesAmount() : BigDecimal.ZERO);
        data.put("travelCharge", q.getTravelCharge() != null ? q.getTravelCharge() : BigDecimal.ZERO);
        data.put("otherCharges", q.getOtherCharges() != null ? q.getOtherCharges() : BigDecimal.ZERO);
        data.put("discountAmount", q.getDiscountAmount() != null ? q.getDiscountAmount() : BigDecimal.ZERO);
        data.put("taxableAmount", q.getTaxableAmount() != null ? q.getTaxableAmount() : BigDecimal.ZERO);
        data.put("gstPercentage", q.getGstPercentage() != null ? q.getGstPercentage() : BigDecimal.ZERO);
        data.put("gstAmount", q.getGstAmount() != null ? q.getGstAmount() : BigDecimal.ZERO);
        data.put("totalAmount", q.getTotalAmount() != null ? q.getTotalAmount() : q.getGrandTotal());
        data.put("offerPercentage", q.getOfferPercentage() != null ? q.getOfferPercentage() : BigDecimal.ZERO);
        data.put("offerAmount", q.getOfferAmount() != null ? q.getOfferAmount() : BigDecimal.ZERO);
        data.put("grandTotal", q.getGrandTotal() != null ? q.getGrandTotal() : BigDecimal.ZERO);

        List<QuotationRoom> rooms = quotationRoomService.getActiveByQuotationId(id);
        List<Map<String, Object>> roomList = new ArrayList<>();
        for (QuotationRoom room : rooms) {
            Map<String, Object> rMap = new LinkedHashMap<>();
            rMap.put("floor", room.getFloor());
            rMap.put("room", room.getRoom());
            rMap.put("workDescription", room.getWorkDescription());
            List<QuotationItem> items = quotationItemService.getActiveByQuotationRoomId(room.getId());
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

                List<QuotationItemSize> sizes = quotationItemSizeService.getActiveSizes(item.getId());
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
            Quotation quotation = quotationRepository.findById(quotationId).orElse(null);
            BigDecimal rate = quotationCalculationService.resolveItemRate(
                    quotationId,
                    materialOptionId
            );

            String formula = resolveFormula(quotation, materialOptionId, null);

            return ResponseEntity.ok(
                    mapData(
                            "success", true,
                            "rate", rate,
                            "formula", formula,
                            "pricingMode", (quotation != null && quotation.getPricingMode() != null)
                                    ? quotation.getPricingMode().name() : ""
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.ok(
                    mapData("success", false, "message", e.getMessage())
            );
        }
    }

    private String resolveFormula(Quotation quotation, Long materialOptionId, String requestedFormula) {
        if (requestedFormula != null && !requestedFormula.isBlank()) {
            return com.example.pravin_quotation.util.CalculationFormula.normalizeFormula(requestedFormula);
        }
        if (materialOptionId != null) {
            MaterialOption mo = materialOptionRepository.findById(materialOptionId).orElse(null);
            if (mo != null && mo.getMaterial() != null && mo.getMaterial().getFormula() != null && !mo.getMaterial().getFormula().isBlank() && !"(L + W) × H".equals(mo.getMaterial().getFormula())) {
                return com.example.pravin_quotation.util.CalculationFormula.normalizeFormula(mo.getMaterial().getFormula());
            }
        }
        if (quotation != null && materialOptionId != null && quotation.getPricingMode() != null) {
            Pricing pricing = pricingRepository
                    .findByMaterialOptionIdAndPricingMode(
                            materialOptionId,
                            quotation.getPricingMode()
                    ).orElse(null);
            if (pricing != null && pricing.getFormula() != null && !pricing.getFormula().isBlank()) {
                return com.example.pravin_quotation.util.CalculationFormula.normalizeFormula(pricing.getFormula());
            }
        }
        if (materialOptionId != null) {
            MaterialOption mo = materialOptionRepository.findById(materialOptionId).orElse(null);
            if (mo != null && mo.getFormula() != null && !mo.getFormula().isBlank()) {
                return com.example.pravin_quotation.util.CalculationFormula.normalizeFormula(mo.getFormula());
            }
            if (mo != null && mo.getMaterial() != null && mo.getMaterial().getFormula() != null && !mo.getMaterial().getFormula().isBlank()) {
                return com.example.pravin_quotation.util.CalculationFormula.normalizeFormula(mo.getMaterial().getFormula());
            }
        }
        return com.example.pravin_quotation.util.CalculationFormula.FORMULA_L_X_W;
    }


    // ============================================================
    @PostMapping("/save")
    public String saveQuotation(

            @RequestParam(required = false)
            Long customerId,

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

        Customer customer;
        if (customerId != null) {
            customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            if (customer.getBranch() == null || !customer.getBranch().getId().equals(employee.getBranch().getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Unauthorized branch customer access");
            }
        } else {
            String finalCustomerName = (customerName == null || customerName.isBlank()) ? "Draft Customer" : customerName.trim();
            String finalPhone = (phone == null || phone.isBlank()) ? "0000000000" : phone.trim();
            customer = customerService.createOrUpdateForQuotation(
                    finalCustomerName,
                    email,
                    finalPhone,
                    address,
                    employee.getBranch().getId()
            );
        }

        LocalDate date = (quotationDate == null || quotationDate.isBlank())
                ? LocalDate.now()
                : LocalDate.parse(quotationDate, DateTimeFormatter.ISO_LOCAL_DATE);
        PricingMode mode = (pricingMode == null) ? PricingMode.ECONOMY : pricingMode;

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

    @GetMapping({"/builder/{id}", "/edit/{id}"})
    public String quotationBuilder(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User employee = getLoggedInEmployee(authentication);
            getQuotationAndCheckOwnership(id, employee);
            return "redirect:/employee/quotations/builder?quotationId=" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/employee/quotations";
        }
    }


    // ============================================================
    // UPDATE CUSTOMER DETAILS FROM BUILDER
    // ============================================================

    @PostMapping("/builder/{id}/customer")
    public String updateBuilderCustomer(

            @PathVariable Long id,

            @RequestParam(required = false)
            Long customerId,

            @RequestParam(required = false)
            String customerName,

            @RequestParam(required = false)
            String phone,

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

        Customer customer;
        if (customerId != null) {
            customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            if (customer.getBranch() == null || !customer.getBranch().getId().equals(employee.getBranch().getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Unauthorized branch customer access");
            }
        } else {
            customer = customerService.createOrUpdateForQuotation(
                    customerName,
                    email,
                    phone,
                    address,
                    employee.getBranch().getId()
            );
        }

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
    // SAVE AND VIEW FULL QUOTATION FROM BUILDER
    // ============================================================

    @PostMapping("/builder/{id}/save")
    public String saveAndFinishBuilder(
            @PathVariable Long id,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String quotationDate,
            @RequestParam(required = false) String customerRequirements,
            @RequestParam(required = false) String notes,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User employee = getLoggedInEmployee(authentication);
        Quotation quotation = getQuotationAndCheckOwnership(id, employee);

        if (customerName != null && !customerName.isBlank() && phone != null && !phone.isBlank()) {
            Customer customer = customerService.createOrUpdateForQuotation(
                    customerName.trim(),
                    email,
                    phone.trim(),
                    address,
                    employee.getBranch().getId()
            );
            quotation.setCustomer(customer);
        }

        if (quotationDate != null && !quotationDate.isBlank()) {
            quotation.setQuotationDate(
                    LocalDate.parse(quotationDate, DateTimeFormatter.ISO_LOCAL_DATE)
            );
        }
        if (customerRequirements != null) {
            quotation.setCustomerRequirements(customerRequirements);
        }
        if (notes != null) {
            quotation.setNotes(notes);
        }

        quotationCalculationService.calculateQuotation(id);
        quotationRepository.save(quotation);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Quotation " + quotation.getQuotationNumber() + " saved successfully."
        );
        return "redirect:/employee/quotations/view/" + id;
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

            @RequestParam(required = false)
            String formula,

            @RequestParam(required = false, defaultValue = "in")
            String dimensionUnit,

            Authentication authentication
    ) {

        User employee =
                getLoggedInEmployee(authentication);

        Quotation quotation =
                getQuotationAndCheckOwnership(
                        id,
                        employee
                );

        String resolvedFormula =
                resolveFormula(quotation, materialOptionId, formula);

        // Pad width and height lists if needed
        int sizeCount = lengthValue != null ? lengthValue.size() : 0;
        if (widthValue == null) {
            widthValue = new ArrayList<>();
        }
        while (widthValue.size() < sizeCount) {
            widthValue.add(BigDecimal.ZERO);
        }
        if (heightValue == null) {
            heightValue = new ArrayList<>();
        }
        while (heightValue.size() < sizeCount) {
            heightValue.add(BigDecimal.ZERO);
        }

        validateSizeLists(
                resolvedFormula,
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

        BigDecimal firstSqft =
                quotationItemService.calculateSqft(
                        resolvedFormula,
                        firstLength,
                        firstWidth,
                        firstHeight,
                        dimensionUnit
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
                        resolvedFormula,
                        finalSpecification
                );

        QuotationItemSize firstSize =
                quotationItemSizeService.create(
                        quotationItem,
                        firstLength,
                        firstWidth,
                        firstHeight,
                        firstOfferPrice,
                        1,
                        dimensionUnit
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

            BigDecimal sqft =
                    quotationItemService.calculateSqft(
                            resolvedFormula,
                            length,
                            width,
                            height,
                            dimensionUnit
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
                            i + 1,
                            dimensionUnit
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
            getQuotationAndCheckOwnership(
                    id,
                    employee
            );

            rate = quotationCalculationService.resolveItemRate(id, materialOptionId, mode);
        } else {
            Long branchId = employee.getBranch() != null ? employee.getBranch().getId() : null;
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

        String formula = "(L + W) × H";
        if (pricing != null && pricing.getFormula() != null && !pricing.getFormula().isBlank()) {
            formula = pricing.getFormula();
        } else {
            MaterialOption mo = materialOptionRepository.findById(materialOptionId).orElse(null);
            if (mo != null && mo.getFormula() != null && !mo.getFormula().isBlank()) {
                formula = mo.getFormula();
            }
        }
        result.put("formula", formula);

        String measurementUnit = ("INCH".equalsIgnoreCase(unit) || "INCHES".equalsIgnoreCase(unit) || "SQIN".equalsIgnoreCase(unit)) ? "SQIN" : "SQFT";
        result.put("measurementUnit", measurementUnit);
        result.put("pricingUnit", "₹ / " + measurementUnit);

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

            @RequestParam(required = false)
            String formula,

            @RequestParam(required = false, defaultValue = "in")
            String dimensionUnit,

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

        String resolvedFormula =
                resolveFormula(quotation, materialOptionId, formula);

        BigDecimal safeWidth =
                widthValue != null ? widthValue : BigDecimal.ZERO;
        BigDecimal safeHeight =
                heightValue != null ? heightValue : BigDecimal.ZERO;

        validateDimensions(
                resolvedFormula,
                lengthValue,
                safeWidth,
                safeHeight
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
                        resolvedFormula,
                        lengthValue,
                        safeWidth,
                        safeHeight,
                        dimensionUnit
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
                        safeWidth,
                        safeHeight,
                        calculatedSqft,
                        resolvedRate,
                        amount,
                        finalOfferPrice,
                        resolvedFormula,
                        finalSpecification
                );

        /*
         * Create size row for the item.
         */

        QuotationItemSize quotationItemSize =
                quotationItemSizeService.create(
                        quotationItem,
                        lengthValue,
                        safeWidth,
                        safeHeight,
                        finalOfferPrice,
                        1,
                        dimensionUnit
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

            @RequestParam(required = false)
            String formula,

            @RequestParam(required = false, defaultValue = "in")
            String dimensionUnit,

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

        String resolvedFormula =
                resolveFormula(
                        quotation,
                        materialOptionId,
                        formula != null && !formula.isBlank()
                                ? formula
                                : quotationItem.getFormula()
                );

        BigDecimal safeWidth =
                widthValue != null ? widthValue : BigDecimal.ZERO;
        BigDecimal safeHeight =
                heightValue != null ? heightValue : BigDecimal.ZERO;

        validateDimensions(
                resolvedFormula,
                lengthValue,
                safeWidth,
                safeHeight
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
                        resolvedFormula,
                        lengthValue,
                        safeWidth,
                        safeHeight,
                        dimensionUnit
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
                safeWidth,
                safeHeight,
                calculatedSqft,
                resolvedRate,
                amount,
                finalOfferPrice,
                resolvedFormula,
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
    public Object trackWhatsApp(

            @PathVariable Long id,

            Authentication authentication,

            HttpServletRequest request,

            RedirectAttributes redirectAttributes
    ) {

        String accept = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        boolean wantsJson = (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith);

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

            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            WhatsAppResult result = whatsAppService.sendOrPrepareQuotationPdf(quotation, baseUrl);

            if (wantsJson) {
                return ResponseEntity.ok(result);
            }

            if ("API".equalsIgnoreCase(result.getMethod())) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        result.getMessage()
                );
                return "redirect:/employee/quotations/view/" + id;
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

            @RequestParam(required = false)
            BigDecimal offerPercentage,

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

        return "redirect:/employee/quotations/view/"
                + id;
    }


    // ============================================================
    // DELETE QUOTATION
    // ============================================================

    @PostMapping("/delete/{id}")
    public String deleteQuotation(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            User employee =
                    getLoggedInEmployee(
                            authentication
                    );

            /*
             * Verify that this quotation belongs
             * to the logged-in employee before deletion.
             */
            getQuotationAndCheckOwnership(
                    id,
                    employee
            );

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
                    "Unable to delete quotation: "
                            + e.getMessage()
            );
        }

        return "redirect:/employee/quotations";
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
            String formula,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height
    ) {
        com.example.pravin_quotation.util.CalculationFormula.validateDimensions(
                formula,
                length,
                width,
                height
        );
    }

    private void validateDimensions(
            BigDecimal length,
            BigDecimal width,
            BigDecimal height
    ) {
        validateDimensions(null, length, width, height);
    }


    // ============================================================
    // VALIDATE SIZE LISTS
    // ============================================================

    private void validateSizeLists(
            String formula,
            List<BigDecimal> lengthValue,
            List<BigDecimal> widthValue,
            List<BigDecimal> heightValue
    ) {

        if (lengthValue == null
                || lengthValue.isEmpty()) {

            throw new IllegalArgumentException(
                    "Please add at least one size."
            );
        }

        for (int i = 0; i < lengthValue.size(); i++) {
            BigDecimal l = lengthValue.get(i);
            BigDecimal w = (widthValue != null && i < widthValue.size()) ? widthValue.get(i) : null;
            BigDecimal h = (heightValue != null && i < heightValue.size()) ? heightValue.get(i) : null;
            validateDimensions(formula, l, w, h);
        }
    }

    private void validateSizeLists(
            List<BigDecimal> lengthValue,
            List<BigDecimal> widthValue,
            List<BigDecimal> heightValue
    ) {
        validateSizeLists(null, lengthValue, widthValue, heightValue);
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