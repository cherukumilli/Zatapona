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

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ListView;
import android.view.View;

import com.cheruku.android.zatapona.castmediaendpoint.model.CastMedia;

/**
 * A Dialog that displays a set of media objects and allows for their selection.
 */
public class MediaSelectionDialog extends Dialog {

	private static final String TAG = MediaSelectionDialog.class.getSimpleName();
	
	private MainActivity mCSA;
	private ListView mediaListView;
	private MediaAdapter mAdapter;
	
    /**
     * Creates a new MediaSelectionDialog which can interact with the passed Activity.
     */
	public MediaSelectionDialog(Activity activity) {
		super(activity);
		this.mCSA = (MainActivity) activity;
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v("MediaSelectionDialog", "in onCreate");
        super.onCreate(savedInstanceState);
        mAdapter = new MediaAdapter(mCSA);
        setContentView(R.layout.dialog_media_selection);
        mediaListView = (ListView) findViewById(R.id.media_list);
        mediaListView.setAdapter(mAdapter);
        mediaListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                Log.v("MediaSelectionDialog", "onItemClick");
                if (mCSA.getVideos() == null) return;

                CastMedia mMedia = mAdapter.getItem(position);
                if (mMedia == null) return;
                if(MainActivity.ENABLE_LOGV) Log.v(TAG, "Selected media: " + mMedia.getTitle());
                mCSA.mediaSelected(mMedia);
                MediaSelectionDialog.this.dismiss();
            }
        });
    }
}
