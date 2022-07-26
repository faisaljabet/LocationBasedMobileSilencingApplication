package com.example.mobilesilencer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.GsonBuildConfig;

public class ShowMosqueInfo extends MainActivity {

    private static final String url = "http://10.23.174.173:8080/MobileSilencer/json_user_fetch.php";
    private RecyclerView recview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_mosque_info);
        initView();
        processData();
    }

    private void initView() {
        recview = findViewById(R.id.recview);
        recview.setLayoutManager(new LinearLayoutManager(this));
    }

    private void processData() {
        StringRequest request = new StringRequest(url, new Response.Listener<String>(){
            public void onResponse(String response){
                GsonBuilder builder = new GsonBuilder();
                Gson gson = builder.create();
                MosqueInfo data[] = gson.fromJson(response, MosqueInfo[].class);

                RecyclerViewAdapter adapter = new RecyclerViewAdapter(data);
                recview.setAdapter(adapter);
            }
        }, new Response.ErrorListener(){
            public void onErrorResponse(VolleyError error){
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
            }
        });
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        queue.add(request);
    }
}