package com.freeze.epitech.epicture;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Gallery extends AppCompatActivity {

    private String[] imagesLink;
    private String[] imagesId;
    private String[] imagesTitle;
    private RelativeLayout rootView;
    private Bundle bundle;
    private String accessToken;
    private String username;
    private static final String CLIENT_ID = "cc8e3c91dbda05e";
    private ListView images;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_gallery);

        rootView = findViewById(R.id.gallery_layout);
        images = new ListView(this);
        bundle = getIntent().getExtras();
        accessToken = bundle.getString("accessToken");
        username = bundle.getString("username");
        new GetMyImages().execute();
    }

    class GetMyImages extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            final String gallery = "https://api.imgur.com/3/account/me/images";
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
                System.out.println("MY IMAGES : " + json.toString());
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
                            imagesLink[count] = photo.getString("link");
                            imagesId[count] = photo.getString("id");
                            if (photo.getString("title").equals(""))
                                imagesTitle[count] = "No title";
                            else
                                imagesTitle[count] = photo.getString("title");
                            System.out.println(photo.toString());
                            count++;
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            rootView.removeView(images);
            images.setAdapter(new AdapterImgur(Gallery.this, imagesLink, imagesTitle));
            rootView.addView(images);
            setContentView(rootView);
        }
    }

}
