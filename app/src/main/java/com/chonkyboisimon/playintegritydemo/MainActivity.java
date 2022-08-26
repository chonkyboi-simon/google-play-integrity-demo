package com.chonkyboisimon.playintegritydemo;

import static com.chonkyboisimon.playintegritydemo.JsonStringUtils.getPrettyJsonString;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.chonkyboisimon.playintegritydemo.PlayIntegrityHelper.CredentialLoader;
import com.chonkyboisimon.playintegritydemo.PlayIntegrityHelper.IsAppFoundOnGooglePlay;
import com.chonkyboisimon.playintegritydemo.PlayIntegrityHelper.PlayIntegrityHelper;
import com.chonkyboisimon.playintegritydemo.PlayIntegrityHelper.PlayIntegrityTokenOnlineDecoder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static InputStream CREDENTIAL_INPUT_STREAM;
    private static long CLOUD_PROJECT_NUMBER = BuildConfig.CLOUD_PROJECT_NUMBER;

    Button btn_startPlayIntegrityCheck;
    TextView tv_AppFoundOnGooglePlayStore;
    TextView tv_IntegrityCheckResult;

    private Handler appAvailabilityHandler=new Handler(Looper.myLooper()){
        @Override
        public void handleMessage(Message message) {
            boolean isFound = (boolean) message.obj;
            if (isFound)
                tv_AppFoundOnGooglePlayStore.setText("This app is found on Google Play");
            else
                tv_AppFoundOnGooglePlayStore.setText("This app is NOT found on Google Play");
        }
    };

    private Handler integrityCheckResultHandler=new Handler(Looper.myLooper()){
        @Override
        public void handleMessage(Message message) {
            if (message.arg1== PlayIntegrityTokenOnlineDecoder.TYPE_JSON) {
                JSONObject decodedToken = (JSONObject) message.obj;
                String prettyJsonString = getPrettyJsonString(decodedToken.toString());
                tv_IntegrityCheckResult.setText(tv_IntegrityCheckResult.getText()+"\n"+prettyJsonString);
            } else if (message.arg1==PlayIntegrityTokenOnlineDecoder.TYPE_STRING) {
                String statusUpdate=(String) message.obj;
                tv_IntegrityCheckResult.setText(tv_IntegrityCheckResult.getText()+"\n"+statusUpdate);
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            CREDENTIAL_INPUT_STREAM = CredentialLoader.getCredentialInputStream(BuildConfig.SERVICE_ACCOUNT_CREDENTIAL_JSON_STR);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //setup textview that shows if this app is on Google Play
        tv_AppFoundOnGooglePlayStore=findViewById(R.id.tv_AppFoundOnGooglePlayStore);
        IsAppFoundOnGooglePlay findApp = new IsAppFoundOnGooglePlay(appAvailabilityHandler,getApplicationContext().getPackageName());
        Thread findAppThread = new Thread(findApp);
        findAppThread.start();

        //setup button for integrity check
        btn_startPlayIntegrityCheck=findViewById(R.id.btn_startPlayIntegrityCheck);
        btn_startPlayIntegrityCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tv_IntegrityCheckResult.setText("Waiting for Play Integrity verdict...\n");
                new PlayIntegrityHelper(
                        getApplicationContext(),
                        integrityCheckResultHandler,
                        CREDENTIAL_INPUT_STREAM,
                        CLOUD_PROJECT_NUMBER
                ).startPlayIntegrityCheck();
            }
        });

        //setup textview for integrity check result
        tv_IntegrityCheckResult=findViewById(R.id.tv_IntegrityCheckResult);
        tv_IntegrityCheckResult.setText("Tap button to get Play Integrity verdict.");
    }
}