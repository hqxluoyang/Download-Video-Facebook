package com.example.datvit.facebookvideodownloader.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.models.MyPage;
import com.example.datvit.facebookvideodownloader.models.MyVideo;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by DatVIT on 11/18/2016.
 */

public class PagesVideoAdapter extends BaseAdapter {

    private Context context;
    private List<MyPage> myPages;

    public PagesVideoAdapter(Context context, List<MyPage> myPages) {
        this.context = context;
        this.myPages = myPages;
    }

    @Override
    public int getCount() {
        if (myPages != null) {
            return myPages.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        if (myPages != null) {
            return myPages.get(position);
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
            convertView = inflater.inflate(R.layout.items_page, null);
            holder = createViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        MyPage myPage = myPages.get(position);

        if (myPage.created_time != null) {
            holder.date.setVisibility(View.VISIBLE);
            holder.date.setText(myPage.created_time.substring(0, 10));
        } else {
            holder.date.setVisibility(View.INVISIBLE);
        }
        holder.title.setText(myPage.name);
        holder.category.setText(myPage.category);
        if (myPage.picture != null) {
            Picasso.with(context).load(myPage.picture).into(holder.bg);
            holder.img.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    private ViewHolder createViewHolder(View v) {
        ViewHolder holder = new ViewHolder();
        holder.date = (TextView) v.findViewById(R.id.tvDate);
        holder.title = (TextView) v.findViewById(R.id.titlePage);
        holder.category = (TextView) v.findViewById(R.id.tvCategory);
        holder.bg = (ImageView) v.findViewById(R.id.bgPage);
        holder.img = (ImageView) v.findViewById(R.id.imgPage);
        return holder;
    }

    private static class ViewHolder {
        public TextView date;
        public TextView title;
        public TextView category;
        public ImageView bg;
        public ImageView img;
    }
}