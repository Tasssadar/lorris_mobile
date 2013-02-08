package com.tassadar.lorrismobile;

import java.util.ArrayList;
import java.util.Date;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Toast;

import com.tassadar.lorrismobile.SessionService.SessionServiceListener;
import com.tassadar.lorrismobile.connections.Connection;
import com.tassadar.lorrismobile.connections.ConnectionBtn;
import com.tassadar.lorrismobile.connections.ConnectionMgr;
import com.tassadar.lorrismobile.connections.ConnectionMgr.ConnMgrListener;
import com.tassadar.lorrismobile.modules.Tab;
import com.tassadar.lorrismobile.modules.Tab.TabSelectedListener;
import com.tassadar.lorrismobile.modules.TabListItem;
import com.tassadar.lorrismobile.modules.TabManager;

public class WorkspaceActivity extends FragmentActivity implements TabSelectedListener, ConnMgrListener, SessionServiceListener {

    public static final int REQ_SET_CONN = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionMgr.ensureSessionsLoaded(this);

        if(Build.VERSION.SDK_INT < 11)
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.workspace);

        m_connBtn = new ConnectionBtn((ImageButton)findViewById(R.id.conn_btn));
        m_connBtn.hide();

        m_active_tab = -1;
        m_tab_panel_visible = false;

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

        // Remove fragments restored by Android automatically
        if(savedInstanceState != null && !m_preAttachedFragments.isEmpty()) {
            FragmentManager mgr = getSupportFragmentManager();
            FragmentTransaction t = mgr.beginTransaction();
            for(Fragment f : m_preAttachedFragments)
                t.remove(f);
            t.commit();
            m_preAttachedFragments.clear();
        }

        bindService(new Intent(this, SessionService.class), m_sessionServiceConn, Context.BIND_AUTO_CREATE);
    }

    @TargetApi(11)
    private void setUpActionBar() {
        getActionBar().hide();
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
        m_preAttachedFragments.clear();
    }

    @Override
    public void onAttachFragment(Fragment f) {
        super.onAttachFragment(f);
        m_preAttachedFragments.add(f);
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_BACK:
                long curr = new Date().getTime();
                if(curr - m_lastBackPress < 2000)
                    return super.onKeyDown(keyCode, event);
                Toast.makeText(this, R.string.back_twice, Toast.LENGTH_SHORT).show();
                m_lastBackPress = curr;
                return true;
        }
        return super.onKeyDown(keyCode, event);
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
                    m_connBtn.setConnection(conn);
                    conn.open();
                }
                break;
            }
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        m_connBtn.closePopup();
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

    public void on_tabBtn_clicked(View v) {
        setTabPanelVisible(!m_tab_panel_visible);
        v.setSelected(m_tab_panel_visible);
    }

    private void setTabPanelVisible(boolean visible) {
        if(visible == m_tab_panel_visible)
            return;
        
        m_tab_panel_visible = visible;

        LinearLayout menuLayout = (LinearLayout)findViewById(R.id.tab_panel);
        LinearLayout contentLayout = (LinearLayout)findViewById(R.id.tab_content_layout);

        LayoutParams pContent = (LayoutParams) contentLayout.getLayoutParams();
        LayoutParams pMenu = (LayoutParams) menuLayout.getLayoutParams();

        Resources r = getResources();
        // FIXME: should use real size
        int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 201, r.getDisplayMetrics());

        if(visible){
            pMenu.leftMargin = 0;
            pContent.rightMargin = -px;
            
        }else {
            pMenu.leftMargin = -px;
            pContent.rightMargin = 0;
        }
        contentLayout.requestLayout();
        menuLayout.requestLayout();
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

        m_connBtn.show();
        setEmpty(false);

        String name = getResources().getStringArray(R.array.tab_names)[t.getType()];
        TabListItem it = new TabListItem(this, null, name);
        t.setTabListItem(it);

        LinearLayout l = (LinearLayout)findViewById(R.id.tab_list);
        l.addView(it.getView());

        FragmentManager mgr = getSupportFragmentManager();
        FragmentTransaction transaction = mgr.beginTransaction();
        transaction.add(R.id.tab_content_layout, t);

        Fragment m = t.getMenuFragment();
        if(m != null) {
            transaction.add(R.id.menu_layout, m);
            transaction.hide(m);
        }

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

            Fragment m = old.getMenuFragment();
            if(m != null)
                transaction.hide(m);

            old.setActive(false);
        }
        transaction.show(curr);

        Fragment m = curr.getMenuFragment();
        if(m != null)
            transaction.show(m);

        transaction.commit();

        curr.setActive(true);
        m_connBtn.setConnection(curr.getConnection());

        m_active_tab = idx;
        SessionMgr.getActiveSession().setCurrTab(curr.getTabId());
    }

    private void setEmpty(boolean empty) {
        LinearLayout l = (LinearLayout)findViewById(R.id.no_tabs_layout);

        if(empty && l.getChildCount() < 2) {
            String[] names = getResources().getStringArray(R.array.tab_names);
            for(int i = 0; i < names.length; ++i) {
                Button b = new Button(this);
                b.setId(TabManager.getRIdForTabType(i));
                b.setTextSize(18);
                b.setPadding(50, 10, 50, 10);
                b.setLayoutParams(
                        new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT));
                b.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        createTabByRId(v.getId());
                    }
                });
                b.setText(names[i]);
                l.addView(b);
            }
        }

        l.setVisibility(empty ? View.VISIBLE : View.GONE);
    } 

    private void closeTab(int idx) {
        if(idx < 0 || idx >= TabManager.size())
            return;

        Tab t = TabManager.getTabByPos(idx);
        if(t == null)
            return;

        LinearLayout l = (LinearLayout)findViewById(R.id.tab_list);
        l.removeView(t.getTabListItem().getView());

        TabManager.removeTab(t);
        SessionMgr.getActiveSession().rmTab(t.getTabId());
        if(TabManager.isEmpty()) {
            m_connBtn.setConnection(null);
            m_connBtn.hide();
        }

        FragmentManager mgr = getSupportFragmentManager();
        FragmentTransaction transaction = mgr.beginTransaction();

        Fragment m = t.getMenuFragment();
        if(m != null)
            transaction.remove(m);

        transaction.remove(t);
        transaction.commit();

        if(idx == m_active_tab) {
            m_active_tab = -1;
            setActiveTab(0);
        }

        if(TabManager.isEmpty())
            setEmpty(true);
    }

    @Override
    public void onTabSelectedClicked(int tabId) {
        int idx = TabManager.getTabPos(tabId);
        if(idx == -1)
            return;
        setActiveTab(idx);
    }

    @Override
    public void onTabCloseRequesteed(int tabId) {
        int idx = TabManager.getTabPos(tabId);
        if(idx == -1)
            return;
        closeTab(idx);
    }

    @Override
    public void onConnsLoad(ArrayList<ContentValues> values) {
        runOnUiThread(new LoadConnsRunnable(values));
    }

    private class LoadConnsRunnable implements Runnable {
        ArrayList<ContentValues> m_values;
        public LoadConnsRunnable(ArrayList<ContentValues> values) {
            m_values = values;
        }

        @Override
        public void run() {
            SparseArray<Connection> conns = new SparseArray<Connection>();
            for(ContentValues vals : m_values) {
                Connection c = ConnectionMgr.createFromVals(vals);
                if(c != null)
                    conns.append(c.getId(), c);
            }
            ConnectionMgr.addConnsArray(conns);
        }
    }

    @Override
    public void onTabsLoad(ArrayList<ContentValues> values) {
        if(values.isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setEmpty(true);
                }
            });
        }else {
            for(ContentValues vals : values) {
                runOnUiThread(new LoadTabRunnable(vals));
            }
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

    private class CreateTabListener implements OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            createTabByRId(item.getItemId());
            return true;
        }
    }

    private void createTabByRId(int id) {
        switch(id) {
            case R.id.terminal:
                createNewTab(TabManager.TAB_TERMINAL);
                break;
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
    private int m_active_tab;
    private boolean m_tab_panel_visible;
    private long m_lastBackPress;
    private ConnectionBtn m_connBtn;
    private ArrayList<Fragment> m_preAttachedFragments = new ArrayList<Fragment>();
}
