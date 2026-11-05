package com.example.pravin_quotation.util;

import java.util.ArrayList;
import java.util.List;

public final class PaginationUtils {

    private PaginationUtils() {}

    /**
     * Generates a compact list of page numbers for pagination controls.
     * Ellipsis is represented by -1.
     *
     * @param currentPage current 1-based page number
     * @param totalPages total number of pages
     * @return list of page numbers with -1 for ellipsis
     */
    public static List<Integer> getPageNumbers(int currentPage, int totalPages) {
        List<Integer> pages = new ArrayList<>();
        if (totalPages <= 1) {
            if (totalPages == 1) {
                pages.add(1);
            }
            return pages;
        }

        if (totalPages <= 7) {
            for (int i = 1; i <= totalPages; i++) {
                pages.add(i);
            }
            return pages;
        }

        pages.add(1);
        if (currentPage > 4) {
            pages.add(-1);
        }

        int start = Math.max(2, currentPage - 1);
        int end = Math.min(totalPages - 1, currentPage + 1);

        if (currentPage <= 4) {
            start = 2;
            end = Math.min(5, totalPages - 1);
        } else if (currentPage >= totalPages - 3) {
            start = Math.max(2, totalPages - 4);
            end = totalPages - 1;
        }

        for (int i = start; i <= end; i++) {
            pages.add(i);
        }

        if (currentPage < totalPages - 3) {
            pages.add(-1);
        }
        pages.add(totalPages);

        return pages;
    }
}

