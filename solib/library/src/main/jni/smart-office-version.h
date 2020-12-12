/**
 *
 * Interface for Picsel Version strings.
 *
 * Based on platform/tgv/apps/include/tgv-version.h
 * Copyright (C) Picsel, 2005-2016. All Rights Reserved.
 *
 */


#ifndef SMART_OFFICE_VERSION_H
#define SMART_OFFICE_VERSION_H

#include <time.h>

# ifdef __cplusplus
extern "C" {
# endif

/** Get version information */
typedef enum {
    App_Version_String         = 0x0000,    /**< Get version string */
    App_Version_Expiry         = 0x0001,    /**< Get expiry time    */
    App_Version_ManufactureID  = 0x0002,    /**< Get manufacture ID */
    App_Version_SerialNO       = 0x0003     /**< Get serial number  */
} App_versionString;

/**
 * Get the decoded version string
 *
 * @param type    type of information to retrieve
 *
 * @return res    the @ delimited version string as a character string -
 *                allocated
 */
unsigned char *App_Version_getBuffer(App_versionString type);


/**
 * Get an array of utf8 strings containing the app version info.
 * The array consists of:
 *      Date
 *      Issue
 *      Version
 *      Customer
 * Each of the strings plus the array must be freed by the caller
 */
char **App_Version_getStrings(void);


/**
 * Destroy the array of strings returned by App_getVersionStrings
 */
void App_Version_destroyStrings(char **verString);


/**
 * Get the encoded start/issue time
 *
 * @return res    the encoded start/issue time
 */
time_t App_Version_getStartTime(void);


/**
 * Get the encoded expiry time
 *
 * @return res    the encoded expiry time
 */
time_t App_Version_getExpiryTime(void);


# ifdef __cplusplus
}
# endif

#endif /* SMART_OFFICE_VERSION_H */
