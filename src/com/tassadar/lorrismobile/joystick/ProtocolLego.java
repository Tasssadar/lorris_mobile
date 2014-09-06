package com.tassadar.lorrismobile.joystick;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

import com.tassadar.lorrismobile.connections.Connection;

//  00  0x12  PayloadSize : Byte 0 and 1 contain the size (bytes) of the payload.
//  01  0x00                The size counting starts at byte 2. Here: 00 12 (hex) = 18 (dec)
//-------
//  02  0x01  SecretHeader: Byte 2..5 contain a header (just a guess)
//  03  0x00                The contents are unknown, but every message has these values here.
//  04  0x81
//  05  0x9E
//-------
//  06  0x05  TitleSize   : Byte 6 contains the size (bytes) of the Title field, which follows.
//-------
//  07  0x70  Title       : The title text string (ascii). Here: "ping"
//  08  0x69                The last character is always 0x00.
//  09  0x6E
//  10  0x67
//  11  0x00
//--------
//  12  0x06  ValueSize   : Byte (6 + TitleSize + 1) contains the size (bytes) of the Value field which follows.
//  13  0x00                Here: 00 06 (hex) = 6 (dec)
//--------
//  14  0x68  Value       : The value. Here the text: "hello"
//  15  0x65                The value field can contain:
//  16  0x6C                - Text (length: variable, ends with 0x00) ---> string
//  17  0x6C                - Number (length: 4 bytes)                ---> float (Single)
//  18  0x6F                - Logic (length: 1 byte)                  ---> bool
//  19  0x00

public class ProtocolLego extends Protocol {

    private final Object m_lock = new Object();
    private Integer[] m_axes = new Integer[10];
    private Integer m_buttons = 0;

    protected ProtocolLego(Connection conn) {
        super(conn);
    }

    @Override
    public int getType() {
        return Protocol.LEGO;
    }

    @Override
    public void setMainAxes(int ax1, int ax2) {
        synchronized(m_lock) {
            m_axes[0] = -ax2;
            m_axes[1] = ax1;
        }
    }

    @Override
    public void setExtraAxis(int id, int value) {
        synchronized(m_lock) {
            m_axes[id + 2] = value;
        }
    }

    @Override
    public void setButtons(int buttons) {
        synchronized(m_lock) {
            m_buttons = buttons;
        }
    }

    @Override
    public void send() {
        if(m_conn == null)
            return;

        synchronized(m_lock) {
            for(int i = 0; i < m_extraAxesCount + 2; ++i) {
                sendLegoMsg("a" + String.valueOf(i), m_axes[i]);
            }

            for(int i = 0; i < Joystick.BUTTON_COUNT; ++i) {
                sendLegoMsg("b" + String.valueOf(i), ((m_buttons & (1 << i)) != 0));
            }
        }
    }

    private void sendLegoMsg(String title, Object value) {
        int title_len = title.length();
        if(title_len > 254) {
            Log.e("Lorris", "ProtocolLego::sendLegoMsg: title can't be longer than 254 bytes: " + title);
            return;
        }

        int len = 4 + 1 + title_len + 1 + 2;
        short value_len = 0;
        if(value instanceof Integer || value instanceof Float)
            value_len = 4;
        else if(value instanceof String)
            value_len = (short)(((String)value).length() + 1);
        else if(value instanceof Boolean)
            value_len = 1;
        else {
            Log.e("Lorris", "ProtocolLego::sendLegoMsg: Unknown value type!");
            return;
        }

        len += value_len;
        if(len > 0xFFFF) {
            Log.e("Lorris", "ProtocolLego::sendLegoMsg: message is too long, " + len +" and maximum is 65536");
            return;
        }

        ByteBuffer data = ByteBuffer.allocate(len + 2);
        data.order(ByteOrder.LITTLE_ENDIAN);
        data.putShort((short)len);          // len
        data.put((byte)0x01);               // magic
        data.put((byte)0x00);
        data.put((byte)0x81);
        data.put((byte)0x9E);
        data.put((byte)(title_len + 1));    // title len
        data.put(title.getBytes());         // title
        data.put((byte)0);
        data.putShort(value_len);           // value len
        if(value instanceof Integer)        // value
            data.putFloat(((Integer)value).floatValue());
        else if(value instanceof Float)
            data.putFloat(((Float)value).floatValue());
        else if(value instanceof Boolean)
            data.put((byte) (((Boolean)value).booleanValue() ? 1 : 0));
        else if(value instanceof String) {
            data.put(((String)value).getBytes());
            data.put((byte)0);
        }

        m_conn.write(data.array());
    }

    @Override
    public void run() {
        send();
    }
}
