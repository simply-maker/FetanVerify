package com.example.fetanverify;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private ArrayList<HistoryItem> historyList;

    @Override
    @SuppressWarnings("deprecation") // Apply suppression to the method
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.historyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        historyList = getIntent().getParcelableArrayListExtra("historyList");
        if (historyList == null) {
            historyList = new ArrayList<>();
            findViewById(R.id.emptyTextView).setVisibility(View.VISIBLE);
        } else if (historyList.isEmpty()) {
            findViewById(R.id.emptyTextView).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.emptyTextView).setVisibility(View.GONE);
        }

        adapter = new HistoryAdapter(historyList);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}