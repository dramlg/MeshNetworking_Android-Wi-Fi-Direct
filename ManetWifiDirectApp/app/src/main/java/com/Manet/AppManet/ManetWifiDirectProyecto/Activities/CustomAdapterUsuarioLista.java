////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////// Proyecto Diseño y desarrollo de una red Manet en nodos Android //////////////////////////
////////// AUTOR: Manuel Moya Ferrer ///////////////////////////////////////////////////////////////
////////// Proyecto Fin de Grado en Ingeniería de Telecomunicaciones ///////////////////////////////
////////// 18-Mayo-2016 ////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

package com.Manet.AppManet.ManetWifiDirectProyecto.Activities;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.Manet.AppManet.ManetWifiDirectProyecto.R;

/**
 * Created by Moya on 21 mar 2016.
 */
public class CustomAdapterUsuarioLista extends ArrayAdapter<String> {

    Boolean mensajesSinLeer[];
    String TAG="CALista";

    public CustomAdapterUsuarioLista(Context context, String vector[], Boolean mensajesSinLeer[]) {
        super(context, R.layout.customrowlistviewusuarios, vector );
        this.mensajesSinLeer=mensajesSinLeer;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater vectorInflater = LayoutInflater.from(getContext());
        View customView=vectorInflater.inflate(R.layout.customrowlistviewusuarios, parent, false);

        String itemVector=getItem(position);
        TextView vectorText=(TextView)customView.findViewById((R.id.textViewCustomVectorCloud));
        ImageView vectorImage=(ImageView)customView.findViewById(R.id.imageViewCustomVectorCloud);
        ImageView imagenMensajesSinLeer=(ImageView)customView.findViewById(R.id.imagenMensajesSinLeer);

        vectorText.setText(itemVector);
        vectorImage.setImageResource(R.mipmap.iconperson);
        imagenMensajesSinLeer.setImageResource(android.R.drawable.sym_action_email);

        try {
            if (mensajesSinLeer[position]) {
                imagenMensajesSinLeer.setVisibility(View.VISIBLE);
                Log.d(TAG, "En la posición " + position + " hay notificacion");
            }else{
                imagenMensajesSinLeer.setVisibility(View.INVISIBLE);
            }
        }catch (NullPointerException ex){
            ex.printStackTrace();
        }

        return customView;
    }
}
