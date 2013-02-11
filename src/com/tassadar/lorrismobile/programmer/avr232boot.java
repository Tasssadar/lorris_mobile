package com.tassadar.lorrismobile.programmer;

import java.util.Timer;
import java.util.TimerTask;

import com.tassadar.lorrismobile.Utils;


public class avr232boot extends ProgrammerImpl {

    private static final byte[] STOP_SEQ = { 0x74, 0x7E, 0x7A, 0x33 };
    private static final byte[] START_SEQ = { 0x11 };
    private static final byte[] READ_ID_SEQ = { 0x12 };
    private static final byte[] PAGE_SEQ = { 0x10 };

    private static final int WAIT_FLASH_MODE    = 1;
    private static final int WAIT_ID            = 2;
    private static final int WAIT_FLASH         = 3;

    private static final byte ACK = 20;

    public avr232boot(ProgrammerListener listener) {
        super(listener);
        m_flash_mode = false;
        m_timer = new Timer();
    }

    @Override
    public void switchToFlashMode(int speed_hz) {
        if(m_flash_mode)
            return;

        m_listener.write(STOP_SEQ);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        m_listener.write(STOP_SEQ);
        startTimeout(WAIT_FLASH_MODE, 1000);
    }

    private void startTimeout(int type, int ms) {
        assert m_wait_type == 0 : "Can't start another wait task!";
        m_wait_type = type;
        m_cur_wait_task = new TimeoutTask();
        m_timer.schedule(m_cur_wait_task, ms);
    }

    @Override
    public void switchToRunMode() {
        m_listener.write(START_SEQ);
        m_flash_mode = false;
        m_listener.switchToRunComplete(true);
    }

    @Override
    public boolean isInFlashMode() {
        return m_flash_mode;
    }

    @Override
    public void readDeviceId() {
        if(!m_flash_mode)
            return;

        m_dev_id = new String();
        startTimeout(WAIT_ID, 1000);
        m_listener.write(READ_ID_SEQ);
    }

    @Override
    public void dataRead(byte[] data)
    {
        switch(m_wait_type) {
            case WAIT_FLASH_MODE:
            {
                if(!Utils.byteArrayContains(data, ACK))
                    return;

                stopTimeoutTask();

                m_flash_mode = true;
                m_listener.switchToFlashComplete(true);
                break;
            }
            case WAIT_ID:
            {
                m_dev_id += new String(data);
                if(m_dev_id.length() >= 4) {
                    stopTimeoutTask();

                    if(m_dev_id.length() > 4)
                        m_dev_id = m_dev_id.substring(0, 4);

                    m_listener.chipDefRead(new ChipDefinition("avr232boot:" + m_dev_id));
                }
                break;
            }
            case WAIT_FLASH:
            {
                if(!Utils.byteArrayContains(data, ACK))
                    return;

                stopTimeoutTask();

                if(!sendFlashPage(m_curMemId == HexFile.MEM_FLASH))
                {
                    m_listener.flashComplete(true);
                    m_hexFile.clearPages();
                    m_hexFile = null;
                } else {
                    startTimeout(WAIT_FLASH, 1000);
                }
                break;
            }
        }
    }

    @Override
    public void flashRaw(HexFile hex, int memId, ChipDefinition chip) {
        if(!hex.makePages(chip, memId)) {
            m_listener.flashComplete(false);
            return;
        }

        m_hexFile = hex;
        m_curMemId = memId;
        m_pagesCount = hex.getPagesCount(memId == HexFile.MEM_FLASH);
        m_pageItr = 0;

        if(!sendFlashPage(memId == HexFile.MEM_FLASH))
        {
            m_listener.flashComplete(false);
            return;
        }

        startTimeout(WAIT_FLASH, 1000);
    }

    private boolean sendFlashPage(boolean skip) {
        Page p = new Page();
        if(!m_hexFile.getNextPage(p, skip))
            return false;

        m_listener.write(PAGE_SEQ);

        byte[] data = new byte[2];
        data[0] = (byte)(p.address >> 8);
        data[1] = (byte)(p.address);
        m_listener.write(data);

        m_listener.write(p.data);

        ++m_pageItr;
        m_listener.flashProgress(m_pageItr*100/m_pagesCount);
        return true;
    }

    private void stopTimeoutTask() {
        m_wait_type = 0;
        if(m_cur_wait_task != null) {
            m_cur_wait_task.cancel();
            m_cur_wait_task = null;
        }
    }

    private class TimeoutTask extends TimerTask {
        @Override
        public void run() {
            int t = m_wait_type;
            m_cur_wait_task = null;
            m_wait_type = 0;
            switch(t) {
                case WAIT_FLASH_MODE:
                    m_listener.switchToFlashComplete(false);
                    break;
                case WAIT_ID:
                    m_listener.chipDefRead(new ChipDefinition(""));
                    break;
                case WAIT_FLASH:
                    m_listener.flashComplete(false);
                    break;
            }
        }
    }

    private boolean m_flash_mode;
    private int m_wait_type;
    private Timer m_timer;
    private TimerTask m_cur_wait_task;
    private String m_dev_id;
    private HexFile m_hexFile;
    private int m_curMemId;
    private int m_pageItr;
    private int m_pagesCount;
}