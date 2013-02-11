package com.tassadar.lorrismobile.modules;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.tassadar.lorrismobile.BlobInputStream;
import com.tassadar.lorrismobile.BlobOutputStream;
import com.tassadar.lorrismobile.R;
import com.tassadar.lorrismobile.filemgr.FileManagerActivity;
import com.tassadar.lorrismobile.modules.HexFileCard.HexFileCardListener;
import com.tassadar.lorrismobile.modules.ProgrammerImpl.ProgrammerListener;


public class Programmer extends Tab implements OnClickListener, ProgrammerListener, HexFileCardListener {
    private static final int ACTCODE_OPEN_HEX = 1;

    public Programmer() {
        super();
        m_prog = new avr232boot(this);
        m_hex_card = new HexFileCard(this);
        m_chip_card = new ChipCard();
    }

    @Override
    public int getType() {
        return TabManager.TAB_PROGRAMMER;
    }

    @Override
    public String getName() {
        return "Programmer";
    }

    @Override
    public void onAttach(Activity act) {
        super.onAttach(act);
        SharedPreferences p = act.getPreferences(0);
        m_hex_card.setHexPath(p.getString("prog_hexFolder", null));
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.programmer, container, false);

        View b = v.findViewById(R.id.flash_btn);
        b.setOnClickListener(this);
        b.setEnabled(false);
        
        b = v.findViewById(R.id.stop_btn);
        b.setOnClickListener(this);
        b.setEnabled(false);

        loadCards(v);
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        m_hex_card.setEmpty();
        m_hex_card.setView(null);
        m_chip_card.setEmpty();
        m_chip_card.setView(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK)
            return;

        switch(requestCode) {
            case ACTCODE_OPEN_HEX:
            {
                m_hex_card.loadHexFile(data.getStringExtra("path"), data.getStringExtra("filename"));
                Activity act = getActivity();
                if(act != null) {
                    SharedPreferences.Editor e = act.getPreferences(0).edit();
                    e.putString("prog_hexFolder", m_hex_card.getHexPath());
                    e.commit();
                }
                break;
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(Build.VERSION.SDK_INT >= 13 && newConfig.smallestScreenWidthDp >= 600)
            loadCards(getView());
    }

    private void loadCards(View base) {
        ScrollView scr = (ScrollView)base.findViewById(R.id.prog_cards_view);
        scr.removeAllViews();

        View cards = View.inflate(getActivity(), R.layout.programmer_cards, null);
        scr.addView(cards);

        m_hex_card.setView(cards.findViewById(R.id.prog_card_hex));
        m_chip_card.setView(cards.findViewById(R.id.prog_card_chip));

        cards.findViewById(R.id.browse_hex).setOnClickListener(this);
    }

    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.browse_hex:
            {
                Intent i = new Intent(getActivity(), FileManagerActivity.class);
                i.putExtra("file_suffix", ".hex");
                i.putExtra("title", "Select HEX file");

                String f = m_hex_card.getHexPath();
                if(f != null)
                    i.putExtra("start_path", f);

                startActivityForResult(i, ACTCODE_OPEN_HEX);
                break;
            }
            case R.id.stop_btn:
                v.setEnabled(false);
                if(m_prog.isInFlashMode())
                    m_prog.switchToRunMode();
                else
                    m_prog.switchToFlashMode(0);
                break;
            case R.id.flash_btn:
                v.setEnabled(false);
                HexFile hex = m_hex_card.getHexFile();
                ChipDefinition def = m_chip_card.getDef();
                if(hex != null && def != null) {
                    setProgressBarVisible(true);
                    m_prog.flashRaw(hex, HexFile.MEM_FLASH, def);
                }
                break;
        }
    }

    @Override
    public void connected(boolean connected) {
        super.connected(connected);
        View v = getView();
        if(v == null)
            return;

        v.findViewById(R.id.stop_btn).setEnabled(connected);
        checkFlashButtonEnabled();
    }

    @Override 
    public void dataRead(byte[] data) {
        m_prog.dataRead(data);
    }

    @Override
    public void write(byte[] data) {
        if(m_conn != null)
            m_conn.write(data);
    }

    private void setStopButtonState(boolean stop) {
        View v = getView();
        if(v == null)
            return;

        TextView t = (TextView)v.findViewById(R.id.stop_text);
        t.setText(stop ? R.string.stop : R.string.start);
        t.setCompoundDrawablesWithIntrinsicBounds(stop ? R.drawable.stop : R.drawable.play, 0, 0, 0);
    }

    @Override
    public void switchToFlashComplete(boolean success) {
        Activity act = getActivity();
        if(act != null)
            act.runOnUiThread(new SwitchCompleteRunnable(true, success));
    }

    @Override
    public void switchToRunComplete(boolean success) {
        Activity act = getActivity();
        if(act != null)
            act.runOnUiThread(new SwitchCompleteRunnable(false, success));
    }
    
    private class SwitchCompleteRunnable implements Runnable {
        private boolean m_toFlash;
        private boolean m_success;

        public SwitchCompleteRunnable(boolean toFlash, boolean success) {
            m_toFlash = toFlash;
            m_success = success;
        }

        @Override
        public void run() {
            View v = getView();
            if(v != null)
                v.findViewById(R.id.stop_btn).setEnabled(m_conn != null && m_conn.isOpen());

            if(m_success) {
                if(m_toFlash)
                    m_prog.readDeviceId();
                setStopButtonState(!m_toFlash);
                checkFlashButtonEnabled();
            } else {
                Activity act = getActivity();
                if(act != null)
                    Toast.makeText(act, R.string.switch_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void chipDefRead(ChipDefinition def) {
        Activity act = getActivity();
        if(act != null)
            act.runOnUiThread(new ChipIdReadRunnable(def));
    }

    private class ChipIdReadRunnable implements Runnable {
        private ChipDefinition m_def;
        public ChipIdReadRunnable(ChipDefinition def) {
            m_def = def;
        }

        @Override
        public void run() {
            m_chip_card.setFull(m_def, m_hex_card.getHexFile());
            checkFlashButtonEnabled();
        }
    }

    @Override
    public void flashProgress(int pct) {
        View v = getView();
        if(v != null)
            v.post(new ProgressRunnable(pct));
    }

    @Override
    public void flashComplete(boolean success) {
        View v = getView();
        if(v != null)
            v.post(new ProgressRunnable(101));
    }

    @Override
    public void hexFileLoaded(HexFile f) {
        ChipDefinition def = m_chip_card.getDef();
        if(def != null)
            m_chip_card.setFull(def, f);

        checkFlashButtonEnabled();
    }

    private void checkFlashButtonEnabled() {
        View v = getView();
        if(v == null)
            return;

        ChipDefinition def = m_chip_card.getDef();

        View b = v.findViewById(R.id.flash_btn);
        b.setEnabled(
                m_conn != null && m_conn.isOpen() &&
                m_prog.isInFlashMode() && 
                def != null && def.getName() != null && def.getName().length() != 0 &&
                m_hex_card.getHexFile() != null
            );
    }

    public void setProgressBarVisible(boolean visible) {
        View v = getView();
        if(v == null)
            return;
        v = v.findViewById(R.id.flash_progress);
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private class ProgressRunnable implements Runnable {
        private int m_value;
        public ProgressRunnable(int value) {
            m_value = value;
        }

        @Override
        public void run() {
            View v = getView();
            if(v == null)
                return;
            ProgressBar p = (ProgressBar)v.findViewById(R.id.flash_progress);
            if(m_value > 100) {
                p.setVisibility(View.GONE);
                checkFlashButtonEnabled();
            }
            else
                p.setProgress(m_value);
        }
    }

    @Override
    protected void saveDataStream(BlobOutputStream str) {
        super.saveDataStream(str);

        m_hex_card.save(str);
    }

    @Override
    protected void loadDataStream(BlobInputStream str) {
        super.loadDataStream(str);

        m_hex_card.load(str);
    }

    private HexFileCard m_hex_card;
    private ChipCard m_chip_card;
    private ProgrammerImpl m_prog;
}