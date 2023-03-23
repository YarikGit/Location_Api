package com.example.locationapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int CHECK_SETTINGS_CODE = 111;
    private static final int REQUEST_LOCATION_PERMISSION = 222;

    private Button startLocationUpdateButton, stopLocationUpdateButton;
    private TextView locationTextView;
    private TextView locationUpdateTimeTextView;

    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;

    private boolean isLocationUpdateActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startLocationUpdateButton = findViewById(R.id.startLocationUpdateButton);
        stopLocationUpdateButton = findViewById(R.id.stopLocationUpdateButton);
        locationTextView = findViewById(R.id.locationTextView);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationUpdateTimeTextView = findViewById(R.id.locationUpdateTimeTextView);
        settingsClient = LocationServices.getSettingsClient(this);

        buildLocationRequest();
        buildLocationCallback();
        buildLocationSettingsRequest();

        startLocationUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLocationUpdate();
            }
        });

        stopLocationUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationUpdate();
            }
        });

    }

    private void startLocationUpdate() {
        isLocationUpdateActive = true;
        startLocationUpdateButton.setEnabled(false);
        stopLocationUpdateButton.setEnabled(true);

        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                                android.Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(MainActivity.this,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION)
                                        != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        fusedLocationClient.requestLocationUpdates(locationRequest
                                , locationCallback
                                , Looper.myLooper());
                        updateLocationUi();
                    }
                }).addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        int statusCode = ((ApiException) e).getStatusCode();

                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                try {
                                    ResolvableApiException resolvableApiException =
                                            (ResolvableApiException) e;
                                    resolvableApiException.startResolutionForResult(MainActivity.this,
                                            CHECK_SETTINGS_CODE);
                                } catch (IntentSender.SendIntentException sie) {
                                    sie.printStackTrace();
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String message = "Adjust location settings on your device";
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();

                                isLocationUpdateActive = false;
                                startLocationUpdateButton.setEnabled(true);
                                stopLocationUpdateButton.setEnabled(false);


                        }
                        updateLocationUi();

                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CHECK_SETTINGS_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.d("MainActivity", "User has agreed to change location settings");
                        startLocationUpdate();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.d("MainActivity", "User has not agreed to change location settings");
                        isLocationUpdateActive = false;
                        startLocationUpdateButton.setEnabled(true);
                        stopLocationUpdateButton.setEnabled(false);
                        updateLocationUi();
                        break;
                }
                break;
        }
    }

    private void stopLocationUpdate() {

        if(!isLocationUpdateActive){
            return;
        }

        fusedLocationClient.removeLocationUpdates(locationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        isLocationUpdateActive = false;
                        startLocationUpdateButton.setEnabled(true);
                        stopLocationUpdateButton.setEnabled(false);
                    }
                });

        isLocationUpdateActive = false;
        startLocationUpdateButton.setEnabled(true);
        stopLocationUpdateButton.setEnabled(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdate();
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    private void buildLocationCallback() {

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                currentLocation = locationResult.getLastLocation();

                updateLocationUi();
            }
        };

    }

    private void updateLocationUi() {

        if(currentLocation != null){
            locationTextView.setText("" + currentLocation.getLatitude()
                    + " / " + currentLocation.getLongitude());

            locationUpdateTimeTextView.setText(DateFormat.getTimeInstance().format(new Date()));
        }

    }

    private void buildLocationRequest() {

        locationRequest = new LocationRequest
                .Builder(10000)
                .setMinUpdateIntervalMillis(3000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isLocationUpdateActive && checkLocationPermission()) {

            startLocationUpdate();
        } else if (!checkLocationPermission()) {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {

        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        );

        if (shouldProvideRationale) {
            showSnackBar("Location permission is needed for app functionality",
                    "Ok",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                    },REQUEST_LOCATION_PERMISSION
                            );
                        }
                    });
        } else{
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },REQUEST_LOCATION_PERMISSION
            );
        }


    }

    private void showSnackBar(final String mainText,
                              final String action,
                              View.OnClickListener listener) {

        Snackbar.make(findViewById(android.R.id.content),
                        mainText,
                        Snackbar.LENGTH_INDEFINITE)
                .setAction(
                        action,
                        listener)
                .show();
    }

    private boolean checkLocationPermission() {

        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length <= 0) {
                Log.d("onRequestPermissionsResult", "Request was cancelled");

            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLocationUpdateActive) {
                    startLocationUpdate();
                }
            } else {
                showSnackBar("Turn on location on settings",
                        "Settings",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS);
                                Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID,
                                        null
                                );
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }
}