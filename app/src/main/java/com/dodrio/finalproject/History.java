package com.dodrio.finalproject;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
import java.util.Scanner;

import static com.dodrio.finalproject.MainActivity.BASE_MUSIXMATCH_URL;
import static com.dodrio.finalproject.MainActivity.MUSIXMATCH_API_KEY;

public class History extends AppCompatActivity {
    public ArrayList<String> historyList = new ArrayList<>();
    public ListView listView;
    public boolean MusixMatchFlag;
    public JSONObject result;
    public String historyQuery;
    ToneAnalyzer toneAnalyzer = new ToneAnalyzer(ToneAnalyzer.VERSION_DATE_2016_05_19);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        String FILENAME = "previous_results";
        historyList.clear();
        try {
            FileInputStream fis = openFileInput(FILENAME);
            StringBuilder builder = new StringBuilder();
            int ch;
            while((ch = fis.read()) != -1){
                if((char)ch =='\n') {
                    historyList.add(builder.toString());
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
        String[] listItems = new String[historyList.size()];
        for(int i = 0; i<historyList.size(); i++) {
            String string = historyList.get(i);
            string = string.replace('\t','\n');
            listItems[i] = string;
        }
        listView = (ListView)findViewById(R.id.history);
        ArrayAdapter adapter = new ArrayAdapter(History.this, android.R.layout.simple_list_item_1, listItems);
        //Log.d("onPostExecute",adapter.toString());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
                callMusixMatchAPI(view);
            }
        });
    }

    public void clearHistory(View view) {
        String FILENAME = "previous_results";
        try {
            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            String string = "";
            fos.write(string.getBytes());
            fos.close();
        }catch(Exception e) {
            Log.e("Writing to file", e.getMessage());
        }
        historyList.clear();
        String[] listItems = new String[0];
        listView = (ListView)findViewById(R.id.history);
        ArrayAdapter adapter = new ArrayAdapter(History.this, android.R.layout.simple_list_item_1, listItems);
        //Log.d("onPostExecute",adapter.toString());
        listView.setAdapter(adapter);
    }
    public void callMusixMatchAPI(View view) {
        String search = ((TextView) view).getText().toString();

        MusixMatchFlag = true;
        new History.CallMusixMatchAPI().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,search);
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
                String pieces[] = search.split("\n");
                String name = pieces[0].substring(6);
                historyQuery = name;
                String[] parts = name.split(" by ");
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
                        new History.CallMusixMatchAPI().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,track_id);
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
                        new History.CallWatsonAPI().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,lyrics_body);
                        Toast init = Toast.makeText(getApplicationContext(), "Loading...", Toast.LENGTH_SHORT);
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

            toneAnalyzer.setUsernameAndPassword("f7e02d6d-08e8-4a2c-8d1e-94416d05c40f","IOnwJZqpJ8sg");
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
                    Intent intent = new Intent(History.this, ResultsActivity.class);
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
                    Log.d("callwatsonapi",readableOutput);
                    intent.putExtra("Query",historyQuery);
                    History.this.startActivity(intent);
                } catch (JSONException e) {
                    Log.d("callwatsonapi", "JSON Exception");
                    Log.d("callwatsonapi", e.getMessage());
                }
            }
        }
    }

    public String getResponseText(InputStream inStream) {
        // very nice trick from
        // http://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
        return new Scanner(inStream).useDelimiter("\\A").next();
    }
}
