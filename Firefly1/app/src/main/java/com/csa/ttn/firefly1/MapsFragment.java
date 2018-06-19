package com.csa.ttn.firefly1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MapsFragment extends SupportMapFragment implements OnMapReadyCallback{

    private static final String TAG = "MapsFragment";
    private final int NEW_REPORT = 11;
    public static GoogleMap mMap;
    public static List<LatLng> LLList = new ArrayList<>();
    // Declare a variable for the cluster manager.
    public static ClusterManager<MyItem> mClusterManager;

    private static boolean mapReady = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment = new SupportMapFragment();
        getFragmentManager().beginTransaction().replace(R.id.content_map, mapFragment).commit();
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapReady){
            mClusterManager.cluster();
            showAllMarkers();
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mapReady = true;
        mMap = map;
        setUpClusterer();

        // Clear the Map Market arraylist to be repopulated
        mClusterManager.clearItems();
        // Clear the map
        mMap.clear();
        // Reload all markers from the csv, now including the new location
        loadMapMarkers();
    }

    public static void loadMapMarkers(){
        // Load map markers stored in csv file
        try {
            File file = new File(Environment.getExternalStorageDirectory(),
                    StartActivity.PARENT_DIRECTORY + "/" + MapsActivityLocationSettings.csvFileName);
            if (!file.exists()){
                return;
            }
            FileReader fs = new FileReader(file);
            BufferedReader br = new BufferedReader(fs);
            // Skip the header
            br.readLine();
            // Start here
            String line;
            while ((line = br.readLine()) != null) {
                /* Split the row string by recognizing "," from csv formats
                 * This separates the cells into String[] arrays
                 * Ensure both Lat and Lng are valid before plotting
                 * Convert string to double for addMapMarker method
                 */
                if ((!line.split(",")[0].equals(""))) {
                    double LatRead = Double.parseDouble(line.split(",")[1]);
                    double LngRead = Double.parseDouble(line.split(",")[2]);
                    addMapMarker(LatRead, LngRead, line.split(",")[3], line.split(",")[4], line.split(",")[5]);
                }
            }
            br.close();
            mClusterManager.cluster();
            showAllMarkers();
        }catch(IOException e) {
            e.printStackTrace();
        }
    }


    public static void addMapMarker(double lat, double lng, String placeName, String disease, String details){

        MyItem setItem = new MyItem(lat, lng, placeName, disease + "\n"+ details);
        mClusterManager.addItem(setItem);

        // Record the position of this marker for camera zoom scaling
        LLList.add(new LatLng(lat, lng));

    }

    public static void showAllMarkers(){
        if (LLList.size()>1){
            // Reorient the camera to display all markers on the map at once
            // With 130px padding from the screen edges
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng ll : LLList) {
                builder.include(ll);
            }
            LatLngBounds bounds = builder.build();
            int padding = 150; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mMap.animateCamera(cu);
        }
    }

    private void setUpClusterer() {

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<MyItem>(getActivity(), mMap);

        final CustomClusterRenderer renderer = new CustomClusterRenderer(getActivity(), mMap, mClusterManager);

        mClusterManager.setRenderer(renderer);

        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mClusterManager.getMarkerCollection().setOnInfoWindowAdapter(new CustomInfoViewAdapter(LayoutInflater.from(getActivity())));

        mClusterManager.setOnClusterClickListener(
                new ClusterManager.OnClusterClickListener<MyItem>() {
                    @Override
                    public boolean onClusterClick(Cluster<MyItem> cluster) {

                        // Zoom in the cluster. Need to create LatLngBounds and including all the cluster items
                        // inside of bounds, then animate to center of the bounds.

                        // Create the builder to collect all essential cluster items for the bounds.
                        LatLngBounds.Builder builder = LatLngBounds.builder();
                        for (ClusterItem item : cluster.getItems()) {
                            builder.include(item.getPosition());
                        }
                        // Get the LatLngBounds
                        final LatLngBounds bounds = builder.build();

                        // Animate camera to the bounds
                        try {
                            int padding = 130; // offset from edges of the map in pixels
                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                            mMap.animateCamera(cu);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        return true;
                    }
                });

        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setInfoWindowAdapter(mClusterManager.getMarkerManager());
        mMap.setOnMarkerClickListener(mClusterManager);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.lacewing_report).setVisible(true);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.lacewing_report) {
            // Call MapsActivityLocationSettings and return whether a new marker was placed
            Intent intent = new Intent(getContext(), MapsActivityLocationSettings.class);
            startActivityForResult(intent, NEW_REPORT);
            return true;
        }
        else return false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case NEW_REPORT:
                // REPORT SUCCESSFULLY SAVED, can use for successfully sent to server
                if (resultCode == Activity.RESULT_OK) {
                    //DO SOMETHING HERE
                }
                // SAVE REPORT LOCALLY IN ANOTHER FILE TO SEND TO SERVER LATER
                break;
        }
    }
}