*Please be aware that this application / sample is provided as-is for demonstration purposes without any guarantee of support*
=========================================================

# SecureStorageManager-Sample-Android
Sample application to show how to use Zebra's Secure Storage Manager

Secure Storage Manager (SSM) is a feature of Zebra Mobile Computers introduced alongside Android 11 which allows secure storage of data which can then be exchanged between applications and (optionally) persisted across an enterprise reset.  SSM was designed to address some of the use cases no longer possible with the introduction of Android's Scoped Storage model. 

SSM is delivered by a service pre-built into the Android image which interfaces with applications using [Android content providers](https://developer.android.com/guide/topics/providers/content-providers)

The official documentation for Secure Storage Manager is available on TechDocs: [https://techdocs.zebra.com/ssm](https://techdocs.zebra.com/ssm) and the Content Provider API can be found [here](https://techdocs.zebra.com/ssm/1-0/guide/api/).

For more information about how Scoped Storage affects enterprise applications and your available options please see my [previous blog post](https://developer.zebra.com/blog/scoped-storage-enterprise-applications) on the topic.

**This application demonstrates the following:**

- Specifying the package name and signature of the destination application(s)
- Creating a data object (name-value pair)
- Exchanging that name-value pair with another application
- Updating a name-value pair that has been shared with another app
- Deleting a name-value pair
- Persisting the name-value pair across an Enterprise reset.  
  - There is a StageNow barcode to initiate an Enterprise Reset under the /media folder.  Or alternatively, you can initiate an Enterprise reset from the Android settings

**This application does NOT demonstrate the following:**

- Consuming a configuration file (or other binary file) received from Secure Storage Manager
  - Note: to securely exchange a file between two applications you have developed, an [Android file provider](https://developer.android.com/reference/androidx/core/content/FileProvider) can be used
- Encrypting or decrypting data held in Secure Storage Manager
- [RegisterContentObserver](https://developer.android.com/reference/android/content/ContentResolver#registerContentObserver(android.net.Uri,%20boolean,%20android.database.ContentObserver)) to be notified whenever data held by SSM changes.
- 'Chunking' a large data object into smaller objects that can be stored as name-value pairs.
- Communicating between different Android users via SSM (not supported by SSM at the time of writing)
- The advantages / disadvantages of SSM compared to the /enterprise partition.

## Configuring Secure Storage Manager

The following permissions are required in your Manifest to read and write data

```xml
<uses-permission android:name="com.zebra.securestoragemanager.securecontentprovider.PERMISSION.READ"/>
<uses-permission android:name="com.zebra.securestoragemanager.securecontentprovider.PERMISSION.WRITE"/>
```

The following queries provider is required in your Manifest to access SSM on Android 11 and higher.

```xml
<queries>
    <provider android:authorities="com.zebra.securestoragemanager.securecontentprovider" />
</queries>
```
# The Sample app

## Structure of the Sample app

In order to best demonstrate exchange of data between apps, you need more than one app*.

This sample has two app flavors, each flavor generating an app with a different package name, app name and colour scheme.

![Application A](https://github.com/darryncampbell/SecureStorageManager-Sample-Android/raw/main/media/appa_onlaunch.png)
![Application B](https://github.com/darryncampbell/SecureStorageManager-Sample-Android/raw/main/media/appb_onlaunch.png)

To build both app flavours just run  `gradlew build` or use the Build Variants window to select the desired variant and launch via Android Studio.  **Note** this sample assumes that both apps will be signed with the same signature, if you just use the debug builds you will not have to worry about this.

## Using the Sample app

1. Build and install both sample app flavours
2. From app A, modify the `name` and `data` fields
3. Press `Insert` in App A
4. Press `Query` in App A.  Observe that your data is retrieved
5. Launch app B
6. Press `Query` in app B.  Observe that your data is retrieved
7. Launch app A
8. Modify the `value` field and press `Update`
9. Press `Query` in both app A and app B.  Observe that the data is modified
10. Press `Delete All` in app A.
11. Press `Query` in both app A and app B.  Observe that the data is **only** deleted from app A.
12. Press `Delete App` in app B.  Observe using `Query` that the data is now also deleted from app B.

# The Code

## Specifying the application(s) which can receive 

The *Secure* in Secure Storage manager comes from the fact that only specified packages are able to access the data you write.  You must specify both the package name and signature, in base64, of all application(s) which will be able to read the data you have written.  Note that SSM is not bi-directional, only the application writing the data is able to modify it.

Package signatures are used to uniquely identify a package and avoid the possibility of a rogue package masquerading as the intended package simply by mimicking the package name.  To learn more about signatures including how to generate them and convert them I would strongly recommend https://github.com/darryncampbell/MX-SignatureAuthentication-Demo.  TechDocs will point you to the [Zebra App Signature tool](https://techdocs.zebra.com/sigtools/) which, though functional, does not present the full picture.

SSM takes a JSON object defining the package name and signature:

```javascript
{
  "pkgs_sigs": [
    {
      "pkg":"com.myapp.app1",
      "sig":"SGVsbG8g...VGhlcmU="
    },
    {
      "pkg":"com.myapp.app2",
      "sig":"T2JpI...Fdhbg=="
    }
  ]
}
```

## Inserting Data

Inserting data into SSM is achieved as follows:

```java
private String AUTHORITY = "content://com.zebra.securestoragemanager.securecontentprovider/data";
Uri cpUri = Uri.parse(AUTHORITY);
ContentValues values = new ContentValues();
values.put("target_app_package", getPackages());  //  JSON object defined above
values.put("data_name", "key");
values.put("data_value", "value");
values.put("data_persist_required", "false");  //  or true to persist across an Enterprise Reset
values.put("data_input_form", "1"); //  plain text = 1
values.put("data_output_form", "1");  //  plain text = 1
values.put("multi_instance_required", "false"); //  Must be false
Uri createdRow = getContentResolver().insert(cpUri, values);
```

**Usage Notes for Insert Data**
* Persisted data should be considered completely separate from non-persisted data.  You can have two identical keys, one persisted and one not-persisted and SSM will treat these data items separately.
* See also the [techdocs sample](https://techdocs.zebra.com/ssm/1-0/guide/api/#samplecode)
* The size of data is limited to around 10KB per name-value pair.  
* The number of name-value pairs is only limited by the size of the device storage (either internal storage or the /enterprise partition for persistent values).

## Updating Data

```java
private String AUTHORITY = "content://com.zebra.securestoragemanager.securecontentprovider/data";
Uri cpUri = Uri.parse(AUTHORITY);
ContentValues values = new ContentValues();
values.put("target_app_package", getPackages());  //  JSON object defined above
values.put("data_name", "key");
values.put("data_value", "value_modified");
values.put("data_persist_required", "false");  //  or true to persist across an Enterprise Reset
int updatedRecords = getContentResolver().update(cpUri, values, null , null);
```

**Usage Notes for Update Data**
* See also the [techdocs sample](https://techdocs.zebra.com/ssm/1-0/guide/api/#samplecode)
* You must specify whether the data you are updating was previously persisted or not

## Querying Data

```java
private String AUTHORITY = "content://com.zebra.securestoragemanager.securecontentprovider/data";
Uri cpUriQuery = Uri.parse(AUTHORITY + "/[" + getPackageName() + "]");
String selection = "target_app_package = '" + getPackageName() + "'" + "AND " + "data_persist_required = 'false'";
Cursor cursor = null;
cursor = getContentResolver().query(cpUriQuery, null, selection, null, null);
if (cursor != null && cursor.moveToFirst()) {
  StringBuilder strBuild = new StringBuilder();
  String queryResults = "Entries found: " + cursor.getCount() + "\n";
  while (!cursor.isAfterLast()) {
    String record = "\n";
    record += "Original app: " + cursor.getString(cursor.getColumnIndex("orig_app_package")) + "\n";
    record += "Target app: " + cursor.getString(cursor.getColumnIndex("target_app_package")) + "\n";
    record += "Data Name : " + cursor.getString(cursor.getColumnIndex("data_name")) + "\n";
    record += "Data Value: " + cursor.getString(cursor.getColumnIndex("data_value")) + "\n";
    record += "Input  form: " + convertInputForm(cursor.getString(cursor.getColumnIndex("data_input_form"))) + "\n";
    record += "Output form: " + convertOutputForm(cursor.getString(cursor.getColumnIndex("data_output_form"))) + "\n";
    record += "Persistent?: " + cursor.getString(cursor.getColumnIndex("data_persist_required")) + "\n";
    queryResults += record;
    cursor.moveToNext();
  }
  Log.d(LOG_TAG, "Query data: " + strBuild);
} else {
  Log.i(LOG_TAG, "No Records Found");
}
cursor.close();
```

**Usage Notes for Query Data**
* The target_app_package column will only return a single package in the current implementation, even though multiple packages were targeted.
* This app only supports plain text input and output but the input and output form fields are read for completeness


## Deleting Data

```java
private String AUTHORITY = "content://com.zebra.securestoragemanager.securecontentprovider/data";
Uri cpUriDelete = Uri.parse(AUTHORITY + "/[" + getPackageName() + "]");
String selection = "target_app_package = '" + getPackageName() + "'" + "AND " + "data_persist_required = 'false'";  //  or true to persist across an Enterprise Reset
int rowsDeleted = getContentResolver().delete(cpUriDelete, selection , null);
```

**Usage Notes for Delete Data**
* You must specify whether the data you are deleting was previously persisted or not
* You can only delete data associated with your own package name (as defined in the Uri and selection)



*citation needed
