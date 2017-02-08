package com.tassadar.lorrismobile.connections;


// ShupitoPacket is byte[] in Lorris mobile

public class ShupitoPacket {
    public static byte[] make(int cmd, int... data) {
        byte[] res = new byte[data.length + 1];
        int idx = 0;
        res[idx++] = (byte)cmd;

        for(int i : data)
            res[idx++] = (byte)i;

        return res;
    }
}