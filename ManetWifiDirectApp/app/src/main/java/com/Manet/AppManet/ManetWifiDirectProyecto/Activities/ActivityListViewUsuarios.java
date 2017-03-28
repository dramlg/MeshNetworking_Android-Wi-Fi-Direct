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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.Manet.AppManet.ManetWifiDirectProyecto.Datos.MisDatos;
import com.Manet.AppManet.ManetWifiDirectProyecto.Hebras.HebraRX;
import com.Manet.AppManet.ManetWifiDirectProyecto.Hebras.HebraTX;
import com.Manet.AppManet.ManetWifiDirectProyecto.Hebras.ServidorCH;
import com.Manet.AppManet.ManetWifiDirectProyecto.R;

import java.io.Serializable;
import java.util.Iterator;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

/**
 * Es la listView que se mostrará, para ello, cuando se recibe la lista, se procesa, y se añade
 * a ésta listView. La lista original es un hashmap que tiene el servidor
 */
public class ActivityListViewUsuarios extends Activity implements Serializable {

    private ListView listView;
    private EditText editTextRolDispositivo;
    private Button botonActualizarListView;

    private String clienteNombre[]=new String[256];
    public static String clienteIP[]=new String[256];
    public static Boolean mensajeNoLeido[]=new Boolean[256]; //Contiene, para la persona "j", si dicha persona tiene algún mensaje no leido.
    private int contadorUsuarios=0;
    private String clienteNombreAMostrar[];
    private String TAG="ActLVU";

    private MisDatos misDatos;
    private String listaClientesStr;

    private Thread hebraActualizarListView;

    private boolean listaDisponible;
    private boolean hebraParada=false;

    public static Context context;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view_usuarios);
        context=this.getApplicationContext();

        try {
            botonActualizarListView = (Button) findViewById(R.id.buttonActualizarListView);
            misDatos = (MisDatos) getIntent().getExtras().getSerializable("misDatos");
            listView = (ListView) findViewById(R.id.listViewUsuarios);
            editTextRolDispositivo=(EditText)findViewById(R.id.editTextRolDispositivo);

            if(misDatos.getSoyClusterHead()){
                editTextRolDispositivo.setText("Clusterhead");
            }else{
                editTextRolDispositivo.setText("Cliente");
            }

        }catch (NullPointerException ex){
            //Si venimos de la actividad del chat, estos intent seran null
        }

        for(int j=0;j<mensajeNoLeido.length;j++){
            mensajeNoLeido[j]=false;
        }

        actualizarListView();



        hebraActualizarListView=new Thread(){
            @Override
            public void run() {
                while(true){
                    if(hebraParada==false) {


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                actualizarListView();
                            }
                        });

                        try {
                            Thread.sleep(5000);         //ACTUALIZAMOS LA LISTA CADA 5 SEGUNDOS
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        };


       hebraActualizarListView.start();


        botonActualizarListView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actualizarListView();
            }
        });


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent= new Intent(getApplicationContext(), ActivityChat.class); //Pasamos a la ventana del listView
                intent.putExtra("posicionPersona", position);
                intent.putExtra("IPAlOtroExtremo",clienteIP[position]);
                intent.putExtra("NombrePersonaAlOtroExtremo",clienteNombreAMostrar[position]);
                intent.putExtra("misDatos",misDatos);
                startActivity(intent);
            }
        });
    }

    /**
     * Para actualizar el contenido del ListView
     */
    public void actualizarListView(){
        Log.d(TAG,"Actualizar lista de clientes conectados al CH");


        reiniciarContador();
        clienteNombre=new String[256];
        clienteNombreAMostrar=null;
        listaDisponible=true;  //Si la lista no esta disponible, este boolean tomará el valor false.

        if(misDatos.getSoyClusterHead()==false) { //por aqui va mososososos
            AsyncTask hebraTXCliente = new HebraTX(getApplicationContext(), MainActivity.socketSoyCliente, "<ObtenerListaClientes>",misDatos);
            Log.d(TAG,"Solicitar Lista de Clientes conectados al CH");

            hebraTXCliente.executeOnExecutor(THREAD_POOL_EXECUTOR);


            try{
                listaClientesStr=HebraRX.listaClientesString;        // Puede ir con retardo
                if(listaClientesStr!="") {
                    this.procesarListaClientesRecibida(listaClientesStr);
                    Log.d(TAG, "Lista de clientes recibida");
                }else{
                    listaDisponible=false;
                }
            }catch (Exception ex){
                Log.d(TAG, "Aun no esta disponible la lista");
                listaDisponible=false;
            }
        }
        else if(misDatos.getSoyClusterHead()==true){

            for(int j=0;j<ServidorCH.coleccionClientes.size();j++){
                Iterator it=ServidorCH.coleccionClientes.keySet().iterator();
                while(it.hasNext()){
                    String key=(String)it.next();
                    String value=ServidorCH.coleccionClientes.get(key);
                    this.setUsuario(key,value);
                }
            }
        }



        clienteNombreAMostrar=new String[contadorUsuarios];
        for(int j=0;j<contadorUsuarios;j++){
            clienteNombreAMostrar[j]=clienteNombre[j];
        }

        Boolean copiaVectorMensajeNoLeido[]=new Boolean[contadorUsuarios];

        for(int j=0;j<contadorUsuarios;j++){
            copiaVectorMensajeNoLeido[j]=mensajeNoLeido[j];
        }



        if(misDatos.getSoyClusterHead()==true) {
            ArrayAdapter<String> adapter = new CustomAdapterUsuarioLista(getApplicationContext(), clienteNombreAMostrar,copiaVectorMensajeNoLeido);
            listView.setAdapter(adapter);
        }
        else if(misDatos.getSoyClusterHead()==false && listaDisponible){
            ArrayAdapter<String> adapter = new CustomAdapterUsuarioLista(getApplicationContext(), clienteNombreAMostrar,copiaVectorMensajeNoLeido);
            listView.setAdapter(adapter);
        }
        else{
            Log.d(TAG, "No soy CH, y la lista aun no se ha rellenado");
        }
    }

    /**
     * Mediante este metodo añadimos los usuarios a la lista.
     * Se añade el servidor a si mismo en la tercera fase (si soy servidor)
     * Se añaden los clientes de la lista que recibimos (si soy cliente)
     * @param nombre
     * @param IP
     */
    public void setUsuario(String nombre, String IP){

        boolean aniadir=true;


            for (int i = 0; i < contadorUsuarios; i++) {
                if (nombre.equals(clienteNombre[i])) { //Si el cliente ya esta añadido
                    aniadir = false;
                }
            }


            if (aniadir == true && !nombre.equals(MainActivity.nombreOriginalWiFiDirect)) {
                clienteNombre[contadorUsuarios] = nombre;
                clienteIP[contadorUsuarios] = IP;
                contadorUsuarios++;
            }


    }


    public void reiniciarContador(){
        contadorUsuarios=0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list_view_usuarios, menu);
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
     * Mediante este método, recibimos una lista de cliente, y extraemos de esta sus nombres, IPs,
     * así como la posicion en la lista, conforme los vamos conociendo, procedemos a añadirlos a
     * nuestra clase ActivityListViewUsuarios
     * @param listaDeClientes
     */
    public void procesarListaClientesRecibida(String listaDeClientes){
        int tamanio=obtenerTamanoDeListaDeClientes(listaDeClientes);

        for(int j=0;j<tamanio;j++){


            ///////////////////////////////////////////////////////////////////////////////////////
            String nombre="";
            int posInicio=0;         //OBTENEMOS EL NOMBRE
            int posFinal=0;

            for(int i=0;i<listaDeClientes.length();i++) {

                if (listaDeClientes.substring(i,i+("<Nombre"+j+">").length()).equals(("<Nombre"+j+">"))) {
                    posInicio=i+("<Nombre"+j+">").length();
                }
                if(listaDeClientes.substring(i,i+("</Nombre"+j+">").length()).equals(("</Nombre"+j+">"))){
                    posFinal=i;
                    break;
                }
            }
            nombre=listaDeClientes.substring(posInicio,posFinal);
            //////////////////////////////////////////////////////////////////////////////////////

            //////////////////////////////////////////////////////////////////////////////////////

            String IP="";
            posInicio=0;         //OBTENEMOS LA IP
            posFinal=0;

            for(int i=0;i<listaDeClientes.length();i++) {

                if (listaDeClientes.substring(i,i+("<IP"+j+">").length()).equals(("<IP"+j+">"))) {
                    posInicio=i+("<IP"+j+">").length();
                }
                if(listaDeClientes.substring(i,i+("</IP"+j+">").length()).equals(("</IP"+j+">"))){
                    posFinal=i;
                    break;
                }
            }
            IP=listaDeClientes.substring(posInicio,posFinal);
            ///////////////////////////////////////////////////////////////////////////////////////

            this.setUsuario(nombre,IP); //Aádimos nombre e IP, en la posicion j
        }


    }

    /**
     * Obtener el int tamaño del mensaje.
     *
     * @param mensaje
     * @return String nombre
     */
    public int obtenerTamanoDeListaDeClientes(String mensaje){

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

    @Override
    protected void onResume() {
        super.onResume();
        hebraParada=false;
        Log.d(TAG,"ACTIVAMOS HEBRA ACTUALIZAR LISVIEW USUARIOS");


    }

    @Override
    protected void onPause() {
        super.onPause();
        hebraParada=true;
        Log.d(TAG,"PAUSAMOS HEBRA ACTUALIZAR LISVIEW USUARIOS");
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Exit")
                .setMessage("Cortarás la comunicación, ¿Estás seguro?")
                .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }).setNegativeButton("No", null).show();
    }


}
