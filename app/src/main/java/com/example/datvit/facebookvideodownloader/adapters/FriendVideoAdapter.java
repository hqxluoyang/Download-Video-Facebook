package com.example.datvit.facebookvideodownloader.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.datvit.facebookvideodownloader.R;
import com.example.datvit.facebookvideodownloader.models.MyFriend;
import com.example.datvit.facebookvideodownloader.models.MyGroup;
import com.squareup.picasso.Picasso;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by DatVIT on 11/18/2016.
 */

public class FriendVideoAdapter extends BaseAdapter {

    private Context context;
    private List<MyFriend> myFriends;

    public FriendVideoAdapter(Context context, List<MyFriend> myFriends) {
        this.context = context;
        this.myFriends = myFriends;
    }

    @Override
    public int getCount() {
        if (myFriends != null) {
            return myFriends.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        if (myFriends != null) {
            return myFriends.get(position);
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
            convertView = inflater.inflate(R.layout.items_friend, null);
            holder = createViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        MyFriend myFriend = myFriends.get(position);

        holder.title.setText(myFriend.name);
        if (myFriend.gender != null) {
            holder.ln.setVisibility(View.VISIBLE);
            if (myFriend.gender.toLowerCase().equals("male")) {
                holder.gender.setImageResource(R.drawable.ico_male);
            } else if (myFriend.gender.toLowerCase().equals("female")) {
                holder.gender.setImageResource(R.drawable.ico_female);
            } else {
                holder.gender.setImageResource(R.drawable.ico_gender);
            }
        } else {
            holder.ln.setVisibility(View.INVISIBLE);
        }

        if (myFriend.picture != null) {
            Picasso.with(context).load(myFriend.picture).into(holder.bg);
            holder.img.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    private ViewHolder createViewHolder(View v) {
        ViewHolder holder = new ViewHolder();
        holder.title = (TextView) v.findViewById(R.id.titleGroup);
        holder.bg = (ImageView) v.findViewById(R.id.bgGroup);
        holder.img = (ImageView) v.findViewById(R.id.imgGroup);
        holder.gender = (ImageView) v.findViewById(R.id.imgGender);
        holder.ln = (LinearLayout) v.findViewById(R.id.gender);
        return holder;
    }

    private static class ViewHolder {
        public TextView title;
        public ImageView bg;
        public ImageView img;
        public ImageView gender;
        public LinearLayout ln;
    }
}