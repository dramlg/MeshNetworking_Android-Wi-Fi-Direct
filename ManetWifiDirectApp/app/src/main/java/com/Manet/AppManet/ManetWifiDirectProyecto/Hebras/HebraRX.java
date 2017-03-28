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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.Manet.AppManet.ManetWifiDirectProyecto.Activities.ActivityChat;
import com.Manet.AppManet.ManetWifiDirectProyecto.Activities.ActivityListViewUsuarios;
import com.Manet.AppManet.ManetWifiDirectProyecto.Activities.MainActivity;
import com.Manet.AppManet.ManetWifiDirectProyecto.Datos.MisDatos;
import com.Manet.AppManet.ManetWifiDirectProyecto.WiFiDirect.HerramientasYConfiguracionWiFiDirect;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UTFDataFormatException;
import java.net.Socket;
import java.util.Iterator;

/**
 * Created by Moya on 5 mar 2016.
 */
    public class HebraRX extends AsyncTask<Object,String,Void> implements Serializable{
        //< entrada en inbackgroud, parametro onprogressupdate, retornoinbackground //

        private MainActivity mActivity;
        private Socket clientSocketRecepcion;
        private BufferedReader inputReaderTexto;

        private ProgressDialog progressDialog;
        private OutputStream outputStream;
        private DataInputStream inputStreamArchivo;
        private String nombreArchivo;
        private int tamanoArchivo;
        private byte byteArray[];
        private boolean recibiendoArchivo;
        private String nombreOrigenArchivo;
        private String ipOrigenArchivo;
        private int posicionEnListViewUsuariosDelUsuarioQueNosHaEnviadoArchivo;
        private Notification notificationArchivo;
        private Notification.Builder mBuilderNotificacionArchivo;
        private NotificationManager notificationManagerArchivo;
        private double progresoTransferencia=0;
        private File dir;
        private double velocidadTransferenciaMediaFinal=-1; // kB/seg
        private boolean nombreArchivoGuardado=false;
        private boolean tamanoArchivoGuardado=false;

        private int progresoTransferenciaEnEmisor=0;

        //Cuando somos emisor de archivo
        int identificadorNotificacionEmision;


        private String entrada;
        private String nombreCliente;
        private String dirIPCliente;
        private MisDatos misDatos;

        public static String listaClientesString;
        public static String listaArchivosString="";

        private boolean pausarRecepcionEnDoinBackGround;
        private boolean esperandoMasPartesDelMensaje;
        private boolean clienteRegistrado;


        private HerramientasYConfiguracionWiFiDirect herramientasYConfiguracionWiFiDirect;
        private String TAG="HebraRX";



        public HebraRX(MainActivity mActivity, Socket clientSocketRecepcion, MisDatos misDatos, HerramientasYConfiguracionWiFiDirect herramientasYConfiguracionWiFiDirect){
            this.mActivity=mActivity;
            this.clientSocketRecepcion=clientSocketRecepcion;
            this.misDatos=misDatos;
            this.listaClientesString="";
            this.pausarRecepcionEnDoinBackGround=false;
            this.esperandoMasPartesDelMensaje=false;
            this.herramientasYConfiguracionWiFiDirect=herramientasYConfiguracionWiFiDirect;
            this.clienteRegistrado =false;
            this.recibiendoArchivo=false;
        }


        @Override
        protected Void doInBackground(Object[] params) {

            try {

                inputReaderTexto = new BufferedReader(new InputStreamReader(clientSocketRecepcion.getInputStream()));

                while(true) {
                    if(!pausarRecepcionEnDoinBackGround) {

                        while(!recibiendoArchivo) {
                            if(!recibiendoArchivo){
                                entrada = inputReaderTexto.readLine();
                            }
                            publishProgress(entrada);
                            if (entrada == null) {
                                throw new IOException();
                            }

                        }while(recibiendoArchivo) {
                            nombreArchivoGuardado=false;
                            tamanoArchivoGuardado=false;

                            while(!nombreArchivoGuardado){
                                nombreArchivo = inputReaderTexto.readLine();
                                if (nombreArchivo.startsWith("<NombreArchivo>")) {
                                    nombreArchivo = obtenerNombreArchivoDeMensaje(nombreArchivo); //////por aqui
                                    Log.d(TAG, "nombre nombreArchivo:  " + nombreArchivo);
                                    nombreArchivoGuardado=true;
                                }
                            }

                            while(!tamanoArchivoGuardado){
                                String tamanoArchivoString = inputReaderTexto.readLine();
                                if (tamanoArchivoString.startsWith("<TamanoArchivo>")){
                                    tamanoArchivo=obtenerTamanoArchivoDeMensaje(tamanoArchivoString);
                                    Log.d(TAG, "Tamaño del archivo:  " + tamanoArchivo);
                                    tamanoArchivoGuardado=true;
                                }
                            }

                            if(nombreArchivoGuardado && tamanoArchivoGuardado) {

                                File file = null;
                                String state;
                                state = Environment.getExternalStorageState();
                                if (Environment.MEDIA_MOUNTED.equals(state)) {

                                    File root = Environment.getExternalStorageDirectory();
                                    dir = new File(root.getAbsolutePath() + "/ManetFiles");
                                    if (!dir.exists()) {
                                        dir.mkdir();
                                    }
                                    file = new File(dir, nombreArchivo);
                                } else {
                                    Log.d(TAG, "Almacenamiento externo no encontrado");
                                }
                                inputStreamArchivo = new DataInputStream(clientSocketRecepcion.getInputStream());
                                outputStream = new FileOutputStream(file);
                                Log.d(TAG, "Buffer de escritura de archivo preparado");


                                int offsetActual = 0;
                                progresoTransferencia = 0;
                                boolean primeraVez = false;
                                int bytesRead;
                                long bytesTotalesLeidos = 0;
                                int contador = 0;
                                long bytesEnContador0 = 0;
                                long bytesEnUltimoContador = 0;
                                double tiempoEnContador0 = 0;
                                double tiempoEnUltimoContador = 0;
                                byteArray = new byte[8192];

                                while (bytesTotalesLeidos < tamanoArchivo) {
                                    bytesRead = inputStreamArchivo.read(byteArray, offsetActual, byteArray.length - offsetActual);
                                    if (!primeraVez) {
                                        publishProgress("MostrarNotificacion");
                                        publishProgress("ActualizarNotificacion");
                                        primeraVez = true;
                                    }
                                    outputStream.write(byteArray, offsetActual, bytesRead);

                                    offsetActual += bytesRead;
                                    bytesTotalesLeidos += bytesRead;
                                    if (offsetActual == byteArray.length) {
                                        offsetActual = 0;
                                        byteArray = new byte[8192];
                                    }

                                    progresoTransferencia = Math.abs((bytesTotalesLeidos * 100.0 / tamanoArchivo));

                                    if (contador == 0) {
                                        bytesEnContador0 = bytesTotalesLeidos;
                                        tiempoEnContador0 = System.currentTimeMillis();
                                    }
                                    contador++;
                                    if (contador == 30) {
                                        bytesEnUltimoContador = bytesTotalesLeidos;
                                        tiempoEnUltimoContador = System.currentTimeMillis();
                                        velocidadTransferenciaMediaFinal = ((bytesEnUltimoContador - bytesEnContador0) / ((tiempoEnUltimoContador - tiempoEnContador0) / 1000)) / 1000; //kB/s

                                        publishProgress("InformarDelProgreso");
                                        contador = 0;
                                    }

                                    Log.d(TAG, "Bytes recibidos: " + bytesTotalesLeidos);
                                    Log.d(TAG, "Progreso transferencia: " + progresoTransferencia);
                                }
                                outputStream.close();
                                publishProgress("FinalizarProgressDialog");
                                Log.d(TAG, "escrito");


                                outputStream.flush();
                                outputStream.close();
                                Log.d(TAG, "Archivo escrito en dispositivo, con longitud: " + tamanoArchivo + " bytes");

                                recibiendoArchivo = false;
                            }


                        }
                    }

                }

            }  catch (UTFDataFormatException ex){
                ex.printStackTrace();
                recibiendoArchivo=false;
            }  catch (IOException e) {
                e.printStackTrace();
            }  catch (Exception e){
                e.printStackTrace();
            }
            finally {
                try {
                    if (misDatos.getSoyClusterHead() && clienteRegistrado==true ) {
                        Log.d(TAG, "Hemos perdido al cliente " + this.nombreCliente);
                        ServidorCH.coleccionClientes.remove(this.nombreCliente); //Lo eliminamos de la coleccion
                        herramientasYConfiguracionWiFiDirect.pasarelaActualizarServicioLocal(ServidorCH.coleccionClientes.size() - 1);
                        this.cancel(true);
                    }
                }catch (NullPointerException ex){
                    ex.printStackTrace();
                }

            if(!misDatos.getSoyClusterHead() && clientSocketRecepcion.isClosed()){
                Log.d(TAG, "Hemos perdido al servidor " + this.nombreCliente);
                publishProgress("error: hemos perdido al clusterhead, se ha de reanudar el proceso");
                System.exit(0);
            }
        }


            return null;
        }

        @Override
        protected synchronized void onProgressUpdate(String texto[]) {
            String palabra="";
            for(int i=0;i<texto.length;i++){
                palabra=palabra+texto[i];
            }

            if(palabra.contains("error")){
                Toast.makeText(mActivity,palabra,Toast.LENGTH_SHORT).show();
            }

            else if(palabra.startsWith("InformarDelProgreso")){
                AsyncTask hebraTXinformarProgreso = new HebraTX(mActivity, clientSocketRecepcion,"<progresoTransferencia>"+(int)progresoTransferencia+"</progresoTransferencia>", misDatos);
                hebraTXinformarProgreso.executeOnExecutor(THREAD_POOL_EXECUTOR);
            }
            else if(palabra.startsWith("<progresoTransferencia>")){
                progresoTransferenciaEnEmisor=obtenerProgresoTransferenciaDeMensaje(palabra);
            }
            else if (palabra.startsWith("<IP>")) { /**MENSAJE DE REGISTRO*/
                if (misDatos.getSoyClusterHead()) {
                    this.registrarCliente(palabra);
                    clienteRegistrado = true;
                }
            }

            else if (palabra.startsWith("<Reenvio>")){
                this.funcionReenvio(palabra);
            }

            else if(palabra.startsWith("<ObtenerListaClientes>")){
                    String lista = generarListaClientes();
                    this.enviarListaDeClientes(lista);
            }

            else if(palabra.startsWith("<Desregistrar>")){
                String nombre=this.obtenerNombreOrigenDeMensaje(palabra);
                this.desRegistrarCliente(nombre);
            }

            else if(palabra.startsWith("<listaClientes>")){
                this.listaClientesString=palabra;
            }

            else if(palabra.startsWith("<SolicitudEnviarArchivo>")){

               String mensaje=palabra;
                //SOMOS RECEPTOR DEL ARCHIVO
                synchronized ((Boolean)recibiendoArchivo) {
                    recibiendoArchivo = true;
                }

                String miIP="";
                if(misDatos.getSoyClusterHead()){
                    miIP="192.168.49.1";
                }else{
                    miIP=obtenerMiIpConWifiManager();
                }

                Log.d(TAG, "Hemos aceptado la solicitid para recibir un nombreArchivo");
                Log.d(TAG,palabra);
                nombreOrigenArchivo = obtenerNombreOrigenDeMensaje(mensaje);
                ipOrigenArchivo = obtenerIPOrigenDeTextoXML(mensaje);



                AsyncTask hebraTXCliente = new HebraTX(mActivity,clientSocketRecepcion, "<SolicitudEnviarArchivoAceptada>"+"<IPorig>"+miIP+"</IPorig>"+"<NombreOrigen>"
                        +MainActivity.nombreOriginalWiFiDirect+"</NombreOrigen>"+"</SolicitudEnviarArchivoAceptada>",misDatos);
                hebraTXCliente.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                Toast.makeText(mActivity, "Recibiendo archivo de: " + nombreOrigenArchivo, Toast.LENGTH_SHORT).show();

            }else if(palabra.startsWith("<SolicitudEnviarArchivoAceptada>")){

                String mensaje=palabra;

                //SOMOS EMISOR, ABRIR NOTIFICACION
                //PONER MENSAJE EN CHAT DE QUE SE NO PUEDE CONTINUAR LA CONVERSACION HASTA PREVIO AVISO
                String ipRecetorDelArchivo=obtenerIPOrigenDeTextoXML(mensaje);
                String nombreReceptorDelArchivo=obtenerNombreOrigenDeMensaje(mensaje);

                Log.d(TAG,"Ip receptor del archivo: "+ipRecetorDelArchivo);
                int posicionEnListViewUsuariosDelUsuarioReceptorDelArchivo=-1;
                for(int i=0;i<ActivityListViewUsuarios.clienteIP.length;i++){
                    try{
                        if((ActivityListViewUsuarios.clienteIP[i]).equals(ipRecetorDelArchivo)){
                            posicionEnListViewUsuariosDelUsuarioReceptorDelArchivo=i;
                        }
                    }catch (NullPointerException ex){
                    }
                }
                if(posicionEnListViewUsuariosDelUsuarioReceptorDelArchivo!=-1) {
                    ActivityChat.aniadirMensajeAvectorMensajesChat(posicionEnListViewUsuariosDelUsuarioReceptorDelArchivo, "El servicio de chat no está disponible.\n\nEl canal de transmisión esta ocupado ya que" +
                            " un archivo esta siendo enviado, no podrás enviar mensajes de texto hasta" +
                            " ser informado de la finalización de la transmisión.\n\nPuedes consultar el estado" +
                            " de las transmisión en la barra de notificaciones");

                    identificadorNotificacionEmision=posicionEnListViewUsuariosDelUsuarioReceptorDelArchivo+2000;
                    mostrarNotificacionEmitiendoArchivo(nombreReceptorDelArchivo, identificadorNotificacionEmision);

                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    while(progresoTransferenciaEnEmisor<100) {

                                        mBuilderNotificacionArchivo.setProgress(100,progresoTransferenciaEnEmisor, false);

                                        notificationManagerArchivo.notify(identificadorNotificacionEmision, mBuilderNotificacionArchivo.build());
                                        try {
                                            Thread.sleep(2000);
                                        } catch (InterruptedException e) {
                                            Log.d(TAG, "Fallo al hacer sleep en hebra progeso notificacion de recibir archivo");
                                        }
                                    }

                                }
                            }
                    ).start();

                }else {
                    Log.d(TAG,"Usuario receptor del archivo no encontrado en listView");
                }




            }else if(palabra.startsWith("SolicitudEnviarArchivoRechazada")){
                Toast.makeText(mActivity,"Solicitud de transferencia de archivo rechazada",Toast.LENGTH_SHORT).show();
            }
            else if (palabra.startsWith("MostrarNotificacion")){
                    //this.progressDialog.show();
                   // this.progressDialog.setProgress(obtenerValorProgresDialog(palabra));

                if(misDatos.getSoyClusterHead()){
                    posicionEnListViewUsuariosDelUsuarioQueNosHaEnviadoArchivo=-1;
                    for(int i=0;i<ActivityListViewUsuarios.clienteIP.length;i++){
                        try{

                            if((ActivityListViewUsuarios.clienteIP[i]).equals(ipOrigenArchivo)){
                                posicionEnListViewUsuariosDelUsuarioQueNosHaEnviadoArchivo=i+1000;
                            }
                        }catch (NullPointerException ex){
                        }
                    }
                    //En caso del clusterhead, puesto que podemos recibir varios archivos a la vez,
                    //el identificador de la notificacion será posicionEnListViewUsuariosDelUsuarioQueNosHaEnviadoElMensaje+1000
                    mostrarNotificacionRecibiendoArchivo(nombreOrigenArchivo, nombreArchivo, Integer.toString(tamanoArchivo), posicionEnListViewUsuariosDelUsuarioQueNosHaEnviadoArchivo);
                }else{
                    //El identificador de dicha notificacion para el cliente siempre sera el 6666,
                    //será fijo ya que no recibirá más de 2 archivos a la vez
                    mostrarNotificacionRecibiendoArchivo("Clusterhead", nombreArchivo, Integer.toString(tamanoArchivo), 666);
                }

            }
            else if(palabra.startsWith("ActualizarNotificacion")){
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                while(progresoTransferencia<100) {

                                    mBuilderNotificacionArchivo.setProgress(100,(int)progresoTransferencia, false);
                                    if(velocidadTransferenciaMediaFinal==-1){
                                        mBuilderNotificacionArchivo.setContentInfo(" Calculando velocidad de transferencia...");
                                    }else {
                                        mBuilderNotificacionArchivo.setContentInfo(Double.toString(velocidadTransferenciaMediaFinal) + " kB/s");
                                    }
                                    notificationManagerArchivo.notify(posicionEnListViewUsuariosDelUsuarioQueNosHaEnviadoArchivo, mBuilderNotificacionArchivo.build());
                                    try {
                                        Thread.sleep(200);
                                    } catch (InterruptedException e) {
                                        Log.d(TAG, "Fallo al hacer sleep en hebra progeso notificacion de recibir archivo");
                                    }
                                }

                            }
                        }
                ).start();
            }
            else if (palabra.startsWith("FinalizarProgressDialog")){

                //SOMOS RECEPTOR
                progresoTransferencia=100;

                if(misDatos.getSoyClusterHead()){
                    quitarNotificacion(posicionEnListViewUsuariosDelUsuarioQueNosHaEnviadoArchivo);
                    Toast.makeText(mActivity,"Archivo "+nombreArchivo+" guardado en "+dir.getAbsolutePath().toString(),Toast.LENGTH_SHORT).show();
                }else {
                    quitarNotificacion(666);
                }

                String miIP="";
                if(misDatos.getSoyClusterHead()){
                    miIP="192.168.49.1";
                }else{
                    miIP=obtenerMiIpConWifiManager();
                }
                AsyncTask hebraTXCliente = new HebraTX(mActivity,clientSocketRecepcion, "<TransmisionDeFicheroFinalizada>"+"<IPorig>"+miIP+"</IPorig>"+"</TransmisionDeFicheroFinalizada>",misDatos);
                hebraTXCliente.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            }
            else if(palabra.startsWith("<TransmisionDeFicheroFinalizada>")){
                progresoTransferenciaEnEmisor=100;
                //SOMOS EMISOR, ABRIR NOTIFICACION
                //PONER MENSAJE EN CHAT DE QUE SE NO PUEDE CONTINUAR LA CONVERSACION HASTA PREVIO AVISO
                String ipRecetorDelArchivo=obtenerIPOrigenDeTextoXML(palabra);
                int posicionEnListViewUsuariosDelUsuarioReceptorDelArchivo=-1;
                for(int i=0;i<ActivityListViewUsuarios.clienteIP.length;i++){
                    try{
                        if((ActivityListViewUsuarios.clienteIP[i]).equals(ipRecetorDelArchivo)){
                            posicionEnListViewUsuariosDelUsuarioReceptorDelArchivo=i;
                        }
                    }catch (NullPointerException ex){
                    }
                }
                if(posicionEnListViewUsuariosDelUsuarioReceptorDelArchivo!=-1) {
                    ActivityChat.aniadirMensajeAvectorMensajesChat(posicionEnListViewUsuariosDelUsuarioReceptorDelArchivo, "El archivo esta correctamente enviado, el servicio de chat vuelve" +
                            " a estar disponible.");
                    quitarNotificacion(posicionEnListViewUsuariosDelUsuarioReceptorDelArchivo+2000);
                }else {
                    Log.d(TAG,"Usuario receptor del archivo no encontrado en listView");
                }
            }

            else if(palabra.startsWith("<ObtenerListaArchivos>")){
                if(misDatos.getSoyClusterHead()){
                    String listaArchivos=generarStringArchivos();
                    AsyncTask hebraTXCliente = new HebraTX(mActivity,clientSocketRecepcion, listaArchivos,misDatos);
                    hebraTXCliente.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    Log.d(TAG,listaArchivos);
                }
            }

            else if(palabra.startsWith("<ListaArchivos>")){
                Log.d(TAG,"Recibimos la lista de archivos solicitada");
                listaArchivosString=palabra;
            }

            else if(palabra.startsWith("<SolicitudArchivoEnCloud>")){
                enviarArchivoSolicitadoDelCloud(palabra);
            }


            Log.d(TAG,palabra);

            //Si palabra contiene reenvio a otra persona
            //vemos la direccion IP a la que va
            //enviamos el mensaje a dicha direccion con hebraTX, indicandole de donde proviene
            //puesto que estamos en la hebraRX de j, el cliente que nos ha mandado el mensaje
            //es el identificado en esta hebra


        }

        @Override
        protected synchronized void onCancelled() {
            super.onCancelled();
            Log.d(TAG, "Cerramos la hebra " + this.nombreCliente);
            if(this.clientSocketRecepcion!=null){
                try {
                    this.clientSocketRecepcion.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected synchronized void onPostExecute(Void o) {
            super.onPostExecute(o);
            try {
                Log.d(TAG, "Hebra Recepción finalizada");
                clientSocketRecepcion.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected synchronized void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * Mediante este método registramos un cliente y lo añadimos a la colección hashmap
         * @param mensajeRegistro
         */
        public void registrarCliente(String mensajeRegistro){
            this.dirIPCliente = obtenerIPdeMensaje(mensajeRegistro);
            this.nombreCliente = obtenerNombreOrigenDeMensaje(mensajeRegistro);
            ServidorCH.coleccionClientes.put(this.nombreCliente, this.dirIPCliente);
            herramientasYConfiguracionWiFiDirect.pasarelaActualizarServicioLocal(ServidorCH.coleccionClientes.size() - 1);  //El numero de dispositivos enlazados será el de la coleccion -1(el propio CH)
            Log.d(TAG, "CLIENTE REGISTRADO, CON NOMBRE: " + this.nombreCliente + " Y DIRECCIÓN IP: " + this.dirIPCliente);
        }

        /**
         * Mediante este método eliminamos a un cliente del hashmap
         * @param nombreCliente
         */
        public void desRegistrarCliente(String nombreCliente){
            ServidorCH.coleccionClientes.remove(nombreCliente);
            herramientasYConfiguracionWiFiDirect.pasarelaActualizarServicioLocal(ServidorCH.coleccionClientes.size() - 1);
            Log.d(TAG, "El usuario " + nombreCliente + " se ha desregistrado correctamente");
        }

        /**
         * Mediante este metodo obtenemos un fichero XML de la lista de los clientes actualmente registrados
         * */
        public String generarListaClientes(){

            String listaDeClientes="<listaClientes><Tamanio>"+ServidorCH.coleccionClientes.size()+"</Tamanio>"; //Creamos un string lista de clientes para enviarselo en texto
            Iterator it=ServidorCH.coleccionClientes.keySet().iterator();
            int pos=0;
            while(it.hasNext()){
                String key=(String)it.next();
                String value=ServidorCH.coleccionClientes.get(key);
                listaDeClientes=listaDeClientes+"<Nombre"+pos+">"+key+"</Nombre"+pos+">"
                        +"<IP"+pos+">"+value+"</IP"+pos+">";
                pos++;
            }
            listaDeClientes=listaDeClientes+"</listaClientes>";

            return listaDeClientes;
        }

        /**
         * Contiene el procesamiento que se le aplica a un mensaje cuando es recibido, es decir:
         * Si somos nosotros el destinatario, se motrará en la conversación, si no, será reenviado(somos CH en tal caso).
         * @param palabra
         */
        public void funcionReenvio(String palabra){
            String mensaje="";
            mensaje=palabra;


            if(!mensaje.contains("</Reenvio>")){
                esperandoMasPartesDelMensaje=true;
                pausarRecepcionEnDoinBackGround=true;
            }
            while(esperandoMasPartesDelMensaje){   //POR SI EL MENSAJE SE ENVIA EN PARTES SEPARADAS
                try {
                    entrada = inputReaderTexto.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mensaje=mensaje+entrada;
                if(mensaje.contains("</Reenvio>")){
                    esperandoMasPartesDelMensaje=false;
                    pausarRecepcionEnDoinBackGround=false;
                }
            }


            String ipDestino = obtenerIPDestinoDeTextoXML(mensaje);
            String ipOrigen = obtenerIPOrigenDeTextoXML(mensaje);
            String nombreOrigenDeMensaje = obtenerNombreOrigenDeMensaje(mensaje);
            if(ipOrigen.equals("0.0.0.0")){
                ipOrigen="192.168.49.1";
            }
            String miIp=obtenerMiIpConWifiManager();
            Log.d(TAG, "Mensaje recibido, la ip destino es:" + ipDestino + ", mi IP es:" + miIp + ", con origen en Ip: " + ipOrigen);


            if(ipDestino.equals(miIp) || (ipDestino.equals("192.168.49.1") && misDatos.getSoyClusterHead())){
                //El mensaje va hacia nosotros, tenemos que saber quien nos lo ha enviado

                int posicionEnListViewUsuariosDelUsuarioQueNosHaEnviadoElMensaje=-1;
                for(int i=0;i<ActivityListViewUsuarios.clienteIP.length;i++){
                    try{
                        if((ActivityListViewUsuarios.clienteIP[i]).equals(ipOrigen)){
                            posicionEnListViewUsuariosDelUsuarioQueNosHaEnviadoElMensaje=i;
                        }
                    }catch (NullPointerException ex){
                    }
                }

                if(posicionEnListViewUsuariosDelUsuarioQueNosHaEnviadoElMensaje!=-1){
                    String mensajeAmostrar=" "+nombreOrigenDeMensaje+": "+obtenerMensajeDeTextoXML(mensaje)+" ";
                    ActivityChat.aniadirMensajeAvectorMensajesChat(posicionEnListViewUsuariosDelUsuarioQueNosHaEnviadoElMensaje, mensajeAmostrar);
                    Log.d(TAG,"Activity Chat esta Activa?: "+ActivityChat.activityActiva);

                    if(!ActivityChat.activityActiva || (ActivityChat.activityActiva && !(ActivityChat.IPAlOtroExtremo).equals(ipOrigen))){

                        synchronized (ActivityListViewUsuarios.context) {
                            // Prepare intent which is triggered if the
                            // notification is selected
                            Intent intent = new Intent(mActivity, ActivityChat.class);
                            intent.putExtra("posicionPersona",posicionEnListViewUsuariosDelUsuarioQueNosHaEnviadoElMensaje);
                            intent.putExtra("IPAlOtroExtremo", ipOrigen);
                            intent.putExtra("IPAlOtroExtremoNotificacion", ipOrigen);
                            intent.putExtra("misDatos",misDatos);
                            intent.putExtra("NombrePersonaAlOtroExtremo",nombreOrigenDeMensaje);
                            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            PendingIntent pIntent = PendingIntent.getActivity(mActivity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                            // Build notification
                            // Actions are just fake
                            Notification notification = new Notification.Builder(mActivity)
                                    .setContentTitle("Mensaje de: "+nombreOrigenDeMensaje)
                                    .setContentText(obtenerMensajeDeTextoXML(mensaje))
                                    .setSmallIcon(android.R.drawable.sym_action_chat)
                                    .setContentIntent(pIntent)
                                    .build();

                            NotificationManager notificationManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
                            notification.flags |= Notification.FLAG_AUTO_CANCEL; //PendingIntent.FLAG_ONE_SHOT
                            //El identificador de la notificación sera igual a la posición de la
                            //persona en el vector de personas de listView
                            notificationManager.notify(posicionEnListViewUsuariosDelUsuarioQueNosHaEnviadoElMensaje, notification);
                            ActivityListViewUsuarios.mensajeNoLeido[posicionEnListViewUsuariosDelUsuarioQueNosHaEnviadoElMensaje]=true;

                            Log.d(TAG,"MOSTRAMOS NOTIFICACION, ip origen es: "+ipOrigen);
                        }



                    }


                }else{
                    Log.d(TAG,"No hemos encontrado en la lista el usuario que nos ha enviado el mensaje");
                }
            }
            else{ //Soy CH y tengo que reenviar

                for(int i=0;i<MainActivity.socketsDeMisClientes.length;i++){

                    try{
                        String ipRemota=MainActivity.socketsDeMisClientes[i].getInetAddress().getHostAddress();
                        Log.d(TAG,"Soy CH ,REENVIO, COMPARANDO DIRECCIONES: REMOTA: "+ipRemota+ "\n DESTINO : "+ipDestino);
                        if(ipRemota.equals(ipDestino)){
                            AsyncTask hebraTXCliente = new HebraTX(mActivity, MainActivity.socketsDeMisClientes[i],mensaje,misDatos);
                            hebraTXCliente.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    }catch (NullPointerException ex){
                        ex.printStackTrace();
                    }

                }

            }
        }

        /**
         * Mediante este método enviamos un socket con el parámetro introducido de mensaje
         * */
        public void enviarListaDeClientes(String listaClientes){
            AsyncTask hebraTXCliente = new HebraTX(mActivity,this.clientSocketRecepcion, listaClientes,misDatos);
            hebraTXCliente.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        /**
         * Obtener el String IP del mensaje.
         *
         * @param mensajeRegistro
         * @return String dirIP
         */
        public String obtenerIPdeMensaje(String mensajeRegistro){

            String dirIP="";

            int posInicio=0;
            int posFinal=0;

            for(int i=0;i<mensajeRegistro.length();i++) {

                if (mensajeRegistro.substring(i,i+"<IP>".length()).equals("<IP>")) {
                    posInicio=i+"<IP>".length();
                }
                if(mensajeRegistro.substring(i,i+"</IP>".length()).equals("</IP>")){
                    posFinal=i;
                    break;
                }
            }
            dirIP=mensajeRegistro.substring(posInicio,posFinal);

            return dirIP;

        }

        /**
         * Obtener el String nombre del mensaje.
         *
         * @param mensajeRegistro
         * @return String nombre
         */
        public String obtenerNombreOrigenDeMensaje(String mensajeRegistro){

            String nombre="";

            int posInicio=0;
            int posFinal=0;

            for(int i=0;i<mensajeRegistro.length();i++) {

                if (mensajeRegistro.substring(i,i+"<NombreOrigen>".length()).equals("<NombreOrigen>")) {
                    posInicio=i+"<NombreOrigen>".length();
                }
                if(mensajeRegistro.substring(i,i+"</NombreOrigen>".length()).equals("</NombreOrigen>")){
                    posFinal=i;
                    break;
                }
            }
            nombre=mensajeRegistro.substring(posInicio,posFinal);

            return nombre;

        }

        /**
         * Obtener el String nombre de archivo del mensaje inicial al recibir un archivo.
         *
         * @param mensajeRegistro
         * @return String nombre
         */
        public String obtenerNombreArchivoDeMensaje(String mensajeRegistro){

            String nombre="";

            int posInicio=0;
            int posFinal=0;

            for(int i=0;i<mensajeRegistro.length();i++) {

                if (mensajeRegistro.substring(i,i+"<NombreArchivo>".length()).equals("<NombreArchivo>")) {
                    posInicio=i+"<NombreArchivo>".length();
                }
                if(mensajeRegistro.substring(i,i+"</NombreArchivo>".length()).equals("</NombreArchivo>")){
                    posFinal=i;
                    break;
                }
            }
            nombre=mensajeRegistro.substring(posInicio,posFinal);

            return nombre;

        }

        /**
         * Obtener el int tamaño de archivo del mensaje inicial al recibir un archivo.
         *
         * @param mensajeRegistro
         * @return String nombre
         */
        public int obtenerTamanoArchivoDeMensaje(String mensajeRegistro){

            String tamanoArchivo="";

            int posInicio=0;
            int posFinal=0;

            for(int i=0;i<mensajeRegistro.length();i++) {

                if (mensajeRegistro.substring(i,i+"<TamanoArchivo>".length()).equals("<TamanoArchivo>")) {
                    posInicio=i+"<TamanoArchivo>".length();
                }
                if(mensajeRegistro.substring(i,i+"</TamanoArchivo>".length()).equals("</TamanoArchivo>")){
                    posFinal=i;
                    break;
                }
            }
            tamanoArchivo=mensajeRegistro.substring(posInicio,posFinal);

            return Integer.parseInt(tamanoArchivo);

        }

        /**
         * Obtener el progreso de trasnferencia del mensaje que nos informa sobre dicha transferencia,
         * siendo nosotros el emisor del archivo.
         *
         * @param mensajeProgreso
         * @return int progreso
         */
        public int obtenerProgresoTransferenciaDeMensaje(String mensajeProgreso){

            String progreso="";

            int posInicio=0;
            int posFinal=0;

            for(int i=0;i<mensajeProgreso.length();i++) {

                if (mensajeProgreso.substring(i,i+"<progresoTransferencia>".length()).equals("<progresoTransferencia>")) {
                    posInicio=i+"<progresoTransferencia>".length();
                }
                if(mensajeProgreso.substring(i,i+"</progresoTransferencia>".length()).equals("</progresoTransferencia>")){
                    posFinal=i;
                    break;
                }
            }
            progreso=mensajeProgreso.substring(posInicio,posFinal);

            return Integer.parseInt(progreso);

        }

        /**
         * Obtener el String IP de destino del mensaje.
         *
         * @param textoXML
         * @return String dirIP
         */
        public String obtenerIPDestinoDeTextoXML(String textoXML){

            String dirIP="";

            int posInicio=0;
            int posFinal=0;

            for(int i=0;i<textoXML.length();i++) {

                if (textoXML.substring(i,i+"<IPdest>".length()).equals("<IPdest>")) {
                    posInicio=i+"<IPdest>".length();
                }
                if(textoXML.substring(i,i+"</IPdest>".length()).equals("</IPdest>")){
                    posFinal=i;
                    break;
                }
            }
            dirIP=textoXML.substring(posInicio,posFinal);

            return dirIP;

        }

        /**
         * Obtener el String IP de origen del mensaje.
         *
         * @param textoXML
         * @return String dirIP
         */
        public String obtenerIPOrigenDeTextoXML(String textoXML){

            String dirIP="";

            int posInicio=0;
            int posFinal=0;

            for(int i=0;i<textoXML.length();i++) {

                if (textoXML.substring(i,i+"<IPorig>".length()).equals("<IPorig>")) {
                    posInicio=i+"<IPorig>".length();
                }
                if(textoXML.substring(i,i+"</IPorig>".length()).equals("</IPorig>")){
                    posFinal=i;
                    break;
                }
            }
            dirIP=textoXML.substring(posInicio,posFinal);

            return dirIP;

        }

        /**
         * Obtener el String mensaje del textoXML recibido.
         *
         * @param textoXML
         * @return String mensaje
         */
        public String obtenerMensajeDeTextoXML(String textoXML){

            String texto="";

            int posInicio=0;
            int posFinal=0;

            for(int i=0;i<textoXML.length();i++) {

                if (textoXML.substring(i,i+"<Mensaje>".length()).equals("<Mensaje>")) {
                    posInicio=i+"<Mensaje>".length();
                }
                if(textoXML.substring(i,i+"</Mensaje>".length()).equals("</Mensaje>")){
                    posFinal=i;
                    break;
                }
            }
            texto=textoXML.substring(posInicio, posFinal);

            return texto;

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

        public void mostrarNotificacionRecibiendoArchivo(String emisorArchivo, String nombreArchivo, String tamanoArchivo, int id){
            synchronized (ActivityListViewUsuarios.context) {

                mBuilderNotificacionArchivo =new Notification.Builder(mActivity);
                notificationArchivo = mBuilderNotificacionArchivo
                        .setContentTitle("Recibiendo " + nombreArchivo + " de: " + emisorArchivo)
                        .setContentText("Tamaño: " + tamanoArchivo + " bytes")
                        .setSmallIcon(android.R.drawable.btn_radio)
                        .build();

                notificationManagerArchivo = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationArchivo.flags |= Notification.FLAG_AUTO_CANCEL; //PendingIntent.FLAG_ONE_SHOT
                //El identificador de la notificación sera igual a la posición de la
                //persona en el vector de personas de listView
                notificationManagerArchivo.notify(id, notificationArchivo);

                Log.d(TAG,"MOSTRAMOS NOTIFICACION RECIBIR ARCHIVO, ARCHIVO: "+nombreArchivo+" DE : "+emisorArchivo);
            }
        }

        public void mostrarNotificacionEmitiendoArchivo(String receptorArchivo, int id){
            synchronized (ActivityListViewUsuarios.context) {

                mBuilderNotificacionArchivo =new Notification.Builder(mActivity);
                notificationArchivo = mBuilderNotificacionArchivo
                        .setContentTitle("Transmitiendo archivo")
                        .setSmallIcon(android.R.drawable.btn_radio)
                        .build();

                notificationManagerArchivo = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationArchivo.flags |= Notification.FLAG_AUTO_CANCEL; //PendingIntent.FLAG_ONE_SHOT
                //El identificador de la notificación sera igual a la posición de la
                //persona en el vector de personas de listView
                notificationManagerArchivo.notify(id, notificationArchivo);

                Log.d(TAG,"MOSTRAMOS NOTIFICACION RECIBIR ARCHIVO, ARCHIVO: "+nombreArchivo+" DE : "+receptorArchivo);
            }
        }

        public void quitarNotificacion(int id){
            NotificationManager notificationManager = (NotificationManager)mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
            try{
                notificationManager.cancel(id);
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }

        /**
         * Realiza una lectura de los archivos existentes en la carpeta /ManetFiles, y los devuelve
         * en un string, indicando para cada archivo su posicion en la carpeta, título y tamaño en kB.
         *
         * El formato en el que recibimos la lista es el siguiente:
         * <A0>NombreArchivoEnPosicion0</A0><T0>TamañoDelArchivoEnPosicion0</T0><A1>...</A1><T1>...</T1>+...+<A(n-1)>...</A(n-1)><T(n-1)>...</T(n-1)>
         * @return
         */
        public String generarStringArchivos(){
            String stringArchivos="";
            stringArchivos+="<ListaArchivos>";

            File file = null;
            String state;
            state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {

                File root = Environment.getExternalStorageDirectory();
                dir = new File(root.getAbsolutePath() + "/ManetFiles");
                if(!dir.exists()){
                    dir.mkdir();
                }
            }
            File archivosExistentes[]=dir.listFiles();

            stringArchivos+="<Tamanio>"+archivosExistentes.length+"</Tamanio>";
            try{
                for(int i=0;i<archivosExistentes.length;i++){
                    stringArchivos+="<A"+i+">"+archivosExistentes[i].getName()+"</A"+i+">"+"<T"+i+">"+(Long.toString((archivosExistentes[i].length())/1024))+"</T"+i+">"; //longitud en kB
                }
                stringArchivos+="</ListaArchivos>";
            }catch (NullPointerException ex){
                Log.d(TAG,"DIR es null");
                ex.printStackTrace();
            }
            return stringArchivos;
        }

        public void enviarArchivoSolicitadoDelCloud(String palabra){
            //Extraer el nombre del fichero a enviar
            String nombreFicheroSolicitado=obtenerNombreArchivoDeMensaje(palabra);
            String tamanoFicheroSolicitado="";
            File file = null;
            File archivo=null;
            String state;
            state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {

                File root = Environment.getExternalStorageDirectory();
                File directorio = new File(root.getAbsolutePath() + "/ManetFiles");
                if (!directorio.exists()) {
                    directorio.mkdir();
                }
                Log.d(TAG,"Archivo solicitado del cloud: "+nombreFicheroSolicitado);
                archivo = new File(directorio,nombreFicheroSolicitado);
            } else {
                Log.d(TAG, "Almacenamiento externo no encontrado");
            }
            try {
                tamanoFicheroSolicitado = Long.toString(archivo.length());
                InputStream fileInputStream=new FileInputStream(archivo);
                AsyncTask hebraTXClusterHead = new HebraTX(mActivity, clientSocketRecepcion, "<SolicitudEnviarArchivo><NombreOrigen>" + "ClusterHead" + MainActivity.nombreOriginalWiFiDirect + "</NombreOrigen>"
                        + "<IPorig>" + "192.168.49.1" + "</IPorig>"+"</SolicitudEnviarArchivo>", misDatos);
                hebraTXClusterHead.executeOnExecutor(THREAD_POOL_EXECUTOR);
                //Iniciamos la hebra para enviarlo
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                AsyncTask hebraTXClusterheadArchivo = null;
                hebraTXClusterheadArchivo = new HebraTX(mActivity,clientSocketRecepcion, misDatos, 1, fileInputStream, nombreFicheroSolicitado, tamanoFicheroSolicitado);
                hebraTXClusterheadArchivo.executeOnExecutor(THREAD_POOL_EXECUTOR);



            }catch (Exception ex){
                ex.printStackTrace();
                Log.d(TAG,"Dicho archivo pedido no existe");
            }
        }
}






















