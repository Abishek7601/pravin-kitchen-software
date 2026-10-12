package com.example.pravin_quotation;

import com.example.pravin_quotation.model.Branch;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.service.BranchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class BranchAddressTest {

    @Autowired
    private BranchService branchService;

    @Autowired
    private BranchRepository branchRepository;

    @Test
    public void testCreateBranchWithAddress() {
        Branch branch = new Branch();
        branch.setName("Test Address Branch " + System.currentTimeMillis());
        branch.setEmail("testbranch@example.com");
        branch.setWhatsapp("9876543210");
        branch.setBranchAddress("  123 Main Road, Near Bus Stand, Chennai 600001  ");

        Branch saved = branchService.saveBranch(branch);

        assertNotNull(saved.getId());
        assertEquals("123 Main Road, Near Bus Stand, Chennai 600001", saved.getBranchAddress());

        Branch retrieved = branchService.getBranchById(saved.getId());
        assertEquals("123 Main Road, Near Bus Stand, Chennai 600001", retrieved.getBranchAddress());
    }

    @Test
    public void testUpdateBranchAddress() {
        Branch branch = new Branch();
        branch.setName("Update Branch " + System.currentTimeMillis());
        branch.setBranchAddress("Old Address");

        Branch saved = branchService.saveBranch(branch);
        assertEquals("Old Address", saved.getBranchAddress());

        // Update address
        saved.setBranchAddress("New Updated Address, Suite 400");
        Branch updated = branchService.saveBranch(saved);

        assertEquals("New Updated Address, Suite 400", updated.getBranchAddress());
    }

    @Test
    public void testBranchAddressTrimmingAndEmptyHandling() {
        Branch branch = new Branch();
        branch.setName("Trim Branch " + System.currentTimeMillis());
        branch.setBranchAddress("   ");

        Branch saved = branchService.saveBranch(branch);
        assertNull(saved.getBranchAddress(), "Whitespace-only address should be trimmed to null");
    }
}
