
        package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Branch;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;

    public BranchService(
            BranchRepository branchRepository,
            UserRepository userRepository) {

        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
    }


    // =========================================================
    // GET ALL BRANCHES
    // =========================================================

    public List<Branch> getAllBranches() {

        return branchRepository.findAll();
    }


    // =========================================================
    // GET BRANCH BY ID
    // =========================================================

    public Branch getBranchById(Long id) {

        if (id == null) {

            throw new IllegalArgumentException(
                    "Branch ID is required"
            );
        }

        return branchRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Branch not found"
                        )
                );
    }


    // =========================================================
    // GET EMPLOYEES BY BRANCH
    // =========================================================

    public List<User> getEmployeesByBranch(Long branchId) {

        if (branchId == null) {

            throw new IllegalArgumentException(
                    "Branch ID is required"
            );
        }

        return userRepository.findByBranchId(branchId)
                .stream()
                .filter(user -> user.getRole() != null)
                .filter(user ->
                        user.getRole()
                                .name()
                                .equals("EMPLOYEE")
                )
                .toList();
    }


    // =========================================================
    // SAVE / UPDATE BRANCH
    // =========================================================

    @Transactional
    public Branch saveBranch(Branch branch) {

        if (branch == null) {

            throw new IllegalArgumentException(
                    "Branch data is required"
            );
        }


        // -----------------------------------------------------
        // Validate branch name
        // -----------------------------------------------------

        if (branch.getName() == null ||
                branch.getName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Branch name is required"
            );
        }


        String branchName =
                branch.getName().trim();


        // -----------------------------------------------------
        // Duplicate branch validation
        // -----------------------------------------------------

        boolean duplicate;

        if (branch.getId() == null) {

            duplicate =
                    branchRepository
                            .existsByNameIgnoreCase(
                                    branchName
                            );

        } else {

            duplicate =
                    branchRepository
                            .existsByNameIgnoreCaseAndIdNot(
                                    branchName,
                                    branch.getId()
                            );
        }


        if (duplicate) {

            throw new IllegalArgumentException(
                    "Branch name already exists"
            );
        }


        // -----------------------------------------------------
        // Clean values
        // -----------------------------------------------------

        branch.setName(branchName);


        if (branch.getEmail() != null) {

            String email =
                    branch.getEmail().trim();

            branch.setEmail(
                    email.isEmpty() ? null : email
            );
        }


        if (branch.getWhatsapp() != null) {

            String whatsapp =
                    branch.getWhatsapp().trim();

            branch.setWhatsapp(
                    whatsapp.isEmpty() ? null : whatsapp
            );
        }


        // -----------------------------------------------------
        // Default active status
        // -----------------------------------------------------

        if (branch.getActive() == null) {

            branch.setActive(true);
        }


        return branchRepository.save(branch);
    }


    // =========================================================
    // TOGGLE BRANCH STATUS
    // =========================================================

    @Transactional
    public void toggleBranchStatus(Long id) {

        Branch branch =
                getBranchById(id);


        boolean currentStatus =
                Boolean.TRUE.equals(
                        branch.getActive()
                );


        branch.setActive(
                !currentStatus
        );


        branchRepository.save(branch);
    }


    // =========================================================
    // DELETE BRANCH
    // =========================================================

    /*
     * Branch can be connected with:
     *
     * - Employees
     * - Customers
     * - Quotations
     * - Travel Charges
     * - District Pricing
     *
     * So physical deletion can cause
     * foreign-key constraint problems.
     *
     * For safety, we deactivate the branch.
     */

    @Transactional
    public void deleteBranch(Long id) {

        Branch branch =
                getBranchById(id);


        branch.setActive(false);


        branchRepository.save(branch);
    }

}

