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
- Persisting the name-value pair across an Enterprise reset

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

## Structure of the Sample app

In order to best demonstrate exchange of data between apps, you need more than one app*.

This sample has two app flavors, each flavor generating an app with a different package name, app name and colour scheme.

![Application A](/assets/images/tux.png)



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








`gradlew build`

## Techdocs


## Notes from top of file:
    //  todo Note: To delete a key shared between two apps I need to call delete twice, once for each content provider
    //  todo Note: When deleting or querying a key, you need specify the data_persist_required element in query selection clause.
    //  todo Note: The TARGET_APP column during query() returns a single package but the TARGET_APP column in insert takes multiple packages.
    //  todo Note: This sample app does not cover file persistence.
    //  todo Question: Is there a limit on the size of the SSM an app can store?  Is there a limit on each value vs. the overall size?
    //  todo Implement file observer to monitor the value of a content provider --> https://stackoverflow.com/questions/3436682/android-how-to-receive-callback-from-content-provider-when-data-chanandroid ges
    //  todo Implement encryption when this is fully documented in techdocs.
    //  todo Implement multi-instance when this is fully implemented & documented in techdocs.
    //  todo Suggestion It be possible to clone the SSM files from one device to another.


## Other notes
- A11 changes
- Functions not included



*citation needed