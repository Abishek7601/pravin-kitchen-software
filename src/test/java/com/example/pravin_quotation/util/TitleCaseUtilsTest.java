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
    void testMaterialBrandSpecificationCapitalization() {
        // Requirement 2: Brand / Specification – First Letter Capital
        assertEquals("Century Club Prime 16mm", TitleCaseUtils.toTitleCase("century club prime 16mm"));
        assertEquals("Marine Grade Plywood", TitleCaseUtils.toTitleCase("marine grade plywood"));
        assertEquals("Asian Paints Royal Finish", TitleCaseUtils.toTitleCase("asian paints royal finish"));

        // Preserve numbers, special characters, and line breaks
        String multiline = "century club prime 16mm\nmarine grade plywood\nasian paints royal finish";
        String expectedMultiline = "Century Club Prime 16mm\nMarine Grade Plywood\nAsian Paints Royal Finish";
        assertEquals(expectedMultiline, TitleCaseUtils.toTitleCase(multiline));

        String complexSpec = "is:710 marine grade, 16mm bwp (grade-a) + laminate finish";
        assertEquals("Is:710 Marine Grade, 16mm Bwp (Grade-A) + Laminate Finish", TitleCaseUtils.toTitleCase(complexSpec));

        String existingAcronyms = "IS:710 Marine Grade, 16mm BWP Plywood";
        assertEquals("IS:710 Marine Grade, 16mm BWP Plywood", TitleCaseUtils.toTitleCase(existingAcronyms));
    }
}
