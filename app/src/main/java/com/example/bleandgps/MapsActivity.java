package com.example.bleandgps;

import android.*;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.widget.Toast;

import com.example.bleandgps.PermissionUtils.PermissionUtils;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import android.location.LocationListener;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.EddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleEddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.kontakt.sdk.android.common.profile.IEddystoneNamespace;
import com.mapspeople.mapcontrol.MapControl;
import com.mapspeople.models.Point;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private MapControl myMapControl;
    private Location location;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = MapsActivity.class.getSimpleName();
    private GoogleApiClient googleAPI;
    private ProximityManager proximityManager;
    SupportMapFragment mapFragment;
    private float bestPos = 0;
    HashMap<String, JSONObject> m_li = new HashMap<String, JSONObject>();
    // ArrayList<HashMap<String, JSONObject>> formList = new ArrayList<HashMap<String, JSONObject>>();
    /**
     * Flag indicating whether a requested permission has been denied after returning in
     */
    private boolean mPermissionDenied = false;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        initGoogleAPI();
        KontaktSDK.initialize("qNTrzuKEryBCJMYyuBqGNLAbsKNXArAm");

        proximityManager = ProximityManagerFactory.create(this);
        proximityManager.setIBeaconListener(createIBeaconListener());

        fetchJSONData d = new fetchJSONData();
        d.start();
    }

    private void startScanning() {
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                proximityManager.startScanning();
            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMyLocationButtonClickListener(this);
        enableMyLocation();
        myMapControl = new MapControl(this, mapFragment);
        // myMapControl.setOnDataReadyListener(this);
        myMapControl.initMap("55cdde212a91e0049824fe86", "sdu");
    }


    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        // Permission to access the location is missing.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("TAG", "enableMyLocation: AA");
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            Log.d("TAG", "enableMyLocation: BBB");
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Maps Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-ERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    private void initGoogleAPI() {
        Log.d(TAG, "initGoogleAPI: " + googleAPI);
        if (googleAPI != null) return;
        googleAPI = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        googleAPI.connect();
        startScanning();

    }

    @Override
    public void onStop() {
        super.onStop();
        googleAPI.disconnect();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "heresss: ");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        location = LocationServices.FusedLocationApi.getLastLocation(googleAPI);
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        Point p = new Point(location.getLatitude(), location.getLongitude());
        myMapControl.setMapPosition(p, false);
        mMap.moveCamera(cameraUpdate);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                if (bestPos == 0 || ibeacon.getRssi() >= bestPos) {
                    bestPos = ibeacon.getRssi();
                    // TODO
                    String UUID = ibeacon.getUniqueId();
                    JSONObject temp = m_li.get(UUID);
                    JSONArray coords = null;
                    try {
                        if (temp != null && temp.has("coord")) {
                            coords = temp.getJSONArray("coord");
                            coords = coords.getJSONArray(0);
                            double lat = 0;
                            double lng = 0;
                            for (int i = 0; i < coords.length(); i++) {
                                lat += coords.getJSONArray(i).getDouble(0);
                                lng += coords.getJSONArray(i).getDouble(1);
                            }
                            lat = lat / coords.length();
                            lng = lng / coords.length();
                            updateCam(lat, lng, temp.getString("room"), temp.getInt("level"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Log.i("IBeacon", "IBeacon discovered with dist: " + ibeacon.getDistance());
                Log.i("IBeacon", "IBeacon prox: " + ibeacon.getProximity());
                Log.i("IBeacon", "IBeacon prox: " + ibeacon.getProfile());
                Log.i("IBeacon", "IBeacon discovered: " + ibeacon.toString());
            }
        };
    }


    private void updateCam(double lat, double lng, String room, int level) {
        LatLng latLng = new LatLng(lng, lat);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 20);
        Toast.makeText(this, "You are in room " + room, Toast.LENGTH_SHORT).show();
        Point p = new Point(lat, lng);
        myMapControl.setCurrentPosition(p, level);
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("User"));
        mMap.moveCamera(cameraUpdate);
    }

    private EddystoneListener createEddystoneListener() {
        return new SimpleEddystoneListener() {
            @Override
            public void onEddystoneDiscovered(IEddystoneDevice eddystone, IEddystoneNamespace namespace) {
                Log.i("Sample", "Eddystone discovered: " + eddystone.toString());
            }
        };
    }

    public String loadJSONFromAsset(String fileName) {
        String json = null;
        try {
            InputStream is = getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public class fetchJSONData extends Thread {
        public void run(){
            try {
                JSONObject ou44 = new JSONObject(loadJSONFromAsset("ou44_geometry.geojson"));
                JSONObject beacons = new JSONObject(loadJSONFromAsset("beacons.json"));
                JSONArray m_jArry = beacons.getJSONArray("beacons");
                for (int i = 0; i < m_jArry.length(); i++) {
                    JSONObject jo_inside = m_jArry.getJSONObject(i);
                    String alias = jo_inside.getString("alias");
                    String room = jo_inside.getString("room");
                    char level = jo_inside.getString("level").charAt(0);
                    JSONArray ou44Arr = ou44.getJSONArray("features");
                    for (int j = 0; j < ou44Arr.length(); j++) {
                        JSONObject foo = ou44Arr.getJSONObject(j).getJSONObject("properties");
                        if (foo.has("RoomId")) {
                            String roomId = foo.getString("RoomId");
                            if (roomId.equalsIgnoreCase(room)) {
                                JSONArray coord = ou44Arr.getJSONObject(j).getJSONObject("geometry").getJSONArray("coordinates");
                                JSONObject item = new JSONObject();
                                item.put("coord", coord);
                                item.put("level", level);
                                item.put("alias", alias);
                                item.put("room", room);
                                m_li.put(alias, item);
                                // formList.add(m_li);
                                Log.d(TAG, "onCreate: Added");
                            }
                        }
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}

