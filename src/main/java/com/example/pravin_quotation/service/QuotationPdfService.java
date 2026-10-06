package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationItem;
import com.example.pravin_quotation.model.QuotationRoom;
import com.example.pravin_quotation.repository.QuotationItemRepository;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.repository.QuotationRoomRepository;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;

import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
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
     * Unicode fonts for displaying ₹ symbol.
     */
    private Font rupeeFont;
    private Font rupeeBoldFont;

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

        /*
         * Load Arial font from Windows.
         *
         * This allows the PDF to display the
         * Indian Rupee symbol ₹ correctly.
         */
        try {

            BaseFont rupeeBaseFont =
                    BaseFont.createFont(
                            "C:/Windows/Fonts/arial.ttf",
                            BaseFont.IDENTITY_H,
                            BaseFont.EMBEDDED
                    );

            rupeeFont =
                    new Font(
                            rupeeBaseFont,
                            8,
                            Font.NORMAL
                    );

            rupeeBoldFont =
                    new Font(
                            rupeeBaseFont,
                            9,
                            Font.BOLD
                    );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to load PDF Unicode font",
                    e
            );
        }
    }

    // ============================================================
    // GENERATE QUOTATION PDF
    // ============================================================

    public byte[] generateQuotationPdf(Long quotationId) {

        try {

            // --------------------------------------------------------
            // FIND QUOTATION
            // --------------------------------------------------------

            Quotation quotation =
                    quotationRepository
                            .findById(quotationId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Quotation not found with ID: "
                                                    + quotationId
                                    )
                            );

            // --------------------------------------------------------
            // OUTPUT STREAM
            // --------------------------------------------------------

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            // --------------------------------------------------------
            // DOCUMENT
            // --------------------------------------------------------

            Document document =
                    new Document(
                            PageSize.A4,
                            30,
                            30,
                            30,
                            30
                    );

            PdfWriter.getInstance(
                    document,
                    outputStream
            );

            document.open();

            // ========================================================
            // FONTS
            // ========================================================

            Font companyFont =
                    new Font(
                            Font.HELVETICA,
                            17,
                            Font.BOLD
                    );

            Font titleFont =
                    new Font(
                            Font.HELVETICA,
                            14,
                            Font.BOLD
                    );

            Font sectionFont =
                    new Font(
                            Font.HELVETICA,
                            11,
                            Font.BOLD
                    );

            Font normalFont =
                    new Font(
                            Font.HELVETICA,
                            9,
                            Font.NORMAL
                    );

            Font smallFont =
                    new Font(
                            Font.HELVETICA,
                            8,
                            Font.NORMAL
                    );

            Font boldFont =
                    new Font(
                            Font.HELVETICA,
                            9,
                            Font.BOLD
                    );

            // ========================================================
            // COMPANY HEADER
            // ========================================================

            PdfPTable companyTable =
                    new PdfPTable(1);

            companyTable.setWidthPercentage(100);

            PdfPCell companyCell =
                    new PdfPCell();

            companyCell.setBorderWidth(0);

            companyCell.setHorizontalAlignment(
                    Element.ALIGN_CENTER
            );

            Paragraph companyName =
                    new Paragraph(
                            "Pravin Kitchen & Interiors",
                            companyFont
                    );

            companyName.setAlignment(
                    Element.ALIGN_CENTER
            );

            companyCell.addElement(
                    companyName
            );

            Paragraph address =
                    new Paragraph(
                            "Pravin Modular Kitchen, " +
                                    "Tvm Main Road, Azhagiamandapam,\n" +
                                    "Mulagumoodu Post - 629167, T.N.",
                            smallFont
                    );

            address.setAlignment(
                    Element.ALIGN_CENTER
            );

            companyCell.addElement(
                    address
            );

            Paragraph mobile =
                    new Paragraph(
                            "Mobile: +91 9787769970",
                            smallFont
                    );

            mobile.setAlignment(
                    Element.ALIGN_CENTER
            );

            companyCell.addElement(
                    mobile
            );

            companyTable.addCell(
                    companyCell
            );

            document.add(
                    companyTable
            );

            document.add(
                    new Paragraph(" ")
            );

            // ========================================================
            // QUOTATION TITLE
            // ========================================================

            Paragraph quotationTitle =
                    new Paragraph(
                            "MARKETING QUOTATION",
                            titleFont
                    );

            quotationTitle.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(
                    quotationTitle
            );

            document.add(
                    new Paragraph(" ")
            );

            // ========================================================
            // QUOTATION INFORMATION
            // ========================================================

            PdfPTable quotationInfo =
                    new PdfPTable(4);

            quotationInfo.setWidthPercentage(100);

            quotationInfo.setWidths(
                    new float[]{
                            15f,
                            35f,
                            15f,
                            35f
                    }
            );

            addLabelValue(
                    quotationInfo,
                    "Quotation No",
                    safe(
                            quotation.getQuotationNumber()
                    ),
                    "Date",
                    quotation.getQuotationDate() != null
                            ? quotation
                            .getQuotationDate()
                            .format(
                                    DateTimeFormatter.ofPattern(
                                            "dd/MM/yyyy"
                                    )
                            )
                            : "-"
            );

            /*
             * Pricing mode is currently used for the
             * quotation type.
             *
             * Actual Division can be connected later
             * when Division is added to Quotation.
             */
            String divisionName = "-";

            if (quotation.getDivision() != null) {
                divisionName = safe(
                        quotation.getDivision().getName()
                );
            }

            String quotationType = "-";

            if (quotation.getPricingMode() != null) {

                switch (quotation.getPricingMode()) {

                    case PREMIUM:
                        quotationType = "Premium";
                        break;

                    case MIDDLE:
                        quotationType = "Middle / Standard";
                        break;

                    case ECONOMY:
                        quotationType = "Economy";
                        break;

                    default:
                        quotationType =
                                quotation.getPricingMode().name();
                }
            }

            addLabelValue(
                    quotationInfo,
                    "Division",
                    divisionName,
                    "Quotation Type",
                    quotationType
            );

            document.add(
                    quotationInfo
            );

            document.add(
                    new Paragraph(" ")
            );

            // ========================================================
            // CUSTOMER DETAILS
            // ========================================================

            document.add(
                    new Paragraph(
                            "CUSTOMER DETAILS",
                            sectionFont
                    )
            );

            document.add(
                    new Paragraph(" ")
            );

            PdfPTable customerTable =
                    new PdfPTable(2);

            customerTable.setWidthPercentage(100);

            customerTable.setWidths(
                    new float[]{
                            25f,
                            75f
                    }
            );

            if (quotation.getCustomer() != null) {

                addLabelValueSingle(
                        customerTable,
                        "Customer Name",
                        safe(
                                quotation
                                        .getCustomer()
                                        .getName()
                        )
                );

                addLabelValueSingle(
                        customerTable,
                        "Phone",
                        safe(
                                quotation
                                        .getCustomer()
                                        .getPhone()
                        )
                );

                addLabelValueSingle(
                        customerTable,
                        "Email",
                        safe(
                                quotation
                                        .getCustomer()
                                        .getEmail()
                        )
                );

                addLabelValueSingle(
                        customerTable,
                        "Address",
                        safe(
                                quotation
                                        .getCustomer()
                                        .getAddress()
                        )
                );
            }

            if (quotation.getBranch() != null) {

                addLabelValueSingle(
                        customerTable,
                        "District / Branch",
                        safe(
                                quotation
                                        .getBranch()
                                        .getName()
                        )
                );
            }

            document.add(
                    customerTable
            );

            document.add(
                    new Paragraph(" ")
            );

            // ========================================================
            // QUOTE SUMMARY
            // ========================================================

            document.add(
                    new Paragraph(
                            "QUOTE SUMMARY",
                            sectionFont
                    )
            );

            document.add(
                    new Paragraph(" ")
            );

            PdfPTable itemTable =
                    new PdfPTable(8);

            /*
             * Column widths:
             *
             * #             4
             * Room / Work  13
             * Material     15
             * Specification25
             * Size         14
             * Sq.Ft         7
             * Rate         11
             * Offer Price  11
             */
            itemTable.setWidths(
                    new float[]{
                            4f,
                            13f,
                            15f,
                            25f,
                            14f,
                            7f,
                            11f,
                            11f
                    }
            );

            itemTable.setWidthPercentage(
                    100
            );

            itemTable.setSpacingBefore(
                    5f
            );

            itemTable.setSpacingAfter(
                    10f
            );

            // --------------------------------------------------------
            // TABLE HEADER
            // --------------------------------------------------------

            addHeaderCell(
                    itemTable,
                    "#"
            );

            addHeaderCell(
                    itemTable,
                    "ROOM / WORK"
            );

            addHeaderCell(
                    itemTable,
                    "MATERIAL"
            );

            addHeaderCell(
                    itemTable,
                    "SPECIFICATION"
            );

            addHeaderCell(
                    itemTable,
                    "SIZE"
            );

            addHeaderCell(
                    itemTable,
                    "SQ.FT"
            );

            addHeaderCell(
                    itemTable,
                    "RATE"
            );

            addHeaderCell(
                    itemTable,
                    "OFFER PRICE"
            );

            // ========================================================
            // QUOTATION ROOMS
            // ========================================================

            List<QuotationRoom> rooms =
                    quotationRoomRepository
                            .findByQuotationId(
                                    quotation.getId()
                            );

            int serialNumber = 1;

            for (QuotationRoom room : rooms) {

                if (!Boolean.TRUE.equals(
                        room.getActive()
                )) {

                    continue;
                }

                // ----------------------------------------------------
                // GET ITEMS
                // ----------------------------------------------------

                List<QuotationItem> items =
                        quotationItemRepository
                                .findByQuotationRoomId(
                                        room.getId()
                                );

                for (QuotationItem item : items) {

                    if (!Boolean.TRUE.equals(
                            item.getActive()
                    )) {

                        continue;
                    }

                    // =================================================
                    // ROOM / WORK
                    // =================================================

                    String roomName =
                            safe(
                                    room.getRoom()
                            );

                    String workName = "";

                    if (item.getWorkCategory() != null) {

                        workName =
                                safe(
                                        item
                                                .getWorkCategory()
                                                .getName()
                                );
                    }

                    String roomWork;

                    if (!workName.isBlank()) {

                        roomWork =
                                roomName
                                        + "\n"
                                        + workName;

                    } else {

                        roomWork =
                                roomName;
                    }

                    // =================================================
                    // MATERIAL
                    // =================================================

                    String materialText = "";

                    if (item.getMaterial() != null) {

                        materialText =
                                safe(
                                        item
                                                .getMaterial()
                                                .getName()
                                );
                    }

                    if (item.getMaterialOption() != null) {

                        String optionName =
                                safe(
                                        item
                                                .getMaterialOption()
                                                .getName()
                                );

                        if (!optionName.isBlank()) {

                            if (!materialText.isBlank()) {

                                materialText += "\n";
                            }

                            materialText +=
                                    optionName;
                        }
                    }

                    if (materialText.isBlank()) {

                        materialText =
                                "-";
                    }

                    // =================================================
                    // SPECIFICATION
                    // =================================================

                    String specification =
                            safe(
                                    item.getSpecification()
                            );

                    if (specification.isBlank()) {

                        specification =
                                "-";
                    }

                    // =================================================
                    // SIZE
                    // =================================================

                    String size =
                            buildSize(
                                    item.getLengthValue(),
                                    item.getWidthValue(),
                                    item.getHeightValue()
                            );

                    if (size.isBlank()) {

                        size =
                                "-";
                    }

                    // =================================================
                    // SQ.FT
                    // =================================================

                    BigDecimal sqft =
                            item.getCalculatedSqft();

                    if (sqft == null) {

                        sqft =
                                BigDecimal.ZERO;
                    }

                    // =================================================
                    // RATE
                    // =================================================

                    BigDecimal rate =
                            item.getRate();

                    if (rate == null) {

                        rate =
                                BigDecimal.ZERO;
                    }

                    // =================================================
                    // OFFER PRICE
                    // =================================================

                    BigDecimal offerPrice =
                            item.getOfferPrice();

                    /*
                     * If offer price is empty or zero,
                     * use calculated amount.
                     */
                    if (
                            offerPrice == null
                                    ||
                                    offerPrice.compareTo(
                                            BigDecimal.ZERO
                                    ) <= 0
                    ) {

                        offerPrice =
                                item.getAmount();

                        if (offerPrice == null) {

                            offerPrice =
                                    BigDecimal.ZERO;
                        }
                    }

                    // =================================================
                    // ADD ROW
                    // =================================================

                    addBodyCell(
                            itemTable,
                            String.valueOf(
                                    serialNumber++
                            ),
                            Element.ALIGN_CENTER
                    );

                    addBodyCell(
                            itemTable,
                            roomWork,
                            Element.ALIGN_LEFT
                    );

                    addBodyCell(
                            itemTable,
                            materialText,
                            Element.ALIGN_LEFT
                    );

                    addBodyCell(
                            itemTable,
                            specification,
                            Element.ALIGN_LEFT
                    );

                    addBodyCell(
                            itemTable,
                            size,
                            Element.ALIGN_CENTER
                    );

                    addBodyCell(
                            itemTable,
                            format(sqft),
                            Element.ALIGN_RIGHT
                    );

                    /*
                     * Use Unicode font for ₹.
                     */
                    addRupeeCell(
                            itemTable,
                            rupees(rate),
                            false
                    );

                    /*
                     * Use Unicode font for ₹.
                     */
                    addRupeeCell(
                            itemTable,
                            rupees(offerPrice),
                            false
                    );
                }
            }

            document.add(
                    itemTable
            );

            // ========================================================
            // FINANCIAL VALUES
            // ========================================================

            BigDecimal subtotal =
                    zeroIfNull(
                            quotation.getSubtotal()
                    );

            BigDecimal accessories =
                    zeroIfNull(
                            quotation.getAccessoriesAmount()
                    );

            BigDecimal travel =
                    zeroIfNull(
                            quotation.getTravelCharge()
                    );

            BigDecimal otherCharges =
                    zeroIfNull(
                            quotation.getOtherCharges()
                    );

            BigDecimal discount =
                    zeroIfNull(
                            quotation.getDiscountAmount()
                    );

            BigDecimal taxable =
                    zeroIfNull(
                            quotation.getTaxableAmount()
                    );

            BigDecimal gst =
                    zeroIfNull(
                            quotation.getGstAmount()
                    );

            BigDecimal grandTotal =
                    zeroIfNull(
                            quotation.getGrandTotal()
                    );

            // ========================================================
            // PRICE SUMMARY
            // ========================================================

            document.add(
                    new Paragraph(
                            "PRICE SUMMARY",
                            sectionFont
                    )
            );

            document.add(
                    new Paragraph(" ")
            );

            PdfPTable summaryTable =
                    new PdfPTable(2);

            summaryTable.setWidthPercentage(
                    55
            );

            summaryTable.setHorizontalAlignment(
                    Element.ALIGN_RIGHT
            );

            summaryTable.setWidths(
                    new float[]{
                            65f,
                            35f
                    }
            );

            addTotal(
                    summaryTable,
                    "Rooms Subtotal",
                    subtotal
            );

            addTotal(
                    summaryTable,
                    "Accessories",
                    accessories
            );

            addTotal(
                    summaryTable,
                    "Travel Charge",
                    travel
            );

            addTotal(
                    summaryTable,
                    "Other Charges",
                    otherCharges
            );

            addTotal(
                    summaryTable,
                    "Discount",
                    discount
            );

            addTotal(
                    summaryTable,
                    "Taxable Amount",
                    taxable
            );

            addTotal(
                    summaryTable,
                    "GST",
                    gst
            );

            // ========================================================
            // GRAND TOTAL
            // ========================================================

            PdfPCell grandLabel =
                    new PdfPCell(
                            new Phrase(
                                    "GRAND TOTAL",
                                    boldFont
                            )
                    );

            grandLabel.setPadding(
                    6f
            );

            grandLabel.setHorizontalAlignment(
                    Element.ALIGN_LEFT
            );

            PdfPCell grandValue =
                    new PdfPCell(
                            new Phrase(
                                    rupees(grandTotal),
                                    rupeeBoldFont
                            )
                    );

            grandValue.setPadding(
                    6f
            );

            grandValue.setHorizontalAlignment(
                    Element.ALIGN_RIGHT
            );

            summaryTable.addCell(
                    grandLabel
            );

            summaryTable.addCell(
                    grandValue
            );

            document.add(
                    summaryTable
            );

            document.add(
                    new Paragraph(" ")
            );

            // ========================================================
            // AMOUNT IN WORDS
            // ========================================================

            document.add(
                    new Paragraph(
                            "AMOUNT IN WORDS",
                            sectionFont
                    )
            );

            document.add(
                    new Paragraph(
                            numberToWordsService
                                    .convertRupees(
                                            grandTotal
                                    ),
                            normalFont
                    )
            );

            document.add(
                    new Paragraph(" ")
            );

            // ========================================================
            // PAYMENT TERMS
            // ========================================================

            document.add(
                    new Paragraph(
                            "PAYMENT TERMS",
                            sectionFont
                    )
            );

            document.add(
                    new Paragraph(
                            "10% Token: Commencement of 3D design " +
                                    "on receipt of token.",
                            normalFont
                    )
            );

            document.add(
                    new Paragraph(
                            "50% Advance: Drawing will be sent " +
                                    "for production on receipt of advance.",
                            normalFont
                    )
            );

            document.add(
                    new Paragraph(
                            "35% Part Payment: After completion " +
                                    "of frame work.",
                            normalFont
                    )
            );

            document.add(
                    new Paragraph(
                            "5% Final Payment: Last day of work " +
                                    "completion in site.",
                            normalFont
                    )
            );

            document.add(
                    new Paragraph(" ")
            );

            // ========================================================
            // CUSTOMER REQUIREMENTS
            // ========================================================

            if (
                    quotation.getCustomerRequirements()
                            != null
                            &&
                            !quotation
                                    .getCustomerRequirements()
                                    .isBlank()
            ) {

                document.add(
                        new Paragraph(
                                "CUSTOMER REQUIREMENTS",
                                sectionFont
                        )
                );

                document.add(
                        new Paragraph(
                                quotation
                                        .getCustomerRequirements(),
                                normalFont
                        )
                );

                document.add(
                        new Paragraph(" ")
                );
            }

            // ========================================================
            // NOTES
            // ========================================================

            if (
                    quotation.getNotes()
                            != null
                            &&
                            !quotation
                                    .getNotes()
                                    .isBlank()
            ) {

                document.add(
                        new Paragraph(
                                "NOTES",
                                sectionFont
                        )
                );

                document.add(
                        new Paragraph(
                                quotation.getNotes(),
                                normalFont
                        )
                );

                document.add(
                        new Paragraph(" ")
                );
            }

            // ========================================================
            // SIGNATURE
            // ========================================================

            PdfPTable signatureTable =
                    new PdfPTable(2);

            signatureTable.setWidthPercentage(
                    100
            );

            signatureTable.setWidths(
                    new float[]{
                            50f,
                            50f
                    }
            );

            addSignature(
                    signatureTable,
                    "Customer Signature"
            );

            addSignature(
                    signatureTable,
                    "For Pravin Kitchen & Interiors"
            );

            document.add(
                    signatureTable
            );

            // ========================================================
            // CLOSE DOCUMENT
            // ========================================================

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "PDF generation failed",
                    e
            );
        }
    }

    // ============================================================
    // HEADER CELL
    // ============================================================

    private void addHeaderCell(
            PdfPTable table,
            String text
    ) {

        Font headerFont =
                new Font(
                        Font.HELVETICA,
                        7.5f,
                        Font.BOLD
                );

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                safe(text),
                                headerFont
                        )
                );

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPaddingTop(
                6f
        );

        cell.setPaddingBottom(
                6f
        );

        cell.setPaddingLeft(
                3f
        );

        cell.setPaddingRight(
                3f
        );

        cell.setBorderWidth(
                0.8f
        );

        table.addCell(
                cell
        );
    }

    // ============================================================
    // BODY CELL
    // ============================================================

    private void addBodyCell(
            PdfPTable table,
            String text,
            int alignment
    ) {

        Font bodyFont =
                new Font(
                        Font.HELVETICA,
                        7.2f,
                        Font.NORMAL
                );

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                safe(text),
                                bodyFont
                        )
                );

        cell.setHorizontalAlignment(
                alignment
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPaddingTop(
                5f
        );

        cell.setPaddingBottom(
                5f
        );

        cell.setPaddingLeft(
                3f
        );

        cell.setPaddingRight(
                3f
        );

        cell.setBorderWidth(
                0.5f
        );

        table.addCell(
                cell
        );
    }

    // ============================================================
    // RUPEE CELL
    // ============================================================

    private void addRupeeCell(
            PdfPTable table,
            String text,
            boolean bold
    ) {

        Font font =
                bold
                        ? rupeeBoldFont
                        : rupeeFont;

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                safe(text),
                                font
                        )
                );

        cell.setHorizontalAlignment(
                Element.ALIGN_RIGHT
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPaddingTop(
                5f
        );

        cell.setPaddingBottom(
                5f
        );

        cell.setPaddingLeft(
                3f
        );

        cell.setPaddingRight(
                3f
        );

        cell.setBorderWidth(
                0.5f
        );

        table.addCell(
                cell
        );
    }

    // ============================================================
    // LABEL + VALUE
    // ============================================================

    private void addLabelValue(
            PdfPTable table,
            String label1,
            String value1,
            String label2,
            String value2
    ) {

        Font labelFont =
                new Font(
                        Font.HELVETICA,
                        8,
                        Font.BOLD
                );

        Font valueFont =
                new Font(
                        Font.HELVETICA,
                        8,
                        Font.NORMAL
                );

        PdfPCell labelCell1 =
                new PdfPCell(
                        new Phrase(
                                safe(label1),
                                labelFont
                        )
                );

        PdfPCell valueCell1 =
                new PdfPCell(
                        new Phrase(
                                safe(value1),
                                valueFont
                        )
                );

        PdfPCell labelCell2 =
                new PdfPCell(
                        new Phrase(
                                safe(label2),
                                labelFont
                        )
                );

        PdfPCell valueCell2 =
                new PdfPCell(
                        new Phrase(
                                safe(value2),
                                valueFont
                        )
                );

        labelCell1.setPadding(
                5f
        );

        valueCell1.setPadding(
                5f
        );

        labelCell2.setPadding(
                5f
        );

        valueCell2.setPadding(
                5f
        );

        table.addCell(
                labelCell1
        );

        table.addCell(
                valueCell1
        );

        table.addCell(
                labelCell2
        );

        table.addCell(
                valueCell2
        );
    }

    // ============================================================
    // SINGLE LABEL + VALUE
    // ============================================================

    private void addLabelValueSingle(
            PdfPTable table,
            String label,
            String value
    ) {

        Font labelFont =
                new Font(
                        Font.HELVETICA,
                        8,
                        Font.BOLD
                );

        Font valueFont =
                new Font(
                        Font.HELVETICA,
                        8,
                        Font.NORMAL
                );

        PdfPCell labelCell =
                new PdfPCell(
                        new Phrase(
                                safe(label),
                                labelFont
                        )
                );

        PdfPCell valueCell =
                new PdfPCell(
                        new Phrase(
                                safe(value),
                                valueFont
                        )
                );

        labelCell.setPadding(
                5f
        );

        valueCell.setPadding(
                5f
        );

        table.addCell(
                labelCell
        );

        table.addCell(
                valueCell
        );
    }

    // ============================================================
    // PRICE SUMMARY ROW
    // ============================================================

    private void addTotal(
            PdfPTable table,
            String label,
            BigDecimal amount
    ) {

        Font normal =
                new Font(
                        Font.HELVETICA,
                        8,
                        Font.NORMAL
                );

        PdfPCell labelCell =
                new PdfPCell(
                        new Phrase(
                                label,
                                normal
                        )
                );

        labelCell.setPadding(
                5f
        );

        labelCell.setHorizontalAlignment(
                Element.ALIGN_LEFT
        );

        PdfPCell valueCell =
                new PdfPCell(
                        new Phrase(
                                rupees(amount),
                                rupeeFont
                        )
                );

        valueCell.setPadding(
                5f
        );

        valueCell.setHorizontalAlignment(
                Element.ALIGN_RIGHT
        );

        table.addCell(
                labelCell
        );

        table.addCell(
                valueCell
        );
    }

    // ============================================================
    // SIGNATURE
    // ============================================================

    private void addSignature(
            PdfPTable table,
            String text
    ) {

        Font signatureFont =
                new Font(
                        Font.HELVETICA,
                        8,
                        Font.BOLD
                );

        PdfPCell cell =
                new PdfPCell();

        cell.setBorderWidth(
                0
        );

        cell.setMinimumHeight(
                60f
        );

        Paragraph paragraph =
                new Paragraph(
                        "\n\n________________________\n"
                                + text,
                        signatureFont
                );

        paragraph.setAlignment(
                Element.ALIGN_CENTER
        );

        cell.addElement(
                paragraph
        );

        table.addCell(
                cell
        );
    }

    // ============================================================
    // BUILD SIZE
    // ============================================================

    private String buildSize(
            BigDecimal length,
            BigDecimal width,
            BigDecimal height
    ) {

        boolean hasLength =
                length != null;

        boolean hasWidth =
                width != null;

        boolean hasHeight =
                height != null;

        if (
                !hasLength
                        &&
                        !hasWidth
                        &&
                        !hasHeight
        ) {

            return "";
        }

        StringBuilder result =
                new StringBuilder();

        if (hasLength) {

            result.append(
                    format(length)
            );
        }

        if (hasWidth) {

            if (result.length() > 0) {

                result.append(
                        " × "
                );
            }

            result.append(
                    format(width)
            );
        }

        if (hasHeight) {

            if (result.length() > 0) {

                result.append(
                        " × "
                );
            }

            result.append(
                    format(height)
            );
        }

        return result.toString();
    }

    // ============================================================
    // RUPEES
    // ============================================================

    private String rupees(
            BigDecimal amount
    ) {

        if (amount == null) {

            amount =
                    BigDecimal.ZERO;
        }

        return "₹"
                +
                amount.setScale(
                        2,
                        RoundingMode.HALF_UP
                ).toPlainString();
    }

    // ============================================================
    // FORMAT DECIMAL
    // ============================================================

    private String format(
            BigDecimal value
    ) {

        if (value == null) {

            return "0.00";
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        ).toPlainString();
    }

    // ============================================================
    // ZERO IF NULL
    // ============================================================

    private BigDecimal zeroIfNull(
            BigDecimal value
    ) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }

    // ============================================================
    // SAFE STRING
    // ============================================================

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }
}