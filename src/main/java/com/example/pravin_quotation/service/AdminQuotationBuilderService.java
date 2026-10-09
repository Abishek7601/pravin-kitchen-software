package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class AdminQuotationBuilderService {

    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final WorkCategoryRepository workCategoryRepository;
    private final DivisionRepository divisionRepository;
    private final ItemRepository itemRepository;
    private final MaterialRepository materialRepository;
    private final MaterialOptionRepository materialOptionRepository;
    private final PricingRepository pricingRepository;
    private final DistrictPricingRepository districtPricingRepository;
    private final QuotationRoomService quotationRoomService;
    private final QuotationItemService quotationItemService;
    private final QuotationItemSizeService quotationItemSizeService;
    private final QuotationCalculationService quotationCalculationService;

    public AdminQuotationBuilderService(
            QuotationRepository quotationRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            BranchRepository branchRepository,
            WorkCategoryRepository workCategoryRepository,
            DivisionRepository divisionRepository,
            ItemRepository itemRepository,
            MaterialRepository materialRepository,
            MaterialOptionRepository materialOptionRepository,
            PricingRepository pricingRepository,
            DistrictPricingRepository districtPricingRepository,
            QuotationRoomService quotationRoomService,
            QuotationItemService quotationItemService,
            QuotationItemSizeService quotationItemSizeService,
            QuotationCalculationService quotationCalculationService) {

        this.quotationRepository = quotationRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.workCategoryRepository = workCategoryRepository;
        this.divisionRepository = divisionRepository;
        this.itemRepository = itemRepository;
        this.materialRepository = materialRepository;
        this.materialOptionRepository = materialOptionRepository;
        this.pricingRepository = pricingRepository;
        this.districtPricingRepository = districtPricingRepository;
        this.quotationRoomService = quotationRoomService;
        this.quotationItemService = quotationItemService;
        this.quotationItemSizeService = quotationItemSizeService;
        this.quotationCalculationService = quotationCalculationService;
    }

    // =========================================================
    // RESOLVE RATE
    // =========================================================

    @Transactional(readOnly = true)
    public BigDecimal resolveRate(
            Long branchId,
            Long materialOptionId,
            PricingMode mode) {

        if (branchId == null
                || materialOptionId == null
                || mode == null) {

            throw new IllegalArgumentException(
                    "Branch, material option and pricing mode are required."
            );
        }

        Pricing pricing =
                pricingRepository
                        .findByMaterialOptionIdAndPricingMode(
                                materialOptionId,
                                mode
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No pricing is configured for this material option and pricing mode."
                                )
                        );

        if (!Boolean.TRUE.equals(pricing.getActive())) {
            throw new IllegalArgumentException(
                    "The selected pricing is inactive."
            );
        }

        DistrictPricing districtPricing =
                districtPricingRepository
                        .findByBranchIdAndPricingId(
                                branchId,
                                pricing.getId()
                        )
                        .orElse(null);

        if (districtPricing != null
                && Boolean.TRUE.equals(districtPricing.getActive())
                && districtPricing.getRate() != null
                && districtPricing.getRate().compareTo(BigDecimal.ZERO) > 0) {

            return money(districtPricing.getRate());
        }

        return money(pricing.getRate());
    }

    // =========================================================
    // SAVE COMPLETE QUOTATION
    // =========================================================

    @Transactional
    public Quotation save(
            BuilderRequest request,
            String loggedInEmail) {

        validateRequest(request);

        // -----------------------------------------------------
        // Logged-in admin
        // -----------------------------------------------------

        User admin =
                userRepository.findByEmail(loggedInEmail)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Logged-in admin was not found."
                                )
                        );

        if (!Boolean.TRUE.equals(admin.getActive())) {
            throw new IllegalArgumentException(
                    "Admin account is inactive."
            );
        }

        // -----------------------------------------------------
        // Branch
        // -----------------------------------------------------

        Branch branch =
                branchRepository.findById(
                        request.branchId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Branch not found."
                        )
                );

        // -----------------------------------------------------
        // Customer
        // -----------------------------------------------------

        Customer customer =
                findOrCreateCustomer(
                        request.customer(),
                        branch
                );

        // -----------------------------------------------------
        // Generate quotation number
        // -----------------------------------------------------

        String quotationNumber =
                nextQuotationNumber();

        // -----------------------------------------------------
        // Create quotation
        // -----------------------------------------------------

        Quotation quotation =
                new Quotation();

        quotation.setQuotationNumber(
                quotationNumber
        );

        quotation.setCustomer(
                customer
        );

        quotation.setEmployee(
                admin
        );

        quotation.setBranch(
                branch
        );

        quotation.setQuotationDate(
                request.quotationDate() == null
                        ? LocalDate.now()
                        : request.quotationDate()
        );

        quotation.setPricingMode(
                request.pricingMode()
        );

        quotation.setStatus(
                QuotationStatus.DRAFT
        );

        quotation.setCustomerRequirements(
                clean(request.customerRequirements())
        );

        quotation.setNotes(
                clean(request.notes())
        );

        Quotation savedQuotation =
                quotationRepository.save(
                        quotation
                );

        // =====================================================
        // ROOMS
        // =====================================================

        for (RoomRequest roomRequest :
                request.rooms()) {

            QuotationRoom room =
                    quotationRoomService.create(
                            savedQuotation,
                            required(
                                    roomRequest.floor(),
                                    "Floor"
                            ),
                            required(
                                    roomRequest.room(),
                                    "Room"
                            ),
                            roomRequest.workDescription()
                    );

            // =================================================
            // ITEMS
            // =================================================

            for (ItemRequest itemRequest :
                    roomRequest.items()) {

                WorkCategory category =
                        workCategoryRepository
                                .findById(
                                        itemRequest.workCategoryId()
                                )
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Work category not found."
                                        )
                                );

                Division division =
                        divisionRepository
                                .findById(
                                        itemRequest.divisionId()
                                )
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Division not found."
                                        )
                                );

                Item item =
                        itemRepository
                                .findById(
                                        itemRequest.itemId()
                                )
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Item not found."
                                        )
                                );

                Material material =
                        materialRepository
                                .findById(
                                        itemRequest.materialId()
                                )
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Material not found."
                                        )
                                );

                MaterialOption option =
                        materialOptionRepository
                                .findById(
                                        itemRequest.materialOptionId()
                                )
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Material option not found."
                                        )
                                );

                // -------------------------------------------------
                // Validate hierarchy
                // -------------------------------------------------

                if (!option.getMaterial()
                        .getId()
                        .equals(material.getId())) {

                    throw new IllegalArgumentException(
                            "Material option does not belong to the selected material."
                    );
                }

                if (!material.getItem()
                        .getId()
                        .equals(item.getId())) {

                    throw new IllegalArgumentException(
                            "Material does not belong to the selected item."
                    );
                }

                if (!item.getDivision()
                        .getId()
                        .equals(division.getId())) {

                    throw new IllegalArgumentException(
                            "Item does not belong to the selected division."
                    );
                }

                if (!division.getWorkCategory()
                        .getId()
                        .equals(category.getId())) {

                    throw new IllegalArgumentException(
                            "Division does not belong to the selected work category."
                    );
                }

                // -------------------------------------------------
                // Resolve rate ONCE for this item
                // -------------------------------------------------

                BigDecimal rate =
                        resolveRate(
                                request.branchId(),
                                option.getId(),
                                request.pricingMode()
                        );

                // =================================================
                // IMPORTANT
                //
                // ONE QuotationItem
                // MANY QuotationItemSize
                // =================================================

                BigDecimal totalSqft =
                        BigDecimal.ZERO;

                BigDecimal totalAmount =
                        BigDecimal.ZERO;

                BigDecimal totalOfferPrice =
                        BigDecimal.ZERO;

                BigDecimal firstLength = null;
                BigDecimal firstWidth = null;
                BigDecimal firstHeight = null;

                // -------------------------------------------------
                // First pass:
                // Calculate all sizes and totals
                // -------------------------------------------------

                for (int sizeIndex = 0;
                     sizeIndex < itemRequest.sizes().size();
                     sizeIndex++) {

                    SizeRequest size =
                            itemRequest.sizes()
                                    .get(sizeIndex);

                    BigDecimal length =
                            positive(
                                    size.length(),
                                    "Length"
                            );

                    BigDecimal width =
                            positive(
                                    size.width(),
                                    "Width"
                            );

                    BigDecimal height =
                            positive(
                                    size.height(),
                                    "Height"
                            );

                    // Save first size in parent item
                    // for backward compatibility.
                    if (sizeIndex == 0) {
                        firstLength = length;
                        firstWidth = width;
                        firstHeight = height;
                    }

                    BigDecimal sqft =
                            quotationItemSizeService
                                    .calculateSqft(
                                            length,
                                            width,
                                            height
                                    );

                    BigDecimal amount =
                            money(
                                    sqft.multiply(rate)
                            );

                    BigDecimal offerPrice;

                    if (size.offerPrice() == null) {
                        offerPrice = amount;
                    } else {
                        offerPrice =
                                nonNegative(
                                        size.offerPrice(),
                                        "Offer price"
                                );
                    }

                    totalSqft =
                            totalSqft.add(sqft);

                    totalAmount =
                            totalAmount.add(amount);

                    totalOfferPrice =
                            totalOfferPrice.add(
                                    offerPrice
                            );
                }

                totalSqft =
                        money(totalSqft);

                totalAmount =
                        money(totalAmount);

                totalOfferPrice =
                        money(totalOfferPrice);

                // =================================================
                // Create ONE parent QuotationItem
                // =================================================

                QuotationItem quotationItem =
                        quotationItemService.create(
                                room,
                                category,
                                division,
                                item,
                                material,
                                option,
                                itemRequest.itemDescription(),

                                firstLength,
                                firstWidth,
                                firstHeight,

                                totalSqft,
                                rate,
                                totalAmount,
                                totalOfferPrice,

                                "((L + W) × H) / 144",

                                itemRequest.specification()
                        );

                // =================================================
                // Create child QuotationItemSize rows
                // =================================================

                for (int sizeIndex = 0;
                     sizeIndex < itemRequest.sizes().size();
                     sizeIndex++) {

                    SizeRequest size =
                            itemRequest.sizes()
                                    .get(sizeIndex);

                    BigDecimal length =
                            positive(
                                    size.length(),
                                    "Length"
                            );

                    BigDecimal width =
                            positive(
                                    size.width(),
                                    "Width"
                            );

                    BigDecimal height =
                            positive(
                                    size.height(),
                                    "Height"
                            );

                    BigDecimal sqft =
                            quotationItemSizeService
                                    .calculateSqft(
                                            length,
                                            width,
                                            height
                                    );

                    BigDecimal amount =
                            money(
                                    sqft.multiply(rate)
                            );

                    BigDecimal offerPrice;

                    if (size.offerPrice() == null) {
                        offerPrice = amount;
                    } else {
                        offerPrice =
                                nonNegative(
                                        size.offerPrice(),
                                        "Offer price"
                                );
                    }

                    quotationItemSizeService.create(
                            quotationItem,
                            length,
                            width,
                            height,
                            offerPrice,
                            sizeIndex + 1
                    );
                }
            }
        }

        // -----------------------------------------------------
        // Final quotation calculation
        // -----------------------------------------------------

        return quotationCalculationService
                .calculateQuotation(
                        savedQuotation.getId()
                );
    }

    // =========================================================
    // CUSTOMER
    // =========================================================

    private Customer findOrCreateCustomer(
            CustomerRequest request,
            Branch branch) {

        String phone =
                required(
                        request.phone(),
                        "Phone number"
                );

        Customer customer =
                customerRepository
                        .findByPhone(phone)
                        .orElse(null);

        if (customer == null) {
            customer = new Customer();
            customer.setActive(true);
        }

        customer.setName(
                required(
                        request.name(),
                        "Customer name"
                )
        );

        customer.setPhone(phone);

        customer.setEmail(
                clean(request.email())
        );

        customer.setAddress(
                clean(request.address())
        );

        customer.setBranch(branch);

        return customerRepository.save(
                customer
        );
    }

    // =========================================================
    // QUOTATION NUMBER
    // =========================================================

    private String nextQuotationNumber() {

        long next =
                quotationRepository.count() + 1;

        String number;

        do {

            number =
                    String.format(
                            "QT-%05d",
                            next++
                    );

        } while (
                quotationRepository
                        .existsByQuotationNumber(number)
        );

        return number;
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    private void validateRequest(
            BuilderRequest request) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Quotation data is required."
            );
        }

        if (request.branchId() == null) {

            throw new IllegalArgumentException(
                    "Branch is required."
            );
        }

        if (request.pricingMode() == null) {

            throw new IllegalArgumentException(
                    "Pricing mode is required."
            );
        }

        if (request.customer() == null) {

            throw new IllegalArgumentException(
                    "Customer details are required."
            );
        }

        if (request.rooms() == null
                || request.rooms().isEmpty()) {

            throw new IllegalArgumentException(
                    "Add at least one room."
            );
        }

        for (RoomRequest room :
                request.rooms()) {

            if (room.items() == null
                    || room.items().isEmpty()) {

                throw new IllegalArgumentException(
                        "Each room must contain at least one item."
                );
            }

            for (ItemRequest item :
                    room.items()) {

                if (item.sizes() == null
                        || item.sizes().isEmpty()) {

                    throw new IllegalArgumentException(
                            "Each item must contain at least one size."
                    );
                }
            }
        }
    }

    // =========================================================
    // POSITIVE VALUE
    // =========================================================

    private BigDecimal positive(
            BigDecimal value,
            String name) {

        if (value == null
                || value.compareTo(
                BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    name + " must be greater than zero."
            );
        }

        return value;
    }

    // =========================================================
    // NON NEGATIVE
    // =========================================================

    private BigDecimal nonNegative(
            BigDecimal value,
            String name) {

        if (value != null
                && value.compareTo(
                BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    name + " cannot be negative."
            );
        }

        return value == null
                ? BigDecimal.ZERO
                : money(value);
    }

    // =========================================================
    // MONEY
    // =========================================================

    private BigDecimal money(
            BigDecimal value) {

        return (
                value == null
                        ? BigDecimal.ZERO
                        : value
        ).setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    // =========================================================
    // REQUIRED STRING
    // =========================================================

    private String required(
            String value,
            String name) {

        String cleaned =
                clean(value);

        if (cleaned == null) {

            throw new IllegalArgumentException(
                    name + " is required."
            );
        }

        return cleaned;
    }

    // =========================================================
    // CLEAN STRING
    // =========================================================

    private String clean(
            String value) {

        if (value == null) {
            return null;
        }

        String cleaned =
                value.trim();

        return cleaned.isEmpty()
                ? null
                : cleaned;
    }

    // =========================================================
    // REQUEST RECORDS
    // =========================================================

    public record BuilderRequest(
            CustomerRequest customer,
            Long branchId,
            LocalDate quotationDate,
            PricingMode pricingMode,
            String customerRequirements,
            String notes,
            List<RoomRequest> rooms) {
    }

    public record CustomerRequest(
            String name,
            String phone,
            String email,
            String address) {
    }

    public record RoomRequest(
            String floor,
            String room,
            String workDescription,
            List<ItemRequest> items) {
    }

    public record ItemRequest(
            Long workCategoryId,
            Long divisionId,
            Long itemId,
            Long materialId,
            Long materialOptionId,
            String itemDescription,
            String specification,
            List<SizeRequest> sizes) {
    }

    public record SizeRequest(
            BigDecimal length,
            BigDecimal width,
            BigDecimal height,
            BigDecimal offerPrice) {
    }
}