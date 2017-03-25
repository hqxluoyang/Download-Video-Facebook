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
import android.widget.LinearLayout;
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
import com.example.datvit.facebookvideodownloader.activities.FunnyVideoActivity;
import com.example.datvit.facebookvideodownloader.activities.PageVideoActivity;
import com.example.datvit.facebookvideodownloader.adapters.PagesVideoAdapter;
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

public class FunnyPageFragment extends Fragment {


    private String user_id;
    private String token;
    private LinearLayout pageFunny, pageBest, pageCrazy, pageCraziest, pageKeek, pageBestViral, pageAmazing;

    private ConnectionDetector cd;
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

    public FunnyPageFragment() {
        cd = null;
        toolsHelper = null;
        facebookSession = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();

        facebookSession = new FacebookSession(getActivity());
        if (facebookSession.getToken() != null) {
            token = facebookSession.getToken();
        }if (facebookSession.getUserId() != null) {
            user_id = facebookSession.getUserId();
        }

        if (token != null) {
            Log.e("FUNNY_TOKEN", token);
            Log.e("FUNNY_ID", user_id);
        }
        this.cd = new ConnectionDetector(getActivity());
        this.toolsHelper = new ToolsHelper();


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_page_funny, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pageFunny = (LinearLayout) view.findViewById(R.id.pageFunny);
        pageAmazing = (LinearLayout) view.findViewById(R.id.pageAmazing);
        pageBest = (LinearLayout) view.findViewById(R.id.pageBest);
        pageBestViral = (LinearLayout) view.findViewById(R.id.pageBestViral);
        pageCraziest = (LinearLayout) view.findViewById(R.id.pageCraziest);
        pageCrazy = (LinearLayout) view.findViewById(R.id.pageCrazy);
        pageKeek = (LinearLayout) view.findViewById(R.id.pageKeek);

        pageCrazy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), FunnyVideoActivity.class);
                intent.putExtra("page", "Crazy Videos");
                startActivity(intent);
            }
        });

        pageAmazing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), FunnyVideoActivity.class);
                intent.putExtra("page", "Amazing Videos");
                startActivity(intent);
            }
        });

        pageKeek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), FunnyVideoActivity.class);
                intent.putExtra("page", "Keek Videos");
                startActivity(intent);
            }
        });

        pageCraziest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), FunnyVideoActivity.class);
                intent.putExtra("page", "The Craziest Videos");
                startActivity(intent);
            }
        });

        pageBestViral.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), FunnyVideoActivity.class);
                intent.putExtra("page", "Best Viral Videos");
                startActivity(intent);
            }
        });

        pageBest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), FunnyVideoActivity.class);
                intent.putExtra("page", "Best Videos");
                startActivity(intent);
            }
        });

        pageFunny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), FunnyVideoActivity.class);
                intent.putExtra("page", "Funny Videos");
                startActivity(intent);
            }
        });

    }

}
