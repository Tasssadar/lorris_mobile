package com.tassadar.lorrismobile.yunicontrol;

import android.support.v4.app.Fragment;

import com.tassadar.lorrismobile.yunicontrol.Protocol.ProtocolListener;


public abstract class YuniControlFragment extends Fragment implements ProtocolListener {

    public void setProtocol(Protocol p) {
        if(m_protocol != null)
            m_protocol.removeListener(this);

        m_protocol = p;

        if(m_protocol != null)
            m_protocol.addListener(this);
    }

    @Override
    abstract public void onPacketReceived(Packet pkt);

    protected Protocol m_protocol;
}
