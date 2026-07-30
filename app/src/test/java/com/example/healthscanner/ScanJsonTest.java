package com.example.healthscanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.healthscanner.models.Scan;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Date;

/**
 * Unit tests for the local scan-history JSON format, including backwards compatibility with
 * records written before the full nutrition payload was stored.
 */
public class ScanJsonTest {

    private static final double DELTA = 0.001;

    @Test
    public void roundTripPreservesEveryField() throws Exception {
        Scan original = new Scan();
        original.setScanId("scan_1");
        original.setUserId("user_1");
        original.setProductName("Dark Chocolate Bar 70%");
        original.setBrand("SweetTreats");
        original.setBarcode("9876543210987");
        original.setCategory("Chocolate");
        original.setImageUrl("https://example.test/choc.jpg");
        original.setHealthScore(52.5);
        original.setHealthGrade("D");
        original.setCalories(540);
        original.setProtein(7.8);
        original.setCarbs(61);
        original.setFat(31);
        original.setSugar(24);
        original.setSodium(20);
        original.setFiber(11);
        original.setFavorite(true);
        original.setScanMethod("camera");
        original.setScanDate(new Date(1_700_000_000_000L));

        Scan restored = Scan.fromJson(original.toJson());

        assertNotNull(restored);
        assertEquals("scan_1", restored.getScanId());
        assertEquals("user_1", restored.getUserId());
        assertEquals("Dark Chocolate Bar 70%", restored.getProductName());
        assertEquals("SweetTreats", restored.getBrand());
        assertEquals("9876543210987", restored.getBarcode());
        assertEquals("Chocolate", restored.getCategory());
        assertEquals("https://example.test/choc.jpg", restored.getImageUrl());
        assertEquals(52.5, restored.getHealthScore(), DELTA);
        assertEquals("D", restored.getHealthGrade());
        assertEquals(540, restored.getCalories());
        assertEquals(7.8, restored.getProtein(), DELTA);
        assertEquals(61, restored.getCarbs(), DELTA);
        assertEquals(31, restored.getFat(), DELTA);
        assertEquals(24, restored.getSugar(), DELTA);
        assertEquals(20, restored.getSodium(), DELTA);
        assertEquals(11, restored.getFiber(), DELTA);
        assertTrue(restored.isFavorite());
        assertEquals("camera", restored.getScanMethod());
        assertEquals(1_700_000_000_000L, restored.getScanDate().getTime());
    }

    @Test
    public void legacyNameKeyIsStillWrittenForOlderReaders() throws Exception {
        Scan scan = new Scan();
        scan.setProductName("Fresh Red Apple");

        JSONObject json = scan.toJson();

        assertEquals("Fresh Red Apple", json.getString("productName"));
        assertEquals("Fresh Red Apple", json.getString("name"));
    }

    @Test
    public void legacyRecordsWithOnlyBasicFieldsStillParse() throws Exception {
        // The shape written by the app before the scan store was introduced.
        JSONObject legacy = new JSONObject();
        legacy.put("name", "Organic Whole Grain Cereal");
        legacy.put("brand", "HealthyChoice");
        legacy.put("barcode", "1111111111111");
        legacy.put("calories", 350);
        legacy.put("healthScore", 66.0);
        legacy.put("timestamp", 1_600_000_000_000L);

        Scan scan = Scan.fromJson(legacy);

        assertNotNull(scan);
        assertEquals("Organic Whole Grain Cereal", scan.getProductName());
        assertEquals("HealthyChoice", scan.getBrand());
        assertEquals(350, scan.getCalories());
        assertEquals(66.0, scan.getHealthScore(), DELTA);
        // Missing fields fall back to sensible defaults rather than nulls.
        assertEquals("Other", scan.getCategory());
        assertEquals(0, scan.getProtein(), DELTA);
        assertEquals(1_600_000_000_000L, scan.getScanDate().getTime());
    }

    @Test
    public void legacyRecordsGetAStableGeneratedScanId() throws Exception {
        JSONObject legacy = new JSONObject();
        legacy.put("name", "Anything");
        legacy.put("barcode", "2222222222222");
        legacy.put("timestamp", 1_600_000_000_000L);

        Scan first = Scan.fromJson(legacy);
        Scan second = Scan.fromJson(legacy);

        assertNotNull(first.getScanId());
        assertEquals(first.getScanId(), second.getScanId());
    }

    @Test
    public void missingProductNameFallsBackToPlaceholder() {
        Scan scan = Scan.fromJson(new JSONObject());

        assertNotNull(scan);
        assertEquals("Unknown Product", scan.getProductName());
        assertEquals("Unknown Brand", scan.getBrand());
        assertEquals("", scan.getBarcode());
    }

    @Test
    public void nullJsonYieldsNull() {
        assertNull(Scan.fromJson(null));
    }
}
