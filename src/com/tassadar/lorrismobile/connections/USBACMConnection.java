package com.tassadar.lorrismobile.connections;

import java.io.IOException;
import java.lang.ref.WeakReference;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.tassadar.lorrismobile.connections.usb.SerialDevice;
import com.tassadar.lorrismobile.connections.usb.SerialDevice.SerialDeviceListener;


@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class USBACMConnection extends Connection implements SerialDeviceListener {

    private static final int WRITE_STOP = 0;
    private static final int WRITE_DATA = 1;

    private static final int WRITE_TIMEOUT = 5000; // ms

    public USBACMConnection() {
        super(CONN_USB_ACM);
    }

    public void setDevice(SerialDevice dev) {
        m_dev = dev;
    }

    @Override
    public void open() {
        if(m_dev == null && m_state != ST_DISCONNECTED)
            return;
        
        setState(ST_CONNECTING);
        try {
            m_dev.open(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(m_dev.isOpen())
            setState(ST_CONNECTED);
        else 
            setState(ST_DISCONNECTED);
    }

    @Override
    public void setState(int state) {
        if(state == m_state)
            return;

        switch(state) {
            case Connection.ST_CONNECTED:
            {
                m_writeThread = new WriteThread();
                m_writeThread.start();
                break;
            }
            case Connection.ST_DISCONNECTED:
            {
                if(m_writeThread == null)
                    break;
                m_writeThread.getHandler().sendEmptyMessage(WRITE_STOP);
                break;
            }
        }

        super.setState(state);
    }

    @Override
    public void close() {
        if(!isOpen())
            return;

        sendDisconnecting();

        setState(ST_DISCONNECTED);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(m_writeThread != null)
                        m_writeThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                m_writeThread = null;
                m_dev.close();
            }
        }).start();
    }

    @Override
    public void onDisconnect() {
        close();
    }

    @Override
    public void onDataRead(byte[] data) {
        Log.e("Lorris", "onDataRead");
        sendDataRead(data);
    }

    @Override
    public void write(byte[] data, int offset, int count) {
        if(isOpen())
            m_writeThread.getHandler().obtainMessage(WRITE_DATA, offset, count, data.clone()).sendToTarget();
    }

    private class WriteThread extends Thread {
        private volatile Handler m_write_handler = null;
        private Object m_handler_lock = new Object();

        public Handler getHandler() {
            if(m_write_handler == null) {
                try {
                    synchronized(m_handler_lock) {
                        m_handler_lock.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return m_write_handler;
        }

        public void run() {
            Looper.prepare();

            m_write_handler = new WriteHandler(m_dev);

            synchronized(m_handler_lock) {
                m_handler_lock.notifyAll();
            }

            Looper.loop();
        }
    }

    static class WriteHandler extends Handler {
        private final WeakReference<SerialDevice> m_devRef; 

        WriteHandler(SerialDevice dev) {
            m_devRef = new WeakReference<SerialDevice>(dev);
        }

        @Override
        public void handleMessage(Message msg)
        {
             SerialDevice dev = m_devRef.get();
             if (dev == null)
                 return;
             
             switch(msg.what) {
                 case WRITE_STOP:
                     Looper.myLooper().quit();
                     break;
                 case WRITE_DATA:
                 {
                     try {
                        dev.write((byte[])msg.obj, msg.arg1, msg.arg2, WRITE_TIMEOUT);
                     } catch (IOException e) {
                        e.printStackTrace();
                     }
                     break;
                 }
             }
        }
    }

    private SerialDevice m_dev;
    private volatile WriteThread m_writeThread;
}