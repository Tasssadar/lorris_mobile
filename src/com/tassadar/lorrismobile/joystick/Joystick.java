package com.tassadar.lorrismobile.joystick;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.tassadar.lorrismobile.BlobInputStream;
import com.tassadar.lorrismobile.BlobOutputStream;
import com.tassadar.lorrismobile.R;
import com.tassadar.lorrismobile.Utils;
import com.tassadar.lorrismobile.joystick.JoystickMenu.JoystickMenuListener;
import com.tassadar.lorrismobile.modules.Tab;
import com.tassadar.lorrismobile.modules.TabManager;

public class Joystick extends Tab implements JoystickListener, OnCheckedChangeListener,
    OnClickListener, JoystickMenuListener, android.content.DialogInterface.OnClickListener {

    public static final int BUTTON_COUNT = 8;
    private static final int SEND_PERIOD = 50;

    public Joystick() {
        super();

        m_menu = new JoystickMenu();
        m_menu.setListener(this);

        m_swapAxes = false;

        m_protocol = Protocol.AVAKAR;
        Protocol.initializeProperties(m_protocolProps);
    }

    @Override
    public int getType() {
        return TabManager.TAB_JOYSTICK;
    }

    @Override
    public String getName() {
        return "Joystick";
    }

    @Override
    public Fragment getMenuFragment() {
        return m_menu;
    }

    @Override
    public void onAttach(Activity act) {
        super.onAttach(act);
    }

    @Override
    public boolean enableSwipeGestures() {
        return false;
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.joystick_container, container, false);
        fillContainer(v);
        return v;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        JoystickView j = (JoystickView)getView().findViewById(R.id.joystick_view);

        int axis3 = getAxis3(null).getProgress();
        int maxVal = j.getMaxValue();

        fillContainer(getView());

        j = (JoystickView)getView().findViewById(R.id.joystick_view);
        j.setMaxValue(maxVal);
        getAxis3(null).setProgress(axis3);

        onLockChanged(m_menu.isLockSelected());
    }

    private void fillContainer(View v) {
        if(v == null)
            return;

        LinearLayout l = (LinearLayout)v.findViewById(R.id.joystick_container);
        l.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View joyLayout = inflater.inflate(R.layout.joystick, l, true);

        JoystickView j = (JoystickView)joyLayout.findViewById(R.id.joystick_view);
        j.setListener(this);
        m_joyView = j;

        SeekBar axis3 = (SeekBar)joyLayout.findViewById(R.id.axis3);
        axis3.setOnSeekBarChangeListener(j);

        addButtons(joyLayout);
    }

    private void addButtons(View v) {
        m_buttons.clear();

        LinearLayout l = (LinearLayout)v.findViewById(R.id.layout_buttons);

        boolean press = m_menu.isPressSelected();
        Context ctx = getActivity();
        for(int i = 0; i < BUTTON_COUNT; ++i) {
            ToggleButton b = new ToggleButton(ctx);
            b.setText(String.valueOf(i));
            b.setTextOn(b.getText());
            b.setTextOff(b.getText());
            b.setChecked((m_btnMask & (1 << i)) != 0);

            if(!press)
                b.setOnCheckedChangeListener(this);
            else
                b.setOnClickListener(this);

            l.addView(b);

            m_buttons.add(b);
        }
    }

    @Override
    public void onValueChanged(int ax1, int ax2) {
        if(m_sendTask != null) {
            if(m_swapAxes) {
                ax1 ^= ax2;
                ax2 ^= ax1;
                ax1 ^= ax2;
            }

            m_sendTask.setAxes(ax1, ax2);
        }
    }

    @Override
    public void onAxis3Changed(int ax3) {
        if(m_sendTask != null)
            m_sendTask.setAxis3(ax3);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        ToggleButton b = (ToggleButton)buttonView;
        int idx = m_buttons.indexOf(b);
        if(idx == -1)
            return;

        if(isChecked)
            m_btnMask |= (1 << idx);
        else
            m_btnMask &= ~(1 << idx);

        if(m_sendTask != null)
            m_sendTask.setButtons(m_btnMask);
    }

    @Override
    public void onClick(View v) {
        ToggleButton b = (ToggleButton)v;
        int idx = m_buttons.indexOf(b);
        if(idx == -1)
            return;

        b.setChecked(false);

        if(m_sendTask == null)
            return;

        m_sendTask.setButtons(m_btnMask | (1 << idx));
        m_sendTask.send();
        m_sendTask.setButtons(m_btnMask);

        Log.e("Lorris", "trigger btn " + idx + " " + m_btnMask);
    }

    @Override
    public void connected(boolean connected) {
        super.connected(connected);

        if(connected && m_sendTask == null) {
            createProtocol();
        } else if(!connected && m_sendTask != null) {
            m_sendTask.cancel();
            m_sendTask = null;
        }
    }

    private void createProtocol() {
        if(m_sendTask != null)
            m_sendTask.cancel();

        m_sendTask = Protocol.getProtocol(m_protocol, m_conn, m_protocolProps);
        m_sendTimer.scheduleAtFixedRate(m_sendTask, SEND_PERIOD, SEND_PERIOD);
        m_sendTask.setAxis3(getAxis3(null).getProgress());
        m_sendTask.setButtons(m_btnMask);
    }

    @Override
    public void onLockChanged(boolean locked) {
        SeekBar b = getAxis3(null);
        b.setEnabled(!locked);
        b.setClickable(!locked);

        Utils.lockScreenOrientation(getActivity(), locked);
    }

    @Override
    public void onBtnTypeClicked() {
        boolean press = m_menu.isPressSelected();

        for(int i = 0; i < BUTTON_COUNT; ++i) {
            ToggleButton b = m_buttons.get(i);
            b.setOnCheckedChangeListener(press ? null : this);
            b.setOnClickListener(press ? this : null);
        }

        m_btnMask = 0;
        if(m_sendTask != null)
            m_sendTask.setButtons(0);
    }

    @Override
    public void onProtocolClicked() {
        JoystickView joy = (JoystickView)getView().findViewById(R.id.joystick_view);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.protocol));
        builder.setCancelable(true);
        builder.setInverseBackgroundForced(true);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                m_maxValDialog = null;
            }
        });
        builder.setPositiveButton(R.string.ok, this);

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.protocol_dialog, (ViewGroup) getView(), false);
        TextView t = (TextView)layout.findViewById(R.id.max_val);
        t.setText(String.valueOf(joy.getMaxValue()));
        t = (TextView)layout.findViewById(R.id.device_id);
        t.setText("0x" + Integer.toHexString((Integer)m_protocolProps.get(ProtocolChessbot.PROP_DEVICE_ID)));

        CheckBox c = (CheckBox)layout.findViewById(R.id.swap_axes);
        c.setChecked(m_swapAxes);
        c = (CheckBox)layout.findViewById(R.id.invert_forward_backward);
        c.setChecked(m_joyView.isInvertedY());
        c = (CheckBox)layout.findViewById(R.id.invert_left_right);
        c.setChecked(m_joyView.isInvertedX());

        RadioButton b = (RadioButton)layout.findViewById(R.id.protocol_chessbot);
        b.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton btn, boolean checked) {
                ViewGroup layout = (ViewGroup)btn.getRootView();
                View v = layout.findViewById(R.id.device_id_title);
                v.setVisibility(checked ? View.VISIBLE : View.GONE);
                v = layout.findViewById(R.id.device_id);
                v.setVisibility(checked ? View.VISIBLE : View.GONE);
        }
        });

        switch(m_protocol)
        {
            case Protocol.AVAKAR:
                ((RadioButton)layout.findViewById(R.id.protocol_avakar)).setChecked(true);
                break;
            case Protocol.LEGO:
                ((RadioButton)layout.findViewById(R.id.protocol_lego)).setChecked(true);
                break;
            case Protocol.CHESSBOT:
                ((RadioButton)layout.findViewById(R.id.protocol_chessbot)).setChecked(true);
                break;
        }
        
        builder.setView(layout);
        m_maxValDialog = builder.create();
        m_maxValDialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        View v = getView();
        if(m_maxValDialog == null || v == null)
            return;
        
        JoystickView joy = (JoystickView)v.findViewById(R.id.joystick_view);

        try {
            TextView t = (TextView)m_maxValDialog.findViewById(R.id.max_val);
            int val = Integer.parseInt(t.getText().toString());
            if(val <= 0)
                throw new NumberFormatException("max val is negative");
            joy.setMaxValue(val);
        } catch(NumberFormatException e) {
            e.printStackTrace();
        }

        CheckBox c = (CheckBox)m_maxValDialog.findViewById(R.id.swap_axes);
        m_swapAxes = c.isChecked();
        c = (CheckBox)m_maxValDialog.findViewById(R.id.invert_forward_backward);
        m_joyView.setInvertY(c.isChecked());
        c = (CheckBox)m_maxValDialog.findViewById(R.id.invert_left_right);
        m_joyView.setInvertX(c.isChecked());

        int protocol;
        if(((RadioButton)m_maxValDialog.findViewById(R.id.protocol_avakar)).isChecked()) {
            protocol = Protocol.AVAKAR;
        } else if(((RadioButton)m_maxValDialog.findViewById(R.id.protocol_lego)).isChecked()) {
            protocol = Protocol.LEGO;
        } else {
            protocol = Protocol.CHESSBOT;

            try {
                TextView t = (TextView)m_maxValDialog.findViewById(R.id.device_id);
                Integer val = Integer.decode(t.getText().toString());
                m_protocolProps.put(ProtocolChessbot.PROP_DEVICE_ID, val);
            } catch(NumberFormatException e) {
                e.printStackTrace();
            }
        }

        if(m_protocol != protocol) {
            m_protocol = protocol;
            if(m_sendTask != null)
                createProtocol();
        } else {
            if(m_sendTask != null)
                m_sendTask.loadProperies(m_protocolProps);
        }

        m_maxValDialog.dismiss();
        m_maxValDialog = null;
    }

    @Override
    protected void saveDataStream(BlobOutputStream str) {
        super.saveDataStream(str);

        str.writeBool("pressBtns", m_menu.isPressSelected());
        str.writeBool("lockAxis3", m_menu.isLockSelected());

        str.writeInt("maxVal", m_joyView.getMaxValue());
        str.writeInt("axis3Val", m_joyView.getAxis3Value());

        str.writeInt("protocol", m_protocol);
        str.writeHashMap("protocolProps", m_protocolProps);

        str.writeBool("invertX", m_joyView.isInvertedX());
        str.writeBool("invertY", m_joyView.isInvertedY());
        str.writeBool("swapAxes", m_swapAxes);
    }

    @Override
    protected void loadDataStream(BlobInputStream str) {
        super.loadDataStream(str);

        m_menu.setPressSelected(str.readBool("pressBtns"));
        m_menu.setLockSelected(str.readBool("lockAxis3"));
        onBtnTypeClicked();
        onLockChanged(m_menu.isLockSelected());

        m_joyView.setMaxValue(str.readInt("maxVal", m_joyView.getMaxValue()));

        getAxis3(null).setProgress(str.readInt("axis3Val", 500));

        m_protocol = str.readInt("protocol", Protocol.AVAKAR);
        m_protocolProps.putAll(str.readHashMap("protocolProps"));
        if(m_sendTask != null)
            m_sendTask.loadProperies(m_protocolProps);

        m_joyView.setInvertX(str.readBool("invertX", false));
        m_joyView.setInvertY(str.readBool("invertY", false));
        m_swapAxes = str.readBool("swapAxes", m_swapAxes);
    }
    
    private SeekBar getAxis3(View v) {
        if(v == null)
            v = getView();
        if(v == null)
            return null;
        return (SeekBar)v.findViewById(R.id.axis3);
    }

    private ArrayList<ToggleButton> m_buttons = new ArrayList<ToggleButton>();
    private int m_btnMask;
    private Timer m_sendTimer = new Timer();
    private Protocol m_sendTask;
    private JoystickMenu m_menu;
    private AlertDialog m_maxValDialog;
    private JoystickView m_joyView;
    private int m_protocol;
    private Map<String, Object> m_protocolProps = new HashMap<String, Object>();
    private boolean m_swapAxes;
}
