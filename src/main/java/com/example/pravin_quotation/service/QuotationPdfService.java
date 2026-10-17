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

import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class QuotationPdfService {

    private final QuotationRepository quotationRepository;
    private final QuotationRoomRepository quotationRoomRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final NumberToWordsService numberToWordsService;
    private final BranchRepository branchRepository;
    private final PricingRepository pricingRepository;

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

    /*
     * Fonts for Rupee symbol and typography
     */
    private Font rupeeFont;
    private Font rupeeBoldFont;
    private Font rupeeGoldBoldFont;
    private boolean hasUnicodeRupee = false;

    public QuotationPdfService(
            QuotationRepository quotationRepository,
            QuotationRoomRepository quotationRoomRepository,
            QuotationItemRepository quotationItemRepository,
            NumberToWordsService numberToWordsService,
            BranchRepository branchRepository,
            PricingRepository pricingRepository
    ) {
        this.quotationRepository = quotationRepository;
        this.quotationRoomRepository = quotationRoomRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.numberToWordsService = numberToWordsService;
        this.branchRepository = branchRepository;
        this.pricingRepository = pricingRepository;

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

    // ============================================================
    // GENERATE 7-PAGE REFERENCE-BASED QUOTATION PDF
    // ============================================================

    public byte[] generateQuotationPdf(Long quotationId) {
        try {
            Quotation quotation = quotationRepository.findById(quotationId)
                    .orElseThrow(() -> new RuntimeException("Quotation not found with ID: " + quotationId));

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
                ? quotation.getQuotationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "-";
        addMetaRow(metaTable, "Date:", dateStr);

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
        // 3. 10-COLUMN QUOTE SUMMARY TABLE
        // ========================================================
        PdfPTable itemTable = new PdfPTable(10);
        itemTable.setWidthPercentage(100);
        itemTable.setHeaderRows(1);
        itemTable.setWidths(new float[]{3.0f, 15.0f, 12.0f, 9.0f, 19.0f, 9.0f, 6.5f, 5.5f, 10.5f, 10.5f});
        itemTable.setSpacingAfter(5f);

        // --- TABLE HEADERS ---
        addHeaderCell(itemTable, "#", Element.ALIGN_CENTER);
        addHeaderCell(itemTable, "WORK CATEGORY", Element.ALIGN_LEFT);
        addHeaderCell(itemTable, "ROOM", Element.ALIGN_LEFT);
        addHeaderCell(itemTable, "FLOOR", Element.ALIGN_CENTER);
        addHeaderCell(itemTable, "MATERIAL", Element.ALIGN_LEFT);
        addHeaderCell(itemTable, "SIZE", Element.ALIGN_CENTER);
        addHeaderCell(itemTable, "SQ.FT", Element.ALIGN_RIGHT);
        addHeaderCell(itemTable, "UNIT", Element.ALIGN_CENTER);
        addHeaderCell(itemTable, "TOTAL AMOUNT", Element.ALIGN_RIGHT);
        addHeaderCell(itemTable, "OFFER PRICE", Element.ALIGN_RIGHT);

        // --- QUOTATION ROOMS & ITEMS ---
        List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationId(quotation.getId());
        int serialNumber = 1;
        int activeItemCount = 0;
        boolean isAltRow = false;

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

                // Material & Option
                String materialText = item.getMaterial() != null ? safe(item.getMaterial().getName()) : "";
                if (item.getMaterialOption() != null) {
                    String optionName = safe(item.getMaterialOption().getName());
                    if (!optionName.isBlank()) {
                        materialText = !materialText.isBlank() ? materialText + " - " + optionName : optionName;
                    }
                }
                if (materialText.isBlank()) {
                    materialText = "-";
                }

                // Specifications
                String specification = safe(item.getSpecification());
                if (specification.isBlank() && item.getMaterialSpecification() != null) {
                    specification = safe(item.getMaterialSpecification().getName());
                }

                // Size
                String sizeStr = buildSize(item.getLengthValue(), item.getWidthValue(), item.getHeightValue());

                // Sq.Ft
                BigDecimal sqft = item.getCalculatedSqft() != null ? item.getCalculatedSqft() : BigDecimal.ZERO;

                // Unit
                String unitStr = resolveUnit(item, quotation.getPricingMode());

                // Amounts
                BigDecimal rate = item.getRate() != null ? item.getRate() : BigDecimal.ZERO;
                BigDecimal amount = item.getAmount() != null ? item.getAmount() : rate.multiply(sqft);
                BigDecimal offerPrice = item.getOfferPrice();
                if (offerPrice == null || offerPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    offerPrice = amount;
                }

                // Add Row Cells
                addBodyCell(itemTable, String.valueOf(serialNumber++), Element.ALIGN_CENTER, isAltRow);
                addBodyCell(itemTable, workCat, Element.ALIGN_LEFT, isAltRow);
                addBodyCell(itemTable, roomName, Element.ALIGN_LEFT, isAltRow);
                addBodyCell(itemTable, floorName, Element.ALIGN_CENTER, isAltRow);
                addMaterialCell(itemTable, materialText.toUpperCase(), specification != null ? specification.toUpperCase() : "", isAltRow);
                addBodyCell(itemTable, sizeStr, Element.ALIGN_CENTER, isAltRow);
                addBodyCell(itemTable, format(sqft), Element.ALIGN_RIGHT, isAltRow);
                addBodyCell(itemTable, unitStr, Element.ALIGN_CENTER, isAltRow);
                addRupeeCell(itemTable, rupees(amount), false, false, isAltRow);
                addRupeeCell(itemTable, rupees(offerPrice), true, false, isAltRow);

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

        // --- FINANCIAL VALUES ---
        BigDecimal subtotal = zeroIfNull(quotation.getSubtotal());
        BigDecimal accessories = zeroIfNull(quotation.getAccessoriesAmount());
        BigDecimal travel = zeroIfNull(quotation.getTravelCharge());
        BigDecimal otherCharges = zeroIfNull(quotation.getOtherCharges());
        BigDecimal discount = zeroIfNull(quotation.getDiscountAmount());
        BigDecimal taxable = zeroIfNull(quotation.getTaxableAmount());
        BigDecimal gstPercentage = zeroIfNull(quotation.getGstPercentage());
        BigDecimal gst = zeroIfNull(quotation.getGstAmount());
        BigDecimal totalAmount = quotation.getTotalAmount() != null && quotation.getTotalAmount().compareTo(BigDecimal.ZERO) > 0
                ? quotation.getTotalAmount()
                : taxable.add(gst);
        BigDecimal offerPercentage = zeroIfNull(quotation.getOfferPercentage());
        BigDecimal offerAmount = zeroIfNull(quotation.getOfferAmount());
        BigDecimal grandTotal = zeroIfNull(quotation.getGrandTotal());

        // --- SUMMARY ROWS INTEGRATED AT TABLE FOOTER ---
        if (accessories.compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(itemTable, "Accessories", accessories, accessories, false);
        }
        if (travel.compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(itemTable, "Travel Charge", travel, travel, false);
        }
        if (otherCharges.compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(itemTable, "Other Charges", otherCharges, otherCharges, false);
        }
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(itemTable, "Discount", discount.negate(), discount.negate(), false);
        }
        if (gst.compareTo(BigDecimal.ZERO) > 0 || discount.compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(itemTable, "Taxable Amount", taxable, taxable, false);
        }
        if (gst.compareTo(BigDecimal.ZERO) > 0) {
            String gstLabel = "GST (" + format(gstPercentage) + "%)";
            addSummaryRow(itemTable, gstLabel, gst, gst, false);
        }

        // Total Amount Row
        addSummaryRow(itemTable, "Total Amount", totalAmount, totalAmount, false);

        // Special Offer Row
        if (offerAmount.compareTo(BigDecimal.ZERO) > 0 || offerPercentage.compareTo(BigDecimal.ZERO) > 0) {
            String offerLabel = "Special Offer (" + format(offerPercentage) + "%)";
            addSummaryRow(itemTable, offerLabel, offerAmount.negate(), offerAmount.negate(), false);
        }

        // Grand Total Row (Highlighted with dark green header fill and gold text)
        addGrandTotalRow(itemTable, "GRAND TOTAL", grandTotal);

        document.add(itemTable);

        // ========================================================
        // 4. AMOUNT IN WORDS
        // ========================================================
        Paragraph wordsPar = new Paragraph();
        wordsPar.setSpacingBefore(2f);
        wordsPar.setSpacingAfter(4f);
        wordsPar.add(new Chunk("AMOUNT IN WORDS - ", new Font(Font.HELVETICA, 8.0f, Font.BOLD, COLOR_NAVY)));
        String wordsText = numberToWordsService.convertRupees(grandTotal).toUpperCase();
        wordsPar.add(new Chunk(wordsText, new Font(Font.HELVETICA, 8.0f, Font.BOLD, COLOR_TEXT_DARK)));
        document.add(wordsPar);

        // ========================================================
        // 5. PAYMENT TERMS BOX
        // ========================================================
        PdfPTable termsTable = new PdfPTable(1);
        termsTable.setWidthPercentage(100);
        termsTable.setSpacingBefore(4f);
        termsTable.setSpacingAfter(6f);

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

        termsCell.addElement(buildBullet("-  10% Token: ", "Commencement of 3D design on receipt of token.", bulletTitleFont, bulletTextFont));
        termsCell.addElement(buildBullet("-  50% Advance: ", "Drawing will be sent for production on receipt of advance.", bulletTitleFont, bulletTextFont));
        termsCell.addElement(buildBullet("-  35% Part Payment: ", "After the completion of frame work.", bulletTitleFont, bulletTextFont));
        termsCell.addElement(buildBullet("-  5% Final Payment: ", "The last day of work completion in site.", bulletTitleFont, bulletTextFont));

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
        Font mainFont = new Font(Font.HELVETICA, 6.8f, Font.BOLD, COLOR_NAVY);
        Font specFont = new Font(Font.HELVETICA, 6.0f, Font.NORMAL, COLOR_TEXT_MUTED);

        Paragraph p = new Paragraph();
        p.setLeading(8.5f);
        p.add(new Chunk(safe(materialName).toUpperCase(), mainFont));
        if (specification != null && !specification.isBlank() && !specification.equals("-")) {
            p.add(new Chunk("\n" + safe(specification).toUpperCase(), specFont));
        }

        PdfPCell cell = new PdfPCell(p);
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
        table.addCell(cell);
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
        for (int i = 0; i < 10; i++) {
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

    private void addSummaryRow(PdfPTable table, String label, BigDecimal amountVal, BigDecimal offerVal, boolean isTotal) {
        Font labelFont = new Font(Font.HELVETICA, 7.5f, Font.BOLD, COLOR_TEXT_DARK);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setColspan(8);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPaddingTop(4f);
        labelCell.setPaddingBottom(4f);
        labelCell.setPaddingRight(6f);
        labelCell.setBorderColor(COLOR_BORDER);
        labelCell.setBorderWidth(0.5f);
        labelCell.setBackgroundColor(COLOR_LIGHT_BG);
        table.addCell(labelCell);

        // AMOUNT
        String amtText = amountVal != null ? rupees(amountVal) : "-";
        PdfPCell amtCell = new PdfPCell(new Phrase(amtText, rupeeFont));
        amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amtCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        amtCell.setPaddingTop(4f);
        amtCell.setPaddingBottom(4f);
        amtCell.setPaddingRight(2.5f);
        amtCell.setBorderColor(COLOR_BORDER);
        amtCell.setBorderWidth(0.5f);
        amtCell.setBackgroundColor(COLOR_LIGHT_BG);
        table.addCell(amtCell);

        // OFFER PRICE
        String offerText = offerVal != null ? rupees(offerVal) : "-";
        PdfPCell offerCell = new PdfPCell(new Phrase(offerText, rupeeBoldFont));
        offerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        offerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        offerCell.setPaddingTop(4f);
        offerCell.setPaddingBottom(4f);
        offerCell.setPaddingRight(2.5f);
        offerCell.setBorderColor(COLOR_BORDER);
        offerCell.setBorderWidth(0.5f);
        offerCell.setBackgroundColor(COLOR_LIGHT_BG);
        table.addCell(offerCell);
    }

    private void addGrandTotalRow(PdfPTable table, String label, BigDecimal grandTotal) {
        Font labelFont = new Font(Font.HELVETICA, 8.5f, Font.BOLD, COLOR_HEADER_TEXT);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setColspan(8);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPaddingTop(5f);
        labelCell.setPaddingBottom(5f);
        labelCell.setPaddingRight(6f);
        labelCell.setBackgroundColor(COLOR_HEADER_BG);
        labelCell.setBorderColor(COLOR_HEADER_BG);
        table.addCell(labelCell);

        // GRAND TOTAL VALUE (Colspan 2)
        String amtText = rupees(grandTotal);
        PdfPCell totalCell = new PdfPCell(new Phrase(amtText, rupeeGoldBoldFont));
        totalCell.setColspan(2);
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
                        return "NOS";
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