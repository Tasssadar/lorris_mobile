package com.tassadar.lorrismobile.joystick;

import java.util.TimerTask;

import com.tassadar.lorrismobile.connections.Connection;

public abstract class Protocol extends TimerTask {

    public static final int AVAKAR = 0;
    public static final int LEGO   = 1;

    public static Protocol getProtocol(int type, Connection conn) {
        switch(type) {
            case AVAKAR: return new ProtocolAvakar(conn);
            case LEGO:   return new ProtocolLego(conn);
            default:
                return null;
        }
    }

    protected Protocol(Connection conn) {
        super();
        m_conn = conn;
    }

    public abstract void setAxes(int ax1, int ax2);
    public abstract void setAxis3(int ax3);
    public abstract void setButtons(int buttons);
    public abstract void send();

    protected Connection m_conn;

}