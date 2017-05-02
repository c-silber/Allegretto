package goldie.allegretto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MusicPref extends AppCompatActivity implements OnItemSelectedListener, View.OnClickListener {
    String musicPref = "Music Preference";

    Boolean getColor = true;
    int colored = 0;
    HashMap<String, Object> workoutData;
    Constants session;
    private SharedPreferences pref;

    TableLayout table;

    // buttons
    private AppCompatButton btn_submit;

    public void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.ThemeForProfile);

        session = new Constants(this.getApplicationContext());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musicpref);

        musicPref = getIntent().getStringExtra("musicPreference");

        TextView welcomeMessage = (TextView) findViewById(R.id.welcome);
        String welcomeUser = "Welcome " + session.getName();
        welcomeMessage.setText(welcomeUser);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        // Spinner Element
        Spinner music_pref = (Spinner) findViewById(R.id.music_pref);

        // On click listener
        music_pref.setOnItemSelectedListener(this);

        List<String> mGenre = new ArrayList<String>();
        String[] genre = {musicPref, "chill", "country", "dance", "disco", "disney", "electronic", "funk", "happy", "hard-rock", "heavy-metal",
                "hip-hop", "latino", "metal", "new-release", "party", "pop", "r-n-b", "reggae", "rock", "rock-n-roll", "work-out"};

        mGenre.addAll(Arrays.asList(genre));

        // Create the adaptor for the spinner
        ArrayAdapter<String> dataAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mGenre);

        //  Attaching the data adapter
        music_pref.setAdapter(dataAdapter1);

        btn_submit = (AppCompatButton) findViewById(R.id.submit);
        btn_submit.setOnClickListener(this);

        getWorkoutData();
    }

    public void getWorkoutData(){
        workoutData = new HashMap<String, Object>();

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

        try {
            mDatabase.child(session.getId()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange (DataSnapshot snapshot){
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        Log.d("SNAP: ", snap.toString());
                        Log.d("USER:", session.getId());
                        workoutData.put(snap.getKey(), snap.getValue());
                    }
                    setUpTable();
                    }
                    public void onCancelled(DatabaseError error) {
                        Log.d("ERROR", "HERE");
                    }
            });
        } catch (Exception e) {
            Log.d("EXCEPTION: ", e.toString());
        }
    }

    public void setUpTable() {
        table = (TableLayout) findViewById(R.id.tblPrevious);

        setupTableRow("Start Time", "End Time", "Date", "Genre");

        for (Object val: workoutData.values()){
            String value = val.toString().substring(1, val.toString().length()-1);
            String [] workout = value.split(",");
            setupTableRow(getData(workout[0]), getData(workout[3]), getData(workout[2]), getData(workout[1]));
        }
    }

    private String getData(String data){
        return data.split("=")[1];
    }

    private void setupTableRow(String sStartTime, String sEndTime, String sDate, String sGenre){
        TableRow rowHeader = new TableRow(this);

        tableRow(rowHeader, sDate);
        tableRow(rowHeader, sStartTime);
        tableRow(rowHeader, sEndTime);
        tableRow(rowHeader, sGenre);

        table.addView(rowHeader);
    }

    private void tableRow(TableRow row, String text){
        TableRow.LayoutParams aParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
        aParams.topMargin = 2;
        aParams.rightMargin = 2;

        TextView var = new TextView(this);
        var.setLayoutParams(aParams);

        if (colored < 4) var.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        colored = colored < 7 ? colored+=1 : 0;

        var.setPadding(15, 15, 15, 15);
        var.setText(text);

        row.addView(var);
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // selecting a spinner item
        String itemSelected;
        itemSelected = parent.getItemAtPosition(position).toString();
        musicPref = itemSelected;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }

    @Override
    public void onClick(View view) {
        if (validateForm()) {
            session.setMusicPref(musicPref);
            Intent intent = new Intent(this, Auth.class);
            startActivity(intent);
        }
    }

    private boolean validateForm() {
        if (musicPref.equals("Music Preference")) {
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

