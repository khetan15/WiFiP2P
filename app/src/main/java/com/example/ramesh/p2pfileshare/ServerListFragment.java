package com.example.ramesh.p2pfileshare;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import edu.pdx.cs410.wifi.direct.file.transfer.R;

/**
 * Created by rsriram on 23/10/15.
 */
public class ServerListFragment extends ListFragment {

    onServerSelectedListener mCallback;
    String[] serverNames;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //values = savedInstanceState.getStringArray("peer_names");
        serverNames = getArguments().getStringArray("server_names");
        Log.v("WIFIP2PNEW","Inside onCreateView");
        return inflater.inflate(R.layout.my_list_fragment,container,false);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        MyArrayAdapter myArrayAdapter = new MyArrayAdapter(getActivity(),serverNames);
        setListAdapter(myArrayAdapter);
        Log.v("WIFIP2PNEW","Inside onAcitivtyCreated");

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try{
            mCallback = (onServerSelectedListener)activity;
            Log.v("WIFIP2PNEW","onServerSelectedListener received");
        } catch(ClassCastException e){
            throw new ClassCastException(activity.toString()+" must implement onServerSelectedListener");
        }
    }

    public interface onServerSelectedListener {
        public void onServerSelected(int position);
    }
}
