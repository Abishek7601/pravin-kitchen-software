package com.example.pravin_quotation.dto;

public record BranchCustomerCountDto(
        Long branchId,
        String branchName,
        long customerCount
) {
}
