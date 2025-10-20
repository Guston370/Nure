package com.example.healthscanner.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthscanner.R;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying recent scans in the profile page
 */
public class RecentScansProfileAdapter extends RecyclerView.Adapter<RecentScansProfileAdapter.ViewHolder> {

    private final Context context;
    private final List<ScanItem> scanItems;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(ScanItem item);
    }

    public static class ScanItem {
        private final String productName;
        private final String brand;
        private final double healthScore;
        private final int calories;
        private final long timestamp;
        private final String barcode;

        public ScanItem(String productName, String brand, double healthScore, int calories, long timestamp, String barcode) {
            this.productName = productName;
            this.brand = brand;
            this.healthScore = healthScore;
            this.calories = calories;
            this.timestamp = timestamp;
            this.barcode = barcode;
        }

        // Getters
        public String getProductName() { return productName; }
        public String getBrand() { return brand; }
        public double getHealthScore() { return healthScore; }
        public int getCalories() { return calories; }
        public long getTimestamp() { return timestamp; }
        public String getBarcode() { return barcode; }
    }

    public RecentScansProfileAdapter(Context context, List<ScanItem> scanItems) {
        this.context = context;
        this.scanItems = scanItems;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_scan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanItem item = scanItems.get(position);
        
        // Set product name
        holder.productName.setText(item.getProductName());
        
        // Set calories
        holder.calories.setText(item.getCalories() + " cal");
        
        // Set scan time (relative)
        holder.scanTime.setText(getRelativeTime(item.getTimestamp()));
        
        // Set health score with color
        holder.healthScore.setText(String.format(Locale.getDefault(), "%.1f", item.getHealthScore()));
        
        // Set health score background color based on score
        int scoreColor = getHealthScoreColor(item.getHealthScore());
        holder.healthScoreBadge.setCardBackgroundColor(ContextCompat.getColor(context, scoreColor));
        
        // Set product icon based on health score
        int iconRes = getProductIcon(item.getHealthScore());
        holder.productIcon.setImageResource(iconRes);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return scanItems.size();
    }

    private String getRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days == 1 ? "1 day ago" : days + " days ago";
        } else if (hours > 0) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        } else if (minutes > 0) {
            return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        } else {
            return "Just now";
        }
    }

    private int getHealthScoreColor(double score) {
        if (score >= 8.0) {
            return R.color.health_excellent;
        } else if (score >= 6.0) {
            return R.color.health_good;
        } else if (score >= 4.0) {
            return R.color.health_moderate;
        } else if (score >= 2.0) {
            return R.color.health_poor;
        } else {
            return R.color.health_unhealthy;
        }
    }

    private int getProductIcon(double score) {
        if (score >= 8.0) {
            return R.drawable.ic_check_circle;
        } else if (score >= 6.0) {
            return R.drawable.ic_trending_up;
        } else if (score >= 4.0) {
            return R.drawable.ic_warning;
        } else {
            return R.drawable.ic_error;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView productName;
        final TextView scanTime;
        final TextView calories;
        final TextView healthScore;
        final ImageView productIcon;
        final MaterialCardView healthScoreBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.product_name);
            scanTime = itemView.findViewById(R.id.scan_time);
            calories = itemView.findViewById(R.id.calories);
            healthScore = itemView.findViewById(R.id.health_score);
            productIcon = itemView.findViewById(R.id.product_icon);
            healthScoreBadge = itemView.findViewById(R.id.health_score).getParent() instanceof MaterialCardView 
                ? (MaterialCardView) itemView.findViewById(R.id.health_score).getParent() 
                : null;
        }
    }
}