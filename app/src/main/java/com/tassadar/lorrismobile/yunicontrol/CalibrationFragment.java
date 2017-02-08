package com.tassadar.lorrismobile.yunicontrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.tassadar.lorrismobile.R;
import com.tassadar.lorrismobile.yunicontrol.Protocol.ProtocolListener;


public class CalibrationFragment extends DialogFragment implements ProtocolListener, OnClickListener, OnItemClickListener {

    private static final int STATE_REQ     = 0;
    private static final int STATE_LIST    = 1;
    private static final int STATE_CONFIRM = 2;
    private static final int STATE_ERROR   = 3;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.joystick_calibration);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.yc_cal_dialog,  container, false);
        v.findViewById(R.id.confirm_btn).setOnClickListener(this);
        ((ListView)v.findViewById(R.id.device_list)).setOnItemClickListener(this);
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        setState(STATE_REQ);

        View v = getView();
        ((TextView)v.findViewById(R.id.cal_text)).setText(R.string.get_cal_info);

        Packet p = new Packet(m_protocol.getCurBoardId(), Protocol.SMSG_GET_CALIBRATION_INFO);
        m_protocol.sendPacket(p);

        m_timer = new Timer();
        m_timeoutTask = new CalInfoTimeout();
        m_timer.schedule(m_timeoutTask, 3000);
    }

    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        if(m_state == STATE_CONFIRM) {
            Packet p = new Packet(m_protocol.getCurBoardId(), Protocol.SMSG_FINISH_CALIBRATION);
            p.write8(0); // cancel
            m_protocol.sendPacket(p);
        }
    }

    public void onDismiss (DialogInterface dialog) {
        super.onDismiss(dialog);
        setProtocol(null);
    }

    private void setState(int state) {
        m_state = state;

        View v = getView();
        if(v == null)
            return;

        View m = v.findViewById(R.id.progress_bar);
        m.setVisibility(state == STATE_REQ ? View.VISIBLE : View.GONE);

        m = v.findViewById(R.id.cal_text);
        m.setVisibility(state == STATE_REQ || state == STATE_CONFIRM || state == STATE_ERROR ?
                View.VISIBLE : View.GONE);

        m = v.findViewById(R.id.device_list);
        m.setVisibility(state == STATE_LIST ? View.VISIBLE : View.GONE);

        m = v.findViewById(R.id.confirm_btn);
        m.setVisibility(state == STATE_CONFIRM ? View.VISIBLE : View.GONE);
    }

    public void setProtocol(Protocol p) {
        if(m_protocol != null)
            m_protocol.removeListener(this);

        m_protocol = p;

        if(p != null)
            p.addListener(this);
    }

    @Override
    public void onPacketReceived(Packet pkt) {
        switch(pkt.opcode) {
            case Protocol.CMSG_CALIBRATION_INFO:
            {
                if(!clearTimeoutTask() || m_state != STATE_REQ)
                    break;

                int cnt = pkt.read8();
                if(cnt <= 0) {
                    setState(STATE_ERROR);
                    View v = getView();
                    if(v != null)
                        ((TextView)v.findViewById(R.id.cal_text)).setText(R.string.no_cal_dev);
                    break;
                }

                m_devices = new CalDevice[cnt];
                for(int i = 0; i < cnt; ++i) {
                    m_devices[i] = new CalDevice();
                    m_devices[i].id = pkt.read8();
                    m_devices[i].name = pkt.readString();
                }

                if(m_devices.length > 1)
                    setupDevList();
                else
                    startCalibration(0);
                break;
            }
        }
    }

    @Override
    public void onInfoRequested() { }

    @Override
    public void onInfoReceived(GlobalInfo i) { }

    @Override
    public void onBoardChange(BoardInfo b) { }

    private void setupDevList() {
        View v = getView();
        if(v == null)
            return;

        setState(STATE_LIST);

        final String[] from = new String[] { "text" };
        final int[] to = new int[] { R.id.text };
        List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
        for(int i = 0; i < m_devices.length; i++){
            HashMap<String, String> map = new HashMap<String, String>();
            if(m_devices[i].name.length() > 0)
                map.put("text", "" + m_devices[i].id + ": " + m_devices[i].name);
            else
                map.put("text", "" + m_devices[i].id + ": " + getString(R.string.joystick));
            fillMaps.add(map);
        }

        ListView l = (ListView)v.findViewById(R.id.device_list);
        l.setAdapter(new SimpleAdapter(getActivity(), fillMaps, R.layout.yc_cal_device, from, to));
    }

    private void startCalibration(int idx) {
        View v = getView();
        if(v == null)
            return;

        m_cal_dev = idx;

        Packet pkt = new Packet(m_protocol.getCurBoardId(), Protocol.SMSG_START_CALIBRATION);
        pkt.write8(m_devices[m_cal_dev].id);
        m_protocol.sendPacket(pkt);

        setState(STATE_CONFIRM);
        ((TextView)v.findViewById(R.id.cal_text)).setText(R.string.calibrate);
    }

    @Override
    public void onClick(View v) {
        Packet p = new Packet(m_protocol.getCurBoardId(), Protocol.SMSG_FINISH_CALIBRATION);
        p.write8(1); // success
        m_protocol.sendPacket(p);
        dismiss();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
        startCalibration(pos);
    }

    private boolean clearTimeoutTask() {
        if(m_timer == null)
            return false;

        boolean res = m_timeoutTask.cancel();
        m_timeoutTask = null;
        m_timer = null;
        return res;
    }

    private class CalInfoTimeout extends TimerTask {
        @Override
        public void run() {
            clearTimeoutTask();

            View v = getView();
            if(v == null)
                return;
            v.post(new Runnable() {
                @Override
                public void run() {
                    View v = getView();
                    if(v == null)
                        return;

                    setState(STATE_ERROR);
                    ((TextView)v.findViewById(R.id.cal_text)).setText(R.string.cal_failed);
                }
            });
        }
    }

    private class CalDevice {
        public int id;
        public String name;
    }

    private Protocol m_protocol;
    private int m_state;
    private CalInfoTimeout m_timeoutTask;
    private Timer m_timer;
    private CalDevice[] m_devices;
    private int m_cal_dev;
}
