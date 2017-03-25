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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.activities.OnlineVideosActivity;
import com.example.datvit.facebookvideodownloader.adapters.FriendVideoAdapter;
import com.example.datvit.facebookvideodownloader.models.MyFriend;
import com.example.datvit.facebookvideodownloader.models.MyVideo;
import com.example.datvit.facebookvideodownloader.utils.ToolsHelper;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.wang.avi.AVLoadingIndicatorView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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
public class SearchVideoFragment extends Fragment {

    public static String URL = "https://m.facebook.com/search/?search=Search&search_source=top_nav&query=";
    private TextView textView;
    private ListView listView;
    private ArrayList<MyFriend> friends;
    private FriendVideoAdapter friendVideoAdapter;
    private String cookie;
    private Button buttonLoadMore;
    private AsyncHttpClient client;
    private AVLoadingIndicatorView loadingIndicatorView;
    private int counter;

    public boolean isShare;
    public boolean isDownload;
    private EditText etSearch;
    private ImageView btnSearch;

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


    public SearchVideoFragment() {
        this.isDownload = false;
        buttonLoadMore = null;
        counter = 0;
        isShare = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        friends = new ArrayList<>();

        loginState = getActivity().getSharedPreferences("LOGIN_STATE", Context.MODE_PRIVATE);
        loginStateEditor = loginState.edit();

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_video, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = (ListView) view.findViewById(R.id.listVideoWatched);
        textView = (TextView) view.findViewById(R.id.tvNoVideo);
        etSearch = (EditText) view.findViewById(R.id.etSearch);
        btnSearch = (ImageView) view.findViewById(R.id.btnSearch);
        loadingIndicatorView = (AVLoadingIndicatorView) view.findViewById(R.id.fbProgress);
        listView.setFastScrollEnabled(true);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (friends.size() > 0) {
                    friends.clear();
                    friendVideoAdapter.notifyDataSetChanged();
                }
                if (etSearch.getText() != null && !etSearch.getText().toString().trim().isEmpty()) {
                    getData(etSearch.getText().toString().trim());
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.you_do_not_keyword), Toast.LENGTH_SHORT).show();
                }
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (friends.size() > 0) {
                    Intent intent = new Intent(getActivity(), OnlineVideosActivity.class);
                    intent.putExtra("link", friends.get(i).id);
                    intent.putExtra("name", friends.get(i).name);
                    startActivity(intent);
                }
            }
        });
    }

    private void getData(String url) {
        textView.setVisibility(View.INVISIBLE);
        loadingIndicatorView.smoothToShow();
        excuteUrl(URL + url);
    }

    private void loadData() {
        loadingIndicatorView.smoothToHide();
        if (friends.size() > 0) {
//            Collections.sort(friends, new FriendComparator());
            friendVideoAdapter = new FriendVideoAdapter(getActivity(), friends);
            listView.setAdapter(friendVideoAdapter);
        } else {
            textView.setVisibility(View.VISIBLE);
        }
    }

    class FriendComparator implements Comparator<MyFriend> {

        public int compare(MyFriend friend1, MyFriend friend2) {
            String name1 = friend1.name;
            String name2 = friend2.name;
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
        Log.d("SEARCH", "Url : " + str);
        this.client = new AsyncHttpClient();
        this.cookie = CookieManager.getInstance().getCookie(str);
        this.client.addHeader("user-agent", "Mozilla/5.0 (Linux; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0");
        this.client.addHeader("Cookie", this.cookie);
        this.client.get(str, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try{
                counter++;
                Element element;
                Document document = Jsoup.parse(new String(responseBody));
                Log.e("SEARCH", "Document : " + document.outerHtml());
                Iterator it = document.select("img").iterator();
                while (it.hasNext()) {
                    element = (Element) it.next();
                    String attr = element.attr("src");
                    if (attr != null && attr.contains(".jpg")) {
                        Iterator it2 = element.parent().siblingElements().iterator();
                        while (it2.hasNext()) {
                            element = (Element) it2.next();
                            String text = element.select("a").first().text();
                            String attr2 = element.select("a").first().attr("href");
                            String like = element.select("div").first().text();
                            Log.e("SEARCH", "Element : " + element.outerHtml());
                            Log.e("SEARCH", "FRIEND : " + text + " - " + attr2 + " - " + like);
                            if (text != null && attr2 != null) {
                                MyFriend myFriend = new MyFriend();
                                myFriend.name = text;
                                myFriend.id = "https://m.facebook.com" + attr2;
                                myFriend.picture = attr;
                                myFriend.like = like;
                                friends.add(myFriend);
                                break;
                            }
                        }
                    }
                }
                Iterator it3 = document.getElementsByTag("a").iterator();
                while (it3.hasNext()) {
                    element = (Element) it3.next();
                    String text2 = element.text();
                    Log.e("SEARCH", "MORE : " + text2);
                    if (text2 != null && (text2.equals("See More Results") || text2.equals("Xem thêm kết quả"))) {
                        String str2 = "https://m.facebook.com" + element.attr("href");
                        excuteUrl(str2);
                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadData();
                            }
                        });
                    }
                }

                } catch (Exception e) {}

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }
}
