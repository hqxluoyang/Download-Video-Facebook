package com.example.datvit.facebookvideodownloader.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.datvit.facebookvideodownloader.BuildConfig;
import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.models.ItemDialog;
import com.example.datvit.facebookvideodownloader.utils.ToolsHelper;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by DatVIT on 11/15/2016.
 */

public class VideoDownloadedFragment extends Fragment {

    private static final String MEDIA_DATA = "_data";
    private static final Uri MEDIA_EXTERNAL_CONTENT_URI;
    private static final String _ID = "_id";
    private int _columnIndex;
    private String _columndata;
    protected Context _context;
    private Cursor _cursor;
    private GridView _gallery;
    private List<Integer> _videosId;
    private VideoGalleryAdapter adap;
    private int deleteposition;
    private int number;
    private final ItemDialog[] items;
    private ListAdapter ladapter;
    private String thumbnailPath;
    private List<String> video_path;
    private ToolsHelper toolsHelper;
    private AVLoadingIndicatorView loadingIndicatorView;

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
        try {
            this.ladapter = new SelectionVideo(getActivity(), R.layout.dialog_view, R.id.text1, this.items);
        } catch (Exception e) {
            e.printStackTrace();
        }
        toolsHelper = new ToolsHelper();
    }

    class SelectionVideo extends ArrayAdapter<ItemDialog> {
        SelectionVideo(Context x0, int x1, int x2, ItemDialog[] x3) {
            super(x0, x1, x2, x3);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            TextView tv = (TextView) v.findViewById(R.id.text1);
            ImageView img = (ImageView) v.findViewById(R.id.imgView);
            tv.setText(items[position].text);
            img.setImageResource(items[position].icon);
            return v;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video_downloaded, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        _context = getActivity();
        _gallery = (GridView) view.findViewById(R.id.gridView1);
        loadingIndicatorView = (AVLoadingIndicatorView) view.findViewById(R.id.fbProgress);
        loadingIndicatorView.setVisibility(View.VISIBLE);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new GetVideo().execute();
            }
        });
    }

    private class GetVideo extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingIndicatorView.smoothToShow();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            loadingIndicatorView.smoothToHide();
            adap = new VideoGalleryAdapter(getActivity());
            _gallery.setAdapter(adap);
            _gallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    deleteposition = i;
                    showMyDialog(deleteposition);
                }
            });

            if (_videosId.size() <= 0) {
                Toast.makeText(getActivity(), getResources().getString(R.string.no_found_video_downloaded), Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        protected Void doInBackground(Void... voids) {
            initVideosId();
            return null;
        }
    }

    public void showMyDialog(final int pos) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setIcon(R.mipmap.ic_launcher);
            builder.setTitle(getResources().getString(R.string.app_name)).setAdapter(this.ladapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int position) {
                    Intent intent;
                    if (position == 0) {
                        dialog.cancel();
                        intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(new File(video_path.get(pos))), "video/mp4");
                        startActivity(intent);
                    } else if (position == 1) {
                        dialog.cancel();
                        intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("video/.mp4");
                        intent.putExtra("android.intent.extra.STREAM", Uri.fromFile(new File(video_path.get(pos))));
                        startActivity(Intent.createChooser(intent, getResources().getString(R.string.share)));
                    } else if (position == 2) {
                        dialog.cancel();
                        AlertDialog ad1 = new AlertDialog.Builder(getActivity()).create();
                        ad1.setTitle(getResources().getString(R.string.confirm));
                        ad1.setMessage(getResources().getString(R.string.are_you_delete_video));
                        ad1.setButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                File file = new File(video_path.get(pos));
                                boolean deleted = file.delete();
                                if (deleted) {
                                    showToast(getResources().getString(R.string.delete_video_success));
                                    video_path.remove(pos);
                                    _videosId.remove(pos);
                                    adap.notifyDataSetChanged();
                                    _gallery.setAdapter(adap);
                                } else {
                                    showToast(getResources().getString(R.string.delete_video_failed));
                                }
                                getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                            }
                        });
                        ad1.setButton2(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        });
                        ad1.show();
                    } else if (position == 3) {
                        dialog.cancel();
                        displayInfo(pos);
                    } else if (position == 4) {
                        dialog.cancel();
                    }
                }
            });
            builder.create().show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayInfo(int position) {
        try {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            File f = new File(this.video_path.get(position));
            String filePath = f.getParentFile().toString();
            String title = f.getName();
//            String totalSize = toolsHelper.size(Integer.valueOf((int) f.length()).intValue());
            String totalSize = toolsHelper.readableFileSize(f.length());
            String message = getResources().getString(R.string.title) + " : " + "\n"
                    + title + "\n\n" + getResources().getString(R.string.size) + " : "
                    + totalSize + "\n\n" + getResources().getString(R.string.download_location) + " : " + "\n" + filePath;
            alertDialogBuilder.setTitle("Info");
            alertDialogBuilder.setMessage(message);
            alertDialogBuilder.create().show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class VideoGalleryAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public VideoGalleryAdapter(Context c) {
            _context = c;
        }

        public int getCount() {
            this.mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            return _videosId.size();
        }

        public Object getItem(int position) {
            return Integer.valueOf(position);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = this.mInflater.inflate(R.layout.item_video_downloaded, null);
                holder.imageview1 = (ImageView) convertView.findViewById(R.id.imageView1);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            if (_videosId.size() != 0) {
                if (thumbnailPath != null) {
                    holder.imageview1.setImageURI(Uri.fromFile(new File(getImage(_cursor.getInt(_columnIndex)))));
                } else {
                    holder.imageview1.setImageBitmap(get_video_Image((long) (_videosId.get(position)).intValue()));
                }
            }
            return convertView;
        }

        private Bitmap get_video_Image(long integer) {
            return MediaStore.Video.Thumbnails.getThumbnail(getActivity().getContentResolver(), integer, 1, null);
        }
    }

    class ViewHolder {
        int id;
        ImageView imageview1;

        ViewHolder() {
        }
    }

    public VideoDownloadedFragment() {
        this.number = 0;
        this.ladapter = null;
        items = new ItemDialog[]{new ItemDialog(Integer.valueOf(R.string.play),
                Integer.valueOf(R.drawable.ico_play_1)),
                new ItemDialog(Integer.valueOf(R.string.share),
                        Integer.valueOf(R.drawable.ico_share)),
                new ItemDialog(Integer.valueOf(R.string.delete),
                        Integer.valueOf(R.drawable.ico_recycle)),
                new ItemDialog(Integer.valueOf(R.string.info),
                        Integer.valueOf(R.drawable.ico_info)),
                new ItemDialog(Integer.valueOf(R.string.cancel),
                        Integer.valueOf(R.drawable.ico_delete))};
    }

    static {
        MEDIA_EXTERNAL_CONTENT_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    }

    private void initVideosId() {
//        String lockate = getActivity().getSharedPreferences("foldername", 0).getString("path", "GPVideoDownloader");
        String lockate = "GPVideoDownloader";
        lockate = "'" + lockate + "'";
        try {
            String[] proj = new String[]{_ID, MEDIA_DATA};
            this._cursor = getActivity().managedQuery(MEDIA_EXTERNAL_CONTENT_URI, proj, "album = " + lockate, null, "datetaken DESC");
            int count = this._cursor.getCount();
            this._videosId = new ArrayList();
            this.video_path = new ArrayList();
            while (this._cursor != null && this._cursor.moveToNext()) {
                this._columnIndex = this._cursor.getColumnIndex(_ID);
                if (count != 0) {
                    this._columndata = this._cursor.getString(this._cursor.getColumnIndexOrThrow(MEDIA_DATA));
                    this.video_path.add(this._columndata);
                }
                this._videosId.add(Integer.valueOf(this._cursor.getInt(this._columnIndex)));
                this.number++;
            }
            Log.e("DOWNLOADED_VIDEO", "Count = " + number);
        } catch (Exception ex) {
            showToast(ex.getMessage().toString());
        }
    }

    protected void showToast(String msg) {
        Toast.makeText(this._context, msg, Toast.LENGTH_SHORT).show();
    }

    private String getImage(long fileId) {
        Cursor thumbCursor = null;
        try {
            thumbCursor = getActivity().getContentResolver().query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, null, "video_id = " + fileId + " AND " + "kind" + " = " + 1, null, null);
            if (thumbCursor.moveToFirst()) {
                this.thumbnailPath = thumbCursor.getString(thumbCursor.getColumnIndexOrThrow(MEDIA_DATA));
            } else {
                this.thumbnailPath = null;
            }
            if (thumbCursor != null) {
                thumbCursor.close();
            }
            return this.thumbnailPath;
        } catch (Throwable th) {
            if (thumbCursor != null) {
                thumbCursor.close();
            }
        }
        return null;
    }
}
