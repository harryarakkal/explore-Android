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
import java.util.HashMap;

/**
 * Created by harryarakkal on 10/14/17.
 */

public class Details {
    private Context context;
    public String address;
    public String name;
    public double rating;
    public ArrayList<HashMap<String, String>> reviews;
    public String website;

    public Details(Context context) {
        this.context = context;
    }

    public void getDetails(String place_id){
        RequestQueue queue = Volley.newRequestQueue(context);

        //Form the url request
        String url = "https://maps.googleapis.com/maps/api/place/details/json?";
        url += "key=" + context.getString(R.string.places_key);
        url += "&place_id=" + place_id;

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

    private void parse(JSONObject response){
        JSONObject result;
        try {
            result = response.getJSONObject("result");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        address = result.optString("formatted_address");
        name = result.optString("name");
        rating = result.optDouble("rating");
        website = result.optString("website");
        JSONArray r = result.optJSONArray("reviews");
        for(int i = 0; i < r.length(); i++){
            HashMap<String, String> rev = new HashMap<>();
            JSONObject obj = (JSONObject) r.opt(i);
            if(obj == null) continue;
            rev.put("author", obj.optString("author_name"));
            rev.put("rating", String.valueOf(obj.optInt("rating")));
            rev.put("time", obj.optString("relative_time_description"));
            rev.put("text", obj.optString("text"));
        }
    }
}
