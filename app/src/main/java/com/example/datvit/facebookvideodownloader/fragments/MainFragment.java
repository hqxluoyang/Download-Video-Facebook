package com.example.datvit.facebookvideodownloader.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.activities.MainActivity;
import com.example.datvit.facebookvideodownloader.activities.StartActivity;
import com.example.datvit.facebookvideodownloader.utils.FacebookSession;
import com.facebook.AccessToken;
import com.squareup.picasso.Picasso;

import java.util.Locale;

/**
 * Created by DatVIT on 12/8/2016.
 */
public class MainFragment extends Fragment {

    private Locale myLocale;
    private String lang;
    private FacebookSession facebookSession;
    private SharedPreferences loginState;
    private SharedPreferences.Editor loginStateEditor;
    private ImageView avatar;
    private TextView name, email;
    private String profile_name = "", profile_id = "", profile_avatar = null,
            token = null, profile_email = "", profile_cover = null;
    private Button btnLogout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        SharedPreferences prefs = getActivity().getSharedPreferences("GPVideo", Activity.MODE_PRIVATE);
        lang = prefs.getString("Language", "en");
        facebookSession = new FacebookSession(getActivity());
        this.loginState = getActivity().getSharedPreferences("LOGIN_STATE", Context.MODE_PRIVATE);
        this.loginStateEditor = this.loginState.edit();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        avatar = (ImageView) view.findViewById(R.id.profile_image);
        name = (TextView) view.findViewById(R.id.profile_name);
        email = (TextView) view.findViewById(R.id.profile_user_id);
        btnLogout = (Button) view.findViewById(R.id.btnLogout);
        getInfor();

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logOutFacebook();
            }
        });
        super.onViewCreated(view, savedInstanceState);
    }


    private void getInfor() {
        profile_name = facebookSession.getUsername();
        profile_id = facebookSession.getUserId();
        profile_email = facebookSession.getEmail();
        token = facebookSession.getToken();
        profile_cover = facebookSession.getUserCover();
        profile_avatar = facebookSession.getUserPicture();
        setUpInfo();
    }

    private void setUpInfo() {
        if (profile_avatar != null) {
            Picasso.with(getActivity()).load(profile_avatar)
                    .error(R.drawable.silhouette).into(avatar);
        }

        if (profile_name != null) {
            name.setText(profile_name);
        }

        if (profile_email != null) {
            email.setText(profile_email);
        }
    }

    public void loadLocale() {
        String langPref = "Language";
        SharedPreferences prefs = getActivity().getSharedPreferences("GPVideo", Activity.MODE_PRIVATE);
        String language = prefs.getString(langPref, "en");
        myLocale = new Locale(language);
        Locale.setDefault(myLocale);
        Configuration config = new Configuration();
        config.locale = myLocale;
        getActivity().getBaseContext().getResources().updateConfiguration(config,
                getActivity().getBaseContext().getResources().getDisplayMetrics());

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
