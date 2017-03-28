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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.Manet.AppManet.ManetWifiDirectProyecto.Activities.MainActivity;
import com.Manet.AppManet.ManetWifiDirectProyecto.Datos.MisDatos;
import com.Manet.AppManet.ManetWifiDirectProyecto.Fases.PrimeraFase;
import com.Manet.AppManet.ManetWifiDirectProyecto.Fases.SegundaFase;
import com.Manet.AppManet.ManetWifiDirectProyecto.Fases.TerceraFase;
import com.Manet.AppManet.ManetWifiDirectProyecto.Hebras.ServidorCH;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Esta clase contiene los diferentes métodos necesarios para configurar nuestra red WiFi Direct,
 * tales como crear un Grupo, buscar los vecinos existentes, obtener la PASS de un fichero XML
 * recibido, cambiar nuestro nombre Wi-Fi Direct, etcétera.
 */
public class HerramientasYConfiguracionWiFiDirect implements Serializable {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private MainActivity mActivity;

    private String SSID[]=new String[256];
    private String PASS[]=new String[256];/*SSID y PASS del punto de acceso encontrado al ejecutar el método buscar puntos de acceso*/
    private String ipOwner[]=new String[256];
    private String dispositivosEnlazados[]=new String[256];

    private String dispositivosEnlazadosRecibidoEnRecord[]=new String[256];
    private String SSIDRecibidoEnRecord[]=new String[256];
    private int contadorLecturasEnRecord=0;


    private int numeroCHEncontrados;
    private AsyncTask servidorCH;
    private MisDatos misDatos;

    private WifiManager wifiManager;
    private WifiConfiguration configuracion;
    private WiFiDirectBroadcastReceiver mReceiver;
    private String TAG="HyCWD";



    public HerramientasYConfiguracionWiFiDirect(WifiP2pManager mManager, WifiP2pManager.Channel mChannel,
                                                MainActivity mActivity, MisDatos misDatos,WiFiDirectBroadcastReceiver mReceiver){
        this.mManager=mManager;
        this.mChannel=mChannel;
        this.mReceiver=mReceiver;
        this.mActivity=mActivity;
        this.numeroCHEncontrados =0;
        this.misDatos=misDatos;
    }

    /**
     * Mediante éste método creamos un grupo WiFi-Direct, es decir, un AP( Access-Point ) Wifi-Direct,
     * con seguridad WPA2. Dicho grupo será creado por el elegido ClusterHead.
     */
    public void crearGrupo(){
        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(mActivity, "SOY CH, grupo creado", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "SOY CH, grupo creado");
                //Si ha ido bien, se llama a OnGroupInfoAvailable(BroadcastReceiver), y si no se ha
                //creado el servicio loca, este llamará al método
            }

            @Override
            public void onFailure(int reason) {
                //reason = 0 error interno
                //reason = 1 no se soporta wifi direct en el dispositivo
                //reason = 2 servicio ocupado

                if (reason == 2) {
                    Toast.makeText(mActivity, "El servicio ya estaba creado anteriormente", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mActivity, "Fallo, reason: " + reason, Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    /**
     * Llamará al método actualizarServicioLocal ubicado en WiFiDirectBroadcastReceiver
     * @param dispositivosEnlazados
     */
    public void pasarelaActualizarServicioLocal(int dispositivosEnlazados){
       synchronized (mReceiver) {
           mReceiver.actualizarServicioLocal(dispositivosEnlazados);
       }
    }

    /**
     *
     */
    public void encenderServidorCH(){
        servidorCH=new ServidorCH(mActivity,misDatos,this);
        servidorCH.execute();
    }

    /**
     Con éste método llamamos a discoverPeers, que realizará la actualizacion de los dispositivos
     cercanos con WiFi-Direct. !!Realmente, los vecinos se actualizan aunque no realizemos esta accion!!!
     */
    public void buscarVecinos(){

        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                WiFiDirectBroadcastReceiver.busquedaDispositivosActiva = true;
                Log.d(TAG, "Iniciada la búsqueda de vecinos");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Fallo al iniciar la búsqueda de vecinos");
            }
        });
        if(PrimeraFase.primeraFaseActiva){
            mReceiver.reiniciarArrayPeersPrimeraFase();
        }
        if(SegundaFase.segundaFaseActiva){
            mReceiver.reiniciarArrayPeersSegundaFase();
        }
    }

    /**
     * Puesto que hemos terminado la búsqueda de vecinos, llamamos a éste metodo para pausar el
     * servicio discovery
     */
    public void pararBusquedaVecinos(){
        mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                WiFiDirectBroadcastReceiver.busquedaDispositivosActiva=false;
                Log.d(TAG, "Parada la búsqueda de vecinos");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Fallo al parar la búsqueda de vecinos");
            }
        });
        if(SegundaFase.segundaFaseActiva){
            mReceiver.agregarArrayPeersSegundaFaseAMainActivity();
        }
        if(PrimeraFase.primeraFaseActiva){
            mReceiver.agregarArrayPeersPrimeraFaseAMainActivity();
        }
    }

    /**
     * Mediante éste método, activamos setDnsSdResponseListeners, añadimos addServiceRequest,
     * discoverServices, ... Mediante éstos, recibimos los AP
     * encontrados (ClusterHeads), para guardar la información sobre éstos (SSID, PASS, IP) y
     * posteriormente conectarnos a ellos.
     *
     * ATENCIÓN: Además de buscar los grupos, es necesarior llamar a este método para que un
     * AP emita su información de contacto al exterior (SSID,PASS e IP).
     */
    public void buscarGrupos() {
        if (PrimeraFase.primeraFaseActiva || TerceraFase.terceraFaseActiva) { //Solo se buscan en 1º y 3º fase

            mManager.setDnsSdResponseListeners(mChannel, new WifiP2pManager.DnsSdServiceResponseListener() {
                @Override
                public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
                //Hemos encontrado un grupo WiFiDirect


                    boolean elCHYaEstaAniadido = false;
                    Log.d(TAG, "Grupo Encontrado: " + obtenerSSIDdeXMLinstancia(instanceName));

                    if (numeroCHEncontrados > 0) {
                        for (int i = 0; i < numeroCHEncontrados; i++) {
                            if (SSID[i].equals(obtenerSSIDdeXMLinstancia(instanceName))) {
                                elCHYaEstaAniadido = true;
                                Log.d(TAG, "Grupos en memoria " + SSID[i]);
                            }
                        }
                        if (!elCHYaEstaAniadido) { //Si anteriormente no estaba añadido, lo añadimos
                            SSID[numeroCHEncontrados] = obtenerSSIDdeXMLinstancia(instanceName);
                            PASS[numeroCHEncontrados] = obtenerPASSdeXMLinstancia(instanceName);
                            ipOwner[numeroCHEncontrados] = obtenerIPdeXMLinstancia(instanceName);
                            numeroCHEncontrados++;
                            Log.d(TAG, "Grupo Encontrado: " + obtenerSSIDdeXMLinstancia(instanceName) + " guardado correctamente");
                        } else {
                            Log.d(TAG, "Grupo encontrado: " + obtenerSSIDdeXMLinstancia(instanceName) + " ya estaba guardado anteriormente");
                        }


                    }
                    if (numeroCHEncontrados == 0) {

                        SSID[0] = obtenerSSIDdeXMLinstancia(instanceName);
                        PASS[0] = obtenerPASSdeXMLinstancia(instanceName);
                        ipOwner[0] = obtenerIPdeXMLinstancia(instanceName);


                        numeroCHEncontrados++;
                        Log.d(TAG, "Grupo Encontrado: " + obtenerSSIDdeXMLinstancia(instanceName) + " guardado correctamente");
                    }

                }
            }, new WifiP2pManager.DnsSdTxtRecordListener() {
                @Override
                public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
                    Log.d(TAG, "Record: " + fullDomainName + " Dispositivos enlazados: " + txtRecordMap.get("d"));

                    String dispEnlazados = txtRecordMap.get("d");

                    SSIDRecibidoEnRecord[contadorLecturasEnRecord] = obtenerSSIDdeXMLinstancia(fullDomainName);
                    dispositivosEnlazadosRecibidoEnRecord[contadorLecturasEnRecord] = dispEnlazados;
                    contadorLecturasEnRecord++;

                    for (int i = 0; i < contadorLecturasEnRecord; i++) {
                        for (int j = 0; j < numeroCHEncontrados; j++) {
                            if (SSIDRecibidoEnRecord[i].equals(SSID[j].toLowerCase())) {
                                dispositivosEnlazados[j] = dispositivosEnlazadosRecibidoEnRecord[i];
                                Log.d(TAG, "IP: " + ipOwner[j] + " SSID: " + SSID[j] + " PASS: " + PASS[j] + " Dispositivos Enlazados: " + dispositivosEnlazados[j]);
                                synchronized (mActivity) {
                                        Toast.makeText(mActivity, "IP: " + ipOwner[j] + " SSID: " + SSID[j] + " PASS: " + PASS[j] + " Dispositivos Enlazados: " + dispositivosEnlazados[j], Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }

                }
            });

            WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

            mManager.addServiceRequest(mChannel, serviceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Servicio Discovery Request añadido");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Servicio Discovery Request NO añadido, reason: "+reason);
                }
            });

            mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(mActivity, "Iniciando descubrimiento de servicios", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Búsqueda de grupos iniciada");
                }

                @Override
                public void onFailure(int reason) {
                    Toast.makeText(mActivity, "Fallo al iniciar descubrimiento de servicios", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Fallo al iniciar la búsqueda de grupos, reason: "+reason);
                }
            });
        }
    }

    /**
     * Puesto que la búsqueda se ha completado, ya no es necesario continuar con el servicio request
     * activado, por ello llamamos a éste metodo
     */
    public void pararBusquedaGrupos(){
        mManager.clearServiceRequests(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Búsqueda de grupos parada");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Búsqueda de grupos NO parada, reason: "+reason);
            }
        });
    }

    /**
     * Este metodo nos devuelve el nº de grupos encontrados
     * @return int NºgruposEncontrados
     */
    public int getNumeroCHEncontrados(){
        return numeroCHEncontrados;
    }

    /**
     * Este método devolverá la posición del vector dispositivosEnlazadosRecibidoEnRecord[], en la cual se encuentra
     * el SSID del grupo con más dispositivos enlazados.
     * Devolverá valor -1 si aún no hemos recibido los dispositivos enlazados de un grupo.
     * @return
     */
    public int determinarCHalQueConectar(){
        int posicionEnVectorDelGrupoConElNumeroMaximoDeDispositivosEnlazados=0;
        int maximoDispositivosEnlazados=0;

        try {

            for (int i = 0; i < numeroCHEncontrados; i++) {
                if (Integer.parseInt(dispositivosEnlazadosRecibidoEnRecord[i]) >= maximoDispositivosEnlazados) {
                    maximoDispositivosEnlazados = Integer.parseInt(dispositivosEnlazadosRecibidoEnRecord[i]);
                    posicionEnVectorDelGrupoConElNumeroMaximoDeDispositivosEnlazados = i;
                }
            }

        }catch (Exception ex){
            Log.d(TAG,"Excepcion, no hemos recibido el numero de dispositivos enlazados para ejecutar" +
                    "el método determinarCHalQueConectar");
            posicionEnVectorDelGrupoConElNumeroMaximoDeDispositivosEnlazados=-1;
        }
        Log.d(TAG,"El grupo con el máximo número de dispositivos enlazados es: "+SSID[posicionEnVectorDelGrupoConElNumeroMaximoDeDispositivosEnlazados]);
        return posicionEnVectorDelGrupoConElNumeroMaximoDeDispositivosEnlazados;
    }

    /**
     * Una vez hemos buscado los ClusterHeads existentes (APs), nos conectamos al necesario,
     * introduciendo sus datos de contacto (SSID, PASS e IP), así como el nº del grupo al que nos vamos
     * a conectar.
     *
     */
    public void guardarAPwifiDirect(int numeroGrupo){


        if(SSID[numeroGrupo]!=null && PASS[numeroGrupo]!="") {
            Log.d(TAG, "Método para guardar el AP óptimo encontrado en memoria");

            wifiManager = (WifiManager) mActivity.getSystemService(Context.WIFI_SERVICE);
            configuracion = new WifiConfiguration();

            configuracion.SSID = "\"" + SSID[numeroGrupo] + "\"";
            configuracion.preSharedKey = "\"" + PASS[numeroGrupo] + "\""; /**WPA2*/

            configuracion.hiddenSSID = false;
            configuracion.status = WifiConfiguration.Status.ENABLED;

            configuracion.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            configuracion.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            configuracion.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            configuracion.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            configuracion.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            configuracion.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            configuracion.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            configuracion.allowedProtocols.set(WifiConfiguration.Protocol.RSN);


            }
    }

    public synchronized boolean conectarApGuardadoWiFi(int numeroGrupo){
        boolean conectado=false;

        try {
            int r = wifiManager.addNetwork(configuracion);
            wifiManager.enableNetwork(r, true);
            wifiManager.saveConfiguration();
            boolean rec = wifiManager.reconnect();

            ConnectivityManager connManager = (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (mWifi.isConnected()) {
                conectado = true;
            }

            Log.d(TAG,"Conectar al AP: " + SSID[numeroGrupo] + " Con IP: " + ipOwner[numeroGrupo] + " boolean : " + conectado);
        }catch (NullPointerException ex){
            Log.d(TAG, "Aún no hay ningún AP óptimo guardado");
        }
        return conectado;
    }

    /**
     * Podemos obtener nuestra IP
     * mediante éste metodo
     */
    public String obtenerMiIpConWifiManager(){

        String stringip;

        WifiManager wifiMgr = (WifiManager)mActivity.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        stringip = android.text.format.Formatter.formatIpAddress(ip);

        return stringip;
    }

    /**
     * Mediante éste metodo, recibimos los datos de contacto de un AP encontrado (fichero XML),
     * y de éste extraemos un SSID, el cual devolvemos como return.
     *
     * @param instancia
     * @return SSIDXML
     */
    public String obtenerSSIDdeXMLinstancia(String instancia){
        /**Obtener el SSID*/
        String nSSID;

        int posInicio=0;
        int posFinal=0;

        for(int i=0;i<instancia.length();i++) {

            if (instancia.substring(i,i+"<name>".length()).equals("<name>")) {
                posInicio=i+"<name>".length();
            }
            if(instancia.substring(i,i+"</name>".length()).equals("</name>")){
                posFinal=i;
                break;
            }
        }
        nSSID=instancia.substring(posInicio,posFinal);

        return nSSID;
        /****************/
    }

    /**
     * Mediante éste metodo, recibimos los datos de contacto de un AP encontrado (fichero XML),
     * y de éste extraemos un PASS, el cual devolvemos como return.
     *
     * @param instancia
     * @return PASSXML
     */
    public String obtenerPASSdeXMLinstancia(String instancia){
        /**Obtener la PASS*/
        String nPASS;

        int posInicio=0;
        int posFinal=0;

        for(int i=0;i<instancia.length();i++) {

            if (instancia.substring(i,i+"<pass>".length()).equals("<pass>")) {
                posInicio=i+"<pass>".length();
            }
            if(instancia.substring(i,i+"</pass>".length()).equals("</pass>")){
                posFinal=i;
                break;
            }
        }
        nPASS=instancia.substring(posInicio,posFinal);

        return nPASS;
        /****************/
    }

    /**
     * Mediante éste metodo, recibimos los datos de contacto de un AP encontrado (fichero XML),
     * y de éste extraemos una IP, el cual devolvemos como return, suele ser 192.168.49.1.
     *
     * @param instancia
     * @return IPXML
     */
    public String obtenerIPdeXMLinstancia(String instancia){
        /**Obtener la IP*/
        String nIP;

        int posInicio=0;
        int posFinal=0;

        for(int i=0;i<instancia.length();i++) {

            if (instancia.substring(i,i+"<ipowner>".length()).equals("<ipowner>")) {
                posInicio=i+"<ipowner>".length();
            }
            if(instancia.substring(i,i+"</ipowner>".length()).equals("</ipowner>")){
                posFinal=i;
                break;
            }
        }
        nIP=instancia.substring(posInicio,posFinal);

        return nIP;
        /****************/
    }

    /**
     * Mediante éste método cambiamos el nombre con el que se identifica un dispositivo WiFi-Direct.
     * Introducimos como entrada un String el cual será el nuevo nombre.
     *
     * 33 caracteres Maximo
     *
     * @param nuevoNombre
     */
    public void cambiarNombreWiFiDirect(final String nuevoNombre){

        Method m = null;
        try {
            m = mManager.getClass().getMethod("setDeviceName", new Class[]{mChannel.getClass(), String.class, WifiP2pManager.ActionListener.class});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        try {
            if (m != null) {
                m.invoke(mManager, mChannel,nuevoNombre, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Nombre cambiado a : " + nuevoNombre);
                    }
                    @Override
                    public void onFailure(int reason) {
                        Log.d(TAG, "No se ha podido cambiar el nombre WiFi Direct");
                    }
                });
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    /**
     * Obtenemos el SSID, en la posición "n" del vector SSID
     * @param n
     * @return String SSID
     */
    public String getSSID(int n){
        return SSID[n];
    }

    /**
     * Obtenemos la IP, en la posición "n" del vector ipOwner
     * @param n
     * @return String IP
     */
    public String getIP(int n){
        return ipOwner[n];
    }

}
