package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminController;
import com.example.pravin_quotation.model.Branch;
import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.service.AdminDashboardService;
import com.example.pravin_quotation.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdminDashboardTotalEmployeesTest {

    @Autowired
    private AdminDashboardService adminDashboardService;

    @Autowired
    private AdminController adminController;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    private Branch branch;

    @BeforeEach
    void setUp() {
        branch = new Branch();
        branch.setName("Test Branch " + System.nanoTime());
        branch.setActive(true);
        branch = branchRepository.save(branch);
    }

    @Test
    @DisplayName("Admin Dashboard: Total Employees counts only active EMPLOYEE users, excluding SUPER_ADMIN and inactive")
    void testTotalEmployeesCountsOnlyActiveEmployees() {
        long initialActiveCount = adminDashboardService.getTotalEmployees();

        // 1. Adding a SUPER_ADMIN must NOT increase the count
        User admin = new User();
        admin.setName("Super Admin Test");
        admin.setEmail("admin-" + System.nanoTime() + "@pravin.com");
        admin.setPassword("pass");
        admin.setRole(Role.SUPER_ADMIN);
        admin.setBranch(branch);
        admin.setActive(true);
        userRepository.save(admin);

        assertEquals(initialActiveCount, adminDashboardService.getTotalEmployees(),
                "Adding a SUPER_ADMIN must not increase Total Employees count");

        // 2. Adding an active EMPLOYEE must increase count by 1
        User emp1 = new User();
        emp1.setName("Employee One");
        emp1.setEmail("emp1-" + System.nanoTime() + "@pravin.com");
        emp1.setPassword("pass");
        emp1.setRole(Role.EMPLOYEE);
        emp1.setBranch(branch);
        emp1.setActive(true);
        emp1 = userRepository.save(emp1);

        assertEquals(initialActiveCount + 1, adminDashboardService.getTotalEmployees(),
                "Adding an active employee must increase Total Employees count by 1");

        // 3. Adding an inactive/deactivated EMPLOYEE must NOT increase count
        User inactiveEmp = new User();
        inactiveEmp.setName("Inactive Employee");
        inactiveEmp.setEmail("inactive-" + System.nanoTime() + "@pravin.com");
        inactiveEmp.setPassword("pass");
        inactiveEmp.setRole(Role.EMPLOYEE);
        inactiveEmp.setBranch(branch);
        inactiveEmp.setActive(false);
        userRepository.save(inactiveEmp);

        assertEquals(initialActiveCount + 1, adminDashboardService.getTotalEmployees(),
                "Adding an inactive employee must not increase Total Employees count");

        // 4. Deactivating an active employee decreases count by 1
        emp1.setActive(false);
        emp1 = userRepository.save(emp1);

        assertEquals(initialActiveCount, adminDashboardService.getTotalEmployees(),
                "Deactivating an employee must decrease Total Employees count by 1");

        // 5. Reactivating an employee increases count by 1
        emp1.setActive(true);
        emp1 = userRepository.save(emp1);

        assertEquals(initialActiveCount + 1, adminDashboardService.getTotalEmployees(),
                "Reactivating an employee must increase Total Employees count by 1");

        // 6. Deleting an employee decreases count by 1
        userRepository.delete(emp1);

        assertEquals(initialActiveCount, adminDashboardService.getTotalEmployees(),
                "Deleting an employee must decrease Total Employees count by 1");
    }

    @Test
    @DisplayName("EmployeeService: create, toggleStatus, and delete reflect dynamically in Admin Dashboard count")
    void testEmployeeServiceLifecycleUpdatesDashboardCount() {
        long baseCount = adminDashboardService.getTotalEmployees();

        // 1. Create employee via EmployeeService
        String email = "lifecycle-" + System.nanoTime() + "@pravin.com";
        User created = employeeService.createEmployee("Lifecycle Employee", email, "password123", branch.getId());
        assertNotNull(created.getId());
        assertEquals(baseCount + 1, adminDashboardService.getTotalEmployees(),
                "Creating employee via EmployeeService must increase dashboard count by 1");
        assertEquals(baseCount + 1, employeeService.getActiveEmployeeCount(),
                "EmployeeService.getActiveEmployeeCount must match dashboard count");

        // 2. Deactivate employee via toggleEmployeeStatus
        employeeService.toggleEmployeeStatus(created.getId());
        assertEquals(baseCount, adminDashboardService.getTotalEmployees(),
                "Deactivating employee via EmployeeService must decrease dashboard count by 1");

        // 3. Reactivate employee via toggleEmployeeStatus
        employeeService.toggleEmployeeStatus(created.getId());
        assertEquals(baseCount + 1, adminDashboardService.getTotalEmployees(),
                "Reactivating employee via EmployeeService must increase dashboard count by 1");

        // 4. Delete employee via EmployeeService
        employeeService.deleteEmployee(created.getId());
        assertEquals(baseCount, adminDashboardService.getTotalEmployees(),
                "Deleting employee via EmployeeService must decrease dashboard count by 1");
    }

    @Test
    @DisplayName("AdminController: dashboard model populates dynamic Total Employees and preserves other cards")
    void testAdminControllerDashboardPopulatesTotalEmployees() {
        ExtendedModelMap model = new ExtendedModelMap();
        String view = adminController.dashboard(model);

        assertEquals("admin/dashboard", view);
        assertNotNull(model.get("totalEmployees"));
        assertEquals(adminDashboardService.getTotalEmployees(), model.get("totalEmployees"));

        // Verify other cards remain populated and intact
        assertNotNull(model.get("totalBranches"));
        assertNotNull(model.get("oneYearQuotationCount"));
        assertNotNull(model.get("last60DaysQuotationCount"));
        assertNotNull(model.get("last60DaysQuotedValue"));
        assertNotNull(model.get("oneYearCustomerCount"));
        assertNotNull(model.get("pendingQuotationCount"));
        assertNotNull(model.get("workingQuotationCount"));
        assertNotNull(model.get("sentQuotationCount"));
        assertNotNull(model.get("totalPortfolioValue"));
    }

    @Test
    @DisplayName("Template: admin/dashboard.html preserves card structure, label and Active employees subtitle")
    void testAdminDashboardTemplateIntegrity() throws IOException {
        Path dashboardPath = Paths.get("src/main/resources/templates/admin/dashboard.html");
        assertTrue(Files.exists(dashboardPath));

        String html = Files.readString(dashboardPath);
        assertTrue(html.contains("Total Employees"), "Template must contain 'Total Employees' card title");
        assertTrue(html.contains("Active employees"), "Template must contain 'Active employees' subtitle");
        assertTrue(html.contains("th:text=\"${totalEmployees}\""), "Template must bind totalEmployees dynamically");
    }
}

