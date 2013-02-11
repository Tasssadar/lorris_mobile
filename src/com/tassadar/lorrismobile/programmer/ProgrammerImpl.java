package com.tassadar.lorrismobile.programmer;


public abstract class ProgrammerImpl {
    public interface ProgrammerListener {
        public void write(byte[] data);
        public void switchToFlashComplete(boolean success);
        public void switchToRunComplete(boolean success);
        public void chipDefRead(ChipDefinition def);
        public void flashProgress(int pct);
        public void flashComplete(boolean success);
    }

    protected ProgrammerImpl(ProgrammerListener listener) {
        m_listener = listener;
    }

    public abstract void switchToFlashMode(int speed_hz);
    public abstract void switchToRunMode();
    public abstract boolean isInFlashMode();
    public abstract void dataRead(byte[] data);
    public abstract void readDeviceId();
    public abstract void flashRaw(HexFile hex, int memId, ChipDefinition chip);

    protected ProgrammerListener m_listener;
}