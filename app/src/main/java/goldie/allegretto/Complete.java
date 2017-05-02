package goldie.allegretto;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;

import retrofit.RetrofitError;
import retrofit.client.Response;

public class Complete extends AppCompatActivity {
    Toolbar toolbar;
    String accessToken;
    String uri;
    AppCompatButton createPlaylist;

    // Shared Preferences for the app
    Constants session;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accessToken = getIntent().getStringExtra("sAccess");
        ArrayList<Integer> hrData = (ArrayList<Integer>) getIntent().getSerializableExtra("hrData");
        ArrayList<Float> tempoData = (ArrayList<Float>) getIntent().getSerializableExtra("tempoData");
        ArrayList<String> trackData = (ArrayList<String>) getIntent().getSerializableExtra("playlistData");

        // set the theme so we have the title bar
        setTheme(R.style.ThemeForProfile);
        // set the content view
        setContentView(R.layout.activity_complete);

        // initialize the toolbar that contains the app name
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        GraphView hrGraph = (GraphView) findViewById(R.id.hrGraph);
        GraphView tempoGraph = (GraphView) findViewById(R.id.tempoGraph);

        CardView hrCard = (CardView) findViewById(R.id.hrCard);

        DataPoint[] hrPoints = new DataPoint[hrData.size()];
        DataPoint[] tempoPoints = new DataPoint[tempoData.size()];

        Integer time = 0;
        for (int i = 0; i < hrData.size(); i++) {
            hrPoints[i] = new DataPoint(time, hrData.get(i));
            time += 1;
        }

        time = 0;
        for (int i = 0; i < tempoData.size(); i++) {
            Log.d("TEMPO:", tempoData.get(i).toString());
            tempoPoints[i] = new DataPoint(time, tempoData.get(i));
            time += 1;
        }

        plotGraphData(hrGraph, hrPoints, "Heart Rate Graph", "Heart Rate", Color.RED);
        plotGraphData(tempoGraph, tempoPoints, "Tempo Graph", "Tempo", Color.BLUE);

        int wColor = 0;
        for (int i = 0; i < trackData.size(); i++){
            String[] trackInfo = trackData.get(i).split("&");
            int color = wColor%2 == 0 ? Color.WHITE : Color.LTGRAY;
            uri = i == 0 ? uri = trackInfo[0] : (uri += "," + trackInfo[0]);
            wColor++;
            generatePlaylist(trackInfo[1], trackInfo[2], i, color);
        }

        if (hrData.size() == 0){
            hrCard.setVisibility(View.GONE);
        }
    }

    public void plotGraphData(GraphView graph, DataPoint [] dataPoints, String title, String vAxis, int color){
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints);
        graph.addSeries(series);

        graph.setTitle(title);
        series.setColor(color);

        GridLabelRenderer gridLabel = graph.getGridLabelRenderer();
        gridLabel.setHorizontalAxisTitle("Time");
        gridLabel.setVerticalAxisTitle(vAxis);
    }

    private void generatePlaylist(String trackName, String artistName, int index, int color){
        CardView cv = new CardView(this);
        cv.setId(index);
        cv.setBackgroundColor(color);
        cv.setMinimumHeight(100);
        cv.setRadius(50);
        cv.setCardElevation(50);
        cv.setContentPadding(40, 40, 40, 40);

        TextView txtTrack = new TextView(this);
        txtTrack.setText(trackName + "\n" + artistName);
        txtTrack.setTextSize(16);
        txtTrack.setGravity(Gravity.CENTER);
        cv.addView(txtTrack);

        LinearLayout sv = (LinearLayout) findViewById(R.id.lComplete);
        sv.addView(cv);
    }

    public void createPlaylist(View view){
        createPlaylist = (AppCompatButton) findViewById(R.id.btnPlaylist);
        Thread hrThread = new Thread (new Runnable () {
            @Override
            public void run() {
                try {
                    SpotifyApi api = new SpotifyApi();
                    api.setAccessToken(accessToken);
                    SpotifyService service = api.getService();

                    UserPrivate User = service.getMe();
                    String date = new SimpleDateFormat("MM/dd HH:mm").format(Calendar.getInstance().getTime());

                    Map<String, Object> options = new HashMap<>();
                    options.put("name", "Allegretto Playlist - " + date);
                    options.put("public", "false");
                    options.put("description", "Playlist generated by Allegretto");

                    Playlist newPlaylist = service.createPlaylist(User.id, options);

                    Map <String, Object> qParam = new HashMap<>();
                    qParam.put("uris", uri);

                    Callback callback = new Callback() {
                        @Override
                        public void success(Object o, Response response) {
                            toast("Created Successfully!");
                            createPlaylist.setVisibility(View.GONE);
                        }

                        @Override
                        public void failure(RetrofitError retrofitError) {
                            toast(retrofitError.getMessage());
                        }
                    };

                    service.addTracksToPlaylist(User.id, newPlaylist.id, qParam, qParam, callback);
                } catch (Exception e) {
                    Log.d("EXCEPTION: ", e.toString());
                }
            }
        });
        hrThread.start();
    }

    public void toast(String message){
        Toast.makeText(this, message,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MusicPref.class);
        intent.putExtra("musicPreference", "Music Preference");
        startActivity(intent);
    }

}
