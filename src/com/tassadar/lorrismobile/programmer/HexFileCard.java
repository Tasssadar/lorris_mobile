package com.tassadar.lorrismobile.programmer;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tassadar.lorrismobile.BlobInputStream;
import com.tassadar.lorrismobile.BlobOutputStream;
import com.tassadar.lorrismobile.LorrisApplication;
import com.tassadar.lorrismobile.R;


public class HexFileCard {
    public interface HexFileCardListener {
        void hexFileLoaded(HexFile f);
    }

    public static final int STATE_EMPTY     = 0;
    public static final int STATE_LOADING   = 1;
    public static final int STATE_FULL      = 2;
    public static final int STATE_INVALID   = 3;
    
    public static final int FULL_NAME       = 0;
    public static final int FULL_PATH       = 1;
    public static final int FULL_DATE       = 2;
    public static final int FULL_SIZE       = 3;
    
    public static final int FULL_MAX        = 4;

    private static final int[] FULL_TEXTS = {
        R.id.hex_name, R.id.path, R.id.last_mod, R.id.prog_size
    };

    public HexFileCard(HexFileCardListener listener) {
        m_state = STATE_EMPTY;
        m_listener = listener;
    }

    public void setView(View v) {
        CharSequence[] vals = null;
        if(m_view != null) {
            switch(m_state) {
                case STATE_FULL:
                {
                    TextView t;
                    vals = new CharSequence[FULL_MAX];
                    for(int i = 0; i < FULL_MAX; ++i) {
                        t = (TextView)m_view.findViewById(FULL_TEXTS[i]);
                        vals[i] = t.getText();
                    }
                    break;
                }
                case STATE_INVALID:
                    TextView t = (TextView)m_view.findViewById(R.id.invalid_hex_text);
                    vals = new CharSequence[1];
                    vals[0] = t.getText();
                    break;
            }
        }

        m_view = v;

        if(v != null) {
            initViewState();

            switch(m_state) {
                case STATE_FULL:
                {
                    TextView t;
                    for(int i = 0; i < FULL_MAX; ++i) {
                        t = (TextView)m_view.findViewById(FULL_TEXTS[i]);
                        t.setText(vals[i]);
                    }
                    break;
                }
                case STATE_INVALID:
                    TextView t = (TextView)m_view.findViewById(R.id.invalid_hex_text);
                    t.setText(vals[0]);
                    break;
            }
        }
    }

    public void setState(int state) {
        m_state = state;
        if(m_view != null)
            initViewState();
    }

    public void setEmpty() {
        setState(STATE_EMPTY);
        if(m_hexFile != null) {
            m_hexFile.destroy();
            m_hexFile = null;
        }
    }

    public void setFull(String[] texts) {
        TextView t;
        for(int i = 0; i < texts.length; ++i) {
            t = (TextView)m_view.findViewById(FULL_TEXTS[i]);
            t.setText(Html.fromHtml(texts[i]));
        }
        setState(STATE_FULL);
    }

    public void setInvalid(String error) {
        TextView t = (TextView)m_view.findViewById(R.id.invalid_hex_text);
        t.setText(error);
        setState(STATE_INVALID);
    }

    public boolean isFull() {
        return m_state == STATE_FULL;
    }

    private void initViewState() {
        View v;

        // STATE_EMPTY
        {
            v = m_view.findViewById(R.id.no_hex_text);
            setVisible(v, m_state == STATE_EMPTY);
        }

        // STATE_LOADING
        {
            v = m_view.findViewById(R.id.hex_progress);
            setVisible(v, m_state == STATE_LOADING);

            v = m_view.findViewById(R.id.browse_hex);
            v.setEnabled(m_state != STATE_LOADING);
        }

        // STATE_FULL
        {
            for(int i = 0; i <  FULL_TEXTS.length; ++i) {
                v = m_view.findViewById(FULL_TEXTS[i]);
                setVisible(v, m_state == STATE_FULL);
            }
        }

        // STATE_INVALID
        {
            v = m_view.findViewById(R.id.invalid_hex_text);
            setVisible(v, m_state == STATE_INVALID);
        }
    }

    private void setVisible(View v, boolean visible) {
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public HexFile getHexFile() {
        return m_hexFile;
    }

    public String getHexPath() {
        return m_hexFilePath;
    }

    public String getHexFilename() {
        return m_hexFilename;
    }

    public void setHexPath(String path) {
        m_hexFilePath = path;
    }

    public void loadHexFile(String path, String filename) {
        m_hexFilePath = path;
        m_hexFilename = filename;
        new HexLoadTask().execute(path, filename);
    }

    private class HexLoadTask extends AsyncTask<String, Void, String[]> {
        @Override
        protected void onPreExecute() {
            setState(STATE_LOADING);
        }

        @Override
        protected String[] doInBackground(String... arg) {
            File f = new File(arg[0], arg[1]);
            if(!f.exists() || !f.canRead()) {
                Log.e("Lorris", "Failed to open file " + arg[0] + "/" + arg[1]);
                String res = LorrisApplication.getAppContext().getString(R.string.invalid_hex);
                return new String[] { String.format(res, new String("")) };
            }

            if(m_hexFile == null)
                m_hexFile = new HexFile();

            String loadRes = m_hexFile.loadFile(f.getAbsolutePath());
            if(loadRes != null) {
                m_hexFile.destroy();
                m_hexFile = null;

                String res = LorrisApplication.getAppContext().getString(R.string.invalid_hex);
                return new String[] { String.format(res, loadRes) };
            }

            String[] res = new String[FULL_MAX];
            res[FULL_NAME] = arg[1];
            res[FULL_PATH] = arg[0];

            Context ctx = LorrisApplication.getAppContext();
            String base = ctx.getString(R.string.last_mod);
            SimpleDateFormat fmt = new SimpleDateFormat("H:mm:ss d.M.yyyy", Locale.getDefault());
            res[FULL_DATE] = String.format(base, fmt.format(new Date(f.lastModified())));

            base = ctx.getString(R.string.prog_size);
            long size = m_hexFile.getSize();
            String size_str = String.valueOf(size) + " B";
            if(size > 1024) {
                DecimalFormat dec = new DecimalFormat("0.##");
                size_str += " (" + dec.format(((double)size)/1024) + " kB)";
            }
                
            res[FULL_SIZE] = String.format(base, size_str);
            return res;
        }

        @Override
        protected void onPostExecute(String[] res) {
            if(m_view == null)
                return;

            if(res.length == 1)
                setInvalid(res[0]);
            else {
                setFull(res);
                m_listener.hexFileLoaded(m_hexFile);
            }
        }
    }

    public void save(BlobOutputStream str) {
        if(m_hexFilename == null || m_hexFilePath == null)
            return;
        str.writeString("hexFilename", m_hexFilename);
        str.writeString("hexFilePath", m_hexFilePath);
    }

    public void load(BlobInputStream str) {
        String filename = str.readString("hexFilename", null);
        String path = str.readString("hexFilePath", null);
        if(path != null && filename != null)
            loadHexFile(path, filename);
    }

    private int m_state;
    private View m_view;
    private HexFile m_hexFile;
    private String m_hexFilePath;
    private String m_hexFilename;
    private HexFileCardListener m_listener;
}