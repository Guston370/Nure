package com.example.healthscanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying health category scores in ProductDetailsActivity
 */
public class CategoryScoresAdapter extends RecyclerView.Adapter<CategoryScoresAdapter.CategoryViewHolder> {
    
    private List<CategoryScore> categoryScores;
    
    public CategoryScoresAdapter(List<CategoryScore> categoryScores) {
        this.categoryScores = categoryScores;
    }
    
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_category_score_simple, parent, false);
        return new CategoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryScore categoryScore = categoryScores.get(position);
        holder.bind(categoryScore);
    }
    
    @Override
    public int getItemCount() {
        return categoryScores.size();
    }
    
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private TextView categoryName;
        private TextView scoreText;
        private TextView analysisText;
        private ProgressBar scoreProgress;
        
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.category_name);
            scoreText = itemView.findViewById(R.id.score_text);
            analysisText = itemView.findViewById(R.id.analysis_text);
            scoreProgress = itemView.findViewById(R.id.score_progress);
        }
        
        public void bind(CategoryScore categoryScore) {
            if (categoryName != null) {
                categoryName.setText(categoryScore.category);
            }
            
            if (scoreText != null) {
                scoreText.setText(String.format("%.0f/%.0f", categoryScore.score, categoryScore.maxScore));
            }
            
            if (analysisText != null) {
                analysisText.setText(categoryScore.analysis);
            }
            
            if (scoreProgress != null) {
                int progressPercentage = (int) ((categoryScore.score / categoryScore.maxScore) * 100);
                scoreProgress.setProgress(progressPercentage);
            }
        }
    }
    
    // Data class for category scores
    public static class CategoryScore {
        public String category;
        public double score;
        public double maxScore;
        public String analysis;
        
        public CategoryScore(String category, double score, double maxScore, String analysis) {
            this.category = category;
            this.score = score;
            this.maxScore = maxScore;
            this.analysis = analysis;
        }
    }
}