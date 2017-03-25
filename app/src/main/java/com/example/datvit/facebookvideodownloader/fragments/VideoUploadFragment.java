package com.example.datvit.facebookvideodownloader.fragments;

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
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.example.datvit.facebookvideodownloader.activities.MainActivity;
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

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by DatVIT on 11/15/2016.
 */

public class VideoUploadFragment extends Fragment {

    public static final String TAG = "VolleyPatterns";
    private static final String TAG_DATA = "data";
    private static final String TAG_DATA_Next = "paging";

    private String user_id;
    private String token;
    private String urlJsonArry;
    private String fb_user_group_url;
    private String videourl;
    private String videourlmain;
    private String videoname;
    private String videouid;
    private String videoid;
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

    public VideoUploadFragment() {
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        this.token = MainActivity.token;
        this.user_id = MainActivity.profile_id;
        if (token != null) {
            Log.e("UPLOAD_TOKEN", token);
            Log.e("UPLOAD_ID", user_id);
        }
        this.cd = new ConnectionDetector(getActivity());
        this.toolsHelper = new ToolsHelper();
        this.mgr = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        try {
            this.ladapter = new SelectionVideo(getActivity(), R.layout.dialog_view, R.id.text1, this.items);
        } catch (Exception e) {
            e.printStackTrace();
        }
        movieArrayList = new ArrayList<>();
        getActivity().registerReceiver(this.onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        getActivity().registerReceiver(this.onNotificationClick, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
    }

    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(this.onComplete);
        getActivity().unregisterReceiver(this.onNotificationClick);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movies_watched, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = (ListView) view.findViewById(R.id.listVideoWatched);
        tvNoVideo = (TextView) view.findViewById(R.id.tvNoMovie);
        tvNoVideo.setText(getResources().getString(R.string.no_video_found));
        fbProgress = (AVLoadingIndicatorView) view.findViewById(R.id.fbProgress);
        tvNoVideo.setVisibility(View.INVISIBLE);

        rLayout = (RelativeLayout) getActivity().getLayoutInflater().inflate(R.layout.footer_view_list, null);
        loderprogress = (AVLoadingIndicatorView) rLayout.findViewById(R.id.loadingprogress);
        fbProgress.setVisibility(View.VISIBLE);
        this.listView.addFooterView(this.rLayout);
        if (token != null) {
            loadData();
        } else {
            Toast.makeText(getActivity(), getResources().getString(R.string.please_login_fb), Toast.LENGTH_SHORT).show();
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
                    reloadData();
                }
            }
        });

        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i < movieArrayList.size()) {
                    String videolink = movieArrayList.get(i).link;
                    videoname = movieArrayList.get(i).name;
                    videoid = movieArrayList.get(i).id;
                    videourlmain = movieArrayList.get(i).source;
                    videouid = toolsHelper.getVideoid(videolink);
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
            fb_user_group_url = "https://graph.facebook.com/v2.2/me/videos/uploaded" +
                    "?fields=id,picture,created_time,source,updated_time&limit=20&access_token=";
            fb_user_group_url += this.token;
            addToRequestQueue(new JsonObjectRequest(0, fb_user_group_url, null, new Response.Listener<JSONObject>() {
                public void onResponse(JSONObject response) {
                    if (response != null) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("data");
                            Log.e("TIMELINE_JSON_ARRAY", jsonArray.toString());
                            if (jsonArray.length() == 0) {
                                tvNoVideo.setVisibility(View.VISIBLE);
                                rLayout.setVisibility(View.INVISIBLE);
                                fbProgress.smoothToHide();
                                return;
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                Log.e("TIMELINE_JSON_OBJECT", jsonObject.toString());
                                if (jsonObject.has("source")) {
                                    if (jsonObject.getString("source").contains(".mp4")) {
                                        MyVideo movie = new MyVideo();
                                        movie.source = jsonObject.getString("source");
                                        movie.id = jsonObject.getString("id");
                                        movie.created_time = toolsHelper.getFormattedTime(jsonObject.getString("created_time"));

                                        if (jsonObject.has("story")) {
                                            movie.name = jsonObject.getString("story");
                                        } else if (jsonObject.has("name")) {
                                            movie.name = jsonObject.getString("name");
                                        } else {
                                            movie.name = "Video untitled";
                                        }
                                        if (jsonObject.has("link")) {
                                            movie.link = jsonObject.getString("link");
                                        }

                                        if (jsonObject.has("picture")) {
                                            movie.picture = jsonObject.getString("picture");
                                        }
                                        movieArrayList.add(movie);
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
                                myVideoAdapter = new MyVideoAdapter(getActivity(), movieArrayList);
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
                    fbProgress.smoothToHide();
                    Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
                    tvNoVideo.setVisibility(View.VISIBLE);
                }
            }));
        } else {
            toolsHelper.showAlertDialog(getActivity(), getResources().getString(R.string.network_connection),
                    getResources().getString(R.string.not_connection), Boolean.valueOf(false));
        }
    }

    private void reloadData() {

        if (!this.cd.isConnectingToInternet()) {
            toolsHelper.showAlertDialog(getActivity(), getResources().getString(R.string.network_connection),
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
                            Log.e("UPLOAD_JSON_ARRAY", jsonArray.toString());
                            if (jsonArray.length() == 0) {
                                Toast.makeText(getActivity(), getResources().getString(R.string.no_more_video), Toast.LENGTH_SHORT).show();
                                rLayout.setVisibility(View.INVISIBLE);
                                fbProgress.smoothToHide();
                                return;
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                Log.e("UPLOAD_JSON_OBJECT", jsonObject.toString());
                                if (jsonObject.has("source")) {
                                    if (jsonObject.getString("source").contains(".mp4")) {
                                        MyVideo movie = new MyVideo();
                                        movie.source = jsonObject.getString("source");
                                        movie.id = jsonObject.getString("id");
                                        movie.created_time = toolsHelper.getFormattedTime(jsonObject.getString("created_time"));

                                        if (jsonObject.has("story")) {
                                            movie.name = jsonObject.getString("story");
                                        } else if (jsonObject.has("name")) {
                                            movie.name = jsonObject.getString("name");
                                        } else {
                                            movie.name = "Video untitled";
                                        }
                                        if (jsonObject.has("link")) {
                                            movie.link = jsonObject.getString("link");
                                        }

                                        if (jsonObject.has("picture")) {
                                            movie.picture = jsonObject.getString("picture");
                                        }
                                        movieArrayList.add(movie);
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
                                tvNoVideo.setVisibility(View.INVISIBLE);
                            } else {
                                tvNoVideo.setVisibility(View.VISIBLE);
                            }

                            if (myVideoAdapter != null && movieArrayList.size() > 0) {
                                myVideoAdapter.notifyDataSetChanged();
                            } else {
                                myVideoAdapter = new MyVideoAdapter(getActivity(), movieArrayList);
                                listView.setAdapter(myVideoAdapter);
                            }
                            listView.setSelectionFromTop(listView.getFirstVisiblePosition() + 1, 0);

                            flag_loading = false;
                            fbProgress.smoothToHide();
                            return;
                        } catch (JSONException e) {
                            fbProgress.smoothToHide();
                            e.printStackTrace();
                        }
                    }
                    fbProgress.smoothToHide();
                    Toast.makeText(getActivity(), getResources().getString(R.string.no_more_video), Toast.LENGTH_SHORT).show();
                }
            }, new Response.ErrorListener() {
                public void onErrorResponse(VolleyError error) {
                    fbProgress.smoothToHide();
                    Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
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
                        VideoUploadFragment.this.videourl = response.getString("source");
                    } else {
                        VideoUploadFragment.this.videourl = VideoUploadFragment.this.videourlmain;
                    }
                    downloadVideoHd();
                } catch (Exception e) {
                }
            }
        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                fbProgress.setVisibility(View.INVISIBLE);
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void downloadVideoHd() {
        fbProgress.setVisibility(View.INVISIBLE);
        if (cd.isConnectingToInternet()) {
            String url = this.videourl;
            if (url.contains("https")) {
                url = url.replace("https", "http");
            }
            String filename = this.videoname.replaceAll("[-+.^:,]", BuildConfig.VERSION_NAME) + ".mp4";
            String new_downloadname = this.videoid + ".mp4";
            String mainname = getActivity().getSharedPreferences("pref", 0).getString("path", "GPVideoDownloader").trim();
            Uri uri = Uri.parse(url.toString());
            try {
                if (Build.VERSION.SDK_INT >= 11) {
                    this.lastDownload = this.mgr.enqueue(new DownloadManager.Request(uri).setAllowedNetworkTypes(3).setAllowedOverRoaming(false).setTitle("").setDescription(getResources().getString(R.string.downloading)).setNotificationVisibility(1).setDestinationInExternalPublicDir(mainname, new_downloadname));
                } else {
                    this.lastDownload = this.mgr.enqueue(new DownloadManager.Request(uri).setAllowedNetworkTypes(3).setAllowedOverRoaming(false).setTitle(this.videoname).setDescription(getResources().getString(R.string.downloading)).setDestinationInExternalPublicDir(mainname, new_downloadname));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(filename))));
            Toast.makeText(getActivity(), getResources().getString(R.string.video_downloading), Toast.LENGTH_SHORT).show();
        }
    }

    public void showMyDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
                        if (videourlmain.contains(".mp4")) {
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.parse(videourlmain), "video/*");
                            List<ResolveInfo> intents = getActivity().getPackageManager().queryIntentActivities(intent, 0);
                            if (intents != null && intents.size() > 0) {
                                startActivity(Intent.createChooser(intent, getResources().getString(R.string.choose_player)));
                            }
                        }
                    } else if (position == 3) {
                        dialog.cancel();
                        ToolsHelper.shareLinkVideo(getActivity(), getResources().getString(R.string.click_video), videourlmain);
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
