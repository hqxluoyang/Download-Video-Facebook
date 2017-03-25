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

import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.adapters.MyVideoAdapter;
import com.example.datvit.facebookvideodownloader.models.MyVideo;
import com.example.datvit.facebookvideodownloader.utils.ConnectionDetector;
import com.example.datvit.facebookvideodownloader.utils.ToolsHelper;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.wang.avi.AVLoadingIndicatorView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import cz.msebera.android.httpclient.Header;

/**
 * Created by DatVIT on 12/2/2016.
 */
public class NewsFeedVideoFragment extends Fragment implements View.OnClickListener {

    public static final String BASE_URL = "https://m.facebook.com";
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
    private HashMap<String, String> hashMap;
    public boolean isShare;
    public boolean isDownload;
    private ConnectionDetector cd;
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


    public NewsFeedVideoFragment() {
        this.videoArrayList = new ArrayList();
        this.urlBase = "";
        this.isCheck = false;
        this.isDownload = false;
        buttonLoadMore = null;
        counter = 0;
        hashMap = new HashMap<>();
        isShare = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        this.downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        toolsHelper = new ToolsHelper();
        cd = new ConnectionDetector(getActivity());
        loginState = getActivity().getSharedPreferences("LOGIN_STATE", Context.MODE_PRIVATE);
        loginStateEditor = loginState.edit();
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
        if (this.cd.isConnectingToInternet()) {
            textView.setVisibility(View.INVISIBLE);
            loadingIndicatorView.setVisibility(View.VISIBLE);
            loadingIndicatorView.show();
            if (this.urlBase == null || this.urlBase.equals("")) {
                excuteUrl("https://m.facebook.com/?locale=en_US");
                return;
            } else {
                excuteUrl(BASE_URL + this.urlBase);
                return;
            }
        } else {
            toolsHelper.showAlertDialog(getActivity(), getResources().getString(R.string.network_connection),
                    getResources().getString(R.string.not_connection), Boolean.valueOf(false));
        }
    }

    private void loadData() {
        loadingIndicatorView.smoothToHide();
        Collections.sort(videoArrayList, new VideoComparator());
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
            String name1 = group1.created_time;
            String name2 = group2.created_time;
            if (name1.compareTo(name2) > 0) {
                return 1;
            } else if (name1.compareTo(name2) == 0) {
                return 0;
            } else {
                return -1;
            }
        }
    }

    private void excuteUrl(String str) {
        this.isCheck = false;
        Log.d("NEW_FEED_VIDEO", "Url : " + str);
        this.client = new AsyncHttpClient();
        this.cookie = CookieManager.getInstance().getCookie(str);
        this.client.addHeader("user-agent", "Mozilla/5.0 (Linux; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0");
        this.client.addHeader("Cookie", this.cookie);
        this.client.get(str, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                counter++;
                Element element;
                Document parse = Jsoup.parse(new String(responseBody));
                Elements elements = parse.select("a[href*=stories.php?]");
                if (elements != null && elements.size() > 0) {
                    element = elements.get(0);
                    String text = element.text();
                    if (!(text == null || text.equals("") || text.toLowerCase().equals("more feed"))) {
                        urlBase = element.attr("href") + "&locale=en_US";
                        buttonLoadMore.setVisibility(View.VISIBLE);
                        buttonLoadMore.setText(text.toLowerCase().contains("more") ? text : text + " ("
                                + getResources().getString(R.string.load_more) + "...)");
                    }
                }
                Iterator it = parse.select("a[href*=.mp4]").iterator();
                while (it.hasNext()) {
                    element = (Element) it.next();
                    element.text();
                    try {
                        String attr = element.getAllElements().first().select("img[src*=.jpg").first().attr("src");
                        String attr2 = element.attr("href");
                        if (!(hashMap == null || hashMap.containsKey(attr))) {
                            Element first = element.parents().select("p").first();
                            MyVideo myVideo = new MyVideo();
                            if (first == null || first.text().equals("")) {
                                myVideo.name = "";
                            } else {
                                myVideo.name = first.text();
                            }
                            myVideo.picture = attr;
                            try {
                                Elements parents = element.parents();
                                myVideo.created_time = parents.select("abbr").first().text();
                                myVideo.message = Jsoup.parse(parents.select("h3").first().text()).text();
                            } catch (Exception e) {
                            }
                            try {
                                String decode = URLDecoder.decode(attr2.substring(attr2.indexOf("src=") + 4), "UTF-8");
                                myVideo.source = decode.substring(0, decode.indexOf("&source=misc"));
                                hashMap.put(attr, "");
                                isCheck = true;
                                videoArrayList.add(myVideo);

                            } catch (UnsupportedEncodingException e4) {
                                e4.printStackTrace();
                            }

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    loadData();
                                }
                            });
                        }
                    } catch (Exception e5) {
                        e5.printStackTrace();
                    }
                }
                if (!isCheck) {
                    if (counter <= 3) {
                        getData();
                    } else {
                        loadingIndicatorView.smoothToHide();
                        buttonLoadMore.setVisibility(View.VISIBLE);
                        buttonLoadMore.setText(getResources().getString(R.string.please_try_again));
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                loadingIndicatorView.smoothToHide();
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
                    this.dialog.cancel();
                    if (video_url.contains(".mp4")) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(video_url), "video/*");
                        List<ResolveInfo> intents = getActivity().getPackageManager().queryIntentActivities(intent, 0);
                        if (intents != null && intents.size() > 0) {
                            startActivity(Intent.createChooser(intent, getResources().getString(R.string.choose_player)));
                        }
                    }
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
            this.request.setDestinationInExternalPublicDir(toolsHelper.file.getPath(), this.download_title + ".mp4");
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
