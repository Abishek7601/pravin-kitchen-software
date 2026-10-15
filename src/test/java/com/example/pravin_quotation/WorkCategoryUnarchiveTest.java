package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.WorkCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import jakarta.persistence.EntityManager;
import java.io.InputStream;
import java.math.BigDecimal;
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
public class WorkCategoryUnarchiveTest {

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
    private QuotationRepository quotationRepository;

    @Autowired
    private QuotationItemRepository quotationItemRepository;

    @Autowired
    private WorkCategoryService workCategoryService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private EntityManager entityManager;

    private WorkCategory archivedCategory;
    private Division childDivision;
    private Material childMaterial;
    private MaterialOption childOption;
    private String categoryName;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        long ts = System.currentTimeMillis();
        categoryName = "UNARCHIVE_CAT_" + ts;

        archivedCategory = new WorkCategory();
        archivedCategory.setName(categoryName);
        archivedCategory.setDescription("Test category for unarchive functionality");
        archivedCategory.setIcon("🚪");
        archivedCategory.setDisplayOrder(5);
        archivedCategory.setActive(false);
        archivedCategory.setArchived(true);
        archivedCategory = workCategoryRepository.save(archivedCategory);

        childDivision = new Division();
        childDivision.setName("UNARCHIVE_DIV_" + ts);
        childDivision.setWorkCategory(archivedCategory);
        childDivision.setActive(false);
        childDivision.setDisplayOrder(1);
        childDivision = divisionRepository.save(childDivision);

        childMaterial = new Material();
        childMaterial.setName("UNARCHIVE_MAT_" + ts);
        childMaterial.getDivisions().add(childDivision);
        childMaterial.setActive(true);
        childMaterial = materialRepository.save(childMaterial);

        childOption = new MaterialOption();
        childOption.setName("UNARCHIVE_OPT_" + ts);
        childOption.setMaterial(childMaterial);
        childOption.setActive(true);
        childOption = materialOptionRepository.save(childOption);

        entityManager.flush();
    }

    @Test
    @DisplayName("Verify UI Templates contain Unarchive action and JS confirmation")
    void testUiTemplatesContainUnarchive() throws Exception {
        // 1. work-categories.html contains Unarchive action button and JS handler
        Resource wcRes = resourceLoader.getResource("classpath:templates/admin/work-categories.html");
        assertTrue(wcRes.exists(), "work-categories.html must exist");
        try (InputStream is = wcRes.getInputStream()) {
            String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(html).contains("th:action=\"@{/admin/work-categories/unarchive/{id}(id=${category.id})}\"");
            assertThat(html).contains("confirmUnarchiveCategory(this)");
            assertThat(html).contains("class=\"action-btn unarchive-btn\"");
            assertThat(html).contains("function confirmUnarchiveCategory(form)");
        }

        // 2. work-category-view.html contains Unarchive button in banner for archived category
        Resource wcvRes = resourceLoader.getResource("classpath:templates/admin/work-category-view.html");
        assertTrue(wcvRes.exists(), "work-category-view.html must exist");
        try (InputStream is = wcvRes.getInputStream()) {
            String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(html).contains("th:action=\"@{/admin/work-categories/unarchive/{id}(id=${category.id})}\"");
            assertThat(html).contains("Unarchive Category");
        }
    }

    @Test
    @DisplayName("Archived categories view displays archived category with Unarchive button")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testArchivedCategoriesViewDisplaysUnarchiveButton() throws Exception {
        mockMvc.perform(get("/admin/work-categories").param("showArchived", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isArchivedView", true))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(categoryName)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/admin/work-categories/unarchive/" + archivedCategory.getId() + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Unarchive")));
    }

    @Test
    @DisplayName("Unarchiving category restores it to active list and updates database correctly")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testUnarchiveWorkCategorySuccess() throws Exception {
        Long catId = archivedCategory.getId();

        mockMvc.perform(post("/admin/work-categories/unarchive/" + catId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/work-categories"))
                .andExpect(flash().attribute("successMessage", org.hamcrest.Matchers.containsString("successfully unarchived")));

        entityManager.flush();
        entityManager.clear();

        // Verify database state
        WorkCategory restored = workCategoryRepository.findById(catId).orElseThrow();
        assertFalse(restored.isArchived(), "Category must no longer be archived");
        assertTrue(restored.getActive(), "Category must be restored to active status");
        assertEquals(categoryName, restored.getName(), "Name must be preserved");
        assertEquals("🚪", restored.getIcon(), "Icon/emoji must be preserved");
        assertEquals("Test category for unarchive functionality", restored.getDescription());
        assertEquals(5, restored.getDisplayOrder());

        // Verify child division is also reactivated
        Division div = divisionRepository.findById(childDivision.getId()).orElseThrow();
        assertTrue(div.getActive(), "Child division must be reactivated");
        assertEquals(catId, div.getWorkCategory().getId(), "Division must still belong to this category");

        // Verify it appears in active categories list and count of archived is reduced
        List<WorkCategory> activeList = workCategoryService.getAllCategories();
        assertTrue(activeList.stream().anyMatch(c -> c.getId().equals(catId)), "Category must appear in active categories list");

        List<WorkCategory> archivedList = workCategoryService.getArchivedCategories();
        assertFalse(archivedList.stream().anyMatch(c -> c.getId().equals(catId)), "Category must NOT be in archived list");

        // Verify no duplicate category was created
        long countWithName = workCategoryRepository.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(categoryName))
                .count();
        assertEquals(1, countWithName, "There must be exactly one category with this name, no duplicate");
    }

    @Test
    @DisplayName("Quotation history and calculations are completely preserved when category is unarchived")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testQuotationHistoryPreservedAfterUnarchive() throws Exception {
        Long catId = archivedCategory.getId();

        // Create a historical quotation linked to this category
        Quotation quotation = new Quotation();
        quotation.setCustomerName("CUSTOMER UNARCHIVE TEST");
        quotation.setCustomerPhone("9876543210");
        quotation.setTotalAmount(new BigDecimal("50000.00"));
        quotation.setGrandTotal(new BigDecimal("50000.00"));
        quotation = quotationRepository.save(quotation);

        QuotationItem qItem = new QuotationItem();
        qItem.setQuotation(quotation);
        qItem.setWorkCategory(archivedCategory);
        qItem.setDivision(childDivision);
        qItem.setMaterial(childMaterial);
        qItem.setMaterialOption(childOption);
        qItem.setTotalAmount(new BigDecimal("50000.00"));
        qItem = quotationItemRepository.save(qItem);

        entityManager.flush();

        // Perform unarchive
        mockMvc.perform(post("/admin/work-categories/unarchive/" + catId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        entityManager.flush();
        entityManager.clear();

        // Verify quotation still exists and is 100% intact
        Quotation loadedQuotation = quotationRepository.findById(quotation.getId()).orElseThrow();
        assertEquals("CUSTOMER UNARCHIVE TEST", loadedQuotation.getCustomerName());
        assertEquals(new BigDecimal("50000.00"), loadedQuotation.getTotalAmount());
        assertEquals(new BigDecimal("50000.00"), loadedQuotation.getGrandTotal());

        QuotationItem loadedItem = quotationItemRepository.findById(qItem.getId()).orElseThrow();
        assertNotNull(loadedItem.getWorkCategory());
        assertEquals(catId, loadedItem.getWorkCategory().getId(), "Quotation item must still reference the unarchived category");
        assertEquals(new BigDecimal("50000.00"), loadedItem.getTotalAmount());
    }

    @Test
    @DisplayName("Unarchiving category via JSON request returns JSON response")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testUnarchiveWorkCategoryJson() throws Exception {
        Long catId = archivedCategory.getId();

        mockMvc.perform(post("/admin/work-categories/unarchive/" + catId)
                        .with(csrf())
                        .header("Accept", "application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("successfully unarchived")));

        entityManager.flush();
        WorkCategory restored = workCategoryRepository.findById(catId).orElseThrow();
        assertFalse(restored.isArchived());
    }

    @Test
    @DisplayName("Unarchive supports redirectUrl parameter")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testUnarchiveWithRedirectUrl() throws Exception {
        Long catId = archivedCategory.getId();
        String customRedirect = "/admin/work-categories/view/" + catId;

        mockMvc.perform(post("/admin/work-categories/unarchive/" + catId)
                        .with(csrf())
                        .param("redirectUrl", customRedirect))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(customRedirect));
    }
}
