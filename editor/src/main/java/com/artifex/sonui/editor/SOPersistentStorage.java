package com.artifex.sonui.editor;

import android.content.Context;

import java.util.Map;

/**
 * This interface specifies the basis for implementing a class allowing
 * for storage/retrieval of key/value pairs.
 */

public interface SOPersistentStorage
{
    /**
     * This method will be called to obtain a storage object for the named
     * store.<br><br>
     *
     * @param context   The application context
     * @param storeName The data store identifier
     *
     * @return The object to be used to manage the store.
     */
    public Object getStorageObject(Context context,
                                   String  storeName);

    /**
     * This method will be called to set a key/value pair in the store.<br><br>
     *
     * @param storageObject The object to be used to manage the store.
     * @param key           The data key
     * @param value         The data value
     *
     */
    public void setStringPreference(Object storageObject,
                                    String key,
                                    String value);

    /**
     * This method will be called to retrieve data, identified  by key, from
     * the store.<br><br>
     *
     * The default value should be returned if the key does not reside in
     * the store.
     *
     * @param storageObject The object to be used to manage the store.
     * @param key           The data key
     * @param defaultValue  The default data value
     *
     * @return The retrieved string.
     */
    public String getStringPreference(Object storageObject,
                                      String key,
                                      String defaultValue);

    /**
     * This method will be called to retrieve all entries from the persistent
     * store. <br><br>
     *
     * @param storageObject The object to be used to manage the store.
     *
     * @return The retrieved key/value pairs.
     */
    public Map<String,?> getAllStringPreferences(Object storageObject);

    /**
     * This method will be called to remove an entry from the persistent store.
     * .<br><br>
     *
     * @param storageObject The object to be used to manage the store.
     * @param key           The data key
     */
    public void removePreference(Object storageObject,
                                 String key);
}
