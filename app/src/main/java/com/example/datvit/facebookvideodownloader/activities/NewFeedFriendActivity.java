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
import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.adapters.MyVideoAdapter;
import com.example.datvit.facebookvideodownloader.models.ItemDialog;
import com.example.datvit.facebookvideodownloader.models.MyVideo;
import com.example.datvit.facebookvideodownloader.utils.ConnectionDetector;
import com.example.datvit.facebookvideodownloader.utils.FacebookSession;
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
 * Created by DatVIT on 11/15/2016.
 */

public class NewFeedFriendActivity extends AppCompatActivity {

    public static final String TAG = "VolleyPatterns";
    private static final String TAG_DATA = "data";
    private static final String TAG_DATA_Next = "paging";

    private String token;
    private String urlJsonArry;
    private String fb_user_group_url;
    private String videourl;
    private String videourlmain;
    private String videoname;
    private String videouid;
    private String videoid;
    private String video_link;
    private String nextpage_url;

    private int currentFirstVisibleItem;
    private int currentScrollState;
    private int currentVisibleItemCount;
    private boolean flag_loading;
    private boolean flag_loadingagain;
    private boolean isCheck = false;
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

    private ConnectionDetector cd;
    private RequestQueue mRequestQueue;
    private ToolsHelper toolsHelper;
    private DownloadManager mgr;
    private BroadcastReceiver onComplete;
    private BroadcastReceiver onNotificationClick;
    private FacebookSession facebookSession;

    private String friend_id;
    private String username;
    private String subtitle;
    private ImageView btnBack;
    private TextView namePage;

    private Locale myLocale;
    private List<String> checkList;

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

    public NewFeedFriendActivity() {
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
        setContentView(R.layout.fragment_video_friends);
        facebookSession = new FacebookSession(this);
        checkList = new ArrayList<>();

        if (facebookSession.getToken() != null) {
            token = facebookSession.getToken();
        }

        if (token != null) {
            Log.e("TIMELINE_TOKEN", token);
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

        getIntentData();

        setUpData();
    }

    private void getIntentData() {
        this.friend_id = getIntent().getStringExtra("id");
        this.username = getIntent().getStringExtra("name");
        this.subtitle = getIntent().getStringExtra("title");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.onComplete);
        unregisterReceiver(this.onNotificationClick);
    }


    public void setUpData() {
        listView = (ListView) findViewById(R.id.listVideoWatched);
        tvNoVideo = (TextView) findViewById(R.id.tvNoMovie);
        tvNoVideo.setText(getResources().getString(R.string.no_video_found));
        fbProgress = (AVLoadingIndicatorView) findViewById(R.id.fbProgress);
        fbProgress.setVisibility(View.VISIBLE);
        tvNoVideo.setVisibility(View.INVISIBLE);
        this.namePage = (TextView) findViewById(R.id.namePage);
        this.btnBack = (ImageView) findViewById(R.id.btnBack);
        namePage.setText(username + " - " + subtitle);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        rLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.footer_view_list, null);
        loderprogress = (AVLoadingIndicatorView) rLayout.findViewById(R.id.loadingprogress);
        this.listView.addFooterView(this.rLayout);

        if (token != null) {
            loadData();
        } else {
            Toast.makeText(NewFeedFriendActivity.this, getResources().getString(R.string.please_login_fb), Toast.LENGTH_SHORT).show();
        }

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
                    if (token != null) {
                        reloadData();

                    } else {
                        Toast.makeText(NewFeedFriendActivity.this, getResources().getString(R.string.please_login_fb), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i < movieArrayList.size()) {
                    video_link = movieArrayList.get(i).link;
                    videoname = movieArrayList.get(i).name;
                    videoid = movieArrayList.get(i).id;
                    videourlmain = movieArrayList.get(i).source;
                    videouid = toolsHelper.getVideoid(video_link);
                    showMyDialog();
                }
            }
        });
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
            fb_user_group_url = "https://graph.facebook.com/v2.2/" + friend_id + "/home" +
                    "?fields=id,description,source,created_time,picture,link,story,properties&limit=20&access_token=";
            fb_user_group_url += this.token;
            addToRequestQueue(new JsonObjectRequest(0, fb_user_group_url, null, new Response.Listener<JSONObject>() {
                public void onResponse(JSONObject response) {
                    if (response != null) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("data");
                            Log.e("NEW_FEED_JSON_ARRAY", jsonArray.toString());
                            if (jsonArray.length() == 0) {
                                tvNoVideo.setVisibility(View.VISIBLE);
                                rLayout.setVisibility(View.INVISIBLE);
                                fbProgress.smoothToHide();
                                return;
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                Log.e("NEW_FEED_JSON_OBJECT", jsonObject.toString());
                                if (jsonObject.has("source") && jsonObject.getString("source").contains(".mp4")) {
                                    String pic = jsonObject.getString("picture");
                                    if (!checkList.contains(pic)) {
                                        MyVideo myVideo = new MyVideo();
                                        myVideo.id = jsonObject.getString("id");
                                        myVideo.created_time = toolsHelper.getFormattedTime(jsonObject.getString("created_time"));
                                        myVideo.picture = jsonObject.getString("picture");

                                        if (jsonObject.has("link")) {
                                            myVideo.link = jsonObject.getString("link");
                                        }
                                        if (jsonObject.has("story")) {
                                            myVideo.name = jsonObject.getString("story");
                                        } else {
                                            myVideo.name = "Video untitled";
                                        }

                                        if (jsonObject.has("source")) {
                                            myVideo.source = jsonObject.getString("source");
                                        }
                                        movieArrayList.add(myVideo);
                                    }
                                }
                            }

                            JSONObject pagenextobject = response.getJSONObject("paging");
                            if (pagenextobject.has("next")) {
                                nextpage_url = pagenextobject.getString("next");
                            } else {
                                nextpage_url = null;
                                flag_loadingagain = true;
                            }

                            if (movieArrayList.size() > 0) {
                                myVideoAdapter = new MyVideoAdapter(NewFeedFriendActivity.this, movieArrayList);
                                listView.setAdapter(myVideoAdapter);
                                listView.setSelectionFromTop(listView.getFirstVisiblePosition(), 0);
                                fbProgress.smoothToHide();
                                return;
                            }
                            reloadData();
                        } catch (JSONException e) {
                            fbProgress.smoothToHide();
                            e.printStackTrace();
                        }
                    } else {
                        tvNoVideo.setVisibility(View.VISIBLE);
                    }
                    fbProgress.smoothToHide();
                }
            }, new Response.ErrorListener() {
                public void onErrorResponse(VolleyError error) {
//                    Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
                    tvNoVideo.setVisibility(View.VISIBLE);
                    fbProgress.smoothToHide();
                }
            }));
        } else {
            toolsHelper.showAlertDialog(NewFeedFriendActivity.this, getResources().getString(R.string.network_connection),
                    getResources().getString(R.string.not_connection), Boolean.valueOf(false));
        }
    }

    private void reloadData() {

        if (!this.cd.isConnectingToInternet()) {
            toolsHelper.showAlertDialog(NewFeedFriendActivity.this, getResources().getString(R.string.network_connection),
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
                            Log.e("NEW_FEED_JSON_ARRAY", jsonArray.toString());
                            if (jsonArray.length() == 0) {
                                Toast.makeText(NewFeedFriendActivity.this, getResources().getString(R.string.no_more_video), Toast.LENGTH_SHORT).show();
                                rLayout.setVisibility(View.INVISIBLE);
                                fbProgress.smoothToHide();
                                return;
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                Log.e("NEW_FEED_JSON_OBJECT", jsonObject.toString());
                                if (jsonObject.has("source") && jsonObject.getString("source").contains(".mp4")) {
                                    String pic = jsonObject.getString("picture");
                                    if (checkList.contains(pic)) {
                                        continue;
                                    } else {
                                        MyVideo myVideo = new MyVideo();
                                        myVideo.id = jsonObject.getString("id");
                                        myVideo.created_time = toolsHelper.getFormattedTime(jsonObject.getString("created_time"));

                                        if (jsonObject.has("link")) {
                                            myVideo.link = jsonObject.getString("link");
                                        }
                                        if (jsonObject.has("story")) {
                                            myVideo.name = jsonObject.getString("story");
                                        } else {
                                            myVideo.name = "Video untitled";
                                        }
                                        if (jsonObject.has("picture")) {
                                            myVideo.picture = jsonObject.getString("picture");
                                        }
                                        if (jsonObject.has("source")) {
                                            myVideo.source = jsonObject.getString("source");
                                        }
                                        movieArrayList.add(myVideo);
                                    }
                                } else {
                                    continue;
                                }
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
                                myVideoAdapter = new MyVideoAdapter(NewFeedFriendActivity.this, movieArrayList);
                                listView.setAdapter(myVideoAdapter);
                            }
                            listView.setSelectionFromTop(listView.getFirstVisiblePosition() + 1, 0);

                            flag_loading = false;
                            fbProgress.smoothToHide();
                            return;
                        } catch (JSONException e) {
                            e.printStackTrace();
                            fbProgress.smoothToHide();
                        }
                    }
                    fbProgress.smoothToHide();
                    Toast.makeText(NewFeedFriendActivity.this, getResources().getString(R.string.no_more_video), Toast.LENGTH_SHORT).show();
                }
            }, new Response.ErrorListener() {
                public void onErrorResponse(VolleyError error) {
                    fbProgress.smoothToHide();
                    Toast.makeText(NewFeedFriendActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }));
        }
    }


    private void getHdLink(String video_id) {
        fbProgress.setVisibility(View.INVISIBLE);
        urlJsonArry = "https://graph.facebook.com/v2.2/" + video_id + "?access_token=";
        this.urlJsonArry += this.token;
        this.urlJsonArry += "&fields=source";

        addToRequestQueue(new JsonObjectRequest(0, this.urlJsonArry, null, new Response.Listener<JSONObject>() {
            public void onResponse(JSONObject response) {
                try {
                    if (response.has("source")) {
                        NewFeedFriendActivity.this.videourl = response.getString("source");
                    } else {
                        NewFeedFriendActivity.this.videourl = NewFeedFriendActivity.this.videourlmain;
                    }
                    downloadVideoHd();
                } catch (Exception e) {
                }
            }
        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                fbProgress.setVisibility(View.INVISIBLE);
                Toast.makeText(NewFeedFriendActivity.this, getResources().getString(R.string.not_found_link_hd), Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void downloadVideoHd() {
        fbProgress.setVisibility(View.INVISIBLE);
        if (cd.isConnectingToInternet()) {
            String url = this.videourl;
            if (url.contains("youtube") || url.contains("vimeo")) {
//                appManagedDownload.main(new String[]{url, toolsHelper.file.getAbsolutePath()});
                Toast.makeText(NewFeedFriendActivity.this, getResources().getString(R.string.no_support_download), Toast.LENGTH_SHORT).show();
            } else if (url.contains("mp4")) {
                if (url.contains("https")) {
                    url = url.replace("https", "http");
                }
                String filename = this.videoname.replaceAll("[-+.^:,]", "") + ".mp4";
                String new_downloadname = this.videoid + ".mp4";
                String mainname = getSharedPreferences("pref", 0).getString("path", "GPVideoDownloader").trim();
                Uri uri = Uri.parse(url.toString());
                try {
                    if (Build.VERSION.SDK_INT >= 11) {
                        this.lastDownload = this.mgr.enqueue(new DownloadManager.Request(uri).setAllowedNetworkTypes(3).
                                setAllowedOverRoaming(false).setTitle("")
                                .setDescription(getResources().getString(R.string.downloading)).setNotificationVisibility(1)
                                .setDestinationInExternalPublicDir(mainname, new_downloadname));
                    } else {
                        this.lastDownload = this.mgr.enqueue(new DownloadManager.Request(uri)
                                .setAllowedNetworkTypes(3).setAllowedOverRoaming(false)
                                .setTitle(this.videoname).setDescription(getResources().getString(R.string.downloading))
                                .setDestinationInExternalPublicDir(mainname, new_downloadname));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(filename))));
                Toast.makeText(NewFeedFriendActivity.this, getResources().getString(R.string.video_downloading), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(NewFeedFriendActivity.this, getResources().getString(R.string.not_download_video), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void showMyDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(NewFeedFriendActivity.this);
            builder.setIcon(R.mipmap.ic_launcher);
            builder.setTitle(this.videoname).setAdapter(this.ladapter, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int position) {
                    if (position == 0) {
                        dialog.cancel();
                        videourl = videourlmain;
                        downloadVideoHd();
                    } else if (position == 1) {
                        dialog.cancel();
                        getHdLink(videouid);
                    } else if (position == 2) {
                        dialog.cancel();
                        if (videourlmain.contains("youtube") || videourlmain.contains("vimeo")) {
//                            Toast.makeText(getActivity(), "No support play link video youtube and vimeo", Toast.LENGTH_SHORT).show();
                            Intent openVideo = new Intent(NewFeedFriendActivity.this, PlayVideoYoutubeActivity.class);
                            openVideo.putExtra("url_video", video_link);
                            startActivity(openVideo);
                        } else if (videourlmain.contains(".mp4")) {
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.parse(videourlmain), "video/*");
                            List<ResolveInfo> intents = NewFeedFriendActivity.this.getPackageManager().queryIntentActivities(intent, 0);
                            if (intents != null && intents.size() > 0) {
                                startActivity(Intent.createChooser(intent, getResources().getString(R.string.choose_player)));
                            }
                        } else {
                            Toast.makeText(NewFeedFriendActivity.this, getResources().getString(R.string.not_play_video), Toast.LENGTH_SHORT).show();
                        }
                    } else if (position == 3) {
                        dialog.cancel();
                        ToolsHelper.shareLinkVideo(NewFeedFriendActivity.this, getResources().getString(R.string.click_video), videourlmain);
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
