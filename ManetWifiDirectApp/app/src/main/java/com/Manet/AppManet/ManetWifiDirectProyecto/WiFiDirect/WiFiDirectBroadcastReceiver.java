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

package com.Manet.AppManet.ManetWifiDirectProyecto.WiFiDirect;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;

import android.util.Log;
import android.widget.Toast;


import com.Manet.AppManet.ManetWifiDirectProyecto.Activities.MainActivity;
import com.Manet.AppManet.ManetWifiDirectProyecto.Fases.PrimeraFase;
import com.Manet.AppManet.ManetWifiDirectProyecto.Fases.SegundaFase;
import com.Manet.AppManet.ManetWifiDirectProyecto.Fases.TerceraFase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver implements WifiP2pManager.ConnectionInfoListener,
        WifiP2pManager.ChannelListener, WifiP2pManager.GroupInfoListener{


    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private MainActivity mActivity;  //Objeto de la actividad principal
    private Context context;
    private String XMLinstancia;
    private String TAG="BR";
    private boolean nombreOriginalPrimeraVezSolo=true;


    public static int dispositivosEnlazados;

    public static boolean busquedaDispositivosActiva=false;

    private String direccion="";
    private String mNetworkName="";
    private String mPass="";

    private ArrayList arrayPeersPrimeraFase,arrayPeersSegundaFase;


    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       MainActivity activity, Context context) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
        this.context=context;
        this.dispositivosEnlazados=0;
    }

    public WiFiDirectBroadcastReceiver() {
    }

    /**
     * Con onReceive, recibimos los intent del sistema android, mediante la utilidad del
     * BroadCast Receiver.
     *
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(final Context context, Intent intent) {

        String actionIntent = intent.getAction();

        if(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(actionIntent)){


            int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED);
            String persTatu = "Discovery state changed to ";

            if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED){
                persTatu = persTatu + "Stopped.";
                if(busquedaDispositivosActiva==true) {
                    mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onFailure(int reason) {

                        }
                    });
                }

            }else if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED){
                persTatu = persTatu + "Started.";
            }else{
                persTatu = persTatu + "unknown  " + state;
            }
            Log.d(TAG,persTatu);

        }


        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(actionIntent)){
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);

            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                Log.d(TAG, "WiFi Direct está activado");
            }
            else if(state == WifiP2pManager.WIFI_P2P_STATE_DISABLED && !TerceraFase.terceraFaseActiva){ // Si el WiFi esta desactivado, y tercera fase inactiva(ya q esta puede reiniciar el wifi)
                                                                                        // salta un Dialog para sugerir al usuario que lo encienda.
                AlertDialog.Builder builder=new AlertDialog.Builder(context);
                builder.setTitle("Atención");
                builder.setMessage("Tienes que activar Wi-Fi, debido a que la herramienta principal " +
                        "de comunicación en esta app es el WiFi-Direct de tu SmartPhone");
                builder.setCancelable(false);
                builder.setPositiveButton("Encender", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                        wifiManager.setWifiEnabled(true);
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                builder.setNegativeButton("No quiero", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        System.exit(0);  //Si no quiere encender el WiFi cerramos el programa, que se lo piense para otra.
                    }
                });

                AlertDialog alertDialog=builder.create();
                alertDialog.show();

            }


        }

        /////////ACTUALIZACIÓN DE LA LISTA DE DISPOSITIVOS ALCANZABLES/////////////////////////////
        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(actionIntent)) {

            if(!TerceraFase.terceraFaseActiva && !PrimeraFase.primeraFaseBuscandoGrupos && busquedaDispositivosActiva) { //Si estamos en Tercera Fase no es necesario actualizar la lista.
                if (mManager != null) {
                    mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                        @Override
                        public void onPeersAvailable(WifiP2pDeviceList peers) {

                            Log.d(TAG, "\n\nLista de dispositivos --> \n" + peers.toString() + "\n\n");

                            ArrayList arrayPeers = new ArrayList();
                            arrayPeers.clear();
                            arrayPeers.addAll(peers.getDeviceList());


                            if (arrayPeers.size() != 0) {
                                if(PrimeraFase.primeraFaseActiva) {
                                    actualizarListaPeersEnPrimeraFase(arrayPeers);
                                }
                                if(SegundaFase.segundaFaseActiva){
                                    actualizarListaPeersEnSegundaFase(arrayPeers);
                                }
                            } else {
                                Log.d(TAG, "No se han encontrado vecinos");
                            }
                        }
                    });
                }
            }

            }


        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(actionIntent)) {


            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                Log.d(TAG, "Connection changed actionIntent, conectados");
                mManager.requestConnectionInfo(mChannel, (WifiP2pManager.ConnectionInfoListener) this);
                //Llama a OnConnectionInfoAvailable
            }else {
                Log.d(TAG, "Connection changed actionIntent, Desconectados");
            }
        }


        if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(actionIntent)){
            Log.d(TAG, "CONECTADOS WIFI MANAGER");
        }

        if(WifiManager.ACTION_PICK_WIFI_NETWORK.equals(actionIntent)){
            Log.d(TAG, "Action pick wifi network");
        }

        /***Cuando cambiamos el estado de WiFi Direct, nos mostrará el nombre***/
        if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(actionIntent)){

            WifiP2pDevice miMovil=intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            String thisDeviceName=miMovil.deviceName;

            if(nombreOriginalPrimeraVezSolo==true) {
                MainActivity.nombreOriginalWiFiDirect=thisDeviceName;
                Toast.makeText(context, "El nombre original de este dispositivo es: " + MainActivity.nombreOriginalWiFiDirect, Toast.LENGTH_LONG).show();
                nombreOriginalPrimeraVezSolo=false;
            }

        }



    }

    private void actualizarListaPeersEnPrimeraFase(ArrayList arrayPeers) {
        for (int i = 0; i <arrayPeers.size(); i++) {
            String informacionDispotivo = "";
            informacionDispotivo = arrayPeers.get(i).toString();
            if (!arrayPeersPrimeraFase.contains(arrayPeers.get(i))) {
                arrayPeersPrimeraFase.add(arrayPeers.get(i));
            }
        }
    }

    public void reiniciarArrayPeersPrimeraFase(){
        arrayPeersPrimeraFase=new ArrayList();
        arrayPeersPrimeraFase.clear();
    }

    public void agregarArrayPeersPrimeraFaseAMainActivity(){
        mActivity.agregarListaDispositivosEnPrimeraFase(arrayPeersPrimeraFase);
    }

    private void actualizarListaPeersEnSegundaFase(ArrayList arrayPeers) {
        for (int i = 0; i <arrayPeers.size(); i++) {
            String informacionDispotivo = "";
            informacionDispotivo = arrayPeers.get(i).toString();
            if (informacionDispotivo.contains("<C>") && !arrayPeersSegundaFase.contains(arrayPeers.get(i))) {
                arrayPeersSegundaFase.add(arrayPeers.get(i));
            }
        }
    }

    public void reiniciarArrayPeersSegundaFase(){
        arrayPeersSegundaFase=new ArrayList();
        arrayPeersSegundaFase.clear();
    }

    public void agregarArrayPeersSegundaFaseAMainActivity(){
        mActivity.agregarListaDispositivosEnSegundaFase(arrayPeersSegundaFase);
    }

    /**
     * Cuando existe información de un grupo existente, se llama a éste método automáticamente, de
     * otra manera, también es llamado cuando se llama a mManager.requestGroupinfo(mChannel,this);
     *
     * Es llamado cuando en MainActivity--> CrearGrupo ejecutamos mManager.createGroup.
     * @param group
     */
    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group){

        /**La primera vez que se entra a éste método es cuando se crea el grupo, posteriormente
         * se entra cuando un cliente se conecta -- ESTE MÉTODO SE EJECUTA EN EL CLUSTERHEAD */

        try {
            Collection<WifiP2pDevice> devlist = group.getClientList();

            int numm = 0;
            for (WifiP2pDevice peer : group.getClientList()) {  /**Mostramos los clientes del grupo*/
                numm++;
                Toast.makeText(context,"Client " + numm + " : " + peer.deviceName + " " + peer.deviceAddress,Toast.LENGTH_SHORT).show();
            }

            if (mNetworkName.equals(group.getNetworkName()) && mPass.equals(group.getPassphrase())) {
                //Ya sabemos la contraseña y nombre del grupo
            } else {
                /*WifiManager wifi=(WifiManager) ************/
                mNetworkName = group.getNetworkName();
                mPass = group.getPassphrase();
                XMLinstancia="<name>"+group.getNetworkName()+"</name>"+"<pass>"+group.getPassphrase()+"</pass>"+"<ipowner>"+direccion+"</ipowner>";
                Toast.makeText(context,XMLinstancia, Toast.LENGTH_SHORT).show();
                startLocalService(XMLinstancia);
            }
            }catch(Exception e) {
                Toast.makeText(context, "Excepción try devlist : "+e.toString(), Toast.LENGTH_SHORT).show();
            }
        }

    /**
     * Cuando cambia el estado de la conexión a conectado, este método es llamado.
     * @param info
     */
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {

        // After the group negotiation, we can determine the group owner.
        if (info.isGroupOwner) {
            direccion=info.groupOwnerAddress.getHostAddress();
            mManager.requestGroupInfo(mChannel,this);  // Se ha creado el grupo y queremos la información
                                                       // de éste.
        } else if (info.groupFormed) {
            Toast.makeText(context, "Soy cliente, la IP del CH es : " + direccion, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     *
     */
    @Override
    public void onChannelDisconnected() {

    }

    /**
     * Cuando vamos a crear un AP, en MainActivity hemos llamado a createGroup, posteriormente
     * autmáticamente es llamado onGroupInfoAvailable, y en este, al ver que el grupo aún no
     * existe, se llama a éste método que crea el Access-Point, con el fichero XML de contacto
     * (String instance), el cual se enviará a los dispositivos que busquen grupos.
     *
     * @param instance
     */
    private void startLocalService(String instance) {
        mManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG,"Servicios Locales eliminados");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG,"Fallo: Servicios Locales no eliminados");
            }
        });

        // Creamos un Hashmap que contiene informacion sobre nuestro servicio
        Map record = new HashMap();
        record.put("d","0");
        //record.put("a", "visible");

        Log.d(TAG,"Grupo creado, con instancia :"+instance);
        WifiP2pDnsSdServiceInfo servicio = WifiP2pDnsSdServiceInfo.newInstance( instance, "_wdm_p2p._tcp" , record);
                                                                        /**Sintaxis = _<protocolo>.<capaTransporte>**/

        synchronized(mManager) {
            mManager.addLocalService(mChannel, servicio, new WifiP2pManager.ActionListener() {
                public void onSuccess() {
                    synchronized (context) {
                        Toast.makeText(context, "Servicio local añadido", Toast.LENGTH_SHORT).show();
                    }
                }

                public void onFailure(int reason) {
                    synchronized (context) {
                        Toast.makeText(context, "Servicio local no añadido reason: " + reason, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    /**
     * Actualizamos el servicio local puesto que el número de dispositivos que tenemos enlazado
     * ha cambiado.
     * @param dispositivosEnlazados
     */
    public void actualizarServicioLocal(int dispositivosEnlazados){


        mManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Servicios Locales eliminados");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Fallo: Servicios Locales no eliminados");
            }
        });
        // Creamos un Hashmap que contiene informacion sobre nuestro servicio
        Map record = new HashMap();
        record.put("d", Integer.toString(dispositivosEnlazados));
        //record.put("a", "visible");


        WifiP2pDnsSdServiceInfo servicio = WifiP2pDnsSdServiceInfo.newInstance( XMLinstancia, "_wdm_p2p._tcp" , record);
                                                                              /**Sintaxis = _<protocolo>.<capaTransporte>**/

        synchronized(mManager) {
            mManager.addLocalService(mChannel, servicio, new WifiP2pManager.ActionListener() {
                public void onSuccess() {
                       Log.d(TAG,"Servicio Local Actualizado");
                }

                public void onFailure(int reason) {
                        Log.d(TAG, "Servicio Local no Actualizado");
                }
            });
        }
    }



    }








