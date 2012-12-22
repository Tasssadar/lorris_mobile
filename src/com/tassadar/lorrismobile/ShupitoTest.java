package com.tassadar.lorrismobile;

import java.io.IOException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.widget.TextView;

import com.tassadar.usb_acm.SerialDevice;
import com.tassadar.usb_acm.SerialDevice.SerialDeviceListener;
import com.tassadar.usb_acm.SerialDeviceMgr;
import com.tassadar.usb_acm.SerialDeviceMgr.SerialDeviceMgrListener;

@TargetApi(12)
public class ShupitoTest extends Activity implements SerialDeviceMgrListener,SerialDeviceListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        setContentView(R.layout.shupito_test);

        m_out = (TextView)findViewById(R.id.out);
        
        m_devMgr = new SerialDeviceMgr(this, this);
        m_devMgr.registerReceiver(this);

        m_devMgr.handleRawDevice((UsbDevice)getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE));
    }

    @Override
    protected void onPause () {
        super.onPause();

        m_devMgr.unregisterReceiver(this);
        if(m_dev != null) {
            m_dev.close();
            m_dev = null;
        }
    }

    @Override
    protected void onResume () {
        super.onResume();

        m_devMgr.registerReceiver(this);
    }

    @Override
    public void onNewDevice(SerialDevice dev) {
        
        try {
            dev.open(this);
        } catch(IOException ex) {
            m_out.setText(ex.getMessage());
            return;
        }

        try {
            m_out.append("Sending \"?\" command to Shupito...\n");

            byte[] cmd = { '?' };
            dev.write(cmd, 1000);

            m_out.append("Getting response...\n");
        } catch(IOException ex) {
            m_out.append(ex.getMessage());
        }
        m_dev = dev;
    }

    @Override
    public void onDataRead(byte[] data) {
        m_out.append("Read: ");
        m_out.append(new String(data) + "\n");
    }

    private TextView m_out;
    private SerialDeviceMgr m_devMgr;
    private SerialDevice m_dev;
}