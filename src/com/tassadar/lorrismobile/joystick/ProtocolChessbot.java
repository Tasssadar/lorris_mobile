package com.tassadar.lorrismobile.joystick;

import com.tassadar.lorrismobile.connections.Connection;

public class ProtocolChessbot extends Protocol {

    private static final byte SMSG_SET_MOTOR_POWER_16 = 4;

    private Object m_lock = new Object();
    private byte[] m_data = new byte[8];


    protected ProtocolChessbot(Connection conn) {
        super(conn);
        m_data[0] = (byte)0xFF;                 // start byte
        m_data[1] = (byte)0x01;                 // device id
        m_data[2] = (byte)5;                    // len
        m_data[3] = SMSG_SET_MOTOR_POWER_16;    // cmd
    }

    @Override
    public void setAxes(int ax1, int ax2) {
        final int r = ((ax1 - (ax2/3)));
        final int l = ((ax1 + (ax2/3))); 

        synchronized(m_lock) {
            m_data[4] = (byte)(r >> 8);
            m_data[5] = (byte)(r);
            m_data[6] = (byte)(l >> 8);
            m_data[7] = (byte)(l);
        }
    }

    @Override
    public void setAxis3(int ax3) {
        
    }

    @Override
    public void setButtons(int buttons) {
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
