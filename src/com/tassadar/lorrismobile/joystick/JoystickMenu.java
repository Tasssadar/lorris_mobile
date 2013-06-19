package com.tassadar.lorrismobile.joystick;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.tassadar.lorrismobile.R;
import com.tassadar.lorrismobile.TooltipLongClickListener;

public class JoystickMenu extends Fragment implements OnClickListener {
    public interface JoystickMenuListener {
        void onBtnTypeClicked();
        void onMaxValueClicked();
        void onLockChanged(boolean lock);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.joystick_menu, container, false);
        
        View b;
        final int ids[] = {
            R.id.btn_type, R.id.max_val, R.id.lock_axis3
        };
        for(int id : ids) {
            b = v.findViewById(id);
            b.setOnClickListener(this);
            b.setOnLongClickListener(TooltipLongClickListener.get());
        }

        b = v.findViewById(R.id.btn_type);
        b.setSelected(m_pressSelected);
        b = v.findViewById(R.id.lock_axis3);
        b.setSelected(m_lockSelected);
        return v;
    }

    public void setListener(JoystickMenuListener listener) {
        m_listener = listener;
    }

    public void setPressSelected(boolean selected) {
        View v = getView();
        if(v != null) {
            View b = v.findViewById(R.id.btn_type);
            if(b != null)
                b.setSelected(selected);
        }
        m_pressSelected = selected;
    }

    public boolean isPressSelected() {
        return m_pressSelected;
    }

    public void setLockSelected(boolean selected) {
        View v = getView();
        if(v != null) {
            View b = v.findViewById(R.id.lock_axis3);
            if(b != null)
                b.setSelected(selected);
        }
        m_lockSelected = selected;
    }

    public boolean isLockSelected() {
        return m_lockSelected;
    }

    @Override
    public void onClick(View v) {
        if(m_listener == null)
            return;

        switch(v.getId()) {
            case R.id.btn_type:
                setPressSelected(!m_pressSelected);
                m_listener.onBtnTypeClicked();
                break;
            case R.id.max_val:
                m_listener.onMaxValueClicked();
                break;
            case R.id.lock_axis3:
                setLockSelected(!m_lockSelected);
                m_listener.onLockChanged(m_lockSelected);
                break;
        }
    }


    private JoystickMenuListener m_listener;
    private boolean m_pressSelected = false;
    private boolean m_lockSelected = false;
}