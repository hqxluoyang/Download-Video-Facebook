package com.example.datvit.facebookvideodownloader.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.models.MyVideo;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by DatVIT on 11/18/2016.
 */

public class MyVideoAdapter extends BaseAdapter {

    private Context context;
    private List<MyVideo> myMovies;

    public MyVideoAdapter(Context context, List<MyVideo> myMovies) {
        this.context = context;
        this.myMovies = myMovies;
    }

    @Override
    public int getCount() {
        if (myMovies != null) {
            return myMovies.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        if (myMovies != null) {
            return myMovies.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.items_video, null);
            holder = createViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        MyVideo video = myMovies.get(position);

        if (video.created_time != null) {
            holder.createTime.setText(video.created_time);
        } else if (video.duration != null) {
            holder.createTime.setText(video.duration);
        }

        if (video.message != null) {
            holder.message.setText(video.message);
        }

        holder.title.setText(video.name);
        if (video.picture != null) {
            Picasso.with(context).load(video.picture).into(holder.img);
//            holder.imgPlay.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    private ViewHolder createViewHolder(View v) {
        ViewHolder holder = new ViewHolder();
        holder.createTime = (TextView) v.findViewById(R.id.createdTimeVideo);
        holder.title = (TextView) v.findViewById(R.id.titleVideo);
        holder.message = (TextView) v.findViewById(R.id.message);
        holder.img = (ImageView) v.findViewById(R.id.imgVideo);
        holder.imgPlay = (ImageView) v.findViewById(R.id.imgPlay);
        return holder;
    }

    private static class ViewHolder {
        public TextView createTime;
        public TextView title;
        public TextView message;
        public ImageView img;
        public ImageView imgPlay;
    }
}