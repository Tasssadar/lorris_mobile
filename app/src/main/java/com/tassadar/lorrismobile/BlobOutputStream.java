package com.tassadar.lorrismobile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

import android.util.Log;

public class BlobOutputStream {
    public BlobOutputStream() {
        m_str = new ByteArrayOutputStream();
    }

    public void close() {
        try {
            m_str.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public void writeDouble(String key, double val) {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putDouble(val);
        writePackage(key, buf.array());
    }

    public void writeHashMap(String key, Map<String, Object> map) {
        ByteArrayOutputStream byteArrayStr = new ByteArrayOutputStream();
        try {
            ObjectOutputStream out = new ObjectOutputStream(byteArrayStr);
            out.writeInt(map.size());

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                out.writeUTF(entry.getKey());
                Object val = entry.getValue();
                if(val == null) {
                    out.writeInt(BlobDataTypes.NULL);
                }else if(val instanceof String) {
                    out.writeInt(BlobDataTypes.STRING);
                    out.writeUTF((String)val);
                } else if(val instanceof Integer) {
                    out.writeInt(BlobDataTypes.INT);
                    out.writeInt((Integer)val);
                } else if(val instanceof Boolean) {
                    out.writeInt(BlobDataTypes.BOOLEAN);
                    out.writeBoolean((Boolean) val);
                } else if(val instanceof Double) {
                    out.writeInt(BlobDataTypes.DOUBLE);
                    out.writeDouble((Double) val);
                } else {
                    out.writeInt(BlobDataTypes.UNKNOWN);
                    Log.e("Lorris", "Failed to save memeber of class " +
                            val.getClass().getName() + " in writeHashMap!");
                }
            }
            out.close();
            byteArrayStr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        writePackage(key, byteArrayStr.toByteArray());
    }

    private ByteArrayOutputStream m_str;
}
