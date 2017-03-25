package com.example.datvit.facebookvideodownloader.fragments;

import android.app.Activity;
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
import com.example.datvit.facebookvideodownloader.activities.GroupVideoActivity;
import com.example.datvit.facebookvideodownloader.activities.PageVideoActivity;
import com.example.datvit.facebookvideodownloader.adapters.GroupsVideoAdapter;
import com.example.datvit.facebookvideodownloader.adapters.PagesVideoAdapter;
import com.example.datvit.facebookvideodownloader.models.MyGroup;
import com.example.datvit.facebookvideodownloader.models.MyPage;
import com.example.datvit.facebookvideodownloader.utils.ConnectionDetector;
import com.example.datvit.facebookvideodownloader.utils.FacebookSession;
import com.example.datvit.facebookvideodownloader.utils.ToolsHelper;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by DatVIT on 11/15/2016.
 */

public class GroupVideoFragment extends Fragment {

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

    private ListView listView;
    private ArrayList<MyGroup> groupArrayList;
    private GroupsVideoAdapter groupsVideoAdapter;
    private RelativeLayout rLayout;
    private AVLoadingIndicatorView loderprogress;
    private AVLoadingIndicatorView fbProgress;
    private TextView tvNoPage;

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


    public GroupVideoFragment() {
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
            Log.e("GROUP_TOKEN", token);
            Log.e("GROUP_ID", user_id);
        }
        this.cd = new ConnectionDetector(getActivity());
        this.toolsHelper = new ToolsHelper();
        groupArrayList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_video, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = (ListView) view.findViewById(R.id.listGroup);
        tvNoPage = (TextView) view.findViewById(R.id.tvNoGroup);
        fbProgress = (AVLoadingIndicatorView) view.findViewById(R.id.fbProgress);
        fbProgress.setVisibility(View.VISIBLE);
        tvNoPage.setVisibility(View.INVISIBLE);

        rLayout = (RelativeLayout) getActivity().getLayoutInflater().inflate(R.layout.footer_view_list, null);
        loderprogress = (AVLoadingIndicatorView) rLayout.findViewById(R.id.loadingprogress);
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
                if (i < groupArrayList.size()) {
                    Intent intent = new Intent(getActivity(), GroupVideoActivity.class);
                    intent.putExtra("group_id", groupArrayList.get(i).id);
                    intent.putExtra("group_name", groupArrayList.get(i).name);
                    intent.putExtra("access_token", token);
                    startActivity(intent);
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
        if (this.cd.isConnectingToInternet()) {
            fbProgress.smoothToShow();
            this.fb_user_group_url = "https://graph.facebook.com/v2.2/me/groups?access_token=";
            this.fb_user_group_url += this.token;
            this.fb_user_group_url += "&fields=cover,picture.type(large),description,icon,name,owner,updated_time,id&limit=20";
            addToRequestQueue(new JsonObjectRequest(0, fb_user_group_url, null, new Response.Listener<JSONObject>() {
                public void onResponse(JSONObject response) {
                    if (response != null) {
                        try {
                            JSONArray jsonArray = response.getJSONArray(TAG_DATA);

                            if (jsonArray.length() == 0) {
                                tvNoPage.setVisibility(View.VISIBLE);
                                listView.removeFooterView(rLayout);
                                fbProgress.smoothToHide();
                                return;
                            }

                            int i = 0;
                            while (i < jsonArray.length()) {
                                try {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
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

                            if (groupArrayList.size() > 0) {
                                groupsVideoAdapter = new GroupsVideoAdapter(getActivity(), groupArrayList);
                                listView.setAdapter(groupsVideoAdapter);
                                listView.setSelectionFromTop(listView.getFirstVisiblePosition(), 0);
                            }
                            fbProgress.smoothToHide();
                            return;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    fbProgress.smoothToHide();
                }
            }, new Response.ErrorListener() {
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
                    tvNoPage.setVisibility(View.VISIBLE);
                    fbProgress.smoothToHide();
                }
            }));
        } else {
            toolsHelper.showAlertDialog(getActivity(), getResources().getString(R.string.network_connection),
                    getResources().getString(R.string.not_connection), Boolean.valueOf(false));

        }
    }

    private void reloadData() {

        if (nextpage_url == null) {
            Toast.makeText(getActivity(), getResources().getString(R.string.no_more_group), Toast.LENGTH_SHORT).show();
            listView.removeFooterView(this.rLayout);
        } else if (this.cd.isConnectingToInternet()) {
            fbProgress.smoothToShow();
            addToRequestQueue(new JsonObjectRequest(0, nextpage_url, null, new Response.Listener<JSONObject>() {
                public void onResponse(JSONObject response) {
                    if (response != null) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("data");
                            Log.e("GROUP_JSON_ARRAY", jsonArray.toString());
                            if (nextpage_url.contains("Futerox")) {
                                fbProgress.smoothToHide();
                                Toast.makeText(getActivity(), getResources().getString(R.string.no_more_group), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                MyGroup myGroup = new MyGroup();
                                if (jsonObject.has("id")) {
                                    myGroup.id = jsonObject.getString("id");
                                }
                                if (jsonObject.has("name")) {
                                    myGroup.name = jsonObject.getString("name");
                                } else {
                                    myGroup.name = "Group untitled";
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
                            }

                            JSONObject pagenextobject = response.getJSONObject("paging");
                            if (pagenextobject.has("next")) {
                                nextpage_url = pagenextobject.getString("next");
                            } else {
                                nextpage_url = null;
                                flag_loadingagain = true;
                            }

                            if (groupArrayList.size() > 0) {
                                tvNoPage.setVisibility(View.INVISIBLE);
                            } else {
                                tvNoPage.setVisibility(View.VISIBLE);
                            }

                            if (groupsVideoAdapter != null && groupArrayList.size() > 0) {
                                groupsVideoAdapter.notifyDataSetChanged();
                            } else {
                                groupsVideoAdapter = new GroupsVideoAdapter(getActivity(), groupArrayList);
                                listView.setAdapter(groupsVideoAdapter);
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
                    Toast.makeText(getActivity(), getResources().getString(R.string.no_more_group), Toast.LENGTH_SHORT).show();
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
