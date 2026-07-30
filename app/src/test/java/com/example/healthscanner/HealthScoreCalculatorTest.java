package com.example.healthscanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the shared health score model.
 */
public class HealthScoreCalculatorTest {

    private static final double DELTA = 0.001;

    @Test
    public void nullNutritionScoresZero() {
        assertEquals(0, HealthScoreCalculator.calculate(null), DELTA);
    }

    @Test
    public void idealProductScoresFullMarks() {
        // Low calories/sugar/fat/sodium, high protein/fiber: every bucket maxes out.
        HealthScoreCalculator.Nutrition ideal =
                new HealthScoreCalculator.Nutrition(90, 25, 1, 2, 10, 12, 50);

        assertEquals(100, HealthScoreCalculator.calculate(ideal), DELTA);
        assertEquals("A", HealthScoreCalculator.gradeFor(HealthScoreCalculator.calculate(ideal)));
    }

    @Test
    public void worstCaseProductScoresZero() {
        HealthScoreCalculator.Nutrition worst =
                new HealthScoreCalculator.Nutrition(900, 0, 60, 45, 80, 0, 2500);

        assertEquals(0, HealthScoreCalculator.calculate(worst), DELTA);
        assertEquals("E", HealthScoreCalculator.gradeFor(0));
    }

    @Test
    public void scoreIsClampedToOneHundred() {
        HealthScoreCalculator.Nutrition ideal =
                new HealthScoreCalculator.Nutrition(0, 100, 0, 0, 0, 100, 0);

        assertTrue(HealthScoreCalculator.calculate(ideal) <= 100);
    }

    @Test
    public void bucketBoundariesAwardTheHigherBand() {
        // Boundaries are inclusive on the "better" side.
        assertEquals(20, HealthScoreCalculator.scoreCalories(100));
        assertEquals(16, HealthScoreCalculator.scoreCalories(100.1));
        assertEquals(20, HealthScoreCalculator.scoreSugar(2));
        assertEquals(16, HealthScoreCalculator.scoreSugar(2.5));
        assertEquals(15, HealthScoreCalculator.scoreFat(3));
        assertEquals(15, HealthScoreCalculator.scoreProtein(20));
        assertEquals(15, HealthScoreCalculator.scoreFiber(10));
        assertEquals(15, HealthScoreCalculator.scoreSodium(100));
    }

    @Test
    public void moderateProductLandsInMiddleGrades() {
        // 300 kcal (12) + 8g sugar (12) + 9g fat (12) + 6g protein (6)
        // + 2g fiber (6) + 400mg sodium (9) = 57
        HealthScoreCalculator.Nutrition moderate =
                new HealthScoreCalculator.Nutrition(300, 6, 8, 9, 40, 2, 400);

        assertEquals(57, HealthScoreCalculator.calculate(moderate), DELTA);
        assertEquals("C", HealthScoreCalculator.gradeFor(57));
    }

    @Test
    public void gradeBoundariesMatchScoreBands() {
        assertEquals("A", HealthScoreCalculator.gradeFor(85));
        assertEquals("B", HealthScoreCalculator.gradeFor(84));
        assertEquals("B", HealthScoreCalculator.gradeFor(70));
        assertEquals("C", HealthScoreCalculator.gradeFor(69));
        assertEquals("D", HealthScoreCalculator.gradeFor(40));
        assertEquals("E", HealthScoreCalculator.gradeFor(39));
    }

    @Test
    public void everyGradeHasAnEmoji() {
        for (double score = 0; score <= 100; score += 5) {
            assertTrue("missing emoji for score " + score,
                    HealthScoreCalculator.emojiFor(score).length() > 0);
        }
    }
}
