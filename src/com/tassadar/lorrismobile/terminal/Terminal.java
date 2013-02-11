package com.tassadar.lorrismobile.terminal;


import jackpal.androidterm.emulatorview.ByteQueue;
import jackpal.androidterm.emulatorview.EmulatorView;
import jackpal.androidterm.emulatorview.TermSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.tassadar.lorrismobile.BlobInputStream;
import com.tassadar.lorrismobile.BlobOutputStream;
import com.tassadar.lorrismobile.R;
import com.tassadar.lorrismobile.modules.Tab;
import com.tassadar.lorrismobile.modules.TabManager;
import com.tassadar.lorrismobile.terminal.TerminalMenu.TerminalMenuListener;
import com.tassadar.lorrismobile.terminal.TerminalSettingsDialog.TerminalSettingsListener;

public class Terminal extends Tab implements TerminalMenuListener, TerminalSettingsListener {

    private static final int HEX_LINE = 16;

    public Terminal() {
        super();
        m_loadThread = new WeakReference<LoadTermDataThread>(null);

        m_termSession = new TermSession();
        m_outStr = new TermInStream();

        m_termSession.setTermIn(m_outStr);
        m_termSession.setTermOut(new TermOutStream());
        m_data = new ByteArrayOutputStream();

        m_menu = new TerminalMenu();
        m_menu.setListener(this);

        m_settings = new TerminalSettings();
    }

    @Override
    public int getType() {
        return TabManager.TAB_TERMINAL;
    }

    @Override
    public String getName() {
        return "Terminal";
    }

    public void onAttach(Activity act) {
        super.onAttach(act);

        SharedPreferences p = act.getPreferences(0);
        m_settings.load(p);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.terminal, container, false);

        EmulatorView e = (EmulatorView)v.findViewById(R.id.term);
        e.attachSession(m_termSession);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        e.setDensity(metrics);

        e.setTextSize(m_settings.fontSize);
        e.setColorScheme(m_settings.getColorScheme());
        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        m_termSession.finish();

        try {
            m_data.close();
            m_outStr.close();
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dataRead(byte[] data) {
        synchronized(m_readStrLock) {
            try {
                m_data.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            readToTerm(data);
        }
    }

    private void readToTerm(byte[] data) {
        synchronized(m_outStr) {
            try {
                if(m_settings.hexMode) {
                    // FIXME: This will copy the data byte array, which can be
                    // big. This is not ideal.
                    if(m_lastHexLine != null) {
                        final byte[] cr = { '\r' };
                        m_outStr.write(cr);

                        byte [] tmp = new byte[data.length + m_lastHexLine.length];
                        System.arraycopy(m_lastHexLine, 0, tmp, 0, m_lastHexLine.length);
                        System.arraycopy(data, 0, tmp, m_lastHexLine.length, data.length);
                        data = tmp;
                        m_lastHexLine = null;
                    } else {
                        final byte[] nl = { '\n' };
                        m_outStr.write(nl);
                    }

                    byte[] res = null;
                    if(m_settings.hex16bytes)
                        res = convertToHex16(data, m_hexPos);
                    else
                        res = convertToHex8(data, m_hexPos);

                    m_hexPos += data.length;
                    m_outStr.write(res);

                    int l = m_hexPos%(m_settings.hex16bytes ? 16 : 8);
                    if(l != 0) {
                        m_lastHexLine = new byte[l];
                        System.arraycopy(data, data.length-l, m_lastHexLine, 0, l);
                        m_hexPos -= l;
                    }
                } else {
                    m_outStr.write(data);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class TermOutStream extends OutputStream {
        @Override
        public void write(int oneByte) throws IOException {
            // not used
        }

        @Override
        public void write (byte[] buffer, int offset, int count) throws IOException {
            if(m_conn == null)
                return;

            if(buffer[offset] == 0x0D) // Enter key
                m_conn.write(m_settings.getEnterKeyPressSeq());
            else
                m_conn.write(buffer, offset, count);
        }
    }

    private class TermInStream extends InputStream {
        private ByteQueue m_queue;
        private Object m_queueLock;

        public TermInStream() {
            m_queue = new ByteQueue(4096);
            m_queueLock = new Object();
        }

        @Override
        public void close() {
            synchronized(m_queueLock) {
                if(m_queue != null)
                    m_queue.close();
                m_queue = null;
            }
        }

        public void write (byte[] buffer) throws IOException {
            write(buffer, 0, buffer.length);
        }

        public void write (byte[] buffer, int offset, int count) throws IOException {
            ByteQueue queue = null;
            synchronized(m_queueLock) {
                queue = m_queue;
            }

            if(queue == null)
                return;

            try {
                while (count > 0) {
                    int written = queue.write(buffer, offset, count);
                    offset += written;
                    count -= written;

                    while(queue.getBytesAvailable() == 4096)
                        Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int available() {
            synchronized(m_queueLock) {
                return m_queue.getBytesAvailable();
            }
        }

        @Override
        public int read() throws IOException {
            return -1;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            ByteQueue queue = null;
            synchronized(m_queueLock) {
                queue = m_queue;
            }

            if(queue == null)
                return -1;

            int len = -1;
            try {
                len = queue.read(buffer, offset, length);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return len;
        }
    }
    

    @Override
    public void connected(boolean connected) {
        //if(connected)
            //m_conn.write(new byte[] { 0x74, 0x7E, 0x7A, 0x33 });
    }

    public Fragment getMenuFragment() {
        return m_menu;
    }

    @Override
    public void onClearClicked() {
        LoadTermDataThread t = m_loadThread.get();
        if(t != null && t.isAlive()){
            Toast.makeText(getActivity(), R.string.terminal_loading, Toast.LENGTH_SHORT).show();
            return;
        }
        synchronized(m_readStrLock) {
            clearTerminal(true);
        }
    }

    private void clearTerminal(boolean clearData) {
        if(clearData) {
            try {
                m_data.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            m_data = new ByteArrayOutputStream();
        }

        m_termSession.finish();

        m_outStr = new TermInStream();
        m_hexPos = 0;
        m_lastHexLine = null;

        m_termSession = new TermSession();
        m_termSession.setTermIn(m_outStr);
        m_termSession.setTermOut(new TermOutStream());

        EmulatorView e = (EmulatorView)getView().findViewById(R.id.term);
        e.attachSession(m_termSession);
        e.initialize();
    }

    @Override
    public void onToggleKeyboardClicked() {
        toggleKeyboard();
    }

    @Override
    public void onShowSettingsClicked() {
        FragmentActivity a = (FragmentActivity)getActivity();
        FragmentManager mgr = a.getSupportFragmentManager();

        TerminalSettingsDialog s = new TerminalSettingsDialog();
        s.setSettings(m_settings);
        s.setListener(this);
        s.show(mgr, "TermSettings");
    }

    @Override
    public void onSettingsChanged() {
        EmulatorView e = (EmulatorView)getView().findViewById(R.id.term);
        e.setTextSize(m_settings.fontSize);
        e.setColorScheme(m_settings.getColorScheme());

        m_menu.setHexSelected(m_settings.hexMode);
    }

    @Override
    public void onHexModeClicked() {
        LoadTermDataThread t = m_loadThread.get();
        if(t != null && t.isAlive()){
            Toast.makeText(getActivity(), R.string.terminal_loading, Toast.LENGTH_SHORT).show();
            return;
        }

        synchronized(m_readStrLock) {
            clearTerminal(m_settings.clearOnHex);

            m_settings.hexMode = !m_settings.hexMode;
            m_menu.setHexSelected(m_settings.hexMode);
    
            if(!m_settings.clearOnHex) {
                t = new LoadTermDataThread(m_data.toByteArray(), false);
                m_loadThread = new WeakReference<LoadTermDataThread>(t);
                t.setName("LoadTermDataThread");
                t.start();
            }
        }
    }

    private void toggleKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        EmulatorView e = (EmulatorView)getView().findViewById(R.id.term);
        e.requestFocus();

        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
    }

    @Override
    protected void saveDataStream(BlobOutputStream str) {
        super.saveDataStream(str);

        m_settings.saveToStr(str);

        str.writeByteArray("termData", m_data.toByteArray());
    }

    @Override
    protected void loadDataStream(BlobInputStream str) {
        super.loadDataStream(str);

        m_settings.loadFromStr(str);
        onSettingsChanged();

        byte[] data = str.readByteArray("termData");

        if(data != null) {
            LoadTermDataThread t = new LoadTermDataThread(data, true);
            m_loadThread = new WeakReference<LoadTermDataThread>(t);
            t.setName("LoadTermDataThread");
            t.start();
        }
    }

    private void setLoadBarVisiblity(boolean visible) {
        View v = getView().findViewById(R.id.load_bar);
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private class ShowLoadBarRunnable implements Runnable {
        public volatile boolean execute = true;
        @Override
        public void run() {
            if(execute)
                setLoadBarVisiblity(true);
        }
    }

    private class LoadTermDataThread extends Thread {
        byte[] m_data;
        boolean m_putIntoData;
        public LoadTermDataThread(byte[] data, boolean putIntoData) {
            m_data = data;
            m_putIntoData = putIntoData;
        }

        @Override
        public void run() {
            Log.e("Lorris", "LoadTermDataThread started " + m_data.length);

            if(m_conn != null)
                m_conn.removeInterface(Terminal.this);

            EmulatorView e = (EmulatorView)getView().findViewById(R.id.term);
            e.setUpdateEnable(false);

            ShowLoadBarRunnable r = new ShowLoadBarRunnable();
            e.postDelayed(r,  100);

            if(m_putIntoData)
                dataRead(m_data);
            else
                readToTerm(m_data);

            m_data = null;

            e.setUpdateEnable(true);
            e.postInvalidate();

            r.execute = false;
            e.post(new Runnable() {
                @Override
                public void run() {
                    setLoadBarVisiblity(false);
                }
            });

            if(m_conn != null)
                m_conn.addInterface(Terminal.this);

            Log.e("Lorris", "LoadTermDataThread ended");
        }
    }

    private native byte[] convertToHex16(byte[] dataArray, int hexPos);
    private native byte[] convertToHex8(byte[] dataArray, int hexPos);
    static {
        System.loadLibrary("functions");
    }

    private TermSession m_termSession;
    private TermInStream m_outStr;
    private ByteArrayOutputStream m_data;
    private WeakReference<LoadTermDataThread> m_loadThread;
    private TerminalMenu m_menu;
    private TerminalSettings m_settings;
    private byte[] m_lastHexLine;
    private int m_hexPos;
    private Object m_readStrLock = new Object();
}
