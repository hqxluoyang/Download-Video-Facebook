package com.example.datvit.facebookvideodownloader.models;

/**
 * Created by DatVIT on 11/25/2016.
 */

public class ItemDialog {
    public final int icon;
    public final int text;

    public ItemDialog(Integer text, Integer icon) {
        this.text = text.intValue();
        this.icon = icon.intValue();
    }
}
