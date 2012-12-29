package com.tassadar.lorrismobile;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

public class WorkspaceActivity extends FragmentActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT < 11)
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.workspace);

        if(Build.VERSION.SDK_INT >= 11)
            setUpActionBar();

        LinearLayout l = (LinearLayout)findViewById(R.id.tab_list);
        l.removeAllViews();
        
        for(int i = 0; i < 10; ++i)
        {
            TabListItem it = new TabListItem(this, null, "Terminal");
            l.addView(it.getView());
        }
        
        LinearLayout content = (LinearLayout)findViewById(R.id.tab_content_layout);
        content.setOnClickListener(new OnContentClicked());
    }

    @TargetApi(11)
    private void setUpActionBar() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void setTabPanelVisible(boolean visible) {
        LinearLayout l = (LinearLayout)findViewById(R.id.tab_panel);
        if(visible){
            l.setVisibility(View.VISIBLE);
            l.startAnimation(AnimationUtils.loadAnimation(this, R.anim.tab_panel_show));
        }else {
            l.setVisibility(View.GONE);
            l.startAnimation(AnimationUtils.loadAnimation(this, R.anim.tab_panel_hide));
        }
    }
    
    private class OnContentClicked implements View.OnClickListener {
        private boolean visible = false; 
        @Override
        public void onClick(View v) {
            visible = !visible;
            setTabPanelVisible(visible);
        }
        
    }
}