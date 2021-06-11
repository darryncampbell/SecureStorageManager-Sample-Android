# SecureStorageManager-Sample-Android
Sample application to show how to use Zebra's Secure Storage Manager

```xml
<uses-permission android:name="com.zebra.securestoragemanager.securecontentprovider.PERMISSION.WRITE"/>
<uses-permission android:name="com.zebra.securestoragemanager.securecontentprovider.PERMISSION.READ"/>
```

```xml
<queries>
    <provider android:authorities="com.zebra.securestoragemanager.securecontentprovider" />
</queries>
```

`gradlew build`