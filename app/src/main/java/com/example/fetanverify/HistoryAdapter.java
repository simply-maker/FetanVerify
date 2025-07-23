package com.example.fetanverify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private ArrayList<HistoryItem> historyList;

    public HistoryAdapter(ArrayList<HistoryItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);
        holder.transactionIdTextView.setText("ID: " + item.getTransactionId());
        holder.statusTextView.setText("Status: " + item.getStatus());
        holder.amountTextView.setText("Amount: " + item.getAmount());
        holder.timestampTextView.setText("Time: " + item.getTimestamp());
        
        // Set colors based on status
        if ("Verified".equals(item.getStatus())) {
            holder.statusTextView.setTextColor(0xFF1DB584); // Green
        } else {
            holder.statusTextView.setTextColor(0xFFE53E3E); // Red
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView transactionIdTextView, statusTextView, timestampTextView, amountTextView;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            transactionIdTextView = itemView.findViewById(R.id.transactionIdTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
        }
    }
}