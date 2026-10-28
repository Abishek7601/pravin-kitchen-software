package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.PricingController;
import com.example.pravin_quotation.controller.WorkCategoryController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class FixedSidebarNavigationTest {

    @Autowired
    private WorkCategoryController workCategoryController;

    @Autowired
    private PricingController pricingController;

    @Autowired
    private ResourceLoader resourceLoader;

    private String loadResource(String path) throws Exception {
        Resource resource = resourceLoader.getResource("classpath:" + path);
        assertTrue(resource.exists(), "Resource must exist: " + path);
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Admin Sidebar contains Work Category, Configure Master Data, and preserves all existing items")
    public void testAdminSidebarMenuLinks() throws Exception {
        String adminNav = loadResource("templates/fragments/admin-navigation.html");

        // Existing menu items preserved
        assertTrue(adminNav.contains("@{/admin/dashboard}"), "Admin sidebar must contain Dashboard link");
        assertTrue(adminNav.contains("Dashboard"), "Admin sidebar must contain Dashboard label");

        assertTrue(adminNav.contains("@{/admin/employees}"), "Admin sidebar must contain Employees link");
        assertTrue(adminNav.contains("Employees"), "Admin sidebar must contain Employees label");

        assertTrue(adminNav.contains("@{/admin/branches}"), "Admin sidebar must contain Branches link");
        assertTrue(adminNav.contains("Branches / Districts"), "Admin sidebar must contain Branches label");

        assertTrue(adminNav.contains("@{/admin/quotations}"), "Admin sidebar must contain Quotations link");
        assertTrue(adminNav.contains("Quotations"), "Admin sidebar must contain Quotations label");

        assertTrue(adminNav.contains("@{/admin/customers}"), "Admin sidebar must contain Customers link");
        assertTrue(adminNav.contains("Customers"), "Admin sidebar must contain Customers label");

        assertFalse(adminNav.contains("@{/admin/pricing}"), "Admin sidebar must NOT contain Pricing link");

        // New menu items added
        assertTrue(adminNav.contains("@{/admin/work-categories}"), "Admin sidebar must contain Work Category link");
        assertTrue(adminNav.contains("Work Category"), "Admin sidebar must contain Work Category label");

        assertTrue(adminNav.contains("@{/admin/pricing/new}"), "Admin sidebar must contain Configure Master Data link");
        assertTrue(adminNav.contains("Configure Master Data"), "Admin sidebar must contain Configure Master Data label");
    }

    @Test
    @DisplayName("Role-based isolation: Employee sidebar does NOT contain Work Category or Configure Master Data")
    public void testEmployeeSidebarRoleIsolation() throws Exception {
        String empNav = loadResource("templates/fragments/employee-navigation.html");

        assertFalse(empNav.contains("work-categories"), "Employee sidebar must not have work-categories route");
        assertFalse(empNav.contains("Work Category"), "Employee sidebar must not have Work Category label");
        assertFalse(empNav.contains("Configure Master Data"), "Employee sidebar must not have Configure Master Data label");
        assertFalse(empNav.contains("/admin/"), "Employee sidebar must not contain any /admin/ routes");

        // Preserve employee menu items
        assertTrue(empNav.contains("@{/employee/dashboard}"), "Employee sidebar must have Dashboard");
        assertTrue(empNav.contains("@{/employee/quotations}"), "Employee sidebar must have Quotations");
        assertTrue(empNav.contains("@{/employee/customers}"), "Employee sidebar must have Customers");
    }

    @Test
    @DisplayName("Fixed Sidebar and Top Navbar CSS positioning in admin-nav.css")
    public void testAdminNavCssFixedSidebar() throws Exception {
        String css = loadResource("static/css/admin-nav.css");

        // Top navbar fixed
        assertTrue(css.contains(".top-navbar"), "CSS must define .top-navbar");
        assertTrue(css.contains("position: fixed;"), "Top navbar or sidebar must have position: fixed");
        assertTrue(css.contains("z-index: 1000;"), "Top navbar must have z-index: 1000");

        // Sidebar fixed below top navbar
        assertTrue(css.contains(".sidebar {"), "CSS must define .sidebar");
        assertTrue(css.contains("z-index: 900;"), "Sidebar must have z-index below navbar (900)");
        assertTrue(css.contains("top: var(--admin-navbar-height);"), "Sidebar top must be below navbar height");
        assertTrue(css.contains("overflow-y: auto;"), "Sidebar must have overflow-y: auto for internal scrolling");

        // Main content offset
        assertTrue(css.contains("margin-left: var(--admin-sidebar-width);"), "Main content must have margin-left to prevent overlap with fixed sidebar");

        // Responsive behavior
        assertTrue(css.contains("@media (max-width: 850px)"), "CSS must contain responsive breakpoint for mobile/tablet (<= 850px)");
        assertTrue(css.contains("position: static;"), "Sidebar and navbar must switch to static on responsive breakpoint");
    }

    @Test
    @DisplayName("Fixed Sidebar and Top Navbar CSS positioning in dashboard.css")
    public void testDashboardCssFixedSidebar() throws Exception {
        String css = loadResource("static/css/dashboard.css");

        // Top navbar fixed
        assertTrue(css.contains(".top-navbar {"), "dashboard.css must define .top-navbar");
        assertTrue(css.contains("position: fixed;"), "dashboard.css must have position: fixed for top navbar");
        assertTrue(css.contains("z-index: 1000;"), "dashboard.css top navbar must have z-index: 1000");

        // Sidebar fixed below top navbar
        assertTrue(css.contains(".sidebar {"), "dashboard.css must define .sidebar");
        assertTrue(css.contains("z-index: 900;"), "dashboard.css sidebar must have z-index 900");
        assertTrue(css.contains("top: 82px;"), "dashboard.css sidebar top must be 82px");
        assertTrue(css.contains("overflow-y: auto;"), "dashboard.css sidebar must have overflow-y: auto");

        // Main content offset
        assertTrue(css.contains("margin-left: 255px;"), "dashboard.css main-content must have margin-left");

        // Responsive behavior
        assertTrue(css.contains("@media (max-width: 850px)"), "dashboard.css must have 850px breakpoint");
        assertTrue(css.contains("position: static;"), "dashboard.css sidebar must switch to static on responsive breakpoint");
    }

    @Test
    @DisplayName("Work Categories route /admin/work-categories loads successfully")
    public void testWorkCategoriesRoute() {
        Model model = new ConcurrentModel();
        String view = workCategoryController.categories(model, false);
        assertEquals("admin/work-categories", view, "Route must return admin/work-categories view");
        assertTrue(model.containsAttribute("categories"), "Model must contain categories attribute");
        assertFalse((Boolean) model.getAttribute("isArchivedView"), "isArchivedView must be false");
    }

    @Test
    @DisplayName("Configure Master Data route /admin/pricing/new loads successfully")
    public void testConfigureMasterDataRoute() {
        Model model = new ConcurrentModel();
        MockHttpServletRequest request = new MockHttpServletRequest();
        String view = pricingController.newPricing(null, null, request, model);
        assertEquals("admin/pricing-form", view, "Route must return admin/pricing-form view");
        assertTrue(model.containsAttribute("pricing"), "Model must contain pricing attribute");
        assertEquals(false, model.getAttribute("isEdit"), "isEdit must be false for new master data configuration");
    }
}
