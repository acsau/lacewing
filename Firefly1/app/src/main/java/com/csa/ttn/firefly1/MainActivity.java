package com.csa.ttn.firefly1;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    final FragmentManager fragmentManager = getSupportFragmentManager();

    // define fragments
    final Fragment fragmentInfo = new InfoFragment();
    public Fragment fragmentDgns = new DgnsFragment();
    public Fragment fragmentMaps = new MapsFragment();

    private static boolean dgns = false;
    private static boolean dgnsSwap = false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.actions, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);

        FragmentTransaction fragmentTransaction1 = fragmentManager.beginTransaction();
        fragmentTransaction1.replace(R.id.content, fragmentInfo).commit();

        // handle navigation selection
        navigation.setOnNavigationItemSelectedListener(
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.navigation_info:
                            /*if (dgns){
                                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                FragmentTransaction fragmentTransaction1 = fragmentManager.beginTransaction();
                                                fragmentTransaction1.replace(R.id.content, fragmentInfo).commit();
                                                dgns = false;
                                                break;

                                            case DialogInterface.BUTTON_NEGATIVE:
                                                dgns = true;
                                                break;
                                        }
                                    }
                                };

                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setMessage("Leaving Diagnostics Window")
                                        .setPositiveButton("Leave", dialogClickListener).setNegativeButton("Stay", dialogClickListener).show();

                            }
                            return !dgns;*/

                        FragmentTransaction fragmentTransaction1 = fragmentManager.beginTransaction();
                        fragmentTransaction1.replace(R.id.content, fragmentInfo).commit();
                        return true;

                        case R.id.navigation_dgns:
                            dgns = true;
                            FragmentTransaction fragmentTransaction2 = fragmentManager.beginTransaction();
                            fragmentTransaction2.replace(R.id.content, fragmentDgns).commit();
                            return true;
                        case R.id.navigation_maps:
                            /*if (dgns){
                            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case DialogInterface.BUTTON_POSITIVE:
                                            FragmentTransaction fragmentTransaction3 = fragmentManager.beginTransaction();
                                            fragmentTransaction3.replace(R.id.content, fragmentMaps).commit();
                                            dgns = false;
                                            break;

                                        case DialogInterface.BUTTON_NEGATIVE:
                                            dgns = true;
                                            break;
                                    }

                                }
                            };

                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setMessage("Leaving Diagnostics Window")
                                    .setPositiveButton("Leave", dialogClickListener).setNegativeButton("Stay", dialogClickListener).show();

                            }

                            return !dgns;*/

                        FragmentTransaction fragmentTransaction3 = fragmentManager.beginTransaction();
                        fragmentTransaction3.replace(R.id.content, fragmentMaps).commit();
                        return  true;
                    }
                    return false;
                }
            }
        );
    }
}
