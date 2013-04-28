package com.tassadar.lorrismobile.yunicontrol;

import java.util.ArrayList;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tassadar.lorrismobile.R;


public class InfoFragment extends YuniControlFragment {

    public InfoFragment() {
        m_axes = new ArrayList<ProgressBar>();
        m_buttons = new ArrayList<RadioButton>();
        m_tristate = new ArrayList<Button>();
        m_itemsVisibility = 3;
        m_voltageFormat = "";
    }

    @Override
    public void onAttach(Activity act) {
        super.onAttach(act);
        m_voltageFormat = act.getString(R.string.voltage);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.yunicontrol_base, container, false);

        m_adapter = new ArrayAdapter<CharSequence>(getActivity(), android.R.layout.simple_spinner_item);
        m_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadContentView(getView());

        BoardInfo b = m_protocol.getCurBoard();
        if(b != null)
            setUpFromInfo(b);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        loadContentView(getView());
    }

    private void loadContentView(View v) {
        Activity act = getActivity();
        if(v == null || act == null)
            return;

        int axes = m_axes.size();
        int btns = m_buttons.size();
        int tristate = m_tristate.size();

        LinearLayout base = (LinearLayout)v.findViewById(R.id.yunicontrol_info_base);
        base.removeAllViews();

        m_axes.clear();
        m_buttons.clear();
        m_tristate.clear();

        View content = View.inflate(act, R.layout.yunicontrol_info, null);
        base.addView(content);

        setAxisCount(axes);
        setButtonCount(btns);
        setTristateCount(tristate);

        TextView n = (TextView)v.findViewById(R.id.board_name);
        n.setText(m_boardName);

        View b = v.findViewById(R.id.yc_timeout_btn);
        b.setOnClickListener(m_onTryAgainClickListener);

        m_voltageText = (TextView)v.findViewById(R.id.cur_voltage);

        setItemsVisibility(m_itemsVisibility);
    }

    public void setAxisCount(int count)
    {
        View v = getView();
        Activity act = getActivity();
        if(act == null || v == null || m_axes.size() == count)
            return;

        int ax_size = m_axes.size();
        LinearLayout axis_layout = (LinearLayout)v.findViewById(R.id.bar_layout);

        while(ax_size != count)
        {
            if(ax_size < count)
            {
                RelativeLayout l = (RelativeLayout)act.getLayoutInflater()
                        .inflate(R.layout.axis_item, axis_layout, false);

                ((TextView)l.findViewById(R.id.bar_text)).
                setText(getResources().getString(R.string.pot) + " " + ax_size);

                axis_layout.addView(l);
                m_axes.add((ProgressBar)l.findViewById(R.id.progress_bar));
                ++ax_size;
            }
            else
            {
                --ax_size;
                ViewParent p = m_axes.remove(ax_size).getParent();
                axis_layout.removeView((View)p);
            }
        }
    }

    public void setButtonCount(int count)
    {
        View v = getView();
        Activity act = getActivity();
        if(v == null || act == null || m_buttons.size() == count)
            return;

        int size = m_buttons.size();
        LinearLayout btn_layout = (LinearLayout)v.findViewById(R.id.button_layout);

        while(size != count)
        {
            if(size < count)
            {
                RelativeLayout l = (RelativeLayout)act.getLayoutInflater()
                        .inflate(R.layout.button_item, btn_layout, false);

                ((TextView)l.findViewById(R.id.button_text)).setText(String.valueOf(size));

                btn_layout.addView(l);
                m_buttons.add((RadioButton)l.findViewById(R.id.button_radio));
                ++size;
            }
            else
            {
                --size;
                ViewParent p = m_buttons.remove(size).getParent();
                btn_layout.removeView((View)p);
            }
        }
    }

    public void setTristateCount(int count)
    {
        View v = getView();
        Activity act = getActivity();
        if(act == null || v == null || m_tristate.size() == count)
            return;

        int size = m_tristate.size();
        LinearLayout btn_layout = (LinearLayout)v.findViewById(R.id.tristate_layout);

        while(size != count)
        {
            if(size < count)
            {
                RelativeLayout l = (RelativeLayout)act.getLayoutInflater()
                        .inflate(R.layout.tristate_item, btn_layout, false);

                ((TextView)l.findViewById(R.id.tristate_text)).setText(String.valueOf(size));

                btn_layout.addView(l);
                m_tristate.add((Button)l.findViewById(R.id.tristate_button));
                ++size;
            }
            else
            {
                --size;
                ViewParent p = m_tristate.remove(size).getParent();
                btn_layout.removeView((View)p);
            }
        }
    }

    private void setItemsVisibility(int state) {
        View v = getView();
        if(v == null)
            return;

        m_itemsVisibility = state;

        View p = v.findViewById(R.id.board_name);
        p.setVisibility(state == 0 ? View.VISIBLE : View.GONE);

        p = v.findViewById(R.id.yc_info_bar);
        p.setVisibility(state == 1 ? View.VISIBLE : View.GONE);

        p = v.findViewById(R.id.yc_timeout_btn);
        p.setVisibility(state == 2 ? View.VISIBLE : View.GONE);
        p = v.findViewById(R.id.yc_timeout_text);
        p.setVisibility(state == 2 ? View.VISIBLE : View.GONE);

        p = v.findViewById(R.id.yc_no_data_text);
        p.setVisibility(state == 3 ? View.VISIBLE : View.GONE);
    }

    private void setUpFromInfo(BoardInfo i) {
        View v = getView();
        if(v == null)
            return;

        TextView n = (TextView)v.findViewById(R.id.board_name);
        n.setText(i.name);
        m_boardName = i.name;

        setAxisCount(i.potCount);
        setButtonCount(i.btnCount);
        setTristateCount(i.triStateCount);
        setItemsVisibility(0);
    }

    @Override
    public void onPacketReceived(Packet pkt) {
        switch(pkt.opcode) {
            case Protocol.CMSG_POT:
            {
                for(int i = 0; !pkt.atEnd() && i < m_axes.size(); ++i)
                    m_axes.get(i).setProgress((short)pkt.read16() + 32768);
                break;
            }
            case Protocol.CMSG_BUTTONS:
            {
                int itr = 0;
                while(!pkt.atEnd()) {
                    int s = pkt.read8();
                    for(int i = 0; i < 8 && i+itr < m_buttons.size(); ++i)
                        m_buttons.get(i+itr).setChecked((s & (1 << i)) != 0);
                    itr += 8;
                }
                break;
            }
            case Protocol.CMSG_TRISTATE:
            {
                for(int i = 0; !pkt.atEnd() && i < m_tristate.size(); ++i)
                    m_tristate.get(i).setText(String.valueOf(pkt.read8()));
                break;
            }
            case Protocol.CMSG_BOARD_VOLTAGE:
            {
                if(m_voltageText != null)
                    m_voltageText.setText(String.format(m_voltageFormat, (float)pkt.read16()/1000));
                break;
            }
        }
    }

    @Override
    public void onInfoRequested() {
        setAxisCount(0);
        setButtonCount(0);
        setTristateCount(0);
        setItemsVisibility(1);
    }

    @Override
    public void onInfoReceived(GlobalInfo i) {
        setItemsVisibility(i != null ? 0 : 2);
        BoardInfo b = m_protocol.getCurBoard();
        if(b != null)
            setUpFromInfo(b);
    }

    @Override
    public void onBoardChange(BoardInfo b) {
        setUpFromInfo(b);
    }

    private final OnClickListener m_onTryAgainClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            m_protocol.requestGlobalInfo();
        }
    };

    private ArrayList<ProgressBar> m_axes;
    private ArrayList<RadioButton> m_buttons;
    private ArrayList<Button> m_tristate;
    private TextView m_voltageText;
    private String m_boardName;
    private int m_itemsVisibility;
    private String m_voltageFormat;

    private ArrayAdapter<CharSequence> m_adapter;
}
