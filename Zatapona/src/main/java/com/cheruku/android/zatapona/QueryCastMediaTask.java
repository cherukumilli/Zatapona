package com.cheruku.android.zatapona;

import android.os.AsyncTask;
import android.util.Log;

import com.cheruku.android.zatapona.castmediaendpoint.Castmediaendpoint;
import com.cheruku.android.zatapona.castmediaendpoint.model.CastMedia;
import com.cheruku.android.zatapona.castmediaendpoint.model.CollectionResponseCastMedia;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by cherukumilli on 10/29/13.
 */
public class QueryCastMediaTask extends AsyncTask<Void, Void, CollectionResponseCastMedia> {
    private Exception exceptionThrown = null;
    private Castmediaendpoint mCastMediaEndpoint=null;
    private OnQueryCastMediaTaskCompleted mListener=null;
    private List<CastMedia> mCastMediaList = null;

    public QueryCastMediaTask(OnQueryCastMediaTaskCompleted listener) {
        Log.v("QueryCastMediaTask", "in constructor");
        this.mListener = listener;

        Castmediaendpoint.Builder castMediaEndpointBuilder = new Castmediaendpoint.Builder(
                AndroidHttp.newCompatibleTransport(), new JacksonFactory(),
                new HttpRequestInitializer() {
                    public void initialize(HttpRequest httpRequest) {
                    }
                });
        //castMediaEndpointBuilder.setApplicationName("Zatapona");
        mCastMediaEndpoint = CloudEndpointUtils.updateBuilder(castMediaEndpointBuilder).build();
    }

    @Override
    protected CollectionResponseCastMedia doInBackground(Void... params) {
        Log.v("QueryCastMediaTask", "in doInBackground");
        try {
            return mCastMediaEndpoint.listCastMedia().execute();
        } catch (IOException e) {
            exceptionThrown = e;
            return null;
            //Handle exception in PostExecute
        }
    }

    protected void onPostExecute(CollectionResponseCastMedia castMedia) {
        Log.v("QueryCastMediaTask", "in onPostExecute");
        // Check if exception was thrown
        if (exceptionThrown != null) {
            Log.e(QueryCastMediaTask.class.getName(), "Exception when listing castMedia", exceptionThrown);
        } else {
            mCastMediaList = castMedia.getItems();
        }
        mListener.onQueryCastMediaTaskCompleted();
    }

    public List<CastMedia> getCastMediaList() {
        return mCastMediaList;
    }

    public void setOnQueryCastMediaTaskCompleted(OnQueryCastMediaTaskCompleted aOnQueryCastMediaTaskCompleted){
        mListener = aOnQueryCastMediaTaskCompleted;
    }

    public interface OnQueryCastMediaTaskCompleted{
        public void onQueryCastMediaTaskCompleted();
    }
}
