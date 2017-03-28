package com.Manet.AppManet.ManetWifiDirectProyecto.Activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.Manet.AppManet.ManetWifiDirectProyecto.R;

/**
 * Created by Moya on 1 abr 2016.
 */
public class CustomAdapterMensaje extends ArrayAdapter<String> {

    public CustomAdapterMensaje(Context context, String vector[]) {
        super(context, R.layout.custommensaje, vector );
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater vectorInflater = LayoutInflater.from(getContext());
        View customView=vectorInflater.inflate(R.layout.custommensaje, parent, false);

        String itemVector=getItem(position);
        TextView vectorText=(TextView)customView.findViewById((R.id.textViewCustomMensaje));

        vectorText.setText(itemVector);


        return customView;
    }
}

