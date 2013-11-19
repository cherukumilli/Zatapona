package com.cheruku.android.zatapona;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.cheruku.android.zatapona.castmediaendpoint.model.CastMedia;
import com.cheruku.android.zatapona.mediaroutedialog.SampleMediaRouteDialogFactory;
import com.google.cast.ApplicationChannel;
import com.google.cast.ApplicationMetadata;
import com.google.cast.ApplicationSession;
import com.google.cast.CastContext;
import com.google.cast.CastDevice;
import com.google.cast.ContentMetadata;
import com.google.cast.MediaProtocolCommand;
import com.google.cast.MediaProtocolMessageStream;
import com.google.cast.MediaRouteAdapter;
import com.google.cast.MediaRouteHelper;
import com.google.cast.MediaRouteStateChangeListener;
import com.google.cast.SessionError;

import android.support.v7.app.MediaRouteButton;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity
        implements MediaRouteAdapter, QueryCastMediaTask.OnQueryCastMediaTaskCompleted, DownloadImagesTask.OnDownloadImagesTaskCompleted{
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final boolean ENABLE_LOGV = true;
    private boolean mPlayButtonShowsPlay = false;
    private boolean mVideoIsStopped = false;
    private boolean mActivityStopped = true;

    private CastContext mCastContext = null;
    private CastMedia mMedia = null;
    private ContentMetadata mMetaData = null;

    private SampleMediaRouteDialogFactory mDialogFactory = null;
    private MediaRouter mMediaRouter = null;
    private MediaRouteSelector mMediaRouteSelector = null;
    private ApplicationSession mSession = null;
    private MediaProtocolMessageStream mMessageStream = null;
    private CastDevice mSelectedDevice = null;
    private MediaRouteButton mMediaRouteButton = null;
    private MediaRouter.Callback mMediaRouterCallback = null;
    private TextView mCurrentlyPlaying = null;
    private MediaSelectionDialog mMediaSelectionDialog = null;
    private MenuItem mRegistrationStateMenuItem = null;
    private MediaProtocolCommand mStatus = null;
    private String mCurrentItemId=null;
    private MediaRouter.RouteInfo mCurrentRoute = null;
    private SharedPreferences mSharedPreferences = null;
    private SharedPreferences.OnSharedPreferenceChangeListener mListener = null;
    private Thread mStatusRunnerThread = null;
    private MediaController mMediaController = null;
    private List<CastMedia> mVideos = null;
    private ArrayList<Bitmap> mImages = null;
    private MediaAdapter mMediaAdapter=null;
    private VideoView mVideoView = null;
    private QueryCastMediaTask mQueryCastMediaTask = null;
    private DownloadImagesTask mDownloadImagesTask = null;

    protected static final double MAX_VOLUME_LEVEL = 20;
    private static final double VOLUME_INCREMENT = 0.05;
    private static final int SEEK_FORWARD = 1;
    private static final int SEEK_BACK = 2;
    private static final int SEEK_INCREMENT = 10;

    public static final String PREFS_NAME = "zatapona";
    public static final String DEVICE_REGISTERED = "deviceRegistered";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        logVIfEnabled("onCreate called");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNewClassVariables();
        initViews();

        boolean registerMediaRouteProviderSuccessful = MediaRouteHelper.registerMinimalMediaRouteProvider(mCastContext, this);
        logVIfEnabled("registration state = " + registerMediaRouteProviderSuccessful);

        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = MediaRouteHelper.buildMediaRouteSelector(MediaRouteHelper.CATEGORY_CAST, getResources().getString(R.string.app_name), null);

        setupMediaRouteButtonOnActionBar();

        initSharedPreferences();

        //display the sender device registration window if the user hasn't registered the device
        if (!senderDeviceRegistered())
            displayRegistrationWindow();

        mQueryCastMediaTask.execute(); //get the videos from the content mgmt system
    }

    private void createNewClassVariables(){
        logVIfEnabled("in createNewClassVariables");
        mCastContext = new CastContext(getApplicationContext());
        mMedia = new CastMedia();
        mMetaData = new ContentMetadata();

        mDialogFactory = new SampleMediaRouteDialogFactory();
        mMediaRouterCallback = new CustomMediaRouterCallback(this);
        mMediaSelectionDialog = new MediaSelectionDialog(this);
        mVideos = new ArrayList<CastMedia>();
        mMediaController = new MediaController(this);
        mVideoView = (VideoView)findViewById(R.id.video_player);
        mVideoView.setMediaController(mMediaController);
        mQueryCastMediaTask = new QueryCastMediaTask(this);
    }

    private void initViews(){
        logVIfEnabled("in initViews");
        mCurrentlyPlaying = (TextView) findViewById(R.id.currently_playing);
    }
    protected void startStatusRunnerThread(){
        logVIfEnabled("in startStatusRunnerThread");
        Runnable runnable = new StatusRunner(this);
        mStatusRunnerThread = new Thread(runnable);
        mStatusRunnerThread.start();
    }
    private void initSharedPreferences(){
        logVIfEnabled("in initSharedPreferences");
        mSharedPreferences = getSharedPreferences(PREFS_NAME, 0);

        //Setup a shared preferences listener
        mListener = new SharedPreferences.OnSharedPreferenceChangeListener(){
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key){
                if (key.equals(DEVICE_REGISTERED)){
                    if (prefs.getBoolean(DEVICE_REGISTERED, false))
                        mRegistrationStateMenuItem.setTitle(R.string.unregister);
                    else
                        mRegistrationStateMenuItem.setTitle(R.string.register);
                }
            }
        };
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mListener);
    }
    private boolean senderDeviceRegistered(){
        logVIfEnabled("in senderDeviceRegistered");
        return mSharedPreferences.getBoolean(DEVICE_REGISTERED, false);
    }
    private void displayRegistrationWindow() {
        logVIfEnabled("in displayRegistrationWindow");
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        logVIfEnabled("in onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);

        mRegistrationStateMenuItem = menu.findItem(R.id.registration_state);
        if (senderDeviceRegistered())
            mRegistrationStateMenuItem.setTitle(R.string.unregister);

        //set menu item text based on user selected preference
        if (mSharedPreferences.getBoolean(getString(R.string.enable_autoconnect), false))
            menu.findItem(R.id.autoconnect).setTitle(R.string.disable_autoconnect);
        else
            menu.findItem(R.id.autoconnect).setTitle(R.string.enable_autoconnect);

        setupMediaRouteButtonOnActionBar();
        setMediaRouteButtonVisible();

        return true;
    }

    private void setupMediaRouteButtonOnActionBar(){
        logVIfEnabled("in setupMediaRouteButtonOnActionBar");
        ActionBar actionBar = getActionBar();
        // add the custom view to the action bar
        assert actionBar != null;
        actionBar.setCustomView(R.layout.title_cast_button);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        mMediaRouteButton = (MediaRouteButton) actionBar.getCustomView().findViewById(R.id.media_route_button);
        mMediaRouteButton.setRouteSelector(mMediaRouteSelector);
        mMediaRouteButton.setDialogFactory(mDialogFactory);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        logVIfEnabled("in onOptionsItemSelected");
        switch (item.getItemId()) {
            case R.id.registration_state:
                displayRegistrationWindow();
                break;
            case R.id.autoconnect:
                if (mSharedPreferences.getBoolean(getString(R.string.enable_autoconnect), false)){
                    //user is requesting disabling of auto connect to chromecast
                    item.setTitle(getString(R.string.enable_autoconnect));
                    mSharedPreferences.edit().putBoolean(getString(R.string.enable_autoconnect), false).commit();
                    Toast.makeText(this, R.string.toast_disable_autoconnect, Toast.LENGTH_SHORT).show();

                    //clear up the shared preferences for the current route name
                    if (mCurrentRoute != null)
                        mSharedPreferences.edit().putString(getString(R.string.sp_route_name_key), "").commit();
                }
                else{
                    //user is requesting enabling of auto connect to chromecast
                    item.setTitle(getString(R.string.disable_autoconnect));
                    mSharedPreferences.edit().putBoolean(getString(R.string.enable_autoconnect), true).commit();
                    Toast.makeText(this, R.string.toast_enable_autoconnect, Toast.LENGTH_SHORT).show();

                    //save the current route name in shared preferences
                    if (mCurrentRoute != null)
                        mSharedPreferences.edit().putString(getString(R.string.sp_route_name_key), mCurrentRoute.getName()).commit();
                }
                break;
            case R.id.action_settings:

                break;
            default:
                break;
        }

        return true;
    }

    /**
     * Stores and attempts to load the passed piece of media.
     */
    protected void mediaSelected(CastMedia media) {
        logVIfEnabled("in mediaSelected");
        this.mMedia = media;
        mCurrentItemId = media.getTitle();

        updateCurrentlyPlaying();

        mVideoView.setVideoPath(mMedia.getVideoUrl());
        mVideoView.setTag(mMedia.getVideoUrl());
        mVideoView.requestFocus();
        mVideoView.start();
        //mMediaController.show(0);

        if (mMessageStream != null) {
            loadMedia();
            logVIfEnabled("after mediaselected.loadMedia. mMedia.getVideoUrl="+mMedia.getVideoUrl()+". mMessageStream.getContentId="+mMessageStream.getContentId());
            mVideoView.pause();
            //mVideoView.stopPlayback();
            //mVideoView.suspend();
            updateStatus();
        }
    }

    /**
     * Starts a new video playback session with the current CastContext and selected device.
     */
    private void openSession() {
        logVIfEnabled("in openSession");
        mSession = new ApplicationSession(mCastContext, mSelectedDevice);

        int flags = 0;
        mSession.setApplicationOptions(flags);

        logVIfEnabled("Beginning session with context: " + mCastContext);
        logVIfEnabled("The session to begin: " + mSession);
        mSession.setListener(new ApplicationSession.Listener() {
            @Override
            public void onSessionStarted(ApplicationMetadata appMetadata) {
                logVIfEnabled("Getting channel after session start");
                ApplicationChannel channel = mSession.getChannel();
                if (channel == null) {
                    Log.e(TAG, "channel = null");
                    return;
                }
                logVIfEnabled("Creating and attaching Message Stream");
                mMessageStream = new MediaProtocolMessageStream();
                channel.attachMessageStream(mMessageStream);

                if (mMessageStream.getPlayerState() == null) {
                    if (mMedia != null) {loadMedia(); }
                } else {
                    logVIfEnabled("Found player already running; updating status");
                    updateStatus();
                }
            }

            @Override
            public void onSessionStartFailed(SessionError error) {Log.e(TAG, "onStartFailed " + error); }

            @Override
            public void onSessionEnded(SessionError error) {
                Log.i(TAG, "onEnded " + error);
                if (isActivityStopped())
                    finish();
            }
        });

        try {
            logVIfEnabled("Starting session with app name " + getString(R.string.app_name));
            mSession.startSession(getString(R.string.app_name));
        } catch (IOException e) {
            Log.e(TAG, "Failed to open session", e);
        }
    }

    /**
     * Loads the stored media object and casts it to the currently selected device.
     */
    protected void loadMedia() {
        logVIfEnabled("Loading selected media on device");
        if (mMedia == null || mMedia.getTitle() == null) return;

        mMetaData.setTitle(mMedia.getTitle());
        try {
            MediaProtocolCommand cmd = mMessageStream.loadMedia(mMedia.getVideoUrl(), mMetaData, true);
            cmd.setListener(new MediaProtocolCommand.Listener() {

                @Override
                public void onCompleted(MediaProtocolCommand mPCommand) {
                    logVIfEnabled("Load completed - starting playback");
                    //mPlayPauseButton.setImageResource(R.drawable.pause_button);
                    //mPlayButtonShowsPlay = false;
                    onSetVolume(0.5);
                }

                @Override
                public void onCancelled(MediaProtocolCommand mPCommand) {
                    logVIfEnabled("Load cancelled");
                }
            });
        } catch (IllegalStateException e) {
            Log.e(TAG, "Problem occurred with MediaProtocolCommand during loading", e);
        } catch (IOException e) {
            Log.e(TAG, "Problem opening MediaProtocolCommand during loading", e);
        }
    }

    @Override
    public void onDeviceAvailable(CastDevice device, String myString, MediaRouteStateChangeListener listener) {
        logVIfEnabled("in onDeviceAvailable");
        mSelectedDevice = device;
        logVIfEnabled("Available device found: " + myString);
        openSession();
    }

    /**
     * Plays or pauses the currently loaded media, depending on the current state of the <code>
     * mPlayPauseButton</code>.
     * @param playState indicates that Play was clicked if true, and Pause was clicked if false
     */
    public void onPlayClicked(boolean playState) {
        logVIfEnabled("in onPlayClicked");
        if (playState) {
            try {
                if (mMessageStream != null) {
                    mMessageStream.stop();
                } else {
                    Log.e(TAG, "onClick-Play - mMPMS==null");
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to send stop comamnd.");
            }
            //mPlayPauseButton.setImageResource(R.drawable.play_button);
        } else {
            try {
                if (mMessageStream != null) {
                    if (mVideoIsStopped) {
                        mMessageStream.play();
                        mVideoIsStopped = !mVideoIsStopped;
                    } else {
                        mMessageStream.resume();
                    }
                } else {
                    Log.e(TAG, "onClick-Play - mMPMS==null");
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to send play/resume comamnd.");
            }
            //mPlayPauseButton.setImageResource(R.drawable.pause_button);
        }
        mPlayButtonShowsPlay = !mPlayButtonShowsPlay;
    }

    /**
     * Handles stopping the currently playing media upon the stop button being pressed.
     */
    public void onStopClicked() {
        logVIfEnabled("in onStopClicked");
        try {
            if (mMessageStream != null) {
                mMessageStream.stop();
                mVideoIsStopped = true;
                //mPlayPauseButton.setImageResource(R.drawable.play_button);
                mPlayButtonShowsPlay = true;
            } else {
                Log.e(TAG, "onStopClicked - mMPMS==null");
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to send pause comamnd.");
        }
    }

    @Override
    public void onUpdateVolume(double volumeChange) {
        logVIfEnabled("in onUpdateVolume");
        try {
            if ((mCurrentItemId != null) && (mCurrentRoute != null)) {
                mCurrentRoute.requestUpdateVolume((int) (volumeChange * MAX_VOLUME_LEVEL));
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Problem sending Update Volume", e);
        }
    }

    @Override
    public void onSetVolume(double volume) {
        logVIfEnabled("in onSetVolume");
        try {
            mMessageStream.setVolume(volume);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Problem sending Set Volume", e);
        } catch (IOException e) {
            Log.e(TAG, "Problem sending Set Volume", e);
        }
    }

    /**
     * Mutes the currently playing media when the mute button is pressed.
     */
    public void onMuteClicked() {
        logVIfEnabled("in onMuteClicked");
        try {
            if (mMessageStream != null) {
                if (mMessageStream.isMuted()) {
                    mMessageStream.setMuted(false);
                } else {
                    mMessageStream.setMuted(true);
                }
            } else {
                Log.e(TAG, "onMutedClicked - mMPMS==null");
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to send pause comamnd.");
        }
    }

    /**
     * Skips forward or backward by some fixed increment in the currently playing media.
     * @param direction an integer corresponding to either SEEK_FORWARD or SEEK_BACK
     */
    public void onSeekClicked(int direction) {
        logVIfEnabled("in onSeekClicked");
        try {
            if (mMessageStream != null) {
                double cPosition = mMessageStream.getStreamPosition();
                if (direction == SEEK_FORWARD) {
                    mMessageStream.playFrom(cPosition + SEEK_INCREMENT);
                } else if (direction == SEEK_BACK) {
                    mMessageStream.playFrom(cPosition - SEEK_INCREMENT);
                } else {
                    Log.e(TAG, "onSeekClicked was not FWD or BACK");
                }
            } else {
                Log.e(TAG, "onSeekClicked - mMPMS==null");
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to send pause comamnd.");
        }
    }

    /**
     * Logs in verbose mode with the given tag and message, if the LOCAL_LOGV tag is set.
     */
    private void logVIfEnabled(String message){
        if(ENABLE_LOGV){
            Log.v(TAG, message);
        }
    }

    @Override
    protected void onStart() {
        logVIfEnabled("in onStart");
        mActivityStopped = false;
        super.onStart();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
        logVIfEnabled("onStart called and callback added");

        startStatusRunnerThread();

        if (mMediaRouter.isRouteAvailable(mMediaRouteSelector, 0)){
            for (MediaRouter.RouteInfo ri : mMediaRouter.getRoutes()){
                ((CustomMediaRouterCallback)mMediaRouterCallback).autoConnect(mMediaRouter, ri);
            }
        }
    }

    /**
     * Closes a running session upon destruction of this Activity.
     */
    @Override
    protected void onStop() {
        mActivityStopped = true;
        logVIfEnabled("in onStop");
        mMediaRouter.removeCallback(mMediaRouterCallback);
        super.onStop();
        logVIfEnabled("onStop called and callback removed");
    }

    @Override
    protected void onDestroy() {
        logVIfEnabled("onDestroy called, ending session if session exists");
        if (mSession != null) {
            try {
                if (!mSession.hasStopped()){
                    ///DC
                    if (mSession.getChannel() != null){
                        mSession.getChannel().detachMessageStream(mMessageStream);
                        mMessageStream = null;
                    }
                    /// DC

                    mSession.endSession();
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to end session.");
            }
        }
        mSession = null;

        //unregister the SharedPreferencesChangeListener
        if (mSharedPreferences != null){
            try{mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mListener);}
            catch (Exception e){ Log.e(TAG, "Unable to unregister OnSharedPreferenceChangeListener"); }
        }
        mSharedPreferences = null;

        mListener = null;

        try{
            Thread.sleep(1500);
        }catch (Exception e){
            Log.e(TAG, "Thread sleep failed");
        }
        mStatusRunnerThread = null;

        try{
            MediaRouteHelper.unregisterMediaRouteProvider(mCastContext);
        } catch (Exception e){
            Log.e(TAG, "Exception on MediaRouteHelper.unregisterMediaRouteProvider()");
        }
        mDialogFactory = null;
        mMediaRouteSelector = null;
        mMediaRouter = null;
        mMediaRouterCallback = null;
        mMediaSelectionDialog = null;
        mMetaData = null;
        mMedia = null;
        mCastContext.dispose();
        mCastContext = null;

        super.onDestroy();
    }

    /**
     * Processes volume up and volume down actions upon receiving them as key events.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        logVIfEnabled("in dispatchKeyEvent");
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mMessageStream != null) {
                        double currentVolume = mMessageStream.getVolume();
                        logVIfEnabled("Volume up from " + currentVolume);
                        if (currentVolume < 1.0) {
                            logVIfEnabled("New volume: " + (currentVolume + VOLUME_INCREMENT));
                            onSetVolume(currentVolume + VOLUME_INCREMENT);
                        }
                    } else {
                        Log.e(TAG, "dispatchKeyEvent - volume up - mMPMS==null");
                    }
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mMessageStream != null) {
                        double currentVolume = mMessageStream.getVolume();
                        logVIfEnabled("Volume down from: " + currentVolume);
                        if (currentVolume > 0.0) {
                            logVIfEnabled("New volume: " + (currentVolume - VOLUME_INCREMENT));
                            onSetVolume(currentVolume - VOLUME_INCREMENT);
                        }
                    } else {
                        Log.e(TAG, "dispatchKeyEvent - volume down - mMPMS==null");
                    }
                }
                return true;
            default:
                logVIfEnabled("Keycode = "+keyCode+". action="+action);
                return super.dispatchKeyEvent(event);
        }
    }

    private void setMediaFromStream(String title, String videoUrl){
        logVIfEnabled("in setMediaFromStream");
        try{
            mMedia = new CastMedia();
            if (mMedia.getTitle() == null){
                mMedia.setTitle(title);
                mMedia.setVideoUrl(videoUrl);
            }
        }catch (Exception e){
            Log.e(TAG, "setMediaFromStream Exception: " + e);
        }
    }

    /**
     * Updates the status of the currently playing video in the dedicated message view.
     */
    public void updateStatus() {
        logVIfEnabled("in updateStatus");
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logVIfEnabled("*******Inside run()");

                try {
                    setMediaRouteButtonVisible();
                    if (mCurrentRoute  == null) return;  //DC

                    if (mMessageStream != null) {
                        String msgStreamUrl = mMessageStream.getContentId();
                        logVIfEnabled("messagestream videourl=" + msgStreamUrl);
                        String videoViewUrl = (String)mVideoView.getTag();
                        logVIfEnabled("mVideoView.gettag=" + videoViewUrl);
                        if (msgStreamUrl != null){
                            if (!msgStreamUrl.equals(videoViewUrl)){
                                mVideoView.setVideoPath(msgStreamUrl);
                                mVideoView.setTag(msgStreamUrl);
                                logVIfEnabled("setVideoPath to: " + msgStreamUrl);
                                mVideoView.requestFocus();
                                mVideoView.start();
                                mVideoView.pause();
                            }

                            if (!mVideoView.isPlaying()){
                                int streamPosition = (Double.valueOf(mMessageStream.getStreamPosition()*1000)).intValue();
                                logVIfEnabled("streamposition = " + streamPosition);
                                mVideoView.seekTo(streamPosition);
                            }

                            if (mMedia == null || mMedia.getTitle() == null){
                                setMediaFromStream(mMessageStream.getTitle(), msgStreamUrl);
                            }
                        } else {
                            logVIfEnabled("mMessageStream.getcontentID is null");
                        }

                        mStatus = mMessageStream.requestStatus();

                        String currentStatus = "Player State: " + mMessageStream.getPlayerState() + "\n";
                        currentStatus += "Device " + mSelectedDevice.getFriendlyName() + "\n";
                        currentStatus += "Title " + mMessageStream.getTitle() + "\n";
                        currentStatus += "Current Position: " + mMessageStream.getStreamPosition() + "\n";
                        currentStatus += "Duration: " + mMessageStream.getStreamDuration() + "\n";
                        currentStatus += "Volume set at: " + (mMessageStream.getVolume() * 100) + "%\n";
                        currentStatus += "requestStatus: " + mStatus.getType() + "\n";
                        //mStatusText.setText(currentStatus);
                    } else {
                        //mStatusText.setText(getResources().getString(R.string.tap_icon));
                        Log.e(TAG, "updateStatus.mMessageStream == null");
                    }

                    updateCurrentlyPlaying();

                } catch (Exception e) {
                    Log.e(TAG, "Status request failed: " + e);
                }
            }
        });
    }

    /**
     * Sets the Cast Device Selection button to visible or not, depending on the availability of
     * devices.
     */
    protected final void setMediaRouteButtonVisible() {
        try{
            logVIfEnabled("in setMediaRouteButtonVisible()");
            mMediaRouteButton.setVisibility(View.VISIBLE);
        } catch (Exception e){
            Log.e(TAG, "setMediaRouteButtonVisible. " + e);
        }
    }

    /**
     * Updates a view with the title of the currently playing media.
     */
    protected void updateCurrentlyPlaying() {
        logVIfEnabled("in updateCurrentlyPlaying");
        try{
            if (mMedia != null && mMedia.getTitle() != null) {
                String playing = "<font color=#0066FF>Media Selected: " + mMedia.getTitle() + "</font>";

                if (mMessageStream != null) {
                    String colorString = "<font color=\"red\">";
                    colorString += ". Casting to: " + mSelectedDevice.getFriendlyName();
                    colorString += "</font>";
                    playing += colorString;
                }
                mCurrentlyPlaying.setText(Html.fromHtml(playing));
            } else {
                String castString = "<font color=#FF0000>";
                castString += getResources().getString(R.string.tap_to_select);
                castString += "</font>";
                mCurrentlyPlaying.setText(Html.fromHtml(castString));
            }
        } catch (Exception e){
            Log.e(TAG, "Exception in updateCurrentlyPlaying: " + e);
        }
    }

    public void onQueryCastMediaTaskCompleted(){
        logVIfEnabled("in onQueryCastMediaTaskCompleted");
        mVideos = mQueryCastMediaTask.getCastMediaList();
        (mMediaAdapter = new MediaAdapter(this)).populateMediaList();
        mDownloadImagesTask = new DownloadImagesTask(this, getImageUrlList());
        mDownloadImagesTask.execute(); //get the bitmaps for each of the videos
    }
    public void onDownloadImagesTaskCompleted(){
        logVIfEnabled("in onDownloadImagesTaskCompleted");

        mImages = mDownloadImagesTask.getBitmapList();
        mMediaAdapter.setImagesDownloaded(true);
        mMediaAdapter.refreshMediaList();
    }
    private ArrayList<String> getImageUrlList(){
        logVIfEnabled("in getImageUrlList");
        ArrayList<String> imageUrlList = new ArrayList<String>();
        for (CastMedia castMedia : mVideos){
            imageUrlList.add(castMedia.getImageUrl());
        }
        return imageUrlList;
    }
    public ApplicationSession getSession() { return mSession; }
    public void setMessageStream(MediaProtocolMessageStream messageStream) {this.mMessageStream = messageStream;}
    public void setSelectedDevice(CastDevice selectedDevice) {this.mSelectedDevice = selectedDevice;}
    public void setCurrentItemId(String currentItemId) {this.mCurrentItemId = currentItemId;}
    public String getCurrentItemId() {return mCurrentItemId;}
    public void setCurrentRoute(MediaRouter.RouteInfo currentRoute) {this.mCurrentRoute = currentRoute;}
    public MediaRouter.RouteInfo getCurrentRoute(){return mCurrentRoute;}
    public SharedPreferences getSharedPreferences() {return mSharedPreferences; }
    public boolean isActivityStopped() {
        return mActivityStopped;
    }
    public void setVideos(List<CastMedia> mVideos) {
        this.mVideos = mVideos;
    }
    public List<CastMedia> getVideos() {
        if (mVideos == null) mVideos = new ArrayList<CastMedia>();
        return mVideos;
    }
    public ArrayList<Bitmap> getImages() {return mImages; }
    public void setImages(ArrayList<Bitmap> mImages) {this.mImages = mImages;}
}
