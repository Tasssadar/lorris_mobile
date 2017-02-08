package com.tassadar.lorrismobile.terminal;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.tassadar.lorrismobile.R;

public class TerminalSettingsDialog extends DialogFragment implements OnClickListener {
    public interface TerminalSettingsListener {
        void onSettingsChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.terminal_settings, container, false);
        
        Button b = (Button)v.findViewById(R.id.save_btn);
        b.setOnClickListener(this);

        EditText font = (EditText)v.findViewById(R.id.font_size);
        Spinner clr = (Spinner)v.findViewById(R.id.colors);
        Spinner enter = (Spinner)v.findViewById(R.id.enter_key_press);
        CheckBox clrOnHex = (CheckBox)v.findViewById(R.id.clear_on_hex);
        RadioGroup hexBytes = (RadioGroup)v.findViewById(R.id.hex_bytes);

        font.setText(Integer.toString(m_settings.fontSize));
        clr.setSelection(m_settings.colors);
        enter.setSelection(m_settings.enterKeyPress);
        clrOnHex.setChecked(m_settings.clearOnHex);
        hexBytes.check(m_settings.hex16bytes ? R.id.hex16bytes : R.id.hex8bytes);
        return v;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstance) {
        Dialog d = super.onCreateDialog(savedInstance);
        d.setTitle(R.string.term_settings);
        return d;
    }

    public void setSettings(TerminalSettings s) {
        m_settings = s;
    }

    public void setListener(TerminalSettingsListener listener) {
        m_listener = listener;
    }

    @Override
    public void onClick(View btn) {
        View v = getView();
        if(v == null)
            return;

        EditText font = (EditText)v.findViewById(R.id.font_size);
        Spinner clr = (Spinner)v.findViewById(R.id.colors);
        Spinner enter = (Spinner)v.findViewById(R.id.enter_key_press);
        CheckBox clrOnHex = (CheckBox)v.findViewById(R.id.clear_on_hex);
        RadioGroup hexBytes = (RadioGroup)v.findViewById(R.id.hex_bytes);

        try {
            m_settings.fontSize = Integer.valueOf(font.getText().toString());
        } catch(NumberFormatException e) { 
            // Ignore
        }

        m_settings.colors = clr.getSelectedItemPosition();
        m_settings.enterKeyPress = enter.getSelectedItemPosition();
        m_settings.clearOnHex = clrOnHex.isChecked();
        m_settings.hex16bytes = (hexBytes.getCheckedRadioButtonId() == R.id.hex16bytes);

        Activity a = getActivity();
        if(a != null)
            m_settings.save(a.getPreferences(0));

        if(m_listener != null)
            m_listener.onSettingsChanged();

        dismiss();
    }

    private TerminalSettings m_settings;
    private TerminalSettingsListener m_listener;
}