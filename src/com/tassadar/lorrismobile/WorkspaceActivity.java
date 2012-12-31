package com.tassadar.lorrismobile;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import com.tassadar.lorrismobile.modules.Tab;
import com.tassadar.lorrismobile.modules.Tab.TabSelectedListener;
import com.tassadar.lorrismobile.modules.TabListItem;

public class WorkspaceActivity extends FragmentActivity implements TabSelectedListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT < 11)
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.workspace);

        m_gest_detect = new GestureDetectorCompat(this, new SwipeListener());
        m_tabs = new ArrayList<Tab>();
        m_active_tab = -1;

        if(Build.VERSION.SDK_INT >= 11)
            setUpActionBar();
    }

    @TargetApi(11)
    private void setUpActionBar() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
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
        Tab t = Tab.createTab(this, type);
        assert(t != null);

        String name = getResources().getStringArray(R.array.tab_names)[type];
        TabListItem it = new TabListItem(this, null, name);
        t.setTabListItem(it);

        LinearLayout l = (LinearLayout)findViewById(R.id.tab_list);
        l.addView(it.getView());
        m_tabs.add(t);

        FragmentManager mgr = getSupportFragmentManager();
        FragmentTransaction transaction = mgr.beginTransaction();
        transaction.add(R.id.tab_content_layout, t);
        transaction.commit();

        setActiveTab(m_tabs.size()-1);
    }

    private void setActiveTab(int idx) {
        if(idx < 0 || idx >= m_tabs.size())
            return;

        if(idx == m_active_tab)
            return;

        Tab old = null; 
        if(m_active_tab != -1)
            old = m_tabs.get(m_active_tab);
        Tab curr = m_tabs.get(idx); 

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
    }

    private Tab findTabById(int tabId) {
        int size = m_tabs.size();
        Tab t;
        for(int i = 0; i < size; ++i)
        {
            t = m_tabs.get(i);
            if(t.getTabId() == tabId)
                return t;
        }
        return null;
    }

    private int findTabIdxById(int tabId) {
        int size = m_tabs.size();
        for(int i = 0; i < size; ++i)
            if(m_tabs.get(i).getTabId() == tabId)
                return i;
        return -1;
    }

    @Override
    public void onTabSelectedClicked(int tabId) {
        int idx = findTabIdxById(tabId);
        if(idx == -1)
            return;
        setActiveTab(idx);
    }

    @Override 
    public boolean onTouchEvent(MotionEvent event){ 
        m_gest_detect.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private class SwipeListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent ev1, MotionEvent ev2, float velX, float velY) {
            if(Math.abs(velX) > Math.abs(velY))
                setTabPanelVisible(velX > 0);
            return true;
        }
    }

    public void on_add_tab_btn_clicked(View v) {
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

    private GestureDetectorCompat m_gest_detect;
    private ArrayList<Tab> m_tabs;
    private int m_active_tab;
}
