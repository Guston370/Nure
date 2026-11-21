package com.example.healthscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tflite.java.TfLite;

import org.tensorflow.lite.InterpreterApi;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs Nure's optional Fruits-360 classifier on-device through Google Play services LiteRT.
 */
final class ProduceClassifier {

    private static final String TAG = "ProduceClassifier";
    private static final String MODEL_ASSET = "produce_model.tflite";
    private static final String LABELS_ASSET = "produce_labels.txt";
    private static final int IMAGE_SIZE = 100;
    private static final int MAX_RESULTS = 5;

    private final ByteBuffer modelBuffer;
    private final List<String> labels;
    private final TaskCompletionSource<Void> ready = new TaskCompletionSource<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile InterpreterApi interpreter;
    private volatile boolean closed;

    ProduceClassifier(Context context) {
        ByteBuffer loadedModel = null;
        List<String> loadedLabels = Collections.emptyList();
        try {
            loadedModel = readModel(context);
            loadedLabels = readLabels(context);
            if (loadedLabels.isEmpty()) {
                throw new IOException("The produce model has no labels");
            }
        } catch (FileNotFoundException e) {
            Log.i(TAG, "No bundled produce model; using the standard food recognizer");
        } catch (IOException e) {
            Log.e(TAG, "Could not load the bundled produce model", e);
        }

        modelBuffer = loadedModel;
        labels = loadedLabels;
        if (!isAvailable()) {
            return;
        }

        TfLite.initialize(context.getApplicationContext())
                .addOnSuccessListener(unused -> {
                    try {
                        executor.execute(this::createInterpreter);
                    } catch (RuntimeException e) {
                        failInitialization(e);
                    }
                })
                .addOnFailureListener(this::failInitialization);
    }

    boolean isAvailable() {
        return modelBuffer != null && !labels.isEmpty();
    }

    void classify(Bitmap bitmap, Callback callback) {
        if (!isAvailable()) {
            callback.onFailure(new IllegalStateException("Produce model is not bundled"));
            return;
        }
        if (bitmap == null) {
            callback.onFailure(new IllegalArgumentException("Image bitmap is null"));
            return;
        }

        ready.getTask().addOnCompleteListener(task -> {
            if (closed) {
                return;
            }
            if (!task.isSuccessful()) {
                Exception error = task.getException();
                postFailure(callback, error != null
                        ? error : new IllegalStateException("Could not initialize produce model"));
                return;
            }

            try {
                executor.execute(() -> runInference(bitmap, callback));
            } catch (RuntimeException e) {
                postFailure(callback, e);
            }
        });
    }

    private void createInterpreter() {
        if (closed) {
            ready.trySetException(new IllegalStateException("Produce classifier was closed"));
            return;
        }

        InterpreterApi created = null;
        try {
            ByteBuffer model = modelBuffer.asReadOnlyBuffer().order(ByteOrder.nativeOrder());
            created = InterpreterApi.create(model,
                    new InterpreterApi.Options().setRuntime(
                            InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY));
            validateModel(created);
            interpreter = created;
            ready.trySetResult(null);
        } catch (RuntimeException e) {
            if (created != null) {
                created.close();
            }
            failInitialization(e);
        }
    }

    private void validateModel(InterpreterApi model) {
        int[] inputShape = model.getInputTensor(0).shape();
        int[] outputShape = model.getOutputTensor(0).shape();
        if (inputShape.length != 4 || inputShape[0] != 1
                || inputShape[1] != IMAGE_SIZE || inputShape[2] != IMAGE_SIZE
                || inputShape[3] != 3) {
            throw new IllegalStateException("Unexpected produce model input shape");
        }
        if (outputShape.length != 2 || outputShape[0] != 1
                || outputShape[1] != labels.size()) {
            throw new IllegalStateException("Produce model output does not match labels");
        }
    }

    private void runInference(Bitmap bitmap, Callback callback) {
        if (closed) {
            return;
        }

        InterpreterApi current = interpreter;
        if (current == null) {
            postFailure(callback, new IllegalStateException("Produce model is not initialized"));
            return;
        }

        try {
            int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
            int cropLeft = (bitmap.getWidth() - cropSize) / 2;
            int cropTop = (bitmap.getHeight() - cropSize) / 2;
            Bitmap cropped = Bitmap.createBitmap(
                    bitmap, cropLeft, cropTop, cropSize, cropSize);
            Bitmap scaled = Bitmap.createScaledBitmap(cropped, IMAGE_SIZE, IMAGE_SIZE, true);
            int[] pixels = new int[IMAGE_SIZE * IMAGE_SIZE];
            scaled.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);
            if (scaled != bitmap) {
                scaled.recycle();
            }
            if (cropped != bitmap && cropped != scaled) {
                cropped.recycle();
            }

            ByteBuffer input = ByteBuffer.allocateDirect(
                    IMAGE_SIZE * IMAGE_SIZE * 3 * Float.BYTES).order(ByteOrder.nativeOrder());
            for (int pixel : pixels) {
                input.putFloat(Color.red(pixel));
                input.putFloat(Color.green(pixel));
                input.putFloat(Color.blue(pixel));
            }
            input.rewind();

            float[][] scores = new float[1][labels.size()];
            synchronized (current) {
                current.run(input, scores);
            }
            List<Prediction> predictions = topPredictions(scores[0]);
            mainHandler.post(() -> {
                if (!closed) {
                    callback.onSuccess(predictions);
                }
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "Produce model inference failed", e);
            postFailure(callback, e);
        }
    }

    private List<Prediction> topPredictions(float[] scores) {
        List<Prediction> predictions = new ArrayList<>();
        boolean[] selected = new boolean[scores.length];
        for (int rank = 0; rank < Math.min(MAX_RESULTS, scores.length); rank++) {
            int bestIndex = -1;
            float bestScore = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < scores.length; i++) {
                if (!selected[i] && scores[i] > bestScore) {
                    bestIndex = i;
                    bestScore = scores[i];
                }
            }
            if (bestIndex < 0) {
                break;
            }
            selected[bestIndex] = true;
            predictions.add(new Prediction(labels.get(bestIndex), bestScore));
        }
        return predictions;
    }

    private void postFailure(Callback callback, Exception error) {
        Log.e(TAG, "Produce classification unavailable", error);
        mainHandler.post(() -> {
            if (!closed) {
                callback.onFailure(error);
            }
        });
    }

    private void failInitialization(Exception error) {
        Log.e(TAG, "Could not initialize produce model", error);
        ready.trySetException(error);
    }

    private static ByteBuffer readModel(Context context) throws IOException {
        try (InputStream input = context.getAssets().open(MODEL_ASSET);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            byte[] bytes = output.toByteArray();
            ByteBuffer model = ByteBuffer.allocateDirect(bytes.length)
                    .order(ByteOrder.nativeOrder());
            model.put(bytes);
            model.rewind();
            return model;
        }
    }

    private static List<String> readLabels(Context context) throws IOException {
        List<String> labels = new ArrayList<>();
        try (InputStream input = context.getAssets().open(LABELS_ASSET);
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String label = line.trim();
                if (!label.isEmpty()) {
                    labels.add(label);
                }
            }
        }
        return labels;
    }

    void close() {
        closed = true;
        executor.shutdownNow();
        InterpreterApi current = interpreter;
        if (current != null) {
            synchronized (current) {
                current.close();
            }
        }
    }

    interface Callback {
        void onSuccess(List<Prediction> predictions);

        void onFailure(Exception error);
    }

    static final class Prediction {
        final String label;
        final float confidence;

        Prediction(String label, float confidence) {
            this.label = label;
            this.confidence = confidence;
        }
    }
}
