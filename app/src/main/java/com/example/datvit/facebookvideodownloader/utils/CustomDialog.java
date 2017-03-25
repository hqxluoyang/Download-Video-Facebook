package com.example.datvit.facebookvideodownloader.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;

import com.example.datvit.facebookvideodownloader.R;

public class CustomDialog {
    Activity context;
    AlertDialog dialogsa;

//    class MyRating implements OnRatingBarChangeListener {
//        MyRating() {
//        }
//
//        public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
//            int barvale = Math.round(rating);
//            Log.e("RATING", "this is barvale :" + barvale);
//            Editor editor = PreferenceManager.getDefaultSharedPreferences(CustomDialog.this.context).edit();
//            editor.putBoolean("locked", true);
//            editor.commit();
//            if (barvale >= 4) {
//                CustomDialog.this.dialogsa.cancel();
//                Intent i = new Intent(Intent.ACTION_VIEW);
//                i.setData(Uri.parse("market://details?id=" + CustomDialog.this.context.getPackageName()));
//                CustomDialog.this.context.startActivity(i);
//                CustomDialog.this.context.finish();
//                return;
//            }
//            CustomDialog.this.dialogsa.cancel();
//            CustomDialog.this.context.finish();
//        }
//    }


    public CustomDialog(Activity applicationContext) {
        this.context = applicationContext;
    }

    public Dialog showRatePopup() {
        View rateview = View.inflate(this.context, R.layout.rating_view, null);
        RatingBar ratebar = (RatingBar) rateview.findViewById(R.id.ratingBar);
        Builder alertDialog = new Builder(this.context);
        alertDialog.setView(rateview);
        alertDialog.setTitle(context.getResources().getString(R.string.rating_header));
        alertDialog.setCancelable(true);
//        ratebar.setOnRatingBarChangeListener(new MyRating());

        alertDialog.setPositiveButton(context.getResources().getString(R.string.rating_no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Editor editor = PreferenceManager.getDefaultSharedPreferences(CustomDialog.this.context).edit();
                editor.putBoolean("locked", true);
                editor.commit();
                CustomDialog.this.dialogsa.cancel();
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("market://details?id=" + CustomDialog.this.context.getPackageName()));
                CustomDialog.this.context.startActivity(i);
                CustomDialog.this.context.finish();
            }
        });

        alertDialog.setNegativeButton(context.getResources().getString(R.string.rating_later), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Editor editor = PreferenceManager.getDefaultSharedPreferences(CustomDialog.this.context).edit();
                editor.putBoolean("locked", false);
                editor.commit();
                CustomDialog.this.dialogsa.cancel();
                CustomDialog.this.context.finish();

            }
        });
        this.dialogsa = alertDialog.create();
        this.dialogsa.show();
        return this.dialogsa;
    }
}
