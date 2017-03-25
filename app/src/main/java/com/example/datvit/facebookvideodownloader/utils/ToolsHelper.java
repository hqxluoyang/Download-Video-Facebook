package com.example.datvit.facebookvideodownloader.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

import com.example.datvit.facebookvideodownloader.BuildConfig;
import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.models.MyVideo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by DatVIT on 11/25/2016.
 */

public class ToolsHelper {

    private String[] days;
    private String minutes;
    private String[] months;
    private String[] am_pm;
    public static File file;

    public ToolsHelper() {
        this.days = new String[]{BuildConfig.VERSION_NAME, "Sun", "Mon", "Tue", "Wed", "Thr", "Fri", "Sat"};
        this.months = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        this.am_pm = new String[]{"am", "pm"};
        this.file = new File(Environment.getExternalStorageDirectory() + "/GPVideoDownloader");
        if (!this.file.exists()) {
            this.file.mkdir();
        }
    }

    public String getFormattedTime(String time) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ").parse(time);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int day_of_month = cal.get(Calendar.DAY_OF_MONTH);
            int am_or_pm = cal.get(Calendar.AM_PM);
            int day = cal.get(Calendar.DAY_OF_WEEK);
            int hour = cal.get(Calendar.HOUR);
            int minute = cal.get(Calendar.MINUTE);
            int month = cal.get(Calendar.MONTH);
            return this.days[day] + " " + day_of_month + " " + this.months[month] + " " + cal.get(Calendar.YEAR) + " at " + hour + ":" + minute + " " + this.am_pm[am_or_pm];
        } catch (ParseException e) {
            e.printStackTrace();
            return time;
        }
    }

    public void showAlertDialog(final Activity context, String title, String message, Boolean status) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setIcon(status.booleanValue() ? R.drawable.success : R.drawable.fail);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                context.startActivityForResult(new Intent("android.settings.SETTINGS"), ConstantHelper.SETTING_NETWORK);
                context.finish();
            }
        });
        alertDialog.show();
    }

    public String getVideoid(String link) {
        if (link != null) {
            String ids = link.substring(0, link.lastIndexOf("/"));
            return ids.substring(ids.lastIndexOf("/") + 1, ids.length());
        }
        return null;
    }

    public static String getCurrentTimeStamp() {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap downloadImage(String url) {
        Bitmap bitmap = null;
        InputStream stream = null;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inSampleSize = 1;

        try {
            stream = getHttpConnection(url);
            bitmap = BitmapFactory.decodeStream(stream, null, bmOptions);
            stream.close();
        } catch (IOException e1) {
            e1.printStackTrace();
            System.out.println("downloadImage" + e1.toString());
        }
        return bitmap;
    }

    public static InputStream getHttpConnection(String urlString) throws IOException {

        InputStream stream = null;
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();

        try {
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            httpConnection.setConnectTimeout(30000);
            httpConnection.setReadTimeout(30000);
            httpConnection.setInstanceFollowRedirects(true);
            httpConnection.setRequestMethod("GET");
            httpConnection.connect();

            if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                stream = httpConnection.getInputStream();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("downloadImage" + ex.toString());
        }
        return stream;
    }

    public static void shareLinkVideo(Activity activity, String str, String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        String shareBody = url;
        intent.putExtra(Intent.EXTRA_SUBJECT, str);
        intent.putExtra(Intent.EXTRA_TEXT, shareBody);
        activity.startActivity(Intent.createChooser(intent, "Share Link Video"));
    }

    public static void shareVideo(Activity activity, String str, String str2) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_SUBJECT, str);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(str2)));
        activity.startActivity(Intent.createChooser(intent, "Share Video"));
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
