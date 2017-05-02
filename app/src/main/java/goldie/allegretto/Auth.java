package goldie.allegretto;

import com.fitbit.authentication.AuthenticationHandler;
import com.fitbit.authentication.AuthenticationResult;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import com.fitbit.authentication.AuthenticationConfiguration;
import com.fitbit.authentication.AuthenticationConfigurationBuilder;
import com.fitbit.authentication.AuthenticationManager;
import com.fitbit.authentication.ClientCredentials;
import com.fitbit.authentication.Scope;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.SpotifyPlayer;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.util.Set;

import static com.fitbit.authentication.Scope.activity;

public class Auth extends AppCompatActivity implements AuthenticationHandler, SpotifyPlayer.NotificationCallback, ConnectionStateCallback {
    Toolbar toolbar;
    AppCompatButton btnFitbit, btnSpotify;

    // SPOTIFY DATA
        // Spotify API CLIENT ID
        private static final String CLIENT_ID = "48ca26c6f60f488a859eb2ab01842e76";
        // Spotify API REDIRECT URI
        private static final String REDIRECT_URI = "https://10.0.2.2/";
        // Needed for onAuthenticationResult
        private static final int REQUEST_CODE = 1337;
        // Tag for logging
        public static final String TAG = "Spotify";

        // authentication token for Spotify
        private String AUTH_TOKEN = "TOKEN";

    // FITBIT DATA
        // Fitbit API CLIENT ID
        private static final String FITBIT_CLIENT_ID = "3a2d236f96cfbbb9dcb77a2756018a02";
        private static final String SECURE_KEY = "CVPdQNAT6fBI4rrPLEn9x0+UV84DoqLFiNHpKOPLRW0=";
        private String FIT_ATUH_TOKEN = "TOKEN";

    // Shared Preferences for the app
    Constants session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        session = new Constants(this.getApplicationContext());

        // set the theme so we have the title bar
        setTheme(R.style.ThemeForProfile);
        super.onCreate(savedInstanceState);
        // set the content view
        setContentView(R.layout.activity_auth);

        // initialize the toolbar that contains the app name
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btnFitbit = (AppCompatButton) findViewById(R.id.connect_fitbit);
        btnSpotify = (AppCompatButton) findViewById(R.id.connect_spotify);

        // configure the authentication manager
        AuthenticationManager.configure(this, generateAuthenticationConfiguration(this, Auth.class));
    }

    // Authentication
    private void openLoginWindow() {
        final AuthenticationRequest request = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI)
                .setScopes(new String[]{"user-read-private", "playlist-read", "playlist-read-private", "streaming", "playlist-modify-public", "playlist-modify-private"})
                .setShowDialog(true)
                .build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
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
                    AUTH_TOKEN = response.getAccessToken();
                    session.setSpotifyAuth(AUTH_TOKEN);
                    break;

                // Auth flow returned an error
                case ERROR:
                    Log.d("Auth error: ", response.getError());
                    break;

                // Most likely auth flow was cancelled
                default:
                    Log.d("Auth result: ", response.getType().toString());
            }
        }
        else {
            AuthenticationManager.onActivityResult(requestCode, resultCode, intent, (AuthenticationHandler) this);
        }
    }

    // Handle UI Events
    public void spotifyLogin(View view){
        if (AUTH_TOKEN.equals("TOKEN")){
            openLoginWindow();
        }
        else {
            btnSpotify.setText("Connected to Spotify");
        }
    }

    public void onContinueButtonClicked(View view) {
        if (AUTH_TOKEN.equals("TOKEN")) {
            openLoginWindow();
        }
        else if (FIT_ATUH_TOKEN.equals("TOKEN")){
            AuthenticationManager.login(this);
        }
        else {
            Intent intent = new Intent(this, Music.class);
            startActivity(intent);
        }
    }

    /**
     * This method sets up the authentication config needed for
     * successfully connecting to the Fitbit API. Here we include our client credentials,
     * requested scopes, and  where to return after login
     */
    public static AuthenticationConfiguration generateAuthenticationConfiguration(Context context, Class<? extends Activity> mainActivityClass) {

        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;

            // Load clientId and redirectUrl from application manifest
            String clientId = bundle.getString("goldie.allegretto.CLIENT_ID");
            String redirectUrl = bundle.getString("goldie.allegretto.REDIRECT_URL");

            ClientCredentials CLIENT_CREDENTIALS = new ClientCredentials(clientId, FITBIT_CLIENT_ID, redirectUrl);

            return new AuthenticationConfigurationBuilder()
                    .setClientCredentials(CLIENT_CREDENTIALS)
                    .setEncryptionKey(SECURE_KEY)
                    .setTokenExpiresIn(3600L) // 1 hour
                    .setBeforeLoginActivity(new Intent(context, mainActivityClass))
                    .addRequiredScopes(Scope.profile, Scope.heartrate)
                    .addOptionalScopes(activity, Scope.weight)
                    .setLogoutOnAuthFailure(true)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*FitBit Authentication starts here*/

    // User clicks on the login button and is redirected to login to fitbit
    public void onFitBitLoginClick(View view) {
        AuthenticationManager.login(this);

    }

    // When the user is successfully logged in, set UI button
    public void onFitbitLoggedIn() {
        FIT_ATUH_TOKEN = "LOGGED_IN";
        btnFitbit.setText(R.string.connected_fitbit);
    }

    @Override
    public void onLoggedIn() {
        btnSpotify.setText("Connected to Spotify ");
    }

    @Override
    public void onLoggedOut() {
        Log.d(TAG, "Logout complete");
    }

    public void onLoginFailed(Error error) {
        Log.d("Login error ", error.toString());
    }

    @Override
    public void onTemporaryError() {
        Log.d(TAG, "Temporary error occurred");
    }

    @Override
    public void onPlaybackEvent(PlayerEvent event) {
        Log.d(TAG, event.toString());
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d(TAG, error.toString());
    }

    @Override
    public void onConnectionMessage(final String message) {
        Log.d("Connection message: ", message);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
         *  2. If we are logged in, go to next activity
         *      Otherwise, display the login screen
         */
        if (AuthenticationManager.isLoggedIn()) {
            onFitbitLoggedIn();
        }
    }

    @Override
    public void onAuthFinished(AuthenticationResult authenticationResult) {
        //binding.setLoading(false);
        /**
         * 5. Now we can parse the auth response! If the auth was successful, we can continue onto
         *      the next activity. Otherwise, we display a generic error message here
         */
        if (authenticationResult.isSuccessful()) {
            FIT_ATUH_TOKEN = authenticationResult.getAccessToken().toString();
            Log.d("AUTHENTICATION RESULT: ", authenticationResult.getAccessToken().getAccessToken());
            onLoggedIn();
        } else {
            displayAuthError(authenticationResult);
        }
    }

    private void displayAuthError(AuthenticationResult authenticationResult) {
        String message = "";

        switch (authenticationResult.getStatus()) {
            case dismissed:
                //message = getString(R.string.login_dismissed);
                Log.d("FIBIT", message);
                break;
            case error:
                message = authenticationResult.getErrorMessage();
                break;
            case missing_required_scopes:
                Set<Scope> missingScopes = authenticationResult.getMissingScopes();
                String missingScopesText = TextUtils.join(", ", missingScopes);
                //message = getString(R.string.missing_scopes_error) + missingScopesText;
                Log.d("FIBIT: ", message);
                break;
        }

        new AlertDialog.Builder(this)
            .setTitle(R.string.login_button_fitbit)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            })
            .create()
            .show();
    }

    @Override
    public void onBackPressed() {
    }
}
