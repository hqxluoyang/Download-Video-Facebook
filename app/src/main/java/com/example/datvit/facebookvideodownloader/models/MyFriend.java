package com.example.datvit.facebookvideodownloader.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by DatVIT on 11/27/2016.
 */

public class MyFriend {

    @SerializedName("id")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("picture")
    public String picture;
    @SerializedName("like")
    public String like;
    @SerializedName("gender")
    public String gender;
    @SerializedName("cover")
    public String cover;

}
