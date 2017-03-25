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
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
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

import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.adapters.MyVideoAdapter;
import com.example.datvit.facebookvideodownloader.models.MyVideo;
import com.example.datvit.facebookvideodownloader.utils.ConnectionDetector;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.loopj.android.http.AsyncHttpClient;
import com.wang.avi.AVLoadingIndicatorView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

/**
 * Created by DatVIT on 11/30/2016.
 */
public class FriendUploadedVideoActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
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
    private TextView namePage;
    private TextView norecord;
    private ImageView btnBack;
    private BroadcastReceiver onComplete;
    private BroadcastReceiver onNotificationClick;
    private SharedPreferences pref;
    private RelativeLayout rLayout;
    Document doc;
    private String FRIENDS_URL;

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

    public FriendUploadedVideoActivity() {
        this.mgr = null;
        this.lastDownload = -1;
        this.PAGE_URL = "http://www.facebook.com/";
        this.BASE_URL = "https://m.facebook.com";
        this.FRIENDS_URL = IdentityProviders.FACEBOOK;
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
        this.btnBack = (ImageView) findViewById(R.id.btnBack);
        this.webView = (WebView) findViewById(R.id.webView);
        this.lv.setOnItemClickListener(this);
        rLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.footer_view_list, null);
        this.loderprogress = (AVLoadingIndicatorView) this.rLayout.findViewById(R.id.loadingprogress);
        this.lv.addFooterView(this.rLayout);
        this.norecord = (TextView) findViewById(R.id.tvNoVideo);
        this.namePage = (TextView) findViewById(R.id.namePage);

        namePage.setText(username + " - " + subtitle);

        this.videoArrayList = new ArrayList();

        loderprogress.setVisibility(View.VISIBLE);
        loderprogress.show();

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void getIntentData() {
        this.group_id = getIntent().getStringExtra("id");
        this.username = getIntent().getStringExtra("name");
        this.subtitle = getIntent().getStringExtra("tged");
    }


    public void showAlertDialog(Context context, String title, String message, Boolean status) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setIcon(status.booleanValue() ? R.drawable.success : R.drawable.fail);
        alertDialog.setButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
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
        lv.removeFooterView(rLayout);
        alertDialog.dismiss();
        loderprogress.hide();
        if (videoArrayList.size() > 0) {
            norecord.setVisibility(View.INVISIBLE);
            this.adapter = new MyVideoAdapter(FriendUploadedVideoActivity.this, videoArrayList);
            this.lv.setAdapter(this.adapter);
        } else {
            norecord.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint({"NewApi"})
    private void setUpWebViewProperties(WebView myweb) {
        myweb.setWebChromeClient(new WebChromeClient());
        myweb.setWebViewClient(new MyWebViewClient(myweb));
        WebSettings ws = myweb.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setBuiltInZoomControls(true);
        ws.setSupportZoom(true);
        myweb.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
        Configuration config = getResources().getConfiguration();
        if (Build.VERSION.SDK_INT > 14) {
            ws.setTextZoom((int) (config.fontScale * 100.0f));
        }
        this.client = new AsyncHttpClient();
        String userNameUrl = "http://www.facebook.com/" + this.group_id;

        myweb.loadUrl(userNameUrl);
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
//
        showNewview(position);
    }

    private void showNewview(int position) {
        Intent intent = new Intent(this, FriendsVideoPlayBrowserActivity.class);
        intent.putExtra("Url", videoArrayList.get(position).source);
        startActivity(intent);
    }

    class MyWebViewClient extends WebViewClient {
        final /* synthetic */ WebView val$myweb;

        MyWebViewClient(WebView webView) {
            this.val$myweb = webView;
        }

        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            if (!FriendUploadedVideoActivity.this.gotUserName) {
                if (url.contains("?_rdr")) {
                    FriendUploadedVideoActivity.this.gotUserName = true;
                    String username = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("?"));

                    FriendUploadedVideoActivity.this.FRIENDS_URL += username + "/videos_by";

                    this.val$myweb.getSettings().setUserAgentString("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0");
                    this.val$myweb.loadUrl(FriendUploadedVideoActivity.this.FRIENDS_URL);
                } else if (url.contains("m.facebook.com")) {

                    FriendUploadedVideoActivity.this.FRIENDS_URL += url.substring(url.lastIndexOf("/"), url.length()) + "/videos_by";

                    this.val$myweb.getSettings().setUserAgentString("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0");
                    this.val$myweb.loadUrl(FriendUploadedVideoActivity.this.FRIENDS_URL);
                }
            }
        }

        public void onPageFinished(WebView view, String url) {

            this.val$myweb.loadUrl("javascript:window.HTMLOUT.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
        }
    }


    class MyJavaScriptInterface {
        MyJavaScriptInterface() {
        }

        @JavascriptInterface
        public void processHTML(String html) {
            FriendUploadedVideoActivity.this.doc = Jsoup.parse(html);
            if (!FriendUploadedVideoActivity.this.doc.body().hasText()) {
                Log.d("TAGGED VIDEO", "Html body was null");
            } else if (FriendUploadedVideoActivity.this.videoArrayList.size() <= 0) {
                Log.d("TAGGED VIDEO", "Html body was not null....");
                Log.d("TAGGED VIDEO", "doc : " + FriendUploadedVideoActivity.this.doc);
                Iterator it = FriendUploadedVideoActivity.this.doc.select("a").iterator();
                while (it.hasNext()) {
                    Elements newelements = ((Element) it.next()).select("a[rel*=theater] ");
                    Log.d("TAGGED VIDEO", "this is new newelements :" + newelements);
                    if (newelements.hasAttr("aria-label")) {
                        String href = newelements.attr("ajaxify");
                        Log.d("TAGGED VIDEO", "this is href new main : " + href);
                        Log.d("TAGGED VIDEO", "this is children of new element : " + newelements.first().children());
                        Log.d("TAGGED VIDEO", "this is second childeren : " + newelements.first().child(0).children());
                        Log.d("TAGGED VIDEO", "this is third childeren : " + newelements.first().child(0).child(0).children());
                        Log.d("TAGGED VIDEO", "this is SRC : " + newelements.first().child(0).child(0).children().first().attr("src"));

                        MyVideo myVideo = new MyVideo();
                        myVideo.name = "Video untitled";
                        myVideo.picture = newelements.first().child(0).child(0).children().first().attr("src");
                        myVideo.source = href;

                        videoArrayList.add(myVideo);
                    }
                }
                FriendUploadedVideoActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showData();
                    }
                });
            }
        }
    }

}
