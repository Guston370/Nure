package com.example.healthscanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for reducing an OCR block to a usable product-name search term.
 *
 * <p>This is the second recognition pass: when image labelling can't name the food, packaged
 * products usually have the name printed on the wrapper.</p>
 */
public class FoodRecognizerOcrTest {

    @Test
    public void emptyInputYieldsNoTerm() {
        assertNull(FoodRecognizer.bestOcrTerm(null));
        assertNull(FoodRecognizer.bestOcrTerm(""));
        assertNull(FoodRecognizer.bestOcrTerm("   \n  \n "));
    }

    @Test
    public void longestAlphabeticLineIsChosen() {
        String ocr = "NET WT 52g\nLay's\nIndian Magic Masala\n8901491100274";

        assertEquals("indian magic masala", FoodRecognizer.bestOcrTerm(ocr));
    }

    @Test
    public void nutritionTablesAndBarcodesAreIgnored() {
        // These lines are mostly digits and symbols, so none of them qualify.
        String ocr = "8901491100274\n52 g\n1234 5678\n230 kcal / 100 g";

        assertNull(FoodRecognizer.bestOcrTerm(ocr));
    }

    @Test
    public void veryShortLinesAreIgnored() {
        assertNull(FoodRecognizer.bestOcrTerm("a\nbc\n x "));
    }

    @Test
    public void veryLongLinesAreIgnoredAsLegalText() {
        String legal = "This product is manufactured in a facility that also processes nuts and dairy";

        assertNull(FoodRecognizer.bestOcrTerm(legal));
    }

    @Test
    public void resultIsLowerCasedForSearching() {
        assertEquals("greek yogurt", FoodRecognizer.bestOcrTerm("GREEK YOGURT"));
    }

    @Test
    public void mixedContentPicksTheProductNameOverTheWeight() {
        String ocr = "500g\nOrganic Whole Grain Cereal\nBest before 01/2027";

        assertEquals("organic whole grain cereal", FoodRecognizer.bestOcrTerm(ocr));
    }

    @Test
    public void windowsLineEndingsAreHandled() {
        assertEquals("dark chocolate", FoodRecognizer.bestOcrTerm("70%\r\nDark Chocolate\r\n100g"));
    }
}
