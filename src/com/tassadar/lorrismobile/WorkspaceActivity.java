package com.tassadar.lorrismobile;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.tassadar.lorrismobile.SessionService.SessionServiceListener;
import com.tassadar.lorrismobile.connections.Connection;
import com.tassadar.lorrismobile.connections.ConnectionMgr;
import com.tassadar.lorrismobile.connections.ConnectionMgr.ConnMgrListener;
import com.tassadar.lorrismobile.connections.ConnectionsActivity;
import com.tassadar.lorrismobile.modules.Tab;
import com.tassadar.lorrismobile.modules.Tab.TabSelectedListener;
import com.tassadar.lorrismobile.modules.TabListItem;
import com.tassadar.lorrismobile.modules.TabManager;

public class WorkspaceActivity extends FragmentActivity implements TabSelectedListener, ConnMgrListener, SessionServiceListener {

    private static final int REQ_SET_CONN = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionMgr.ensureSessionsLoaded(this);

        if(Build.VERSION.SDK_INT < 11)
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.workspace);

        m_gest_detect = new GestureDetectorCompat(this, m_gestListener);
        m_active_tab = -1;

        if(Build.VERSION.SDK_INT >= 11)
            setUpActionBar();

        Session s = SessionMgr.getActiveSession();
        if(s == null && savedInstanceState != null) {
            s = SessionMgr.get(this, savedInstanceState.getString("activeSession"));
            if(s == null) {
                Log.e("Lorris", "Failed to get active session, will probably crash.\n");
            }
            SessionMgr.setActiveSession(s);
        }

        TabManager.setTabIdCounter(s.getMaxTabId()+1);
        ConnectionMgr.setConnIdCounter(s.getMaxConnId()+1);
        ConnectionMgr.setListener(this);

        // Clear leftovers from previous session save
        s.clearChanges();

        bindService(new Intent(this, SessionService.class), m_sessionServiceConn, Context.BIND_AUTO_CREATE);
    }

    @TargetApi(11)
    private void setUpActionBar() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Session s =  SessionMgr.getActiveSession();
        if(s != null)
            outState.putString("activeSession",s.getName());
    }

    @Override
    public void onPause() {
        super.onPause();
        m_sessionService.saveSession(SessionMgr.getActiveSession(),
                TabManager.cloneTabArray(), ConnectionMgr.cloneConnArray());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setResult(RESULT_OK);
        m_active_tab = -1;
        unbindService(m_sessionServiceConn);

        TabManager.takeTabArray();
        ConnectionMgr.takeConnArray();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.workspace, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch(item.getItemId()) {
            case R.id.connection:
                return true;
            case R.id.set_connection:
                Intent i = new Intent(this, ConnectionsActivity.class);
                startActivityForResult(i, REQ_SET_CONN);
                return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK)
            return;
        switch(requestCode) {
            case REQ_SET_CONN:
            {
                if(data != null && !TabManager.isEmpty()) {
                    int id = data.getIntExtra("connId", -1);
                    Connection conn = ConnectionMgr.getConnection(id); 
                    TabManager.getTabByPos(m_active_tab).setConnection(conn);
                    conn.open();
                }
                break;
            }
        }
    }

    @Override
    public void onConnAdded(Connection c) {
        SessionMgr.getActiveSession().addConn(c.getId());
    }

    @Override
    public void onConnRemoved(int id) {
        SessionMgr.getActiveSession().rmConn(id);
        Log.e("Lorris", "Connection " + id + " removed \n");
    }


    private void setTabPanelVisible(boolean visible) {
        LinearLayout l = (LinearLayout)findViewById(R.id.tab_panel);
        if(visible == (l.getVisibility() == View.VISIBLE))
            return;

        if(visible){
            l.setVisibility(View.VISIBLE);
            l.startAnimation(AnimationUtils.loadAnimation(this, R.anim.tab_panel_show));
        }else {
            l.setVisibility(View.GONE);
            l.startAnimation(AnimationUtils.loadAnimation(this, R.anim.tab_panel_hide));
        }
    }

    private void createNewTab(int type) {
        Tab t = TabManager.createTab(this, type);
        assert(t != null);

        t.setTabId(TabManager.generateTabId());
        registerTab(t);

        SessionMgr.getActiveSession().addTab(t.getTabId());
        setActiveTab(TabManager.size()-1);
    }

    private void registerTab(Tab t) {
        TabManager.addTab(t);

        String name = getResources().getStringArray(R.array.tab_names)[t.getType()];
        TabListItem it = new TabListItem(this, null, name);
        t.setTabListItem(it);

        LinearLayout l = (LinearLayout)findViewById(R.id.tab_list);
        l.addView(it.getView());

        FragmentManager mgr = getSupportFragmentManager();
        FragmentTransaction transaction = mgr.beginTransaction();
        transaction.add(R.id.tab_content_layout, t);
        transaction.hide(t);
        transaction.commit();
    }

    private void setActiveTab(int idx) {
        if(idx < 0 || idx >= TabManager.size())
            return;

        if(idx == m_active_tab)
            return;

        Tab old = null; 
        if(m_active_tab != -1)
            old = TabManager.getTabByPos(m_active_tab);
        Tab curr = TabManager.getTabByPos(idx); 

        FragmentManager mgr = getSupportFragmentManager();
        FragmentTransaction transaction = mgr.beginTransaction();

        if(old != null) {
            transaction.hide(old);
            old.setActive(false);
        }
        transaction.show(curr);
        transaction.commit();

        curr.setActive(true);

        m_active_tab = idx;
        SessionMgr.getActiveSession().setCurrTab(curr.getTabId());
    }

    @Override
    public void onTabSelectedClicked(int tabId) {
        int idx = TabManager.getTabPos(tabId);
        if(idx == -1)
            return;
        setActiveTab(idx);
    }

    @Override
    public void onConnsLoad(SparseArray<Connection> conns) {
        ConnectionMgr.addConnsArray(conns);
    }

    @Override
    public void onTabsLoad(ArrayList<ContentValues> values) {
        for(ContentValues vals : values) {
            runOnUiThread(new LoadTabRunnable(vals));
        }
    }

    private class LoadTabRunnable implements Runnable {
        private ContentValues m_vals;
        public LoadTabRunnable(ContentValues vals) {
            m_vals = vals;
        }

        @Override
        public void run() {
            Tab t = TabManager.createTab(WorkspaceActivity.this, m_vals.getAsInteger("type"));
            if(t == null)
                return;

            t.setTabId(m_vals.getAsInteger("id"));
            registerTab(t);
            Log.i("Lorris", "Tab has connection id " + m_vals.getAsInteger("conn_id") + "\n");
            Connection c = ConnectionMgr.getConnection(m_vals.getAsInteger("conn_id"));
            if(c != null)
                t.setConnection(c);
            t.loadData(m_vals.getAsByteArray("data"));

            if(SessionMgr.getActiveSession().getCurrTabId() == t.getTabId())
                setActiveTab(TabManager.size()-1);
        }
    }

    @Override 
    public boolean onTouchEvent(MotionEvent event){ 
        m_gest_detect.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private GestureDetector.SimpleOnGestureListener m_gestListener = new  GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onFling(MotionEvent ev1, MotionEvent ev2, float velX, float velY) {
            if(Math.abs(velX) > Math.abs(velY))
                setTabPanelVisible(velX > 0);
            return true;
        }
    };

    public GestureDetector.SimpleOnGestureListener getGestureListener() {
        return m_gestListener;
    }

    private class CreateTabListener implements OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch(item.getItemId()) {
            case R.id.analyzer:
                createNewTab(TabManager.TAB_ANALYZER);
                return true;
            case R.id.terminal:
                createNewTab(TabManager.TAB_TERMINAL);
                return true;
            }
            return false;
        }
        
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void showCreateTabMenuICS(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(new CreateTabListener());
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.create_tab, popup.getMenu());
        popup.show();
    }

    private void showCreateTabMenuLegacy() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.new_tab)
            .setItems(R.array.tab_names, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    createNewTab(which);
                }
            });
        builder.create().show();
    }

    public void on_add_tab_btn_clicked(View v) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            showCreateTabMenuLegacy();
        else
            showCreateTabMenuICS(v);
    }

    private ServiceConnection m_sessionServiceConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            m_sessionService = ((SessionService.SessionBinder)service).getService();
            m_sessionService.loadSession(SessionMgr.getActiveSession(), WorkspaceActivity.this);
        }

        public void onServiceDisconnected(ComponentName className) {
            m_sessionService = null;
        }
    };

    private SessionService m_sessionService;
    private GestureDetectorCompat m_gest_detect;
    private int m_active_tab;
}
