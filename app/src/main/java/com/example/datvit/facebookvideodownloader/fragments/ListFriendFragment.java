package com.example.datvit.facebookvideodownloader.fragments;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.AbsListView;
import android.widget.AdapterView;
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
import com.example.datvit.facebookvideodownloader.activities.FriendTaggedVideoActivity;
import com.example.datvit.facebookvideodownloader.activities.FriendTimelineVideoActivity;
import com.example.datvit.facebookvideodownloader.activities.FriendUploadedVideoActivity;
import com.example.datvit.facebookvideodownloader.activities.NewFeedFriendActivity;
import com.example.datvit.facebookvideodownloader.activities.PageVideoActivity;
import com.example.datvit.facebookvideodownloader.activities.TaggedFriendActivity;
import com.example.datvit.facebookvideodownloader.activities.TimelineFriendActivity;
import com.example.datvit.facebookvideodownloader.activities.UploadedFriendActivity;
import com.example.datvit.facebookvideodownloader.adapters.FriendVideoAdapter;
import com.example.datvit.facebookvideodownloader.adapters.PagesVideoAdapter;
import com.example.datvit.facebookvideodownloader.models.MyFriend;
import com.example.datvit.facebookvideodownloader.models.MyPage;
import com.example.datvit.facebookvideodownloader.utils.ConnectionDetector;
import com.example.datvit.facebookvideodownloader.utils.FacebookSession;
import com.example.datvit.facebookvideodownloader.utils.ToolsHelper;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by DatVIT on 11/15/2016.
 */

public class ListFriendFragment extends Fragment {

    public static final String TAG = "VolleyPatterns";
    private static final String TAG_DATA = "data";
    private static final String TAG_DATA_Next = "paging";

    private String user_id;
    private String token;
    private String fb_user_group_url;
    private String nextpage_url;

    private int currentScrollState;
    private int currentVisibleItemCount;
    private boolean flag_loading;
    private boolean flag_loadingagain;
    private AlertDialog alertDialog;

    private ListView listView;
    private ArrayList<MyFriend> friendArrayList;
    private FriendVideoAdapter friendVideoAdapter;
    private RelativeLayout rLayout;
    private AVLoadingIndicatorView loderprogress;
    private AVLoadingIndicatorView fbProgress;
    private TextView tvNoFriend;

    private ConnectionDetector cd;
    private RequestQueue mRequestQueue;
    private ToolsHelper toolsHelper;
    private FacebookSession facebookSession;

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


    public ListFriendFragment() {
        cd = null;
        toolsHelper = null;
        flag_loading = false;
        facebookSession = null;
        flag_loadingagain = false;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        facebookSession = new FacebookSession(getActivity());
        if (facebookSession.getToken() != null) {
            token = facebookSession.getToken();
        }
        if (facebookSession.getUserId() != null) {
            user_id = facebookSession.getUserId();
        }

        if (token != null) {
            Log.e("LISTFRINED_TOKEN", token);
            Log.e("LISTFRINED_ID", user_id);
        }
        this.cd = new ConnectionDetector(getActivity());
        this.toolsHelper = new ToolsHelper();
        friendArrayList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = (ListView) view.findViewById(R.id.listFriend);
        tvNoFriend = (TextView) view.findViewById(R.id.tvNoFriend);
        fbProgress = (AVLoadingIndicatorView) view.findViewById(R.id.fbProgress);
        tvNoFriend.setVisibility(View.INVISIBLE);

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
                if (i < friendArrayList.size()) {
                    showMyDialog(i);
                }
            }
        });
    }

    private void showMyDialog(int position) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle((this.friendArrayList.get(position)).name);
        String name = (this.friendArrayList.get(position)).name;
        String id = (this.friendArrayList.get(position)).id;
        Log.d("LIST_FRIEND", "id of friends :" + id);
        View tilleview = View.inflate(getActivity(), R.layout.dialog_video_friend_app, null);
        TextView tagged = (TextView) tilleview.findViewById(R.id.tvTagged);
        TextView upload = (TextView) tilleview.findViewById(R.id.tvUpload);
        TextView timeline = (TextView) tilleview.findViewById(R.id.tvTimeline);
//        TextView newsfeed = (TextView) tilleview.findViewById(R.id.tvNewFeed);
        tagged.setOnClickListener(new TaggedListener(id, name));
        upload.setOnClickListener(new UploadListener(id, name));
        timeline.setOnClickListener(new TimelineListener(id, name));
//        newsfeed.setOnClickListener(new NewFeedListener(id, name));
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
            Intent intent = new Intent(getActivity(), TaggedFriendActivity.class);
            intent.putExtra("id", this.val$id);
            intent.putExtra("title", "Tagged Video");
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
            Intent intent = new Intent(getActivity(), UploadedFriendActivity.class);
            intent.putExtra("id", this.val$id);
            intent.putExtra("title", "Uploaded Video");
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
            Intent intent = new Intent(getActivity(), TimelineFriendActivity.class);
            alertDialog.cancel();
            intent.putExtra("id", this.val$id);
            intent.putExtra("title", "Timeline Video");
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
        if (this.cd.isConnectingToInternet()) {
            fbProgress.smoothToShow();
            this.fb_user_group_url = "https://graph.facebook.com/v2.8/me/friends?&access_token=";
            this.fb_user_group_url += this.token;
            this.fb_user_group_url += "&fields=cover,id,name,picture.type(large),gender&limit=20";
            addToRequestQueue(new JsonObjectRequest(0, fb_user_group_url, null, new Response.Listener<JSONObject>() {
                public void onResponse(JSONObject response) {
                    if (response != null) {
                        try {
                            JSONArray jsonArray = response.getJSONArray(TAG_DATA);

                            if (jsonArray.length() == 0) {
                                tvNoFriend.setVisibility(View.VISIBLE);
                                listView.removeFooterView(rLayout);
                                fbProgress.smoothToHide();
                                return;
                            }

                            int i = 0;
                            while (i < jsonArray.length()) {
                                try {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    MyFriend friend = new MyFriend();
                                    friend.id = jsonObject.getString("id");
                                    friend.name = jsonObject.getString("name");
                                    if (jsonObject.has("gender")) {
                                        friend.gender = jsonObject.getString("gender");
                                    }
                                    if (jsonObject.has("picture")) {
                                        friend.picture = jsonObject.getJSONObject("picture").getJSONObject(TAG_DATA).getString("url");
                                    }
                                    if (jsonObject.has("cover")) {
                                        friend.cover = jsonObject.getJSONObject("cover").getString("source");
                                    }
                                    friendArrayList.add(friend);
                                    i++;
                                } catch (JSONException e2) {
                                    e2.printStackTrace();
                                }
                            }
                            try {
                                JSONObject jsonObject = response.getJSONObject(TAG_DATA_Next);
                                if (jsonObject.isNull("next")) {
                                    nextpage_url = null;
                                    flag_loadingagain = true;
                                } else {
                                    nextpage_url = jsonObject.getString("next");
                                }
                            } catch (Exception e3) {
                                e3.printStackTrace();
                            }

                            if (friendArrayList.size() > 0) {
                                Collections.sort(friendArrayList, new FriendComparator());
                                friendVideoAdapter = new FriendVideoAdapter(getActivity(), friendArrayList);
                                listView.setAdapter(friendVideoAdapter);
                                listView.setSelectionFromTop(listView.getFirstVisiblePosition(), 0);
                            }
                            fbProgress.smoothToHide();
                            return;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, new Response.ErrorListener() {
                public void onErrorResponse(VolleyError error) {
                    fbProgress.smoothToHide();
                    Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
                    tvNoFriend.setVisibility(View.VISIBLE);
                }
            }));
        } else {
            toolsHelper.showAlertDialog(getActivity(), getResources().getString(R.string.network_connection),
                    getResources().getString(R.string.not_connection), Boolean.valueOf(false));

        }
    }

    private void reloadData() {

        if (nextpage_url == null) {
            Toast.makeText(getActivity(), getResources().getString(R.string.no_more_friend), Toast.LENGTH_SHORT).show();
            listView.removeFooterView(this.rLayout);
        } else if (this.cd.isConnectingToInternet()) {
            fbProgress.smoothToShow();
            addToRequestQueue(new JsonObjectRequest(0, nextpage_url, null, new Response.Listener<JSONObject>() {
                public void onResponse(JSONObject response) {
                    if (response != null) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("data");
                            Log.e("FRIEND_JSON_ARRAY", jsonArray.toString());
                            if (nextpage_url.contains("Futerox")) {
                                fbProgress.smoothToHide();
                                Toast.makeText(getActivity(), getResources().getString(R.string.no_friend_found), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                MyFriend friend = new MyFriend();
                                friend.id = jsonObject.getString("id");
                                friend.name = jsonObject.getString("name");
                                if (jsonObject.has("gender")) {
                                    friend.gender = jsonObject.getString("gender");
                                }
                                if (jsonObject.has("picture")) {
                                    friend.picture = jsonObject.getJSONObject("picture").getJSONObject(TAG_DATA).getString("url");
                                }
                                if (jsonObject.has("cover")) {
                                    friend.cover = jsonObject.getJSONObject("cover").getString("source");
                                }
                                friendArrayList.add(friend);
                            }

                            JSONObject pagenextobject = response.getJSONObject("paging");
                            if (pagenextobject.has("next")) {
                                nextpage_url = pagenextobject.getString("next");
                            } else {
                                nextpage_url = null;
                                flag_loadingagain = true;
                            }

                            if (friendArrayList.size() > 0) {
                                tvNoFriend.setVisibility(View.INVISIBLE);
                                Collections.sort(friendArrayList, new FriendComparator());
                            } else {
                                tvNoFriend.setVisibility(View.VISIBLE);
                            }

                            if (friendVideoAdapter != null && friendArrayList.size() > 0) {
                                friendVideoAdapter.notifyDataSetChanged();
                            } else {
                                friendVideoAdapter = new FriendVideoAdapter(getActivity(), friendArrayList);
                                listView.setAdapter(friendVideoAdapter);
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
                    Toast.makeText(getActivity(), getResources().getString(R.string.no_friend_found), Toast.LENGTH_SHORT).show();
                }
            }, new Response.ErrorListener() {
                public void onErrorResponse(VolleyError error) {
                    fbProgress.smoothToHide();
                    Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }));
        }
    }

}
