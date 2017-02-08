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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Spinner;

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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private void addShupitoTunnel(SerialDevice dev) {
        View v = getView();
        if(v == null)
            return;

        LinearLayout l = (LinearLayout)v.findViewById(R.id.usb_devices);

        Activity act = getActivity();
        View it = View.inflate(act, R.layout.shupito_tunnel_item, null);

        Spinner s = (Spinner)it.findViewById(R.id.tunnel_speed);
        s.setSelection(2);

        LinearLayout lit = (LinearLayout)it.findViewById(R.id.shupito_tunnel_item_layout);
        lit.setOnClickListener(new TunnelClickListener(dev));

        l.addView(it);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Override
    public void onNewDevice(SerialDevice dev) {
        // Shupito 2.0
        if (dev.getUsbDevice().getVendorId() == 0x4a61 &&
            dev.getUsbDevice().getProductId() == 0x679a)
        {
            addShupitoTunnel(dev);
        }
    }

    private class TunnelClickListener implements OnClickListener {
        private SerialDevice m_dev;

        public TunnelClickListener(SerialDevice dev) {
            m_dev = dev;
        }

        @Override
        public void onClick(View v) {
            Spinner s = (Spinner)v.findViewById(R.id.tunnel_speed);

            ShupitoTunnelConnection conn = ConnectionMgr.createShupitoTunnel(m_dev);
            conn.setTunnelSpeed(Integer.parseInt((String)s.getSelectedItem()));
            m_interface.onConnectionSelected(conn);
        }
    }

    private SerialDeviceMgr m_mgr;
    private ShupitoPortConnection m_conn;
}
