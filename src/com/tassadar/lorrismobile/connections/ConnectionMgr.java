package com.tassadar.lorrismobile.connections;

import java.lang.ref.WeakReference;

import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.util.SparseArray;

import com.tassadar.lorrismobile.Utils;

public class ConnectionMgr {

    public interface ConnMgrListener {
        void onConnAdded(Connection c);
        void onConnRemoved(int id);
    }

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
        addConnection(p);
        return p;
    }

    static public TCPConnection createTcpConn(TCPConnProto p) {
        int size = m_connections.size();
        for(int i = 0; i < size; ++i) {
            Connection c = m_connections.valueAt(i);
            if(c.getType() != Connection.CONN_TCP)
                continue;

            TCPConnection t = (TCPConnection)c;
            if(p.sameAs(t.getProto()))
                return t;
        }

        TCPConnection t = new TCPConnection();
        t.setProto(p);
        t.setId(m_idCounter++);
        addConnection(t);
        return t;
    }

    static public Connection createFromVals(ContentValues vals) {
        Connection c = null;
        switch(vals.getAsInteger("type")) {
            case Connection.CONN_BT_SP:
                c = new BTSerialPort();
                break;
            case Connection.CONN_TCP:
                c = new TCPConnection();
                break;
            default:
                return null;
        }

        c.loadData(vals.getAsByteArray("data"));
        c.setId(vals.getAsInteger("id"));
        return c;
    }

    static public synchronized void addConnection(Connection c) {
        m_connections.put(c.getId(), c);

        if(m_listener != null && m_listener.get() != null)
            m_listener.get().onConnAdded(c);
    }

    static public synchronized void removeConnection(int id) {
        if(m_listener != null && m_listener.get() != null)
            m_listener.get().onConnRemoved(id);
        m_connections.remove(id);
    }

    static public Connection getConnection(int id) {
        return m_connections.get(id);
    }

    static public synchronized SparseArray<Connection> cloneConnArray() {
        return Utils.cloneSparseArray(m_connections);
    }

    static public synchronized SparseArray<Connection> takeConnArray() {
        SparseArray<Connection> res = m_connections;
        m_connections = new SparseArray<Connection>();
        return res;
    }
    
    static public synchronized void addConnsArray(SparseArray<Connection> conns) {
        int size = conns.size();
        for(int i = 0; i < size; ++i)
            m_connections.put(conns.keyAt(i), conns.valueAt(i));
    }

    static public void setConnIdCounter(int value) {
        m_idCounter = value;
    }

    static public void setListener(ConnMgrListener listener) {
        m_listener = new WeakReference<ConnMgrListener>(listener);
    }

    private static int m_idCounter = 0;
    private static SparseArray<Connection> m_connections = new SparseArray<Connection>();
    private static WeakReference<ConnMgrListener> m_listener = null;
}