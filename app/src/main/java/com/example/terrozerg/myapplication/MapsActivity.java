package com.example.terrozerg.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.SphericalUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class MapsActivity extends FragmentActivity
        implements GoogleMap.OnMyLocationClickListener,
        GoogleMap.OnMyLocationButtonClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        OnMapReadyCallback, GoogleMap.OnCameraMoveStartedListener {
    public static final String Tag = "DEBUG_LOGS";

    private static final int LOCATION_REQUEST_CHECK_SETTINGS = 580;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int POLYLINES_ACTIVITY_REQUEST_CODE = 55;

    //Callback
    private final boolean requestingLocationUpdates = true;
    private LocationCallback locationCallback;
    //request
    private LocationRequest locationRequest;

    //polyline
    private Polyline polyline;
    private List<LatLng> polylinePoints;
    private PolylineOptions polylineOptions;
    //temp save polyline
    private List<LatLng> savedPolyPoints;

    //loaded polyline
    private Polyline loadedPolyline;
    private List<LatLng> loadedPolylinePoints;
    private PolylineOptions loadedPolylineOptions;

    private View polyLoad;
    private View polySave;
    private View clearBtn;

    //time
    private long startTime;
    private long currTime;

    //camera movement
    private int cameraUserInputCounter;

    //distance and time on map screen
    private float dist;
    private int time;
    //distance measure
    private String measure;
    private int multi;

    private LatLng savedLtg;
    public float scale;
    private int padding;

    private GoogleMap mMap;

    private TextView speedText;

    private FusedLocationProviderClient fusedLocationClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        speedText = (TextView) findViewById(R.id.speedText);
        //test 3 lines
        speedText.setLines(3);

        //onscreen distance measure default
        measure = "m";
        multi=1;

        clearBtn = (Button) findViewById(R.id.clearBtn);
        polyLoad = (Button) findViewById(R.id.loadBtn);
        polySave = (Button) findViewById(R.id.saveBtn);

        //dp to pxl scale
        scale = getResources().getDisplayMetrics().density;
        //map border padding
        padding = (int) (getResources().getDimension(R.dimen.map_borders_padding) * scale + 0.5f);

        //poly
        polylinePoints = new ArrayList<>();
        polyline = null;
        savedPolyPoints = new ArrayList<>();
        //poly styling
        polylineOptions = new PolylineOptions()
                .color(Color.RED)
                .width(getResources().getDimensionPixelSize(R.dimen.map_poly_width));
        //loaded route
        loadedPolylinePoints = new ArrayList<>();
        loadedPolyline = null;
        loadedPolylineOptions = new PolylineOptions()
                .color(Color.RED)
                .width(getResources().getDimensionPixelSize(R.dimen.map_poly_width));

        //time
        startTime = 0;
        currTime = 0;

        //camera counter and flag
        cameraUserInputCounter = 15;

        //TODO remake this btn to "get back from loaded route" button

        clearBtn.setVisibility(View.INVISIBLE);
        //move camera to last saved location, if it exists
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*if(polyline != null) {
                    Log.d("DEBUG_LOGS", "clearing polyline and points.");
                    //re-add polyline on the map
                    polylinePoints.clear();
                    polyline.remove();
                    polyline = mMap.addPolyline(polylineOptions.addAll(polylinePoints));
                    savedPolyPoints.clear();

                    //test delete distance
                    dist=0;
                    savedLtg=null;
                    //measures
                    measure = "m";
                    multi=1;

                    startTime = Calendar.getInstance().getTimeInMillis();
                }*/

                //test return to old polyline
                onBackPressed();
            }
        });

        //poly load
        polyLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MapsActivity.this, PolylinesActivity.class);
                startActivityForResult(intent, POLYLINES_ACTIVITY_REQUEST_CODE);
            }
        });

        //TODO use sql database to store data?
        //poly save
        polySave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //save points
                if(polyline != null) {
                    //CALCULATE DIFF
                    currTime = Calendar.getInstance(Locale.ENGLISH).getTimeInMillis();
                    long diff = currTime - startTime;

                    savedPolyPoints = polyline.getPoints();
                    //tempSave.setPoints(polyline.getPoints());

                    if(savedPolyPoints==null) {
                        Log.d(Tag, "cant save null points.");
                        return;
                    }

                    //create folder for routes
                    final File dir = new File(getApplicationContext().getFilesDir(),"routes");
                    if(!dir.isDirectory()){
                        if(!dir.mkdirs()) {
                            Log.d(Tag, "cant create directory.");
                        }
                    } else{
                        Log.d(Tag, "directory already exists.");
                    }


                    //String data = savedPolyPoints.toString();
                    StringBuilder data = new StringBuilder();

                    //fist line is diff
                    data.append(diff).append("\n");

                    for(int i=0;i<savedPolyPoints.size();i++){
                        data.append(savedPolyPoints.get(i).latitude).append("\n").append(savedPolyPoints.get(i).longitude).append("\n");
                    }

                    //folder to save image and route in
                    final File folder = new File(dir, String.valueOf(currTime));
                    if(!folder.mkdirs()) {
                        Log.d(Tag, "cant create directory.");
                    }

                    //SNAPSHOT
                    makeRequest(folder);

                    try (FileOutputStream fos = new FileOutputStream(new File(folder, "route"))) {
                        //try (FileOutputStream fos = getApplicationContext().openFileOutput("route"+currTime, Context.MODE_PRIVATE)) {
                        fos.write(data.toString().getBytes());
                    } catch (IOException e) {
                        Log.d(Tag, "fileoutputstream error." + e.getMessage());
                    }

                    Log.d(Tag,"saved poly points:"+savedPolyPoints);
                    Toast.makeText(MapsActivity.this, "route saved", Toast.LENGTH_SHORT).show();
                }
                else{
                    Log.d(Tag,"poly on SAVE click executed but poly points are null");
                }


            }
        });


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d(Tag,"location callback result null");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.d(Tag,"location callback on location result.");

                    LatLng ltg = new LatLng(location.getLatitude(),location.getLongitude());

                    //distance
                    if(savedLtg != null){
                        dist += SphericalUtil.computeDistanceBetween(savedLtg, ltg)/multi;

                        //change measure to km
                        if(measure.equals("m") && dist>999){
                            measure = "km";
                            multi=1000;
                            dist = dist/multi;
                        }
                    }
                    else {
                        dist = 0;
                    }
                    savedLtg = ltg;

                    //Getting time
                    if(startTime == 0) {
                        startTime = Calendar.getInstance().getTimeInMillis();
                    }

                    //calculate position change to filter unnecessary camera movement and etc
                    //double latChange = Math.abs(savedLtg.latitude-ltg.latitude);
                    //double lngChange = Math.abs(savedLtg.longitude-ltg.longitude);

                    //camera movement
                    //if(latChange>0.001 || lngChange>0.001 || latChange+lngChange>0.001){

                        //test camera movement user cancel
                        //resume updates every 15 iterations

                    if(cameraUserInputCounter == 15) {
                        Log.d(Tag, "changed camera pos.");
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(ltg));
                        //savedLtg = ltg;
                    }
                    //TODO kostil
                    //test
                    else if (cameraUserInputCounter>0){
                        Log.d(Tag, "camera movement stopped due to user input.");
                        cameraUserInputCounter++;
                    }
                    //}

                    Log.d(Tag,"ltg changed to:"+ltg);

                    //adding cords to poly array
                    polylinePoints.add(ltg);

                    //add polyline
                    if(polyline!=null){
                        polyline.setPoints(polylinePoints);
                    }
                    else{
                        //test
                        polyline = mMap.addPolyline(new PolylineOptions()
                                .color(Color.RED)
                                .width(getResources().getDimensionPixelSize(R.dimen.map_poly_width))
                                .addAll(polylinePoints));
                    }

                    //update speed
                    double speed = Math.round(location.getSpeed()*3.6);

                    //set main text
                    speedText.setText(String.format(Locale.ENGLISH,"%s k/h\n%.1f %s", speed, dist, measure));

                    //accuracy
                    //Log.d(Tag, "Accuracy: "+location.getAccuracy());
                }
            }
        };

    }

    interface internetCheckCallback {
        void onComplete(boolean b);
    }

    //class for handling internet connection check
    private static class PingRepository{
        private final Executor executor;
        private final Handler handler;

        public PingRepository(Executor executor, Handler handler){
            this.executor = executor;
            this.handler = handler;
        }

        //inet check
        private boolean check(){
            try {
                InetAddress address = InetAddress.getByName("google.com");
                return !address.equals("");

            } catch (Exception e) {
                return false;
            }
        }

        //main func
        public void request(final internetCheckCallback callback){
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    boolean result = check();
                    notifyResult(callback, result);
                }
            });
        }

        //notify main thread
        private void notifyResult(final internetCheckCallback callback, final boolean result){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onComplete(result);
                }
            });
        }

    }

    //wraper
    public void makeRequest(final File folder){
        Executor executor = Executors.newFixedThreadPool(4);
        Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

        PingRepository pingRepository = new PingRepository(executor, mainThreadHandler);

        pingRepository.request(new internetCheckCallback() {
            @Override
            public void onComplete(boolean b) {
                if(b==Boolean.TRUE){
                    saveSnapshot(folder);
                }
                else{
                    Log.d(Tag, "Internet connection error.");
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                            R.drawable.ic_broken_image_black_36dp);

                    try (FileOutputStream fos = new FileOutputStream(new File(folder, "map_image"))) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);
                    } catch (IOException e) {
                        Log.d(Tag, "fileoutputstream error." + e.getMessage());
                    }
                }
            }
        });
    }

    private LatLngBounds buildBounds(List<LatLng> route){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (int i = 0; i < route.size(); i++) {
            builder.include(route.get(i));
        }
        return builder.build();
    }

    //TODO save only poly points and make route image out of them?
    //SNAPSHOT
    private void saveSnapshot(final File folder){
        stopLocationUpdates();

        LatLngBounds bounds = buildBounds(savedPolyPoints);

        CameraUpdate cameraPosition;
        cameraPosition = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        //ugly premove to set zoom
        mMap.moveCamera(CameraUpdateFactory.zoomTo(18));

        //pipe
        //move camera
        mMap.animateCamera(cameraPosition, 100, new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {

                //wait for camera to move
                mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {

                        //wait for camera to take snapshot
                        mMap.snapshot(new GoogleMap.SnapshotReadyCallback() {
                            @Override
                            public void onSnapshotReady(Bitmap bitmap) {
                                if (bitmap != null) {
                                    Log.d(Tag, "on snap rdy shot: " + bitmap.toString());

                                    //save image to file
                                    try (FileOutputStream fos = new FileOutputStream(new File(folder, "map_image"))) {
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);
                                    } catch (IOException e) {
                                        Log.d(Tag, "fileoutputstream error." + e.getMessage());
                                    }
                                    Log.d(Tag, "snapshot taken and saved.");
                                } else {
                                    Log.d(Tag, "snapshot bitmap is null.");
                                }
                                startLocationUpdates();
                            }
                        });
                    }
                });
            }

            @Override
            public void onCancel() {

            }
        });
    }

    //TODO add onclick onto polylines

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        UiSettings settings = mMap.getUiSettings();
        settings.setZoomControlsEnabled(true);
        settings.setMapToolbarEnabled(true);
        settings.setZoomGesturesEnabled(true);

        //padding
        int mapPadding = (int) (getResources().getDimension(R.dimen.map_padding)*scale);
        //mMap.setPadding(0,mapPadding/2,0, mapPadding);

        mMap.setOnMyLocationClickListener(this);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnCameraMoveStartedListener(this);

        mMap.setMaxZoomPreference(18);
        mMap.setMinZoomPreference(10);

        //set last known position
        CameraPosition position = mMap.getCameraPosition();
        if(position.zoom !=16){
            LatLng ltg = loadLastLocation();
            if (ltg != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ltg,16));
            } else {
                Toast.makeText(MapsActivity.this, "saved location is null", Toast.LENGTH_SHORT).show();
            }
        }

        //TODO ask for permissions nicely
        //check for permissions and ask user to turn them on if needed
        createLocationRequest();

        //set permissions for location access
        switchMyLocation(true);

        //save lsat location to local variable
        getLastLocation();

    }

    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(8);
        locationRequest.setMaxWaitTime(4000*2*100);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Log.d("DEBUG_LOGS", "task on success executed.");
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
                startLocationUpdates();
                //requestingLocationUpdates = true;
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("DEBUG_LOGS", "task on failure executed.");
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MapsActivity.this,
                                LOCATION_REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        //stopLocationUpdates();
        Log.d(Tag, "on stop executed.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        Log.d(Tag, "on destroy executed.");
    }

    @Override
    public void onBackPressed() {
        Log.d(Tag, "On back pressed.");
        //turn live route back on screen and remove loaded route
        if(!loadedPolylinePoints.isEmpty()){
            loadedPolylinePoints.clear();
            loadedPolyline.remove();

            if(polyline!=null){
                Log.d(Tag, "Re-adding polyline: "+polyline.getPoints());
                Log.d(Tag, "Re- polyline points: "+polylinePoints);
            }

            //return old polyline and set camera updates on
            //test

            //TODO cant reuse poly options,
            // need to make a new options instance each time

            polyline = mMap.addPolyline(new PolylineOptions()
                    .color(Color.RED)
                    .width(getResources().getDimensionPixelSize(R.dimen.map_poly_width))
                    .addAll(polylinePoints));

            cameraUserInputCounter = 15;

            switchMyLocation(true);
            clearBtn.setVisibility(View.INVISIBLE);
            polySave.setVisibility(View.VISIBLE);
            Log.d(Tag, "removed loaded polyline and returned to old one.");
        }
        else super.onBackPressed();
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("DEBUG_LOGS","onActvivtyResult executed.");
        if(requestCode==MapsActivity.LOCATION_REQUEST_CHECK_SETTINGS){
            switch (resultCode) {
                case Activity.RESULT_OK:
                    Log.d("DEBUG_LOGS","onActvivtyResult RESULT_OK.");
                    // All required changes were successfully made

                    break;
                case Activity.RESULT_CANCELED:
                    Log.d("DEBUG_LOGS","onActvivtyResult RESULT_CANCELED.");
                    // The user was asked to change settings, but chose not to

                    break;
                default:
                    break;
            }
        }

        //receive data about chosen poly route
        else if(requestCode==POLYLINES_ACTIVITY_REQUEST_CODE){
            switch (resultCode){
                case Activity.RESULT_OK:
                    Log.d("DEBUG_LOGS","onActvivtyResult RESULT_OK.");

                    if(data!=null) {
                        Bundle bundle = data.getExtras();

                        if (bundle != null) {
                            clearBtn.setVisibility(View.VISIBLE);
                            polySave.setVisibility(View.INVISIBLE);

                            //stop camera updates here
                            cameraUserInputCounter = 0;

                            //TODO rewrite existing polyline since all points are saved in savedPolyPoints?
                            polyline.remove();

                            switchMyLocation(false);

                            /*
                            polylinePoints.clear();
                            polylinePoints = bundle.getParcelableArrayList("route");
                            polyline.remove();
                            polyline = mMap.addPolyline(polylineOptions.addAll(polylinePoints));*/

                            loadedPolylinePoints = bundle.getParcelableArrayList("route");
                            //test
                            loadedPolyline = mMap.addPolyline(new PolylineOptions().addAll(loadedPolylinePoints));

                            //ugly premove to set zoom
                            mMap.moveCamera(CameraUpdateFactory.zoomTo(18));

                            LatLngBounds bounds = buildBounds(loadedPolylinePoints);
                            CameraUpdate cameraPosition = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                            mMap.moveCamera(cameraPosition);

                            Log.d(Tag, "Loaded points: "+loadedPolylinePoints);
                        }
                        else{
                            Log.d(Tag,"activity result bundle is null.");
                        }

                    }

                    break;
                case Activity.RESULT_CANCELED:
                    Log.d("DEBUG_LOGS","onActvivtyResult RESULT_CANCELED.");
                    break;
                default:
                    break;
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void switchMyLocation(boolean flag) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(flag);
            }
        } else {
            // Permission to access the location is missing. Show rationale and request permission
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        }
    }

    private void getLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    //Toast.makeText(MapsActivity.this, "Last location:\n" + location, Toast.LENGTH_LONG).show();

                                    saveLastLocation(location);
                                }
                                else{
                                    Toast.makeText(MapsActivity.this, "Last location is null.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
    }

    private void saveLastLocation(Location location){
        if(location!=null) {
            String data = location.getLatitude() + "\n" + location.getLongitude();
            Log.d("DEBUG_LOGS", "saving last location data: " + data);

            try (FileOutputStream fos = getApplicationContext().openFileOutput("saved_location", Context.MODE_PRIVATE)) {
                fos.write(data.getBytes());
            } catch (IOException e) {
                Log.d("DEBUG_LOGS", "fileoutputstream error." + e.getMessage());
            }
        }
        else{
            Log.d(Tag,"savelastlocation location is null.");
        }
    }

    private LatLng loadLastLocation(){
        LatLng contents = null;
        Double [] latlang = new Double[2];


        try(FileInputStream fis = getApplicationContext().openFileInput("saved_location")) {

            InputStreamReader inputStreamReader =
                    new InputStreamReader(fis, StandardCharsets.UTF_8);
            //StringBuilder stringBuilder = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                String line = reader.readLine();
                int iterator = 0;

                while (line != null) {
                    latlang[iterator]=Double.valueOf(line);

                    //stringBuilder.append(line).append('\n');
                    line = reader.readLine();

                    iterator++;
                }
            } catch (IOException e) {
                // Error occurred when opening raw file for reading.
                Log.d("DEBUG_LOGS","raw file reading error."+e.getMessage());
            } finally {
                contents = new LatLng(latlang[0],latlang[1]);
                inputStreamReader.close();
                //contents = stringBuilder.toString();
            }

        } catch (IOException e){
            Log.d("DEBUG_LOGS","fileinputstream error."+e.getMessage());
        }

        return contents;
    }

    //remove
    @Override
    public void onMyLocationClick(@NonNull Location location) {
        //Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        //Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).

        getLastLocation();

        mMap.moveCamera(CameraUpdateFactory.zoomTo(16));

        return false;
    }

    @Override
    public void onCameraMoveStarted(int i) {
        if(i== GoogleMap.OnCameraMoveStartedListener.REASON_API_ANIMATION){
            Log.d(Tag,"move started by api buttons.");
            cameraUserInputCounter = 15;
        }
        else if(i== GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION){
            Log.d(Tag,"move started by developer.");
        }
        else if (i == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            Log.d(Tag,"move started by user input.");
            cameraUserInputCounter = 1;
        }
    }

}