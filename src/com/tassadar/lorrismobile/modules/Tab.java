package com.tassadar.lorrismobile.modules;

import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.tassadar.lorrismobile.connections.Connection;
import com.tassadar.lorrismobile.connections.ConnectionInterface;
import com.tassadar.lorrismobile.modules.TabListItem.TabItemClicked;

public class Tab extends Fragment implements TabItemClicked, ConnectionInterface {
    public interface TabSelectedListener {
        void onTabSelectedClicked(int tabId);
    }
    
    public static final int TAB_ANALYZER = 0;
    public static final int TAB_TERMINAL = 1;

    public static Tab createTab(TabSelectedListener listener, int type) {
        switch(type) {
            case TAB_ANALYZER:
                return new Analyzer(listener);
            case TAB_TERMINAL:
                return new Terminal(listener);
            default:
                return null;
        }
    }

    private static int m_id_counter = 0;
    private static int generateTabId() {
        return m_id_counter++;
    }

    public Tab(TabSelectedListener listener) {
        super();

        m_id = generateTabId();
        m_listener = listener;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setConnection(null);
    }

    public int getTabId() {
        return m_id;
    }

    public int getType() {
        return m_type;
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
            m_listener.onTabSelectedClicked(m_id);
    }

    public void setConnection(Connection conn) {
        if(conn == m_conn)
            return;

        if(m_conn != null) {
            m_conn.removeInterface(this);
            m_conn.rmRef();
        }

        m_conn = conn;

        if(m_conn != null) {
            m_conn.addInterface(this);
            m_conn.addRef();
            m_conn.open();

            Toast.makeText(getActivity(), "Connection selected: " + m_conn.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void connected(boolean connected) { }
    @Override
    public void stateChanged(int state) { }
    @Override
    public void disconnecting() { }
    @Override
    public void dataRead(byte[] data) { }

    protected int m_id;
    protected int m_type;
    private TabListItem m_tab_list_it;
    private TabSelectedListener m_listener;
    protected Connection m_conn;
}
