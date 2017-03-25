package com.example.datvit.facebookvideodownloader.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by DatVIT on 11/27/2016.
 */

public class MyGroup {

    @SerializedName("id")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("icon")
    public String icon;
    @SerializedName("cover")
    public String cover;
    @SerializedName("owner")
    public String owner;
    @SerializedName("description")
    public String description;
    @SerializedName("updated_time")
    public String updated_time;
    @SerializedName("time")
    public String time;
    @SerializedName("picture")
    public String picture;

}
