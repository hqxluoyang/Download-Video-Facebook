package com.example.datvit.facebookvideodownloader.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.models.MyMovie;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by DatVIT on 11/18/2016.
 */

public class MovieWatchedAdapter extends BaseAdapter {

    private Context context;
    private List<MyMovie> myMovies;

    public MovieWatchedAdapter(Context context, List<MyMovie> myMovies) {
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
        MyMovie myMovie = myMovies.get(position);

        if (myMovie.created_time != null) {
            holder.time.setText(myMovie.created_time);
        } else if (myMovie.end_time != null) {
            holder.time.setText(myMovie.end_time);
        }
        if (myMovie.about != null) {
            holder.about.setText(myMovie.about);
        } else if (myMovie.description != null) {
            holder.about.setText(myMovie.description);
        }
        holder.title.setText(myMovie.title);
        holder.imgPlay.setVisibility(View.INVISIBLE);

        if (myMovie.picture != null) {
            Picasso.with(context).load(myMovie.picture).into(holder.imgVideo);
        } else if (myMovie.cover != null) {
            Picasso.with(context).load(myMovie.cover).into(holder.imgVideo);
        } else {
            holder.imgVideo.setImageResource(R.drawable.img_movie);
        }
        return convertView;
    }

    private ViewHolder createViewHolder(View v) {
        ViewHolder holder = new ViewHolder();
        holder.time = (TextView) v.findViewById(R.id.createdTimeVideo);
        holder.title = (TextView) v.findViewById(R.id.titleVideo);
        holder.about = (TextView) v.findViewById(R.id.message);
        holder.imgVideo = (ImageView) v.findViewById(R.id.imgVideo);
        holder.imgPlay = (ImageView) v.findViewById(R.id.imgPlay);
        return holder;
    }

    private static class ViewHolder {
        public TextView time;
        public TextView title;
        public TextView about;
        public ImageView imgVideo;
        public ImageView imgPlay;
    }
}