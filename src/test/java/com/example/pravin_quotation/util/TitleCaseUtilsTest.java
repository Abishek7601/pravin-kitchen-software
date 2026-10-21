package com.example.pravin_quotation.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TitleCaseUtilsTest {

    @Test
    void testNullAndEmpty() {
        assertNull(TitleCaseUtils.toTitleCase(null));
        assertEquals("", TitleCaseUtils.toTitleCase(""));
        assertEquals("   ", TitleCaseUtils.toTitleCase("   "));
    }

    @Test
    void testBasicNames() {
        assertEquals("Abishek Kumar", TitleCaseUtils.toTitleCase("abishek kumar"));
        assertEquals("Pravin Kitchens", TitleCaseUtils.toTitleCase("pravin kitchens"));
        assertEquals("Kanyakumari", TitleCaseUtils.toTitleCase("kanyakumari"));
    }

    @Test
    void testAddressesAndPunctuation() {
        assertEquals("12, Cross Street, Anna Nagar - 629001",
                TitleCaseUtils.toTitleCase("12, cross street, anna nagar - 629001"));
        assertEquals("Flat No. 4/A, 2nd Floor, Gandhi Road",
                TitleCaseUtils.toTitleCase("flat no. 4/a, 2nd floor, gandhi road"));
        assertEquals("#15, Lake View (Opp. Park)",
                TitleCaseUtils.toTitleCase("#15, lake view (opp. park)"));
    }

    @Test
    void testPreserveInternalCasing() {
        // Do not force every letter to uppercase or lowercase
        assertEquals("McDonald", TitleCaseUtils.toTitleCase("McDonald"));
        assertEquals("Pravin KITCHENS", TitleCaseUtils.toTitleCase("Pravin KITCHENS"));
        assertEquals("Abishek Kumar", TitleCaseUtils.toTitleCase("Abishek Kumar"));
    }

    @Test
    void testMultilineAddresses() {
        String input = "10 north street\nkanyakumari\ntamil nadu";
        String expected = "10 North Street\nKanyakumari\nTamil Nadu";
        assertEquals(expected, TitleCaseUtils.toTitleCase(input));
    }
}
