package com.tassadar.lorrismobile.programmer;

import java.text.DecimalFormat;

import android.content.res.Resources;
import android.text.Html;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tassadar.lorrismobile.LorrisApplication;
import com.tassadar.lorrismobile.R;


public class ChipCard extends Card{

    private static final int STATE_EMPTY    = 0x00;
    private static final int STATE_LOADING  = 0x01;
    private static final int STATE_FULL     = 0x02;
    private static final int STATE_USED_MEM = 0x04;

    //private static final int FULL_NAME    = 0;
    //private static final int FULL_SIGN    = 1;
    //private static final int FULL_FLASH   = 2;
    //private static final int FULL_EEPROM  = 3;
    //private static final int FULL_PAGE    = 4;
    private static final int FULL_MAX     = 5;
    
    private static final int USED_MEM      = FULL_MAX;
    private static final int USED_PROGRESS = FULL_MAX+1;
    private static final int FULL_USED_MAX = FULL_MAX+2;

    private static final int[] FULL_TEXTS = {
        R.id.chip_name, R.id.chip_signature, R.id.flash_size,
        R.id.eeprom_size, R.id.page_size
    };

    @Override
    public int getType() {
        return CARD_CHIP;
    }

    public ChipDefinition getDef() {
        return m_chipdef;
    }

    @Override
    public void setView(View v) {

        CharSequence[] stored = null;
        if(m_view != null && (m_state & STATE_FULL) != 0) {
            stored = new CharSequence[FULL_USED_MAX];
            TextView t;
            for(int i = 0; i < FULL_MAX; ++i) {
                t = (TextView)m_view.findViewById(FULL_TEXTS[i]);
                stored[i] = t.getText(); 
            }

            if((m_state & STATE_USED_MEM) != 0) {
                t = (TextView)m_view.findViewById(R.id.mem_usage);
                stored[USED_MEM] = t.getText();
                ProgressBar p = (ProgressBar)m_view.findViewById(R.id.chip_used_progress);
                stored[USED_PROGRESS] = String.valueOf(p.getProgress());
            }
        }

        m_view = v;

        if(m_view != null) {
            initViewState();
            
            if((m_state & STATE_FULL) != 0) {
                TextView t;
                for(int i = 0; i < FULL_MAX; ++i) {
                    t = (TextView)m_view.findViewById(FULL_TEXTS[i]);
                    t.setText(stored[i]); 
                }

                if((m_state & STATE_USED_MEM) != 0) {
                    t = (TextView)m_view.findViewById(R.id.mem_usage);
                    t.setText(stored[USED_MEM]);
                    ProgressBar p = (ProgressBar)m_view.findViewById(R.id.chip_used_progress);
                    p.setProgress(Integer.valueOf((String) stored[USED_PROGRESS]));
                }
            }
        }
    }

    private void initViewState() {
        View v;

        // STATE_EMPTY
        {
            v = m_view.findViewById(R.id.no_chip_text);
            setVisible(v, m_state == STATE_EMPTY);
        }

        // STATE_LOADING
        {
            v = m_view.findViewById(R.id.chip_progress);
            setVisible(v, m_state == STATE_LOADING);
        }

        // STATE_FULL
        {
            for(int id : FULL_TEXTS) {
                v = m_view.findViewById(id);
                setVisible(v,  (m_state & STATE_FULL));
            }
        }

        // STATE_USED_MEM
        {
            v = m_view.findViewById(R.id.mem_usage);
            setVisible(v, m_state & STATE_USED_MEM);
            v = m_view.findViewById(R.id.chip_used_progress);
            setVisible(v, m_state & STATE_USED_MEM);
        }
    }

    private void setVisible(View v, boolean visible) {
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setVisible(View v, int visible) {
        v.setVisibility(visible != 0 ? View.VISIBLE : View.GONE);
    }

    public void setState(int state) {
        m_state = state;
        if(m_view != null)
            initViewState();
    }

    public void setEmpty() {
        setState(STATE_EMPTY);
        if(m_chipdef != null) {
            m_chipdef.destroy();
            m_chipdef = null;
        }
    }

    public void setFull(ChipDefinition def, HexFile hex) {
        if(m_view == null)
            return;

        m_chipdef = def;

        int state = STATE_FULL;
        if(hex != null)
            state |= STATE_USED_MEM;

        setState(state);

        Resources r = LorrisApplication.getAppContext().getResources();
        String s = r.getString(R.string.chip_sign);
        setTextView(R.id.chip_signature, String.format(s, def.getSign()));

        s = def.getName();
        if(s == null || s.length() == 0) {
            setTextView(R.id.chip_name, R.string.unknown_chip);

            setTextView(R.id.flash_size, "");
            setTextView(R.id.eeprom_size, "");
            setTextView(R.id.page_size, "");
        } else {
            setTextView(R.id.chip_name, s);

            setSizeTextView(R.id.flash_size, r.getString(R.string.flash_mem),
                    def.getMemSize(HexFile.MEM_FLASH));
            setSizeTextView(R.id.eeprom_size, r.getString(R.string.eeprom),
                    def.getMemSize(HexFile.MEM_EEPROM));
            setSizeTextView(R.id.page_size, r.getString(R.string.page_size),
                    def.getMemPageSize(HexFile.MEM_FLASH));

            if(hex != null) {
                s = r.getString(R.string.used_mem);
                long prog = hex.getSize();
                int mem = def.getMemSize(HexFile.MEM_FLASH);
                s = String.format(s, prog, mem, (prog*100)/mem);
                setTextView(R.id.mem_usage, s);

                ProgressBar p = (ProgressBar)m_view.findViewById(R.id.chip_used_progress);
                p.setProgress((int) ((prog*100)/mem));
            }
        }
    }

    private void setTextView(int id, String text) {
        TextView t = (TextView)m_view.findViewById(id);
        t.setText(Html.fromHtml(text));
    }

    private void setTextView(int id, int textId) {
        TextView t = (TextView)m_view.findViewById(id);
        t.setText(textId);
    }

    private void setSizeTextView(int id, String base, int size) {
        TextView t = (TextView)m_view.findViewById(id);
        String size_str = String.valueOf(size) + " B";
        if(size > 1024) {
            DecimalFormat dec = new DecimalFormat("0.##");
            size_str += " (" + dec.format(((double)size)/1024) + " kB)";
        }
        t.setText(Html.fromHtml(String.format(base, size_str)));
    }

    private int m_state;
    private ChipDefinition m_chipdef;
}
