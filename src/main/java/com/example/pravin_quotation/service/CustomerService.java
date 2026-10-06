package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Branch;
import com.example.pravin_quotation.model.Customer;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.CustomerRepository;
import com.example.pravin_quotation.repository.QuotationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final QuotationRepository quotationRepository;

    public CustomerService(
            CustomerRepository customerRepository,
            BranchRepository branchRepository,
            QuotationRepository quotationRepository) {

        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.quotationRepository = quotationRepository;
    }


    // ==========================================
    // GET ALL CUSTOMERS
    // ==========================================

    public List<Customer> getAllCustomers() {

        return customerRepository.findAll();
    }


    // ==========================================
    // GET CUSTOMER BY ID
    // ==========================================

    public Customer getCustomerById(Long id) {

        return customerRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found"));
    }


    // ==========================================
    // GET CUSTOMERS BY DISTRICT
    // ==========================================

    public List<Customer> getCustomersByBranch(Long branchId) {

        return customerRepository.findByBranchId(branchId);
    }


    // ==========================================
    // GET ACTIVE CUSTOMERS
    // ==========================================

    public List<Customer> getActiveCustomers() {

        return customerRepository.findByActiveTrue();
    }


    // ==========================================
    // SEARCH CUSTOMERS
    // ==========================================

    public List<Customer> searchCustomers(String name) {

        if (name == null || name.trim().isEmpty()) {
            return getAllCustomers();
        }

        return customerRepository
                .findByNameContainingIgnoreCase(name.trim());
    }


    // ==========================================
    // CREATE CUSTOMER
    // ==========================================

    public Customer createCustomer(
            String name,
            String email,
            String phone,
            String address,
            Long branchId) {

        validateCustomer(
                name,
                phone,
                branchId
        );

        String customerName = name.trim();

        String customerEmail =
                email == null
                        ? null
                        : email.trim().toLowerCase();

        String customerPhone =
                phone.trim();

        String customerAddress =
                address == null
                        ? null
                        : address.trim();


        // Duplicate email check

        if (customerEmail != null
                && !customerEmail.isEmpty()
                && customerRepository.existsByEmail(customerEmail)) {

            throw new IllegalArgumentException(
                    "Email address already exists");
        }


        // Duplicate phone check

        if (customerRepository.existsByPhone(customerPhone)) {

            throw new IllegalArgumentException(
                    "Phone number already exists");
        }


        Branch branch =
                branchRepository.findById(branchId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "District not found"));


        Customer customer = new Customer();

        customer.setName(customerName);

        customer.setEmail(
                customerEmail != null
                        && !customerEmail.isEmpty()
                        ? customerEmail
                        : null
        );

        customer.setPhone(customerPhone);

        customer.setAddress(
                customerAddress != null
                        && !customerAddress.isEmpty()
                        ? customerAddress
                        : null
        );

        customer.setBranch(branch);

        customer.setActive(true);


        return customerRepository.save(customer);
    }


    // ==========================================
    // UPDATE CUSTOMER
    // ==========================================

    public Customer updateCustomer(
            Long id,
            String name,
            String email,
            String phone,
            String address,
            Long branchId) {

        Customer customer =
                getCustomerById(id);

        validateCustomer(
                name,
                phone,
                branchId
        );


        String customerEmail =
                email == null
                        ? null
                        : email.trim().toLowerCase();

        String customerPhone =
                phone.trim();


        // Check duplicate email

        if (customerEmail != null
                && !customerEmail.isEmpty()) {

            Customer existingEmail =
                    customerRepository.findAll()
                            .stream()
                            .filter(existing ->
                                    existing.getEmail() != null
                                            && existing.getEmail()
                                            .equalsIgnoreCase(
                                                    customerEmail))
                            .findFirst()
                            .orElse(null);

            if (existingEmail != null
                    && !existingEmail.getId().equals(id)) {

                throw new IllegalArgumentException(
                        "Email address already exists");
            }
        }


        // Check duplicate phone

        Customer existingPhone =
                customerRepository.findAll()
                        .stream()
                        .filter(existing ->
                                existing.getPhone() != null
                                        && existing.getPhone()
                                        .equals(customerPhone))
                        .findFirst()
                        .orElse(null);

        if (existingPhone != null
                && !existingPhone.getId().equals(id)) {

            throw new IllegalArgumentException(
                    "Phone number already exists");
        }


        Branch branch =
                branchRepository.findById(branchId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "District not found"));


        customer.setName(name.trim());

        customer.setEmail(
                customerEmail != null
                        && !customerEmail.isEmpty()
                        ? customerEmail
                        : null
        );

        customer.setPhone(customerPhone);

        customer.setAddress(
                address == null
                        ? null
                        : address.trim()
        );

        customer.setBranch(branch);


        return customerRepository.save(customer);
    }


    // ==========================================
    // ACTIVATE / DEACTIVATE
    // ==========================================

    public void toggleCustomerStatus(Long id) {

        Customer customer =
                getCustomerById(id);

        customer.setActive(
                !Boolean.TRUE.equals(
                        customer.getActive())
        );

        customerRepository.save(customer);
    }

    public void deleteCustomer(Long id) {

        Customer customer = getCustomerById(id);

        long quotationCount =
                quotationRepository.countByCustomerId(id);

        if (quotationCount > 0) {
            throw new IllegalStateException(
                    "This customer has " + quotationCount
                            + " linked quotation(s). "
                            + "Please deactivate the customer instead."
            );
        }

        customerRepository.delete(customer);
    }


    // ==========================================
    // VALIDATION
    // ==========================================

    private void validateCustomer(
            String name,
            String phone,
            Long branchId) {

        if (name == null
                || name.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Customer name is required");
        }


        if (phone == null
                || phone.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Customer phone number is required");
        }


        if (branchId == null) {

            throw new IllegalArgumentException(
                    "District is required");
        }
    }
}