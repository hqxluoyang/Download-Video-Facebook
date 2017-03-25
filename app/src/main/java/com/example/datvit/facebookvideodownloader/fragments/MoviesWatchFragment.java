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
import com.example.datvit.facebookvideodownloader.activities.PageVideoActivity;
import com.example.datvit.facebookvideodownloader.adapters.MovieWatchedAdapter;
import com.example.datvit.facebookvideodownloader.models.MyMovie;
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

public class MoviesWatchFragment extends Fragment {

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
    private ArrayList<MyMovie> movieArrayList;
    private MovieWatchedAdapter movieWatchedAdapter;
    private RelativeLayout rLayout;
    private AVLoadingIndicatorView loderprogress;
    private AVLoadingIndicatorView fbProgress;
    private TextView tvNoMovie;

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


    public MoviesWatchFragment() {
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
            Log.e("TIMELINE_TOKEN", token);
            Log.e("TIMELINE_ID", user_id);
        }
        this.cd = new ConnectionDetector(getActivity());
        this.toolsHelper = new ToolsHelper();
        movieArrayList = new ArrayList<>();
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
        tvNoMovie = (TextView) view.findViewById(R.id.tvNoMovie);
        fbProgress = (AVLoadingIndicatorView) view.findViewById(R.id.fbProgress);
        tvNoMovie.setVisibility(View.INVISIBLE);

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
                    Intent intent = new Intent(getActivity(), PageVideoActivity.class);
                    intent.putExtra("page_id", movieArrayList.get(i).id);
                    intent.putExtra("page_name", movieArrayList.get(i).title);
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
            this.fb_user_group_url = "https://graph.facebook.com/v2.2/me/video.watches?&access_token=";
            this.fb_user_group_url += this.token;
            this.fb_user_group_url += "&limit=20";
            addToRequestQueue(new JsonObjectRequest(0, fb_user_group_url, null, new Response.Listener<JSONObject>() {
                public void onResponse(JSONObject response) {
                    if (response != null) {
                        try {
                            JSONArray jsonArray = response.getJSONArray(TAG_DATA);

                            if (jsonArray.length() == 0) {
                                tvNoMovie.setVisibility(View.VISIBLE);
                                listView.removeFooterView(rLayout);
                                fbProgress.smoothToHide();
                                return;
                            }

                            int i = 0;
                            while (i < jsonArray.length()) {
                                try {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    MyMovie myMovie = new MyMovie();
                                    if (jsonObject.has("end_time")) {
                                        myMovie.end_time = toolsHelper.getFormattedTime(jsonObject.getString("end_time"));
                                    }
                                    if (jsonObject.has("start_time")) {
                                        myMovie.start_time = toolsHelper.getFormattedTime(jsonObject.getString("start_time"));
                                    }


                                    if (jsonObject != null && jsonObject.has("data")) {
                                        JSONObject jsonObject2 = jsonObject.getJSONObject("data");

                                        if (jsonObject2 != null) {
                                            if (jsonObject2.has("movie")) {
                                                JSONObject jsonObject3 = jsonObject2.getJSONObject("movie");
                                                if (jsonObject3 != null) {
                                                    Log.e("MOVIE", jsonObject3.toString());
                                                    if (jsonObject3.has("id")) {
                                                        myMovie.id = jsonObject3.getString("id");
                                                    }
                                                    if (jsonObject3.has("title")) {
                                                        myMovie.title = jsonObject3.getString("title");
                                                    }
                                                    if (jsonObject3.has("id")) {
                                                        myMovie.type = jsonObject3.getString("type");
                                                    }
                                                    if (jsonObject3.has("id")) {
                                                        myMovie.url = jsonObject3.getString("url");
                                                    }
                                                }
                                            } else if (jsonObject2.has("tv_show")) {
                                                JSONObject jsonObject3 = jsonObject2.getJSONObject("tv_show");

                                                if (jsonObject3 != null) {
                                                    Log.e("TV_SHOW", jsonObject3.toString());
                                                    if (jsonObject3.has("id")) {
                                                        myMovie.id = jsonObject3.getString("id");
                                                    }
                                                    if (jsonObject3.has("title")) {
                                                        myMovie.title = jsonObject3.getString("title");
                                                    }
                                                    if (jsonObject3.has("id")) {
                                                        myMovie.type = jsonObject3.getString("type");
                                                    }
                                                    if (jsonObject3.has("id")) {
                                                        myMovie.url = jsonObject3.getString("url");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    movieArrayList.add(myMovie);
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

                            if (movieArrayList.size() > 0) {
                                movieWatchedAdapter = new MovieWatchedAdapter(getActivity(), movieArrayList);
                                listView.setAdapter(movieWatchedAdapter);
                                listView.setSelectionFromTop(listView.getFirstVisiblePosition(), 0);
                            }
                            fbProgress.smoothToHide();
                            return;
                        } catch (JSONException e) {
                            e.printStackTrace();
                            fbProgress.smoothToHide();
                        }
                    }
                }
            }, new Response.ErrorListener() {
                public void onErrorResponse(VolleyError error) {
                    fbProgress.smoothToHide();
                    Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
                    tvNoMovie.setVisibility(View.VISIBLE);
                }
            }));
        } else {
            toolsHelper.showAlertDialog(getActivity(), getResources().getString(R.string.network_connection),
                    getResources().getString(R.string.not_connection), Boolean.valueOf(false));

        }
    }

    private void reloadData() {

        if (nextpage_url == null) {
            Toast.makeText(getActivity(), getResources().getString(R.string.no_more_movie), Toast.LENGTH_SHORT).show();
            listView.removeFooterView(this.rLayout);
        } else if (this.cd.isConnectingToInternet()) {
            fbProgress.smoothToShow();
            addToRequestQueue(new JsonObjectRequest(0, nextpage_url, null, new Response.Listener<JSONObject>() {
                public void onResponse(JSONObject response) {
                    if (response != null) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("data");
                            Log.e("TIMELINE_JSON_ARRAY", jsonArray.toString());
                            if (nextpage_url.contains("Futerox")) {
                                Toast.makeText(getActivity(), getResources().getString(R.string.no_movie_found), Toast.LENGTH_SHORT).show();
                                fbProgress.smoothToHide();
                                return;
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                MyMovie myMovie = new MyMovie();
                                if (jsonObject.has("end_time")) {
                                    myMovie.end_time = toolsHelper.getFormattedTime(jsonObject.getString("end_time"));
                                }
                                if (jsonObject.has("start_time")) {
                                    myMovie.start_time = toolsHelper.getFormattedTime(jsonObject.getString("start_time"));
                                }


                                if (jsonObject != null && jsonObject.has("data")) {
                                    JSONObject jsonObject2 = jsonObject.getJSONObject("data");

                                    if (jsonObject2 != null) {
                                        if (jsonObject2.has("movie")) {
                                            JSONObject jsonObject3 = jsonObject2.getJSONObject("movie");
                                            if (jsonObject3 != null) {
                                                Log.e("MOVIE", jsonObject3.toString());
                                                if (jsonObject3.has("id")) {
                                                    myMovie.id = jsonObject3.getString("id");
                                                }
                                                if (jsonObject3.has("title")) {
                                                    myMovie.title = jsonObject3.getString("title");
                                                }
                                                if (jsonObject3.has("id")) {
                                                    myMovie.type = jsonObject3.getString("type");
                                                }
                                                if (jsonObject3.has("id")) {
                                                    myMovie.url = jsonObject3.getString("url");
                                                }
                                            }
                                        } else if (jsonObject2.has("tv_show")) {
                                            JSONObject jsonObject3 = jsonObject2.getJSONObject("tv_show");

                                            if (jsonObject3 != null) {
                                                Log.e("TV_SHOW", jsonObject3.toString());
                                                if (jsonObject3.has("id")) {
                                                    myMovie.id = jsonObject3.getString("id");
                                                }
                                                if (jsonObject3.has("title")) {
                                                    myMovie.title = jsonObject3.getString("title");
                                                }
                                                if (jsonObject3.has("id")) {
                                                    myMovie.type = jsonObject3.getString("type");
                                                }
                                                if (jsonObject3.has("id")) {
                                                    myMovie.url = jsonObject3.getString("url");
                                                }
                                            }
                                        }
                                    }
                                }
                                movieArrayList.add(myMovie);
                            }

                            JSONObject pagenextobject = response.getJSONObject("paging");
                            if (pagenextobject.has("next")) {
                                nextpage_url = pagenextobject.getString("next");
                            } else {
                                nextpage_url = null;
                                flag_loadingagain = true;
                            }

                            if (movieArrayList.size() > 0) {
                                tvNoMovie.setVisibility(View.INVISIBLE);

                            } else {
                                tvNoMovie.setVisibility(View.VISIBLE);
                            }

                            if (movieWatchedAdapter != null && movieArrayList.size() > 0) {
                                movieWatchedAdapter.notifyDataSetChanged();
                            } else {
                                movieWatchedAdapter = new MovieWatchedAdapter(getActivity(), movieArrayList);
                                listView.setAdapter(movieWatchedAdapter);
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
                    Toast.makeText(getActivity(), getResources().getString(R.string.no_movie_found), Toast.LENGTH_SHORT).show();
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
