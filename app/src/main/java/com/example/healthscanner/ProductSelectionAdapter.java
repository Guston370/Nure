package com.example.healthscanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProductSelectionAdapter extends RecyclerView.Adapter<ProductSelectionAdapter.ViewHolder> {

    public static class ProductItem {
        public String name;
        public String brand;
        
        public ProductItem(String name, String brand) {
            this.name = name;
            this.brand = brand;
        }
    }

    private List<ProductItem> productList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ProductItem item);
    }

    public ProductSelectionAdapter(List<ProductItem> productList, OnItemClickListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    public void updateData(List<ProductItem> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductItem item = productList.get(position);
        holder.nameText.setText(item.name);
        holder.brandText.setText(item.brand != null && !item.brand.isEmpty() ? item.brand : "Unknown Brand");
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView brandText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.item_product_name);
            brandText = itemView.findViewById(R.id.item_product_brand);
        }
    }
}
