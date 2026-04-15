import tensorflow as tf
import tensorflow_datasets as tfds
from tensorflow.keras import layers, applications, models, optimizers
import os
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.metrics import classification_report, confusion_matrix
import json

# Hyperparameters
IMG_SIZE = 224
BATCH_SIZE = 32
AUTOTUNE = tf.data.AUTOTUNE
EPOCHS_PHASE_1 = 10
EPOCHS_PHASE_2 = 10
LEARNING_RATE_1 = 1e-3
LEARNING_RATE_2 = 1e-5

def preprocess(image, label):
    image = tf.image.resize(image, [IMG_SIZE, IMG_SIZE])
    # Normalize pixel values to [0,1] as per image instructions
    image = tf.cast(image, tf.float32) / 255.0
    return image, label

def augment(image, label):
    image = tf.image.random_flip_left_right(image)
    image = tf.image.random_brightness(image, 0.15)
    image = tf.image.random_contrast(image, 0.8, 1.2)
    image = tf.image.random_saturation(image, 0.8, 1.2)
    return image, label

def build_data_pipeline():
    print("Loading Food-101 using TFDS...")
    (train_ds, val_ds), info = tfds.load(
        'food101',
        split=['train', 'validation'],
        as_supervised=True,
        with_info=True
    )

    print("Building tf.data pipelines with explicit mapping...")
    
    # Validation Pipeline
    val_ds = (val_ds
        .map(preprocess, num_parallel_calls=AUTOTUNE)
        .batch(BATCH_SIZE)
        .prefetch(AUTOTUNE))

    # Training Pipeline
    train_ds = (train_ds
        .map(preprocess, num_parallel_calls=AUTOTUNE)
        .map(augment, num_parallel_calls=AUTOTUNE)
        .shuffle(2000)
        .batch(BATCH_SIZE)
        .prefetch(AUTOTUNE))

    return train_ds, val_ds, info

def build_model(num_classes):
    print("Loading EfficientNetB0...")
    base_model = applications.EfficientNetB0(
        include_top=False,
        weights='imagenet',
        input_shape=(IMG_SIZE, IMG_SIZE, 3)
    )
    base_model.trainable = False

    inputs = layers.Input(shape=(IMG_SIZE, IMG_SIZE, 3))
    x = base_model(inputs, training=False)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dropout(0.3)(x)
    x = layers.Dense(256, activation='relu')(x)
    x = layers.Dropout(0.2)(x)
    outputs = layers.Dense(num_classes, activation='softmax')(x)

    model = models.Model(inputs, outputs)
    return model, base_model

def generate_confusion_matrix(model, val_ds, class_names):
    print("Generating Confusion Matrix...")
    y_true, y_pred = [], []
    for images, labels in val_ds:
        preds = model.predict(images, verbose=0)
        y_pred.extend(np.argmax(preds, axis=1))
        y_true.extend(labels.numpy())

    print(classification_report(y_true, y_pred, target_names=class_names))

    # Confusion matrix (top 20 classes for readability)
    cm = confusion_matrix(y_true, y_pred)
    
    # Find worst classes
    per_class_acc = cm.diagonal() / cm.sum(axis=1)
    worst = np.argsort(per_class_acc)[:10]
    print("Worst classes:", [class_names[i] for i in worst])

    plt.figure(figsize=(24, 20))
    sns.heatmap(cm, annot=False, cmap='Blues', xticklabels=class_names, yticklabels=class_names)
    plt.title('Food-101 Confusion Matrix')
    plt.ylabel('True Label')
    plt.xlabel('Predicted Label')
    plt.tight_layout()
    plt.savefig('confusion_matrix.png', dpi=300)
    print("Confusion matrix saved as confusion_matrix.png")

def main():
    train_ds, val_ds, info = build_data_pipeline()
    num_classes = info.features['label'].num_classes
    class_names = info.features['label'].names
    
    try:
        BASE_DIR = os.path.dirname(os.path.abspath(__file__))
        data_dir = os.path.join(BASE_DIR, '..', 'data')
        os.makedirs(data_dir, exist_ok=True)
        class_names_path = os.path.join(data_dir, 'class_names.json')
    except NameError:
        # Running in Colab/Jupyter where __file__ is undefined
        BASE_DIR = os.getcwd()
        class_names_path = os.path.join(BASE_DIR, 'class_names.json')

    # Save class names locally just in case
    with open(class_names_path, 'w') as f:
        json.dump(class_names, f)
    
    model, base_model = build_model(num_classes)
    
    # Phase 1: Train just the head
    print("--- Phase 1: Training Head ---")
    model.compile(
        optimizer=tf.keras.optimizers.Adam(1e-3),
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
                  
    history1 = model.fit(
        train_ds,
        epochs=10,
        validation_data=val_ds,
        callbacks=[
            tf.keras.callbacks.EarlyStopping(
                patience=3, restore_best_weights=True),
            tf.keras.callbacks.ReduceLROnPlateau(
                factor=0.5, patience=2)
        ]
    )

    # Phase 2: Fine-Tuning
    print("--- Phase 2: Fine-Tuning Top 30 Layers ---")
    # Unfreeze top 30 layers of the base
    base_model.trainable = True
    for layer in base_model.layers[:-30]:
        layer.trainable = False
        
    # Recompile with much lower LR
    model.compile(
        optimizer=tf.keras.optimizers.Adam(1e-5),
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
                  
    history2 = model.fit(
        train_ds,
        epochs=20,
        validation_data=val_ds,
        callbacks=[
            tf.keras.callbacks.ModelCheckpoint(
                'best_food_model.h5',
                save_best_only=True,
                monitor='val_accuracy'),
            tf.keras.callbacks.EarlyStopping(
                patience=5, restore_best_weights=True)
        ]
    )
    
    # Export SavedModel
    save_path = os.path.join(BASE_DIR, 'food_classifier_savedmodel')
    try:
        # Keras 3 (TF 2.16+)
        model.export(save_path)
    except AttributeError:
        # Keras 2 (TF < 2.16)
        model.save(save_path)
    print(f"Model saved to {save_path}")

    # Export TFLite
    print("Exporting to TFLite for Mobile Deployment...")
    converter = tf.lite.TFLiteConverter.from_saved_model(save_path)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()

    tflite_path = os.path.join(BASE_DIR, 'food_classifier.tflite')
    with open(tflite_path, 'wb') as f:
        f.write(tflite_model)
    print(f"TFLite model saved as {tflite_path}")

    # Final requirement: Confusion Matrix
    generate_confusion_matrix(model, val_ds, class_names)

if __name__ == '__main__':
    main()
