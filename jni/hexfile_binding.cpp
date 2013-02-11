#include <jni.h>
#include <vector>
#include <set>
#include "hexfile.h"
#include "chipdefs.h"

extern "C" {
    JNIEXPORT jlong JNICALL Java_com_tassadar_lorrismobile_programmer_HexFile_newNative(JNIEnv *env, jobject obj);
    JNIEXPORT void JNICALL Java_com_tassadar_lorrismobile_programmer_HexFile_deleteNative(JNIEnv *env, jobject obj, jlong hex_pointer);
    JNIEXPORT jstring JNICALL Java_com_tassadar_lorrismobile_programmer_HexFile_loadFileNative(JNIEnv *env, jobject obj, jlong hex_ptr, jstring path_str);
    JNIEXPORT jlong JNICALL Java_com_tassadar_lorrismobile_programmer_HexFile_getSizeNative(JNIEnv *env, jobject obj, jlong hex_ptr);
    JNIEXPORT jstring JNICALL Java_com_tassadar_lorrismobile_programmer_HexFile_makePagesNative(JNIEnv *env, jobject obj, jlong hex_ptr, jlong chipdef_ptr, jint memId);
    JNIEXPORT jboolean JNICALL Java_com_tassadar_lorrismobile_programmer_HexFile_getNextPage(JNIEnv *env, jobject obj, jobject pageObject, jboolean skip);
    JNIEXPORT void JNICALL Java_com_tassadar_lorrismobile_programmer_HexFile_clearPages(JNIEnv *env, jobject obj);
    JNIEXPORT jint JNICALL Java_com_tassadar_lorrismobile_programmer_HexFile_getPagesCount(JNIEnv *env, jobject obj, jboolean skip);
};

JNIEXPORT jlong JNICALL Java_com_tassadar_lorrismobile_programmer_HexFile_newNative(JNIEnv *env, jobject obj)
{
    HexFile *hex = new HexFile();
    return (jlong)hex;
}

JNIEXPORT void JNICALL Java_com_tassadar_lorrismobile_programmer_HexFile_deleteNative(JNIEnv *env, jobject obj, jlong hex_ptr)
{
    HexFile *hex = (HexFile*)hex_ptr;
    delete hex;
}

JNIEXPORT jstring JNICALL Java_com_tassadar_lorrismobile_programmer_HexFile_loadFileNative(JNIEnv *env, jobject obj, jlong hex_ptr, jstring path_str)
{
    HexFile *hex = (HexFile*)hex_ptr;

    const char *path = env->GetStringUTFChars(path_str, NULL);
    
    std::string res;

    try {
        hex->LoadFromFile(path);
    } catch(std::string ex) {
        res = ex;
    }
    env->ReleaseStringUTFChars(path_str, path);
    
    return env->NewStringUTF(res.c_str());
}

JNIEXPORT jlong JNICALL Java_com_tassadar_lorrismobile_programmer_HexFile_getSizeNative(JNIEnv *env, jobject obj, jlong hex_ptr)
{
    HexFile *hex = (HexFile*)hex_ptr;
    return (jlong)hex->getProgSize();
}

static std::vector<page> pages = std::vector<page>();
static std::set<uint32_t> skip_pages = std::set<uint32_t>();
static size_t curPage = 0;

JNIEXPORT jstring JNICALL Java_com_tassadar_lorrismobile_programmer_HexFile_makePagesNative(JNIEnv *env, jobject obj, jlong hex_ptr, jlong chipdef_ptr, jint memId)
{
    pages.clear();
    skip_pages.clear();
    curPage = 0;
    
    HexFile *hex = (HexFile*)hex_ptr;
    chip_definition *chip = (chip_definition*)chipdef_ptr;
    
    try {
        hex->makePages(pages, memId, *chip, memId == MEM_FLASH ? &skip_pages : NULL);
    } catch(std::string ex) {
        return env->NewStringUTF(ex.c_str());
    }
    return env->NewStringUTF("");
}

JNIEXPORT jboolean JNICALL Java_com_tassadar_lorrismobile_programmer_HexFile_getNextPage(JNIEnv *env, jobject obj, jobject pageObject, jboolean skip)
{
    while(curPage < pages.size())
    {
        if(skip && skip_pages.find(curPage) != skip_pages.end())
        {
            ++curPage;
            continue;
        }
        
        const page& p = pages[curPage];
        ++curPage;
        
        jclass pageClass = env->GetObjectClass(pageObject);
        
        jfieldID id = env->GetFieldID(pageClass, "address", "I");
        env->SetIntField(pageObject, id, p.address);
     
        jbyteArray data = env->NewByteArray(p.data.size());
        jbyte *d = env->GetByteArrayElements(data, NULL);
        std::copy(p.data.begin(), p.data.end(), d);
        env->ReleaseByteArrayElements(data, d, 0);
        
        id = env->GetFieldID(pageClass, "data", "[B");
        env->SetObjectField(pageObject, id, data);
        return true;
    }
    return false;
}

JNIEXPORT void JNICALL Java_com_tassadar_lorrismobile_programmer_HexFile_clearPages(JNIEnv *env, jobject obj)
{
    pages.clear();
    skip_pages.clear();
    curPage = 0;
}

JNIEXPORT jint JNICALL Java_com_tassadar_lorrismobile_programmer_HexFile_getPagesCount(JNIEnv *env, jobject obj, jboolean skip)
{
    if(!skip)
        return pages.size();
    
    jint res = 0;
    for(size_t i = 0; i < pages.size(); ++i)
    {
        if(skip_pages.find(curPage) != skip_pages.end())
            continue;
        ++res;
    }
    return res;
}
