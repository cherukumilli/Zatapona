package com.cheruku.android.zatapona;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Created by cherukumilli on 11/1/13.
 * Takes as input an arraylist of URL's that point to an image in the contructor and
 * Retrieves the bitmaps and stores them in a class member mImageList
 */
public class DownloadImagesTask extends AsyncTask<String, Integer, ArrayList<Bitmap>> {
    private Exception exceptionThrown = null;
    private OnDownloadImagesTaskCompleted mListener = null;
    private ArrayList<Bitmap> mBitmapList = null;
    private ArrayList<String> mImageUrlList = null;

    public DownloadImagesTask(OnDownloadImagesTaskCompleted listener, ArrayList<String> imageUrlList) {
        Log.v("DownloadImagesTask", "in constructor");
        mBitmapList = new ArrayList<Bitmap>();
        mListener = listener;
        mImageUrlList = imageUrlList;
    }

    @Override
    protected ArrayList<Bitmap> doInBackground(String... params) {
        Log.v("DownloadImagesTask", "in doInBackground");
        try {
            for (String imageUrl : mImageUrlList){
                Bitmap bitmap = DownloadImage(imageUrl);
                mBitmapList.add(bitmap);
            }
            return mBitmapList;
        } catch (Exception e) {
            exceptionThrown = e;
            return null;
            //Handle exception in PostExecute
        }
    }

    protected void onPostExecute(ArrayList<Bitmap> imageList) {
        Log.v("DownloadImagesTask", "in onPostExecute");
        // Check if exception was thrown
        if (exceptionThrown != null) {
            Log.e(DownloadImagesTask.class.getName(), "Exception when listing DownloadImagesTask", exceptionThrown);
        }
        //else {
        //    mActivity.setImages(imageList);
        //}
        mListener.onDownloadImagesTaskCompleted();
    }

    protected InputStream OpenHttpConnection(String urlString) throws IOException {
        Log.v("DownloadImagesTask", "in OpenHttpConnection");
        InputStream in = null;
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();

        if (!(conn instanceof HttpURLConnection))
            throw new IOException("Not an HTTP connection");

        try{
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();

            if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
            }
        }
        catch (Exception ex) {
            throw new IOException("Error connecting");
        }
        return in;
    }

    protected Bitmap DownloadImage(String URL){
        Log.v("DownloadImagesTask", "in DownloadImage");
        Bitmap bitmap = null;

        try {
            InputStream in = OpenHttpConnection(URL);
            bitmap = BitmapFactory.decodeStream(in);
            in.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return bitmap;
    }
    public ArrayList<Bitmap> getBitmapList() {
        return mBitmapList;
    }


    public interface OnDownloadImagesTaskCompleted{
        public void onDownloadImagesTaskCompleted();
    }
}
