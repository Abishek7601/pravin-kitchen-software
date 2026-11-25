package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.util.CalculationFormula;

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
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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
    private QuotationViewGradeService quotationViewGradeService;

    /*
     * Design Colors matching the Reference Document (1.pdf)
     */
    private static final Color COLOR_NAVY = new Color(2, 69, 65);          // #024541 Dark Teal brand primary
    private static final Color COLOR_HEADER_BG = new Color(2, 69, 65);      // #024541 Dark Teal table header background
    private static final Color COLOR_GOLD = new Color(168, 123, 46);        // #A87B2E Warm Gold accent lines & section borders
    private static final Color COLOR_HEADER_TEXT = new Color(245, 207, 83); // #F5CF53 Yellow/Gold header font
    private static final Color COLOR_LIGHT_BG = new Color(248, 250, 250);   // #F8FAFA Summary row fill & terms box
    private static final Color COLOR_ROW_ALT = new Color(245, 247, 247);    // #F5F7F7 Alternate table row fill
    private static final Color COLOR_BORDER = new Color(226, 230, 230);     // Very light, subtle table grid & cell borders
    private static final Color COLOR_TEXT_DARK = new Color(25, 25, 25);      // #191919 Crisp main text
    private static final Color COLOR_TEXT_MUTED = new Color(70, 70, 70);     // #464646 Clear charcoal specifications & subtitles

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
            @org.springframework.beans.factory.annotation.Autowired(required = false) QuotationCalculationService quotationCalculationService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) QuotationViewGradeService quotationViewGradeService
    ) {
        this.quotationRepository = quotationRepository;
        this.quotationRoomRepository = quotationRoomRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.numberToWordsService = numberToWordsService;
        this.branchRepository = branchRepository;
        this.pricingRepository = pricingRepository;
        this.quotationCalculationService = quotationCalculationService;
        this.quotationViewGradeService = quotationViewGradeService;

        try {
            BaseFont rupeeBaseFont = BaseFont.createFont(
                    "C:/Windows/Fonts/arial.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
            );
            rupeeFont = new Font(rupeeBaseFont, 10.0f, Font.BOLD, COLOR_TEXT_DARK);
            rupeeBoldFont = new Font(rupeeBaseFont, 10.5f, Font.BOLD, COLOR_TEXT_DARK);
            rupeeGoldBoldFont = new Font(rupeeBaseFont, 11.5f, Font.BOLD, COLOR_HEADER_TEXT);
            hasUnicodeRupee = true;
        } catch (Exception e) {
            rupeeFont = new Font(Font.HELVETICA, 10.0f, Font.BOLD, COLOR_TEXT_DARK);
            rupeeBoldFont = new Font(Font.HELVETICA, 10.5f, Font.BOLD, COLOR_TEXT_DARK);
            rupeeGoldBoldFont = new Font(Font.HELVETICA, 11.5f, Font.BOLD, COLOR_HEADER_TEXT);
            hasUnicodeRupee = false;
        }
    }

    public QuotationPdfService(
            QuotationRepository quotationRepository,
            QuotationRoomRepository quotationRoomRepository,
            QuotationItemRepository quotationItemRepository,
            NumberToWordsService numberToWordsService,
            BranchRepository branchRepository,
            PricingRepository pricingRepository,
            QuotationCalculationService quotationCalculationService
    ) {
        this(quotationRepository, quotationRoomRepository, quotationItemRepository,
             numberToWordsService, branchRepository, pricingRepository, quotationCalculationService, null);
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
             numberToWordsService, branchRepository, pricingRepository, null, null);
    }

    public void setQuotationViewGradeService(QuotationViewGradeService quotationViewGradeService) {
        this.quotationViewGradeService = quotationViewGradeService;
    }

    // ============================================================
    // GENERATE 7-PAGE REFERENCE-BASED QUOTATION PDF
    // ============================================================

    public byte[] generateQuotationPdf(Long quotationId) {
        return generateQuotationPdf(quotationId, null);
    }

    public byte[] generateQuotationPdf(Long quotationId, PricingMode targetGrade) {
        try {
            Quotation quotation = quotationRepository.findById(quotationId)
                    .orElseThrow(() -> new RuntimeException("Quotation not found with ID: " + quotationId));

            // Determine effective grade
            PricingMode effectiveGrade = targetGrade;
            if (effectiveGrade == null) {
                if (quotation.getSelectedGrade() != null) {
                    effectiveGrade = quotation.getSelectedGrade();
                } else if (quotation.getParent() != null && quotation.getParent().getSelectedGrade() != null) {
                    effectiveGrade = quotation.getParent().getSelectedGrade();
                } else if (quotation.getPricingMode() != null) {
                    effectiveGrade = quotation.getPricingMode();
                } else {
                    effectiveGrade = PricingMode.PREMIUM;
                }
            }

            // Check if there is a child quotation for this grade
            Quotation parent = quotation.getParent() != null ? quotation.getParent() : quotation;
            Quotation childQuote = parent.getChildForGrade(effectiveGrade);
            if (childQuote != null && childQuote.getId() != null) {
                quotation = childQuote;
            }

            // Fetch branch dynamically from Branch Management data
            Branch branch = quotation.getBranch();
            if (branch == null && parent.getBranch() != null) {
                branch = parent.getBranch();
            }
            if (branch != null && branch.getId() != null) {
                branch = branchRepository.findById(branch.getId()).orElse(branch);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 25, 25, 46, 20);
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.setMargins(25, 25, 46, 20);
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
            renderDynamicMarketingQuotationPage(document, writer, quotation, branch, effectiveGrade);
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
        renderDynamicMarketingQuotationPage(document, writer, quotation, branch, null);
    }

    private void renderDynamicMarketingQuotationPage(
            Document document,
            PdfWriter writer,
            Quotation quotation,
            Branch branch,
            PricingMode effectiveGrade
    ) throws Exception {

        // For all subsequent continuation pages (pages 3, 4, etc.), apply a noticeably increased top margin (72pt = 1.0 inch)
        // Note: The first quotation page has already opened with its existing 46pt top margin.
        // Calling setMargins here configures nextMarginTop so every continuation page (both manual and automatic
        // page breaks) starts noticeably further down with generous top white space.
        document.setMargins(25, 25, 72f, 20);

        // ========================================================
        // 1. TOP HEADER TABLE (BRAND & ADDRESS ON LEFT, QUOTE INFO ON RIGHT)
        // ========================================================
        PdfPTable topHeaderTable = new PdfPTable(2);
        topHeaderTable.setWidthPercentage(100);
        topHeaderTable.setWidths(new float[]{48f, 52f});
        topHeaderTable.setSpacingAfter(6f);

        // --- LEFT COLUMN: BRAND NAME + ADDRESS ---
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0);
        leftCell.setPaddingRight(14f); // Spacing between company info and quotation info

        Paragraph companyName = new Paragraph("PRAVIN KITCHENS & INTERIORS", new Font(Font.HELVETICA, 20.5f, Font.BOLD, COLOR_NAVY));
        companyName.setLeading(24f);
        companyName.setSpacingAfter(5f);
        leftCell.addElement(companyName);

        Paragraph addressPar = new Paragraph();
        addressPar.setFont(new Font(Font.HELVETICA, 9.8f, Font.NORMAL, COLOR_TEXT_MUTED));
        addressPar.setLeading(14.5f);

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

        // --- RIGHT COLUMN: QUOTATION METADATA ---
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(0);
        rightCell.setPaddingLeft(6f);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PricingMode gradeForPage = effectiveGrade;
        if (gradeForPage == null) {
            if (quotation.getSelectedGrade() != null) {
                gradeForPage = quotation.getSelectedGrade();
            } else if (quotation.getParent() != null && quotation.getParent().getSelectedGrade() != null) {
                gradeForPage = quotation.getParent().getSelectedGrade();
            } else if (quotation.getPricingMode() != null) {
                gradeForPage = quotation.getPricingMode();
            } else {
                gradeForPage = PricingMode.PREMIUM;
            }
        }

        String qType = formatSingleGrade(gradeForPage);

        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{40f, 60f});
        metaTable.setSpacingBefore(0f);

        addMetaRow(metaTable, "Quotation No:", safe(quotation.getQuotationNumber()));
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

        String divisionVal = "—";
        if (quotation.getBranch() != null && quotation.getBranch().getName() != null && !quotation.getBranch().getName().isBlank()) {
            divisionVal = quotation.getBranch().getName().trim();
        } else if (quotation.getCustomer() != null && quotation.getCustomer().getBranch() != null && quotation.getCustomer().getBranch().getName() != null && !quotation.getCustomer().getBranch().getName().isBlank()) {
            divisionVal = quotation.getCustomer().getBranch().getName().trim();
        } else if (quotation.getDivision() != null && quotation.getDivision().getName() != null && !quotation.getDivision().getName().isBlank()) {
            divisionVal = quotation.getDivision().getName().trim();
        } else if (branch != null && branch.getName() != null && !branch.getName().isBlank()) {
            divisionVal = branch.getName().trim();
        }
        addMetaRow(metaTable, "Division:", divisionVal);

        rightCell.addElement(metaTable);
        topHeaderTable.addCell(rightCell);

        document.add(topHeaderTable);

        // Full-width Accent Line
        LineSeparator headerDivider = new LineSeparator(1.5f, 100, COLOR_GOLD, Element.ALIGN_CENTER, -2);
        document.add(headerDivider);

        // ========================================================
        // 2. SECTION TITLE: QUOTE SUMMARY
        // ========================================================
        Paragraph summaryTitle = new Paragraph("Quote Summary", new Font(Font.HELVETICA, 14.0f, Font.BOLD, COLOR_NAVY));
        summaryTitle.setSpacingBefore(5f);
        summaryTitle.setSpacingAfter(5f);
        document.add(summaryTitle);

        // ========================================================
        // 3. QUOTE SUMMARY TABLE (6 to 8 COLUMNS BASED ON TOGGLES)
        // ========================================================
        boolean showSize = quotation.getShowSize() == null || quotation.getShowSize();
        boolean showSqft = quotation.getShowSqft() == null || quotation.getShowSqft();
        int numColumns = 6 + (showSize ? 1 : 0) + (showSqft ? 1 : 0);
        float[] columnWidths;
        if (showSize && showSqft) {
            // 8 columns: #, Work Type, Materials, Size, Sqft, Unit, Price, Offer Price
            columnWidths = new float[]{4.0f, 14.5f, 24.5f, 14.5f, 6.5f, 6.0f, 14.5f, 15.5f};
        } else if (showSize && !showSqft) {
            // 7 columns: #, Work Type, Materials, Size, Unit, Price, Offer Price
            columnWidths = new float[]{4.0f, 16.0f, 27.5f, 16.0f, 6.5f, 14.5f, 15.5f};
        } else if (!showSize && showSqft) {
            // 7 columns: #, Work Type, Materials, Sqft, Unit, Price, Offer Price
            columnWidths = new float[]{4.0f, 17.0f, 31.0f, 8.0f, 7.0f, 16.0f, 17.0f};
        } else {
            // 6 columns: #, Work Type, Materials, Unit, Price, Offer Price
            columnWidths = new float[]{4.5f, 19.5f, 36.0f, 7.5f, 16.0f, 16.5f};
        }

        // --- COLLECT AND GROUP ITEMS BY FLOOR AND CATEGORY ---
        java.util.Map<String, java.util.Map<String, List<PdfItemData>>> itemsByFloorAndCategory = new java.util.LinkedHashMap<>();
        boolean usedGradeService = false;
        QuotationViewGradeService.GradeViewResponse gradeData = null;

        if (quotationViewGradeService != null) {
            try {
                gradeData = quotationViewGradeService.getGradeViewData(quotation.getId(), gradeForPage);
                if (gradeData != null && gradeData.success() && gradeData.rooms() != null && !gradeData.rooms().isEmpty()) {
                    usedGradeService = true;
                    for (QuotationViewGradeService.GradeViewRoomDto roomDto : gradeData.rooms()) {
                        if (roomDto.items() == null || roomDto.items().isEmpty()) continue;
                        String floorName = roomDto.floor() != null && !roomDto.floor().isBlank() ? roomDto.floor() : "Ground Floor";

                        for (QuotationViewGradeService.GradeViewItemDto itemDto : roomDto.items()) {
                            String catName = itemDto.categoryName() != null && !itemDto.categoryName().isBlank() ? itemDto.categoryName() : "General";
                            String workType = safe(itemDto.workTypeName());
                            if (workType.isBlank() || workType.equals("-")) {
                                workType = "-";
                            }

                            String materialText = safe(itemDto.materialName());
                            if (itemDto.materialOptionName() != null && !itemDto.materialOptionName().isBlank()
                                    && !itemDto.materialOptionName().equalsIgnoreCase(materialText)) {
                                materialText += " - " + itemDto.materialOptionName();
                            }

                            String spec = safe(itemDto.specification()).trim();
                            String rawMatNameDto = itemDto.materialName() != null ? itemDto.materialName().trim() : "";
                            int parenIdx = rawMatNameDto.indexOf('(');
                            if (parenIdx > 0) {
                                rawMatNameDto = rawMatNameDto.substring(0, parenIdx).trim();
                            }
                            if (!rawMatNameDto.isBlank() && spec.toUpperCase().startsWith(rawMatNameDto.toUpperCase())) {
                                spec = spec.substring(rawMatNameDto.length()).trim();
                                if (spec.startsWith("-") || spec.startsWith(":")) {
                                    spec = spec.substring(1).trim();
                                }
                            }
                            String sizeStr = safe(itemDto.pdfDimensions() != null ? itemDto.pdfDimensions() : itemDto.dimensions()).replace("<br>", "\n").replace("<br/>", "\n");
                            String sqftStr = safe(itemDto.sqft());
                            String unitStr = safe(itemDto.unit());
                            String amtStr = safe(itemDto.amount()).replace("₹", "").replace(",", "").trim();
                            BigDecimal itemAmt = BigDecimal.ZERO;
                            try {
                                if (!amtStr.isBlank()) itemAmt = new BigDecimal(amtStr);
                            } catch (Exception ignored) {}

                            BigDecimal offerAmt = itemAmt;
                            if (itemDto.offerPrice() != null && !itemDto.offerPrice().isBlank()) {
                                String offStr = safe(itemDto.offerPrice()).replace("₹", "").replace(",", "").trim();
                                try {
                                    if (!offStr.isBlank()) offerAmt = new BigDecimal(offStr);
                                } catch (Exception ignored) {}
                            }

                            PdfItemData itemData = new PdfItemData(
                                    workType,
                                    materialText,
                                    spec,
                                    itemDto.imageUrl(),
                                    sizeStr,
                                    sqftStr,
                                    unitStr,
                                    itemAmt,
                                    offerAmt
                            );
                            itemsByFloorAndCategory
                                    .computeIfAbsent(floorName, k -> new java.util.LinkedHashMap<>())
                                    .computeIfAbsent(catName, k -> new ArrayList<>())
                                    .add(itemData);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error getting grade view data for PDF: {}", e.getMessage());
                usedGradeService = false;
            }
        }

        List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationId(quotation.getId());
        if (!usedGradeService) {
            for (QuotationRoom room : rooms) {
                if (!Boolean.TRUE.equals(room.getActive())) {
                    continue;
                }
                String floorName = room.getFloor() != null && !room.getFloor().isBlank() ? room.getFloor() : "Ground Floor";
                List<QuotationItem> items = quotationItemRepository.findByQuotationRoomId(room.getId());

                for (QuotationItem item : items) {
                    if (!Boolean.TRUE.equals(item.getActive())) {
                        continue;
                    }

                    // Work Category
                    String catName = item.getWorkCategory() != null ? safe(item.getWorkCategory().getName()) : "General";

                    // Work Type
                    String workType = QuotationViewGradeService.resolveWorkTypeName(item, catName);

                    // Material & Option
                    String rawMatName = item.getMaterial() != null ? safe(item.getMaterial().getName()).trim() : "";
                    String optionName = item.getMaterialOption() != null ? safe(item.getMaterialOption().getName()).trim() : "";

                    PricingMode itemGrade = item.getEffectivePricingMode() != null ? item.getEffectivePricingMode() : gradeForPage;
                    String gradeSuffix = "";
                    if (itemGrade != null) {
                        switch (itemGrade) {
                            case PREMIUM: gradeSuffix = " (Premium)"; break;
                            case MIDDLE: gradeSuffix = " (Middle)"; break;
                            case ECONOMY: gradeSuffix = " (Economy)"; break;
                            default: gradeSuffix = " (" + itemGrade.name() + ")"; break;
                        }
                    }

                    String baseName = !rawMatName.isBlank() ? rawMatName : optionName;
                    String optionSuffix = (!optionName.isBlank() && !optionName.equalsIgnoreCase(rawMatName) && !rawMatName.isBlank())
                            ? " - " + optionName : "";
                    String materialText = baseName;
                    if (!materialText.isBlank()) {
                        materialText = materialText + gradeSuffix + optionSuffix;
                    } else {
                        materialText = !gradeSuffix.isBlank() ? "-" + gradeSuffix : "-";
                    }

                    // Specifications
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
                    boolean isRoll = item.getMeasurementType() == MeasurementType.ROLL || (item.getMaterial() != null && item.getMaterial().getMeasurementType() == MeasurementType.ROLL);
                    boolean isSqft = !isRoll && (item.getMeasurementType() == null || item.getMeasurementType().isSqft());
                    String sizeStr;
                    String sqftStr;
                    if (isRoll || isSqft) {
                        String itemFormula = resolveItemFormula(item);
                        sizeStr = QuotationViewGradeService.formatFormulaDimension(itemFormula, item.getLengthValue(), item.getWidthValue(), item.getHeightValue());
                        sqftStr = format(sqft);
                    } else {
                        BigDecimal qty = (item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                                ? item.getQuantity()
                                : BigDecimal.ONE;
                        sizeStr = "QTY: " + qty.stripTrailingZeros().toPlainString();
                        sqftStr = "-";
                    }

                    // Unit
                    PricingMode effectiveMode = item.getEffectivePricingMode() != null ? item.getEffectivePricingMode() : gradeForPage;
                    String unitStr = resolveUnit(item, effectiveMode);

                    // Amounts
                    BigDecimal rate = item.getRate() != null ? item.getRate() : BigDecimal.ZERO;
                    BigDecimal amount = item.getAmount() != null
                            ? item.getAmount()
                            : (isRoll
                                    ? rate.multiply(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE)
                                    : (isSqft ? rate.multiply(sqft) : rate.multiply(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE)));

                    if (isRoll) {
                        BigDecimal rolls = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
                        BigDecimal cov = item.getCoveragePerRoll() != null ? item.getCoveragePerRoll()
                                : (item.getMaterial() != null && item.getMaterial().getCoveragePerRoll() != null ? item.getMaterial().getCoveragePerRoll() : null);
                        String covText = cov != null ? cov.stripTrailingZeros().toPlainString() : "-";
                        String rollDetails = rolls.stripTrailingZeros().toPlainString() + " Roll(s) (Coverage: " + covText + " Sq.ft/Roll @ " + (hasUnicodeRupee ? "₹\u00A0" : "Rs.\u00A0") + com.example.pravin_quotation.util.IndianCurrencyFormatter.formatBigDecimal(rate) + "/Roll)";
                        if (specification == null || specification.isBlank()) {
                            specification = rollDetails;
                        } else {
                            specification = specification + "\n" + rollDetails;
                        }
                    }
                    BigDecimal offerPrice = item.getOfferPrice();
                    if (offerPrice == null || offerPrice.compareTo(BigDecimal.ZERO) <= 0) {
                        offerPrice = amount;
                    }

                    String itemImageUrl = item.getImageUrl();
                    if ((itemImageUrl == null || itemImageUrl.isBlank()) && item.getMaterial() != null) {
                        itemImageUrl = item.getMaterial().getImageUrl();
                    }

                    PdfItemData itemData = new PdfItemData(
                            workType,
                            materialText,
                            specification,
                            itemImageUrl,
                            sizeStr,
                            sqftStr,
                            unitStr,
                            amount,
                            offerPrice
                    );
                    itemsByFloorAndCategory
                            .computeIfAbsent(floorName, k -> new java.util.LinkedHashMap<>())
                            .computeIfAbsent(catName, k -> new ArrayList<>())
                            .add(itemData);
                }
            }
        }

        // --- RENDER TABLE ROWS: FLOOR -> CATEGORY -> WORK TYPE -> MATERIALS ---
        int serialNumber = 1;
        BigDecimal totalOriginalPrice = BigDecimal.ZERO;
        BigDecimal subtotalOfferPrice = BigDecimal.ZERO;
        boolean isFirstTable = true;

        if (itemsByFloorAndCategory.isEmpty()) {
            PdfPTable emptyTable = createSectionTable(numColumns, columnWidths);
            addTableHeaders(emptyTable, showSize, showSqft);
            document.add(emptyTable);
            isFirstTable = false;
        }

        for (java.util.Map.Entry<String, java.util.Map<String, List<PdfItemData>>> floorEntry : itemsByFloorAndCategory.entrySet()) {
            String floorName = floorEntry.getKey();
            boolean isFloorStart = true;

            for (java.util.Map.Entry<String, List<PdfItemData>> catEntry : floorEntry.getValue().entrySet()) {
                String catName = catEntry.getKey();
                boolean isCategoryStart = true;
                int lastCategoryHeadingPage = -1;

                // Group items under category by Work Type (preserving insertion order)
                java.util.Map<String, List<PdfItemData>> workTypeGroups = new java.util.LinkedHashMap<>();
                for (PdfItemData it : catEntry.getValue()) {
                    String wt = (it.workType != null && !it.workType.isBlank()) ? it.workType.trim() : "-";
                    workTypeGroups.computeIfAbsent(wt, k -> new ArrayList<>()).add(it);
                }

                for (java.util.Map.Entry<String, List<PdfItemData>> wtEntry : workTypeGroups.entrySet()) {
                    String workTypeName = wtEntry.getKey();
                    List<PdfItemData> materials = wtEntry.getValue();
                    int groupSize = materials.size();
                    String serialStr = String.valueOf(serialNumber);
                    String formattedWorkType = formatSectionHeading(workTypeName);

                    PdfPTable sectionTable = createSectionTable(numColumns, columnWidths);
                    if (isFirstTable) {
                        addTableHeaders(sectionTable, showSize, showSqft);
                        isFirstTable = false;
                    }

                    if (isFloorStart) {
                        addFloorHeadingRow(sectionTable, floorName);
                        isFloorStart = false;
                    }

                    boolean addedCategoryHeadingInThisSection = false;
                    if (isCategoryStart) {
                        addCategoryHeadingRow(sectionTable, catName);
                        addedCategoryHeadingInThisSection = true;
                        isCategoryStart = false;
                    }

                    for (int i = 0; i < groupSize; i++) {
                        PdfItemData it = materials.get(i);
                        totalOriginalPrice = totalOriginalPrice.add(it.price);
                        subtotalOfferPrice = subtotalOfferPrice.add(it.offerPrice);

                        // Display Serial # and Work Type only once for the entire Work Type group using rowspan
                        if (i == 0) {
                            addBodyCell(sectionTable, serialStr, Element.ALIGN_CENTER, false, true, groupSize, false);
                            addBodyCell(sectionTable, formattedWorkType, Element.ALIGN_LEFT, false, false, groupSize, false);
                        }

                        addMaterialCell(sectionTable, it.materialText, formatSpecificationForPdf(it.specification), it.imageUrl, false);
                        if (showSize) {
                            addBodyCell(sectionTable, it.sizeStr, Element.ALIGN_CENTER, false, false, 1, false);
                        }
                        if (showSqft) {
                            addBodyCell(sectionTable, it.sqftStr, Element.ALIGN_CENTER, false, true, 1, false);
                        }
                        addBodyCell(sectionTable, it.unitStr, Element.ALIGN_CENTER, false, true, 1, false);
                        addRupeeCell(sectionTable, rupees(it.price), true, false, false, 2.0f, 10.0f);
                        addRupeeCell(sectionTable, rupees(it.offerPrice), true, false, false, 6.0f, 4.0f);
                    }

                    serialNumber++;

                    sectionTable.setKeepTogether(true);
                    sectionTable.setTotalWidth(document.right() - document.left());
                    sectionTable.calculateHeights(true);
                    float sectionHeight = sectionTable.getTotalHeight();

                    float availableSpace = writer.getVerticalPosition(false) - document.bottom();
                    float fullPageHeight = document.top() - document.bottom();

                    // If insufficient space is available on current page, move to next page
                    if (availableSpace < sectionHeight + 25f && availableSpace < fullPageHeight - 50f) {
                        document.newPage();
                    }

                    int currentPage = writer.getPageNumber();
                    // If this Work Type was not the start of the Category (continuation within the same Category onto a new page),
                    // repeat the Category heading at the top of the new page to keep category association clear.
                    if (!addedCategoryHeadingInThisSection && currentPage > lastCategoryHeadingPage) {
                        PdfPTable contCatTable = createSectionTable(numColumns, columnWidths);
                        addCategoryHeadingRow(contCatTable, catName);
                        document.add(contCatTable);
                        lastCategoryHeadingPage = writer.getPageNumber();
                    }

                    document.add(sectionTable);
                    if (addedCategoryHeadingInThisSection) {
                        lastCategoryHeadingPage = currentPage;
                    }
                }
            }
        }

        // --- PDF FINANCIAL VALUES (SINGLE SOURCE OF TRUTH) ---
        BigDecimal subtotal = subtotalOfferPrice;
        if (subtotal.compareTo(BigDecimal.ZERO) <= 0 && quotation.getSubtotal() != null && quotation.getSubtotal().compareTo(BigDecimal.ZERO) > 0) {
            subtotal = quotation.getSubtotal();
        }

        BigDecimal totalOriginal = totalOriginalPrice.compareTo(BigDecimal.ZERO) > 0 ? totalOriginalPrice : subtotal;

        BigDecimal accessories = zeroIfNull(quotation.getAccessoriesAmount());
        BigDecimal travel = zeroIfNull(quotation.getTravelCharge());
        BigDecimal otherCharges = zeroIfNull(quotation.getOtherCharges());
        BigDecimal discount = zeroIfNull(quotation.getDiscountAmount());
        BigDecimal gstAmount = zeroIfNull(quotation.getGstAmount());
        BigDecimal totalAmount = quotation.getTotalAmount();
        BigDecimal grandTotal = null;

        boolean isGradeSwitched = (effectiveGrade != null && quotation.getPricingMode() != null && effectiveGrade != quotation.getPricingMode());
        if (isGradeSwitched && usedGradeService && gradeData != null && gradeData.grandTotalRaw() != null) {
            grandTotal = gradeData.grandTotalRaw();
        } else if (quotation.getGrandTotal() != null) {
            grandTotal = quotation.getGrandTotal();
        } else if (usedGradeService && gradeData != null && gradeData.grandTotalRaw() != null) {
            grandTotal = gradeData.grandTotalRaw();
        }

        BigDecimal calculatedTotalAmount = subtotal.add(accessories).add(travel).add(otherCharges).subtract(discount);
        if (calculatedTotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            calculatedTotalAmount = BigDecimal.ZERO;
        }

        if (totalAmount == null || isGradeSwitched) {
            totalAmount = calculatedTotalAmount;
        }

        BigDecimal gstPct = zeroIfNull(quotation.getGstPercentage());
        if (gstPct.compareTo(BigDecimal.ZERO) > 0 && gstAmount.compareTo(BigDecimal.ZERO) <= 0) {
            gstAmount = calculatedTotalAmount.multiply(gstPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        if (grandTotal == null) {
            grandTotal = calculatedTotalAmount.add(gstAmount);
        }

        // --- FINANCIAL SUMMARY ROWS (KEPT TOGETHER AS ONE CONTINUOUS BLOCK) ---
        PdfPTable summaryTable = createSectionTable(numColumns, columnWidths);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(columnWidths);
        summaryTable.setKeepTogether(true);
        summaryTable.setSpacingBefore(0f);
        summaryTable.setSpacingAfter(4f);

        // 1. Total: Sum of original material prices (Price column) and Offer Prices (Offer Price column)
        addTotalRow(summaryTable, totalOriginalPrice, subtotalOfferPrice);

        // 2. SubTotal: Sum of material offer prices (Offer Price)
        addSummaryRow(summaryTable, "Subtotal", rupees(subtotal), false);

        // 3. Travel Charges (if configured)
        if (travel.compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(summaryTable, "Travel Charges", rupees(travel), false);
        }

        // 4. Other Charges (with description if configured)
        if (otherCharges.compareTo(BigDecimal.ZERO) > 0) {
            String otherChargesLabel = "Other Charges";
            if (quotation.getOtherChargesDescription() != null && !quotation.getOtherChargesDescription().trim().isEmpty()) {
                String desc = quotation.getOtherChargesDescription().trim();
                char first = desc.charAt(0);
                if (Character.isLowerCase(first)) {
                    desc = Character.toUpperCase(first) + desc.substring(1);
                }
                otherChargesLabel = "Other Charges (" + desc + ")";
            }
            addSummaryRow(summaryTable, otherChargesLabel, rupees(otherCharges), false);
        }

        // Row: Discount (if configured)
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(summaryTable, "Discount", "- " + rupees(discount), false);
        }

        // Row: Offer Price (if configured at quotation level)
        if (quotation.getOfferAmount() != null && quotation.getOfferAmount().compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(summaryTable, "Offer Price", "- " + rupees(quotation.getOfferAmount()), false);
        }

        // 5. Total Amount
        addSummaryRow(summaryTable, "Total Amount", rupees(totalAmount), true);

        // 6. GST
        if (gstAmount.compareTo(BigDecimal.ZERO) > 0) {
            String gstLabel = (quotation.getGstPercentage() != null && quotation.getGstPercentage().compareTo(BigDecimal.ZERO) > 0)
                    ? "GST (" + quotation.getGstPercentage().stripTrailingZeros().toPlainString() + "%)"
                    : "GST";
            addSummaryRow(summaryTable, gstLabel, rupees(gstAmount), false);
        }

        // 7. Grand Total
        addGrandTotalRow(summaryTable, "GRAND TOTAL", grandTotal);

        // ========================================================
        // 4. AMOUNT IN WORDS (Matching exact quotation Grand Total)
        // ========================================================
        Paragraph wordsPar = new Paragraph();
        wordsPar.setKeepTogether(true);
        wordsPar.setSpacingBefore(2f);
        wordsPar.setSpacingAfter(3f);
        wordsPar.add(new Chunk("AMOUNT IN WORDS - ", new Font(Font.HELVETICA, 11.0f, Font.BOLD, COLOR_NAVY)));
        String wordsText = numberToWordsService.convertRupees(grandTotal).toUpperCase();
        wordsPar.add(new Chunk(wordsText, new Font(Font.HELVETICA, 10.5f, Font.BOLD, COLOR_TEXT_DARK)));

        // --- CALCULATE WHETHER COMPLETE AMOUNT SUMMARY BLOCK FITS ON CURRENT PAGE ---
        float tableEndY = writer.getVerticalPosition(false);
        summaryTable.setTotalWidth(document.right() - document.left());
        summaryTable.calculateHeights(true);
        float summaryTableHeight = summaryTable.getTotalHeight();
        float wordsEstimatedHeight = 22f;
        float safetyMargin = 10f;
        float requiredBlockSpace = summaryTableHeight + wordsEstimatedHeight + safetyMargin;
        float availableSpace = tableEndY - document.bottom();

        // If insufficient space is available on current page, move the entire summary block to next page
        if (availableSpace < requiredBlockSpace) {
            document.newPage();
        }

        document.add(summaryTable);
        document.add(wordsPar);

        // ========================================================
        // 5. PAYMENT TERMS BOX (Calculated from Grand Total)
        // ========================================================
        PdfPTable termsTable = new PdfPTable(1);
        termsTable.setWidthPercentage(100);
        termsTable.setKeepTogether(true);
        termsTable.setSpacingBefore(3f);
        termsTable.setSpacingAfter(6f);

        PdfPCell termsCell = new PdfPCell();
        termsCell.setBackgroundColor(COLOR_LIGHT_BG);
        termsCell.setBorderColor(COLOR_BORDER);
        termsCell.setBorderWidth(0.8f);
        termsCell.setPadding(7f);

        Paragraph termsHeading = new Paragraph("Payment Terms:", new Font(Font.HELVETICA, 13.0f, Font.BOLD, COLOR_GOLD));
        termsHeading.setSpacingAfter(4f);
        termsCell.addElement(termsHeading);

        Font bulletTitleFont = new Font(Font.HELVETICA, 10.5f, Font.BOLD, COLOR_TEXT_DARK);
        Font bulletTextFont = new Font(Font.HELVETICA, 10.0f, Font.NORMAL, COLOR_TEXT_DARK);

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
            Paragraph reqHeading = new Paragraph("Customer Requirements:", new Font(Font.HELVETICA, 11.0f, Font.BOLD, COLOR_NAVY));
            reqHeading.setSpacingBefore(3f);
            document.add(reqHeading);
            Paragraph reqText = new Paragraph(quotation.getCustomerRequirements(), new Font(Font.HELVETICA, 10.0f, Font.NORMAL, COLOR_TEXT_DARK));
            reqText.setSpacingAfter(4f);
            document.add(reqText);
        }

        if (quotation.getNotes() != null && !quotation.getNotes().isBlank()) {
            Paragraph notesHeading = new Paragraph("Notes & Special Instructions:", new Font(Font.HELVETICA, 11.0f, Font.BOLD, COLOR_NAVY));
            notesHeading.setSpacingBefore(3f);
            document.add(notesHeading);
            Paragraph notesText = new Paragraph(quotation.getNotes(), new Font(Font.HELVETICA, 10.0f, Font.NORMAL, COLOR_TEXT_DARK));
            notesText.setSpacingAfter(4f);
            document.add(notesText);
        }

        // ========================================================
        // 7. SIGNATURES BLOCK (Positioned dynamically near bottom margin)
        // ========================================================
        PdfPTable sigTable = new PdfPTable(2);
        sigTable.setWidthPercentage(100);
        sigTable.setWidths(new float[]{50f, 50f});

        Font sigFont = new Font(Font.HELVETICA, 11.5f, Font.BOLD, COLOR_TEXT_DARK);

        // Customer Signature on the Left
        Paragraph sig1Par = new Paragraph("Customer Signature", sigFont);
        sig1Par.setAlignment(Element.ALIGN_LEFT);
        PdfPCell sig1 = new PdfPCell(sig1Par);
        sig1.setBorder(Rectangle.NO_BORDER);
        sig1.setHorizontalAlignment(Element.ALIGN_LEFT);
        sig1.setVerticalAlignment(Element.ALIGN_BOTTOM);
        sig1.setPaddingTop(38f);
        sig1.setPaddingBottom(2f);
        sig1.setPaddingLeft(6f);
        sig1.setPaddingRight(0f);

        // For Pravin KITCHENS & INTERIORS on the Right
        Paragraph sig2Par = new Paragraph("For Pravin KITCHENS & INTERIORS", sigFont);
        sig2Par.setAlignment(Element.ALIGN_RIGHT);
        PdfPCell sig2 = new PdfPCell(sig2Par);
        sig2.setBorder(Rectangle.NO_BORDER);
        sig2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        sig2.setVerticalAlignment(Element.ALIGN_BOTTOM);
        sig2.setPaddingTop(38f);
        sig2.setPaddingBottom(2f);
        sig2.setPaddingLeft(0f);
        sig2.setPaddingRight(6f);

        sigTable.addCell(sig1);
        sigTable.addCell(sig2);

        // Dynamic vertical positioning:
        // Measure current vertical position after Payment Terms (and Notes/Requirements if any)
        float currentY = writer.getVerticalPosition(false);
        sigTable.setTotalWidth(document.right() - document.left());
        sigTable.calculateHeights(true);
        float sigHeight = sigTable.getTotalHeight();
        if (sigHeight <= 0) {
            sigHeight = 55f;
        }

        // Target: place the bottom of the signature table just above the footer & bottom margin
        // document.bottom() is 20f, footer "Page 2 of 7" is at y = 15f.
        float targetBottomY = document.bottom() + 15f; // 35f
        float targetTopY = targetBottomY + sigHeight;   // ~90f

        PdfContentByte directCb = writer.getDirectContent();

        if (currentY >= targetTopY + 10f) {
            // Fits cleanly on current page at bottom
            sigTable.writeSelectedRows(0, -1, document.left(), targetTopY, directCb);
        } else if (currentY >= targetBottomY + 5f) {
            // Squeeze right below current content if vertical space is tight
            sigTable.writeSelectedRows(0, -1, document.left(), currentY - 5f, directCb);
        } else {
            // Insufficient vertical space on page without clipping, move to new page
            document.newPage();
            sigTable.writeSelectedRows(0, -1, document.left(), targetTopY, writer.getDirectContent());
        }

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

        // Reset margins back to standard 46pt for subsequent template pages
        document.setMargins(25, 25, 46f, 20);
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
        Font labelFont = new Font(Font.HELVETICA, 11.2f, Font.BOLD, COLOR_TEXT_DARK);
        Font valueFont = new Font(Font.HELVETICA, 10.8f, Font.NORMAL, COLOR_TEXT_DARK);

        PdfPCell lCell = new PdfPCell(new Phrase(label, labelFont));
        lCell.setBorder(Rectangle.NO_BORDER);
        lCell.setPaddingTop(3.2f);
        lCell.setPaddingBottom(3.2f);
        lCell.setPaddingLeft(0f);
        lCell.setPaddingRight(4f);
        lCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        lCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPCell vCell = new PdfPCell(new Phrase(safe(value), valueFont));
        vCell.setBorder(Rectangle.NO_BORDER);
        vCell.setPaddingTop(3.2f);
        vCell.setPaddingBottom(3.2f);
        vCell.setPaddingLeft(4f);
        vCell.setPaddingRight(0f);
        vCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        vCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        table.addCell(lCell);
        table.addCell(vCell);
    }

    private void addHeaderCell(PdfPTable table, String text, int alignment) {
        addHeaderCell(table, text, alignment, false);
    }

    private void addHeaderCell(PdfPTable table, String text, int alignment, boolean noWrap) {
        addHeaderCell(table, text, alignment, noWrap, 2.0f, 2.0f);
    }

    private void addHeaderCell(PdfPTable table, String text, int alignment, boolean noWrap, float paddingLeft, float paddingRight) {
        Font headerFont = new Font(Font.HELVETICA, 10.5f, Font.BOLD, COLOR_HEADER_TEXT);
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), headerFont));
        if (noWrap) {
            cell.setNoWrap(true);
        }
        cell.setBackgroundColor(COLOR_HEADER_BG);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(5.5f);
        cell.setPaddingBottom(5.5f);
        cell.setPaddingLeft(paddingLeft);
        cell.setPaddingRight(paddingRight);
        cell.setBorderColor(COLOR_HEADER_BG);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, int alignment, boolean isAltRow) {
        addBodyCell(table, text, alignment, isAltRow, false, 1, false);
    }

    private void addBodyCell(PdfPTable table, String text, int alignment, boolean isAltRow, boolean noWrap) {
        addBodyCell(table, text, alignment, isAltRow, noWrap, 1, false);
    }

    private void addBodyCell(PdfPTable table, String text, int alignment, boolean isAltRow, boolean noWrap, int rowspan) {
        addBodyCell(table, text, alignment, isAltRow, noWrap, rowspan, false);
    }

    private void addBodyCell(PdfPTable table, String text, int alignment, boolean isAltRow, boolean noWrap, int rowspan, boolean isBold) {
        Font bodyFont = isBold
                ? new Font(Font.HELVETICA, 10.0f, Font.BOLD, COLOR_TEXT_DARK)
                : new Font(Font.HELVETICA, 10.0f, Font.NORMAL, COLOR_TEXT_DARK);
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), bodyFont));
        if (noWrap) {
            cell.setNoWrap(true);
        }
        if (rowspan > 1) {
            cell.setRowspan(rowspan);
        }
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(3.5f);
        cell.setPaddingBottom(3.5f);
        cell.setPaddingLeft(2.5f);
        cell.setPaddingRight(2.5f);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
        cell.setBackgroundColor(Color.WHITE);
        table.addCell(cell);
    }

    private void addMaterialCell(PdfPTable table, String materialName, String specification, boolean isAltRow) {
        addMaterialCell(table, materialName, specification, null, isAltRow);
    }

    private void addMaterialCell(PdfPTable table, String materialName, String specification, String imageUrl, boolean isAltRow) {
        Font mainFont = new Font(Font.HELVETICA, 10.5f, Font.NORMAL, Color.BLACK);
        Font specFont = new Font(Font.HELVETICA, 10.0f, Font.NORMAL, Color.BLACK);

        PdfPCell cell = new PdfPCell();
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(3.5f);
        cell.setPaddingBottom(3.5f);
        cell.setPaddingLeft(3.0f);
        cell.setPaddingRight(3.0f);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
        cell.setBackgroundColor(Color.WHITE);

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
            namePar.setLeading(12.0f);
            namePar.setAlignment(Element.ALIGN_LEFT);
        }

        boolean hasSpec = (specification != null && !specification.isBlank() && !specification.equals("-"));
        Paragraph specPar = null;
        if (hasSpec) {
            specPar = new Paragraph(formatSpecificationForPdf(safe(specification)), specFont);
            specPar.setLeading(12.5f);
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
            fallback.setLeading(13.0f);
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
        addRupeeCell(table, text, bold, isGold, isAltRow, 2.0f, 4.0f);
    }

    private void addRupeeCell(PdfPTable table, String text, boolean bold, boolean isGold, boolean isAltRow, float paddingLeft, float paddingRight) {
        Font font;
        if (isGold) {
            font = rupeeGoldBoldFont;
        } else if (bold) {
            font = rupeeBoldFont;
        } else {
            font = rupeeFont;
        }

        PdfPCell cell = new PdfPCell(new Phrase(safe(text), font));
        cell.setNoWrap(true);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(4.0f);
        cell.setPaddingBottom(4.0f);
        cell.setPaddingLeft(paddingLeft);
        cell.setPaddingRight(paddingRight);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
        cell.setBackgroundColor(Color.WHITE);
        table.addCell(cell);
    }

    private void addFillerRow(PdfPTable table, boolean isAltRow) {
        int colCount = table.getNumberOfColumns();
        for (int i = 0; i < colCount; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(" ", rupeeFont));
            cell.setNoWrap(true);
            cell.setBorderColor(COLOR_BORDER);
            cell.setBorderWidth(0.5f);
            cell.setPadding(4.0f);
            cell.setBackgroundColor(Color.WHITE);
            table.addCell(cell);
        }
    }

    private PdfPTable createSectionTable(int numColumns, float[] columnWidths) {
        try {
            PdfPTable table = new PdfPTable(numColumns);
            table.setWidthPercentage(100);
            table.setWidths(columnWidths);
            table.setHeaderRows(0);
            table.setSpacingBefore(0f);
            table.setSpacingAfter(0f);
            return table;
        } catch (Exception e) {
            throw new RuntimeException("Error creating table: " + e.getMessage(), e);
        }
    }

    private void addTableHeaders(PdfPTable table, boolean showSize, boolean showSqft) {
        addHeaderCell(table, "#", Element.ALIGN_CENTER, true);
        addHeaderCell(table, "WORK TYPE", Element.ALIGN_LEFT, false);
        addHeaderCell(table, "MATERIALS", Element.ALIGN_LEFT, false);
        if (showSize) {
            addHeaderCell(table, "SIZE", Element.ALIGN_CENTER, false);
        }
        if (showSqft) {
            addHeaderCell(table, "SQ.FT", Element.ALIGN_CENTER, true);
        }
        addHeaderCell(table, "UNIT", Element.ALIGN_CENTER, true);
        addHeaderCell(table, "PRICE", Element.ALIGN_RIGHT, true, 2.0f, 10.0f);
        addHeaderCell(table, "OFFER PRICE", Element.ALIGN_RIGHT, true, 6.0f, 4.0f);
    }

    private void addFloorHeadingRow(PdfPTable table, String floorName) {
        Font floorFont;
        try {
            BaseFont arialBold = BaseFont.createFont("C:/Windows/Fonts/arialbd.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            floorFont = new Font(arialBold, 12.0f, Font.NORMAL, COLOR_GOLD);
        } catch (Exception e) {
            floorFont = new Font(Font.HELVETICA, 12.0f, Font.BOLD, COLOR_GOLD);
        }
        PdfPCell cell = new PdfPCell(new Phrase(formatSectionHeading(floorName), floorFont));
        cell.setColspan(table.getNumberOfColumns());
        cell.setBackgroundColor(Color.WHITE);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(7.0f);
        cell.setPaddingBottom(7.0f);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private void addCategoryHeadingRow(PdfPTable table, String categoryName) {
        Font catFont;
        try {
            BaseFont arialBold = BaseFont.createFont("C:/Windows/Fonts/arialbd.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            catFont = new Font(arialBold, 11.5f, Font.NORMAL, COLOR_TEXT_DARK);
        } catch (Exception e) {
            catFont = new Font(Font.HELVETICA, 11.5f, Font.BOLD, COLOR_TEXT_DARK);
        }
        PdfPCell cell = new PdfPCell(new Phrase(formatSectionHeading(categoryName), catFont));
        cell.setColspan(table.getNumberOfColumns());
        cell.setBackgroundColor(Color.WHITE);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(7.0f);
        cell.setPaddingBottom(7.0f);
        cell.setPaddingLeft(12.0f);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    public static String formatSectionHeading(String text) {
        if (text == null || text.isBlank()) {
            return "-";
        }
        String trimmed = text.trim();
        boolean isAllCaps = trimmed.equals(trimmed.toUpperCase(Locale.ROOT)) && !trimmed.equals(trimmed.toLowerCase(Locale.ROOT));
        if (!isAllCaps) {
            return trimmed;
        }
        String[] words = trimmed.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            if (!w.isEmpty()) {
                if (i > 0) sb.append(" ");
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) {
                    sb.append(w.substring(1).toLowerCase(Locale.ROOT));
                }
            }
        }
        return sb.toString();
    }

    private static class PdfItemData {
        final String workType;
        final String materialText;
        final String specification;
        final String imageUrl;
        final String sizeStr;
        final String sqftStr;
        final String unitStr;
        final BigDecimal price;
        final BigDecimal offerPrice;

        PdfItemData(String workType, String materialText, String specification, String imageUrl,
                    String sizeStr, String sqftStr, String unitStr, BigDecimal price, BigDecimal offerPrice) {
            this.workType = workType;
            this.materialText = materialText;
            this.specification = specification;
            this.imageUrl = imageUrl;
            this.sizeStr = sizeStr;
            this.sqftStr = sqftStr;
            this.unitStr = unitStr;
            this.price = price != null ? price : BigDecimal.ZERO;
            this.offerPrice = offerPrice != null ? offerPrice : this.price;
        }
    }

    private void addTotalRow(PdfPTable table, BigDecimal priceTotal, BigDecimal offerPriceTotal) {
        Font labelFont = new Font(Font.HELVETICA, 10.5f, Font.BOLD, COLOR_TEXT_DARK);

        int labelColspan = Math.max(1, table.getNumberOfColumns() - 2);
        PdfPCell labelCell = new PdfPCell(new Phrase("Total", labelFont));
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

        // Price Total under PRICE column (colspan 1)
        PdfPCell priceCell = new PdfPCell(new Phrase(rupees(priceTotal), rupeeBoldFont));
        priceCell.setNoWrap(true);
        priceCell.setColspan(1);
        priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        priceCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        priceCell.setPaddingTop(4.5f);
        priceCell.setPaddingBottom(4.5f);
        priceCell.setPaddingLeft(2.0f);
        priceCell.setPaddingRight(10.0f);
        priceCell.setBorderColor(COLOR_BORDER);
        priceCell.setBorderWidth(0.5f);
        priceCell.setBackgroundColor(COLOR_LIGHT_BG);
        table.addCell(priceCell);

        // Offer Price Total under OFFER PRICE column (colspan 1)
        PdfPCell offerCell = new PdfPCell(new Phrase(rupees(offerPriceTotal), rupeeBoldFont));
        offerCell.setNoWrap(true);
        offerCell.setColspan(1);
        offerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        offerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        offerCell.setPaddingTop(4.5f);
        offerCell.setPaddingBottom(4.5f);
        offerCell.setPaddingLeft(6.0f);
        offerCell.setPaddingRight(4.0f);
        offerCell.setBorderColor(COLOR_BORDER);
        offerCell.setBorderWidth(0.5f);
        offerCell.setBackgroundColor(COLOR_LIGHT_BG);
        table.addCell(offerCell);
    }

    private void addSummaryRow(PdfPTable table, String label, String amountText, boolean isBold) {
        Font labelFont = new Font(Font.HELVETICA, 10.5f, Font.BOLD, COLOR_TEXT_DARK);
        Font valFont = isBold ? rupeeBoldFont : rupeeFont;

        int labelColspan = Math.max(1, table.getNumberOfColumns() - 2);
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
        valCell.setNoWrap(true);
        valCell.setColspan(2);
        valCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        valCell.setPaddingTop(4.5f);
        valCell.setPaddingBottom(4.5f);
        valCell.setPaddingLeft(2.0f);
        valCell.setPaddingRight(4.0f);
        valCell.setBorderColor(COLOR_BORDER);
        valCell.setBorderWidth(0.5f);
        valCell.setBackgroundColor(COLOR_LIGHT_BG);
        table.addCell(valCell);
    }

    private void addGrandTotalRow(PdfPTable table, String label, BigDecimal grandTotal) {
        Font labelFont = new Font(Font.HELVETICA, 11.5f, Font.BOLD, COLOR_HEADER_TEXT);

        int labelColspan = Math.max(1, table.getNumberOfColumns() - 2);
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setColspan(labelColspan);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPaddingTop(6.0f);
        labelCell.setPaddingBottom(6.0f);
        labelCell.setPaddingRight(6f);
        labelCell.setBackgroundColor(COLOR_HEADER_BG);
        labelCell.setBorderColor(COLOR_HEADER_BG);
        table.addCell(labelCell);

        // GRAND TOTAL VALUE (Colspan 2)
        String amtText = rupees(grandTotal);
        PdfPCell totalCell = new PdfPCell(new Phrase(amtText, rupeeGoldBoldFont));
        totalCell.setNoWrap(true);
        totalCell.setColspan(2);
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        totalCell.setPaddingTop(6.0f);
        totalCell.setPaddingBottom(6.0f);
        totalCell.setPaddingLeft(2.0f);
        totalCell.setPaddingRight(4.0f);
        totalCell.setBackgroundColor(COLOR_HEADER_BG);
        totalCell.setBorderColor(COLOR_HEADER_BG);
        table.addCell(totalCell);
    }

    private Paragraph buildBullet(String prefix, String text, Font prefixFont, Font textFont) {
        Paragraph p = new Paragraph();
        p.setLeading(13.5f);
        p.add(new Chunk(prefix, prefixFont));
        p.add(new Chunk(text, textFont));
        return p;
    }

    private String resolveItemFormula(QuotationItem item) {
        if (item == null) return CalculationFormula.FORMULA_L_X_W;
        if (item.getFormula() != null && !item.getFormula().isBlank()) {
            return item.getFormula();
        }
        if (item.getMaterial() != null && item.getMaterial().getFormula() != null && !item.getMaterial().getFormula().isBlank()) {
            return item.getMaterial().getFormula();
        }
        if (item.getMaterialOption() != null && item.getMaterialOption().getFormula() != null && !item.getMaterialOption().getFormula().isBlank()) {
            return item.getMaterialOption().getFormula();
        }
        return CalculationFormula.FORMULA_L_PLUS_W_X_H;
    }

    private String resolveUnit(QuotationItem item, PricingMode pricingMode) {
        if (item.getMeasurementType() == MeasurementType.ROLL || (item.getMaterial() != null && item.getMaterial().getMeasurementType() == MeasurementType.ROLL)) {
            return "ROLL";
        }
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
                    case "ROLL":
                    case "ROLLS":
                        return "ROLL";
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
        String formatted = com.example.pravin_quotation.util.IndianCurrencyFormatter.formatBigDecimal(amount);
        return (hasUnicodeRupee ? "₹\u00A0" : "Rs.\u00A0") + formatted;
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

    public String resolveQuotationType(Quotation quotation) {
        if (quotation == null || quotation.getId() == null) {
            return formatSingleGrade(quotation != null ? quotation.getPricingMode() : null);
        }
        List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationId(quotation.getId());
        Set<PricingMode> usedGrades = new LinkedHashSet<>();
        if (rooms != null) {
            for (QuotationRoom room : rooms) {
                if (room == null || Boolean.FALSE.equals(room.getActive())) {
                    continue;
                }
                List<QuotationItem> items = quotationItemRepository.findByQuotationRoomId(room.getId());
                if (items != null) {
                    for (QuotationItem item : items) {
                        if (item != null && Boolean.TRUE.equals(item.getActive())) {
                            PricingMode mode = item.getEffectivePricingMode();
                            if (mode != null) {
                                usedGrades.add(mode);
                            }
                        }
                    }
                }
            }
        }

        if (usedGrades.isEmpty()) {
            return formatSingleGrade(quotation.getPricingMode());
        }

        List<String> gradeNames = new ArrayList<>();
        if (usedGrades.contains(PricingMode.PREMIUM)) {
            gradeNames.add("Premium");
        }
        if (usedGrades.contains(PricingMode.MIDDLE)) {
            gradeNames.add(usedGrades.size() > 1 ? "Middle" : "Middle / Standard");
        }
        if (usedGrades.contains(PricingMode.ECONOMY)) {
            gradeNames.add("Economy");
        }

        return gradeNames.isEmpty() ? formatSingleGrade(quotation.getPricingMode()) : String.join(" + ", gradeNames);
    }

    private static String formatSingleGrade(PricingMode mode) {
        if (mode == null) return "-";
        switch (mode) {
            case PREMIUM: return "Premium";
            case MIDDLE: return "Middle / Standard";
            case ECONOMY: return "Economy";
            default: return mode.name();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}