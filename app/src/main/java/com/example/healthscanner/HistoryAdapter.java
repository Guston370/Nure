package com.example.healthscanner;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<HistoryActivity.ScanHistoryItem> historyList;
    private HistoryActivity historyActivity;

    public HistoryAdapter(List<HistoryActivity.ScanHistoryItem> historyList, HistoryActivity activity) {
        this.historyList = historyList;
        this.historyActivity = activity;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryActivity.ScanHistoryItem item = historyList.get(position);

        holder.productName.setText(item.getProductName());
        holder.barcode.setText("Barcode: " + item.getBarcode());
        holder.category.setText(item.getCategory());
        holder.timestamp.setText(item.getFormattedTime());
        holder.healthRating.setText(item.getHealthRating());
        
        // Set nutrition values
        if (holder.calories != null) {
            holder.calories.setText(item.getCalories() + " cal");
        }
        if (holder.sugar != null) {
            holder.sugar.setText(item.getSugar() + "g sugar");
        }
        if (holder.protein != null) {
            holder.protein.setText(item.getProtein() + "g protein");
        }

        // Set health rating color based on rating
        String rating = item.getHealthRating().toLowerCase();
        if (rating.contains("excellent") || rating.contains("good") || rating.contains("healthy")) {
            holder.healthRating.setTextColor(Color.parseColor("#059669")); // Green
        } else if (rating.contains("moderate") || rating.contains("average")) {
            holder.healthRating.setTextColor(Color.parseColor("#F59E0B")); // Orange
        } else if (rating.contains("poor") || rating.contains("unhealthy") || rating.contains("bad")) {
            holder.healthRating.setTextColor(Color.parseColor("#EF4444")); // Red
        } else {
            holder.healthRating.setTextColor(Color.parseColor("#6B7280")); // Gray
        }

        // Add click listener to show product details
        holder.cardView.setOnClickListener(v -> {
            if (historyActivity != null) {
                historyActivity.showProductDetailsDialog(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView productName, barcode, category, timestamp, healthRating, calories, sugar, protein;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.historyCardView);
            productName = itemView.findViewById(R.id.productName);
            barcode = itemView.findViewById(R.id.barcode);
            category = itemView.findViewById(R.id.category);
            timestamp = itemView.findViewById(R.id.timestamp);
            healthRating = itemView.findViewById(R.id.healthRating);
            calories = itemView.findViewById(R.id.calories);
            sugar = itemView.findViewById(R.id.sugar);
            protein = itemView.findViewById(R.id.protein);
        }
    }
}