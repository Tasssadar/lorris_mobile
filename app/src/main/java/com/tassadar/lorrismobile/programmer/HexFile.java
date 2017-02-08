package com.tassadar.lorrismobile.programmer;


import android.util.Log;


public class HexFile {
    
    // HexFile.h, enum MemoryTypes
    public static final int MEM_FLASH   = 1;
    public static final int MEM_EEPROM  = 2;
    public static final int MEM_FUSES   = 3;
    public static final int MEM_SDRAM   = 4;
    public static final int MEM_COUNT   = 5;

    public HexFile() {
        m_hexfile_ptr = newNative();
    }

    @Override
    protected void finalize() throws Throwable{
        destroy();
        super.finalize();
    }

    public void destroy() {
        if(m_hexfile_ptr != 0) {
            deleteNative(m_hexfile_ptr);
            m_hexfile_ptr = 0;
        }
    }

    public String loadFile(String path) {
        String res = loadFileNative(m_hexfile_ptr, path);
        return res.length() == 0 ? null : res;
    }

    public long getSize() {
        return getSizeNative(m_hexfile_ptr);
    }

    public boolean makePages(ChipDefinition def, int memId) {
        String res = makePagesNative(m_hexfile_ptr, def.getNativePtr(), memId);
        if(res.length() != 0) {
            Log.e("Lorris", "Failed to make pages! " + res);
            return false;
        }
        return true;
    }

    static { System.loadLibrary("lorris_native"); }
    private native long newNative();
    private native void deleteNative(long hex_ptr);
    private native String loadFileNative(long hex_ptr, String path);
    private native long getSizeNative(long hex_ptr);
    private native String makePagesNative(long hex_ptr, long chipdef_ptr, int memId);
    public native boolean getNextPage(Object page, boolean skip);
    public native void clearPages();
    public native int getPagesCount(boolean skip);

    private long m_hexfile_ptr;
}
