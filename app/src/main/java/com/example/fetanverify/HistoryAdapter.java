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
        holder.transactionIdTextView.setText("Transaction ID: " + item.getTransactionId());
        holder.statusTextView.setText("Status: " + item.getStatus());
        holder.timestampTextView.setText("Timestamp: " + item.getTimestamp());
        holder.itemView.setBackgroundColor(0xFFFFFFFF); // Optional: Ensure background is white for contrast
        holder.transactionIdTextView.setTextColor(0xFF000000); // Black
        holder.statusTextView.setTextColor(0xFF000000); // Black
        holder.timestampTextView.setTextColor(0xFF000000); // Black
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView transactionIdTextView, statusTextView, timestampTextView;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            transactionIdTextView = itemView.findViewById(R.id.transactionIdTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }
    }
}