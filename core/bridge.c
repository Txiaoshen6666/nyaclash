// JNI glue for libclash.so.
//
// This file is compiled by cgo together with the Go sources in this package,
// so the JNI symbols below end up in the very same shared object. Kotlin loads
// it with System.loadLibrary("clash").
//
// The Go functions it calls (coreInit, coreStartTun, ...) are declared in the
// cgo-generated header `_cgo_export.h`.

#include <jni.h>
#include <stdlib.h>
#include <string.h>

#include "bridge.h"
#include "_cgo_export.h"

static JavaVM *g_vm = NULL;

static jobject g_tun_callback = NULL;
static jmethodID g_mark_socket = NULL;
static jmethodID g_query_socket_uid = NULL;

static JNIEnv *nyaclash_env(void) {
    if (g_vm == NULL) {
        return NULL;
    }
    JNIEnv *env = NULL;
    jint status = (*g_vm)->GetEnv(g_vm, (void **) &env, JNI_VERSION_1_6);
    if (status == JNI_EDETACHED) {
        if ((*g_vm)->AttachCurrentThread(g_vm, &env, NULL) != JNI_OK) {
            return NULL;
        }
    } else if (status != JNI_OK) {
        return NULL;
    }
    return env;
}

int jni_protect(int fd) {
    if (g_tun_callback == NULL || g_mark_socket == NULL) {
        return -1;
    }
    JNIEnv *env = nyaclash_env();
    if (env == NULL) {
        return -1;
    }
    (*env)->CallVoidMethod(env, g_tun_callback, g_mark_socket, (jint) fd);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        return -1;
    }
    return 0;
}

int jni_query_socket_uid(int protocol, const char *source, const char *target) {
    if (g_tun_callback == NULL || g_query_socket_uid == NULL) {
        return -1;
    }
    JNIEnv *env = nyaclash_env();
    if (env == NULL) {
        return -1;
    }
    jstring j_source = (*env)->NewStringUTF(env, source);
    jstring j_target = (*env)->NewStringUTF(env, target);
    jint uid = (*env)->CallIntMethod(env, g_tun_callback, g_query_socket_uid,
                                     (jint) protocol, j_source, j_target);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        uid = -1;
    }
    (*env)->DeleteLocalRef(env, j_source);
    (*env)->DeleteLocalRef(env, j_target);
    return (int) uid;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void) reserved;
    g_vm = vm;
    return JNI_VERSION_1_6;
}

/* Copy a Java string into a malloc'ed UTF-8 buffer (caller frees). */
static char *dup_utf(JNIEnv *env, jstring s) {
    if (s == NULL) {
        return NULL;
    }
    const char *chars = (*env)->GetStringUTFChars(env, s, NULL);
    if (chars == NULL) {
        return NULL;
    }
    char *copy = strdup(chars);
    (*env)->ReleaseStringUTFChars(env, s, chars);
    return copy;
}

static void clear_tun_callback(JNIEnv *env) {
    if (g_tun_callback != NULL) {
        (*env)->DeleteGlobalRef(env, g_tun_callback);
        g_tun_callback = NULL;
    }
    g_mark_socket = NULL;
    g_query_socket_uid = NULL;
}

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
Java_com_autumn_nyaclash_core_NativeBridge_nativeInit(JNIEnv *env, jobject thiz,
                                                      jstring home, jstring version_name,
                                                      jstring git_version, jint sdk_version) {
    (void) thiz;
    char *h = dup_utf(env, home);
    char *vn = dup_utf(env, version_name);
    char *gv = dup_utf(env, git_version);

    coreInit(h, vn, gv, (int) sdk_version);

    free(h);
    free(vn);
    free(gv);
}

JNIEXPORT void JNICALL
Java_com_autumn_nyaclash_core_NativeBridge_nativeReset(JNIEnv *env, jobject thiz) {
    (void) env;
    (void) thiz;
    coreReset();
}

JNIEXPORT void JNICALL
Java_com_autumn_nyaclash_core_NativeBridge_nativeForceGc(JNIEnv *env, jobject thiz) {
    (void) env;
    (void) thiz;
    coreForceGc();
}

JNIEXPORT jstring JNICALL
Java_com_autumn_nyaclash_core_NativeBridge_nativeLoadConfig(JNIEnv *env, jobject thiz,
                                                            jstring path) {
    (void) thiz;
    char *p = dup_utf(env, path);
    if (p == NULL) {
        return NULL;
    }
    char *err = coreLoadConfig(p);
    free(p);
    if (err == NULL) {
        return NULL;
    }
    jstring result = (*env)->NewStringUTF(env, err);
    free(err);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_autumn_nyaclash_core_NativeBridge_nativeStartTun(JNIEnv *env, jobject thiz,
                                                          jint fd, jstring stack,
                                                          jstring gateway, jstring portal,
                                                          jstring dns, jobject callback) {
    (void) thiz;

    clear_tun_callback(env);
    if (callback != NULL) {
        g_tun_callback = (*env)->NewGlobalRef(env, callback);
        jclass cls = (*env)->GetObjectClass(env, callback);
        g_mark_socket = (*env)->GetMethodID(env, cls, "markSocket", "(I)V");
        g_query_socket_uid = (*env)->GetMethodID(env, cls, "querySocketUid",
                                                 "(ILjava/lang/String;Ljava/lang/String;)I");
        (*env)->DeleteLocalRef(env, cls);
    }

    char *s = dup_utf(env, stack);
    char *g = dup_utf(env, gateway);
    char *p = dup_utf(env, portal);
    char *d = dup_utf(env, dns);

    jint rc = coreStartTun((int) fd, s, g, p, d);

    free(s);
    free(g);
    free(p);
    free(d);
    return rc;
}

JNIEXPORT void JNICALL
Java_com_autumn_nyaclash_core_NativeBridge_nativeStopTun(JNIEnv *env, jobject thiz) {
    (void) thiz;
    coreStopTun();
    clear_tun_callback(env);
}
