package com.dodrio.finalproject;

import java.util.ArrayList;

/**
 * Created by Spivey on 11/4/2016.
 */

public class Playlist extends Music{
    private ArrayList<Song> songs;
    private String owner;
    public Playlist(String n, String o) {
        name = n;
        owner = o;
    }
    @Override
    public String toString() {
        if(owner.equals("")) {
            return name;
        } else {
            return "\"" + name + "\"" + " owned by " + owner + "\n";
        }

    }

    public ArrayList<Song> getSongs() {
        return songs;
    }

    public void setSongs(ArrayList<Song> songs) {
        this.songs = songs;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
