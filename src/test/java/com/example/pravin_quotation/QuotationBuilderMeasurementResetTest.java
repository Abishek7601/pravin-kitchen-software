package com.example.pravin_quotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class QuotationBuilderMeasurementResetTest {

    @Test
    @DisplayName("Admin Quotation Builder resets measurements to 0 when selecting another material")
    void testAdminQuotationBuilderResetsMeasurements() throws IOException {
        String content = Files.readString(Path.of("src/main/resources/templates/admin/quotation-builder.html"));

        // 1. Verify material button click passes null for fresh materials
        assertTrue(content.contains("const isSame = state.draft && state.draft.materialId === m.id && state.draft.divisionId === divisionId;"),
                "Admin builder must check if clicked material is already selected in draft");
        assertTrue(content.contains("const curMeas = isSame ? readActiveWorkTypeMeasurements(divisionId) : null;"),
                "Admin builder must pass null when selecting a different material");

        // 2. Verify selectWorkTypeMaterial checks allowInitial
        assertTrue(content.contains("const allowInitial = isSameMaterial || isGradeSwitch || isResumeOrEdit;"),
                "Admin builder must check allowInitial for dimensions");
        assertTrue(content.contains("const lenVal = (allowInitial && initialMeasurements && initialMeasurements.length != null"),
                "Admin builder must initialize lenVal to 0 when selecting another material");

        // 3. Verify readActiveWorkTypeMeasurements does not pull old dimensions from state.rooms
        assertFalse(content.contains("if ((!meas.length || !meas.quantity) && state.rooms)"),
                "Admin builder readActiveWorkTypeMeasurements must not resurrect old dimensions from state.rooms");

        // 4. Verify grade switcher sets isGradeSwitch = true to preserve counterpart material measurements
        assertTrue(content.contains("activeMeasurements.isGradeSwitch = true;"),
                "Admin builder handleTopPricingModeChange must preserve dimensions for counterpart materials");
    }

    @Test
    @DisplayName("Employee Quotation Builder resets measurements to 0 when selecting another material")
    void testEmployeeQuotationBuilderResetsMeasurements() throws IOException {
        String content = Files.readString(Path.of("src/main/resources/templates/employee/quotation-builder.html"));

        // 1. Verify material button click passes null for fresh materials
        assertTrue(content.contains("const isSame = state.draft && state.draft.materialId === m.id && state.draft.divisionId === divisionId;"),
                "Employee builder must check if clicked material is already selected in draft");
        assertTrue(content.contains("const curMeas = isSame ? readActiveWorkTypeMeasurements(divisionId) : null;"),
                "Employee builder must pass null when selecting a different material");

        // 2. Verify selectWorkTypeMaterial checks allowInitial
        assertTrue(content.contains("const allowInitial = isSameMaterial || isGradeSwitch || isResumeOrEdit;"),
                "Employee builder must check allowInitial for dimensions");
        assertTrue(content.contains("const lenVal = (allowInitial && initialMeasurements && initialMeasurements.length != null"),
                "Employee builder must initialize lenVal to 0 when selecting another material");

        // 3. Verify readActiveWorkTypeMeasurements does not pull old dimensions from state.rooms
        assertFalse(content.contains("if ((!meas.length || !meas.quantity) && state.rooms)"),
                "Employee builder readActiveWorkTypeMeasurements must not resurrect old dimensions from state.rooms");

        // 4. Verify grade switcher sets isGradeSwitch = true to preserve counterpart material measurements
        assertTrue(content.contains("activeMeasurements.isGradeSwitch = true;"),
                "Employee builder handleTopPricingModeChange must preserve dimensions for counterpart materials");
    }

    @Test
    @DisplayName("Admin and Employee Quotation Details View pages update Download PDF link with active grade")
    void testQuotationViewPdfDownloadLinkUpdatesWithGrade() throws IOException {
        String adminView = Files.readString(Path.of("src/main/resources/templates/admin/quotation-view.html"));
        assertTrue(adminView.contains("var pdfBtn = document.getElementById('btnDownloadPdf');"),
                "Admin view must reference btnDownloadPdf");
        assertTrue(adminView.contains("pdfBtn.setAttribute('href', pdfBase + '?grade=' + encodeURIComponent(data.activeGrade));"),
                "Admin view must update PDF href with active grade");

        String empWorkspace = Files.readString(Path.of("src/main/resources/templates/employee/quotation-workspace.html"));
        assertTrue(empWorkspace.contains("var pdfBtn = document.getElementById('btnDownloadPdf');"),
                "Employee workspace must reference btnDownloadPdf");
        assertTrue(empWorkspace.contains("pdfBtn.setAttribute('href', pdfBase + '?grade=' + encodeURIComponent(data.activeGrade));"),
                "Employee workspace must update PDF href with active grade");
    }
}
