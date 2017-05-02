package goldie.allegretto;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import goldie.allegretto.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.support.v7.app.AppCompatActivity;

public class Register extends AppCompatActivity {

    // UI
    ProgressBar progress;

    private SharedPreferences pref;
    Constants session;

    // Firebase Authorization
    private static final String TAG = "EmailPassword";
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    // database instance
    private DatabaseReference mDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FontsOverride.setDefaultFont(this, "SERIF", "ABeeZee-Regular.ttf");
        FontsOverride.setDefaultFont(this, "SANS_SERIF", "ABeeZee-Regular.ttf");
        FontsOverride.setDefaultFont(this, "DEFAULT", "ABeeZee-Regular.ttf");

        // set the content view
        setContentView(R.layout.activity_register);

        pref = getPreferences(0);
        session = new Constants(this.getApplicationContext());

        progress = (ProgressBar) findViewById(R.id.register_progress);
        initializeFirebase();
    }

    private void initializeFirebase(){
        mAuth = FirebaseAuth.getInstance();
        mAuth.signOut(); // get rid of this

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    goToHealthProfile();
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
    }

    public void clickRegister(View v){
        EditText et_first_name = (EditText) findViewById(R.id.first_name);
        EditText et_last_name = (EditText) findViewById(R.id.last_name);
        EditText et_email = (EditText) findViewById(R.id.email);
        EditText et_password = (EditText)findViewById(R.id.password);

        String first_name = et_first_name.getText().toString();
        String last_name = et_last_name.getText().toString();
        String email = et_email.getText().toString();
        String password = et_password.getText().toString();

        if(!first_name.isEmpty() && !last_name.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
            progress.setVisibility(View.VISIBLE);
            registerProcess(first_name, last_name, email, password);
        } else if (first_name.isEmpty()) {
            Snackbar.make(v, "First Name is required!", Snackbar.LENGTH_LONG).show();
        } else if (last_name.isEmpty()){
            Snackbar.make(v, "Last Name is required!", Snackbar.LENGTH_LONG).show();
        } else if (email.isEmpty()){
            Snackbar.make(v, "Email is required!", Snackbar.LENGTH_LONG).show();
        } else if (password.isEmpty()){
            Snackbar.make(v, "Password is required!", Snackbar.LENGTH_LONG).show();
        }
    }

    private void registerProcess(final String first_name, final String last_name, final String email, String password){
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            // save the users name in the database
                            String UserID = task.getResult().getUser().getUid();
                            User user = new User(first_name, last_name, email);
                            mDatabase.child("users").child(UserID).setValue(user);

                            // create a log-in session
                            session.createLoginSession(first_name + " "+ last_name, email, UserID);

                            // Send user an email confirming account
                            //sendEmailVerification();

                            // Navigate to profile
                            goToHealthProfile();
                        }

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthWeakPasswordException e){
                                displayToast(R.string.invalid_password);
                            } catch (FirebaseAuthUserCollisionException e) {
                                displayToast(R.string.user_exists);
                            } catch (Exception e){
                                displayToast(R.string.auth_failed);
                            }
                            progress.setVisibility(View.GONE);
                        }
                    }
                });
    }

    public void goToLogin(View view){
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
    }

    private void goToHealthProfile(){
        Intent intent = new Intent(this, HealthProfile.class);
        startActivity(intent);
    }

    private void displayToast (int message){
        Toast.makeText(this, message,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
    }

}
