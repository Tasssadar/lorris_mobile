package com.tassadar.lorrismobile.connections;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.tassadar.lorrismobile.BlobInputStream;
import com.tassadar.lorrismobile.BlobOutputStream;

public class TCPConnection extends Connection {

    private static final int STATE_OK      = 0;
    private static final int STATE_FAILED  = 1;

    private static final int SRC_CONNECT_THREAD = 0;
    private static final int SRC_POLL_THREAD    = 1;
    private static final int SRC_WRITE_THREAD   = 2;

    private static final int WRITE_STOP = 0;
    private static final int WRITE_DATA = 1;

    public TCPConnection() {
        super(CONN_TCP);
        m_proto = new TCPConnProto();
        m_handler = new StateHandler(this);
    }

    public void setProto(TCPConnProto p) {
        m_proto = p;
    }

    @Override
    public void open() {
        if(m_socket != null || m_state != ST_DISCONNECTED || m_connectThread != null)
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
                    if(m_writeThread != null)
                        m_writeThread.join();

                    if(m_pollThread != null)
                        m_pollThread.join();
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

    @Override
    protected void saveDataStream(BlobOutputStream str) {
        str.writeString("name", m_proto.name);
        str.writeString("address", m_proto.address);
        str.writeInt("port", m_proto.port);
    }

    @Override
    protected void loadDataStream(BlobInputStream str) {
        m_proto.name = str.readString("name");
        m_proto.address = str.readString("address");
        m_proto.port = str.readInt("port");
    }

    @Override
    public String getName() {
        return m_proto.name;
    }

    public TCPConnProto getProto() {
        return m_proto;
    }

    @Override
    public void write(byte[] data, int offset, int count) {
        if(isOpen())
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
        private final WeakReference<TCPConnection> m_conn; 

        StateHandler(TCPConnection conn) {
            super(Looper.getMainLooper());
            m_conn = new WeakReference<TCPConnection>(conn);
        }

        @Override
        public void handleMessage(Message msg)
        {
             TCPConnection c = m_conn.get();
             if (c == null)
                 return;
             
             switch(msg.what) {
                 case SRC_CONNECT_THREAD:
                 {
                     c.m_connectThread = null;
                     if(msg.arg1 == STATE_OK) {
                         c.m_socket = (Socket)msg.obj;
                         c.setState(Connection.ST_CONNECTED);
                     }
                     else
                         c.setState(Connection.ST_DISCONNECTED);
                     break;
                 }
                 case SRC_POLL_THREAD:
                 {
                     if(msg.arg1 == STATE_OK)
                         c.sendDataRead((byte[])msg.obj);
                     else
                         c.close();
                     break;
                 }
             }
        }
    }

    private class ConnectThread extends Thread {

        @Override
        public void run() {
            InetAddress addr = null;
            try {
                addr = InetAddress.getByName(m_proto.address);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                m_handler.obtainMessage(SRC_CONNECT_THREAD, STATE_FAILED, 0).sendToTarget();
                return;
            }

            try {
                Socket s = new Socket(addr, m_proto.port);
                m_handler.obtainMessage(SRC_CONNECT_THREAD, STATE_OK, 0, s).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
                m_handler.obtainMessage(SRC_CONNECT_THREAD, STATE_FAILED, 0).sendToTarget();
                return;
            }
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

            byte[] buffer = new byte[4096];
            int bytes;

            m_run = true;
            while(m_run) {
                try {
                    bytes = in.read(buffer);

                    if(bytes == -1)
                        throw new IOException("Failed read");

                    byte[] out = new byte[bytes];
                    System.arraycopy(buffer, 0, out, 0, bytes);
                    sendDataRead(out);
                } catch (IOException e) {
                    e.printStackTrace();
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

    private TCPConnProto m_proto;
    private Socket m_socket;
    private StateHandler m_handler;
    private volatile ConnectThread m_connectThread;
    private volatile WriteThread m_writeThread;
    private volatile PollThread m_pollThread;
}