/**
 * Pravin Kitchens & Interiors
 * Customer Details Auto-Capitalization Utility
 * Automatically capitalizes the first letter of each word in customer text fields
 * while preserving existing casing, intentional punctuation, and spacing.
 */
(function (global) {
    'use strict';

    // Regex to match the start of a word: start of string or whitespace/punctuation followed by a lowercase letter
    var WORD_START_REGEX = /(^|[\s,.\-\/\\()\[\]#{}:;]+)(\p{Ll})/gu;

    /**
     * Capitalize the first letter of each word in a string without forcing other letters to lower/upper case.
     * Examples:
     *   "abishek kumar" -> "Abishek Kumar"
     *   "pravin kitchens" -> "Pravin Kitchens"
     *   "kanyakumari" -> "Kanyakumari"
     *   "McDonald" -> "McDonald"
     */
    function toTitleCase(str) {
        if (!str) return str;
        return String(str).replace(WORD_START_REGEX, function (match, delimiter, char) {
            return delimiter + char.toUpperCase();
        });
    }

    /**
     * Attach title capitalization to an input or textarea element.
     * Formats while typing and on blur, preserving cursor position so user typing is not disrupted.
     */
    function attachTitleCapitalization(input) {
        if (!input) return;

        function applyFormat() {
            var val = input.value;
            if (!val) return;
            var formatted = toTitleCase(val);
            if (formatted !== val) {
                var start = input.selectionStart;
                var end = input.selectionEnd;
                input.value = formatted;
                if (start !== null && end !== null && document.activeElement === input) {
                    try {
                        input.setSelectionRange(start, end);
                    } catch (e) {
                        // Some input types don't support selectionRange
                    }
                }
            }
        }

        input.addEventListener('input', applyFormat);
        input.addEventListener('blur', applyFormat);
    }

    /**
     * Initialize title capitalization for customer fields on quotation and customer forms.
     */
    function initCustomerTitleCapitalization() {
        // Target customer detail text fields
        var targetIds = [
            'customerName',
            'customerAddress',
            'customerCity',
            'customerDistrict',
            'name',
            'address',
            'city',
            'district'
        ];

        targetIds.forEach(function (id) {
            var el = document.getElementById(id);
            if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA')) {
                // Ensure it is not email, phone, tel, number, date, hidden, or checkbox/radio
                var type = (el.getAttribute('type') || '').toLowerCase();
                if (type !== 'email' && type !== 'tel' && type !== 'number' && type !== 'date' && type !== 'hidden' && type !== 'checkbox' && type !== 'radio') {
                    attachTitleCapitalization(el);
                }
            }
        });

        // Also target any inputs/textareas with class 'capitalize-title' or attribute 'data-title-case'
        var customElements = document.querySelectorAll('.capitalize-title, [data-title-case]');
        customElements.forEach(function (el) {
            if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA')) {
                attachTitleCapitalization(el);
            }
        });

        // Email fields: ensure email stays lowercase where appropriate and trimmed on blur
        var emailIds = ['customerEmail', 'email'];
        emailIds.forEach(function (id) {
            var el = document.getElementById(id);
            if (el) {
                el.addEventListener('blur', function () {
                    if (this.value) {
                        this.value = this.value.trim().toLowerCase();
                    }
                });
            }
        });
    }

    // Auto-run on DOMContentLoaded if DOM is still loading, else run immediately
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initCustomerTitleCapitalization);
    } else {
        initCustomerTitleCapitalization();
    }

    // Export to global scope
    global.toTitleCase = toTitleCase;
    global.attachTitleCapitalization = attachTitleCapitalization;
    global.initCustomerTitleCapitalization = initCustomerTitleCapitalization;

})(typeof window !== 'undefined' ? window : this);
