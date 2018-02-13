package com.freeze.epitech.epicture;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;

import android.support.design.widget.Snackbar;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by redleader on 11/02/2018.
 */

public class FavoritesActivity extends AppCompatActivity {

    private Bundle bundle;
    private String accessToken;
    private String username;
    private String[] imagesLink;
    private ListView images;
    private RelativeLayout rootView;
    private int favoritePage = 1;
    private String[] imagesTitle;
    private String[] imagesId;
    private boolean flag_loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        rootView = findViewById(R.id.favorites_layout);
        images = new ListView(this);
        bundle = getIntent().getExtras();
        accessToken = bundle.getString("accessToken");
        username = bundle.getString("username");
        System.out.println("ACCESS TOKEN " + accessToken + " USERNAME " + username);
        images.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                System.out.println("CLICK ON : " + id + " " + position + " " + imagesLink[position] + " " + imagesId[position]);
                Snackbar mySnackbar = Snackbar.make(findViewById(R.id.favorites_layout),
                        R.string.ask_remove_favorite, Snackbar.LENGTH_SHORT);
                mySnackbar.setAction(R.string.ok, new FavoritesActivity.UnfavoriteImgur(imagesId[position]));
                mySnackbar.show();
            }
        });
        new GetFavorites().execute(String.valueOf(favoritePage));

    }

    public class UnfavoriteImgur implements View.OnClickListener{

        private String idImage;
        UnfavoriteImgur(String id) {
            idImage = id;
        }
        @Override
        public void onClick(View v) {
            System.out.println("IN CLICK UNFAVORITE LISTENER");
            new RemoveFavorites().execute(idImage);
        }
    }

    @SuppressLint("StaticFieldLeak")
    class RemoveFavorites extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            final String fav = "https://api.imgur.com/3/image/" + params[0] + "/favorite/";
            try {
                final JSONObject json;
                URL reqURL = new URL(fav);
                HttpURLConnection request = (HttpURLConnection) (reqURL.openConnection());
                request.setRequestMethod("POST");
                request.setRequestProperty("Authorization", "Bearer " + accessToken);
                request.connect();
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(request.getInputStream()));
                String inputLine;

                inputLine = in.readLine();
                json = new JSONObject(inputLine);
                System.out.println("RESPONSE FAVORITE : " + json.toString());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return true;
        }
        @Override
        protected void onPostExecute(Boolean aBoolean) {
            System.out.println("POSTEXECUTE");
        }
    }


    class GetFavorites extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            final String gallery = "https://api.imgur.com/3/account/" + username + "/favorites/";
            try {
                final JSONObject json;
                URL reqURL = new URL(gallery);
                HttpURLConnection request = (HttpURLConnection) (reqURL.openConnection());
                request.setRequestMethod("GET");
                request.setRequestProperty("Authorization", "Bearer " + accessToken);
                request.connect();
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(request.getInputStream()));
                String inputLine;

                inputLine = in.readLine();
                System.out.println("INPUTLINE : " + inputLine);
                json = new JSONObject(inputLine);
                JSONArray data = json.getJSONArray("data");
                in.close();
                System.out.println("FAVORITES : " + json.toString());
                int length = data.length();
                int count = 0;
                System.out.println("LENGTH : " + length);
                for (int i = 0; i < length; i++) {
                    count++;
                }
                imagesLink = new String[count];
                imagesTitle = new String[count];
                imagesId = new String[count];
                count = 0;
                for (int i = 0; i < length; i++) {
                    JSONObject photo = data.getJSONObject(i);
                    {
                        if (photo.getString("is_album").equals("true")) {
                            JSONArray image = photo.getJSONArray("images");
                            JSONObject finale = image.getJSONObject(0);
                            imagesLink[count] = finale.getString("link");
                            if (photo.getString("title") != null)
                                imagesTitle[count] = finale.getString("title");
                            else
                                imagesTitle[count] = "No title";                            imagesId[count] = finale.getString("id");
                            System.out.println(image.toString());
                            count++;
                        }
                        else {
                            imagesLink[count] = photo.getString("link");
                            imagesId[count] = photo.getString("id");
                            if (photo.getString("title") != null)
                                imagesTitle[count] = photo.getString("title");
                            else
                                imagesTitle[count] = "No title";
                            System.out.println(photo.toString());
                            count++;
                        }
                    }
                }
                favoritePage += 1;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            rootView.removeView(images);
            images.setAdapter(new AdapterImgur(FavoritesActivity.this, imagesLink, imagesTitle));
            rootView.addView(images);
            setContentView(rootView);
        }
    }
}