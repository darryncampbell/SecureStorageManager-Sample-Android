package com.darryncampbell.emdk_ssm_sample;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //  todo Question: To delete a name/value inserted and shared between two apps I need to call delete twice, once for each content provider, is this how it is supposed to work?
    //  todo Question: Why do I NEED to specify the data_persist_required element in query selection clause?  If I don't specify this, the cursor query returns nothing.  Am I using it correctly?
    //  todo Question: How can we delete a specific value?
    //  todo Question: When will multi-instance be implemented?
    //  todo Logging
    //  todo Test exchanging data between the two app flavours
    //  todo Test persistence
    //  todo Encryption of inserted data & reading it back
    //  todo Tidy code & Update Readme

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

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        Switch switchInputDataFormat = findViewById(R.id.switchEncryptInput);
        switchInputDataFormat.setEnabled(false);
        Spinner spinnerOutputFormat = findViewById(R.id.spinnerOutputDataFormat);
        spinnerOutputFormat.setEnabled(false);
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
                return;
            }
            String value = txtValue.getText().toString();
            if (value.trim().equals(""))
            {
                dataStorageResult.setText("Value cannot be blank");
                return;
            }
            ContentValues values = new ContentValues();
            values.put(COLUMN_TARGET_APP_PACKAGE, getAuthorizedPackages());
            values.put(COLUMN_DATA_NAME, key);
            values.put(COLUMN_DATA_VALUE, value);
            String persistData = switchPersistence.isChecked() ? "true" : "false";
            values.put(COLUMN_DATA_PERSIST_REQUIRED, persistData);
            Switch switchInputDataFormat = findViewById(R.id.switchEncryptInput);
            String inputDataFormat = switchInputDataFormat.isChecked() ? "2" : "1";
            values.put(COLUMN_DATA_INPUT_FORM, inputDataFormat); //plain text =1
            Spinner spinnerOutputFormat = findViewById(R.id.spinnerOutputDataFormat);
            String outputFormat = "1"; //  plain text
            switch (spinnerOutputFormat.getSelectedItemPosition())
            {
                case (1):
                    outputFormat = "2"; //  Encrypted
                    break;
                case (2):
                    outputFormat = "3"; //  Keystrokes
                    break;
            }
            values.put(COLUMN_DATA_OUTPUT_FORM, outputFormat);
            values.put(COLUMN_MULTI_INSTANCE_REQUIRED, "false"); //  Not implemented yet

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
                return;
            }
            String value = txtValue.getText().toString();
            if (value.trim().equals(""))
            {
                dataStorageResult.setText("Value cannot be blank");
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
            String whereClauseAll = null;
            //  Other where clause examples:
            String whereClauseAllWithSelectedPersistence = COLUMN_TARGET_APP_PACKAGE + " = '" + currentPackage + "' AND " + COLUMN_DATA_PERSIST_REQUIRED + " = '" + persistData + "'";
            String whereClauseSpecificKey = COLUMN_TARGET_APP_PACKAGE + " = '" + currentPackage + "' AND " + COLUMN_DATA_NAME + " = 'key'";
            String whereClauseSpecificValue = COLUMN_TARGET_APP_PACKAGE + " = '" + currentPackage + "' AND " + COLUMN_DATA_VALUE + " = 'data'";
            int rowsAffected = getContentResolver().delete(cpUriDelete, whereClauseAll , null);
            dataStorageResult.setText("Deleted " + rowsAffected + " rows");
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
        }

        try {
            if (cursor != null && cursor.moveToFirst()) {
                StringBuilder strBuild = new StringBuilder();
                String queryResults = "Entries found: " + cursor.getCount() + "\n";
                while (!cursor.isAfterLast()) {
                    String record = "\n";
                    record += "Original app: " + cursor.getString(cursor.getColumnIndex(COLUMN_ORIG_APP_PACKAGE)) + "\n";
                    record += "Target app: " + cursor.getString(cursor.getColumnIndex(COLUMN_ORIG_APP_PACKAGE)) + "\n";
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

    /*
     * Get string as JSON array of target package name and base64 signature.
     * Param - target package name as string
     * @return - String as JSON array of target package name and base64 signature
     * */
    private String getAuthorizedPackages() {
        String otherID = BuildConfig.OtherAppId;
        String targetAppPackageContent =
                "{\"pkgs_sigs\": [" +
                        "{\"pkg\":\"" + currentPackage + "\",\"sig\":\"" + getCurrentPackageSignature() + "\"}," +
                        "{\"pkg\":\"" + otherID + "\",\"sig\":\"" + getCurrentPackageSignature() + "\"}" +
                        "]}";
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
        else if (view.getId() == R.id.btnUpdate)
            update();
        else if (view.getId() == R.id.btnDelete)
            delete();
        else if (view.getId() == R.id.btnQuery)
            query();
    }
}
