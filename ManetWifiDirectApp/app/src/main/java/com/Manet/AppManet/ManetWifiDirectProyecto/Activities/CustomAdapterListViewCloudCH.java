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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.Manet.AppManet.ManetWifiDirectProyecto.R;

/**
 * Created by Moya on 27 abr 2016.
 */
public class CustomAdapterListViewCloudCH extends ArrayAdapter<String> {
    private String vectorTamanoArchivos[];
    public CustomAdapterListViewCloudCH(Context context, String vectorNombresArchivos[],String vectorTamanoArchivos[]) {
        super(context, R.layout.customrowlistviewcloudch, vectorNombresArchivos);
        this.vectorTamanoArchivos=vectorTamanoArchivos;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater vectorInflater = LayoutInflater.from(getContext());
        View customView=vectorInflater.inflate(R.layout.customrowlistviewcloudch, parent, false);

        String itemVector=getItem(position);
        TextView vectorTextNombreArchivo=(TextView)customView.findViewById((R.id.textViewCustomVectorCloud));
        TextView vectorTextTamanoArchivo=(TextView)customView.findViewById((R.id.textViewTamano));

        ImageView vectorImage=(ImageView)customView.findViewById(R.id.imageViewCustomVectorCloud);
        vectorTextTamanoArchivo.setText(vectorTamanoArchivos[position]);
        vectorTextNombreArchivo.setText(itemVector);
        //vectorImage.setImageResource(R.mipmap.iconperson);

        return customView;
    }
}
