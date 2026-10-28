package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.Branch;
import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.model.WorkCategory;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CategoryButtonSingleEmojiTest {

    @Autowired
    private ITemplateEngine templateEngine;

    @Autowired
    private AdminQuotationBuilderController adminQuotationBuilderController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Test
    @DisplayName("1. WorkCategory cleanName and effectiveIcon produce exactly one emoji for all test categories")
    void testWorkCategorySingleEmojiResolution() {
        // Test cases from problem description:
        // 💡 FALSE CEILING, 🏠 FOYER, 🔍 MODULAR KITCHEN, 🧱 WALL PANEL, 🪙 WALLPAPER

        WorkCategory falseCeiling = new WorkCategory();
        falseCeiling.setIcon("💡");
        falseCeiling.setName("💡 FALSE CEILING");
        assertEquals("💡", falseCeiling.getEffectiveIcon());
        assertEquals("FALSE CEILING", falseCeiling.getCleanName());
        assertEquals("💡 FALSE CEILING", falseCeiling.getDisplayName());

        WorkCategory foyer = new WorkCategory();
        foyer.setIcon("🏠");
        foyer.setName("🏠 FOYER");
        assertEquals("🏠", foyer.getEffectiveIcon());
        assertEquals("FOYER", foyer.getCleanName());
        assertEquals("🏠 FOYER", foyer.getDisplayName());

        WorkCategory kitchen = new WorkCategory();
        kitchen.setIcon("🔍");
        kitchen.setName("🔍 MODULAR KITCHEN");
        assertEquals("🔍", kitchen.getEffectiveIcon());
        assertEquals("MODULAR KITCHEN", kitchen.getCleanName());
        assertEquals("🔍 MODULAR KITCHEN", kitchen.getDisplayName());

        WorkCategory wallPanel = new WorkCategory();
        wallPanel.setIcon("🧱");
        wallPanel.setName("🧱 WALL PANEL");
        assertEquals("🧱", wallPanel.getEffectiveIcon());
        assertEquals("WALL PANEL", wallPanel.getCleanName());
        assertEquals("🧱 WALL PANEL", wallPanel.getDisplayName());

        WorkCategory wallpaper = new WorkCategory();
        wallpaper.setIcon("🪙");
        wallpaper.setName("🪙 WALLPAPER");
        assertEquals("🪙", wallpaper.getEffectiveIcon());
        assertEquals("WALLPAPER", wallpaper.getCleanName());
        assertEquals("🪙 WALLPAPER", wallpaper.getDisplayName());
    }

    @Test
    @DisplayName("2. Multi-repeated emojis in category name are fully stripped for cleanName")
    void testMultipleRepeatedEmojisStripped() {
        WorkCategory cat = new WorkCategory();
        cat.setIcon("🪙");
        cat.setName("🪙 🪙 🪙 WALLPAPER");
        assertEquals("🪙", cat.getEffectiveIcon());
        assertEquals("WALLPAPER", cat.getCleanName());
        assertEquals("🪙 WALLPAPER", cat.getDisplayName());
    }

    @Test
    @DisplayName("3. Clean name without emoji preserves icon and clean name without duplication")
    void testCleanCategoryNameWithoutEmoji() {
        WorkCategory cat = new WorkCategory();
        cat.setIcon("🏠");
        cat.setName("FOYER");
        assertEquals("🏠", cat.getEffectiveIcon());
        assertEquals("FOYER", cat.getCleanName());
        assertEquals("🏠 FOYER", cat.getDisplayName());
    }

    @Test
    @DisplayName("4. Category with null icon extracts leading emoji from name as effectiveIcon")
    void testNullIconExtractsFromLeadingEmoji() {
        WorkCategory cat = new WorkCategory();
        cat.setIcon(null);
        cat.setName("🧱 WALL PANEL");
        assertEquals("🧱", cat.getEffectiveIcon());
        assertEquals("WALL PANEL", cat.getCleanName());
        assertEquals("🧱 WALL PANEL", cat.getDisplayName());
    }

    @Test
    @DisplayName("5. Admin quotation builder renders category buttons with exactly one emoji per category")
    void testAdminQuotationBuilderCategoryButtons() throws Exception {
        String templatePath = "src/main/resources/templates/admin/quotation-builder.html";
        String content = Files.readString(Paths.get(templatePath));

        assertTrue(content.contains("class=\"category-choice-btn category-choice\""),
                "Admin quotation builder must contain category-choice-btn");
        assertTrue(content.contains("cat-btn-icon"),
                "Admin quotation builder must contain cat-btn-icon span");
        assertTrue(content.contains("cat-btn-label"),
                "Admin quotation builder must contain cat-btn-label span");
        assertTrue(content.contains("category.effectiveIcon"),
                "cat-btn-icon must use category.effectiveIcon");
        assertTrue(content.contains("category.cleanName"),
                "cat-btn-label must use category.cleanName");
        assertFalse(content.contains("cat-btn-label\" th:text=\"${category.displayName"),
                "cat-btn-label must not use category.displayName which causes emoji duplication");

        List<WorkCategory> testCategories = createTestCategories();

        ExtendedModelMap model = new ExtendedModelMap();
        adminQuotationBuilderController.builder(null, null, model);
        model.put("workCategories", testCategories);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var servletApp = JakartaServletWebApplication.buildApplication(request.getServletContext());
        var webExchange = servletApp.buildExchange(request, response);
        WebContext context = new WebContext(webExchange, request.getLocale(), model.asMap());

        String renderedHtml = templateEngine.process("admin/quotation-builder", context);
        assertNotNull(renderedHtml);

        // Verify category buttons render exactly 1 emoji
        assertSingleEmojiInButton(renderedHtml, "💡", "FALSE CEILING");
        assertSingleEmojiInButton(renderedHtml, "🏠", "FOYER");
        assertSingleEmojiInButton(renderedHtml, "🔍", "MODULAR KITCHEN");
        assertSingleEmojiInButton(renderedHtml, "🧱", "WALL PANEL");
        assertSingleEmojiInButton(renderedHtml, "🪙", "WALLPAPER");
    }

    @Test
    @DisplayName("6. Employee quotation builder renders category buttons with exactly one emoji per category")
    void testEmployeeQuotationBuilderCategoryButtons() throws Exception {
        String templatePath = "src/main/resources/templates/employee/quotation-builder.html";
        String content = Files.readString(Paths.get(templatePath));

        assertTrue(content.contains("class=\"category-choice-btn category-choice\""),
                "Employee quotation builder must contain category-choice-btn");
        assertTrue(content.contains("cat-btn-icon"),
                "Employee quotation builder must contain cat-btn-icon span");
        assertTrue(content.contains("cat-btn-label"),
                "Employee quotation builder must contain cat-btn-label span");
        assertTrue(content.contains("category.effectiveIcon"),
                "cat-btn-icon must use category.effectiveIcon");
        assertTrue(content.contains("category.cleanName"),
                "cat-btn-label must use category.cleanName");
        assertFalse(content.contains("cat-btn-label\" th:text=\"${category.displayName"),
                "cat-btn-label must not use category.displayName which causes emoji duplication");

        long ts = System.currentTimeMillis();
        Branch branch = new Branch();
        branch.setName("TestBranch-" + ts);
        branch.setBranchAddress("123 Test St");
        branch.setActive(true);
        branch = branchRepository.save(branch);

        User employee = new User();
        employee.setName("Test Emp " + ts);
        employee.setEmail("emp_" + ts + "@test.com");
        employee.setPassword("secret");
        employee.setRole(Role.EMPLOYEE);
        employee.setBranch(branch);
        employee.setActive(true);
        employee = userRepository.save(employee);

        List<WorkCategory> testCategories = createTestCategories();

        ExtendedModelMap model = new ExtendedModelMap();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                employee.getEmail(), "secret", Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
        employeeQuotationController.builder(null, null, auth, model);
        model.put("workCategories", testCategories);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var servletApp = JakartaServletWebApplication.buildApplication(request.getServletContext());
        var webExchange = servletApp.buildExchange(request, response);
        WebContext context = new WebContext(webExchange, request.getLocale(), model.asMap());

        String renderedHtml = templateEngine.process("employee/quotation-builder", context);
        assertNotNull(renderedHtml);

        assertSingleEmojiInButton(renderedHtml, "💡", "FALSE CEILING");
        assertSingleEmojiInButton(renderedHtml, "🏠", "FOYER");
        assertSingleEmojiInButton(renderedHtml, "🔍", "MODULAR KITCHEN");
        assertSingleEmojiInButton(renderedHtml, "🧱", "WALL PANEL");
        assertSingleEmojiInButton(renderedHtml, "🪙", "WALLPAPER");
    }

    private List<WorkCategory> createTestCategories() {
        WorkCategory c1 = new WorkCategory();
        c1.setId(101L);
        c1.setIcon("💡");
        c1.setName("💡 FALSE CEILING");

        WorkCategory c2 = new WorkCategory();
        c2.setId(102L);
        c2.setIcon("🏠");
        c2.setName("🏠 FOYER");

        WorkCategory c3 = new WorkCategory();
        c3.setId(103L);
        c3.setIcon("🔍");
        c3.setName("🔍 MODULAR KITCHEN");

        WorkCategory c4 = new WorkCategory();
        c4.setId(104L);
        c4.setIcon("🧱");
        c4.setName("🧱 WALL PANEL");

        WorkCategory c5 = new WorkCategory();
        c5.setId(105L);
        c5.setIcon("🪙");
        c5.setName("🪙 WALLPAPER");

        return Arrays.asList(c1, c2, c3, c4, c5);
    }

    private void assertSingleEmojiInButton(String html, String expectedEmoji, String expectedLabel) {
        Pattern btnPattern = Pattern.compile("<button[^>]*class=\"[^\"]*category-choice[^\"]*\"[^>]*>(.*?)</button>", Pattern.DOTALL);
        Matcher matcher = btnPattern.matcher(html);
        boolean found = false;
        while (matcher.find()) {
            String btnContent = matcher.group(1);
            if (btnContent.contains(expectedLabel)) {
                found = true;
                int count = 0;
                int idx = 0;
                while ((idx = btnContent.indexOf(expectedEmoji, idx)) != -1) {
                    count++;
                    idx += expectedEmoji.length();
                }
                assertEquals(1, count, "Expected exactly 1 occurrence of emoji " + expectedEmoji + " in button for " + expectedLabel + ", but found: " + count + " in:\n" + btnContent);

                Pattern labelPattern = Pattern.compile("<span[^>]*class=\"cat-btn-label\"[^>]*>(.*?)</span>", Pattern.DOTALL);
                Matcher labelMatcher = labelPattern.matcher(btnContent);
                assertTrue(labelMatcher.find(), "Must have cat-btn-label span");
                assertFalse(labelMatcher.group(1).contains(expectedEmoji), "cat-btn-label must not contain emoji");
                assertEquals(expectedLabel, labelMatcher.group(1).trim(), "cat-btn-label must contain exact label name");
            }
        }
        assertTrue(found, "Button for category " + expectedLabel + " must be found in rendered HTML");
    }
}
