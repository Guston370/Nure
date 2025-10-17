package com.example.healthscanner.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthscanner.R;
import com.example.healthscanner.models.SavedProduct;

import java.util.List;

public class SavedProductsAdapter extends RecyclerView.Adapter<SavedProductsAdapter.ViewHolder> {

    private final List<SavedProduct> savedProducts;
    private final Context context;
    private final OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(SavedProduct product);
        void onDeleteProduct(SavedProduct product);
    }

    public SavedProductsAdapter(Context context, List<SavedProduct> savedProducts, OnProductClickListener listener) {
        this.context = context;
        this.savedProducts = savedProducts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_saved_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedProduct product = savedProducts.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return savedProducts.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView productName;
        private final TextView healthScore;
        private final ImageView productImage;
        private final ImageView favoriteIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.product_name);
            healthScore = itemView.findViewById(R.id.health_score);
            productImage = itemView.findViewById(R.id.product_image);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onProductClick(savedProducts.get(position));
                }
            });

            favoriteIcon.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeleteProduct(savedProducts.get(position));
                }
            });
        }

        void bind(SavedProduct product) {
            productName.setText(product.getName());
            healthScore.setText(String.format("%d%%", product.getHealthScore()));
            // Load product image using Glide or similar library
            favoriteIcon.setImageResource(R.drawable.ic_favorite);
        }
    }
}