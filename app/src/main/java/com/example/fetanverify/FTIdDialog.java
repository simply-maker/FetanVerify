package com.example.fetanverify;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.journeyapps.barcodescanner.CaptureActivity;
import java.util.regex.Pattern;

public class FTIdDialog {
    private static final Pattern FT_PATTERN = Pattern.compile("^FT[A-Z0-9]{12}$");
    private static final Pattern CH_PATTERN = Pattern.compile("^CH[A-Z0-9]{8}$");

    public interface FTIdCallback {
        void onFTIdEntered(String ftId);
    }

    public static void showFTIdDialog(Context context, FTIdCallback callback) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_ft_id);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(params);

        TextView titleText = dialog.findViewById(R.id.dialogTitle);
        TextView messageText = dialog.findViewById(R.id.dialogMessage);
        TextInputLayout ftIdLayout = dialog.findViewById(R.id.ftIdLayout);
        TextInputEditText ftIdEditText = dialog.findViewById(R.id.ftIdEditText);
        MaterialButton enterManuallyButton = dialog.findViewById(R.id.enterManuallyButton);
        MaterialButton cancelButton = dialog.findViewById(R.id.cancelButton);

        titleText.setText(R.string.qr_verification_failed);
        messageText.setText(R.string.qr_failed_message);
        
        // Set input to always uppercase
        ftIdEditText.setFilters(new android.text.InputFilter[] {
            new android.text.InputFilter.AllCaps()
        });

        enterManuallyButton.setOnClickListener(v -> {
            String ftId = ftIdEditText.getText().toString().trim().toUpperCase();

            if (TextUtils.isEmpty(ftId)) {
                ftIdLayout.setError(context.getString(R.string.enter_ft_id));
                return;
            }

            // Clean the input - remove any non-alphanumeric characters
            ftId = ftId.replaceAll("[^A-Z0-9]", "");

            // Auto-detect and add prefix if missing
            if (ftId.length() == 8 && !ftId.startsWith("CH") && !ftId.startsWith("FT")) {
                ftId = "CH" + ftId; // Assume CH for 8-character input
            } else if (ftId.length() == 12 && !ftId.startsWith("FT") && !ftId.startsWith("CH")) {
                ftId = "FT" + ftId; // Assume FT for 12-character input
            }

            // Fix CHA prefix to CH
            if (ftId.startsWith("CHA")) {
                ftId = "CH" + ftId.substring(3);
            }

            if (!TransactionExtractor.isValidTransactionId(ftId)) {
                ftIdLayout.setError(context.getString(R.string.invalid_ft_format));
                return;
            }

            ftIdLayout.setError(null);
            callback.onFTIdEntered(ftId);
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}