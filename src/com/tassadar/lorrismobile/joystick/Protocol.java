package com.tassadar.lorrismobile.joystick;

import java.util.Map;
import java.util.TimerTask;

import com.tassadar.lorrismobile.connections.Connection;

public abstract class Protocol extends TimerTask {

    public static final int AVAKAR = 0;
    public static final int LEGO   = 1;
    public static final int CHESSBOT   = 2;

    public static Protocol getProtocol(int type, Connection conn, Map<String, Object> props) {
        Protocol res;
        switch(type) {
            case AVAKAR:   res = new ProtocolAvakar(conn); break;
            case LEGO:     res = new ProtocolLego(conn); break;
            case CHESSBOT: res = new ProtocolChessbot(conn); break;
            default:
                return null;
        }

        res.loadProperies(props);
        return res;
    }

    public static void initializeProperties(Map<String,Object> props) {
        ProtocolChessbot.initializeProperties(props);
    }

    protected Protocol(Connection conn) {
        super();
        m_conn = conn;
    }

    public void loadProperies(Map<String, Object> props) { }

    public abstract void setAxes(int ax1, int ax2);
    public abstract void setAxis3(int ax3);
    public abstract void setButtons(int buttons);
    public abstract void send();

    protected Connection m_conn;
}
