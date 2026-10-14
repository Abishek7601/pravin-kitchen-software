package com.example.pravin_quotation;

import com.example.pravin_quotation.model.Division;
import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.model.MaterialOption;
import com.example.pravin_quotation.model.WorkCategory;
import com.example.pravin_quotation.repository.DivisionRepository;
import com.example.pravin_quotation.repository.MaterialOptionRepository;
import com.example.pravin_quotation.repository.MaterialRepository;
import com.example.pravin_quotation.model.MaterialSpecification;
import com.example.pravin_quotation.repository.MaterialSpecificationRepository;
import com.example.pravin_quotation.repository.WorkCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class MaterialOptionEditSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

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
    private ResourceLoader resourceLoader;

    private WorkCategory testCategory;
    private Division testDivision;
    private Material testMaterial;
    private MaterialOption testOption;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        testCategory = new WorkCategory();
        testCategory.setName("SECURITY TEST FOYER");
        testCategory.setActive(true);
        testCategory.setIcon("🚪");
        testCategory.setDisplayOrder(1);
        testCategory = workCategoryRepository.save(testCategory);

        testDivision = new Division();
        testDivision.setName("SECURITY TEST SHOE RACK");
        testDivision.setWorkCategory(testCategory);
        testDivision.setActive(true);
        testDivision.setDisplayOrder(1);
        testDivision = divisionRepository.save(testDivision);

        testMaterial = new Material();
        testMaterial.setName("SECURITY TEST PLYWOOD");
        testMaterial.getDivisions().add(testDivision);
        testMaterial.setActive(true);
        testMaterial.setDisplayOrder(1);
        testMaterial = materialRepository.save(testMaterial);

        testOption = new MaterialOption();
        testOption.setName("16MM COMMERCIAL");
        testOption.setDescription("Initial specification");
        testOption.setMaterial(testMaterial);
        testOption.setActive(true);
        testOption.setDisplayOrder(1);
        testOption = materialOptionRepository.save(testOption);
    }

    @Test
    @DisplayName("Root Cause Verification: POST without CSRF token returns 403 Forbidden")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void postWithoutCsrfToken_returns403Forbidden() throws Exception {
        // Without csrf() post-processor, Spring Security's CsrfFilter rejects with HTTP 403 Forbidden
        mockMvc.perform(post("/admin/material-options/update/" + testOption.getId())
                        .param("name", "18MM BWP")
                        .param("description", "Updated without CSRF")
                        .param("materialId", testMaterial.getId().toString())
                        .param("displayOrder", "2")
                        .param("redirectUrl", "/admin/work-categories/view/" + testCategory.getId()))
                .andExpect(status().isForbidden());

        // Verify the database record was NOT modified
        MaterialOption unmodified = materialOptionRepository.findById(testOption.getId()).orElseThrow();
        assertEquals("16MM COMMERCIAL", unmodified.getName());
        assertEquals("Initial specification", unmodified.getDescription());
    }

    @Test
    @DisplayName("Fix Verification: POST with valid CSRF token and SUPER_ADMIN succeeds with 302 and updates record")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void postWithCsrfToken_updatesExistingOptionSuccessfully() throws Exception {
        Long originalId = testOption.getId();
        long countBefore = materialOptionRepository.count();

        mockMvc.perform(post("/admin/material-options/update/" + originalId)
                        .with(csrf())
                        .param("name", "19mm marine bwp")
                        .param("description", "Waterproof boiling water proof grade A")
                        .param("materialId", testMaterial.getId().toString())
                        .param("displayOrder", "5")
                        .param("redirectUrl", "/admin/work-categories/view/" + testCategory.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/work-categories/view/" + testCategory.getId()))
                .andExpect(flash().attribute("successMessage", "Material option updated successfully."));

        // Verify record count unchanged (no duplicate created)
        long countAfter = materialOptionRepository.count();
        assertEquals(countBefore, countAfter, "Record count must remain the same without duplicates");

        // Verify record in database updated in place
        MaterialOption updated = materialOptionRepository.findById(originalId).orElseThrow();
        assertEquals(originalId, updated.getId());
        assertEquals("19MM MARINE BWP", updated.getName(), "Name must be converted to UPPERCASE");
        assertEquals("Waterproof boiling water proof grade A", updated.getDescription());
        assertEquals(5, updated.getDisplayOrder());
        assertEquals(testMaterial.getId(), updated.getMaterial().getId(), "Parent Material association must be preserved");

        // Verify MaterialSpecification was synchronized and updated in place
        List<MaterialSpecification> specs = materialSpecificationRepository.findByMaterialOptionIdOrderByDisplayOrderAsc(originalId);
        assertFalse(specs.isEmpty(), "MaterialSpecification must be synchronized with updated option description");
        assertEquals("WATERPROOF BOILING WATER PROOF GRADE A", specs.get(0).getName());
        assertEquals("Waterproof boiling water proof grade A", specs.get(0).getDescription());
        assertEquals(testMaterial.getId(), specs.get(0).getMaterial().getId());
    }

    @Test
    @DisplayName("Security Check: Unauthorized employee user (with OTP verified) gets 403 Forbidden even with CSRF token")
    @WithMockUser(username = "emp", roles = {"EMPLOYEE"})
    void postByEmployee_returns403Forbidden() throws Exception {
        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute("OTP_VERIFIED", true);

        mockMvc.perform(post("/admin/material-options/update/" + testOption.getId())
                        .session(session)
                        .with(csrf())
                        .param("name", "HACKED NAME")
                        .param("materialId", testMaterial.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security Check: Employee without OTP verified gets redirected to /employee/otp")
    @WithMockUser(username = "emp", roles = {"EMPLOYEE"})
    void postByEmployeeWithoutOtp_redirectsToOtp() throws Exception {
        mockMvc.perform(post("/admin/material-options/update/" + testOption.getId())
                        .with(csrf())
                        .param("name", "HACKED NAME")
                        .param("materialId", testMaterial.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"));
    }

    @Test
    @DisplayName("Security Check: Unauthenticated request redirects to /login")
    void postUnauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/admin/material-options/update/" + testOption.getId())
                        .with(csrf())
                        .param("name", "ANONYMOUS UPDATE")
                        .param("materialId", testMaterial.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Validation Check: Empty name returns graceful error flash attribute without Whitelabel 403/500")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void postWithEmptyName_redirectsWithErrorMessage() throws Exception {
        mockMvc.perform(post("/admin/material-options/update/" + testOption.getId())
                        .with(csrf())
                        .param("name", "   ")
                        .param("description", "Blank name attempt")
                        .param("materialId", testMaterial.getId().toString())
                        .param("redirectUrl", "/admin/work-categories/view/" + testCategory.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/work-categories/view/" + testCategory.getId()))
                .andExpect(flash().attribute("errorMessage", "Material option name is required"));

        // Value in DB should remain unchanged
        MaterialOption current = materialOptionRepository.findById(testOption.getId()).orElseThrow();
        assertEquals("16MM COMMERCIAL", current.getName());
    }

    @Test
    @DisplayName("Template Verification: work-category-view.html includes CSRF meta tags and hidden CSRF form input")
    void verifyWorkCategoryViewTemplateContainsCsrf() throws Exception {
        Resource res = resourceLoader.getResource("classpath:templates/admin/work-category-view.html");
        assertTrue(res.exists(), "work-category-view.html must exist");
        try (InputStream is = res.getInputStream()) {
            String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // CSRF meta tags in head
            assertThat(html).contains("<meta name=\"_csrf\"");
            assertThat(html).contains("<meta name=\"_csrf_header\"");

            // CSRF hidden input in modal form
            assertThat(html).contains("id=\"editOptionForm\"");
            assertThat(html).contains("th:action=\"@{/admin/material-options/update/0}\"");
            assertThat(html).contains("th:name=\"${_csrf.parameterName}\"");
            assertThat(html).contains("th:value=\"${_csrf.token}\"");

            // Success and Error alert containers
            assertThat(html).contains("th:if=\"${successMessage}\"");
            assertThat(html).contains("th:if=\"${errorMessage}\"");
        }
    }

    @Test
    @DisplayName("Template Verification: material-option-form.html includes hidden CSRF form input and alert containers")
    void verifyMaterialOptionFormTemplateContainsCsrf() throws Exception {
        Resource res = resourceLoader.getResource("classpath:templates/admin/material-option-form.html");
        assertTrue(res.exists(), "material-option-form.html must exist");
        try (InputStream is = res.getInputStream()) {
            String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(html).contains("th:name=\"${_csrf.parameterName}\"");
            assertThat(html).contains("th:value=\"${_csrf.token}\"");
            assertThat(html).contains("th:if=\"${successMessage}\"");
            assertThat(html).contains("th:if=\"${errorMessage}\"");
        }
    }

    @Test
    @DisplayName("Data Flow Verification: Edited Material Option updates are persisted in DB and reflected under Material Specifications")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testMaterialOptionUpdateReflectedInSpecifications() throws Exception {
        // Step 1: Pre-populate an initial MaterialSpecification under testOption
        MaterialSpecification initialSpec = new MaterialSpecification();
        initialSpec.setName("MATTE FINISH");
        initialSpec.setDescription("Matte finish specification");
        initialSpec.setMaterial(testMaterial);
        initialSpec.setMaterialOption(testOption);
        initialSpec.setActive(true);
        initialSpec.setDisplayOrder(0);
        initialSpec = materialSpecificationRepository.save(initialSpec);
        Long specId = initialSpec.getId();

        long specCountBefore = materialSpecificationRepository.count();

        // Step 2: Edit Material Option via POST Save Changes with new specification
        mockMvc.perform(post("/admin/material-options/update/" + testOption.getId())
                        .with(csrf())
                        .param("name", "18MM BWP")
                        .param("description", "GLOSS ACRYLIC FINISH")
                        .param("materialId", testMaterial.getId().toString())
                        .param("displayOrder", "1")
                        .param("redirectUrl", "/admin/work-categories/view/" + testCategory.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/work-categories/view/" + testCategory.getId()))
                .andExpect(flash().attribute("successMessage", "Material option updated successfully."));

        // Step 3: Verify DB updates: Existing specification record modified in place (no duplicate)
        long specCountAfter = materialSpecificationRepository.count();
        assertEquals(specCountBefore, specCountAfter, "Specification count must remain the same (updated in place)");

        MaterialSpecification updatedSpec = materialSpecificationRepository.findById(specId).orElseThrow();
        assertEquals("GLOSS ACRYLIC FINISH", updatedSpec.getName(), "Specification name must be updated in DB in UPPERCASE");
        assertEquals("GLOSS ACRYLIC FINISH", updatedSpec.getDescription(), "Specification description must be updated in DB");
        assertEquals(testOption.getId(), updatedSpec.getMaterialOption().getId(), "Relationship with MaterialOption preserved");
        assertEquals(testMaterial.getId(), updatedSpec.getMaterial().getId(), "Relationship with Material preserved");

        // Step 4: Verify Work Category View displays the updated Material Specification
        mockMvc.perform(get("/admin/work-categories/view/" + testCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("GLOSS ACRYLIC FINISH")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("18MM BWP")));

        // Step 5: Verify Quotation Builder API retrieves the updated Material Specification
        mockMvc.perform(get("/admin/quotations/builder/material-specifications")
                        .param("materialOptionId", testOption.getId().toString())
                        .param("materialId", testMaterial.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("GLOSS ACRYLIC FINISH")));

        // Step 6: Verify Pricing API retrieves the updated Material Specification
        mockMvc.perform(get("/admin/pricing/api/material-specifications")
                        .param("materialOptionId", testOption.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("GLOSS ACRYLIC FINISH")));

        // Step 7: Page refresh verification: second GET still retrieves persisted updated value
        mockMvc.perform(get("/admin/work-categories/view/" + testCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("GLOSS ACRYLIC FINISH")));
    }

    @Test
    @DisplayName("Data Flow Verification: Creating a new Material Option automatically links and saves MaterialSpecification")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testNewMaterialOptionSyncsInitialSpecification() throws Exception {
        mockMvc.perform(post("/admin/material-options/save")
                        .with(csrf())
                        .param("name", "12MM PLYWOOD")
                        .param("description", "CENTURY CLUB WATER RESISTANT")
                        .param("materialId", testMaterial.getId().toString())
                        .param("displayOrder", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/material-options"));

        // Verify Option created
        MaterialOption opt = materialOptionRepository
                .findByMaterialIdOrderByDisplayOrderAsc(testMaterial.getId())
                .stream()
                .filter(o -> o.getName().equals("12MM PLYWOOD"))
                .findFirst()
                .orElseThrow();

        // Verify Specification automatically linked and persisted
        List<MaterialSpecification> specs = materialSpecificationRepository.findByMaterialOptionIdOrderByDisplayOrderAsc(opt.getId());
        assertFalse(specs.isEmpty(), "Specification must be automatically created and linked to the option");
        assertEquals("CENTURY CLUB WATER RESISTANT", specs.get(0).getName());

        // Verify Work Category view renders this specification
        mockMvc.perform(get("/admin/work-categories/view/" + testCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CENTURY CLUB WATER RESISTANT")));
    }
}
