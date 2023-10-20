package ru.accesstech.opendooors;

import static android.text.InputType.TYPE_CLASS_NUMBER;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

import ru.tinkoff.decoro.MaskImpl;
import ru.tinkoff.decoro.slots.PredefinedSlots;
import ru.tinkoff.decoro.watchers.MaskFormatWatcher;

public class AuthorizationScreen extends AppCompatActivity {

    //private static final String TAG = "PhoneAuthActivity";

    //Create auth variables
    // [START declare_auth_variables]
    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String phoneNumber;
    private String code;
    private Boolean phoneIsEntered = false;
    // [END declare_auth_variables]

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create Activity dependency
        // [START declare_and_initialize_elements]
        setContentView(R.layout.activity_authorization_screen);
        TextView data_field_header = findViewById(R.id.data_input_field_header);
        EditText data_input_field = findViewById(R.id.data_input_field);
        Button authorization_button = findViewById(R.id.authorization_button);
        // [END declare_and_initialize_elements]

        //Phone field mask
        // [START phone_field_mask_setting]
        MaskImpl mask = MaskImpl.createTerminated(PredefinedSlots.RUS_PHONE_NUMBER);
        mask.setHideHardcodedHead(true);
        MaskFormatWatcher formatWatcher = new MaskFormatWatcher(mask);
        formatWatcher.installOn(data_input_field);
        // [END phone_field_mask_setting]

        // Initialize Firebase Auth
        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]

        // Initialize phone auth callbacks
        // [START phone_auth_callbacks]
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                //Log.d(TAG, "onVerificationCompleted:" + credential);
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                //Log.w(TAG, "onVerificationFailed", e);

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                } else if (e instanceof FirebaseAuthMissingActivityForRecaptchaException) {
                    // reCAPTCHA verification attempted with null Activity
                }

                // Show a message and update the UI
                Toast.makeText(AuthorizationScreen.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                //Log.d(TAG, "onCodeSent:" + verificationId);

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                // Change fields setting for entering code
                data_field_header.setText(R.string.password_input_field_header_text);
                data_input_field.setInputType(TYPE_CLASS_NUMBER);
                formatWatcher.removeFromTextView();
                data_input_field.setText("");
                data_input_field.setHint("");
                phoneIsEntered = true;
            }
        };
        // [END phone_auth_callbacks]

        //Authorization
        //[START auth_process]
        authorization_button.setOnClickListener(v -> {
            if(!phoneIsEntered) {
                phoneNumber = data_input_field.getText().toString();
                if (phoneNumber.isEmpty()) {
                    Toast.makeText(this, "Введите номер телефона", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (phoneNumber.length() < 12) {
                    Toast.makeText(this, "Неверный формат номера телефона", Toast.LENGTH_SHORT).show();
                    return;
                }
                startPhoneNumberVerification(phoneNumber);
            } else {
                 code = data_input_field.getText().toString();
                if (code.isEmpty()) {
                    Toast.makeText(this, "Введите код из смс", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (code.length() != 6) {
                    Toast.makeText(this, "Неверный пароль", Toast.LENGTH_SHORT).show();
                    return;
                }
                verifyPhoneNumberWithCode(mVerificationId, code);
            }
        });
        //[END auth_process]
    }

    // [START on_start_check_user]
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }
    // [END on_start_check_user]


    private void startPhoneNumberVerification(String phoneNumber) {
        // [START phone_auth]
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)               // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS)         // Timeout and unit
                        .setActivity(this)                         // (optional) Activity for callback binding
                        .setCallbacks(mCallbacks)                  // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        // [END phone_auth]
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        // [START verify_with_code]
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        // [END verify_with_code]
        signInWithPhoneAuthCredential(credential);
    }

    // [START resend_verification]
    private void resendVerificationCode(String phoneNumber,
                                        PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)               // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS)         // Timeout and unit
                        .setActivity(this)                         // Activity (for callback binding)
                        .setCallbacks(mCallbacks)                  // OnVerificationStateChangedCallbacks
                        .setForceResendingToken(token)             // ForceResendingToken from callbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }
    // [END resend_verification]

    // [START sign_in_with_phone]
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            // Log.d(TAG, "signInWithCredential:success");

                            FirebaseUser user = task.getResult().getUser();
                            // Update UI
                            updateUI(user);
                        } else {
                            // Sign in failed, display a message and update the UI
                            // Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                resendVerificationCode(phoneNumber, mResendToken);
                                // Update UI
                                updateUI(null);
                            }
                        }
                    }
                });
    }
    // [END sign_in_with_phone]

    private void updateUI(FirebaseUser user) {
        if(user != null) {
            //Redirect on Main Activity screen
            Intent intent = new Intent(AuthorizationScreen.this, MainActivity.class);
            AuthorizationScreen.this.startActivity(intent);
            AuthorizationScreen.this.finish();
        }
    }
}