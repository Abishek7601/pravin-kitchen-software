package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Branch;
import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(UserRepository userRepository,
                           BranchRepository branchRepository,
                           PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================
    // GET ALL EMPLOYEES
    // =========================

    public List<User> getAllEmployees() {

        return userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() == Role.EMPLOYEE)
                .toList();
    }

    // =========================
    // GET EMPLOYEE BY ID
    // =========================

    public User getEmployeeById(Long id) {

        User employee = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Employee not found"));

        if (employee.getRole() != Role.EMPLOYEE) {
            throw new IllegalArgumentException("User is not an employee");
        }

        return employee;
    }

    // =========================
    // CREATE EMPLOYEE
    // =========================

    public User createEmployee(String name,
                               String email,
                               String password,
                               Long branchId) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee name is required");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee email is required");
        }

        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Employee password is required");
        }

        if (branchId == null) {
            throw new IllegalArgumentException("District is required");
        }

        email = email.trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email address already exists");
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new IllegalArgumentException("District not found"));

        User employee = new User();

        employee.setName(name.trim());
        employee.setEmail(email);
        employee.setPassword(passwordEncoder.encode(password));
        employee.setRole(Role.EMPLOYEE);
        employee.setBranch(branch);
        employee.setActive(true);

        return userRepository.save(employee);
    }

    // =========================
    // UPDATE EMPLOYEE
    // =========================

    public User updateEmployee(Long id,
                               String name,
                               String email,
                               Long branchId) {

        User employee = getEmployeeById(id);

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee name is required");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee email is required");
        }

        if (branchId == null) {
            throw new IllegalArgumentException("District is required");
        }

        email = email.trim().toLowerCase();

        User existingUser = userRepository
                .findByEmail(email)
                .orElse(null);

        if (existingUser != null &&
                !existingUser.getId().equals(id)) {

            throw new IllegalArgumentException(
                    "Email address already exists");
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new IllegalArgumentException("District not found"));

        employee.setName(name.trim());
        employee.setEmail(email);
        employee.setBranch(branch);

        return userRepository.save(employee);
    }

    // =========================
    // ACTIVATE / DEACTIVATE
    // =========================

    public void toggleEmployeeStatus(Long id) {

        User employee = getEmployeeById(id);

        employee.setActive(
                !Boolean.TRUE.equals(employee.getActive())
        );

        userRepository.save(employee);
    }

    // =========================
    // CHANGE PASSWORD
    // =========================

    public void changePassword(Long id, String newPassword) {

        User employee = getEmployeeById(id);

        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException(
                    "Password cannot be empty");
        }

        employee.setPassword(
                passwordEncoder.encode(newPassword)
        );

        userRepository.save(employee);
    }

    // =========================
    // DELETE EMPLOYEE
    // =========================

    public void deleteEmployee(Long id) {

        User employee = getEmployeeById(id);

        userRepository.delete(employee);
    }
}