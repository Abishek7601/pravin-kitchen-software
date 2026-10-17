/**
 * Pravin Kitchens & Interiors - Universal Table Pagination Engine
 * Brand Theme: #024541
 * 
 * Features:
 * - 10 records per page (customizable)
 * - Page numbers (1, 2, 3...) with intelligent ellipsis for many pages
 * - Previous & Next buttons with automatic disable states
 * - Slices records (Page 1 = 1-10, Page 2 = 11-20, Page 3 = 21-30, etc.)
 * - Auto-hides or disables controls when records <= 10
 * - Fully synchronized with instant client search and filter dropdowns
 * - Returns to Page 1 whenever search query or filter changes
 * - Preserves URL parameters & updates `?page=X` cleanly
 * - Accessible, responsive, and compatible across desktop and mobile
 */

(function (window, document) {
    'use strict';

    var DEFAULT_PAGE_SIZE = 10;

    function parsePageFromUrl() {
        try {
            var params = new URLSearchParams(window.location.search);
            var page = parseInt(params.get('page'), 10);
            return (page && page > 0) ? page : 1;
        } catch (e) {
            return 1;
        }
    }

    function updateUrlPage(pageNumber) {
        try {
            var url = new URL(window.location.href);
            if (pageNumber > 1) {
                url.searchParams.set('page', pageNumber);
            } else {
                url.searchParams.delete('page');
            }
            window.history.replaceState({ page: pageNumber }, '', url.toString());
        } catch (e) {
            // URL modification may fail in unsupported environments, safely ignored
        }
    }

    function TablePagination(config) {
        if (!config || !config.table) return;

        this.table = typeof config.table === 'string' ? document.querySelector(config.table) : config.table;
        if (!this.table) return;

        this.pageSize = config.pageSize || DEFAULT_PAGE_SIZE;
        this.rowSelector = config.rowSelector || 'tbody tr:not(.empty-state-row):not(#noResultsRow):not(#noSearchResults):not(.no-search-results):not(.table-search-empty)';
        this.emptyRowSelector = config.emptyRowSelector || '#noResultsRow, #noSearchResults, .empty-state-row, tr:has(.empty-state)';
        this.customFilter = config.customFilter || null;
        this.onPageChange = config.onPageChange || null;
        this.searchInputs = config.searchInputs || [];
        this.filterSelects = config.filterSelects || [];
        this.recordLabel = config.recordLabel || 'records';
        this.updateSerialNumbers = config.updateSerialNumbers !== false;

        this.currentPage = parsePageFromUrl();
        this.matchedRows = [];

        this.initDOM();
        this.bindEvents();
        this.applyFilterAndRender();
    }

    TablePagination.prototype.initDOM = function () {
        // Find or create pagination footer container
        var existingFooter = this.table.parentNode.querySelector('.pagination-footer');
        if (!existingFooter && this.table.closest('.table-card, .content-card, .section-card, .table-wrapper, .table-responsive')) {
            var parentCard = this.table.closest('.table-card, .content-card, .section-card') || this.table.parentNode;
            existingFooter = parentCard.querySelector('.pagination-footer');
        }

        if (!existingFooter) {
            this.footer = document.createElement('div');
            this.footer.className = 'pagination-footer';
            this.footer.innerHTML = 
                '<div class="pagination-info">' +
                    'Showing <strong class="page-start">0</strong>–<strong class="page-end">0</strong> of <strong class="page-total">0</strong> ' + this.recordLabel +
                '</div>' +
                '<nav class="pagination-nav" aria-label="Table navigation">' +
                    '<button type="button" class="pagination-btn pagination-prev" aria-label="Previous page">&lsaquo; Previous</button>' +
                    '<div class="pagination-pages" style="display: inline-flex; align-items: center; gap: 4px;"></div>' +
                    '<button type="button" class="pagination-btn pagination-next" aria-label="Next page">Next &rsaquo;</button>' +
                '</nav>';

            // Insert after table wrapper or table
            var targetWrapper = this.table.closest('.table-responsive, .table-wrapper') || this.table;
            if (targetWrapper.nextSibling) {
                targetWrapper.parentNode.insertBefore(this.footer, targetWrapper.nextSibling);
            } else {
                targetWrapper.parentNode.appendChild(this.footer);
            }
        } else {
            this.footer = existingFooter;
        }

        this.infoStart = this.footer.querySelector('.page-start');
        this.infoEnd = this.footer.querySelector('.page-end');
        this.infoTotal = this.footer.querySelector('.page-total');
        this.prevBtn = this.footer.querySelector('.pagination-prev');
        this.nextBtn = this.footer.querySelector('.pagination-next');
        this.pagesContainer = this.footer.querySelector('.pagination-pages');
    };

    TablePagination.prototype.bindEvents = function () {
        var self = this;

        if (this.prevBtn) {
            this.prevBtn.addEventListener('click', function (e) {
                e.preventDefault();
                if (self.currentPage > 1) {
                    self.goToPage(self.currentPage - 1);
                }
            });
        }

        if (this.nextBtn) {
            this.nextBtn.addEventListener('click', function (e) {
                e.preventDefault();
                var totalPages = self.getTotalPages();
                if (self.currentPage < totalPages) {
                    self.goToPage(self.currentPage + 1);
                }
            });
        }

        // Attach listeners to search inputs
        this.searchInputs.forEach(function (inputRef) {
            var el = typeof inputRef === 'string' ? document.querySelector(inputRef) : inputRef;
            if (el) {
                el.addEventListener('input', function () {
                    self.currentPage = 1;
                    self.applyFilterAndRender();
                });
            }
        });

        // Attach listeners to filter selects
        this.filterSelects.forEach(function (selectRef) {
            var el = typeof selectRef === 'string' ? document.querySelector(selectRef) : selectRef;
            if (el) {
                el.addEventListener('change', function () {
                    self.currentPage = 1;
                    self.applyFilterAndRender();
                });
            }
        });
    };

    TablePagination.prototype.getAllRows = function () {
        var rows = Array.prototype.slice.call(this.table.querySelectorAll(this.rowSelector));
        return rows.filter(function (row) {
            return !row.classList.contains('empty-state-row') &&
                   row.id !== 'noResultsRow' &&
                   row.id !== 'noSearchResults' &&
                   !row.querySelector('.empty-state');
        });
    };

    TablePagination.prototype.applyFilterAndRender = function () {
        var allRows = this.getAllRows();
        var self = this;

        if (typeof this.customFilter === 'function') {
            this.matchedRows = allRows.filter(function (row, idx) {
                return self.customFilter(row, idx);
            });
        } else {
            // Check default search & filters from configured inputs
            var searchQuery = '';
            self.searchInputs.forEach(function (inputRef) {
                var el = typeof inputRef === 'string' ? document.querySelector(inputRef) : inputRef;
                if (el && el.value) {
                    searchQuery = el.value.trim().toLowerCase();
                }
            });

            this.matchedRows = allRows.filter(function (row) {
                var isRowHiddenByExistingScript = row.getAttribute('data-filtered-out') === 'true';
                if (isRowHiddenByExistingScript) {
                    return false;
                }

                if (searchQuery) {
                    var text = row.innerText.toLowerCase();
                    return text.includes(searchQuery);
                }
                return true;
            });
        }

        var totalPages = this.getTotalPages();
        if (this.currentPage > totalPages) {
            this.currentPage = totalPages > 0 ? totalPages : 1;
        }

        this.render();
    };

    TablePagination.prototype.getTotalPages = function () {
        return Math.ceil(this.matchedRows.length / this.pageSize);
    };

    TablePagination.prototype.goToPage = function (pageNumber) {
        var totalPages = this.getTotalPages();
        if (pageNumber < 1) pageNumber = 1;
        if (totalPages > 0 && pageNumber > totalPages) pageNumber = totalPages;

        this.currentPage = pageNumber;
        updateUrlPage(pageNumber);
        this.render();

        if (typeof this.onPageChange === 'function') {
            this.onPageChange(pageNumber);
        }
    };

    TablePagination.prototype.render = function () {
        var allRows = this.getAllRows();
        var totalRecords = this.matchedRows.length;
        var totalPages = this.getTotalPages();

        // 1. Hide all rows by default
        allRows.forEach(function (row) {
            row.style.display = 'none';
        });

        // 2. Determine slice range
        var startIndex = (this.currentPage - 1) * this.pageSize;
        var endIndex = Math.min(startIndex + this.pageSize, totalRecords);

        // 3. Show current page rows
        for (var i = startIndex; i < endIndex; i++) {
            if (this.matchedRows[i]) {
                this.matchedRows[i].style.display = '';
            }
        }

        // 4. Update empty state visibility if present
        var emptyRows = this.table.querySelectorAll(this.emptyRowSelector);
        emptyRows.forEach(function (emptyRow) {
            emptyRow.style.display = (totalRecords === 0 && allRows.length > 0) ? '' : 'none';
        });

        // 5. Update pagination text
        if (this.infoStart) this.infoStart.textContent = totalRecords === 0 ? '0' : (startIndex + 1);
        if (this.infoEnd) this.infoEnd.textContent = endIndex;
        if (this.infoTotal) this.infoTotal.textContent = totalRecords;

        // 6. Handle visibility of pagination footer when records <= 10 or 0
        if (totalRecords <= this.pageSize) {
            this.footer.classList.add('is-hidden');
        } else {
            this.footer.classList.remove('is-hidden');
        }

        // 7. Update button states
        if (this.prevBtn) {
            this.prevBtn.disabled = (this.currentPage <= 1);
            if (this.currentPage <= 1) {
                this.prevBtn.classList.add('disabled');
            } else {
                this.prevBtn.classList.remove('disabled');
            }
        }

        if (this.nextBtn) {
            this.nextBtn.disabled = (this.currentPage >= totalPages || totalPages === 0);
            if (this.currentPage >= totalPages || totalPages === 0) {
                this.nextBtn.classList.add('disabled');
            } else {
                this.nextBtn.classList.remove('disabled');
            }
        }

        // 8. Render page number buttons
        this.renderPageNumbers(totalPages);
    };

    TablePagination.prototype.renderPageNumbers = function (totalPages) {
        if (!this.pagesContainer) return;
        this.pagesContainer.innerHTML = '';

        if (totalPages <= 1) return;

        var self = this;
        var pages = [];

        if (totalPages <= 7) {
            for (var p = 1; p <= totalPages; p++) {
                pages.push(p);
            }
        } else {
            pages.push(1);
            if (this.currentPage > 3) {
                pages.push('...');
            }

            var start = Math.max(2, this.currentPage - 1);
            var end = Math.min(totalPages - 1, this.currentPage + 1);

            if (this.currentPage <= 3) {
                start = 2;
                end = 4;
            } else if (this.currentPage >= totalPages - 2) {
                start = totalPages - 3;
                end = totalPages - 1;
            }

            for (var i = start; i <= end; i++) {
                pages.push(i);
            }

            if (this.currentPage < totalPages - 2) {
                pages.push('...');
            }
            pages.push(totalPages);
        }

        pages.forEach(function (item) {
            if (item === '...') {
                var ellipsis = document.createElement('span');
                ellipsis.className = 'pagination-ellipsis';
                ellipsis.innerHTML = '&hellip;';
                self.pagesContainer.appendChild(ellipsis);
            } else {
                var pageBtn = document.createElement('button');
                pageBtn.type = 'button';
                pageBtn.className = 'pagination-page-btn' + (item === self.currentPage ? ' active' : '');
                pageBtn.textContent = item;
                pageBtn.setAttribute('aria-label', 'Page ' + item);
                if (item === self.currentPage) {
                    pageBtn.setAttribute('aria-current', 'page');
                }

                pageBtn.addEventListener('click', function (e) {
                    e.preventDefault();
                    self.goToPage(item);
                });

                self.pagesContainer.appendChild(pageBtn);
            }
        });
    };

    // Global helper
    window.initTablePagination = function (config) {
        return new TablePagination(config);
    };

    // Auto-init on DOMContentLoaded for tables with data-pagination="true" or data-paginate="10"
    document.addEventListener('DOMContentLoaded', function () {
        var autoTables = document.querySelectorAll('table[data-pagination="true"], table[data-paginate]');
        autoTables.forEach(function (table) {
            var pageSize = parseInt(table.getAttribute('data-paginate'), 10) || DEFAULT_PAGE_SIZE;
            var searchInputSelector = table.getAttribute('data-search-input');
            var searchInputs = searchInputSelector ? [searchInputSelector] : [];
            var filterSelectSelector = table.getAttribute('data-filter-select');
            var filterSelects = filterSelectSelector ? [filterSelectSelector] : [];

            initTablePagination({
                table: table,
                pageSize: pageSize,
                searchInputs: searchInputs,
                filterSelects: filterSelects
            });
        });
    });

})(window, document);
