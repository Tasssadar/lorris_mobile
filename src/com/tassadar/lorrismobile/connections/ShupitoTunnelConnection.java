package com.tassadar.lorrismobile.connections;

import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

import com.tassadar.lorrismobile.ByteArray;


public class ShupitoTunnelConnection extends Connection implements ConnectionInterface {

    private static final byte MSG_TUNNEL = 0x09;

    public ShupitoTunnelConnection() {
        super(CONN_SHUPITO_TUNNEL);

        m_holdsTabRef = false;
        m_tunnelPipe = 0;
        m_tunnelSpeed = 38400;
        m_readBuff = new ByteArray();
    }

    @Override
    public void open() {
        if(m_shupitoConn == null)
            return;

        setState(ST_CONNECTING);

        addShupitoTabRef();

        if(!m_shupitoConn.isOpen()) {
            m_shupitoConn.open();
            if(m_shupitoConn.getState() == ST_DISCONNECTED) {
                setState(ST_DISCONNECTED);
                releaseShupitoTabRef();
            }
        } else {
            if(!startTunnel()) {
                setState(ST_DISCONNECTED);
                releaseShupitoTabRef();
            }
        }
    }

    @Override
    public void close() {
        if(!isOpen())
            return;

        if(m_readDispatchThread != null) {
            m_readDispatchThread.stopThread();
            try {
                m_readDispatchThread.join(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            m_readDispatchThread = null;
        }

        stopTunnel();

        sendDisconnecting();
        setState(ST_DISCONNECTED);
        releaseShupitoTabRef();
        m_tunnelCfg = null;
        m_tunnelPipe = 0;
    }

    @Override
    public void write(byte[] data, int offset, int count) {
        if(!isOpen() || m_tunnelPipe == 0)
            return;

        int sent = 0;
        while (sent != count) {
            int chunk = count - sent;
            if(chunk > 14)
                chunk = 14;

            byte[] packet = new byte[2+chunk];
            packet[0] = (byte) m_tunnelCfg.cmd;
            packet[1] = (byte) m_tunnelPipe;
            System.arraycopy(data,  offset,  packet, 2, chunk);

            m_shupitoConn.sendPacket(packet);
            sent += chunk;
        }
    }

    public int getTunnelSpeed() {
        return m_tunnelSpeed;
    }

    public void setTunnelSpeed(int speed) {
        m_tunnelSpeed = speed;
        sendSetComSpeed(speed);
    }

    public void setShupitoConn(ShupitoConnection conn) {
        assert isOpen() == false : "ShupitoTunnelConnection: Can't set shupito when connection is opened!";

        if(m_shupitoConn == conn)
            return;

        if( m_shupitoConn != null) {
            m_shupitoConn.removeInterface(this);
            releaseShupitoTabRef();
            m_shupitoConn.rmRef();
        }

        m_shupitoConn = conn;

        if(m_shupitoConn != null) {
            m_shupitoConn.addInterface(this);
            m_shupitoConn.addRef();

            stateChanged(m_shupitoConn.getState());
        }
    }

    private void addShupitoTabRef() {
        if(m_shupitoConn == null || m_holdsTabRef)
            return;

        m_holdsTabRef = true;
        m_shupitoConn.addTabRef();
    }

    private void releaseShupitoTabRef() {
        if(!m_holdsTabRef)
            return;

        m_holdsTabRef = false;
        if(m_shupitoConn != null)
            m_shupitoConn.rmTabRef();
    }

    @Override
    public void stateChanged(int state) {
        switch(state) {
            case ST_DISCONNECTED:
                releaseShupitoTabRef();
                setState(ST_DISCONNECTED);
                break;
            case ST_CONNECTED:
                if(!startTunnel()) {
                    setState(ST_DISCONNECTED);
                    releaseShupitoTabRef();
                }
                break;
        }
    }

    @Override
    public void dataRead(byte[] p) {
        if(m_state == ST_DISCONNECTED || p[0] != MSG_TUNNEL)
            return;

        if(p.length >= 3 && p[1] == 0) {
            switch(p[2]) {
                case 0: // tunnel list
                    Log.i("Lorris", "ShupitoTunnelConnection: packet with tunnel list received");
                    break;
                case 1: // tunnel activated
                {
                    if(p.length != 4)
                        break;

                    m_tunnelPipe = p[3];
                    if(m_tunnelPipe == 0)
                        break;

                    sendSetComSpeed(m_tunnelSpeed);
                    if(clearTimeoutTask()) {
                        setState(ST_CONNECTED);
                        m_readDispatchThread = new ReadDispatchThread();
                        m_readDispatchThread.start();
                    }
                    break;
                }
                case 2: // tunnel disabled
                {
                    if(p.length != 4 || p[3] == m_tunnelPipe)
                        break;
                    m_tunnelPipe = 0;
                    close();
                    break;
                }
            }
        } else if (m_tunnelPipe != 0 && p[1] == m_tunnelPipe) {
            synchronized(m_readBuff) {
                m_readBuff.append(p, 2, p.length-2);
            }
        }
    }

    @Override
    public void connected(boolean connected) { }
    @Override
    public void disconnecting() { }

    private boolean startTunnel() {
        ShupitoDesc desc = m_shupitoConn.getDesc();
        if(desc == null) {
            Log.e("Lorris", "ShupitoTunnelConnection: no ShupitoDesc");
            return false;
        }

        m_tunnelCfg = desc.getConfig("356e9bf7-8718-4965-94a4-0be370c8797c");
        if(m_tunnelCfg == null) {
            Log.e("Lorris", "ShupitoTunnelConnection: desc does not have tunnel cfg");
            return false;
        }

        if(!m_tunnelCfg.always_active())
            m_shupitoConn.sendPacket(m_tunnelCfg.getStateChangeCmd(true));

        setTunnelState(true);

        m_statusTimer = new Timer();
        m_statusTimeoutTask = new StatusTimeoutTask();
        m_statusTimer.schedule(m_statusTimeoutTask, 1000);
        return true;
    }

    private void stopTunnel() {
        setTunnelState(false);
    }

    private void sendSetComSpeed(int speed) {
        if (m_shupitoConn == null || m_tunnelCfg == null || m_tunnelPipe == 0 ||
            m_tunnelCfg.data.size() != 5 || m_tunnelCfg.data.at(0) != 1)
        {
            return;
        }

        long base_freq = m_tunnelCfg.data.uAt(1) | (m_tunnelCfg.data.uAt(2) << 8) |
                (m_tunnelCfg.data.uAt(3) << 16) | (m_tunnelCfg.data.uAt(4) << 24);

        double bsel = ((double)base_freq / speed) - 1;
        byte bscale = 0;
        while(bscale > -6 && bsel < 2048) {
            bsel *= 2;
            --bscale;
        }

        int res = (int)(bsel + 0.5);
        res |= bscale << 12;

        byte[] pkt = ShupitoPacket.make(m_tunnelCfg.cmd, 0, 3, m_tunnelPipe, (byte)res, (byte)(res >> 8));
        m_shupitoConn.sendPacket(pkt);
    }

    private void setTunnelState(boolean enable) {
        if(m_tunnelCfg == null)
            return;

        if(enable && m_tunnelPipe == 0) {
            byte[] cmd = ShupitoPacket.make(m_tunnelCfg.cmd, 0, 1, 'a', 'p', 'p');
            m_shupitoConn.sendPacket(cmd);
        } else if (!enable && m_tunnelPipe != 0) {
            byte[] cmd = ShupitoPacket.make(m_tunnelCfg.cmd, 0, 2, m_tunnelPipe);
            m_shupitoConn.sendPacket(cmd);
        }
    }

    private boolean clearTimeoutTask() {
        if(m_statusTimer == null)
            return false;

        boolean res = m_statusTimeoutTask.cancel();
        m_statusTimeoutTask = null;
        m_statusTimer = null;
        return res;
    }

    private class StatusTimeoutTask extends TimerTask {
        @Override
        public void run() {
            setState(ST_DISCONNECTED);
            releaseShupitoTabRef();
            clearTimeoutTask();
        }
    }

    private class ReadDispatchThread extends Thread {
        private boolean m_run;
        public ReadDispatchThread() {
            m_run = true;
        }

        public void stopThread() {
            m_run = false;
        }

        @Override
        public void run() {
            while(m_run) {
                byte[] data = null;
                synchronized(m_readBuff) {
                    if(!m_readBuff.empty()) {
                        data = m_readBuff.toByteArray();
                        m_readBuff.resize(0);
                    }
                }

                if(data != null)
                    sendDataRead(data);

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ShupitoConnection m_shupitoConn;
    private boolean m_holdsTabRef;
    private ShupitoDesc.config m_tunnelCfg;
    private Timer m_statusTimer;
    private StatusTimeoutTask m_statusTimeoutTask;
    private int m_tunnelPipe;
    private int m_tunnelSpeed;
    private ByteArray m_readBuff;
    private ReadDispatchThread m_readDispatchThread;
}
