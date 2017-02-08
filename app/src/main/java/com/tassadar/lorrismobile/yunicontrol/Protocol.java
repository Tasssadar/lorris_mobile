package com.tassadar.lorrismobile.yunicontrol;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Message;

import com.tassadar.lorrismobile.connections.Connection;


public class Protocol {

    public interface ProtocolListener {
        public void onPacketReceived(Packet pkt);
        public void onInfoRequested();
        public void onInfoReceived(GlobalInfo i);
        public void onBoardChange(BoardInfo b);
    }

    private static final int TIMEOUT = 3000;

    private static final short DEVICE = 0x00;

    public static final int SMSG_GET_DEVICE_INFO      = 0xFF;
    public static final int CMSG_DEVICE_INFO          = 0xFE;
    public static final int SMSG_GET_DATA             = 0xFD;
    public static final int CMSG_POT                  = 0xFC;
    public static final int CMSG_BUTTONS              = 0xFB;
    public static final int CMSG_TRISTATE             = 0xFA;
    public static final int CMSG_BOARD_VOLTAGE        = 0x00;
    public static final int SMSG_GET_CALIBRATION_INFO = 0xF9;
    public static final int CMSG_CALIBRATION_INFO     = 0xF8;
    public static final int SMSG_START_CALIBRATION    = 0xF7;
    public static final int SMSG_FINISH_CALIBRATION   = 0xF6;

    private static final int EVENT_PACKET = 0;
    private static final int EVENT_INFO   = 1;

    public Protocol() {
        m_curPkt = new Packet();
        m_listeners = new ProtocolListener[0];
        m_eventHandler = new EventHandler(this);
        m_timer = new Timer();

        m_getDataDelay = 100;
        m_getDataMask = (0x01 | 0x02 | 0x04 | 0x08);
        m_enableGetData = true;
        m_lastBoard = new String();
    }

    public void connected(boolean connected) {
        if(connected)
            restartGetDataTask();
        else
            stopGetDataTask();
    }

    public void dataRead(byte[] data) {
        int offset = 0;
        while(offset < data.length)
        {
            offset += m_curPkt.addData(data, offset);
            if(m_curPkt.isValid()) {
                Packet pkt = new Packet();
                m_curPkt.swap(pkt);
                m_eventHandler.obtainMessage(EVENT_PACKET, pkt).sendToTarget();
            }
        }
    }

    static class EventHandler extends Handler {
        private final WeakReference<Protocol> m_protocol;

        public EventHandler(Protocol protocol) {
            m_protocol = new WeakReference<Protocol>(protocol);
        }

        @Override
        public void handleMessage(Message msg)
        {
             Protocol p = m_protocol.get();
             if (p == null)
                 return;

             switch(msg.what) {
                 case EVENT_PACKET:
                 {
                     Packet pkt = (Packet)msg.obj;
                     p.handlePacket(pkt);

                     if(pkt.device != p.m_curBoard)
                         break;

                     for(ProtocolListener l : p.m_listeners) {
                         pkt.resetRead();
                         l.onPacketReceived(pkt);
                     }
                     break;
                 }
                 case EVENT_INFO:
                     for(ProtocolListener l : p.m_listeners)
                         l.onInfoReceived((GlobalInfo)msg.obj);
                     break;
             }
        }
    }

    public void addListener(ProtocolListener l) {
        for(ProtocolListener li : m_listeners)
            if(li == l)
                return;

        ProtocolListener[] newArray = new ProtocolListener[m_listeners.length+1];
        System.arraycopy(m_listeners, 0, newArray, 0, m_listeners.length);
        newArray[m_listeners.length] = l;
        m_listeners = newArray;
    }

    public void removeListener(ProtocolListener l) {
        int pos = 0;
        for(ProtocolListener li : m_listeners) {
            if(li == l)
                break;
            ++pos;
        }

        if(pos >= m_listeners.length)
            return;

        if(pos != m_listeners.length-1) {
            ProtocolListener tmp = m_listeners[m_listeners.length-1];
            m_listeners[m_listeners.length-1] = m_listeners[pos];
            m_listeners[pos] = tmp;
        }

        ProtocolListener[] newArray = new ProtocolListener[m_listeners.length-1];
        System.arraycopy(m_listeners, 0, newArray, 0, newArray.length);
        m_listeners = newArray;
    }

    public void setConnection(Connection conn) {
        m_conn = conn;
    }

    public void sendPacket(Packet pkt) {
        if(m_conn == null)
            return;

        byte[] data = new byte[4+pkt.data.size()];
        data[0] = (byte)0xFF;
        data[1] = (byte)pkt.device;
        data[2] = (byte)(pkt.data.size()+1);
        data[3] = (byte)pkt.opcode;
        System.arraycopy(pkt.data.data(), 0, data, 4, pkt.data.size());
        m_conn.write(data);
    }

    public void requestGlobalInfo() {
        clearInfoTimeout();
        m_info = null;

        Packet pkt = new Packet(DEVICE, SMSG_GET_DEVICE_INFO);
        sendPacket(pkt);

        for(ProtocolListener l : m_listeners)
            l.onInfoRequested();

        startInfoTimeout();
    }

    private void startInfoTimeout() {
        m_infoTimeoutTask = new InfoTimeoutTask();
        m_timer.schedule(m_infoTimeoutTask, TIMEOUT);
    }

    private boolean clearInfoTimeout() {
        if(m_infoTimeoutTask == null)
            return false;

        boolean res = m_infoTimeoutTask.cancel();
        m_infoTimeoutTask = null;
        return res;
    }

    private class InfoTimeoutTask extends TimerTask {
        @Override
        public void run() {
            clearInfoTimeout();
            m_info = null;
            m_eventHandler.obtainMessage(EVENT_INFO, null).sendToTarget();
        }
    }

    private void handlePacket(Packet p) {
        switch(p.opcode) {
            case CMSG_DEVICE_INFO:
            {
                if(!clearInfoTimeout())
                    break;

                m_curBoard = 0;
                m_info = new GlobalInfo();
                m_info.name = p.readString();

                int cnt = p.read8();
                m_info.boards = new BoardInfo[cnt];
                for(int i = 0; i < cnt; ++i) {
                    m_info.boards[i] = new BoardInfo();
                    m_info.boards[i].name = p.readString();
                    m_info.boards[i].potCount = p.read8();
                    m_info.boards[i].btnCount = p.read8();
                    m_info.boards[i].triStateCount = p.read8();

                    if(m_info.boards[i].name.equals(m_lastBoard))
                        m_curBoard = i;
                }

                if(m_curBoard < cnt)
                    m_lastBoard = m_info.boards[m_curBoard].name;

                for(ProtocolListener l : m_listeners)
                    l.onInfoReceived(m_info);

                restartGetDataTask();
                break;
            }
        }
    }

    public GlobalInfo getInfo() {
        return m_info;
    }

    public BoardInfo getBoard(int idx) {
        if(m_info == null || idx >= m_info.boards.length)
            return null;
        return m_info.boards[idx];
    }

    public BoardInfo getCurBoard() {
        return getBoard(m_curBoard);
    }

    public void selectBoard(int board) {
        if (m_curBoard == board || m_info == null ||
            board < 0 || board >= m_info.boards.length)
            return;

        m_curBoard = board;

        m_lastBoard = m_info.boards[board].name;

        for(ProtocolListener l : m_listeners)
            l.onBoardChange(m_info.boards[board]);
    }

    private void stopGetDataTask() {
        if(m_getDataTask != null)
            m_getDataTask.cancel();
        m_getDataTask = null;
    }

    private void restartGetDataTask() {
        stopGetDataTask();

        if(!m_enableGetData)
            return;

        m_getDataTask = new GetDataTask();
        m_timer.schedule(m_getDataTask, m_getDataDelay, m_getDataDelay);
    }

    private class GetDataTask extends TimerTask {
        @Override
        public void run() {
            Packet pkt = new Packet(m_curBoard, SMSG_GET_DATA);
            pkt.write8(m_getDataMask);
            sendPacket(pkt);
        }
    }

    public void setGetDataDelay(int delay) {
        m_getDataDelay = delay;
        restartGetDataTask();
    }

    public void setGetDataEnabled(boolean enable) {
        m_enableGetData = enable;
        restartGetDataTask();
    }

    public void setGetDataMask(int mask) {
        m_getDataMask = mask;
    }

    public int getDataDelay() { return m_getDataDelay; }
    public int getDataMask() { return m_getDataMask; }
    public boolean getDataEnabled() { return m_enableGetData; }

    public void setLastBoard(String name) {
        m_lastBoard = name;
    }
    public String getLastBoard() {
        return m_lastBoard;
    }

    public int getCurBoardId() {
        return m_curBoard;
    }

    private Packet m_curPkt;
    private ProtocolListener[] m_listeners;
    private EventHandler m_eventHandler;
    private Connection m_conn;
    private Timer m_timer;
    private InfoTimeoutTask m_infoTimeoutTask;
    private GlobalInfo m_info;
    private int m_curBoard;
    private int m_getDataDelay;
    private int m_getDataMask;
    private boolean m_enableGetData;
    private GetDataTask m_getDataTask;
    private String m_lastBoard;
}
