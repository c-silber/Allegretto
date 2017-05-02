package goldie.allegretto;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
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

public class RegisterFragment extends Fragment implements View.OnClickListener{
    private AppCompatButton btn_register;
    private EditText et_email,et_password,et_first_name, et_last_name;
    private TextView tv_login;
    private ProgressBar progress;

    private static final String TAG = "EmailPassword";
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    Constants session;
    private SharedPreferences pref;

    // Database reference
    private DatabaseReference mDatabase;
    Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = container.getContext();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

        View view = inflater.inflate(R.layout.fragment_register, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view){
        mAuth.addAuthStateListener(mAuthListener);
        session = new Constants(getActivity().getApplicationContext());
        pref = getActivity().getPreferences(0);

        btn_register = (AppCompatButton)view.findViewById(R.id.register_button);
        tv_login = (TextView)view.findViewById(R.id.login_link);
        et_first_name = (EditText)view.findViewById(R.id.first_name);
        et_last_name = (EditText)view.findViewById(R.id.last_name);
        et_email = (EditText)view.findViewById(R.id.email);
        et_password = (EditText)view.findViewById(R.id.password);

        progress = (ProgressBar)view.findViewById(R.id.register_progress);

        btn_register.setOnClickListener(this);
        tv_login.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.login_link:
               goToLogin();
               break;

            case R.id.register_button:
                String first_name = et_first_name.getText().toString();
                String last_name = et_last_name.getText().toString();
                String email = et_email.getText().toString();
                String password = et_password.getText().toString();

                if(!first_name.isEmpty() && !last_name.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
                    progress.setVisibility(View.VISIBLE);
                    registerProcess(first_name, last_name, email, password);
                } else if (first_name.isEmpty()) {
                    Snackbar.make(getView(), "First Name is required!", Snackbar.LENGTH_LONG).show();
                } else if (last_name.isEmpty()){
                    Snackbar.make(getView(), "Last Name is required!", Snackbar.LENGTH_LONG).show();
                } else if (email.isEmpty()){
                    Snackbar.make(getView(), "Email is required!", Snackbar.LENGTH_LONG).show();
                } else if (password.isEmpty()){
                    Snackbar.make(getView(), "Password is required!", Snackbar.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void registerProcess(final String first_name, final String last_name, final String email, String password){
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this.getActivity(), new OnCompleteListener<AuthResult>() {

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
                            Toast.makeText(getActivity(), R.string.invalid_password,
                                    Toast.LENGTH_SHORT).show();
                            progress.setVisibility(View.GONE);
                        } catch (FirebaseAuthUserCollisionException e) {
                            Toast.makeText(getActivity(), R.string.user_exists,
                                    Toast.LENGTH_SHORT).show();
                            progress.setVisibility(View.GONE);
                        } catch (Exception e){
                            Toast.makeText(getActivity(), R.string.auth_failed,
                                    Toast.LENGTH_SHORT).show();
                            progress.setVisibility(View.GONE);
                        }
                    }
                }
            });
    }

    private void goToLogin(){
        Fragment login = new LoginFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_frame, login);
        ft.commit();
    }

    private void goToHealthProfile(){
        Intent intent = new Intent(context, HealthProfile.class);
        startActivity(intent);
    }

    private void sendEmailVerification() {
        //final FirebaseUser user = mAuth.getCurrentUser();
        //user.sendEmailVerification();
    }
}
