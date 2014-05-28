package com.cheruku.android.zatapona;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by cherukumilli on 12/1/13.
 */
public class DownloadImage {
    Bitmap mBitmap = null;

    public DownloadImage(String URL){
        Log.v("DownloadImagesTask", "in DownloadImage");

        try {
            InputStream in = OpenHttpConnection(URL);
            mBitmap = BitmapFactory.decodeStream(in);
            in.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
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

    public Bitmap getBitmap(){
        return mBitmap;
    }
}
