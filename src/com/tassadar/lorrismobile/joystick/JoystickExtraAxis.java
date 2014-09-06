package com.tassadar.lorrismobile.joystick;

import android.view.View;
import android.widget.SeekBar;

import com.tassadar.lorrismobile.BlobInputStream;
import com.tassadar.lorrismobile.BlobOutputStream;

public class JoystickExtraAxis implements SeekBar.OnSeekBarChangeListener {
    public interface OnExtraAxisChangedListener {
        void onExtraAxisChanged(int id, int value);
    }

    public JoystickExtraAxis(int id, SeekBar bar, OnExtraAxisChangedListener listener) {
        m_id = id;
        m_listener = listener;
        m_value = 500;
        setBar(bar);
    }

    public void setValue(int val) {
        m_value = val;
        m_bar.setProgress(val);
    }

    public void setBar(SeekBar bar) {
        m_bar = bar;
        m_bar.setMax(1000);
        m_bar.setProgress(m_value);
        m_bar.setOnSeekBarChangeListener(this);
    }

    public int getValue() {
        return m_value;
    }

    public View getBar() {
        return m_bar;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        m_value = progress;
        m_listener.onExtraAxisChanged(m_id, progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void saveDataStream(BlobOutputStream str) {
        str.writeInt("extraAxis" + String.valueOf(m_id), m_value);
    }

    public void loadDataStream(BlobInputStream str) {
        setValue(str.readInt("extraAxis" + String.valueOf(m_id), m_value));
    }

    private int m_id;
    private int m_value;
    private SeekBar m_bar;
    private OnExtraAxisChangedListener m_listener;
}
