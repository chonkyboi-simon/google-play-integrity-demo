package com.chonkyboisimon.playintegritydemo.PlayIntegrityHelper;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class IsAppFoundOnGooglePlay implements Runnable{
    private static final String TAG = "IsAppFoundOnGooglePlay";

    String baseURL = "https://play.google.com/store/apps/details?id=";
    private Handler appAvailabilityHandler=null;
    private String packageName;
    boolean isAppFoundOnGooglePlay=false;

    public IsAppFoundOnGooglePlay(Handler appAvailabilityHandler,String packageName) {
        this.appAvailabilityHandler = appAvailabilityHandler;
        this.packageName=packageName;
    }

    @Override
    public void run() {
        Request request = new Request.Builder()
                .url(baseURL + packageName)
                .build();
        OkHttpClient client = new OkHttpClient();
        Call call = client.newCall(request);
        Response response=null;
        try {
            response = call.execute();
            if (200==response.code()) {//based on https://stackoverflow.com/a/30935718
                Log.d(TAG, "doInBackground: package "+packageName+" exists on Google Play Store");
                isAppFoundOnGooglePlay=true;
            } else {
                Log.d(TAG, "doInBackground: package "+packageName+" does not exist on Google Play Store");
                isAppFoundOnGooglePlay=false;
            }
            if (appAvailabilityHandler!=null) {
                Message newMessage = new Message();
                newMessage.obj=isAppFoundOnGooglePlay;
                appAvailabilityHandler.sendMessageDelayed(newMessage,1000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
