package com.tassadar.lorrismobile.yunicontrol;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tassadar.lorrismobile.BlobInputStream;
import com.tassadar.lorrismobile.BlobOutputStream;
import com.tassadar.lorrismobile.LorrisApplication;
import com.tassadar.lorrismobile.R;
import com.tassadar.lorrismobile.connections.Connection;
import com.tassadar.lorrismobile.modules.Tab;
import com.tassadar.lorrismobile.modules.TabManager;
import com.tassadar.lorrismobile.yunicontrol.Protocol.ProtocolListener;
import com.tassadar.lorrismobile.yunicontrol.YuniControlMenu.YCMenuListener;


public class YuniControl extends Tab implements YCMenuListener,ProtocolListener {

    public static final int FRAGMENT_COUNT = 2;

    public YuniControl() {
        super();

        m_menu = new YuniControlMenu();
        m_menu.setListener(this);

        m_protocol = new Protocol();
        m_protocol.addListener(this);

        m_fragments = new YuniControlFragment[FRAGMENT_COUNT];
        m_fragments[0] = new InfoFragment();
        m_fragments[0].setProtocol(m_protocol);
        m_fragments[1] = new SettingsFragment();
        m_fragments[1].setProtocol(m_protocol);
    }

    @Override
    public int getType() {
        return TabManager.TAB_YUNICONTROL;
    }

    @Override
    public String getName() {
        return "YuniControl";
    }

    @Override
    public void onAttach(Activity act) {
        super.onAttach(act);
        SharedPreferences p = act.getPreferences(0);
        m_protocol.setGetDataDelay(p.getInt("yc_getDataDelay", m_protocol.getDataDelay()));
        m_protocol.setGetDataEnabled(p.getBoolean("yc_getDataEnabled", m_protocol.getDataEnabled()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        m_protocol.connected(false);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.yunicontrol, container, false);

        ViewPager pager = (ViewPager)v.findViewById(R.id.view_pager);
        pager.setId(LorrisApplication.generateViewId());

        YCPagerAdapter adapter = new YCPagerAdapter(getActivity().getSupportFragmentManager());
        pager.setAdapter(adapter);
        return v;
    }

    @Override
    public Fragment getMenuFragment() {
        return m_menu;
    }

    private class YCPagerAdapter extends FragmentPagerAdapter {
        public YCPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return FRAGMENT_COUNT;
        }

        @Override
        public Fragment getItem(int idx) {
            if(idx >= m_fragments.length)
                return null;
            return m_fragments[idx];
        }

        @Override
        public CharSequence getPageTitle(int idx) {
            switch(idx) {
                case 0:
                    return getResources().getString(R.string.info);
                case 1:
                    return getResources().getString(R.string.settings);
            }
            return null;
        }
    }

    @Override
    public void connected(boolean connected) {
        if(connected)
            m_protocol.requestGlobalInfo();
        m_protocol.connected(connected);
    }

    @Override
    public void dataRead(byte[] data) {
        m_protocol.dataRead(data);
    }

    @Override
    public void setConnection(Connection conn) {
        if(conn == m_conn)
            return;

        super.setConnection(conn);
        m_protocol.setConnection(conn);
    }

    @Override
    public void onBoardSelected(int board) {
        m_protocol.selectBoard(board);
    }

    @Override
    public void onPacketReceived(Packet pkt) {
    }

    @Override
    public void onInfoRequested() { }

    @Override
    public void onInfoReceived(GlobalInfo i) {
        m_menu.setInfo(i);
    }

    @Override
    public void onBoardChange(BoardInfo b) { }

    @Override
    protected void saveDataStream(BlobOutputStream str) {
        super.saveDataStream(str);

        str.writeString("lastBoard", m_protocol.getLastBoard());

        for(YuniControlFragment f : m_fragments)
            f.saveDataStream(str);
    }

    @Override
    protected void loadDataStream(BlobInputStream str) {
        super.loadDataStream(str);

        m_protocol.setLastBoard(str.readString("lastBoard"));

        for(YuniControlFragment f : m_fragments)
            f.loadDataStream(str);
    }

    private YuniControlMenu m_menu;
    private Protocol m_protocol;
    private YuniControlFragment[] m_fragments;
}
