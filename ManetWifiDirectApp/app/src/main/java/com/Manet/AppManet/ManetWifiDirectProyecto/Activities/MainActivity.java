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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.Manet.AppManet.ManetWifiDirectProyecto.Hebras.ConectarACH;
import com.Manet.AppManet.ManetWifiDirectProyecto.Datos.MisDatos;
import com.Manet.AppManet.ManetWifiDirectProyecto.Fases.PrimeraFase;
import com.Manet.AppManet.ManetWifiDirectProyecto.Fases.SegundaFase;
import com.Manet.AppManet.ManetWifiDirectProyecto.Fases.TerceraFase;
import com.Manet.AppManet.ManetWifiDirectProyecto.WiFiDirect.HerramientasYConfiguracionWiFiDirect;
import com.Manet.AppManet.ManetWifiDirectProyecto.R;
import com.Manet.AppManet.ManetWifiDirectProyecto.WiFiDirect.WiFiDirectBroadcastReceiver;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WiFiDirectBroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;

    /***Objetos creados de clases***/
    private HerramientasYConfiguracionWiFiDirect herramientasYanalizarWiFiDirectInfo;
    private MisDatos misDatos;
    private ConectarACH conectarACH;
    private AsyncTask hebraRXCliente;

    /***/

    /**Botones*/
    private Button botonFasesAutomaticas;
    private Button botonConfiguracion;
    private Button botonAyuda;
    private Button botonSalir;

    /***/

    /**Fases*/
    private PrimeraFase primeraFase;
    private SegundaFase segundaFase;
    private TerceraFase terceraFase;
    /***/

    private EditText editTextPuerto;

    public static String nombreOriginalWiFiDirect="";
    public static int puertoCH;
    public static Socket socketSoyCliente;
    public static Socket socketsDeMisClientes[];
    public static ServerSocket serverSocket;

    private ArrayList peers1Fase;
    private ArrayList peers2Fase;
    private int maximoPeers1Fase;
    private int maximoPeers2Fase;

    private String TAG="MainAct";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        botonFasesAutomaticas=(Button)findViewById(R.id.buttonFasesAutomatico);
        botonConfiguracion=(Button)findViewById(R.id.buttonConfiguracion);
        botonAyuda=(Button)findViewById(R.id.ButtonAyuda);
        botonSalir=(Button)findViewById(R.id.buttonCerrarAplicacion);

        /**PILARES BÁSICOS WIFI DIRECT*/
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel =  mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this, getApplicationContext());

        /**Inicialización de los objetos de las clases*/

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        /*****************************/


        botonFasesAutomaticas.setOnClickListener(new View.OnClickListener() {  /***POR AQUI VAMOSSSSSSSSSSSSSSSS KSEDFNSKAJFKSJAFVKADBGKAJBL****/
            @Override
            public void onClick(View v) {
                fasesAutomaticas();
            }
        });



        botonConfiguracion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(),ActivityConfiguracion.class);
                startActivity(intent);
            }
        });

        botonAyuda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(),ActivityAyuda.class);
                startActivity(intent);
            }
        });

        botonSalir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStop();
                onDestroy();
                System.exit(0);
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    /**
     *  unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    /**
     * Devulve la lista que se ha agregado del WifiDirectBroadCastReceiver
     * @return
     */
    public ArrayList getPeers1Fase(){
        return peers1Fase;
    }

    public ArrayList getPeers2Fase(){
        return peers2Fase;
    }

    /**
     * Devuelve el número maximo de vecinos que hemos encontrado en las listas agregadas
     * desde WifiDirectBroadCastReveiver
     * @return
     */
    public int getMaximoPeers1Fase(){
        return maximoPeers1Fase;
    }
    /**
     * Devuelve el número maximo de vecinos que hemos encontrado en las listas agregadas
     * desde WifiDirectBroadCastReveiver, en dispositivos en segunda fase
     * @return
     */
    public int getMaximoPeers2Fase(){
        return maximoPeers2Fase;
    }


    /**
    Sirve para agregar una copiar una lista arraylist a nuestro arraylist "peers1Fase".
     */
    public void agregarListaDispositivosEnPrimeraFase(ArrayList peers){
        this.peers1Fase =peers;
        if(peers.size()> maximoPeers1Fase){
            maximoPeers1Fase =peers.size();
        }
    }

    /**
     Sirve para agregar una copiar una lista arraylist a nuestro arraylist "peers2Fase".
     */
    public void agregarListaDispositivosEnSegundaFase(ArrayList peers){
        this.peers2Fase=peers;
        if(peers.size()>maximoPeers2Fase){
            maximoPeers2Fase=peers.size();
        }
    }







    /**
     * Mediante este método, llamaremos una a una a las fases con las que estableceremos la red
     */
    public void fasesAutomaticas(){
        Log.d(TAG,"Inicio de las fases automáticas");
        //Declaracion de las clases a usar
        socketsDeMisClientes=new Socket[128];
        peers1Fase =new ArrayList();
        peers2Fase=new ArrayList();
        maximoPeers1Fase =0;
        maximoPeers2Fase=0;
        misDatos=new MisDatos();

        try{
            puertoCH=ActivityConfiguracion.puerto;
        }catch (NullPointerException ex){
            puertoCH=7070;
        }
        try{
            misDatos.setSoyClusterHead(ActivityConfiguracion.CHforzado);
        }catch (NullPointerException ex){
            misDatos.setSoyClusterHead(false);
        }

        herramientasYanalizarWiFiDirectInfo=new HerramientasYConfiguracionWiFiDirect(mManager,mChannel,MainActivity.this,misDatos,mReceiver);

        primeraFase=new PrimeraFase(herramientasYanalizarWiFiDirectInfo, misDatos,MainActivity.this);
        segundaFase=new SegundaFase(herramientasYanalizarWiFiDirectInfo, misDatos,MainActivity.this,primeraFase);
        terceraFase=new TerceraFase(herramientasYanalizarWiFiDirectInfo, misDatos,MainActivity.this,conectarACH,primeraFase,hebraRXCliente);

        primeraFase.execute();
        segundaFase.execute();
        terceraFase.execute();

    }

    /**
     * Al cerrar el programa, cerraremos todos los sockets activos para que no se quede dicho puerto
     * bloqueado.
     */
    @Override
    protected void onStop() {
        super.onStop();
        try{
            herramientasYanalizarWiFiDirectInfo.cambiarNombreWiFiDirect(nombreOriginalWiFiDirect);
        }catch (NullPointerException ex){
            //Esta excepcion salta porque aún no se ha iniciado herramientasYanalizarWiFiDirectInfo
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {

            if(socketSoyCliente !=null) {
                socketSoyCliente.close();
            }
            if(serverSocket!=null) {
                serverSocket.close();
            }
            for(int i=0;i<socketsDeMisClientes.length;i++){
                if(socketsDeMisClientes[i]!=null){
                    socketsDeMisClientes[i].close();
                }
            }

            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Grupo apagado");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Grupo no apagado");
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }

//        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//        wifiManager.setWifiEnabled(false);


    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Exit")
                .setMessage("Vas a abandonar la aplicación, ¿Estás seguro?")
                .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                }).setNegativeButton("No", null).show();
    }
}
