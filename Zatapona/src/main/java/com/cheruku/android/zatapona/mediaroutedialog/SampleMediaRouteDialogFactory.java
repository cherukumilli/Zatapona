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

package com.cheruku.android.zatapona.mediaroutedialog;

import android.support.v7.app.MediaRouteDialogFactory;
import android.util.Log;

/**
 * An extension of MediaRouteDialogFactory that can be used to create a new 
 * SampleMediaRouteControllerDialogFragment.
 */
public class SampleMediaRouteDialogFactory extends MediaRouteDialogFactory {
    
    private static final String TAG = SampleMediaRouteDialogFactory.class.getSimpleName();
    
    private static final SampleMediaRouteDialogFactory sDefault = new SampleMediaRouteDialogFactory();

    /**
     * Creates a new SampleMediaRouteDialogFactory.
     */
    public SampleMediaRouteDialogFactory() {
        super();
        Log.v(TAG, "in constructor");
    }

    /**
     * Returns a default SampleMediaRouteDialogFactory.
     */
    public static SampleMediaRouteDialogFactory getDefault() {
        Log.v(TAG, "in getDefault");
        return sDefault;
    }

    @Override
    public SampleMediaRouteControllerDialogFragment onCreateControllerDialogFragment() {
        Log.v(TAG, "in onCreateControllerDialogFragment");
        return new SampleMediaRouteControllerDialogFragment();
    }

}
