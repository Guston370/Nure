import os
import json
import requests
import numpy as np
import tensorflow as tf
from fastapi import FastAPI, File, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
import io

# Load configurations
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(BASE_DIR, 'data')

with open(os.path.join(DATA_DIR, 'class_names.json'), 'r') as f:
    CLASS_NAMES = json.load(f)

with open(os.path.join(DATA_DIR, 'food_map.json'), 'r') as f:
    FOOD_MAP = json.load(f)

app = FastAPI()
app.add_middleware(CORSMiddleware,
    allow_origins=["*"], allow_methods=["*"])

MODEL_PATH = os.path.join(BASE_DIR, 'ml', 'food_classifier_savedmodel')
model = None

USDA_KEY = "gqqSgdnuyNOB7p4MRoLaFD7JDrTfe4ikWSjIEUw1"
BASE_URL = "https://api.nal.usda.gov/fdc/v1"

@app.on_event("startup")
async def load_model():
    global model
    try:
        model = tf.keras.models.load_model(MODEL_PATH)
        print("Model loaded successfully.")
    except Exception as e:
        print(f"Warning: Model not found at {MODEL_PATH}. Error: {e}")

def get_nutrition(food_label: str) -> dict:
    query = FOOD_MAP.get(food_label, food_label)
    
    # Step 1: search for food
    r = requests.get(f"{BASE_URL}/foods/search", params={
        "query": query,
        "pageSize": 1,
        "dataType": "Survey (FNDDS)",
        "api_key": USDA_KEY
    })
    
    try:
        foods = r.json().get("foods", [])
        if not foods:
            return {}
            
        fdc_id = foods[0]["fdcId"]
        
        # Step 2: get full nutrients
        r2 = requests.get(f"{BASE_URL}/food/{fdc_id}",
            params={"api_key": USDA_KEY})
        food = r2.json()
        
        nutrients = {}
        for n in food.get("foodNutrients", []):
            if "nutrient" in n:
                name = n["nutrient"].get("name", "")
                val = n.get("amount", 0)
                unit = n["nutrient"].get("unitName", "")
                nutrients[name] = {"value": val, "unit": unit}
                
        return {
            "food": query,
            "calories": nutrients.get("Energy", {}).get("value"),
            "protein_g": nutrients.get("Protein", {}).get("value"),
            "carbs_g": nutrients.get("Carbohydrate, by difference", {}).get("value"),
            "fat_g": nutrients.get("Total lipid (fat)", {}).get("value"),
            "fiber_g": nutrients.get("Fiber, total dietary", {}).get("value"),
            "sugar_g": nutrients.get("Sugars, Total", {}).get("value"),
        }
    except Exception as e:
        return {"error": str(e)}

@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    if model is None:
        return {"error": "Model missing"}
    
    img_bytes = await file.read()
    img = Image.open(io.BytesIO(img_bytes)).convert("RGB")
    img = img.resize((224, 224))
    arr = np.array(img) / 255.0
    arr = np.expand_dims(arr, 0) # shape (1, 224, 224, 3)
    
    preds = model.predict(arr)[0]
    top3 = np.argsort(preds)[-3:][::-1]
    
    food_label = CLASS_NAMES[top3[0]]
    confidence = float(preds[top3[0]])
    nutrition = get_nutrition(food_label) # from step 6
    
    return {
        "food": food_label,
        "confidence": round(confidence * 100, 1),
        "top3": [
            {"food": CLASS_NAMES[i], "score": round(float(preds[i])*100,1)}
            for i in top3
        ],
        "nutrition_per_100g": nutrition
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
