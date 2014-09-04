package com.tassadar.lorrismobile.connections;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Message;

import com.tassadar.lorrismobile.BlobInputStream;
import com.tassadar.lorrismobile.BlobOutputStream;


public class Connection {
    public static final int CONN_BT_SP          = 0;
    public static final int CONN_TCP            = 1;
    public static final int CONN_USB_ACM        = 2;
    public static final int CONN_SHUPITO        = 3;
    public static final int CONN_SHUPITO_TUNNEL = 4;

    public static final int ST_DISCONNECTED = 0;
    public static final int ST_CONNECTING   = 1;
    public static final int ST_CONNECTED    = 2;

    private static final int EVENT_STATE         = 0;
    private static final int EVENT_CONNECTED     = 1;
    private static final int EVENT_DISCONNECTING = 2;

    protected Connection(int type) {
        m_type = type;
        m_state = ST_DISCONNECTED;
        m_interfaces = new ConnectionInterface[0];
        m_stateHandler = new StateEventHandler(this);
    }

    public void open() { }
    public void close() { }

    public boolean isOpen() { return m_state == ST_CONNECTED; }

    public synchronized void addInterface(ConnectionInterface in) {
        for(ConnectionInterface i : m_interfaces)
            if(i == in)
                return;

        ConnectionInterface[] newArray = new ConnectionInterface[m_interfaces.length+1];
        System.arraycopy(m_interfaces, 0, newArray, 0, m_interfaces.length);
        newArray[m_interfaces.length] = in;
        m_interfaces = newArray;
    }

    public synchronized void removeInterface(ConnectionInterface in) {
        int pos = 0;
        for(ConnectionInterface i : m_interfaces) {
            if(i == in) 
                break;
            ++pos;
        }

        if(pos >= m_interfaces.length)
            return;

        if(pos != m_interfaces.length-1) {
            ConnectionInterface tmp = m_interfaces[m_interfaces.length-1]; 
            m_interfaces[m_interfaces.length-1] = m_interfaces[pos];
            m_interfaces[pos] = tmp;
        }

        ConnectionInterface[] newArray = new ConnectionInterface[m_interfaces.length-1];
        System.arraycopy(m_interfaces, 0, newArray, 0, newArray.length);
        m_interfaces = newArray;
    }

    protected void sendConnected(boolean connected) {
        m_stateHandler.obtainMessage(EVENT_CONNECTED, connected ? 1 : 0, 0)
                      .sendToTarget();
    }

    protected void sendStateChanged(int state) {
        m_stateHandler.obtainMessage(EVENT_STATE, state, 0)
                      .sendToTarget();
    }

    protected void sendDisconnecting() {
        m_stateHandler.sendEmptyMessage(EVENT_DISCONNECTING);
    }

    protected void sendDataRead(byte[] data) {
        for(ConnectionInterface i : m_interfaces)
            i.dataRead(data);
    }

    static class StateEventHandler extends Handler {
        private final WeakReference<Connection> m_conn;

        StateEventHandler(Connection conn) {
            m_conn = new WeakReference<Connection>(conn);
        }

        @Override
        public void handleMessage(Message msg)
        {
             Connection c = m_conn.get();
             if (c == null)
                 return;

             switch(msg.what) {
                 case EVENT_STATE:
                     for(ConnectionInterface i : c.m_interfaces)
                         i.stateChanged(msg.arg1);
                     break;
                 case EVENT_DISCONNECTING:
                     for(ConnectionInterface i : c.m_interfaces)
                         i.disconnecting();
                     break;
                 case EVENT_CONNECTED:
                     for(ConnectionInterface i : c.m_interfaces)
                         i.connected(msg.arg1 == 1);
                     break;
             }
        }
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
        if(--m_refCount <= 0)
            ConnectionMgr.removeConnection(m_id);
    }

    public void addTabRef() {
        ++m_tabCount;
        addRef();
    }

    public void rmTabRef() {
        if(--m_tabCount <= 0)
            close();
        rmRef();
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

        int oldstate = m_state;
        m_state = state;

        sendStateChanged(state);

        switch(state) {
            case ST_CONNECTED:
                sendConnected(true);
                break;
            case ST_DISCONNECTED:
                if(oldstate == Connection.ST_CONNECTED)
                    sendConnected(false);
                break;
        }
    }

    public int getState() {
        return m_state;
    }

    public void write(byte[] data) {
        write(data, 0, data.length);
    }

    public void write(byte[] data, int offset, int count) {

    }

    public byte[] saveData() {
        BlobOutputStream str = new BlobOutputStream();
        saveDataStream(str);
        str.close();
        return str.toByteArray();
    }

    public void loadData(byte[] data) {
        BlobInputStream str = new BlobInputStream(data);
        loadDataStream(str);
        str.close();
    }

    protected void saveDataStream(BlobOutputStream str) { }
    protected void loadDataStream(BlobInputStream str) { }

    protected int m_id;
    protected int m_type;
    protected int m_state;
    protected int m_refCount;
    protected int m_tabCount;
    protected ConnectionInterface[] m_interfaces;
    private StateEventHandler m_stateHandler;
}
