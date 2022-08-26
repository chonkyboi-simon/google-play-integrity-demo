package com.chonkyboisimon.playintegritydemo.PlayIntegrityHelper;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.playintegrity.v1.PlayIntegrity;
import com.google.api.services.playintegrity.v1.model.DecodeIntegrityTokenRequest;
import com.google.api.services.playintegrity.v1.model.DecodeIntegrityTokenResponse;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

public class PlayIntegrityTokenOnlineDecoder extends AsyncTask<String, Integer, String> {
    private static final String TAG = "GetTokenResponse";
    public static final int TYPE_JSON=1;
    public static final int TYPE_STRING=2;
    private static final int STATUS_UPDATE_CLIENT_AUTH =1;
    private static final int STATUS_UPDATE_DECODE_TOKEN=2;
    private static final int STATUS_UPDATE_TOKEN_DECODED=3;

    private boolean hasError = false;
    private Context context;
    private Handler integrityCheckResultHandler;
    private InputStream credentialInputStream;

    public PlayIntegrityTokenOnlineDecoder(Context context, Handler integrityCheckResultHandler, InputStream credentialInputStream) {
        this.context=context;
        this.integrityCheckResultHandler=integrityCheckResultHandler;
        this.credentialInputStream =credentialInputStream;
    }

    @Override
    protected String doInBackground(String... strings) {
        String encodedToken= strings[0];

        try {
            return decodeTokenWithGoogleCloudApi(encodedToken).toString();
        } catch (GeneralSecurityException e) {
            return e.getCause()+"\n"+e.getMessage();
        } catch (IOException e) {
            return e.getCause()+"\n"+e.getMessage();
        }
    }

    private JSONObject decodeTokenWithGoogleCloudApi(String encodedToken) throws GeneralSecurityException, IOException {
        publishProgress(STATUS_UPDATE_CLIENT_AUTH);
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = new JacksonFactory();

        //based on https://github.com/googleapis/google-auth-library-java#explicit-credential-loading
        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialInputStream);
//            credentials.refreshIfExpired(); // this is not necessary, credential refresh commented out

        //the idea of using requestInitializer to authenticate access Google API comes from: https://github.com/googleapis/google-auth-library-java#using-credentials-with-google-http-client
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        //the followings are my own code
        PlayIntegrity playIntegrity = new PlayIntegrity(httpTransport,jsonFactory,requestInitializer);
        DecodeIntegrityTokenRequest decodeIntegrityTokenRequest = new DecodeIntegrityTokenRequest();
        decodeIntegrityTokenRequest.setIntegrityToken(encodedToken);
        PlayIntegrity.V1.DecodeIntegrityToken decodeIntegrityToken = playIntegrity.v1().decodeIntegrityToken(this.context.getPackageName(),decodeIntegrityTokenRequest);
        publishProgress(STATUS_UPDATE_DECODE_TOKEN);
        DecodeIntegrityTokenResponse decodedToken = decodeIntegrityToken.execute();
        publishProgress(STATUS_UPDATE_TOKEN_DECODED);
        return new JSONObject(decodedToken);
    }

    private void dumpDecodedTokenToLog(JSONObject decodedToken) {
        //note that the decoded token data structure does not match https://developer.android.com/google/play/integrity/verdict#returned-payload-format
        Log.d(TAG, "dumpDecodedTokenToLog: decodedToken= "+decodedToken.toString());
    }
    @Override
    protected void onPostExecute(String decodedTokenInString) {
        JSONObject decodedToken;
        try {
            //when decodedTokenInString is JSON
            decodedToken=new JSONObject(decodedTokenInString);
        } catch (JSONException e) {
            //when decodedTokenInString is not JSON, it contains exception information, put exception information in a new JSON
            e.printStackTrace();
            decodedToken=new JSONObject();
            try {
                decodedToken.put("ExceptionMsg",decodedTokenInString);
            } catch (JSONException ex) {
                ex.printStackTrace();
            }

        }

        dumpDecodedTokenToLog(decodedToken);

        //update UI
        if (integrityCheckResultHandler!=null) {
            Message result = new Message();
            result.arg1=TYPE_JSON;
            result.obj=decodedToken;
            integrityCheckResultHandler.sendMessageDelayed(result,0);
        }
    }

    @Override
    protected void onPreExecute() {
        //update UI
        if (integrityCheckResultHandler!=null) {

            Message result = new Message();
            result.arg1=TYPE_STRING;
            result.obj="[1] Calling Google Cloud Play Integrity API...\n";
            integrityCheckResultHandler.sendMessageDelayed(result,0);
        }
    }


    @Override
    protected void onProgressUpdate(Integer... update) {
        Message result = new Message();
        result.arg1=TYPE_STRING;

        switch (update[0]) {
            case STATUS_UPDATE_CLIENT_AUTH:
                result.obj="[2] Authenticating HTTP client...\n";
                break;
            case STATUS_UPDATE_DECODE_TOKEN:
                result.obj="[3] Requesting token decode...\n";
                break;
            case STATUS_UPDATE_TOKEN_DECODED:
                result.obj="[4] Received decoded token from Google cloud Play Integrity API.";
                break;
            default:
                result.obj="Unknown state\n";
        }
        integrityCheckResultHandler.sendMessageDelayed(result,0);
    }


}
