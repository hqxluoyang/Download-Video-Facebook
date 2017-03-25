package com.example.datvit.facebookvideodownloader.fragments;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.datvit.facebookvideodownloader.BuildConfig;
import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.activities.MainActivity;
import com.example.datvit.facebookvideodownloader.activities.StartActivity;
import com.example.datvit.facebookvideodownloader.models.ItemDialog;
import com.example.datvit.facebookvideodownloader.utils.ConnectionDetector;
import com.example.datvit.facebookvideodownloader.utils.FacebookSession;
import com.example.datvit.facebookvideodownloader.utils.ToolsHelper;
import com.facebook.AccessToken;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.protocol.HTTP;

import static com.example.datvit.facebookvideodownloader.utils.ConnectionDetector.isNetworkAvailable;
import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by DatVIT on 11/12/2016.
 */

public class BrowserFragment extends Fragment {

    private ConnectivityManager cm;
    private final ItemDialog[] items;
    private ListAdapter ladapter;
    private long lastDownload;
    @SuppressLint({"SetJavaScriptEnabled"})
    private WebView mWebView;
    private DownloadManager mgr;
    private BroadcastReceiver onComplete;
    private BroadcastReceiver onNotificationClick;
    private String subStringUrl1;
    private AVLoadingIndicatorView bar;
    private ToolsHelper toolsHelper;
    private FacebookSession facebookSession;
    private ConnectionDetector cd;
    private String user_id;
    private String token;
    private SharedPreferences loginState;
    private SharedPreferences.Editor loginStateEditor;

    private Locale myLocale;

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.browser_fragment, container, false);
    }



    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.mgr = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        getActivity().registerReceiver(this.onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        getActivity().registerReceiver(this.onNotificationClick, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
        this.mWebView = (WebView) view.findViewById(R.id.webBrowser);
        this.bar = (AVLoadingIndicatorView) view.findViewById(R.id.avi);

        if (cd.isNetworkAvailable(getApplicationContext()) && cd.isConnectingToInternet()) {
            setUpClient(savedInstanceState);
        } else {
            toolsHelper.showAlertDialog(getActivity(), getResources().getString(R.string.network_connection),
                    getResources().getString(R.string.not_connection), Boolean.valueOf(false));
        }
    }

    class VideoAdapter extends ArrayAdapter<ItemDialog> {
        VideoAdapter(Context x0, int x1, int x2, ItemDialog[] x3) {
            super(x0, x1, x2, x3);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            TextView tv = (TextView) v.findViewById(R.id.text1);
            ImageView img = (ImageView) v.findViewById(R.id.imgView);
            tv.setText(items[position].text);
            img.setImageResource(items[position].icon);
            return v;
        }
    }

    class MyDialogClass implements DialogInterface.OnClickListener {
        final /* synthetic */ WebView val$view;

        MyDialogClass(WebView webView) {
            this.val$view = webView;
        }

        public void onClick(DialogInterface dialog, int position) {
            if (position == 0) {
                dialog.cancel();
                if (Build.VERSION.SDK_INT >= 11) {
                    this.val$view.onResume();
                }
                downloadVideo(subStringUrl1);
            } else if (position == 1) {
                dialog.cancel();
                if (Build.VERSION.SDK_INT >= 11) {
                    this.val$view.onResume();
                }
                String plyurl = null;
                if (subStringUrl1.matches("^(https|ftp)://.*$")) {
                    plyurl = "http" + subStringUrl1.substring(5);
                }
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(plyurl), "video/*");
                List<ResolveInfo> intents = getActivity().getPackageManager().queryIntentActivities(intent, 0);
                if (intents != null && intents.size() > 0) {
                    startActivity(Intent.createChooser(intent, getResources().getString(R.string.choose_player)));
                }
            } else if (position == 2) {
                dialog.cancel();
                if (Build.VERSION.SDK_INT >= 11) {
                    this.val$view.onResume();
                }
                String plyurl = null;
                if (subStringUrl1.matches("^(https|ftp)://.*$")) {
                    plyurl = "http" + subStringUrl1.substring(5);
                }
                ToolsHelper.shareLinkVideo(getActivity(), getResources().getString(R.string.click_video), plyurl);
            } else if (position == 3) {
                dialog.cancel();
                if (Build.VERSION.SDK_INT >= 11) {
                    this.val$view.onResume();
                }
            }
        }
    }

    public BrowserFragment() {
        this.mgr = null;
        this.lastDownload = -1;
        this.ladapter = null;
        this.items = new ItemDialog[]{new ItemDialog(Integer.valueOf(R.string.download),
                Integer.valueOf(R.drawable.ico_download)),
                new ItemDialog(Integer.valueOf(R.string.play),
                        Integer.valueOf(R.drawable.ico_play_1)),
                new ItemDialog(Integer.valueOf(R.string.share),
                        Integer.valueOf(R.drawable.ico_share)),
                new ItemDialog(Integer.valueOf(R.string.cancel),
                        Integer.valueOf(R.drawable.ico_delete))};
        this.onNotificationClick = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {
            }
        };
        this.onComplete = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {
            }
        };
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        facebookSession = new FacebookSession(getActivity());
        loginState = getActivity().getSharedPreferences("LOGIN_STATE", Context.MODE_PRIVATE);
        loginStateEditor = this.loginState.edit();
        if (facebookSession.getToken() != null) {
            token = facebookSession.getToken();
        }
        if (facebookSession.getUserId() != null) {
            user_id = facebookSession.getUserId();
        }
        if (token != null) {
            Log.e("BROWSER_TOKEN", token);
            Log.e("BROWSER_ID", user_id);
        }

        this.cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        cd = new ConnectionDetector(getActivity());
        try {
            this.ladapter = new VideoAdapter(getActivity(), R.layout.dialog_view, R.id.text1, this.items);
        } catch (Exception e) {
            e.printStackTrace();
        }
        toolsHelper = new ToolsHelper();

    }

    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(this.onComplete);
        getActivity().unregisterReceiver(this.onNotificationClick);
    }

    private void downloadVideo(String url) {
        if (url.matches("^(https|ftp)://.*$")) {
            url = url.substring(5);
            url = "http" + url;
        }
        if (Boolean.valueOf(Environment.getExternalStorageState().equals("mounted")).booleanValue()) {
            String Titlefilename;
            String savename;
            Uri uri = Uri.parse(url);
            String mainname = getActivity().getSharedPreferences("pref", 0).getString("path", "GPVideoDownloader").trim();
            File RootFile = new File(Environment.getExternalStorageDirectory() + File.separator + mainname);
            if (!RootFile.isDirectory()) {
                RootFile.mkdirs();
            }
            Timestamp timestamp = new Timestamp((long) ((int) System.currentTimeMillis()));

            String todaytime = getCurrentTimeStamp();
            if (todaytime != null) {
                Titlefilename = todaytime;
                savename = todaytime;
            } else {
                Titlefilename = timestamp.toString();
                savename = timestamp.toString();
            }
            String filename = savename.replaceAll("[-+.^:,]", BuildConfig.VERSION_NAME) + ".mp4";
            try {
                if (Build.VERSION.SDK_INT >= 11) {
                    this.lastDownload = this.mgr.enqueue(new DownloadManager.Request(uri).setAllowedNetworkTypes(3)
                            .setAllowedOverRoaming(false).setTitle(Titlefilename)
                            .setDescription(getResources().getString(R.string.downloading)).setNotificationVisibility(1)
                            .setDestinationInExternalPublicDir(mainname, filename));
                } else {
                    this.lastDownload = this.mgr.enqueue(new DownloadManager.Request(uri).setAllowedNetworkTypes(3)
                            .setAllowedOverRoaming(false).setTitle(Titlefilename)
                            .setDescription(getResources().getString(R.string.downloading))
                            .setDestinationInExternalPublicDir(mainname, filename));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(filename))));
        }
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.video_downloading), Toast.LENGTH_SHORT).show();
    }

    public static String getCurrentTimeStamp() {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressLint({"SetJavaScriptEnabled"})
    @TargetApi(14)
    private void setUpClient(Bundle savedInstanceState) {
        CookieSyncManager.createInstance(getActivity());
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        this.mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
            }

            public Bitmap getDefaultVideoPoster() {
                return super.getDefaultVideoPoster();
            }
        });
        bar.smoothToShow();

        this.mWebView.setWebViewClient(new WebClientClass());
        if (Build.VERSION.SDK_INT > 19) {
            this.mWebView.setLayerType(2, null);
        } else if (Build.VERSION.SDK_INT >= 11) {
            this.mWebView.setLayerType(1, null);
        }
        WebSettings ws = this.mWebView.getSettings();
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
        ws.setRenderPriority(WebSettings.RenderPriority.HIGH);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setPluginState(WebSettings.PluginState.ON);
        ws.setJavaScriptEnabled(true);
        ws.setUserAgentString("Mozilla/5.0 (Linux; U; Android 2.0; en-us; Droid Build/ESD20) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Mobile Safari/530.17");
        ws.setDomStorageEnabled(true);
        ws.setBuiltInZoomControls(true);
        ws.setSupportZoom(true);
        Configuration config = getResources().getConfiguration();
        if (Build.VERSION.SDK_INT > 14) {
            ws.setTextZoom((int) (config.fontScale * 100.0f));
        }

        if (savedInstanceState == null) {
            this.mWebView.loadUrl("http://www.facebook.com");
            this.mWebView.setId(getResources().getInteger(R.integer.valurof));
            this.mWebView.setInitialScale(0);
            this.mWebView.requestFocus();
            this.mWebView.requestFocusFromTouch();
            return;
        }

        this.mWebView.restoreState(savedInstanceState);
        this.mWebView.setId(getResources().getInteger(R.integer.valurof));
        this.mWebView.requestFocus();
        this.mWebView.requestFocusFromTouch();
    }

    public class WebClientClass extends WebViewClient {
        @SuppressLint({"SetJavaScriptEnabled"})
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.contains("mp4")) {
                try {
                    String decodedUrl = URLDecoder.decode(url, HTTP.ASCII);
//                    String mainurl = URLEncoder.encode(url, "utf-8");
                    String subStringUrl = decodedUrl.substring(decodedUrl.indexOf("src="), decodedUrl.indexOf("&source"));
                    subStringUrl1 = subStringUrl.substring(subStringUrl.indexOf("https:"));
                    showMyDialog(view);
                    return true;
                } catch (UnsupportedEncodingException localUnsupportedEncodingException) {
                    localUnsupportedEncodingException.printStackTrace();
                    return false;
                }
            } else if (Build.VERSION.SDK_INT < 11) {
                return false;
            } else {
                if (super.shouldInterceptRequest(view, url) == null) {
                    return false;
                }
                return true;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            bar.smoothToShow();
        }

        public void onLoadResource(WebView view, String url) {
        }

        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            try {
                URLDecoder.decode(url, "ASCII");
                bar.smoothToHide();
                Log.e("Cookie", "url: " + url + ", cookies: " + CookieManager.getInstance().getCookie(url));
                loginStateEditor.putString("cookie", CookieManager.getInstance().getCookie(url));
                loginStateEditor.putString("url", url);
                loginStateEditor.commit();
                Log.e("Cookie","cookies: " + loginState.getString("url", null));
                if (url.contains(MainActivity.URL_FB)
                        && url.contains(MainActivity.URL_FB_1) ) {
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
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                bar.smoothToHide();
            }
        }

        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        }

        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
        }
    }

    public void showMyDialog(WebView view) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setIcon(R.mipmap.ic_launcher);
            builder.setTitle(getResources().getString(R.string.choose_option))
                    .setAdapter(this.ladapter, new MyDialogClass(view));
            builder.create().show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void backPress() {
        if (this.mWebView != null && this.mWebView.canGoBack()) {
            this.mWebView.goBack();
        } else if (isAdded()) {
            exit();
        }
    }

    private void exit() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getResources().getString(R.string.quit))
                .setMessage(getResources().getString(R.string.are_you_exit_app));

        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                getActivity().finish();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
