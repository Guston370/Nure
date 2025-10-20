package com.example.healthscanner.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthscanner.R;
import com.example.healthscanner.models.HistoryItem;

import java.util.List;

/**
 * Adapter for displaying history items in RecyclerView
 * Provides smooth animations and interactive elements
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    
    private List<HistoryItem> historyItems;
    private OnHistoryItemClickListener clickListener;
    private OnHistoryItemLongClickListener longClickListener;
    
    public interface OnHistoryItemClickListener {
        void onItemClick(HistoryItem item);
    }
    
    public interface OnHistoryItemLongClickListener {
        void onItemLongClick(HistoryItem item);
    }
    
    public HistoryAdapter(List<HistoryItem> historyItems, 
                         OnHistoryItemClickListener clickListener,
                         OnHistoryItemLongClickListener longClickListener) {
        this.historyItems = historyItems;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }
    
    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_history_card, parent, false);
        return new HistoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem item = historyItems.get(position);
        holder.bind(item);
        
        // Add entrance animation
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(
            holder.itemView.getContext(), R.anim.slide_up));
    }
    
    @Override
    public int getItemCount() {
        return historyItems.size();
    }
    
    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private ImageView productImage;
        private TextView productName;
        private TextView brandName;
        private TextView healthEmoji;
        private TextView caloriesValue;
        private TextView proteinValue;
        private TextView sugarValue;
        private TextView healthRating;
        private TextView scanDateTime;
        private ImageView moreOptionsButton;
        
        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            
            productImage = itemView.findViewById(R.id.productImage);
            productName = itemView.findViewById(R.id.productName);
            brandName = itemView.findViewById(R.id.brandName);
            healthEmoji = itemView.findViewById(R.id.healthEmoji);
            caloriesValue = itemView.findViewById(R.id.caloriesValue);
            proteinValue = itemView.findViewById(R.id.proteinValue);
            sugarValue = itemView.findViewById(R.id.sugarValue);
            healthRating = itemView.findViewById(R.id.healthRating);
            scanDateTime = itemView.findViewById(R.id.scanDateTime);
            moreOptionsButton = itemView.findViewById(R.id.moreOptionsButton);
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    v.startAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.scale_bounce));
                    clickListener.onItemClick(historyItems.get(getAdapterPosition()));
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    v.startAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.scale_bounce));
                    longClickListener.onItemLongClick(historyItems.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });
            
            moreOptionsButton.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.scale_bounce));
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(historyItems.get(getAdapterPosition()));
                }
            });
        }
        
        public void bind(HistoryItem item) {
            productName.setText(item.getProductName());
            brandName.setText(item.getBrandName());
            healthEmoji.setText(item.getHealthEmoji());
            caloriesValue.setText(item.getCalories());
            proteinValue.setText(item.getProtein());
            sugarValue.setText(item.getSugar());
            healthRating.setText(item.getHealthRating());
            scanDateTime.setText(item.getScanDateTime());
            
            // Set health rating background color based on category
            int backgroundColor;
            int textColor;
            
            switch (item.getHealthCategory().toLowerCase()) {
                case "healthy":
                    backgroundColor = ContextCompat.getColor(itemView.getContext(), R.color.health_excellent);
                    textColor = ContextCompat.getColor(itemView.getContext(), R.color.white);
                    break;
                case "moderate":
                    backgroundColor = ContextCompat.getColor(itemView.getContext(), R.color.health_moderate);
                    textColor = ContextCompat.getColor(itemView.getContext(), R.color.white);
                    break;
                case "unhealthy":
                    backgroundColor = ContextCompat.getColor(itemView.getContext(), R.color.health_unhealthy);
                    textColor = ContextCompat.getColor(itemView.getContext(), R.color.white);
                    break;
                default:
                    backgroundColor = ContextCompat.getColor(itemView.getContext(), R.color.health_text_secondary);
                    textColor = ContextCompat.getColor(itemView.getContext(), R.color.white);
                    break;
            }
            
            healthRating.setBackgroundColor(backgroundColor);
            healthRating.setTextColor(textColor);
            
            // Set product image (placeholder for now)
            productImage.setImageResource(R.drawable.ic_nutrition_facts);
        }
    }
}