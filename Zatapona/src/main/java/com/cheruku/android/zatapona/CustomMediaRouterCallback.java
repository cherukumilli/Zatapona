package com.cheruku.android.zatapona;

import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.widget.Toast;

import com.google.cast.MediaRouteHelper;

import java.io.IOException;

/**
 * A callback class which listens for route select or unselect events and processes devices
 * and sessions accordingly.
 * Make the class to only have package scope
 */
public class CustomMediaRouterCallback extends MediaRouter.Callback {
    private static final String TAG = CustomMediaRouterCallback.class.getSimpleName();
    public static final boolean ENABLE_LOGV = true;

    private boolean bAutoConnectWhenRouteAdded = false;
    private MainActivity activity = null;

    public CustomMediaRouterCallback(MainActivity activity){
        logVIfEnabled("in constructor()");
        this.activity = activity;
    }

    public void autoConnect(MediaRouter router, MediaRouter.RouteInfo route){
        logVIfEnabled("in autoConnect()");
        if (activity.getSharedPreferences().getBoolean(activity.getString(R.string.enable_autoconnect), false)){ //auto connect is enabled
            if (router.getSelectedRoute() == router.getDefaultRoute()){ // no route is selected currently
                String sharedPreferencesKeyName = activity.getString(R.string.sp_route_name_key);
                String lastUsedRouteName = activity.getSharedPreferences().getString(sharedPreferencesKeyName, route.getName());
                if (route.getName().equals(lastUsedRouteName)){ //route added matches the last used router
                    router.selectRoute(route);
                    Toast.makeText(activity.getApplicationContext(), activity.getString(R.string.toast_on_route_added) + route.getName(), Toast.LENGTH_SHORT).show();
                    bAutoConnectWhenRouteAdded = true;
                }
            }
        }
    }

    @Override
    public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route){
        logVIfEnabled("in onRouteAdded()");
        super.onRouteAdded(router, route);
        autoConnect(router, route); //auto connect to last used route if auto connect is enabled
    }

    @Override
    public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
        logVIfEnabled("in onRouteSelected()");
        MediaRouteHelper.requestCastDeviceForRoute(route);
        activity.setCurrentRoute(route);
        //activity.startStatusRunnerThread();

        if (bAutoConnectWhenRouteAdded){ // no need to display the toast Text popup
            bAutoConnectWhenRouteAdded = false;
            return;
        }

        if (activity.getSharedPreferences().getBoolean(activity.getString(R.string.enable_autoconnect), false)){
            activity.getSharedPreferences().edit().putString(activity.getString(R.string.sp_route_name_key), route.getName()).commit();
            String toastText = activity.getString(R.string.toast_on_route_selected) + route.getName() + "'";
            Toast.makeText(activity.getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
        logVIfEnabled("in onRouteUnselected()");
        try {
            if (activity.getSession() != null) {
                logVIfEnabled("Ending session and stopping application");
                activity.getSession().setStopApplicationWhenEnding(true);
                activity.getSession().endSession();
            } else {
                Log.e(TAG, "onRouteUnselected: mSession is null");
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "onRouteUnselected:");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "onRouteUnselected:");
            e.printStackTrace();
        }
        activity.setMessageStream(null);
        activity.setSelectedDevice(null);
        activity.setCurrentRoute(null);
        activity.setCurrentItemId(null);
    }

    /**
     * Logs in verbose mode with the given tag and message, if the LOCAL_LOGV tag is set.
     */
    private void logVIfEnabled(String message){
        if(ENABLE_LOGV){
            Log.v(TAG, message);
        }
    }
}

