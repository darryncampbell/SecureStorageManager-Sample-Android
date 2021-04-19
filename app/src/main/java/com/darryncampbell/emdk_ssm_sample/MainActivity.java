package com.darryncampbell.emdk_ssm_sample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.database.ContentObserver;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //  todo Remove contentobserver and datasetobserver
    //  todo implement Insert, Update, Clear and Query logic
    //  todo Logging (with app name)
    //  todo Update UI to match Zebra standards
    //  todo create pre-compiled app with different name (new branch) signed with key added to this app, to demo how to share between apps
    //  todo Improve commenting
    //  todo Update Readme

    private static final String AUTHORITY = "content://com.zebra.securestoragemanager.securecontentprovider/data";
    private static final String COLUMN_TARGET_APP_PACKAGE = "target_app_package";
    private static final String COLUMN_KEY = "data_name";
    private static final String COLUMN_DATA_VALUE = "data_value";
    private static final String COLUMN_DATA_INPUT_FORM = "data_input_form";
    private static final String COLUMN_DATA_OUTPUT_FORM = "data_output_form";
    private String COLUMN_DATA_PERSIST_REQUIRED = "data_persist_required";
    private String COLUMN_MULTI_INSTANCE_REQUIRED = "multi_instance_required";

    private String currentPackage = "";
    private String currentPackageSignature = "";
    Uri cpUri;
    ContentProviderClient cpClient;
    LocalContentObserver myContentObserver;
    LocalDataSetObserver myDataSetObserver;
    TextView dataStorageResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView txtCurrentPackage = findViewById(R.id.txtCurrentPackage);
        currentPackage = getPackageName();
        currentPackageSignature = getCurrentPackageSignature();
        txtCurrentPackage.setText(currentPackage);
        TextView txtCurrentPackageSig = findViewById(R.id.txtCurrentPackageSig);
        txtCurrentPackageSig.setText(currentPackageSignature);
        cpUri = Uri.parse(AUTHORITY);
        //  todo this line is causing E/ActivityThread: Failed to find provider info for com.zebra.securestoragemanager.securecontentprovider
        cpClient = getContentResolver().acquireContentProviderClient(cpUri);

        myContentObserver = new LocalContentObserver(null);
        myDataSetObserver = new LocalDataSetObserver();

        dataStorageResult = findViewById(R.id.txtDataStorageResult);
        Button btnInsert = findViewById(R.id.btnInsert);
        btnInsert.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        getContentResolver().registerContentObserver(cpUri, true, myContentObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
//        getContentResolver().unregisterContentObserver(myContentObserver);
    }

    private void insert()
    {
        String key = "the-key"; //  todo read from UI & validate
        String value = "the-value"; //  todo read value from UI & validate
        ContentValues values = new ContentValues();
        values.put(COLUMN_TARGET_APP_PACKAGE, getAuthorizedPackages());
        values.put(COLUMN_KEY, key);
        values.put(COLUMN_DATA_VALUE, value);
        values.put(COLUMN_DATA_INPUT_FORM, "1"); //plaintext =1  //  todo read from UI
        values.put(COLUMN_DATA_OUTPUT_FORM, "1"); //plaintext=1 //  todo read from UI
        values.put(COLUMN_DATA_PERSIST_REQUIRED, "false");  //  todo read from UI
        values.put(COLUMN_MULTI_INSTANCE_REQUIRED, "true"); //  todo read from UI (not implemented yet)

        //  todo catch exception if add duplicate entry
        Uri createdRow = getContentResolver().insert(cpUri, values);
        dataStorageResult.setText(createdRow.toString());
    }

    /*
     * Get string as JSON array of target package name and base64 signature.
     * Param - target package name as string
     * @return - String as JSON array of target package name and base64 signature
     * */
    private String getAuthorizedPackages() {

        String targetAppPackageContent = "{\"pkgs_sigs\": " +
                "[" +
                "{" +
                "\"pkg\":" +
                "\"" +
                currentPackage +
                "\"" +
                "," +
                "\"sig\":" +
                "\"" +
                currentPackageSignature +
                "\"" +
                "}]}";
        return targetAppPackageContent;
    }

    private String getCurrentPackageSignature() {
        Signature[] sigs;
        SigningInfo signingInfo = new SigningInfo();
        try {
            signingInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES).signingInfo;
            sigs = signingInfo.getApkContentsSigners();
            if (sigs.length > 0) {
                String sigAsHex = sigs[0].toCharsString();
                byte[] decodedHex = Hex.decodeHex(sigAsHex);
                byte[] encodedHexB64 = Base64.encodeBase64(decodedHex);
                return new String(encodedHexB64);
            } else
                return "Could not get signature";
        } catch (PackageManager.NameNotFoundException e) {
            return "Could not get signature";
        }
         catch (DecoderException e) {
            return "Could not get signature";
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnInsert)
            insert();
    }
}

class LocalContentObserver extends ContentObserver {
    public LocalContentObserver(Handler handler) {
        super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
        this.onChange(selfChange, null);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
    }
}

class LocalDataSetObserver extends DataSetObserver {
    public LocalDataSetObserver() {

    }

    @Override
    public void onInvalidated() {
        super.onInvalidated();
    }

    @Override
    public void onChanged() {
        super.onChanged();
    }
}
