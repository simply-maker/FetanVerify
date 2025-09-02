package com.example.fetanverify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText emailEditText, passwordEditText;
    private TextInputLayout emailLayout, passwordLayout;
    private MaterialButton loginButton;
    private CheckBox rememberMeCheckBox;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private static final String REMEMBER_PREFS = "RememberPrefs";
    private static final String REMEMBER_EMAIL = "email";
    private static final String REMEMBER_PASSWORD = "password";
    private static final String REMEMBER_CHECKED = "rememberChecked";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply language before setting content view
        LanguageHelper.applyLanguage(this);
        
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(REMEMBER_PREFS, MODE_PRIVATE);
        
        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        boolean stayLoggedIn = sharedPreferences.getBoolean("stay_logged_in", false);
        
        if (currentUser != null && stayLoggedIn) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);

        // Load remembered credentials
        loadRememberedCredentials();

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                emailLayout.setError("Enter email");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                passwordLayout.setError("Enter password");
                return;
            }

            emailLayout.setError(null);
            passwordLayout.setError(null);

            loginButton.setEnabled(false);
            loginButton.setText("Logging in...");

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        loginButton.setEnabled(true);
                        loginButton.setText("Login");
                        
                        if (task.isSuccessful()) {
                            // Save login state
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("stay_logged_in", true);
                            
                            // Save credentials if remember me is checked
                            if (rememberMeCheckBox.isChecked()) {
                                editor.putString(REMEMBER_EMAIL, email);
                                editor.putString(REMEMBER_PASSWORD, password);
                                editor.putBoolean(REMEMBER_CHECKED, true);
                            } else {
                                // Clear remembered credentials if unchecked
                                editor.remove(REMEMBER_EMAIL);
                                editor.remove(REMEMBER_PASSWORD);
                                editor.putBoolean(REMEMBER_CHECKED, false);
                            }
                            editor.apply();
                            
                            // Load global verified IDs on successful login
                            loadGlobalVerifiedIdsOnLogin();
                            
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, getString(R.string.login_failed, task.getException().getMessage()), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void loadRememberedCredentials() {
        String rememberedEmail = sharedPreferences.getString(REMEMBER_EMAIL, "");
        String rememberedPassword = sharedPreferences.getString(REMEMBER_PASSWORD, "");
        boolean wasRememberChecked = sharedPreferences.getBoolean(REMEMBER_CHECKED, false);
        
        if (!rememberedEmail.isEmpty()) {
            emailEditText.setText(rememberedEmail);
        }
        if (!rememberedPassword.isEmpty()) {
            passwordEditText.setText(rememberedPassword);
        }
        rememberMeCheckBox.setChecked(wasRememberChecked);
    }
    
    private void loadGlobalVerifiedIdsOnLogin() {
        // This method ensures global verified IDs are available immediately after login
        // The actual loading will happen in MainActivity.onCreate()
    }
}