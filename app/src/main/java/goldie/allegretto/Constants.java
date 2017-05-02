package goldie.allegretto;

import android.content.Context;
import android.content.SharedPreferences;

public class Constants {
    // Shared Preferences
    SharedPreferences pref;

    // Editor for Shared preferences
    SharedPreferences.Editor editor;

    // Context
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    public static final String BASE_URL = "http://10.0.2.2/";
    public static final String REGISTER_OPERATION = "register";
    public static final String LOGIN_OPERATION = "login";
    public static final String CHANGE_PASSWORD_OPERATION = "chgPass";

    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";
    public static final String IS_LOGGED_IN = "isLoggedIn";

    public static final String NAME = "name";
    public static final String EMAIL = "email";
    public static final String UNIQUE_ID = "unique_id";
    public static final String SPOTIFY_AUTH = "spotify_auth";
    public static final String MUSIC_PREF = "none";

    public static final String TAG = "Allegretto";

    // Constructor
    public Constants(Context context){
        this._context = context;
        pref = _context.getSharedPreferences(TAG, PRIVATE_MODE);
        editor = pref.edit();
    }

    // Login Session
    public void createLoginSession(String name, String email, String unique_id){
        editor.putBoolean(IS_LOGGED_IN, true);
        editor.putString(NAME, name);
        editor.putString(EMAIL, email);
        editor.putString(UNIQUE_ID, unique_id);

        // commit changes
        editor.commit();
    }

    public void setSpotifyAuth(String auth){
        editor.putString(SPOTIFY_AUTH, auth);
        editor.commit();
    }

    public void setMusicPref(String genre){
        editor.putString(MUSIC_PREF, genre);
        editor.commit();
    }

    public String getName() {
        return pref.getString(NAME, null);
    }
    public String getId() {
        return pref.getString(UNIQUE_ID, null);
    }
    public String getSpotifyAuth() {return pref.getString(SPOTIFY_AUTH, null);}
    public String getMusicPref() {return pref.getString(MUSIC_PREF, null);}


}
