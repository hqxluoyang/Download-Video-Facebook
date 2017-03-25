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
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.example.datvit.facebookvideodownloader.BuildConfig;
import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.adapters.MyVideoAdapter;
import com.example.datvit.facebookvideodownloader.models.ItemDialog;
import com.example.datvit.facebookvideodownloader.models.MyVideo;
import com.example.datvit.facebookvideodownloader.utils.ConnectionDetector;
import com.example.datvit.facebookvideodownloader.utils.ToolsHelper;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by DatVIT on 11/27/2016.
 */

public class GroupVideoActivity extends AppCompatActivity {

    public static final String TAG = "VolleyPatterns";
    private static final String TAG_DATA = "data";
    private static final String TAG_DATA_Next = "paging";

    private String group_id;
    private String group_name;
    private String token;
    private String urlJsonArry;
    private String page_video_url;
    private String videourl;
    private String videoname;
    private String videoid;
    private String nextpage_url;

    private int currentFirstVisibleItem;
    private int currentScrollState;
    private int currentVisibleItemCount;
    private boolean flag_loading;
    private boolean flag_loadingagain;
    private long lastDownload;

    private final ItemDialog[] items;

    private ListView listView;
    private ArrayList<MyVideo> movieArrayList;
    private MyVideoAdapter myVideoAdapter;
    private ListAdapter ladapter;
    private RelativeLayout rLayout;
    private AVLoadingIndicatorView loderprogress;
    private AVLoadingIndicatorView fbProgress;
    private TextView tvNoVideo;
    private TextView namePage;
    private ImageView btnBack;

    private ConnectionDetector cd;
    private RequestQueue mRequestQueue;
    private ToolsHelper toolsHelper;
    private DownloadManager mgr;
    private BroadcastReceiver onComplete;
    private BroadcastReceiver onNotificationClick;

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

    public GroupVideoActivity() {
        cd = null;
        lastDownload = -1;
        toolsHelper = null;
        mgr = null;
        flag_loading = false;
        flag_loadingagain = false;
        ladapter = null;

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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        setContentView(R.layout.fragment_video);

        Intent intent = getIntent();
        if (intent != null) {
            group_id = intent.getStringExtra("group_id");
            group_name = intent.getStringExtra("group_name");
            token = intent.getStringExtra("access_token");
        }

        if (token != null) {
            Log.e("GROUP_TOKEN", token);
            Log.e("GROUP_ID", group_id);
        }
        this.cd = new ConnectionDetector(this);
        this.toolsHelper = new ToolsHelper();
        this.mgr = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        try {
            this.ladapter = new SelectionVideo(this, R.layout.dialog_view, R.id.text1, this.items);
        } catch (Exception e) {
            e.printStackTrace();
        }
        movieArrayList = new ArrayList<>();
        registerReceiver(this.onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(this.onNotificationClick, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));

        listView = (ListView) findViewById(R.id.listVideo);
        tvNoVideo = (TextView) findViewById(R.id.tvNoVideo);
        namePage = (TextView) findViewById(R.id.namePage);
        btnBack = (ImageView) findViewById(R.id.btnBack);
        tvNoVideo.setVisibility(View.INVISIBLE);
        fbProgress = (AVLoadingIndicatorView) findViewById(R.id.fbProgress);
        fbProgress.setVisibility(View.VISIBLE);

        rLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.footer_view_list, null);
        loderprogress = (AVLoadingIndicatorView) rLayout.findViewById(R.id.loadingprogress);

        this.listView.addFooterView(this.rLayout);
        loadData();

        namePage.setText(group_name);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
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
                    reloadData();
                }
            }
        });

        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                videoname = movieArrayList.get(i).name;
                videoid = movieArrayList.get(i).id;
                videourl = movieArrayList.get(i).source;
                showMyDialog();
            }
        });
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

    private void loadData() {
        if (cd.isConnectingToInternet()) {
            fbProgress.smoothToShow();
            this.page_video_url = "https://graph.facebook.com/v2.2/";
            this.page_video_url += this.group_id;
            this.page_video_url += "/videos?limit=30&access_token=";
            this.page_video_url += this.token;
            this.page_video_url += "&fields=id,description,source,picture,updated_time";
            addToRequestQueue(new JsonObjectRequest(0, page_video_url, null, new Response.Listener<JSONObject>() {
                public void onResponse(JSONObject response) {
                    if (response != null) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("data");
                            Log.e("PAGE_JSON_ARRAY", jsonArray.toString());
                            if (jsonArray.length() == 0) {
                                tvNoVideo.setVisibility(View.VISIBLE);
                                rLayout.setVisibility(View.INVISIBLE);
                                fbProgress.smoothToHide();
                                return;
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                String pagename;
                                JSONObject pageobject = jsonArray.getJSONObject(i);
                                String pageid = pageobject.getString("id");
                                if (pageobject.has("name")) {
                                    pagename = pageobject.getString("name");
                                } else if (pageobject.has("description")) {
                                    pagename = pageobject.getString("description");
                                } else {
                                    pagename = "Video untitled";
                                }
                                String videopic = pageobject.getString("picture");
                                String source = pageobject.getString("source");
                                String update_time = toolsHelper.getFormattedTime(pageobject.getString("updated_time"));

                                MyVideo myVideo = new MyVideo();
                                myVideo.id = pageid;
                                myVideo.name = pagename;
                                myVideo.source = source;
                                myVideo.created_time = update_time;
                                myVideo.picture = videopic;

                                movieArrayList.add(myVideo);
                            }

                            JSONObject pagenextobject = response.getJSONObject("paging");
                            if (pagenextobject.has("next")) {
                                nextpage_url = pagenextobject.getString("next");
                            } else {
                                nextpage_url = null;
                                flag_loadingagain = true;
                            }

                            if (movieArrayList.size() > 0) {
                                myVideoAdapter = new MyVideoAdapter(GroupVideoActivity.this, movieArrayList);
                                listView.setAdapter(myVideoAdapter);
                                listView.setSelectionFromTop(listView.getFirstVisiblePosition(), 0);
                                fbProgress.smoothToHide();
                                return;
                            }
                            reloadData();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        tvNoVideo.setVisibility(View.VISIBLE);
                    }
                    fbProgress.smoothToHide();
                }
            }, new Response.ErrorListener() {
                public void onErrorResponse(VolleyError error) {
                    fbProgress.smoothToHide();
                    Toast.makeText(GroupVideoActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    tvNoVideo.setVisibility(View.VISIBLE);
                }
            }));
        } else {
            toolsHelper.showAlertDialog(GroupVideoActivity.this, getResources().getString(R.string.network_connection),
                    getResources().getString(R.string.not_connection), Boolean.valueOf(false));
        }
    }

    private void reloadData() {
        if (!this.cd.isConnectingToInternet()) {
            toolsHelper.showAlertDialog(GroupVideoActivity.this, getResources().getString(R.string.network_connection),
                    getResources().getString(R.string.not_connection), Boolean.valueOf(false));
        } else if (flag_loadingagain) {
            listView.removeFooterView(this.rLayout);
        } else {
            fbProgress.smoothToShow();
            addToRequestQueue(new JsonObjectRequest(0, nextpage_url, null, new Response.Listener<JSONObject>() {
                public void onResponse(JSONObject response) {
                    if (response != null) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("data");
                            Log.e("TIMELINE_JSON_ARRAY", jsonArray.toString());
                            if (jsonArray.length() == 0) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_more_video), Toast.LENGTH_SHORT).show();
                                rLayout.setVisibility(View.INVISIBLE);
                                fbProgress.smoothToHide();
                                return;
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                String pagename;
                                JSONObject pageobject = jsonArray.getJSONObject(i);
                                String pageid = pageobject.getString("id");
                                if (pageobject.has("name")) {
                                    pagename = pageobject.getString("name");
                                } else if (pageobject.has("description")) {
                                    pagename = pageobject.getString("description");
                                } else {
                                    pagename = "Video untitled";
                                }
                                String videopic = pageobject.getString("picture");
                                String source = pageobject.getString("source");
                                String update_time = toolsHelper.getFormattedTime(pageobject.getString("updated_time"));

                                MyVideo myVideo = new MyVideo();
                                myVideo.id = pageid;
                                myVideo.name = pagename;
                                myVideo.source = source;
                                myVideo.created_time = update_time;
                                myVideo.picture = videopic;

                                movieArrayList.add(myVideo);
                            }

                            JSONObject pagenextobject = response.getJSONObject("paging");
                            if (pagenextobject.has("next")) {
                                nextpage_url = pagenextobject.getString("next");
                            } else {
                                nextpage_url = null;
                                flag_loadingagain = true;
                            }

                            if (movieArrayList.size() > 0) {
                                tvNoVideo.setVisibility(View.INVISIBLE);
                            } else {
                                tvNoVideo.setVisibility(View.VISIBLE);
                            }

                            if (myVideoAdapter != null && movieArrayList.size() > 0) {
                                myVideoAdapter.notifyDataSetChanged();
                            } else {
                                myVideoAdapter = new MyVideoAdapter(GroupVideoActivity.this, movieArrayList);
                                listView.setAdapter(myVideoAdapter);
                            }
                            listView.setSelectionFromTop(listView.getFirstVisiblePosition() + 1, 0);

                            flag_loading = false;
                            fbProgress.smoothToHide();
                            return;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    fbProgress.smoothToHide();
                    Toast.makeText(GroupVideoActivity.this, getResources().getString(R.string.no_more_video), Toast.LENGTH_SHORT).show();
                }
            }, new Response.ErrorListener() {
                public void onErrorResponse(VolleyError error) {
                    fbProgress.smoothToHide();
                    Toast.makeText(GroupVideoActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();

                }
            }));
        }
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
                        ToolsHelper.shareLinkVideo(GroupVideoActivity.this, getResources().getString(R.string.click_video), videourl);
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
                    String filename = this.videoname.replaceAll("[-+.^:,]", BuildConfig.VERSION_NAME) + ".mp4";
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
                    Toast.makeText(GroupVideoActivity.this, getResources().getString(R.string.video_downloading), Toast.LENGTH_SHORT).show();
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
                                    String mainname = GroupVideoActivity.this.getSharedPreferences("pref", 0).getString(str, str2).trim();
                                    File RootFile = new File(Environment.getExternalStorageDirectory() + File.separator + mainname);
                                    if (!RootFile.isDirectory()) {
                                        RootFile.mkdirs();
                                    }
                                    String new_downloadname = "Unknown";
                                    if (videoid != null) {
                                        new_downloadname = GroupVideoActivity.this.videoid + ".mp4";
                                    } else {
                                        new_downloadname = GroupVideoActivity.this.videoname + ".mp4";
                                    }
                                    String filename = GroupVideoActivity.this.videoname.replaceAll("[-+.^:,]", BuildConfig.VERSION_NAME) + ".mp4";
                                    String sub_path = File.separator + group_name + File.separator + filename;
                                    try {
                                        if (Build.VERSION.SDK_INT >= 11) {
                                            GroupVideoActivity.this.lastDownload = GroupVideoActivity.this.mgr.enqueue(new DownloadManager.Request(uri).setAllowedNetworkTypes(3)
                                                    .setAllowedOverRoaming(false).setTitle(filename).setDescription(getResources().getString(R.string.downloading))
                                                    .setNotificationVisibility(1).setDestinationInExternalPublicDir(mainname, new_downloadname));
                                        } else {
                                            GroupVideoActivity.this.lastDownload = GroupVideoActivity.this.mgr.enqueue(new DownloadManager.Request(uri).
                                                    setAllowedNetworkTypes(3).setAllowedOverRoaming(false).setTitle(filename)
                                                    .setDescription(getResources().getString(R.string.downloading)).setDestinationInExternalPublicDir(mainname, new_downloadname));
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        String msgnew = e.getMessage();
                                        Toast.makeText(GroupVideoActivity.this, msgnew, Toast.LENGTH_SHORT).show();
                                    }
                                    File f = new File(filename);
                                    GroupVideoActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f)));
                                    Toast.makeText(GroupVideoActivity.this, getResources().getString(R.string.video_downloading), Toast.LENGTH_SHORT).show();
                                } catch (JSONException e2) {
                                    e2.printStackTrace();
                                }
                            }
                        }
                    }, new Response.ErrorListener() {
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(GroupVideoActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
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
