package com.example.explaration;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static java.lang.Math.atan;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

/**
 * Created by harryarakkal on 10/13/17.
 */

public class Places implements LocationListener, SensorEventListener{
    final int radius = 2500;
    final double empty  = 0.000000000000000000001;

    Context context;

    private ArrayList<Place> nearbyPlaces;
    private ArrayList<ArrayList<double[]>> routes;
    private double[] l;
    private double[] angles;

    SensorManager mSensorManager;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];
    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    public Places(Context c) {
        context = c;
        nearbyPlaces = new ArrayList<>();
        angles = new double[]{0,0,0};
        l = new double[2];
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
    }

    /*
    Input: type - String : type of locations to look for.
     */
    public void findNearby(String type) {
        RequestQueue queue = Volley.newRequestQueue(context);

        //Form the url request
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
        getLocation();
        while(l == null){}
        url += "location=" + Double.toString(l[0])+","+Double.toString(l[1]);
        url += "&radius=" + radius;
        url += "&type=";
        switch(type){
            case("poi"):
                url += context.getString(R.string.poi);
                break;
            case("food"):
                url += context.getString(R.string.food);
                break;
            case("fast_food"):
                url += context.getString(R.string.fast_food);
                break;
            case("night_life"):
                url += context.getString(R.string.night_life);
                break;
        }
        url += "&key=" + context.getString(R.string.places_key);
        System.out.println(url);

        //Request a JSON response
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                parse(response);
                if(nearbyPlaces.size() > 1) {
                    Routes r = new Routes(context, nearbyPlaces, l);
                    r.calculateRoutes();
                    routes = r.getRoutes();
                }else{
                    System.out.println("Not enough places!");
                }
            }
        }, new Response.ErrorListener(){

            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Places Request Error");
                System.out.println(error);
            }
        });
        queue.add(jsonObjectRequest);
    }

    private void getLocation() {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // Check if fine location and coarse location permissions are granted.
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        assert lm != null;
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location != null) {
            l[0] = location.getLatitude();
            l[1] = location.getLongitude();
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
    }

    private void parse(JSONObject response){
        JSONArray results = null;
        try {
            results = response.getJSONArray("results");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        for(int i = 0; i < results.length(); ++i){
            JSONObject p;
            try {
                p = (JSONObject) results.get(i);
            } catch (JSONException e) {
                continue;
            }
            String pid;
            try {
                pid = p.getString("place_id");
            } catch (JSONException e) {
                continue;
            }
            String name;
            try {
                name = p.getString("name");
            } catch (JSONException e) {
                continue;
            }
            double rating = p.optDouble("rating", Double.MIN_VALUE);
            String address = p.optString("vicinity","No address");
            nearbyPlaces.add(new Place(pid, name, rating, address));
            System.out.println(nearbyPlaces.get(i).print());
        }
    }

    public String getName(int index){
        return nearbyPlaces.get(index).name;
    }

    public double getAngle(int index){
        return angles[index];
    }

    @Override
    public void onLocationChanged(Location location) {
        l[0] = location.getLatitude();
        l[1] = location.getLongitude();
        updateOrientationAngles();
        if(routes != null){
            for(int i = 0; i < Routes.numPlaces; i++){
                if(angles[i] == empty) continue;
                double latdif = abs(l[0]-routes.get(i).get(0)[0]);
                double lngdif = abs(l[1]-routes.get(i).get(0)[1]);
                double distance = sqrt(latdif*latdif+lngdif*lngdif);
                if(distance <= .00005) {
                    routes.get(i).remove(0);
                    angles[0] = empty;
                    angles[1] = empty;
                    angles[2] = empty;
                }
                double bearing = atan(latdif/lngdif);
                angles[i] = bearing - mRotationMatrix[7];
            }
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }
    }

    public void updateOrientationAngles() {
        SensorManager.getRotationMatrix(mRotationMatrix, null, mAccelerometerReading, mMagnetometerReading);
        SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);
        SensorManager.remapCoordinateSystem(mRotationMatrix,SensorManager.AXIS_Y,SensorManager.AXIS_MINUS_X, mRotationMatrix);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
