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

package com.Manet.AppManet.ManetWifiDirectProyecto.Fases;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.Manet.AppManet.ManetWifiDirectProyecto.Activities.ActivityConfiguracion;
import com.Manet.AppManet.ManetWifiDirectProyecto.Activities.ActivityListViewUsuarios;
import com.Manet.AppManet.ManetWifiDirectProyecto.Hebras.ConectarACH;
import com.Manet.AppManet.ManetWifiDirectProyecto.Datos.MisDatos;
import com.Manet.AppManet.ManetWifiDirectProyecto.WiFiDirect.HerramientasYConfiguracionWiFiDirect;
import com.Manet.AppManet.ManetWifiDirectProyecto.Activities.MainActivity;

/**
 *
 * En esta tercera fase, ya se encuentran establecidos los CH oportunos, y por tanto, la tarea a
 * realizar ahora es la de la conexión a estos (en caso de no ser clusterhead), o la de recibir
 * clientes y guardar sus direcciones IP. Es por esto que la tarea principal de esta fase se
 * dividirá en 2 casos: Si soy CH - Si NO soy CH.
 *
 * Created by Moya on 23/02/2016.
 */
public class TerceraFase extends AsyncTask<Object,String,Void> {
    //< entrada en inbackgroud, parametro onprogressupdate, retornoinbackground //

    private HerramientasYConfiguracionWiFiDirect herramientasYanalizarWiFiDirectInfo;
    private MisDatos misDatos;
    private MainActivity mActivity;
    private ProgressDialog progressDialog;
    private AsyncTask conectarAsocketCH;
    private int porcentajeInicialProgressDialog; // Maximo valor de dispositivos alcanzables que hemos tenido
    private PrimeraFase primeraFase;
    private AsyncTask hebraRXCliente;
    private boolean conectado;
    private int intentosDeConexion;
    private String TAG="3ºFase";


    private int timeoutBusquedaGrupos= ActivityConfiguracion.tiempoBusquedaGrupos;

    public static boolean terceraFaseActiva;

    public TerceraFase(HerramientasYConfiguracionWiFiDirect herramientasYanalizarWiFiDirectInfo,MisDatos misDatos,
                       MainActivity mActivity,AsyncTask conectarAsocketCH,PrimeraFase primeraFase,AsyncTask hebraRXCliente) {

        this.porcentajeInicialProgressDialog=0;
        this.misDatos = misDatos;
        this.herramientasYanalizarWiFiDirectInfo =herramientasYanalizarWiFiDirectInfo;
        this.mActivity=mActivity;
        this.progressDialog=new ProgressDialog(mActivity);
        this.conectarAsocketCH=conectarAsocketCH;
        this.primeraFase=primeraFase;
        this.hebraRXCliente=hebraRXCliente;
        this.terceraFaseActiva=false;
        this.conectado=false;

    }

    @Override
    protected Void doInBackground(Object... params) {

        terceraFaseActiva = true;
        Log.d(TAG, "TERCERA FASE : " + terceraFaseActiva);

        publishProgress("Iniciando fase 3");

        /**BLOQUE 1 - SI NO SOY CLUSTERHEAD*/
        if (misDatos.getSoyClusterHead() == false) {

            if (primeraFase.getCasoA()==false && primeraFase.getCasoB()==false && (primeraFase.getCasoC()==true || primeraFase.getCasoD()==true)) {  //Si nos hemos saltado la segunda fase es porque ya hemos encontrado un CH
            /*Buscamos los CH existentes*/

                publishProgress("Buscando ClusterHead...");
                double referenciaTemporizador;
                referenciaTemporizador = System.currentTimeMillis();
                int numeroDeGruposAnterior = 0;
                int numeroDeGruposActual = 0;
                boolean busquedaDeGruposActiva = true;
                herramientasYanalizarWiFiDirectInfo.buscarGrupos();
                while (busquedaDeGruposActiva) {
                    if ((System.currentTimeMillis() - referenciaTemporizador) > timeoutBusquedaGrupos) {
                        numeroDeGruposActual = herramientasYanalizarWiFiDirectInfo.getNumeroCHEncontrados();
                        if ((numeroDeGruposActual - numeroDeGruposAnterior) >= 1) {
                            numeroDeGruposAnterior = numeroDeGruposActual;
                            busquedaDeGruposActiva = true;
                            referenciaTemporizador = System.currentTimeMillis();
                            timeoutBusquedaGrupos=timeoutBusquedaGrupos/2;
                        } else {
                            busquedaDeGruposActiva = false;
                        }
                    }
                }
                herramientasYanalizarWiFiDirectInfo.pararBusquedaGrupos();


            }

            if(herramientasYanalizarWiFiDirectInfo.getNumeroCHEncontrados()>0) {
            /*Posteriormente nos conectamos a uno de los encontrados, (tenemos que mejorar esto
            ya que no sabemos a cual de ellos conectarnos)*/

                publishProgress("Conectando al ClusterHead");

                ConnectivityManager connManager = (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                WifiManager wifiManager = (WifiManager) mActivity.getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wifiManager.getConnectionInfo();

            /*Siya estamos conectados, no es necesario realizar el proceso*/
                if ((info.getSSID().equals("\"" + herramientasYanalizarWiFiDirectInfo.getSSID(0) + "\"")) == false) {

                    Log.d(TAG, "Proceso de conexión al ClusterHead óptimo");

                    publishProgress("DeterminarYGuardarCHoptimo");

                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    publishProgress("ConectaraAPGuardado");

                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    info = wifiManager.getConnectionInfo();
                    if (mWifi.isConnected() && (info.getSSID().equals("\"" + herramientasYanalizarWiFiDirectInfo.getSSID(0) + "\""))) {
                        conectado = true;
                    }

                    //Si este proceso no ha funcionado, reiniciamos WiFi, en los móviles antiguos suele ocurrir
                    while (conectado == false) {
                        Log.d(TAG, "No se ha conectado correctamente, se reiniciará WiFi para reiniciar el proceso");
                        publishProgress("DesactivarWifi");

                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        publishProgress("ActivarWifi");

                        try {
                            Thread.sleep(20000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        connManager = (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
                        mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                        if (mWifi.isConnected()) {

                            info = wifiManager.getConnectionInfo();
                            //Si se a conectado a una red diferente, reconectamos a la que queremos
                            if ((info.getSSID().equals("\"" + herramientasYanalizarWiFiDirectInfo.getSSID(0) + "\"")) == false) {
                                publishProgress("ConectaraAPGuardado");

                                try {
                                    Thread.sleep(20000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }


                            }
                            info = wifiManager.getConnectionInfo();
                            if ((info.getSSID().equals("\"" + herramientasYanalizarWiFiDirectInfo.getSSID(0) + "\""))) {
                                conectado = true;
                            }
                            intentosDeConexion++;
                            if (intentosDeConexion == 3) {
                                publishProgress("error: No se ha podido conectar");
                                System.exit(0);
                            }

                        }

                    }
                }

                publishProgress("Activando canal comunicación hacía el CH");
                publishProgress("ActivarComunicacionSockets");
            }else{
                publishProgress("No grupos encontrados");

            }
        }
        /*****/

        /**BLOQUE 2 - SI SOY CLUSTERHEAD*/
        if (misDatos.getSoyClusterHead() == true) {
            publishProgress("Encendiendo Servidor...");
            herramientasYanalizarWiFiDirectInfo.buscarGrupos();
            herramientasYanalizarWiFiDirectInfo.encenderServidorCH();
        }
        /*****/



        return null;
    }

    @Override
    protected synchronized void onProgressUpdate(String... texto) {
        super.onProgressUpdate(texto);
        this.progressDialog.show();
        String palabra="";
        for(int i=0;i<texto.length;i++){
            palabra=palabra+texto[i];
        }

        if (palabra.equals("DeterminarYGuardarCHoptimo")) {
            int CHalqueconectar=-1;

            CHalqueconectar=herramientasYanalizarWiFiDirectInfo.determinarCHalQueConectar();
            if(CHalqueconectar!=-1) {
                herramientasYanalizarWiFiDirectInfo.guardarAPwifiDirect(CHalqueconectar);
                Log.d(TAG,"CH optimo guardado");
            }else{
                herramientasYanalizarWiFiDirectInfo.guardarAPwifiDirect(0);
                Log.d(TAG,"No se ha podido determinar el CH óptimo, se ha guardado el CH ubicado en la pos 0");
            }
        }

        else if(palabra.equals("ConectaraAPGuardado")){
            boolean conectar=false;
            conectar=herramientasYanalizarWiFiDirectInfo.conectarApGuardadoWiFi(0);
        }

        else if(palabra.equals("ActivarComunicacionSockets")){
            conectarAsocketCH = new ConectarACH(mActivity,herramientasYanalizarWiFiDirectInfo.getIP(0), MainActivity.puertoCH, herramientasYanalizarWiFiDirectInfo, misDatos,hebraRXCliente);
            conectarAsocketCH.execute();
        }
        else if(palabra.equals("Aniadirnos a la lista si soy CH")){
            //listViewUsuarios.setUsuario(MainActivity.nombreOriginalWiFiDirect,"192.168.49.1");
        }
        else if(palabra.equals("DesactivarWifi")){
            WifiManager wifiManager = (WifiManager) mActivity.getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(false);
        }
        else if(palabra.equals("ActivarWifi")){
            WifiManager wifiManager = (WifiManager) mActivity.getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(true);
        }else if(palabra.contains("error")){
            Toast.makeText(mActivity,palabra,Toast.LENGTH_LONG).show();
        }
        else if(palabra.equals("No grupos encontrados")){
            Toast.makeText(mActivity,"Ha habido un fallo, ya que no se ha encontrado ningún grupo como debería haber ocurrido, " +
                    "reinicia el proceso.",Toast.LENGTH_SHORT).show();
        }
        else{
            if(porcentajeInicialProgressDialog<=100) {
                porcentajeInicialProgressDialog = porcentajeInicialProgressDialog + 25;
            }
            progressDialog.setMessage(palabra);
            progressDialog.setProgress(porcentajeInicialProgressDialog);
        }

    }

    @Override
    protected synchronized void onCancelled() {
        super.onCancelled();
        this.progressDialog.dismiss();
        terceraFaseActiva=false;
    }

    @Override
    protected synchronized void onPostExecute(Void o) {
        super.onPostExecute(o);
        progressDialog.dismiss();
        terceraFaseActiva=false;
        if(misDatos.getSoyClusterHead()){
            Intent intent= new Intent(mActivity, ActivityListViewUsuarios.class); //Pasamos a la ventana del listView
            intent.putExtra("misDatos", misDatos);
            mActivity.startActivity(intent);
        }
        herramientasYanalizarWiFiDirectInfo.cambiarNombreWiFiDirect(MainActivity.nombreOriginalWiFiDirect);

    }

    @Override
    protected synchronized void onPreExecute() {
        super.onPreExecute();

        //Inizializamos el ProgressDialog

        this.progressDialog.setCancelable(false);
        this.progressDialog.setMessage("Iniciando Fase 3...");
        this.progressDialog.setMax(100);
        this.progressDialog.setProgress(0);
        this.progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        this.progressDialog.setTitle("Fase 3 - Establecimiento de la red");

    }

}
