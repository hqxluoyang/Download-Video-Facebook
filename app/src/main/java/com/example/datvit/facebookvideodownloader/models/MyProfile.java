package com.example.datvit.facebookvideodownloader.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by DatVIT on 11/19/2016.
 */

public class MyProfile {

    @SerializedName("id")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("middle_name")
    public String middle_name;
    @SerializedName("last_name")
    public String last_name;
    @SerializedName("first_name")
    public String first_name;
    @SerializedName("email")
    public String email;
    @SerializedName("cover")
    public String cover;
    @SerializedName("picture")
    public String picture;

}
