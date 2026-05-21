import torch
import os
import json
from torchvision import models

print('Loading ImageNet model...')
model = models.mobilenet_v2(pretrained=True)
model.eval()

device = torch.device('cpu')
model = model.to(device)

dummy_input = torch.randn(1, 3, 224, 224)
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

IMAGENET_FOOD_MAP = {
    924: "gulab_jamun",
    927: "ice_cream",
    928: "ice_cream",
    929: "french_fries",
    930: "pizza",
    931: "burrito",
    932: "sandwich",
    933: "burger",
    934: "burger",
    935: "pasta",
    936: "noodles",
    937: "soup",
    938: "pizza",
    941: "omelette",
    947: "banana",
    948: "apple",
    949: "apple",
    950: "orange",
    951: "mango",
    952: "mango",
    953: "banana",
    954: "grapes",
    955: "orange",
    956: "salad",
    959: "salad",
    963: "pizza"
}

with open('app/src/main/assets/imagenet_food_map.json', 'w') as f:
    json.dump(IMAGENET_FOOD_MAP, f)
print('imagenet_food_map.json saved to assets.')
