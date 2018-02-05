package com.education.sirkel.earthquakegeo.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.education.sirkel.earthquakegeo.Model.EarthQuake;
import com.education.sirkel.earthquakegeo.R;
import com.education.sirkel.earthquakegeo.UI.CustomInfoWindow;
import com.education.sirkel.earthquakegeo.Util.Constants;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener{

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private RequestQueue queue;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private BitmapDescriptor [] iconColors;
    private Button showListBtn;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        showListBtn = (Button) findViewById(R.id.showListBtn);
        showListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MapsActivity.this,QuakesListActivity.class));
            }
        });

        //Untuk random color marker
        iconColors = new BitmapDescriptor[] {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
                //  BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)

        };

        queue = Volley.newRequestQueue(this);
        //Fungsi mengambil API dari Earthquakes
        getEarthQuakes();
    }

    //Fungsi untuk mengambil API dari Earhquakes
    private void getEarthQuakes() {
        //Mendefinisikan Object EarthQuake
        final EarthQuake earthQuake = new EarthQuake();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                Constants.URL, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {

                    JSONArray features = response.getJSONArray("features");
                    for (int i = 0 ; i < Constants.LIMIT;i++){
                        //get properties
                        JSONObject properties = features.getJSONObject(i).getJSONObject("properties");

                        //get geometry object
                        JSONObject geometry = features.getJSONObject(i).getJSONObject("geometry");

                        //get coordinates array
                        JSONArray coordinates = geometry.getJSONArray("coordinates");
                        double lon = coordinates.getDouble(0);
                        double lat = coordinates.getDouble(1);

                        earthQuake.setPlace(properties.getString("place"));
                        earthQuake.setType(properties.getString("type"));
                        earthQuake.setTime(properties.getLong("time"));
                        earthQuake.setLat(lat);
                        earthQuake.setLon(lon);
                        earthQuake.setMagnitude(properties.getDouble("mag"));
                        earthQuake.setDetailLink(properties.getString("detail"));

                        //Untuk mengubah besaran time yang masih dalam mili sekon ke bentuk jam
                        java.text.DateFormat dateFormat = java.text.DateFormat.getDateInstance();
                        String formatedDate = dateFormat.format(new Date(Long.valueOf(properties.getLong("time")))
                                .getTime());

                        //Untuk mendefinisikan marker seperti apa
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.icon(iconColors[Constants.randomInt(iconColors.length,0)]);
                        markerOptions.title(earthQuake.getPlace());
                        markerOptions.position(new LatLng(lat, lon));
                        markerOptions.snippet("Magnitude : " + earthQuake.getMagnitude() + "\n"
                                + "Date : "+ formatedDate);
                        //Saat ada magnitude yang berbahaya akan ditandai merah
                        if (earthQuake.getMagnitude() > 6.0){
                            CircleOptions circleOptions = new CircleOptions();
                            circleOptions.center(new LatLng(lat,lon));
                            circleOptions.radius(30000);
                            circleOptions.strokeWidth(2.5f);
                            circleOptions.fillColor(Color.RED);

                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                            mMap.addCircle(circleOptions);

                        }

                        Marker marker = mMap.addMarker(markerOptions);
                        //untuk ke link
                        marker.setTag(earthQuake.getDetailLink());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat,lon), 1));


                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(jsonObjectRequest);
    }



    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //Memasukan fungsi dibawah setelah membuat adapter di CustomInfoWindow
        mMap.setInfoWindowAdapter(new CustomInfoWindow(getApplicationContext()));
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerClickListener(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        if (Build.VERSION.SDK_INT < 23){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                //Ask permission
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);

            }else {
                //we have permission
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location location = getLastKnownLocation();
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//                mMap.addMarker(new MarkerOptions()
//                        .position(latLng)
//                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
//                        .title("Hello"));
//                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 8));
            }
        }

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED){
                //awal di tulis
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            }
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
    }

    LocationManager mLocationManager;

    //Fungsi dari google, untuk mencari posisi terakhir di googleMaps
    private Location getLastKnownLocation() {
        mLocationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

        getQuakeDetails(marker.getTag().toString());
//        Toast.makeText(getApplicationContext(), marker.getTag().toString(),Toast.LENGTH_LONG).show();
    }

    //Untuk menampilkan saat marker di klik --> terhubung dngan fungsi getMoreDetails
    private void getQuakeDetails(String url) {

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                String detailsUrl = "";
                try {
                    JSONObject properties = response.getJSONObject("properties");
                    JSONObject products = properties.getJSONObject("products");
                    JSONArray geoserve = products.getJSONArray("geoserve");

                    for (int i = 0 ; i<geoserve.length();i++){
                        JSONObject geoserveObj = geoserve.getJSONObject(i);

                        JSONObject contentObj = geoserveObj.getJSONObject("contents");
                        JSONObject geoJsonObj = contentObj.getJSONObject("geoserve.json");

                        detailsUrl = geoJsonObj.getString("url");
                    }

                    getMoreDetails(detailsUrl);



                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(jsonObjectRequest);

    }
    //Fungsi untuk popup saat di klik detail
    public void getMoreDetails(String url){

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET
                , url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                dialogBuilder = new AlertDialog.Builder(MapsActivity.this);
                View view = getLayoutInflater().inflate(R.layout.popup, null);

                Button dismissBtn = (Button) view.findViewById(R.id.dismissPop);
                Button dismissBtnTop = (Button) view.findViewById(R.id.dismissPopTop);
                TextView popList = (TextView) view.findViewById(R.id.popList);
                WebView htmlPop = (WebView) view.findViewById(R.id.htmlWebview);

                StringBuilder stringBuilder = new StringBuilder();

                try {

                    if (response.has("tectonicSummary") && response.getString("tectonicSummary") != null){
                        JSONObject tectonic = response.getJSONObject("tectonicSummary");
                        if (tectonic.has("text") && tectonic.getString("text") != null){
                            String text = tectonic.getString("text");

                            htmlPop.loadDataWithBaseURL(null,text,"text/html", "UTF-8",null);
                        }
                    }

                    JSONArray cities = response.getJSONArray("cities");

                    for (int i = 0; i < cities.length() ; i++){
                        JSONObject citiesObj = cities.getJSONObject(i);

                        stringBuilder.append("City : " + citiesObj.getString("name")
                                +"\n" + "Distance : " + citiesObj.getString("distance")
                                +"\n" + "Population : " + citiesObj.getString("population"));

                        stringBuilder.append("\n\n");
                    }
                    popList.setText(stringBuilder);

                    dialogBuilder.setView(view);
                    dialog = dialogBuilder.create();
                    dialog.show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                dismissBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                dismissBtnTop.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(jsonObjectRequest);

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }
}
