package com.dodrio.finalproject;

import java.util.ArrayList;

/**
 * Created by Spivey on 11/4/2016.
 */

public class Song extends Music{
    private ArrayList<Artist> artists;
    public Song(String n, ArrayList<Artist> a) {
        name = n;
        artists = a;
    }
    @Override
    public String toString() {
        String results = "";
        results += name;
        if(artists != null) {
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

    public ArrayList<Artist> getArtists() {
        return artists;
    }

    public void setArtists(ArrayList<Artist> artists) {
        this.artists = artists;
    }
}
