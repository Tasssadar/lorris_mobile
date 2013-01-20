package com.tassadar.lorrismobile.modules;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tassadar.lorrismobile.R;

public class Terminal extends Tab {

    public Terminal(TabSelectedListener listener) {
        super(listener);
        m_type = TAB_TERMINAL;
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.terminal, container, false);
        m_dataView = (TextView)v.findViewById(R.id.data_view);
        return v;
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        //inflater.inflate(R.menu.session_details, menu);
    }

    @Override
    public void dataRead(byte[] data) {
        if(m_dataView == null)
            return;

        m_dataView.append(new String(data));
    }

    @Override
    public void connected(boolean connected) {
        //if(connected)
            //m_conn.write(new byte[] { 0x74, 0x7E, 0x7A, 0x33 });
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch(item.getItemId()) {
        }
        return false;
    }

    TextView m_dataView;
}
