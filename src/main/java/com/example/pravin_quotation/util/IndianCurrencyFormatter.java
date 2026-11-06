package com.example.pravin_quotation.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reusable utility for formatting monetary amounts using the Indian Numbering System (en-IN)
 * with exactly two decimal places and proper comma separators.
 *
 * Examples:
 *   74810.00    -> 74,810.00       / ₹ 74,810.00
 *   125000.00   -> 1,25,000.00      / ₹ 1,25,000.00
 *   1250000.00  -> 12,50,000.00     / ₹ 12,50,000.00
 *   10000000.00 -> 1,00,00,000.00    / ₹ 1,00,00,000.00
 *   500.00      -> 500.00          / ₹ 500.00
 */
@Component("inr")
public class IndianCurrencyFormatter {

    public String format(Object value) {
        return formatAmount(value);
    }

    public String rupees(Object value) {
        return formatWithRupee(value);
    }

    public static String formatAmount(Object value) {
        if (value == null) {
            return "0.00";
        }
        if (value instanceof BigDecimal bd) {
            return formatBigDecimal(bd);
        }
        if (value instanceof Number num) {
            return formatBigDecimal(BigDecimal.valueOf(num.doubleValue()));
        }
        String str = value.toString().trim();
        if (str.isEmpty() || "-".equals(str)) {
            return "0.00";
        }
        try {
            String clean = str.replaceAll("[^0-9.-]", "");
            if (clean.isEmpty() || "-".equals(clean) || ".".equals(clean)) {
                return "0.00";
            }
            return formatBigDecimal(new BigDecimal(clean));
        } catch (Exception e) {
            return "0.00";
        }
    }

    public static String formatBigDecimal(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        boolean isNegative = amount.compareTo(BigDecimal.ZERO) < 0;
        BigDecimal absVal = amount.abs().setScale(2, RoundingMode.HALF_UP);
        String plain = absVal.toPlainString();
        int dotIdx = plain.indexOf('.');
        String intPart = dotIdx >= 0 ? plain.substring(0, dotIdx) : plain;
        String decPart = dotIdx >= 0 ? plain.substring(dotIdx + 1) : "00";

        if (intPart.length() > 3) {
            String last3 = intPart.substring(intPart.length() - 3);
            String leading = intPart.substring(0, intPart.length() - 3);
            StringBuilder sb = new StringBuilder();
            int len = leading.length();
            for (int i = 0; i < len; i++) {
                sb.append(leading.charAt(i));
                int digitsAfter = len - 1 - i;
                if (digitsAfter > 0 && digitsAfter % 2 == 0) {
                    sb.append(',');
                }
            }
            intPart = sb.append(',').append(last3).toString();
        }

        String result = intPart + "." + decPart;
        return isNegative ? "-" + result : result;
    }

    public static String formatWithRupee(Object value) {
        if (value == null) {
            return "₹ 0.00";
        }
        BigDecimal bd;
        if (value instanceof BigDecimal b) {
            bd = b;
        } else if (value instanceof Number num) {
            bd = BigDecimal.valueOf(num.doubleValue());
        } else {
            try {
                String clean = value.toString().replaceAll("[^0-9.-]", "");
                bd = clean.isEmpty() ? BigDecimal.ZERO : new BigDecimal(clean);
            } catch (Exception e) {
                bd = BigDecimal.ZERO;
            }
        }
        boolean isNegative = bd.compareTo(BigDecimal.ZERO) < 0;
        String formatted = formatBigDecimal(bd);
        if (isNegative) {
            return "- ₹ " + formatted.substring(1);
        } else {
            return "₹ " + formatted;
        }
    }

    /**
     * Parses a formatted amount string (which may contain '₹', commas, spaces)
     * into a clean BigDecimal for calculations, saving, or database operations.
     */
    public static BigDecimal parseAmount(String input) {
        if (input == null || input.isBlank()) {
            return BigDecimal.ZERO;
        }
        String clean = input.replaceAll("[^0-9.-]", "");
        if (clean.isEmpty() || "-".equals(clean) || ".".equals(clean)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(clean);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}

