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
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
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
    OnClickListener, JoystickMenuListener, DialogInterface.OnClickListener, JoystickExtraAxis.OnExtraAxisChangedListener {

    public static final int BUTTON_COUNT = 8;
    private static final int SEND_PERIOD = 50;

    public Joystick() {
        super();

        m_menu = new JoystickMenu();
        m_menu.setListener(this);

        m_swapAxes = false;

        m_protocol = Protocol.AVAKAR;
        Protocol.initializeProperties(m_protocolProps);
        m_protocolProps.put(Protocol.PROP_MAX_AXIS_VAL, 32767);
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
    protected boolean keepScreenOn() {
        return m_conn != null && m_conn.isOpen();
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
    public void setActive(boolean active) {
        super.setActive(active);

        // SurfaceView doesn't work properly in Fragment, .hide won't really hide it
        // because it is a separate "window". TextureView fixes this, but is API >= 14.
        // This isn't optimal, but actually works.
        if(m_joyView != null)
            m_joyView.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.joystick_container, container, false);
        fillContainer(v);
        setExtraAxesCount(v, Protocol.getDefaultExtraAxes(m_protocol));
        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(m_sendTask != null) {
            m_sendTask.cancel();
            m_sendTask = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        BlobOutputStream out = new BlobOutputStream();
        JoystickView j = (JoystickView)getView().findViewById(R.id.joystick_view);
        j.saveDataStream(out);

        fillContainer(getView());

        BlobInputStream in = new BlobInputStream(out.toByteArray());
        j = (JoystickView)getView().findViewById(R.id.joystick_view);
        j.loadDataStream(in);

        onLockChanged(m_menu.isLockSelected());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_MENU) {
            onProtocolClicked();
            return true;
        }
        return false;
    }

    private void fillContainer(View v) {
        if(v == null)
            return;

        LinearLayout l = (LinearLayout)v.findViewById(R.id.joystick_container);
        l.removeAllViews();

        LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View joyLayout = inflater.inflate(R.layout.joystick, l, true);

        JoystickView j = (JoystickView)joyLayout.findViewById(R.id.joystick_view);
        j.setListener(this);
        m_joyView = j;

        for(JoystickExtraAxis a : m_extraAxes) {
            a.setBar(addExtraAxisWidget(joyLayout));
        }

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

            m_sendTask.setMainAxes(ax1, ax2);
        }
    }

    @Override
    public void onExtraAxisChanged(int id, int value) {
        if(m_sendTask != null) {
            m_sendTask.setExtraAxis(id, ((value-500)*m_joyView.getMaxValue()*2)/1000);
        }
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
        for(int i = 0; i < m_extraAxes.size(); ++i)
            onExtraAxisChanged(i, m_extraAxes.get(i).getValue());
        m_sendTask.setButtons(m_btnMask);
    }

    @Override
    public void onLockChanged(boolean locked) {
        for(JoystickExtraAxis a : m_extraAxes) {
            a.getBar().setEnabled(!locked);
            a.getBar().setClickable(!locked);
        }

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

    private static void updateDialogExtraAxesCnt(int protocol, EditText extraAxesEdit) {
        int val = Protocol.getDefaultExtraAxes(protocol);
        try {
            val = Integer.valueOf(extraAxesEdit.getText().toString());
        } catch(NumberFormatException e) {
            // Ignore
        }

        if(val < 0)
            val = 0;
        else if(val > Protocol.getMaxExtraAxes(protocol))
            val = Protocol.getMaxExtraAxes(protocol);
        extraAxesEdit.setText(String.valueOf(val));
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
        t = (TextView)layout.findViewById(R.id.extra_axes);
        t.setText(String.valueOf(m_extraAxes.size()));

        CheckBox c = (CheckBox)layout.findViewById(R.id.swap_axes);
        c.setChecked(m_swapAxes);
        c = (CheckBox)layout.findViewById(R.id.invert_forward_backward);
        c.setChecked(m_joyView.isInvertedY());
        c = (CheckBox)layout.findViewById(R.id.invert_left_right);
        c.setChecked(m_joyView.isInvertedX());

        RadioButton b = (RadioButton)layout.findViewById(R.id.protocol_avakar);
        b.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton btn, boolean checked) {
                if (checked) {
                    ViewGroup layout = (ViewGroup) btn.getRootView();
                    updateDialogExtraAxesCnt(Protocol.AVAKAR, (EditText) layout.findViewById(R.id.extra_axes));
                }
            }
        });

        b = (RadioButton)layout.findViewById(R.id.protocol_chessbot);
        b.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton btn, boolean checked) {
                ViewGroup layout = (ViewGroup)btn.getRootView();
                View v = layout.findViewById(R.id.device_id_title);
                v.setVisibility(checked ? View.VISIBLE : View.GONE);
                v = layout.findViewById(R.id.device_id);
                v.setVisibility(checked ? View.VISIBLE : View.GONE);

                if(checked)
                    updateDialogExtraAxesCnt(Protocol.CHESSBOT, (EditText)layout.findViewById(R.id.extra_axes));
        }
        });

        b = (RadioButton)layout.findViewById(R.id.protocol_lego);
        b.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton btn, boolean checked) {
                if (checked) {
                    ViewGroup layout = (ViewGroup) btn.getRootView();
                    updateDialogExtraAxesCnt(Protocol.LEGO, (EditText) layout.findViewById(R.id.extra_axes));
                }
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
            m_protocolProps.put(Protocol.PROP_MAX_AXIS_VAL, val);
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

        EditText t = (EditText)m_maxValDialog.findViewById(R.id.extra_axes);
        int extra_axes = Protocol.getDefaultExtraAxes(protocol);
        try {
            extra_axes = Integer.valueOf(t.getText().toString());
            if(extra_axes < 0)
                extra_axes = 0;
            else if(extra_axes > Protocol.getMaxExtraAxes(protocol))
                extra_axes = Protocol.getMaxExtraAxes(protocol);
        } catch(NumberFormatException e) {
            // Ignore
        }

        m_protocolProps.put(Protocol.PROP_EXTRA_AXES, extra_axes);

        if(m_protocol != protocol) {
            m_protocol = protocol;
            if(m_sendTask != null)
                createProtocol();
        } else {
            if(m_sendTask != null)
                m_sendTask.loadProperies(m_protocolProps);
        }

        setExtraAxesCount(null, extra_axes);

        m_maxValDialog.dismiss();
        m_maxValDialog = null;
    }

    @Override
    protected void saveDataStream(BlobOutputStream str) {
        super.saveDataStream(str);

        str.writeBool("pressBtns", m_menu.isPressSelected());
        str.writeBool("lockAxis3", m_menu.isLockSelected());

        str.writeInt("protocol", m_protocol);
        str.writeHashMap("protocolProps", m_protocolProps);

        str.writeBool("swapAxes", m_swapAxes);

        m_joyView.saveDataStream(str);

        str.writeInt("extraAxesCnt", m_extraAxes.size());
        for(JoystickExtraAxis a : m_extraAxes)
            a.saveDataStream(str);
    }

    @Override
    protected void loadDataStream(BlobInputStream str) {
        super.loadDataStream(str);

        m_menu.setPressSelected(str.readBool("pressBtns"));
        m_menu.setLockSelected(str.readBool("lockAxis3"));
        onBtnTypeClicked();
        onLockChanged(m_menu.isLockSelected());

        m_protocol = str.readInt("protocol", Protocol.AVAKAR);
        m_protocolProps.putAll(str.readHashMap("protocolProps"));
        if(m_sendTask != null)
            m_sendTask.loadProperies(m_protocolProps);

        m_swapAxes = str.readBool("swapAxes", m_swapAxes);

        m_joyView.loadDataStream(str);

        int extraAxesCnt = str.readInt("extraAxesCnt", -1);
        if(extraAxesCnt != -1) {
            setExtraAxesCount(null, extraAxesCnt);
            for(JoystickExtraAxis a : m_extraAxes)
                a.loadDataStream(str);
        }
    }

    private SeekBar addExtraAxisWidget(View v) {
        SeekBar bar = new SeekBar(v.getContext());

        LinearLayout l = (LinearLayout)v.findViewById(R.id.extra_axes_layout);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        l.addView(bar, p);

        bar.setEnabled(!m_menu.isLockSelected());
        bar.setClickable(!m_menu.isLockSelected());

        return bar;
    }

    private void addExtraAxis(View v) {
        if(v == null)
            v = getView();
        if(v == null)
            return;

        SeekBar bar = addExtraAxisWidget(v);
        m_extraAxes.add(new JoystickExtraAxis(m_extraAxes.size(), bar, this));
    }

    private void rmExtraAxis(View v) {
        if(v == null)
            v = getView();
        if(v == null)
            return;

        int idx = m_extraAxes.size()-1;
        JoystickExtraAxis ax = m_extraAxes.get(idx);
        m_extraAxes.remove(idx);

        LinearLayout l = (LinearLayout)v.findViewById(R.id.extra_axes_layout);
        l.removeView(ax.getBar());
    }

    private void setExtraAxesCount(View v, int count) {
        int current = m_extraAxes.size();
        if(v == null)
            v = getView();
        if(v == null)
            return;

        while(current != count) {
            if(current > count) {
                rmExtraAxis(v);
                --current;
            } else {
                addExtraAxis(v);
                ++current;
            }
        }
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
    private ArrayList<JoystickExtraAxis> m_extraAxes = new ArrayList<JoystickExtraAxis>();
}
