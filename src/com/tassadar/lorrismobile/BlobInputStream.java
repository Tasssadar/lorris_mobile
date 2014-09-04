package com.tassadar.lorrismobile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class BlobInputStream {
    public BlobInputStream(byte[] data) {
        m_buff = ByteBuffer.wrap(data);
        loadKeys();
    }

    public void close() {
        m_buff = null;
    }

    private void loadKeys() {
        byte[] buff = new byte[64];
        m_buff.rewind();
        m_keys.clear();

        int len;
        while(m_buff.hasRemaining()) {
             len = m_buff.get();
             m_buff.get(buff, 0, len);

             String key = new String(buff, 0, len);
             m_keys.put(key, m_buff.position());

             len = m_buff.getInt();
             m_buff.position(m_buff.position()+len);
        }
    }

    private int getLen(String key) {
        if(!m_keys.containsKey(key))
            return -1;

        int pos = m_keys.get(key);
        int len = m_buff.getInt(pos);
        m_buff.position(pos+4);
        return len;
    }

    public String readString(String key, String def) {
        int len = getLen(key);
        if(len == -1)
            return def;

        byte[] data = new byte[len];
        m_buff.get(data);
        return new String(data);
    }

    public String readString(String key) {
        return readString(key, "");
    }

    public byte[] readByteArray(String key) {
        int len = getLen(key);
        if(len == -1)
            return new byte[0];
        
        byte[] data = new byte[len];
        m_buff.get(data);
        return data;
    }

    public int readInt(String key, int def) {
        int len = getLen(key);
        if(len == -1)
            return def;

        return m_buff.getInt();
    }

    public int readInt(String key) {
        return readInt(key, 0);
    }

    public boolean readBool(String key, boolean def) {
        int len = getLen(key);
        if(len == -1)
            return def;

        return m_buff.get() == 1;
    }

    public boolean readBool(String key) {
        return readBool(key, false);
    }

    public Map<String, Object> readHashMap(String key) {
        Map<String, Object> res = new HashMap<String, Object>();
        int len = getLen(key);

        if(len == -1)
            return res;

        try {
            ByteArrayInputStream byteArrayStr = new ByteArrayInputStream(m_buff.array(),
                    m_buff.position(), len);
            ObjectInputStream in = new ObjectInputStream(byteArrayStr);

            final int count = in.readInt();
            for(int i = 0; i < count; ++i) {
                String it_key = in.readUTF();
                int type = in.readInt();
                switch(type) {
                    case BlobDataTypes.NULL:
                        res.put(it_key, null);
                        break;
                    case BlobDataTypes.STRING:
                        res.put(it_key, in.readUTF());
                        break;
                    case BlobDataTypes.INT:
                        res.put(it_key, in.readInt());
                        break;
                    case BlobDataTypes.BOOLEAN:
                        res.put(it_key, in.readBoolean());
                        break;
                    case BlobDataTypes.UNKNOWN:
                    default:
                        res.put(it_key, null);
                        break;
                }
            }

            in.close();
            byteArrayStr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public boolean containsKey(String key) {
        return m_keys.containsKey(key);
    }

    private HashMap<String, Integer> m_keys = new HashMap<String, Integer>();
    private ByteBuffer m_buff;
}
