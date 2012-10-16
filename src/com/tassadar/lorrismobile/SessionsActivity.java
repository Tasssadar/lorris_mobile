package com.tassadar.lorrismobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;

public class SessionsActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sessions);

        // FIXME: there must be way to do this correctly
        if(Build.VERSION.SDK_INT >= 11)
            fixActionBarTitle();

        setListEmpty(false);

        List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("session_name", "3pi ovladani");
        map.put("session_desc", "popis session");
        fillMaps.add(map);
        
        map = new HashMap<String, String>();
        map.put("session_name", "Snake");
        map.put("session_desc", "popis session");
        fillMaps.add(map);
        
        map = new HashMap<String, String>();
        map.put("session_name", "Barevnej senzor");
        map.put("session_desc", "popis session");
        fillMaps.add(map);

        String[] from = new String[] { "session_name", "session_desc"};
        int[] to = new int[] { R.id.session_name, R.id.session_desc };
        setListAdapter(new SimpleAdapter(this, fillMaps, R.layout.activity_sessions_item, from, to)); 
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_sessions, menu);
        return true;
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
}
