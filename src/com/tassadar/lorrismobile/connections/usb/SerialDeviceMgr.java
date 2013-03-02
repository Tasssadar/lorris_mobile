package com.tassadar.lorrismobile.connections.usb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

@TargetApi(12)
public class SerialDeviceMgr {

    private static final String ACTION_USB_PERMISSION = "com.tassadar.lorrismobile.USB_PERMISSION";

    public interface SerialDeviceMgrListener {
        void onNewDevice(SerialDevice dev);
    }

    public SerialDeviceMgr(SerialDeviceMgrListener listener, Context ctx) {
        m_listener = listener;
        m_usbManager = (UsbManager)ctx.getSystemService(Context.USB_SERVICE);

        m_devices = new ArrayList<SerialDevice>();
        m_permissionIntent = PendingIntent.getBroadcast(ctx, 0, new Intent(ACTION_USB_PERMISSION), 0);
    }

    public void registerReceiver(Context ctx) { 
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        // Not working. Of course not.
        //filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        ctx.registerReceiver(m_usbReceiver, filter);
    }

    public void unregisterReceiver(Context ctx) {
        ctx.unregisterReceiver(m_usbReceiver);
    }

    public void handleRawDevice(UsbDevice dev) {
        m_usbManager.requestPermission(dev, m_permissionIntent);
    }

    public static int matchRawDeviceToType(UsbDevice dev) {
        if (dev.getInterfaceCount() >= 2 && 
            dev.getInterface(0).getInterfaceClass() == UsbConstants.USB_CLASS_COMM &&
            dev.getInterface(1).getInterfaceClass() == UsbConstants.USB_CLASS_CDC_DATA)
        {
            return SerialDevice.TYPE_CDC_ACM;
        }

        return SerialDevice.TYPE_UNK;
    }

    private void processRawDevice(UsbDevice usbdev) {
        SerialDevice dev = SerialDevice.create(usbdev, this);
        if(dev == null)
            return;

        m_listener.onNewDevice(dev);
    }
    
    public UsbDeviceConnection openDevConnection(UsbDevice dev) {
        return m_usbManager.openDevice(dev);
    }

    public void enumerate() {
        HashMap<String, UsbDevice> deviceList = m_usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();

            boolean added = false;
            for(SerialDevice d : m_devices)
                if(d.getUsbDevice() == device)
                    added = true;

            if(!added)
                handleRawDevice(device);
        }
    }

    private final BroadcastReceiver m_usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e("Lorris", "received " + action);
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        processRawDevice(device);
                    }
                }
            } else if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null)
                        handleRawDevice(device);
                }
            } else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null) {
                        for(SerialDevice d : m_devices) {
                            if(d.getUsbDevice() == device){
                                d.usbDeviceDeatached();
                                break;
                            }
                        }
                    }
                }
            }
        }
    };

    private SerialDeviceMgrListener m_listener;
    private UsbManager m_usbManager;
    private PendingIntent m_permissionIntent;
    private ArrayList<SerialDevice> m_devices;
}