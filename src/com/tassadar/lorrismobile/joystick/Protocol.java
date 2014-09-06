package com.tassadar.lorrismobile.joystick;

import java.util.Map;
import java.util.TimerTask;

import com.tassadar.lorrismobile.connections.Connection;

public abstract class Protocol extends TimerTask {

    public static final int AVAKAR = 0;
    public static final int LEGO   = 1;
    public static final int CHESSBOT   = 2;

    public static String PROP_EXTRA_AXES = "extra_axes_cnt";
    public static String PROP_MAX_AXIS_VAL = "max_axis_val";

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

    public static int getMaxExtraAxes(int type) {
        switch(type) {
            case AVAKAR:   return 2;
            case LEGO:     return 8;
            case CHESSBOT: return 8;
            default:
                return 0;
        }
    }

    public static int getDefaultExtraAxes(int type) {
        switch(type) {
            case AVAKAR:   return 1;
            case LEGO:     return 1;
            case CHESSBOT: return 2;
            default:
                return 0;
        }
    }

    public static void initializeProperties(Map<String,Object> props) {
        ProtocolChessbot.initializeProperties(props);
    }

    protected Protocol(Connection conn) {
        super();
        m_conn = conn;
        m_extraAxesCount = Protocol.getDefaultExtraAxes(getType());
    }

    public void loadProperies(Map<String, Object> props) {
        if(props.containsKey(PROP_EXTRA_AXES)) {
            setExtraAxesCount((Integer)props.get(PROP_EXTRA_AXES));
        }
        if(props.containsKey(PROP_MAX_AXIS_VAL)) {
            m_maxAxisVal = (Integer)props.get(PROP_MAX_AXIS_VAL);
        }
    }

    public void setExtraAxesCount(int count) {
        m_extraAxesCount = count;
    }

    public abstract int getType();
    public abstract void setMainAxes(int ax1, int ax2);
    public void setExtraAxis(int id, int value) { }
    public abstract void setButtons(int buttons);
    public abstract void send();

    protected int m_extraAxesCount;
    protected int m_maxAxisVal;
    protected Connection m_conn;
}
