package com.tassadar.lorrismobile.programmer;

import com.tassadar.lorrismobile.ByteArray;


public abstract class ProgrammerImpl {
    public interface ProgrammerListener {
        public void write(byte[] data);
        public void write(ByteArray data);
        public void switchToFlashComplete(boolean success);
        public void switchToRunComplete(boolean success);
        public void chipDefRead(ChipDefinition def);
        public void flashProgress(int pct);
        public void flashComplete(boolean success);
        public Card getCard(int card);
    }

    public static final int PROG_AVR232BOOT = 0;
    public static final int PROG_AVR109     = 1;

    private static final String[] m_names = {
        "avr232boot", // 0
        "avr109",     // 1
    };

    public static int progNameToType(String name) {
        for(int i = 0; i < m_names.length; ++i)
            if(m_names[i].equals(name))
                return i;

        return -1;
    }

    public static String progTypeToName(int type) {
        if(type < 0 || type >= m_names.length)
            return null;
        return m_names[type];
    }

    protected ProgrammerImpl(ProgrammerListener listener) {
        m_listener = listener;
    }

    public abstract int getType();

    public abstract void switchToFlashMode(int speed_hz);
    public abstract void switchToRunMode();
    public abstract boolean isInFlashMode();
    public abstract void dataRead(byte[] data);
    public abstract void readDeviceId();
    public abstract void flashRaw(HexFile hex, int memId, ChipDefinition chip);
    public abstract int getReqCards();

    protected ProgrammerListener m_listener;
}
