package com.example.explaration;

import android.content.Context;

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

/**
 * Created by harryarakkal on 10/14/17.
 */

public class Routes {
    static final int numPlaces = 3;

    private Context context;
    private ArrayList<Place> nearbyPlaces;
    private ArrayList<ArrayList<double[]>> routes;
    private double[] location;

    public Routes(Context c, ArrayList<Place> np, double[] l){
        context = c;
        nearbyPlaces = np;
        location = l;
        routes = new ArrayList<>();
    }

    public void calculateRoutes(){
        RequestQueue queue = Volley.newRequestQueue(context);

        //Form the url request
        String url = "https://maps.googleapis.com/maps/api/directions/json?";
        url += "origin=" +location[0]+ "," +location[1];
        url += "&key=" + context.getString(R.string.places_key);
         url += "&mode=" + "walking";
        String[] urls = new String[numPlaces];
        for(int i = 0; i < numPlaces; i++){
           urls[i] = url + "&destination=place_id:" + nearbyPlaces.get(i).place_id;
           System.out.println(urls[i]);
        }
        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                parse(response);
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Direction Request Error");
                System.out.println(error);
            }
        };
        //Request directions to each place.
        for(String u : urls) {
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, u, null, listener, errorListener);
            queue.add(jsonObjectRequest);
        }
    }

    private void parse(JSONObject response){
        JSONArray steps  = null;
        try {
            JSONArray routes = response.getJSONArray("routes");
            JSONArray legs = routes.getJSONObject(0).getJSONArray("legs");
            steps = legs.getJSONObject(0).getJSONArray("steps");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ArrayList<double[]> s = new ArrayList<>();
        for(int i = 0; i < steps.length(); i++){
            JSONObject step = null;
            try {
                step = (JSONObject) steps.get(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            assert step != null;
            double lat = 0, lng = 0;
            try {
                lat = step.getJSONObject("end_location").getDouble("lat");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                lng = step.getJSONObject("end_location").getDouble("lng");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            s.add(new double[]{lat, lng});
        }
        routes.add(s);
    }

    public ArrayList<ArrayList<double[]>> getRoutes(){
        return routes;
    }
}
