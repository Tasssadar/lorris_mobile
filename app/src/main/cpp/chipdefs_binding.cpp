#include <jni.h>
#include "chipdefs.h"

extern "C" {
    JNIEXPORT jlong JNICALL Java_com_tassadar_lorrismobile_programmer_ChipDefinition_newNative(JNIEnv *env, jobject obj, jstring sign_str);
    JNIEXPORT void JNICALL Java_com_tassadar_lorrismobile_programmer_ChipDefinition_deleteNative(JNIEnv *env, jobject obj, jlong def_ptr);
    JNIEXPORT jboolean JNICALL Java_com_tassadar_lorrismobile_programmer_ChipDefinition_loadChipdefs(JNIEnv *env, jobject obj, jstring data_str);
    JNIEXPORT jstring JNICALL Java_com_tassadar_lorrismobile_programmer_ChipDefinition_getNameNative(JNIEnv *env, jobject obj, jlong def_ptr);
    JNIEXPORT jint JNICALL Java_com_tassadar_lorrismobile_programmer_ChipDefinition_getMemSizeNative(JNIEnv *env, jobject obj, jlong def_ptr, jint memId);
    JNIEXPORT jint JNICALL Java_com_tassadar_lorrismobile_programmer_ChipDefinition_getMemPageSizeNative(JNIEnv *env, jobject obj, jlong def_ptr, jint memId);
};

JNIEXPORT jlong JNICALL Java_com_tassadar_lorrismobile_programmer_ChipDefinition_newNative(JNIEnv *env, jobject obj, jstring sign_str)
{
    const char *sign = env->GetStringUTFChars(sign_str, NULL);
    chip_definition *def = get_chipdef(sign);
    env->ReleaseStringUTFChars(sign_str, sign);
    return (jlong)def;
}

JNIEXPORT void JNICALL Java_com_tassadar_lorrismobile_programmer_ChipDefinition_deleteNative(JNIEnv *env, jobject obj, jlong def_ptr)
{
    chip_definition *def = (chip_definition*)def_ptr;
    delete def;
}

JNIEXPORT jboolean JNICALL Java_com_tassadar_lorrismobile_programmer_ChipDefinition_loadChipdefs(JNIEnv *env, jobject obj, jstring data_str)
{   
    jboolean res = true;
    const char *data = env->GetStringUTFChars(data_str, NULL);

    try {
        load_chipdefs((char*)data);
    } catch(const char *ex) {
        res = false;
    }
   
    env->ReleaseStringUTFChars(data_str, data);
    return res;
}

JNIEXPORT jstring JNICALL Java_com_tassadar_lorrismobile_programmer_ChipDefinition_getNameNative(JNIEnv *env, jobject obj, jlong def_ptr)
{
    chip_definition *def = (chip_definition*)def_ptr;
    return env->NewStringUTF(def->getName().c_str());
}

JNIEXPORT jint JNICALL Java_com_tassadar_lorrismobile_programmer_ChipDefinition_getMemSizeNative(JNIEnv *env, jobject obj, jlong def_ptr, jint memId)
{
    chip_definition *def = (chip_definition*)def_ptr;
    chip_definition::memorydef *m = def->getMemDef(memId);
    if(m)
        return m->size;
    return -1;
}

JNIEXPORT jint JNICALL Java_com_tassadar_lorrismobile_programmer_ChipDefinition_getMemPageSizeNative(JNIEnv *env, jobject obj, jlong def_ptr, jint memId)
{
    chip_definition *def = (chip_definition*)def_ptr;
    chip_definition::memorydef *m = def->getMemDef(memId);
    if(m)
        return m->pagesize;
    return -1;
}
