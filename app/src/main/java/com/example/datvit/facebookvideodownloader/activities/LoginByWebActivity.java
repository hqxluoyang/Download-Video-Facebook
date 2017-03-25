package com.example.datvit.facebookvideodownloader.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.utils.ConnectionDetector;
import com.example.datvit.facebookvideodownloader.utils.FacebookSession;
import com.example.datvit.facebookvideodownloader.utils.ToolsHelper;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Created by DatVIT on 11/12/2016.
 */

public class LoginByWebActivity extends AppCompatActivity {

    public static final String TAG = "VolleyPatterns";

    private String email;
    private String id;
    private String main_token;
    private String name;
    private String picurl;
    private String coverurl;
    private String urlJsonArry;

    private WebView myweb;
    private ToolsHelper toolsHelper;
    private RequestQueue mRequestQueue;
    private AVLoadingIndicatorView bar;
    private ConnectionDetector cd;
    public static String PERMISSIONS =
            "user_posts," +
                    "user_videos," +
                    "email," +
                    "user_friends," +
                    "public_profile," +
                    "user_likes," +
                    "read_stream," +
                    "user_groups," +
                    "user_actions.video";

    private FacebookSession facebookSession;
    private int method_login = 0;
    private SharedPreferences loginState;
    private SharedPreferences.Editor loginStateEditor;

    private Locale myLocale;

    public void loadLocale() {
        String langPref = "Language";
        SharedPreferences prefs = getSharedPreferences("GPVideo", Activity.MODE_PRIVATE);
        String language = prefs.getString(langPref, "en");
        myLocale = new Locale(language);
        Locale.setDefault(myLocale);
        Configuration config = new Configuration();
        config.locale = myLocale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        setContentView(R.layout.login_facebook_web);
        cd = new ConnectionDetector(this);
        toolsHelper = new ToolsHelper();
        facebookSession = new FacebookSession(this);
        this.myweb = (WebView) findViewById(R.id.webLogin);
        this.bar = (AVLoadingIndicatorView) findViewById(R.id.avi);
        loginState = getSharedPreferences("LOGIN_STATE", Context.MODE_PRIVATE);
        loginStateEditor = loginState.edit();
        Intent intent = getIntent();
        if (intent != null) {
            method_login = intent.getIntExtra("method_login", 0);
        }
        if (cd.isNetworkAvailable(getApplicationContext()) && cd.isConnectingToInternet()) {
            setUpWebProperties();
        } else {
            toolsHelper.showAlertDialog(LoginByWebActivity.this, getResources().getString(R.string.network_connection),
                    getResources().getString(R.string.not_connection),
                    Boolean.valueOf(false));
        }
    }

    @SuppressLint({"NewApi"})
    public void setUpWebProperties() {
        String loginUrl = "https://m.facebook.com/dialog/oauth?client_id=" +
                getResources().getString(R.string.facebook_graph_id) +
                "&redirect_uri=" + getResources().getString(R.string.redirect_uri) +
                "&domain=&origin=file%3A%2F%2F%2Ff3b6d7ef1c&relation=parent" +
                "&response_type=token,signed_request,code&sdk=joey" +
                "&scope=" + PERMISSIONS;

        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        runOnUiThread(new Runnable() {
            public void run() {
                bar.setVisibility(View.VISIBLE);
            }
        });
        WebSettings webSettings = myweb.getSettings();
        webSettings.setJavaScriptEnabled(true);
        this.myweb.setWebChromeClient(new WebChromeClient());
        this.myweb.setWebViewClient(new MyWebClient());
        WebSettings ws = this.myweb.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setBuiltInZoomControls(true);
        ws.setSupportZoom(true);
        Configuration config = getResources().getConfiguration();
        if (Build.VERSION.SDK_INT > 14) {
            ws.setTextZoom((int) (config.fontScale * 100.0f));
        }
        this.myweb.loadUrl(loginUrl);
    }

    class MyWebClient extends WebViewClient {

        class MyRunnable implements Runnable {
            MyRunnable() {
            }

            public void run() {
                LoginByWebActivity.this.bar.setVisibility(View.INVISIBLE);
            }
        }


        class MyRunnableTwo implements Runnable {
            MyRunnableTwo() {
            }

            public void run() {
                LoginByWebActivity.this.bar.setVisibility(View.VISIBLE);
                LoginByWebActivity.this.notifyActivity();
            }
        }

        MyWebClient() {
        }

        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.e("LOGIN_WEB", "this is onPageStarted : " + url);
        }

        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.e("LOGIN_WEB", "this is onPageFinished : " + url);
            LoginByWebActivity.this.runOnUiThread(new MyRunnable());
            if (url.contains("&access_token=")) {
                String[] main = url.split("&access_token=");
                String[] main1 = main[main.length - 1].split("&expires_in=");
                Log.e("LOGIN_WEB", "this is exp_time : " + main1[main1.length - 1].split("&code=")[0]);
                LoginByWebActivity.this.main_token = main1[0];
                Log.e("LOGIN_WEB", "this is main token" + LoginByWebActivity.this.main_token);
                LoginByWebActivity.this.hideWebView();
                LoginByWebActivity.this.runOnUiThread(new MyRunnableTwo());
            }
        }
    }

    private void notifyActivity() {
        userQueryUrl();
    }


    public RequestQueue getRequestQueue() {
        if (this.mRequestQueue == null) {
            this.mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        return this.mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }


    private void getUserInfo() {
        this.bar.setVisibility(View.VISIBLE);
        addToRequestQueue(new JsonObjectRequest(0, this.urlJsonArry, null, new Response.Listener<JSONObject>() {
            public void onResponse(JSONObject response) {
                try {
                    name = response.getString("name");
                    id = response.getString("id");
                    if (response.has("email")) {
                        email = response.getString("email");
                    } else {
                        email = null;
                    }
                    if (response.has("picture")) {
                        picurl = response.getJSONObject("picture").getJSONObject("data").getString("url");
                    }
                    if (response.has("cover")) {
                        coverurl = response.getJSONObject("cover").getString("source");
                    }
                    facebookSession.storeAccessToken(name, email, picurl, main_token, id, coverurl);
                    Log.e("USER_LOGIN", "ID: " + id + " - Name: " + name + " - Token: " + main_token);
                    bar.setVisibility(View.INVISIBLE);
                    loginStateEditor.putBoolean("login_state", true);
                    loginStateEditor.putInt("login_method", 2);
                    loginStateEditor.commit();
                    goActivity();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext().getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                bar.setVisibility(View.INVISIBLE);
            }
        }));
    }

    private void userQueryUrl() {
        this.urlJsonArry = "https://graph.facebook.com/v2.2/me?&access_token=";
        this.urlJsonArry += this.main_token;
        this.urlJsonArry += "&fields=name,middle_name,id,last_name,first_name,email,cover,picture.type(large)";
        getUserInfo();
    }

    private void goActivity() {
        Intent intent = new Intent(LoginByWebActivity.this, MainActivity.class);
        intent.putExtra("method_login", method_login);
        startActivity(intent);
        finish();
    }

    private void showWebView() {
        this.myweb.setVisibility(View.VISIBLE);
    }

    private void hideWebView() {
        this.myweb.setVisibility(View.INVISIBLE);
    }

}
