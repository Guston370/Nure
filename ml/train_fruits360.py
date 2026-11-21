#!/usr/bin/env python
"""Train and export Nure's on-device produce classifier from Fruits-360."""

import argparse
import json
import random
import urllib.request
import zipfile
from pathlib import Path, PurePosixPath, PureWindowsPath

import numpy as np
import tensorflow as tf


ROOT = Path(__file__).resolve().parents[1]
DATA_DIR = Path(__file__).resolve().parent / "data"
DEFAULT_DATASET = DATA_DIR / "fruits-360-100x100-main"
DATASET_URL = (
    "https://github.com/fruits-360/fruits-360-100x100/"
    "archive/refs/heads/main.zip"
)
IMAGE_SIZE = (100, 100)
SEED = 42


def download_dataset() -> Path:
    if ((DEFAULT_DATASET / "Training").is_dir()
            and (DEFAULT_DATASET / "Test").is_dir()):
        return DEFAULT_DATASET

    DATA_DIR.mkdir(parents=True, exist_ok=True)
    archive_path = DATA_DIR / "fruits-360-100x100.zip"
    if not archive_path.exists():
        print(f"Downloading Fruits-360 from {DATASET_URL}")
        urllib.request.urlretrieve(DATASET_URL, archive_path)

    with zipfile.ZipFile(archive_path) as archive:
        for member in archive.infolist():
            path = PurePosixPath(member.filename)
            windows_path = PureWindowsPath(member.filename)
            if (path.is_absolute() or windows_path.is_absolute()
                    or ".." in path.parts or ".." in windows_path.parts):
                raise ValueError(f"Unsafe path in dataset archive: {member.filename}")
        archive.extractall(DATA_DIR)

    if not (DEFAULT_DATASET / "Training").is_dir():
        raise FileNotFoundError(
            f"Expected dataset training folder at {DEFAULT_DATASET / 'Training'}"
        )
    return DEFAULT_DATASET


def build_datasets(dataset_root: Path, batch_size: int):
    training_dir = dataset_root / "Training"
    test_dir = dataset_root / "Test"
    if not training_dir.is_dir() or not test_dir.is_dir():
        raise FileNotFoundError(
            f"Dataset must contain Training and Test directories: {dataset_root}"
        )

    class_names = sorted(path.name for path in training_dir.iterdir() if path.is_dir())
    test_classes = sorted(path.name for path in test_dir.iterdir() if path.is_dir())
    training_classes_by_key = {name.casefold(): name for name in class_names}
    if (not class_names
            or len(training_classes_by_key) != len(class_names)
            or {name.casefold() for name in test_classes} != set(training_classes_by_key)):
        raise ValueError("Training and Test must contain the same non-empty class folders")
    class_indices = {name: index for index, name in enumerate(class_names)}
    test_label_map = tf.constant(
        [class_indices[training_classes_by_key[name.casefold()]] for name in test_classes],
        dtype=tf.int32,
    )

    train = tf.keras.utils.image_dataset_from_directory(
        training_dir,
        class_names=class_names,
        image_size=IMAGE_SIZE,
        batch_size=batch_size,
        label_mode="int",
        seed=SEED,
    )
    test = tf.keras.utils.image_dataset_from_directory(
        test_dir,
        class_names=test_classes,
        image_size=IMAGE_SIZE,
        batch_size=batch_size,
        label_mode="int",
        shuffle=False,
    )

    autotune = tf.data.AUTOTUNE
    test = test.map(
        lambda images, labels: (images, tf.gather(test_label_map, labels)),
        num_parallel_calls=autotune,
    )
    augmentation = tf.keras.Sequential([
        tf.keras.layers.RandomFlip("horizontal"),
        tf.keras.layers.RandomRotation(0.08),
    ])
    train = train.map(
        lambda images, labels: (augmentation(images, training=True), labels),
        num_parallel_calls=autotune,
    )
    return (
        train.prefetch(autotune),
        test.prefetch(autotune),
        class_names,
    )


def build_model(class_count: int):
    base = tf.keras.applications.MobileNetV2(
        input_shape=(*IMAGE_SIZE, 3),
        alpha=0.35,
        include_top=False,
        weights="imagenet",
    )
    base.trainable = False

    inputs = tf.keras.Input(shape=(*IMAGE_SIZE, 3), name="image")
    x = tf.keras.layers.Rescaling(1.0 / 127.5, offset=-1.0, name="normalize")(inputs)
    x = base(x, training=False)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dropout(0.2)(x)
    outputs = tf.keras.layers.Dense(class_count, activation="softmax", name="predictions")(x)
    model = tf.keras.Model(inputs, outputs)
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=1e-3),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    return model, base


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dataset-root", type=Path, default=DEFAULT_DATASET)
    parser.add_argument("--download", action="store_true", help="Download and extract Fruits-360")
    parser.add_argument("--epochs", type=int, default=5, help="Classifier-head training epochs")
    parser.add_argument("--fine-tune-epochs", type=int, default=2)
    parser.add_argument("--batch-size", type=int, default=64)
    args = parser.parse_args()

    random.seed(SEED)
    np.random.seed(SEED)
    tf.random.set_seed(SEED)

    dataset_root = download_dataset() if args.download else args.dataset_root
    train, test, class_names = build_datasets(dataset_root, args.batch_size)
    print(f"Training {len(class_names)} produce classes")
    print(f"TensorFlow devices: {tf.config.list_physical_devices()}")

    model, base = build_model(len(class_names))
    model.fit(train, epochs=args.epochs, verbose=1)

    if args.fine_tune_epochs > 0:
        base.trainable = True
        for layer in base.layers[:-30]:
            layer.trainable = False
        model.compile(
            optimizer=tf.keras.optimizers.Adam(learning_rate=1e-5),
            loss="sparse_categorical_crossentropy",
            metrics=["accuracy"],
        )
        model.fit(train, epochs=args.fine_tune_epochs, verbose=1)

    test_loss, test_accuracy = model.evaluate(test, verbose=1)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    model_bytes = converter.convert()

    assets_dir = ROOT / "app" / "src" / "main" / "assets"
    assets_dir.mkdir(parents=True, exist_ok=True)
    (assets_dir / "produce_model.tflite").write_bytes(model_bytes)
    (assets_dir / "produce_labels.txt").write_text(
        "\n".join(class_names) + "\n", encoding="utf-8"
    )

    metrics_dir = Path(__file__).resolve().parent / "output"
    metrics_dir.mkdir(parents=True, exist_ok=True)
    metrics = {
        "dataset": "Fruits-360 100x100",
        "classes": len(class_names),
        "test_loss": float(test_loss),
        "test_accuracy": float(test_accuracy),
        "model_bytes": len(model_bytes),
        "input_shape": [1, *IMAGE_SIZE, 3],
        "input_type": "float32 RGB pixels in [0, 255]",
    }
    (metrics_dir / "produce_model_metrics.json").write_text(
        json.dumps(metrics, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(metrics, indent=2))
    print(f"Saved on-device model and labels to {assets_dir}")


if __name__ == "__main__":
    main()
