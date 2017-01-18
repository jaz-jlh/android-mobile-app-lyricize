package com.dodrio.finalproject;

import java.util.ArrayList;

/**
 * Created by Spivey on 11/4/2016.
 */

public class Album extends Music{
    private int year;
    private String id;
    private ArrayList<Artist> artists;
    public Album(String n, String i, ArrayList<Artist> a) {
        name = n;
        id = i;
        artists = a;
    }
    @Override
    public String toString() {
        String results = "";
        results += name;
        if (artists != null) {
            results += " by ";
            if (artists.size() == 2) {
                results += artists.get(0).getName() + " & " + artists.get(1).getName();
            }else {
                for (int j=0; j<this.getArtists().size();j++) {
                    if(j>0 && j!=this.getArtists().size()) results += ", ";
                    results += artists.get(j).getName();
                }
            }
        }

        return results;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<Artist> getArtists() {
        return artists;
    }

    public void setArtists(ArrayList<Artist> artists) {
        this.artists = artists;
    }
}
