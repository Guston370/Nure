import torch
import os
import shutil
import json
from backend.food_classifier import FoodClassifier

print('Loading model...')
classifier = FoodClassifier()
model = classifier.model
model.eval()

dummy_input = torch.randn(1, 3, 224, 224).to(classifier.device)
onnx_path = 'app/src/main/assets/food_classifier.onnx'

os.makedirs('app/src/main/assets', exist_ok=True)

print('Exporting to ONNX...')
torch.onnx.export(
    model, 
    dummy_input, 
    onnx_path, 
    export_params=True,
    opset_version=14,
    do_constant_folding=True,
    input_names=['input'],
    output_names=['output']
)

print(f'ONNX model saved to {onnx_path}')

# Copy categories
shutil.copy2('backend/food_categories.json', 'app/src/main/assets/food_categories.json')
print('Categories copied to assets.')
