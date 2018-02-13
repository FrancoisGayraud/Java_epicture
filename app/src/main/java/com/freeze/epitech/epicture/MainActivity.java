package com.freeze.epitech.epicture;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class MainActivity extends AppCompatActivity {

    private int randomPage = 1;
    private int searchingPage = 1;
    public static final int REQUEST_CODE_PICK_IMAGE = 1001;
    private static final String AUTHORIZATION_URL = "https://api.imgur.com/oauth2/authorize";
    private static final String CLIENT_ID = "cc8e3c91dbda05e";
    private RelativeLayout rootView;
    private String accessToken;
    private String refreshToken;
    private String picturePath = "";
    private String uploadedImageUrl = "";
    private TextView tv;
    private ListView images;
    private String[] imagesTitle;
    private String[] imagesLink;
    private String username;
    private String imagesId[];
    private String keywords;
    private boolean searching = false;
    private boolean viewUpload = false;
    private Menu menu;
    private boolean flag_loading = false;
    private static final int READ_STORAGE_PERMISSION_REQUEST_CODE = 0;
    private LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = new TextView(this);
        rootView = findViewById(R.id.main_layout);
        images = new ListView(this);
        tv.setLayoutParams(llp);
        rootView.addView(tv);
        setContentView(rootView);

        String action = getIntent().getAction();

        if (action == null || !action.equals(Intent.ACTION_VIEW)) {
            tv.setText("OAuth Authorization");
            Uri uri = Uri.parse(AUTHORIZATION_URL).buildUpon()
                    .appendQueryParameter("client_id", CLIENT_ID)
                    .appendQueryParameter("response_type", "token")
                    .appendQueryParameter("state", "init")
                    .build();

            Intent intent = new Intent();
            intent.setData(uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();

        } else {
            if (!checkPermissionForReadExternalStorage())
                try {
                    requestPermissionForReadExternalStorage();
                } catch (Exception e) {
                    System.out.println("Error on request Permission for read external storage : " + e);
                }
            openActivity();
        }
    }

    public static Map<String, String> getQueryMap(String query) {
        int pos = query.indexOf("#");
        pos += 1;
        String epur = query.substring(pos);
        String[] params = epur.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
            System.out.println("QUERY ARGUMENT : " + name + " " + value);
        }
        return map;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (picturePath != null && picturePath.length() != 0) {
            (new UploadToImgurTask()).execute(picturePath);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("tag", "request code : " + requestCode + ", result code : " + resultCode);
        if (data == null) {
            Log.d("tag", "data is null");
        }
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PICK_IMAGE && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            picturePath = cursor.getString(columnIndex);
            Log.d("tag", "image path : " + picturePath);
            if (!uploadedImageUrl.isEmpty()) {
                System.out.println("UPLOADEDIMAGEURL : " + uploadedImageUrl);
            }
            cursor.close();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("StaticFieldLeak")
    class UploadToImgurTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            final String upload_to = "https://api.imgur.com/3/upload";
            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            HttpPost httpPost = new HttpPost(upload_to);
            try {
                HttpEntity entity = MultipartEntityBuilder.create()
                        .addPart("image", new FileBody(new File(params[0])))
                        .build();
                httpPost.setHeader("Authorization", "Bearer " + accessToken);
                httpPost.setEntity(entity);
                final HttpResponse response = httpClient.execute(httpPost,
                        localContext);
                final String response_string = EntityUtils.toString(response
                        .getEntity());
                final JSONObject json = new JSONObject(response_string);
                Log.d("tag", json.toString());
                JSONObject data = json.optJSONObject("data");
                uploadedImageUrl = data.optString("link");
                Log.d("tag", "uploaded image url : " + uploadedImageUrl);
                viewUpload = true;
                picturePath = "";
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean.booleanValue()) {
                        if (viewUpload) {
                            Intent intent = new Intent();
                            intent.setData(Uri.parse(uploadedImageUrl));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            viewUpload = false;
                        }
                    }
            }
        }

    @SuppressLint("StaticFieldLeak")
    class AddFavorites extends AsyncTask<String, Void, Boolean> {

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

    @SuppressLint("StaticFieldLeak")
    class GetMyGallery extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            final String gallery = "https://api.imgur.com/3/gallery/random/random/" + params[0];
            try {
                final JSONObject json;
                URL reqURL = new URL(gallery);
                HttpURLConnection request = (HttpURLConnection) (reqURL.openConnection());
                request.setRequestMethod("GET");
                request.setRequestProperty ("Authorization", "Bearer " + accessToken);
                request.connect();
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(request.getInputStream()));
                String inputLine;

                inputLine = in.readLine();
                json = new JSONObject(inputLine);
                JSONArray data = json.getJSONArray("data");
                in.close();
                int length = data.length();
                int count = 0;
                for (int i = 0; i < length; i++) {
                    JSONObject photo = data.getJSONObject(i);
                    if (!photo.getBoolean("is_album")) {
                        if (!photo.getString("type").equals("image/gif")) {
                            count++;
                        }
                    }
                }
                imagesLink = new String[count];
                imagesTitle = new String[count];
                imagesId = new String[count];
                count = 0;
                for (int i = 0; i < length; i++) {
                    JSONObject photo = data.getJSONObject(i);
                    if (!photo.getBoolean("is_album")) {
                        if (!photo.getString("type").equals("image/gif")) {
                            imagesLink[count] = photo.getString("link");
                            imagesId[count] = photo.getString("id");
                            if (photo.getString("title") != null)
                                imagesTitle[count] = photo.getString("title");
                            else
                                imagesTitle[count] = "No title";
                            count++;
                            System.out.println(photo.toString());
                        }
                    }
                }
                randomPage += 1;
                if (randomPage == 50)
                    randomPage = 1;
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            FloatingActionButton fab = findViewById(R.id.uploadImage);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
                }
            });
            images.setAdapter(new AdapterImgur(MainActivity.this, imagesLink, imagesTitle));
            images.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position,
                                        long id) {
                    System.out.println("CLICK ON : " + id + " " + position + " " + imagesLink[position] + " " + imagesId[position]);
                    Snackbar mySnackbar = Snackbar.make(findViewById(R.id.main_layout),
                            R.string.ask_favorite, Snackbar.LENGTH_SHORT);
                    mySnackbar.setAction(R.string.ok, new FavoriteImgur(imagesId[position]));
                    mySnackbar.show();
                }
            });
            rootView.removeView(images);
            rootView.addView(images);
            flag_loading = false;
        }
    }

    @SuppressLint("StaticFieldLeak")
    class SearchImgur extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            final String search = "https://api.imgur.com/3/gallery/search/" + params[1] + "?q=" + params[0];
            try {
                final JSONObject json;
                URL reqURL = new URL(search);
                HttpURLConnection request = (HttpURLConnection) (reqURL.openConnection());
                request.setRequestMethod("GET");
                request.setRequestProperty("Authorization", "Bearer " + accessToken);
                request.connect();
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(request.getInputStream()));
                String inputLine;

                inputLine = in.readLine();
                json = new JSONObject(inputLine);
                System.out.println("RESPONSE SEARCH : " + json.toString());
                JSONArray data = json.getJSONArray("data");
                in.close();
                int length = data.length();
                int count = 0;
                for (int i = 0; i < length; i++) {
                    JSONObject photo = data.getJSONObject(i);
                    if (!photo.getBoolean("is_album")) {
                        if (!photo.getString("type").equals("image/gif")) {
                            count++;
                        }
                    }
                    else {
                        JSONArray image = photo.getJSONArray("images");
                        JSONObject finale = image.getJSONObject(0);
                        if (!finale.getString("type").equals("image/gif")) {
                            count++;
                        }
                    }
                }
                imagesLink = new String[count];
                imagesId = new String[count];
                imagesTitle = new String[count];
                count = 0;
                for (int i = 0; i < length; i++) {
                    JSONObject photo = data.getJSONObject(i);
                    if (!photo.getBoolean("is_album")) {
                        if (!photo.getString("type").equals("image/gif")) {
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
                    else {
                        JSONArray image = photo.getJSONArray("images");
                        JSONObject finale = image.getJSONObject(0);
                        if (!finale.getString("type").equals("image/gif")) {
                            imagesLink[count] = finale.getString("link");
                            imagesId[count] = finale.getString("id");
                            if (photo.getString("title") != null)
                                imagesTitle[count] = finale.getString("title");
                            else
                                imagesTitle[count] = "No title";
                            System.out.println(image.toString());
                            count++;
                        }
                    }
                }
                searching = true;
                searchingPage += 1;
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
            images.setAdapter(new AdapterImgur(MainActivity.this, imagesLink, imagesTitle));
            images.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position,
                                        long id) {
                    System.out.println("CLICK ON : " + id + " " + position + " " + imagesLink[position] + " " + imagesId[position]);
                    Snackbar mySnackbar = Snackbar.make(findViewById(R.id.main_layout),
                            R.string.ask_favorite, Snackbar.LENGTH_SHORT);
                    mySnackbar.setAction(R.string.ok, new FavoriteImgur(imagesId[position]));
                    mySnackbar.show();
                }
            });
            rootView.removeView(images);
            rootView.addView(images);
            flag_loading = false;
        }
    }


    private void openActivity() {

        Uri uri = getIntent().getData();
        System.out.println("URI : " + uri.toString());
        //String uriString = uri.toString();
        //String paramsString = "http://callback?" + uriString.substring(uriString.indexOf("#") + 1);
        Map<String, String> params = getQueryMap(uri.toString());
        accessToken = params.get("access_token");
        refreshToken = params.get("refresh_token");
        username = params.get("account_username");
        images.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {

                if(firstVisibleItem+visibleItemCount == totalItemCount && totalItemCount!=0)
                {
                    if(!flag_loading) {
                        flag_loading = true;
                        if (!searching) {
                            new GetMyGallery().execute(String.valueOf(randomPage));
                        }
                        else
                            new SearchImgur().execute(keywords, String.valueOf(searchingPage));
                        System.out.println("END OF LISTVIEW");
                    }
                }
            }
        });
        new GetMyGallery().execute(String.valueOf(randomPage));

    }

    public class FavoriteImgur implements View.OnClickListener{

        private String idImage;
        FavoriteImgur(String id) {
            idImage = id;
        }
        @Override
        public void onClick(View v) {
            System.out.println("IN CLICK FAVORITE LISTENER");
            new AddFavorites().execute(idImage);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.favorites:
                String token = accessToken;
                Bundle bundle = new Bundle();
                bundle.putString("accessToken", token);
                bundle.putString("username", username);
                Intent intent = new Intent(this, FavoritesActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case R.id.home:
                String tok = accessToken;
                Bundle bun = new Bundle();
                bun.putString("accessToken", tok);
                bun.putString("username", username);
                Intent i = new Intent(this, Gallery.class);
                i.putExtras(bun);
                startActivity(i);
                break;
            case R.id.search:
                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(20, 0, 30, 0);
                final EditText search = new EditText(this);
                search.setHint(" Search a gallery");
                layout.addView(search, params);
                new AlertDialog.Builder(this)
                        .setTitle("Browse Imgur")
                        .setView(layout)
                        .setPositiveButton("Go", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                searchingPage = 1;
                                String searching = search.getText().toString();
                                keywords = searching;
                                System.out.println("GOIN TO SEARCH FOR : " + searching);
                                new SearchImgur().execute(searching, String.valueOf(searchingPage));
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .show();
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean checkPermissionForReadExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            System.out.println("IN CHECK 1");
            int result = this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    public void requestPermissionForReadExternalStorage() throws Exception {
        try {
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_STORAGE_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}