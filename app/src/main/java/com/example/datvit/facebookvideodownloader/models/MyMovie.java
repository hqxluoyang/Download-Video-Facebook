package com.example.datvit.facebookvideodownloader.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by DatVIT on 11/18/2016.
 */

public class MyMovie {

    @SerializedName("id")
    public String id;
    @SerializedName("title")
    public String title;
    @SerializedName("type")
    public String type;
    @SerializedName("url")
    public String url;
    @SerializedName("start_time")
    public String start_time;
    @SerializedName("end_time")
    public String end_time;
    @SerializedName("cover")
    public String cover;
    @SerializedName("picture")
    public String picture;
    @SerializedName("description")
    public String description;
    @SerializedName("about")
    public String about;
    @SerializedName("created_time")
    public String created_time;
}
