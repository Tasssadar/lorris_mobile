package com.tassadar.lorrismobile.joystick;


public interface JoystickListener {
    public void onValueChanged(int ax1, int ax2);
    public void onAxis3Changed(int ax3);
}