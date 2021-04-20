package com.darryncampbell.emdk_ssm_sample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //  todo Logging (with app name)
    //  todo Encryption of inserted data
    //  todo create ER barcode for repository
    //  todo Remove other todos & tidy code.
    //  todo Update UI to match Zebra standards
    //  todo create pre-compiled app with different name (new branch) signed with key added to this app, to demo how to share between apps
    //  todo Improve commenting
    //  todo Update Readme

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
    private String currentPackageSignature = "";
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
        setContentView(R.layout.activity_main);
        dataStorageResult = findViewById(R.id.txtDataStorageResult);
        queryResult = findViewById(R.id.txtQueryResult);
        TextView txtCurrentPackage = findViewById(R.id.txtCurrentPackage);
        currentPackage = getPackageName();
        currentPackageSignature = getCurrentPackageSignature();
        txtCurrentPackage.setText(currentPackage);
        TextView txtCurrentPackageSig = findViewById(R.id.txtCurrentPackageSig);
        txtCurrentPackageSig.setText(currentPackageSignature);
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
            values.put(COLUMN_MULTI_INSTANCE_REQUIRED, "false"); //  todo read from UI (not implemented yet)

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
            Uri cpUriDelete = Uri.parse(AUTHORITY + "/[" + currentPackage + "]");
            String whereClauseAll = null;
            //  Other where clause examples:
            String whereClauseNonPersistentOnly = COLUMN_TARGET_APP_PACKAGE + " = '" + currentPackage + "' AND " + COLUMN_DATA_PERSIST_REQUIRED + " = 'false'";
            String whereClauseSpecificKey = COLUMN_TARGET_APP_PACKAGE + " = '" + currentPackage + "' AND " + COLUMN_DATA_NAME + " = 'key'";
            //  todo How can we delete a specific value?
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

        //  todo question: Why do I NEED to specify the data_persist_required element here?  Otherwise the cursor query returns nothing.
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
                String queryResults = "Entries found: " + cursor.getCount() + "\n\n";
                while (!cursor.isAfterLast()) {
                    String record = "";
                    record += "Original app: " + cursor.getString(cursor.getColumnIndex(COLUMN_ORIG_APP_PACKAGE)) + "\n";
                    //  todo can you send the same data to multiple apps?
                    record += "Target app: " + cursor.getString(cursor.getColumnIndex(COLUMN_ORIG_APP_PACKAGE)) + "\n";
                    record += "Data Name : " + cursor.getString(cursor.getColumnIndex(COLUMN_DATA_NAME)) + "\n";
                    record += "Data Value: " + cursor.getString(cursor.getColumnIndex(COLUMN_DATA_VALUE)) + "\n";
                    record += "Input  form: " + convertInputForm(cursor.getString(cursor.getColumnIndex(COLUMN_DATA_INPUT_FORM))) + "\n";
                    record += "Output form: " + cursor.getString(cursor.getColumnIndex(COLUMN_DATA_OUTPUT_FORM)) + "\n";
                    record += "Persistent?: " + cursor.getString(cursor.getColumnIndex(COLUMN_DATA_PERSIST_REQUIRED)) + "\n";
                    record += "\n";
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
        else if (view.getId() == R.id.btnUpdate)
            update();
        else if (view.getId() == R.id.btnDelete)
            delete();
        else if (view.getId() == R.id.btnQuery)
            query();
    }
}
