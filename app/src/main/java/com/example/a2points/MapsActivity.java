package com.example.a2points;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int LOCATION_REQUEST=500;
    ArrayList<LatLng>listPoints;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        listPoints = new ArrayList<>();


    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST);
                return;
            }
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                //reset marker daca avem deja 2
                if(listPoints.size()==2){
                    listPoints.clear();
                    mMap.clear();
                }
                //save first point selected
                listPoints.add(latLng);
                //set a marker
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);

                if(listPoints.size() == 1){
                    //Adaug primul marker
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

                }else{
                    //Al doilea marker
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }
                mMap.addMarker(markerOptions);

                if(listPoints.size()==2){
                    //Create url de la 1 la 2
                    String url = getReqestUrl(listPoints.get(0),listPoints.get(1));
                    TaskRequestDirection taskRequestDirection = new TaskRequestDirection() ;
                    taskRequestDirection.execute(url);


                }

            }
        });

    }

    private String getReqestUrl(LatLng origin, LatLng dest) {

        //Valoarea originii
        String str_org = "origin" + origin.latitude+","+origin.longitude;
        //Value of destination
        String str_dest = "destination"+dest.latitude+","+dest.longitude;
        String sensor = "sensor-false";
        String mode = "mode-driving";
        String param = str_org+"&"+str_dest+"&"+sensor+"&"+mode;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions" + output + "?"+param;
        return url;

    }

        private String requestDirection(String reqUrl) throws IOException {

            String responseString = "";
            InputStream inputStream = null;
            HttpURLConnection httpURLConnection = null;
            try{
                URL url = new URL(reqUrl);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.connect();

                //Get the response
                inputStream = httpURLConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuffer stringBuffer = new StringBuffer();
                String line = "";
                    while((line = bufferedReader.readLine()) != null){

                        stringBuffer.append(line);

                    }
                    responseString=stringBuffer.toString();
                    bufferedReader.close();
                    inputStreamReader.close();

            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                if(inputStream != null){
                    inputStream.close();
                }
                httpURLConnection.disconnect();

            }

            return responseString;
        }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST:
                if (grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    mMap.setMyLocationEnabled(true);
            }break;

        }

    }
    public class TaskRequestDirection extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... strings) {
            String responseString="";
            try {
                responseString = requestDirection(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //parse json here
            TaskParser taskParser= new TaskParser();
            taskParser.execute(s);

        }
    }
    public class TaskParser extends AsyncTask<String, Void,List<List<HashMap<String,String>>>>{

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject = null;
            List<List<HashMap<String,String>>> routes=null;
            try {
                jsonObject = new JSONObject(strings[0]);
                DirectionParser directionParser = new DirectionParser();
                routes = directionParser.parse(jsonObject);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;

        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            //Get the route and display on the map
            ArrayList points = null;
            PolylineOptions polylineOptions = null;

                for(List<HashMap<String, String>> path:lists){
                    points = new ArrayList();
                    polylineOptions = new PolylineOptions();

                    for(HashMap<String, String> point: path){
                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        points.add(new LatLng(lat,lng));

                    }
                    polylineOptions.addAll(points);
                    polylineOptions.width(15);
                    polylineOptions.color(Color.BLUE);
                    polylineOptions.geodesic(true);

            }

                if(polylineOptions!=null){
                    mMap.addPolyline(polylineOptions);
                }else{
                    Toast.makeText(getApplicationContext(),"Direction not found",Toast.LENGTH_SHORT).show();
                }
        }
    }
}
