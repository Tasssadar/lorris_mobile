package com.tassadar.lorrismobile.connections;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;

import com.tassadar.lorrismobile.R;

public class ConnectionsActivity extends FragmentActivity {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT < 11)
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.connections);

        if(Build.VERSION.SDK_INT >= 11)
            setUpActionBar();

        m_connBtns = new ImageButton[3];
        m_connBtns[Connection.CONN_BT_SP] = (ImageButton)findViewById(R.id.con_bt_btn);
        m_connBtns[Connection.CONN_TCP] = (ImageButton)findViewById(R.id.con_tcp_btn);
        m_connBtns[Connection.CONN_USB] = (ImageButton)findViewById(R.id.con_usb_btn);
        
        m_currType = -1;
        switchFragment(Connection.CONN_BT_SP);
        m_connBtns[Connection.CONN_BT_SP].setSelected(true);
    }

    @TargetApi(11)
    private void setUpActionBar() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.connections, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch(item.getItemId()) {
        }
        return false;
    }

    public void on_conBtn_clicked(View btn) {
        int type = -1;
        for(int i = 0; i < m_connBtns.length; ++i)
        {
            m_connBtns[i].setSelected(m_connBtns[i] == btn);
            if(m_connBtns[i] == btn)
                type = i;
        }

        if(type == -1 || m_currType == type)
            return;

        switchFragment(type);
    }

    private void switchFragment(int type) {
        Fragment f;
        switch(type) {
            case Connection.CONN_BT_SP:
                f = new BTConnFragment();
                break;
            case Connection.CONN_TCP:
                f = new TCPConnFragment();
                break;
            case Connection.CONN_USB:
                f = new USBConnFragment();
                break;
            default:
                return;
        }

        FragmentManager mgr = getSupportFragmentManager();
        FragmentTransaction transaction = mgr.beginTransaction();
        transaction.replace(R.id.conn_fragment_area, f);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.commit();

        m_currType = type;
    }

    private ImageButton[] m_connBtns;
    private int m_currType;
}