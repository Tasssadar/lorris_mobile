#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

static inline int min(int a, int b)
{
    if(a < b)
        return a;
    else
        return b;
}

extern "C" {
    JNIEXPORT jbyteArray JNICALL Java_com_tassadar_lorrismobile_modules_Terminal_convertToHex(JNIEnv *env, jobject obj, jbyteArray dataArray, jint hexPos);
};

JNIEXPORT jbyteArray JNICALL Java_com_tassadar_lorrismobile_modules_Terminal_convertToHex(JNIEnv *env, jobject obj, jbyteArray dataArray, jint hexPos)
{
    jbyte *res = NULL;
    uint32_t total_len = 0;

    jbyte *itr;
    jbyte line[78];
    line[8] = line[57] = line[58] = line[59] = ' ';
    line[60] = line[77] = '|';

    int chunk_size;
    
    jbyte *b = (jbyte *)env->GetByteArrayElements(dataArray, NULL);
    jbyte *chunk;

    jsize len = env->GetArrayLength(dataArray);
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
        
        uint32_t newlen = total_len + 62+chunk_size;
        if(total_len != 0)
            ++newlen;

        res = (jbyte*)realloc(res, newlen);

        if(total_len != 0)
            res[total_len++] = '\n';

        memcpy(res+total_len, line, 62+chunk_size);
        total_len = newlen;
    }

    env->ReleaseByteArrayElements(dataArray, b, 0 );
    
    jbyteArray resArr = env->NewByteArray(total_len);
    env->SetByteArrayRegion(resArr, 0, total_len, res);
    free(res);
    return resArr;
}