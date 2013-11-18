/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.cheruku.android.zatapona;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.cheruku.android.zatapona.castmediaendpoint.model.CastMedia;

/**
 * A BaseAdapter containing a fixed set of CastMedia objects.
 */
public class MediaAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private MainActivity mActivity = null;
    private boolean mImagesDownloaded = false;
    private ListView mMediaListView = null;

    /**
     * Creates a new MediaAdapter for the given activity.
     */
    public MediaAdapter(MainActivity activity) {
        mActivity = activity;
        mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    @Override
    public int getCount() {
        return mActivity.getVideos().size();
    }

    @Override
    public CastMedia getItem(int position) {
        return mActivity.getVideos().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        CastVideoViewHolder viewHolder;

        if(view == null) {
            view = mInflater.inflate(R.layout.item_cast_media, null);
            viewHolder = new CastVideoViewHolder(view);
            view.setTag(viewHolder);
        } else {
            viewHolder = (CastVideoViewHolder) view.getTag();
        }
        viewHolder.setPosition(position);
        return view;
    }
    protected void populateMediaList(){
        mMediaListView = (ListView) mActivity.findViewById(R.id.media_list);
        mMediaListView.setAdapter(this);
        mMediaListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                Log.v("mediaListView", "onItemClick");
                if (mActivity.getVideos() == null) return;

                CastMedia mMedia = getItem(position);
                if (mMedia == null) return;
                if(MainActivity.ENABLE_LOGV) Log.v("QueryCastMediaTask", "Selected media: " + mMedia.getTitle());
                mActivity.mediaSelected(mMedia);
            }
        });
    }

    public void setImagesDownloaded(boolean mImagesDownloaded) {
        this.mImagesDownloaded = mImagesDownloaded;
    }

    public void refreshMediaList(){
        ((BaseAdapter)mMediaListView.getAdapter()).notifyDataSetChanged();
    }

    private class CastVideoViewHolder {
        private TextView mVideoTitle;
        private ImageView mImageView;

        public CastVideoViewHolder(View view) {
            mVideoTitle = (TextView) view.findViewById(R.id.item_cast_video_title_textview);
            mImageView = (ImageView) view.findViewById(R.id.cast_video_image);
        }

        public void setPosition(int position) {
            mVideoTitle.setText(mActivity.getVideos().get(position).getTitle());
            if (mImagesDownloaded)
                mImageView.setImageBitmap(mActivity.getImages().get(position));
        }
    }
}
