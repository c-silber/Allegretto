package goldie.allegretto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class HealthProfile extends AppCompatActivity implements OnItemSelectedListener, View.OnClickListener {
    EditText height, weight, age;
    String activityLevel = "Activity Level", musicPref = "Music Preference";

    Constants session;
    private SharedPreferences pref;

    // Database reference
    private DatabaseReference mDatabase;

    // buttons
    AppCompatButton btn_submit;

    public void onCreate (Bundle savedInstanceState){
        setTheme(R.style.ThemeForProfile);

        session = new Constants(this.getApplicationContext());
        mDatabase = FirebaseDatabase.getInstance().getReference();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.health_profile);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        // Spinner Element
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        Spinner music_pref = (Spinner) findViewById(R.id.music_pref);

        // On click listener
        spinner.setOnItemSelectedListener(this);
        music_pref.setOnItemSelectedListener(this);

        List <String> levels = new ArrayList<String>();
        String [] aLevels = {"Activity Level", "Extremely Inactive", "Sedentary", "Moderately Active", "Vigorously Active", "Extremely Active"};

        levels.addAll(Arrays.asList(aLevels));

        // Create the adaptor for the spinner
        ArrayAdapter <String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, levels);

        //  Attaching the data adapter
        spinner.setAdapter(dataAdapter);

        List <String> mGenre = new ArrayList<String>();
        String[] genre = {"Music Preference","chill","country","dance","disco","disney","electronic","funk","happy","hard-rock","heavy-metal",
        "hip-hop","latino","metal","new-release","party","pop","r-n-b","reggae","rock","rock-n-roll","work-out"};

        mGenre.addAll(Arrays.asList(genre));

        // Create the adaptor for the spinner
        ArrayAdapter <String> dataAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mGenre);

        //  Attaching the data adapter
        music_pref.setAdapter(dataAdapter1);

        btn_submit = (AppCompatButton) findViewById(R.id.submit);
        btn_submit.setOnClickListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
        // selecting a spinner item
        String itemSelected;
        itemSelected = parent.getItemAtPosition(position).toString();
        if (isActivity(itemSelected)){
            activityLevel = itemSelected;
        }
        else {
            musicPref = itemSelected;
        }
    }

    private boolean isActivity(String value){
        String [] activityLevels  = {"Activity Level", "Extremely Inactive", "Sedentary", "Moderately Active", "Vigorously Active", "Extremely Active"};
        return (Arrays.asList(activityLevels).contains(value));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }

    @Override
    public void onClick (View view){
        if (view.getId() == R.id.submit){
            age = (EditText) findViewById(R.id.age);

            String sAge = age.getText().toString();

            boolean isValid = validateForm(sAge);
            // set the music preference for this user
            session.setMusicPref(musicPref);

            if (isValid) {
                Map<String, String> activityMap = new HashMap<String, String>();
                activityMap.put("Age", sAge);
                activityMap.put("Activity Level", activityLevel);
                activityMap.put("Music Preference", musicPref);

                mDatabase.child("activityLevel").child(session.getId()).setValue(activityMap);
                Intent intent = new Intent(this, Auth.class);
                startActivity(intent);
            }
        }
    }

    private boolean validateForm(String age){
        int ageCheck;
        try {
            ageCheck = Integer.parseInt(age);
        }
        catch(NumberFormatException e){
            Toast.makeText(this, R.string.invalid_age,
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (ageCheck < 20){
            Toast.makeText(this, R.string.min_age,
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (activityLevel.equals("Activity Level")){
            Toast.makeText(this, "Please select an activity level",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (musicPref.equals("Music Preference")){
            Toast.makeText(this, "Please select a music preference",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    @Override
    public void onBackPressed() {
    }
}

