package com.tassadar.lorrismobile.joystick;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.tassadar.lorrismobile.BlobInputStream;
import com.tassadar.lorrismobile.BlobOutputStream;
import com.tassadar.lorrismobile.R;
import com.tassadar.lorrismobile.Utils;
import com.tassadar.lorrismobile.connections.Connection;
import com.tassadar.lorrismobile.joystick.JoystickMenu.JoystickMenuListener;
import com.tassadar.lorrismobile.modules.Tab;
import com.tassadar.lorrismobile.modules.TabManager;

public class Joystick extends Tab implements JoystickListener, OnCheckedChangeListener,
    OnClickListener, JoystickMenuListener, android.content.DialogInterface.OnClickListener {

    private static final int BUTTON_COUNT = 8;
    private static final int SEND_PERIOD = 200;

    public Joystick() {
        super();

        m_menu = new JoystickMenu();
        m_menu.setListener(this);
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
        if(m_sendTask != null)
            m_sendTask.setAxes(ax1, ax2);
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
            m_sendTask = new SendDataTask(m_conn);
            m_sendTimer.scheduleAtFixedRate(m_sendTask, SEND_PERIOD, SEND_PERIOD);
            m_sendTask.setAxis3(getAxis3(null).getProgress());
            m_sendTask.setButtons(m_btnMask);
        } else if(!connected && m_sendTask != null) {
            m_sendTask.cancel();
            m_sendTask = null;
        }
    }

    private class SendDataTask extends TimerTask {

        private Object m_lock = new Object();
        private byte[] m_data = new byte[11];
        private Connection m_conn;

        public SendDataTask(Connection conn) {
            super();
            m_conn = conn; 
            m_data[0] = (byte)0x80;
            m_data[1] = (byte)0x19;
        }

        public void setAxes(int ax1, int ax2) {
            synchronized(m_lock) {
                m_data[2] = (byte)(ax2);
                m_data[3] = (byte)(ax2 >> 8);
                m_data[4] = (byte)(ax1);
                m_data[5] = (byte)(ax1 >> 8);
            }
        }

        public void setAxis3(int ax3) {
            synchronized(m_lock) {
                //m_data[6] = (byte)(ax3);
                //m_data[7] = (byte)(ax3 >> 8);
                m_data[8] = (byte)(ax3);
                m_data[9] = (byte)(ax3 >> 8);
            }
        }

        public void setButtons(int buttons) {
            synchronized(m_lock) {
                m_data[10] = (byte)buttons;
            }
        }

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
    public void onMaxValueClicked() {
        JoystickView joy = (JoystickView)getView().findViewById(R.id.joystick_view);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.max_val));
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                m_maxValDialog = null;
            }
        });
        builder.setPositiveButton(R.string.ok, this);

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.joystick_max_val, (ViewGroup) getView(), false);
        TextView t = (TextView)layout.findViewById(R.id.max_val);
        t.setText(String.valueOf(joy.getMaxValue()));

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

        TextView t = (TextView)m_maxValDialog.findViewById(R.id.max_val);
        try {
            int val = Integer.parseInt(t.getText().toString());
            if(val <= 0)
                throw new NumberFormatException("max val is negative");
            joy.setMaxValue(val);
        } catch(NumberFormatException e) {
            e.printStackTrace();
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
    private SendDataTask m_sendTask;
    private JoystickMenu m_menu;
    private AlertDialog m_maxValDialog;
    private JoystickView m_joyView;
}
