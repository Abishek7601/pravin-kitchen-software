package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.MaterialController;
import com.example.pravin_quotation.controller.WorkCategoryController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.WorkCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AddMaterialQuotationNavigationTest {

    @Autowired
    private MaterialController materialController;

    @Autowired
    private WorkCategoryController workCategoryController;

    @Autowired
    private WorkCategoryService workCategoryService;

    @Autowired
    private AdminQuotationBuilderController quotationBuilderController;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private BranchRepository branchRepository;

    private WorkCategory modularKitchenCategory;
    private Division wallCabinetDivision;
    private Branch mainBranch;

    @BeforeEach
    public void setup() {
        mainBranch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setName("Chennai Central");
            b.setActive(true);
            return branchRepository.save(b);
        });

        modularKitchenCategory = new WorkCategory();
        modularKitchenCategory.setName("MODULAR KITCHEN");
        modularKitchenCategory.setIcon("🍳");
        modularKitchenCategory.setActive(true);
        modularKitchenCategory = workCategoryRepository.save(modularKitchenCategory);

        wallCabinetDivision = new Division();
        wallCabinetDivision.setName("WALL CABINET");
        wallCabinetDivision.setWorkCategory(modularKitchenCategory);
        wallCabinetDivision.setActive(true);
        wallCabinetDivision = divisionRepository.save(wallCabinetDivision);
    }

    @Test
    @DisplayName("1. MaterialController /new redirects to /admin/work-categories with category, work type, and returnUrl")
    public void testMaterialControllerNewRedirectsToWorkCategories() {
        Model model = new ConcurrentModel();
        String redirectResult = materialController.newMaterial(
                wallCabinetDivision.getId(),
                null,
                modularKitchenCategory.getId(),
                "MODULAR KITCHEN",
                "WALL CABINET",
                "/admin/quotations/builder",
                null,
                model
        );

        assertNotNull(redirectResult);
        assertTrue(redirectResult.startsWith("redirect:/admin/work-categories"), "Must redirect to /admin/work-categories");
        assertTrue(redirectResult.contains("workTypeId=" + wallCabinetDivision.getId()), "Must include target workTypeId");
        assertTrue(redirectResult.contains("addMaterial=true"), "Must include addMaterial=true flag");
        assertTrue(redirectResult.contains("categoryId=" + modularKitchenCategory.getId()), "Must include categoryId");
        assertTrue(redirectResult.contains("returnUrl="), "Must preserve returnUrl");
    }

    @Test
    @DisplayName("2. Save Material under Category and Work Type with Unit, Formula, Specification, and Branch Rates")
    public void testCreateMaterialWithBranchRatesUnderWorkType() {
        String branchRatesJson = String.format("{\"%d\":{\"PREMIUM\":1850.00,\"MIDDLE\":1450.00,\"ECONOMY\":1100.00}}", mainBranch.getId());

        ResponseEntity<?> response = workCategoryController.createWorkTypeMaterial(
                wallCabinetDivision.getId(),
                "PREMIUM",
                null,
                "ACRYLIC SHUTTER GLOSS 18MM",
                "L × W",
                null,
                "Century Marine Ply with 1mm Acrylic",
                "Sq.ft",
                "SQFT",
                null,
                new BigDecimal("1850.00"),
                branchRatesJson,
                Map.of()
        );

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> respBody = (Map<?, ?>) response.getBody();
        assertEquals(true, respBody.get("success"));
        assertNotNull(respBody.get("materialId"), "Response must return saved materialId");

        Long createdMaterialId = ((Number) respBody.get("materialId")).longValue();
        Material saved = materialRepository.findById(createdMaterialId).orElse(null);
        assertNotNull(saved);
        assertEquals("ACRYLIC SHUTTER GLOSS 18MM", saved.getName());
        assertEquals("L × W", saved.getFormula());
        assertEquals(MeasurementType.SQFT, saved.getMeasurementType());
        assertTrue(saved.getDivisions().contains(wallCabinetDivision), "Material must belong to the selected work type");

        // Verify the newly created material is immediately available in Quotation Builder
        List<Map<String, Object>> builderMaterials = quotationBuilderController.materials(
                mainBranch.getId(),
                wallCabinetDivision.getId(),
                null,
                "PREMIUM"
        );

        assertNotNull(builderMaterials);
        boolean foundInBuilder = builderMaterials.stream()
                .anyMatch(m -> createdMaterialId.equals(((Number) m.get("id")).longValue()));
        assertTrue(foundInBuilder, "Quotation builder material list must include newly added material under the selected work type");
    }

    @Test
    @DisplayName("3. Verify Quotation Builder HTML contains draft saving, restoration, and navigation logic")
    public void testQuotationBuilderTemplateContainsAddMaterialNavigationAndRestoration() throws Exception {
        String html = Files.readString(Paths.get("src/main/resources/templates/admin/quotation-builder.html"));

        assertTrue(html.contains("wt-add-material-btn"), "Quotation builder must include the + Add Material button");
        assertTrue(html.contains("openAddMaterial"), "Quotation builder must contain openAddMaterial handler");
        assertTrue(html.contains("saveQuotationBuilderDraft"), "Quotation builder must contain saveQuotationBuilderDraft");
        assertTrue(html.contains("restoreQuotationBuilderDraftIfPresent"), "Quotation builder must contain restoreQuotationBuilderDraftIfPresent");
        assertTrue(html.contains("pravin_quotation_builder_draft"), "Quotation builder must use pravin_quotation_builder_draft in sessionStorage");
        assertTrue(html.contains("savedMaterialId"), "Quotation builder must inspect savedMaterialId upon return");
        assertTrue(html.contains("/admin/work-categories?addMaterial=true"), "Quotation builder must navigate to /admin/work-categories with addMaterial=true");
    }

    @Test
    @DisplayName("4. Verify Work Categories HTML contains breadcrumb, auto-open, and returnUrl redirects")
    public void testWorkCategoriesTemplateContainsBreadcrumbAndReturnUrlHandling() throws Exception {
        String html = Files.readString(Paths.get("src/main/resources/templates/admin/work-categories.html"));

        assertTrue(html.contains("materialCategoryTypeBreadcrumb"), "Work categories page must display Category & Work Type breadcrumb in material form");
        assertTrue(html.contains("matFormCategoryName"), "Work categories page must display matFormCategoryName");
        assertTrue(html.contains("matFormWorkTypeName"), "Work categories page must display matFormWorkTypeName");
        assertTrue(html.contains("deepReturnUrl"), "Work categories page must support deepReturnUrl parameter");
        assertTrue(html.contains("deepAddMaterialPending"), "Work categories page must support deepAddMaterialPending auto-open");
        assertTrue(html.contains("savedMaterialId="), "Work categories page must append savedMaterialId on redirect back to quotation builder");
        assertTrue(html.contains("handleMaterialFormCancel"), "Work categories page must handle cancel and return to quotation builder");
    }
}

