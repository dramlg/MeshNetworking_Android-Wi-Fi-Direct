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
import android.widget.Toast;

import com.Manet.AppManet.ManetWifiDirectProyecto.Activities.ActivityConfiguracion;
import com.Manet.AppManet.ManetWifiDirectProyecto.Activities.MainActivity;
import com.Manet.AppManet.ManetWifiDirectProyecto.Datos.MisDatos;
import com.Manet.AppManet.ManetWifiDirectProyecto.WiFiDirect.HerramientasYConfiguracionWiFiDirect;

import java.util.ArrayList;


/**
 *
 * En esta fase, se produce la primera búsqueda de CH, si se encuentra alguno activo, se saltará la
 * fase 2 y se conectará a éste, para evitar la formación de nuevas Redes. En caso contrario, se
 * llevará a cabo una búsqueda de dispositivos para la formación de una nueva Red. Se establece el
 * máximo número de usuario existentes que alcanza dicha lista, y se establece un nombre
 * WiFiDirect que contiene los datos necesarios para implementar el algoritmo
 * kConid(nº maximo de la lista (nº de dispositivos alcanzables),
 * numero de identificación NID, y nombre original para volver a reestablecerlo al final de la
 * segunda fase). Dichos datos se almacenan en la clase MisDatos.
 *
 * Created by Moya on 23/12/2015.
 */
public class PrimeraFase extends AsyncTask<Object,String,Void> {
    //< entrada en inbackgroud, parametro onprogressupdate, retornoinbackground //


    private MisDatos misDatos;
    private MainActivity mActivity;
    private ProgressDialog progressDialog;

    private ArrayList peersPrimeraBusqueda;
    private int maximoPeersPrimeraBusqueda =0;
    private ArrayList peersSegundaBusqueda;
    private int maximoPeersSegundaBusqueda =0;



    private int numeroGruposEncontrados;
    private boolean saltarSegundaFase,saltarTerceraFase, hayUsuariosEncontrados;
    private HerramientasYConfiguracionWiFiDirect herramientasYanalizarWiFiDirectInfo;
    private boolean dispositivoEncontradoEnSegundaFase;
    private String TAG="1ºFase";


    private double timeoutBusquedaGrupos= ActivityConfiguracion.tiempoBusquedaGrupos; //ms
    private double timeoutBusquedaVecinos=ActivityConfiguracion.tiempoBusquedaDispositivos; //ms


    public static boolean primeraFaseActiva,primeraFaseBuscandoGrupos;

    private boolean casoA;
    private boolean casoB;
    private boolean casoC;
    private boolean casoD;

    public PrimeraFase(HerramientasYConfiguracionWiFiDirect herramientasYanalizarWiFiDirectInfo,MisDatos misDatos,MainActivity mActivity) {



        this.herramientasYanalizarWiFiDirectInfo =herramientasYanalizarWiFiDirectInfo;
        this.misDatos = misDatos;
        this.mActivity =mActivity;
        this.progressDialog= new ProgressDialog(mActivity);
        this.numeroGruposEncontrados=0;
        this.saltarSegundaFase=false;
        this.hayUsuariosEncontrados =true;
        this.saltarTerceraFase=false;
        this.dispositivoEncontradoEnSegundaFase =false;
        this.primeraFaseActiva=false;
        this.primeraFaseBuscandoGrupos=false;

        this.casoA =false;
        this.casoB =false;
        this.casoC =false;
        this.casoD =false;
    }

    @Override
    protected Void doInBackground(Object... params) {

        primeraFaseActiva=true;
        Log.d(TAG, "PRIMERA FASE : " + primeraFaseActiva);

        if(!misDatos.getSoyClusterHead()) {

            publishProgress("Iniciando fase 1");
            primeraFaseBuscandoGrupos = true;

            /**BÚSQUEDA DE CHS**/
            publishProgress("Buscando posibles CH ya existentes");
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
                        timeoutBusquedaGrupos = timeoutBusquedaGrupos / 2;
                    } else {
                        busquedaDeGruposActiva = false;
                    }
                }
            }
            herramientasYanalizarWiFiDirectInfo.pararBusquedaGrupos();


            numeroGruposEncontrados = herramientasYanalizarWiFiDirectInfo.getNumeroCHEncontrados();
            primeraFaseBuscandoGrupos = false;


            /**SI NO EXISTEN CH EXISTENTES, BÚSQUEDA DE VECINOS PARA FORMAR RED**/
            if (numeroGruposEncontrados == 0) {

                publishProgress("Buscando vecinos...");

                //PRIMERA BÚSQUEDA
                maximoPeersPrimeraBusqueda = 0;
                herramientasYanalizarWiFiDirectInfo.buscarVecinos();
                try {
                    Thread.sleep((long) timeoutBusquedaVecinos/2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                herramientasYanalizarWiFiDirectInfo.pararBusquedaVecinos();
                peersPrimeraBusqueda = mActivity.getPeers1Fase();
                maximoPeersPrimeraBusqueda = mActivity.getMaximoPeers1Fase();
                Log.d(TAG, "El tamaño de la lista de dispositivos encontrados en la primera búsqueda es: " + maximoPeersPrimeraBusqueda);

                //SEGUNDA BÚSQUEDA
                maximoPeersSegundaBusqueda = 0;
                herramientasYanalizarWiFiDirectInfo.buscarVecinos();
                try {
                    Thread.sleep((long) timeoutBusquedaVecinos/2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                herramientasYanalizarWiFiDirectInfo.pararBusquedaVecinos();
                peersSegundaBusqueda = mActivity.getPeers1Fase();
                maximoPeersSegundaBusqueda = mActivity.getMaximoPeers1Fase();
                Log.d(TAG, "El tamaño de la lista de dispositivos encontrados en la segunda búsqueda es: " + maximoPeersPrimeraBusqueda);




                for (int q = 0; q < this.peersPrimeraBusqueda.size(); q++) {
                    String informacionDispotivo = "";
                    informacionDispotivo = peersPrimeraBusqueda.get(q).toString();
                    if (informacionDispotivo.contains("<C>")) {
                        this.dispositivoEncontradoEnSegundaFase = true;
                    }
                }

                if (maximoPeersPrimeraBusqueda == 0 && maximoPeersSegundaBusqueda==0) {
                    this.dispositivoEncontradoEnSegundaFase = false;
                    this.hayUsuariosEncontrados = false;
                }

            }

        }

        /**En este momento tenemos 3 casos:
         *
         * Caso A: Ya se ha encontrado una Red Formada , por lo que
         * estableceremos el booleano saltarSegundaFase=true, y directamente pasaremos a la
         * TerceraFase para unirnos a dicho CH.
         *
         * Caso B: No hemos encontrado ningún usuario ni grupo, por ello pasamos directamente a ser CH, sin
         * establecer nombre k-CONID. Para ello, la variable booleana hayUsuariosEncontrados = true
         *
         * Caso C: No existe ninguna Red previamente formada, a parte de nosotros.
         * Estableceremos nuestro nombre K-Conid y pasaremos a la fase 2
         *
         * Caso D: Hemos encontrado un dispositivo en segunda fase. Pasamos directamente a la 3º fase
         * a realizar una búsqueda de grupos para conectarnos al CH que será formado por dicho/s
         * dispositivos encontrados
         *
         * */


        if(misDatos.getSoyClusterHead()){
            //CH forzado por configuracion, el caso B, pero sin atneriormente buscar nada
            casoB=true;
            saltarSegundaFase=false;
        }
        else if(numeroGruposEncontrados>0){  /**CASO A*/
            casoA=true;
            saltarSegundaFase=true;
            misDatos.setSoyClusterHead(false);
        }
        else if(maximoPeersPrimeraBusqueda >0 && dispositivoEncontradoEnSegundaFase){ /**CASO D*/
            casoD=true;
            saltarSegundaFase=true;
            misDatos.setSoyClusterHead(false);
        }
        else if(!this.hayUsuariosEncontrados){  /**CASO B*/
            casoB=true;
            saltarSegundaFase=false;
            misDatos.setSoyClusterHead(true);
        }
        else{    /**CASO C*/
            //Creamos el nombre k-CONID
            try {
                Thread.sleep(15000);  //ESPERAMOS 15 SEGUNDOS PARA NO CAMBIAR EL NOMBRE KCONID INMEDIATAMENTE
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            casoC=true;
            misDatos.resetearNumeroUsuariosAlcanzables();
            for (int i = 0; i < maximoPeersPrimeraBusqueda; i++) {
                misDatos.aniadirUsuarioAlcanzable();
            }
            misDatos.generarNumeroAleatorioNID();
            misDatos.setNombreKCONID(mActivity.nombreOriginalWiFiDirect);
            herramientasYanalizarWiFiDirectInfo.cambiarNombreWiFiDirect(misDatos.getNombreKCONID()); /*Establecemos el nombre k-CONID*/
            Log.d(TAG, "NOMBRE WiFi Direct, cambiado a nombre KCONID : " + misDatos.getNombreKCONID());
            saltarSegundaFase=false;
        }

        Log.d(TAG,"Caso A: "+casoA+" Caso B: "+casoB+" Caso C: "+casoC+" Caso D: "+casoD);
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
    }

    @Override
    protected synchronized void onCancelled() {
        super.onCancelled();
        progressDialog.dismiss();
        primeraFaseActiva=false;

    }

    @Override
    protected synchronized void onPostExecute(Void o) {
        super.onPostExecute(o);



        if(casoC==true) { //vamos a ir a la segunda fase
            Toast.makeText(mActivity.getApplicationContext(), "El nombre nuevo durante la segunda fase será: " + misDatos.getNombreKCONID(), Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(mActivity.getApplicationContext(), "CH encontrado, pasamos a 3º fase" + misDatos.getNombreKCONID(), Toast.LENGTH_SHORT).show();
        }
        progressDialog.dismiss();
        primeraFaseActiva=false;
        Log.d(TAG, "PRIMERA FASE : " + primeraFaseActiva);


    }

    @Override
    protected synchronized void
    onPreExecute() {
        super.onPreExecute();

        //Inizializamos el ProgressDialog
        this.progressDialog.setCancelable(false);
        this.progressDialog.setMessage("Iniciando Fase 1...");
        this.progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        this.progressDialog.setTitle("Primera Fase");

    }

    /**
     * Saber si la segunda Fase será descartada, mediante el boolean SaltarSegundaFase
     * @return boolean
     */
    public synchronized boolean getSaltarSegundaFase(){
        return saltarSegundaFase;
    }

    public synchronized boolean getCasoA(){
        return casoA;
    }

    public synchronized boolean getCasoB(){
        return casoB;
    }

    public synchronized boolean getCasoC() {
        return casoC;
    }

    public synchronized boolean getCasoD() {
        return casoD;
    }


}
