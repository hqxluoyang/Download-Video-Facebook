package com.example.datvit.facebookvideodownloader.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.adapters.MyVideoAdapter;
import com.example.datvit.facebookvideodownloader.models.MyVideo;
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

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import cz.msebera.android.httpclient.Header;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by DatVIT on 12/2/2016.
 */
public class VideoSharedFragment extends Fragment implements View.OnClickListener {

    public static final String BASE_URL = "https://m.facebook.com";
    public static final String LIKED_URL = "/allactivity?log_filter=cluster_11&locale=en_US";
    private TextView textView;
    private ListView listView;
    private ArrayList<MyVideo> videoArrayList;
    private MyVideoAdapter myVideoAdapter;
    private String urlBase;
    private boolean isCheck;
    private String cookie;
    private Button buttonLoadMore;
    private AsyncHttpClient client;
    private AVLoadingIndicatorView loadingIndicatorView;
    private int counter;
    public String image_url;
    public String download_title;
    public String download_uri;
    public String download_uri_hq;
    public String video_url;

    private Dialog dialog;
    private LinearLayout btnPlay, btnShare, btnLink, btnDownload;
    private EditText editText;
    private DownloadManager.Request request;
    private long count;
    private DownloadManager downloadManager;
    private ProgressDialog progressDialog;
    private ToolsHelper toolsHelper;
    private LinkedHashSet<String> linkedHashSet;
    private boolean isShare;
    private boolean isDownload;
    private SharedPreferences loginState;
    private SharedPreferences.Editor loginStateEditor;
    private String token;
    private FacebookSession facebookSession;
    public static final String TAG = "VolleyPatterns";
    private String fb_user_group_url;
    private RequestQueue mRequestQueue;

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

    public VideoSharedFragment() {
        this.videoArrayList = new ArrayList();
        this.urlBase = "";
        this.isCheck = false;
        this.isDownload = false;
        buttonLoadMore = null;
        counter = 0;
        linkedHashSet = new LinkedHashSet<>();
        isShare = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        this.downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        toolsHelper = new ToolsHelper();


        facebookSession = new FacebookSession(getActivity());
        if (facebookSession != null && facebookSession.getToken() != null) {
            this.token = facebookSession.getToken();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movies_watched, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = (ListView) view.findViewById(R.id.listVideoWatched);
        textView = (TextView) view.findViewById(R.id.tvNoMovie);
        textView.setText(getResources().getString(R.string.no_video_found));
        loadingIndicatorView = (AVLoadingIndicatorView) view.findViewById(R.id.fbProgress);
        try {
            listView.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {

                }

                @Override
                public void onScroll(AbsListView absListView, int i, int i1, int i2) {

                }
            });
            listView.setFastScrollEnabled(true);
            ViewGroup viewGroup = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.bottom_view_list, listView, false);
            buttonLoadMore = (Button) viewGroup.findViewById(R.id.loadMore);
            buttonLoadMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getData();
                    counter = 0;
                }
            });

            buttonLoadMore.setVisibility(View.INVISIBLE);
            listView.addFooterView(viewGroup, null, false);
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
        }

        initDialog();

        getData();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                doItemClick(i, false);
            }
        });
    }

    private void initDialog() {
        dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_download);

        editText = (EditText) dialog.findViewById(R.id.etNameVideo);
        btnDownload = (LinearLayout) dialog.findViewById(R.id.btnDownload);
        btnShare = (LinearLayout) dialog.findViewById(R.id.btnShare);
        btnLink = (LinearLayout) dialog.findViewById(R.id.btnLink);
        btnPlay = (LinearLayout) dialog.findViewById(R.id.btnPlay);

        btnShare.setOnClickListener(this);
        btnLink.setOnClickListener(this);
        btnPlay.setOnClickListener(this);
        btnDownload.setOnClickListener(this);
    }

    private void getData() {
        textView.setVisibility(View.INVISIBLE);
        loadingIndicatorView.setVisibility(View.VISIBLE);
        loadingIndicatorView.show();
        if (this.urlBase == null || this.urlBase.equals("")) {
            excuteUrl("https://m.facebook.com/" + "me" + this.LIKED_URL);
            return;
        } else {
            excuteUrl(BASE_URL + this.urlBase);
            return;
        }
    }

    private void loadData() {
        loadingIndicatorView.hide();
        if (videoArrayList.size() > 0) {
            textView.setVisibility(View.INVISIBLE);
            myVideoAdapter = new MyVideoAdapter(getActivity(), videoArrayList);
            listView.setAdapter(myVideoAdapter);
        } else {
            textView.setVisibility(View.VISIBLE);
        }
    }

    class VideoComparator implements Comparator<MyVideo> {

        public int compare(MyVideo group1, MyVideo group2) {
            String name1 = group1.time;
            String name2 = group2.time;
            if (name1.compareTo(name2) > 0) {
                return -1;
            } else if (name1.compareTo(name2) == 0) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    private void excuteUrl(String str) {
        this.isCheck = false;
        Log.d("WATCHED VIDEO", "Url : " + str);
        this.client = new AsyncHttpClient();
        this.cookie = CookieManager.getInstance().getCookie(str);
        this.client.addHeader("user-agent", "Mozilla/5.0 (Linux; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0");
        this.client.addHeader("Cookie", this.cookie);
        this.client.get(str, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    Iterator it;
                    counter++;
                    Document parse = Jsoup.parse(new String(responseBody));
                    Elements elements = parse.select("a[href*=allactivity?]");
                    Iterator it2 = elements.iterator();
                    while (it2.hasNext()) {
                        Element element = (Element) it2.next();
                        String text = element.text();
//                        if (element.parents().select("h3") != null) {
//                            Log.d("WATCHED VIDEO", "Text h3 : " + element.parents().select("h3").first().text());
////                            if (element.parents().select("h3").first().text().toLowerCase().contains("more")) {
//                            text = element.parents().select("h3").first().text();
////                            }
//                        }
                        if (!(text == null || text.equals("") || text.toLowerCase().equals("this month") || text.toLowerCase().equals("month"))) {
                            String attr = element.attr("href");
                            String section = attr.substring(attr.indexOf("sectionID=") + 10);
                            Log.d("WATCHED VIDEO", "Subtring : " + section);
                            if (!linkedHashSet.contains(section)) {
                                linkedHashSet.add(section);
                                urlBase = attr + "&locale=en_US";
                                buttonLoadMore.setVisibility(View.VISIBLE);
                                buttonLoadMore.setText(text.toLowerCase().contains("more") ? text : text + " ("
                                        + getResources().getString(R.string.load_more) + "...)");
                                it = parse.select("a[href*=story.php]").iterator();
                                while (it.hasNext()) {
                                    String text2;
                                    element = (Element) it.next();
                                    text2 = element.text();
                                    Log.d("FB-Txt", text2 + "");
                                    if (text2 != null && text2.contains("video")) {
                                        isCheck = true;
                                        if (element != null) {
//                                        getVideo(BASE_URL + element.attr("href"));
                                            getVideoWatch(element.attr("href").substring(element.attr("href").indexOf("=") + 1,
                                                    element.attr("href").indexOf("&")));
                                        }
                                    }
                                }
                                if (!isCheck) {
                                    if (counter <= 3) {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                getData();
                                            }
                                        });
                                    } else {
                                        loadingIndicatorView.hide();
                                        buttonLoadMore.setVisibility(View.VISIBLE);
                                        buttonLoadMore.setText(getResources().getString(R.string.please_try_again));
                                    }
                                }
                                return;
                            }
                        }
                    }

                } catch (Exception e) {
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

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

    private void getVideoWatch(String id) {
        this.fb_user_group_url = "https://graph.facebook.com/v2.2/" + id +
                "?fields=source,created_time,description,id,picture,from"
                + "&access_token=" + token;
        Log.e("WATCHED_VIDEO_TIME", fb_user_group_url);
        addToRequestQueue(new JsonObjectRequest(0, fb_user_group_url, null, new Response.Listener<JSONObject>() {
            public void onResponse(JSONObject response) {
                if (response != null) {
                    JSONObject jsonObject = response;
                    if (jsonObject != null) {
                        try {
                            MyVideo myVideo = new MyVideo();
                            if (jsonObject.has("id")) {
                                myVideo.id = jsonObject.getString("id");
                            }
                            if (jsonObject.has("from")) {
                                myVideo.message = jsonObject.getJSONObject("from").getString("name");
                            }
                            if (jsonObject.has("created_time")) {
                                myVideo.created_time = toolsHelper.getFormattedTime(jsonObject.getString("created_time"));
                            }
                            if (jsonObject.has("created_time")) {
                                myVideo.time = jsonObject.getString("created_time");
                            }
                            if (jsonObject.has("picture")) {
                                myVideo.picture = jsonObject.getString("picture");
                            }

                            if (jsonObject.has("description")) {
                                myVideo.name = jsonObject.getString("description");
                            } else {
                                myVideo.name = "Video untitled";
                            }
                            if (jsonObject.has("source")) {
                                myVideo.source = jsonObject.getString("source");
                            }
                            videoArrayList.add(myVideo);
                            Log.e("WATCHED_VIDEO_TIME", "VIDEO_DETAIL: " + videoArrayList.size() + " - " + myVideo.id + " - " + myVideo.created_time);

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    loadingIndicatorView.hide();
                                    Collections.sort(videoArrayList, new VideoComparator());
                                    if (myVideoAdapter == null && videoArrayList.size() > 0) {
                                        textView.setVisibility(View.INVISIBLE);
                                        myVideoAdapter = new MyVideoAdapter(getActivity(), videoArrayList);
                                        listView.setAdapter(myVideoAdapter);
                                    } else if (myVideoAdapter != null && videoArrayList.size() > 0) {
                                        textView.setVisibility(View.INVISIBLE);
                                        myVideoAdapter.notifyDataSetChanged();
                                    } else {
                                        textView.setVisibility(View.VISIBLE);
                                    }
                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                            loadingIndicatorView.hide();
                            textView.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                loadingIndicatorView.hide();
                textView.setVisibility(View.VISIBLE);
            }
        }));
    }

    public void getVideo(String str) {
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        asyncHttpClient.addHeader("Cookie", CookieManager.getInstance().getCookie(str));
        asyncHttpClient.addHeader("user-agent", "Mozilla/5.0 (Linux; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0");
        asyncHttpClient.get(str, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Element first;
                try {
                    Element element;
                    Element element2;
                    Element element3;
                    Document parse = Jsoup.parse(new String(responseBody));
                    Element first2 = parse.select("p").first();

                    element = parse.select("h3").get(1);
                    element2 = parse.select("abbr").get(0);
                    element3 = element;

                    Log.e("WATCHED_VIDEO", "Element: " + element.toString());
                    Log.e("WATCHED_VIDEO", "Element2: " + element2.toString());
                    Log.e("WATCHED_VIDEO", "Element3: " + element3.toString());

                    first = parse.select("a[href*=mp4]").first();
                    element = parse.select("img[src*=.jpg]").first();
                    if (first != null && element != null) {
                        MyVideo myVideo = new MyVideo();
                        myVideo.picture = element.attr("src");
                        myVideo.name = first2 != null ? first2.text() : "Unknown";
                        Log.e("WATCHED_VIDEO", "TITLE: " + (first2 != null ? first2.text() : ""));
                        String attr2 = first.attr("href");
                        String decode = URLDecoder.decode(attr2.substring(attr2.indexOf("src=") + 4), "UTF-8");
                        myVideo.source = decode.substring(0, decode.indexOf("&source=misc"));
                        Log.e("WATCHED_VIDEO", "URL_VIDEO: " + myVideo.source);
                        if (element3 != null) {
                            myVideo.message = element3.select("a").html();
                            Log.e("WATCHED_VIDEO", "AUTHOR: " + element3.select("a").html());
                        }
                        if (element2 != null) {
                            myVideo.created_time = element2.html();
                            Log.e("WATCHED_VIDEO", "DATE: " + element2.html());
                        }
                        videoArrayList.add(myVideo);
                        //Removing Duplicates;
                        Set<MyVideo> s = new HashSet<MyVideo>();
                        s.addAll(videoArrayList);
                        videoArrayList = new ArrayList<MyVideo>();
                        videoArrayList.addAll(s);

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadData();
                            }
                        });
                    }
                } catch (Exception e3) {
                    e3.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    public void doItemClick(int pos, boolean isDownload) {
        try {
            String str = "";
            this.image_url = str;
            this.download_title = str;
            if (videoArrayList != null && videoArrayList.size() > 0) {
                MyVideo myVideo = videoArrayList.get(pos);
                if (myVideo.source != null) {
                    str = myVideo.source;
                    this.download_uri = str;
                    this.download_uri_hq = str;
                    video_url = str;
                }
                this.download_uri = this.download_uri.replace("https", "http");
                this.download_uri_hq = this.download_uri_hq.replace("https", "http");
                if (myVideo.name != null && !myVideo.name.isEmpty()) {
                    this.download_title = myVideo.name;
                } else {
                    this.download_title = "Facebook Video";
                }
                if (myVideo.picture != null) {
                    this.image_url = myVideo.picture;
                }

                this.editText.setText("");
                String temp = "";
                if (this.download_title.equals("")) {
                    char[] toCharArray = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
                    StringBuilder stringBuilder = new StringBuilder();
                    Random random = new Random();
                    for (int i = 0; i < 12; i++) {
                        stringBuilder.append(toCharArray[random.nextInt(toCharArray.length)]);
                    }
                    this.download_title = stringBuilder.toString();
                    temp = temp.replaceAll(" ", "_").replaceAll("[^\\p{L}\\p{Nd}\\-]", "");
                    if (!(temp == null || temp.equals(""))) {
                        this.download_title = temp + ": " + this.download_title;
                    }
                }
                editText.setText(this.download_title + "");
                if (this.download_title.length() > 30) {
                    this.download_title = this.download_title.substring(0, 29);
                }
                editText.setText(this.download_title);
                if (this.download_uri == null || this.download_uri.equals("")) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.please_login_see_more), Toast.LENGTH_SHORT).show();
                } else if (isDownload) {
                    onClick(btnDownload);
                } else {
                    dialog.show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint({"NewApi"})
    public void onClick(View view) {
        try {
            switch (view.getId()) {
                case R.id.btnPlay:
                    if (video_url.contains(".mp4")) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(video_url), "video/*");
                        List<ResolveInfo> intents = getActivity().getPackageManager().queryIntentActivities(intent, 0);
                        if (intents != null && intents.size() > 0) {
                            startActivity(Intent.createChooser(intent, getResources().getString(R.string.choose_player)));
                        }
                    }
                    this.dialog.cancel();
                    return;
                case R.id.btnLink:
                    copyToClipboard(this.download_uri_hq);
                    return;
                case R.id.btnDownload:
                    isShare = false;
                    if (!(this.editText == null || this.editText.getText().toString().equals(""))) {
                        this.download_title = this.editText.getText().toString();
                    }
                    if (this.download_title.length() > 30) {
                        this.download_title = this.download_title.substring(0, 29);
                    }
                    this.download_title = this.download_title.replaceAll(" ", "_").replaceAll("[^\\p{L}\\p{Nd}\\_]", "");
                    downloadVideo();
                    return;
                case R.id.btnShare:
                    isShare = true;
                    if (!(this.editText == null || this.editText.getText().toString().equals(""))) {
                        this.download_title = this.editText.getText().toString();
                    }
                    if (this.download_title.length() > 30) {
                        this.download_title = this.download_title.substring(0, 29);
                    }
                    this.download_title = this.download_title.replaceAll(" ", "_").replaceAll("[^\\p{L}\\p{Nd}\\_]", "");
                    downloadVideo();
                    return;
                default:
                    return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint({"NewApi"})
    public void copyToClipboard(String str) {
        try {
            if (Build.VERSION.SDK_INT < 11) {
                ((ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE)).setText(str);
            } else {
                ((android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE))
                        .setPrimaryClip(ClipData.newPlainText("Clipboard Video", str));
            }
            Toast.makeText(getActivity(), getResources().getString(R.string.video_link_copy), Toast.LENGTH_SHORT).show();
            if (this.dialog != null) {
                this.dialog.cancel();
            }
        } catch (Exception e) {
            if (this.dialog != null) {
                this.dialog.cancel();
            }
        } catch (Throwable th) {
            if (this.dialog != null) {
                this.dialog.cancel();
            }
        }
    }

    private void downloadVideo() {
        Uri parse = Uri.parse(this.download_uri);
        try {
            if (this.download_uri == null || this.download_uri.equals("")) {
                Toast.makeText(getActivity(), getResources().getString(R.string.try_other_video), Toast.LENGTH_SHORT).show();
                this.dialog.cancel();
            }
            this.request = new DownloadManager.Request(parse);
            this.request.setAllowedNetworkTypes(3);
            this.request.setTitle(this.download_title);
//            this.request.setDestinationInExternalPublicDir(toolsHelper.file.getPath(), this.download_title + ".mp4");
            this.request.setDestinationInExternalPublicDir("/GPVideoDownloader", this.download_title + ".mp4");
            if (Build.VERSION.SDK_INT >= 11) {
                this.request.allowScanningByMediaScanner();
            }
            this.count = this.downloadManager.enqueue(this.request);
            DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
            this.progressDialog = new ProgressDialog(getActivity());
            this.progressDialog.setMessage(getResources().getString(R.string.downloading));
            this.progressDialog.setProgressStyle(1);
            this.progressDialog.setIndeterminate(false);
            this.progressDialog.setProgress(0);
            this.progressDialog.show();
            new Thread(new MyDownload(downloadManager)).start();
            this.dialog.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class MyDownload implements Runnable {
        final /* synthetic */ DownloadManager dm;

        class MyThread implements Runnable {
            final /* synthetic */ int number;
            final /* synthetic */ MyDownload myDownload;

            MyThread(MyDownload c19832, int i) {
                this.myDownload = c19832;
                this.number = i;
            }

            public void run() {
                progressDialog.setProgress(this.number);
                if (number >= progressDialog.getMax() && isDownload) {
                    isDownload = false;
                    Toast.makeText(getActivity(), getResources().getString(R.string.download_successfully), Toast.LENGTH_SHORT).show();
                }
            }
        }

        MyDownload(DownloadManager downloadManager) {
            this.dm = downloadManager;
        }

        public void run() {
            int i = 1;
            while (i != 0) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(new long[]{count});
                Cursor query2 = this.dm.query(query);
                query2.moveToFirst();
                int i2 = query2.getInt(query2.getColumnIndex("bytes_so_far"));
                int i3 = query2.getInt(query2.getColumnIndex("total_size"));
                if (query2.getInt(query2.getColumnIndex(NotificationCompat.CATEGORY_STATUS)) == 8) {
                    progressDialog.dismiss();
                    isDownload = true;
                    if (isShare) {
                        ToolsHelper.shareVideo(getActivity(), download_title, toolsHelper.file.getPath() + "/" + download_title + ".mp4");
                    }
                    i = 0;
                }
                Log.e("-->", i2 + "-" + i3);
                i2 = (int) ((((long) i2) * 100) / ((long) i3));
                Log.e("-->", i2 + "");
                getActivity().runOnUiThread(new MyThread(this, i2));
                query2.close();
            }
        }
    }
}
