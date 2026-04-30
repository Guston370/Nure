"""
Nutrition Database for Food Recognition
Maps food items to their nutritional info and ingredients.
"""

NUTRITION_DATA = {
    "idli": {
        "serving_size": "2 pieces (100g)",
        "ingredients": ["Rice", "Urad Dal", "Salt", "Water"],
        "nutrition": {"calories": "130 kcal", "protein": "4g", "total_fat": "0.5g", "carbohydrates": "26g", "fiber": "1.5g", "sugar": "0.5g", "sodium": "280mg"},
        "health_score": 82, "health_tags": ["Low Fat", "High Carb"], "is_vegetarian": True, "is_vegan": True, "allergens": []
    },
    "dosa": {
        "serving_size": "1 piece (120g)",
        "ingredients": ["Rice Batter", "Urad Dal", "Oil", "Salt"],
        "nutrition": {"calories": "168 kcal", "protein": "4g", "total_fat": "5g", "carbohydrates": "27g", "fiber": "1g", "sugar": "1g", "sodium": "300mg"},
        "health_score": 70, "health_tags": ["Fermented", "Moderate Fat"], "is_vegetarian": True, "is_vegan": True, "allergens": []
    },
    "masala_dosa": {
        "serving_size": "1 piece (180g)",
        "ingredients": ["Rice Batter", "Urad Dal", "Potato", "Onion", "Mustard Seeds", "Turmeric", "Oil"],
        "nutrition": {"calories": "250 kcal", "protein": "6g", "total_fat": "9g", "carbohydrates": "38g", "fiber": "3g", "sugar": "2g", "sodium": "450mg"},
        "health_score": 65, "health_tags": ["Fermented", "Moderate Fat"], "is_vegetarian": True, "is_vegan": True, "allergens": []
    },
    "biryani": {
        "serving_size": "1 plate (300g)",
        "ingredients": ["Basmati Rice", "Spices", "Onion", "Yogurt", "Ghee", "Saffron", "Mint"],
        "nutrition": {"calories": "350 kcal", "protein": "10g", "total_fat": "12g", "carbohydrates": "52g", "fiber": "2g", "sugar": "3g", "sodium": "680mg"},
        "health_score": 60, "health_tags": ["High Carb", "Aromatic Spices"], "is_vegetarian": True, "is_vegan": False, "allergens": ["Dairy"]
    },
    "chicken_biryani": {
        "serving_size": "1 plate (350g)",
        "ingredients": ["Basmati Rice", "Chicken", "Onion", "Yogurt", "Ghee", "Saffron", "Spices", "Mint"],
        "nutrition": {"calories": "450 kcal", "protein": "28g", "total_fat": "15g", "carbohydrates": "48g", "fiber": "2g", "sugar": "3g", "sodium": "750mg"},
        "health_score": 62, "health_tags": ["High Protein", "Moderate Fat"], "is_vegetarian": False, "is_vegan": False, "allergens": ["Dairy"]
    },
    "butter_chicken": {
        "serving_size": "1 bowl (250g)",
        "ingredients": ["Chicken", "Tomato Puree", "Butter", "Cream", "Cashew Paste", "Spices", "Garlic", "Ginger"],
        "nutrition": {"calories": "380 kcal", "protein": "25g", "total_fat": "22g", "carbohydrates": "14g", "fiber": "2g", "sugar": "6g", "sodium": "620mg"},
        "health_score": 55, "health_tags": ["High Protein", "High Fat"], "is_vegetarian": False, "is_vegan": False, "allergens": ["Dairy", "Nuts"]
    },
    "paneer_butter_masala": {
        "serving_size": "1 bowl (250g)",
        "ingredients": ["Paneer", "Tomato Puree", "Butter", "Cream", "Cashew Paste", "Spices"],
        "nutrition": {"calories": "360 kcal", "protein": "18g", "total_fat": "24g", "carbohydrates": "16g", "fiber": "2g", "sugar": "5g", "sodium": "580mg"},
        "health_score": 52, "health_tags": ["High Protein", "High Fat", "Rich"], "is_vegetarian": True, "is_vegan": False, "allergens": ["Dairy", "Nuts"]
    },
    "dal": {
        "serving_size": "1 bowl (200g)",
        "ingredients": ["Toor Dal", "Turmeric", "Cumin", "Garlic", "Tomato", "Oil"],
        "nutrition": {"calories": "180 kcal", "protein": "12g", "total_fat": "4g", "carbohydrates": "26g", "fiber": "6g", "sugar": "2g", "sodium": "400mg"},
        "health_score": 85, "health_tags": ["High Protein", "High Fiber", "Low Fat"], "is_vegetarian": True, "is_vegan": True, "allergens": []
    },
    "samosa": {
        "serving_size": "2 pieces (120g)",
        "ingredients": ["Refined Flour", "Potato", "Peas", "Cumin", "Oil (deep fried)", "Spices"],
        "nutrition": {"calories": "310 kcal", "protein": "5g", "total_fat": "18g", "carbohydrates": "34g", "fiber": "3g", "sugar": "2g", "sodium": "520mg"},
        "health_score": 35, "health_tags": ["Deep Fried", "High Fat"], "is_vegetarian": True, "is_vegan": True, "allergens": ["Gluten"]
    },
    "roti": {
        "serving_size": "2 pieces (60g)",
        "ingredients": ["Whole Wheat Flour", "Water", "Salt"],
        "nutrition": {"calories": "160 kcal", "protein": "5g", "total_fat": "1g", "carbohydrates": "33g", "fiber": "4g", "sugar": "0.5g", "sodium": "200mg"},
        "health_score": 80, "health_tags": ["Whole Grain", "Low Fat"], "is_vegetarian": True, "is_vegan": True, "allergens": ["Gluten"]
    },
    "pizza": {
        "serving_size": "2 slices (200g)",
        "ingredients": ["Refined Flour", "Cheese", "Tomato Sauce", "Olive Oil", "Toppings", "Yeast"],
        "nutrition": {"calories": "450 kcal", "protein": "16g", "total_fat": "18g", "carbohydrates": "52g", "fiber": "3g", "sugar": "6g", "sodium": "900mg"},
        "health_score": 40, "health_tags": ["High Sodium", "Processed"], "is_vegetarian": True, "is_vegan": False, "allergens": ["Gluten", "Dairy"]
    },
    "burger": {
        "serving_size": "1 piece (220g)",
        "ingredients": ["Bun", "Patty", "Lettuce", "Tomato", "Cheese", "Sauce", "Onion"],
        "nutrition": {"calories": "520 kcal", "protein": "22g", "total_fat": "28g", "carbohydrates": "42g", "fiber": "2g", "sugar": "8g", "sodium": "850mg"},
        "health_score": 35, "health_tags": ["High Fat", "Processed"], "is_vegetarian": False, "is_vegan": False, "allergens": ["Gluten", "Dairy"]
    },
    "salad": {
        "serving_size": "1 bowl (200g)",
        "ingredients": ["Lettuce", "Tomato", "Cucumber", "Carrot", "Onion", "Lemon", "Olive Oil"],
        "nutrition": {"calories": "80 kcal", "protein": "2g", "total_fat": "4g", "carbohydrates": "10g", "fiber": "4g", "sugar": "5g", "sodium": "150mg"},
        "health_score": 95, "health_tags": ["Low Calorie", "High Fiber", "Nutrient Dense"], "is_vegetarian": True, "is_vegan": True, "allergens": []
    },
    "apple": {
        "serving_size": "1 medium (180g)",
        "ingredients": ["Apple"],
        "nutrition": {"calories": "95 kcal", "protein": "0.5g", "total_fat": "0.3g", "carbohydrates": "25g", "fiber": "4.5g", "sugar": "19g", "sodium": "2mg"},
        "health_score": 92, "health_tags": ["Natural Sugar", "High Fiber"], "is_vegetarian": True, "is_vegan": True, "allergens": []
    },
    "banana": {
        "serving_size": "1 medium (120g)",
        "ingredients": ["Banana"],
        "nutrition": {"calories": "105 kcal", "protein": "1.3g", "total_fat": "0.4g", "carbohydrates": "27g", "fiber": "3g", "sugar": "14g", "sodium": "1mg"},
        "health_score": 88, "health_tags": ["Potassium Rich", "Natural Energy"], "is_vegetarian": True, "is_vegan": True, "allergens": []
    },
    "omelette": {
        "serving_size": "1 piece (100g)",
        "ingredients": ["Eggs", "Onion", "Tomato", "Green Chili", "Salt", "Oil"],
        "nutrition": {"calories": "175 kcal", "protein": "12g", "total_fat": "13g", "carbohydrates": "2g", "fiber": "0.5g", "sugar": "1g", "sodium": "350mg"},
        "health_score": 72, "health_tags": ["High Protein", "Keto Friendly"], "is_vegetarian": True, "is_vegan": False, "allergens": ["Eggs"]
    },
    "chole": {
        "serving_size": "1 bowl (200g)",
        "ingredients": ["Chickpeas", "Onion", "Tomato", "Ginger", "Garlic", "Spices", "Oil"],
        "nutrition": {"calories": "210 kcal", "protein": "10g", "total_fat": "6g", "carbohydrates": "30g", "fiber": "8g", "sugar": "4g", "sodium": "480mg"},
        "health_score": 78, "health_tags": ["High Fiber", "Plant Protein"], "is_vegetarian": True, "is_vegan": True, "allergens": []
    },
    "poha": {
        "serving_size": "1 plate (200g)",
        "ingredients": ["Flattened Rice", "Onion", "Peanuts", "Mustard Seeds", "Turmeric", "Green Chili", "Lemon"],
        "nutrition": {"calories": "250 kcal", "protein": "5g", "total_fat": "8g", "carbohydrates": "42g", "fiber": "2g", "sugar": "3g", "sodium": "350mg"},
        "health_score": 68, "health_tags": ["Light Meal", "Quick Energy"], "is_vegetarian": True, "is_vegan": True, "allergens": ["Peanuts"]
    },
    "momos": {
        "serving_size": "6 pieces (150g)",
        "ingredients": ["Refined Flour", "Cabbage", "Carrot", "Onion", "Garlic", "Soy Sauce"],
        "nutrition": {"calories": "200 kcal", "protein": "6g", "total_fat": "5g", "carbohydrates": "32g", "fiber": "2g", "sugar": "2g", "sodium": "550mg"},
        "health_score": 60, "health_tags": ["Steamed", "Moderate"], "is_vegetarian": True, "is_vegan": True, "allergens": ["Gluten", "Soy"]
    },
    "ice_cream": {
        "serving_size": "1 scoop (100g)",
        "ingredients": ["Milk", "Sugar", "Cream", "Vanilla", "Stabilizers"],
        "nutrition": {"calories": "210 kcal", "protein": "3.5g", "total_fat": "11g", "carbohydrates": "24g", "fiber": "0g", "sugar": "21g", "sodium": "80mg"},
        "health_score": 25, "health_tags": ["High Sugar", "Dessert"], "is_vegetarian": True, "is_vegan": False, "allergens": ["Dairy"]
    },
    "tea": {
        "serving_size": "1 cup (200ml)",
        "ingredients": ["Tea Leaves", "Milk", "Sugar", "Water"],
        "nutrition": {"calories": "60 kcal", "protein": "2g", "total_fat": "1.5g", "carbohydrates": "9g", "fiber": "0g", "sugar": "8g", "sodium": "30mg"},
        "health_score": 55, "health_tags": ["Caffeine", "Antioxidants"], "is_vegetarian": True, "is_vegan": False, "allergens": ["Dairy"]
    },
    "coffee": {
        "serving_size": "1 cup (200ml)",
        "ingredients": ["Coffee Beans", "Milk", "Sugar", "Water"],
        "nutrition": {"calories": "70 kcal", "protein": "2g", "total_fat": "2g", "carbohydrates": "10g", "fiber": "0g", "sugar": "9g", "sodium": "40mg"},
        "health_score": 50, "health_tags": ["Caffeine", "Energy Boost"], "is_vegetarian": True, "is_vegan": False, "allergens": ["Dairy"]
    },
    "naan": {
        "serving_size": "1 piece (90g)",
        "ingredients": ["Refined Flour", "Yogurt", "Butter", "Yeast", "Sugar", "Salt"],
        "nutrition": {"calories": "260 kcal", "protein": "7g", "total_fat": "5g", "carbohydrates": "46g", "fiber": "2g", "sugar": "3g", "sodium": "480mg"},
        "health_score": 45, "health_tags": ["Refined Flour", "High Carb"], "is_vegetarian": True, "is_vegan": False, "allergens": ["Gluten", "Dairy"]
    },
    "gulab_jamun": {
        "serving_size": "2 pieces (80g)",
        "ingredients": ["Khoya", "Refined Flour", "Sugar Syrup", "Cardamom", "Ghee"],
        "nutrition": {"calories": "320 kcal", "protein": "4g", "total_fat": "14g", "carbohydrates": "46g", "fiber": "0.5g", "sugar": "38g", "sodium": "60mg"},
        "health_score": 20, "health_tags": ["High Sugar", "Deep Fried", "Dessert"], "is_vegetarian": True, "is_vegan": False, "allergens": ["Dairy", "Gluten"]
    },
    "rajma": {
        "serving_size": "1 bowl (200g)",
        "ingredients": ["Kidney Beans", "Onion", "Tomato", "Garlic", "Ginger", "Spices", "Oil"],
        "nutrition": {"calories": "200 kcal", "protein": "11g", "total_fat": "4g", "carbohydrates": "32g", "fiber": "9g", "sugar": "3g", "sodium": "420mg"},
        "health_score": 80, "health_tags": ["High Fiber", "Plant Protein", "Iron Rich"], "is_vegetarian": True, "is_vegan": True, "allergens": []
    },
    "pav_bhaji": {
        "serving_size": "1 plate (300g)",
        "ingredients": ["Mixed Vegetables", "Butter", "Pav Bread", "Onion", "Tomato", "Spices"],
        "nutrition": {"calories": "400 kcal", "protein": "10g", "total_fat": "16g", "carbohydrates": "56g", "fiber": "5g", "sugar": "8g", "sodium": "650mg"},
        "health_score": 48, "health_tags": ["Butter Rich", "Street Food"], "is_vegetarian": True, "is_vegan": False, "allergens": ["Gluten", "Dairy"]
    },
    "pasta": {
        "serving_size": "1 plate (250g)",
        "ingredients": ["Pasta", "Tomato Sauce", "Olive Oil", "Garlic", "Herbs", "Cheese"],
        "nutrition": {"calories": "380 kcal", "protein": "12g", "total_fat": "10g", "carbohydrates": "58g", "fiber": "3g", "sugar": "6g", "sodium": "550mg"},
        "health_score": 50, "health_tags": ["High Carb", "Moderate"], "is_vegetarian": True, "is_vegan": False, "allergens": ["Gluten", "Dairy"]
    },
    "soup": {
        "serving_size": "1 bowl (250ml)",
        "ingredients": ["Mixed Vegetables", "Cornstarch", "Pepper", "Salt", "Garlic"],
        "nutrition": {"calories": "90 kcal", "protein": "3g", "total_fat": "1g", "carbohydrates": "16g", "fiber": "2g", "sugar": "4g", "sodium": "600mg"},
        "health_score": 75, "health_tags": ["Low Calorie", "Hydrating"], "is_vegetarian": True, "is_vegan": True, "allergens": []
    },
    "sandwich": {
        "serving_size": "1 piece (150g)",
        "ingredients": ["Bread", "Butter", "Vegetables", "Cheese", "Sauce"],
        "nutrition": {"calories": "280 kcal", "protein": "8g", "total_fat": "12g", "carbohydrates": "36g", "fiber": "3g", "sugar": "4g", "sodium": "500mg"},
        "health_score": 55, "health_tags": ["Quick Meal", "Moderate"], "is_vegetarian": True, "is_vegan": False, "allergens": ["Gluten", "Dairy"]
    },
    "mango": {
        "serving_size": "1 medium (200g)",
        "ingredients": ["Mango"],
        "nutrition": {"calories": "135 kcal", "protein": "1g", "total_fat": "0.5g", "carbohydrates": "35g", "fiber": "3g", "sugar": "30g", "sodium": "2mg"},
        "health_score": 78, "health_tags": ["Vitamin C", "Natural Sugar", "Seasonal"], "is_vegetarian": True, "is_vegan": True, "allergens": []
    },
    "lassi": {
        "serving_size": "1 glass (250ml)",
        "ingredients": ["Yogurt", "Sugar", "Cardamom", "Water"],
        "nutrition": {"calories": "180 kcal", "protein": "6g", "total_fat": "4g", "carbohydrates": "30g", "fiber": "0g", "sugar": "26g", "sodium": "80mg"},
        "health_score": 55, "health_tags": ["Probiotic", "Calcium Rich"], "is_vegetarian": True, "is_vegan": False, "allergens": ["Dairy"]
    }
}

# Fallback for unknown foods
DEFAULT_NUTRITION = {
    "serving_size": "1 serving",
    "ingredients": ["Unknown"],
    "nutrition": {"calories": "—", "protein": "—", "total_fat": "—", "carbohydrates": "—", "fiber": "—", "sugar": "—", "sodium": "—"},
    "health_score": 50, "health_tags": [], "is_vegetarian": False, "is_vegan": False, "allergens": []
}


def get_nutrition(food_key):
    """Get nutrition data for a food item."""
    key = food_key.lower().replace(" ", "_")
    return NUTRITION_DATA.get(key, DEFAULT_NUTRITION)


def search_similar(food_key, limit=5):
    """Find similar food names for suggestions."""
    key = food_key.lower().replace(" ", "_")
    matches = []
    for k in NUTRITION_DATA:
        if key in k or k in key:
            matches.append(k)
    if not matches:
        # Return all keys as potential matches
        matches = list(NUTRITION_DATA.keys())
    return matches[:limit]
