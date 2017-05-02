package goldie.allegretto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.os.Handler;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.AudioFeaturesTrack;
import kaaes.spotify.webapi.android.models.Recommendations;
import kaaes.spotify.webapi.android.models.Track;

import com.fitbit.authentication.AuthenticationManager;
import com.fitbit.fitbitcommon.network.BasicHttpRequest;
import com.fitbit.fitbitcommon.network.BasicHttpResponse;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Connectivity;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Music extends AppCompatActivity implements
        Player.NotificationCallback, ConnectionStateCallback {

    ArrayList<Float> tempoData = new ArrayList<Float>();
    ArrayList<Integer> hrData = new ArrayList<Integer>();
    ArrayList<String> playList = new ArrayList<String>();

    Timer timer;
    String startTime, endTime;
    TimerTask doAsynchronousTask;

    Toolbar toolbar;
    Constants session;

    // UI Elements
    RelativeLayout buttons;
    RelativeLayout cards;
    TextView selectWorkout;
    Button prevButton;
    Button nextButton;
    ProgressBar playlistProgress;

    private static final String CLIENT_ID = "48ca26c6f60f488a859eb2ab01842e76";
    private static final String REDIRECT_URI = "https://10.0.2.2/";

    private String ACCESS_TOKEN, MUSIC_PREF;
    private List <Track> tracks;
    private int currentTrack = 1;
    private boolean isPlaying = false;

    //Request code that will be passed together with authentication result to the onAuthenticationResult
    private static final int REQUEST_CODE = 1337;

    // UI controls which should only be enabled if the player is actively playing.
    private static final int[] REQUIRES_PLAYING_STATE = {
            R.id.skip_next_button,
            R.id.skip_prev_button,
    };

    public static final String TAG = "Spotify";

    //  _____ _      _     _
    // |  ___(_) ___| | __| |___
    // | |_  | |/ _ \ |/ _` / __|
    // |  _| | |  __/ | (_| \__ \
    // |_|   |_|\___|_|\__,_|___/
    //

    /**
     * The player used by this activity. There is only ever one instance of the player,
     * which is owned by the {@link com.spotify.sdk.android.player.Spotify} class and refcounted.
     * This means that you may use the Player from as many Fragments as you want, and be
     * assured that state remains consistent between them.
     * <p/>
     * However, each fragment, activity, or helper class <b>must</b> call
     * {@link com.spotify.sdk.android.player.Spotify#destroyPlayer(Object)} when they are no longer
     * need that player. Failing to do so will result in leaked resources.
     */
    private SpotifyPlayer mPlayer;

    private PlaybackState mCurrentPlaybackState;

    /**
     * Used to get notifications from the system about the current network state in order
     * to pass them along to
     * {@link SpotifyPlayer#setConnectivityStatus(Player.OperationCallback, Connectivity)}
     * Note that this implies <pre>android.permission.ACCESS_NETWORK_STATE</pre> must be
     * declared in the manifest. Not setting the correct network state in the SDK may
     * result in strange behavior.
     */
    private BroadcastReceiver mNetworkStateReceiver;

    /**
     * Used to log messages to a {@link android.widget.TextView} in this activity.
     */
    private TextView mStatusText;
    private TextView mMetadataText;
    private EditText mSeekEditText;

    /**
     * Used to scroll the {@link #mStatusText} to the bottom after updating text.
     */
    private ScrollView mStatusTextScrollView;
    private Metadata mMetadata;

    private final Player.OperationCallback mOperationCallback = new Player.OperationCallback() {
        @Override
        public void onSuccess() {
            logStatus("OK!");
        }

        @Override
        public void onError(Error error) {
            logStatus("ERROR:" + error);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ThemeForProfile);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get a reference to any UI widgets that we'll need to use later
        mStatusText = (TextView) findViewById(R.id.status_text);
        mMetadataText = (TextView) findViewById(R.id.metadata);
        buttons = (RelativeLayout) findViewById(R.id.buttonLayout);
        cards = (RelativeLayout) findViewById(R.id.cardLayout);
        selectWorkout = (TextView) findViewById(R.id.selectWorkoutText);
        prevButton = (Button) findViewById(R.id.skip_prev_button);
        nextButton = (Button) findViewById(R.id.skip_next_button);
        playlistProgress = (ProgressBar) findViewById(R.id.gen_playlist);

        session = new Constants(this.getApplicationContext());
        ACCESS_TOKEN = session.getSpotifyAuth();
        MUSIC_PREF = session.getMusicPref();
        Log.d("AUTH TOKEN: ", session.getName());
        Log.d("MUSIC PREF: ", session.getMusicPref());

        logStatus("Ready");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Set up the broadcast receiver for network events. Note that we also unregister
        // this receiver again in onPause().
        mNetworkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mPlayer != null) {
                    Connectivity connectivity = getNetworkConnectivity(getBaseContext());
                    logStatus("Network state changed: " + connectivity.toString());
                    mPlayer.setConnectivityStatus(mOperationCallback, connectivity);
                }
            }
        };

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkStateReceiver, filter);

        if (mPlayer != null) {
            mPlayer.addNotificationCallback(Music.this);
            mPlayer.addConnectionStateCallback(Music.this);
        }
    }

    /**
     * Registering for connectivity changes in Android does not actually deliver them to
     * us in the delivered intent.
     *
     * @param context Android context
     * @return Connectivity state to be passed to the SDK
     */
    private Connectivity getNetworkConnectivity(Context context) {
        ConnectivityManager connectivityManager;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            return Connectivity.fromNetworkType(activeNetwork.getType());
        } else {
            return Connectivity.OFFLINE;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    onAuthenticationComplete(response);
                    break;

                // Auth flow returned an error
                case ERROR:
                    logStatus("Auth error: " + response.getError());
                    break;

                // Most likely auth flow was cancelled
                default:
                    logStatus("Auth result: " + response.getType());
            }
        }
    }

    private void onAuthenticationComplete(AuthenticationResponse authResponse) {
        // Once we have obtained an authorization token, we can proceed with creating a Player.
        logStatus("Got authentication token");
        if (mPlayer == null) {
            Config playerConfig = new Config(getApplicationContext(), authResponse.getAccessToken(), CLIENT_ID);
            ACCESS_TOKEN = authResponse.getAccessToken();
            Log.d("ACCESS TOKEN IS: ", ACCESS_TOKEN);
            // Since the Player is a static singleton owned by the Spotify class, we pass "this" as
            // the second argument in order to refcount it properly. Note that the method
            // Spotify.destroyPlayer() also takes an Object argument, which must be the same as the
            // one passed in here. If you pass different instances to Spotify.getPlayer() and
            // Spotify.destroyPlayer(), that will definitely result in resource leaks.
            mPlayer = Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                @Override
                public void onInitialized(SpotifyPlayer player) {
                    logStatus("-- Player initialized --");
                    mPlayer.setConnectivityStatus(mOperationCallback, getNetworkConnectivity(Music.this));
                    mPlayer.addNotificationCallback(Music.this);
                    mPlayer.addConnectionStateCallback(Music.this);
                }

                @Override
                public void onError(Throwable error) {
                    logStatus("Error in initialization: " + error.getMessage());
                }
            });
        } else {
            mPlayer.login(authResponse.getAccessToken());
        }
    }

    public void onCompleteWorkout (View view) {
        // finish the workout - go to dashboard

        // turn off the timer for fitbit
        timer.cancel();
        doAsynchronousTask.cancel();
        timer.purge();

        // destory the spotify player
        Spotify.destroyPlayer(this);

        // Log time for end of workout
        endTime = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());

        getHeartRateData();
        saveWorkout();

        // go to dashboard
        Intent intent = new Intent(this, Complete.class);
        intent.putExtra("sAccess", ACCESS_TOKEN);
        intent.putExtra("playlistData", playList);
        intent.putExtra("hrData", hrData);
        intent.putExtra("tempoData", tempoData);
        startActivity(intent);
    }

    public void onPlayButtonClicked(View view) {
        playlistProgress.setVisibility(View.VISIBLE);

        SearchSpotifyTask task;
        switch (view.getId()) {
            case R.id.start_workout:
                // Start Spotify
                task = new SearchSpotifyTask();
                task.execute();
                break;
            case R.id.start_workout2:
                task = new SearchSpotifyTask();
                task.execute();
                break;
            case R.id.start_workout3:
                task = new SearchSpotifyTask();
                task.execute();
                break;
            default:
                throw new IllegalArgumentException("View ID does not have an associated URI to play");
        }
    }

    public void initializePlayer(String uri){
        // initialize the player!
        if (mPlayer == null) {
            Config playerConfig = new Config(getApplicationContext(), session.getSpotifyAuth(), CLIENT_ID);
            ACCESS_TOKEN = session.getSpotifyAuth();
            Log.d("ACCESS TOKEN IS: ", ACCESS_TOKEN);
            // Since the Player is a static singleton owned by the Spotify class, we pass "this" as
            // the second argument in order to refcount it properly. Note that the method
            // Spotify.destroyPlayer() also takes an Object argument, which must be the same as the
            // one passed in here. If you pass different instances to Spotify.getPlayer() and
            // Spotify.destroyPlayer(), that will definitely result in resource leaks.
            mPlayer = Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                @Override
                public void onInitialized(SpotifyPlayer player) {
                    logStatus("-- Player initialized --");
                    mPlayer.setConnectivityStatus(mOperationCallback, getNetworkConnectivity(Music.this));
                    mPlayer.addNotificationCallback(Music.this);
                    mPlayer.addConnectionStateCallback(Music.this);
                }

                @Override
                public void onError(Throwable error) {
                    logStatus("Error in initialization: " + error.getMessage());
                }
            });
        } else {
            mPlayer.login(session.getSpotifyAuth());
        }

        playlistProgress.setVisibility(View.GONE);
        logStatus("Starting playback for " + uri);
        mPlayer.playUri(mOperationCallback, uri, 0, 0);
    }

    public void startTimer(){
        // Start timer for FitBit
        final Handler handler = new Handler();
        timer = new Timer();
        doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @SuppressWarnings("unchecked")
                    public void run() {
                        try {
                            getData();
                            // Function Call
                        }
                        catch (Exception e) {
                            // TODO Auto-generated catch block
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 10000, 10000);
    }


    // back button clicked
    public void onSkipToPreviousButtonClicked(View view) {
        if (currentTrack != 1) {
            String uri = tracks.get(currentTrack).uri;
            currentTrack--;
            mPlayer.playUri(mOperationCallback, uri, 0, 0);
        }
        else {
            String uri = tracks.get(0).uri;
            currentTrack++;
            mPlayer.playUri(mOperationCallback, uri, 0, 0);
        }
    }

    // next button clicked
    public void onSkipToNextButtonClicked(View view) {
        String uri = tracks.get(currentTrack).uri;
        currentTrack++;
        mPlayer.playUri(mOperationCallback, uri, 0, 0);
    }

    // Generate the spotify playlist
    public class SearchSpotifyTask extends AsyncTask<Void, Void, Void>
    {
        protected Void doInBackground(Void... strings) {

            if (!ACCESS_TOKEN.isEmpty()) {
                SpotifyApi api = new SpotifyApi();
                api.setAccessToken(ACCESS_TOKEN);
                SpotifyService service = api.getService();
                Map<String, Object> options = new HashMap<>();
                options.put("seed_genres", MUSIC_PREF);
                options.put("limit", 50);
                Recommendations rec = service.getRecommendations(options);
                tracks = rec.tracks;

                /*float avgTempo = 0;
                float highestTempo = 0, lowestTempo = 0;
                for (int i = 0; i < tracks.size(); i++){
                    AudioFeaturesTrack aud = service.getTrackAudioFeatures(tracks.get(i).id);
                    if (i == 0) {highestTempo = aud.tempo; lowestTempo = aud.tempo; }
                    highestTempo = aud.tempo > highestTempo ? aud.tempo : highestTempo;
                    lowestTempo = aud.tempo < lowestTempo ? aud.tempo : lowestTempo;
                    avgTempo += aud.tempo;
                }
                Log.d("Average Tempo: ", Float.toString(avgTempo/tracks.size()));
                Log.d("Highest Tempo: ", Float.toString(highestTempo));
                Log.d("Lowest Tempo: ", Float.toString(lowestTempo));*/
            }
            else {
                Log.d("SPOTIFY: ", "ACCESS TOKEN IS EMPTY");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void strings) {
            startTime = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
            Log.d("STARTING TIME: ", startTime);
            initializePlayer(tracks.get(0).uri);
            startTimer();
        }
    }

    @Override
    public void onLoggedIn() {
        logStatus("Login complete");
    }

    @Override
    public void onLoggedOut() {
        logStatus("Logout complete");
    }

    public void onLoginFailed(Error error) {
        logStatus("Login error "+ error);
    }

    @Override
    public void onTemporaryError() {
        logStatus("Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(final String message) {
        logStatus("Incoming connection message: " + message);
    }

    private void logStatus(String status) {
        Log.i(TAG, status);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mNetworkStateReceiver);

        if (mPlayer != null) {
            mPlayer.removeNotificationCallback(Music.this);
            mPlayer.removeConnectionStateCallback(Music.this);
        }
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onPlaybackEvent(PlayerEvent event) {
        // Remember kids, always use the English locale when changing case for non-UI strings!
        // Otherwise you'll end up with mysterious errors when running in the Turkish locale.
        // See: http://java.sys-con.com/node/46241
        logStatus("Event: " + event);
        mCurrentPlaybackState = mPlayer.getPlaybackState();
        // if the song ended, go to the next song!
        if (!mCurrentPlaybackState.isPlaying && isPlaying){
            String uri = tracks.get(currentTrack).uri;
            currentTrack++;
            mPlayer.playUri(mOperationCallback, uri, 0, 0);
            isPlaying = false;
        }
        else if (mCurrentPlaybackState.isPlaying && !isPlaying){
            isPlaying = true;
        }

        mMetadata = mPlayer.getMetadata();
        Log.i(TAG, "Player state: " + mCurrentPlaybackState);
        Log.i(TAG, "Metadata: " + mMetadata);
        updateView();
    }

    @Override
    public void onPlaybackError(Error error) {
        logStatus("Err: " + error);
    }

    // update the album art
    private void updateView(){
        final ImageView coverArtView = (ImageView) findViewById(R.id.cover_art);
        if (mMetadata != null && mMetadata.currentTrack != null) {
            if (currentTrack == 1){
                prevButton.setVisibility(View.GONE);
            }
            else {
                prevButton.setVisibility(View.VISIBLE);
            }

            // allow user to skip/repeat songs
            buttons.setVisibility(View.VISIBLE);
            cards.setVisibility(View.GONE);
            selectWorkout.setVisibility(View.GONE);

            final String songCount = "Song " + Integer.toString(currentTrack);
            String trackName = StringUtils.abbreviate(mMetadata.currentTrack.name, 30);
            String artistName = StringUtils.abbreviate(mMetadata.currentTrack.artistName, 20);
            String addToPlaylist = tracks.get(currentTrack).uri + "&" + trackName + "&" + artistName;
            if (!playList.contains(addToPlaylist)) playList.add(tracks.get(currentTrack).uri + "&" + trackName + "&" + artistName);

            mMetadataText.setText(trackName + "-" + artistName + "\n" + songCount);

            Picasso.with(this)
                .load(mMetadata.currentTrack.albumCoverWebUrl)
                .transform(new Transformation() {
                    @Override
                    public Bitmap transform(Bitmap source) {
                        // really ugly darkening trick
                        final Bitmap copy = source.copy(source.getConfig(), true);
                        source.recycle();
                        final Canvas canvas = new Canvas(copy);
                        canvas.drawColor(0xbb000000);
                        return copy;
                    }

                    @Override
                    public String key() {
                        return "darken";
                    }
                })
                .into(coverArtView);
        } else {
            // Nothing is playing
        }
    }

    // FitBit
    public void getData(){
        new Thread (new Runnable () {
            @Override
            public void run() {
                try {
                    // get tempo for current song
                    SpotifyApi api = new SpotifyApi();
                    api.setAccessToken(ACCESS_TOKEN);
                    SpotifyService service = api.getService();
                    AudioFeaturesTrack aud = service.getTrackAudioFeatures(tracks.get(currentTrack).id);
                    //Log.d("AUDIO FEATURES: ", Float.toString(aud.tempo));
                    if (aud.tempo != 0) tempoData.add(aud.tempo);
                } catch (Exception e) {
                    Log.d("EXCEPTION: ", e.toString());
                }
            }
        }).start();
    }


    public void getHeartRateData(){
        final StringBuilder sbURL = new StringBuilder();
        sbURL.append("https://api.fitbit.com/1/user/-/activities/heart/date/today/1d/1sec/time/");
        sbURL.append(startTime);
        sbURL.append("/");
        sbURL.append(endTime);
        sbURL.append(".json");

        Log.d("URL: ", sbURL.toString());

        Thread hrThread = new Thread (new Runnable () {
            String url = sbURL.toString();

            @Override
            public void run() {
                try {
                    BasicHttpRequest request = AuthenticationManager
                            .createSignedRequest()
                            .setContentType("Application/json")
                            .setUrl(url)
                            .build();

                    final BasicHttpResponse response = request.execute();
                    final String json = response.getBodyAsString();

                    JSONObject hrJson = new JSONObject(json);
                    JSONArray intraJson = hrJson.getJSONObject("activities-heart-intraday").getJSONArray("dataset");
                    for (int i = 0; i < intraJson.length(); i++){
                        //Log.d("HR: ", Integer.toString(intraJson.getJSONObject(i).getInt("time")));
                        hrData.add(intraJson.getJSONObject(i).getInt("value"));
                        //Log.d("HR: ", Integer.toString(intraJson.getJSONObject(i).getInt("value")));
                    }
                } catch (Exception e) {
                    Log.d("EXCEPTION: ", e.toString());
                }
            }
        });

        hrThread.start();

        try {
            hrThread.join();
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
    }

    public void saveWorkout(){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        Map<String, String> workoutInfo = new HashMap<String, String>();
        String date = new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime());
        workoutInfo.put("Date", date);
        workoutInfo.put("StartTime", startTime);
        workoutInfo.put("EndTime", endTime);
        workoutInfo.put("MusicPreference", MUSIC_PREF);
        mDatabase.child(session.getId()).child(startTime).setValue(workoutInfo);
    }
}