package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuotationPdfService {

    private static final Logger log = LoggerFactory.getLogger(QuotationPdfService.class);

    private final QuotationRepository quotationRepository;
    private final QuotationRoomRepository quotationRoomRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final NumberToWordsService numberToWordsService;
    private final BranchRepository branchRepository;
    private final PricingRepository pricingRepository;
    private final QuotationCalculationService quotationCalculationService;

    /*
     * Design Colors matching the Reference Document (1.pdf)
     */
    private static final Color COLOR_NAVY = new Color(2, 69, 65);          // #024541 Dark Teal brand primary
    private static final Color COLOR_HEADER_BG = new Color(2, 69, 65);      // #024541 Dark Teal table header background
    private static final Color COLOR_GOLD = new Color(168, 123, 46);        // #A87B2E Warm Gold accent lines & section borders
    private static final Color COLOR_HEADER_TEXT = new Color(245, 207, 83); // #F5CF53 Yellow/Gold header font
    private static final Color COLOR_LIGHT_BG = new Color(248, 250, 250);   // #F8FAFA Summary row fill & terms box
    private static final Color COLOR_ROW_ALT = new Color(245, 247, 247);    // #F5F7F7 Alternate table row fill
    private static final Color COLOR_BORDER = new Color(210, 215, 215);     // #D2D7D7 Thin table & card borders
    private static final Color COLOR_TEXT_DARK = new Color(30, 30, 30);     // #1E1E1E Main text
    private static final Color COLOR_TEXT_MUTED = new Color(90, 90, 90);    // #5A5A5A Subtitles & specifications

    // Regex pattern to format specifications in Title Case for PDF:
    // Matches digits followed by unit (e.g. 16MM -> 16mm) or words starting with letter (first char upper, rest lower)
    private static final Pattern SPEC_TITLE_CASE_PATTERN = Pattern.compile("(\\d+)(\\p{L}+)|(\\p{L})(\\p{L}*)");

    /*
     * Fonts for Rupee symbol and typography
     */
    private Font rupeeFont;
    private Font rupeeBoldFont;
    private Font rupeeGoldBoldFont;
    private boolean hasUnicodeRupee = false;

    @org.springframework.beans.factory.annotation.Autowired
    public QuotationPdfService(
            QuotationRepository quotationRepository,
            QuotationRoomRepository quotationRoomRepository,
            QuotationItemRepository quotationItemRepository,
            NumberToWordsService numberToWordsService,
            BranchRepository branchRepository,
            PricingRepository pricingRepository,
            QuotationCalculationService quotationCalculationService
    ) {
        this.quotationRepository = quotationRepository;
        this.quotationRoomRepository = quotationRoomRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.numberToWordsService = numberToWordsService;
        this.branchRepository = branchRepository;
        this.pricingRepository = pricingRepository;
        this.quotationCalculationService = quotationCalculationService;

        try {
            BaseFont rupeeBaseFont = BaseFont.createFont(
                    "C:/Windows/Fonts/arial.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
            );
            rupeeFont = new Font(rupeeBaseFont, 7.5f, Font.NORMAL, COLOR_TEXT_DARK);
            rupeeBoldFont = new Font(rupeeBaseFont, 8.0f, Font.BOLD, COLOR_TEXT_DARK);
            rupeeGoldBoldFont = new Font(rupeeBaseFont, 8.2f, Font.BOLD, COLOR_HEADER_TEXT);
            hasUnicodeRupee = true;
        } catch (Exception e) {
            rupeeFont = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, COLOR_TEXT_DARK);
            rupeeBoldFont = new Font(Font.HELVETICA, 8.0f, Font.BOLD, COLOR_TEXT_DARK);
            rupeeGoldBoldFont = new Font(Font.HELVETICA, 8.2f, Font.BOLD, COLOR_HEADER_TEXT);
            hasUnicodeRupee = false;
        }
    }

    public QuotationPdfService(
            QuotationRepository quotationRepository,
            QuotationRoomRepository quotationRoomRepository,
            QuotationItemRepository quotationItemRepository,
            NumberToWordsService numberToWordsService,
            BranchRepository branchRepository,
            PricingRepository pricingRepository
    ) {
        this(quotationRepository, quotationRoomRepository, quotationItemRepository,
             numberToWordsService, branchRepository, pricingRepository, null);
    }

    // ============================================================
    // GENERATE 7-PAGE REFERENCE-BASED QUOTATION PDF
    // ============================================================

    public byte[] generateQuotationPdf(Long quotationId) {
        try {
            Quotation quotation;
            if (quotationCalculationService != null) {
                quotation = quotationCalculationService.calculateQuotation(quotationId);
            } else {
                quotation = quotationRepository.findById(quotationId)
                        .orElseThrow(() -> new RuntimeException("Quotation not found with ID: " + quotationId));
            }

            // Fetch branch dynamically from Branch Management data
            Branch branch = quotation.getBranch();
            if (branch != null && branch.getId() != null) {
                branch = branchRepository.findById(branch.getId()).orElse(branch);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 25, 25, 20, 20);
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();

            // ----------------------------------------------------
            // PAGE 1: COVER PAGE
            // ----------------------------------------------------
            renderTemplatePage(
                    writer,
                    document,
                    "/pdf-templates/page_1.jpg",
                    new String[]{
                            "PRAVIN KITCHENS & INTERIORS",
                            "20 Years of Quality Interiors",
                            "PREMIUM INTERIOR ESTIMATE & GUIDELINES",
                            "Turnkey Modular Interiors & Custom Carpentry",
                            "100% Factory Made",
                            "10 Years Warranty",
                            "Transparent Pricing",
                            "Termite & Borer Proof",
                            "On-Time Delivery",
                            "www.pravinkitchen.com",
                            "Email: pravinkitchens@gmail.com",
                            "Mobile: +91 9787769970"
                    },
                    1
            );
            document.newPage();

            // ----------------------------------------------------
            // PAGE 2: DYNAMIC MARKETING QUOTATION
            // ----------------------------------------------------
            renderDynamicMarketingQuotationPage(document, writer, quotation, branch);
            document.newPage();

            // ----------------------------------------------------
            // PAGE 3: PAYMENT TERMS & WHY CHOOSE US
            // ----------------------------------------------------
            renderTemplatePage(
                    writer,
                    document,
                    "/pdf-templates/page_3.jpg",
                    new String[]{
                            "Payment Terms",
                            "10% Token: Commencement of 3D design on receipt of token",
                            "50% Advance: Drawing will be sent for production on receipt of advance",
                            "35% Part Payment: After the completion of frame work",
                            "5% Final Payment: The last day of work completion in site",
                            "Why Choose Us",
                            "Reason and Your Benefits",
                            "Factory Finish",
                            "10 Years Warranty",
                            "Pravin Kitchens & Interiors"
                    },
                    3
            );
            document.newPage();

            // ----------------------------------------------------
            // PAGE 4: ESTIMATE INCLUDES/EXCLUDES, QUALITY POLICY & WARRANTY
            // ----------------------------------------------------
            renderTemplatePage(
                    writer,
                    document,
                    "/pdf-templates/page_4.jpg",
                    new String[]{
                            "Estimate Includes",
                            "Estimate Excludes",
                            "Item Which Can Be Included Only at the Time of Designing",
                            "Quality Policy",
                            "Warranty",
                            "Pravin Kitchens & Interiors"
                    },
                    4
            );
            document.newPage();

            // ----------------------------------------------------
            // PAGE 5: CARE INSTRUCTIONS
            // ----------------------------------------------------
            renderTemplatePage(
                    writer,
                    document,
                    "/pdf-templates/page_5.jpg",
                    new String[]{
                            "Care Instructions",
                            "Cleaning of laminated plywood, BWP and hardwood finish board cabinets",
                            "Cleaning of accessories and appliances",
                            "Things that may damage metal parts",
                            "Water leakage",
                            "Homes locked for several weeks",
                            "Note"
                    },
                    5
            );
            document.newPage();

            // ----------------------------------------------------
            // PAGE 6: UNEXPECTED DELAYS & HANDOVER
            // ----------------------------------------------------
            renderTemplatePage(
                    writer,
                    document,
                    "/pdf-templates/page_6.jpg",
                    new String[]{
                            "Unexpected Delays",
                            "Final Cleaning",
                            "Handover & Final Inspection",
                            "Handover of the site",
                            "Final inspection",
                            "Contents"
                    },
                    6
            );
            document.newPage();

            // ----------------------------------------------------
            // PAGE 7: INSTRUCTIONS & GUIDELINES
            // ----------------------------------------------------
            renderTemplatePage(
                    writer,
                    document,
                    "/pdf-templates/page_7.jpg",
                    new String[]{
                            "Instructions & Guidelines",
                            "3D Designs",
                            "Copyright",
                            "Site Readiness",
                            "Additional Works / Requirements",
                            "Electrical / Plumbing and Other Requirements",
                            "Pravin Kitchens & Interiors"
                    },
                    7
            );

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // PAGE 2 DYNAMIC MARKETING QUOTATION IMPLEMENTATION
    // ============================================================

    private void renderDynamicMarketingQuotationPage(
            Document document,
            PdfWriter writer,
            Quotation quotation,
            Branch branch
    ) throws Exception {

        // ========================================================
        // 1. TOP HEADER TABLE (BRAND & ADDRESS ON LEFT, QUOTE INFO ON RIGHT)
        // ========================================================
        PdfPTable topHeaderTable = new PdfPTable(2);
        topHeaderTable.setWidthPercentage(100);
        topHeaderTable.setWidths(new float[]{53f, 47f});
        topHeaderTable.setSpacingAfter(4f);

        // --- LEFT COLUMN: LOGO + BRAND NAME + ADDRESS ---
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0);

        PdfPTable brandTable = new PdfPTable(2);
        brandTable.setWidthPercentage(100);
        brandTable.setWidths(new float[]{22f, 78f});

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(0);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        try {
            InputStream logoStream = getClass().getResourceAsStream("/static/images/company-logo.png");
            if (logoStream != null) {
                byte[] logoBytes = logoStream.readAllBytes();
                Image logoImg = Image.getInstance(logoBytes);
                logoImg.scaleToFit(46, 46);
                logoCell.addElement(logoImg);
            }
        } catch (Exception ignored) {}

        PdfPCell nameCell = new PdfPCell();
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setPaddingLeft(6f);
        nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph companyName = new Paragraph("PRAVIN KITCHENS & INTERIORS", new Font(Font.HELVETICA, 12f, Font.BOLD, COLOR_NAVY));
        nameCell.addElement(companyName);

        brandTable.addCell(logoCell);
        brandTable.addCell(nameCell);
        leftCell.addElement(brandTable);

        Paragraph addressPar = new Paragraph();
        addressPar.setSpacingBefore(3f);
        addressPar.setFont(new Font(Font.HELVETICA, 7.5f, Font.NORMAL, COLOR_TEXT_MUTED));

        String branchAddress = (branch != null && branch.getBranchAddress() != null && !branch.getBranchAddress().isBlank())
                ? branch.getBranchAddress().trim()
                : null;

        if (branchAddress != null) {
            addressPar.add(branchAddress + "\n");
        } else if (branch != null && branch.getName() != null && !branch.getName().isBlank()) {
            addressPar.add("Branch: " + branch.getName().trim() + "\n");
        } else {
            addressPar.add("Azhagiamandapam, Kanyakumari Dist - 629167\n");
        }

        if (branch != null && branch.getWhatsapp() != null && !branch.getWhatsapp().isBlank()) {
            addressPar.add("Mobile: " + branch.getWhatsapp().trim() + "\n");
        } else {
            addressPar.add("Mobile: +91 9787769970\n");
        }

        if (branch != null && branch.getEmail() != null && !branch.getEmail().isBlank()) {
            addressPar.add("Email: " + branch.getEmail().trim() + " | Web: www.pravinkitchen.com");
        } else {
            addressPar.add("Email: pravinkitchens@gmail.com | Web: www.pravinkitchen.com");
        }

        leftCell.addElement(addressPar);
        topHeaderTable.addCell(leftCell);

        // --- RIGHT COLUMN: TITLE & QUOTATION METADATA ---
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(0);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        String titleText = "MARKETING QUOTATION";
        Paragraph titlePar = new Paragraph(titleText, new Font(Font.HELVETICA, 13f, Font.BOLD, COLOR_NAVY));
        titlePar.setAlignment(Element.ALIGN_RIGHT);
        titlePar.setSpacingAfter(4f);
        rightCell.addElement(titlePar);

        LineSeparator titleLine = new LineSeparator(1.2f, 100, COLOR_GOLD, Element.ALIGN_RIGHT, -1);
        rightCell.addElement(titleLine);

        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{38f, 62f});
        metaTable.setSpacingBefore(4f);

        addMetaRow(metaTable, "Quotation No:", safe(quotation.getQuotationNumber()));

        String qType = "-";
        if (quotation.getPricingMode() != null) {
            switch (quotation.getPricingMode()) {
                case PREMIUM: qType = "Premium"; break;
                case MIDDLE: qType = "Middle / Standard"; break;
                case ECONOMY: qType = "Economy"; break;
                default: qType = quotation.getPricingMode().name();
            }
        }
        addMetaRow(metaTable, "Quotation Type:", qType);

        String dateStr = quotation.getQuotationDate() != null
                ? quotation.getQuotationDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                : "-";
        addMetaRow(metaTable, "Quotation Date:", dateStr);

        LocalDate validUntil = quotation.getValidUntil();
        if (validUntil == null && quotation.getQuotationDate() != null) {
            validUntil = quotation.getQuotationDate().plusDays(365);
        }
        String validUntilStr = validUntil != null
                ? validUntil.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                : "-";
        addMetaRow(metaTable, "Valid Until:", validUntilStr);

        String custName = quotation.getCustomer() != null ? safe(quotation.getCustomer().getName()) : "—";
        addMetaRow(metaTable, "Customer Name:", custName.isBlank() ? "—" : custName);

        String custAddr = "—";
        if (quotation.getCustomer() != null) {
            if (quotation.getCustomer().getAddress() != null && !quotation.getCustomer().getAddress().isBlank()) {
                custAddr = quotation.getCustomer().getAddress().trim();
            }
        }
        addMetaRow(metaTable, "Customer Address:", custAddr);

        rightCell.addElement(metaTable);
        topHeaderTable.addCell(rightCell);

        document.add(topHeaderTable);

        // Full-width Accent Line
        LineSeparator headerDivider = new LineSeparator(1.2f, 100, COLOR_GOLD, Element.ALIGN_CENTER, -2);
        document.add(headerDivider);

        // ========================================================
        // 2. SECTION TITLE: QUOTE SUMMARY
        // ========================================================
        Paragraph summaryTitle = new Paragraph("Quote Summary", new Font(Font.HELVETICA, 10f, Font.BOLD, COLOR_NAVY));
        summaryTitle.setSpacingBefore(3f);
        summaryTitle.setSpacingAfter(4f);
        document.add(summaryTitle);

        // ========================================================
        // 3. QUOTE SUMMARY TABLE (7 or 9 COLUMNS BASED ON TOGGLE)
        // ========================================================
        boolean showSqft = quotation.getShowSqftSize() == null || quotation.getShowSqftSize();
        int numColumns = showSqft ? 9 : 7;
        PdfPTable itemTable = new PdfPTable(numColumns);
        itemTable.setWidthPercentage(100);
        itemTable.setHeaderRows(1);
        if (showSqft) {
            itemTable.setWidths(new float[]{3.5f, 16.5f, 13.0f, 9.5f, 24.5f, 11.0f, 7.0f, 5.0f, 10.0f});
        } else {
            itemTable.setWidths(new float[]{4.0f, 20.0f, 15.0f, 10.0f, 33.0f, 6.0f, 12.0f});
        }
        itemTable.setSpacingAfter(5f);

        // --- TABLE HEADERS ---
        addHeaderCell(itemTable, "#", Element.ALIGN_CENTER);
        addHeaderCell(itemTable, "WORK CATEGORY", Element.ALIGN_LEFT);
        addHeaderCell(itemTable, "ROOM", Element.ALIGN_LEFT);
        addHeaderCell(itemTable, "FLOOR", Element.ALIGN_CENTER);
        addHeaderCell(itemTable, "MATERIAL", Element.ALIGN_LEFT);
        if (showSqft) {
            addHeaderCell(itemTable, "SIZE", Element.ALIGN_CENTER);
            addHeaderCell(itemTable, "SQ.FT", Element.ALIGN_RIGHT);
        }
        addHeaderCell(itemTable, "UNIT", Element.ALIGN_CENTER);
        addHeaderCell(itemTable, "TOTAL AMOUNT", Element.ALIGN_RIGHT);

        // --- QUOTATION ROOMS & ITEMS ---
        List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationId(quotation.getId());
        int serialNumber = 1;
        int activeItemCount = 0;
        boolean isAltRow = false;

        BigDecimal itemsTotalAmount = BigDecimal.ZERO;

        for (QuotationRoom room : rooms) {
            if (!Boolean.TRUE.equals(room.getActive())) {
                continue;
            }

            List<QuotationItem> items = quotationItemRepository.findByQuotationRoomId(room.getId());

            for (QuotationItem item : items) {
                if (!Boolean.TRUE.equals(item.getActive())) {
                    continue;
                }
                activeItemCount++;

                // Work Category
                String workCat = item.getWorkCategory() != null ? safe(item.getWorkCategory().getName()).toUpperCase() : "-";

                // Room
                String roomName = safe(room.getRoom()).toUpperCase();

                // Floor
                String floorName = safe(room.getFloor()).toUpperCase();
                if (floorName.isBlank()) {
                    floorName = "-";
                }

                // Material & Option (deduplicate so material name is only displayed once)
                String rawMatName = item.getMaterial() != null ? safe(item.getMaterial().getName()).trim() : "";
                String optionName = item.getMaterialOption() != null ? safe(item.getMaterialOption().getName()).trim() : "";

                String materialText = rawMatName;
                if (!optionName.isBlank() && !optionName.equalsIgnoreCase(rawMatName)) {
                    materialText = !materialText.isBlank() ? materialText + " - " + optionName : optionName;
                }
                if (materialText.isBlank()) {
                    materialText = "-";
                }

                // Specifications (display saved specification without duplicating material name)
                String specification = safe(item.getSpecification()).trim();
                if (specification.isBlank() && item.getMaterialSpecification() != null) {
                    specification = safe(item.getMaterialSpecification().getName()).trim();
                }
                if (!rawMatName.isBlank() && specification.toUpperCase().startsWith(rawMatName.toUpperCase())) {
                    specification = specification.substring(rawMatName.length()).trim();
                    if (specification.startsWith("-") || specification.startsWith(":")) {
                        specification = specification.substring(1).trim();
                    }
                }

                // Sq.Ft
                BigDecimal sqft = item.getCalculatedSqft() != null ? item.getCalculatedSqft() : BigDecimal.ZERO;

                // Size & Sq.Ft
                boolean isSqft = item.getMeasurementType() == null || item.getMeasurementType().isSqft();
                String sizeStr;
                String sqftStr;
                if (isSqft) {
                    sizeStr = buildSize(item.getLengthValue(), item.getWidthValue(), item.getHeightValue());
                    sqftStr = format(sqft);
                } else {
                    BigDecimal qty = (item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                            ? item.getQuantity()
                            : BigDecimal.ONE;
                    sizeStr = "QTY: " + qty.stripTrailingZeros().toPlainString();
                    sqftStr = "-";
                }

                // Unit
                String unitStr = resolveUnit(item, quotation.getPricingMode());

                // Amounts
                BigDecimal rate = item.getRate() != null ? item.getRate() : BigDecimal.ZERO;
                BigDecimal amount = item.getAmount() != null
                        ? item.getAmount()
                        : (isSqft ? rate.multiply(sqft) : rate.multiply(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE));
                BigDecimal offerPrice = item.getOfferPrice();
                if (offerPrice == null || offerPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    offerPrice = amount;
                }

                // Sum items' Total Amount before applying Offer Percentage
                itemsTotalAmount = itemsTotalAmount.add(amount);

                // Image URL (check item first, then fallback to material)
                String itemImageUrl = item.getImageUrl();
                if ((itemImageUrl == null || itemImageUrl.isBlank()) && item.getMaterial() != null) {
                    itemImageUrl = item.getMaterial().getImageUrl();
                }

                // Add Row Cells (item-level offer price removed)
                addBodyCell(itemTable, String.valueOf(serialNumber++), Element.ALIGN_CENTER, isAltRow);
                addBodyCell(itemTable, workCat, Element.ALIGN_LEFT, isAltRow);
                addBodyCell(itemTable, roomName, Element.ALIGN_LEFT, isAltRow);
                addBodyCell(itemTable, floorName, Element.ALIGN_CENTER, isAltRow);
                addMaterialCell(itemTable, materialText.toUpperCase(), formatSpecificationForPdf(specification), itemImageUrl, isAltRow);
                if (showSqft) {
                    addBodyCell(itemTable, sizeStr, Element.ALIGN_CENTER, isAltRow);
                    addBodyCell(itemTable, sqftStr, Element.ALIGN_RIGHT, isAltRow);
                }
                addBodyCell(itemTable, unitStr, Element.ALIGN_CENTER, isAltRow);
                addRupeeCell(itemTable, rupees(amount), false, false, isAltRow);

                isAltRow = !isAltRow;
            }
        }

        // Add filler rows if fewer items to preserve layout structure matching reference
        int minRows = 3;
        while (activeItemCount < minRows) {
            addFillerRow(itemTable, isAltRow);
            isAltRow = !isAltRow;
            activeItemCount++;
        }

        // --- PDF FINANCIAL VALUES (SINGLE SOURCE OF TRUTH) ---
        BigDecimal subtotal = quotation.getSubtotal() != null && quotation.getSubtotal().compareTo(BigDecimal.ZERO) > 0
                ? quotation.getSubtotal()
                : itemsTotalAmount;

        BigDecimal accessories = zeroIfNull(quotation.getAccessoriesAmount());
        BigDecimal travel = zeroIfNull(quotation.getTravelCharge());
        BigDecimal otherCharges = zeroIfNull(quotation.getOtherCharges());
        BigDecimal discount = zeroIfNull(quotation.getDiscountAmount());
        BigDecimal gstAmount = zeroIfNull(quotation.getGstAmount());
        BigDecimal totalAmount = quotation.getTotalAmount();
        BigDecimal offerAmount = zeroIfNull(quotation.getOfferAmount());
        BigDecimal grandTotal = quotation.getGrandTotal();

        // Fallback calculation using the exact business rules if fields were not pre-calculated
        if (grandTotal == null) {
            BigDecimal amountBeforeDiscount = subtotal.add(accessories).add(travel).add(otherCharges);
            BigDecimal amountAfterDiscount = amountBeforeDiscount.subtract(discount);
            if (amountAfterDiscount.compareTo(BigDecimal.ZERO) < 0) amountAfterDiscount = BigDecimal.ZERO;

            BigDecimal offerPct = zeroIfNull(quotation.getOfferPercentage());
            if (offerPct.compareTo(BigDecimal.ZERO) < 0) offerPct = BigDecimal.ZERO;
            if (offerPct.compareTo(new BigDecimal("100")) > 0) offerPct = new BigDecimal("100");
            offerAmount = amountAfterDiscount.multiply(offerPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            BigDecimal amountAfterOffer = amountAfterDiscount.subtract(offerAmount);
            if (amountAfterOffer.compareTo(BigDecimal.ZERO) < 0) amountAfterOffer = BigDecimal.ZERO;

            BigDecimal gstPct = zeroIfNull(quotation.getGstPercentage());
            if (gstAmount.compareTo(BigDecimal.ZERO) == 0 && gstPct.compareTo(BigDecimal.ZERO) > 0) {
                gstAmount = amountAfterOffer.multiply(gstPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }
            totalAmount = amountAfterDiscount;
            grandTotal = amountAfterOffer.add(gstAmount);
        } else if (totalAmount == null) {
            totalAmount = grandTotal.subtract(gstAmount).add(offerAmount);
        }

        // --- SUMMARY ROWS INTEGRATED AT TABLE FOOTER ---
        boolean hasConfiguredExtraCharges = accessories.compareTo(BigDecimal.ZERO) > 0
                || travel.compareTo(BigDecimal.ZERO) > 0
                || otherCharges.compareTo(BigDecimal.ZERO) > 0
                || discount.compareTo(BigDecimal.ZERO) > 0;

        if (hasConfiguredExtraCharges) {
            // Row 1: Subtotal
            addSummaryRow(itemTable, "Subtotal", rupees(subtotal), false);

            // Row 2: Accessories (if configured)
            if (accessories.compareTo(BigDecimal.ZERO) > 0) {
                addSummaryRow(itemTable, "Accessories", rupees(accessories), false);
            }

            // Row 3: Travel Charges (if configured)
            if (travel.compareTo(BigDecimal.ZERO) > 0) {
                addSummaryRow(itemTable, "Travel Charges", rupees(travel), false);
            }

            // Row 4: Other Charges (if configured)
            if (otherCharges.compareTo(BigDecimal.ZERO) > 0) {
                addSummaryRow(itemTable, "Other Charges", rupees(otherCharges), false);
            }

            // Row 5: Discount (if configured)
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                addSummaryRow(itemTable, "Discount", "- " + rupees(discount), false);
            }

            // Row 6: Total Amount (Amount Before Offer)
            addSummaryRow(itemTable, "Total Amount", rupees(totalAmount != null ? totalAmount : subtotal), true);
        } else {
            // Standard case: Total Amount equals subtotal
            addSummaryRow(itemTable, "Total Amount", rupees(totalAmount != null ? totalAmount : subtotal), true);
        }

        // Row: Offer Price (always present, applied BEFORE GST)
        String offerPriceStr = offerAmount.compareTo(BigDecimal.ZERO) > 0
                ? "- " + rupees(offerAmount)
                : rupees(BigDecimal.ZERO);
        addSummaryRow(itemTable, "Offer Price", offerPriceStr, false);

        // Row: GST (if configured, applied on taxable amount after offer)
        if (gstAmount.compareTo(BigDecimal.ZERO) > 0) {
            String gstLabel = (quotation.getGstPercentage() != null && quotation.getGstPercentage().compareTo(BigDecimal.ZERO) > 0)
                    ? "GST (" + quotation.getGstPercentage().stripTrailingZeros().toPlainString() + "%)"
                    : "GST";
            addSummaryRow(itemTable, gstLabel, rupees(gstAmount), false);
        }

        // Row: GRAND TOTAL (Highlighted with dark teal header fill and gold text)
        addGrandTotalRow(itemTable, "GRAND TOTAL", grandTotal);

        document.add(itemTable);

        // ========================================================
        // 4. AMOUNT IN WORDS (Matching exact quotation Grand Total)
        // ========================================================
        Paragraph wordsPar = new Paragraph();
        wordsPar.setSpacingBefore(2f);
        wordsPar.setSpacingAfter(4f);
        wordsPar.add(new Chunk("AMOUNT IN WORDS - ", new Font(Font.HELVETICA, 8.0f, Font.BOLD, COLOR_NAVY)));
        String wordsText = numberToWordsService.convertRupees(grandTotal).toUpperCase();
        wordsPar.add(new Chunk(wordsText, new Font(Font.HELVETICA, 8.0f, Font.BOLD, COLOR_TEXT_DARK)));
        document.add(wordsPar);

        // ========================================================
        // 5. PAYMENT TERMS BOX (Calculated from Grand Total)
        // ========================================================
        PdfPTable termsTable = new PdfPTable(1);
        termsTable.setWidthPercentage(100);
        termsTable.setSpacingBefore(4f);
        termsTable.setSpacingAfter(14f);

        PdfPCell termsCell = new PdfPCell();
        termsCell.setBackgroundColor(COLOR_LIGHT_BG);
        termsCell.setBorderColor(COLOR_BORDER);
        termsCell.setBorderWidth(0.6f);
        termsCell.setPadding(6f);

        Paragraph termsHeading = new Paragraph("Payment Terms:", new Font(Font.HELVETICA, 8.5f, Font.BOLD, COLOR_GOLD));
        termsHeading.setSpacingAfter(3f);
        termsCell.addElement(termsHeading);

        Font bulletTitleFont = new Font(Font.HELVETICA, 7.5f, Font.BOLD, COLOR_TEXT_DARK);
        Font bulletTextFont = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, COLOR_TEXT_DARK);

        BigDecimal effectiveGrand = grandTotal != null ? grandTotal : BigDecimal.ZERO;
        BigDecimal tokenVal = effectiveGrand.multiply(new BigDecimal("0.10")).setScale(0, RoundingMode.HALF_UP);
        BigDecimal advanceVal = effectiveGrand.multiply(new BigDecimal("0.50")).setScale(0, RoundingMode.HALF_UP);
        BigDecimal partVal = effectiveGrand.multiply(new BigDecimal("0.35")).setScale(0, RoundingMode.HALF_UP);
        BigDecimal finalPaymentVal = effectiveGrand.setScale(0, RoundingMode.HALF_UP)
                .subtract(tokenVal).subtract(advanceVal).subtract(partVal);
        if (finalPaymentVal.compareTo(BigDecimal.ZERO) < 0) {
            finalPaymentVal = BigDecimal.ZERO;
        }

        termsCell.addElement(buildBullet("-  10% Token: ", rupees(tokenVal) + " - Commencement of 3D design on receipt of token.", bulletTitleFont, bulletTextFont));
        termsCell.addElement(buildBullet("-  50% Advance: ", rupees(advanceVal) + " - Drawing will be sent for production on receipt of advance.", bulletTitleFont, bulletTextFont));
        termsCell.addElement(buildBullet("-  35% Part Payment: ", rupees(partVal) + " - After the completion of frame work.", bulletTitleFont, bulletTextFont));
        termsCell.addElement(buildBullet("-  5% Final Payment: ", rupees(finalPaymentVal) + " - The last day of work completion in site.", bulletTitleFont, bulletTextFont));

        termsTable.addCell(termsCell);
        document.add(termsTable);

        // ========================================================
        // 6. CUSTOMER REQUIREMENTS & NOTES
        // ========================================================
        if (quotation.getCustomerRequirements() != null && !quotation.getCustomerRequirements().isBlank()) {
            Paragraph reqHeading = new Paragraph("Customer Requirements:", new Font(Font.HELVETICA, 8.0f, Font.BOLD, COLOR_NAVY));
            reqHeading.setSpacingBefore(2f);
            document.add(reqHeading);
            Paragraph reqText = new Paragraph(quotation.getCustomerRequirements(), new Font(Font.HELVETICA, 7.5f, Font.NORMAL, COLOR_TEXT_DARK));
            reqText.setSpacingAfter(4f);
            document.add(reqText);
        }

        if (quotation.getNotes() != null && !quotation.getNotes().isBlank()) {
            Paragraph notesHeading = new Paragraph("Notes & Special Instructions:", new Font(Font.HELVETICA, 8.0f, Font.BOLD, COLOR_NAVY));
            notesHeading.setSpacingBefore(2f);
            document.add(notesHeading);
            Paragraph notesText = new Paragraph(quotation.getNotes(), new Font(Font.HELVETICA, 7.5f, Font.NORMAL, COLOR_TEXT_DARK));
            notesText.setSpacingAfter(4f);
            document.add(notesText);
        }

        // ========================================================
        // 7. SIGNATURES BLOCK
        // ========================================================
        PdfPTable sigTable = new PdfPTable(2);
        sigTable.setWidthPercentage(100);
        sigTable.setWidths(new float[]{50f, 50f});
        sigTable.setSpacingBefore(8f);

        Font sigFont = new Font(Font.HELVETICA, 8.0f, Font.BOLD, COLOR_TEXT_DARK);

        PdfPCell sig1 = new PdfPCell(new Paragraph("________________________\nCustomer Signature", sigFont));
        sig1.setBorder(Rectangle.NO_BORDER);
        sig1.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell sig2 = new PdfPCell(new Paragraph("________________________\nFor Pravin KITCHENS & INTERIORS", sigFont));
        sig2.setBorder(Rectangle.NO_BORDER);
        sig2.setHorizontalAlignment(Element.ALIGN_CENTER);

        sigTable.addCell(sig1);
        sigTable.addCell(sig2);
        document.add(sigTable);

        // Add Page 2 Footer
        PdfContentByte cb = writer.getDirectContent();
        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
            cb.beginText();
            cb.setFontAndSize(bf, 8);
            cb.setColorFill(COLOR_TEXT_MUTED);
            cb.showTextAligned(Element.ALIGN_CENTER, "Page 2 of 7", PageSize.A4.getWidth() / 2f, 15f, 0);
            cb.endText();
        } catch (Exception ignored) {}
    }

    // ============================================================
    // TEMPLATE PAGE RENDERER WITH INVISIBLE SELECTABLE TEXT
    // ============================================================

    private void renderTemplatePage(
            PdfWriter writer,
            Document doc,
            String imagePath,
            String[] searchableKeywords,
            int pageNumber
    ) {
        PdfContentByte cb = writer.getDirectContentUnder();
        byte[] bytes = loadTemplateImage(imagePath);
        if (bytes != null) {
            try {
                Image img = Image.getInstance(bytes);
                img.setAbsolutePosition(0, 0);
                img.scaleAbsolute(PageSize.A4.getWidth(), PageSize.A4.getHeight());
                cb.addImage(img);
            } catch (Exception ignored) {}
        }

        // Add selectable / searchable text layer
        if (searchableKeywords != null && searchableKeywords.length > 0) {
            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
                cb.beginText();
                cb.setFontAndSize(bf, 9);
                cb.setTextRenderingMode(3); // RenderMode 3 = Invisible
                float y = 780;
                for (String kw : searchableKeywords) {
                    cb.showTextAligned(Element.ALIGN_LEFT, kw, 40, y, 0);
                    y -= 16;
                }
                cb.endText();
                cb.setTextRenderingMode(0);
            } catch (Exception ignored) {}
        }

        // Add footer page number for template pages 3 to 7
        if (pageNumber > 1) {
            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
                cb.beginText();
                cb.setFontAndSize(bf, 8);
                cb.setColorFill(COLOR_TEXT_MUTED);
                cb.showTextAligned(Element.ALIGN_CENTER, "Page " + pageNumber + " of 7", PageSize.A4.getWidth() / 2f, 15f, 0);
                cb.endText();
            } catch (Exception ignored) {}
        }

        writer.setPageEmpty(false);
    }

    private byte[] loadTemplateImage(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ============================================================
    // TABLE & FORMATTING HELPER METHODS
    // ============================================================

    private void addMetaRow(PdfPTable table, String label, String value) {
        Font labelFont = new Font(Font.HELVETICA, 7.5f, Font.BOLD, COLOR_TEXT_DARK);
        Font valueFont = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, COLOR_TEXT_DARK);

        PdfPCell lCell = new PdfPCell(new Phrase(label, labelFont));
        lCell.setBorder(Rectangle.NO_BORDER);
        lCell.setPadding(1.2f);
        lCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell vCell = new PdfPCell(new Phrase(safe(value), valueFont));
        vCell.setBorder(Rectangle.NO_BORDER);
        vCell.setPadding(1.2f);
        vCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        table.addCell(lCell);
        table.addCell(vCell);
    }

    private void addHeaderCell(PdfPTable table, String text, int alignment) {
        Font headerFont = new Font(Font.HELVETICA, 7.0f, Font.BOLD, COLOR_HEADER_TEXT);
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), headerFont));
        cell.setBackgroundColor(COLOR_HEADER_BG);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(4.5f);
        cell.setPaddingBottom(4.5f);
        cell.setPaddingLeft(2.5f);
        cell.setPaddingRight(2.5f);
        cell.setBorderColor(COLOR_HEADER_BG);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, int alignment, boolean isAltRow) {
        Font bodyFont = new Font(Font.HELVETICA, 6.8f, Font.NORMAL, COLOR_TEXT_DARK);
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), bodyFont));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(3.5f);
        cell.setPaddingBottom(3.5f);
        cell.setPaddingLeft(2.5f);
        cell.setPaddingRight(2.5f);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
        if (isAltRow) {
            cell.setBackgroundColor(COLOR_ROW_ALT);
        }
        table.addCell(cell);
    }

    private void addMaterialCell(PdfPTable table, String materialName, String specification, boolean isAltRow) {
        addMaterialCell(table, materialName, specification, null, isAltRow);
    }

    private void addMaterialCell(PdfPTable table, String materialName, String specification, String imageUrl, boolean isAltRow) {
        Font mainFont = new Font(Font.HELVETICA, 6.8f, Font.BOLD, COLOR_NAVY);
        Font specFont = new Font(Font.HELVETICA, 6.0f, Font.NORMAL, COLOR_TEXT_MUTED);

        PdfPCell cell = new PdfPCell();
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(3.5f);
        cell.setPaddingBottom(3.5f);
        cell.setPaddingLeft(3.0f);
        cell.setPaddingRight(3.0f);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
        if (isAltRow) {
            cell.setBackgroundColor(COLOR_ROW_ALT);
        }

        Image itemImage = loadItemImage(imageUrl);
        boolean hasImage = (itemImage != null);
        String safeName = safe(materialName).trim();
        boolean isPhotoPlaceholder = safeName.equalsIgnoreCase("PHOTO")
                || safeName.equalsIgnoreCase("PHOTO - PHOTO")
                || safeName.toUpperCase().matches("^PHOTO([\\s\\-_/]*[\\[\\(]?(FILE|FILES)[\\]\\)]?)?$");

        // If an actual image is available and the material name is "PHOTO", remove the PHOTO text
        boolean showName = !safeName.isBlank() && !safeName.equals("-") && !(hasImage && isPhotoPlaceholder);

        Paragraph namePar = null;
        if (showName) {
            namePar = new Paragraph(safeName.toUpperCase(), mainFont);
            namePar.setLeading(8.5f);
            namePar.setAlignment(Element.ALIGN_LEFT);
        }

        boolean hasSpec = (specification != null && !specification.isBlank() && !specification.equals("-"));
        Paragraph specPar = null;
        if (hasSpec) {
            specPar = new Paragraph(formatSpecificationForPdf(safe(specification)), specFont);
            specPar.setLeading(7.6f);
            specPar.setAlignment(Element.ALIGN_LEFT);
        }

        if (namePar != null) {
            if (hasSpec || hasImage) {
                namePar.setSpacingAfter(3.5f);
            }
            cell.addElement(namePar);
        }

        if (specPar != null) {
            if (hasImage) {
                specPar.setSpacingAfter(3.5f);
            }
            cell.addElement(specPar);
        }

        if (hasImage) {
            itemImage.setAlignment(Image.ALIGN_LEFT);
            itemImage.scaleToFit(65f, 48f);
            cell.addElement(itemImage);
        } else if (namePar == null && specPar == null) {
            // Fallback if no name, no spec, and no image
            Paragraph fallback = new Paragraph(safeName.toUpperCase(), mainFont);
            fallback.setLeading(8.5f);
            fallback.setAlignment(Element.ALIGN_LEFT);
            cell.addElement(fallback);
        }

        table.addCell(cell);
    }

    /**
     * Loads an image from an uploaded quotation item or material image reference.
     * Supports relative paths (/uploads/...), project-relative paths (uploads/...),
     * absolute file paths, web URLs, classpath resources, and Base64 data URIs.
     * If the image cannot be loaded or is missing, returns null safely without breaking PDF generation.
     */
    private Image loadItemImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        String cleanUrl = imageUrl.trim();

        // 1. Base64 data URI
        if (cleanUrl.startsWith("data:image/") && cleanUrl.contains(";base64,")) {
            try {
                String base64Data = cleanUrl.substring(cleanUrl.indexOf(";base64,") + 8);
                byte[] bytes = Base64.getDecoder().decode(base64Data);
                return Image.getInstance(bytes);
            } catch (Exception ignored) {}
        }

        // 2. Local file system lookup
        try {
            String rel = (cleanUrl.startsWith("/") || cleanUrl.startsWith("\\")) ? cleanUrl.substring(1) : cleanUrl;
            Path path = Paths.get(rel);
            if (!Files.isRegularFile(path)) {
                path = Paths.get("uploads").resolve(rel.replaceFirst("^uploads[/\\\\]", ""));
            }
            if (!Files.isRegularFile(path)) {
                path = Paths.get(cleanUrl);
            }
            if (!Files.isRegularFile(path)) {
                path = Paths.get(System.getProperty("user.dir", ".")).resolve(rel);
            }
            if (!Files.isRegularFile(path)) {
                path = Paths.get(System.getProperty("user.dir", ".")).resolve("uploads").resolve(rel.replaceFirst("^uploads[/\\\\]", ""));
            }

            if (Files.isRegularFile(path)) {
                byte[] bytes = Files.readAllBytes(path);
                try {
                    return Image.getInstance(bytes);
                } catch (Exception e) {
                    // Fallback to ImageIO decoding if format is not recognized natively by OpenPDF
                    try {
                        BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(bytes));
                        if (bimg != null) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(bimg, "png", baos);
                            return Image.getInstance(baos.toByteArray());
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        // 3. Web URL
        if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            try {
                return Image.getInstance(URI.create(cleanUrl).toURL());
            } catch (Exception ignored) {}
        }

        // 4. Classpath resource
        try {
            String cpPath = cleanUrl.startsWith("/") ? cleanUrl : "/" + cleanUrl;
            InputStream is = getClass().getResourceAsStream(cpPath);
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                return Image.getInstance(bytes);
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Formats specification text for PDF display in Title Case.
     * Capitalizes the first letter of each word and converts remaining letters to lowercase,
     * while preserving numbers, measurements (e.g. 16mm), special characters, punctuation, and line breaks.
     *
     * Examples:
     * - "MARINE PLY 710 GRADE" -> "Marine Ply 710 Grade"
     * - "16MM THICKNESS" -> "16mm Thickness"
     * - "BRAND: FINE PLY/SABURI" -> "Brand: Fine Ply/Saburi"
     * - "INNER FINISH: FABRIC/OFF WHITE" -> "Inner Finish: Fabric/Off White"
     */
    public static String formatSpecificationForPdf(String specification) {
        if (specification == null || specification.isBlank()) {
            return specification;
        }

        Matcher matcher = SPEC_TITLE_CASE_PATTERN.matcher(specification);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // Digits followed by unit/letters (e.g. "16MM" -> "16mm", "2ND" -> "2nd")
                String digits = matcher.group(1);
                String unit = matcher.group(2).toLowerCase(Locale.ROOT);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(digits + unit));
            } else if (matcher.group(3) != null) {
                // Word starting with letter: first letter capitalized, remaining lowercase
                String firstChar = matcher.group(3).toUpperCase(Locale.ROOT);
                String rest = matcher.group(4) != null ? matcher.group(4).toLowerCase(Locale.ROOT) : "";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(firstChar + rest));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private void addRupeeCell(PdfPTable table, String text, boolean bold, boolean isGold, boolean isAltRow) {
        Font font;
        if (isGold) {
            font = rupeeGoldBoldFont;
        } else if (bold) {
            font = rupeeBoldFont;
        } else {
            font = rupeeFont;
        }

        PdfPCell cell = new PdfPCell(new Phrase(safe(text), font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(3.5f);
        cell.setPaddingBottom(3.5f);
        cell.setPaddingLeft(2.5f);
        cell.setPaddingRight(2.5f);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
        if (isAltRow) {
            cell.setBackgroundColor(COLOR_ROW_ALT);
        }
        table.addCell(cell);
    }

    private void addFillerRow(PdfPTable table, boolean isAltRow) {
        int colCount = table.getNumberOfColumns();
        for (int i = 0; i < colCount; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(" ", rupeeFont));
            cell.setBorderColor(COLOR_BORDER);
            cell.setBorderWidth(0.5f);
            cell.setPadding(3.5f);
            if (isAltRow) {
                cell.setBackgroundColor(COLOR_ROW_ALT);
            }
            table.addCell(cell);
        }
    }

    private void addSummaryRow(PdfPTable table, String label, String amountText, boolean isBold) {
        Font labelFont = new Font(Font.HELVETICA, 7.8f, Font.BOLD, COLOR_TEXT_DARK);
        Font valFont = isBold ? rupeeBoldFont : rupeeFont;

        int labelColspan = Math.max(1, table.getNumberOfColumns() - 1);
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setColspan(labelColspan);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPaddingTop(4.5f);
        labelCell.setPaddingBottom(4.5f);
        labelCell.setPaddingRight(6f);
        labelCell.setBorderColor(COLOR_BORDER);
        labelCell.setBorderWidth(0.5f);
        labelCell.setBackgroundColor(COLOR_LIGHT_BG);
        table.addCell(labelCell);

        PdfPCell valCell = new PdfPCell(new Phrase(amountText, valFont));
        valCell.setColspan(1);
        valCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        valCell.setPaddingTop(4.5f);
        valCell.setPaddingBottom(4.5f);
        valCell.setPaddingRight(4f);
        valCell.setBorderColor(COLOR_BORDER);
        valCell.setBorderWidth(0.5f);
        valCell.setBackgroundColor(COLOR_LIGHT_BG);
        table.addCell(valCell);
    }

    private void addGrandTotalRow(PdfPTable table, String label, BigDecimal grandTotal) {
        Font labelFont = new Font(Font.HELVETICA, 8.5f, Font.BOLD, COLOR_HEADER_TEXT);

        int labelColspan = Math.max(1, table.getNumberOfColumns() - 1);
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setColspan(labelColspan);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPaddingTop(5f);
        labelCell.setPaddingBottom(5f);
        labelCell.setPaddingRight(6f);
        labelCell.setBackgroundColor(COLOR_HEADER_BG);
        labelCell.setBorderColor(COLOR_HEADER_BG);
        table.addCell(labelCell);

        // GRAND TOTAL VALUE (Colspan 1)
        String amtText = rupees(grandTotal);
        PdfPCell totalCell = new PdfPCell(new Phrase(amtText, rupeeGoldBoldFont));
        totalCell.setColspan(1);
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        totalCell.setPaddingTop(5f);
        totalCell.setPaddingBottom(5f);
        totalCell.setPaddingRight(4f);
        totalCell.setBackgroundColor(COLOR_HEADER_BG);
        totalCell.setBorderColor(COLOR_HEADER_BG);
        table.addCell(totalCell);
    }

    private Paragraph buildBullet(String prefix, String text, Font prefixFont, Font textFont) {
        Paragraph p = new Paragraph();
        p.setLeading(9.5f);
        p.add(new Chunk(prefix, prefixFont));
        p.add(new Chunk(text, textFont));
        return p;
    }

    private String resolveUnit(QuotationItem item, PricingMode pricingMode) {
        if (item.getMeasurementType() == MeasurementType.QUANTITY) {
            return "QTY";
        }
        if (item.getMeasurementType() == MeasurementType.FILES) {
            return "FILES";
        }
        if (item.getMaterial() != null && item.getMaterial().getMeasurementType() == MeasurementType.QUANTITY) {
            return "QTY";
        }
        if (item.getMaterial() != null && item.getMaterial().getMeasurementType() == MeasurementType.FILES) {
            return "FILES";
        }
        if (item.getMaterialOption() != null && pricingMode != null) {
            Optional<Pricing> pricingOpt = pricingRepository.findByMaterialOptionIdAndPricingMode(
                    item.getMaterialOption().getId(),
                    pricingMode
            );
            if (pricingOpt.isPresent() && pricingOpt.get().getUnit() != null) {
                String u = pricingOpt.get().getUnit().trim().toUpperCase();
                switch (u) {
                    case "SQFT":
                    case "SQ.FT":
                    case "SQ_FT":
                    case "SFT":
                        return "SQ.FT";
                    case "RUNNING_FEET":
                    case "RFT":
                    case "R.FT":
                        return "R.FT";
                    case "NOS":
                    case "NO":
                    case "QUANTITY":
                        return "QTY";
                    case "FILES":
                        return "FILES";
                    case "LUMP_SUM":
                    case "LS":
                        return "LS";
                    default:
                        return u;
                }
            }
        }
        return "SQ.FT";
    }

    private String buildSize(BigDecimal length, BigDecimal width, BigDecimal height) {
        boolean hasL = length != null && length.compareTo(BigDecimal.ZERO) > 0;
        boolean hasW = width != null && width.compareTo(BigDecimal.ZERO) > 0;
        boolean hasH = height != null && height.compareTo(BigDecimal.ZERO) > 0;

        if (!hasL && !hasW && !hasH) {
            return "-";
        }

        StringBuilder sb = new StringBuilder();
        if (hasL) sb.append(format(length));
        if (hasW) {
            if (sb.length() > 0) sb.append(" x ");
            sb.append(format(width));
        }
        if (hasH) {
            if (sb.length() > 0) sb.append(" x ");
            sb.append(format(height));
        }
        return sb.toString();
    }

    private String rupees(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        String formatted = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        return (hasUnicodeRupee ? "₹" : "Rs. ") + formatted;
    }

    private String format(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}