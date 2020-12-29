package com.example.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    RequestQueue requestQueue;//prepare a queue request  from the android
    WebView webView;
    SQLiteDatabase database;
    ListView listView ;
    ArrayAdapter<String> arrayAdapter;
    ArrayList<String> titlesArray = new ArrayList<>();
    ArrayList<String> contentArray = new ArrayList<>();


      public  void updateListView(){

          Log.d("suyash","UPDATED");
          Cursor cursor = database.rawQuery("SELECT * from  artical",null);

          int contentIndex = cursor.getColumnIndex("content");
          int  titleIndex = cursor.getColumnIndex("title");
     //testing
          if(cursor.moveToFirst()){
          titlesArray.clear();
          contentArray.clear();

          do {

            titlesArray.add(cursor.getString(titleIndex));
            contentArray.add(cursor.getString(contentIndex));

            }while (cursor.moveToNext());
               arrayAdapter.notifyDataSetChanged();
    }
}

    public class DownloadBackground extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... urls) {
            Log.d("suyash", "doInBackground: ran");
            String result = "";
            URL url;
            HttpURLConnection conn;
            try {
                url = new URL(urls[0]);
                conn = (HttpURLConnection) url.openConnection();
                InputStream in = conn.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while (data != -1) {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }

            }
            catch(Exception e){
                e.printStackTrace();
                return "Something went wrong";
            }
            return result;
        }
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d("suyash", "onPostExecute: ran");
           // Log.d("suyash", s);

            //webView.loadUrl("https://google.com");
          //  webView.loadData(s,"text/html","UTF-8");
        }
    }


    public void getNewsUrl(String newsId){
        String getNewsUrl = "https://hacker-news.firebaseio.com/v0/item/"+newsId+".json?print=pretty";
       // String articalTitle = "";

        // Log.d("check",getNewsUrl);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET
                , getNewsUrl, null, new
                Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                        try {
                           // Log.d("suyash","RESPONSE"+response.getString("title")+" "
                                  //  +response.getString("url"));
                             String articalTitle = response.getString("title");
                             String urls = response.getString("url");

                            DownloadBackground myTask = new DownloadBackground();
                            String articalContent ="";

                            if (!response.isNull("title") && !response.isNull("url")) {
                            articalContent =   myTask.execute(urls).get();
                            }

                            Log.d("suyash",articalContent);
                            //database.execSQL("Create table if not exists artical(id INTEGER PRIMARY KEY ,articalId INTEGER,title varchar,content varchar)");


                            String sql = "INSERT INTO artical (articalId ,title,content) Values(?,?,?)";
                            SQLiteStatement sqLiteStatement = database.compileStatement(sql);
                            sqLiteStatement.bindString(1,newsId);
                            sqLiteStatement.bindString(2,articalTitle);
                            sqLiteStatement.bindString(3,articalContent);
                            sqLiteStatement.execute();///Data is inserted into SQL DATA BASE
                            updateListView();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("suyash","response Error");

            }
        });
        requestQueue.add(jsonObjectRequest);

    }

    public void getNewsId(String url){


        JsonArrayRequest jsonArrayRequest  = new JsonArrayRequest(Request.Method.GET
                , url, null, new
                Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {

                        //testing
                        database.execSQL("Delete from artical");
                        int n = 2 ;

                        if(response.length()<n){
                            n = response.length();
                        }

                        for (int i = 0 ;i<n;i++){
                            try {
                                //Log.d("suyash",response.getString(i));
                                String newId = response.getString(i);
                                getNewsUrl(newId);
                            } catch (JSONException e) {
                                Log.d("suyash","NOT WORKING");
                                e.printStackTrace();
                            }
                        }
                    }

                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("suyash","response Error");

            }
        });
        requestQueue.add(jsonArrayRequest);

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database  = this.openOrCreateDatabase("Articals",MODE_PRIVATE,null);
        
         database.execSQL("Create table if not exists artical(id INTEGER PRIMARY KEY ,articalId INTEGER,title varchar,content varchar)");

        ListView listView = findViewById(R.id.titles);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titlesArray);
        listView.setAdapter(arrayAdapter);

        requestQueue = Volley.newRequestQueue(this);

        String newsIdUrl ="https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty";
        getNewsId(newsIdUrl);
        
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),Artical.class);
                intent.putExtra("content",contentArray.get(position));

                startActivity(intent);
            }
        });

        updateListView();
    }
}

