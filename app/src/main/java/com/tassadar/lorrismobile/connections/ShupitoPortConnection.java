package com.tassadar.lorrismobile.connections;

import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

import com.tassadar.lorrismobile.ByteArray;


public class ShupitoPortConnection extends ShupitoConnection implements ConnectionInterface {
    
    private static final int PST_INIT0     = 0;
    private static final int PST_INIT1     = 1;
    private static final int PST_INIT2     = 2;
    private static final int PST_DISCARD   = 3;
    private static final int PST_CMD       = 4;
    private static final int PST_DATA      = 5;

    private static final byte[] PKT_START = new byte[] { (byte)0x80 };

    public ShupitoPortConnection() {
        super();
        m_holdsTabRef = false;
        m_readDesc = false;
        m_partialDesc = new ByteArray();
        m_partialPacket = new ByteArray();
        m_parserLen = 0;
    }

    public void setPort(Connection port) {
        assert isOpen() == false : "Can't set port when connection is opened!";
        
        if(port == m_port)
            return;
        
        if(m_port != null) {
            m_port.removeInterface(this);
            releasePortTabRef();
            m_port.rmRef();
        }

        m_port = port;

        if(m_port != null) {
            m_port.addInterface(this);
            m_port.addRef();

            stateChanged(m_port.getState());
        }
    }

    @Override
    public void open() {
        if(m_port == null)
            return;

        m_readDesc = false;
        setState(ST_CONNECTING);

        addPortTabRef();

        if(!m_port.isOpen()) {
            m_port.open();
            if(m_port.getState() == ST_DISCONNECTED) {
                setState(ST_DISCONNECTED);
                releasePortTabRef();
            }
        } else {
            m_parserState = PST_INIT0;
            requestDesc();
        }
    }

    @Override
    public void close() {
        if(!isOpen())
            return;

        sendDisconnecting();
        setState(ST_DISCONNECTED);
        releasePortTabRef();

        m_desc = null;
        m_partialDesc.clear();
        m_partialPacket.clear();
    }

    @Override
    public void sendPacket(byte[] packet) {
        assert(packet.length >= 1 && packet.length <= 16);
        assert(packet[0] < 16);

        packet[0] = (byte)((packet[0] << 4) | (packet.length-1));
        m_port.write(PKT_START);
        m_port.write(packet);
    }

    @Override
    public void requestDesc() {
        if(!m_readDesc) {
            Log.i("Lorris", "ShupitoPortConnection: request descriptor");
            m_descTimer = new Timer();
            m_descTimeoutTask = new DescTimeoutTask();
            m_descTimer.schedule(m_descTimeoutTask, 1000);

            // GC because we are expecting stream, and 
            // if android GC's during the stream, 
            // we are screwed - packet loss.
            System.gc();
            sendPacket(ShupitoPacket.make(0, 0x00));
            m_readDesc = true;
        }
    }


    @Override
    public void connected(boolean connected) { }

    // stateChanged of m_port
    @Override
    public void stateChanged(int state) {
        switch(state) {
            case ST_DISCONNECTED:
                releasePortTabRef();
                setState(ST_DISCONNECTED);
                break;
            case ST_CONNECTED:
                m_parserState = PST_INIT0;
                requestDesc();
                break;
        }
    }

    @Override
    public void disconnecting() {
        close();
    }

    @Override
    public void dataRead(byte[] data) {
        if(m_state == ST_DISCONNECTED)
            return;

        for(byte b : data) {
            switch(m_parserState) {
            case PST_INIT0:
                if(b == (byte)0x80)
                    m_parserState = PST_INIT1;
                break;
            case PST_INIT1:
                if(b == (byte)0x0f)
                    m_parserState = PST_INIT2;
                else if(b != (byte)0x80)
                    m_parserState = PST_INIT0;
                break;
            case PST_INIT2:
                if(b == (byte)0x01) {
                    m_parserState = PST_DATA;
                    m_partialPacket.clear();
                    m_parserLen = 0xF;
                    m_partialPacket.append(0x00);
                    m_partialPacket.append(0x01);
                } else if(b == (byte)0x80) {
                    m_parserState = PST_INIT1;
                } else {
                    m_parserState = PST_INIT0;
                }
                break;
            case PST_DISCARD:
                if(b == (byte)0x80) {
                    m_parserState = PST_CMD;
                    m_partialPacket.clear();
                }
                break;
            case PST_CMD:
            {
                int n = (int)(b & 0xFF);
                m_partialPacket.clear();
                m_parserLen = n & 0xF;
                m_partialPacket.append(n >> 4);
                if(m_parserLen == 0) {
                    handlePacket();
                    m_parserState = PST_DISCARD;
                } else {
                    m_parserState = PST_DATA;
                }
                break;
            }
            case PST_DATA:
                assert(m_partialPacket.size() < m_parserLen + 1);
                m_partialPacket.append(b);
                if(m_partialPacket.size() == m_parserLen + 1) {
                    handlePacket();
                    m_parserState = PST_DISCARD;
                }
                break;
            }
        }
    }

    private void handlePacket() {
        Log.e("Lorris", "handle packet");
        if(m_readDesc && m_partialPacket.at(0) == 0) {
            m_partialDesc.append(m_partialPacket.data(), 1, m_partialPacket.size()-1);
            if(m_partialPacket.size() < 16) {
                m_readDesc = false;
                ShupitoDesc desc = new ShupitoDesc();
                try {
                    Log.e("Lorris", "add data");
                    desc.addData(m_partialDesc);

                    if(clearTimeoutTask()) {
                        m_desc = desc;
                        setState(ST_CONNECTED);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
                m_partialPacket.clear();
            }
        } else {
            sendDataRead(m_partialPacket.toByteArray());
            Log.e("Lorris", "packet " + m_partialPacket);
        }
    }

    private void addPortTabRef() {
        if(m_port == null || m_holdsTabRef)
            return;

        m_holdsTabRef = true;
        m_port.addTabRef();
    }

    private void releasePortTabRef() {
        if(!m_holdsTabRef)
            return;

        m_holdsTabRef = false;
        if(m_port != null)
            m_port.rmTabRef();
    }

    @Override
    public ShupitoDesc getDesc() {
        return m_desc;
    }

    private boolean clearTimeoutTask() {
        if(m_descTimer == null)
            return false;

        boolean res = m_descTimeoutTask.cancel();
        m_descTimeoutTask = null;
        m_descTimer = null;
        return res;
    }

    private class DescTimeoutTask extends TimerTask {
        @Override
        public void run() {
            setState(ST_DISCONNECTED);
            releasePortTabRef();
            m_readDesc = false;
            clearTimeoutTask();
        }
    }

    private Connection m_port;
    private boolean m_holdsTabRef;
    private int m_parserState;
    private boolean m_readDesc;
    private ShupitoDesc m_desc;
    private ByteArray m_partialDesc;
    private ByteArray m_partialPacket;
    private int m_parserLen;
    private Timer m_descTimer;
    private DescTimeoutTask m_descTimeoutTask;
}