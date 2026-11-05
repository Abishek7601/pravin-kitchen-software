package com.example.pravin_quotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpecificationFontSizeConsistencyTest {

    @Test
    @DisplayName("Verify admin-quotation-builder.css defines required font styling and media queries for .mat-spec")
    void testAdminQuotationBuilderCssSpecificationRules() throws IOException {
        Path cssPath = Paths.get("src/main/resources/static/css/admin-quotation-builder.css");
        assertTrue(Files.exists(cssPath), "admin-quotation-builder.css must exist");

        String cssContent = Files.readString(cssPath);

        // 1. Base desktop rule for .mat-spec
        assertTrue(cssContent.contains(".mat-spec"), "CSS must contain .mat-spec selector");
        assertTrue(cssContent.matches("(?s).*\\.mat-spec[^\\{]*\\{[^\\}]*font-size:\\s*15px;.*"),
                "Desktop .mat-spec must have font-size: 15px");
        assertTrue(cssContent.matches("(?s).*\\.mat-spec[^\\{]*\\{[^\\}]*font-weight:\\s*500;.*"),
                "Desktop .mat-spec must have font-weight: 500");
        assertTrue(cssContent.matches("(?s).*\\.mat-spec[^\\{]*\\{[^\\}]*line-height:\\s*1\\.5;.*"),
                "Desktop .mat-spec must have line-height: 1.5");
        assertTrue(cssContent.matches("(?s).*\\.mat-spec[^\\{]*\\{[^\\}]*word-break:\\s*break-word;.*"),
                "Desktop .mat-spec must have word-break: break-word");
        assertTrue(cssContent.matches("(?s).*\\.mat-spec[^\\{]*\\{[^\\}]*white-space:\\s*pre-line;.*"),
                "Desktop .mat-spec must have white-space: pre-line");

        // 2. Tablet responsive rule (max-width: 900px)
        assertTrue(cssContent.matches("(?s).*@media\\s*\\(max-width:\\s*900px\\)[^\\{]*\\{.*?\\.mat-spec[^\\{]*\\{[^\\}]*font-size:\\s*14px\\s*!important;.*"),
                "Tablet media query (max-width: 900px) must set .mat-spec font-size: 14px !important");

        // 3. Mobile responsive rule (max-width: 600px)
        assertTrue(cssContent.matches("(?s).*@media\\s*\\(max-width:\\s*600px\\)[^\\{]*\\{.*?\\.mat-spec[^\\{]*\\{[^\\}]*font-size:\\s*14px\\s*!important;.*"),
                "Mobile media query (max-width: 600px) must set .mat-spec font-size: 14px !important");
    }

    @Test
    @DisplayName("Verify both Admin and Employee quotation-builder templates use .mat-spec inside .col-material")
    void testTemplatesUseConsistentMatSpecElement() throws IOException {
        Path adminPath = Paths.get("src/main/resources/templates/admin/quotation-builder.html");
        Path empPath = Paths.get("src/main/resources/templates/employee/quotation-builder.html");

        assertTrue(Files.exists(adminPath), "admin quotation-builder.html must exist");
        assertTrue(Files.exists(empPath), "employee quotation-builder.html must exist");

        String adminHtml = Files.readString(adminPath);
        String empHtml = Files.readString(empPath);

        // Check CSS links
        assertTrue(adminHtml.contains("/css/admin-quotation-builder.css"),
                "Admin quotation builder must link to /css/admin-quotation-builder.css");
        assertTrue(empHtml.contains("/css/admin-quotation-builder.css"),
                "Employee quotation builder must link to /css/admin-quotation-builder.css");

        // Check .mat-spec in Quote Summary MATERIAL column
        assertTrue(adminHtml.contains("<td class=\"col-material\">"),
                "Admin template must contain <td class=\"col-material\">");
        assertTrue(adminHtml.contains("<div class=\"mat-spec\">${esc(item.specification || item.materialSpecificationName || '')}</div>"),
                "Admin template must render specification in <div class=\"mat-spec\">");

        assertTrue(empHtml.contains("<td class=\"col-material\">"),
                "Employee template must contain <td class=\"col-material\">");
        assertTrue(empHtml.contains("<div class=\"mat-spec\">${esc(item.specification || item.materialSpecificationName || '')}</div>"),
                "Employee template must render specification in <div class=\"mat-spec\">");
    }
}

