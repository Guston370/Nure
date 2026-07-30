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
        
        // Set product image (placeholder for now)
        holder.productImage.setImageResource(R.drawable.ic_nutrition_facts);
        
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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productName = itemView.findViewById(R.id.productName);
            caloriesText = itemView.findViewById(R.id.caloriesText);
            healthScoreText = itemView.findViewById(R.id.healthScoreText);
            healthEmoji = itemView.findViewById(R.id.healthEmoji);
            healthScoreProgress = itemView.findViewById(R.id.healthScoreProgress);
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