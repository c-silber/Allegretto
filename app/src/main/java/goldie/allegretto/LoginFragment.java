package goldie.allegretto;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.AppCompatButton;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
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

public class LoginFragment extends Fragment implements View.OnClickListener {

    AppCompatButton btn_login;
    TextView tv_register;
    private EditText et_email, et_password;
    private ProgressBar progress;

    private static final String TAG = "EmailPassword";
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    // database instance
    private DatabaseReference mDatabase;

    Constants session;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
                    goToProfile();
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

        View view = inflater.inflate(R.layout.fragment_login, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view){
        mAuth.addAuthStateListener(mAuthListener);
        session = new Constants(getActivity().getApplicationContext());

        btn_login = (AppCompatButton)view.findViewById(R.id.email_sign_in_button);
        tv_register = (TextView)view.findViewById(R.id.register);
        et_email = (EditText)view.findViewById(R.id.email);
        et_password = (EditText)view.findViewById(R.id.password);

        progress = (ProgressBar)view.findViewById(R.id.login_progress);

        btn_login.setOnClickListener(this);
        tv_register.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.email_sign_in_button:
                String email = et_email.getText().toString();
                String password = et_password.getText().toString();

                if(!email.isEmpty() && !password.isEmpty()) {
                    progress.setVisibility(View.VISIBLE);
                    signIn(email,password);

                } else if (email.isEmpty()){
                    Snackbar.make(getView(), "Email is required!", Snackbar.LENGTH_LONG).show();
                    progress.setVisibility(View.GONE);
                }
                else if (password.isEmpty()){
                    Snackbar.make(getView(), "Password is required!", Snackbar.LENGTH_LONG).show();
                    progress.setVisibility(View.GONE);
                }
                break;
        }
    }

    private void signIn(final String email, String password) {
        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this.getActivity(), new OnCompleteListener<AuthResult>() {
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
                                        }
                                    }
                                }

                                public void onCancelled(DatabaseError error) {
                                    Log.d("ERROR", "HERE");
                                }
                            });
                            goToProfile();
                        }
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail:failed", task.getException());
                            Toast.makeText(getActivity(), R.string.auth_failed,
                                    Toast.LENGTH_SHORT).show();
                            progress.setVisibility(View.GONE);
                        }
                    }
                });
        // [END sign_in_with_email]
    }

    private void goToProfile(){
        Intent intent = new Intent(getActivity(), Music.class);
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

}
