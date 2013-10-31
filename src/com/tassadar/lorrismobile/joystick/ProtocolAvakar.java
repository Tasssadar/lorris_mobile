package com.tassadar.lorrismobile.joystick;

import com.tassadar.lorrismobile.connections.Connection;

public class ProtocolAvakar extends Protocol {

    private Object m_lock = new Object();
    private byte[] m_data = new byte[11];

    protected ProtocolAvakar(Connection conn) {
        super(conn);
        m_data[0] = (byte)0x80;
        m_data[1] = (byte)0x19;
    }

    @Override
    public void setAxes(int ax1, int ax2) {
        synchronized(m_lock) {
            m_data[2] = (byte)(ax2);
            m_data[3] = (byte)(ax2 >> 8);
            m_data[4] = (byte)(ax1);
            m_data[5] = (byte)(ax1 >> 8);
        }
    }

    @Override
    public void setAxis3(int ax3) {
        synchronized(m_lock) {
            //m_data[6] = (byte)(ax3);
            //m_data[7] = (byte)(ax3 >> 8);
            m_data[8] = (byte)(ax3);
            m_data[9] = (byte)(ax3 >> 8);
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
