package com.tassadar.lorrismobile.programmer;

import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

import com.tassadar.lorrismobile.ByteArray;
import com.tassadar.lorrismobile.Utils;


public class avr109 extends ProgrammerImpl {

    private static final int WAIT_SUPPORTED     = 1;
    private static final int WAIT_ID            = 2;
    private static final int WAIT_HAS_BLOCK     = 3;
    private static final int WAIT_HAS_AUTO_INC  = 4;
    private static final int WAIT_ERASE_CHIP    = 5;
    private static final int WAIT_FLASH_BLOCK   = 7;

    private static final byte[] SUPPORTED_DEVS    = { 't' };
    private static final byte[] BOOTLOADER_EXIT   = { 'E' };
    private static final byte[] ID_REQ            = { 's' };
    private static final byte[] HAS_BLOCK         = { 'b' };
    private static final byte[] HAS_AUTO_INC      = { 'a' };
    private static final byte[] ERASE_CHIP        = { 'e' };
    private static final byte   WRITE_BLOCK       = 'B';
    private static final byte   BLK_TYPE_FLASH    = 'F';
    private static final byte   SET_ADDR          = 'A';
    private static final byte   SET_ADDR_BIG      = 'H';

    protected avr109(ProgrammerListener listener) {
        super(listener);
        m_flash_mode = false;
        m_timer = new Timer();
        m_rec_buff = new ByteArray();
    }

    @Override
    public int getType() {
        return PROG_AVR109;
    }

    @Override
    public int getReqCards() {
        return  (1 << Card.CARD_HEX) | (1 << Card.CARD_CHIP) |
                (1 << Card.CARD_AVR109);
    }

    @Override
    public void switchToFlashMode(int speed_hz) {
        avr109Card c = (avr109Card)m_listener.getCard(Card.CARD_AVR109);
        byte[] bootseq = c.getBootseq();

        if(bootseq.length == 0)
        {
            m_listener.switchToFlashComplete(false);
            return;
        }

        m_listener.write(bootseq);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        m_listener.write(SUPPORTED_DEVS);
        startTimeout(WAIT_SUPPORTED);
    }

    @Override
    public void switchToRunMode() {
        m_listener.write(BOOTLOADER_EXIT);
        m_flash_mode = false;
        m_listener.switchToRunComplete(true);
    }

    @Override
    public boolean isInFlashMode() {
        return m_flash_mode;
    }

    @Override
    public void dataRead(byte[] data) {
        switch(m_wait_type) {
            case WAIT_SUPPORTED:
            {
                if(!Utils.byteArrayContains(data, (byte)0))
                    return;

                stopTimeout();
                m_flash_mode = true;
                m_listener.switchToFlashComplete(true);
                break;
            }
            case WAIT_ID:
            {
                m_rec_buff.append(data);
                if(m_rec_buff.size() < 3)
                    return;

                stopTimeout();
                m_rec_buff.resize(3);
                m_dev_id = makeChipId(m_rec_buff.data());
                m_listener.chipDefRead(new ChipDefinition(m_dev_id));
                break;
            }
            case WAIT_HAS_BLOCK:
            {
                m_rec_buff.append(data);
                if(m_rec_buff.size() < 3)
                    return;

                stopTimeout();
                m_rec_buff.resize(3);
                if(m_rec_buff.at(0) == 'Y') {
                    m_blockSize = m_rec_buff.uAt(1) << 8;
                    m_blockSize |= m_rec_buff.uAt(2);
                    Log.i("Lorris", "avr109: this chip supports block operations, block size: " + m_blockSize);
                } else {
                    m_blockSize = -1;
                    Log.e("Lorris", "avr109: this chip does not support block operations");
                    m_listener.flashComplete(false);
                    m_hexFile.clearPages();
                    m_hexFile = null;
                    return;
                }

                checkBlockAutoInc();
                break;
            }
            case WAIT_HAS_AUTO_INC:
            {
                stopTimeout();
                m_hasAutoInc = data[0] == 'Y';
                Log.i("Lorris", "avr109: chip has auto increment support: " + m_hasAutoInc);

                eraseChip();
                break;
            }
            case WAIT_ERASE_CHIP:
            {
                stopTimeout();
                if(data[0] != '\r')
                {
                    Log.e("Lorris", "avr109: failed to erase chip");
                    m_listener.flashComplete(false);
                    m_hexFile.clearPages();
                    m_hexFile = null;
                    return;
                }

                if(!sendFlashBlock())
                {
                    m_listener.flashComplete(false);
                    Log.e("Lorris", "avr109: flash failed, no first page!");
                }
                break;
            }
            case WAIT_FLASH_BLOCK:
            {
                m_rec_buff.append(data);
                if(m_rec_buff.size() < 2)
                    return;

                stopTimeout();
                m_rec_buff.resize(2);
                if(m_rec_buff.at(0) != '\r' || m_rec_buff.at(1) != '\r') {
                    Log.e("Lorris", "avr109: failed to flash block (wrong response)");
                    m_listener.flashComplete(false);
                    m_hexFile.clearPages();
                    m_hexFile = null;
                    return;
                }

                if(!sendFlashBlock())
                    m_listener.flashComplete(true);
                break;
            }
        }
    }

    @Override
    public void readDeviceId() {
        m_listener.write(ID_REQ);
        startTimeout(WAIT_ID);
    }

    @Override
    public void flashRaw(HexFile hex, int memId, ChipDefinition chip) {
        assert memId == HexFile.MEM_FLASH : "avr109 supports only MEM_FLASH!";

        if(!hex.makePages(chip, memId)) {
            m_listener.flashComplete(false);
            return;
        }

        m_hexFile = hex;
        m_curMemId = memId;
        m_pagesCount = hex.getPagesCount(memId == HexFile.MEM_FLASH);
        m_pageItr = 0;

        checkBlock();
    }

    private boolean sendFlashBlock() {
        Page p = new Page();
        if(!m_hexFile.getNextPage(p, true))
        {
            m_hexFile.clearPages();
            m_hexFile = null;
            return false;
        }

        sendSetAddress(p.address >> 1);

        byte[] cmd = new byte[4];
        cmd[0] = WRITE_BLOCK;
        cmd[1] = (byte)(p.data.length >> 8);
        cmd[2] = (byte)p.data.length;
        cmd[3] = BLK_TYPE_FLASH;
        m_listener.write(cmd);
        m_listener.write(p.data);

        ++m_pageItr;
        m_listener.flashProgress(m_pageItr*100/m_pagesCount);
        startTimeout(WAIT_FLASH_BLOCK, 1000);
        return true;
    }

    private void sendSetAddress(int address) {
        ByteArray cmd = new ByteArray();
        if(address < 0x10000)
        {
            cmd.append(SET_ADDR);
            cmd.append((address >> 8) & 0xFF);
            cmd.append(address & 0xFF);
        }
        else
        {
            cmd.append(SET_ADDR_BIG);
            cmd.append((address >> 16) & 0xFF);
            cmd.append((address >> 8) & 0xFF);
            cmd.append(address & 0xFF);
        }
        m_listener.write(cmd);
    }

    private void checkBlock() {
        m_rec_buff.clear();
        m_listener.write(HAS_BLOCK);
        startTimeout(WAIT_HAS_BLOCK);
    }

    private void checkBlockAutoInc() {
        m_listener.write(HAS_AUTO_INC);
        startTimeout(WAIT_HAS_AUTO_INC);
    }

    private void eraseChip() {
        m_listener.write(ERASE_CHIP);
        startTimeout(WAIT_ERASE_CHIP);
    }

    private void startTimeout(int type, int ms) {
        assert m_wait_type == 0 : "Can't start another wait task!";
        m_wait_type = type;
        m_rec_buff.clear();
        m_cur_wait_task = new TimeoutTask();
        m_timer.schedule(m_cur_wait_task, ms);
    }

    private void startTimeout(int type) {
        startTimeout(type, 1000); // dammit, java
    }

    private void stopTimeout() {
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
                case WAIT_SUPPORTED:
                    m_listener.switchToFlashComplete(false);
                    break;
                case WAIT_ID:
                    m_listener.chipDefRead(new ChipDefinition(""));
                    break;
                case WAIT_HAS_BLOCK:
                case WAIT_HAS_AUTO_INC:
                case WAIT_ERASE_CHIP:
                case WAIT_FLASH_BLOCK:
                    m_listener.flashComplete(false);
                    m_hexFile.clearPages();
                    m_hexFile = null;
                    break;
            }
        }
    }

    private native String makeChipId(byte[] dataArray);
    static { System.loadLibrary("functions"); }

    private boolean m_flash_mode;
    private int m_wait_type;
    private Timer m_timer;
    private TimerTask m_cur_wait_task;
    private ByteArray m_rec_buff;
    private String m_dev_id;
    private HexFile m_hexFile;
    private int m_curMemId;
    private int m_blockSize;
    private boolean m_hasAutoInc;
    private int m_pageItr;
    private int m_pagesCount;
}
