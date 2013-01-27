package com.tassadar.lorrismobile.connections;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;


public class BTSerialPort extends Connection {

    private static final String RFCOMM_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private static final int STATE_OK      = 0;
    private static final int STATE_FAILED  = 1;

    private static final int SRC_CONNECT_THREAD = 0;
    private static final int SRC_POLL_THREAD    = 1;
    private static final int SRC_WRITE_THREAD   = 2;

    private static final int WRITE_STOP = 0;
    private static final int WRITE_DATA = 1;

    static public BTSerialPort fromData(byte[] data) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter == null)
            return null;

        
        BluetoothDevice dev = null;
        try {
            dev = adapter.getRemoteDevice(new String(data));
        }catch(IllegalArgumentException ex) {
            ex.printStackTrace();
            return null;
        }

        BTSerialPort sp = new BTSerialPort(dev);
        return sp;
    }

    public BTSerialPort(BluetoothDevice device) {
        super(Connection.CONN_BT_SP);
        m_device = device;
        m_handler = new StateHandler(this);
        m_connectThread = null;
    }

    @Override
    public void open() {
        if(m_state != ST_DISCONNECTED || m_connectThread != null)
            return;

        setState(Connection.ST_CONNECTING);
        m_connectThread = new ConnectThread();
        m_connectThread.start();
    }

    @Override
    public void close() {
        if(m_state == ST_DISCONNECTED)
            return;

        if(m_state == ST_CONNECTED)
            sendDisconnecting();

        setState(ST_DISCONNECTED);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(m_writeThread != null) {
                        synchronized(m_writeThread) {
                            m_writeThread.wait(5000);
                        }
                    }
                    if(m_pollThread != null) {
                        synchronized(m_pollThread) {
                            m_pollThread.wait(5000);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                m_writeThread = null;
                m_pollThread = null;
                try {
                    if(m_socket != null)
                        m_socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                m_socket = null;
            }
        }).start();
    }

    public byte[] saveData() {
        return m_device.getAddress().getBytes();
    }

    @Override
    public String getName() {
        if(m_device == null)
            return super.getName();
        return m_device.getName();
    }

    public String getAddress() {
        if(m_device != null)
            return m_device.getAddress();
        return "";
    }

    @Override
    public void write(byte[] data, int offset, int count) {
        m_writeThread.getHandler().obtainMessage(WRITE_DATA, offset, count, data.clone()).sendToTarget();
    }

    @Override
    public void setState(int state) {
        if(state == m_state)
            return;

        switch(state) {
            case Connection.ST_CONNECTED:
            {
                m_pollThread = new PollThread();
                m_pollThread.start();
                m_writeThread = new WriteThread();
                m_writeThread.start();
                break;
            }
            case Connection.ST_DISCONNECTED:
            {
                if(m_pollThread == null)
                    break;
                m_pollThread.stopPolling();
                m_writeThread.getHandler().sendEmptyMessage(WRITE_STOP);
                break;
            }
        }

        super.setState(state);
    }

    static class StateHandler extends Handler {
        private final WeakReference<BTSerialPort> m_port; 

        StateHandler(BTSerialPort port) {
            super(Looper.getMainLooper());
            m_port = new WeakReference<BTSerialPort>(port);
        }

        @Override
        public void handleMessage(Message msg)
        {
             BTSerialPort p = m_port.get();
             if (p == null)
                 return;
             
             switch(msg.what) {
                 case SRC_CONNECT_THREAD:
                 {
                     p.m_connectThread = null;
                     if(msg.arg1 == STATE_OK) {
                         p.m_socket = (BluetoothSocket)msg.obj;
                         p.setState(Connection.ST_CONNECTED);
                     }
                     else
                         p.setState(Connection.ST_DISCONNECTED);
                     break;
                 }
                 case SRC_POLL_THREAD:
                 {
                     if(msg.arg1 == STATE_OK)
                         p.sendDataRead((byte[])msg.obj);
                     else
                         p.close();
                     break;
                 }
             }
        }
    }

    private class ConnectThread extends Thread {

        @Override
        public void run() {
            BluetoothSocket socket = null;
            try {
                // FIXME: Is this method of using UUID correct?
                socket = m_device.createRfcommSocketToServiceRecord(UUID.fromString(RFCOMM_UUID));
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(socket == null) {
                m_handler.obtainMessage(SRC_CONNECT_THREAD, STATE_FAILED, 0).sendToTarget();
                return;
            }

            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            adapter.cancelDiscovery();

            try {
                socket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                m_handler.obtainMessage(SRC_CONNECT_THREAD, STATE_FAILED, 0).sendToTarget();
                return;
            }

            m_handler.obtainMessage(SRC_CONNECT_THREAD, STATE_OK, 0, socket).sendToTarget();
        }
    }

    private class PollThread extends Thread {
        private volatile boolean m_run = true;

        @Override
        public void run() {
            InputStream in = null;
            try {
                in = m_socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(in == null)
                m_handler.obtainMessage(SRC_POLL_THREAD, STATE_FAILED, 0).sendToTarget();

            byte[] buffer = new byte[1024];
            int bytes;

            m_run = true;
            while(m_run) {
                try {
                    bytes = in.read(buffer);
                    
                    byte[] out = new byte[bytes];
                    System.arraycopy(buffer, 0, out, 0, bytes);
                    sendDataRead(out);
                    //m_handler.obtainMessage(SRC_POLL_THREAD, STATE_OK, 0, out).sendToTarget();
                } catch (IOException e) {
                    m_handler.obtainMessage(SRC_POLL_THREAD, STATE_FAILED, 1).sendToTarget();
                    break;
                }
            }

            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void stopPolling() {
            m_run = false;
        }
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
            OutputStream out = null;
            
            try {
                out = m_socket.getOutputStream();
            } catch(IOException ex) {
                ex.printStackTrace();
            }

            if(out == null) {
                m_handler.obtainMessage(SRC_WRITE_THREAD, STATE_FAILED, 0).sendToTarget();
                return;
            }

            Looper.prepare();
            
            m_write_handler = new WriteHandler(out);

            synchronized(m_handler_lock) {
                m_handler_lock.notifyAll();
            }

            Looper.loop();

            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    static class WriteHandler extends Handler {
        private final WeakReference<OutputStream> m_str; 

        WriteHandler(OutputStream out) {
            m_str = new WeakReference<OutputStream>(out);
        }

        @Override
        public void handleMessage(Message msg)
        {
             OutputStream out = m_str.get();
             if (out == null)
                 return;
             
             switch(msg.what) {
                 case WRITE_STOP:
                     Looper.myLooper().quit();
                     break;
                 case WRITE_DATA:
                 {
                     try {
                        out.write((byte[])msg.obj, msg.arg1, msg.arg2);
                     } catch (IOException e) {
                        e.printStackTrace();
                     }
                     break;
                 }
             }
        }
    }

    private BluetoothDevice m_device;
    private StateHandler m_handler;
    private BluetoothSocket m_socket;
    private volatile ConnectThread m_connectThread;
    private volatile WriteThread m_writeThread;
    private volatile PollThread m_pollThread;
}