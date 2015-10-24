package com.example.ramesh.p2pfileshare;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.zip.Inflater;

import edu.pdx.cs410.wifi.direct.file.transfer.R;

/**
 * Created by rsriram on 23/10/15.
 */
public class MyArrayAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final String[] values;

    public MyArrayAdapter(Context context, String[] values ) {
        super(context, R.layout.rowlayout);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Log.v("WIFIP2PNEW", "Creating row view and inflating it");
        LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = layoutInflater.inflate(R.layout.rowlayout,parent,false);

        TextView textView = (TextView) rowView.findViewById(R.id.label);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        textView.setText(values[position]);

        return rowView;
    }
}
