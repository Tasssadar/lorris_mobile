package com.tassadar.lorrismobile.connections;

import android.bluetooth.BluetoothAdapter;

public class BTSerialPort extends Connection {

    public static boolean isAvailable() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if(adapter == null)
                return false;
        } catch(Exception ex) {
            return false;
        }
        return true;
    }

    public BTSerialPort() {
        super(Connection.CONN_BT_SP);
    }

    @Override
    public void open() {
        
    }

    @Override
    public void close() {
        
    }

    private BluetoothAdapter m_adapter;
}