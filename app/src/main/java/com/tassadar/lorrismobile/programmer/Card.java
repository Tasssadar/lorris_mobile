package com.tassadar.lorrismobile.programmer;

import android.content.SharedPreferences;
import android.view.View;

import com.tassadar.lorrismobile.BlobInputStream;
import com.tassadar.lorrismobile.BlobOutputStream;

public abstract class Card {
    public static final int CARD_HEX    = 0;
    public static final int CARD_CHIP   = 1;
    public static final int CARD_AVR109 = 2;

    public static final int CARD_MAX    = 3;

    public abstract int getType();
    public abstract void setView(View v);

    public void setVisibility(boolean visible) {
        if(m_view == null)
            return;

        if(visible)
            m_view.setVisibility(View.VISIBLE);
        else
            m_view.setVisibility(View.GONE);
    }

    public void loadPrefs(SharedPreferences p) { }
    public void save(BlobOutputStream str) { }
    public void load(BlobInputStream str) { }

    protected View m_view;
}