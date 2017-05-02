package goldie.allegretto;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the font for the application
        FontsOverride.setDefaultFont(this, "SERIF", "ABeeZee-Regular.ttf");
        FontsOverride.setDefaultFont(this, "SANS_SERIF", "ABeeZee-Regular.ttf");
        FontsOverride.setDefaultFont(this, "DEFAULT", "ABeeZee-Regular.ttf");
        setContentView(R.layout.activity_main);
        pref = getPreferences(0);
        initFragment();
    }

    private void initFragment(){
        if (pref.getBoolean(Constants.IS_LOGGED_IN, false)){
            Fragment fragment;
            fragment = new LoginFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_frame, fragment);
            ft.commit();
        } else {
           Fragment fragment;
           fragment = new LoginFragment();
           FragmentTransaction ft = getFragmentManager().beginTransaction();
           ft.replace(R.id.fragment_frame, fragment);
           ft.commit();
        }
    }

    public void clickRegister(View view){
        Log.d("CLICKED", "HI");
        Fragment fragment;
        fragment = new RegisterFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_frame, fragment);
        ft.commit();
    }
}