package com.tassadar.lorrismobile.joystick;

import com.tassadar.lorrismobile.connections.Connection;

public class ProtocolAvakar extends Protocol {

    private final Object m_lock = new Object();
    private byte[] m_data = new byte[11];

    protected ProtocolAvakar(Connection conn) {
        super(conn);
        m_data[0] = (byte)0x80;
        m_data[1] = (byte)0x19;
    }

    @Override
    public int getType() {
        return Protocol.AVAKAR;
    }

    @Override
    public void setMainAxes(int ax1, int ax2) {
        synchronized(m_lock) {
            m_data[4] = (byte)ax1;
            m_data[5] = (byte)(ax1 >> 8);
            m_data[2] = (byte)ax2;
            m_data[3] = (byte)(ax2 >> 8);
        }
    }

    @Override
    public void setExtraAxis(int id, int value) {
        // Convert to 0-1000 range
        value = (value+m_maxAxisVal)*500/m_maxAxisVal;

        synchronized(m_lock) {
            int idx = id == 0 ? 8 : 6;
            m_data[idx++] = (byte) (value);
            m_data[idx] = (byte) (value >> 8);
        }
    }

    @Override
    public void setButtons(int buttons) {
        synchronized(m_lock) {
            m_data[10] = (byte)buttons;
        }
    }

    @Override
    public void send() {
        if(m_conn == null)
            return;

        synchronized(m_lock) {
            m_conn.write(m_data);
        }
    }

    @Override
    public void run() {
        send();
    }
}
