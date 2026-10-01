// JNI glue for libclash.so.
//
// This file is compiled by cgo together with the Go sources in this package,
// so the JNI symbols below end up in the very same shared object. Kotlin loads
// it with System.loadLibrary("clash").
//
// The Go functions it calls (coreVersion, coreInit, ...) are declared in the
// cgo-generated header `_cgo_export.h`.

#include <jni.h>
#include <stdlib.h>

#include "_cgo_export.h"

JNIEXPORT jstring JNICALL
Java_com_autumn_nyaclash_core_NativeBridge_nativeVersion(JNIEnv *env, jobject thiz) {
    (void) thiz;
    char *version = coreVersion();
    if (version == NULL) {
        return NULL;
    }
    jstring result = (*env)->NewStringUTF(env, version);
    free(version);
    return result;
}

JNIEXPORT void JNICALL
Java_com_autumn_nyaclash_core_NativeBridge_nativeInit(JNIEnv *env, jobject thiz, jstring home) {
    (void) thiz;
    if (home == NULL) {
        return;
    }
    const char *home_chars = (*env)->GetStringUTFChars(env, home, NULL);
    if (home_chars == NULL) {
        return;
    }
    coreInit((char *) home_chars);
    (*env)->ReleaseStringUTFChars(env, home, home_chars);
}
