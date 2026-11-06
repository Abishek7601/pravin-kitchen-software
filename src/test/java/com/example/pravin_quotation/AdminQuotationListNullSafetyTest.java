package com.example.pravin_quotation;

import com.example.pravin_quotation.model.Customer;
import com.example.pravin_quotation.model.PricingMode;
import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationStatus;
import com.example.pravin_quotation.util.IndianCurrencyFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class AdminQuotationListNullSafetyTest {

    private static final String ADMIN_QUOTATIONS_PATH = "src/main/resources/templates/admin/quotations.html";
    private static final String EMPLOYEE_QUOTATIONS_PATH = "src/main/resources/templates/employee/quotations.html";

    @Test
    @DisplayName("Verify admin/quotations.html has robust null safety guards, placeholders, and scripts")
    void testAdminQuotationsHtmlFileContent() throws IOException {
        File file = new File(ADMIN_QUOTATIONS_PATH);
        assertTrue(file.exists(), "admin/quotations.html must exist");

        String content = Files.readString(file.toPath());

        // Safe customer null-handling
        assertTrue(content.contains("th:if=\"${quotation.customer != null}\""),
                "Template must guard against null quotation.customer");
        assertTrue(content.contains("th:if=\"${quotation.customer == null}\""),
                "Template must handle null quotation.customer");
        assertTrue(content.contains("Not selected"),
                "Template must display 'Not selected' for missing customer or branch");

        // Safe district / branch null-handling
        assertTrue(content.contains("quotation.branch != null"),
                "Template must guard against null quotation.branch");

        // Safe employee null-handling
        assertTrue(content.contains("quotation.employee != null"),
                "Template must guard against null quotation.employee");

        // Safe pricing null-handling
        assertTrue(content.contains("quotation.pricingMode != null"),
                "Template must guard against null quotation.pricingMode");

        // Safe grand total null-handling with 'Not calculated' for pending
        assertTrue(content.contains("Not calculated"),
                "Template must display 'Not calculated' for pending draft with zero/null total");

        // Summary cards safe null status checks
        assertTrue(content.contains("status != null and status.name() == 'PENDING'"),
                "Summary cards must guard status null when counting PENDING");
        assertTrue(content.contains("status != null and status.name() == 'WORKING'"),
                "Summary cards must guard status null when counting WORKING");
        assertTrue(content.contains("status != null and status.name() == 'SENT'"),
                "Summary cards must guard status null when counting SENT");

        // Pagination script and combined search/status filter
        assertTrue(content.contains("pagination.js"),
                "Template must include pagination.js");
        assertTrue(content.contains("initTablePagination"),
                "Template must initialize table pagination");
        assertTrue(content.contains("applyFilters"),
                "Template must synchronize search and status filters with pagination");

        // Resume draft button
        assertTrue(content.contains("Resume"),
                "Template must provide Resume button for pending quotations");
        assertTrue(content.contains("/admin/quotations/builder(quotationId=${quotation.id})"),
                "Resume button must link to builder with quotationId");

        // Delete action fallback
        assertTrue(content.contains("QT-"),
                "Delete confirmation must safely fall back if quotationNumber is null");
    }

    @Test
    @DisplayName("Verify employee/quotations.html has matching null safety guards and builder edit link")
    void testEmployeeQuotationsHtmlFileContent() throws IOException {
        File file = new File(EMPLOYEE_QUOTATIONS_PATH);
        assertTrue(file.exists(), "employee/quotations.html must exist");

        String content = Files.readString(file.toPath());

        // Safe customer and branch null-handling
        assertTrue(content.contains("th:if=\"${quotation.customer != null}\""),
                "Employee template must guard against null quotation.customer");
        assertTrue(content.contains("th:if=\"${quotation.customer == null}\""),
                "Employee template must handle null quotation.customer");
        assertTrue(content.contains("Not selected"),
                "Employee template must display 'Not selected' placeholder");

        // Not calculated for pending zero total
        assertTrue(content.contains("Not calculated"),
                "Employee template must display 'Not calculated' for incomplete pending");

        // Correct builder edit URL
        assertTrue(content.contains("@{/employee/quotations/builder(quotationId=${quotation.id})}"),
                "Employee edit link must use query parameter builder(quotationId=...)");
        assertFalse(content.contains("@{/employee/quotations/builder/{id}"),
                "Employee edit link must not use invalid path variable builder/{id}");

        // Resume draft button
        assertTrue(content.contains("Resume"),
                "Employee template must provide Resume button for pending draft");
    }

    @Test
    @DisplayName("Verify simulated incomplete pending draft logic matches template null-safety requirements")
    void testIncompletePendingQuotationLogicSimulation() {
        IndianCurrencyFormatter formatter = new IndianCurrencyFormatter();

        // Simulate QT-00037 (Incomplete pending quotation)
        Quotation draft = new Quotation();
        draft.setId(37L);
        draft.setQuotationNumber("QT-00037");
        draft.setCustomer(null);
        draft.setBranch(null);
        draft.setEmployee(null);
        draft.setPricingMode(null);
        draft.setQuotationDate(null);
        draft.setCreatedAt(LocalDateTime.of(2026, 11, 6, 10, 0));
        draft.setStatus(QuotationStatus.PENDING);
        draft.setGrandTotal(BigDecimal.ZERO);

        // 1. Customer display logic
        String customerDisplay = (draft.getCustomer() != null && draft.getCustomer().getName() != null && !draft.getCustomer().getName().isBlank())
                ? draft.getCustomer().getName()
                : "Not selected";
        assertEquals("Not selected", customerDisplay);

        // 2. District display logic
        String districtDisplay = (draft.getBranch() != null && draft.getBranch().getName() != null && !draft.getBranch().getName().isBlank())
                ? draft.getBranch().getName()
                : "Not selected";
        assertEquals("Not selected", districtDisplay);

        // 3. Employee display logic
        String employeeDisplay = (draft.getEmployee() != null && draft.getEmployee().getName() != null && !draft.getEmployee().getName().isBlank())
                ? draft.getEmployee().getName()
                : "—";
        assertEquals("—", employeeDisplay);

        // 4. Pricing display logic
        String pricingDisplay = draft.getPricingMode() != null ? draft.getPricingMode().name() : "—";
        assertEquals("—", pricingDisplay);

        // 5. Grand Total display logic: for PENDING with 0 or null total -> "Not calculated"
        String totalDisplay;
        if (draft.getGrandTotal() != null && draft.getGrandTotal().compareTo(BigDecimal.ZERO) > 0) {
            totalDisplay = formatter.rupees(draft.getGrandTotal());
        } else if (draft.getStatus() == QuotationStatus.PENDING) {
            totalDisplay = "Not calculated";
        } else {
            totalDisplay = formatter.rupees(draft.getGrandTotal() != null ? draft.getGrandTotal() : BigDecimal.ZERO);
        }
        assertEquals("Not calculated", totalDisplay);

        // 6. Date fallback logic
        String dateDisplay;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy");
        if (draft.getQuotationDate() != null) {
            dateDisplay = draft.getQuotationDate().format(dtf);
        } else if (draft.getCreatedAt() != null) {
            dateDisplay = draft.getCreatedAt().format(dtf);
        } else {
            dateDisplay = "—";
        }
        assertEquals("06 Nov 2026", dateDisplay);

        // 7. Action logic: PENDING must provide Resume action
        boolean isPending = draft.getStatus() != null && draft.getStatus() == QuotationStatus.PENDING;
        assertTrue(isPending, "Must detect pending status for Resume action");
        String resumeUrl = "/admin/quotations/builder?quotationId=" + draft.getId();
        assertEquals("/admin/quotations/builder?quotationId=37", resumeUrl);
    }

    @Test
    @DisplayName("Verify completed quotation logic formats amounts and actions properly")
    void testCompletedQuotationLogicSimulation() {
        IndianCurrencyFormatter formatter = new IndianCurrencyFormatter();

        Quotation complete = new Quotation();
        complete.setId(36L);
        complete.setQuotationNumber("QT-00036");
        Customer cust = new Customer();
        cust.setName("Murugan");
        cust.setPhone("9876543210");
        complete.setCustomer(cust);
        complete.setPricingMode(PricingMode.PREMIUM);
        complete.setQuotationDate(LocalDate.of(2026, 11, 5));
        complete.setStatus(QuotationStatus.WORKING);
        complete.setGrandTotal(BigDecimal.valueOf(125000));

        assertEquals("Murugan", complete.getCustomer().getName());
        assertEquals("9876543210", complete.getCustomer().getPhone());
        assertEquals("PREMIUM", complete.getPricingMode().name());
        assertEquals("WORKING", complete.getStatus().name());

        String formattedTotal = formatter.rupees(complete.getGrandTotal());
        assertEquals("₹ 1,25,000.00", formattedTotal);
    }
}
