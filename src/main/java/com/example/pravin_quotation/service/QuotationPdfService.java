package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationItem;
import com.example.pravin_quotation.model.QuotationRoom;
import com.example.pravin_quotation.repository.QuotationItemRepository;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.repository.QuotationRoomRepository;

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

@Service
public class QuotationPdfService {

    private final QuotationRepository quotationRepository;
    private final QuotationRoomRepository quotationRoomRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final NumberToWordsService numberToWordsService;

    /*
     * Design Colors matching the Reference Document
     */
    private static final Color COLOR_NAVY = new Color(27, 54, 93);       // #1B365D Primary title
    private static final Color COLOR_HEADER_BG = new Color(19, 43, 39);   // #132B27 Dark Header fill
    private static final Color COLOR_GOLD = new Color(168, 123, 46);      // #A87B2E Warm Gold accent
    private static final Color COLOR_LIGHT_BG = new Color(248, 250, 250); // #F8FAFA Summary row fill
    private static final Color COLOR_ROW_ALT = new Color(245, 247, 247);  // #F5F7F7 Alternate table row fill
    private static final Color COLOR_BORDER = new Color(210, 215, 215);   // #D2D7D7 Thin table borders
    private static final Color COLOR_TEXT_DARK = new Color(30, 30, 30);   // #1E1E1E Body text
    private static final Color COLOR_TEXT_MUTED = new Color(90, 90, 90);  // #5A5A5A Subtitle & spec text

    /*
     * Fonts for Rupee ₹ symbol & text
     */
    private Font rupeeFont;
    private Font rupeeBoldFont;
    private Font rupeeGoldBoldFont;

    public QuotationPdfService(
            QuotationRepository quotationRepository,
            QuotationRoomRepository quotationRoomRepository,
            QuotationItemRepository quotationItemRepository,
            NumberToWordsService numberToWordsService
    ) {
        this.quotationRepository = quotationRepository;
        this.quotationRoomRepository = quotationRoomRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.numberToWordsService = numberToWordsService;

        try {
            BaseFont rupeeBaseFont = BaseFont.createFont(
                    "C:/Windows/Fonts/arial.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
            );
            rupeeFont = new Font(rupeeBaseFont, 7.8f, Font.NORMAL, COLOR_TEXT_DARK);
            rupeeBoldFont = new Font(rupeeBaseFont, 8.2f, Font.BOLD, COLOR_TEXT_DARK);
            rupeeGoldBoldFont = new Font(rupeeBaseFont, 8.5f, Font.BOLD, COLOR_GOLD);
        } catch (Exception e) {
            rupeeFont = new Font(Font.HELVETICA, 7.8f, Font.NORMAL, COLOR_TEXT_DARK);
            rupeeBoldFont = new Font(Font.HELVETICA, 8.2f, Font.BOLD, COLOR_TEXT_DARK);
            rupeeGoldBoldFont = new Font(Font.HELVETICA, 8.5f, Font.BOLD, COLOR_GOLD);
        }
    }

    // ============================================================
    // GENERATE QUOTATION PDF
    // ============================================================

    public byte[] generateQuotationPdf(Long quotationId) {
        try {
            Quotation quotation = quotationRepository.findById(quotationId)
                    .orElseThrow(() -> new RuntimeException("Quotation not found with ID: " + quotationId));

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 25, 25, 25, 25);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // ========================================================
            // 1. TOP HEADER TABLE (BRAND & ADDRESS ON LEFT, QUOTE INFO ON RIGHT)
            // ========================================================
            PdfPTable topHeaderTable = new PdfPTable(2);
            topHeaderTable.setWidthPercentage(100);
            topHeaderTable.setWidths(new float[]{52f, 48f});

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
                    logoImg.scaleToFit(48, 48);
                    logoCell.addElement(logoImg);
                }
            } catch (Exception ignored) {}

            PdfPCell nameCell = new PdfPCell();
            nameCell.setBorder(Rectangle.NO_BORDER);
            nameCell.setPaddingLeft(6f);
            nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            Paragraph companyName = new Paragraph("Pravin Kitchen & Interiors", new Font(Font.HELVETICA, 13f, Font.BOLD, COLOR_GOLD));
            nameCell.addElement(companyName);

            brandTable.addCell(logoCell);
            brandTable.addCell(nameCell);
            leftCell.addElement(brandTable);

            Paragraph addressPar = new Paragraph();
            addressPar.setSpacingBefore(5f);
            addressPar.setFont(new Font(Font.HELVETICA, 7.8f, Font.NORMAL, COLOR_TEXT_MUTED));
            addressPar.add("Address: Pravin Modular Kitchen,\n");
            addressPar.add("Tvm Main Road, Azhagiamandapam,\n");
            addressPar.add("Mulagumoodu Post - 629167, T.N.\n");
            addressPar.add("Mobile: +91 9787769970");
            leftCell.addElement(addressPar);

            topHeaderTable.addCell(leftCell);

            // --- RIGHT COLUMN: TITLE & QUOTATION METADATA ---
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setPadding(0);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            String titleText = "MARKETING QUOTATION";
            if (quotation.getDivision() != null && quotation.getDivision().getName() != null && !quotation.getDivision().getName().isBlank()) {
                titleText = quotation.getDivision().getName().toUpperCase() + " QUOTATION";
            }

            Paragraph titlePar = new Paragraph(titleText, new Font(Font.HELVETICA, 14f, Font.BOLD, COLOR_NAVY));
            titlePar.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(titlePar);

            LineSeparator titleLine = new LineSeparator(1.5f, 100, COLOR_GOLD, Element.ALIGN_RIGHT, -2);
            rightCell.addElement(titleLine);

            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setWidths(new float[]{40f, 60f});
            metaTable.setSpacingBefore(6f);

            addMetaRow(metaTable, "Quotation No:", safe(quotation.getQuotationNumber()));

            String divName = quotation.getDivision() != null ? safe(quotation.getDivision().getName()) : "Marketing";
            addMetaRow(metaTable, "Division:", divName);

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
            addMetaRow(metaTable, "Customer:", custName.isBlank() ? "—" : custName);

            String custAddr = "—";
            if (quotation.getCustomer() != null) {
                if (quotation.getCustomer().getAddress() != null && !quotation.getCustomer().getAddress().isBlank()) {
                    custAddr = quotation.getCustomer().getAddress();
                } else if (quotation.getBranch() != null && quotation.getBranch().getName() != null) {
                    custAddr = quotation.getBranch().getName();
                }
            }
            addMetaRow(metaTable, "Address:", custAddr);

            rightCell.addElement(metaTable);
            topHeaderTable.addCell(rightCell);

            document.add(topHeaderTable);

            // Full-width Accent Line
            LineSeparator headerDivider = new LineSeparator(1.5f, 100, COLOR_GOLD, Element.ALIGN_CENTER, -4);
            headerDivider.setLineWidth(1.5f);
            document.add(headerDivider);

            document.add(new Paragraph(" "));

            // ========================================================
            // 2. SECTION TITLE: QUOTE SUMMARY
            // ========================================================
            Paragraph summaryTitle = new Paragraph("Quote Summary", new Font(Font.HELVETICA, 11f, Font.BOLD, COLOR_NAVY));
            summaryTitle.setSpacingAfter(6f);
            document.add(summaryTitle);

            // ========================================================
            // 3. QUOTE SUMMARY TABLE
            // ========================================================
            PdfPTable itemTable = new PdfPTable(7);
            itemTable.setWidthPercentage(100);
            itemTable.setHeaderRows(1); // Repeat header on subsequent pages!
            itemTable.setWidths(new float[]{4f, 16f, 37f, 15f, 7f, 10.5f, 10.5f});
            itemTable.setSpacingAfter(8f);

            // --- TABLE HEADERS ---
            addHeaderCell(itemTable, "#");
            addHeaderCell(itemTable, "ROOM");
            addHeaderCell(itemTable, "MATERIAL");
            addHeaderCell(itemTable, "SIZE");
            addHeaderCell(itemTable, "SQ.FT");
            addHeaderCell(itemTable, "AMOUNT");
            addHeaderCell(itemTable, "OFFER PRICE");

            // --- QUOTATION ROOMS & ITEMS ---
            List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationId(quotation.getId());
            int serialNumber = 1;
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

                    // Room & Work Name
                    String roomName = safe(room.getRoom());
                    String workName = item.getWorkCategory() != null ? safe(item.getWorkCategory().getName()) : "";
                    String roomWork = !workName.isBlank() ? roomName + "\n" + workName : roomName;

                    // Material & Option
                    String materialText = item.getMaterial() != null ? safe(item.getMaterial().getName()) : "";
                    if (item.getMaterialOption() != null) {
                        String optionName = safe(item.getMaterialOption().getName());
                        if (!optionName.isBlank()) {
                            materialText = !materialText.isBlank() ? materialText + " – " + optionName : optionName;
                        }
                    }
                    if (materialText.isBlank()) {
                        materialText = "-";
                    }

                    // Specifications
                    String specification = safe(item.getSpecification());

                    // Size
                    String sizeStr = buildSize(item.getLengthValue(), item.getWidthValue(), item.getHeightValue());

                    // Sq.Ft
                    BigDecimal sqft = item.getCalculatedSqft() != null ? item.getCalculatedSqft() : BigDecimal.ZERO;

                    // Amounts
                    BigDecimal rate = item.getRate() != null ? item.getRate() : BigDecimal.ZERO;
                    BigDecimal amount = item.getAmount() != null ? item.getAmount() : rate.multiply(sqft);
                    BigDecimal offerPrice = item.getOfferPrice();
                    if (offerPrice == null || offerPrice.compareTo(BigDecimal.ZERO) <= 0) {
                        offerPrice = amount;
                    }

                    // Add Row Cells
                    addBodyCell(itemTable, String.valueOf(serialNumber++), Element.ALIGN_CENTER, isAltRow);
                    addBodyCell(itemTable, roomWork.toUpperCase(), Element.ALIGN_LEFT, isAltRow);
                    addMaterialCell(itemTable, materialText, specification, isAltRow);
                    addBodyCell(itemTable, sizeStr, Element.ALIGN_CENTER, isAltRow);
                    addBodyCell(itemTable, format(sqft), Element.ALIGN_RIGHT, isAltRow);
                    addRupeeCell(itemTable, rupees(amount), false, false, isAltRow);
                    addRupeeCell(itemTable, rupees(offerPrice), true, true, isAltRow);

                    isAltRow = !isAltRow;
                }
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
            BigDecimal grandTotal = zeroIfNull(quotation.getGrandTotal());

            // --- SUMMARY ROWS INTEGRATED AT TABLE FOOTER ---
            addSummaryRow(itemTable, "Rooms Subtotal", subtotal, subtotal, false);

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

            // Grand Total Row
            addSummaryRow(itemTable, "Grand Total (Rooms + Accessories)", grandTotal, grandTotal, true);

            document.add(itemTable);

            // ========================================================
            // 4. AMOUNT IN WORDS
            // ========================================================
            Paragraph wordsPar = new Paragraph();
            wordsPar.setSpacingBefore(4f);
            wordsPar.setSpacingAfter(8f);
            wordsPar.add(new Chunk("Amount in Words: ", new Font(Font.HELVETICA, 8.5f, Font.BOLD, COLOR_TEXT_DARK)));
            wordsPar.add(new Chunk(numberToWordsService.convertRupees(grandTotal), new Font(Font.HELVETICA, 8.5f, Font.ITALIC, COLOR_TEXT_DARK)));
            document.add(wordsPar);

            // ========================================================
            // 5. PAYMENT TERMS BOX
            // ========================================================
            PdfPTable termsTable = new PdfPTable(1);
            termsTable.setWidthPercentage(100);
            termsTable.setSpacingBefore(6f);
            termsTable.setSpacingAfter(10f);

            PdfPCell termsCell = new PdfPCell();
            termsCell.setBackgroundColor(COLOR_LIGHT_BG);
            termsCell.setBorderColor(COLOR_BORDER);
            termsCell.setBorderWidth(0.8f);
            termsCell.setPadding(8f);

            Paragraph termsHeading = new Paragraph("Payment Terms:", new Font(Font.HELVETICA, 8.8f, Font.BOLD, COLOR_GOLD));
            termsHeading.setSpacingAfter(4f);
            termsCell.addElement(termsHeading);

            Font bulletTitleFont = new Font(Font.HELVETICA, 8f, Font.BOLD, COLOR_TEXT_DARK);
            Font bulletTextFont = new Font(Font.HELVETICA, 8f, Font.NORMAL, COLOR_TEXT_DARK);

            Paragraph b1 = new Paragraph();
            b1.setLeading(10.5f);
            b1.add(new Chunk("▪  10% Token: ", bulletTitleFont));
            b1.add(new Chunk("Commencement of 3D design on receipt of token.", bulletTextFont));
            termsCell.addElement(b1);

            Paragraph b2 = new Paragraph();
            b2.setLeading(10.5f);
            b2.add(new Chunk("▪  50% Advance: ", bulletTitleFont));
            b2.add(new Chunk("Drawing will be sent for production on receipt of advance.", bulletTextFont));
            termsCell.addElement(b2);

            Paragraph b3 = new Paragraph();
            b3.setLeading(10.5f);
            b3.add(new Chunk("▪  35% Part Payment: ", bulletTitleFont));
            b3.add(new Chunk("After the completion of frame work.", bulletTextFont));
            termsCell.addElement(b3);

            Paragraph b4 = new Paragraph();
            b4.setLeading(10.5f);
            b4.add(new Chunk("▪  5% Final Payment: ", bulletTitleFont));
            b4.add(new Chunk("The last day of work completion in site.", bulletTextFont));
            termsCell.addElement(b4);

            termsTable.addCell(termsCell);
            document.add(termsTable);

            // ========================================================
            // 6. CUSTOMER REQUIREMENTS & NOTES
            // ========================================================
            if (quotation.getCustomerRequirements() != null && !quotation.getCustomerRequirements().isBlank()) {
                Paragraph reqHeading = new Paragraph("Customer Requirements", new Font(Font.HELVETICA, 9f, Font.BOLD, COLOR_NAVY));
                reqHeading.setSpacingBefore(4f);
                document.add(reqHeading);
                Paragraph reqText = new Paragraph(quotation.getCustomerRequirements(), new Font(Font.HELVETICA, 8f, Font.NORMAL, COLOR_TEXT_DARK));
                reqText.setSpacingAfter(6f);
                document.add(reqText);
            }

            if (quotation.getNotes() != null && !quotation.getNotes().isBlank()) {
                Paragraph notesHeading = new Paragraph("Notes & Special Instructions", new Font(Font.HELVETICA, 9f, Font.BOLD, COLOR_NAVY));
                notesHeading.setSpacingBefore(4f);
                document.add(notesHeading);
                Paragraph notesText = new Paragraph(quotation.getNotes(), new Font(Font.HELVETICA, 8f, Font.NORMAL, COLOR_TEXT_DARK));
                notesText.setSpacingAfter(6f);
                document.add(notesText);
            }

            // ========================================================
            // 7. SIGNATURES BLOCK
            // ========================================================
            document.add(new Paragraph(" "));
            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(100);
            sigTable.setWidths(new float[]{50f, 50f});
            sigTable.setSpacingBefore(15f);

            Font sigFont = new Font(Font.HELVETICA, 8.5f, Font.BOLD, COLOR_TEXT_DARK);

            PdfPCell sig1 = new PdfPCell(new Paragraph("\n\n________________________\nCustomer Signature", sigFont));
            sig1.setBorder(Rectangle.NO_BORDER);
            sig1.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell sig2 = new PdfPCell(new Paragraph("\n\n________________________\nFor Pravin KITCHENS & INTERIORS", sigFont));
            sig2.setBorder(Rectangle.NO_BORDER);
            sig2.setHorizontalAlignment(Element.ALIGN_CENTER);

            sigTable.addCell(sig1);
            sigTable.addCell(sig2);
            document.add(sigTable);

            // Close Document
            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private void addMetaRow(PdfPTable table, String label, String value) {
        Font labelFont = new Font(Font.HELVETICA, 7.8f, Font.BOLD, COLOR_TEXT_DARK);
        Font valueFont = new Font(Font.HELVETICA, 7.8f, Font.NORMAL, COLOR_TEXT_DARK);

        PdfPCell lCell = new PdfPCell(new Phrase(label, labelFont));
        lCell.setBorder(Rectangle.NO_BORDER);
        lCell.setPadding(1.5f);
        lCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell vCell = new PdfPCell(new Phrase(safe(value), valueFont));
        vCell.setBorder(Rectangle.NO_BORDER);
        vCell.setPadding(1.5f);
        vCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        table.addCell(lCell);
        table.addCell(vCell);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font headerFont = new Font(Font.HELVETICA, 7.5f, Font.BOLD, COLOR_GOLD);
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), headerFont));
        cell.setBackgroundColor(COLOR_HEADER_BG);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(5f);
        cell.setPaddingBottom(5f);
        cell.setPaddingLeft(3f);
        cell.setPaddingRight(3f);
        cell.setBorderColor(COLOR_HEADER_BG);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, int alignment, boolean isAltRow) {
        Font bodyFont = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, COLOR_TEXT_DARK);
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), bodyFont));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(4f);
        cell.setPaddingLeft(3f);
        cell.setPaddingRight(3f);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
        if (isAltRow) {
            cell.setBackgroundColor(COLOR_ROW_ALT);
        }
        table.addCell(cell);
    }

    private void addMaterialCell(PdfPTable table, String materialName, String specification, boolean isAltRow) {
        Font mainFont = new Font(Font.HELVETICA, 7.8f, Font.BOLD, COLOR_HEADER_BG);
        Font specFont = new Font(Font.HELVETICA, 6.8f, Font.NORMAL, COLOR_TEXT_MUTED);

        Paragraph p = new Paragraph();
        p.setLeading(9f);
        p.add(new Chunk(safe(materialName), mainFont));
        if (specification != null && !specification.isBlank() && !specification.equals("-")) {
            p.add(new Chunk("\n" + specification, specFont));
        }

        PdfPCell cell = new PdfPCell(p);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(4f);
        cell.setPaddingLeft(4f);
        cell.setPaddingRight(4f);
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
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(4f);
        cell.setPaddingLeft(3f);
        cell.setPaddingRight(3f);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
        if (isAltRow) {
            cell.setBackgroundColor(COLOR_ROW_ALT);
        }
        table.addCell(cell);
    }

    private void addSummaryRow(PdfPTable table, String label, BigDecimal amountVal, BigDecimal offerVal, boolean isGrandTotal) {
        Font labelFont = isGrandTotal
                ? new Font(Font.HELVETICA, 8.5f, Font.BOLD, COLOR_TEXT_DARK)
                : new Font(Font.HELVETICA, 8f, Font.BOLD, COLOR_TEXT_DARK);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setColspan(5);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPaddingTop(5f);
        labelCell.setPaddingBottom(5f);
        labelCell.setPaddingRight(8f);
        labelCell.setBorderColor(COLOR_BORDER);
        labelCell.setBorderWidth(0.5f);
        labelCell.setBackgroundColor(isGrandTotal ? COLOR_LIGHT_BG : COLOR_ROW_ALT);
        table.addCell(labelCell);

        // AMOUNT
        String amtText = amountVal != null ? rupees(amountVal) : "-";
        Font amtFont = isGrandTotal ? rupeeBoldFont : rupeeFont;
        PdfPCell amtCell = new PdfPCell(new Phrase(amtText, amtFont));
        amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amtCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        amtCell.setPaddingTop(5f);
        amtCell.setPaddingBottom(5f);
        amtCell.setPaddingRight(3f);
        amtCell.setBorderColor(COLOR_BORDER);
        amtCell.setBorderWidth(0.5f);
        amtCell.setBackgroundColor(isGrandTotal ? COLOR_LIGHT_BG : COLOR_ROW_ALT);
        table.addCell(amtCell);

        // OFFER PRICE
        String offerText = offerVal != null ? rupees(offerVal) : "-";
        Font offerFont = isGrandTotal ? rupeeGoldBoldFont : (offerVal != null ? rupeeGoldBoldFont : rupeeFont);
        PdfPCell offerCell = new PdfPCell(new Phrase(offerText, offerFont));
        offerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        offerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        offerCell.setPaddingTop(5f);
        offerCell.setPaddingBottom(5f);
        offerCell.setPaddingRight(3f);
        offerCell.setBorderColor(COLOR_BORDER);
        offerCell.setBorderWidth(0.5f);
        offerCell.setBackgroundColor(isGrandTotal ? COLOR_LIGHT_BG : COLOR_ROW_ALT);
        table.addCell(offerCell);
    }

    private String buildSize(BigDecimal length, BigDecimal width, BigDecimal height) {
        boolean hasL = length != null && length.compareTo(BigDecimal.ZERO) > 0;
        boolean hasW = width != null && width.compareTo(BigDecimal.ZERO) > 0;
        boolean hasH = height != null && height.compareTo(BigDecimal.ZERO) > 0;

        if (!hasL && !hasW && !hasH) {
            return "-";
        }

        StringBuilder sb = new StringBuilder();
        if (hasL) sb.append("L:").append(format(length));
        if (hasW) {
            if (sb.length() > 0) sb.append(" × ");
            sb.append("W:").append(format(width));
        }
        if (hasH) {
            if (sb.length() > 0) sb.append(" × ");
            sb.append("H:").append(format(height));
        }
        return sb.toString();
    }

    private String rupees(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return "₹" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
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