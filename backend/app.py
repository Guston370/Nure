"""
Nure Food Recognition API Server
- /predict           : Recognize food from image → return name + nutrition + ingredients
- /store-feedback    : RLHF feedback from human corrections
- /retrain           : Trigger model retraining with accumulated feedback
- /model-stats       : Get RLHF training statistics
- /ocr-detect        : OCR-based text detection fallback
"""

import os
import json
import traceback
from flask import Flask, request, jsonify
from flask_cors import CORS

from food_classifier import FoodClassifier
from nutrition_db import get_nutrition, search_similar
from rlhf_manager import RLHFManager

app = Flask(__name__)
CORS(app)

# Initialize ML model and RLHF manager
print("=" * 60)
print("  NURE Food Recognition Server - Starting...")
print("=" * 60)

classifier = FoodClassifier()
rlhf = RLHFManager(classifier)

UPLOAD_DIR = os.path.join(os.path.dirname(__file__), "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)


@app.route("/predict", methods=["POST"])
def predict():
    """
    Accept a food image, run ML recognition, return food name + nutrition.
    Expects: multipart form with 'image' field.
    Returns: JSON with food_name, confidence, ingredients, nutrition, health info.
    """
    if "image" not in request.files:
        return jsonify({"error": "No image provided"}), 400

    image_file = request.files["image"]
    if image_file.filename == "":
        return jsonify({"error": "Empty filename"}), 400

    # Save uploaded image
    filename = f"predict_{image_file.filename}"
    image_path = os.path.join(UPLOAD_DIR, filename)
    image_file.save(image_path)

    try:
        # Run ML model prediction
        food_name, confidence, top_predictions = classifier.predict(image_path)

        # Get nutrition data for the predicted food
        nutrition_data = get_nutrition(food_name)

        # Get similar foods for the feedback popup
        similar = search_similar(food_name, limit=5)
        display_names = classifier.display_names

        # Build response
        response = {
            "product": display_names.get(food_name, food_name.replace("_", " ").title()),
            "food_key": food_name,
            "confidence": round(confidence, 4),
            "ingredients": nutrition_data.get("ingredients", []),
            "nutrition": nutrition_data.get("nutrition", {}),
            "serving_size": nutrition_data.get("serving_size", "1 serving"),
            "health_score": nutrition_data.get("health_score", 50),
            "health_tags": nutrition_data.get("health_tags", []),
            "is_vegetarian": nutrition_data.get("is_vegetarian", False),
            "is_vegan": nutrition_data.get("is_vegan", False),
            "allergens": nutrition_data.get("allergens", []),
            "similar_products": [display_names.get(s, s.replace("_", " ").title()) for s in similar],
            "top_predictions": top_predictions[:5],
            "model_version": "mobilenet_v2_food",
            "rlhf_enabled": True
        }

        # Determine if confidence is too low → suggest fallback
        if confidence < 0.3:
            response["product"] = "fallback_product"
            response["message"] = "Low confidence - please select the correct food item"

        return jsonify(response)

    except Exception as e:
        traceback.print_exc()
        return jsonify({"error": str(e), "product": "unknown", "confidence": 0}), 500


@app.route("/store-feedback", methods=["POST"])
def store_feedback():
    """
    RLHF endpoint: Store human feedback to improve the model.
    Expects: multipart form with 'image' and 'label' fields.
    Optional: 'predicted_label' for tracking prediction accuracy.
    """
    label = request.form.get("label", "")
    predicted = request.form.get("predicted_label", "")

    if not label:
        return jsonify({"error": "No label provided"}), 400

    image_file = request.files.get("image")
    if image_file is None:
        # Try to use a path instead
        image_path = request.form.get("image_path", "")
        if image_path and os.path.exists(image_path):
            result = rlhf.store_feedback(image_path, predicted, label)
        else:
            return jsonify({"error": "No image provided"}), 400
    else:
        result = rlhf.store_feedback(image_file, predicted, label)

    # Auto-retrain if enough feedback has accumulated
    if result.get("should_retrain"):
        retrain_result = rlhf.retrain_model()
        result["retrain_triggered"] = True
        result["retrain_result"] = retrain_result

    return jsonify(result)


@app.route("/retrain", methods=["POST"])
def retrain():
    """Manually trigger model retraining with accumulated RLHF feedback."""
    result = rlhf.retrain_model()
    return jsonify(result)


@app.route("/model-stats", methods=["GET"])
def model_stats():
    """Get current model performance and RLHF statistics."""
    stats = rlhf.get_stats()
    stats["model"] = "MobileNetV2 (Transfer Learning)"
    stats["num_categories"] = classifier.num_classes
    stats["device"] = str(classifier.device)
    return jsonify(stats)


@app.route("/ocr-detect", methods=["POST"])
def ocr_detect():
    """
    Fallback: Try to identify food from OCR-extracted text.
    Expects: form with 'text' field containing OCR output.
    """
    text = request.form.get("text", "").lower()
    if not text:
        return jsonify({"error": "No text provided"}), 400

    # Try to match against known food categories
    best_match = None
    best_score = 0

    for cat in classifier.categories:
        display = classifier.display_names.get(cat, cat)
        # Simple text matching
        cat_words = cat.replace("_", " ").lower()
        if cat_words in text or display.lower() in text:
            score = len(cat_words) / len(text) if text else 0
            if score > best_score:
                best_score = score
                best_match = cat

    if best_match:
        nutrition_data = get_nutrition(best_match)
        display = classifier.display_names.get(best_match, best_match)
        similar = search_similar(best_match, limit=5)

        return jsonify({
            "product": display,
            "food_key": best_match,
            "confidence": min(0.7, best_score + 0.3),
            "ingredients": nutrition_data.get("ingredients", []),
            "nutrition": nutrition_data.get("nutrition", {}),
            "serving_size": nutrition_data.get("serving_size", "1 serving"),
            "health_score": nutrition_data.get("health_score", 50),
            "health_tags": nutrition_data.get("health_tags", []),
            "similar_products": [classifier.display_names.get(s, s) for s in similar]
        })

    return jsonify({
        "product": "Unknown",
        "confidence": 0.0,
        "similar_products": [classifier.display_names.get(c, c) for c in classifier.categories[:5]]
    })


@app.route("/health", methods=["GET"])
def health_check():
    """Health check endpoint."""
    return jsonify({"status": "ok", "model": "loaded", "categories": classifier.num_classes})


if __name__ == "__main__":
    print("\n🚀 Nure Food Recognition Server running on http://0.0.0.0:5000")
    print(f"📊 Model: MobileNetV2 | Categories: {classifier.num_classes}")
    print(f"🔄 RLHF Feedback entries: {len(rlhf.feedback_data)}\n")
    app.run(host="0.0.0.0", port=5000, debug=True)
