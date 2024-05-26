#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <openssl/aes.h>
#include "com_example_notepadl_utils_NativeCryptoUtil.h"
#include <jni.h>

#define AES_BLOCK_SIZE 16

void encrypt(const unsigned char *plaintext, int plaintext_len, unsigned char *key, unsigned char *iv, unsigned char *ciphertext) {
    AES_KEY encrypt_key;
    AES_set_encrypt_key(key, 128, &encrypt_key);
    AES_cbc_encrypt(plaintext, ciphertext, plaintext_len, &encrypt_key, iv, AES_ENCRYPT);
}

void decrypt(const unsigned char *ciphertext, int ciphertext_len, unsigned char *key, unsigned char *iv, unsigned char *plaintext) {
    AES_KEY decrypt_key;
    AES_set_decrypt_key(key, 128, &decrypt_key);
    AES_cbc_encrypt(ciphertext, plaintext, ciphertext_len, &decrypt_key, iv, AES_DECRYPT);
}

JNIEXPORT jbyteArray JNICALL Java_com_example_notepadl_utils_NativeCryptoUtil_encryptInternal(JNIEnv *env, jobject obj, jbyteArray input, jbyteArray key, jbyteArray iv) {
    jbyte *input_data = (*env)->GetByteArrayElements(env, input, NULL);
    jsize input_len = (*env)->GetArrayLength(env, input);
    jbyte *key_data = (*env)->GetByteArrayElements(env, key, NULL);
    jbyte *iv_data = (*env)->GetByteArrayElements(env, iv, NULL);

    int plaintext_len = input_len;
    unsigned char ciphertext[plaintext_len + AES_BLOCK_SIZE];
    encrypt((unsigned char *)input_data, plaintext_len, (unsigned char *)key_data, (unsigned char *)iv_data, ciphertext);

    int encrypted_len = plaintext_len;
    jbyteArray result = (*env)->NewByteArray(env, encrypted_len);
    if (result == NULL) {
        return NULL; // OutOfMemoryError already thrown
    }

    (*env)->SetByteArrayRegion(env, result, 0, encrypted_len, (jbyte *)ciphertext);
    (*env)->ReleaseByteArrayElements(env, input, input_data, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, key, key_data, JNI_ABORT);

    return result;
}

JNIEXPORT jbyteArray JNICALL Java_com_example_notepadl_utils_NativeCryptoUtil_decryptInternal(JNIEnv *env, jobject obj, jbyteArray input, jbyteArray key, jbyteArray iv) {
    jbyte *input_data = (*env)->GetByteArrayElements(env, input, NULL);
    jsize input_len = (*env)->GetArrayLength(env, input);
    jbyte *key_data = (*env)->GetByteArrayElements(env, key, NULL);
    jbyte *iv_data = (*env)->GetByteArrayElements(env, iv, NULL);

    int decrypted_len = input_len;
    unsigned char decrypted_text[decrypted_len + AES_BLOCK_SIZE];
    memset(decrypted_text, 0, sizeof(decrypted_text));
    decrypt((unsigned char *)input_data, decrypted_len + AES_BLOCK_SIZE, (unsigned char *)key_data, (unsigned char *)iv_data, decrypted_text);

    jbyteArray result = (*env)->NewByteArray(env, decrypted_len);
    if (result == NULL) {
        return NULL; // OutOfMemoryError already thrown
    }

    (*env)->SetByteArrayRegion(env, result, 0, decrypted_len, (jbyte *)decrypted_text);
    (*env)->ReleaseByteArrayElements(env, input, input_data, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, key, key_data, JNI_ABORT);

    return result;
}
