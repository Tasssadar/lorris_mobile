package com.tassadar.lorrismobile.connections;

import android.support.v4.app.Fragment;

public class ConnFragment extends Fragment {
    
    protected ConnFragment(ConnFragmentInterface in) {
        m_interface = in;
    }

    protected ConnFragmentInterface m_interface;
}