package goldie.allegretto;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Login extends AppCompatActivity {
    // UI
    ProgressBar progress;

    String musicPref = "Music Preference";
    SharedPreferences pref;
    Constants session;

    // Firebase Authorization
    private static final String TAG = "EmailPassword";
    private FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;

    // database instance
    private DatabaseReference mDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FontsOverride.setDefaultFont(this, "SERIF", "ABeeZee-Regular.ttf");
        FontsOverride.setDefaultFont(this, "SANS_SERIF", "ABeeZee-Regular.ttf");
        FontsOverride.setDefaultFont(this, "DEFAULT", "ABeeZee-Regular.ttf");

        // set the content view
        setContentView(R.layout.activity_logins);

        pref = getPreferences(0);
        session = new Constants(this.getApplicationContext());

        progress = (ProgressBar) findViewById(R.id.login_progress);

        initializeFirebase();
    }

    public void clickRegister(View view){
        Intent intent = new Intent(this, Register.class);
        startActivity(intent);
    }

    private void initializeFirebase(){
        mAuth = FirebaseAuth.getInstance();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    goToProfile();
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
    }

    private void goToProfile(){
        Intent intent = new Intent(this, MusicPref.class);
        Log.d("Passing for mPref: ", musicPref);
        intent.putExtra("musicPreference", musicPref);
        startActivity(intent);
    }

    public static Map<String, String> splitToMap(String source, String entriesSeparator, String keyValueSeparator) {
        Map<String, String> map = new HashMap<String, String>();
        String[] entries = source.split(entriesSeparator);
        for (String entry : entries) {
            if (!TextUtils.isEmpty(entry) && entry.contains(keyValueSeparator)) {
                String[] keyValue = entry.split(keyValueSeparator);
                map.put(keyValue[0], keyValue[1]);
            }
        }
        return map;
    }

    public void btnSignIn(View v){
        EditText et_email = (EditText)findViewById(R.id.email);
        EditText et_password = (EditText)findViewById(R.id.password);

        String email = et_email.getText().toString();
        String password = et_password.getText().toString();

        if(!email.isEmpty() && !password.isEmpty()) {

            progress.setVisibility(View.VISIBLE);
            signIn(email,password);

        } else if (email.isEmpty()){

            Snackbar.make(v, "Email is required!", Snackbar.LENGTH_LONG).show();
            progress.setVisibility(View.GONE);

        }
        else if (password.isEmpty()){

            Snackbar.make(v, "Password is required!", Snackbar.LENGTH_LONG).show();
            progress.setVisibility(View.GONE);

        }
    }

    private void signIn(final String email, String password) {
        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());
                        if (task.isSuccessful()){
                            final String UserID = task.getResult().getUser().getUid();
                            mDatabase.child("users").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange (DataSnapshot snapshot){
                                    List users = new ArrayList<>();
                                    for (DataSnapshot snap : snapshot.getChildren()) {
                                        if (snap.getKey().equals(UserID)) {
                                            String userInfo = snap.getValue().toString();
                                            int userLength = userInfo.length();
                                            Map<String, String> User = splitToMap(userInfo.substring(1, userLength - 1), ", ", "=");
                                            // create Login Session
                                            session.createLoginSession(User.get("name"), User.get("email"), UserID);
                                            getMusicPreference(session.getId());
                                        }
                                    }
                                }

                                public void onCancelled(DatabaseError error) {
                                    Log.d("ERROR", "HERE");
                                }
                            });
                        }
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            displayToast(R.string.auth_failed);
                            Log.w(TAG, "signInWithEmail:failed", task.getException());
                            progress.setVisibility(View.GONE);
                        }
                        // [END_EXCLUDE]
                    }
                });


        // [END sign_in_with_email]
    }

    private void displayToast(int message){
        Toast.makeText(this, message,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
    }

    public void getMusicPreference(final String UserID){
        try {
            mDatabase.child("activityLevel").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange (DataSnapshot snapshot){
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        if (snap.getKey().equals(UserID)) {
                            String userInfo = snap.getValue().toString();
                            int userLength = userInfo.length();
                            Map<String, String> User = splitToMap(userInfo.substring(1, userLength - 1), ", ", "=");
                            musicPref = User.get("Music Preference");
                            Log.d("mPREF:", musicPref);
                            goToProfile();
                        }
                    }
                }
                public void onCancelled(DatabaseError error) {
                    Log.d("ERROR", "HERE");
                }
            });

        } catch (Exception e) {
            Log.d("EXCEPTION: ", e.toString());
        }

    }
}
