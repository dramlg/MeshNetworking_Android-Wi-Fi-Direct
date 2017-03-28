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

package com.Manet.AppManet.ManetWifiDirectProyecto.Hebras;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.Manet.AppManet.ManetWifiDirectProyecto.Activities.ActivityListViewUsuarios;
import com.Manet.AppManet.ManetWifiDirectProyecto.Activities.MainActivity;
import com.Manet.AppManet.ManetWifiDirectProyecto.Datos.MisDatos;
import com.Manet.AppManet.ManetWifiDirectProyecto.WiFiDirect.HerramientasYConfiguracionWiFiDirect;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;

/**
 * Created by Moya on 5 mar 2016.
 */
public class ConectarACH extends AsyncTask<Object,String,Void> implements Serializable{

    private MainActivity mActivity;
    private ProgressDialog progressDialog;
    private String IP;
    private int puertoDestino;
    private AsyncTask hebraTXCliente,hebraRXCliente;
    private HerramientasYConfiguracionWiFiDirect herramientasYConfiguracionWiFiDirect;
    private MisDatos misDatos;
    private boolean conectado;
    private String TAG="ConxACH";

    public ConectarACH(MainActivity mActivity,String IP, int puertoDestino,HerramientasYConfiguracionWiFiDirect herramientasYConfiguracionWiFiDirect, MisDatos misDatos,
                       AsyncTask hebraRXCliente){
        this.mActivity=mActivity;
        this.IP=IP;
        this.puertoDestino=puertoDestino;
        this.herramientasYConfiguracionWiFiDirect = herramientasYConfiguracionWiFiDirect;
        this.misDatos=misDatos;
        this.conectado=false;
        this.hebraRXCliente=hebraRXCliente;
    }

    @Override
    protected Void doInBackground(Object... params) {

        publishProgress("Creando socket cliente...");


        while(!conectado) {
            try {
                MainActivity.socketSoyCliente = new Socket(IP, puertoDestino);
                Thread.sleep(2000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(MainActivity.socketSoyCliente!=null){
                if(MainActivity.socketSoyCliente.isConnected()){
                    conectado=true;

                    Intent intent= new Intent(mActivity, ActivityListViewUsuarios.class); //Pasamos a la ventana del listView
                    intent.putExtra("misDatos", misDatos);
                    mActivity.startActivity(intent);
                }
            }
        }

        Log.d(TAG, "Encendiendo HebraRX en cliente");


        publishProgress("TurnOnHebraRX");
        publishProgress("Registrarse");
        publishProgress("SOCKET CONECTADO");

        return null;
    }


    @Override
    protected synchronized void onProgressUpdate(String texto[]) {
        String palabra="";
        for(int i=0;i<texto.length;i++){
            palabra=palabra+texto[i];
        }

        if(palabra.equals("TurnOnHebraRX")){
            Log.d(TAG, "Hebra Recepción encendida");
            hebraRXCliente = new HebraRX(mActivity, MainActivity.socketSoyCliente,misDatos,herramientasYConfiguracionWiFiDirect);
            hebraRXCliente.executeOnExecutor(THREAD_POOL_EXECUTOR);
        }
        else if(palabra.equals("TurnOnHebraTX")){
            hebraTXCliente.executeOnExecutor(THREAD_POOL_EXECUTOR);
        }

        else if(palabra.equals("SOCKET CONECTADO")){
            this.progressDialog.cancel();
        }
        else if(palabra.equals("Creando socket cliente...")){
            this.progressDialog.show();
        }

        else if(palabra.equals("Registrarse")){
            String dirIP= herramientasYConfiguracionWiFiDirect.obtenerMiIpConWifiManager();
            AsyncTask hebraTXCliente = new HebraTX(mActivity, MainActivity.socketSoyCliente, "<IP>"+dirIP+"</IP>"+"<NombreOrigen>"+MainActivity.nombreOriginalWiFiDirect+"</NombreOrigen>",misDatos);
            hebraTXCliente.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    @Override
    protected synchronized void onPreExecute() {
        super.onPreExecute();

        //Inizializamos el ProgressDialog
        this.progressDialog=new ProgressDialog(mActivity);
        this.progressDialog.setCancelable(true);
        this.progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        this.progressDialog.setTitle("Iniciando la comunicación");
    }
}
