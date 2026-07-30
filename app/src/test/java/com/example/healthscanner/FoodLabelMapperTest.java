package com.example.healthscanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for the ranking of ML Kit image labels into searchable food names.
 */
public class FoodLabelMapperTest {

    private static final String LABEL_JSON = "{"
            + "\"specific\": {\"Pizza\": \"pizza\", \"Apple\": \"apple\", "
            + "\"Corn\": \"sweetcorn\", \"Frozen dessert\": \"ice cream\", "
            + "\"Ice cream\": \"ice cream\"},"
            + "\"generic\": [\"Food\", \"Dessert\", \"Tableware\"]"
            + "}";

    private FoodLabelMapper mapper;

    @Before
    public void setUp() {
        mapper = FoodLabelMapper.fromJson(LABEL_JSON);
    }

    @Test
    public void labelKindsAreClassifiedFromTheMap() {
        assertEquals(FoodLabelMapper.Kind.SPECIFIC, mapper.kindOf("Pizza"));
        assertEquals(FoodLabelMapper.Kind.GENERIC, mapper.kindOf("Food"));
        assertEquals(FoodLabelMapper.Kind.UNKNOWN, mapper.kindOf("Bicycle"));
    }

    @Test
    public void matchingIsCaseAndWhitespaceInsensitive() {
        assertEquals(FoodLabelMapper.Kind.SPECIFIC, mapper.kindOf("  pIZZa "));
        assertEquals("pizza", mapper.canonicalNameFor("PIZZA"));
    }

    @Test
    public void labelsAreMappedOntoDatasetNames() {
        // ML Kit says "Corn", the nutrition dataset calls it "sweetcorn".
        assertEquals("sweetcorn", mapper.canonicalNameFor("Corn"));
    }

    @Test
    public void unmappedLabelsPassThroughUnchanged() {
        assertEquals("Bicycle", mapper.canonicalNameFor("Bicycle"));
    }

    @Test
    public void malformedOrMissingJsonDegradesToEmpty() {
        assertEquals(FoodLabelMapper.Kind.UNKNOWN, FoodLabelMapper.fromJson(null).kindOf("Pizza"));
        assertEquals(FoodLabelMapper.Kind.UNKNOWN, FoodLabelMapper.fromJson("").kindOf("Pizza"));
        assertEquals(FoodLabelMapper.Kind.UNKNOWN, FoodLabelMapper.fromJson("not json").kindOf("Pizza"));
    }

    @Test
    public void specificLabelsOutrankHigherConfidenceUnknownLabels() {
        // This is the whole point of the ranking: a weak "Pizza" beats a strong "Tableware".
        FoodLabelMapper.Outcome outcome = mapper.rank(
                Arrays.asList("Bicycle", "Pizza"),
                Arrays.asList(0.95f, 0.40f),
                5);

        assertTrue(outcome.hasIdentifiedFood());
        assertEquals("pizza", outcome.candidates.get(0).foodName);
        assertEquals("Bicycle", outcome.candidates.get(1).rawLabel);
    }

    @Test
    public void genericLabelsSetTheFoodFlagButAreNotCandidates() {
        FoodLabelMapper.Outcome outcome = mapper.rank(
                Collections.singletonList("Food"),
                Collections.singletonList(0.9f),
                5);

        assertTrue(outcome.looksLikeFood);
        assertTrue(outcome.candidates.isEmpty());
        assertFalse(outcome.hasIdentifiedFood());
    }

    @Test
    public void lowConfidenceLabelsAreDropped() {
        FoodLabelMapper.Outcome outcome = mapper.rank(
                Arrays.asList("Pizza", "Apple"),
                Arrays.asList(0.10f, 0.80f),
                5);

        assertEquals(1, outcome.candidates.size());
        assertEquals("apple", outcome.candidates.get(0).foodName);
    }

    @Test
    public void labelsCollapsingOntoTheSameFoodAreDeduplicated() {
        FoodLabelMapper.Outcome outcome = mapper.rank(
                Arrays.asList("Ice cream", "Frozen dessert"),
                Arrays.asList(0.9f, 0.8f),
                5);

        assertEquals(1, outcome.candidates.size());
        assertEquals("ice cream", outcome.candidates.get(0).foodName);
    }

    @Test
    public void candidateListRespectsTheLimit() {
        FoodLabelMapper.Outcome outcome = mapper.rank(
                Arrays.asList("Pizza", "Apple", "Corn"),
                Arrays.asList(0.9f, 0.8f, 0.7f),
                2);

        assertEquals(2, outcome.candidates.size());
    }

    @Test
    public void higherConfidenceWinsWithinTheSameKind() {
        FoodLabelMapper.Outcome outcome = mapper.rank(
                Arrays.asList("Apple", "Pizza"),
                Arrays.asList(0.55f, 0.85f),
                5);

        assertEquals("pizza", outcome.candidates.get(0).foodName);
        assertEquals("apple", outcome.candidates.get(1).foodName);
    }

    @Test
    public void emptyAndMismatchedInputIsTolerated() {
        assertTrue(mapper.rank(null, null, 5).candidates.isEmpty());
        assertTrue(mapper.rank(new ArrayList<>(), new ArrayList<>(), 5).candidates.isEmpty());
        // More labels than confidences: only the paired prefix is used.
        List<String> labels = Arrays.asList("Pizza", "Apple");
        List<Float> confidences = Collections.singletonList(0.9f);
        assertEquals(1, mapper.rank(labels, confidences, 5).candidates.size());
    }

    @Test
    public void nullAndBlankLabelsAreSkipped() {
        FoodLabelMapper.Outcome outcome = mapper.rank(
                Arrays.asList(null, "   ", "Pizza"),
                Arrays.asList(0.9f, 0.9f, 0.9f),
                5);

        assertEquals(1, outcome.candidates.size());
    }

    @Test
    public void displayCaseTitleCasesFoodNames() {
        assertEquals("French Fries", FoodLabelMapper.toDisplayCase("french fries"));
        assertEquals("Gulab Jamun", FoodLabelMapper.toDisplayCase("gulab_jamun"));
        assertEquals("Pizza", FoodLabelMapper.toDisplayCase("PIZZA"));
        assertEquals("", FoodLabelMapper.toDisplayCase(null));
        assertEquals("", FoodLabelMapper.toDisplayCase("   "));
    }

    @Test
    public void candidateDisplayNameIsTitleCased() {
        FoodLabelMapper.Outcome outcome = mapper.rank(
                Collections.singletonList("Corn"),
                Collections.singletonList(0.9f),
                5);

        assertEquals("Sweetcorn", outcome.candidates.get(0).displayName());
    }
}
