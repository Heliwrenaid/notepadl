/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_example_notepadl_utils_NativeCryptoUtil */

#ifndef _Included_com_example_notepadl_utils_NativeCryptoUtil
#define _Included_com_example_notepadl_utils_NativeCryptoUtil
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_example_notepadl_utils_NativeCryptoUtil
 * Method:    encrypt
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_example_notepadl_utils_NativeCryptoUtil_encryptInternal
  (JNIEnv *, jobject, jbyteArray, jbyteArray, jbyteArray);

/*
 * Class:     com_example_notepadl_utils_NativeCryptoUtil
 * Method:    decrypt
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_example_notepadl_utils_NativeCryptoUtil_decryptInternal
  (JNIEnv *, jobject, jbyteArray, jbyteArray, jbyteArray);

#ifdef __cplusplus
}
#endif
#endif
