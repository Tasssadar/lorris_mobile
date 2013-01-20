package com.tassadar.lorrismobile.connections;

import com.tassadar.lorrismobile.R;
import com.tassadar.lorrismobile.R.layout;
import com.tassadar.lorrismobile.R.menu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class TCPConnFragment extends ConnFragment {

    public TCPConnFragment(ConnFragmentInterface in) {
        super(in);
    }

    
    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.conn_tcp_fragment, container, false);
        return v;
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.conn_tcp_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch(item.getItemId()) {
        }
        return false;
    }
}