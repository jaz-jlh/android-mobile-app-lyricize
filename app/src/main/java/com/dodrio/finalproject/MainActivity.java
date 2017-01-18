/*
Used https://ihofmann.wordpress.com/2012/08/09/restful-web-services-with-json-in-android/

Used http://stackoverflow.com/questions/3400028/close-virtual-keyboard-on-button-press and http://stackoverflow.com/questions/3553779/android-dismiss-keyboard
    for hiding soft keyboard on button press

Used Sherriff's Storage example to work out file storage

Used https://www.raywenderlich.com/124438/android-listview-tutorial
    to assist in listview construction

Used Sherriff's service example for GPS

Used http://stackoverflow.com/questions/32083913/android-gps-requires-access-fine-location-error-even-though-my-manifest-file
    to figure out Sherriff's GPS code permission issues

Used http://stackoverflow.com/questions/11845423/how-to-send-data-from-service-to-my-activity
    to implement broadcast receiver to get GPS coordinates

Used http://jsonviewer.stack.hu/
    to visualize JSON organization

Used http://stackoverflow.com/questions/9596663/how-to-make-items-clickable-in-list-view
    to make the items in the list clickable

Used http://stackoverflow.com/questions/11409912/android-how-to-update-value-of-progressbar
    to set progress bars to a number value

Used http://stackoverflow.com/questions/16176210/how-to-display-2-textviews-in-the-same-line-in-android
    to remind us of how weights and gravity works to create nice layouts

Used http://stackoverflow.com/questions/2115758/how-do-i-display-an-alert-dialog-on-android
    to learn how alert dialog work and getting them to display

Used http://www.androidpeople.com/android-portrait-amp-landscape-differeent-layouts
    to learn how to make custom layouts for landscape view


***
Valid musicmatch query
http://api.musixmatch.com/ws/1.1/track.lyrics.get?track_id=15953433&format=json&apikey=d6c4651127df9a586bc7f645389b7227
***


 */

package com.dodrio.finalproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneAnalysis;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import static java.lang.Math.sqrt;

public class MainActivity extends AppCompatActivity {

    // APIs
    public static final String BASE_SPOTIFY_URL = "https://api.spotify.com/v1/";
    public static final String BASE_MUSIXMATCH_URL = "http://api.musixmatch.com/ws/1.1/";
    public static final String MUSIXMATCH_API_KEY = "apikey";
    public String searchType = "track";
    public ListView listView;
    public boolean MusixMatchFlag;
    ToneAnalyzer toneAnalyzer = new ToneAnalyzer(ToneAnalyzer.VERSION_DATE_2016_05_19);

    public String lastSearch = "";
    public String lastSearchType = "";
    public JSONObject lastJSONObject = null;
    public JSONObject result;

    public ArrayList<String> history = new ArrayList<>();
    public String historyQuery = "";
    public String coordinates = "";
    public ArrayList<Music> searchResultsList = new ArrayList<>();

    public ArrayList<String> topArtists= new ArrayList<>();
    public ArrayList<String> topAlbums = new ArrayList<>();
    public ArrayList<String> topSongs = new ArrayList<>();

    public GPSReceiver gpsReceiver = new GPSReceiver();

    public int flag = 0;

    // Accelerometer
    private SensorManager sensorManager;
    private float accel;
    private float accelCurrent;
    private float accelLast;
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];
            accelLast = accelCurrent;
            accelCurrent = (float) Math.sqrt((double)(x*x + y*y+z*z));
            float delta = accelCurrent-accelLast;
            accel = accel *0.9f + delta;
            if(accel > 8 && flag == 0) {
                flag = 1;
                EditText editText = (EditText) findViewById(R.id.search);
                Log.d("Accel","accel triggered");
                Random random = new Random();
                int low = 0;
                int high = 99;
                int result = random.nextInt(high-low) + low;
                switch (searchType) {
                    case "artist":
                        editText.setText(topArtists.get(result));
                        break;
                    case "album":
                        high = 199;
                        result = random.nextInt(high-low) + low;
                        editText.setText(topAlbums.get(result));
                        break;
                    case "track":
                        editText.setText(topSongs.get(result));
                        break;
                }
            }
            if(accel < 1.3) flag = 0;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("onCreate","starting oncreate");
        this.registerReceiver(gpsReceiver,new IntentFilter("GPS"));

        setContentView(R.layout.activity_main);
        listView = (ListView)findViewById(R.id.searchResults);

        //Accelerometer
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), sensorManager.SENSOR_DELAY_NORMAL);
        accel = 0.00f;
        accelCurrent = SensorManager.GRAVITY_EARTH;
        accelLast = SensorManager.GRAVITY_EARTH;

        // Populate topArtists and topSongs
        try {
            AssetManager assetManager = this.getAssets();
            //AssetFileDescriptor fileDescriptor = assetManager.openFd(FILENAME_TOP);
            //FileInputStream fis = fileDescriptor.createInputStream();
            InputStream inputStream = assetManager.open("topSongs.txt");
            StringBuilder builder = new StringBuilder();
            int ch;
            while((ch = inputStream.read()) != -1){
                if((char)ch =='\n') {
                    topSongs.add(builder.toString());
                    builder.delete(0,builder.length());
                } else {
                    builder.append((char)ch);
                }
            }
            inputStream= assetManager.open("topArtists.txt");
            while((ch = inputStream.read()) != -1){
                if((char)ch =='\n') {
                    topArtists.add(builder.toString());
                    builder.delete(0,builder.length());
                } else {
                    builder.append((char)ch);
                }
            }
            inputStream = assetManager.open("topAlbums.txt");
            while((ch = inputStream.read()) != -1){
                if((char)ch =='\n') {
                    topAlbums.add(builder.toString());
                    builder.delete(0,builder.length());
                } else {
                    builder.append((char)ch);
                }
            }
            inputStream.close();
        }catch(Exception e) {
            Log.e("Restoring from file", e.getMessage());
        }
        // Restore History file to History arraylist
        String FILENAME = "previous_results";
        history.clear();
        try {
            FileInputStream fis = openFileInput(FILENAME);
            StringBuilder builder = new StringBuilder();
            int ch;
            while((ch = fis.read()) != -1){
                if((char)ch =='\n') {
                    history.add(builder.toString());
                    Log.d("Restoring from file", builder.toString());
                    builder.delete(0,builder.length());
                } else {
                    builder.append((char)ch);
                }
            }
            fis.close();
        }catch(Exception e) {
            Log.e("Restoring from file", "Failed");
        }


    }

    // APIs
    public void callSpotifyAPI(View view) {
        //Log.d("Callspotifyapi","started spotify api call");
        Button button = (Button) view;
        if(button.isEnabled()) {
            button.setEnabled(false);
            button.setText("Searching...");
            EditText text = (EditText) findViewById(R.id.search);
            String search = text.getText().toString();
            //textResults = (TextView) findViewById(R.id.searchresults);
            new CallSpotifyAPI().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,search);
        }
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null : getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        //stopService(view);
    }
    public class CallSpotifyAPI extends AsyncTask<String, Void, JSONObject> {
        protected JSONObject doInBackground(String... text) {
            String search = "";
            for (String s : text)
                search = s;
            if (search.length()==0) {
                //Toast noText = Toast.makeText(getApplicationContext(), "Please enter a search query", Toast.LENGTH_SHORT);
                //noText.show();
                Log.d("CallSpotifyAPI", "no search entered");
                //cancel(true);
                return null;
            } else {
                search = search.trim();
                search = search.replace(' ', '+');
                //Log.d("CallSpotifyAPI", "search: " + search + " lastSearch = "+ lastSearch);
                if(search.equals(lastSearch) && searchType.equals(lastSearchType)) {
                    return lastJSONObject;
                }

                Log.d("CallSpotifyAPI", search);
                String serviceUrl = BASE_SPOTIFY_URL + "search?q=" + search + "&type=" + searchType;
                Log.d("CallSpotifyAPI", serviceUrl);
                HttpURLConnection urlConnection = null;
                try {
                    // create connection
                    //Log.d("CallSpotifyAPI", "I at least tried");
                    URL urlToRequest = new URL(serviceUrl);
                    urlConnection = (HttpURLConnection)
                            urlToRequest.openConnection();
                    // handle issues
                    int statusCode = urlConnection.getResponseCode();
                    Log.d("CallSpotifyAPI", "Status code:" + Integer.toString(statusCode));
                    if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        // handle unauthorized (if service requires user login)
                    } else if (statusCode != HttpURLConnection.HTTP_OK) {
                        // handle any other errors, like 404, 500,..
                    }
                    // create JSON object from content
                    InputStream in = new BufferedInputStream(
                            urlConnection.getInputStream());
                    result = new JSONObject(getResponseText(in));
                    lastSearch = search;
                    lastSearchType = searchType;
                    lastJSONObject = result;
                    return result;
                } catch (MalformedURLException e) {
                    // URL is invalid
                } catch (SocketTimeoutException e) {
                    // data retrieval or connection timed out
                } catch (IOException e) {
                    // could not read response body
                    // (could not create input stream)
                } catch (JSONException e) {
                    // response body is no valid JSON string
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }
            return null;
        }
        protected void onPostExecute(JSONObject result) {
            Button button = (Button)findViewById(R.id.button);
            button.setEnabled(true);
            button.setText("Search");
            if (result == null) {
                String[] listItems = new String[1];
                listItems[0] = "Please enter a search query";
                ArrayAdapter adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, listItems);
                //Log.d("onPostExecute",adapter.toString());
                listView.setAdapter(adapter);
                listView.setBackgroundColor(0xffffffff);
            } else {
                Log.d("onPostExecute", "trying...");
                //Log.d("onPostExecute",searchType);
                String results = "";
                searchType = searchType.toLowerCase();
                switch (searchType) {
                    case "artist":
                        searchResultsList.clear();
                        searchResultsList = parseArtist(result);
                        break;
                    case "album":
                        searchResultsList.clear();
                        searchResultsList = parseAlbum(result);
                        break;
//                    case "playlist":
//                        searchResultsList.clear();
//                        searchResultsList = parsePlaylist(result);
//                        break;
                    case "track":
                        searchResultsList.clear();
                        searchResultsList = parseSong(result);
                        break;
                }
                String[] listItems = new String[searchResultsList.size()];
                //Log.d("onPostExecute","finished switch");
                for (int i = 0; i < searchResultsList.size(); i++) {
                    //Log.d("onPostExecute","inside searchresultslist loop");
                    listItems[i] = searchResultsList.get(i).toString();
                    //Log.d("onPostExecute",searchResultsList.get(i).toString());
                }
                //Log.d("onPostExecute",listItems.toString());
                ArrayAdapter adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, listItems);
                //Log.d("onPostExecute",adapter.toString());
                listView.setAdapter(adapter);
                listView.setBackgroundColor(0xffffffff);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView <? > arg0, View view, int position, long id) {
                        String search = ((TextView) view).getText().toString();
                        if(searchType.equals("album")) {
                            Album album = (Album)searchResultsList.get(position);
                            String albumId = album.getId();
                            RadioGroup radioGroup = (RadioGroup)findViewById(R.id.radiogroup);
                            radioGroup.clearCheck();
                            radioGroup.check(R.id.songButton);
                            searchType = "track";
                            new CallSpotifyAPIAlbumTracks().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,albumId);
                        } else if(searchType.equals("artist")) {
                            Artist artist = (Artist) searchResultsList.get(position);
                            String artistId = artist.getId();
                            RadioGroup radioGroup = (RadioGroup)findViewById(R.id.radiogroup);
                            radioGroup.clearCheck();
                            radioGroup.check(R.id.albumButton);
                            searchType = "album";
                            new CallSpotifyAPIArtistAlbums().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,artistId);
                        } else {
                            if (search.trim().toLowerCase().equals("no results") || search.trim().equals("Please enter a search query")) {
                                Toast init = Toast.makeText(getApplicationContext(), "Still no results!", Toast.LENGTH_SHORT);
                                init.show();
                            } else {
                                startService(view);
                                callMusixMatchAPI(view);
                            }
                        }

                    }
                });
        }
        }
    }
    public void callMusixMatchAPI(View view) {
        Button historyButton = (Button)findViewById(R.id.button2);
        historyButton.setEnabled(false);
        historyButton.setText("Saving...");
        String search = ((TextView) view).getText().toString();
        MusixMatchFlag = true;
        new CallMusixMatchAPI().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,search);
        historyQuery = search;
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null : getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }
    public class CallMusixMatchAPI extends AsyncTask<String, Void, JSONObject> {
        protected JSONObject doInBackground(String... text) {
            String search = "";
            for (String s : text)
                search = s;
            if (MusixMatchFlag) {
                //search to get track id
                String[] parts = search.split(" by ");
                parts[0] = parts[0].replaceAll(" ", "%20");
                String song = parts[0];
                parts[1] = parts[1].replaceAll(" ", "%20");
                String artist = parts[1];
                Log.d("CallMusixMatchAPI", search);
                String serviceUrl = BASE_MUSIXMATCH_URL + "track.search?q_track=" + song + "&q_artist=" + artist+ "&format=json&apikey=" +MUSIXMATCH_API_KEY;
                Log.d("CallMusixMatchAPI", serviceUrl);
                HttpURLConnection urlConnection = null;
                try {
                    // create connection
                    //Log.d("CallSpotifyAPI", "I at least tried");
                    URL urlToRequest = new URL(serviceUrl);
                    urlConnection = (HttpURLConnection)
                            urlToRequest.openConnection();
                    // handle issues
                    int statusCode = urlConnection.getResponseCode();
                    Log.d("CallMusixMatchAPI", "Status code:" + Integer.toString(statusCode));
                    if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        // handle unauthorized (if service requires user login)
                    } else if (statusCode != HttpURLConnection.HTTP_OK) {
                        // handle any other errors, like 404, 500,..
                    }
                    // create JSON object from content
                    InputStream in = new BufferedInputStream(
                            urlConnection.getInputStream());
                    result = new JSONObject(getResponseText(in));
                    return result;
                } catch (MalformedURLException e) {
                    // URL is invalid
                } catch (SocketTimeoutException e) {
                    // data retrieval or connection timed out
                } catch (IOException e) {
                    // could not read response body
                    // (could not create input stream)
                } catch (JSONException e) {
                    // response body is no valid JSON string
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            return null;
            } else {
                //get lyrics
                search = search.trim();
                Log.d("CallMusixMatchAPI", search);
                String serviceUrl = BASE_MUSIXMATCH_URL + "track.lyrics.get?track_id=" + search + "&format=json&apikey=" +MUSIXMATCH_API_KEY;
                Log.d("CallMusixMatchAPI", serviceUrl);
                HttpURLConnection urlConnection = null;
                try {
                    // create connection
                    //Log.d("CallSpotifyAPI", "I at least tried");
                    URL urlToRequest = new URL(serviceUrl);
                    urlConnection = (HttpURLConnection)
                            urlToRequest.openConnection();
                    // handle issues
                    int statusCode = urlConnection.getResponseCode();
                    Log.d("CallMusixMatchAPI", "Status code:" + Integer.toString(statusCode));
                    if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        // handle unauthorized (if service requires user login)
                    } else if (statusCode != HttpURLConnection.HTTP_OK) {
                        // handle any other errors, like 404, 500,..
                    }
                    // create JSON object from content
                    InputStream in = new BufferedInputStream(
                            urlConnection.getInputStream());
                    result = new JSONObject(getResponseText(in));
                    return result;
                } catch (MalformedURLException e) {
                    // URL is invalid
                } catch (SocketTimeoutException e) {
                    // data retrieval or connection timed out
                } catch (IOException e) {
                    // could not read response body
                    // (could not create input stream)
                } catch (JSONException e) {
                    // response body is no valid JSON string
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
                return null;
            }
        }
        protected void onPostExecute(JSONObject result){
            if(MusixMatchFlag) {
                try {
                    //parse json to get track id
                    JSONObject message = result.getJSONObject("message");
                    JSONObject body = message.getJSONObject("body");
                    JSONArray trackList = body.getJSONArray("track_list");
                    JSONObject item0 = trackList.getJSONObject(0);
                    JSONObject track = item0.getJSONObject("track");
                    String track_id = track.getString("track_id");
                    String has_lyrics = track.getString("has_lyrics");
                    //call api with id
                    if(has_lyrics.equals("1")) {
                        new CallMusixMatchAPI().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,track_id);
                        //update flag
                        MusixMatchFlag = false;
                    }else {
                        Toast init = Toast.makeText(getApplicationContext(), "No lyrics, try different item.", Toast.LENGTH_SHORT);
                        init.show();
                    }
                } catch (JSONException e) {
                    Log.d("onPostExecute", "JSON Exception");
                    Log.d("onPostExecute", e.getMessage());
                }
            }
            else {
                //parse json to get lyrics
                try {
                    //parse json to get track id
                    JSONObject message = result.getJSONObject("message");
                    JSONObject body = message.getJSONObject("body");
                    JSONObject lyrics = body.getJSONObject("lyrics");
                    String lyrics_body = lyrics.getString("lyrics_body");
                    //Log.d("MM onpost execute",lyrics_body);
                    //call api with id
                    if(lyrics_body.isEmpty()){
                        Toast init = Toast.makeText(getApplicationContext(), "No lyrics, try different item.", Toast.LENGTH_SHORT);
                        init.show();
                    } else {
                        new CallWatsonAPI().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,lyrics_body);
                        Toast init = Toast.makeText(getApplicationContext(), "Fetching and analyzing lyrics...", Toast.LENGTH_SHORT);
                        init.show();
                        //update flag
                        MusixMatchFlag = false;
                    }
                } catch (JSONException e) {
                    Log.d("onPostExecute", "JSON Exception");
                    Log.d("onPostExecute", e.getMessage());
                }
            }
        }
    }
    public class CallWatsonAPI extends AsyncTask<String, Void, JSONObject> {
        protected JSONObject doInBackground(String...text) {

            toneAnalyzer.setUsernameAndPassword("username","password");
            String search = "";
            for (String s : text)
                search = s;
            ToneAnalysis tone = toneAnalyzer.getTone(search,null).execute();
            //Log.d("ToneAnalyzer",tone.toString());
            try {
                JSONObject toneJSON = new JSONObject(tone.toString());
                return toneJSON;
            } catch (JSONException e) {
                Log.d("Callwatsonapi",e.getMessage());
            }
            return null;
        }
        protected void onPostExecute(JSONObject result) {
            if(result == null) {
                Log.d("callwatsonapi","no result");
            } else {
                try {
                    //parse json to get tones
                    Intent intent = new Intent(MainActivity.this, ResultsActivity.class);
                    String readableOutput = "";
                    JSONObject document_tone = result.getJSONObject("document_tone");
                    JSONArray tone_categories = document_tone.getJSONArray("tone_categories");
                    for(int i=0; i<tone_categories.length();i++) {
                        JSONObject category = tone_categories.getJSONObject(i);
                        //readableOutput += category.getString("category_name") + "\n";
                        JSONArray tones = category.getJSONArray("tones");
                        for(int j=0;j<tones.length();j++) {
                            JSONObject tone = tones.getJSONObject(j);
                            String toneName = tone.getString("tone_name").toLowerCase();
                            float score = Float.parseFloat(tone.getString("score"));
                            switch (toneName) {
                                case "anger":
                                    intent.putExtra("anger",score);
                                    break;
                                case "disgust":
                                    intent.putExtra("disgust",score);
                                    break;
                                case "fear":
                                    intent.putExtra("fear",score);
                                    break;
                                case "joy":
                                    intent.putExtra("joy",score);
                                    break;
                                case "sadness":
                                    intent.putExtra("sadness",score);
                                    break;
                                case "analytical":
                                    intent.putExtra("analytical",score);
                                    break;
                                case "confident":
                                    intent.putExtra("confident",score);
                                    break;
                                case "tentative":
                                    intent.putExtra("tentative",score);
                                    break;
                                case "emotional range":
                                    intent.putExtra("emotional range",score);
                                    break;
                            }
                            //readableOutput +=tone.getString("tone_name") + ":" + tone.getString("score") + "\n";
                        }
                    }
                    intent.putExtra("Query",historyQuery);
                    Log.d("callwatsonapi",readableOutput);
                    MainActivity.this.startActivity(intent);
                } catch (JSONException e) {
                    Log.d("callwatsonapi", "JSON Exception");
                    Log.d("callwatsonapi", e.getMessage());
                }
            }
        }
    }

    //album track fetching
    public class CallSpotifyAPIAlbumTracks extends AsyncTask<String, Void, JSONObject> {
        protected JSONObject doInBackground(String... text) {
            String search = "";
            for (String s : text)
                search = s;
            if (search.length() == 0) {
                Log.d("albumtracks", "no search entered");
                //cancel(true);
                return null;
            } else {
                search = search.trim();
                search = search.replace(' ', '+');
                String serviceUrl = BASE_SPOTIFY_URL + "albums/" + search + "/tracks";
                Log.d("albumtracks", serviceUrl);
                HttpURLConnection urlConnection = null;
                try {
                    // create connection
                    //Log.d("CallSpotifyAPI", "I at least tried");
                    URL urlToRequest = new URL(serviceUrl);
                    urlConnection = (HttpURLConnection)
                            urlToRequest.openConnection();
                    // handle issues
                    int statusCode = urlConnection.getResponseCode();
                    Log.d("albumtracks", "Status code:" + Integer.toString(statusCode));
                    if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        // handle unauthorized (if service requires user login)
                    } else if (statusCode != HttpURLConnection.HTTP_OK) {
                        // handle any other errors, like 404, 500,..
                    }
                    // create JSON object from content
                    InputStream in = new BufferedInputStream(
                            urlConnection.getInputStream());
                    result = new JSONObject(getResponseText(in));
                    return result;
                } catch (MalformedURLException e) {
                    // URL is invalid
                } catch (SocketTimeoutException e) {
                    // data retrieval or connection timed out
                } catch (IOException e) {
                    // could not read response body
                    // (could not create input stream)
                } catch (JSONException e) {
                    // response body is no valid JSON string
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }
            return null;
        }
        protected void onPostExecute(JSONObject result) {
            if (result == null) {
                String[] listItems = new String[1];
                listItems[0] = "Please enter a search query";
                ArrayAdapter adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, listItems);
                //Log.d("onPostExecute",adapter.toString());
                listView.setAdapter(adapter);
                listView.setBackgroundColor(0xffffffff);
            } else {
                Log.d("onPostExecute", "trying...");
                //Log.d("onPostExecute",searchType);
                String results = "";
                searchType = searchType.toLowerCase();
                searchResultsList.clear();
                searchResultsList = parseAlbumSongs(result);
                String[] listItems = new String[searchResultsList.size()];
                    //Log.d("onPostExecute","finished switch");
                    for (int i = 0; i < searchResultsList.size(); i++) {
                        //Log.d("onPostExecute","inside searchresultslist loop");
                        listItems[i] = searchResultsList.get(i).toString();
                        //Log.d("onPostExecute",searchResultsList.get(i).toString());
                }
                //Log.d("onPostExecute",listItems.toString());
                ArrayAdapter adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, listItems);
                //Log.d("onPostExecute",adapter.toString());
                listView.setAdapter(adapter);
                listView.setBackgroundColor(0xffffffff);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView <? > arg0, View view, int position, long id) {
                        String search = ((TextView) view).getText().toString();
                        if (search.trim().toLowerCase().equals("no results") || search.trim().equals("Please enter a search query")) {
                            Toast init = Toast.makeText(getApplicationContext(), "Still no results!", Toast.LENGTH_SHORT);
                            init.show();
                        } else {
                            callMusixMatchAPI(view);
                        }

                    }
                });
            }
        }
    }
    public class CallSpotifyAPIArtistAlbums extends AsyncTask<String, Void, JSONObject> {
        protected JSONObject doInBackground(String... text) {
            String search = "";
            for (String s : text)
                search = s;
            if (search.length() == 0) {
                Log.d("artistalbums", "no search entered");
                //cancel(true);
                return null;
            } else {
                search = search.trim();
                search = search.replace(' ', '+');
                String serviceUrl = BASE_SPOTIFY_URL + "artists/" + search + "/albums";
                Log.d("artistalbums", serviceUrl);
                HttpURLConnection urlConnection = null;
                try {
                    // create connection
                    //Log.d("CallSpotifyAPI", "I at least tried");
                    URL urlToRequest = new URL(serviceUrl);
                    urlConnection = (HttpURLConnection)
                            urlToRequest.openConnection();
                    // handle issues
                    int statusCode = urlConnection.getResponseCode();
                    Log.d("artistalbums", "Status code:" + Integer.toString(statusCode));
                    if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        // handle unauthorized (if service requires user login)
                    } else if (statusCode != HttpURLConnection.HTTP_OK) {
                        // handle any other errors, like 404, 500,..
                    }
                    // create JSON object from content
                    InputStream in = new BufferedInputStream(
                            urlConnection.getInputStream());
                    result = new JSONObject(getResponseText(in));
                    return result;
                } catch (MalformedURLException e) {
                    // URL is invalid
                } catch (SocketTimeoutException e) {
                    // data retrieval or connection timed out
                } catch (IOException e) {
                    // could not read response body
                    // (could not create input stream)
                } catch (JSONException e) {
                    // response body is no valid JSON string
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }
            return null;
        }
        protected void onPostExecute(JSONObject result) {
            if (result == null) {
                String[] listItems = new String[1];
                listItems[0] = "Please enter a search query";
                ArrayAdapter adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, listItems);
                //Log.d("onPostExecute",adapter.toString());
                listView.setAdapter(adapter);
                listView.setBackgroundColor(0xffffffff);
            } else {
                Log.d("onPostExecute", "trying...");
                //Log.d("onPostExecute",searchType);
                String results = "";
                searchType = searchType.toLowerCase();
                searchResultsList.clear();
                searchResultsList = parseArtistAlbums(result);
                String[] listItems = new String[searchResultsList.size()];
                //Log.d("onPostExecute","finished switch");
                for (int i = 0; i < searchResultsList.size(); i++) {
                    //Log.d("onPostExecute","inside searchresultslist loop");
                    listItems[i] = searchResultsList.get(i).toString();
                    //Log.d("onPostExecute",searchResultsList.get(i).toString());
                }
                //Log.d("onPostExecute",listItems.toString());
                ArrayAdapter adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, listItems);
                //Log.d("onPostExecute",adapter.toString());
                listView.setAdapter(adapter);
                listView.setBackgroundColor(0xffffffff);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView <? > arg0, View view, int position, long id) {
                        Album album = (Album)searchResultsList.get(position);
                        String albumId = album.getId();
                        RadioGroup radioGroup = (RadioGroup)findViewById(R.id.radiogroup);
                        radioGroup.clearCheck();
                        radioGroup.check(R.id.songButton);
                        searchType = "track";
                        new CallSpotifyAPIAlbumTracks().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,albumId);

                    }
                });
            }
        }
    }

    // JSON Parsing
    public ArrayList<Music> parseArtist(JSONObject jsonObject) {
        ArrayList<Music> artists = new ArrayList<>();
        try {
            //Log.d("onPostExecute","made it to the switch");
            JSONObject artistList = jsonObject.getJSONObject("artists");
            //Log.d("onPostExecute",artistList.get("total").toString());
            //Log.d("onPostExecute","made artists object");
            //Log.d("onPostExecute",artistList.toString());
            artistList.getString("href");
            //Log.d("onPostExecute","consumed href");
            JSONArray artistsArray = artistList.getJSONArray("items");
            //Log.d("onPostExecute","made array");
            if (artistsArray.length() != 0) {
                //Log.d("onPostExecute","artists array length was nonzero");
                for (int i = 0; i < artistsArray.length(); i++) {
                    JSONObject artistJSONObject = artistsArray.getJSONObject(i);
                    Artist artist = new Artist(artistJSONObject.getString("name"), artistJSONObject.getString("id"));
                    artists.add(artist);
                }
            } else {
                //Log.d("onPostExecute","artists array length was zero");
                //textResults.setText("No results");
                Artist artist = new Artist("No results", "");
                artists.add(artist);
            }
        }
        catch (JSONException e) {
            Log.d("onPostExecute", "JSON Exception");
            Log.d("onPostExecute", e.getMessage());
        }
        return artists;
    }
    public ArrayList<Music> parseAlbum(JSONObject result) {
        ArrayList<Music> albums = new ArrayList<>();
        try {
            JSONObject albumList = result.getJSONObject("albums");
            //Log.d("onPostExecute","made albums object");
            //Log.d("onPostExecute",albumList.toString());
            albumList.getString("href");
            //Log.d("onPostExecute","consumed href");
            JSONArray albumArray = albumList.getJSONArray("items");
            //Log.d("onPostExecute","made array");
            if (albumArray.length() != 0) {
                for (int i = 0; i < albumArray.length(); i++) {
                    JSONObject jsonObject = albumArray.getJSONObject(i);
                    jsonObject.getString("album_type");
                    //Log.d("onPostExecute","consumed album_type");
                    JSONArray albumArtistsArray = jsonObject.getJSONArray("artists");
                    //Log.d("onPostExecute","made array");
                    ArrayList<Artist> albumArtists = new ArrayList<>();
                    if (albumArtistsArray.length() != 0) {
                        for (int j = 0; j < albumArtistsArray.length(); j++) {
                            JSONObject artistObject = albumArtistsArray.getJSONObject(j);
                            Artist artist = new Artist(artistObject.getString("name"), artistObject.getString("id"));
                            albumArtists.add(artist);
                        }
                    }
                    Album album = new Album(jsonObject.getString("name"), jsonObject.getString("id"), albumArtists);
                    albums.add(album);
                }
            } else {
                //textResults.setText("No results");
                Album album = new Album("No results", "", null);
                albums.add(album);
            }
        }
        catch (JSONException e) {
            Log.d("onPostExecute", "JSON Exception");
            Log.d("onPostExecute", e.getMessage());
        }
        return albums;
    }
    public ArrayList<Music> parsePlaylist(JSONObject result) {
        ArrayList<Music> playlists = new ArrayList<>();
        try {
            JSONObject playlistList = result.getJSONObject("playlists");
            //Log.d("onPostExecute","made playlists object");
            //Log.d("onPostExecute",playlistList.toString());
            playlistList.getString("href");
            //Log.d("onPostExecute","consumed href");
            JSONArray playlistArray = playlistList.getJSONArray("items");
            //Log.d("onPostExecute","made array");
            if (playlistArray.length() != 0) {
                for (int i = 0; i < playlistArray.length(); i++) {
                    JSONObject jsonObject = playlistArray.getJSONObject(i);
                    JSONObject owner = jsonObject.getJSONObject("owner");
                    Playlist playlist = new Playlist(jsonObject.getString("name"), owner.getString("id"));
                    playlists.add(playlist);
                }
            } else {
                //textResults.setText("No results");
                Playlist playlist = new Playlist("No results", "");
                playlists.add(playlist);
            }
        }
        catch (JSONException e) {
            Log.d("onPostExecute", "JSON Exception");
            Log.d("onPostExecute", e.getMessage());
        }
        return playlists;
    }
    public ArrayList<Music> parseSong(JSONObject result) {
        ArrayList<Music> songs = new ArrayList<>();
        try {
            JSONObject songList = result.getJSONObject("tracks");
            //Log.d("onPostExecute","made songs object");
            //Log.d("onPostExecute",albumList.toString());
            songList.getString("href");
            // Log.d("onPostExecute","consumed href");
            JSONArray songArray = songList.getJSONArray("items");
            //Log.d("onPostExecute","made array");
            if (songArray.length() != 0) {
                for (int i = 0; i < songArray.length(); i++) {
                    JSONObject jsonObject = songArray.getJSONObject(i);
                    //jsonObject.getString("album_type");
                    //Log.d("onPostExecute","consumed album_type");
                    JSONArray songArtistsArray = jsonObject.getJSONArray("artists");
                    //Log.d("onPostExecute","made array");
                    ArrayList<Artist> songArtists = new ArrayList<>();
                    if (songArtistsArray.length() != 0) {
                        for (int j = 0; j < songArtistsArray.length(); j++) {
                            JSONObject artistObject = songArtistsArray.getJSONObject(j);
                            Artist artist = new Artist(artistObject.getString("name"), artistObject.getString("id"));
                            songArtists.add(artist);
                        }
                    }
                    Song song = new Song(jsonObject.getString("name"), songArtists);
                    songs.add(song);
                }
            } else {
                //textResults.setText("No results");
                Song song = new Song("No results", null);
                songs.add(song);
            }
        }
        catch (JSONException e) {
            Log.d("onPostExecute", "JSON Exception");
            Log.d("onPostExecute", e.getMessage());
        }
        return songs;
    }
    public ArrayList<Music> parseAlbumSongs(JSONObject result) {
        ArrayList<Music> songs = new ArrayList<>();
        try {
            //Log.d("onPostExecute","made songs object");
            //Log.d("onPostExecute",albumList.toString());
            result.getString("href");
            // Log.d("onPostExecute","consumed href");
            JSONArray songArray = result.getJSONArray("items");
            //Log.d("onPostExecute","made array");
            if (songArray.length() != 0) {
                for (int i = 0; i < songArray.length(); i++) {
                    JSONObject jsonObject = songArray.getJSONObject(i);
                    //jsonObject.getString("album_type");
                    //Log.d("onPostExecute","consumed album_type");
                    JSONArray songArtistsArray = jsonObject.getJSONArray("artists");
                    //Log.d("onPostExecute","made array");
                    ArrayList<Artist> songArtists = new ArrayList<>();
                    if (songArtistsArray.length() != 0) {
                        for (int j = 0; j < songArtistsArray.length(); j++) {
                            JSONObject artistObject = songArtistsArray.getJSONObject(j);
                            Artist artist = new Artist(artistObject.getString("name"), artistObject.getString("id"));
                            songArtists.add(artist);
                        }
                    }
                    Song song = new Song(jsonObject.getString("name"), songArtists);
                    songs.add(song);
                }
            } else {
                //textResults.setText("No results");
                Song song = new Song("No results", null);
                songs.add(song);
            }
        }
        catch (JSONException e) {
            Log.d("onPostExecute", "JSON Exception");
            Log.d("onPostExecute", e.getMessage());
        }
        return songs;
    }
    public ArrayList<Music> parseArtistAlbums(JSONObject result) {
        ArrayList<Music> albums = new ArrayList<>();
        try {
            result.getString("href");
            //Log.d("onPostExecute","consumed href");
            JSONArray albumArray = result.getJSONArray("items");
            //Log.d("onPostExecute","made array");
            if (albumArray.length() != 0) {
                for (int i = 0; i < albumArray.length(); i++) {
                    JSONObject jsonObject = albumArray.getJSONObject(i);
                    jsonObject.getString("album_type");
                    //Log.d("onPostExecute","consumed album_type");
                    JSONArray albumArtistsArray = jsonObject.getJSONArray("artists");
                    //Log.d("onPostExecute","made array");
                    ArrayList<Artist> albumArtists = new ArrayList<>();
                    if (albumArtistsArray.length() != 0) {
                        for (int j = 0; j < albumArtistsArray.length(); j++) {
                            JSONObject artistObject = albumArtistsArray.getJSONObject(j);
                            Artist artist = new Artist(artistObject.getString("name"), artistObject.getString("id"));
                            albumArtists.add(artist);
                        }
                    }
                    Album album = new Album(jsonObject.getString("name"), jsonObject.getString("id"), albumArtists);
                    albums.add(album);
                }
            } else {
                //textResults.setText("No results");
                Album album = new Album("No results", "", null);
                albums.add(album);
            }
        }
        catch (JSONException e) {
            Log.d("onPostExecute", "JSON Exception");
            Log.d("onPostExecute", e.getMessage());
        }
        return albums;
    }


    public String getResponseText(InputStream inStream) {
        // very nice trick from
        // http://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
        return new Scanner(inStream).useDelimiter("\\A").next();
    }

//    public class Accel extends AsyncTask<String, Void, String> {
//        protected String doInBackground(String... text) {
//            while(accel < 12) {
//                try{
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    Log.e("Accel",e.getMessage());
//                }
//
//            }
//            return "";
//        }
//        protected void onPostExecute(String string) {
//            EditText editText = (EditText) findViewById(R.id.search);
//            Random random = new Random();
//            int low = 0;
//            int high = 99;
//            int result = random.nextInt(high-low) + low;
//            switch (searchType) {
//                case "artist":
//                    editText.setText(topArtists.get(result));
//                    break;
//                case "album":
//                    high = 199;
//                    result = random.nextInt(high-low) + low;
//                    editText.setText(topAlbums.get(result));
//                    break;
//                case "track":
//                    editText.setText(topSongs.get(result));
//                    break;
//            }
//            new Accel().execute("start");
//        }
//    }

    @Override
    protected void onStop() {
        super.onStop();
        //this.unregisterReceiver(gpsReceiver);

    }
    @Override
    protected void onResume() {
        super.onResume();
        //this.registerReceiver(gpsReceiver,new IntentFilter("GPS"));
        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), sensorManager.SENSOR_DELAY_NORMAL);
        String FILENAME = "previous_results";
        history.clear();
        try {
            FileInputStream fis = openFileInput(FILENAME);
            StringBuilder builder = new StringBuilder();
            int ch;
            while((ch = fis.read()) != -1){
                if((char)ch =='\n') {
                    history.add(builder.toString());
                    Log.d("Restoring from file", builder.toString());
                    builder.delete(0,builder.length());
                } else {
                    builder.append((char)ch);
                }
            }
            fis.close();
        }catch(Exception e) {
            Log.e("Restoring from file", "Failed");
        }

    }
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);

    }


    public void setSearchType(View view) {
        RadioButton radioButton = (RadioButton)view;
        String text = radioButton.getText().toString();
        text = text.toLowerCase();
        text = text.trim();
        if(text.equals("song")) {
            searchType = "track";
        } else {
            searchType = text;
        }
    }

    public void startService(View view) {
        Log.d("MainActivity", "starting service...");
        Intent intent = new Intent(this, GPSService.class);
        startService(intent);
    }

    public void stopService(View view) {
        Intent intent = new Intent(this, GPSService.class);
        stopService(intent);
    }

    public class GPSReceiver extends BroadcastReceiver {
        public GPSReceiver() {
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            coordinates = intent.getStringExtra("coordinates");
            Log.d("GPS receiver",coordinates);
            View view = (View)findViewById(R.id.search);
            stopService(view);
            String FILENAME = "previous_results";
            history.add("Query: " + historyQuery + "\t" + "Coordinates: " + coordinates);
            try {
                FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
                for(int i=0;i<history.size();i++) {
                    String string = history.get(i) + "\n";
                    Log.d("writing to file", string);
                    fos.write(string.getBytes());
                }
                fos.close();
                //history.clear();
            }catch(Exception e) {
                Log.e("Writing to file", e.getMessage());
            }
            Button historyButton = (Button)findViewById(R.id.button2);
            historyButton.setText("History");
            historyButton.setEnabled(true);
        }
    }

    public void openHistory(View view) {
        history.clear();
        Intent intent = new Intent(MainActivity.this, History.class);
        MainActivity.this.startActivity(intent);
    }

    public void aboutPopup(View view) {
        EditText search = (EditText)findViewById(R.id.search);
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(search.getWindowToken(), 0);
        new AlertDialog.Builder(this)
                .setTitle("About Lyricize")
                .setMessage("This app is a music tone analyzer tool. Use the Radio Buttons to select the type of search, then type your query into the search bar. The results are displayed in a clickable, scrollable list. Clicking on an artist repopulates the list with albums by that artist. Clicking on an album repopulates the list with songs on that album. Clicking on a song starts tone analysis on the lyrics, and displays the results on a new page. Can't come up with anything? Shake the device to get a random search!")
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        dialog.cancel();
                    }
                })
                .setIcon(android.R.drawable.ic_media_play)
                .show();
    }

}


