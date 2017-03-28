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

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.Manet.AppManet.ManetWifiDirectProyecto.Activities.ActivityListViewUsuarios;
import com.Manet.AppManet.ManetWifiDirectProyecto.Activities.MainActivity;
import com.Manet.AppManet.ManetWifiDirectProyecto.Datos.MisDatos;
import com.Manet.AppManet.ManetWifiDirectProyecto.WiFiDirect.HerramientasYConfiguracionWiFiDirect;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;

/**
 * Esta clase hará lo siguiente: Conforme una nueva petición llegue(nuevo cliente), aceptará dicha
 * conexión, y con el socket resultante, creará 2 nuevas hebras, una para recibir datos y otra para
 * transmitir.
 *
 * Ademas contendrá los Clientes
 */
public class ServidorCH extends AsyncTask<Object,String,Void> {
    //< entrada en inbackgroud, parametro onprogressupdate, retornoinbackground //

    private AsyncTask hebraSocketRecepcion[]=new AsyncTask[128];
    private AsyncTask hebraSocketTransmision[]=new AsyncTask[128];
    private int j;
    private MainActivity mActivity;
    private int puerto;
    private MisDatos misDatos;
    private ActivityListViewUsuarios listViewUsuarios;
    public static HashMap<String,String> coleccionClientes=new HashMap<String,String>(); //Key,Value
    public boolean primeraVez;
    private HerramientasYConfiguracionWiFiDirect herramientasYConfiguracionWiFiDirect;
    private String TAG="ServCH";
    private Boolean hebrainiciada=false;


    public ServidorCH(MainActivity mActivity,MisDatos misDatos,HerramientasYConfiguracionWiFiDirect herramientasYConfiguracionWiFiDirect){
        this.mActivity=mActivity;
        this.misDatos=misDatos;
        this.puerto= MainActivity.puertoCH;
        this.coleccionClientes.put(MainActivity.nombreOriginalWiFiDirect, "192.168.49.1");
        this.j=0;
        this.primeraVez=true;
        this.herramientasYConfiguracionWiFiDirect=herramientasYConfiguracionWiFiDirect;
    }


    @Override
    protected Void doInBackground(Object[] params) {

        try {
            primeraVez=true;
            j=0;
            MainActivity.serverSocket = new ServerSocket(puerto);
            Log.d(TAG,"Servidor CH a la espera de peticiones");


            while(true) {

                synchronized ((Integer)j) {

                    if(primeraVez==false){
                        while (hebrainiciada == false) {
                        }
                        j++;
                        Log.d(TAG,"Siguiente hebra: "+j);
                    }
                    if(j==0) {
                       primeraVez=false;
                    }
                }

                hebrainiciada=false;
                MainActivity.socketsDeMisClientes[j] = MainActivity.serverSocket.accept();
                publishProgress("EjHebraRecepcion");

            }

        } catch (IOException e) {
            e.printStackTrace();
        }



        return null;
    }

    @Override
    protected synchronized void onProgressUpdate(String texto[]) {
        String palabra="";
        for(int i=0;i<texto.length;i++){
            palabra=palabra+texto[i];
        }

        if(palabra.equals("EjHebraRecepcion")){
            hebraSocketRecepcion[j] = new HebraRX(mActivity, MainActivity.socketsDeMisClientes[j], misDatos, herramientasYConfiguracionWiFiDirect);
            hebraSocketRecepcion[j].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            synchronized (hebrainiciada) {
                hebrainiciada = true;
                Log.d(TAG, "Hebra recepción nº " + j + " iniciada");
            }




        }

    }

    @Override
    protected synchronized void onCancelled() {
        super.onCancelled();
        try {
            Log.d(TAG, "Hebra Servidor CH cancelada, SE HA CERRADO EL SERVER SOCKET");
            MainActivity.serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected synchronized void onPostExecute(Void o) {
        super.onPostExecute(o);
        try {
            MainActivity.serverSocket.close();
            Log.d(TAG, "Hebra Servidor CH terminada, SE HA CERRADO EL SERVER SOCKET");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException ex){
            ex.printStackTrace();
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }




}
