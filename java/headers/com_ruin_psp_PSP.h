/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_ruin_psp_PSP */

#ifndef _Included_com_ruin_psp_PSP
#define _Included_com_ruin_psp_PSP
#ifdef __cplusplus
extern "C" {
#endif
#undef com_ruin_psp_PSP_numSkills
#define com_ruin_psp_PSP_numSkills 577L
#undef com_ruin_psp_PSP_skillOffset
#define com_ruin_psp_PSP_skillOffset 17274844L
/*
 * Class:     com_ruin_psp_PSP
 * Method:    greetSelf
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_ruin_psp_PSP_greetSelf
  (JNIEnv *, jclass);

/*
 * Class:     com_ruin_psp_PSP
 * Method:    startEmulator
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_ruin_psp_PSP_startEmulator
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_ruin_psp_PSP
 * Method:    step
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_ruin_psp_PSP_step
  (JNIEnv *, jclass);

/*
 * Class:     com_ruin_psp_PSP
 * Method:    nstep
 * Signature: (IFF)V
 */
JNIEXPORT void JNICALL Java_com_ruin_psp_PSP_nstep
  (JNIEnv *, jclass, jint, jfloat, jfloat);

/*
 * Class:     com_ruin_psp_PSP
 * Method:    shutdown
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_ruin_psp_PSP_shutdown
  (JNIEnv *, jclass);

/*
 * Class:     com_ruin_psp_PSP
 * Method:    loadSaveState
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_ruin_psp_PSP_loadSaveState
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_ruin_psp_PSP
 * Method:    saveSaveState
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_ruin_psp_PSP_saveSaveState
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_ruin_psp_PSP
 * Method:    setFramelimit
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_ruin_psp_PSP_setFramelimit
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     com_ruin_psp_PSP
 * Method:    readRAMU8
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_ruin_psp_PSP_readRAMU8
  (JNIEnv *, jclass, jint);

/*
 * Class:     com_ruin_psp_PSP
 * Method:    readRAMU16
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_ruin_psp_PSP_readRAMU16
  (JNIEnv *, jclass, jint);

/*
 * Class:     com_ruin_psp_PSP
 * Method:    readRAMU32
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_ruin_psp_PSP_readRAMU32
  (JNIEnv *, jclass, jint);

/*
 * Class:     com_ruin_psp_PSP
 * Method:    readRAMU32Float
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_com_ruin_psp_PSP_readRAMU32Float
  (JNIEnv *, jclass, jint);

/*
 * Class:     com_ruin_psp_PSP
 * Method:    readRam
 * Signature: (II)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_ruin_psp_PSP_readRam
  (JNIEnv *, jclass, jint, jint);

#ifdef __cplusplus
}
#endif
#endif
