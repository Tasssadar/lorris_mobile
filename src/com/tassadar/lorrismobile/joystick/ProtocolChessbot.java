package com.tassadar.lorrismobile.joystick;

import com.tassadar.lorrismobile.connections.Connection;

import java.util.Map;

public class ProtocolChessbot extends Protocol {

    public static String PROP_DEVICE_ID = "chessbot_deviceId";

    private static final byte SMSG_BINARY_VALUES = 0x02;
    private static final byte SMSG_SET_MOTOR_POWER_16 = 0x04;
    private static final byte SMSG_SET_SERVOS_16 = 0x06;

    private static final byte DEFAULT_DEVICE_ID = 0x01;

    private final Object m_lock = new Object();
    private byte[] m_data = new byte[8];

    static public void initializeProperties(Map<String, Object> props) {
        props.put(PROP_DEVICE_ID, Integer.valueOf(DEFAULT_DEVICE_ID));
    }

    protected ProtocolChessbot(Connection conn) {
        super(conn);
        m_data[0] = (byte)0xFF;                 // start byte
        m_data[1] = (byte)DEFAULT_DEVICE_ID;    // device id
        m_data[2] = (byte)5;                    // len
        m_data[3] = SMSG_SET_MOTOR_POWER_16;    // cmd
    }

    @Override
    public int getType() {
        return Protocol.CHESSBOT;
    }

    @Override
    public void loadProperies(Map<String,Object> props) {
        super.loadProperies(props);

        if(props.containsKey(PROP_DEVICE_ID)) {
            synchronized(m_lock) {
                m_data[1] = ((Integer)props.get(PROP_DEVICE_ID)).byteValue();
            }
        }
    }

    @Override
    public void setMainAxes(int ax1, int ax2) {
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
    public void setExtraAxis(int id, int value) {
        if(m_conn == null)
            return;

        byte[] data = new byte[7];
        data[0] = (byte)0xFF;
        synchronized(m_lock) {
            data[1] = m_data[1]; // device id
        }
        data[2] = 4; // len
        data[3] = SMSG_SET_SERVOS_16;

        data[4] = (byte) id;
        data[5] = (byte)(value >> 8);
        data[6] = (byte)(value);

        m_conn.write(data);
    }

    @Override
    public void setButtons(int buttons) {
        if(m_conn == null)
            return;

        byte[] data = new byte[5 + (int)Math.ceil(((double)Joystick.BUTTON_COUNT)/8)];
        data[0] = (byte)0xFF;
        synchronized(m_lock) {
            data[1] = m_data[1]; // device id
        }
        data[2] = (byte)(data.length - 3); // len
        data[3] = SMSG_BINARY_VALUES;

        data[4] = (byte) Joystick.BUTTON_COUNT;
        int data_idx = 4;
        for(int i = 0; i < Joystick.BUTTON_COUNT; ++i) {
            final int byte_idx = i % 8;
            if(byte_idx == 0)
                ++data_idx;
            if((buttons & (1 << i)) != 0)
                data[data_idx] |= (1 << byte_idx);
        }

        m_conn.write(data);
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
