package com.example.pravin_quotation.service;

import org.springframework.stereotype.Service;

@Service
public class NumberToWordsService {

    private static final String[] ONES = {
            "",
            "One",
            "Two",
            "Three",
            "Four",
            "Five",
            "Six",
            "Seven",
            "Eight",
            "Nine",
            "Ten",
            "Eleven",
            "Twelve",
            "Thirteen",
            "Fourteen",
            "Fifteen",
            "Sixteen",
            "Seventeen",
            "Eighteen",
            "Nineteen"
    };

    private static final String[] TENS = {
            "",
            "",
            "Twenty",
            "Thirty",
            "Forty",
            "Fifty",
            "Sixty",
            "Seventy",
            "Eighty",
            "Ninety"
    };

    public String convert(long number) {

        if (number == 0) {
            return "Zero";
        }

        if (number < 0) {
            return "Minus " + convert(-number);
        }

        StringBuilder words = new StringBuilder();

        if (number >= 10000000) {

            words.append(
                    convert(number / 10000000)
            );

            words.append(" Crore ");

            number %= 10000000;
        }

        if (number >= 100000) {

            words.append(
                    convert(number / 100000)
            );

            words.append(" Lakh ");

            number %= 100000;
        }

        if (number >= 1000) {

            words.append(
                    convert(number / 1000)
            );

            words.append(" Thousand ");

            number %= 1000;
        }

        if (number >= 100) {

            words.append(
                    convert(number / 100)
            );

            words.append(" Hundred ");

            number %= 100;
        }

        if (number > 0) {

            if (number < 20) {

                words.append(
                        ONES[(int) number]
                );

            } else {

                words.append(
                        TENS[(int) (number / 10)]
                );

                if (number % 10 != 0) {

                    words.append(" ");

                    words.append(
                            ONES[(int) (number % 10)]
                    );
                }
            }
        }

        return words.toString().trim();
    }

    public String convertRupees(
            java.math.BigDecimal amount) {

        if (amount == null) {
            return "Rupees Zero Only";
        }

        long rupees = amount
                .longValue();

        int paise = amount
                .remainder(
                        java.math.BigDecimal.ONE
                )
                .movePointRight(2)
                .intValue();

        StringBuilder result =
                new StringBuilder();

        result.append("Rupees ");

        result.append(
                convert(rupees)
        );

        if (paise > 0) {

            result.append(" and ");

            result.append(
                    convert(paise)
            );

            result.append(" Paise");
        }

        result.append(" Only");

        return result.toString();
    }
}