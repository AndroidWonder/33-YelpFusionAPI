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

import static android.R.attr.data;

public class MainActivity extends AppCompatActivity {

    private final String CLIENT_ID = "2zvUWE9erOVhGn8lLzdzuQ";
    private final String CLIENT_SECRET = "G4csiHa4ntUGNFJJtchEjxhvHXgEbkmCmKFPWeTVUwfgKaH5ZXfudIimP307Z8gZ";

// API constants
    private final String API_HOST = "https://api.yelp.com";
    private final String SEARCH_PATH = "/v3/businesses/search";
    private final String BUSINESS_PATH = "/v3/businesses/";  // Business ID will come after slash.
    private final String TOKEN_PATH = "/oauth2/token";
    private final String GRANT_TYPE = "client_credentials";

    private String ACCESS_TOKEN = null;

// Defaults for example
    private String TERM = "dinner";
    private String LOCATION = "San Diego, CA";
    private int SEARCH_LIMIT = 5;

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

        //using client id and client secret, get access token and then search
        Thread t = new Thread(AuthToken);
        t.start();

    }
    //this runnable object will first get an access token and then using it
    //do a Yelp search so there will be 2 commands issued to the Yelp API
     Runnable AuthToken = new Runnable(){
         public void run(){

             StringBuilder builder = new StringBuilder();

             InputStream is = null;

             String query_string = "?client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET +
                     "&grant_type=" + GRANT_TYPE;

             //first get access token
             try {
                 URL url = new URL(API_HOST + TOKEN_PATH  + query_string);
                 HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                 conn.setReadTimeout(10000 /* milliseconds */);
                 conn.setConnectTimeout(15000 /* milliseconds */);
                 conn.setRequestMethod("POST");
                 conn.setDoInput(true);
                 conn.setUseCaches(false);
                 conn.setRequestProperty("Content-Language", "en-US");
                 conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

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

             //decode JSON and get access token
             try {
                 JSONObject reader = new JSONObject(readJSONFeed);
                 ACCESS_TOKEN = reader.getString("access_token");
                 Log.e("JSON", ACCESS_TOKEN);

             } catch (JSONException e) {e.getMessage();
                 e.printStackTrace();
             }

             //now do search on same thread
             query_string = "?term=" + TERM + "&location=" + LOCATION + "&limit=" + SEARCH_LIMIT;

             try {
                 URL url = new URL(API_HOST + SEARCH_PATH  + query_string);
                 HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                 conn.setReadTimeout(10000 /* milliseconds */);
                 conn.setConnectTimeout(15000 /* milliseconds */);
                 conn.setRequestMethod("GET");
                 conn.setDoInput(true);
                 conn.setUseCaches(false);
                 conn.setRequestProperty("Content-Language", "en-US");
                 conn.setRequestProperty("authorization", "Bearer " + ACCESS_TOKEN);

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
             readJSONFeed = builder.toString();
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
