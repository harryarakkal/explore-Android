package com.example.explaration;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.LocationSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by harryarakkal on 10/13/17.
 */

public class Places implements LocationSource.OnLocationChangedListener{
    final int radius = 50000;

    Context context;

    ArrayList<Place> nearbyPlaces;
    double[] l = null;

    public Places(Context c) {
        context = c;
        nearbyPlaces = new ArrayList<>();
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
            case("poi"): url += context.getString(R.string.poi);
            case("food"): url += context.getString(R.string.food);
            case("fast_food"): url += context.getString(R.string.fast_food);
            case("night_life"): url += context.getString(R.string.night_life);
        }
        url += "&key=" + context.getString(R.string.places_key);
        System.out.println(url);

        //Request a JSON response
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                parse(response);
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
        String bestProvider = String.valueOf(lm.getBestProvider(new Criteria(), true));
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location != null) {
            l = new double[]{location.getLatitude(), location.getLongitude()};
        }else{
            lm.requestLocationUpdates(bestProvider, 1000, 0, (LocationListener) this);
        }
    }

    private void parse(JSONObject response){
        JSONArray results = null;
        try {
            results = response.getJSONArray("results");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        for(int i = 0; i < results.length(); ++i){
            JSONObject p = null;
            try {
                p = (JSONObject) results.get(i);
            } catch (JSONException e) {
                continue;
            }
            String pid = null;
            try {
                pid = p.getString("place_id");
            } catch (JSONException e) {
                continue;
            }
            String name = null;
            try {
                name = p.getString("name");
            } catch (JSONException e) {
                continue;
            }
            double rating = 0;
            try {
                rating = p.getDouble("rating");
            } catch (JSONException e) {
                rating = Double.MIN_VALUE;
            }
            String address = null;
            try {
                address = p.getString("vicinity");
            } catch (JSONException e) {
                address = "No address";
            }
            nearbyPlaces.add(new Place(pid, name, rating, address));
            System.out.println(nearbyPlaces.get(i).print());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        l = new double[]{location.getLatitude(), location.getLongitude()};
    }
}
