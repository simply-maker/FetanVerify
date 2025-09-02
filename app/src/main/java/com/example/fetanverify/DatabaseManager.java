package com.example.fetanverify;

import android.content.Context;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages database operations for verified transactions
 */
public class DatabaseManager {
    private static final String TAG = "DatabaseManager";
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    
    public interface VerificationCallback {
        void onVerificationResult(boolean isVerified, String sender, String timestamp, String amount, boolean isNewVerification);
        void onError(String error);
    }
    
    public DatabaseManager() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUser.getUid()).child("verified_transactions");
        }
    }
    
    /**
     * Save a verified transaction to the database
     */
    public void saveVerifiedTransaction(String transactionId, String sender, String amount, long timestamp) {
        if (databaseReference == null) {
            Log.e(TAG, "Database reference is null");
            return;
        }
        
        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("transactionId", transactionId.toUpperCase());
        transactionData.put("sender", sender);
        transactionData.put("amount", amount);
        transactionData.put("timestamp", timestamp);
        transactionData.put("verifiedAt", System.currentTimeMillis());
        
        databaseReference.child(transactionId.toUpperCase()).setValue(transactionData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Transaction saved to database: " + transactionId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save transaction: " + e.getMessage()));
    }
    
    /**
     * Check if a transaction is already verified in the database
     */
    public void checkVerifiedTransaction(String transactionId, VerificationCallback callback) {
        if (databaseReference == null) {
            callback.onError("Database not available");
            return;
        }
        
        String upperTransactionId = transactionId.toUpperCase();
        databaseReference.child(upperTransactionId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Transaction is already verified
                    String sender = dataSnapshot.child("sender").getValue(String.class);
                    String amount = dataSnapshot.child("amount").getValue(String.class);
                    Long timestampLong = dataSnapshot.child("timestamp").getValue(Long.class);
                    String timestamp = timestampLong != null ? 
                            new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault())
                                    .format(new java.util.Date(timestampLong)) : "N/A";
                    
                    callback.onVerificationResult(true, sender, timestamp, amount, false);
                } else {
                    // Transaction not found in verified database, proceed with SMS verification
                    callback.onVerificationResult(false, null, null, null, true);
                }
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError(databaseError.getMessage());
            }
        });
    }
}