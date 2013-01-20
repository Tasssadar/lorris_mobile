package com.tassadar.lorrismobile.connections;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tassadar.lorrismobile.R;

public class BTConnFragment extends ConnFragment {

    public BTConnFragment(ConnFragmentInterface in) {
        super(in);
    }

    private static final int REQUEST_ENABLE_BT = 1;

    @Override 
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(m_bt_receiver, filter); // Don't forget to unregister during onDestroy
        
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        activity.registerReceiver(m_bt_receiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        activity.registerReceiver(m_bt_receiver, filter);
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        
        m_displayedDevices = new ArrayList<String>();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(m_adapter != null)
            m_adapter.cancelDiscovery();

        setProgressVisible(false);
        if(getActivity() != null)
            getActivity().unregisterReceiver(m_bt_receiver);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.conn_bt_fragment, container, false);
        v.findViewById(R.id.enable_bt_btn).setOnClickListener(new BtEnableClickedLister());
        return v;
    }
    

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.conn_bt_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch(item.getItemId()) {
            case R.id.refresh_devices:
                searchForDevices();
                return true;
        }
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();

        checkBluetooth();
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == Activity.RESULT_OK)
                    checkBluetooth();
                break;
        }
    }

    private void checkBluetooth() {
        m_adapter = BluetoothAdapter.getDefaultAdapter();
        if(m_adapter == null) {
            setActiveSection(R.id.bt_not_found);
            return;
        }
        if(!m_adapter.isEnabled()) {
            setActiveSection(R.id.bt_not_enabled_layout);
            return;
        }

        setActiveSection(R.id.devices_view);
        searchForDevices();
    }

    private void searchForDevices() {
        m_displayedDevices.clear();

        ((LinearLayout)getView().findViewById(R.id.new_devices)).removeAllViews();
        ((LinearLayout)getView().findViewById(R.id.paired_devices)).removeAllViews();
        
        if(m_adapter == null || !m_adapter.isEnabled())
            return;

        Set<BluetoothDevice> paired = m_adapter.getBondedDevices();
        for(BluetoothDevice d : paired)
            addBtDevice(d, R.id.paired_devices);

        if(m_adapter.isDiscovering())
            setProgressVisible(true);
        else
            m_adapter.startDiscovery();
    }

    private void setActiveSection(int id) {
        final int[] sections = { R.id.devices_view, R.id.bt_not_enabled_layout, R.id.bt_not_found };
        for(int i : sections) {
            getView().findViewById(i).setVisibility(id == i ? View.VISIBLE : View.GONE);
        }
    }

    private class BtEnableClickedLister implements OnClickListener {
        @Override
        public void onClick(View btn) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        
    }

    private final BroadcastReceiver m_bt_receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                addBtDevice(device, R.id.new_devices);
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
                setProgressVisible(true);
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
                setProgressVisible(false);
        }
    };

    private void addBtDevice(BluetoothDevice device, int list) {
        if(m_displayedDevices.contains(device.getAddress()))
            return;

        LinearLayout l = (LinearLayout)getView().findViewById(list);
        View v = View.inflate(getActivity(), R.layout.bt_list_item, null);

        TextView t = (TextView)v.findViewById(R.id.device_name);
        t.setText(device.getName());
        t = (TextView)v.findViewById(R.id.mac_address);
        t.setText(device.getAddress());

        v.findViewById(R.id.bt_list_item_layout)
            .setOnClickListener(new DeviceClickedLister());

        l.addView(v);
        
        m_displayedDevices.add(device.getAddress());
    }

    private void setProgressVisible(boolean visible) {
        try {
            ConnectionsActivity act = (ConnectionsActivity)getActivity();
            act.setProgressIndicator(visible);
        } catch(ClassCastException ex) {
            assert(false);
        }
    }

    private class DeviceClickedLister implements OnClickListener {
        @Override
        public void onClick(View l) {
            TextView t = (TextView)l.findViewById(R.id.mac_address);
            if(t == null || t.getText().length() == 0)
                return;

            m_adapter.cancelDiscovery();
            BluetoothDevice device;
            try {
                device = m_adapter.getRemoteDevice(t.getText().toString());

                BTSerialPort conn = ConnectionMgr.createBTSerial(device);
                m_interface.onConnectionSelected(conn);
            } catch(IllegalArgumentException ex) {
                Toast.makeText(getActivity(), "Failed to create BT device", Toast.LENGTH_SHORT).show();
            }
        }
        
    }

    private BluetoothAdapter m_adapter;
    private ArrayList<String> m_displayedDevices;
}