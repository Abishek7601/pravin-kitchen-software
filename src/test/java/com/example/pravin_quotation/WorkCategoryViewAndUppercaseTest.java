package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.controller.MasterDataHierarchyController;
import com.example.pravin_quotation.controller.MaterialOptionController;
import com.example.pravin_quotation.controller.WorkCategoryController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.*;
import com.example.pravin_quotation.util.CalculationFormula;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class WorkCategoryViewAndUppercaseTest {

    @Autowired
    private PricingService pricingService;

    @Autowired
    private WorkCategoryService workCategoryService;

    @Autowired
    private DivisionService divisionService;

    @Autowired
    private MaterialOptionService materialOptionService;

    @Autowired
    private MaterialSpecificationService materialSpecificationService;

    @Autowired
    private MasterDataHierarchyController masterDataHierarchyController;

    @Autowired
    private WorkCategoryController workCategoryController;

    @Autowired
    private AdminQuotationBuilderController adminQuotationBuilderController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialOptionRepository materialOptionRepository;

    @Autowired
    private MaterialSpecificationRepository materialSpecificationRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    @DisplayName("Req 1 & 3: Configure Master Data automatically converts all entered text to UPPERCASE and trims spaces")
    public void testConfigureMasterDataUppercaseConversion() {
        // Save using mixed case and extra whitespaces
        Pricing pricing = pricingService.saveUnifiedMasterData(
                null,
                null, "  foyer  ", "🏠",
                null, "  shoe rack  ",
                null, "  plywood  ",
                null, "  marine ply  ",
                "  1mm edge band  ",
                PricingMode.PREMIUM,
                new BigDecimal("1250.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_W,
                "Foyer shoe rack entry"
        );

        assertNotNull(pricing);
        assertNotNull(pricing.getMaterialOption());
        assertEquals("MARINE PLY", pricing.getMaterialOption().getName(), "Material option name must be converted to UPPERCASE");

        Material material = pricing.getMaterialOption().getMaterial();
        assertNotNull(material);
        assertEquals("PLYWOOD", material.getName(), "Material name must be converted to UPPERCASE");

        WorkCategory wc = workCategoryRepository.findByNameIgnoreCase("FOYER").orElseThrow();
        assertEquals("FOYER", wc.getName(), "Work category name must be converted to UPPERCASE");
        assertEquals("🏠", wc.getIcon());

        Division div = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(wc.getId()).get(0);
        assertEquals("SHOE RACK", div.getName(), "Division name must be converted to UPPERCASE");

        List<MaterialSpecification> specs = materialSpecificationRepository.findByMaterialOptionIdAndActiveTrueOrderByDisplayOrderAsc(pricing.getMaterialOption().getId());
        assertFalse(specs.isEmpty());
        assertEquals("1MM EDGE BAND", specs.get(0).getName(), "Material specification name must be converted to UPPERCASE");
    }

    @Test
    @DisplayName("Req 3: Direct creation services convert names to UPPERCASE and prevent duplicates")
    public void testDirectCreationUppercaseAndNoDuplicates() {
        // WorkCategoryService
        WorkCategory wc1 = workCategoryService.createCategory("kitchen", "Kitchen desc", "🍳", null);
        assertEquals("KITCHEN", wc1.getName());

        assertThrows(IllegalArgumentException.class, () -> {
            workCategoryService.createCategory("  KITCHEN  ", "Kitchen desc duplicate", "🍳", null);
        }, "Case-insensitive duplicate category creation should be blocked");

        // DivisionService
        Division div1 = divisionService.createDivision("counter top", "Counter desc", wc1.getId(), null);
        assertEquals("COUNTER TOP", div1.getName());

        assertThrows(IllegalArgumentException.class, () -> {
            divisionService.createDivision("COUNTER TOP", "Duplicate div", wc1.getId(), null);
        }, "Case-insensitive duplicate division creation should be blocked");

        // MaterialOptionService
        Material mat = new Material();
        mat.setName("GRANITE");
        mat.setActive(true);
        mat.addDivision(div1);
        mat = materialRepository.save(mat);

        MaterialOption opt1 = materialOptionService.createOption("black galaxy", "Polished slab", mat.getId(), null);
        assertEquals("BLACK GALAXY", opt1.getName());

        // MaterialSpecificationService
        MaterialSpecification spec1 = materialSpecificationService.findOrCreateSpecification(mat, opt1, "bullnose chamfer");
        assertEquals("BULLNOSE CHAMFER", spec1.getName());

        MaterialSpecification spec2 = materialSpecificationService.findOrCreateSpecification(mat, opt1, "  BULLNOSE CHAMFER  ");
        assertEquals(spec1.getId(), spec2.getId(), "Duplicate specification should be reused");
    }

    @Test
    @DisplayName("Req 1: Work Category View Details returns clean hierarchy scoped only to selected Work Category")
    public void testWorkCategoryViewDetailsHierarchy() {
        String suffix = String.valueOf(System.currentTimeMillis());
        String foyerCatName = "FOYER_" + suffix;
        String bedroomCatName = "BEDROOM_" + suffix;

        // 1. Create FOYER with SHOE RACK
        pricingService.saveUnifiedMasterData(
                null,
                null, foyerCatName, "🏠",
                null, "SHOE RACK",
                null, "PLYWOOD",
                null, "COMMERCIAL PLY",
                "PVC EDGE BAND",
                PricingMode.PREMIUM,
                new BigDecimal("950.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_W,
                "Foyer note"
        );

        // 2. Create BEDROOM with WARDROBE
        pricingService.saveUnifiedMasterData(
                null,
                null, bedroomCatName, "🛏️",
                null, "WARDROBE",
                null, "MDF BOARD",
                null, "HDHMR",
                "ACRYLIC FINISH",
                PricingMode.PREMIUM,
                new BigDecimal("1650.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_PLUS_W_X_H,
                "Bedroom note"
        );

        WorkCategory foyer = workCategoryRepository.findByNameIgnoreCase(foyerCatName).orElseThrow();
        WorkCategory bedroom = workCategoryRepository.findByNameIgnoreCase(bedroomCatName).orElseThrow();

        // View FOYER via MasterDataHierarchyController
        Model foyerModel = new ConcurrentModel();
        String foyerView = masterDataHierarchyController.viewCategoryDetails(foyer.getId(), foyerModel);
        assertEquals("admin/work-category-view", foyerView);

        WorkCategory modelCategory = (WorkCategory) foyerModel.getAttribute("category");
        assertNotNull(modelCategory);
        assertEquals(foyerCatName.toUpperCase(), modelCategory.getName());

        MasterDataHierarchyController.HierarchyCategoryDto catDto =
                (MasterDataHierarchyController.HierarchyCategoryDto) foyerModel.getAttribute("categoryDto");
        assertNotNull(catDto);
        assertEquals(foyerCatName.toUpperCase(), catDto.getCategory().getName());
        assertEquals(1, catDto.getDivisions().size(), "Only divisions for FOYER should be present");

        MasterDataHierarchyController.HierarchyDivisionDto shoeRackDiv = catDto.getDivisions().get(0);
        assertEquals("SHOE RACK", shoeRackDiv.getDivision().getName());

        // Ensure BEDROOM and WARDROBE are NOT present in FOYER view
        boolean containsBedroom = catDto.getCategory().getName().equalsIgnoreCase(bedroomCatName);
        assertFalse(containsBedroom, "Foyer view must NOT include Bedroom category");

        boolean containsWardrobe = shoeRackDiv.getDivision().getName().equalsIgnoreCase("WARDROBE");
        assertFalse(containsWardrobe, "Foyer view must NOT include Wardrobe division");

        // Also test WorkCategoryController route
        Model wcControllerModel = new ConcurrentModel();
        String wcView = workCategoryController.viewCategory(foyer.getId(), wcControllerModel);
        assertEquals("admin/work-category-view", wcView);
        assertTrue(wcControllerModel.containsAttribute("category"));
        assertTrue(wcControllerModel.containsAttribute("categoryDto"));
    }

    @Autowired
    private MaterialOptionController materialOptionController;

    @Test
    @DisplayName("Req 2: Verify Dashboard Cards for Divisions, Materials, Material Options, and Complete Hierarchy have been removed")
    public void testDashboardCardsRemoved() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:templates/admin/dashboard.html");
        assertTrue(resource.exists(), "dashboard.html must exist");

        String content;
        try (InputStream is = resource.getInputStream()) {
            content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Must NOT contain standalone dashboard card links to divisions, materials, and material-options
        assertFalse(content.contains("href=\"/admin/divisions\"") || content.contains("@{/admin/divisions}"),
                "Dashboard must not contain standalone link to /admin/divisions");
        assertFalse(content.contains("href=\"/admin/materials\"") || content.contains("@{/admin/materials}"),
                "Dashboard must not contain standalone link to /admin/materials");
        assertFalse(content.contains("href=\"/admin/material-options\"") || content.contains("@{/admin/material-options}"),
                "Dashboard must not contain standalone link to /admin/material-options");

        // Must NOT contain Complete Hierarchy on the dashboard
        assertFalse(content.contains("Complete Hierarchy"),
                "Dashboard must not contain Complete Hierarchy button or card");

        // Verify shortcut cards for Work Categories and Pricing are removed from dashboard
        assertFalse(content.contains("@{/admin/work-categories}") || content.contains("/admin/work-categories"),
                "Dashboard must not contain shortcut card for Work Categories");
        assertFalse(content.contains("@{/admin/pricing}") || content.contains("/admin/pricing"),
                "Dashboard must not contain shortcut card for Pricing");
    }

    @Test
    @DisplayName("Req 1: Material Option Edit from Work Category View updates existing record in place without duplication")
    public void testMaterialOptionEditInWorkCategoryView() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String catName = "BALCONY_" + suffix;

        Pricing saved = pricingService.saveUnifiedMasterData(
                null,
                null, catName, "🌿",
                null, "DECKING",
                null, "WPC PLANK",
                null, "SOLID 20MM",
                "TEXTURED SURFACE",
                PricingMode.PREMIUM,
                new BigDecimal("320.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_W,
                "Balcony decking note"
        );

        assertNotNull(saved);
        MaterialOption opt = saved.getMaterialOption();
        assertNotNull(opt);
        Long optId = opt.getId();
        Long matId = opt.getMaterial().getId();
        WorkCategory cat = workCategoryRepository.findByNameIgnoreCase(catName).orElseThrow();

        // 1. Verify view template contains the Edit button and Modal
        Resource resource = resourceLoader.getResource("classpath:templates/admin/work-category-view.html");
        assertTrue(resource.exists());
        String html;
        try (InputStream is = resource.getInputStream()) {
            html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(html.contains("btn-edit-option") || html.contains("Edit Material Option"),
                "Work category view must contain Edit Material Option trigger");
        assertTrue(html.contains("id=\"editOptionModal\""),
                "Work category view must contain editOptionModal");

        // 2. Perform Edit via MaterialOptionController
        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String viewUrl = "/admin/work-categories/view/" + cat.getId();
        String result = materialOptionController.updateOption(
                optId,
                "solid 25mm heavy duty",
                "Updated heavy duty WPC plank",
                matId,
                5,
                viewUrl,
                redirectAttributes
        );

        // 3. Verify redirect URL returns immediately to the Work Category view
        assertEquals("redirect:" + viewUrl, result, "Should redirect directly back to the work category view page");

        // 4. Verify existing record was updated in place (NO duplicate record created)
        MaterialOption updated = materialOptionRepository.findById(optId).orElseThrow();
        assertEquals("SOLID 25MM HEAVY DUTY", updated.getName(), "Name must be updated and converted to UPPERCASE");
        assertEquals("Updated heavy duty WPC plank", updated.getDescription());
        assertEquals(5, updated.getDisplayOrder());

        long countForMaterial = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(matId).size();
        assertEquals(1, countForMaterial, "Exactly 1 option must exist under material, no duplicate record should be created");

        // 5. Verify Work Category View loads updated values immediately
        Model viewModel = new ConcurrentModel();
        String catViewName = workCategoryController.viewCategory(cat.getId(), viewModel);
        assertEquals("admin/work-category-view", catViewName);

        MasterDataHierarchyController.HierarchyCategoryDto catDto =
                (MasterDataHierarchyController.HierarchyCategoryDto) viewModel.getAttribute("categoryDto");
        assertNotNull(catDto);
        MasterDataHierarchyController.HierarchyOptionDto loadedOptDto = catDto.getDivisions().get(0)
                .getMaterials().get(0)
                .getOptions().get(0);
        assertEquals("SOLID 25MM HEAVY DUTY", loadedOptDto.getOption().getName(),
                "Work category view hierarchy must display updated option name immediately");
    }

    @Test
    @DisplayName("Req 4: Quotation Builder APIs return UPPERCASE names for dropdowns and choice cards")
    public void testQuotationBuilderApisUppercase() {
        // Setup master data
        pricingService.saveUnifiedMasterData(
                null,
                null, "LIVING ROOM", "🛋️",
                null, "TV UNIT",
                null, "VENEER",
                null, "TEAK VENEER",
                "PU POLISH",
                PricingMode.PREMIUM,
                new BigDecimal("2100.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                "Living room TV unit"
        );

        WorkCategory living = workCategoryRepository.findByNameIgnoreCase("LIVING ROOM").orElseThrow();

        // 1. Admin Divisions API
        List<Map<String, Object>> divs = adminQuotationBuilderController.divisions(living.getId());
        assertNotNull(divs);
        assertFalse(divs.isEmpty());
        for (Map<String, Object> d : divs) {
            String name = (String) d.get("name");
            assertEquals(name.toUpperCase(), name, "Division name must be uppercase");
        }

        Long divId = (Long) divs.get(0).get("id");

        // 2. Admin Items API
        List<Map<String, Object>> items = adminQuotationBuilderController.items(divId);
        assertNotNull(items);
        assertFalse(items.isEmpty());
        for (Map<String, Object> item : items) {
            String name = (String) item.get("name");
            assertEquals(name.toUpperCase(), name, "Item name must be uppercase");
        }

        Long itemId = (Long) items.get(0).get("id");

        // 3. Admin Materials API
        List<Map<String, Object>> mats = adminQuotationBuilderController.materials(divId, itemId);
        assertNotNull(mats);
        assertFalse(mats.isEmpty());
        Map<String, Object> mat = mats.get(0);
        String matName = (String) mat.get("name");
        assertEquals(matName.toUpperCase(), matName, "Material name must be uppercase");

        Long matId = (Long) mat.get("id");

        // 4. Admin Material Options API
        List<Map<String, Object>> opts = adminQuotationBuilderController.materialOptions(matId);
        assertNotNull(opts);
        assertFalse(opts.isEmpty());
        for (Map<String, Object> opt : opts) {
            String name = (String) opt.get("name");
            assertEquals(name.toUpperCase(), name, "Material option name must be uppercase");
        }

        Long optId = (Long) opts.get(0).get("id");

        // 5. Admin Material Specifications API
        List<Map<String, Object>> specs = adminQuotationBuilderController.materialSpecifications(optId, matId);
        assertNotNull(specs);
        assertFalse(specs.isEmpty());
        for (Map<String, Object> spec : specs) {
            String specName = (String) spec.get("name");
            assertEquals(specName.toUpperCase(), specName, "Material specification name must be uppercase");
        }

        // 6. Employee Quotation Controller APIs
        List<Map<String, Object>> empDivs = employeeQuotationController.builderDivisions(living.getId());
        for (Map<String, Object> d : empDivs) {
            String name = (String) d.get("name");
            assertEquals(name.toUpperCase(), name, "Employee division API must return uppercase");
        }

        List<Map<String, Object>> empOpts = employeeQuotationController.builderMaterialOptions(matId);
        for (Map<String, Object> o : empOpts) {
            String name = (String) o.get("name");
            assertEquals(name.toUpperCase(), name, "Employee options API must return uppercase");
        }

        List<Map<String, Object>> empSpecs = employeeQuotationController.builderMaterialSpecifications(optId, matId);
        for (Map<String, Object> s : empSpecs) {
            String name = (String) s.get("name");
            assertEquals(name.toUpperCase(), name, "Employee specifications API must return uppercase");
        }
    }

    @Test
    @DisplayName("Req: Verify Brand / Specification field in Add/Edit Material form is a multiline textarea with proper styling and multiline support")
    public void testMaterialSpecificationFieldIsTextareaWithMultilineSupport() throws Exception {
        // 1. Check HTML template structure
        Resource resource = resourceLoader.getResource("classpath:templates/admin/work-categories.html");
        assertTrue(resource.exists(), "work-categories.html must exist");

        String content;
        try (InputStream is = resource.getInputStream()) {
            content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Must contain label: Brand / Specification (Optional)
        assertTrue(content.contains("Brand / Specification"), "Template must contain 'Brand / Specification'");
        assertTrue(content.contains("(Optional)"), "Template must contain '(Optional)'");

        // Must NOT contain input type="text" id="materialSpecInput"
        assertFalse(content.contains("<input type=\"text\" id=\"materialSpecInput\""),
                "Field must no longer be a single-line input");

        // Must contain <textarea id="materialSpecInput"
        assertTrue(content.contains("<textarea id=\"materialSpecInput\""),
                "Field must be a multiline textarea");
        assertTrue(content.contains("placeholder=\"e.g. Century Ply Club Prime 16mm, IS:710 Marine Grade\""),
                "Placeholder must be preserved");
        assertTrue(content.contains("resize: vertical;"),
                "Textarea must support vertical resizing");

        // 2. Test saving and editing multiline specification via service/controller
        WorkCategory testCategory = workCategoryService.createCategory(
                "SPEC_TEST_" + System.nanoTime(),
                "Description",
                "🏷️",
                0
        );

        Division testDivision = workCategoryService.createWorkType(
                testCategory.getId(),
                "Wardrobe Specs " + System.nanoTime(),
                "Description",
                "🚪"
        );

        String multilineSpec = "Century Ply Club Prime 16mm\nIS:710 Marine Grade BWP\n10-Year Manufacturer Warranty";

        // Create material with multiline specification
        Material created = workCategoryService.createWorkTypeMaterial(
                testDivision.getId(),
                PricingMode.PREMIUM,
                "Premium Marine Ply " + System.nanoTime(),
                multilineSpec,
                "Sq.ft",
                new BigDecimal("450.00"),
                "L × W",
                Map.of(),
                MeasurementType.SQFT,
                null
        );

        assertNotNull(created.getId());
        assertEquals(multilineSpec, created.getSpecification(), "Multiline specification must be preserved on creation");
        assertTrue(created.getSpecification().contains("\n"), "Specification must contain line breaks");

        // Update material with updated multiline specification
        String updatedMultilineSpec = "Greenlam Laminate 1mm Glossy\nAnti-Bacterial Coating\nScratch & Heat Resistant";
        Material updated = workCategoryService.updateWorkTypeMaterial(
                created.getId(),
                created.getName(),
                updatedMultilineSpec,
                "Sq.ft",
                new BigDecimal("500.00"),
                "L × W",
                Map.of(),
                MeasurementType.SQFT,
                null,
                false
        );

        assertEquals(updatedMultilineSpec, updated.getSpecification(), "Updated multiline specification must be preserved on update");

        // Fetch JSON via WorkCategoryController and verify multiline specification returned
        org.springframework.http.ResponseEntity<?> response = workCategoryController.getWorkTypeMaterials(testDivision.getId());
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, Object>>> materialsByGrade = (Map<String, List<Map<String, Object>>>) body.get("materials");
        assertNotNull(materialsByGrade);
        Map<String, Object> targetMat = materialsByGrade.values().stream()
                .flatMap(List::stream)
                .filter(m -> m.get("id").equals(created.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(targetMat, "Created material must be present in JSON response");
        assertEquals(updatedMultilineSpec, targetMat.get("specification"), "JSON response must contain exact multiline specification with line breaks");
    }

    @Test
    @DisplayName("Req 6: Automatic Text Capitalization for Material Name (UPPERCASE) and Brand / Specification (Title Case)")
    public void testMaterialNameUppercaseAndBrandSpecificationTitleCase() throws Exception {
        WorkCategory testCategory = workCategoryService.createCategory(
                "CAP_TEST_" + System.nanoTime(),
                "Description",
                "📐",
                0
        );

        Division testDivision = workCategoryService.createWorkType(
                testCategory.getId(),
                "Living Area " + System.nanoTime(),
                "Living Room Types",
                "🛋️"
        );

        Branch branch = new Branch();
        branch.setName("Branch " + System.nanoTime());
        branch.setActive(true);
        branch = branchRepository.save(branch);

        // 1. Create with lowercase material name: "bwp plywood" and spec: "century club prime 16mm"
        Material mat1 = workCategoryService.createWorkTypeMaterial(
                testDivision.getId(),
                PricingMode.PREMIUM,
                "bwp plywood",
                "century club prime 16mm",
                "Sq.ft",
                new BigDecimal("400.00"),
                "L × W",
                Map.of(),
                MeasurementType.SQFT,
                null
        );

        assertNotNull(mat1.getId());
        assertEquals("BWP PLYWOOD", mat1.getName(), "Material name must automatically be normalized to ALL CAPITAL LETTERS");
        assertEquals("Century Club Prime 16mm", mat1.getSpecification(), "Brand / Specification must have first letter of each word capitalized");

        // 2. Create with "gypsum board + finish" and spec "marine grade plywood\nasian paints royal finish"
        String rawSpec2 = "marine grade plywood\nasian paints royal finish";
        String expectedSpec2 = "Marine Grade Plywood\nAsian Paints Royal Finish";
        Material mat2 = workCategoryService.createWorkTypeMaterial(
                testDivision.getId(),
                PricingMode.PREMIUM,
                "gypsum board + finish",
                rawSpec2,
                "Sq.ft",
                new BigDecimal("350.00"),
                "L × W",
                Map.of(),
                MeasurementType.SQFT,
                null
        );

        assertEquals("GYPSUM BOARD + FINISH", mat2.getName(), "Material name with symbols/spaces must be ALL UPPERCASE");
        assertEquals(expectedSpec2, mat2.getSpecification(), "Multiline Brand/Spec must preserve newlines and capitalize first letter of each word");

        // 3. Update existing material to new lowercase values
        Material updatedMat1 = workCategoryService.updateWorkTypeMaterial(
                mat1.getId(),
                "pvc foam sheet",
                "greenply club 18mm\nheat & scratch resistant",
                "Sq.ft",
                new BigDecimal("420.00"),
                "L × W",
                Map.of(),
                MeasurementType.SQFT,
                null,
                false
        );

        assertEquals("PVC FOAM SHEET", updatedMat1.getName(), "Updated material name must be stored in UPPERCASE");
        assertEquals("Greenply Club 18mm\nHeat & Scratch Resistant", updatedMat1.getSpecification(), "Updated specification must follow title capitalization format");

        // 4. Verify WorkCategoryController endpoints enforce normalization
        org.springframework.http.ResponseEntity<?> resp1 = workCategoryController.createWorkTypeMaterial(
                testDivision.getId(),
                "PREMIUM",
                null,
                "acrylic laminate",
                "L × W",
                null,
                "century club prime 16mm, is:710",
                "Sq.ft",
                "SQFT",
                null,
                new BigDecimal("550.00"),
                "{}",
                Map.of("rate_branch_" + branch.getId() + "_PREMIUM", "550.00")
        );
        assertEquals(200, resp1.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> respBody1 = (Map<String, Object>) resp1.getBody();
        assertNotNull(respBody1);
        Long createdMatId = (Long) respBody1.get("materialId");
        assertNotNull(createdMatId);
        Material fetchedMat = materialRepository.findById(createdMatId).orElseThrow();
        assertEquals("ACRYLIC LAMINATE", fetchedMat.getName(), "Controller endpoint must normalize Material Name to UPPERCASE");
        assertEquals("Century Club Prime 16mm, Is:710", fetchedMat.getSpecification(), "Controller endpoint must normalize Brand/Spec to Title Case");

        // 5. Verify HTML Templates configuration
        java.io.File workCatFile = new java.io.File("src/main/resources/templates/admin/work-categories.html");
        String workCatContent = java.nio.file.Files.readString(workCatFile.toPath());
        assertTrue(workCatContent.contains("id=\"materialNameInput\""), "work-categories.html must contain materialNameInput");
        assertTrue(workCatContent.contains("text-transform: uppercase"), "materialNameInput must have uppercase text-transform style");
        assertTrue(workCatContent.contains("id=\"materialSpecInput\""), "work-categories.html must contain materialSpecInput");
        assertTrue(workCatContent.contains("title-capitalization.js"), "work-categories.html must include title-capitalization.js");
        assertTrue(workCatContent.contains("handleMaterialNameUppercase"), "work-categories.html must attach uppercase live handler");
        assertTrue(workCatContent.contains("handleMaterialSpecTitleCase"), "work-categories.html must attach title case live handler");

        java.io.File matFormFile = new java.io.File("src/main/resources/templates/admin/material-form.html");
        String matFormContent = java.nio.file.Files.readString(matFormFile.toPath());
        assertTrue(matFormContent.contains("uppercase-input"), "material-form.html must have uppercase-input class");
        assertTrue(matFormContent.contains("title-capitalization.js"), "material-form.html must include title-capitalization.js");
    }
}
