package com.example.pravin_quotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PaginationListingPagesTest {

    private static final String BASE_TEMPLATES = "src/main/resources/templates/";
    private static final String STATIC_PATH = "src/main/resources/static/";

    @Test
    @DisplayName("Verify pagination.css exists with brand theme #024541 and responsive rules")
    public void testPaginationCss() throws IOException {
        File cssFile = new File(STATIC_PATH + "css/pagination.css");
        assertTrue(cssFile.exists(), "pagination.css must exist");

        String content = Files.readString(cssFile.toPath());
        assertTrue(content.contains("#024541"), "pagination.css must use primary brand color #024541");
        assertTrue(content.contains(".pagination-footer"), "pagination.css must define .pagination-footer");
        assertTrue(content.contains(".pagination-btn"), "pagination.css must define .pagination-btn");
        assertTrue(content.contains(".pagination-page-btn"), "pagination.css must define .pagination-page-btn");
        assertTrue(content.contains(".is-hidden"), "pagination.css must define .is-hidden to hide controls when <= 10 records");
        assertTrue(content.contains("@media"), "pagination.css must include mobile responsiveness rules");
    }

    @Test
    @DisplayName("Verify pagination.js exists with 10 records per page, page navigation, and filter logic")
    public void testPaginationJs() throws IOException {
        File jsFile = new File(STATIC_PATH + "js/pagination.js");
        assertTrue(jsFile.exists(), "pagination.js must exist");

        String content = Files.readString(jsFile.toPath());
        assertTrue(content.contains("DEFAULT_PAGE_SIZE = 10"), "pagination.js must default to 10 records per page");
        assertTrue(content.contains("initTablePagination"), "pagination.js must expose initTablePagination");
        assertTrue(content.contains("goToPage"), "pagination.js must support page switching");
        assertTrue(content.contains("applyFilterAndRender"), "pagination.js must support filter and search re-rendering");
        assertTrue(content.contains("is-hidden"), "pagination.js must toggle is-hidden when total records <= pageSize");
        assertTrue(content.contains("pagination-prev"), "pagination.js must handle previous button");
        assertTrue(content.contains("pagination-next"), "pagination.js must handle next button");
        assertTrue(content.contains("pagination-page-btn"), "pagination.js must render page number buttons");
    }

    @Test
    @DisplayName("Verify pagination is implemented across all details listing pages")
    public void testPaginationAcrossListingPages() throws IOException {
        List<String> listingTemplates = List.of(
                "admin/work-categories.html",
                "admin/divisions.html",
                "admin/materials.html",
                "admin/material-options.html",
                "admin/customers.html",
                "employee/customers.html",
                "admin/branches.html",
                "admin/branch-quotations.html",
                "admin/employees.html",
                "admin/quotations.html",
                "employee/quotations.html",
                "admin/pricing.html",
                "admin/travel-charges.html",
                "admin/gst-settings.html",
                "admin/items.html",
                "admin/formula-calculation.html",
                "admin/portfolio.html",
                "admin/quotation-value.html"
        );

        for (String templatePath : listingTemplates) {
            File file = new File(BASE_TEMPLATES + templatePath);
            assertTrue(file.exists(), "Template " + templatePath + " must exist");

            String html = Files.readString(file.toPath());

            assertTrue(html.contains("pagination.css"),
                    templatePath + " must include pagination.css stylesheet");
            assertTrue(html.contains("pagination.js"),
                    templatePath + " must include pagination.js script");
            assertTrue(html.contains("initTablePagination"),
                    templatePath + " must initialize pagination via initTablePagination");
        }
    }

    @Test
    @DisplayName("Verify forms and single-record views do NOT have pagination controls")
    public void testNonListingPagesDoNotHavePagination() throws IOException {
        List<String> singleRecordTemplates = List.of(
                "admin/customer-form.html",
                "admin/branch-form.html",
                "admin/division-form.html",
                "admin/employee-form.html",
                "admin/gst-setting-form.html",
                "admin/item-form.html",
                "admin/material-form.html",
                "admin/material-option-form.html",
                "admin/travel-charge-form.html",
                "admin/work-category-form.html"
        );

        for (String templatePath : singleRecordTemplates) {
            File file = new File(BASE_TEMPLATES + templatePath);
            if (file.exists()) {
                String html = Files.readString(file.toPath());
                assertFalse(html.contains("initTablePagination"),
                        "Form template " + templatePath + " must not initialize table pagination");
            }
        }
    }
}
