"""
Food Classifier using pretrained MobileNetV2 with transfer learning.
Supports fine-tuning via RLHF feedback.
"""

import os
import json
import torch
import torch.nn as nn
from torchvision import models, transforms
from PIL import Image

MODEL_DIR = os.path.join(os.path.dirname(__file__), "models")
CATEGORIES_FILE = os.path.join(os.path.dirname(__file__), "food_categories.json")

# ImageNet classes that map to food items (subset)
IMAGENET_FOOD_MAP = {
    924: "gulab_jamun",  # guacamole -> closest
    927: "ice_cream",    # ice cream
    928: "ice_cream",    # ice lolly
    929: "french_fries",
    930: "pizza",
    931: "burrito",
    932: "sandwich",
    933: "burger",       # cheeseburger
    934: "burger",       # hotdog
    935: "pasta",        # spaghetti
    936: "noodles",      # chow mein close
    937: "soup",         # meat loaf -> fallback
    938: "pizza",        # pizza
    941: "omelette",     # omelet close
    947: "banana",
    948: "apple",        # Granny Smith
    949: "apple",
    950: "orange",
    951: "mango",        # fig -> mango fallback
    952: "mango",        # pineapple -> close
    953: "banana",
    954: "grapes",       # strawberry -> grapes
    955: "orange",
    956: "salad",        # broccoli -> salad
    959: "salad",        # carbonara close
    963: "pizza",        # pizza close
}


def _load_categories():
    """Load food categories from JSON file."""
    with open(CATEGORIES_FILE, "r") as f:
        data = json.load(f)
    return data["categories"], data.get("display_names", {})


class CustomCNN(nn.Module):
    """Custom Convolutional Neural Network (CNN) for Food Classification."""
    def __init__(self, num_classes):
        super(CustomCNN, self).__init__()
        self.features = nn.Sequential(
            # Block 1: Input 3x224x224 -> Conv 32x224x224 -> MaxPool 32x112x112
            nn.Conv2d(3, 32, kernel_size=3, padding=1),
            nn.BatchNorm2d(32),
            nn.ReLU(inplace=True),
            nn.Conv2d(32, 32, kernel_size=3, padding=1),
            nn.BatchNorm2d(32),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(kernel_size=2, stride=2),
            
            # Block 2: Input 32x112x112 -> Conv 64x112x112 -> MaxPool 64x56x56
            nn.Conv2d(32, 64, kernel_size=3, padding=1),
            nn.BatchNorm2d(64),
            nn.ReLU(inplace=True),
            nn.Conv2d(64, 64, kernel_size=3, padding=1),
            nn.BatchNorm2d(64),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(kernel_size=2, stride=2),
            
            # Block 3: Input 64x56x56 -> Conv 128x56x56 -> MaxPool 128x28x28
            nn.Conv2d(64, 128, kernel_size=3, padding=1),
            nn.BatchNorm2d(128),
            nn.ReLU(inplace=True),
            nn.Conv2d(128, 128, kernel_size=3, padding=1),
            nn.BatchNorm2d(128),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(kernel_size=2, stride=2),
            
            # Block 4: Input 128x28x28 -> Conv 256x28x28 -> MaxPool 256x14x14
            nn.Conv2d(128, 256, kernel_size=3, padding=1),
            nn.BatchNorm2d(256),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(kernel_size=2, stride=2)
        )
        
        self.classifier = nn.Sequential(
            nn.AdaptiveAvgPool2d((7, 7)),
            nn.Flatten(),
            nn.Linear(256 * 7 * 7, 512),
            nn.ReLU(inplace=True),
            nn.Dropout(0.5),
            nn.Linear(512, num_classes)
        )
        
    def forward(self, x):
        x = self.features(x)
        x = self.classifier(x)
        return x


class FoodClassifier:
    """Custom CNN-based food classifier with RLHF fine-tuning support."""

    def __init__(self):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.categories, self.display_names = _load_categories()
        self.num_classes = len(self.categories)

        # Image preprocessing pipeline
        self.transform = transforms.Compose([
            transforms.Resize(256),
            transforms.CenterCrop(224),
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406],
                                 std=[0.229, 0.224, 0.225]),
        ])

        # Build model
        self._build_model()
        self.model.eval()

        # Also load vanilla ImageNet model for fallback
        self.imagenet_model = models.mobilenet_v2(pretrained=True).to(self.device)
        self.imagenet_model.eval()

        # Load ImageNet labels
        self._load_imagenet_labels()

        print(f"[FoodClassifier] Loaded with {self.num_classes} food categories on {self.device}")

    def _build_model(self):
        """Build Custom CNN classifier."""
        self.model = CustomCNN(self.num_classes)
        self.model = self.model.to(self.device)

        # Try loading saved weights
        weights_path = os.path.join(MODEL_DIR, "food_classifier.pth")
        if os.path.exists(weights_path):
            try:
                state = torch.load(weights_path, map_location=self.device)
                self.model.load_state_dict(state)
                print("[FoodClassifier] Loaded custom CNN fine-tuned weights")
            except Exception as e:
                print(f"[FoodClassifier] Error loading custom CNN weights: {e}. Starting fresh.")
        else:
            print("[FoodClassifier] No custom CNN weights found, using fresh initialization")

    def _load_imagenet_labels(self):
        """Load ImageNet class labels for fallback classification."""
        # Simplified: we only use the food-related mapping
        self.imagenet_food_map = IMAGENET_FOOD_MAP

    def predict(self, image_path):
        """
        Predict food item from an image.
        Returns: (food_name, confidence, top_predictions)
        """
        try:
            image = Image.open(image_path).convert("RGB")
            input_tensor = self.transform(image).unsqueeze(0).to(self.device)
        except Exception as e:
            print(f"[FoodClassifier] Error loading image: {e}")
            return "unknown", 0.0, []

        # Try custom model first
        custom_result = self._predict_custom(input_tensor)

        # Also try ImageNet fallback
        imagenet_result = self._predict_imagenet(input_tensor)

        # Use whichever has higher confidence
        if custom_result[1] > 0.4:
            return custom_result
        elif imagenet_result[1] > 0.3:
            return imagenet_result
        else:
            # Return custom result even with low confidence
            return custom_result

    def _predict_custom(self, input_tensor):
        """Run prediction through custom food classifier."""
        with torch.no_grad():
            output = self.model(input_tensor)
            probabilities = torch.nn.functional.softmax(output[0], dim=0)

            top5_prob, top5_idx = torch.topk(probabilities, min(5, len(probabilities)))

            best_idx = top5_idx[0].item()
            best_conf = top5_prob[0].item()
            best_name = self.categories[best_idx] if best_idx < len(self.categories) else "unknown"

            top_preds = []
            for i in range(len(top5_idx)):
                idx = top5_idx[i].item()
                if idx < len(self.categories):
                    top_preds.append({
                        "name": self.categories[idx],
                        "display_name": self.display_names.get(self.categories[idx], self.categories[idx]),
                        "confidence": round(top5_prob[i].item(), 4)
                    })

            return best_name, best_conf, top_preds

    def _predict_imagenet(self, input_tensor):
        """Fallback: use ImageNet model and map food classes."""
        with torch.no_grad():
            output = self.imagenet_model(input_tensor)
            probabilities = torch.nn.functional.softmax(output[0], dim=0)

            top10_prob, top10_idx = torch.topk(probabilities, 10)

            for i in range(len(top10_idx)):
                class_idx = top10_idx[i].item()
                if class_idx in self.imagenet_food_map:
                    food_name = self.imagenet_food_map[class_idx]
                    conf = top10_prob[i].item()
                    return food_name, conf, [{
                        "name": food_name,
                        "display_name": self.display_names.get(food_name, food_name),
                        "confidence": round(conf, 4)
                    }]

            return "unknown", 0.0, []

    def save_model(self):
        """Save current model weights."""
        os.makedirs(MODEL_DIR, exist_ok=True)
        weights_path = os.path.join(MODEL_DIR, "food_classifier.pth")
        torch.save(self.model.state_dict(), weights_path)
        print(f"[FoodClassifier] Model saved to {weights_path}")

    def fine_tune(self, images_and_labels, epochs=5, lr=0.001):
        """
        Fine-tune the custom CNN model using RLHF feedback data.
        images_and_labels: list of (image_path, label_index) tuples
        """
        if not images_and_labels:
            return {"status": "no_data"}

        self.model.train()
        # Train all parameters of our custom CNN
        optimizer = torch.optim.Adam(self.model.parameters(), lr=lr)
        criterion = nn.CrossEntropyLoss()

        total_loss = 0
        correct = 0
        total = 0

        for epoch in range(epochs):
            epoch_loss = 0
            for img_path, label_idx in images_and_labels:
                try:
                    image = Image.open(img_path).convert("RGB")
                    input_tensor = self.transform(image).unsqueeze(0).to(self.device)
                    target = torch.tensor([label_idx]).to(self.device)

                    optimizer.zero_grad()
                    output = self.model(input_tensor)
                    loss = criterion(output, target)
                    loss.backward()
                    optimizer.step()

                    epoch_loss += loss.item()
                    _, predicted = torch.max(output, 1)
                    total += 1
                    correct += (predicted == target).sum().item()
                except Exception as e:
                    print(f"[FoodClassifier] Error training on {img_path}: {e}")
                    continue

            total_loss = epoch_loss

        self.model.eval()
        self.save_model()

        accuracy = correct / total if total > 0 else 0
        return {
            "status": "success",
            "epochs": epochs,
            "samples": len(images_and_labels),
            "final_loss": round(total_loss, 4),
            "accuracy": round(accuracy, 4)
        }

    def get_category_index(self, food_name):
        """Get the index for a food category, adding it if needed."""
        key = food_name.lower().replace(" ", "_")
        if key in self.categories:
            return self.categories.index(key)
        # New category discovered via RLHF - would need model expansion
        # For now, find the closest match
        for i, cat in enumerate(self.categories):
            if key in cat or cat in key:
                return i
        return -1
