"""
RLHF Manager - Reinforcement Learning from Human Feedback
Manages the feedback loop: collect corrections -> store -> retrain -> improve.
"""

import os
import json
import shutil
import time
from datetime import datetime

FEEDBACK_DIR = os.path.join(os.path.dirname(__file__), "feedback_data")
IMAGES_DIR = os.path.join(FEEDBACK_DIR, "images")
FEEDBACK_FILE = os.path.join(FEEDBACK_DIR, "feedback.json")
STATS_FILE = os.path.join(FEEDBACK_DIR, "rlhf_stats.json")

# Minimum feedback samples before retraining
MIN_RETRAIN_SAMPLES = 50


class RLHFManager:
    """Manages the human feedback loop for model improvement."""

    def __init__(self, classifier):
        self.classifier = classifier
        os.makedirs(IMAGES_DIR, exist_ok=True)
        self.feedback_data = self._load_feedback()
        self.stats = self._load_stats()
        print(f"[RLHF] Loaded {len(self.feedback_data)} feedback entries")

    def _load_feedback(self):
        """Load existing feedback data."""
        if os.path.exists(FEEDBACK_FILE):
            with open(FEEDBACK_FILE, "r") as f:
                return json.load(f)
        return []

    def _save_feedback(self):
        """Persist feedback data to disk."""
        with open(FEEDBACK_FILE, "w") as f:
            json.dump(self.feedback_data, f, indent=2)

    def _load_stats(self):
        """Load RLHF training statistics."""
        if os.path.exists(STATS_FILE):
            with open(STATS_FILE, "r") as f:
                return json.load(f)
        return {
            "total_feedback": 0,
            "confirmations": 0,
            "corrections": 0,
            "retrains": 0,
            "last_retrain": None,
            "accuracy_history": [],
            "reward_history": []
        }

    def _save_stats(self):
        """Persist stats to disk."""
        with open(STATS_FILE, "w") as f:
            json.dump(self.stats, f, indent=2)

    def store_feedback(self, image_file, predicted_label, correct_label):
        """
        Store a single feedback entry.
        - image_file: uploaded file object (from Flask request)
        - predicted_label: what the model predicted
        - correct_label: what the human says it actually is
        Returns: feedback entry dict
        """
        timestamp = datetime.now().isoformat()
        filename = f"feedback_{int(time.time())}_{correct_label}.jpg"
        image_path = os.path.join(IMAGES_DIR, filename)

        # Save the image
        if hasattr(image_file, 'save'):
            image_file.save(image_path)
        elif isinstance(image_file, str) and os.path.exists(image_file):
            shutil.copy2(image_file, image_path)

        # Calculate reward
        is_correct = predicted_label and predicted_label.lower().replace(" ", "_") == correct_label.lower().replace(" ", "_")
        reward = 1.0 if is_correct else -0.5

        entry = {
            "id": len(self.feedback_data) + 1,
            "timestamp": timestamp,
            "image_path": image_path,
            "predicted_label": predicted_label,
            "correct_label": correct_label,
            "is_correct": is_correct,
            "reward": reward,
            "used_in_training": False
        }

        self.feedback_data.append(entry)
        self._save_feedback()

        # Update stats
        self.stats["total_feedback"] += 1
        if is_correct:
            self.stats["confirmations"] += 1
        else:
            self.stats["corrections"] += 1
        self.stats["reward_history"].append({"reward": reward, "timestamp": timestamp})
        self._save_stats()

        # Check if we should auto-retrain
        untrained = [e for e in self.feedback_data if not e.get("used_in_training")]
        should_retrain = len(untrained) >= MIN_RETRAIN_SAMPLES

        return {
            "status": "stored",
            "entry_id": entry["id"],
            "is_correct": is_correct,
            "reward": reward,
            "total_feedback": self.stats["total_feedback"],
            "pending_retrain": len(untrained),
            "should_retrain": should_retrain
        }

    def retrain_model(self):
        """
        Retrain the model using all un-trained feedback data.
        This is the core RLHF loop: Human corrections → Model improvement.
        """
        untrained = [e for e in self.feedback_data if not e.get("used_in_training")]
        if len(untrained) < MIN_RETRAIN_SAMPLES:
            return {
                "status": "insufficient_data",
                "pending": len(untrained),
                "required": MIN_RETRAIN_SAMPLES
            }

        # Prepare training data
        training_pairs = []
        for entry in untrained:
            img_path = entry.get("image_path", "")
            label = entry.get("correct_label", "")
            if os.path.exists(img_path) and label:
                label_idx = self.classifier.get_category_index(label)
                if label_idx >= 0:
                    # Weight corrections higher than confirmations (RLHF reward shaping)
                    weight = 2 if not entry.get("is_correct") else 1
                    for _ in range(weight):
                        training_pairs.append((img_path, label_idx))

        if not training_pairs:
            return {"status": "no_valid_training_data"}

        # Fine-tune the model
        result = self.classifier.fine_tune(training_pairs, epochs=5, lr=0.0005)

        # Mark entries as used
        for entry in untrained:
            entry["used_in_training"] = True
        self._save_feedback()

        # Update stats
        self.stats["retrains"] += 1
        self.stats["last_retrain"] = datetime.now().isoformat()
        if result.get("accuracy"):
            self.stats["accuracy_history"].append({
                "accuracy": result["accuracy"],
                "timestamp": datetime.now().isoformat(),
                "samples": len(training_pairs)
            })
        self._save_stats()

        return {
            "status": "success",
            "training_result": result,
            "total_retrains": self.stats["retrains"],
            "accuracy_history": self.stats["accuracy_history"][-5:]
        }

    def get_stats(self):
        """Get current RLHF statistics."""
        untrained = len([e for e in self.feedback_data if not e.get("used_in_training")])
        return {
            **self.stats,
            "pending_retrain": untrained,
            "can_retrain": untrained >= MIN_RETRAIN_SAMPLES,
            "total_entries": len(self.feedback_data)
        }
