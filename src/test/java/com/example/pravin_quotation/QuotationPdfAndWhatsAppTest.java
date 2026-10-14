package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.QuotationPdfService;
import com.example.pravin_quotation.service.WhatsAppService;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuotationPdfAndWhatsAppTest {

    @Autowired
    private QuotationPdfService quotationPdfService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private QuotationRoomRepository quotationRoomRepository;

    @Autowired
    private QuotationItemRepository quotationItemRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private QuotationCommunicationRepository communicationRepository;

    private Branch branchA;
    private Branch branchB;
    private Customer customer;
    private User employee;
    private Division testDivision;
    private Quotation quotation;

    @BeforeEach
    public void setup() {
        branchA = new Branch();
        branchA.setName("Branch Coimbatore " + System.currentTimeMillis());
        branchA.setBranchAddress("100 Cross Cut Road, Gandhipuram, Coimbatore 641012");
        branchA.setWhatsapp("9876543210");
        branchA.setEmail("cbe@pravinkitchens.com");
        branchA = branchRepository.save(branchA);

        branchB = new Branch();
        branchB.setName("Branch Madurai " + System.currentTimeMillis());
        branchB.setBranchAddress("45 Mattuthavani Main Road, Madurai 625007");
        branchB.setWhatsapp("9123456780");
        branchB.setEmail("mdu@pravinkitchens.com");
        branchB = branchRepository.save(branchB);

        customer = new Customer();
        customer.setName("Suresh Kumar");
        customer.setPhone("9840123456");
        customer.setEmail("suresh@example.com");
        customer.setAddress("Flat 4B, Emerald Heights, Avinashi Road");
        customer = customerRepository.save(customer);

        employee = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User u = new User();
            u.setName("Test Employee");
            u.setEmail("testemp" + System.currentTimeMillis() + "@example.com");
            u.setPassword("password");
            u.setRole(Role.EMPLOYEE);
            return userRepository.save(u);
        });

        WorkCategory category = workCategoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            WorkCategory wc = new WorkCategory();
            wc.setName("Kitchen");
            return workCategoryRepository.save(wc);
        });

        testDivision = new Division();
        testDivision.setName("Modular Kitchen Division");
        testDivision.setWorkCategory(category);
        testDivision = divisionRepository.save(testDivision);

        quotation = new Quotation();
        quotation.setQuotationNumber("PK-TEST-" + System.currentTimeMillis());
        quotation.setCustomer(customer);
        quotation.setBranch(branchA);
        quotation.setEmployee(employee);
        quotation.setDivision(testDivision);
        quotation.setPricingMode(PricingMode.MIDDLE);
        quotation.setQuotationDate(LocalDate.now());
        quotation.setStatus(QuotationStatus.PENDING);
        quotation.setSubtotal(new BigDecimal("50000.00"));
        quotation.setGrandTotal(new BigDecimal("59000.00"));
        quotation = quotationRepository.save(quotation);
    }

    private String extractRawPdfText(byte[] pdfBytes) throws Exception {
        PdfReader reader = new PdfReader(pdfBytes);
        StringBuilder sb = new StringBuilder();
        int totalPages = reader.getNumberOfPages();
        for (int i = 1; i <= totalPages; i++) {
            byte[] pageContent = reader.getPageContent(i);
            if (pageContent != null) {
                sb.append(new String(pageContent, java.nio.charset.StandardCharsets.ISO_8859_1));
            }
        }
        reader.close();
        return sb.toString();
    }

    @Test
    public void testRemoveDivisionFromPdf() throws Exception {
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(quotation.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        String pdfText = extractRawPdfText(pdfBytes);

        // Division should NOT appear in the PDF
        assertFalse(pdfText.contains("Division:"), "PDF should not contain 'Division:' label");
        assertFalse(pdfText.contains("Modular Kitchen Division"), "PDF should not contain the division name");
        assertFalse(pdfText.contains("MODULAR KITCHEN DIVISION QUOTATION"), "PDF title should not contain division name");

        // Title should be QUOTATION
        assertTrue(pdfText.contains("QUOTATION"), "PDF title should be QUOTATION");

        // Database quotation must still retain Division
        Quotation reloaded = quotationRepository.findById(quotation.getId()).orElseThrow();
        assertNotNull(reloaded.getDivision());
        assertEquals("Modular Kitchen Division", reloaded.getDivision().getName());
    }

    @Test
    public void testDisplayCorrectBranchAddressInPdf() throws Exception {
        // 1. Test Branch A
        byte[] pdfBytesA = quotationPdfService.generateQuotationPdf(quotation.getId());
        String pdfTextA = extractRawPdfText(pdfBytesA);

        // Branch A address should appear
        assertTrue(pdfTextA.contains("100 Cross Cut Road"), "Branch A address must appear in PDF header");
        assertTrue(pdfTextA.contains("Gandhipuram, Coimbatore 641012"), "Branch A address city/pincode must appear in PDF header");

        // Customer address should be separate in Customer Details
        assertTrue(pdfTextA.contains("Flat 4B, Emerald Heights, Avinashi Road"), "Customer address must be present in customer details");

        // 2. Change quotation to Branch B
        quotation.setBranch(branchB);
        quotation = quotationRepository.save(quotation);

        byte[] pdfBytesB = quotationPdfService.generateQuotationPdf(quotation.getId());
        String pdfTextB = extractRawPdfText(pdfBytesB);

        // Branch B address must appear
        assertTrue(pdfTextB.contains("45 Mattuthavani Main Road"), "Branch B address must appear when Branch B is selected");
        assertTrue(pdfTextB.contains("Madurai 625007"), "Branch B pincode must appear");

        // Branch A address should NOT appear
        assertFalse(pdfTextB.contains("100 Cross Cut Road"), "Branch A address should not appear when Branch B is selected");

        // Hardcoded Azhagiamandapam address should NOT appear
        assertFalse(pdfTextB.contains("Azhagiamandapam"), "Default hardcoded address should not be used when branch has configured address");
    }

    @Test
    public void testCustomerAddressNotReplacedByBranchAddressWhenCustomerAddressEmpty() throws Exception {
        // Set customer address to null/blank
        Customer noAddrCustomer = new Customer();
        noAddrCustomer.setName("No Address Customer");
        noAddrCustomer.setPhone("9988776655");
        noAddrCustomer.setEmail("noaddr@example.com");
        noAddrCustomer.setAddress("");
        noAddrCustomer = customerRepository.save(noAddrCustomer);

        quotation.setCustomer(noAddrCustomer);
        quotation = quotationRepository.save(quotation);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(quotation.getId());
        String pdfText = extractRawPdfText(pdfBytes);

        // Branch address must still appear in company header
        assertTrue(pdfText.contains("100 Cross Cut Road"), "Branch address must appear in header");

        // Customer details address should be "—" and NOT replaced with branch name or address
        assertFalse(pdfText.contains(noAddrCustomer.getName() + " Address: " + branchA.getName()),
                "Customer address should not be replaced with branch name");
    }

    @Test
    public void testWhatsAppPdfGenerationAndSharing() {
        WhatsAppResult result = whatsAppService.sendOrPrepareQuotationPdf(quotation, "http://localhost:8080");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("919840123456", result.getPhone());
        assertTrue(result.getPdfSizeBytes() > 0, "PDF should have been generated with bytes > 0");
        assertNotNull(result.getWhatsappUrl());
        assertTrue(result.getWhatsappUrl().startsWith("https://wa.me/919840123456"));

        // Status should be SENT
        Quotation reloaded = quotationRepository.findById(quotation.getId()).orElseThrow();
        assertEquals(QuotationStatus.SENT, reloaded.getStatus());

        // Communication history should be recorded
        List<QuotationCommunication> comms = communicationRepository.findByQuotationIdOrderBySentAtDesc(quotation.getId());
        assertFalse(comms.isEmpty());
        QuotationCommunication comm = comms.get(0);
        assertEquals(QuotationCommunication.CommunicationType.WHATSAPP, comm.getType());
        assertEquals(QuotationCommunication.CommunicationStatus.SENT, comm.getStatus());
        assertTrue(comm.getMessage().contains("Quotation PDF generated"));
    }

    @Test
    public void testWhatsAppValidationMissingPhone() {
        Customer noPhoneCust = new Customer();
        noPhoneCust.setName("No Phone Customer");
        noPhoneCust.setPhone("");
        noPhoneCust = customerRepository.save(noPhoneCust);

        quotation.setCustomer(noPhoneCust);
        quotation = quotationRepository.save(quotation);

        assertThrows(IllegalArgumentException.class, () -> {
            whatsAppService.sendOrPrepareQuotationPdf(quotation, "http://localhost:8080");
        });
    }

    @Autowired
    private com.example.pravin_quotation.controller.PublicQuotationPdfController publicQuotationPdfController;

    @Autowired
    private com.example.pravin_quotation.controller.QuotationController quotationController;

    @Autowired
    private com.example.pravin_quotation.controller.EmployeeQuotationController employeeQuotationController;

    @Test
    public void testPublicQuotationPdfEndpoint() {
        org.springframework.http.ResponseEntity<byte[]> response = publicQuotationPdfController.getPublicQuotationPdf(quotation.getId());
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(org.springframework.http.MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
        assertTrue(response.getHeaders().getFirst(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION).contains("inline"));
    }

    @Test
    public void testAdminControllerWhatsAppSharingJson() {
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("Accept", "application/json");

        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        Object resultObj = quotationController.trackWhatsApp(quotation.getId(), request, redirectAttributes);
        assertTrue(resultObj instanceof org.springframework.http.ResponseEntity<?>);

        org.springframework.http.ResponseEntity<?> response = (org.springframework.http.ResponseEntity<?>) resultObj;
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof WhatsAppResult);

        WhatsAppResult wr = (WhatsAppResult) response.getBody();
        assertTrue(wr.isSuccess());
        assertTrue(wr.getPdfSizeBytes() > 0);
    }

    @Test
    public void testAdminControllerWhatsAppSharingRedirect() {
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        Object resultObj = quotationController.trackWhatsApp(quotation.getId(), request, redirectAttributes);
        assertTrue(resultObj instanceof String);
        String redirectUrl = (String) resultObj;
        assertTrue(redirectUrl.startsWith("redirect:https://wa.me/919840123456"));
    }

    @Test
    public void testEmployeeControllerWhatsAppSharing() {
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        org.springframework.security.core.Authentication auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        employee.getEmail(), employee.getPassword(), java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_EMPLOYEE"))
                );

        Object resultObj = employeeQuotationController.trackWhatsApp(quotation.getId(), auth, request, redirectAttributes);
        assertTrue(resultObj instanceof String);
        String redirectUrl = (String) resultObj;
        assertTrue(redirectUrl.startsWith("redirect:https://wa.me/919840123456"));
    }
}
