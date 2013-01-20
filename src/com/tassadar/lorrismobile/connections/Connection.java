package com.tassadar.lorrismobile.connections;

import java.util.ArrayList;

public class Connection {
    public static final int CONN_BT_SP   = 0;
    public static final int CONN_TCP     = 1;
    public static final int CONN_USB     = 2;

    public static final int ST_DISCONNECTED = 0;
    public static final int ST_CONNECTING   = 1;
    public static final int ST_CONNECTED    = 2;

    protected Connection(int type) {
        m_type = type;
        m_state = ST_DISCONNECTED;
        m_interfaces = new ArrayList<ConnectionInterface>();
    }

    public void open() { }
    public void close() { }

    public boolean isOpen() { return m_state == ST_CONNECTED; }

    protected void sendConnected(boolean connected) {
        int len = m_interfaces.size();
        for(int i = 0; i < len; ++i)
            m_interfaces.get(i).connected(connected);
    }

    protected void sendStateChanged(int state) {
        int len = m_interfaces.size();
        for(int i = 0; i < len; ++i)
            m_interfaces.get(i).stateChanged(state);
    }

    protected void sendDisconnecting() {
        int len = m_interfaces.size();
        for(int i = 0; i < len; ++i)
            m_interfaces.get(i).disconnecting();
    }

    protected void sendDataRead(byte[] data) {
        int len = m_interfaces.size();
        for(int i = 0; i < len; ++i)
            m_interfaces.get(i).dataRead(data);
    }


    protected int m_type;
    protected int m_state;
    protected ArrayList<ConnectionInterface> m_interfaces;
}