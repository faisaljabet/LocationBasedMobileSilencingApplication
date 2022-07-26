package com.example.mobilesilencer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.LocusId;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.GsonBuildConfig;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    boolean isPermissionGranted;
    GoogleMap mGoogleMap;
    private Button openMap, mosqueList;
    private FusedLocationProviderClient mLocationClient;
    private int GPS_REQUEST_CODE = 9001;

    private double currentLatitude;
    private double currentLongitude;
    private double currMosqueLat = 0;
    private double currMosqueLng = 0;
    private MosqueLocation[] mosqueLocationArray;
    private int mosqueLocationArraySize;

    //Audio mode
    private AudioManager am;
    private boolean insideMosque = false;

    //Run on background
    private Thread thread = null;

    //URL of json_user_fetch.php file in localhost
    private static final String url = "http://10.23.174.173:8080/MobileSilencer/json_user_fetch.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isGPSenable();
        checkMyPermission();
        mLocationClient = new FusedLocationProviderClient(this);
        readMosqueLocationsFromFirebase();
//        Log.d("Latitude", ""+1.0111);
//        readMosqueLocationFromDatabase();
//        Log.d("Latitude", ""+2.0111);
        Log.d("Latitude", ""+1.0111);
        getJSON(url);
        Log.d("Latitude", ""+2.0111);
        checkAndUpdateAudioMode();
        openMap = findViewById(R.id.openMap);
        mosqueList = findViewById(R.id.mosqueList);

        openMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                startActivity(new Intent(MainActivity.this, AddMosque.class));
            }
        });
        mosqueList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                startActivity(new Intent(MainActivity.this, ShowMosqueInfo.class));
            }
        });
    }

    //this method is actually fetching the json string
    private void getJSON(final String urlWebService) {
        /*
         * As fetching the json string is a network operation
         * And we cannot perform a network operation in main thread
         * so we need an AsyncTask
         * The constrains defined here are
         * Void -> We are not passing anything
         * Void -> Nothing at progress update as well
         * String -> After completion it should return a string and it will be the json string
         * */
        class GetJSON extends AsyncTask<Void, Void, String> {

            //this method will be called before execution
            //you can display a progress bar or something
            //so that user can understand that he should wait
            //as network operation may take some time
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            //this method will be called after execution
            //so here we are displaying a toast with the json string
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                //Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
                try {
                    loadIntoArray(s);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            //in this method we are fetching the json string
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    //creating a URL
                    URL url = new URL(urlWebService);

                    //Opening the URL using HttpURLConnection
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();

                    //StringBuilder object to read the string from the service
                    StringBuilder sb = new StringBuilder();

                    //We will use a buffered reader to read the string from service
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                    //A simple string to read values from each line
                    String json;

                    //reading until we don't find null
                    while ((json = bufferedReader.readLine()) != null) {

                        //appending it to string builder
                        sb.append(json + "\n");
                    }

                    //finally returning the read string
                    return sb.toString().trim();
                } catch (Exception e) {
                    return null;
                }
            }
        }

        //creating asynctask object and executing it
        GetJSON getJSON = new GetJSON();
        getJSON.execute();
    }

    private void loadIntoArray(String json) throws JSONException {
        //creating a json array from the json string
        JSONArray jsonArray = new JSONArray(json);

        mosqueLocationArray = new MosqueLocation[jsonArray.length()];

        //looping through all the elements in json array
        for (int i = 0; i < jsonArray.length(); i++) {

            //getting json object from the json array
            JSONObject obj = jsonArray.getJSONObject(i);

            MosqueLocation mosqueLocation = new MosqueLocation(obj.getString("latitude"), obj.getString("longitude"));

            //getting the name from the json object and putting it inside string array
            mosqueLocationArray[i] = mosqueLocation;
            Log.d("Location", "mosqueLocationArray["+i+"].getLatitude() = "+mosqueLocationArray[i].getLatitude());
            Log.d("Location", "mosqueLocationArray["+i+"].getLongitude() = "+mosqueLocationArray[i].getLongitude());
        }
    }

    private void readMosqueLocationFromDatabase() {

        StringRequest request = new StringRequest(url, new Response.Listener<String>(){
            public void onResponse(String response){
                GsonBuilder builder = new GsonBuilder();
                Gson gson = builder.create();
                MosqueInfo data[] = gson.fromJson(response, MosqueInfo[].class);
                int size = data.length;
                Log.d("Length", ""+size);

                mosqueLocationArray = new MosqueLocation[size];
                for(int i=0; i<size; i++)
                {
                    String lat = data[i].getLatitude(), lng = data[i].getLongitude();
                    Log.d("Latitude", lat);
                    Log.d("Longitude", lng);
                    MosqueLocation mosqueLocation = new MosqueLocation(lat, lng);
                    mosqueLocationArray[i] = mosqueLocation;
                    Log.d("Location", "mosqueLocationArray["+i+"].getLatitude() = "+mosqueLocationArray[i].getLatitude());
                    Log.d("Location", "mosqueLocationArray["+i+"].getLongitude() = "+mosqueLocationArray[i].getLongitude());
                }
            }
        }, new Response.ErrorListener(){
            public void onErrorResponse(VolleyError error){
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
            }
        });

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        queue.add(request);
    }

    private void checkAndUpdateAudioMode() {
        thread = new Thread((new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        updateAudioMode();
                        Thread.sleep(10000);
                    } catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }));
        thread.start();
    }

    private void readMosqueLocationsFromFirebase()
    {
        // Read a message from the database
        DatabaseReference mosqueLocation = FirebaseDatabase.getInstance().getReference().child("MosqueLocation");
        mosqueLocation.addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mosqueLocationArraySize = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) { mosqueLocationArraySize++; }
                mosqueLocationArray = new MosqueLocation[mosqueLocationArraySize];
                int i = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    mosqueLocationArray[i] = snapshot.getValue(MosqueLocation.class);
//                    Log.d("Location", "mosqueLocationArray["+i+"].getLatitude() ="+mosqueLocationArray[i].getLatitude());
//                    Log.d("Location", "mosqueLocationArray["+i+"].getLongitude() ="+mosqueLocationArray[i].getLongitude());
                    i++;
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void updateAudioMode(){
        setCurrentLocation();

        double radDiffOfLat = 0;
        double radDiffOfLng = 0;
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (insideMosque) {
            radDiffOfLat = Math.toRadians(currMosqueLat - currentLatitude);
            radDiffOfLng = Math.toRadians(currMosqueLng - currentLongitude);
            double radCurrMosqueLat = Math.toRadians(currMosqueLat);
            double radCurrentLatitude = Math.toRadians(currentLatitude);
            double a = Math.pow(Math.sin(radDiffOfLat / 2), 2)
                    + Math.cos(radCurrentLatitude) * Math.cos(radCurrMosqueLat)
                    * Math.pow(Math.sin(radDiffOfLng / 2), 2);
            double c = 2 * Math.asin(Math.sqrt(a));
            // Radius of earth in meters. Use 3956
            // for miles
            double r = 6371000;
                    Log.d("insideMosque", "");
                    Log.d("currentLatitude", "" + currentLatitude);
                    Log.d("currMosqueLat", "" + currMosqueLat);
                    Log.d("currentLongitude", "" + currentLongitude);
                    Log.d("currMosqueLng", "" + currMosqueLng);
                    Log.d("Distance", "" + (c * r));
            // calculate the result
            if (c * r > 10) {
                am.setRingerMode(2);
                insideMosque = false;
            }
            else{
                am.setRingerMode(1);
            }
        } else {
            for (int i=0; i<mosqueLocationArraySize; i++) {
                double mosqueLatitude = Double.parseDouble(mosqueLocationArray[i].getLatitude());
                double mosqueLongitude = Double.parseDouble(mosqueLocationArray[i].getLongitude());

                radDiffOfLat = Math.toRadians(mosqueLatitude - currentLatitude);
                radDiffOfLng = Math.toRadians(mosqueLongitude - currentLongitude);
                double radMosqueLatitude = Math.toRadians(mosqueLatitude);
                double radCurrentLatitude = Math.toRadians(currentLatitude);
                double a = Math.pow(Math.sin(radDiffOfLat / 2), 2)
                        + Math.cos(radCurrentLatitude) * Math.cos(radMosqueLatitude)
                        * Math.pow(Math.sin(radDiffOfLng / 2), 2);
                double c = 2 * Math.asin(Math.sqrt(a));
                // Radius of earth in meters. Use 3956
                // for miles
                double r = 6371000;

                Log.d("Not insideMosque", "");
                Log.d("currentLatitude", "" + currentLatitude);
                Log.d("mosqueLatitude", "" + mosqueLatitude);
                Log.d("currentLongitude", "" + currentLongitude);
                Log.d("mosqueLongitude", "" + mosqueLongitude);
                Log.d("Distance", "" + (c * r));
                // calculate the result
                if (c * r <= 10) {
                    am.setRingerMode(1);
                    insideMosque = true;
                    currMosqueLat = mosqueLatitude;
                    currMosqueLng = mosqueLongitude;

                    break;
                }

            }
        }
    }

    @SuppressLint("MissingPermission")
    private void setCurrentLocation(){
        LatLng latLng = null;
        mLocationClient.getLastLocation().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Location location = task.getResult();
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();
            }
        });
    }

    private boolean isGPSenable() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        boolean providerEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(providerEnable){
            return true;
        } else{
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle("GPS Permission")
                    .setMessage("GPS is required for this app to operate. Please enable GPS")
                    .setPositiveButton("Yes", ((dialogInterface, i) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, GPS_REQUEST_CODE);
                    }))
                    .setCancelable(false)
                    .show();
        }
        return false;
    }

    private void checkMyPermission() {

        Dexter.withContext(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                isPermissionGranted = true;
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), "");
                intent.setData(uri);
                startActivity(intent);
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {}

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GPS_REQUEST_CODE){
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            boolean providerEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if(providerEnable){
                Toast.makeText(this, "GPS is enable", Toast.LENGTH_SHORT).show();
            } else{
                Toast.makeText(this, "GPS is not enable", Toast.LENGTH_SHORT).show();
            }
        }
    }
}