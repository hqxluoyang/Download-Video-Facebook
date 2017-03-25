package com.example.datvit.facebookvideodownloader.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
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
import com.example.datvit.facebookvideodownloader.activities.GroupVideoActivity;
import com.example.datvit.facebookvideodownloader.activities.MainActivity;
import com.example.datvit.facebookvideodownloader.adapters.FriendVideoAdapter;
import com.example.datvit.facebookvideodownloader.adapters.GroupsVideoAdapter;
import com.example.datvit.facebookvideodownloader.adapters.MovieWatchedAdapter;
import com.example.datvit.facebookvideodownloader.models.MyFriend;
import com.example.datvit.facebookvideodownloader.models.MyGroup;
import com.example.datvit.facebookvideodownloader.models.MyMovie;
import com.example.datvit.facebookvideodownloader.utils.ConnectionDetector;
import com.example.datvit.facebookvideodownloader.utils.FacebookSession;
import com.example.datvit.facebookvideodownloader.utils.ToolsHelper;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by DatVIT on 11/12/2016.
 */

public class ListGroupFragment extends Fragment {

    private String BASE_URL;
    private String FRIENDS_URL;
    private GroupsVideoAdapter adapter;
    private AsyncHttpClient client;
    private String cookies;
    private int counter;
    private ArrayList<MyGroup> groupArrayList;
    private ListView lv;
    private TextView tvNoGroup;
    private RequestQueue mRequestQueue;
    private ConnectionDetector cd;
    private String group_id;
    private String fb_user_group_url;
    private String token;
    private ToolsHelper toolsHelper;
    private AVLoadingIndicatorView avLoadingIndicatorView;
    private FacebookSession facebookSession;
    public static final String TAG = "VolleyPatterns";

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


    public ListGroupFragment() {
        this.counter = 0;
        this.FRIENDS_URL = "https://m.facebook.com/groups/?ref_component=mbasic_bookmark&ref_page=XMenuController";
        this.BASE_URL = "https://m.facebook.com";

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        this.toolsHelper = new ToolsHelper();
        cd = new ConnectionDetector(getActivity());
        facebookSession = new FacebookSession(getActivity());
        if (facebookSession != null && facebookSession.getToken() != null) {
            this.token = facebookSession.getToken();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_video, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.lv = (ListView) view.findViewById(R.id.listGroup);
        this.tvNoGroup = (TextView) view.findViewById(R.id.tvNoGroup);
        this.avLoadingIndicatorView = (AVLoadingIndicatorView) view.findViewById(R.id.fbProgress);
        this.groupArrayList = new ArrayList();
        this.client = new AsyncHttpClient();

        avLoadingIndicatorView.smoothToShow();

        if (token != null) {
            executeUrl(FRIENDS_URL);
        } else {
            Toast.makeText(getActivity(), getResources().getString(R.string.please_login_fb), Toast.LENGTH_SHORT).show();
        }

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getActivity(), GroupVideoActivity.class);
                intent.putExtra("group_id", groupArrayList.get(i).id);
                intent.putExtra("group_name", groupArrayList.get(i).name);
                intent.putExtra("access_token", token);
                startActivity(intent);
            }
        });
    }

    private void executeUrl(final String group_url) {
        Log.e("LIST_GROUPS", "this is friends url : " + group_url);
        this.cookies = CookieManager.getInstance().getCookie(group_url);
        this.client.addHeader("Cookie", this.cookies);
        this.client.addHeader("user-agent", "Mozilla/5.0 (Linux; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0");
        this.client.get(group_url, new AsyncHttpResponseHandler() {
            public void onFailure(int arg0, Header[] header, byte[] response, Throwable arg3) {
            }

            @Override
            public void onStart() {
                super.onStart();
                avLoadingIndicatorView.smoothToShow();
            }

            @Override
            public void onFinish() {
                super.onFinish();
                avLoadingIndicatorView.smoothToHide();
            }

            public void onSuccess(int arg0, Header[] header, byte[] response) {
                try {
                    Element element;
                    Document document = Jsoup.parse(new String(response));
                    Iterator it = document.select("a[href*=/groups/]").iterator();
                    while (it.hasNext()) {
                        element = (Element) it.next();
                        String text = element.text();
                        String attr = element.attr("href");
                        Log.e("LIST_GROUPS", "ELEMENT : " + element.outerHtml());
                        element = element.parent().nextElementSibling();
                        if (!((element != null && element.select("a").attr("href").contains("/group/join/")) || text == null || text.length() <= 0 || text.contains("Create New Group") || text.contains("See More Groups"))) {
                            group_id = attr.substring(attr.lastIndexOf("/") + 1, attr.indexOf("?"));
                            Log.e("LIST_GROUPS", "NAME : " + text + " - " + "ID: " + attr.substring(attr.lastIndexOf("/") + 1, attr.indexOf("?")));
                            if (group_id != null && !group_id.isEmpty()) {
                                getGroup(group_id);
                            }

                        }
                    }
                    Iterator it2 = document.select("div#m_more_item a").iterator();
                    while (it2.hasNext()) {
                        element = (Element) it2.next();
                        String attr2 = element.attr("href");
                        Log.e("LIST_GROUPS", "HREF : " + attr2 + " - " + element.text());
                        if (element.text().equals("See More Groups") || element.text().equals("Xem tất cả")) {
                            executeUrl(BASE_URL + attr2);
                            return;
                        }
                    }
                } catch (Exception e) {
                }
            }
        });
    }

    class GroupComparator implements Comparator<MyGroup> {

        public int compare(MyGroup group1, MyGroup group2) {
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

    private void getGroup(String id) {
        this.fb_user_group_url = "https://graph.facebook.com/v2.8/" + id +
                "?fields=cover,picture,description,icon,name,owner,updated_time,id"
                + "&access_token=" + token;
        Log.e("LIST_GROUPS", fb_user_group_url);
        addToRequestQueue(new JsonObjectRequest(0, fb_user_group_url, null, new Response.Listener<JSONObject>() {
            public void onResponse(JSONObject response) {
                if (response != null) {
                    JSONObject jsonObject = response;
                    if (jsonObject != null) {
                        try {
                            MyGroup myGroup = new MyGroup();
                            if (jsonObject.has("id")) {
                                myGroup.id = jsonObject.getString("id");
                            }
                            if (jsonObject.has("name")) {
                                myGroup.name = jsonObject.getString("name");
                            }
                            if (jsonObject.has("updated_time")) {
                                myGroup.updated_time = toolsHelper.getFormattedTime(jsonObject.getString("updated_time"));
                            }
                            if (jsonObject.has("updated_time")) {
                                myGroup.time = jsonObject.getString("updated_time");
                            }
                            if (jsonObject.has("picture")) {
                                myGroup.picture = jsonObject.getJSONObject("picture").getJSONObject("data").getString("url");
                            }
                            if (jsonObject.has("cover")) {
                                myGroup.cover = jsonObject.getJSONObject("cover").getString("source");
                            }
                            if (jsonObject.has("owner")) {
                                myGroup.owner = jsonObject.getJSONObject("owner").getString("name");
                            }
                            if (jsonObject.has("description")) {
                                myGroup.description = jsonObject.getString("description");
                            }
                            if (jsonObject.has("icon")) {
                                myGroup.icon = jsonObject.getString("icon");
                            }
                            groupArrayList.add(myGroup);

                            counter++;

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    avLoadingIndicatorView.smoothToHide();
                                    Collections.sort(groupArrayList, new GroupComparator());
                                    if (groupArrayList != null && groupArrayList.size() > 0) {
                                        tvNoGroup.setVisibility(View.INVISIBLE);
                                        adapter = new GroupsVideoAdapter(getActivity(), groupArrayList);
                                        lv.setAdapter(adapter);
                                    } else {
                                        tvNoGroup.setVisibility(View.VISIBLE);
                                    }

                                }
                            });

                            Log.e("LIST_GROUPS", "COUNT : " + counter);

                        } catch (JSONException e) {
                            avLoadingIndicatorView.smoothToHide();
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                avLoadingIndicatorView.smoothToHide();
//                Toast.makeText(getActivity(), "ErrorResponse: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }));
    }
}
