package com.tassadar.lorrismobile.modules;

import java.io.IOException;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.tassadar.lorrismobile.BlobInputStream;
import com.tassadar.lorrismobile.BlobOutputStream;
import com.tassadar.lorrismobile.connections.Connection;
import com.tassadar.lorrismobile.connections.ConnectionInterface;
import com.tassadar.lorrismobile.modules.TabListItem.TabItemClicked;

public class Tab extends Fragment implements TabItemClicked, ConnectionInterface {
    public interface TabSelectedListener {
        void onTabSelectedClicked(int tabId);
        void onTabCloseRequesteed(int tabId);
    }

    public Tab() {
        super();
        m_lastConnId = -1;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(m_loadData != null) {
            BlobInputStream str = new BlobInputStream(m_loadData);
            str.loadKeys();
            loadDataStream(str);
            str.close();
            m_loadData = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(m_conn != null)
            m_lastConnId = m_conn.getId(); 
        setConnection(null);
    }

    public void setListener(TabSelectedListener listener) {
        m_listener = listener;
    }

    public int getTabId() {
        return getArguments().getInt("id");
    }

    public void setTabId(int id) {
        Bundle b = new Bundle();
        b.putInt("id", id);
        setArguments(b);
    }

    public int getType() {
        return -1;
    }

    public String getName() {
        return "Tab";
    }

    public void setTabListItem(TabListItem it) {
        m_tab_list_it = it;
        if(it != null)
            it.setOnClickListener(this);
    }

    public TabListItem getTabListItem() {
        return m_tab_list_it;
    }

    public void setActive(boolean active) {
        if(m_tab_list_it != null)
            m_tab_list_it.setActive(active);
    }

    @Override
    public void onTabItemClicked() {
        if(m_listener != null)
            m_listener.onTabSelectedClicked(getTabId());
    }

    @Override
    public void onTabCloseRequested() {
        if(m_listener != null)
            m_listener.onTabCloseRequesteed(getTabId());
    }

    public void setConnection(Connection conn) {
        if(conn == m_conn)
            return;

        Log.i("Lorris", ("setting connection " + (conn != null ? conn.getId() : "null") + "\n"));

        if(m_conn != null) {
            m_conn.removeInterface(this);
            m_conn.rmRef();
        }

        m_conn = conn;

        if(m_conn != null) {
            m_conn.addInterface(this);
            m_conn.addRef();
        }
    }

    public Connection getConnection() {
        return m_conn;
    }

    public int getConnId() {
        if(m_conn != null)
            return m_conn.getId();
        return -1;
    }

    public int getLastConnId() {
        return m_lastConnId == -1 && m_conn != null ? m_conn.getId() : m_lastConnId;
    }

    public byte[] saveData() {
        BlobOutputStream str = new BlobOutputStream();
        saveDataStream(str);

        try {
            str.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return str.toByteArray();
    }

    protected void saveDataStream(BlobOutputStream str) {
        boolean connected = m_conn != null && m_conn.isOpen();
        str.writeBool("connected", connected);
    }

    public void loadData(byte[] data) {
        m_loadData = data;
    }

    protected void loadDataStream(BlobInputStream str) {
        if(m_conn != null && str.readBool("connected"))
            m_conn.open();
    }

    @Override
    public void connected(boolean connected) { }
    @Override
    public void stateChanged(int state) { }
    @Override
    public void disconnecting() { }
    @Override
    public void dataRead(byte[] data) { }

    private TabListItem m_tab_list_it;
    private TabSelectedListener m_listener;
    protected Connection m_conn;
    protected byte[] m_loadData;
    protected int m_lastConnId;
}
