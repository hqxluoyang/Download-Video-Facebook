package com.example.datvit.facebookvideodownloader.activities;

import android.annotation.SuppressLint;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.datvit.facebookvideodownloader.BuildConfig;
import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.adapters.MyVideoAdapter;
import com.example.datvit.facebookvideodownloader.models.MyVideo;
import com.example.datvit.facebookvideodownloader.utils.ConnectionDetector;
import com.loopj.android.http.AsyncHttpClient;
import com.wang.avi.AVLoadingIndicatorView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by DatVIT on 11/15/2016.
 */

public class FriendTimelineVideoActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private boolean loadMoreBoolean;
    private int loadMoreCounter;
    private String BASE_URL;
    private String FINAL_URL;
    private String PAGE_URL;
    private String Titlefilename;
    private MyVideoAdapter adapter;
    private ConnectionDetector cd;
    private int click_position;
    private AsyncHttpClient client;
    private String cookies;
    private boolean forceWebShutDown;
    private boolean gotUserName;
    private long lastDownload;
    private AVLoadingIndicatorView loderprogress;
    private ListView lv;
    private DownloadManager mgr;
    private TextView norecord;
    private ImageView btnBack;
    private TextView namePage;
    private BroadcastReceiver onComplete;
    private BroadcastReceiver onNotificationClick;
    private SharedPreferences pref;
    private RelativeLayout rLayout;

    private String group_id;
    private String savename;
    private String subtitle;
    private String url;
    private String username;

    private AlertDialog.Builder dialogBuilder;
    private AlertDialog alertDialog;
    private TextView nameshow;

    private ArrayList<MyVideo> videoArrayList;

    private int countMore;
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

    public FriendTimelineVideoActivity() {
        this.mgr = null;
        this.lastDownload = -1;
        this.PAGE_URL = "https://m.facebook.com/";
        this.BASE_URL = "https://m.facebook.com";
        this.countMore = 2;
        this.gotUserName = false;
        this.forceWebShutDown = false;
        loadMoreCounter = 0;
        loadMoreBoolean = false;

        this.onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        };

        this.onNotificationClick = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        };
    }

    static {

    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        setContentView(R.layout.fragment_video);

        getIntentData();
        initializeViews();
        friendsCountDialog();
        setUpDownloadManagerConfig();
        setUpWebViewProperties(webView);
    }

    private void setUpDownloadManagerConfig() {
        this.mgr = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        registerReceiver(this.onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(this.onNotificationClick, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
    }

    private void initializeViews() {
        loadMoreBoolean = false;
        this.client = new AsyncHttpClient();
        this.cd = new ConnectionDetector(getApplicationContext());
        this.pref = getSharedPreferences("username", 0);
        this.lv = (ListView) findViewById(R.id.listVideo);

        this.webView = (WebView) findViewById(R.id.webView);
        this.lv.setOnItemClickListener(this);
        rLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.footer_view_list, null);
        this.loderprogress = (AVLoadingIndicatorView) this.rLayout.findViewById(R.id.loadingprogress);
        this.lv.addFooterView(this.rLayout);
        this.norecord = (TextView) findViewById(R.id.tvNoVideo);

        this.namePage = (TextView) findViewById(R.id.namePage);
        this.btnBack = (ImageView) findViewById(R.id.btnBack);
        namePage.setText(username + " - " + subtitle);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        this.videoArrayList = new ArrayList();

        loderprogress.setVisibility(View.VISIBLE);
        loderprogress.show();

        this.rLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loderprogress.setVisibility(View.VISIBLE);
                loadMoreData();
            }
        });

    }

    private void getIntentData() {
        this.group_id = getIntent().getStringExtra("key");
        this.username = getIntent().getStringExtra("name");
        this.subtitle = getIntent().getStringExtra("tged");
    }

    private void loadMoreData() {
        loadMoreBoolean = true;
        this.countMore += 3;
        webView.loadUrl(this.FINAL_URL);
    }

    private void loadUrl() {
        loadMoreBoolean = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(FriendTimelineVideoActivity.this.FINAL_URL);
            }
        });
    }

    public void showAlertDialog(Context context, String title, String message, Boolean status) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setIcon(status.booleanValue() ? R.drawable.success : R.drawable.fail);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        alertDialog.show();
    }

    public void onDestroy() {
        super.onDestroy();
        this.gotUserName = false;
        unregisterReceiver(this.onComplete);
        unregisterReceiver(this.onNotificationClick);
    }

    private void showData() {
        webView.setVisibility(View.INVISIBLE);
        alertDialog.dismiss();
        lv.removeFooterView(rLayout);
        loderprogress.hide();
        if (videoArrayList.size() > 0) {
            norecord.setVisibility(View.INVISIBLE);
            this.adapter = new MyVideoAdapter(FriendTimelineVideoActivity.this, videoArrayList);
            int currentPosition = this.lv.getFirstVisiblePosition();
            this.lv.setAdapter(this.adapter);
            this.lv.setSelectionFromTop(currentPosition, 0);
        } else {
            norecord.setVisibility(View.VISIBLE);
        }
    }

    private static String decodeUrl(String p1) {
        p1 = p1.replace("%3A", ":").replace("%2F", "/").replace("%3F", "?").replace("%3D", "=").replace("%25", "%").replace("%26", "&");
        Log.d("TIMELINE_VIDEO_FRIEND", "local String >>" + p1);
        return p1;
    }

    private void friendsCountDialog() {
        this.dialogBuilder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_count, null);
        this.dialogBuilder.setTitle(getResources().getString(R.string.loading_video));
        this.dialogBuilder.setView(dialogView);
        this.nameshow = (TextView) dialogView.findViewById(R.id.textView1);
        this.nameshow.setText(getResources().getString(R.string.load_video));
        this.alertDialog = this.dialogBuilder.create();
        this.alertDialog.setCancelable(true);
        this.alertDialog.setCanceledOnTouchOutside(false);
        this.alertDialog.show();
    }

    private void executeQuery(WebView web) {
        if (this.group_id.contains("me")) {
            this.gotUserName = true;
            this.PAGE_URL += this.pref.getString("user_name", "me") + "?_rdr#_=_";
            setUpWebViewProperties(web);
            return;
        }
        setUpWebViewProperties(web);
    }

    @SuppressLint({"NewApi"})
    private void setUpWebViewProperties(WebView myweb) {
        myweb.setWebChromeClient(new WebChromeClient());
        myweb.setWebViewClient(new MyWebViewClient(myweb));
        WebSettings ws = myweb.getSettings();
        ws.setJavaScriptEnabled(true);
        myweb.addJavascriptInterface(new MyJavaScriptInterface(), "NEWHTMLOUT");
        ws.setUserAgentString("Mozilla/5.0 (Linux; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0");
        ws.setBuiltInZoomControls(true);
        ws.setSupportZoom(true);
        Configuration config = getResources().getConfiguration();
        if (Build.VERSION.SDK_INT > 14) {
            ws.setTextZoom((int) (config.fontScale * 100.0f));
        }
        String userNameUrl = "http://m.facebook.com/" + this.group_id;
        if (this.group_id.contains("me")) {
            myweb.loadUrl(this.PAGE_URL);
            return;
        }
        myweb.loadUrl(userNameUrl);
        Log.d("TIMELINE_VIDEO_FRIEND", "Page Url : " + userNameUrl);
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        click_position = position;
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.app_name));
        alertDialogBuilder.setIcon(R.mipmap.ic_launcher);
        this.Titlefilename = videoArrayList.get(position).name;
        this.savename = videoArrayList.get(position).id;

        alertDialogBuilder.setMessage(getResources().getString(R.string.title)+" :\n" + this.Titlefilename +
                "\n\n"+ getResources().getString(R.string.save_as)+" :\n" + this.savename + ".mp4")
                .setCancelable(true)
                .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).setNeutralButton(getResources().getString(R.string.download), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                if (cd.isConnectingToInternet()) {
                    url = videoArrayList.get(click_position).source;
                    if (url.matches("^(https|ftp)://.*$")) {
                        url = url.substring(5);
                        url = "http" + url;
                    }

                    if (Boolean.valueOf(Environment.getExternalStorageState().equals("mounted")).booleanValue()) {
                        Uri uri = Uri.parse(url);
                        String mainname = getSharedPreferences("pref", 0).getString("path", "GPVideoDownloader").trim();
                        File RootFile = new File(Environment.getExternalStorageDirectory() + File.separator + mainname);
                        if (!RootFile.isDirectory()) {
                            RootFile.mkdirs();
                        }
                        String filename = FriendTimelineVideoActivity.this.savename.replaceAll("[-+.^:,]", BuildConfig.VERSION_NAME) + ".mp4";
                        try {
                            if (Build.VERSION.SDK_INT >= 11) {
                                lastDownload = FriendTimelineVideoActivity.this.mgr.enqueue(new DownloadManager.Request(uri)
                                        .setAllowedNetworkTypes(3)
                                        .setAllowedOverRoaming(false)
                                        .setTitle(FriendTimelineVideoActivity.this.Titlefilename)
                                        .setDescription(getResources().getString(R.string.downloading))
                                        .setNotificationVisibility(1)
                                        .setDestinationInExternalPublicDir(mainname, filename));
                            } else {
                                lastDownload = FriendTimelineVideoActivity.this.mgr.enqueue(new DownloadManager.Request(uri)
                                        .setAllowedNetworkTypes(3)
                                        .setAllowedOverRoaming(false)
                                        .setTitle(FriendTimelineVideoActivity.this.Titlefilename)
                                        .setDescription(getResources().getString(R.string.downloading))
                                        .setDestinationInExternalPublicDir(mainname, filename));
                            }
                        } catch (Exception e) {
                            Toast.makeText(FriendTimelineVideoActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        File f = new File(filename);
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f)));
                    }
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.video_downloading), Toast.LENGTH_SHORT).show();
                    return;
                }
                showAlertDialog(FriendTimelineVideoActivity.this, getResources().getString(R.string.network_connection),
                        getResources().getString(R.string.not_connection), Boolean.valueOf(false));
            }
        }).setPositiveButton("Play", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                if (FriendTimelineVideoActivity.this.cd.isConnectingToInternet()) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(videoArrayList.get(click_position).source), "video/*");
                    List<ResolveInfo> intents = FriendTimelineVideoActivity.this.getPackageManager().queryIntentActivities(intent, 0);
                    if (intents != null && intents.size() > 0) {
                        startActivity(Intent.createChooser(intent,getResources().getString(R.string.choose_player)));
                        return;
                    }
                    return;
                }
                showAlertDialog(FriendTimelineVideoActivity.this, getResources().getString(R.string.network_connection),
                        getResources().getString(R.string.not_connection), Boolean.valueOf(false));
            }
        });
        alertDialogBuilder.create().show();
    }

    class MyWebViewClient extends WebViewClient {
        final /* synthetic */ WebView val$myweb;

        MyWebViewClient(WebView webView) {
            this.val$myweb = webView;
        }

        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.d("TIMELINE_VIDEO_FRIEND", "TimelineMessage - onPageStarted : " + url);
            if (!gotUserName) {
                if (url.contains("?_rdr") || !url.contains(group_id)) {
                    gotUserName = true;
                    String username = url.substring(url.lastIndexOf("/") + 1, url.length());
                    Log.d("TIMELINE_VIDEO_FRIEND", "Username : " + username);
                    PAGE_URL += username;
                    val$myweb.loadUrl(PAGE_URL);
                }
            }
        }

        public void onPageFinished(WebView view, String url) {
            if (url.contains("https://m.facebook.com/login.php?")) {
//                FriendTimelineVideoActivity.this.alertDialog.dismiss();
//                loderprogress.hide();
                setUpWebViewProperties(webView);
                return;
            }
            Log.d("TIMELINE_VIDEO_FRIEND", "TimelineMessage -  onPageFinished : " + url);
            FriendTimelineVideoActivity.this.cookies = CookieManager.getInstance().getCookie(url);
            Log.d("TIMELINE_VIDEO_FRIEND", "Session Cookie : " + FriendTimelineVideoActivity.this.cookies);
            if (forceWebShutDown) {
                forceWebShutDown = false;
            } else {
                this.val$myweb.loadUrl("javascript:window.NEWHTMLOUT.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
            }
        }
    }


    class MyJavaScriptInterface {
        MyJavaScriptInterface() {
        }

        @JavascriptInterface
        public void processHTML(String html) {
            Document doc = Jsoup.parse(html);
            Log.d("TIMELINE_VIDEO_FRIEND", "Doc : " + doc);
            if (doc.body().hasText()) {
                Log.d("TIMELINE_VIDEO_FRIEND", "Html body was not null....");
                if (forceWebShutDown) {
                    forceWebShutDown = false;
                    return;
                }
                Iterator it = doc.select("a").iterator();
                while (it.hasNext()) {
                    Element e = (Element) it.next();
                    Log.d("TIMELINE_VIDEO_FRIEND", "Element: " + e.toString());
                    if (forceWebShutDown) {
                        forceWebShutDown = false;
                        break;
                    } else if (e.attr("href").contains(".mp4")) {
                        String link_of_video = FriendTimelineVideoActivity.decodeUrl(e.attr("href")).split("src=")[1];
                        String close_to_id = link_of_video.split("id=")[1];
                        String id_of_video = close_to_id.substring(0, close_to_id.indexOf("&"));
                        link_of_video = link_of_video.substring(0, link_of_video.lastIndexOf("&source"));

                        Log.d("TIMELINE_VIDEO_FRIEND", "video ID >>" + id_of_video);
                        Log.d("TIMELINE_VIDEO_FRIEND", "video link >>" + link_of_video);
                        Log.d("TIMELINE_VIDEO_FRIEND", "video IMG >>" + e.select("img").attr("src"));

                        MyVideo myVideo = new MyVideo();
                        myVideo.id = id_of_video;
                        myVideo.name = "Video untitled";
                        myVideo.source = link_of_video;
                        myVideo.picture = e.select("img").attr("src");
                        videoArrayList.add(myVideo);
                    }
                }
                if (forceWebShutDown) {
                    forceWebShutDown = false;
                    return;
                }
                it = doc.select("a").iterator();
                while (it.hasNext()) {
                    Element element = (Element) it.next();
                    if (forceWebShutDown) {
                        forceWebShutDown = false;
                        break;
                    }
                    Log.d("TIMELINE_VIDEO_FRIEND", "Elements Text : " + element.text());
                    if (element.text().equalsIgnoreCase("See More Stories") || element.text().equalsIgnoreCase("Show more")) {
                        String load_more_link = element.attr("href");
                        FINAL_URL = BASE_URL + load_more_link;
                        if (videoArrayList.size() < countMore) {
                            Log.d("TIMELINE_VIDEO_FRIEND", "final url:" + FINAL_URL);
                            if (!loadMoreBoolean) {
                                loadUrl();
                                return;
                            } else if (loadMoreCounter < 5) {
                                loadMoreCounter++;
                                executeQuery(webView);
                                loadUrl();
                                return;
                            } else {
                                loadMoreCounter = 0;
                                loadUrl();
                            }
                        } else {
                            continue;
                        }
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showData();
                    }
                });
                return;
            }
            Log.d("TIMELINE_VIDEO_FRIEND", "Html body was null");
        }
    }

}