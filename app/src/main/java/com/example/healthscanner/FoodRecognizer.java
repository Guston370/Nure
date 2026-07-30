package com.example.healthscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Names the food in a photo.
 *
 * <p>Uses ML Kit's on-device general image labeller, which ships a trained model and needs no
 * network, API key or bundled weights. Its raw labels are mapped onto searchable food names by
 * {@link FoodLabelMapper}; naming is all this class does, nutrition is
 * {@link NutritionRepository}'s job.</p>
 *
 * <p>This replaces the previous bundled 28 MB {@code food_classifier.onnx}, which was an
 * untrained network whose 100-class output was compared against label indices in the
 * 924-963 range and therefore never produced a match.</p>
 *
 * <p>When labelling can't name the food, OCR is used as a second pass: packaged food usually
 * has its name printed on the wrapper, and that text makes a perfectly good search term.</p>
 */
public class FoodRecognizer {

    private static final String TAG = "FoodRecognizer";

    private static final String LABEL_MAP_ASSET = "food_labels.json";

    /** Maximum candidates offered to the user for correction. */
    private static final int MAX_CANDIDATES = 5;

    /** OCR text longer than this is probably a full ingredients panel, not a product name. */
    private static final int MAX_OCR_TERM_LENGTH = 40;

    private final ImageLabeler labeler;
    private final FoodLabelMapper labelMapper;
    private final com.google.mlkit.vision.text.TextRecognizer textRecognizer;

    public FoodRecognizer(Context context) {
        // The confidence floor is applied in FoodLabelMapper so a weak-but-specific label
        // still reaches us; ML Kit's own default would discard it first.
        ImageLabelerOptions options = new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(FoodLabelMapper.MIN_CONFIDENCE)
                .build();
        this.labeler = ImageLabeling.getClient(options);
        this.textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        this.labelMapper = loadLabelMapper(context);
    }

    private static FoodLabelMapper loadLabelMapper(Context context) {
        try (InputStream stream = context.getAssets().open(LABEL_MAP_ASSET)) {
            byte[] buffer = new byte[stream.available()];
            int read = stream.read(buffer);
            if (read <= 0) {
                return FoodLabelMapper.empty();
            }
            return FoodLabelMapper.fromJson(new String(buffer, 0, read, StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e(TAG, "Could not read " + LABEL_MAP_ASSET + ": " + e.getMessage(), e);
            return FoodLabelMapper.empty();
        }
    }

    /**
     * How the food name was arrived at, so the UI can be honest about it.
     */
    public enum Method {
        /** ML Kit named a specific food. */
        IMAGE_LABEL,
        /** The name was read off the packaging. */
        OCR_TEXT,
        /** Nothing could be identified. */
        NONE
    }

    /**
     * A recognition result: the best food name plus the runners-up for correction.
     */
    public static class Recognition {
        public final String foodName;
        public final float confidence;
        public final Method method;
        /** Alternatives the user can pick instead, best first, excluding {@link #foodName}. */
        public final List<String> alternatives;
        /** True when the photo looked like food even if nothing specific was named. */
        public final boolean looksLikeFood;

        Recognition(String foodName, float confidence, Method method,
                List<String> alternatives, boolean looksLikeFood) {
            this.foodName = foodName;
            this.confidence = confidence;
            this.method = method;
            this.alternatives = alternatives;
            this.looksLikeFood = looksLikeFood;
        }

        public boolean isIdentified() {
            return method != Method.NONE && foodName != null && !foodName.isEmpty();
        }

        static Recognition none(boolean looksLikeFood, List<String> alternatives) {
            return new Recognition(null, 0f, Method.NONE, alternatives, looksLikeFood);
        }
    }

    public interface Callback {
        void onResult(Recognition recognition);
    }

    /**
     * Recognise the food in a bitmap. The callback runs on the main thread (ML Kit posts its
     * listeners there).
     */
    public void recognize(Bitmap bitmap, Callback callback) {
        if (bitmap == null) {
            callback.onResult(Recognition.none(false, new ArrayList<>()));
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        labeler.process(image)
                .addOnSuccessListener(labels -> onLabelsReady(labels, image, callback))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Image labelling failed, trying OCR: " + e.getMessage());
                    runOcr(image, false, new ArrayList<>(), callback);
                });
    }

    private void onLabelsReady(List<ImageLabel> labels, InputImage image, Callback callback) {
        List<String> texts = new ArrayList<>();
        List<Float> confidences = new ArrayList<>();
        for (ImageLabel label : labels) {
            texts.add(label.getText());
            confidences.add(label.getConfidence());
        }

        FoodLabelMapper.Outcome outcome = labelMapper.rank(texts, confidences, MAX_CANDIDATES);
        Log.d(TAG, "ML Kit returned " + labels.size() + " labels, "
                + outcome.candidates.size() + " usable, looksLikeFood=" + outcome.looksLikeFood);

        List<String> alternatives = new ArrayList<>();
        for (int i = 1; i < outcome.candidates.size(); i++) {
            alternatives.add(outcome.candidates.get(i).foodName);
        }

        if (outcome.hasIdentifiedFood()) {
            FoodLabelMapper.Candidate best = outcome.candidates.get(0);
            callback.onResult(new Recognition(
                    best.foodName, best.confidence, Method.IMAGE_LABEL, alternatives, true));
            return;
        }

        // Nothing specific was named. Packaged food usually has its name printed on it, so
        // read the label before giving up.
        //
        // Unmapped image labels are deliberately NOT used as search terms. They are things
        // like "Plant", "Tableware" or "Yellow", and feeding them to a product search
        // returns an arbitrary branded item whose nutrition then gets attached to the photo.
        // They are only kept as suggestions the user can pick from.
        runOcr(image, outcome.looksLikeFood, new ArrayList<>(), callback);
    }

    private void runOcr(InputImage image, boolean looksLikeFood, List<String> fallbacks,
            Callback callback) {
        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String term = bestOcrTerm(visionText.getText());
                    if (term != null) {
                        Log.d(TAG, "OCR produced search term: " + term);
                        List<String> alternatives = new ArrayList<>(fallbacks);
                        callback.onResult(new Recognition(
                                term, 0.5f, Method.OCR_TEXT, alternatives, looksLikeFood));
                        return;
                    }
                    finishWithFallbacks(looksLikeFood, fallbacks, callback);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "OCR failed: " + e.getMessage());
                    finishWithFallbacks(looksLikeFood, fallbacks, callback);
                });
    }

    /**
     * Report failure to identify the food, carrying any suggestions forward.
     *
     * <p>Reporting "not identified" is the honest outcome here: the caller prompts the user
     * for the name, which is far better than searching a meaningless term and presenting
     * whatever nutrition comes back as fact.</p>
     */
    private void finishWithFallbacks(boolean looksLikeFood, List<String> fallbacks,
            Callback callback) {
        callback.onResult(Recognition.none(looksLikeFood, new ArrayList<>(fallbacks)));
    }

    /**
     * Reduce an OCR block to a plausible product name.
     *
     * <p>Picks the longest mostly-alphabetic line, which on packaging is almost always the
     * brand or product name rather than legal text or a nutrition table.</p>
     */
    static String bestOcrTerm(String ocrText) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            return null;
        }

        String best = null;
        for (String rawLine : ocrText.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.length() < 3 || line.length() > MAX_OCR_TERM_LENGTH) {
                continue;
            }

            int letters = 0;
            for (int i = 0; i < line.length(); i++) {
                if (Character.isLetter(line.charAt(i))) {
                    letters++;
                }
            }
            // Skip nutrition tables, weights and barcodes: mostly digits and symbols.
            if (letters < line.length() * 0.6) {
                continue;
            }

            if (best == null || line.length() > best.length()) {
                best = line;
            }
        }

        return best == null ? null : best.toLowerCase(Locale.US);
    }

    /** Release ML Kit resources. Call from the host activity's {@code onDestroy}. */
    public void close() {
        try {
            labeler.close();
        } catch (Exception e) {
            Log.w(TAG, "Error closing labeler: " + e.getMessage());
        }
        try {
            textRecognizer.close();
        } catch (Exception e) {
            Log.w(TAG, "Error closing text recognizer: " + e.getMessage());
        }
    }
}
