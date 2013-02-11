package com.tassadar.lorrismobile.filemgr;

import java.io.File;
import java.io.FileFilter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.tassadar.lorrismobile.R;
import com.tassadar.lorrismobile.filemgr.FileListItem.FileItemClicked;

public class FileManagerActivity extends Activity implements FileItemClicked
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT  < 14)
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.file_manager);

        if(Build.VERSION.SDK_INT >= 14)
            setUpActionBar();

        m_scroll_pos = new HashMap<String, Integer>();
        m_base_path = Environment.getExternalStorageDirectory().toString();

        String startPath = m_base_path;

        Intent i = getIntent();
        if(i != null) {
            m_suffix = i.getStringExtra("file_suffix");

            String title = i.getStringExtra("title");
            if(title != null) {
                setTitle(title);
                View t = findViewById(R.id.action_bar_title);
                if(t != null)
                    ((TextView)t).setText(title);
            }

            if(i.hasExtra("start_path"))
                startPath = i.getStringExtra("start_path");
        }

        loadPath(startPath);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setUpActionBar() {
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return false;
            default: break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void on_cancel_clicked(View v) {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_BACK:
            {
                if(m_cur_path.equals(m_base_path))
                   break;

                if(m_loadTask != null && m_loadTask.getStatus() != AsyncTask.Status.FINISHED)
                    break;

                loadPath(m_cur_path.substring(0, m_cur_path.lastIndexOf("/")));
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private class LoadPathTask extends AsyncTask<String, Void, Object[] >  {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Object[] doInBackground(String... path) {
            File folder = null;
            String cur_path = null;
            Object[] res = new Object[2];

            final String[] paths = { path[0], m_base_path };
            for(String p : paths) {
                folder = new File(p);
                if(folder.exists() && folder.isDirectory() && folder.canRead()) {
                    cur_path = p;
                    break;
                }
            }

            if(cur_path == null) {
                res[0] = new ArrayList<String>();
                res[1] = new ArrayList<String>();
                return res;
            }

            m_cur_path = cur_path;

            ArrayList<String> fileNames = new ArrayList<String>();
            ArrayList<String> folderNames = new ArrayList<String>();
            File[] list = folder.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return !pathname.isHidden() && pathname.canRead() && 
                            (pathname.isDirectory() || m_suffix == null ||
                            pathname.getName().endsWith(m_suffix));
                }
            });

            for(File f : list) {
                if(f.isDirectory())
                    folderNames.add(f.getName());
                else
                    fileNames.add(f.getName());
            }

            Collator collator = Collator.getInstance(Locale.getDefault());
            Collections.sort(fileNames, collator);
            Collections.sort(folderNames, collator);

            if(!cur_path.equals(m_base_path))
                folderNames.add(0, "..");

            res[0] = folderNames;
            res[1] = fileNames; 
            return res;
        }

        @SuppressWarnings("unchecked")
        protected void onPostExecute(Object[] res) {
            LinearLayout l = (LinearLayout)findViewById(R.id.file_list);

            View v = findViewById(R.id.scroll_view);
            v.setVisibility(View.VISIBLE);

            v = findViewById(R.id.progress);
            v.setVisibility(View.GONE);

            if(res == null)
                return;

            ArrayList<String> folderNames = (ArrayList<String>)res[0];
            ArrayList<String> fileNames = (ArrayList<String>)res[1];

            for(int i = 0; i < folderNames.size(); ++i) {
                FileListItem listItem = new FileListItem(FileManagerActivity.this, null, folderNames.get(i), true);
                l.addView(listItem.getView());
           }

           for(int i = 0; i < fileNames.size(); ++i) {
                FileListItem listItem = new FileListItem(FileManagerActivity.this, null, fileNames.get(i), false);
                l.addView(listItem.getView());
           }

           l.post(new Runnable(){
               @Override
               public void run() {
                   ScrollView v = (ScrollView)findViewById(R.id.scroll_view);
                   Integer pos = m_scroll_pos.get(m_cur_path);
                   v.scrollTo(0, pos != null ? pos : 0);
               }
           });
        }
    }

    private void loadPath(String path) {
        m_cur_path = path;

        TextView t = (TextView)findViewById(R.id.path_text);
        t.setText(path);

        LinearLayout l = (LinearLayout)findViewById(R.id.file_list);
        l.removeAllViews();

        View v = findViewById(R.id.scroll_view);
        v.setVisibility(View.GONE);

        v = findViewById(R.id.progress);
        v.setVisibility(View.VISIBLE);

        m_loadTask = new LoadPathTask();
        m_loadTask.execute(path);
    }

    @Override
    public void onFileItemSelected(String name, boolean is_folder) {
        if(is_folder) {
            String path = m_cur_path;
            if(name.equals(".."))
                path = m_cur_path.substring(0, m_cur_path.lastIndexOf("/"));
            else
                path += "/" + name;

            ScrollView v = (ScrollView)findViewById(R.id.scroll_view);
            m_scroll_pos.put(m_cur_path, v.getScrollY());
            loadPath(path);
        }
        else
        {
            Intent res = new Intent();
            res.putExtra("filename", name);
            res.putExtra("path", m_cur_path);
            res.putExtra("file_path", m_cur_path + "/" + name);
            setResult(RESULT_OK, res);
            finish();
        }
    }

    private String m_cur_path;
    private HashMap<String, Integer> m_scroll_pos;
    private String m_base_path;
    private LoadPathTask m_loadTask;
    private String m_suffix;
}