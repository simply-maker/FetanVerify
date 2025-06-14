package com.example.fetanverify;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

public class VerificationPopup {
    
    public static void showSuccessPopup(Context context, String transactionId, String sender, String timestamp) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.popup_verification_result);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        // Set dialog properties
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(params);
        
        // Find views
        ImageView statusIcon = dialog.findViewById(R.id.statusIcon);
        TextView statusTitle = dialog.findViewById(R.id.statusTitle);
        TextView statusMessage = dialog.findViewById(R.id.statusMessage);
        TextView transactionDetails = dialog.findViewById(R.id.transactionDetails);
        MaterialButton okButton = dialog.findViewById(R.id.okButton);
        
        // Set success content
        statusIcon.setImageResource(R.drawable.ic_check_circle);
        statusIcon.setColorFilter(Color.parseColor("#4CAF50")); // Green
        statusTitle.setText("Verification Successful");
        statusTitle.setTextColor(Color.parseColor("#4CAF50"));
        statusMessage.setText("Transaction ID has been verified successfully!");
        
        String details = "Transaction ID: " + transactionId + "\n" +
                        "Sender: " + sender + "\n" +
                        "Timestamp: " + timestamp;
        transactionDetails.setText(details);
        transactionDetails.setVisibility(View.VISIBLE);
        
        okButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    public static void showErrorPopup(Context context, String transactionId) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.popup_verification_result);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        // Set dialog properties
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(params);
        
        // Find views
        ImageView statusIcon = dialog.findViewById(R.id.statusIcon);
        TextView statusTitle = dialog.findViewById(R.id.statusTitle);
        TextView statusMessage = dialog.findViewById(R.id.statusMessage);
        TextView transactionDetails = dialog.findViewById(R.id.transactionDetails);
        MaterialButton okButton = dialog.findViewById(R.id.okButton);
        
        // Set error content
        statusIcon.setImageResource(R.drawable.ic_error_circle);
        statusIcon.setColorFilter(Color.parseColor("#F44336")); // Red
        statusTitle.setText("Verification Failed");
        statusTitle.setTextColor(Color.parseColor("#F44336"));
        statusMessage.setText("Transaction ID could not be verified. Please check and try again.");
        
        if (!transactionId.equals("Database Error")) {
            transactionDetails.setText("Transaction ID: " + transactionId);
            transactionDetails.setVisibility(View.VISIBLE);
        } else {
            transactionDetails.setVisibility(View.GONE);
        }
        
        okButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
}