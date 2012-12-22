package com.tassadar.lorrismobile;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

@TargetApi(11)
public class SessionDetailFragment extends Fragment {

    private static final int ACTCODE_EDIT_SESSION = 1;

    public interface OnSessionChangedListener {
        public void onSessionsChanged();
        public void deleteSession(Session s);
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        m_session_name = null;
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.session_detail, container, false);
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.session_details, menu);
        m_menu = menu;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch(item.getItemId()) {
        case R.id.edit_session:
            Intent i = new Intent(getActivity(), SessionEditActivity.class);
            i.putExtra("edit_session", m_session_name);
            startActivityForResult(i, ACTCODE_EDIT_SESSION);
            return true;
        case R.id.delete_session:
            Session s = SessionMgr.get(getActivity(), m_session_name);
            ((OnSessionChangedListener)getActivity()).deleteSession(s);
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK)
            return;
        
        switch(requestCode) {
            case ACTCODE_EDIT_SESSION:
                SessionMgr.loadAvailableNames(getActivity());
                Session s = SessionMgr.get(getActivity(), data.getExtras().getString("session_name"));
                if(s != null)
                    loadSession(s);

                ((OnSessionChangedListener)getActivity()).onSessionsChanged();
                break;
        }
    }

    public void loadSession(Session session) {
        TextView name = (TextView)getView().findViewById(R.id.session_name);
        TextView desc = (TextView)getView().findViewById(R.id.session_desc);
        ImageView image = (ImageView)getView().findViewById(R.id.session_image);
        
        if(name == null || desc == null || image == null)
            return;
        
        m_session_name = session.getName();
        
        name.setText(m_session_name);
        desc.setText(session.getDesc());
        
        Bitmap bmp = session.getImage();
        if(bmp != null)
            image.setImageBitmap(session.getImage());
        else
            image.setImageResource(R.drawable.photo_ph);
    }

    public void setVisible(boolean visible) {
        if(m_menu != null)
        {
            final int[] items = { R.id.edit_session, R.id.delete_session };
            for(int i = 0; i < items.length; ++i)
            {
                MenuItem it = m_menu.findItem(items[i]);
                if(it != null)
                    it.setVisible(visible);
            }
        }
        getView().setVisibility(visible ? View.VISIBLE : View.GONE);
    }
    

    private String m_session_name;
    private Menu m_menu;
}