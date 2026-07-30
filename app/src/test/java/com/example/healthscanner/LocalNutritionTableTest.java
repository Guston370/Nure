package com.example.healthscanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.StringReader;

/**
 * Unit tests for the offline nutrition tier, including the CSV quirks of
 * {@code nutrition_dataset.csv} (a quoted column containing commas).
 */
public class LocalNutritionTableTest {

    private static final double DELTA = 0.001;

    /** Same header and quoting style as the real asset. */
    private static final String CSV = "label,image_paths,calories,protein_g,fat_g,carbohydrates_g,fiber_g\n"
            + "apple,\"[\"\"apple/1.jpg\"\", \"\"apple/2.jpg\"\"]\",52,0.3,0.2,13.8,2.4\n"
            + "banana,\"[\"\"banana/1.jpg\"\"]\",89,1.1,0.3,22.8,2.6\n"
            + "sweetcorn,\"[]\",86,3.2,1.2,19.0,2.7\n"
            + "sweetpotato,\"[]\",86,1.6,0.1,20.1,3.0\n"
            + "grapes,\"[]\",69,0.7,0.2,18.1,0.9\n";

    private static LocalNutritionTable table() {
        return LocalNutritionTable.fromCsv(new StringReader(CSV));
    }

    @Test
    public void quotedColumnsContainingCommasAreParsedCorrectly() {
        LocalNutritionTable table = table();

        assertEquals(5, table.size());
        HealthScoreCalculator.Nutrition apple = table.lookup("apple");
        assertNotNull(apple);
        assertEquals(52, apple.calories, DELTA);
        assertEquals(0.3, apple.protein, DELTA);
        assertEquals(0.2, apple.fat, DELTA);
        assertEquals(13.8, apple.carbs, DELTA);
        assertEquals(2.4, apple.fiber, DELTA);
    }

    @Test
    public void datasetHasNoSugarOrSodiumColumnsSoTheyStayZero() {
        HealthScoreCalculator.Nutrition apple = table().lookup("apple");

        assertEquals(0, apple.sugar, DELTA);
        assertEquals(0, apple.sodium, DELTA);
    }

    @Test
    public void lookupIsCaseAndSeparatorInsensitive() {
        LocalNutritionTable table = table();

        assertNotNull(table.lookup("Apple"));
        assertNotNull(table.lookup("  APPLE  "));
        assertNotNull(table.lookup("sweet_corn".replace('_', ' ').replace(" ", "")));
    }

    @Test
    public void pluralAndSingularVariantsResolve() {
        LocalNutritionTable table = table();

        // Recogniser says "Apples", dataset says "apple".
        assertNotNull(table.lookup("apples"));
        // Recogniser says "Grape", dataset says "grapes".
        assertNotNull(table.lookup("grape"));
    }

    @Test
    public void containmentMatchPrefersTheLongerDatasetLabel() {
        LocalNutritionTable table = table();

        // "sweet" is contained by both sweetcorn and sweetpotato; the longer label wins so
        // the match is at least deterministic.
        assertEquals("sweetpotato", table.matchedLabel("sweet"));
    }

    @Test
    public void descriptiveNamesFindTheUnderlyingFood() {
        // "green apple" is not in the dataset but contains "apple".
        assertNotNull(table().lookup("green apple"));
    }

    @Test
    public void unknownFoodsReturnNull() {
        LocalNutritionTable table = table();

        assertNull(table.lookup("paneer butter masala"));
        assertNull(table.lookup(""));
        assertNull(table.lookup(null));
        assertNull(table.matchedLabel("paneer butter masala"));
    }

    @Test
    public void malformedRowsAreSkippedWithoutLosingGoodRows() {
        String csv = "label,image_paths,calories,protein_g,fat_g,carbohydrates_g,fiber_g\n"
                + "apple,\"[]\",52,0.3,0.2,13.8,2.4\n"
                + "broken,\"[]\",not-a-number,0.3,0.2,13.8,2.4\n"
                + "short,\"[]\",10\n"
                + ",\"[]\",1,1,1,1,1\n"
                + "\n"
                + "banana,\"[]\",89,1.1,0.3,22.8,2.6\n";

        LocalNutritionTable table = LocalNutritionTable.fromCsv(new StringReader(csv));

        assertEquals(2, table.size());
        assertNotNull(table.lookup("apple"));
        assertNotNull(table.lookup("banana"));
        assertNull(table.lookup("broken"));
    }

    @Test
    public void nullReaderYieldsEmptyTable() {
        LocalNutritionTable table = LocalNutritionTable.fromCsv(null);

        assertEquals(0, table.size());
        assertNull(table.lookup("apple"));
    }

    @Test
    public void headerOnlyCsvYieldsEmptyTable() {
        LocalNutritionTable table = LocalNutritionTable.fromCsv(
                new StringReader("label,image_paths,calories,protein_g,fat_g,carbohydrates_g,fiber_g\n"));

        assertEquals(0, table.size());
    }

    @Test
    public void csvLineSplitterHonoursQuotesAndEscapedQuotes() {
        String[] fields = LocalNutritionTable.parseCsvLine("a,\"b,c\",\"say \"\"hi\"\"\",d");

        assertEquals(4, fields.length);
        assertEquals("a", fields[0]);
        assertEquals("b,c", fields[1]);
        assertEquals("say \"hi\"", fields[2]);
        assertEquals("d", fields[3]);
    }

    @Test
    public void labelsAreExposedForDebugging() {
        assertTrue(table().labels().contains("apple"));
    }

    @Test
    public void localHitsProduceAUsableHealthScore() {
        // An apple should score well through the shared calculator: low calories, low fat,
        // low sodium, some fibre.
        double score = HealthScoreCalculator.calculate(table().lookup("apple"));

        assertTrue("apple scored " + score, score >= 70);
    }
}
