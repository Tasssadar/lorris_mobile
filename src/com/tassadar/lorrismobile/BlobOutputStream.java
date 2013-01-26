package com.tassadar.lorrismobile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.util.Log;

public class BlobOutputStream {
    public BlobOutputStream() {
        m_str = new ByteArrayOutputStream();
    }

    public void close() throws IOException {
        m_str.close();
    }

    public byte[] toByteArray() {
        return m_str.toByteArray();
    }

    private void writePackage(String key, byte[] data) {
        byte[] keyData = key.getBytes();

        if(keyData.length >= 64) {
            Log.e("Lorris", "Failed to save key " + key + "," +
                  " it is too long (" + keyData.length + ", max 64)\n");
            return;
        }
        try {
            m_str.write(keyData.length);
            m_str.write(keyData);
    
            byte[] size = new byte[4];
            Utils.writeIntToByteArray(data.length, size, 0);
            m_str.write(size);
            m_str.write(data);
        } catch(IOException e) {
            Log.e("Lorris", "Error writing BlobOutputStream");
            e.printStackTrace();
        }
    }

    public void writeString(String key, String value) {
        writePackage(key, value.getBytes());
    }

    public void writeByteArray(String key, byte[] data) {
        writePackage(key, data);
    }

    public void writeInt(String key, int val) {
        byte[] array = new byte[4];
        Utils.writeIntToByteArray(val, array, 0);
        writePackage(key, array);
    }

    public void writeBool(String key, boolean val) {
        byte[] array = { (byte)(val ? 1 : 0) };
        writePackage(key, array);
    }

    private ByteArrayOutputStream m_str;
}