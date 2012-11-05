package com.tassadar.lorrismobile;

import java.util.ArrayList;
import java.util.LinkedList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tassadar.lorrismobile.SessionDetailFragment.OnSessionChangedListener;

public class SessionListActivity extends FragmentActivity implements OnSessionChangedListener {

    private static final int ACTCODE_NEW_SESSION = 1;
    private static final int ACTCODE_EDIT_SESSION = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT  < 11)
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.session_list);

        // FIXME: there must be way to do this correctly
        if(Build.VERSION.SDK_INT >= 11)
            fixActionBarTitle();

        getListView().setLongClickable(true);
        getListView().setOnItemLongClickListener(new OnSessionLongClick());
        getListView().setOnItemClickListener(new OnSessionClickListener());

        loadSessions();

        m_last_selected = ListView.INVALID_POSITION;
        if(savedInstanceState != null)
            m_last_selected = savedInstanceState.getInt("selected_idx", ListView.INVALID_POSITION);
        
        if(m_last_selected == ListView.INVALID_POSITION && getListView().getCount() != 0)
            m_last_selected = 0;

        if(m_last_selected != ListView.INVALID_POSITION)
        {
            getListView().setItemChecked(m_last_selected, true);
            loadSessionDetail(m_last_selected);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putInt("selected_idx", m_last_selected);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.session_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
        case R.id.create_session:
            startActivityForResult(new Intent(this, SessionEditActivity.class), ACTCODE_NEW_SESSION);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK)
            return;

        switch(requestCode)
        {
        case ACTCODE_NEW_SESSION:
        {
            loadSessions();

            int pos = m_adapter.getSessionPos(data.getStringExtra("session_name"));
            if(pos != ListView.INVALID_POSITION) {
                getListView().setItemChecked(pos, true);
                loadSessionDetail(pos);
            }
            break;
        }
        case ACTCODE_EDIT_SESSION:
            onSessionsChanged();
            break;
        default:
            super.onActivityResult(requestCode, resultCode, data);
            break;
        }
    }

    @Override
    public void onSessionsChanged() {
        Session s = m_adapter.getSession(m_last_selected);
        String name = s.getName();
        s = null;

        loadSessions();
        
        m_last_selected = m_adapter.getSessionPos(name);
        if(m_last_selected == ListView.INVALID_POSITION && getListView().getCount() != 0)
            m_last_selected = 0;

        getListView().setItemChecked(m_last_selected, true);
        loadSessionDetail(m_last_selected);
    }

    @Override
    public void deleteSession(Session s) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String text = getResources().getString(R.string.erase_session_text);

        builder.setMessage(String.format(text, s.getName()))
               .setTitle(R.string.erase_session_title)
               .setPositiveButton(R.string.ok, new DeleteListener(s))
               .setNegativeButton(R.string.cancel, null)
               .setIcon(R.drawable.alerts_and_states_warning);

        builder.create().show();
    }
    
    private class DeleteListener implements OnClickListener {
        public DeleteListener(Session s) {
            m_session = s;
        }

        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            String text = getResources().getString(R.string.erase_session_toast);
            Toast.makeText(SessionListActivity.this, String.format(text,  m_session.getName()), Toast.LENGTH_SHORT).show();

            SessionMgr.deleteSession(SessionListActivity.this, m_session.getName());
            onSessionsChanged();
        }

        private Session m_session;
    }

    public void on_createSession_clicked(View button) {
        startActivityForResult(new Intent(this, SessionEditActivity.class), ACTCODE_NEW_SESSION);
    }

    private void setListEmpty(boolean empty) {
        getListView().setVisibility(empty ? View.GONE : View.VISIBLE);

        ((LinearLayout)findViewById(R.id.layout_create_session))
            .setVisibility(empty ? View.VISIBLE : View.GONE);

        Fragment f = getSupportFragmentManager().findFragmentById(R.id.detail_fragment);
        if(f != null)
            f.getView().setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @TargetApi(11)
    private void fixActionBarTitle() {
        getActionBar().setTitle(R.string.title_activity_sessions);
    }

    private void loadSessions() {

        SessionMgr.loadAvailableNames(this);

        ArrayList<Session> sessions = new ArrayList<Session>();
        LinkedList<String> names = SessionMgr.getSessionNames();

        for(String name : names) {
            Session s = SessionMgr.get(this, name);
            if(s == null)
                continue;
            sessions.add(s);
        }

        setListEmpty(sessions.isEmpty());
        m_adapter = new SessionListAdapter(this, R.layout.session_list_item, sessions);
        getListView().setAdapter(m_adapter);
    }

    private ListView getListView() {
        return (ListView)findViewById(R.id.session_list);
    }

    private void loadSessionDetail(int pos) {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.detail_fragment);
        if(f == null || m_adapter == null)
            return;

        m_last_selected = pos;

        Session s = m_adapter.getSession(pos);
        ((SessionDetailFragment)f).loadSession(s);
    }

    private class OnSessionClickListener implements OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
            loadSessionDetail(pos);
        }
    }

    private class SessionListAdapter extends ArrayAdapter<Session> {

        public SessionListAdapter(Context context, int textViewResourceId, ArrayList<Session> sessions) {
            super(context, textViewResourceId, sessions);
            m_sessions = sessions;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.session_list_item, null);
            }

            Session s = m_sessions.get(position);
            if(s == null)
                return null;
            
            CheckedTextView checked_name = (CheckedTextView)v.findViewById(R.id.session_name_checked);
            
            if(checked_name != null) {
                checked_name.setText(s.getName());
                Bitmap bmp = s.getImage();
                Drawable d;
                if(bmp != null)
                    d = new BitmapDrawable(getResources(), bmp);
                else
                    d = getResources().getDrawable(R.drawable.photo_ph);

                d.setBounds(0, 0, 64, 64);
                checked_name.setCompoundDrawables(d, null, null, null);
            } else {
                TextView name = (TextView)v.findViewById(R.id.session_name);
                TextView desc = (TextView)v.findViewById(R.id.session_desc);
                ImageView img = (ImageView)v.findViewById(R.id.session_icon);

                if(name != null) {
                    name.setText(s.getName());
                }
                if(desc != null) {
                    desc.setText(s.getDesc());
                }
                if(img != null) {
                    Bitmap bmp = s.getImage();
                    if(bmp != null)
                        img.setImageBitmap(bmp);
                    else
                        img.setImageResource(R.drawable.photo_ph);
                }
            }
            return v;
        }
        
        public Session getSession(int pos) {
            return m_sessions.get(pos);
        }

        public int getSessionPos(String name) { 
            for(int i = 0; i < m_sessions.size(); ++i) {
                if(m_sessions.get(i).getName().equals(name))
                    return i;
            }
            return ListView.INVALID_POSITION;
        }

        private ArrayList<Session> m_sessions;
    }
    
    private class OnSessionLongClick implements OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View v, int pos, long id) {
            
            Session s = m_adapter.getSession(pos);
            if(s == null)
                return false;

            AlertDialog.Builder builder = new AlertDialog.Builder(SessionListActivity.this);

            builder.setTitle(s.getName())
                   .setItems(R.array.session_options, new OnSessionOptionClicked(s));

            builder.create().show();
            return true;
        }
    }
    
    private class OnSessionOptionClicked implements DialogInterface.OnClickListener {
        public OnSessionOptionClicked(Session s) {
            m_session = s;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch(which) {
            case 0:
                Intent i = new Intent(SessionListActivity.this, SessionEditActivity.class);
                i.putExtra("edit_session", m_session.getName());
                startActivityForResult(i, ACTCODE_EDIT_SESSION);
                break;
            case 1:
                deleteSession(m_session);
                break;
            }
        }

        private Session m_session;
    }
    
    private SessionListAdapter m_adapter;
    private int m_last_selected;
}
