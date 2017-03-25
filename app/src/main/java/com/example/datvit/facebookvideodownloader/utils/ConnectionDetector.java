package com.example.datvit.facebookvideodownloader.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;

public class ConnectionDetector {
    private Context _context;

    public ConnectionDetector(Context context) {
        this._context = context;
    }

    public boolean isConnectingToInternet() {
        if (_context != null) {
            ConnectivityManager connectivity = (ConnectivityManager) this._context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null) {
                NetworkInfo[] info = connectivity.getAllNetworkInfo();
                if (info != null) {
                    for (NetworkInfo state : info) {
                        if (state.getState() == State.CONNECTED) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean isNetworkAvailable(Context context) {
        boolean status = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getNetworkInfo(0);
            if (netInfo == null || netInfo.getState() != State.CONNECTED) {
                netInfo = cm.getNetworkInfo(1);
                if (netInfo != null && netInfo.getState() == State.CONNECTED) {
                    status = true;
                }
            } else {
                status = true;
            }
            return status;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
