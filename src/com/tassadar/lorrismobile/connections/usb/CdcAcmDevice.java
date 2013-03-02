// Largely inspired by http://code.google.com/p/usb-serial-for-android/

package com.tassadar.lorrismobile.connections.usb;

import java.io.IOException;

import android.annotation.TargetApi;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import com.tassadar.lorrismobile.ByteArray;

@TargetApi(12)
public class CdcAcmDevice extends SerialDevice {

    private static final int DEFAULT_BAUDRATE = 38400;
    private static final int DEFAULT_STOPBITS = 1;
    private static final int DEFAULT_PARITY = 0;
    private static final int DEFAULT_DATABITS = 8;

    private static final int SET_LINE_CODING = 0x20;
    private static final int USB_RECIP_INTERFACE = 0x01;
    private static final int USB_RT_ACM = UsbConstants.USB_TYPE_CLASS | USB_RECIP_INTERFACE;

    public CdcAcmDevice(UsbDevice dev, SerialDeviceMgr mgr) {
        super(dev, mgr);
    }

    @Override
    public int getType() { return SerialDevice.TYPE_CDC_ACM; }

    @Override
    protected void openDevice() throws IOException {
        if(m_dev.getInterfaceCount() < 2)
            throw new IOException("Couldn't open CDC ACM device: interface count is too low");

        m_ctrlInterface = m_dev.getInterface(0);
        if(m_ctrlInterface.getInterfaceClass() != UsbConstants.USB_CLASS_COMM)
            throw new IOException("Couldn't open CDC ACM device: wrong ctrl interface class");
        
        m_dataInterface = m_dev.getInterface(1);
        if(m_dataInterface.getInterfaceClass() != UsbConstants.USB_CLASS_CDC_DATA)
            throw new IOException("Couldn't open CDC ACM device: wrong data interface class");
        
        if(!m_conn.claimInterface(m_ctrlInterface, true))
            throw new IOException("Couldn't open CDC ACM device: failed to claim ctrl interface");
        
        if(!m_conn.claimInterface(m_dataInterface, true)) {
            m_conn.releaseInterface(m_ctrlInterface);
            throw new IOException("Couldn't open CDC ACM device: failed to claim data interface");
        }

        //m_ctrlEndpoint = m_ctrlInterface.getEndpoint(0);

        m_writeEndpoint = m_dataInterface.getEndpoint(0);
        m_readEndpoint = m_dataInterface.getEndpoint(1);
        
        m_baudrate = DEFAULT_BAUDRATE;
        m_stopBits = DEFAULT_STOPBITS;
        m_parity = DEFAULT_PARITY;
        m_dataBits = DEFAULT_DATABITS;

        //m_waiterThread = new WaiterThread();
        //m_waiterThread.start();

        m_readThread = new ReadThread(m_readBuffer, m_readEndpoint, m_conn);
        m_readThread.start();

        m_dispatchThread = new DispatchThread(m_readBuffer);
        m_dispatchThread.start();

        //m_poolThread = new PoolThread();
        //m_poolThread.start();

        sendLineCoding();
    }

    @Override
    public void close() {
        /*if(m_waiterThread != null) {
            m_waiterThread.stop = true;
            m_waiterThread = null;
        }*/

        if(m_dispatchThread != null) {
            m_readThread.stopThread();
            m_readThread = null;
            m_readBuffer.close();
            m_dispatchThread.stopThread();
            m_dispatchThread = null;
            //m_poolThread.stopThread();
            //m_poolThread = null;
        }
        super.close();
    }

    private void sendLineCoding() {
        
        byte[] msg = { 
                (byte)(m_baudrate),
                (byte)((m_baudrate >> 8)),
                (byte)((m_baudrate >> 16)),
                (byte)((m_baudrate >> 24)),
                
                (byte)m_stopBits,
                (byte)m_parity,
                (byte)m_dataBits
        };

        sendAcmCtrlMsg(SET_LINE_CODING, 0, msg);
    }

    private int sendAcmCtrlMsg(int request, int value, byte[] buf) {
        return m_conn.controlTransfer(USB_RT_ACM, request, value, 0, buf, buf != null ? buf.length : 0, 5000);
    }

    @Override
    public int read(byte[] dest, int offset, int timeoutMs) throws IOException {
        int read;
        synchronized (m_readLock) {
            int readAmt = Math.min(dest.length-offset, m_readBuff.length);
            readAmt = Math.min(readAmt, m_readEndpoint.getMaxPacketSize());
            read = m_conn.bulkTransfer(m_readEndpoint, m_readBuff, readAmt, timeoutMs);
            if (read < 0) {
                // This sucks: we get -1 on timeout, not 0 as preferred.
                // We *should* use UsbRequest, except it has a bug/api oversight
                // where there is no way to determine the number of bytes read
                // in response :\ -- http://b.android.com/28023
                return 0;
            }
            System.arraycopy(m_readBuff, 0, dest, offset, read);
        }
        return read;
    }

    @Override
    public int write(byte[] src, int offset, int count, int timeoutMs) throws IOException {
        while (count > 0) {
            final int writeLength;
            final int written;

            synchronized (m_writeLock) {
                final byte[] writeBuffer;

                writeLength = Math.min(count, m_writeBuff.length);
                if (offset == 0) {
                    writeBuffer = src;
                } else {
                    // bulkTransfer does not support offsets, make a copy.
                    System.arraycopy(src, offset, m_writeBuff, 0, writeLength);
                    writeBuffer = m_writeBuff;
                }

                written = m_conn.bulkTransfer(m_writeEndpoint, writeBuffer, writeLength, timeoutMs);
            }
            if (written <= 0) {
                throw new IOException("Error writing " + writeLength
                        + " bytes at offset " + offset + " length=" + count);
            }

            offset += written;
            count -= written;
        }
        return offset;
    }

    private class ReadBuffer {
        private ByteArray m_data = new ByteArray();
        private boolean m_open = true;

        public ReadBuffer() {
            m_data.reserve(512);
        }

        public void close() {
            synchronized(this) {
                m_open = false;
            }
        }

        public boolean getData(ByteArray target) {
            synchronized(this) {
                if(!m_open)
                    return false;
                if(!m_data.empty())
                    m_data.swap(target);
                return true;
            }
        }

        public void post(byte[] data, int len) {
            synchronized(this) {
                m_data.append(data, 0, len);
            }
        }
    }

    private class ReadThread extends Thread {
        private ReadBuffer m_buff;
        private boolean m_run;
        private UsbEndpoint m_end;
        private UsbDeviceConnection m_connect;

        public ReadThread(ReadBuffer b, UsbEndpoint e, UsbDeviceConnection conn) {
            m_buff = b;
            m_end = e;
            m_connect = conn;
            setPriority(MAX_PRIORITY);
        }

        @Override
        public void run() {
            m_run = true;
            final int maxLen = m_readEndpoint.getMaxPacketSize();
            byte[] buff = new byte[maxLen];
            int len;
            while(m_run) {
                len = m_connect.bulkTransfer(m_end, buff, maxLen, 0);
                if(len > 0)
                    m_buff.post(buff, len);
            }
        }

        public void stopThread() {
            m_run = false;
        }
    }

    private class DispatchThread extends Thread {
        private ReadBuffer m_buff;
        private boolean m_run;
        private ByteArray myBuff;
        public DispatchThread(ReadBuffer b) {
            m_buff = b;
            myBuff = new ByteArray();
        }

        @Override
        public void run() {
            m_run = true;
            while(m_run) {
                if(!m_buff.getData(myBuff))
                    break;

                if(m_listener != null && !myBuff.empty()) {
                    m_listener.onDataRead(myBuff.toByteArray());
                }

                myBuff.setEmpty();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
        
        public void stopThread() {
            m_run = false;
        }
    }


/*    private class UsbReadRunnable implements Callable<Integer> {
        public ByteArray m_buff;
        @Override
        public Integer call() throws Exception {
            int len;
            synchronized(m_readBuff) {
                len = m_conn.bulkTransfer(m_readEndpoint, m_buff.data(), m_readEndpoint.getMaxPacketSize(), 0);
                //m_buff.setSize(len);
            }
            if(len > 0)
                m_readBuffer.post(m_buff.data(), len);
            return 1;
        }
    }

    private class UsbDispatchRunnable implements Callable<Integer> {
        public ByteArray m_buff;
        @Override
        public Integer call() throws Exception {
            m_readBuffer.post(m_buff.data(), m_buff.size());
            return 2;
        }
    }
    
    private class PoolThread extends Thread {
        private ExecutorService m_service;
        private ArrayList<Future<Integer>> m_futures;
        private int m_head;
        private ByteArray[] m_readBuffers;
        private boolean m_run;
        
        private static final int TASKS = 16;

        public PoolThread() {
            m_service = Executors.newFixedThreadPool(TASKS);
            m_futures = new ArrayList<Future<Integer>>();
            m_head = 0;
            m_readBuffers = new ByteArray[TASKS];
            for(int i = 0; i < m_readBuffers.length; ++i)
            {
                m_readBuffers[i] = new ByteArray();
                m_readBuffers[i].reserve(m_readEndpoint.getMaxPacketSize());
            }
        }

        public void run() {
            m_run = true;
            m_futures.clear();

            for(int i = 0; i < TASKS; ++i) {
                UsbReadRunnable r = new UsbReadRunnable();
                r.m_buff = m_readBuffers[i];
                m_futures.add(m_service.submit(r));
            }

            for (; m_run; m_head = (m_head + 1) % TASKS) {
                if(!m_futures.get(m_head).isDone())
                    continue;

                //if(m_readBuffers[m_head].size() > 0)

                UsbReadRunnable r = new UsbReadRunnable();
                r.m_buff = m_readBuffers[m_head];
                m_futures.set(m_head, m_service.submit(r));
            }
        }

        public void stopThread() {
            m_run = false;
        }
    }*/
    
    
    public void setBaudRate(int baudrate) {
        m_baudrate = baudrate;
        sendLineCoding();
    }

    public void setStopBits(int stopbits) {
        m_stopBits = stopbits;
        sendLineCoding();
    }

    public void setParity(int parity) {
        m_parity = parity;
        sendLineCoding();
    }

    public void setDataBits(int databits) {
        m_dataBits = databits;
        sendLineCoding();
    }

    /*
    private class WaiterThread extends Thread {
        public boolean stop;
        @Override
        public void run() {
            UsbRequest req = new UsbRequest();
            req.initialize(m_conn, m_readEndpoint);

            int max_len = 16;//m_readEndpoint.getMaxPacketSize();
            ByteBuffer buff = ByteBuffer.allocate(max_len);

            Log.e("Lorris", "len " + max_len);

            req.queue(buff, 2);
            while(true) {
                synchronized(this) {
                    if(stop)
                        return;
                }

                if(m_conn.requestWait() == req) {
                    if(buff.get(0) == (byte)0x80) {
                        int len = (buff.get(1) & 0xF)-1;
                        Log.e("Lorris", "cont len " + len);
                        buff = ByteBuffer.allocate(max_len);
                        req.queue(buff, len);
                    }
                    else
                        continue;
                } else {
                    Log.e("Lorris", "Empty USBRequest returned!");
                    break;
                }

                if(m_conn.requestWait() == req) {
                    ByteBuffer old = buff;
                    buff = ByteBuffer.allocate(max_len);
                    req.queue(buff, 2);
                    if(m_listener != null) {
                        Log.e("Lorris", "buff " + old.capacity() + " neco " + old.position());
                        m_listener.onDataRead(old.array());
                    }
                }
            }
        }
    }*/

    private UsbInterface m_ctrlInterface;
    private UsbInterface m_dataInterface;
    
    //private UsbEndpoint m_ctrlEndpoint;
    private UsbEndpoint m_readEndpoint;
    private UsbEndpoint m_writeEndpoint;
    //private WaiterThread m_waiterThread;
    private ReadBuffer m_readBuffer = new ReadBuffer();
    private ReadThread m_readThread;
    private DispatchThread m_dispatchThread;
    //private PoolThread m_poolThread;
}
