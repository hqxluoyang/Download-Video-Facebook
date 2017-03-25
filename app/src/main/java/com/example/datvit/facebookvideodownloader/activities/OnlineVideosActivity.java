package com.example.datvit.facebookvideodownloader.activities;

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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.adapters.MyVideoAdapter;
import com.example.datvit.facebookvideodownloader.models.ItemDialog;
import com.example.datvit.facebookvideodownloader.models.MyVideo;
import com.example.datvit.facebookvideodownloader.utils.ConnectionDetector;
import com.example.datvit.facebookvideodownloader.utils.FacebookSession;
import com.example.datvit.facebookvideodownloader.utils.ToolsHelper;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class OnlineVideosActivity extends Activity {
    private TextView textView;
    private TextView title;
    private ListView listView;
    private ArrayList<MyVideo> videoArrayList;
    private MyVideoAdapter myVideoAdapter;
    private String cookie;
    private Button buttonLoadMore;
    private AsyncHttpClient client;
    private ImageView btnBack;
    private AVLoadingIndicatorView loadingIndicatorView;
    private int counter;
    private int count;
    private String url_id, name;
    private String token;
    private String nameUser;

    private String videourl;
    private String videoname;
    private String videoid;
    private String more_video;

    private int currentFirstVisibleItem;
    private int currentScrollState;
    private int currentVisibleItemCount;
    private boolean flag_loading;
    private boolean flag_loadingagain;
    private long lastDownload;


    public static final String TAG = "VolleyPatterns";

    private ListAdapter ladapter;
    private RelativeLayout rLayout;
    public boolean isShare;
    private ToolsHelper toolsHelper;
    public boolean isCheck;
    private final ItemDialog[] items;
    private DownloadManager mgr;
    private BroadcastReceiver onComplete;
    private BroadcastReceiver onNotificationClick;
    private ConnectionDetector cd;
    private RequestQueue mRequestQueue;
    private FacebookSession facebookSession;

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

    public OnlineVideosActivity() {
        this.isCheck = true;
        buttonLoadMore = null;
        counter = 0;
        isShare = false;
        isCheck = true;
        count = 3;

        onComplete = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {
            }
        };

        onNotificationClick = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {
            }
        };

        items = new ItemDialog[]{new ItemDialog(Integer.valueOf(R.string.download),
                Integer.valueOf(R.drawable.ico_download)),
                new ItemDialog(Integer.valueOf(R.string.downloadhq),
                        Integer.valueOf(R.drawable.ico_download_hd)),
                new ItemDialog(Integer.valueOf(R.string.play),
                        Integer.valueOf(R.drawable.ico_play_1)),
                new ItemDialog(Integer.valueOf(R.string.share),
                        Integer.valueOf(R.drawable.ico_share)),
                new ItemDialog(Integer.valueOf(R.string.cancel),
                        Integer.valueOf(R.drawable.ico_delete))};
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        setContentView(R.layout.fragment_video);
        videoArrayList = new ArrayList<>();


        facebookSession = new FacebookSession(this);
        if (facebookSession.getToken() != null) {
            token = facebookSession.getToken();
        }
        if (facebookSession.getUsername() != null) {
            nameUser = facebookSession.getUsername();
        }
        this.cd = new ConnectionDetector(this);
        this.toolsHelper = new ToolsHelper();
        this.mgr = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        try {
            this.ladapter = new SelectionVideo(this, R.layout.dialog_view, R.id.text1, this.items);
        } catch (Exception e) {
            e.printStackTrace();
        }

        registerReceiver(this.onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(this.onNotificationClick, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
        listView = (ListView) findViewById(R.id.listVideo);
        textView = (TextView) findViewById(R.id.tvNoVideo);
        title = (TextView) findViewById(R.id.namePage);
        btnBack = (ImageView) findViewById(R.id.btnBack);
        loadingIndicatorView = (AVLoadingIndicatorView) findViewById(R.id.fbProgress);
        listView.setFastScrollEnabled(true);

        rLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.footer_view_list, null);
        this.listView.addFooterView(this.rLayout);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                videoname = videoArrayList.get(i).name;
                videoid = videoArrayList.get(i).id;
                videourl = videoArrayList.get(i).source;
                showMyDialog();
            }
        });

        this.listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                currentScrollState = scrollState;
                if (currentVisibleItemCount > 0 && currentScrollState == 0
                        && rLayout.getVisibility() != View.VISIBLE) {
                    listView.addFooterView(rLayout);
                }
            }

            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem + visibleItemCount == totalItemCount && totalItemCount != 0 && !flag_loading) {
                    flag_loading = true;
//                    reload(more_video);
                }
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Intent intent = getIntent();
        if (intent != null) {
            url_id = intent.getStringExtra("link");
            name = intent.getStringExtra("name");
            getData(url_id);
            title.setText(name);
        }
    }


    private void getData(String url) {
        textView.setVisibility(View.INVISIBLE);
        loadingIndicatorView.smoothToShow();
        excuteUrl(url);
    }

    private void loadData() {
        loadingIndicatorView.smoothToHide();
        listView.removeFooterView(rLayout);
        if (videoArrayList.size() > 0) {
            myVideoAdapter = new MyVideoAdapter(this, videoArrayList);
            listView.setAdapter(myVideoAdapter);
        } else {
            textView.setVisibility(View.VISIBLE);
        }
    }


    private void excuteUrl(String str) {
        Log.d("SEARCH_ACTIVITY", "Url : " + str);
        this.client = new AsyncHttpClient();
        this.cookie = CookieManager.getInstance().getCookie(str);
        this.client.addHeader("user-agent", "Mozilla/5.0 (Linux; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0");
        this.client.addHeader("Cookie", this.cookie);
        this.client.get(str, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                counter++;
                Element element = null, element1 = null, element2 = null;
                String attr;
                Document document = Jsoup.parse(new String(responseBody));
                Log.e("SEARCH_ACTIVITY", "Document : " + document.outerHtml());
                Elements select_h3 = document.select("div div div h3");
                Elements select_a = document.select("a[href*=/video_redirect/]");
                Elements select_abbr = document.select("div abbr");

                int i = 0;

                while (i < select_a.size()) {
                    String attr2, time = "", name = "";
                    try {
                        element = select_a.get(i);
                        if (i < select_h3.size()) {
                            element1 = select_h3.get(i);
                        }
                        if (i < select_abbr.size()) {
                            element2 = select_abbr.get(i);
                        }
                        Log.e("SEARCH_ACTIVITY", "Element " + i + ": " + element.outerHtml());

                        MyVideo myVideo = new MyVideo();

                        if (element != null) {
                            attr2 = element.attr("href");
                            attr = element.select("img").attr("src");

                            if (attr2 != null && attr2.contains(".mp4")) {
                                String decode = URLDecoder.decode(attr2.substring(attr2.indexOf("src=") + 4), "UTF-8");
                                myVideo.source = decode.substring(0, decode.indexOf("&source=misc"));
                                myVideo.picture = attr;

                                if (element1 != null) {
                                    name = element1.select("a").last().text();
                                }

                                if (element2 != null) {
                                    time = element2.text();
                                }

                                myVideo.name = name;
                                myVideo.created_time = time;

                                Log.e("SEARCH_ACTIVITY", "HREF : " + attr2 + " - " + attr + " - " + name + " - " + time);

                                videoArrayList.add(myVideo);
                            }
                        }
                        i++;
                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadData();
                    }
                });

                Iterator it2 = document.getElementsByTag("a").iterator();
                while (it2.hasNext()) {
                    element = (Element) it2.next();
                    attr = element.text();
                    Log.e("SEARCH_ACTIVITY", "More : " + attr);
                    if (attr != null && (attr.equals("Show More") || attr.equals("Xem thêm"))) {
                        more_video = "https://m.facebook.com" + element.attr("href");
                        return;
                    } else {
                        flag_loadingagain = true;
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    private void reload(String str) {
        loadingIndicatorView.setVisibility(View.INVISIBLE);
        if (flag_loadingagain) {
            listView.removeFooterView(this.rLayout);
        } else {
            Log.d("SEARCH_ACTIVITY", "Url : " + str);
            this.client = new AsyncHttpClient();
            this.cookie = CookieManager.getInstance().getCookie(str);
            this.client.addHeader("user-agent", "Mozilla/5.0 (Linux; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0");
            this.client.addHeader("Cookie", this.cookie);
            this.client.get(str, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    counter++;
                    Element element = null, element1 = null, element2 = null;
                    String attr;
                    Document document = Jsoup.parse(new String(responseBody));
                    Log.e("SEARCH_ACTIVITY", "Document : " + document.outerHtml());
                    Elements select_h3 = document.select("div div div h3");
                    Elements select_a = document.select("a[href*=/video_redirect/]");
                    Elements select_abbr = document.select("div abbr");

                    int i = 0;

                    while (i < select_a.size()) {
                        String attr2, time = "", name = "";
                        try {
                            element = select_a.get(i);
                            if (i < select_h3.size()) {
                                element1 = select_h3.get(i);
                            }
                            if (i < select_abbr.size()) {
                                element2 = select_abbr.get(i);
                            }
                            Log.e("SEARCH_ACTIVITY", "Element " + i + ": " + element.outerHtml());

                            MyVideo myVideo = new MyVideo();

                            if (element != null) {
                                attr2 = element.attr("href");
                                attr = element.select("img").attr("src");

                                if (attr2 != null && attr2.contains(".mp4")) {
                                    String decode = URLDecoder.decode(attr2.substring(attr2.indexOf("src=") + 4), "UTF-8");
                                    myVideo.source = decode.substring(0, decode.indexOf("&source=misc"));
                                    myVideo.picture = attr;

                                    if (element1 != null) {
                                        name = element1.select("a").last().text();
                                    }

                                    if (element2 != null) {
                                        time = element2.text();
                                    }

                                    myVideo.name = name;
                                    myVideo.created_time = time;

                                    Log.e("SEARCH_ACTIVITY", "HREF : " + attr2 + " - " + attr + " - " + name + " - " + time);

                                    videoArrayList.add(myVideo);
                                }
                            }
                            i++;
                        } catch (Exception e) {
                            e.printStackTrace();

                        }
                    }

                    Iterator it2 = document.getElementsByTag("a").iterator();
                    while (it2.hasNext()) {
                        element = (Element) it2.next();
                        attr = element.text();
                        Log.e("SEARCH_ACTIVITY", "More : " + attr);
                        if (attr != null && (attr.equals("Show More") || attr.equals("Xem thêm"))) {
                            more_video = "https://m.facebook.com" + element.attr("href");
                        } else {
                            more_video = null;
                            flag_loadingagain = true;
                        }
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (videoArrayList.size() > 0) {
                                textView.setVisibility(View.INVISIBLE);
                            } else {
                                textView.setVisibility(View.VISIBLE);
                            }

                            if (myVideoAdapter != null && videoArrayList.size() > 0) {
                                myVideoAdapter.notifyDataSetChanged();
                            } else {
                                myVideoAdapter = new MyVideoAdapter(OnlineVideosActivity.this, videoArrayList);
                                listView.setAdapter(myVideoAdapter);
                            }
                            listView.setSelectionFromTop(listView.getFirstVisiblePosition() + 1, 0);

                            flag_loading = false;
                            return;
                        }
                    });
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.onComplete);
        unregisterReceiver(this.onNotificationClick);
    }

    public RequestQueue getRequestQueue() {
        if (this.mRequestQueue == null) {
            this.mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        return this.mRequestQueue;
    }

    public <T> void addToRequestQueue(com.android.volley.Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }


    public void showMyDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(R.mipmap.ic_launcher);
            builder.setTitle(this.videoname).setAdapter(this.ladapter, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int position) {
                    if (position == 0) {
                        dialog.cancel();
                        downloadVideo();
                    } else if (position == 1) {
                        dialog.cancel();
                        downloadVideo();
                    } else if (position == 2) {
                        dialog.cancel();
                        if (videourl.contains(".mp4")) {
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.parse(videourl), "video/*");
                            List<ResolveInfo> intents = getPackageManager().queryIntentActivities(intent, 0);
                            if (intents != null && intents.size() > 0) {
                                startActivity(Intent.createChooser(intent, getResources().getString(R.string.choose_player)));
                            }
                        }
                    } else if (position == 3) {
                        dialog.cancel();
                        ToolsHelper.shareLinkVideo(OnlineVideosActivity.this, getResources().getString(R.string.click_video), videourl);
                    } else if (position == 4) {
                        dialog.cancel();
                    }
                }
            });
            builder.create().show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void downloadVideo() {
        if (this.cd.isConnectingToInternet()) {
            String url = this.videourl;
            if (url.contains("https")) {
                url = url.replace("https", "http");
            }
            Boolean isSDPresent = Boolean.valueOf(Environment.getExternalStorageState().equals("mounted"));
            if (isSDPresent.booleanValue()) {
                if (url.contains(".mp4")) {
                    Uri uri = Uri.parse(url.toString());
                    String mainname = getSharedPreferences("pref", 0).getString("path", "GPVideoDownloader").trim();
                    File RootFile = new File(Environment.getExternalStorageDirectory() + File.separator + mainname);
                    if (!RootFile.isDirectory()) {
                        RootFile.mkdirs();
                    }

                    String new_downloadname = "Unknown";
                    if (videoid != null) {
                        new_downloadname = this.videoid + ".mp4";
                    } else {
                        new_downloadname = this.videoname + ".mp4";
                    }
                    String filename = this.videoname.replaceAll("[-+.^:,]", "") + ".mp4";
                    try {
                        if (Build.VERSION.SDK_INT >= 11) {
                            this.lastDownload = this.mgr.enqueue(new DownloadManager.Request(uri).setAllowedNetworkTypes(3)
                                    .setAllowedOverRoaming(false).setTitle(filename).setDescription(getResources().getString(R.string.downloading))
                                    .setNotificationVisibility(1).setDestinationInExternalPublicDir(mainname, new_downloadname));
                        } else {
                            this.lastDownload = this.mgr.enqueue(new DownloadManager.Request(uri).setAllowedNetworkTypes(3)
                                    .setAllowedOverRoaming(false).setTitle(filename).setDescription(getResources().getString(R.string.downloading))
                                    .setDestinationInExternalPublicDir(mainname, new_downloadname));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(filename))));
                    Toast.makeText(OnlineVideosActivity.this, getResources().getString(R.string.video_downloading), Toast.LENGTH_SHORT).show();
                } else {
                    String video_id = this.videoid;
                    String fb_user_video_query = "https://graph.facebook.com/v2.2/" + video_id;
                    fb_user_video_query = fb_user_video_query + "?fields=source";
                    fb_user_video_query = fb_user_video_query + "&access_token=" + token;

                    addToRequestQueue(new JsonObjectRequest(0, fb_user_video_query, null, new Response.Listener<JSONObject>() {
                        public void onResponse(JSONObject response) {
                            if (response != null) {
                                try {
                                    Uri uri = Uri.parse(response.getString("source").toString());
                                    String str = "path";
                                    String str2 = "GPVideoDownloader";
                                    String mainname = OnlineVideosActivity.this.getSharedPreferences("pref", 0).getString(str, str2).trim();
                                    File RootFile = new File(Environment.getExternalStorageDirectory() + File.separator + mainname);
                                    if (!RootFile.isDirectory()) {
                                        RootFile.mkdirs();
                                    }
                                    String new_downloadname = "Unknown";
                                    if (videoid != null) {
                                        new_downloadname = OnlineVideosActivity.this.videoid + ".mp4";
                                    } else {
                                        new_downloadname = OnlineVideosActivity.this.videoid + ".mp4";
                                    }
                                    String filename = OnlineVideosActivity.this.videoname.replaceAll("[-+.^:,]", com.example.datvit.facebookvideodownloader.BuildConfig.VERSION_NAME) + ".mp4";
                                    try {
                                        if (Build.VERSION.SDK_INT >= 11) {
                                            OnlineVideosActivity.this.lastDownload = OnlineVideosActivity.this.mgr.enqueue(new DownloadManager.Request(uri).setAllowedNetworkTypes(3)
                                                    .setAllowedOverRoaming(false).setTitle(filename).setDescription(getResources().getString(R.string.downloading))
                                                    .setNotificationVisibility(1).setDestinationInExternalPublicDir(mainname, new_downloadname));
                                        } else {
                                            OnlineVideosActivity.this.lastDownload = OnlineVideosActivity.this.mgr.enqueue(new DownloadManager.Request(uri).
                                                    setAllowedNetworkTypes(3).setAllowedOverRoaming(false).setTitle(filename)
                                                    .setDescription(getResources().getString(R.string.downloading)).setDestinationInExternalPublicDir(mainname, new_downloadname));
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        String msgnew = e.getMessage();
                                        Toast.makeText(OnlineVideosActivity.this, msgnew, Toast.LENGTH_SHORT).show();
                                    }
                                    File f = new File(filename);
                                    OnlineVideosActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f)));
                                    Toast.makeText(OnlineVideosActivity.this, getResources().getString(R.string.video_downloading), Toast.LENGTH_SHORT).show();
                                } catch (JSONException e2) {
                                    e2.printStackTrace();
                                }
                            }
                        }
                    }, new Response.ErrorListener() {
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(OnlineVideosActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }));
                }
            } else {

                Toast.makeText(this, getResources().getString(R.string.video_downloaded), Toast.LENGTH_SHORT).show();
            }
        } else {
            toolsHelper.showAlertDialog(this, getResources().getString(R.string.network_connection),
                    getResources().getString(R.string.not_connection), Boolean.valueOf(false));
        }
    }

    class SelectionVideo extends ArrayAdapter<ItemDialog> {
        SelectionVideo(Context x0, int x1, int x2, ItemDialog[] x3) {
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
}
