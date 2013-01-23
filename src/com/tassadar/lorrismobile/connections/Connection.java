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

    public void addInterface(ConnectionInterface in) {
        m_interfaces.add(in);
    }

    public void removeInterface(ConnectionInterface in) {
        m_interfaces.remove(in);
    }

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

    public String getName() {
        return "Connection";
    }

    public int getType() {
        return m_type;
    }

    public void addRef() {
        ++m_refCount;
    }

    public void rmRef() {
        if(--m_refCount <= 0) {
            close();
            ConnectionMgr.removeConnection(m_id);
        }
    }

    public void setId(int id) {
        m_id = id;
    }

    public int getId() {
        return m_id;
    }

    protected void setState(int state) {
        if(m_state == state)
            return;

        sendStateChanged(state);

        switch(state) {
            case ST_CONNECTED:
                sendConnected(true);
                break;
            case ST_DISCONNECTED:
                if(m_state == Connection.ST_CONNECTED)
                    sendConnected(false);
                break;
        }

        m_state = state;
    }

    public void write(byte[] data) {
        write(data, 0, data.length);
    }

    public void write(byte[] data, int offset, int count) {

    } 
    

    protected int m_id;
    protected int m_type;
    protected int m_state;
    protected int m_refCount;
    protected ArrayList<ConnectionInterface> m_interfaces;
}