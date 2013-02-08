package com.tassadar.lorrismobile.modules;

import android.util.SparseArray;

import com.tassadar.lorrismobile.R;
import com.tassadar.lorrismobile.Utils;
import com.tassadar.lorrismobile.modules.Tab.TabSelectedListener;

public class TabManager {
 
    public static final int TAB_TERMINAL = 0;

    public static Tab createTab(TabSelectedListener listener, int type) {
        Tab res = null;
        switch(type) {
            case TAB_TERMINAL:
                res = new Terminal();
                break;
            default:
                return null;
        }
        res.setListener(listener);
        return res;
    }

    public static int getRIdForTabType(int type) {
        final int ids[] = { R.id.terminal };
        try {
            return ids[type];
        } catch(IndexOutOfBoundsException ex) {
            ex.printStackTrace(); 
        }
        return 0;
    }

    private static int m_id_counter = 0;
    public static int generateTabId() {
        return m_id_counter++;
    }

    public static void setTabIdCounter(int value) {
        m_id_counter = value;
    }

    public static synchronized void addTab(Tab tab) {
        m_tabs.put(tab.getTabId(), tab);
    }

    public static synchronized void removeTab(Tab tab) {
        m_tabs.remove(tab.getTabId());
    }

    public static synchronized void clearTabs() {
        m_tabs.clear();
    }

    public static boolean isEmpty() {
        return m_tabs.size() == 0;
    }

    public static Tab getTab(int id) {
        return m_tabs.get(id);
    }

    public static Tab getTabByPos(int pos) {
        if(pos >= 0 && pos < m_tabs.size())
            return m_tabs.valueAt(pos);
        else
            return null;
    }

    public static int getTabPos(int id) {
        return m_tabs.indexOfKey(id);
    }

    public static int size() {
        return m_tabs.size();
    }

    public static synchronized SparseArray<Tab> cloneTabArray() {
        return Utils.cloneSparseArray(m_tabs);
    }

    public static synchronized SparseArray<Tab> takeTabArray() {
        SparseArray<Tab> res = m_tabs;
        m_tabs = new SparseArray<Tab>();
        return res;
    }

    private static SparseArray<Tab> m_tabs = new SparseArray<Tab>(); 
}