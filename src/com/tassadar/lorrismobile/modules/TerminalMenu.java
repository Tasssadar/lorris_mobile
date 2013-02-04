package com.tassadar.lorrismobile.modules;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.tassadar.lorrismobile.R;

public class TerminalMenu extends Fragment implements OnClickListener {
    public interface TerminalMenuListener {
        void onClearClicked();
        void onToggleKeyboardClicked();
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.terminal_menu, container, false);
        
        ImageButton b = (ImageButton)v.findViewById(R.id.clear_btn);
        b.setOnClickListener(this);
        b = (ImageButton)v.findViewById(R.id.toggle_keyboard);
        b.setOnClickListener(this);
        return v;
    }

    public void setListener(TerminalMenuListener listener) {
        m_listener = listener;
    }

    @Override
    public void onClick(View v) {
        if(m_listener == null)
            return;

        switch(v.getId()) {
            case R.id.clear_btn:
                m_listener.onClearClicked();
                break;
            case R.id.toggle_keyboard:
                m_listener.onToggleKeyboardClicked();
                break;
        }
    }

    private TerminalMenuListener m_listener;
}