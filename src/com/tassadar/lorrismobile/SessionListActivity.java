package com.tassadar.lorrismobile;

import java.util.ArrayList;
import java.util.LinkedList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SessionListActivity extends ListActivity {

    private static final int ACTCODE_NEW_SESSION = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT  < 11)
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.session_list);

        // FIXME: there must be way to do this correctly
        if(Build.VERSION.SDK_INT >= 11)
            fixActionBarTitle();

        loadSessions();
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
            break;
        }
        default:
            super.onActivityResult(requestCode, resultCode, data);
            break;
        }
        
    }
    
    public void on_createSession_clicked(View button) {
        startActivityForResult(new Intent(this, SessionEditActivity.class), ACTCODE_NEW_SESSION);
    }

    private void setListEmpty(boolean empty) {
        getListView().setVisibility(empty ? View.GONE : View.VISIBLE);

        ((LinearLayout)findViewById(R.id.layout_create_session))
            .setVisibility(empty ? View.VISIBLE : View.GONE);
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
        setListAdapter(new SessionListAdapter(this, R.layout.session_list_item, sessions));
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
            return v;
        }
        
        private ArrayList<Session> m_sessions;
    }
}
