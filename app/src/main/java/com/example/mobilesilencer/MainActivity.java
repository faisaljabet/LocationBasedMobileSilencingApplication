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
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

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
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    boolean isPermissionGranted;
    GoogleMap mGoogleMap;
    private Button addNewMosque;
    private FusedLocationProviderClient mLocationClient;
    private int GPS_REQUEST_CODE = 9001;

    private double currentLatitude;
    private double currentLongitude;
    private double currMosqueLat = 0;
    private double currMosqueLng = 0;

    //Audio mode
    private AudioManager am;
    private boolean insideMosque = false;

    //Run on background
    private Thread thread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isGPSenable();
        checkMyPermission();
        mLocationClient = new FusedLocationProviderClient(this);
        checkAndUpdateAudioMode();
        addNewMosque = findViewById(R.id.addNewMosque);
        addNewMosque.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                startActivity(new Intent(MainActivity.this, AddMosque.class));
            }
        });
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

    private void updateAudioMode(){
        setCurrentLocation();
        // Read a message from the database
        DatabaseReference mosqueLocation = FirebaseDatabase.getInstance().getReference().child("MosqueLocation");
        mosqueLocation.addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
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
//                    Log.d("insideMosque", "");
//                    Log.d("currentLatitude", "" + currentLatitude);
//                    Log.d("currMosqueLat", "" + currMosqueLat);
//                    Log.d("currentLongitude", "" + currentLongitude);
//                    Log.d("currMosqueLng", "" + currMosqueLng);
//                    Log.d("Distance", "" + (c * r));
                    // calculate the result
                    if (c * r > 10) {
                        am.setRingerMode(2);
                        insideMosque = false;
                    }
                    else{
                        am.setRingerMode(1);
                    }
                } else {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        MosqueLocation mosqueLocation = snapshot.getValue(MosqueLocation.class);
                        double mosqueLatitude = Double.parseDouble(mosqueLocation.getLatitude());
                        double mosqueLongitude = Double.parseDouble(mosqueLocation.getLongitude());

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
                        // calculate the result
                        if (c * r <= 10) {
                            am.setRingerMode(1);
                            insideMosque = true;
                            currMosqueLat = mosqueLatitude;
                            currMosqueLng = mosqueLongitude;
//                            Log.d("Not insideMosque", "");
//                            Log.d("currentLatitude", "" + currentLatitude);
//                            Log.d("mosqueLatitude", "" + mosqueLatitude);
//                            Log.d("currentLongitude", "" + currentLongitude);
//                            Log.d("mosqueLongitude", "" + mosqueLongitude);
//                            Log.d("Distance", "" + (c * r));
                            break;
                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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