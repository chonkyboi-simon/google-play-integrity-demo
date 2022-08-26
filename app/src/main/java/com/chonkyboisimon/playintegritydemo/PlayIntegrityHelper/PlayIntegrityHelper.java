package com.chonkyboisimon.playintegritydemo.PlayIntegrityHelper;

import static com.chonkyboisimon.playintegritydemo.PlayIntegrityHelper.PlayIntegrityTokenOnlineDecoder.TYPE_STRING;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.integrity.IntegrityManager;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.IntegrityTokenRequest;
import com.google.android.play.core.integrity.IntegrityTokenResponse;
import com.google.android.play.core.integrity.model.IntegrityErrorCode;

import java.io.InputStream;


public class PlayIntegrityHelper {
    private static final String TAG = "PlayIntegrityHelper";

    private Context context;
    private Handler integrityCheckResultHandler;
    private InputStream credentialInputStream;
    private long cloudProjectNumber;

    public PlayIntegrityHelper(Context context, Handler integrityCheckResultHandler, InputStream credentialInputStream, long cloudProjectNumber) {
        this.context = context;
        this.integrityCheckResultHandler = integrityCheckResultHandler;
        this.credentialInputStream = credentialInputStream;
        this.cloudProjectNumber=cloudProjectNumber;
    }

    public void startPlayIntegrityCheck(){
        String nonce = generateNonce(30);

        // based on https://developer.android.com/google/play/integrity/verdict#request
        // Create an instance of a manager.
        IntegrityManager integrityManager = IntegrityManagerFactory.create(this.context);

        // Request the integrity token by providing a nonce.
        Task<IntegrityTokenResponse> integrityTokenResponse = integrityManager.requestIntegrityToken(
                IntegrityTokenRequest.builder()
                        .setCloudProjectNumber(this.cloudProjectNumber)//project number is needed if the app is not on Google Play
                        .setNonce(nonce)
                        .build());

        //task listeners
        integrityTokenResponse.addOnSuccessListener(new OnSuccessListener<IntegrityTokenResponse>() {
            @Override
            public void onSuccess(IntegrityTokenResponse integrityTokenResponse) {
                //decode Play Integrity token
                String integrityToken = integrityTokenResponse.token();
                Log.d(TAG, "onSuccess: received token from Play Integrity API\n"+integrityToken);

                //update UI
                if (integrityCheckResultHandler!=null) {

                    Message result = new Message();
                    result.arg1 = TYPE_STRING;
                    result.obj = "[0] Obtained encrypted Play Integrity verdict:\n"+integrityToken+"\n";
                    integrityCheckResultHandler.sendMessageDelayed(result, 0);
                }
                new PlayIntegrityTokenOnlineDecoder(context,integrityCheckResultHandler, credentialInputStream).execute(integrityToken);
            }
        });

        integrityTokenResponse.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "onFailure: fail to token from Play Integrity API. Error message: " + getErrorText(e));
            }
        });
    }

    private String generateNonce(int length){
        String nonce = "";
        String allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for(int i = 0; i < length; i++) {
            nonce = nonce.concat(String.valueOf(allowed.charAt((int) Math.floor(Math.random() * allowed.length()))));
        }
        return nonce;
    }


    private String getErrorText(Exception e){
        String msg = e.getMessage();
        if (msg == null){
            return "Unknown Error";
        }

        int errorCode = Integer.parseInt(msg.replaceAll("\n", "").replaceAll(":(.*)", ""));
        switch(errorCode){
            case IntegrityErrorCode.API_NOT_AVAILABLE:
                return "API_NOT_AVAILABLE";
            case IntegrityErrorCode.NO_ERROR:
                return "NO_ERROR";
            case IntegrityErrorCode.INTERNAL_ERROR:
                return "INTERNAL_ERROR";
            case IntegrityErrorCode.NETWORK_ERROR:
                return "NETWORK_ERROR";
            case IntegrityErrorCode.PLAY_STORE_NOT_FOUND:
                return "PLAY_STORE_NOT_FOUND";
            case IntegrityErrorCode.PLAY_STORE_ACCOUNT_NOT_FOUND:
                return "PLAY_STORE_ACCOUNT_NOT_FOUND";
            case IntegrityErrorCode.APP_NOT_INSTALLED:
                return "APP_NOT_INSTALLED";
            case IntegrityErrorCode.PLAY_SERVICES_NOT_FOUND:
                return "PLAY_SERVICES_NOT_FOUND";
            case IntegrityErrorCode.APP_UID_MISMATCH:
                return "APP_UID_MISMATCH";
            case IntegrityErrorCode.TOO_MANY_REQUESTS:
                return "TOO_MANY_REQUESTS";
            case IntegrityErrorCode.CANNOT_BIND_TO_SERVICE:
                return "CANNOT_BIND_TO_SERVICE";
            case IntegrityErrorCode.NONCE_TOO_SHORT:
                return "NONCE_TOO_SHORT";
            case IntegrityErrorCode.NONCE_TOO_LONG:
                return "NONCE_TOO_LONG";
            case IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE:
                return "GOOGLE_SERVER_UNAVAILABLE";
            case IntegrityErrorCode.NONCE_IS_NOT_BASE64:
                return "NONCE_IS_NOT_BASE64";
            default:
                return "Unknown Error";
        }
    }
}
