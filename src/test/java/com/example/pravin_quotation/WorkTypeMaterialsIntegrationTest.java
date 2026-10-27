package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.WorkCategoryController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.WorkCategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class WorkTypeMaterialsIntegrationTest {

    @Autowired
    private WorkCategoryService workCategoryService;

    @Autowired
    private WorkCategoryController workCategoryController;

    @Autowired
    private AdminQuotationBuilderController adminQuotationBuilderController;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private QuotationRoomRepository quotationRoomRepository;

    @Autowired
    private QuotationItemRepository quotationItemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    private WorkCategory createCategory(String name) {
        WorkCategory cat = new WorkCategory();
        cat.setName(name + " " + System.nanoTime());
        cat.setDescription("Category for " + name);
        cat.setActive(true);
        return workCategoryRepository.save(cat);
    }

    private Division createWorkType(WorkCategory cat, String name) {
        Division wt = new Division();
        wt.setName(name);
        wt.setWorkCategory(cat);
        wt.setActive(true);
        wt.setIcon("📁");
        return divisionRepository.save(wt);
    }

    @Test
    @DisplayName("1. Configure Materials for Work Type across Premium, Middle, Economy and Read Counts")
    void testCreateMaterialsInThreeGradesAndReadCounts() {
        WorkCategory cat = createCategory("Modular Kitchen Test");
        Division bottomCabinet = createWorkType(cat, "Bottom Cabinet Test");

        // 1. Add Premium material
        ResponseEntity<?> res1 = workCategoryController.createWorkTypeMaterial(
                bottomCabinet.getId(),
                "PREMIUM",
                "Premium Marine Plywood",
                "710 BWP Grade IS:710",
                "Sq.ft",
                BigDecimal.valueOf(1850.00)
        );
        assertEquals(HttpStatus.OK, res1.getStatusCode());

        // 2. Add Middle / Standard material
        ResponseEntity<?> res2 = workCategoryController.createWorkTypeMaterial(
                bottomCabinet.getId(),
                "MIDDLE",
                "Standard MR Plywood",
                "Commercial 303 Grade IS:303",
                "Sq.ft",
                BigDecimal.valueOf(1450.00)
        );
        assertEquals(HttpStatus.OK, res2.getStatusCode());

        // 3. Add Economy material
        ResponseEntity<?> res3 = workCategoryController.createWorkTypeMaterial(
                bottomCabinet.getId(),
                "ECONOMY",
                "Economy Particle Board",
                "Pre-laminated Interior Grade",
                "Sq.ft",
                BigDecimal.valueOf(950.00)
        );
        assertEquals(HttpStatus.OK, res3.getStatusCode());

        // Read all materials for this work type
        ResponseEntity<?> getRes = workCategoryController.getWorkTypeMaterials(bottomCabinet.getId());
        assertEquals(HttpStatus.OK, getRes.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) getRes.getBody();
        assertNotNull(data);

        assertEquals(1, data.get("premiumCount"));
        assertEquals(1, data.get("middleCount"));
        assertEquals(1, data.get("economyCount"));
        assertEquals(3, data.get("totalCount"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> premiumList = (List<Map<String, Object>>) data.get("premium");
        assertEquals(1, premiumList.size());
        assertEquals("Premium Marine Plywood", premiumList.get(0).get("name"));
        assertEquals("710 BWP Grade IS:710", premiumList.get(0).get("specification"));
        assertEquals("Sq.ft", premiumList.get(0).get("unit"));
        assertEquals(0, new BigDecimal("1850.00").compareTo((BigDecimal) premiumList.get(0).get("rate")));
        assertEquals("PREMIUM", premiumList.get(0).get("pricingGrade"));
    }

    @Test
    @DisplayName("2. Separate Material Configuration for Each Work Type (Isolation)")
    void testWorkTypeIsolation() {
        WorkCategory kitchen = createCategory("Kitchen Category");
        Division bottomCabinet = createWorkType(kitchen, "Bottom Cabinet");
        Division overheadCabinet = createWorkType(kitchen, "Overhead Cabinet");

        WorkCategory bedroom = createCategory("Bedroom Category");
        Division wardrobe = createWorkType(bedroom, "Wardrobe");

        // Add material to Bottom Cabinet only
        workCategoryController.createWorkTypeMaterial(
                bottomCabinet.getId(),
                "PREMIUM",
                "Action TESA HDHMR",
                "High Density Water Resistant",
                "Sq.ft",
                BigDecimal.valueOf(1700.00)
        );

        // Verify Bottom Cabinet has 1 material
        @SuppressWarnings("unchecked")
        Map<String, Object> bcData = (Map<String, Object>) workCategoryController.getWorkTypeMaterials(bottomCabinet.getId()).getBody();
        assertEquals(1, bcData.get("totalCount"));
        assertEquals(1, bcData.get("premiumCount"));

        // Verify Overhead Cabinet has 0 materials
        @SuppressWarnings("unchecked")
        Map<String, Object> ocData = (Map<String, Object>) workCategoryController.getWorkTypeMaterials(overheadCabinet.getId()).getBody();
        assertEquals(0, ocData.get("totalCount"));
        assertEquals(0, ocData.get("premiumCount"));

        // Verify Wardrobe has 0 materials
        @SuppressWarnings("unchecked")
        Map<String, Object> wData = (Map<String, Object>) workCategoryController.getWorkTypeMaterials(wardrobe.getId()).getBody();
        assertEquals(0, wData.get("totalCount"));
        assertEquals(0, wData.get("premiumCount"));
    }

    @Test
    @DisplayName("3. Material Update Operation")
    void testUpdateMaterial() {
        WorkCategory cat = createCategory("Living Room");
        Division tvUnit = createWorkType(cat, "TV Unit");

        workCategoryController.createWorkTypeMaterial(
                tvUnit.getId(),
                "ECONOMY",
                "Initial Board",
                "Old Spec",
                "Sq.ft",
                BigDecimal.valueOf(800.00)
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) workCategoryController.getWorkTypeMaterials(tvUnit.getId()).getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("economy");
        Long matId = ((Number) list.get(0).get("id")).longValue();

        // Update the material
        ResponseEntity<?> updateRes = workCategoryController.updateWorkTypeMaterial(
                matId,
                "Updated Acrylic Finish Board",
                "Charcoal Gloss 1.5mm",
                "Running ft",
                BigDecimal.valueOf(1250.00)
        );
        assertEquals(HttpStatus.OK, updateRes.getStatusCode());

        // Verify updated values
        Material updated = materialRepository.findById(matId).orElseThrow();
        assertEquals("Updated Acrylic Finish Board", updated.getName());
        assertEquals("Charcoal Gloss 1.5mm", updated.getSpecification());
        assertEquals("Running ft", updated.getUnit());
        assertEquals(0, new BigDecimal("1250.00").compareTo(updated.getRate()));
    }

    @Test
    @DisplayName("4. Duplicate Material Name and Negative Rate Validation")
    void testDuplicateAndValidationRules() {
        WorkCategory cat = createCategory("Office");
        Division desk = createWorkType(cat, "Workstation Desk");

        // Add first material
        ResponseEntity<?> res1 = workCategoryController.createWorkTypeMaterial(
                desk.getId(),
                "PREMIUM",
                "Century Plywood",
                "Club Prime",
                "Sq.ft",
                BigDecimal.valueOf(2200.00)
        );
        assertEquals(HttpStatus.OK, res1.getStatusCode());

        // Try adding duplicate in same work type and same grade
        ResponseEntity<?> resDuplicate = workCategoryController.createWorkTypeMaterial(
                desk.getId(),
                "PREMIUM",
                "century plywood", // case insensitive match
                "Different spec",
                "Sq.ft",
                BigDecimal.valueOf(2300.00)
        );
        assertEquals(HttpStatus.BAD_REQUEST, resDuplicate.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> errBody = (Map<String, Object>) resDuplicate.getBody();
        assertTrue(((String) errBody.get("message")).toLowerCase().contains("already exists"));

        // Try adding with negative rate
        ResponseEntity<?> resNeg = workCategoryController.createWorkTypeMaterial(
                desk.getId(),
                "PREMIUM",
                "Another Board",
                "Spec",
                "Sq.ft",
                BigDecimal.valueOf(-50.00)
        );
        assertEquals(HttpStatus.BAD_REQUEST, resNeg.getStatusCode());

        // Adding SAME name in a DIFFERENT grade (e.g. MIDDLE) is allowed
        ResponseEntity<?> resDiffGrade = workCategoryController.createWorkTypeMaterial(
                desk.getId(),
                "MIDDLE",
                "Century Plywood",
                "Sainik 710",
                "Sq.ft",
                BigDecimal.valueOf(1400.00)
        );
        assertEquals(HttpStatus.OK, resDiffGrade.getStatusCode());
    }

    @Test
    @DisplayName("5. Safe Material Deletion when Referenced in Quotation")
    void testSafeDeletionWhenReferenced() {
        Branch branch = new Branch();
        branch.setName("Main Showroom");
        branch.setActive(true);
        branch = branchRepository.save(branch);

        Customer customer = new Customer();
        customer.setName("Test Client");
        customer.setPhone("9876543210");
        customer.setBranch(branch);
        customer.setActive(true);
        customer = customerRepository.save(customer);

        WorkCategory cat = createCategory("Foyer");
        Division shoeRack = createWorkType(cat, "Shoe Rack");

        workCategoryController.createWorkTypeMaterial(
                shoeRack.getId(),
                "MIDDLE",
                "Carcass Board",
                "Moisture Resistant",
                "Sq.ft",
                BigDecimal.valueOf(1100.00)
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) workCategoryController.getWorkTypeMaterials(shoeRack.getId()).getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("middle");
        Long matId = ((Number) list.get(0).get("id")).longValue();
        Material mat = materialRepository.findById(matId).orElseThrow();

        User user = userRepository.findAll().stream().findFirst().orElseThrow();

        // Create historical quotation referencing this material
        Quotation quotation = new Quotation();
        quotation.setCustomer(customer);
        quotation.setBranch(branch);
        quotation.setEmployee(user);
        quotation.setPricingMode(PricingMode.MIDDLE);
        quotation.setQuotationNumber("QT-TEST-SAFE-DEL");
        quotation = quotationRepository.save(quotation);

        QuotationRoom room = new QuotationRoom(quotation, "Ground Floor", "Living", "Shoe rack setup");
        room = quotationRoomRepository.save(room);

        QuotationItem item = new QuotationItem();
        item.setQuotationRoom(room);
        item.setMaterial(mat);
        item.setWorkCategory(cat);
        item.setDivision(shoeRack);
        item.setRate(BigDecimal.valueOf(1100.00));
        item.setAmount(BigDecimal.valueOf(11000.00));
        quotationItemRepository.save(item);

        // Now attempt to delete the material
        ResponseEntity<?> delRes = workCategoryController.deleteWorkTypeMaterial(matId);
        assertEquals(HttpStatus.OK, delRes.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> delBody = (Map<String, Object>) delRes.getBody();
        assertTrue(((String) delBody.get("message")).toLowerCase().contains("deactivated"));

        // Historical quotation item still references the material
        List<QuotationItem> items = quotationItemRepository.findByMaterialIdOrderByCreatedAtDesc(matId);
        assertEquals(1, items.size());
        assertEquals(matId, items.get(0).getMaterial().getId());

        // Material is deactivated so it won't show in new selections
        Material reloaded = materialRepository.findById(matId).orElseThrow();
        assertFalse(reloaded.isActive());
    }

    @Test
    @DisplayName("6. Quotation Builder loads configured materials and rates by Work Type and Grade")
    void testQuotationBuilderIntegration() {
        WorkCategory cat = createCategory("Pantry");
        Division shelf = createWorkType(cat, "Open Shelves");

        workCategoryController.createWorkTypeMaterial(
                shelf.getId(),
                "PREMIUM",
                "Teak Finish Wood",
                "Solid Wood 25mm",
                "Sq.ft",
                BigDecimal.valueOf(2500.00)
        );

        workCategoryController.createWorkTypeMaterial(
                shelf.getId(),
                "MIDDLE",
                "Rubberwood Board",
                "Finger Jointed 18mm",
                "Sq.ft",
                BigDecimal.valueOf(1600.00)
        );

        // Fetch builder materials for Premium
        List<Map<String, Object>> premiumMaterials = adminQuotationBuilderController.materials(shelf.getId(), null, "PREMIUM");
        assertEquals(1, premiumMaterials.size());
        assertTrue("TEAK FINISH WOOD".equalsIgnoreCase((String) premiumMaterials.get(0).get("name")));
        assertEquals("Solid Wood 25mm", premiumMaterials.get(0).get("specification"));
        assertEquals(0, new BigDecimal("2500.00").compareTo((BigDecimal) premiumMaterials.get(0).get("rate")));

        // Fetch builder materials for Middle
        List<Map<String, Object>> middleMaterials = adminQuotationBuilderController.materials(shelf.getId(), null, "MIDDLE");
        assertEquals(1, middleMaterials.size());
        assertTrue("RUBBERWOOD BOARD".equalsIgnoreCase((String) middleMaterials.get(0).get("name")));
        assertEquals(0, new BigDecimal("1600.00").compareTo((BigDecimal) middleMaterials.get(0).get("rate")));

        // Verify rate endpoint returns material rate
        Long premiumMatId = ((Number) premiumMaterials.get(0).get("id")).longValue();
        ResponseEntity<?> rateRes = adminQuotationBuilderController.rate(null, null, premiumMatId, PricingMode.PREMIUM);
        assertEquals(HttpStatus.OK, rateRes.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> rateMap = (Map<String, Object>) rateRes.getBody();
        assertEquals(0, new BigDecimal("2500.00").compareTo((BigDecimal) rateMap.get("rate")));
        assertEquals("Sq.ft", rateMap.get("unit"));
    }
}
