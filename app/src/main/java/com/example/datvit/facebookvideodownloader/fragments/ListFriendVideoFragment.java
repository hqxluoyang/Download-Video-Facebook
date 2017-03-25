package com.example.datvit.facebookvideodownloader.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.activities.FriendTaggedVideoActivity;
import com.example.datvit.facebookvideodownloader.activities.FriendTimelineVideoActivity;
import com.example.datvit.facebookvideodownloader.activities.FriendUploadedVideoActivity;
import com.example.datvit.facebookvideodownloader.activities.NewFeedFriendActivity;
import com.example.datvit.facebookvideodownloader.activities.TaggedFriendActivity;
import com.example.datvit.facebookvideodownloader.activities.TimelineFriendActivity;
import com.example.datvit.facebookvideodownloader.activities.UploadedFriendActivity;
import com.example.datvit.facebookvideodownloader.adapters.FriendVideoAdapter;
import com.example.datvit.facebookvideodownloader.models.MyFriend;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

/**
 * Created by DatVIT on 11/12/2016.
 */

public class ListFriendVideoFragment extends Fragment {

    private String BASE_URL;
    private String FRIENDS_URL;
    private FriendVideoAdapter adapter;
    private AlertDialog alertDialog;
    private AsyncHttpClient client;
    private String cookies;
    private int counter;
    private Builder dialogBuilder;
    private ArrayList<MyFriend> friend_itms;
    private ListView lv;
    private TextView nameshow;
    private TextView tvNoFriend;

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
    }

    public ListFriendVideoFragment() {
        this.counter = 0;
        this.FRIENDS_URL = "https://m.facebook.com/friends/center/friends?locale=en_US";
        this.BASE_URL = "https://m.facebook.com";
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend_video, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.lv = (ListView) view.findViewById(R.id.listFriend);
        this.tvNoFriend = (TextView) view.findViewById(R.id.tvNoFriend);
        this.dialogBuilder = new AlertDialog.Builder(getActivity());
        this.friend_itms = new ArrayList();
        this.client = new AsyncHttpClient();

        friendsCountDialog();
        executeUrl(FRIENDS_URL);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showMyDialog(i);
            }
        });
    }

    private void executeUrl(String fRIENDS_URL2) {
        Log.d("LIST_FRIEND", "this is friends url : " + fRIENDS_URL2);
        this.cookies = CookieManager.getInstance().getCookie(fRIENDS_URL2);
        this.client.addHeader("Cookie", this.cookies);

        this.client.addHeader("user-agent", "Mozilla/5.0 (Linux; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0");

        this.client.get(fRIENDS_URL2, new AsyncHttpResponseHandler() {
            public void onFailure(int arg0, Header[] header, byte[] response, Throwable arg3) {
            }

            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onFinish() {
                super.onFinish();
            }

            public void onSuccess(int arg0, Header[] header, byte[] response) {
                try {

                    Document doc = Jsoup.parse(new String(response));
                    Log.d("LIST_FRIEND", "this is doc : " + doc.toString());
                    Elements select = doc.select("a[href*=?uid]");
                    Iterator it = select.iterator();
                    Iterator it2 = doc.select("img[src*=.jpg]").iterator();
                    if (select.size() == select.size()) {
                        String attr;
                        while (it.hasNext()) {
                            Element element = (Element) it.next();
                            String text = element.text();
                            Element element2 = (Element) it2.next();
                            if (text != null) {
                                String attr2 = element2.attr("src");
                                attr = element.attr("href");
                                attr = attr.substring(attr.indexOf("uid="), attr.indexOf("&")).substring(4);
                                MyFriend myFriend = new MyFriend();
                                myFriend.name = text;
                                myFriend.id = attr;

                                myFriend.picture = attr2;
                                friend_itms.add(myFriend);
                                counter++;
                                nameshow.setText(getResources().getString(R.string.load_friend) + " "
                                        + counter + " " + getResources().getString(R.string.friend));
                                Log.e("FRIEND", "ID = " + attr);
                                nameshow.invalidate();
                            }
                        }
                        select = doc.select("div#u_0_0");
                        if (!it.hasNext() && select != null) {
                            attr = select.select("a").attr("href");
                            if (attr.isEmpty()) {
                                if (alertDialog != null) {
                                    alertDialog.dismiss();
                                }
                                if (friend_itms.size() > 0) {
                                    Collections.sort(friend_itms, new FriendComparator());
                                    adapter = new FriendVideoAdapter(getActivity(), friend_itms);
                                    lv.setAdapter(adapter);
                                    return;
                                } else {
                                    tvNoFriend.setVisibility(View.VISIBLE);
                                    return;
                                }
                            }
                            executeUrl(BASE_URL + attr);
                        }
                    }
                } catch (Exception e) {
                }
            }
        });
    }

    private void friendsCountDialog() {
        try {
            View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_count, null);
            this.dialogBuilder.setTitle(getResources().getString(R.string.loading_friends));
            this.dialogBuilder.setView(dialogView);
            this.nameshow = (TextView) dialogView.findViewById(R.id.textView1);
            this.nameshow.setText(getResources().getString(R.string.load_friend_2));
            this.alertDialog = this.dialogBuilder.create();
            this.alertDialog.setCancelable(true);
            this.alertDialog.setCanceledOnTouchOutside(false);
            this.alertDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            this.dialogBuilder = new AlertDialog.Builder(getActivity());
        }
    }

    private void showMyDialog(int position) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle((this.friend_itms.get(position)).name);
        String name = (this.friend_itms.get(position)).name;
        String id = (this.friend_itms.get(position)).id;
        Log.d("LIST_FRIEND", "id of friends :" + id);
        View tilleview = View.inflate(getActivity(), R.layout.dialog_video_friend_app, null);
        TextView tagged = (TextView) tilleview.findViewById(R.id.tvTagged);
        TextView upload = (TextView) tilleview.findViewById(R.id.tvUpload);
        TextView timeline = (TextView) tilleview.findViewById(R.id.tvTimeline);
//        TextView tvNewFeed = (TextView) tilleview.findViewById(R.id.tvNewFeed);
        tagged.setOnClickListener(new TaggedListener(id, name));
        upload.setOnClickListener(new UploadListener(id, name));
        timeline.setOnClickListener(new TimelineListener(id, name));
//        tvNewFeed.setOnClickListener(new NewFeedListener(id, name));
        alertDialogBuilder.setView(tilleview).setCancelable(true).setCancelable(true);
        this.alertDialog = alertDialogBuilder.create();
        this.alertDialog.show();
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

    class TaggedListener implements View.OnClickListener {
        final /* synthetic */ String val$id;
        final /* synthetic */ String val$name;

        TaggedListener(String str, String str2) {
            this.val$id = str;
            this.val$name = str2;
        }

        public void onClick(View v) {
            alertDialog.cancel();
            Intent intent = new Intent(getActivity(), FriendTaggedVideoActivity.class);
            intent.putExtra("id", this.val$id);
            intent.putExtra("tged", "Tagged Video");
            intent.putExtra("name", this.val$name);
            startActivity(intent);
        }
    }

    class UploadListener implements View.OnClickListener {
        final /* synthetic */ String val$id;
        final /* synthetic */ String val$name;

        UploadListener(String str, String str2) {
            this.val$id = str;
            this.val$name = str2;
        }

        public void onClick(View v) {
            alertDialog.cancel();
            Intent intent = new Intent(getActivity(), FriendUploadedVideoActivity.class);
            intent.putExtra("id", this.val$id);
            intent.putExtra("tged", "Uploaded Video");
            intent.putExtra("name", this.val$name);
            startActivity(intent);
        }
    }

    class TimelineListener implements View.OnClickListener {
        final /* synthetic */ String val$id;
        final /* synthetic */ String val$name;

        TimelineListener(String str, String str2) {
            this.val$id = str;
            this.val$name = str2;
        }

        public void onClick(View v) {
            Intent intent = new Intent(getActivity(), FriendTimelineVideoActivity.class);
            alertDialog.cancel();
            intent.putExtra("key", this.val$id);
            intent.putExtra("tged", "Timeline Video");
            intent.putExtra("name", this.val$name);
            startActivity(intent);
        }
    }

    class NewFeedListener implements View.OnClickListener {
        final /* synthetic */ String val$id;
        final /* synthetic */ String val$name;

        NewFeedListener(String str, String str2) {
            this.val$id = str;
            this.val$name = str2;
        }

        public void onClick(View v) {
            Intent intent = new Intent(getActivity(), NewFeedFriendActivity.class);
            alertDialog.cancel();
            intent.putExtra("id", this.val$id);
            intent.putExtra("title", "NewsFeed Video");
            intent.putExtra("name", this.val$name);
            startActivity(intent);
        }
    }
}
