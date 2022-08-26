package com.chonkyboisimon.playintegritydemo.PlayIntegrityHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class CredentialLoader {
    public static InputStream getCredentialInputStream(String credentialString) throws JSONException {
        JSONObject credentialJson = new JSONObject(credentialString);
        return new ByteArrayInputStream(credentialJson.toString().getBytes());
    }
}
