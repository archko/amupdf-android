### This is a pdf reader base on mupdf.

how to use it.
git clone https://github.com/archko/AMupdf.git.
cd to AMupdf dir:

```
git clone https://github.com/archko/viewer.git
```
import AMupdf into android studio.
> only support ndk.abiFilters 'armeabi-v7a', 'arm64-v8a',you can modify reader/build.gradle
> from 4.5.0 remove native cropper,don't need ndk to compile it
> add keystore.properties to Amupdf,key content is :
```
keyAlias=xx_key
storeFile=../xx.jks
keyPassword=your pass
storePassword=your pass 
```
the viewer has been modified for textreflow with image.
the git patch is archko.patch

AMupdf 4.6.2 dependence on https://github.com/archko/viewer.git mupdf_1.18 branch

### features:
* textreflow
* auto crop white column
* multitouch
* zoom
* file browser
* history and favorite 

### how to use the reader module:
create a class AMuPDFRecyclerViewActivity : MuPDFRecyclerViewActivity()

regist activity:
```
<activity
    android:name="xx.AMuPDFRecyclerViewActivity"
    android:configChanges="orientation|keyboardHidden|screenSize"
    android:screenOrientation="sensor">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />

        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />

        <data android:mimeType="application/pdf" />
        <data android:mimeType="application/vnd.ms-xpsdocument" />
        <data android:mimeType="application/oxps" />
        <data android:mimeType="application/x-cbz" />
        <data android:mimeType="application/epub+zip" />
        <data android:mimeType="text/xml" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />

        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />

        <data android:pathPattern=".*\\.pdf" />
        <data android:pathPattern=".*\\.xps" />
        <data android:pathPattern=".*\\.oxps" />
        <data android:pathPattern=".*\\.cbz" />
        <data android:pathPattern=".*\\.epub" />
        <data android:pathPattern=".*\\.fb2" />
    </intent-filter>
</activity>
```

open AMuPDFRecyclerViewActivity with a intent():
``` 
val intent = Intent()
intent.setDataAndType(Uri.fromFile(f), "application/pdf")
intent.setClass(activity!!, AMuPDFRecyclerViewActivity::class.java)
intent.action = "android.intent.action.VIEW"
activity?.startActivity(intent)
```      

### released apks:
- [https://www.coolapk.com/apk/233108](https://www.coolapk.com/apk/233108)
- [https://play.google.com/store/apps/details?id=cn.archko.mupdf](https://play.google.com/store/apps/details?id=cn.archko.mupdf)
