package com.tassadar.lorrismobile.connections;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.tassadar.lorrismobile.R;
import com.tassadar.lorrismobile.connections.usb.SerialDevice;
import com.tassadar.lorrismobile.connections.usb.SerialDeviceMgr;
import com.tassadar.lorrismobile.connections.usb.SerialDeviceMgr.SerialDeviceMgrListener;

public class USBConnFragment extends ConnFragment implements SerialDeviceMgrListener {

    @Override
    public void onAttach(Activity act) {
        super.onAttach(act);
        m_mgr = new SerialDeviceMgr(this, act);
        m_mgr.registerReceiver(act);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(m_conn != null) {
            m_conn.close();
        }
        m_mgr.unregisterReceiver(getActivity());
        m_mgr = null;
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.conn_usb_fragment, container, false);
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        m_mgr.enumerate();
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.conn_bt_usb_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch(item.getItemId()) {
        case R.id.refresh_devices:
            LinearLayout l = (LinearLayout)getView().findViewById(R.id.usb_devices);
            l.removeAllViews();
            m_mgr.enumerate();
            break;
        }
        return false;
    }

    /*private void addShupitoTunnel(UsbDevice dev) {
        
    }*/

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Override
    public void onNewDevice(SerialDevice dev) {
        // Shupito 2.0
        if (dev.getUsbDevice().getVendorId() == 0x4a61 &&
            dev.getUsbDevice().getProductId() == 0x679a)
        {
            USBACMConnection c = new USBACMConnection();
            c.setDevice(dev);

            ShupitoPortConnection s = new ShupitoPortConnection();
            s.setPort(c);

            DescWaiter w = new DescWaiter(s);
            s.addInterface(w);

            s.open();
        }
    }

    private class DescWaiter implements Runnable, ConnectionInterface {

        private ShupitoPortConnection m_conn;
        private ShupitoDesc m_desc;
        public DescWaiter(ShupitoPortConnection conn) {
            m_conn = conn;
            m_desc = null;
        }

        @Override
        public void connected(boolean connected) {
            if(!connected) {
                m_conn.close();
                m_conn.setPort(null);
                m_conn.removeInterface(this);
                m_conn = null;
                return;
            }

            View v = getView();
            if(v == null)
                return;

            v.postDelayed(this, 1000);
            m_conn.requestDesc();
        }

        @Override
        public void stateChanged(int state) { }
        @Override
        public void disconnecting() { }
        @Override
        public void dataRead(byte[] data) { }
        @Override
        public void onDescRead(ShupitoDesc desc) {
            synchronized(m_conn) {
                m_desc = desc;
            }
        }

        @Override
        public void run() {
            synchronized(m_conn) {
                if(m_desc != null) {
                    // Shupito tunnel
                    if(m_desc.getConfig("356e9bf7-8718-4965-94a4-0be370c8797c") != null) {
                        
                    }
                }
                m_conn.close();
                m_conn.setPort(null);
                m_conn.removeInterface(this);
            }
            m_conn = null;
        }
        
    }


    private SerialDeviceMgr m_mgr;
    private ShupitoPortConnection m_conn;
}