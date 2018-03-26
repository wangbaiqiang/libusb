#include <jni.h>

#ifndef _Included_com_wbq_libusb_bulkTransfer
#define _Included_com_wbq_libusb_bulkTransfer
#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT void JNICALL
Java_com_hipad_libusb_activity_MainActivity_libBulkTransfer(
        JNIEnv *env, jobject jobj, jint pid, jint vid);
#ifdef __cplusplus
}
#endif
#endif