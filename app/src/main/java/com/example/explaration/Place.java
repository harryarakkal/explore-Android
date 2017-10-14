package com.example.explaration;

/**
 * Created by harryarakkal on 10/13/17.
 */

public class Place {
    public String place_id;
    public String name;
    public double rating;
    public String address;

    public Place(String p, String n, double r, String a){
        place_id = p;
        name = n;
        rating = r;
        address = a;
    }

    public String print(){
        return name + " at " + address + " is rated " + rating;
    }
}
