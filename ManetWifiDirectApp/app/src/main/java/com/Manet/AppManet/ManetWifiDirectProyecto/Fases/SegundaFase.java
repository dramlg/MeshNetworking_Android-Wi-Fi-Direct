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
import android.os.AsyncTask;
import android.util.Log;

import com.Manet.AppManet.ManetWifiDirectProyecto.Activities.ActivityConfiguracion;
import com.Manet.AppManet.ManetWifiDirectProyecto.Datos.MisDatos;
import com.Manet.AppManet.ManetWifiDirectProyecto.WiFiDirect.HerramientasYConfiguracionWiFiDirect;
import com.Manet.AppManet.ManetWifiDirectProyecto.Activities.MainActivity;

import java.util.ArrayList;

/**
 *
 * En esta segunda fase, ya tenemos el nombre kConid. Procedemos de nuevo a la búsqueda de
 * dispositivos, y analizando su nombre kConid, obtenemos quien será el ClusterHead, así como
 * activamos éste, el cual crea un AP WiFi Direct.
 *
 * El funcionamiento de selección de ClusterHead será el siguiente:
 *
 * Todos los móviles creerán que son el ClusterHead, cuando reciban un dispositivo cuyo nº
 * de dispositivos alcanzables sea mayor, dejará de ser ClusterHead. En el caso de igual
 * numero de dispositivos alcanzables, se tendrá en cuenta el NID, en este caso, si recibe un
 * nombre kConid con igual conectividad, y menor NID, el dispositivo dejará de pensar que es CH.
 *
 * Created by Moya on 23/12/2015.
 */
public class SegundaFase extends AsyncTask<Object,String,Void> {
    //< entrada en inbackgroud, parametro onprogressupdate, retornoinbackground //

    private ArrayList peers2Fase; //Con nombre kConid
    private HerramientasYConfiguracionWiFiDirect herramientasYanalizarWiFiDirectInfo;
    private MisDatos misDatos;
    private MainActivity mActivity;
    private ProgressDialog progressDialog;
    private int porcentajeInicialProgressDialog; // Maximo valor de dispositivos alcanzables que hemos tenido
    private PrimeraFase primeraFase;
    private String TAG="2ºFase";

    private int timeoutBusquedaVecinos= ActivityConfiguracion.tiempoBusquedaDispositivos*2;

    public static boolean segundaFaseActiva;


    public SegundaFase(HerramientasYConfiguracionWiFiDirect herramientasYanalizarWiFiDirectInfo,MisDatos misDatos,MainActivity mActivity,PrimeraFase primeraFase) {

        this.porcentajeInicialProgressDialog=0;
        this.misDatos = misDatos;
        this.herramientasYanalizarWiFiDirectInfo =herramientasYanalizarWiFiDirectInfo;
        this.mActivity =mActivity;
        this.progressDialog=new ProgressDialog(mActivity);
        this.primeraFase=primeraFase;
        this.segundaFaseActiva=false;

    }

    @Override
    protected Void doInBackground(Object... params) {

        segundaFaseActiva=true;
        Log.d(TAG, "SEGUNDA FASE : " + segundaFaseActiva);

        if(primeraFase.getSaltarSegundaFase()==true){ //Si la fase 2 se cancela debido al CASO 3 (Borrador Proyecto)
            segundaFaseActiva=false;
            Log.d(TAG,"SEGUNDA FASE : " + segundaFaseActiva);
            this.cancel(true);
        }

        if(primeraFase.getCasoA()==false && primeraFase.getCasoB()==false && primeraFase.getCasoD()==false && primeraFase.getCasoC()==true) { /**Si no se encontraron usuarios en la primera fase, pasamos directamente a ser CH*/

            //ESPERAMOS UN TIEMPO A LOS DISPOSITIVOS QUE ESTÉN TERMINANDO LA PRIMERA FASE,
            //PARA CUANDO LA BÚSQUEDA ESTÉ ACTIVA QUE TODOS LOS DISPOSITIVOS ESTÉN EN SEGUNDA FASE
            //(ANTES DE ESTA ESPERA EL NOMBRE HA SIDO CAMBIADO AL FINAL DE LA PRIMERA FASE)
            publishProgress("Esperando al resto de dispositivos a terminar primera fase");
           /* try {
                Thread.sleep(30000); //Tiempo que dura primera fase
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/

            publishProgress("Iniciando Segunda Fase");

            publishProgress("Buscando vecinos...");
            herramientasYanalizarWiFiDirectInfo.buscarVecinos();
            try {
                Thread.sleep((long) timeoutBusquedaVecinos);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            herramientasYanalizarWiFiDirectInfo.pararBusquedaVecinos();
            peers2Fase =mActivity.getPeers2Fase();
            Log.d(TAG, "Dispositivos totales encontrados en 2º Fase: " + peers2Fase);


            publishProgress("Buscando vecinos con nombre kConid...");
            Log.d(TAG, "Comenzamos algoritmo K-CONID con la lista de dispositivos obtenida");

            publishProgress("Seleccionando al ClusterHead");
            misDatos.setSoyClusterHead(true);
            for (int i = 0; i < this.peers2Fase.size(); i++) {
                String informacionDispotivo = "";
                int conectividadDispositivo = -1;
                int NID;

                informacionDispotivo = peers2Fase.get(i).toString();
                    conectividadDispositivo = obtenerConectividadDeNombreKConid(informacionDispotivo);
                    NID = obtenerNIDDeNombreKConid(informacionDispotivo);

                    /***2 CONDICIONES BÁSICAS QUE NO PUEDEN OCURRIR PARA SER CLUSTERHEAD***/


                    if (conectividadDispositivo > misDatos.getNumeroUsuariosAlcanzables()) {
                        misDatos.setSoyClusterHead(false);
                    } else if (conectividadDispositivo == misDatos.getNumeroUsuariosAlcanzables() && NID < misDatos.getNID()) {
                        misDatos.setSoyClusterHead(false);
                    }

                    /**Aunque improbable, una condicion que podría ocurrir*/
                    else if (conectividadDispositivo == misDatos.getNumeroUsuariosAlcanzables() && NID == misDatos.getNID()) {
                        if (misDatos.getNombreKCONID().length() > informacionDispotivo.length()) { //La longitud varia ya que contienen además el nombre original
                            misDatos.setSoyClusterHead(false);
                        }
                    }

                    /***--------------------------------------------------------------***/

            }//Fin del for
            herramientasYanalizarWiFiDirectInfo.cambiarNombreWiFiDirect(MainActivity.nombreOriginalWiFiDirect);
            Log.d(TAG, "Terminada ejecución algoritmo K-CONID");
        }


        if(primeraFase.getCasoA()==false && primeraFase.getCasoD()==false && (primeraFase.getCasoB()==true || primeraFase.getCasoC()==true)) {

            /**Comprobamos si somos CH***/
            if (misDatos.getSoyClusterHead()) {
                Log.d(TAG,"Soy ClusterHead");

                publishProgress("Nos establecemos como CH");
                publishProgress("FINALIZACIONCH");

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }else{
                Log.d(TAG,"No soy ClusterHead");
            }
        }

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

        progressDialog.setMessage(palabra);

        if(palabra.equals("FINALIZACIONCH")){
            if(primeraFase.getCasoA()==false && primeraFase.getCasoD()==false && (primeraFase.getCasoB()==true || primeraFase.getCasoC()==true)) {

                herramientasYanalizarWiFiDirectInfo.cambiarNombreWiFiDirect(MainActivity.nombreOriginalWiFiDirect);

                /**Comprobamos si somos CH***/
                if (misDatos.getSoyClusterHead()) {
                    //Activamos el AP de clusterHead
                    progressDialog.show(); //Si venimos del Caso 3.B de la primera fase, aun no hemos activado este progressDialog
                    progressDialog.setProgress(100);
                    progressDialog.setMessage("Estableciendonos como CH");
                    herramientasYanalizarWiFiDirectInfo.crearGrupo(); //Creamos el AP WiFi Direct

                }
                segundaFaseActiva = false;
                Log.d(TAG,"SEGUNDA FASE : " + segundaFaseActiva);
            }

        }


    }

    @Override
    protected synchronized void onCancelled() {
        super.onCancelled();
        progressDialog.dismiss();
        segundaFaseActiva=false;
    }

    @Override
    protected synchronized void onPreExecute() {
        super.onPreExecute();

        //Inizializamos el ProgressDialog
        this.progressDialog.setCancelable(false);
        this.progressDialog.setMessage("Iniciando Fase 2...");
        this.progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        this.progressDialog.setTitle("Fase 2 - Selección del ClusterHead");

    }

    @Override
    protected synchronized void onPostExecute(Void o) {
        super.onPostExecute(o);
        progressDialog.dismiss();

    }

    /**
     * Obtener el parámetro de conectividad (nº de dispositivos alcanzables), de un nombre kConid
     *
     * @param nombreKCONID
     * @return int nºdispositivosalcanzables
     */
    public synchronized int obtenerConectividadDeNombreKConid(String nombreKCONID){

        String conectividad;

        int posInicio=0;
        int posFinal=0;

        for(int i=0;i<nombreKCONID.length();i++) {

            if (nombreKCONID.substring(i,i+"<C>".length()).equals("<C>")) {
                posInicio=i+"<C>".length();
            }
            if(nombreKCONID.substring(i,i+"<I>".length()).equals("<I>")){
                posFinal=i;
                break;
            }
        }
        conectividad=nombreKCONID.substring(posInicio,posFinal);

        return Integer.parseInt(conectividad);

    }

    /**
     * Obtener el NID de un nombre kConid
     *
     * @param nombreKCONID
     * @return int NID
     */
    public synchronized int obtenerNIDDeNombreKConid(String nombreKCONID){

        String NID;

        int posInicio=0;
        int posFinal=0;

        for(int i=0;i<nombreKCONID.length();i++) {

            if (nombreKCONID.substring(i,i+"<I>".length()).equals("<I>")) {
                posInicio=i+"<I>".length();
            }
            if(nombreKCONID.substring(i,i+"</I>".length()).equals("</I>")){
                posFinal=i;
                break;
            }
        }
        NID=nombreKCONID.substring(posInicio,posFinal);

        return Integer.parseInt(NID);

    }




}
