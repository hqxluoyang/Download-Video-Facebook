package com.example.datvit.facebookvideodownloader.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by DatVIT on 11/27/2016.
 */

public class MyPage {

    @SerializedName("id")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("cover")
    public String cover;
    @SerializedName("category")
    public String category;
    @SerializedName("picture")
    public String picture;
    @SerializedName("created_time")
    public String created_time;
}
