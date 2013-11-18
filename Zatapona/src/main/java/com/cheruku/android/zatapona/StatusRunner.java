package com.cheruku.android.zatapona;

import android.util.Log;

/**
 * A Runnable class that updates a view to display status for the currently playing media.
 * This class has package scope
 */
public class StatusRunner implements Runnable {
    private MainActivity mActivity = null;
    private static final String TAG = StatusRunner.class.getSimpleName();

    public StatusRunner(MainActivity activity){
        Log.v(TAG, "in StatusRunner constructor");
        this.mActivity = activity;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                //if (!mActivity.isActivityStopped() ) {
                    mActivity.updateStatus();
                    Thread.sleep(1500);
                //} else {
                //    break;
                //}
            } catch (Exception e) {
                Log.e(TAG, "Thread interrupted: " + e);
            }
        }
        Log.v(TAG, "*************Status Runner THREAD INTERRUPTED******************");
    }
}
