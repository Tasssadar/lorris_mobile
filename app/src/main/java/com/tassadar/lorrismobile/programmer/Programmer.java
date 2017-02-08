package com.tassadar.lorrismobile.programmer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.tassadar.lorrismobile.ByteArray;
import com.tassadar.lorrismobile.R;
import com.tassadar.lorrismobile.filemgr.FileManagerActivity;
import com.tassadar.lorrismobile.modules.Tab;
import com.tassadar.lorrismobile.modules.TabManager;
import com.tassadar.lorrismobile.programmer.HexFileCard.HexFileCardListener;
import com.tassadar.lorrismobile.programmer.ProgrammerImpl.ProgrammerListener;
import com.tassadar.lorrismobile.programmer.ProgrammerMenu.ProgrammerMenuListener;


public class Programmer extends Tab implements OnClickListener, ProgrammerListener, HexFileCardListener, ProgrammerMenuListener {
    private static final int ACTCODE_OPEN_HEX = 1;

    public Programmer() {
        super();

        m_cards = new Card[Card.CARD_MAX];
        m_cards[Card.CARD_HEX] = new HexFileCard(this);
        m_cards[Card.CARD_CHIP] = new ChipCard();
        m_cards[Card.CARD_AVR109] = new avr109Card();

        m_menu = new ProgrammerMenu();
        m_menu.setListener(this);

        onProgTypeChanged(ProgrammerImpl.PROG_AVR232BOOT);
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
    public Fragment getMenuFragment() {
        return m_menu;
    }

    @Override
    public void onAttach(Activity act) {
        super.onAttach(act);
        SharedPreferences p = act.getPreferences(0);

        for(Card c : m_cards)
            c.loadPrefs(p);
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

        for(Card c : m_cards)
            c.setView(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK)
            return;

        switch(requestCode) {
            case ACTCODE_OPEN_HEX:
            {
                HexFileCard h = (HexFileCard)m_cards[Card.CARD_HEX];
                h.loadHexFile(data.getStringExtra("path"), data.getStringExtra("filename"));
                Activity act = getActivity();
                if(act != null) {
                    SharedPreferences.Editor e = act.getPreferences(0).edit();
                    e.putString("prog_hexFolder", h.getHexPath());
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

        m_cards[Card.CARD_HEX].setView(cards.findViewById(R.id.prog_card_hex));
        m_cards[Card.CARD_CHIP].setView(cards.findViewById(R.id.prog_card_chip));
        m_cards[Card.CARD_AVR109].setView(cards.findViewById(R.id.prog_card_avr109));

        cards.findViewById(R.id.browse_hex).setOnClickListener(this);

        setCardVisibility();
    }

    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.browse_hex:
            {
                String title = getResources().getString(R.string.select_hex);
                Intent i = new Intent(getActivity(), FileManagerActivity.class);
                i.putExtra("file_suffix", ".hex");
                i.putExtra("title", title);

                String f = hexFileCard().getHexPath();
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
            {
                v.setEnabled(false);

                HexFile hex = hexFileCard().getHexFile();
                ChipDefinition def = chipCard().getDef();
                if(hex != null && def != null) {
                    setProgressBarVisible(true);
                    m_prog.flashRaw(hex, HexFile.MEM_FLASH, def);
                }
                break;
            }
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

    @Override
    public void write(ByteArray data) {
        if(m_conn != null)
            m_conn.write(data.data(), 0, data.size());
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
            chipCard().setFull(m_def, hexFileCard().getHexFile());
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
    public Card getCard(int card) {
        return m_cards[card];
    }

    @Override
    public void hexFileLoaded(HexFile f) {
        ChipDefinition def = chipCard().getDef();
        if(def != null)
            chipCard().setFull(def, f);

        checkFlashButtonEnabled();
    }

    private void checkFlashButtonEnabled() {
        View v = getView();
        if(v == null)
            return;

        ChipDefinition def = chipCard().getDef();

        View b = v.findViewById(R.id.flash_btn);
        b.setEnabled(
                m_conn != null && m_conn.isOpen() &&
                m_prog.isInFlashMode() && 
                def != null && def.getName() != null && def.getName().length() != 0 &&
                hexFileCard().getHexFile() != null
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
    public void onProgTypeChanged(int type) {
        if(m_prog != null && type == m_prog.getType())
            return;

        switch(type) {
            case ProgrammerImpl.PROG_AVR232BOOT:
                m_prog = new avr232boot(this);
                break;
            case ProgrammerImpl.PROG_AVR109:
                m_prog = new avr109(this);
                break;
        }

        setCardVisibility();

        m_menu.setActiveProg(type);
    }

    private void setCardVisibility() {
        int cards = m_prog.getReqCards();
        for(Card c : m_cards)
            c.setVisibility((cards & (1 << c.getType())) != 0);
    }

    @Override
    protected void saveDataStream(BlobOutputStream str) {
        super.saveDataStream(str);

        str.writeInt("progType", m_prog.getType());

        for(Card c : m_cards)
            c.save(str);
    }

    @Override
    protected void loadDataStream(BlobInputStream str) {
        super.loadDataStream(str);

        int type = str.readInt("progType", ProgrammerImpl.PROG_AVR232BOOT);
        onProgTypeChanged(type);

        for(Card c : m_cards)
            c.load(str);
    }

    private HexFileCard hexFileCard() {
        return ((HexFileCard)m_cards[Card.CARD_HEX]);
    }

    private ChipCard chipCard() {
        return ((ChipCard)m_cards[Card.CARD_CHIP]);
    }

    @SuppressWarnings("unused")
    private avr109Card avr109Card() {
        return ((avr109Card)m_cards[Card.CARD_AVR109]);
    }

    private Card m_cards[];
    private ProgrammerImpl m_prog;
    private ProgrammerMenu m_menu;
}
