package com.tassadar.lorrismobile.yunicontrol;

import com.tassadar.lorrismobile.ByteArray;


public class Packet {
    public Packet() {
        data = new ByteArray();
    }

    public Packet(int device, int opcode) {
        this.device = device;
        this.opcode = opcode;
        data = new ByteArray();
    }

    public void clear() {
        data.clear();
        m_recvItr = 0;
        m_readItr = 0;
    }

    public void swap(Packet other) {
        data.swap(other.data);

        other.opcode ^= opcode;
        opcode ^= other.opcode;
        other.opcode ^= opcode;

        other.device ^= device;
        device ^= other.device;
        other.device ^= device;

        other.m_recvItr ^= m_recvItr;
        m_recvItr ^= other.m_recvItr;
        other.m_recvItr ^= m_recvItr;

        other.m_readItr ^= m_readItr;
        m_readItr ^= other.m_readItr;
        other.m_readItr ^= m_readItr;
    }

    public int addData(byte[] recv, int offset) {
        int start = offset;

        while(offset < recv.length) {
            int val = ((int)recv[offset]) & 0xFF;

            switch(m_recvItr) {
                case 0:
                    if(val != 0xFF) {
                        ++offset;
                        continue;
                    }
                    break;
                case 1:
                    device = val;
                    break;
                case 2:
                    data.resize(val-1);
                    break;
                case 3:
                    opcode = val;
                    break;
                default:
                    if(m_recvItr-4 >= data.size())
                        return offset-start;
                    data.set(m_recvItr-4, val);
                    break;
            }
            ++m_recvItr;
            ++offset;
        }
        return offset-start;
    }

    public boolean isValid() {
        return (m_recvItr >= 4 && data.size() == m_recvItr-4);
    }

    public int read8() {
        return data.uAt(m_readItr++);
    }

    public int read16() {
        int res = data.uAt(m_readItr++) << 8;
        res |= data.uAt(m_readItr++);
        return res;
    }

    public String readString() {
        int idx = -1;
        for(int i = m_readItr; i < data.size(); ++i) {
            if(data.at(i) == 0) {
                idx = i;
                break;
            }
        }

        if(idx == -1)
            return new String();

        String res = new String(data.data(), m_readItr, idx - m_readItr);
        m_readItr += (idx-m_readItr) + 1;
        return res;
    }

    public void resetRead() {
        m_readItr = 0;
    }

    public boolean atEnd() {
        return m_readItr >= data.size();
    }

    public void write8(int b) {
        data.append(b);
    }

    public void write16(int b) {
        data.append(b >> 8);
        data.append(b);
    }

    ByteArray data;
    int device;
    int opcode;
    private int m_recvItr;
    private int m_readItr;
}
