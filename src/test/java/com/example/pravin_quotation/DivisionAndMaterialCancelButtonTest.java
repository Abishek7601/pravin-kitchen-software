package com.example.pravin_quotation;

import com.example.pravin_quotation.model.Division;
import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.model.WorkCategory;
import com.example.pravin_quotation.repository.DivisionRepository;
import com.example.pravin_quotation.repository.MaterialRepository;
import com.example.pravin_quotation.repository.WorkCategoryRepository;
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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class DivisionAndMaterialCancelButtonTest {

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
    private ResourceLoader resourceLoader;

    private WorkCategory category;
    private Division division;
    private Material material;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        long timestamp = System.currentTimeMillis();

        category = new WorkCategory();
        category.setName("TEST_CATEGORY_" + timestamp);
        category.setActive(true);
        category.setIcon("🏠");
        category.setDisplayOrder(1);
        category = workCategoryRepository.save(category);

        division = new Division();
        division.setName("TEST_DIVISION_" + timestamp);
        division.setWorkCategory(category);
        division.setActive(true);
        division.setDisplayOrder(1);
        division = divisionRepository.save(division);

        material = new Material();
        material.setName("TEST_MATERIAL_" + timestamp);
        material.getDivisions().add(division);
        material.setActive(true);
        material.setDisplayOrder(1);
        material = materialRepository.save(material);

        entityManager.flush();
    }

    @Test
    @DisplayName("Verify Work Category View template contains redirectUrl on Edit Division and Edit Material links")
    void testWorkCategoryViewTemplateLinks() throws Exception {
        Resource res = resourceLoader.getResource("classpath:templates/admin/work-category-view.html");
        assertTrue(res.exists(), "work-category-view.html must exist");

        try (InputStream is = res.getInputStream()) {
            String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // Verify Edit Division link passes redirectUrl
            assertThat(html).contains("@{/admin/divisions/edit/{id}(id=${dDto.division.id}, redirectUrl=@{/admin/work-categories/view/{catId}(catId=${category.id})})}");

            // Verify Edit Material link passes redirectUrl
            assertThat(html).contains("@{/admin/materials/edit/{id}(id=${mDto.material.id}, redirectUrl=@{/admin/work-categories/view/{catId}(catId=${category.id})})}");
        }
    }

    @Test
    @DisplayName("Division Edit Form accessed with redirectUrl renders Cancel button pointing to Work Category View")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testDivisionEditFormCancelUrlWhenAccessedFromWorkCategoryView() throws Exception {
        String expectedRedirectUrl = "/admin/work-categories/view/" + category.getId();

        mockMvc.perform(get("/admin/divisions/edit/" + division.getId())
                        .param("redirectUrl", expectedRedirectUrl))
                .andExpect(status().isOk())
                .andExpect(model().attribute("redirectUrl", expectedRedirectUrl))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"" + expectedRedirectUrl + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"cancel-btn\"")));
    }

    @Test
    @DisplayName("Division Edit Form accessed directly without redirectUrl falls back to /admin/divisions")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testDivisionEditFormCancelUrlDirectAccessFallback() throws Exception {
        mockMvc.perform(get("/admin/divisions/edit/" + division.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/divisions\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"cancel-btn\"")));
    }

    @Test
    @DisplayName("Material Edit Form accessed with redirectUrl renders Cancel button pointing to Work Category View")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testMaterialEditFormCancelUrlWhenAccessedFromWorkCategoryView() throws Exception {
        String expectedRedirectUrl = "/admin/work-categories/view/" + category.getId();

        mockMvc.perform(get("/admin/materials/edit/" + material.getId())
                        .param("redirectUrl", expectedRedirectUrl))
                .andExpect(status().isOk())
                .andExpect(model().attribute("redirectUrl", expectedRedirectUrl))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"" + expectedRedirectUrl + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"btn btn-secondary\"")));
    }

    @Test
    @DisplayName("Material Edit Form accessed directly without redirectUrl falls back to /admin/materials")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testMaterialEditFormCancelUrlDirectAccessFallback() throws Exception {
        mockMvc.perform(get("/admin/materials/edit/" + material.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/materials\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"btn btn-secondary\"")));
    }

    @Test
    @DisplayName("Division Edit Form respects Referer header when redirectUrl query param is absent")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testDivisionEditFormCancelUrlFromRefererHeader() throws Exception {
        String expectedRedirectUrl = "/admin/work-categories/view/" + category.getId();

        mockMvc.perform(get("/admin/divisions/edit/" + division.getId())
                        .header("Referer", "http://localhost:8080" + expectedRedirectUrl))
                .andExpect(status().isOk())
                .andExpect(model().attribute("redirectUrl", expectedRedirectUrl))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"" + expectedRedirectUrl + "\"")));
    }

    @Test
    @DisplayName("Material Edit Form respects Referer header when redirectUrl query param is absent")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testMaterialEditFormCancelUrlFromRefererHeader() throws Exception {
        String expectedRedirectUrl = "/admin/work-categories/view/" + category.getId();

        mockMvc.perform(get("/admin/materials/edit/" + material.getId())
                        .header("Referer", "http://localhost:8080" + expectedRedirectUrl))
                .andExpect(status().isOk())
                .andExpect(model().attribute("redirectUrl", expectedRedirectUrl))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"" + expectedRedirectUrl + "\"")));
    }

    @Test
    @DisplayName("Work Category View page renders Edit Division and Edit Material links with correct category redirectUrl")
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testWorkCategoryViewPageRendersEditLinksWithRedirectUrl() throws Exception {
        mockMvc.perform(get("/admin/work-categories/view/" + category.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/admin/divisions/edit/" + division.getId() + "?redirectUrl=/admin/work-categories/view/" + category.getId()
                )))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/admin/materials/edit/" + material.getId() + "?redirectUrl=/admin/work-categories/view/" + category.getId()
                )));
    }
}
