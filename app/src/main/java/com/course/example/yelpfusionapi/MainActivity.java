package com.course.example.yelpfusionapi;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private final String
            API_KEY = "TSKoW6Rmk9zXWMZP_-t6BeC0BPTPBbFxZR3I7GYSxs6UHnMiN3hOwDeNFkvNqx3d7S5f9e4Qt7iFEoZ6b_wWE4W-k1EsHpqyBktOS4lMR4M8zcyrMzO9BMpezECgWnYx";

// API constants
    private final String API_HOST = "https://api.yelp.com";
    private final String SEARCH_PATH = "/v3/businesses/search";
    private final String BUSINESS_PATH = "/v3/businesses/";  // Business ID will come after slash.
    private final String TOKEN_PATH = "/oauth2/token";
    private final String GRANT_TYPE = "client_credentials";

    private String ACCESS_TOKEN = null;

// Defaults for example
    private String TERM = "Italian";
    private String LOCATION = "Brooklyn, NY";
   // private String LOCATION = "San Francisco, CA";
    private int SEARCH_LIMIT = 15;

    private TextView text = null;

    //messages from background thread contain data for UI
    Handler handler = new Handler(){
        public void handleMessage(Message msg) {
            String title =(String) msg.obj;
            text.append(title + "\n" +"\n");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text=(TextView)findViewById(R.id.texter);


        Thread t = new Thread(AuthToken);
        t.start();

    }

     Runnable AuthToken = new Runnable(){
         public void run(){

             StringBuilder builder = new StringBuilder();

             InputStream is = null;

             //now do search on same thread
             String query_string = "?term=" + TERM + "&location=" + LOCATION + "&limit=" + SEARCH_LIMIT;

             try {
                 URL url = new URL(API_HOST + SEARCH_PATH  + query_string);
                 HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                 conn.setReadTimeout(10000 /* milliseconds */);
                 conn.setConnectTimeout(15000 /* milliseconds */);
                 conn.setRequestMethod("GET");
                 conn.setDoInput(true);
                 conn.setUseCaches(false);
                 conn.setRequestProperty("Content-Language", "en-US");
                 conn.setRequestProperty("authorization", "Bearer " + API_KEY);

                 // Starts the query
                 conn.connect();
                 int response = conn.getResponseCode();
                 Log.e("JSON", "The response is: " + response);

                 //if response code not 200, end thread
                 if (response != 200) return;
                 is = conn.getInputStream();

                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(is));
                 String line;
                 builder = new StringBuilder();
                 while ((line = reader.readLine()) != null) {
                     builder.append(line);
                     Log.e("JSON", line);
                 }

                 // Makes sure that the InputStream is closed after the app is
                 // finished using it.
             }	catch(IOException e) {}
             finally {
                 if (is != null) {
                     try {
                         is.close();
                     } catch(IOException e) {}
                 }
             }

             //convert StringBuilder to String
            String readJSONFeed = builder.toString();
             Log.e("JSON", readJSONFeed);

             //decode JSON and get search results
             try {
                 JSONObject obj = new JSONObject(readJSONFeed);
                 JSONArray businesses = new JSONArray();
                 businesses = obj.getJSONArray("businesses");

                 for (int i = 0; i < businesses.length(); i++){

                     JSONObject place = businesses.getJSONObject(i);
                     String name = place.getString("name");
                     String rating = place.getString("rating");
                     String review = place.getString("review_count");

                     String value = name + ", rating " + rating + ", " + review + " reviews";

                     //sent to Handler queue
                     Message msg = handler.obtainMessage();
                     msg.obj = value;
                     handler.sendMessage(msg);

                     Log.e("JSON", value);

                 }

             } catch (JSONException e) {e.getMessage();
                 e.printStackTrace();
             }

         }
     } ;


}
