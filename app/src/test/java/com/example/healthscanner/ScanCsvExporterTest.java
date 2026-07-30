package com.example.healthscanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.healthscanner.models.Scan;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Unit tests for CSV export of scan history.
 */
public class ScanCsvExporterTest {

    private static Scan sampleScan() {
        Scan scan = new Scan();
        scan.setProductName("Greek Yogurt");
        scan.setBrand("FreshDairy");
        scan.setBarcode("1234567890123");
        scan.setCategory("dairy");
        scan.setHealthScore(78.4);
        scan.setHealthGrade("B");
        scan.setCalories(130);
        scan.setProtein(18);
        scan.setCarbs(9);
        scan.setFat(3.2);
        scan.setSugar(6.5);
        scan.setSodium(65);
        scan.setFiber(0);
        scan.setFavorite(true);
        scan.setScanDate(new Date(0));
        return scan;
    }

    @Test
    public void emptyHistoryStillProducesHeaderOnlyDocument() {
        String csv = ScanCsvExporter.toCsv(Collections.emptyList());

        assertEquals(ScanCsvExporter.HEADER + "\n", csv);
    }

    @Test
    public void nullHistoryIsTolerated() {
        assertEquals(ScanCsvExporter.HEADER + "\n", ScanCsvExporter.toCsv(null));
    }

    @Test
    public void rowContainsEveryNutritionColumn() {
        String csv = ScanCsvExporter.toCsv(Collections.singletonList(sampleScan()));
        String[] lines = csv.split("\n");

        assertEquals(2, lines.length);
        String[] columns = lines[1].split(",");
        assertEquals(15, columns.length);

        assertEquals("Greek Yogurt", columns[1]);
        assertEquals("FreshDairy", columns[2]);
        assertEquals("1234567890123", columns[3]);
        // Categories are normalised on the way out so the CSV matches the analytics labels.
        assertEquals("Dairy", columns[4]);
        assertEquals("78", columns[5]);
        assertEquals("B", columns[6]);
        assertEquals("130", columns[7]);
        assertEquals("18.0", columns[8]);
        assertEquals("yes", columns[14]);
    }

    @Test
    public void gradeIsDerivedWhenMissing() {
        Scan scan = sampleScan();
        scan.setHealthGrade(null);
        scan.setHealthScore(90);

        String csv = ScanCsvExporter.toCsv(Collections.singletonList(scan));

        assertEquals("A", csv.split("\n")[1].split(",")[6]);
    }

    @Test
    public void fieldsWithCommasAreQuoted() {
        Scan scan = sampleScan();
        scan.setProductName("Cereal, Honey & Nut");

        String csv = ScanCsvExporter.toCsv(Collections.singletonList(scan));

        assertTrue(csv.contains("\"Cereal, Honey & Nut\""));
    }

    @Test
    public void embeddedQuotesAreDoubled() {
        assertEquals("\"He said \"\"hi\"\"\"", ScanCsvExporter.escape("He said \"hi\""));
        assertEquals("plain", ScanCsvExporter.escape("plain"));
        assertEquals("", ScanCsvExporter.escape(null));
        assertTrue(ScanCsvExporter.escape("line\nbreak").startsWith("\""));
    }

    @Test
    public void nullScansInListAreSkipped() {
        List<Scan> scans = new ArrayList<>();
        scans.add(sampleScan());
        scans.add(null);

        String csv = ScanCsvExporter.toCsv(scans);

        assertEquals(2, csv.split("\n").length);
    }

    @Test
    public void suggestedFileNameIsCsvAndTimestamped() {
        String name = ScanCsvExporter.suggestedFileName(0L);

        assertTrue(name.startsWith("nure-scan-history-"));
        assertTrue(name.endsWith(".csv"));
    }
}
