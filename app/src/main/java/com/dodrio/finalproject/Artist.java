package com.dodrio.finalproject;

/**
 * Created by Spivey on 11/4/2016.
 */

public class Artist extends Music {
    public Artist(String n, String i) {
        name = n;
        id = i;
    }
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return name;
    }
}
