package com.tassadar.lorrismobile.modules;


import jackpal.androidterm.emulatorview.ByteQueue;
import jackpal.androidterm.emulatorview.EmulatorView;
import jackpal.androidterm.emulatorview.TermSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.tassadar.lorrismobile.BlobInputStream;
import com.tassadar.lorrismobile.BlobOutputStream;
import com.tassadar.lorrismobile.R;
import com.tassadar.lorrismobile.WorkspaceActivity;

public class Terminal extends Tab {

    public Terminal() {
        super();
        m_termSession = new TermSession();

        m_outStr = new TermInStream();

        m_termSession.setTermIn(m_outStr);
        m_termSession.setTermOut(new TermOutStream());
        m_data = new ByteArrayOutputStream();
    }

    @Override
    public int getType() {
        return TabManager.TAB_TERMINAL;
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.terminal, container, false);

        EmulatorView e = (EmulatorView)v.findViewById(R.id.term);
        e.attachSession(m_termSession);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        e.setDensity(metrics);

        Activity act = getActivity();
        if(act instanceof WorkspaceActivity)
            e.setExtGestureListener(((WorkspaceActivity)act).getGestureListener());

        e.setTextSize(16);
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
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.terminal, menu);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch(item.getItemId()) {
            case R.id.toggle_keyboard:
                toggleKeyboard();
                return true;
            case R.id.clear:
            {
                if(m_loadThread != null && m_loadThread.isAlive()) {
                    Toast.makeText(getActivity(), R.string.terminal_loading, Toast.LENGTH_SHORT).show();
                    return true;
                }

                try {
                    m_data.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                m_data = new ByteArrayOutputStream();
                m_termSession.finish();
                
                m_outStr = new TermInStream();

                m_termSession = new TermSession();
                m_termSession.setTermIn(m_outStr);
                m_termSession.setTermOut(new TermOutStream());

                EmulatorView e = (EmulatorView)getView().findViewById(R.id.term);
                e.attachSession(m_termSession);
                e.initialize();
                return true;
            }
                
        }
        return false;
    }

    @Override
    public void dataRead(byte[] data) {
        synchronized(m_outStr) {
        try {
            m_data.write(data);
            m_outStr.write(data);
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
            if(m_conn != null)
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

                    while(queue.getBytesAvailable() == 128)
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

    private void toggleKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
    }

    @Override
    protected void saveDataStream(BlobOutputStream str) {
        super.saveDataStream(str);

        str.writeByteArray("termData", m_data.toByteArray());
    }

    @Override
    protected void loadDataStream(BlobInputStream str) {
        super.loadDataStream(str);
        byte[] data = str.readByteArray("termData");

        if(data != null) {
            m_loadThread = new LoadTermDataThread(data);
            m_loadThread.setName("LoadTermDataThread");
            m_loadThread.start();
        }
    }

    private class LoadTermDataThread extends Thread {
        byte[] m_data;
        public LoadTermDataThread(byte[] data) {
            m_data = data;
        }

        @Override
        public void run() {
            Log.e("Lorris", "LoadTermDataThread started " + m_data.length);
            
            if(m_conn != null)
                m_conn.removeInterface(Terminal.this);

            dataRead(m_data);

            if(m_conn != null)
                m_conn.addInterface(Terminal.this);

            Log.e("Lorris", "LoadTermDataThread ended");
        }
    }

    private TermSession m_termSession;
    private TermInStream m_outStr;
    private ByteArrayOutputStream m_data;
    private LoadTermDataThread m_loadThread;
}
