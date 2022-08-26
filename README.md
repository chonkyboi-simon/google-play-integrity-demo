
# About This Project
This project is created for demonstrating the new Googe Play Integrity API including requesting and decrypting the verdict. The project does not rely on a server to call Google Cloud Play Integrity API to decrypt and verify the verdict; the code is implemented in the app, which is not recommended but provided as a demonstration of the flow of decryption.

# Prerequisites

## Google Cloud setup

According to [apps exclusively distributed outside Google play](https://developer.android.com/google/play/integrity/setup#apps-exclusively-distributed-outside-google-play) and [setup sdks](https://developer.android.com/google/play/integrity/setup#sdks), when the app is not available on Google Play, Google cloud project number is required when requesting Play Integrity verdict. A credential (JSON-based key file) of your Google cloud Play Integrity API service account is needed when decrypting the verdict with Google. The credential serves the purpose of authenticating the program that makes the decryption request to Google Cloud. Follow the following step to get your credential and project number:

1.  Choose an existing project or create a new project from the [Google Cloud Console](https://console.cloud.google.com/).
2.  Go to **APIs and services** and select **enable APIs and services**.
3.  Search for **Play Integrity API**. Select it and then select **Enable**.
4.  On the Google Play Integrity API page go to **Credentials -> Create Credentials -> Service Account**. Set a name and leave everything as default.
5.  Click on the service account email created, Go to the **Keys tab -> Add Key -> Create new key**. The JSON key file that downloads automatically is the JSON you need for the global gradle.properties.
6.  To receive and decrypt Integrity API responses, you will need to include your Cloud project number in your requests. You can find this in **Project info** in your Google Cloud Console.

## Global gradle.properties Setup

Your service account credential (JSON key file) and cloud project number are needed in your global gradle.properties, this allows the app to be built with your project info. A sample of the key-value pairs for the JSON key and cloud project number is shown below, please replace those with your own.

    #always add "L" to the end of your project number
    PLAY_INTEGRITY_DEMO_CLOUD_PROJECT_NUMBER=123456789012L    
    #JSON string must be double-escaped so it can be correctly recognized when building the app    
    PLAY_INTEGRITY_DEMO_SERVICE_ACCOUNT_CREDENTIAL_JSON_STR={ \\\"type\\\": \\\"service_account\\\", \\\"project_id\\\": \\\"euphoric-hull-123456\\\", \\\"private_key_id\\\": \\\"494608e6cea175473da0890ad1e79374b5b83a94\\\", \\\"private_key\\\": \\\"-----BEGIN PRIVATE KEY-----\\\\nMIIEvAIBkijxQEI1Cfe5hLQ==\\\\n-----END PRIVATE KEY-----\\\\n\\\", \\\"client_email\\\": \\\"service-account@euphoric-hull-123456.iam.gserviceaccount.com\\\", \\\"client_id\\\": \\\"714531090812645777859\\\", \\\"auth_uri\\\": \\\"https://accounts.google.com/o/oauth2/auth\\\", \\\"token_uri\\\": \\\"https://oauth2.googleapis.com/token\\\", \\\"auth_provider_x509_cert_url\\\": \\\"https://www.googleapis.com/oauth2/v1/certs\\\", \\\"client_x509_cert_url\\\": \\\"https://www.googleapis.com/robot/v1/metadata/x509/service-account%40euphoric-hull-123456.iam.gserviceaccount.com\\\"}

Copy the above key-value pairs and add them to your global gradle.properties file located at:

-   On Windows: C:\Users<you>.gradle\gradle.properties
-   On Mac/Linux: /Users//.gradle/gradle.properties
-   Note that the JSON key string needs to be double escaped before you add it to your global gradle.properties. You can escape your JSON key string at [https://onlinestringtools.com/escape-string](https://onlinestringtools.com/escape-string).

# How does it work?

## Request Play Integrity verdict

The process of requesting Play Integrity verdict is similar to requesting SafetyNet attestation result. This app is not available on Google Play so your cloud project number is required for Google to understand who you are.

    Task<IntegrityTokenResponse> integrityTokenResponse = integrityManager.requestIntegrityToken(
	    IntegrityTokenRequest.builder()
		    //project number is needed if the app is not on Google Play    
		    .setCloudProjectNumber(this.cloudProjectNumber)
			.setNonce(nonce)
			.build());

## Decrypt Play Integrity verdict

After receiving the encrypted verdict, it can be decrypted by calling Google Cloud Play Integrity API which involves client authentication and verdict decryption. If the app is not on Google Play then Google-managed decryption is the only option you have.

    //based on https://github.com/googleapis/google-auth-library-java#explicit-credential-loading    
    GoogleCredentials credentials = GoogleCredentials.fromStream(credentialInputStream);   
          
    //the idea of using requestInitializer to authenticate access Google API comes from: https://github.com/googleapis/google-auth-library-java#using-credentials-with-google-http-client    
    HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
    PlayIntegrity playIntegrity = new PlayIntegrity(httpTransport,jsonFactory,requestInitializer);
    
    //put token string in a token object    
    DecodeIntegrityTokenRequest decodeIntegrityTokenRequest = new DecodeIntegrityTokenRequest();    
    decodeIntegrityTokenRequest.setIntegrityToken(encodedToken);
    
    //create a inteity token decode object with package name and the token
    PlayIntegrity.V1.DecodeIntegrityToken decodeIntegrityToken = playIntegrity.v1().decodeIntegrityToken(this.context.getPackageName(),decodeIntegrityTokenRequest);
        
    //initiate the decoding process
    DecodeIntegrityTokenResponse decodedToken = decodeIntegrityToken.execute();
