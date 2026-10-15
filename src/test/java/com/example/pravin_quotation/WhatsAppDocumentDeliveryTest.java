package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.controller.PublicQuotationPdfController;
import com.example.pravin_quotation.controller.QuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.QuotationPdfService;
import com.example.pravin_quotation.service.TemporaryMediaTokenService;
import com.example.pravin_quotation.service.WhatsAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
public class WhatsAppDocumentDeliveryTest {

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private QuotationPdfService quotationPdfService;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private QuotationCommunicationRepository communicationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private TemporaryMediaTokenService temporaryMediaTokenService;

    @Autowired
    private PublicQuotationPdfController publicQuotationPdfController;

    @Autowired
    private QuotationController quotationController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    private Branch branch;
    private Customer customer1;
    private Customer customer2;
    private User employee1;
    private User employee2;
    private Quotation quotation1;
    private Quotation quotation2;

    @BeforeEach
    public void setup() {
        branch = new Branch();
        branch.setName("Chennai Central Branch " + System.currentTimeMillis());
        branch.setBranchAddress("12 Mount Road, Anna Salai, Chennai 600002");
        branch.setWhatsapp("9876543210");
        branch.setEmail("chennai@pravinkitchens.com");
        branch = branchRepository.save(branch);

        customer1 = new Customer();
        customer1.setName("Ramesh Kumar");
        customer1.setPhone("9840112233");
        customer1.setEmail("ramesh@example.com");
        customer1.setAddress("Apt 12, Ocean View, Chennai");
        customer1 = customerRepository.save(customer1);

        customer2 = new Customer();
        customer2.setName("Priya Sharma");
        customer2.setPhone("+91 99441 55667");
        customer2.setEmail("priya@example.com");
        customer2.setAddress("44 Lake View, Madurai");
        customer2 = customerRepository.save(customer2);

        employee1 = new User();
        employee1.setName("Emp One");
        employee1.setEmail("emp1_" + System.currentTimeMillis() + "@pravinkitchens.com");
        employee1.setPassword("secret");
        employee1.setRole(Role.EMPLOYEE);
        employee1 = userRepository.save(employee1);

        employee2 = new User();
        employee2.setName("Emp Two");
        employee2.setEmail("emp2_" + System.currentTimeMillis() + "@pravinkitchens.com");
        employee2.setPassword("secret");
        employee2.setRole(Role.EMPLOYEE);
        employee2 = userRepository.save(employee2);

        WorkCategory wc = workCategoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            WorkCategory c = new WorkCategory();
            c.setName("Interiors");
            return workCategoryRepository.save(c);
        });

        Division div = new Division();
        div.setName("Modular Kitchens");
        div.setWorkCategory(wc);
        div = divisionRepository.save(div);

        quotation1 = new Quotation();
        quotation1.setQuotationNumber("PK-DOC-1-" + System.currentTimeMillis());
        quotation1.setCustomer(customer1);
        quotation1.setBranch(branch);
        quotation1.setEmployee(employee1);
        quotation1.setDivision(div);
        quotation1.setPricingMode(PricingMode.MIDDLE);
        quotation1.setQuotationDate(LocalDate.now());
        quotation1.setStatus(QuotationStatus.PENDING);
        quotation1.setSubtotal(new BigDecimal("75000.00"));
        quotation1.setGrandTotal(new BigDecimal("88500.00"));
        quotation1 = quotationRepository.save(quotation1);

        quotation2 = new Quotation();
        quotation2.setQuotationNumber("PK-DOC-2-" + System.currentTimeMillis());
        quotation2.setCustomer(customer2);
        quotation2.setBranch(branch);
        quotation2.setEmployee(employee2);
        quotation2.setDivision(div);
        quotation2.setPricingMode(PricingMode.PREMIUM);
        quotation2.setQuotationDate(LocalDate.now());
        quotation2.setStatus(QuotationStatus.PENDING);
        quotation2.setSubtotal(new BigDecimal("120000.00"));
        quotation2.setGrandTotal(new BigDecimal("141600.00"));
        quotation2 = quotationRepository.save(quotation2);
    }

    @Test
    public void testPhoneNumberNormalizationAndValidation() {
        assertEquals("919840112233", whatsAppService.normalizePhoneNumber("9840112233"));
        assertEquals("919944155667", whatsAppService.normalizePhoneNumber("+91 99441 55667"));
        assertEquals("919840112233", whatsAppService.normalizePhoneNumber("09840112233"));
        assertEquals("919840112233", whatsAppService.normalizePhoneNumber("00919840112233"));
        assertEquals("919840112233", whatsAppService.normalizePhoneNumber("+91-98401-12233"));

        // Invalid numbers
        assertThrows(IllegalArgumentException.class, () -> whatsAppService.normalizePhoneNumber(""));
        assertThrows(IllegalArgumentException.class, () -> whatsAppService.normalizePhoneNumber(null));
        assertThrows(IllegalArgumentException.class, () -> whatsAppService.normalizePhoneNumber("12345"));
        assertThrows(IllegalArgumentException.class, () -> whatsAppService.normalizePhoneNumber("911234567890")); // Invalid mobile digit '1'
    }

    @Test
    public void testSendDocumentViaMetaCloudApi() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.HBgM12345\"}]}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        whatsAppService.setHttpClient(mockHttpClient);
        whatsAppService.setApiEnabled(true);
        whatsAppService.setApiProvider("meta");
        whatsAppService.setPhoneNumberId("10987654321");
        whatsAppService.setApiToken("test_meta_token");
        whatsAppService.setApiUrl("https://graph.facebook.com/v21.0/10987654321/messages");

        WhatsAppResult result = whatsAppService.sendOrPrepareQuotationPdf(quotation1, "http://localhost:8080");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("API", result.getMethod());
        assertEquals("919840112233", result.getPhone());
        assertEquals("wamid.HBgM12345", result.getMessageId());
        assertEquals("meta", result.getProvider());
        assertTrue(result.getPdfSizeBytes() > 0);

        // Verify quotation status transitioned to SENT
        Quotation updated = quotationRepository.findById(quotation1.getId()).orElseThrow();
        assertEquals(QuotationStatus.SENT, updated.getStatus());

        // Verify communication history
        List<QuotationCommunication> comms = communicationRepository.findByQuotationIdOrderBySentAtDesc(quotation1.getId());
        assertFalse(comms.isEmpty());
        QuotationCommunication comm = comms.get(0);
        assertEquals(QuotationCommunication.CommunicationStatus.SENT, comm.getStatus());
        assertTrue(comm.getMessage().contains("wamid.HBgM12345"));
    }

    @Test
    public void testSendDocumentViaTwilioApi() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(201);
        when(mockResponse.body()).thenReturn("{\"sid\":\"SMabc123xyz\",\"status\":\"queued\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        whatsAppService.setHttpClient(mockHttpClient);
        whatsAppService.setApiEnabled(true);
        whatsAppService.setApiProvider("twilio");
        whatsAppService.setAccountSid("AC123456789");
        whatsAppService.setApiToken("twilio_auth_token");
        whatsAppService.setFromNumber("+14155238886");
        whatsAppService.setApiUrl("https://api.twilio.com/2010-04-01/Accounts/AC123456789/Messages.json");

        WhatsAppResult result = whatsAppService.sendOrPrepareQuotationPdf(quotation2, "http://localhost:8080");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("API", result.getMethod());
        assertEquals("919944155667", result.getPhone());
        assertEquals("SMabc123xyz", result.getMessageId());
        assertEquals("twilio", result.getProvider());
        assertTrue(result.getPdfSizeBytes() > 0);

        Quotation updated = quotationRepository.findById(quotation2.getId()).orElseThrow();
        assertEquals(QuotationStatus.SENT, updated.getStatus());
    }

    @Test
    public void testSendDocumentViaGenericApi() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"success\":true,\"messageId\":\"GEN-999888\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        whatsAppService.setHttpClient(mockHttpClient);
        whatsAppService.setApiEnabled(true);
        whatsAppService.setApiProvider("generic");
        whatsAppService.setApiUrl("https://api.customgateway.com/v1/whatsapp/document");
        whatsAppService.setApiToken("custom_gateway_token");

        WhatsAppResult result = whatsAppService.sendOrPrepareQuotationPdf(quotation1, "http://localhost:8080");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("GEN-999888", result.getMessageId());
        assertEquals("generic", result.getProvider());
    }

    @Test
    public void testMultipleCustomersUseCorrectPdfsAndNumbers() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"id\":\"MSG-OK\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        whatsAppService.setHttpClient(mockHttpClient);
        whatsAppService.setApiEnabled(true);
        whatsAppService.setApiProvider("generic");
        whatsAppService.setApiUrl("https://api.gateway.com/send");

        // Customer 1
        WhatsAppResult res1 = whatsAppService.sendOrPrepareQuotationPdf(quotation1, "http://localhost:8080");
        assertEquals("919840112233", res1.getPhone());
        assertEquals(quotation1.getQuotationNumber(), res1.getQuotationNumber());
        assertEquals("Ramesh Kumar", res1.getCustomerName());

        // Customer 2
        WhatsAppResult res2 = whatsAppService.sendOrPrepareQuotationPdf(quotation2, "http://localhost:8080");
        assertEquals("919944155667", res2.getPhone());
        assertEquals(quotation2.getQuotationNumber(), res2.getQuotationNumber());
        assertEquals("Priya Sharma", res2.getCustomerName());

        assertNotEquals(res1.getPhone(), res2.getPhone());
        assertNotEquals(res1.getQuotationNumber(), res2.getQuotationNumber());
    }

    @Test
    public void testSimpleWhatsAppSharingFlowWhenApiDisabled() {
        whatsAppService.setApiEnabled(false);

        WhatsAppResult res = whatsAppService.sendOrPrepareQuotationPdf(quotation1, "http://localhost:8080");
        assertNotNull(res);
        assertTrue(res.isSuccess());
        assertEquals("CLIENT_REDIRECT", res.getMethod());
        assertEquals("919840112233", res.getPhone());
        assertNotNull(res.getWhatsappUrl());
        assertTrue(res.getWhatsappUrl().startsWith("https://wa.me/919840112233"));
        assertTrue(res.getPdfSizeBytes() > 0);

        // Confirm quotation status transitioned to SENT
        Quotation updated = quotationRepository.findById(quotation1.getId()).orElseThrow();
        assertEquals(QuotationStatus.SENT, updated.getStatus());

        // Confirm communication history was recorded
        List<QuotationCommunication> comms = communicationRepository.findByQuotationIdOrderBySentAtDesc(quotation1.getId());
        assertFalse(comms.isEmpty());
        QuotationCommunication comm = comms.get(0);
        assertEquals(QuotationCommunication.CommunicationStatus.SENT, comm.getStatus());
        assertTrue(comm.getMessage().contains("Quotation PDF generated"));
    }

    @Test
    public void testMissingAndInvalidCustomerPhoneHandling() {
        // Customer with null / empty phone
        Customer emptyPhoneCust = new Customer();
        emptyPhoneCust.setName("No Phone Cust");
        emptyPhoneCust.setPhone("");
        emptyPhoneCust = customerRepository.save(emptyPhoneCust);

        quotation1.setCustomer(emptyPhoneCust);
        quotation1 = quotationRepository.save(quotation1);

        Exception exMissing = assertThrows(IllegalArgumentException.class, () -> {
            whatsAppService.sendOrPrepareQuotationPdf(quotation1, "http://localhost:8080");
        });
        assertTrue(exMissing.getMessage().contains("Customer phone number is not available"));

        // Customer with invalid phone characters/digits
        Customer invalidPhoneCust = new Customer();
        invalidPhoneCust.setName("Invalid Phone Cust");
        invalidPhoneCust.setPhone("12345");
        invalidPhoneCust = customerRepository.save(invalidPhoneCust);

        quotation1.setCustomer(invalidPhoneCust);
        quotation1 = quotationRepository.save(quotation1);

        Exception exInvalid = assertThrows(IllegalArgumentException.class, () -> {
            whatsAppService.sendOrPrepareQuotationPdf(quotation1, "http://localhost:8080");
        });
        assertTrue(exInvalid.getMessage().contains("format is invalid"));
    }

    @Test
    public void testProviderHttpErrorRecordsFailure() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn("{\"error\":{\"message\":\"Invalid destination number\"}}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        whatsAppService.setHttpClient(mockHttpClient);
        whatsAppService.setApiEnabled(true);
        whatsAppService.setApiProvider("meta");
        whatsAppService.setApiUrl("https://graph.facebook.com/v21.0/123/messages");

        Exception ex = assertThrows(RuntimeException.class, () -> {
            whatsAppService.sendOrPrepareQuotationPdf(quotation1, "http://localhost:8080");
        });
        assertTrue(ex.getMessage().contains("HTTP 400"));

        List<QuotationCommunication> comms = communicationRepository.findByQuotationIdOrderBySentAtDesc(quotation1.getId());
        assertFalse(comms.isEmpty());
        QuotationCommunication failedComm = comms.get(0);
        assertEquals(QuotationCommunication.CommunicationStatus.FAILED, failedComm.getStatus());
        assertTrue(failedComm.getMessage().contains("WhatsApp API delivery failed"));
    }

    @Test
    public void testTemporaryMediaTokenSecurity() {
        temporaryMediaTokenService.setTokenRequired(true);

        String validToken = temporaryMediaTokenService.generateToken(quotation1.getId());
        assertTrue(temporaryMediaTokenService.isValidToken(quotation1.getId(), validToken));

        // Wrong quotation ID with valid token should fail
        assertFalse(temporaryMediaTokenService.isValidToken(quotation2.getId(), validToken));

        // Tampered token should fail
        assertFalse(temporaryMediaTokenService.isValidToken(quotation1.getId(), validToken + "xyz"));

        // Public endpoint enforces token when tokenRequired is true
        ResponseEntity<byte[]> forbiddenResponse = publicQuotationPdfController.getPublicQuotationPdf(quotation1.getId(), null);
        assertEquals(HttpStatus.FORBIDDEN, forbiddenResponse.getStatusCode());

        ResponseEntity<byte[]> validResponse = publicQuotationPdfController.getPublicQuotationPdf(quotation1.getId(), validToken);
        assertEquals(HttpStatus.OK, validResponse.getStatusCode());
        assertNotNull(validResponse.getBody());

        // Restore tokenRequired to default
        temporaryMediaTokenService.setTokenRequired(false);
    }

    @Test
    public void testAdminControllerWhatsAppSending() {
        whatsAppService.setApiEnabled(true);
        whatsAppService.setApiProvider("mock");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "application/json");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object responseObj = quotationController.trackWhatsApp(quotation1.getId(), request, redirectAttributes);
        assertTrue(responseObj instanceof ResponseEntity<?>);
        ResponseEntity<?> response = (ResponseEntity<?>) responseObj;
        assertEquals(200, response.getStatusCode().value());
        WhatsAppResult wr = (WhatsAppResult) response.getBody();
        assertNotNull(wr);
        assertTrue(wr.isSuccess());
        assertEquals("API", wr.getMethod());
        assertTrue(wr.getMessageId().startsWith("MOCK-WA-"));
    }

    @Test
    public void testEmployeeControllerChecksOwnership() {
        whatsAppService.setApiEnabled(true);
        whatsAppService.setApiProvider("mock");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "application/json");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // Employee 1 owns quotation 1 -> should succeed
        Authentication authEmp1 = new UsernamePasswordAuthenticationToken(
                employee1.getEmail(), employee1.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        Object responseObj1 = employeeQuotationController.trackWhatsApp(quotation1.getId(), authEmp1, request, redirectAttributes);
        assertTrue(responseObj1 instanceof ResponseEntity<?>);
        ResponseEntity<?> resp1 = (ResponseEntity<?>) responseObj1;
        assertEquals(200, resp1.getStatusCode().value());

        // Employee 2 does NOT own quotation 1 -> should fail with 400 Bad Request
        Authentication authEmp2 = new UsernamePasswordAuthenticationToken(
                employee2.getEmail(), employee2.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        Object responseObj2 = employeeQuotationController.trackWhatsApp(quotation1.getId(), authEmp2, request, redirectAttributes);
        assertTrue(responseObj2 instanceof ResponseEntity<?>);
        ResponseEntity<?> resp2 = (ResponseEntity<?>) responseObj2;
        assertEquals(400, resp2.getStatusCode().value());
    }
}
