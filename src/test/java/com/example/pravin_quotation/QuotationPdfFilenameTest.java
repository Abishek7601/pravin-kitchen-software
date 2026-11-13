package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.CustomerRepository;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.util.QuotationPdfFilenameUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class QuotationPdfFilenameTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private User testAdmin;
    private User testEmployee;
    private Branch testBranch;
    private Customer testCustomer;
    private Quotation testParentQuotation;
    private Quotation testChildQuotation;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        testBranch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setName("Chennai Main");
            b.setActive(true);
            return branchRepository.save(b);
        });

        testAdmin = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN && Boolean.TRUE.equals(u.getActive()))
                .findFirst()
                .orElseGet(() -> {
                    User admin = new User();
                    admin.setName("Super Admin");
                    admin.setEmail("admin-pdf-test@pravinkitchens.com");
                    admin.setRole(Role.SUPER_ADMIN);
                    admin.setActive(true);
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    return userRepository.save(admin);
                });

        testEmployee = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE && Boolean.TRUE.equals(u.getActive()))
                .findFirst()
                .orElseGet(() -> {
                    User emp = new User();
                    emp.setName("Employee One");
                    emp.setEmail("employee-pdf-test@pravinkitchens.com");
                    emp.setRole(Role.EMPLOYEE);
                    emp.setActive(true);
                    emp.setPassword(passwordEncoder.encode("emp123"));
                    return userRepository.save(emp);
                });

        testCustomer = new Customer();
        testCustomer.setName("Abishek S R");
        testCustomer.setPhone("9876543210");
        testCustomer.setEmail("abishek@example.com");
        testCustomer.setBranch(testBranch);
        testCustomer = customerRepository.save(testCustomer);

        String uniqueNum = "QT-TEST-" + System.currentTimeMillis();
        testParentQuotation = new Quotation();
        testParentQuotation.setQuotationNumber(uniqueNum);
        testParentQuotation.setPricingMode(PricingMode.PREMIUM);
        testParentQuotation.setQuotationDate(LocalDate.now());
        testParentQuotation.setCustomer(testCustomer);
        testParentQuotation.setEmployee(testEmployee);
        testParentQuotation.setBranch(testBranch);
        testParentQuotation.setStatus(QuotationStatus.WORKING);
        testParentQuotation.setSubtotal(BigDecimal.valueOf(50000));
        testParentQuotation.setGrandTotal(BigDecimal.valueOf(59000));
        testParentQuotation = quotationRepository.save(testParentQuotation);

        testChildQuotation = new Quotation();
        testChildQuotation.setQuotationNumber(uniqueNum + "-ECONOMY");
        testChildQuotation.setQuotationDate(LocalDate.now());
        testChildQuotation.setCustomer(testCustomer);
        testChildQuotation.setEmployee(testEmployee);
        testChildQuotation.setBranch(testBranch);
        testChildQuotation.setParent(testParentQuotation);
        testChildQuotation.setPricingMode(PricingMode.ECONOMY);
        testChildQuotation.setStatus(QuotationStatus.WORKING);
        testChildQuotation.setSubtotal(BigDecimal.valueOf(30000));
        testChildQuotation.setGrandTotal(BigDecimal.valueOf(35400));
        testChildQuotation = quotationRepository.save(testChildQuotation);

        testParentQuotation.getChildQuotations().add(testChildQuotation);
        quotationRepository.save(testParentQuotation);
    }

    @Test
    @DisplayName("Unit Test: QuotationPdfFilenameUtils sanitizes customer names correctly")
    void testQuotationPdfFilenameUtilsSanitization() {
        // 1. Example from prompt: Abishek S R -> Abishek_S_R.pdf
        assertEquals("Abishek_S_R.pdf", QuotationPdfFilenameUtils.sanitizeCustomerFilename("Abishek S R"));

        // 2. Dots and initials
        assertEquals("Abishek_S_R.pdf", QuotationPdfFilenameUtils.sanitizeCustomerFilename("Abishek S. R."));
        assertEquals("Abishek_S_R.pdf", QuotationPdfFilenameUtils.sanitizeCustomerFilename("Abishek S.R."));

        // 3. Hyphenated names
        assertEquals("Mary-Jane_Watson.pdf", QuotationPdfFilenameUtils.sanitizeCustomerFilename("Mary-Jane Watson"));

        // 4. Tamil / Unicode characters
        assertEquals("அபிஷேக்_எஸ்_ஆர்.pdf", QuotationPdfFilenameUtils.sanitizeCustomerFilename("அபிஷேக் எஸ் ஆர்"));
        assertEquals("கார்த்திக்_ராஜா.pdf", QuotationPdfFilenameUtils.sanitizeCustomerFilename("கார்த்திக் ராஜா"));

        // 5. Special characters and invalid filename chars
        assertEquals("Abishek_Kitchen_Quote.pdf", QuotationPdfFilenameUtils.sanitizeCustomerFilename("Abishek / Kitchen : Quote *?"));
        assertEquals("R_K_Interiors.pdf", QuotationPdfFilenameUtils.sanitizeCustomerFilename("R & K Interiors"));

        // 6. Null and blank fallbacks
        assertEquals("Customer_Quotation.pdf", QuotationPdfFilenameUtils.sanitizeCustomerFilename(null));
        assertEquals("Customer_Quotation.pdf", QuotationPdfFilenameUtils.sanitizeCustomerFilename("   "));
        assertEquals("Customer_Quotation.pdf", QuotationPdfFilenameUtils.sanitizeCustomerFilename("...///:::"));
    }

    @Test
    @DisplayName("Unit Test: buildPdfFilename uses customer name only, ignoring quotation numbers and grade names")
    void testBuildPdfFilenameFromQuotation() {
        // Parent quotation
        assertEquals("Abishek_S_R.pdf", QuotationPdfFilenameUtils.buildPdfFilename(testParentQuotation));

        // Child quotation with grade ECONOMY
        assertEquals("Abishek_S_R.pdf", QuotationPdfFilenameUtils.buildPdfFilename(testChildQuotation));

        // Quotation without customer
        Quotation noCust = new Quotation();
        noCust.setQuotationNumber("QT-99999");
        noCust.setPricingMode(PricingMode.PREMIUM);
        assertEquals("Customer_Quotation.pdf", QuotationPdfFilenameUtils.buildPdfFilename(noCust));
    }

    @Test
    @DisplayName("Integration Test: Admin PDF download contains only customer name in filename")
    void testAdminPdfDownloadFilename() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_VERIFIED", true);

        // Download combined/parent quotation
        MvcResult resultParent = mockMvc.perform(get("/admin/quotations/pdf/" + testParentQuotation.getId())
                        .session(session)
                        .with(user(testAdmin.getEmail()).roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn();

        String dispositionParent = resultParent.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(dispositionParent);
        assertTrue(dispositionParent.contains("filename=\"Abishek_S_R.pdf\""),
                "Admin download filename must be Abishek_S_R.pdf: " + dispositionParent);
        assertFalse(dispositionParent.contains("QT-00078"), "Must NOT contain quotation number");
        assertFalse(dispositionParent.contains("ECONOMY"), "Must NOT contain grade name");

        // Download with grade param (?grade=ECONOMY)
        MvcResult resultGrade = mockMvc.perform(get("/admin/quotations/pdf/" + testParentQuotation.getId())
                        .param("grade", "ECONOMY")
                        .session(session)
                        .with(user(testAdmin.getEmail()).roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn();

        String dispositionGrade = resultGrade.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(dispositionGrade);
        assertTrue(dispositionGrade.contains("filename=\"Abishek_S_R.pdf\""),
                "Admin grade download must still use customer name only: " + dispositionGrade);
        assertFalse(dispositionGrade.contains("QT-00078"), "Must NOT contain quotation number");
        assertFalse(dispositionGrade.contains("ECONOMY"), "Must NOT contain grade name");
    }

    @Test
    @DisplayName("Integration Test: Employee PDF download contains only customer name in filename")
    void testEmployeePdfDownloadFilename() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_VERIFIED", true);

        // Download combined/parent quotation
        MvcResult resultParent = mockMvc.perform(get("/employee/quotations/pdf/" + testParentQuotation.getId())
                        .session(session)
                        .with(user(testEmployee.getEmail()).roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn();

        String dispositionParent = resultParent.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(dispositionParent);
        assertTrue(dispositionParent.contains("filename=\"Abishek_S_R.pdf\""),
                "Employee download filename must be Abishek_S_R.pdf: " + dispositionParent);
        assertFalse(dispositionParent.contains("QT-00078"), "Must NOT contain quotation number");
        assertFalse(dispositionParent.contains("ECONOMY"), "Must NOT contain grade name");

        // Download with grade param (?grade=ECONOMY)
        MvcResult resultGrade = mockMvc.perform(get("/employee/quotations/pdf/" + testParentQuotation.getId())
                        .param("grade", "ECONOMY")
                        .session(session)
                        .with(user(testEmployee.getEmail()).roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn();

        String dispositionGrade = resultGrade.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(dispositionGrade);
        assertTrue(dispositionGrade.contains("filename=\"Abishek_S_R.pdf\""),
                "Employee grade download must still use customer name only: " + dispositionGrade);
        assertFalse(dispositionGrade.contains("QT-00078"), "Must NOT contain quotation number");
        assertFalse(dispositionGrade.contains("ECONOMY"), "Must NOT contain grade name");
    }

    @Test
    @DisplayName("Integration Test: Tamil and non-English customer names generate safe Content-Disposition headers")
    void testTamilCustomerNamePdfDownloadFilename() throws Exception {
        Customer tamilCustomer = new Customer();
        tamilCustomer.setName("அபிஷேக் எஸ் ஆர்");
        tamilCustomer.setPhone("9876543211");
        tamilCustomer = customerRepository.save(tamilCustomer);

        Quotation tamilQuotation = new Quotation();
        tamilQuotation.setQuotationNumber("QT-TEST-TAMIL-" + System.currentTimeMillis());
        tamilQuotation.setPricingMode(PricingMode.PREMIUM);
        tamilQuotation.setQuotationDate(LocalDate.now());
        tamilQuotation.setCustomer(tamilCustomer);
        tamilQuotation.setEmployee(testEmployee);
        tamilQuotation.setBranch(testBranch);
        tamilQuotation.setStatus(QuotationStatus.WORKING);
        tamilQuotation.setSubtotal(BigDecimal.valueOf(40000));
        tamilQuotation.setGrandTotal(BigDecimal.valueOf(47200));
        tamilQuotation = quotationRepository.save(tamilQuotation);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_VERIFIED", true);

        MvcResult result = mockMvc.perform(get("/admin/quotations/pdf/" + tamilQuotation.getId())
                        .session(session)
                        .with(user(testAdmin.getEmail()).roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn();

        String disposition = result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        // Spring ContentDisposition encodes non-ASCII using filename*
        assertTrue(disposition.contains("filename*=") || disposition.contains("அபிஷேக்_எஸ்_ஆர்.pdf"),
                "Must support UTF-8 filename encoding: " + disposition);
    }
}

