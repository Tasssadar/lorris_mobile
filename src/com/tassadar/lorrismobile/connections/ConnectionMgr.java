package com.tassadar.lorrismobile.connections;

import android.bluetooth.BluetoothDevice;
import android.util.SparseArray;

public class ConnectionMgr {

    static public BTSerialPort createBTSerial(BluetoothDevice dev) {
        int size = m_connections.size();
        for(int i = 0; i < size; ++i) {
            Connection c = m_connections.valueAt(i);
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
    private static SparseArray<Connection> m_connections = new SparseArray<Connection>();
}