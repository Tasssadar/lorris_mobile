#include <jni.h>
#include <stdio.h>
#include <string>
#include <stdlib.h>

static inline int min(int a, int b)
{
    if(a < b)
        return a;
    else
        return b;
}

extern "C" {
    JNIEXPORT jbyteArray JNICALL Java_com_tassadar_lorrismobile_terminal_Terminal_convertToHex16(JNIEnv *env, jobject obj, jbyteArray dataArray, jint hexPos);
    JNIEXPORT jbyteArray JNICALL Java_com_tassadar_lorrismobile_terminal_Terminal_convertToHex8(JNIEnv *env, jobject obj, jbyteArray dataArray, jint hexPos);
    JNIEXPORT jstring JNICALL Java_com_tassadar_lorrismobile_connections_ShupitoDesc_makeGuid(JNIEnv *env, jobject obj, jbyteArray dataArray, jint offset);
};

JNIEXPORT jbyteArray JNICALL Java_com_tassadar_lorrismobile_terminal_Terminal_convertToHex16(JNIEnv *env, jobject obj, jbyteArray dataArray, jint hexPos)
{
    uint32_t total_len = 0;

    jbyte *itr;
    jbyte line[78];
    line[8] = line[57] = line[58] = line[59] = ' ';
    line[60] = line[77] = '|';

    int chunk_size;

    jbyte *b = (jbyte *)env->GetByteArrayElements(dataArray, NULL);
    jbyte *chunk;

    jsize len = env->GetArrayLength(dataArray);

    uint32_t alloced = (len/16)*79;
    chunk_size = len%16;
    if(chunk_size > 0)
        alloced += 62+chunk_size;

    jbyte *res = (jbyte*)malloc(alloced);

    for(uint32_t i = 0; i < len; i += 16)
    {
        itr = line;
        chunk = b+i;
        chunk_size = min(len - i, 16);

        static const char* hex = "0123456789ABCDEF";
        for(int x = 7; x >= 0; --x, ++itr)
            *itr = hex[(hexPos >> x*4) & 0x0F];
        ++itr;

        hexPos += chunk_size;

        for(int x = 0; x < chunk_size; ++x)
        {
            *(itr++) = hex[uint8_t(chunk[x]) >> 4];
            *(itr++) = hex[uint8_t(chunk[x]) & 0x0F];
            *(itr++) = ' ';

            line[61+x] = (chunk[x] < 32 || chunk[x] > 126) ? '.' : chunk[x];
        }

        memset(itr, ' ', (16 - chunk_size)*3);
        if(chunk_size != 16)
            *(line + chunk_size + 61) = '|';

        if(total_len != 0)
            res[total_len++] = '\n';

        memcpy(res+total_len, line, 62+chunk_size);
        total_len += 62+chunk_size;
    }

    env->ReleaseByteArrayElements(dataArray, b, 0 );
    
    jbyteArray resArr = env->NewByteArray(total_len);
    env->SetByteArrayRegion(resArr, 0, total_len, res);
    free(res);
    return resArr;
}

JNIEXPORT jbyteArray JNICALL Java_com_tassadar_lorrismobile_terminal_Terminal_convertToHex8(JNIEnv *env, jobject obj, jbyteArray dataArray, jint hexPos)
{
    uint32_t total_len = 0;
    
    jbyte *itr;
    jbyte line[46];
    line[8] = line[33] = line[34] = line[35] = ' ';
    line[36] = line[45] = '|';

    int chunk_size;

    jbyte *b = (jbyte *)env->GetByteArrayElements(dataArray, NULL);
    jbyte *chunk;

    jsize len = env->GetArrayLength(dataArray);
    
    uint32_t alloced = (len/8)*47;
    chunk_size = len%8;
    if(chunk_size > 0)
        alloced += 38+chunk_size;

    jbyte *res = (jbyte*)malloc(alloced);

    for(uint32_t i = 0; i < len; i += 8)
    {
        itr = line;
        chunk = b+i;
        chunk_size = min(len - i, 8);

        static const char* hex = "0123456789ABCDEF";
        for(int x = 7; x >= 0; --x, ++itr)
            *itr = hex[(hexPos >> x*4) & 0x0F];
        ++itr;

        hexPos += chunk_size;

        for(int x = 0; x < chunk_size; ++x)
        {
            *(itr++) = hex[uint8_t(chunk[x]) >> 4];
            *(itr++) = hex[uint8_t(chunk[x]) & 0x0F];
            *(itr++) = ' ';

            line[37+x] = (chunk[x] < 32 || chunk[x] > 126) ? '.' : chunk[x];
        }

        memset(itr, ' ', (8 - chunk_size)*3);
        if(chunk_size != 8)
            *(line + chunk_size + 37) = '|';
        
        if(total_len != 0)
            res[total_len++] = '\n';

        memcpy(res+total_len, line, 38+chunk_size);
        total_len += 38+chunk_size;
    }

    env->ReleaseByteArrayElements(dataArray, b, 0 );
    
    jbyteArray resArr = env->NewByteArray(total_len);
    env->SetByteArrayRegion(resArr, 0, total_len, res);
    free(res);
    return resArr;
}

JNIEXPORT jstring JNICALL Java_com_tassadar_lorrismobile_connections_ShupitoDesc_makeGuid(JNIEnv *env, jobject obj, jbyteArray dataArray, jint offset)
{
    jbyte *data = (jbyte*)env->GetByteArrayElements(dataArray, NULL);

    std::string guid;
    guid.reserve(36);
    
    for(int i = offset; i < offset+16; ++i)
    {
        static char const digits[] = "0123456789abcdef";
        guid += digits[uint8_t(data[i]) >> 4];
        guid += digits[uint8_t(data[i]) & 0x0F];
    }

    env->ReleaseByteArrayElements(dataArray, data, JNI_ABORT);
    
    guid.insert(20, "-");
    guid.insert(16, "-");
    guid.insert(12, "-");
    guid.insert(8, "-");
    return env->NewStringUTF(guid.c_str());
}
