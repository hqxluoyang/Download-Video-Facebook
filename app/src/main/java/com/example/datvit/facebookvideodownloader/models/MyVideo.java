package com.example.datvit.facebookvideodownloader.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by DatVIT on 11/25/2016.
 */

public class MyVideo {

    @SerializedName("id")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("message")
    public String message;
    @SerializedName("picture")
    public String picture;
    @SerializedName("created_time")
    public String created_time;
    @SerializedName("time")
    public String time;
    @SerializedName("duration")
    public String duration;
    @SerializedName("source")
    public String source;
    @SerializedName("type")
    public String type;
    @SerializedName("link")
    public String link;
}
