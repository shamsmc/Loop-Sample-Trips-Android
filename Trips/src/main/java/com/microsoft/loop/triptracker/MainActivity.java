package com.microsoft.loop.triptracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.microsoft.loop.triptracker.utils.LoopUtils;
import com.microsoft.loop.triptracker.utils.AppUtils;

import java.util.ArrayList;
import java.util.List;

import ms.loop.loopsdk.core.ILoopServiceCallback;
import ms.loop.loopsdk.core.LoopSDK;
import ms.loop.loopsdk.profile.IProfileDownloadCallback;
import ms.loop.loopsdk.profile.IProfileItemChangedCallback;
import ms.loop.loopsdk.profile.KnownLocation;
import ms.loop.loopsdk.profile.Label;
import ms.loop.loopsdk.profile.Labels;
import ms.loop.loopsdk.profile.Trip;
import ms.loop.loopsdk.util.LoopError;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    //drives
    private BroadcastReceiver mReceiver;
    private TripsViewAdapter adapter;
    private ListView tripListView;
    private Switch locationSwitch;
    private TextView locationText;

    private TextView termsTextView;
    private TextView privacyTextView;
    private TextView txtDescription;


    private static RelativeLayout enableLocation;

    private NavigationView navigationView;

    private static String Loop_URL = "https://www.loop.ms/";
    private static String TOU_URL = "http://go.microsoft.com/fwlink/?LinkID=530144";
    private static String PRIVACY_URL = "http://go.microsoft.com/fwlink/?LinkId=521839";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(null);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        navigationView.setItemIconTintList(null);
        navigationView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();
                item.setChecked(true);
                switch (id) {
                    case R.id.nav_trips: {
                        item.setIcon(getResources().getDrawable(R.drawable.ic_trips_on));
                        updateTripsInUI();
                        break;
                    }
                    case R.id.nav_version: {
                        item.setChecked(false);
                        startActivity(new Intent(MainActivity.this, AboutActivity.class));
                        return false;
                    }
                    case R.id.helpusimprove: {
                        item.setChecked(false);
                        deleteMyData();
                        return false;
                    }
                }

                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                }
                return true;
            }
        });

        Menu m = navigationView.getMenu();
        for (int i = 0; i < m.size(); i++) {
            MenuItem mi = m.getItem(i);
            AppUtils.applyFontToMenuItem(this, mi, "Roboto-Medium");
        }

        navigationView.getMenu().getItem(0).setChecked(true);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.onInitialized");
        mReceiver = new BroadcastReceiver() {
            @Override
                public void onReceive(Context context, Intent intent) {
                registerForTripUpdates();
                loadDrivesAndTrips();
            }
        };
        initLoopProfileItems();
        //registering our receiver
        this.registerReceiver(mReceiver, intentFilter);

        locationSwitch = (Switch) this.findViewById(R.id.locationswitch);
        locationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                boolean isLocationOn = SampleAppApplication.isLocationTurnedOn(MainActivity.this);
                if (isChecked) {
                    if (!isLocationOn) {
                        SampleAppApplication.instance.openLocationServiceSettingPage(MainActivity.this);
                    }
                    LoopUtils.startLocationProvider();
                } else {
                    LoopUtils.stopLocationProvider();
                }
                SampleAppApplication.setSharedPrefValue(getApplicationContext(), "AppTracking", isChecked);
                checkTrackingEnabled();
            }
        });
        locationText = (TextView) this.findViewById(R.id.txtlocationtracking);
        txtDescription = (TextView) this.findViewById(R.id.title_description);
        enableLocation = (RelativeLayout) this.findViewById(R.id.locationstrackingcontainer);

        termsTextView = (TextView) navigationView.findViewById(R.id.terms);

        termsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrlInBrowser(TOU_URL);
            }
        });

        privacyTextView = (TextView) navigationView.findViewById(R.id.privacy);

        privacyTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrlInBrowser(PRIVACY_URL);
            }
        });

        registerForTripUpdates();
    }

    public void initLoopProfileItems() {
        List<Trip> trips = new ArrayList<Trip>(LoopUtils.getTrips());
        adapter = new TripsViewAdapter(this,
                R.layout.tripview, trips);

        tripListView = (ListView) findViewById(R.id.tripslist);
        tripListView.setAdapter(adapter);
    }

    public void registerForTripUpdates() {
        LoopUtils.registerItemChangedCallback(new IProfileItemChangedCallback() {
            @Override
            public void onItemChanged(String entityId) {
                updateTripsInUI();
            }

            @Override
            public void onItemAdded(String entityId) {}

            @Override
            public void onItemRemoved(String entityId) {}
        });
    }

    public void deleteMyData() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete all trips?")
                .setCancelable(true)
                .setMessage("This will delete all your trips permanently.")
                .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        LoopSDK.deleteUser(new ILoopServiceCallback<Void>() {
                            @Override
                            public void onSuccess(Void value) {
                                LoopUtils.deleteItems();
                                SampleAppApplication.instance.unInitializeLoopSDK();
                                SampleAppApplication.instance.initializeLoopSDK();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                                        drawer.closeDrawer(GravityCompat.START);
                                    }
                                });
                            }

                            @Override
                            public void onError(LoopError error) {
                            }
                        });
                    }
                });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {}
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void loadDrivesAndTrips() {
        checkTrackingEnabled();
        if (LoopUtils.getTrips().size() > 0 || !LoopSDK.isInitialized() || TextUtils.isEmpty(LoopSDK.userId)) {
            updateTripsInUI();
            return;
        }

        LoopUtils.downloadTrips(new IProfileDownloadCallback() {
            @Override
            public void onProfileDownloadComplete(int itemCount) {
                updateTripsInUI();
            }
            @Override
            public void onProfileDownloadFailed(LoopError error) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDrivesAndTrips();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mReceiver);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_trips) {
        } else if (id == R.id.nav_version || id == R.id.helpusimprove) {
            return false;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void checkTrackingEnabled() {

        boolean tracking = SampleAppApplication.getBooleanSharedPrefValue(this.getApplicationContext(), "AppTracking", true);
        locationText.setTextColor(tracking ? Color.BLACK : getResources().getColor(R.color.trackingoffcolor));
        locationSwitch.setChecked(tracking);
        txtDescription.setVisibility(View.VISIBLE);
    }

    public void updateTripsInUI() {
       LoopUtils.loadItems();
        final TextView titleTextView = (TextView) findViewById(R.id.toolbar_title);
        String title = "";
        List<Trip> drives = new ArrayList<>();
        Menu m = navigationView.getMenu();
        for (int i = 0; i < m.size(); i++) {
            MenuItem mi = m.getItem(i);
          if (mi.isChecked() && (mi.getItemId() == R.id.nav_trips || mi.getItemId() == R.id.nav_version)) {
                drives = new ArrayList<Trip>(LoopUtils.getTrips());
                break;
            }
        }

        final List<Trip> finalDrives = drives;
        runOnUiThread(new Runnable() {
            public void run() {
                adapter.update(finalDrives);
                checkTrackingEnabled();
            }
        });
    }

    public void openUrlInBrowser(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    public static boolean isKnownLocation(String entityId, String knownLocationType) {
        KnownLocation knownLocation = LoopUtils.getLocation(entityId);
        if (knownLocation == null || !knownLocation.isValid()) return false;
        Labels labels = knownLocation.labels;

        for (Label label : labels) {
            if (label.name.equalsIgnoreCase(knownLocationType))
                return true;
        }
        return false;
    }
}
