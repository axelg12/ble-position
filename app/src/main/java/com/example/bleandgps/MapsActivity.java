package com.example.bleandgps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.bleandgps.PermissionUtils.PermissionUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleScanStatusListener;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.mapspeople.mapcontrol.MapControl;
import com.mapspeople.models.Point;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private MapControl myMapControl;
    private Location location;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = MapsActivity.class.getSimpleName();
    private GoogleApiClient googleAPI;
    private ProximityManager proximityManager;
    SupportMapFragment mapFragment;
    private ArrayList<IBeaconDevice> deviceArrayList = new ArrayList<IBeaconDevice>();
    HashMap<String, JSONObject> m_li = new HashMap<String, JSONObject>();
    Marker m;
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
        proximityManager.setScanStatusListener(new SimpleScanStatusListener() {
            @Override
            public void onScanStart() {
                Log.d("BEACON", "onScanStart: ");
            }

            @Override
            public void onScanStop() {
                Log.d("BEACON", "onScaneEND: ");
            }
        });
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
        // Indoor maps
        myMapControl = new MapControl(this, mapFragment);
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
        proximityManager.stopScanning();
        googleAPI.disconnect();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
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

    private void findRoomInfo(IBeaconDevice ibeacon) {
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

    private void calculateCentroidLocation() {
        double lat = 0;
        double lng = 0;
        int count = 0;
        int level = 0;
        // RSSi goes from 0 (best) to -120
        int bestRSSI = -130;
        for (IBeaconDevice element : deviceArrayList) {
            String UUID = element.getUniqueId();
            JSONObject temp = m_li.get(UUID);
            JSONArray coords;
            try {
                if (temp != null && temp.has("coord")) {
                    double latTemp = 0;
                    double lngTemp = 0;
                    // Let the best RSSi signal determine the floor
                    if (element.getRssi() > bestRSSI) {
                        level = temp.getInt("level");
                    }
                    count += 1;
                    coords = temp.getJSONArray("coord");
                    coords = coords.getJSONArray(0);
                    for (int i = 0; i < coords.length(); i++) {
                        latTemp += coords.getJSONArray(i).getDouble(0);
                        lngTemp += coords.getJSONArray(i).getDouble(1);
                    }
                    latTemp = latTemp / coords.length();
                    lngTemp = lngTemp / coords.length();
                    lat += latTemp;
                    lng += lngTemp;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (lat != 0 && lng != 0) {
             lat = lat / count;
            lng = lng / count;
            updateCam(lat, lng, level);
        }

    }


    private IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            /*
            Code from the snapping test
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                if (bestDevice == null || ibeacon.getRssi() >= bestDevice.getRssi()) {
                    bestDevice = ibeacon;
                    findRoomInfo(ibeacon);
                }
            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {
                for (IBeaconDevice element : iBeacons) {
                    if (element.getRssi() <= bestDevice.getRssi()) {
                        bestDevice = element;
                        findRoomInfo(bestDevice);
                    }
                }
            }

            @Override
            public void onIBeaconLost(IBeaconDevice iBeacon, IBeaconRegion region) {
                if (iBeacon.getUniqueId() == bestDevice.getUniqueId()) {
                    bestDevice = null;
                }
            }
            */
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                deviceArrayList.add(ibeacon);
                calculateCentroidLocation();
            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {
                deviceArrayList.removeAll(iBeacons);
                deviceArrayList.addAll(iBeacons);
                calculateCentroidLocation();
            }

            @Override
            public void onIBeaconLost(IBeaconDevice iBeacon, IBeaconRegion region) {
                deviceArrayList.remove(iBeacon);
                calculateCentroidLocation();

            }
        };
    }


    private void updateCam(double lat, double lng, String room, int level) {
        LatLng latLng = new LatLng(lng, lat);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 20);
        Toast.makeText(this, "You are in room " + room, Toast.LENGTH_SHORT).show();
        Point p = new Point(lat, lng);
        myMapControl.setCurrentPosition(p, level);
        if (m == null) {
            m = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("User"));
        } else {
            m.setPosition(latLng);
        }
        mMap.moveCamera(cameraUpdate);
    }

    private void updateCam(double lat, double lng, int level) {
        LatLng latLng = new LatLng(lng, lat);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 18);
        Point p = new Point(lat, lng);
        myMapControl.setCurrentPosition(p, level);
        if (m == null) {
            m = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("User"));
        } else {
            m.setPosition(latLng);
        }
        mMap.moveCamera(cameraUpdate);
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

