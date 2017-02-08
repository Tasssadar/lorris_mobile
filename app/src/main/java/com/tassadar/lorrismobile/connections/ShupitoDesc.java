package com.tassadar.lorrismobile.connections;

import java.util.HashMap;

import android.util.Log;

import com.tassadar.lorrismobile.ByteArray;
import com.tassadar.lorrismobile.Utils;


public class ShupitoDesc {
    
    public class config {
        public config() {
            guid = new String();
            actseq = new ByteArray();
            data = new ByteArray();
        }

        String guid;
        short flags;
        short cmd;
        short cmd_count;
        ByteArray actseq;
        ByteArray data;
        
        boolean always_active() { return (flags & 1) != 0; }
        boolean default_active() { return (flags & 2) != 0; }

        byte[] getStateChangeCmd(boolean activate) {
            byte[] res = new byte[2 + actseq.size()];
            res[0] = 0;
            res[1] = (byte)(activate ? 0x01 : 0x02);

            System.arraycopy(actseq.data(), 0, res, 2, actseq.size());
            return res;
        }
    }

    public ShupitoDesc() {
        clear();
    }

    public void clear() {
        m_guid = "";
        m_data = new ByteArray();
        m_interface_map = new HashMap<String, config>();
    }

    public String getGuid() {
        return m_guid;
    }

    public boolean isEmpty() {
        return m_guid.length() == 0;
    }

    public config getConfig(String guid) {
        return m_interface_map.get(guid);
    }

    public HashMap<String, config> getCfgMap() {
        return m_interface_map;
    }

    public void addData(ByteArray data) throws Exception {
        m_data.append(data);

        int offset = 0;
        if(m_data.size() < 16 && m_data.at(offset) != 1)
            throw new Exception("Invalid descriptor");

        ++offset;

        m_guid = makeGuid(m_data.data(), offset);
        offset += 16;

        Log.i("Lorris", "GUID: " + m_guid);

        ByteArray act_seq = new ByteArray();
        parseGroupConfig(m_data.data(), offset, 1, act_seq);
    }

    private int[] parseGroupConfig(byte[] data, int offset, int base_cmd, ByteArray actseq) throws Exception {
        if(offset >= data.length)
            throw new Exception("Invalid descriptor");

        if(data[offset] == 0) {
            ++offset;

            return parseConfig(data, offset, base_cmd, actseq);
        }

        int n = data[offset];
        int count = n & 0x7F;
        // OR
        if((n & 0x80) != 0) {
            ++offset;
            int next_base_cmd = base_cmd;
            for(int i = 0; i < count; ++i) {
                int or_base_cmd = base_cmd;
                actseq.append(i);
                int[] res = parseGroupConfig(data, offset, or_base_cmd, actseq);
                // Fuck you, java
                offset = res[0];
                or_base_cmd = res[1];
                actseq.pop_back();
                next_base_cmd = Utils.max(next_base_cmd, or_base_cmd);
            }
            base_cmd = next_base_cmd;
        }
        // AND
        else {
            ++offset;
            for(int i = 0; i < count; ++i) {
                actseq.append(i);
                int[] res = parseGroupConfig(data, offset, base_cmd, actseq);
                offset = res[0];
                base_cmd = res[1];
                actseq.pop_back();
            }
        }
        return new int[] { offset, base_cmd };
    }

    private int[] parseConfig(byte[] data, int offset, int base_cmd, ByteArray actseq) throws Exception {
        if(offset < 19)
            throw new Exception("Invalid descriptor");

        config cfg = new config();
        cfg.flags = (short)(data[offset++] & 0xFF);
        cfg.guid = makeGuid(data, offset);
        offset += 16;

        Log.i("Lorris", "interface " + cfg.guid);

        cfg.cmd = (short)(data[offset++] & 0xFF);
        cfg.cmd_count = (short)(data[offset++] & 0xFF);
        base_cmd += cfg.cmd_count;

        cfg.actseq = actseq.clone();

        int data_len = (data[offset++] & 0xFF);
        cfg.data.assign(data, offset, data_len);
        offset += data_len;

        m_interface_map.put(cfg.guid, cfg);

        return new int[] { offset, base_cmd };
    }

    static { System.loadLibrary("lorris_native"); }
    private native String makeGuid(byte[] data, int offset);

    private String m_guid;
    private ByteArray m_data;
    private HashMap<String, config> m_interface_map;
}
