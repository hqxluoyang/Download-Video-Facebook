package com.example.datvit.facebookvideodownloader.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Button;
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
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.common.Scopes;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Locale;

/**
 * Created by DatVIT on 12/15/2016.
 */
public class StartActivity extends AppCompatActivity implements View.OnClickListener {

    private Button buttonWeb;
    private Button buttonApp;

    public static final String TAG = "VolleyPatterns";
    private LoginButton btnLogin;

    private String email;
    private String id;
    private String main_token;
    private String name;
    private String picurl;
    private String coverurl;
    private String urlJsonArry;

    private RequestQueue mRequestQueue;

    private CallbackManager callbackManager;

    private FacebookSession facebookSession;
    private SharedPreferences loginState;
    private SharedPreferences.Editor loginStateEditor;
    private ConnectionDetector cd;
    private ToolsHelper toolsHelper;
    private int method = 0;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        setContentView(R.layout.start_activity);

        cd = new ConnectionDetector(this);
        toolsHelper = new ToolsHelper();
        callbackManager = CallbackManager.Factory.create();
        facebookSession = new FacebookSession(this);
        loginState = getSharedPreferences("LOGIN_STATE", Context.MODE_PRIVATE);
        loginStateEditor = loginState.edit();
        method = loginState.getInt("login_method", 0);
        buttonApp = (Button) findViewById(R.id.login_button_sdk);
        buttonWeb = (Button) findViewById(R.id.login_button_web);

        buttonApp.setOnClickListener(this);
        buttonWeb.setOnClickListener(this);

        btnLogin = (LoginButton) findViewById(R.id.loginApp);
        btnLogin.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.e("LOGIN_FRAGMENT", "onSuccess");
                String accessToken = loginResult.getAccessToken().getToken();
                String user_id = loginResult.getAccessToken().getUserId();
                Log.e("LOGIN_FRAGMENT", "accessToken: " + accessToken + " - " + user_id);
                main_token = accessToken;
                facebookSuccess();
            }

            @Override
            public void onCancel() {
                Log.e("LOGIN_FRAGMENT", "onCancel");
            }

            @Override
            public void onError(FacebookException exception) {
                Log.e("LOGIN_FRAGMENT", "onError");
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginManager.getInstance().logInWithReadPermissions(StartActivity.this,
                        Arrays.asList(
                                "user_posts",
                                "user_videos",
                                "user_likes",
                                "user_managed_groups",
                                "email",
                                "user_friends",
                                "user_actions.video",
                                "public_profile"
                        ));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (callbackManager != null) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    public RequestQueue getRequestQueue() {
        if (this.mRequestQueue == null) {
            this.mRequestQueue = Volley.newRequestQueue(this);
        }
        return this.mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }


    private void getUserInfo() {
        addToRequestQueue(new JsonObjectRequest(0, this.urlJsonArry, null, new Response.Listener<JSONObject>() {
            public void onResponse(JSONObject response) {
                try {
                    Log.e("USER_LOGIN", response.toString());
                    if (response.has("name")) {
                        name = response.getString("name");
                    }
                    if (response.has("id")) {
                        id = response.getString("id");
                    }
                    if (response.has("email")) {
                        email = response.getString("email");
                    }
                    if (response.has("picture")) {
                        JSONObject innerdataobj = response.getJSONObject("picture").getJSONObject("data");
                        picurl = innerdataobj.getString("url");
                    }
                    if (response.has("cover")) {
                        JSONObject jsonObject = response.getJSONObject("cover");
                        coverurl = jsonObject.getString("source");
                    }
                    facebookSession.storeAccessToken(name, email, picurl, main_token, id, coverurl);
                    Log.e("USER_LOGIN", "ID: " + id + " - Name: " + name + " - Token: " + main_token);
                    loginStateEditor.putBoolean("login_state", true);
                    loginStateEditor.putInt("login_method", 1);
                    loginStateEditor.commit();
                    goActivity();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(StartActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void facebookSuccess() {
        this.urlJsonArry = "https://graph.facebook.com/v2.2/me?&access_token=";
        this.urlJsonArry += this.main_token;
        this.urlJsonArry += "&fields=name,middle_name,id,last_name,first_name,email,cover,picture.type(large)";
        getUserInfo();
    }

    private void goActivity() {
        Intent intent = new Intent(StartActivity.this, MainActivity.class);
        intent.putExtra("method_login", 1);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View view) {
        if (view == buttonApp) {
            if (cd.isConnectingToInternet()) {
                if (method == 1) {
                    goActivity();
                } else {
                    CookieManager.getInstance().removeAllCookie();
                    btnLogin.performClick();
                }
            } else {
                toolsHelper.showAlertDialog(StartActivity.this, getResources().getString(R.string.network_connection),
                        getResources().getString(R.string.not_connection), Boolean.valueOf(false));
            }
        } else if (view == buttonWeb) {
            if (cd.isConnectingToInternet()) {
                if (method == 2) {
                    Intent intent = new Intent(StartActivity.this, MainActivity.class);
                    intent.putExtra("method_login", 2);
                    startActivity(intent);
                    finish();
                } else {
                    AccessToken.setCurrentAccessToken(null);
                    Intent intent = new Intent(StartActivity.this, LoginByWebActivity.class);
                    intent.putExtra("method_login", 2);
                    startActivity(intent);
                    finish();
                }
            } else {
                toolsHelper.showAlertDialog(StartActivity.this, getResources().getString(R.string.network_connection),
                        getResources().getString(R.string.not_connection), Boolean.valueOf(false));
            }
        }
    }
}
