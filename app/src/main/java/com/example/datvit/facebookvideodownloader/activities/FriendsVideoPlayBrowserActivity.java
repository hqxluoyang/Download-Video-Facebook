package com.example.datvit.facebookvideodownloader.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.BuildConfig;
import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.models.ItemDialog;
import com.example.datvit.facebookvideodownloader.utils.ToolsHelper;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FriendsVideoPlayBrowserActivity extends AppCompatActivity {
    private final Activity activity;
    private ConnectivityManager cm;
    private final ItemDialog[] items;
    private ListAdapter ladapter;
    private Context context;
    private long lastDownload;
    private String mainUrl;
    private DownloadManager mgr;
    private BroadcastReceiver onComplete;
    private BroadcastReceiver onNotificationClick;
    private ToolsHelper toolsHelper;

    @SuppressLint({"SetJavaScriptEnabled"})
    private WebView webView;

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

    class MyWebChromeClient extends WebChromeClient {
        MyWebChromeClient() {
        }

        public void onProgressChanged(WebView view, int progress) {
            FriendsVideoPlayBrowserActivity.this.activity.setTitle(getResources().getString(R.string.loading));
            FriendsVideoPlayBrowserActivity.this.activity.setProgress(progress * 100);
            if (progress == 100) {
                FriendsVideoPlayBrowserActivity.this.activity.setTitle(R.string.app_name);
            }
        }
    }

    class WebClientClass extends WebViewClient {
        public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
            if (url.contains("mp4")) {
                try {
                    String decodedUrl = URLDecoder.decode(url, "ASCII");
//                    String mainurl = URLEncoder.encode(url, "utf-8");
                    String subStringUrl = decodedUrl.substring(decodedUrl.indexOf("src="), decodedUrl.indexOf("&source"));
                    String subStringUrl1 = subStringUrl.substring(subStringUrl.indexOf("https:"));
                    showMyDialog(view, subStringUrl1);
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

        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
        }

        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            try {
                URLDecoder.decode(url, "ASCII");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    class MyDialogClass implements DialogInterface.OnClickListener {
        final /* synthetic */ WebView val$view;
        final /* synthetic */ String url;

        MyDialogClass(WebView webView, String url) {
            this.val$view = webView;
            this.url = url;
        }

        public void onClick(DialogInterface dialog, int position) {
            if (position == 0) {
                dialog.cancel();
                if (Build.VERSION.SDK_INT >= 11) {
                    this.val$view.onResume();
                }
                downloadVideo(url);
            } else if (position == 1) {
                dialog.cancel();
                goUri(url);
            } else if (position == 2) {
                dialog.cancel();
                if (Build.VERSION.SDK_INT >= 11) {
                    this.val$view.onResume();
                }
                String plyurl = null;
                if (url.matches("^(https|ftp)://.*$")) {
                    plyurl = "http" + url.substring(5);
                }
                ToolsHelper.shareLinkVideo(FriendsVideoPlayBrowserActivity.this, getResources().getString(R.string.click_video), plyurl);
            } else if (position == 3) {
                dialog.cancel();
                if (Build.VERSION.SDK_INT >= 11) {
                    this.val$view.onResume();
                }
            }
        }
    }

    public FriendsVideoPlayBrowserActivity() {
        this.context = this;
        this.mgr = null;
        this.lastDownload = -1;
        this.activity = this;
        this.ladapter = null;
        this.onNotificationClick = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        };
        this.onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        };

        this.items = new ItemDialog[]{new ItemDialog(Integer.valueOf(R.string.download),
                Integer.valueOf(R.drawable.ico_download)),
                new ItemDialog(Integer.valueOf(R.string.play),
                        Integer.valueOf(R.drawable.ico_play_1)),
                new ItemDialog(Integer.valueOf(R.string.share),
                        Integer.valueOf(R.drawable.ico_share)),
                new ItemDialog(Integer.valueOf(R.string.cancel),
                        Integer.valueOf(R.drawable.ico_delete))};
        toolsHelper = new ToolsHelper();
    }

    @SuppressLint({"NewApi"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        this.mainUrl = getIntent().getStringExtra("Url");
        this.cm = (ConnectivityManager) this.context.getSystemService(CONNECTIVITY_SERVICE);
        setContentView(R.layout.web_viewer);
//        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            this.ladapter = new VideoAdapter(this, R.layout.dialog_view, R.id.text1, this.items);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.mgr = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        registerReceiver(this.onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(this.onNotificationClick, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
        this.webView = (WebView) findViewById(R.id.webViewVideo);
        this.webView.setWebChromeClient(new MyWebChromeClient());
        this.webView.setWebViewClient(new WebClientClass());
        WebSettings ws = this.webView.getSettings();
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
        ws.setRenderPriority(RenderPriority.HIGH);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setPluginState(PluginState.ON);
        ws.setJavaScriptEnabled(true);
        ws.setUserAgentString("Mozilla/5.0 (Linux; U; Android 2.0; en-us; Droid Build/ESD20) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Mobile Safari/530.17");
        ws.setDomStorageEnabled(true);
        ws.setBuiltInZoomControls(true);
        ws.setSupportZoom(true);
        Configuration config = getResources().getConfiguration();
        if (VERSION.SDK_INT > 14) {
            ws.setTextZoom((int) (config.fontScale * 100.0f));
        }
        if (savedInstanceState == null) {
            this.webView.loadUrl("http://www.facebook.com" + this.mainUrl);
            this.webView.setId(getResources().getInteger(R.integer.valurof));
            this.webView.setInitialScale(0);
            this.webView.requestFocus();
            this.webView.requestFocusFromTouch();
            return;
        }
        this.webView.restoreState(savedInstanceState);
        this.webView.setId(getResources().getInteger(R.integer.valurof));
        this.webView.requestFocus();
        this.webView.requestFocusFromTouch();
//        infoDialog();
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

    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        this.webView.saveState(savedInstanceState);
    }

    protected boolean checkConnectivity() {
        NetworkInfo activeNetwork = this.cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            Toast.makeText(FriendsVideoPlayBrowserActivity.this,getResources().getString(R.string.not_connection), Toast.LENGTH_SHORT).show();
        }
        return isConnected;
    }

    protected boolean goUri(String uri) {
        Log.e("FRIEND_VIDEO_PLAY", "Go Uri : " + uri);
        boolean typeVideo = uri.toLowerCase().matches(".*\\.(mp4|m4v)");
        if (!uri.toLowerCase().matches("file:///.*") && !checkConnectivity()) {
            return false;
        }
        if (typeVideo) {
            Uri intentUri = Uri.parse(uri);
            Log.e("FRIEND_VIDEO_PLAY", "Uri of video selected : " + intentUri);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(intentUri, "video/mp4");
            startActivity(intent);
            return true;
        }
        this.webView.loadUrl(uri);
        return true;
    }

    public void onBackPressed() {
        if (this.webView.canGoBack()) {
            this.webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.onComplete);
        unregisterReceiver(this.onNotificationClick);
    }

    @SuppressLint({"NewApi"})
    private void downloadVideo(String url) {
        boolean b = url.matches("^(https|ftp)://.*$");
        Log.e("FRIEND_VIDEO_PLAY", "Is url is https:// - " + b);
        if (b) {
            url = url.substring(5);
            Log.e("FRIEND_VIDEO_PLAY", "https url after substring : " + url);
            url = "http" + url;
            Log.e("FRIEND_VIDEO_PLAY", "https url to http url : " + url);
        } else {
            Log.e("FRIEND_VIDEO_PLAY", "url is already a http// ");
        }
        Boolean isSDPresent = Boolean.valueOf(Environment.getExternalStorageState().equals("mounted"));
        if (isSDPresent.booleanValue()) {
            String Titlefilename;
            String savename;
            Uri uri = Uri.parse(url);
            String name = getSharedPreferences("pref", 0).getString("path", "GPVideoDownloader");
//            String name = toolsHelper.file.getPath();
            Log.e("FRIEND_VIDEO_PLAY", "this is folder path :" + name);
            String mainname = name.trim();
            File RootFile = new File(Environment.getExternalStorageDirectory() + File.separator + mainname);
            Log.e("FRIEND_VIDEO_PLAY", "this is for folder creation of facebook page :" + RootFile);
            if (RootFile.isDirectory()) {
                Log.e("FRIEND_VIDEO_PLAY", "Directory GPVideoDownloader is already Created : ");
            } else {
                RootFile.mkdirs();
            }
            Timestamp timestamp = new Timestamp((long) ((int) System.currentTimeMillis()));
            String todaytime = toolsHelper.getCurrentTimeStamp();
            if (todaytime != null) {
                Titlefilename = todaytime;
                savename = todaytime;
            } else {
                Titlefilename = timestamp.toString();
                savename = timestamp.toString();
            }
            String new_downloadname = Titlefilename + ".mp4";
            Log.e("FRIEND_VIDEO_PLAY", "this is name of downloading picture name :" + new_downloadname);
            String filename = savename.replaceAll("[-+.^:,]", BuildConfig.VERSION_NAME) + ".mp4";
            Log.e("FRIEND_VIDEO_PLAY", "this is for file name checking :" + filename);
            try {
                if (VERSION.SDK_INT >= 11) {
                    this.lastDownload = this.mgr.enqueue(new Request(uri).setAllowedNetworkTypes(3)
                            .setAllowedOverRoaming(false).setTitle(Titlefilename)
                            .setDescription(getResources().getString(R.string.downloading)).setNotificationVisibility(1)
                            .setDestinationInExternalPublicDir(mainname, filename));
                } else {
                    this.lastDownload = this.mgr.enqueue(new Request(uri).setAllowedNetworkTypes(3)
                            .setAllowedOverRoaming(false).setTitle(Titlefilename)
                            .setDescription(getResources().getString(R.string.downloading))
                            .setDestinationInExternalPublicDir(mainname, filename));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(filename))));
        }
        Toast.makeText(this, getResources().getString(R.string.video_downloading), Toast.LENGTH_SHORT).show();
    }


    public void showMyDialog(WebView view, String url) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(R.mipmap.ic_launcher);
            builder.setTitle(getResources().getString(R.string.choose_option)).setAdapter(this.ladapter, new MyDialogClass(view, url));
            builder.create().show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
