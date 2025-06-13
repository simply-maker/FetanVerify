package com.example.fetanverify;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.CaptureActivity;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int SCAN_REQUEST_CODE = 0x0000c0de;
    private TextInputEditText transactionIdEditText;
    private TextInputLayout textInputLayout;
    private MaterialButton verifyButton, scanButton, historyButton;
    private DatabaseReference databaseReference;
    private ArrayList<HistoryItem> historyList;
    private FirebaseAuth mAuth;
    private ActivityResultLauncher<Intent> scanLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        textInputLayout = findViewById(R.id.textInputLayout);
        transactionIdEditText = findViewById(R.id.transactionIdEditText);
        verifyButton = findViewById(R.id.verifyButton);
        scanButton = findViewById(R.id.scanButton);
        historyButton = findViewById(R.id.historyButton);

        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid()).child("sms_messages");
        historyList = new ArrayList<>();

        scanLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String transactionId = result.getData().getStringExtra("SCAN_RESULT");
                        if (transactionId != null) {
                            transactionIdEditText.setText(transactionId.trim());
                            verifyTransaction(transactionId.trim());
                        } else {
                            Toast.makeText(this, "No scan data received", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                    }
                });

        verifyButton.setOnClickListener(v -> {
            String transactionId = transactionIdEditText.getText().toString().trim();
            if (TextUtils.isEmpty(transactionId)) {
                textInputLayout.setError("Enter a transaction ID");
                return;
            }
            textInputLayout.setError(null);
            verifyTransaction(transactionId);
        });

        scanButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
            intent.putExtra("SCAN_ORIENTATION_LOCKED", true);
            intent.putExtra("PROMPT_MESSAGE", "Scan QR Code");
            intent.putExtra("BEEP_ENABLED", true);
            scanLauncher.launch(intent);
        });

        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            intent.putParcelableArrayListExtra("historyList", historyList);
            startActivity(intent);
        });
    }

    private void verifyTransaction(String transactionId) {
        Query query = databaseReference.orderByChild("transactionId").equalTo(transactionId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                TextView resultTextView = findViewById(R.id.resultTextView);
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String sender = snapshot.child("sender").getValue(String.class);
                    Long timestampLong = snapshot.child("timestamp").getValue(Long.class);
                    if (sender != null && timestampLong != null) {
                        String timestamp = String.valueOf(timestampLong); // Convert Long to String for display
                        HistoryItem item = new HistoryItem(transactionId, "Verified", timestamp);
                        historyList.add(0, item);
                        Toast.makeText(MainActivity.this, "Verification Successful: Verified", Toast.LENGTH_SHORT).show();
                        resultTextView.setVisibility(View.VISIBLE);
                        resultTextView.setText("Sender: " + sender + "\nTimestamp: " + timestamp);
                        return;
                    }
                }
                Toast.makeText(MainActivity.this, "Verification Failed: Invalid ID", Toast.LENGTH_SHORT).show();
                resultTextView.setVisibility(View.VISIBLE);
                resultTextView.setText("Invalid Transaction ID");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                TextView resultTextView = findViewById(R.id.resultTextView);
                resultTextView.setVisibility(View.VISIBLE);
                resultTextView.setText("Error: " + databaseError.getMessage());
            }
        });
    }
}