package com.example.pravin_quotation;

import com.example.pravin_quotation.util.IndianCurrencyFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IndianCurrencyFormatterTest {

    @Test
    @DisplayName("Verify user required examples in Indian Numbering System")
    public void testUserExamples() {
        assertEquals("74,810.00", IndianCurrencyFormatter.formatAmount(new BigDecimal("74810.00")));
        assertEquals("₹ 74,810.00", IndianCurrencyFormatter.formatWithRupee(new BigDecimal("74810.00")));

        assertEquals("1,25,000.00", IndianCurrencyFormatter.formatAmount(new BigDecimal("125000.00")));
        assertEquals("₹ 1,25,000.00", IndianCurrencyFormatter.formatWithRupee(new BigDecimal("125000.00")));

        assertEquals("12,50,000.00", IndianCurrencyFormatter.formatAmount(new BigDecimal("1250000.00")));
        assertEquals("₹ 12,50,000.00", IndianCurrencyFormatter.formatWithRupee(new BigDecimal("1250000.00")));

        assertEquals("1,00,00,000.00", IndianCurrencyFormatter.formatAmount(new BigDecimal("10000000.00")));
        assertEquals("₹ 1,00,00,000.00", IndianCurrencyFormatter.formatWithRupee(new BigDecimal("10000000.00")));

        assertEquals("500.00", IndianCurrencyFormatter.formatAmount(new BigDecimal("500.00")));
        assertEquals("₹ 500.00", IndianCurrencyFormatter.formatWithRupee(new BigDecimal("500.00")));
    }

    @Test
    @DisplayName("Verify zero, null, negatives, and double values")
    public void testEdgeCases() {
        assertEquals("0.00", IndianCurrencyFormatter.formatAmount(null));
        assertEquals("₹ 0.00", IndianCurrencyFormatter.formatWithRupee(null));

        assertEquals("0.00", IndianCurrencyFormatter.formatAmount(BigDecimal.ZERO));
        assertEquals("₹ 0.00", IndianCurrencyFormatter.formatWithRupee(BigDecimal.ZERO));

        assertEquals("1,250.50", IndianCurrencyFormatter.formatAmount(1250.5));
        assertEquals("₹ 1,250.50", IndianCurrencyFormatter.formatWithRupee(1250.5));

        assertEquals("-500.00", IndianCurrencyFormatter.formatAmount(new BigDecimal("-500.00")));
        assertEquals("- ₹ 500.00", IndianCurrencyFormatter.formatWithRupee(new BigDecimal("-500.00")));

        assertEquals("-12,50,000.00", IndianCurrencyFormatter.formatAmount(new BigDecimal("-1250000.00")));
        assertEquals("- ₹ 12,50,000.00", IndianCurrencyFormatter.formatWithRupee(new BigDecimal("-1250000.00")));
    }

    @Test
    @DisplayName("Verify parsing formatted strings back to numeric values")
    public void testParseAmount() {
        assertEquals(new BigDecimal("74810.00"), IndianCurrencyFormatter.parseAmount("₹ 74,810.00"));
        assertEquals(new BigDecimal("125000.00"), IndianCurrencyFormatter.parseAmount("1,25,000.00"));
        assertEquals(new BigDecimal("10000000.00"), IndianCurrencyFormatter.parseAmount("₹ 1,00,00,000.00"));
        assertEquals(new BigDecimal("-500.00"), IndianCurrencyFormatter.parseAmount("- ₹ 500.00"));
        assertEquals(BigDecimal.ZERO, IndianCurrencyFormatter.parseAmount(""));
        assertEquals(BigDecimal.ZERO, IndianCurrencyFormatter.parseAmount(null));
    }

    @Test
    @DisplayName("Verify template files use @inr formatting for currency display")
    public void testTemplateCurrencyFormatting() throws IOException {
        String adminQuotations = Files.readString(Path.of("src/main/resources/templates/admin/quotations.html"));
        assertTrue(adminQuotations.contains("@inr.rupees(quotation.grandTotal)"), "admin/quotations.html should format grandTotal with @inr.rupees");

        String empQuotations = Files.readString(Path.of("src/main/resources/templates/employee/quotations.html"));
        assertTrue(empQuotations.contains("@inr.rupees(quotation.grandTotal)"), "employee/quotations.html should format grandTotal with @inr.rupees");

        String adminView = Files.readString(Path.of("src/main/resources/templates/admin/quotation-view.html"));
        assertTrue(adminView.contains("@inr.format(quotation.grandTotal != null ?"), "admin/quotation-view.html should format grandTotal with @inr.format");
        assertTrue(adminView.contains("@inr.format(quotation.travelCharge)"), "admin/quotation-view.html should format travelCharge with @inr.format");
        assertTrue(adminView.contains("@inr.format(quotation.gstAmount)"), "admin/quotation-view.html should format gstAmount with @inr.format");

        String empView = Files.readString(Path.of("src/main/resources/templates/employee/quotation-view.html"));
        assertTrue(empView.contains("@inr.format(quotation.grandTotal)"), "employee/quotation-view.html should format grandTotal with @inr.format");

        String empWorkspace = Files.readString(Path.of("src/main/resources/templates/employee/quotation-workspace.html"));
        assertTrue(empWorkspace.contains("@inr.format(quotation.grandTotal != null ?"), "employee/quotation-workspace.html should format grandTotal with @inr.format");
        assertTrue(empWorkspace.contains("@inr.format(quotation.travelCharge)"), "employee/quotation-workspace.html should format travelCharge with @inr.format");

        String adminBuilder = Files.readString(Path.of("src/main/resources/templates/admin/quotation-builder.html"));
        assertTrue(adminBuilder.contains("function formatIndianNumber"), "admin/quotation-builder.html must include formatIndianNumber");
        assertTrue(adminBuilder.contains("inputmode=\"decimal\""), "admin/quotation-builder.html must have inputmode decimal for amount fields");

        String empBuilder = Files.readString(Path.of("src/main/resources/templates/employee/quotation-builder.html"));
        assertTrue(empBuilder.contains("function formatIndianNumber"), "employee/quotation-builder.html must include formatIndianNumber");
        assertTrue(empBuilder.contains("inputmode=\"decimal\""), "employee/quotation-builder.html must have inputmode decimal for amount fields");
    }

    @Test
    @DisplayName("Verify QuotationPdfService integrates IndianCurrencyFormatter")
    public void testQuotationPdfServiceIntegration() throws IOException {
        String pdfService = Files.readString(Path.of("src/main/java/com/example/pravin_quotation/service/QuotationPdfService.java"));
        assertTrue(pdfService.contains("IndianCurrencyFormatter.formatBigDecimal"), "QuotationPdfService must format amounts using IndianCurrencyFormatter");
    }
}


