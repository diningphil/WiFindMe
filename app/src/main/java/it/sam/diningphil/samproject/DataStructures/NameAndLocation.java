package it.sam.diningphil.samproject.DataStructures;

import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.Serializable;

public class NameAndLocation implements Serializable{

    private static final long serialVersionUID = 42L;

    private String name;
    private double latitude;
    private double longitude;

    public NameAndLocation(String n, Location l){
        name = n;
        latitude = l.getLatitude();
        longitude = l.getLongitude();
    }

    @Override
    public String toString() {
        return "ELEMENTO: " + name + " " + latitude + " " + longitude;
    }

    public Location getLocation(){
        Location loc = new Location("");
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);

        return loc;
    }

    public double getLatitude(){ return latitude; }

    public double getLongitude(){ return longitude; }

    public String getName(){ return name; }

    public JSONArray getJsonArrayForLocation() {
        JSONArray jsonArray = new JSONArray();

        try {
            jsonArray.put(latitude);
            jsonArray.put(longitude);
        } catch (JSONException e) {
            jsonArray.put(0);
            jsonArray.put(0);
        }

        return jsonArray;
    }

}
