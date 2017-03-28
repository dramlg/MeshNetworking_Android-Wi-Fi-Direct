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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.Manet.AppManet.ManetWifiDirectProyecto.Datos.MisDatos;
import com.Manet.AppManet.ManetWifiDirectProyecto.Hebras.HebraRX;
import com.Manet.AppManet.ManetWifiDirectProyecto.Hebras.HebraTX;
import com.Manet.AppManet.ManetWifiDirectProyecto.R;

public class ActivityListViewCloudClusterhead extends Activity {

    private String TAG="LVCloud";
    private MisDatos misDatos;
    private Thread hebraActualizarArchivosCloud;
    private int milisegundosEsperaThread=10000;

    private ListView listViewArchivosCloud;

    private String vectorNombresArchivos[];
    private String vectorTamanosArchivos[];
    private String copiaVectorNombresArchivos[];
    private String copiaVectorTamanosArchivos[];
    private int contadorVectores=0;

    private String listaArchivosString;
    private int tamanioListaArchivos;
    private boolean cargando=true;

    public static boolean activityActiva;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view_cloud_clusterhead);
        activityActiva=true;
        cargando=true;
        try{
            misDatos = (MisDatos) getIntent().getExtras().getSerializable("misDatos");
        }catch (Exception ex){
            ex.printStackTrace();
        }

        new AsyncTask<Object,String,Void>() {
            //< entrada en inbackgroud, parametro onprogressupdate, retornoinbackground //

            ProgressDialog progressDialog=null;
            @Override
            protected Void doInBackground(Object... params) {
                publishProgress("SolicitarLista");
                Log.d(TAG,"Solicitamos lista");
                try {
                    Thread.sleep(milisegundosEsperaThread/2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                publishProgress("ActualizarLista");
                try {
                    Thread.sleep(milisegundosEsperaThread);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(milisegundosEsperaThread==10000) {
                    try {
                        Thread.sleep(milisegundosEsperaThread/2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    publishProgress("ActualizarLista");
                }
                progressDialog.dismiss();

                return null;
            }

            @Override
            protected void onProgressUpdate(String... texto) {
                super.onProgressUpdate(texto);
                String palabra="";
                for(int i=0;i<texto.length;i++){
                    palabra=palabra+texto[i];
                }
                if(palabra.equals("SolicitarLista")){
                    solicitarListaArchivos();
                }
                else if(palabra.equals("ActualizarLista")){
                    actualizarListaArchivosCloud();
                }

            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog = new ProgressDialog(ActivityListViewCloudClusterhead.this);
                progressDialog.setMessage("Cargando...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setCancelable(false);
                progressDialog.setTitle("Cloud");
                progressDialog.show();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                progressDialog.dismiss();
                cargando=false;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


        listViewArchivosCloud=(ListView)findViewById(R.id.listViewArchivosCloud);
        hebraActualizarArchivosCloud =new Thread(){
            @Override
            public void run() {
                while(true){
                    if(activityActiva==true && cargando==false) { //Mientras se carga no actualizamos nada

                        try {
                            Thread.sleep(milisegundosEsperaThread);         //ACTUALIZAMOS LA LISTA CADA 5 SEGUNDOS
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //Generar vectores con algun metodo del string recibido
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                solicitarListaArchivos();
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                actualizarListaArchivosCloud();
                            }
                        });


                    }
                }
            }

        };

        hebraActualizarArchivosCloud.start();

        //Solicitar 2 vectores al CH, de archivos, un vector de nombre de archivos y otro de tamaños,
        //solicitamos el string, y sera un static en HebraRX que procesaremos aquí para sacar los
        //vectores, los cuales mostraremos.

        listViewArchivosCloud.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Enviamos la petición de descarga del archivo
                final int posicion=position;
                AlertDialog.Builder builder=new AlertDialog.Builder(ActivityListViewCloudClusterhead.this);
                builder.setTitle("Descarga de fichero");
                builder.setMessage("¿Proceder a la descarga del fichero: " + copiaVectorNombresArchivos[position] + ", con tamaño: " + copiaVectorTamanosArchivos[position] + " kBytes ?");
                builder.setCancelable(false);
                builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String solicitud="";

                        solicitud+="<SolicitudArchivoEnCloud>";
                        solicitud+="<NombreArchivo>"+copiaVectorNombresArchivos[posicion]+"</NombreArchivo>";
                        solicitud+="</SolicitudArchivoEnCloud>";

                        AsyncTask hebraTXCliente = new HebraTX(getApplicationContext(),MainActivity.socketSoyCliente,solicitud,misDatos);
                        hebraTXCliente.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                AlertDialog alertDialog=builder.create();
                alertDialog.show();



            }
        });


    }

    /**
     * Actualiza nuestra lista de archivos actual
     */
    public void actualizarListaArchivosCloud(){
        listaArchivosString="";
        tamanioListaArchivos=0;
        contadorVectores=0;
        vectorNombresArchivos=new String[300];
        vectorTamanosArchivos=new String[300];

        listaArchivosString= HebraRX.listaArchivosString;
        if(listaArchivosString!=""){
            tamanioListaArchivos=obtenerTamanoDeListaDeArchivos(listaArchivosString);
            Log.d(TAG,"Tamaño lista : "+tamanioListaArchivos);
            if(tamanioListaArchivos>0) {
                procesarListaArchivosRecibida(listaArchivosString);

                copiaVectorNombresArchivos = new String[contadorVectores];
                copiaVectorTamanosArchivos = new String[contadorVectores];

                for (int i = 0; i < contadorVectores; i++) {
                    copiaVectorNombresArchivos[i] = vectorNombresArchivos[i];
                    copiaVectorTamanosArchivos[i] = vectorTamanosArchivos[i];
                }
                ArrayAdapter<String> adapter = new CustomAdapterListViewCloudCH(getApplicationContext(), copiaVectorNombresArchivos, copiaVectorTamanosArchivos);
                listViewArchivosCloud.setAdapter(adapter);
                milisegundosEsperaThread = 60000; //Una vez hemos establecido una lista, pasamos a 30 segundos de espera hasta refrescarla
            }

        }
    }

    public void solicitarListaArchivos(){
        AsyncTask hebraTXCliente = new HebraTX(getApplicationContext(), MainActivity.socketSoyCliente,"<ObtenerListaArchivos>",misDatos);
        hebraTXCliente.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        Log.d(TAG, "Solicitar Lista de archivos del cloud del CH");
    }

    /**
     * Obtener el int tamaño del mensaje.
     *
     * @param mensaje
     * @return String nombre
     */
    public int obtenerTamanoDeListaDeArchivos(String mensaje){

        int tamanio=0;
        int posInicio=0;
        int posFinal=0;

        for(int i=0;i<mensaje.length();i++) {

            if (mensaje.substring(i,i+"<Tamanio>".length()).equals("<Tamanio>")) {
                posInicio=i+"<Tamanio>".length();
            }
            if(mensaje.substring(i,i+"</Tamanio>".length()).equals("</Tamanio>")){
                posFinal=i;
                break;
            }
        }
        tamanio=Integer.parseInt(mensaje.substring(posInicio, posFinal));
        return tamanio;

    }

    /**
     * Procesa la lista estática de archivos que hay en HebraRX, que previamente
     * hemos solicitado mediante el metodo solicitarListaArchivos.
     *
     * @param listaDeArchivosRecibida
     */
    public void procesarListaArchivosRecibida(String listaDeArchivosRecibida){

        for(int j=0;j<tamanioListaArchivos;j++){

            ///////////////////////////////////////////////////////////////////////////////////////
            String nombreArchivo="";
            int posInicio=0;         //OBTENEMOS EL NOMBRE
            int posFinal=0;

            for(int i=0;i<listaArchivosString.length();i++) {

                if (listaArchivosString.substring(i,i+("<A"+j+">").length()).equals(("<A"+j+">"))) {
                    posInicio=i+("<A"+j+">").length();
                }
                if(listaArchivosString.substring(i,i+("</A"+j+">").length()).equals(("</A"+j+">"))){
                    posFinal=i;
                    break;
                }
            }
            nombreArchivo=listaArchivosString.substring(posInicio,posFinal);
            //////////////////////////////////////////////////////////////////////////////////////

            //////////////////////////////////////////////////////////////////////////////////////

            String tamanioArchivo="";
            posInicio=0;                //OBTENEMOS EL TAMAÑO DEL ARCHIVO
            posFinal=0;

            for(int i=0;i<listaArchivosString.length();i++) {

                if (listaArchivosString.substring(i,i+("<T"+j+">").length()).equals(("<T"+j+">"))) {
                    posInicio=i+("<T"+j+">").length();
                }
                if(listaArchivosString.substring(i,i+("</T"+j+">").length()).equals(("</T"+j+">"))){
                    posFinal=i;
                    break;
                }
            }
            tamanioArchivo=listaArchivosString.substring(posInicio,posFinal);
            ///////////////////////////////////////////////////////////////////////////////////////

            this.aniadirArchivoAVector(nombreArchivo,tamanioArchivo); //Aádimos nombreArchivo e tamanioArchivo, en la posicion j
        }
    }

    /**
     * Añade el nombre y tamaño de un archivo a los vectores correspondientes
     * @param nombreArchivo
     * @param tamanoArchivo
     */
    public void aniadirArchivoAVector(String nombreArchivo,String tamanoArchivo){
        synchronized ((Integer)contadorVectores) {
            vectorNombresArchivos[contadorVectores] = nombreArchivo;
            vectorTamanosArchivos[contadorVectores] = tamanoArchivo;
            contadorVectores++;
            Log.d(TAG,"Archivo añadido a vectores, nº "+contadorVectores);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityActiva=true;
        Log.d(TAG,"ACTIVAMOS HEBRA ACTUALIZAR LISVIEW USUARIOS");


    }

    @Override
    protected void onPause() {
        super.onPause();
        activityActiva=false;
        Log.d(TAG,"PAUSAMOS HEBRA ACTUALIZAR LISVIEW USUARIOS");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity_list_view_cloud_clusterhead, menu);
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
}
