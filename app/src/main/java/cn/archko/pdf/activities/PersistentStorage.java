//package cn.archko.pdf.activities;
//
///**
// * This file contains an example implementation, using SharedPreferences,
// * of the SOPersistentStorage interface.
// * <p>
// * This class is mandatory for all NUI Editor based applications.
// */
//
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.util.Log;
//
//import java.util.Map;
//
//import com.artifex.sonui.editor.SOPersistentStorage;
//
//public class PersistentStorage implements SOPersistentStorage {
//    private static final String mDebugTag = "ExamplePersistentStorage";
//    private static final boolean mLogDebug = false;
//
//    /**
//     * This method will be called to obtain a storage object for the named
//     * store.<br><br>
//     *
//     * @param context   The application context
//     * @param storeName The data store identifier
//     *
//     * @return The object to be used to manage the persistent store.
//     *
//     */
//    public Object getStorageObject(Context context,
//                                   String storeName) {
//        if (mLogDebug) {
//            Log.d(mDebugTag, "getStorageObject: '" + storeName + "'");
//        }
//
//        SharedPreferences sharedPref =
//                context.getSharedPreferences(storeName, Context.MODE_PRIVATE);
//
//        return sharedPref;
//    }
//
//    /**
//     * This method will be called to set a key/value pair in the store.<br><br>
//     *
//     * @param context   The object to be used to manage the persistent store
//     * @param key       The data key
//     * @param value     The data value
//     *
//     */
//    public void setStringPreference(Object storageObject,
//                                    String key,
//                                    String value) {
//        if (mLogDebug) {
//            Log.d(mDebugTag, "setStringPreference: Key '" + key + "' Value '" +
//                    value + "'");
//        }
//
//        SharedPreferences sharedPrefs = (SharedPreferences) storageObject;
//        SharedPreferences.Editor editor = sharedPrefs.edit();
//
//        editor.putString(key, value);
//        editor.commit();
//    }
//
//    /**
//     * This method will be called to retrieve data, identified  by key, from
//     * the store.<br><br>
//     *
//     * The default value should be returned if the key does not reside in
//     * the store.
//     *
//     * @param context      The object to be used to manage the persistent store
//     * @param key          The data key
//     * @param defaultValue The default data value
//     *
//     * @return The current author name.
//     *
//     */
//    public String getStringPreference(Object storageObject,
//                                      String key,
//                                      String defaultValue) {
//        SharedPreferences sharedPrefs = (SharedPreferences) storageObject;
//        String value = sharedPrefs.getString(key, defaultValue);
//
//        if (mLogDebug) {
//            Log.d(mDebugTag, "getStringPreference: Key '" + key +
//                    "' DefaultValues'" + defaultValue +
//                    "' Value '" + value + "'");
//        }
//
//        return value;
//    }
//
//    /**
//     * This method will be called to retrieve all entries from the persistent
//     * store. <br><br>
//     *
//     * This method is not required to be implemented.
//     *
//     * @param storageObject The object to be used to manage the store.
//     *
//     * @return The retrieved key/value pairs.
//     */
//    public Map<String, ?> getAllStringPreferences(Object storageObject) {
//        if (mLogDebug) {
//            Log.d(mDebugTag, "getAllStringPreferences");
//        }
//
//        SharedPreferences sharedPrefs = (SharedPreferences) storageObject;
//        return sharedPrefs.getAll();
//    }
//
//    /**
//     * This method will be called to remove an entry from the persistent store.
//     * .<br><br>
//     *
//     * This method is not required to be implemented.
//     *
//     * @param storageObject The object to be used to manage the store.
//     * @param key           The data key
//     */
//    public void removePreference(Object storageObject,
//                                 String key) {
//        if (mLogDebug) {
//            Log.d(mDebugTag, "removePreference: Key '" + key + "'");
//        }
//
//        SharedPreferences sharedPrefs = (SharedPreferences) storageObject;
//        SharedPreferences.Editor editor = sharedPrefs.edit();
//
//        editor.remove(key);
//        editor.commit();
//    }
//}
