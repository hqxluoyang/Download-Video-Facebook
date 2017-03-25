package com.example.datvit.facebookvideodownloader.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.models.MyGroup;
import com.example.datvit.facebookvideodownloader.models.MyPage;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by DatVIT on 11/18/2016.
 */

public class GroupsVideoAdapter extends BaseAdapter {

    private Context context;
    private List<MyGroup> myGroups;

    public GroupsVideoAdapter(Context context, List<MyGroup> myGroups) {
        this.context = context;
        this.myGroups = myGroups;
    }

    @Override
    public int getCount() {
        if (myGroups != null) {
            return myGroups.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        if (myGroups != null) {
            return myGroups.get(position);
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
            convertView = inflater.inflate(R.layout.items_group, null);
            holder = createViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        MyGroup myGroup = myGroups.get(position);

        holder.title.setText(myGroup.name);
        holder.owner.setText(myGroup.owner);

        holder.descrip.setVisibility(View.INVISIBLE);

        if (holder.descrip != null) {
            holder.descrip.setVisibility(View.VISIBLE);
            holder.descrip.setText(myGroup.description);
        }

        if (myGroup.cover != null) {
            Picasso.with(context).load(myGroup.cover).into(holder.bg);
            holder.img.setVisibility(View.INVISIBLE);
        } else if (myGroup.picture != null) {
            Picasso.with(context).load(myGroup.picture).into(holder.bg);
            holder.img.setVisibility(View.INVISIBLE);
        } else if (myGroup.icon != null) {
            Picasso.with(context).load(myGroup.icon).into(holder.bg);
            holder.img.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    private ViewHolder createViewHolder(View v) {
        ViewHolder holder = new ViewHolder();
        holder.title = (TextView) v.findViewById(R.id.titleGroup);
        holder.owner = (TextView) v.findViewById(R.id.ownerGroup);
        holder.descrip = (TextView) v.findViewById(R.id.descriptionGroup);
        holder.bg = (ImageView) v.findViewById(R.id.bgGroup);
        holder.img = (ImageView) v.findViewById(R.id.imgGroup);
        return holder;
    }

    private static class ViewHolder {
        public TextView title;
        public TextView owner;
        public TextView descrip;
        public ImageView bg;
        public ImageView img;
    }
}