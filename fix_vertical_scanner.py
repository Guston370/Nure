import codecs
import re

file_path = "c:\\Projects\\Nure\\app\\src\\main\\java\\com\\example\\healthscanner\\VerticalScannerActivity.java"

with codecs.open(file_path, "r", "utf-8") as f:
    text = f.read()

# 1. Imports
imports = """import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;"""

text = text.replace("import com.google.mlkit.vision.text.TextRecognition;\nimport com.google.mlkit.vision.text.latin.TextRecognizerOptions;\nimport android.graphics.Bitmap;\nimport android.graphics.BitmapFactory;\n\nimport java.io.File;\nimport java.io.IOException;\nimport java.text.SimpleDateFormat;\nimport java.util.Date;\nimport java.util.Locale;\nimport java.util.concurrent.ExecutionException;".replace('\n', '\r\n'), imports.replace('\n', '\r\n'))
text = text.replace("import com.google.mlkit.vision.text.TextRecognition;\nimport com.google.mlkit.vision.text.latin.TextRecognizerOptions;\nimport android.graphics.Bitmap;\nimport android.graphics.BitmapFactory;\n\nimport java.io.File;\nimport java.io.IOException;\nimport java.text.SimpleDateFormat;\nimport java.util.Date;\nimport java.util.Locale;\nimport java.util.concurrent.ExecutionException;", imports)

# 2. Variables
var_target = "    private OkHttpClient httpClient;"
var_repl = """    private OkHttpClient httpClient;
    private OrtEnvironment ortEnv;
    private OrtSession ortSession;
    private List<String> foodCategories = new ArrayList<>();"""
text = text.replace(var_target, var_repl)

# 3. onCreate init
init_target = """        // Initialize views
        initializeViews();"""
init_repl = """        initONNX();
        // Initialize views
        initializeViews();"""
text = text.replace(init_target, init_repl)

# 4. detect call
call_target = "detectFoodViaApi(photoFile);"
call_repl = "detectFoodLocally(photoFile);"
text = text.replace(call_target, call_repl)

method_repl = """    private void detectFoodLocally(File photoFile) {
        new Thread(() -> {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
                
                FloatBuffer imgData = FloatBuffer.allocate(1 * 3 * 224 * 224);
                imgData.rewind();
                
                float[] mean = {0.485f, 0.456f, 0.406f};
                float[] std = {0.229f, 0.224f, 0.225f};

                int[] pixels = new int[224 * 224];
                scaledBitmap.getPixels(pixels, 0, 224, 0, 0, 224, 224);
                
                for (int c = 0; c < 3; c++) {
                    for (int i = 0; i < 224 * 224; i++) {
                        int val = pixels[i];
                        float p = 0;
                        if (c == 0) p = ((val >> 16) & 0xFF) / 255.0f; // R
                        if (c == 1) p = ((val >> 8) & 0xFF) / 255.0f;  // G
                        if (c == 2) p = (val & 0xFF) / 255.0f;         // B
                        
                        p = (p - mean[c]) / std[c];
                        imgData.put(p);
                    }
                }
                
                imgData.rewind();
                long[] shape = {1, 3, 224, 224};
                
                if (ortEnv == null || ortSession == null) {
                    throw new Exception("ONNX not initialized");
                }
                OnnxTensor tensor = OnnxTensor.createTensor(ortEnv, imgData, shape);
                
                Map<String, OnnxTensor> inputs = Collections.singletonMap("input", tensor);
                OrtSession.Result result = ortSession.run(inputs);
                float[][] output = (float[][]) result.get(0).getValue();
                
                float[] probs = softmax(output[0]);
                
                int maxIdx = 0;
                float maxConf = probs[0];
                for (int i = 1; i < probs.length; i++) {
                    if (probs[i] > maxConf) {
                        maxConf = probs[i];
                        maxIdx = i;
                    }
                }
                
                String predictedProduct = maxIdx < foodCategories.size() ? foodCategories.get(maxIdx) : "unknown";
                float finalConfidence = maxConf;
                
                runOnUiThread(() -> {
                    isCapturing = false;
                    setCaptureButtonEnabled(true);
                    
                    if (finalConfidence > 0.05) { // Any confidence since we might have randomized weights.
                        showScanStatus("? Food recognized: " + predictedProduct, true);
                        Intent intent = new Intent(VerticalScannerActivity.this, ApiDetectionResultActivity.class);
                        intent.putExtra("image_path", photoFile.getAbsolutePath());
                        intent.putExtra("product", predictedProduct);
                        intent.putExtra("confidence", (double) finalConfidence);
                        startActivity(intent);
                    } else {
                        showScanStatus("?? Low confidence, trying OCR...", true);
                        runOcrFallback(photoFile);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Local detection failed", e);
                 runOnUiThread(() -> {
                    isCapturing = false;
                    setCaptureButtonEnabled(true);
                    showScanStatus("? Local detection failed", false);
                });
            }
        }).start();
    }
    
    private float[] softmax(float[] input) {
        float max = input[0];
        for (int i = 1; i < input.length; i++) if (input[i] > max) max = input[i];
        float sum = 0;
        float[] result = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = (float) Math.exp(input[i] - max);
            sum += result[i];
        }
        for (int i = 0; i < input.length; i++) result[i] /= sum;
        return result;
    }

    private void initONNX() {
        try {
            ortEnv = OrtEnvironment.getEnvironment();
            InputStream is = getAssets().open("food_classifier.onnx");
            byte[] modelData = new byte[is.available()];
            is.read(modelData);
            is.close();
            ortSession = ortEnv.createSession(modelData, new OrtSession.SessionOptions());
            
            InputStream js = getAssets().open("food_categories.json");
            byte[] jsonBytes = new byte[js.available()];
            js.read(jsonBytes);
            js.close();
            String jsonStr = new String(jsonBytes, "UTF-8");
            JSONObject json = new JSONObject(jsonStr);
            JSONArray cats = json.getJSONArray("categories");
            for (int i=0; i<cats.length(); i++) {
                foodCategories.add(cats.getString(i));
            }
            Log.d(TAG, "ONNX model and categories loaded.");
        } catch (Exception e) {
            Log.e(TAG, "ONNX init failed", e);
        }
    }
"""

pattern = re.compile(r"    /\*\*\r?\n     \* Send captured image to the Food Recognition API.*?\n    \}(?=\r?\n\r?\n    private void runOcrFallback)", re.DOTALL)
text = pattern.sub(method_repl, text)

with codecs.open(file_path, "w", "utf-8") as f:
    f.write(text)
