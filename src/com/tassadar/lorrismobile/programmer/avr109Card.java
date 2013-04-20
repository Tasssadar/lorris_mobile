package com.tassadar.lorrismobile.programmer;

import android.view.View;
import android.widget.EditText;

import com.tassadar.lorrismobile.BlobInputStream;
import com.tassadar.lorrismobile.BlobOutputStream;
import com.tassadar.lorrismobile.ByteArray;
import com.tassadar.lorrismobile.R;


public class avr109Card extends Card {

    private static final String DEF_BOOTSEQ = "0x74 0x7E 0x7A 0x33";

    public avr109Card() {
        m_bootseq = DEF_BOOTSEQ;
    }

    @Override
    public int getType() {
        return CARD_AVR109;
    }

    @Override
    public void setView(View v) {

        m_view = v;

        if(v != null) {
            EditText t = (EditText)v.findViewById(R.id.bootseq);
            t.setText(m_bootseq);
        }
    }

    public byte[] getBootseq() {
        if(m_view == null)
            return new byte[0];

        ByteArray res = new ByteArray();
        EditText t = (EditText)m_view.findViewById(R.id.bootseq);
        String[] tokens = t.getText().toString().split(" ");
        for(String tok : tokens) {
            if(tok.length() == 0)
                continue;

            try {
                int n = Integer.decode(tok);
                if((n & 0xFF) <= 255)
                    res.append(n);
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                continue;
            }
        }
        return res.toByteArray();
    }

    @Override
    public void save(BlobOutputStream str) {
        str.writeString("avr109bootseq", m_bootseq);
    }

    @Override
    public void load(BlobInputStream str) {
        m_bootseq = str.readString("avr109bootseq", DEF_BOOTSEQ);
    }

    private String m_bootseq;
}
