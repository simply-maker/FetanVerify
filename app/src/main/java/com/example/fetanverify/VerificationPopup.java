package com.example.fetanverify;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

public class VerificationPopup {
    
    public static void showSuccessPopup(Context context, String transactionId, String sender, String timestamp, String amount) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.popup_verification_result);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(params);
        
        ImageView statusIcon = dialog.findViewById(R.id.statusIcon);
        TextView statusTitle = dialog.findViewById(R.id.statusTitle);
        TextView statusMessage = dialog.findViewById(R.id.statusMessage);
        TextView transactionDetails = dialog.findViewById(R.id.transactionDetails);
        MaterialButton okButton = dialog.findViewById(R.id.okButton);
        
        statusIcon.setImageResource(R.drawable.ic_check_circle);
        statusIcon.setColorFilter(Color.parseColor("#1DB584"));
        statusTitle.setText("✓ Verification Successful");
        statusTitle.setTextColor(Color.parseColor("#1DB584"));
        statusMessage.setText("Transaction has been verified successfully!");
        
        String details = "Transaction ID: " + transactionId + "\n" +
                        "Sender: " + sender + "\n" +
                        "Amount: " + (amount != null ? amount : "N/A") + "\n" +
                        "Timestamp: " + timestamp;
        transactionDetails.setText(details);
        transactionDetails.setVisibility(android.view.View.VISIBLE);
        
        okButton.setBackgroundColor(Color.parseColor("#1DB584"));
        okButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    public static void showErrorPopup(Context context, String transactionId) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.popup_verification_result);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(params);
        
        ImageView statusIcon = dialog.findViewById(R.id.statusIcon);
        TextView statusTitle = dialog.findViewById(R.id.statusTitle);
        TextView statusMessage = dialog.findViewById(R.id.statusMessage);
        TextView transactionDetails = dialog.findViewById(R.id.transactionDetails);
        MaterialButton okButton = dialog.findViewById(R.id.okButton);
        
        statusIcon.setImageResource(R.drawable.ic_error_circle);
        statusIcon.setColorFilter(Color.parseColor("#E53E3E"));
        statusTitle.setText("✗ Verification Failed");
        statusTitle.setTextColor(Color.parseColor("#E53E3E"));
        statusMessage.setText("Transaction could not be verified. Please check the ID and try again.");
        
        if (!transactionId.equals("Database Error")) {
            transactionDetails.setText("Transaction ID: " + transactionId);
            transactionDetails.setVisibility(android.view.View.VISIBLE);
        } else {
            transactionDetails.setVisibility(android.view.View.GONE);
        }
        
        okButton.setBackgroundColor(Color.parseColor("#E53E3E"));
        okButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
}