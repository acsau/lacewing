package com.csa.ttn.firefly1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

public class MapsActivityLocationSettings extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MapsActivityLocationSettings.class.getSimpleName();

    private GoogleApiClient googleApiClient;
    private boolean mIntentInProgress = false;
    private final int LOCATION_SERVICES = 9;
    private LatLng userSel = null;

    Toast currentToast = null;
    public static String csvFileName = "LacewingReports.csv";

    public GoogleMap mMap;
    public SupportMapFragment mapFragment;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // A default location (Imperial College London) and default zoom to use when
    // location permission is not granted.
    private final LatLng mDefaultLocation = new LatLng(51.498308, -0.176882);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;
    private Location mInitLocation = new Location("temp");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showToastLong("\"Back\" button returns to the Lacewing map");
        // Initialize Location variable to non-null, set at Imperial College
        mInitLocation.setLatitude(51.498308);
        mInitLocation.setLongitude(-0.176882);

        mLastKnownLocation = mInitLocation;

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(MapsActivityLocationSettings.this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).build();
            googleApiClient.connect();

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(30 * 1000);
            locationRequest.setFastestInterval(5 * 1000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            //**************************
            builder.setAlwaysShow(true); //this is the key ingredient
            //**************************

            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    final LocationSettingsStates state = result.getLocationSettingsStates();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            // All location settings are satisfied. The client can initialize location
                            // requests here.
                            trackBegin();
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the user
                            // a dialog.
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(
                                        MapsActivityLocationSettings.this, LOCATION_SERVICES);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            MapsActivityLocationSettings.this.finish();
                            break;
                    }
                }
            });
        }
    }

    public void trackBegin(){

        // Retrieve the content view that renders the map.
        //setContentView(R.layout.fragment_maps);
        setContentView(R.layout.fragment_maps);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.content_map);
        mapFragment.getMapAsync(this);
    }

    public void onStart() {
        super.onStart();
        googleApiClient.connect();
    }


    public void onStop() {
        super.onStop();

        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Handler handler = new Handler();

        switch (requestCode) {
            case LOCATION_SERVICES:
                mIntentInProgress = false;
                if (!googleApiClient.isConnecting()) {
                    googleApiClient.connect();
                }
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        showToastLong("Initializing Location Services\n Please wait 5 seconds");
                        // Wait after enabling location services for GPS/AGPS TTFF
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // Close MapsActivityLocationSettings after 5s = 5000ms
                                finish();
                            }
                        }, 5000);

                        break;
                    case Activity.RESULT_CANCELED:
                        finish();
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // TODO Auto-generated method stub
    }

    public void onConnectionSuspended(int cause) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // TODO Auto-generated method stub

        if (!mIntentInProgress && result.hasResolution()) {
            try {
                mIntentInProgress = true;
                result.startResolutionForResult(MapsActivityLocationSettings.this, // your activity
                        LOCATION_SERVICES);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent. Return to default
                // state and attempt to connect to get an updated ConnectionResult.
                mIntentInProgress = false;
                googleApiClient.connect();
            }
        }
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();


        // Shows the address of a selected location when screen long-pressed
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            Marker mkr;
            @Override
            public void onMapLongClick(LatLng pt) {
                userSel = pt;

                //remove previously placed Marker
                if (mkr != null) {
                    mkr.remove();
                }

                //place marker where user just clicked
                mkr = mMap.addMarker(new MarkerOptions().position(pt).title("Selected Location"));

            }
        });

        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {

            public boolean onMyLocationButtonClick (){
                LacewingReportDialog();
                return true;
            }
        });
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private synchronized void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available. Such cases occur when the app is the
         * FIRST client requesting a location, thus there is effectively no 'last location'.
         * This will happen if location services is just turned on and no other apps
         * requested a connection.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    /**
     * Prompts the user for permission to use the device location.
     */
    private synchronized void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission;
     * Enables the My Location button if location permissions are granted
     */
    private synchronized void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {

            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mMap.getUiSettings().setCompassEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    public void LacewingReportDialog (){

        /* Create a dialog to prompt for location name and disease
         * using two editText in one dialog
         */
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final EditText placeNameGet = new EditText(this);
        final EditText diseaseTypeGet = new EditText(this);
        final TextView locationSelect = new TextView(this);

        // set up linear layout that contains an edit text and has padding at the sides
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        placeNameGet.setHint("Enter Location Name");
        placeNameGet.setSingleLine();
        linearLayout.addView(placeNameGet);
        diseaseTypeGet.setHint("Enter Disease Detected");
        diseaseTypeGet.setSingleLine();
        linearLayout.addView(diseaseTypeGet);
        locationSelect.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        locationSelect.setText("The report will be compiled and sent after selecting the location tagging method");
        locationSelect.setPadding(10,50,10,0);
        linearLayout.addView(locationSelect);
        linearLayout.setPadding(50, 30, 50, 30);
        dialogBuilder.setView(linearLayout);
        dialogBuilder.setTitle("New Lacewing Report");

        dialogBuilder.setPositiveButton("Use GPS-detected tag", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Double LatGPS = mLastKnownLocation.getLatitude();
                Double LngGPS = mLastKnownLocation.getLongitude();
                buildReport(placeNameGet, diseaseTypeGet, LatGPS, LngGPS);
            }
        });
        dialogBuilder.setNegativeButton("Manually place tag", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (userSel==null){
                    showToastLong("Retry after long-holding a location to select it");
                }
                else{
                    Double LatUser = userSel.latitude;
                    Double LngUser = userSel.longitude;
                    buildReport(placeNameGet, diseaseTypeGet, LatUser, LngUser);
                }
            }
        });
        dialogBuilder.show();
    }

    public void buildReport (EditText place, EditText disease, double Lat, double Lng){
        String placeName = place.getText().toString();
        String diseaseType = disease.getText().toString();
        String LatS = String.valueOf(Lat);
        String LngS = String.valueOf(Lng);

                /* Get the current date and time in GMT
                 * Calendar outputs in integers, convert to string
                 * Maintain double digit format by appending zero when needed, e.g. 01/03/2017 at 05:07:08
                 */
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        // Add 1 to returned value of MONTH since Jan = 0 and Dec = 11
        int monthOfYear = c.get(Calendar.MONTH) + 1;

        String day = c.get(Calendar.DAY_OF_MONTH) < 10 ? "0" + c.get(Calendar.DAY_OF_MONTH) : c.get(Calendar.DAY_OF_MONTH) + "";
        String month = monthOfYear < 10 ? "0" + monthOfYear: monthOfYear + "";
        String hour = c.get(Calendar.HOUR_OF_DAY) < 10 ? "0" + c.get(Calendar.HOUR_OF_DAY) : c.get(Calendar.HOUR_OF_DAY) + "";
        String minute = c.get(Calendar.MINUTE) < 10 ? "0" + c.get(Calendar.MINUTE) : c.get(Calendar.MINUTE) + "";
        String second = c.get(Calendar.SECOND) < 10 ? "0" + c.get(Calendar.SECOND) : c.get(Calendar.SECOND) + "";

        String details = "On " + day + "/" + month + "/" + c.get(Calendar.YEAR) +
                " at " + hour + ":" + minute + ":" + second + " (GMT)";

        // Add the entry to MapMarkerLocation.csv in the Firefly folder
        try {

            File file = new File(Environment.getExternalStorageDirectory(), "/" +
                    StartActivity.PARENT_DIRECTORY + "/" + csvFileName);

            String header = "Code,Latitude,Longitude,Place,Disease,Details";
            if (!file.exists()) {
                file.createNewFile();
                // Second argument to FileWriter must be set 'true' to enable file appending
                FileWriter fwInit = new FileWriter(file.getAbsoluteFile(), true);
                // Define an output buffer
                BufferedWriter bwInit = new BufferedWriter(fwInit);
                // Create the headers for each column
                bwInit.write(header);
                bwInit.newLine();
                // .close automatically flushes buffer before closing
                bwInit.close();
            }

            FileWriter fwData = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bwData = new BufferedWriter(fwData);

            // Start a new line first, to make sure data is stored from column A
            bwData.newLine();
            // Store the string into the buffer
            bwData.write(StartActivity.userName + "_" + DgnsFragment.trialCode + "," +
                    LatS + "," + LngS + ","+ placeName+ "," + diseaseType + "," + details);
            bwData.close();

            MapsFragment.addMapMarker(Lat, Lng, placeName, diseaseType, details);

            showToastLong("Report compiled and sent");

            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Function to immediately update the contents of a toast, bypassing the SHORT and LONG intervals
     * @param text
     */
    public void showToastLong (String text){
        if(currentToast == null)
        {
            currentToast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
        }
        currentToast.setText(text);
        currentToast.setDuration(Toast.LENGTH_LONG);
        currentToast.show();
    }
}
