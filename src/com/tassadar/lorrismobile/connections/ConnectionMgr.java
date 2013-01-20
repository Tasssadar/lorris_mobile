package com.tassadar.lorrismobile.connections;

import java.util.HashMap;

import android.bluetooth.BluetoothDevice;

public class ConnectionMgr {

    static public BTSerialPort createBTSerial(BluetoothDevice dev) {
        for(Connection c : m_connections.values()) {
            if(c.getType() != Connection.CONN_BT_SP)
                continue;

            BTSerialPort p = (BTSerialPort)c;
            if(dev.getAddress().equals(p.getAddress()))
                return p;
        }
        BTSerialPort p = new BTSerialPort(dev);
        p.setId(m_idCounter++);
        m_connections.put(p.getId(), p);
        return p;
    }

    static public void removeConnection(int id) {
        m_connections.remove(id);
    }

    static public Connection getConnection(int id) {
        return m_connections.get(Integer.valueOf(id));
    }

    private static int m_idCounter = 0;
    private static HashMap<Integer, Connection> m_connections = new HashMap<Integer, Connection>();
}