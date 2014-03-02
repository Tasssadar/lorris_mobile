package com.tassadar.lorrismobile.programmer;

import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;
import android.util.Log;

import com.tassadar.lorrismobile.LorrisApplication;

public class ChipDefinition {
    
    private static boolean m_chipdefsLoaded = false;

    public static void loadChipdefs() {
        AssetManager mgr = LorrisApplication.getAppContext().getAssets();
        String data = null;
        try {
            InputStream in = mgr.open("chipdefs.txt");
            StringBuilder b = new StringBuilder();
            byte[] buff = new byte[1024];
            while(true) {
                int read = in.read(buff);
                if(read <= 0)
                    break;
                b.append(new String(buff));
            }
            in.close();
            data = b.toString();
        } catch (IOException e) {
            Log.e("Lorris", "Failed to load chipdefs (IOException)!");
            e.printStackTrace();
            return;
        }

        if(!loadChipdefs(data)) {
            Log.e("Lorris", "Failed to load chipdefs (native exception)!");
            return;
        }

        m_chipdefsLoaded = true;
    }

    public ChipDefinition() {
        m_def_ptr = 0;
    }

    public ChipDefinition(String sign) {
        loadFromSign(sign);
    }

    public void loadFromSign(String sign) {
        destroy();

        if(!m_chipdefsLoaded)
            loadChipdefs();

        m_def_ptr = newNative(sign);
        m_sign = sign;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        destroy();
    }

    public void destroy() {
        m_sign = null;
        if(m_def_ptr != 0) {
            deleteNative(m_def_ptr);
            m_def_ptr = 0;
        }
    }

    public long getNativePtr() {
        return m_def_ptr;
    }

    public String getSign() {
        return m_sign;
    }

    public String getName() {
        return getNameNative(m_def_ptr);
    }

    public int getMemSize(int memId) {
        return getMemSizeNative(m_def_ptr, memId);
    }

    public int getMemPageSize(int memId) {
        return getMemPageSizeNative(m_def_ptr, memId);
    }

    static { System.loadLibrary("lorris_native"); }
    private native long newNative(String sign);
    private native void deleteNative(long def_ptr);
    private native String getNameNative(long def_ptr);
    private native int getMemSizeNative(long def_ptr, int memId);
    private native int getMemPageSizeNative(long def_ptr, int memId);
    private static native boolean loadChipdefs(String data);

    private long m_def_ptr;
    private String m_sign;
}
