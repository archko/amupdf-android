#ifndef ANDROID_SECURE_JNI_H
#define ANDROID_SECURE_JNI_H

/* Obtain and hold a reference to the implementation of the SODecureFS if */
JNIEXPORT void JNICALL
JNI_FN(SOLib_initSecureFS)(JNIEnv *env, jobject thiz, jobject impl);

/* Release the reference to the implementation of the SODecureFS if */
JNIEXPORT void JNICALL
JNI_FN(SOLib_finSecureFS)(JNIEnv *env, jobject thiz);

#endif /* ANDROID_SECURE_JNI_H */
