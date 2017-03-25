package com.example.datvit.facebookvideodownloader.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.fragments.BrowserFragment;
import com.example.datvit.facebookvideodownloader.fragments.FunnyPageFragment;
import com.example.datvit.facebookvideodownloader.fragments.GroupVideoFragment;
import com.example.datvit.facebookvideodownloader.fragments.ListFriendFragment;
import com.example.datvit.facebookvideodownloader.fragments.ListFriendVideoFragment;
import com.example.datvit.facebookvideodownloader.fragments.ListPageVideoFragment;
import com.example.datvit.facebookvideodownloader.fragments.MainFragment;
import com.example.datvit.facebookvideodownloader.fragments.MoviesFragment;
import com.example.datvit.facebookvideodownloader.fragments.MoviesWatchFragment;
import com.example.datvit.facebookvideodownloader.fragments.NewFeedFragment;
import com.example.datvit.facebookvideodownloader.fragments.PostVideoFragment;
import com.example.datvit.facebookvideodownloader.fragments.SearchVideoFragment;
import com.example.datvit.facebookvideodownloader.fragments.SettingFragment;
import com.example.datvit.facebookvideodownloader.fragments.TimelineVideoFragment;
import com.example.datvit.facebookvideodownloader.fragments.VideoDownloadedFragment;
import com.example.datvit.facebookvideodownloader.fragments.VideoLikedFragment;
import com.example.datvit.facebookvideodownloader.fragments.VideoSavedFragment;
import com.example.datvit.facebookvideodownloader.fragments.VideoSharedFragment;
import com.example.datvit.facebookvideodownloader.fragments.VideoTaggedFragment;
import com.example.datvit.facebookvideodownloader.fragments.VideoUploadFragment;
import com.example.datvit.facebookvideodownloader.fragments.VideoWatchedFragment;
import com.example.datvit.facebookvideodownloader.utils.ConstantHelper;
import com.example.datvit.facebookvideodownloader.utils.CustomDialog;
import com.example.datvit.facebookvideodownloader.utils.FacebookSession;
import com.example.datvit.facebookvideodownloader.utils.ToolsHelper;
import com.facebook.AccessToken;
import com.squareup.picasso.Picasso;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private int cur_fragment = ConstantHelper.MAIN_FRAGMENT;
    public static final String URL_FB = "https://m.facebook.com/?stype=lo&jlou=";
    public static final String URL_FB_1 = "smuh=26909";

    private DrawerLayout drawer;
    private Toolbar toolbar;
    public static NavigationView navigationView;
    private FacebookSession facebookSession;

    public static String profile_name = "", profile_id = "", profile_avatar = null,
            token = null, profile_email = "", profile_cover = null;
    public static ImageView ivAvatar;
    private int method_login = 0;
    private SharedPreferences loginState;
    private SharedPreferences.Editor loginStateEditor;
    private BrowserFragment browserFragment;
    private SharedPreferences prefs;


    private Locale myLocale;

    public void loadLocale() {
        String langPref = "Language";
        SharedPreferences prefs = getSharedPreferences("GPVideo", Activity.MODE_PRIVATE);
        String language = prefs.getString(langPref, "en");
        myLocale = new Locale(language);
        Locale.setDefault(myLocale);
        Configuration config = new Configuration();
        config.locale = myLocale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        setContentView(R.layout.main);

        facebookSession = new FacebookSession(this);
        loginState = getSharedPreferences("LOGIN_STATE", Context.MODE_PRIVATE);
        loginStateEditor = this.loginState.edit();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.nav_open_drawer, R.string.nav_close_drawer);
        drawer.setDrawerListener(toggle);
        drawer.setStatusBarBackgroundColor(getResources().getColor(R.color.colorSelect));
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        method_login = loginState.getInt("login_method", 0);
        Intent intent = getIntent();
        if (intent != null) {
            method_login = intent.getIntExtra("method_login", 0);
        }

        setUpInfo();

        if (savedInstanceState == null) {
            if (method_login == 2) {
                toolbar.setTitle(getResources().getString(R.string.browser));
                cur_fragment = ConstantHelper.FRAGMENT_EXPLORER;
                browserFragment = new BrowserFragment();
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
                transaction.replace(R.id.frameContainer, browserFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            } else {
                toolbar.setTitle(getResources().getString(R.string.app_name));
                cur_fragment = ConstantHelper.MAIN_FRAGMENT;
                Fragment fragment = new MainFragment();
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
                transaction.replace(R.id.frameContainer, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        }
    }

    private void setUpInfo() {
        profile_name = facebookSession.getUsername();
        profile_id = facebookSession.getUserId();
        profile_email = facebookSession.getEmail();
        token = facebookSession.getToken();
        profile_cover = facebookSession.getUserCover();
        profile_avatar = facebookSession.getUserPicture();

        View view = navigationView.getHeaderView(0);
        ivAvatar = (ImageView) view.findViewById(R.id.profile_image);
        LinearLayout layoutCover = (LinearLayout) view.findViewById(R.id.profile_picture);
        TextView tvName = (TextView) view.findViewById(R.id.profile_name);
        TextView tvId = (TextView) view.findViewById(R.id.profile_user_id);

        if (profile_avatar != null) {
            Picasso.with(MainActivity.this).load(profile_avatar)
                    .error(R.drawable.silhouette).into(ivAvatar);
        }
        if (profile_name != null) {
            tvName.setText(profile_name);
        }

        if (profile_email != null) {
            tvId.setText(profile_email);
        }

        ivAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toolbar.setTitle(getResources().getString(R.string.app_name));
                exchangeFragment(ConstantHelper.MAIN_FRAGMENT);
                drawer.closeDrawer(GravityCompat.START);
            }
        });
//        if (profile_cover != null && !profile_cover.isEmpty()) {
//            Log.e("USER_COVER_MAIN", profile_cover);
//            Bitmap bitmap = null;
//            try {
//                bitmap = new AsyncGettingBitmapFromUrl().execute(profile_cover).get();
//
//                if (bitmap != null) {
//                    BitmapDrawable drawableBitmap = new BitmapDrawable(bitmap);
//                    layoutCover.setBackgroundDrawable(drawableBitmap);
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            } catch (ExecutionException e) {
//                e.printStackTrace();
//            }
//            tvName.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
//            tvId.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
//
//        } else {
//            ivAvatar.setImageResource(R.drawable.silhouette);
//            layoutCover.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
//            tvName.setTextColor(getResources().getColor(R.color.colorWhite));
//            tvId.setTextColor(getResources().getColor(R.color.colorWhite));
//        }
    }

    private class AsyncGettingBitmapFromUrl extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = ToolsHelper.downloadImage(params[0]);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            Log.e("BITMAP:", "bitmap: " + bitmap);
        }
    }

    private void exchangeFragment(int id) {
        if (id == ConstantHelper.FRAGMENT_EXPLORER && cur_fragment != ConstantHelper.FRAGMENT_EXPLORER) {
            cur_fragment = ConstantHelper.FRAGMENT_EXPLORER;
            browserFragment = new BrowserFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, browserFragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_WATCHED_MOVIE && cur_fragment != ConstantHelper.FRAGMENT_WATCHED_MOVIE) {
            cur_fragment = ConstantHelper.FRAGMENT_WATCHED_MOVIE;
            Fragment fragment = new MoviesWatchFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_MY_VIDEO && cur_fragment != ConstantHelper.FRAGMENT_MY_VIDEO) {
            cur_fragment = ConstantHelper.FRAGMENT_MY_VIDEO;
            Fragment fragment = new MoviesFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_NEWS_FEED && cur_fragment != ConstantHelper.FRAGMENT_NEWS_FEED) {
            cur_fragment = ConstantHelper.FRAGMENT_NEWS_FEED;
            Fragment fragment = new NewFeedFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_PAGE_VIDEO && cur_fragment != ConstantHelper.FRAGMENT_PAGE_VIDEO) {
            cur_fragment = ConstantHelper.FRAGMENT_PAGE_VIDEO;
            Fragment fragment = new ListPageVideoFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_GROUP_VIDEO && cur_fragment != ConstantHelper.FRAGMENT_GROUP_VIDEO) {
            cur_fragment = ConstantHelper.FRAGMENT_GROUP_VIDEO;
            Fragment fragment = new GroupVideoFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_FRIEND_VIDEO && cur_fragment != ConstantHelper.FRAGMENT_FRIEND_VIDEO) {
            cur_fragment = ConstantHelper.FRAGMENT_FRIEND_VIDEO;
            Fragment fragment = new ListFriendVideoFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRIEND_LIST && cur_fragment != ConstantHelper.FRIEND_LIST) {
            cur_fragment = ConstantHelper.FRIEND_LIST;
            Fragment fragment = new ListFriendFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_FUNNY_VIDEO && cur_fragment != ConstantHelper.FRAGMENT_FUNNY_VIDEO) {
            cur_fragment = ConstantHelper.FRAGMENT_FUNNY_VIDEO;
            Fragment fragment = new FunnyPageFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_TIMELINE_VIDEO && cur_fragment != ConstantHelper.FRAGMENT_TIMELINE_VIDEO) {
            cur_fragment = ConstantHelper.FRAGMENT_TIMELINE_VIDEO;
            Fragment fragment = new TimelineVideoFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_POST_VIDEO && cur_fragment != ConstantHelper.FRAGMENT_POST_VIDEO) {
            cur_fragment = ConstantHelper.FRAGMENT_POST_VIDEO;
            Fragment fragment = new PostVideoFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_VIDEO_UPLOADED && cur_fragment != ConstantHelper.FRAGMENT_VIDEO_UPLOADED) {
            cur_fragment = ConstantHelper.FRAGMENT_VIDEO_UPLOADED;
            Fragment fragment = new VideoUploadFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_VIDEO_TAGGED && cur_fragment != ConstantHelper.FRAGMENT_VIDEO_TAGGED) {
            cur_fragment = ConstantHelper.FRAGMENT_VIDEO_TAGGED;
            Fragment fragment = new VideoTaggedFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_VIDEO_DOWNLOADED && cur_fragment != ConstantHelper.FRAGMENT_VIDEO_DOWNLOADED) {
            cur_fragment = ConstantHelper.FRAGMENT_VIDEO_DOWNLOADED;
            Fragment fragment = new VideoDownloadedFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_WATCHED_VIDEO && cur_fragment != ConstantHelper.FRAGMENT_WATCHED_VIDEO) {
            cur_fragment = ConstantHelper.FRAGMENT_WATCHED_VIDEO;
            Fragment fragment = new VideoWatchedFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_SAVED_VIDEO && cur_fragment != ConstantHelper.FRAGMENT_SAVED_VIDEO) {
            cur_fragment = ConstantHelper.FRAGMENT_SAVED_VIDEO;
            Fragment fragment = new VideoSavedFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_SEARCH_VIDEO && cur_fragment != ConstantHelper.FRAGMENT_SEARCH_VIDEO) {
            cur_fragment = ConstantHelper.FRAGMENT_SEARCH_VIDEO;
            Fragment fragment = new SearchVideoFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_LIKED_VIDEO && cur_fragment != ConstantHelper.FRAGMENT_LIKED_VIDEO) {
            cur_fragment = ConstantHelper.FRAGMENT_LIKED_VIDEO;
            Fragment fragment = new VideoLikedFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_SHARED_VIDEO && cur_fragment != ConstantHelper.FRAGMENT_SHARED_VIDEO) {
            cur_fragment = ConstantHelper.FRAGMENT_SHARED_VIDEO;
            Fragment fragment = new VideoSharedFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.FRAGMENT_SETTING && cur_fragment != ConstantHelper.FRAGMENT_SETTING) {
            cur_fragment = ConstantHelper.FRAGMENT_SETTING;
            Fragment fragment = new SettingFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if (id == ConstantHelper.MAIN_FRAGMENT && cur_fragment != ConstantHelper.MAIN_FRAGMENT) {
            cur_fragment = ConstantHelper.MAIN_FRAGMENT;
            Fragment fragment = new MainFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction.replace(R.id.frameContainer, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        CookieManager.getInstance().removeAllCookie();
//        loginStateEditor.putBoolean("login_state", false);
//        loginStateEditor.putInt("login_method", 0);
//        loginStateEditor.commit();
//        facebookSession.resetAccessToken();
//        AccessToken.setCurrentAccessToken(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        method_login = loginState.getInt("login_method", 0);
    }

    private void changeToolbarTitle(String title) {
        if (this.toolbar != null) {
            this.toolbar.setTitle((CharSequence) title);
        }
    }

    private void exit() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getResources().getString(R.string.quit))
                .setMessage(getResources().getString(R.string.are_you_exit_app));

        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        item.setChecked(false);
        switch (id) {
            case R.id.nav_explorer:
                if (method_login == 2) {
                    changeToolbarTitle(getResources().getString(R.string.browser));
                    exchangeFragment(ConstantHelper.FRAGMENT_EXPLORER);
                    item.setChecked(true);
                } else {
                    loginFacebookWeb();
                }
                break;
            case R.id.nav_watched_video:
                if (method_login == 2 || method_login == 1) {
                    changeToolbarTitle(getResources().getString(R.string.movie_watch));
                    exchangeFragment(ConstantHelper.FRAGMENT_WATCHED_MOVIE);
                    item.setChecked(true);
                } else {
                    loginFacebookWeb();
                }
                break;
            case R.id.nav_my_video:
                if (method_login == 2 || method_login == 1) {
                    changeToolbarTitle(getResources().getString(R.string.movie));
                    exchangeFragment(ConstantHelper.FRAGMENT_MY_VIDEO);
                    item.setChecked(true);
                } else {
                    loginFacebookWeb();
                }
                break;
            case R.id.nav_newfeed:
                if (method_login == 2) {
                    changeToolbarTitle(getResources().getString(R.string.newsfeed));
                    exchangeFragment(ConstantHelper.FRAGMENT_NEWS_FEED);
                    item.setChecked(true);
                } else {
                    loginFacebookWeb();
                }

                break;
            case R.id.nav_watched:
                if (method_login == 2) {
                    if ((loginState.getString("cookie", null) != null)
                            && !loginState.getString("url", null).contains(URL_FB)
                            && !loginState.getString("url", null).contains(URL_FB_1)) {
                        changeToolbarTitle(getResources().getString(R.string.video_watch));
                        exchangeFragment(ConstantHelper.FRAGMENT_WATCHED_VIDEO);
                        item.setChecked(true);
                    } else {
                        final android.app.AlertDialog.Builder alertDialogBuilder =
                                new android.app.AlertDialog.Builder(this);
                        alertDialogBuilder
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle(getResources().getString(R.string.notification))
                                .setMessage(getResources().getString(R.string.login_fb_while_logout));

                        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                changeToolbarTitle(getResources().getString(R.string.browser));
                                exchangeFragment(ConstantHelper.FRAGMENT_EXPLORER);
                            }
                        });
                        android.app.AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }
                } else {
                    loginFacebookWeb();
                }

                break;
            case R.id.nav_saved:
                if (method_login == 2) {
                    if ((loginState.getString("cookie", null) != null)
                            && !loginState.getString("url", null).contains(URL_FB)
                            && !loginState.getString("url", null).contains(URL_FB_1)) {
                        changeToolbarTitle(getResources().getString(R.string.video_save));
                        exchangeFragment(ConstantHelper.FRAGMENT_SAVED_VIDEO);
                        item.setChecked(true);
                    } else {
                        final android.app.AlertDialog.Builder alertDialogBuilder =
                                new android.app.AlertDialog.Builder(this);
                        alertDialogBuilder
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle(getResources().getString(R.string.notification))
                                .setMessage(getResources().getString(R.string.login_fb_while_logout));

                        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                changeToolbarTitle(getResources().getString(R.string.browser));
                                exchangeFragment(ConstantHelper.FRAGMENT_EXPLORER);
                            }
                        });
                        android.app.AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }

                } else {
                    loginFacebookWeb();
                }
                break;
            case R.id.nav_video_liked:
                if (method_login == 2) {
                    if ((loginState.getString("cookie", null) != null)
                            && !loginState.getString("url", null).contains(URL_FB)
                            && !loginState.getString("url", null).contains(URL_FB_1)) {
                        changeToolbarTitle(getResources().getString(R.string.video_like));
                        exchangeFragment(ConstantHelper.FRAGMENT_LIKED_VIDEO);
                        item.setChecked(true);
                    } else {
                        final android.app.AlertDialog.Builder alertDialogBuilder =
                                new android.app.AlertDialog.Builder(this);
                        alertDialogBuilder
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle(getResources().getString(R.string.notification))
                                .setMessage(getResources().getString(R.string.login_fb_while_logout));

                        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                changeToolbarTitle(getResources().getString(R.string.browser));
                                exchangeFragment(ConstantHelper.FRAGMENT_EXPLORER);
                            }
                        });
                        android.app.AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }

                } else {
                    loginFacebookWeb();
                }
                break;
            case R.id.nav_video_shared:
                if (method_login == 2) {
                    if ((loginState.getString("cookie", null) != null)
                            && !loginState.getString("url", null).contains(URL_FB)
                            && !loginState.getString("url", null).contains(URL_FB_1)) {
                        changeToolbarTitle(getResources().getString(R.string.video_share));
                        exchangeFragment(ConstantHelper.FRAGMENT_SHARED_VIDEO);
                        item.setChecked(true);
                    } else {
                        final android.app.AlertDialog.Builder alertDialogBuilder =
                                new android.app.AlertDialog.Builder(this);
                        alertDialogBuilder
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle(getResources().getString(R.string.notification))
                                .setMessage(getResources().getString(R.string.login_fb_while_logout));

                        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                changeToolbarTitle(getResources().getString(R.string.browser));
                                exchangeFragment(ConstantHelper.FRAGMENT_EXPLORER);
                            }
                        });
                        android.app.AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }
                } else {
                    loginFacebookWeb();
                }
                break;
            case R.id.nav_page_video:
                if (method_login == 2 || method_login == 1) {
                    changeToolbarTitle(getResources().getString(R.string.page_video));
                    exchangeFragment(ConstantHelper.FRAGMENT_PAGE_VIDEO);
                    item.setChecked(true);
                } else {
                    loginFacebookWeb();
                }
                break;
            case R.id.nav_group_video:
                if (method_login == 2 || method_login == 1) {
                    changeToolbarTitle(getResources().getString(R.string.group_video));
                    exchangeFragment(ConstantHelper.FRAGMENT_GROUP_VIDEO);
                    item.setChecked(true);
                } else {
                    loginFacebookWeb();
                }

                break;
            case R.id.nav_friend_video:
                if (method_login == 2) {
                    if ((loginState.getString("cookie", null) != null)
                            && !loginState.getString("url", null).contains(URL_FB)
                            && !loginState.getString("url", null).contains(URL_FB_1)) {
                        changeToolbarTitle(getResources().getString(R.string.friend_video));
                        exchangeFragment(ConstantHelper.FRAGMENT_FRIEND_VIDEO);
                        item.setChecked(true);
                    } else {
                        final android.app.AlertDialog.Builder alertDialogBuilder =
                                new android.app.AlertDialog.Builder(this);
                        alertDialogBuilder
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle(getResources().getString(R.string.notification))
                                .setMessage(getResources().getString(R.string.login_fb_while_logout));

                        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                changeToolbarTitle(getResources().getString(R.string.browser));
                                exchangeFragment(ConstantHelper.FRAGMENT_EXPLORER);
                            }
                        });
                        android.app.AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }

                } else if (method_login == 1) {
                    changeToolbarTitle(getResources().getString(R.string.friend_video));
                    exchangeFragment(ConstantHelper.FRIEND_LIST);
                    item.setChecked(true);
                } else {
                    loginFacebookWeb();
                }
                break;
            case R.id.nav_funny_video:
                if (method_login == 2) {
                    if ((loginState.getString("cookie", null) != null)
                            && !loginState.getString("url", null).contains(URL_FB)
                            && !loginState.getString("url", null).contains(URL_FB_1)) {

                        changeToolbarTitle(getResources().getString(R.string.funny_page));
                        exchangeFragment(ConstantHelper.FRAGMENT_FUNNY_VIDEO);
                        item.setChecked(true);
                    } else {
                        final android.app.AlertDialog.Builder alertDialogBuilder =
                                new android.app.AlertDialog.Builder(this);
                        alertDialogBuilder
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle(getResources().getString(R.string.notification))
                                .setMessage(getResources().getString(R.string.login_fb_while_logout));

                        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                changeToolbarTitle(getResources().getString(R.string.browser));
                                exchangeFragment(ConstantHelper.FRAGMENT_EXPLORER);
                            }
                        });
                        android.app.AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }

                } else {
                    loginFacebookWeb();
                }
                break;
            case R.id.nav_timeline:
                if (method_login == 2 || method_login == 1) {
                    changeToolbarTitle(getResources().getString(R.string.timeline));
                    exchangeFragment(ConstantHelper.FRAGMENT_TIMELINE_VIDEO);
                    item.setChecked(true);
                } else {
                    loginFacebookWeb();
                }
                break;
            case R.id.nav_post:
                if (method_login == 2) {
                    changeToolbarTitle(getResources().getString(R.string.post));
                    exchangeFragment(ConstantHelper.FRAGMENT_POST_VIDEO);
                    item.setChecked(true);
                } else {
                    loginFacebookWeb();
                }
                break;
            case R.id.nav_upload_video:
                if (method_login == 2 || method_login == 1) {
                    changeToolbarTitle(getResources().getString(R.string.video_upload));
                    exchangeFragment(ConstantHelper.FRAGMENT_VIDEO_UPLOADED);
                    item.setChecked(true);
                } else {
                    loginFacebookWeb();
                }
                break;
            case R.id.nav_tagged_video:
                if (method_login == 2 || method_login == 1) {
                    changeToolbarTitle(getResources().getString(R.string.video_tagged));
                    exchangeFragment(ConstantHelper.FRAGMENT_VIDEO_TAGGED);
                    item.setChecked(true);
                } else {
                    loginFacebookWeb();
                }
                break;
            case R.id.nav_search_video:
                if (method_login == 2) {
                    if ((loginState.getString("cookie", null) != null)
                            && !loginState.getString("url", null).contains(URL_FB)
                            && !loginState.getString("url", null).contains(URL_FB_1)) {
                        changeToolbarTitle(getResources().getString(R.string.search_page));
                        exchangeFragment(ConstantHelper.FRAGMENT_SEARCH_VIDEO);
                        item.setChecked(true);
                    } else {
                        final android.app.AlertDialog.Builder alertDialogBuilder =
                                new android.app.AlertDialog.Builder(this);
                        alertDialogBuilder
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle(getResources().getString(R.string.notification))
                                .setMessage(getResources().getString(R.string.login_fb_while_logout));

                        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                changeToolbarTitle(getResources().getString(R.string.browser));
                                exchangeFragment(ConstantHelper.FRAGMENT_EXPLORER);
                            }
                        });
                        android.app.AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }

                } else {
                    loginFacebookWeb();
                }
                break;
            case R.id.nav_download:
                changeToolbarTitle(getResources().getString(R.string.video_download));
                exchangeFragment(ConstantHelper.FRAGMENT_VIDEO_DOWNLOADED);
                item.setChecked(true);
                break;
            case R.id.nav_setting:
                changeToolbarTitle(getResources().getString(R.string.setting));
                exchangeFragment(ConstantHelper.FRAGMENT_SETTING);
                item.setChecked(true);
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loginFacebookWeb() {
        final android.app.AlertDialog.Builder alertDialogBuilder =
                new android.app.AlertDialog.Builder(this);
        alertDialogBuilder
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getResources().getString(R.string.notification))
                .setMessage(getResources().getString(R.string.use_feature_login_web));

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                CookieSyncManager.createInstance(MainActivity.this);
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.removeAllCookie();
                cookieManager.setAcceptCookie(false);
                loginStateEditor.putBoolean("login_state", false);
                loginStateEditor.putInt("login_method", 0);
                loginStateEditor.putString("cookie", null);
                loginStateEditor.putString("url", null);
                loginStateEditor.commit();
                facebookSession.resetAccessToken();
                AccessToken.setCurrentAccessToken(null);
                Intent intent = new Intent(MainActivity.this, LoginByWebActivity.class);
                intent.putExtra("method_login", 2);
                startActivity(intent);
                finish();
            }
        });
        android.app.AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

    public void onBackPressed() {
        if (this.browserFragment == null) {
           Log.e("MAIN", "NULL Browser");
        }
        if (this.browserFragment != null) {
            this.browserFragment.backPress();
        } else if (this.prefs == null) {
            exit();
        } else if (this.prefs.getBoolean("locked", false)) {
            exit();
        } else {
            new CustomDialog(this).showRatePopup();
        }
    }
}
