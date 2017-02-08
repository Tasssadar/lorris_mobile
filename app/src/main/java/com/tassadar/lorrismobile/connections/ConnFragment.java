package com.tassadar.lorrismobile.connections;

import android.app.Activity;
import android.support.v4.app.Fragment;

public class ConnFragment extends Fragment {
    
    public void setConnInterface(ConnFragmentInterface in) {
        m_interface = in;
    }

    protected void setProgressVisible(boolean visible) {
        Activity act = getActivity();
        if(act == null || !(act instanceof ConnectionsActivity))
            return;
        
        ((ConnectionsActivity)act).setProgressIndicator(visible);
    }

    protected ConnFragmentInterface m_interface;
}