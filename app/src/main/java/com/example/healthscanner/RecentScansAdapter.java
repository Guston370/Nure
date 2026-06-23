package com.example.healthscanner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecentScansAdapter extends RecyclerView.Adapter<RecentScansAdapter.ViewHolder> {

    private List<ScanItem> scanItems;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ScanItem item);
    }

    public RecentScansAdapter(Context context, List<ScanItem> scanItems) {
        this.context = context;
        this.scanItems = scanItems;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_scan_enhanced, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanItem item = scanItems.get(position);
        
        holder.productName.setText(item.getProductName());
        holder.caloriesText.setText(item.getCalories() + " cal");
        holder.healthScoreText.setText(String.valueOf(item.getHealthScore()));
        
        // Set health score progress
        int progress = (int) (item.getHealthScore() * 10);
        holder.healthScoreProgress.setProgress(progress);
        
        // Set health emoji based on score
        String emoji = getHealthEmoji(item.getHealthScore());
        holder.healthEmoji.setText(emoji);
        
        // Adjust score badge background color based on score
        double score = item.getHealthScore();
        int badgeColor;
        if (score >= 8.0) {
            badgeColor = android.graphics.Color.parseColor("#CC10B981"); // Emerald Green
        } else if (score >= 5.0) {
            badgeColor = android.graphics.Color.parseColor("#CCF59E0B"); // Warning Orange
        } else {
            badgeColor = android.graphics.Color.parseColor("#CCEF4444"); // Error Red
        }
        if (holder.scoreBadgeContainer != null) {
            holder.scoreBadgeContainer.setCardBackgroundColor(badgeColor);
        }

        // Set dynamic category and placeholder image
        String nameLower = item.getProductName().toLowerCase();
        String category = "PRODUCT";
        int placeholderRes = R.drawable.ic_product_placeholder;
        
        if (nameLower.contains("cereal") || nameLower.contains("granola") || nameLower.contains("oats") || nameLower.contains("muesli") || nameLower.contains("cornflakes")) {
            category = "CEREAL";
            placeholderRes = R.drawable.ic_cereal_placeholder;
        } else if (nameLower.contains("milk") || nameLower.contains("yogurt") || nameLower.contains("cheese") || nameLower.contains("dairy") || nameLower.contains("dahi") || nameLower.contains("paneer") || nameLower.contains("butter")) {
            category = "DAIRY";
            placeholderRes = R.drawable.ic_dairy_placeholder;
        } else if (nameLower.contains("fruit") || nameLower.contains("apple") || nameLower.contains("banana") || nameLower.contains("berry") || nameLower.contains("orange") || nameLower.contains("mango") || nameLower.contains("juice") || nameLower.contains("strawberry")) {
            category = "FRUIT";
            placeholderRes = R.drawable.ic_fruit_placeholder;
        } else if (nameLower.contains("snack") || nameLower.contains("chip") || nameLower.contains("cookie") || nameLower.contains("chocolate") || nameLower.contains("candy") || nameLower.contains("bar") || nameLower.contains("biscuit") || nameLower.contains("namkeen")) {
            category = "SNACK";
            placeholderRes = R.drawable.ic_snack_placeholder;
        }
        
        if (holder.productCategory != null) {
            holder.productCategory.setText(category);
        }
        holder.productImage.setImageResource(placeholderRes);
        
        // Add slide-in animation
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_right));
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return scanItems.size();
    }

    private String getHealthEmoji(double score) {
        if (score >= 8.0) return "😄";
        else if (score >= 6.0) return "😊";
        else if (score >= 4.0) return "😐";
        else if (score >= 2.0) return "😕";
        else return "😞";
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName;
        TextView caloriesText;
        TextView healthScoreText;
        TextView healthEmoji;
        ProgressBar healthScoreProgress;
        TextView productCategory;
        com.google.android.material.card.MaterialCardView scoreBadgeContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productName = itemView.findViewById(R.id.productName);
            caloriesText = itemView.findViewById(R.id.caloriesText);
            healthScoreText = itemView.findViewById(R.id.healthScoreText);
            healthEmoji = itemView.findViewById(R.id.healthEmoji);
            healthScoreProgress = itemView.findViewById(R.id.healthScoreProgress);
            productCategory = itemView.findViewById(R.id.productCategory);
            scoreBadgeContainer = itemView.findViewById(R.id.scoreBadgeContainer);
        }
    }

    // Simple data class for scan items
    public static class ScanItem {
        private String productName;
        private int calories;
        private double healthScore;
        private String imageUrl;

        public ScanItem(String productName, int calories, double healthScore) {
            this.productName = productName;
            this.calories = calories;
            this.healthScore = healthScore;
        }

        // Getters
        public String getProductName() { return productName; }
        public int getCalories() { return calories; }
        public double getHealthScore() { return healthScore; }
        public String getImageUrl() { return imageUrl; }
    }
}