package com.example.datvit.facebookvideodownloader.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.activities.LoginByWebActivity;
import com.example.datvit.facebookvideodownloader.activities.MainActivity;
import com.example.datvit.facebookvideodownloader.activities.StartActivity;
import com.example.datvit.facebookvideodownloader.utils.FacebookSession;
import com.example.datvit.facebookvideodownloader.utils.ToolsHelper;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.squareup.picasso.Picasso;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Created by DatVIT on 12/8/2016.
 */
public class SettingFragment extends Fragment {

    private LinearLayout btnLogout, btnRate, btnChooseSave, btnLanguage, btnShareApp;
    private Locale myLocale;
    private String lang;
    private int selectedPosition;
    private FacebookSession facebookSession;
    private SharedPreferences loginState;
    private SharedPreferences.Editor loginStateEditor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        SharedPreferences prefs = getActivity().getSharedPreferences("GPVideo", Activity.MODE_PRIVATE);
        lang = prefs.getString("Language", "en");

        if (lang.equals("vi")) {
            selectedPosition = 0;
        } else {
            selectedPosition = 1;
        }
        facebookSession = new FacebookSession(getActivity());
        this.loginState = getActivity().getSharedPreferences("LOGIN_STATE",Context.MODE_PRIVATE);
        this.loginStateEditor = this.loginState.edit();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        btnRate = (LinearLayout) view.findViewById(R.id.btnRate);
        btnLogout = (LinearLayout) view.findViewById(R.id.btnLogout);
        btnChooseSave = (LinearLayout) view.findViewById(R.id.btnChooseSave);
        btnLanguage = (LinearLayout) view.findViewById(R.id.btnLanguage);
        btnShareApp = (LinearLayout) view.findViewById(R.id.btnShareApp);

        btnRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGooglePlay();
            }
        });

        btnLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeLanguage();
            }
        });

        btnShareApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareApp();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logOutFacebook();
            }
        });

        super.onViewCreated(view, savedInstanceState);
    }

    private void shareApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            String shareBody = "https://play.google.com/store/apps/details?id=" + getActivity().getPackageName();
            intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share_app));
            intent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_now)));
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void loadLocale() {
        String langPref = "Language";
        SharedPreferences prefs = getActivity().getSharedPreferences("GPVideo", Context.MODE_PRIVATE);
        String language = prefs.getString(langPref, "en");
        myLocale = new Locale(language);
        Locale.setDefault(myLocale);
        Configuration config = new Configuration();
        config.locale = myLocale;
        getActivity().getBaseContext().getResources().updateConfiguration(config,
                getActivity().getBaseContext().getResources().getDisplayMetrics());

    }

    private void openGooglePlay() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getActivity().getPackageName())));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getActivity().getPackageName())));
        }
    }

    private void changeLanguage() {
        CharSequence[] items = {"Vietnamese", "English"};
        final AlertDialog settingLanguage = new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.choose_language))
                .setSingleChoiceItems(items, selectedPosition, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                saveLangage("vi");
                                Toast.makeText(getActivity(), getResources().getString(R.string.reset_app_for_apply), Toast.LENGTH_SHORT).show();
                                break;
                            case 1:
                                saveLangage("en");
                                Toast.makeText(getActivity(), getResources().getString(R.string.reset_app_for_apply), Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                break;
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.cancel), null)
                .create();
        settingLanguage.show();
    }

    private void saveLangage(String lang) {
        String langPref = "Language";
        SharedPreferences prefs = getActivity().getSharedPreferences("GPVideo", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(langPref, lang);
        editor.commit();
    }

    private void logOutFacebook() {
        final AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(getActivity());
        alertDialogBuilder
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getResources().getString(R.string.logout))
                .setMessage(getResources().getString(R.string.are_you_sure));

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                CookieSyncManager.createInstance(getActivity());
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.removeAllCookie();
                cookieManager.setAcceptCookie(false);
                loginStateEditor.putBoolean("login_state", false);
                loginStateEditor.putInt("login_method", 0);
                loginStateEditor.putString("cookie", null);
                loginStateEditor.putString("url", null);
                loginStateEditor.commit();
                facebookSession.resetAccessToken();
                AccessToken.setCurrentAccessToken(null);
                Intent intent = new Intent(getActivity(), StartActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }
}
