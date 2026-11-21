# Nure produce image model

This workflow fine-tunes MobileNetV2 on the public [Fruits-360 100x100 dataset](https://github.com/fruits-360/fruits-360-100x100) and exports an on-device LiteRT model plus its ordered class labels into `app/src/main/assets/`.

Fruits-360 contains 278 produce classes (fruits, vegetables, nuts, seeds and mushrooms). Its images are centered produce on a mostly white background, so the model is a produce classifier, not a general meal detector or a bounding-box detector. Nure runs it only when ML Kit first sees fruit/vegetable/produce; the existing ML Kit food labels and OCR remain the fallback for meals and packaged products.

Nutrition values are not inferred from pixels. The predicted class name is passed to the existing nutrition repository, which resolves values from the offline table and then USDA/Open Food Facts when available.

## Train on Windows

From the repository root, with Python 3.11 installed:

```powershell
py -3.11 -m venv ml\.venv
.\ml\.venv\Scripts\python.exe -m pip install -r ml\requirements.txt
.\ml\.venv\Scripts\python.exe ml\train_fruits360.py --download
```

The archive and extracted images stay under the ignored `ml/data/` directory. Training evaluates against the dataset's test split and writes:

- `app/src/main/assets/produce_model.tflite`
- `app/src/main/assets/produce_labels.txt`
- `ml/output/produce_model_metrics.json`

The TFLite model accepts a `1 x 100 x 100 x 3` float RGB tensor with pixel values in `[0, 255]`. Preprocessing is included in the model. The ordered labels file must remain alongside the model.

To train from an already-extracted dataset, omit `--download` and pass its root:

```powershell
.\ml\.venv\Scripts\python.exe ml\train_fruits360.py --dataset-root "D:\datasets\fruits-360-100x100-main"
```

The model is derived from Fruits-360 and is distributed under CC BY-SA 4.0. Dataset attribution: Mihai Oltean, *Fruits-360*, https://github.com/fruits-360/fruits-360-100x100, licensed under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). The dataset itself is not committed to this repository.
