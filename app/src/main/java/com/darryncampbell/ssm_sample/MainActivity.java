package com.darryncampbell.ssm_sample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.ActionBar;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.SerializationUtils;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String AUTHORITY = "content://com.zebra.securestoragemanager.securecontentprovider/data";
    private static final String COLUMN_ORIG_APP_PACKAGE = "orig_app_package";
    private static final String COLUMN_TARGET_APP_PACKAGE = "target_app_package";
    private static final String COLUMN_DATA_NAME = "data_name";
    private static final String COLUMN_DATA_VALUE = "data_value";
    private static final String COLUMN_DATA_INPUT_FORM = "data_input_form";
    private static final String COLUMN_DATA_OUTPUT_FORM = "data_output_form";
    private static final String COLUMN_DATA_PERSIST_REQUIRED = "data_persist_required";
    private static final String COLUMN_MULTI_INSTANCE_REQUIRED = "multi_instance_required";

    private String currentPackage = "";
    Uri cpUri;
    TextView txtName;
    TextView txtValue;
    TextView dataStorageResult;
    TextView queryResult;
    Switch switchPersistence;
    private final String LOG_TAG = "SSM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.header_layout);
        dataStorageResult = findViewById(R.id.txtDataStorageResult);
        queryResult = findViewById(R.id.txtQueryResult);
        currentPackage = getPackageName();

        cpUri = Uri.parse(AUTHORITY);
        //  Check the content provider will resolve - useful for root causing issues.
        ContentProviderClient cpClient = getContentResolver().acquireContentProviderClient(cpUri);
        if (cpClient == null)
        {
            String message = "Unable to create content resolver, please check your manifest";
            Log.e(LOG_TAG, message);
            dataStorageResult.setText(message);
        }

        Button btnInsert = findViewById(R.id.btnInsert);
        btnInsert.setOnClickListener(this);
        Button btnUpdate = findViewById(R.id.btnUpdate);
        btnUpdate.setOnClickListener(this);
        Button btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(this);
        Button btnQuery = findViewById(R.id.btnQuery);
        btnQuery.setOnClickListener(this);
        txtName = findViewById(R.id.editName);
        txtValue = findViewById(R.id.editValue);
        switchPersistence = findViewById(R.id.switchPersistence);

        populatePackagesUI();
    }

    private void populatePackagesUI() {
        TextView txtPackage1 = findViewById(R.id.txtPackage1);
        TextView txtPackage2 = findViewById(R.id.txtPackage2);
        TextView txtPackageSig1 = findViewById(R.id.txtSignature1);
        TextView txtPackageSig2 = findViewById(R.id.txtSignature2);
        txtPackage1.setText(currentPackage);
        txtPackage2.setText(BuildConfig.OtherAppId);
        txtPackageSig1.setText(getCurrentPackageSignature());
        txtPackageSig2.setText(getCurrentPackageSignature());   //  This is the same because I am building them both with the same developer key
    }

    private void insert()
    {
        try
        {
            String key = txtName.getText().toString();
            if (key.trim().equals(""))
            {
                dataStorageResult.setText("Name cannot be blank");
                Log.e(LOG_TAG, "Name cannot be blank");
                return;
            }
            String value = txtValue.getText().toString();
            if (value.trim().equals(""))
            {
                dataStorageResult.setText("Value cannot be blank");
                Log.e(LOG_TAG, "Value cannot be blank");
                return;
            }
            ContentValues values = new ContentValues();
            values.put(COLUMN_TARGET_APP_PACKAGE, getAuthorizedPackages());
            values.put(COLUMN_DATA_NAME, key);
            String persistData = switchPersistence.isChecked() ? "true" : "false";
            values.put(COLUMN_DATA_PERSIST_REQUIRED, persistData);
            values.put(COLUMN_DATA_VALUE, value);
            values.put(COLUMN_DATA_INPUT_FORM, "1"); //  plain text = 1, encrypted = 2
            values.put(COLUMN_DATA_OUTPUT_FORM, "1");  //  plain text = 1, encrypted = 2, keystrokes = 3
            values.put(COLUMN_MULTI_INSTANCE_REQUIRED, "false"); //  Not implemented in this sample

            Uri createdRow = getContentResolver().insert(cpUri, values);
            String message = "Inserted item at: " + createdRow.toString();
            dataStorageResult.setText(message);
            Log.i(LOG_TAG, message);
        }
        catch (Exception e)
        {
            String message = "Error Inserting: " + e.getMessage();
            Log.e(LOG_TAG, message);
            dataStorageResult.setText(message);
        }
    }

    private void update()
    {
        //  https://developer.android.com/reference/android/content/ContentProvider#update(android.net.Uri,%20android.content.ContentValues,%20java.lang.String,%20java.lang.String[])
        try {
            String key = txtName.getText().toString();
            if (key.trim().equals(""))
            {
                dataStorageResult.setText("Name cannot be blank");
                Log.e(LOG_TAG, "Name cannot be blank");
                return;
            }
            String value = txtValue.getText().toString();
            if (value.trim().equals(""))
            {
                dataStorageResult.setText("Value cannot be blank");
                Log.e(LOG_TAG, "Value cannot be blank");
                return;
            }
            ContentValues values = new ContentValues();
            values.put(COLUMN_TARGET_APP_PACKAGE, getAuthorizedPackages());
            values.put(COLUMN_DATA_NAME, key);
            values.put(COLUMN_DATA_VALUE, value);
            String persistData = switchPersistence.isChecked() ? "true" : "false";
            values.put(COLUMN_DATA_PERSIST_REQUIRED, persistData);

            int rowNumbers = getContentResolver().update(cpUri, values, null , null);
            String message = "Records updated: " + rowNumbers;
            Log.i(LOG_TAG, message);
            dataStorageResult.setText(message);
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "Error Updating: " + e.getMessage());
        }

    }

    private void delete()
    {
        //  https://developer.android.com/reference/android/content/ContentProvider#delete(android.net.Uri,%20java.lang.String,%20java.lang.String[])
        try{
            String persistData = switchPersistence.isChecked() ? "true" : "false";
            Uri cpUriDelete = Uri.parse(AUTHORITY + "/[" + currentPackage + "]");
            String selection = COLUMN_TARGET_APP_PACKAGE + " = '" + currentPackage + "'" + "AND " + "data_persist_required = '" + persistData + "'";
            int rowsAffected = getContentResolver().delete(cpUriDelete, selection , null);
            String message = "Deleted " + rowsAffected + " rows";
            dataStorageResult.setText(message);
            Log.i(LOG_TAG, message);
        }catch(Exception e){
            String message = "Delete - error: " + e.getMessage();
            Log.e(LOG_TAG, message);
            dataStorageResult.setText(message);
        }
    }

    private void query()
    {
        //  https://developer.android.com/reference/android/content/ContentProvider#query(android.net.Uri,%20java.lang.String[],%20java.lang.String,%20java.lang.String[],%20java.lang.String)
        Uri cpUriQuery = Uri.parse(AUTHORITY + "/[" + currentPackage + "]");
        String persistData = switchPersistence.isChecked() ? "true" : "false";
        String selection = COLUMN_TARGET_APP_PACKAGE + " = '" + currentPackage + "'" + "AND " + "data_persist_required = '" + persistData + "'";
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(cpUriQuery, null, selection, null, null);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Cursor Query Error: " + e.getMessage());
            return;
        }

        try {
            if (cursor != null && cursor.moveToFirst()) {
                StringBuilder strBuild = new StringBuilder();
                String queryResults = "Entries found: " + cursor.getCount() + "\n";
                while (!cursor.isAfterLast()) {
                    String record = "\n";
                    record += "Original app: " + cursor.getString(cursor.getColumnIndex(COLUMN_ORIG_APP_PACKAGE)) + "\n";
                    record += "Target app: " + cursor.getString(cursor.getColumnIndex(COLUMN_TARGET_APP_PACKAGE)) + "\n";
                    record += "Data Name : " + cursor.getString(cursor.getColumnIndex(COLUMN_DATA_NAME)) + "\n";
                    record += "Data Value: " + cursor.getString(cursor.getColumnIndex(COLUMN_DATA_VALUE)) + "\n";
                    record += "Input  form: " + convertInputForm(cursor.getString(cursor.getColumnIndex(COLUMN_DATA_INPUT_FORM))) + "\n";
                    record += "Output form: " + convertOutputForm(cursor.getString(cursor.getColumnIndex(COLUMN_DATA_OUTPUT_FORM))) + "\n";
                    record += "Persistent?: " + cursor.getString(cursor.getColumnIndex(COLUMN_DATA_PERSIST_REQUIRED)) + "\n";
                    queryResults += record;
                    cursor.moveToNext();
                }
                Log.d(LOG_TAG, "Query data: " + strBuild);
                queryResult.setText(queryResults);
            } else {
                Log.i(LOG_TAG, "No Records Found");
                queryResult.setText("No Records Found");
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "Query data error: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String convertInputForm(String inputForm)
    {
        if (inputForm.equals("1"))
            return "plain text";
        else if (inputForm.equals("2"))
            return "encrypted";
        else
            return "unknown";
    }
    private String convertOutputForm(String outputForm)
    {
        if (outputForm.equals("1"))
            return "plain text";
        else if (outputForm.equals("2"))
            return "encrypted";
        else if (outputForm.equals("3"))
            return "keystrokes";
        else
            return "unknown";
    }

    //  This logic is specific to this app, assumption is we are targetting ourselves
    //  (both flavours, total 2 packages).  Another assumption is that both app flavours have the same
    //  signing key.
    private String getAuthorizedPackages() {
        String otherID = BuildConfig.OtherAppId;
        //  Return a JSON structure defining the package names and signatures that have permission to access the SSM data
        String targetAppPackageContent =
                "{\"pkgs_sigs\": [" +
                        "{\"pkg\":\"" + currentPackage + "\",\"sig\":\"" + getCurrentPackageSignature() + "\"}," +
                        "{\"pkg\":\"" + otherID + "\",\"sig\":\"" + getCurrentPackageSignature() + "\"}" +
                        "]}";
        return targetAppPackageContent;
    }

    //  This is the signing certificate for the package, expressed in Base 64.
    //  See also https://github.com/darryncampbell/MX-SignatureAuthentication-Demo
    private String getCurrentPackageSignature() {
        Signature[] sigs;
        SigningInfo signingInfo;
        try {
            signingInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES).signingInfo;
            sigs = signingInfo.getApkContentsSigners();
            if (sigs.length > 0) {
                String sigAsHex = sigs[0].toCharsString();
                byte[] decodedHex = Hex.decodeHex(sigAsHex);
                byte[] encodedHexB64 = Base64.encodeBase64(decodedHex);
                Log.d(LOG_TAG, "Signature: " + encodedHexB64);
                return new String(encodedHexB64);
            } else
            {
                Log.e(LOG_TAG, "Could not get signature");
                return "Could not get signature";
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "Could not get signature");
            return "Could not get signature";
        }
         catch (DecoderException e) {
            Log.e(LOG_TAG, "Could not get signature");
            return "Could not get signature";
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnInsert)
            insert();
        else if (view.getId() == R.id.btnUpdate)
            update();
        else if (view.getId() == R.id.btnDelete)
            delete();
        else if (view.getId() == R.id.btnQuery)
            query();
    }


}
