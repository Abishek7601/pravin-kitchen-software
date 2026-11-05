package com.example.pravin_quotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class QuotationBreakdownHtmlTest {

    @Test
    @DisplayName("Verify admin quotation-view.html has no orphan foster-parented amount or duplicate closing td")
    void testAdminQuotationViewHasNoOrphanAmount() throws IOException {
        String content = Files.readString(Path.of("src/main/resources/templates/admin/quotation-view.html"));
        assertFalse(content.contains("</td>\n                                ₹ 0.00"),
                "Admin quotation-view must not contain orphan ₹ 0.00 outside td cell");
        assertFalse(content.contains("</td>\r\n                                ₹ 0.00"),
                "Admin quotation-view must not contain orphan ₹ 0.00 outside td cell");
    }

    @Test
    @DisplayName("Verify employee quotation-workspace.html has no orphan foster-parented amount or duplicate closing td")
    void testEmployeeQuotationWorkspaceHasNoOrphanAmount() throws IOException {
        String content = Files.readString(Path.of("src/main/resources/templates/employee/quotation-workspace.html"));
        assertFalse(content.contains("</td>\n                                        ₹ 0.00"),
                "Employee quotation-workspace must not contain orphan ₹ 0.00 outside td cell");
        assertFalse(content.contains("</td>\r\n                                        ₹ 0.00"),
                "Employee quotation-workspace must not contain orphan ₹ 0.00 outside td cell");
    }
}

