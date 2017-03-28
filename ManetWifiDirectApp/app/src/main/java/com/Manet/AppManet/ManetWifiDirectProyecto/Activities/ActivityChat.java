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
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.Manet.AppManet.ManetWifiDirectProyecto.Datos.MisDatos;
import com.Manet.AppManet.ManetWifiDirectProyecto.Hebras.HebraTX;
import com.Manet.AppManet.ManetWifiDirectProyecto.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

public class ActivityChat extends Activity {


    public static String matrizChats[][]=new String[256][1000]; //Persona y conversacion
    private String copiaVectorMensajesChat[];
    public static Integer contador[]=new Integer[100000];

    public static int posicionPersona;
    private MisDatos misDatos;
    private String TAG="Chat";
    private String nombrePersonaSeleccionada="";

    private ListView listViewMensajesChat;
    private EditText editTextMensajeAEnviar,editTextNombrePersonaConLaQueHablamos;
    private Button botonEnviarMensaje;
    private Button botonEnviarFichero;
    private Button botonVerCloudCH;

    private Thread hebraActualizarListViewChat;

    private static final int FILE_SELECT_CODE = 0;

    public static boolean activityActiva;
    public static String IPAlOtroExtremo;
    private String IPAlOtroExtremoNotificacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        activityActiva=true;
        try{
            if(contador[0]==null){
                for(int i=0;i<contador.length;i++){
                    contador[i]=0;
                }
            }
        }catch (NullPointerException ex){
            for(int i=0;i<contador.length;i++){
                contador[i]=0;
            }
        }

        try {
            posicionPersona = (int) getIntent().getExtras().getSerializable("posicionPersona");
            IPAlOtroExtremo = (String) getIntent().getExtras().getSerializable("IPAlOtroExtremo");
            Log.d(TAG,"Ip al otro extremo oncreate es: "+IPAlOtroExtremo);
            misDatos = (MisDatos) getIntent().getExtras().getSerializable("misDatos");
            nombrePersonaSeleccionada = (String) getIntent().getExtras().getSerializable("NombrePersonaAlOtroExtremo");

            listViewMensajesChat=(ListView)findViewById(R.id.listViewMensajesChat);
            listViewMensajesChat.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
            editTextMensajeAEnviar =(EditText)findViewById(R.id.editTextMensajeAEnviar);
            editTextNombrePersonaConLaQueHablamos=(EditText)findViewById(R.id.editTextPersonName);
            editTextNombrePersonaConLaQueHablamos.setText(nombrePersonaSeleccionada);
            botonEnviarMensaje=(Button)findViewById(R.id.buttonEnviarMensaje);
            botonEnviarFichero=(Button)findViewById(R.id.buttonEnviarFichero);
            botonVerCloudCH=(Button)findViewById(R.id.buttonVerCloudCH);



            if(IPAlOtroExtremo.equals("192.168.49.1") || IPAlOtroExtremo.equals("0.0.0.0")){
                botonVerCloudCH.setVisibility(View.VISIBLE);
                botonEnviarFichero.setVisibility(View.VISIBLE);
            }else {
                botonVerCloudCH.setVisibility(View.GONE);
                botonEnviarFichero.setVisibility(View.GONE);
            }

        }catch (Exception ex){
            ex.printStackTrace();
        }

        hebraActualizarListViewChat=new Thread(){
            @Override
            public void run() {
                while(true){
                if(activityActiva==true) {
                    int longitudVector = contador[posicionPersona];
                    copiaVectorMensajesChat = new String[longitudVector];
                    for (int i = 0; i < longitudVector; i++) {
                        copiaVectorMensajesChat[i] = matrizChats[posicionPersona][i];
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            ArrayAdapter<String> adapter = new CustomAdapterMensaje(getApplicationContext(), copiaVectorMensajesChat);
                            listViewMensajesChat.setAdapter(adapter);
                            listViewMensajesChat.setSelection(copiaVectorMensajesChat.length - 1);

                        }
                    });

                    try {
                        Thread.sleep(1000);         //ACTUALIZAMOS LA LISTA CADA 5 SEGUNDOS
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                }
            }

        };

        hebraActualizarListViewChat.start();

        botonEnviarMensaje.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String mensaje = "";
                String IPorig = "";
                if (misDatos.getSoyClusterHead()) {
                    IPorig = "192.168.49.1"; //Con WifiManager nos daria la 0.0.0.0
                } else {
                    IPorig = obtenerMiIpConWifiManager();
                }

                String textoEscrito = editTextMensajeAEnviar.getText().toString() + " ";
                mensaje = "<Reenvio>" + "<IPorig>" + IPorig + "</IPorig>" + "<NombreOrigen>" + MainActivity.nombreOriginalWiFiDirect +
                        "</NombreOrigen>" + "<IPdest>" + IPAlOtroExtremo + "</IPdest>" + "<Mensaje>" + textoEscrito
                        + "</Mensaje>" + "</Reenvio>";

                Log.d(TAG, "Mensaje a: " + IPAlOtroExtremo);

                if (misDatos.getSoyClusterHead()) {
                    for (int i = 0; i < MainActivity.socketsDeMisClientes.length; i++) {

                        try {
                            String ipRemota = MainActivity.socketsDeMisClientes[i].getInetAddress().getHostAddress();
                            if (ipRemota.equals(IPAlOtroExtremo)) {
                                AsyncTask hebraTXCliente = new HebraTX(getApplicationContext(), MainActivity.socketsDeMisClientes[i], mensaje, misDatos);
                                hebraTXCliente.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }
                        } catch (NullPointerException ex) {
                        }

                    }
                } else {
                    AsyncTask hebraTXCliente = new HebraTX(getApplicationContext(), MainActivity.socketSoyCliente, mensaje, misDatos);
                    hebraTXCliente.executeOnExecutor(THREAD_POOL_EXECUTOR);
                }

                aniadirMensajeAvectorMensajesChat(posicionPersona, MainActivity.nombreOriginalWiFiDirect + ": " + textoEscrito);
                editTextMensajeAEnviar.setText("");
            }
        });

        botonEnviarFichero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Enviar archivo a: " + nombrePersonaSeleccionada);
                showFileChooser();
            }
        });

        botonVerCloudCH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(getApplicationContext(), ActivityListViewCloudClusterhead.class); //Pasamos a la ventana del listView
                //intent.putExtra("posicionPersona", position);
                //intent.putExtra("IPAlOtroExtremo",clienteIP[position]);
                //intent.putExtra("NombrePersonaAlOtroExtremo",clienteNombreAMostrar[position]);
                intent.putExtra("misDatos",misDatos);
                startActivity(intent);
            }
        });



    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "New intent");
        try {
            posicionPersona = (int) getIntent().getExtras().getSerializable("posicionPersona");
            IPAlOtroExtremo = (String) getIntent().getExtras().getSerializable("IPAlOtroExtremo");
            misDatos = (MisDatos) getIntent().getExtras().getSerializable("misDatos");
            nombrePersonaSeleccionada = (String) getIntent().getExtras().getSerializable("NombrePersonaAlOtroExtremo");
            Log.d(TAG,"IP al otro extremo es: "+IPAlOtroExtremo);
            Log.d(TAG,"Nombre al otro extremod es: "+nombrePersonaSeleccionada);

        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity_chat, menu);
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

    /**
     * Añadimos un mensaje recibido, a la posición en el vector matrizChats de la persona que nos
     * lo ha enviado
     * @param posicionPersona
     * @param mensaje
     */
    public static void aniadirMensajeAvectorMensajesChat(int posicionPersona,String mensaje){
        try{
            matrizChats[posicionPersona][contador[posicionPersona]]=mensaje;
            contador[posicionPersona]++;
        }catch (NullPointerException ex){
            for(int i=0;i<contador.length;i++){
                contador[i]=0;
            }
            matrizChats[posicionPersona][contador[posicionPersona]]=mensaje;
            contador[posicionPersona]++;
        }



    }

    @Override
    protected void onResume() {
        super.onResume();
        activityActiva=true;

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        try{
            notificationManager.cancel(posicionPersona);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        ActivityListViewUsuarios.mensajeNoLeido[posicionPersona]=false;


    }

    @Override
    protected void onPause() {
        super.onPause();
        activityActiva=false;

    }

    /**
     * En el caso de estar conectados a un Clusterhead (AP), podemos obtener nuestra IP de cliente
     * mediante éste metodo
     */
    public String obtenerMiIpConWifiManager(){

        String stringip;

        WifiManager wifiMgr = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        stringip = android.text.format.Formatter.formatIpAddress(ip);

        return stringip;
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Selecciona el archivo a enviar"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Has de instalar un explorador de ficheros", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==FILE_SELECT_CODE && resultCode==Activity.RESULT_OK) {
            Boolean elArchivoExiste=true;
            // Get the Uri of the selected file
            final Uri uri = data.getData();
            Log.d(TAG, "URI del archivo a enviar: " + uri.toString());

            try{
                Cursor returnCursor = getContentResolver().query(uri, null, null, null, null);
                int nombreIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                returnCursor.moveToFirst();
                final String nombre = returnCursor.getString(nombreIndex);
                final String tamano = Long.toString(returnCursor.getLong(sizeIndex));

                Log.d(TAG, "Nombre del archivo a enviar: " + nombre);


                try {
                    BufferedInputStream inputStream = new BufferedInputStream(getContentResolver().openInputStream(uri));
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "El archivo no existe");
                    elArchivoExiste = false;
                    e.printStackTrace();
                }
                if (elArchivoExiste) {
                    Log.d(TAG, "El archivo si existe");
                    AlertDialog.Builder builder = new AlertDialog.Builder(ActivityChat.this);
                    builder.setIcon(android.R.drawable.ic_dialog_info);
                    builder.setTitle("Enviar Archivo");
                    builder.setMessage("¿Enviar el archivo: " + nombre + " con tamaño: " + tamano + "(bytes) a: " + nombrePersonaSeleccionada + "?");
                    builder.setCancelable(false);
                    builder.setPositiveButton("Enviar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            String IPorig = "";
                            if (misDatos.getSoyClusterHead()) {
                                IPorig = "192.168.49.1"; //Con WifiManager nos daria la 0.0.0.0
                            } else {
                                IPorig = obtenerMiIpConWifiManager();
                            }

                            if (misDatos.getSoyClusterHead()) {
                                for (int i = 0; i < MainActivity.socketsDeMisClientes.length; i++) {

                                    try {
                                        String ipRemota = MainActivity.socketsDeMisClientes[i].getInetAddress().getHostAddress();
                                        if (ipRemota.equals(IPAlOtroExtremo)) {
                                            AsyncTask hebraTXClusterHead = new HebraTX(getApplicationContext(), MainActivity.socketsDeMisClientes[i], "<SolicitudEnviarArchivo><NombreOrigen>" + "ClusterHead" + MainActivity.nombreOriginalWiFiDirect + "</NombreOrigen>"
                                                    + "<IPorig>" + IPorig + "</IPorig>"+"</SolicitudEnviarArchivo>", misDatos);
                                            hebraTXClusterHead.executeOnExecutor(THREAD_POOL_EXECUTOR);
                                            //Iniciamos la hebra para enviarlo
                                            try {
                                                Thread.sleep(4000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }

                                            AsyncTask hebraTXClusterheadArchivo = null;
                                            try {
                                                hebraTXClusterheadArchivo = new HebraTX(getApplicationContext(), MainActivity.socketsDeMisClientes[i], misDatos, 1, getContentResolver().openInputStream(uri), nombre, tamano);
                                            } catch (FileNotFoundException e) {
                                                e.printStackTrace();
                                            }
                                            hebraTXClusterheadArchivo.executeOnExecutor(THREAD_POOL_EXECUTOR);
                                        }
                                    } catch (NullPointerException ex) {
                                    }

                                }
                            } else {
                                AsyncTask hebraTXCliente = new HebraTX(getApplicationContext(), MainActivity.socketSoyCliente, "<SolicitudEnviarArchivo><NombreOrigen>" + MainActivity.nombreOriginalWiFiDirect + "</NombreOrigen>"
                                        + "<IPorig>" + IPorig + "</IPorig>", misDatos);
                                hebraTXCliente.executeOnExecutor(THREAD_POOL_EXECUTOR);
                                //Iniciamos la hebra para enviarlo
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                AsyncTask hebraTXClienteArchivo = null;
                                try {
                                    hebraTXClienteArchivo = new HebraTX(getApplicationContext(), MainActivity.socketSoyCliente, misDatos, 1, getContentResolver().openInputStream(uri), nombre, tamano);
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                                hebraTXClienteArchivo.executeOnExecutor(THREAD_POOL_EXECUTOR);
                            }


                        }
                    });
                    builder.setNegativeButton("No quiero", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //No hacemos nada
                        }
                    });

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            }catch (NullPointerException ex){
                ex.printStackTrace();
                Toast.makeText(getApplicationContext(),"No hay permisos para enviar dicho archivo",Toast.LENGTH_SHORT).show();
            }


        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
