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

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.Manet.AppManet.ManetWifiDirectProyecto.Datos.MisDatos;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by Moya on 5 mar 2016.
 */
public class HebraTX extends AsyncTask<Object,String,Void> {
    //< entrada en inbackgroud, parametro onprogressupdate, retornoinbackground //

    private Context context;
    private Socket clientSocketTransmision;
    private PrintWriter out;
    private String mensaje;
    private String TAG="HebraTX";
    private MisDatos misDatos;
    private int tipoTransmision; //tipoTransmision=0 si transmitimos mensaje de chat, o 1 si archivo.


    private InputStream inputStream;
    private InputStream inputStreamArchivo;
    private OutputStream outputStream;
    private int bytesLeidos;
    private byte byteArrayArchivo[];
    private String nombreArchivo;
    private String tamanoArchivo; //En bytes

    //Constructor para la emision de mensaje
    public HebraTX(Context context, Socket clientSocketTransmision, String mensaje, MisDatos misDatos){
        this.context = context;
        this.clientSocketTransmision = clientSocketTransmision;
        this.mensaje = mensaje;
        this.misDatos=misDatos;
        this.tipoTransmision=0;
    }

    //Constructor para la emisión de archivo
    public HebraTX(Context context, Socket clientSocketTransmision,  MisDatos misDatos,int tipoTransmision,InputStream inputStreamArchivo,String nombreArchivo,String tamanoArchivo){
        this.context = context;
        this.clientSocketTransmision = clientSocketTransmision;
        this.misDatos=misDatos;
        this.tipoTransmision=tipoTransmision;
        this.inputStreamArchivo=inputStreamArchivo;
        this.nombreArchivo=nombreArchivo;
        this.tamanoArchivo=tamanoArchivo;
    }


    @Override
    protected Void doInBackground(Object[] params) {

        try {
            out = new PrintWriter(clientSocketTransmision.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(tipoTransmision==0) {
            try {
                out.println(mensaje);
                out.flush();
                publishProgress(mensaje);
            } catch (Exception e) {
                if (misDatos.getSoyClusterHead() == false) {
                    synchronized (context) {
                        publishProgress("CHPerdido");
                        Log.d(TAG, "Hemos perdido la conexión al servidor");
                        this.cancel(true);
                        System.exit(0);
                    }
                }
                e.printStackTrace();
            }
        }
        else if(tipoTransmision==1){
            try {
                inputStream =inputStreamArchivo;
                outputStream =clientSocketTransmision.getOutputStream();
                Thread.sleep(5000);
                Log.d(TAG, "Enviamos el nombre del fichero a transmitir: " + nombreArchivo);
                out.println("<NombreArchivo>" + nombreArchivo + "</NombreArchivo>");
                Log.d(TAG, "Enviamos el tamaño del fichero a transmitir: " + tamanoArchivo);
                Thread.sleep(5000);
                out.println("<TamanoArchivo>"+tamanoArchivo+"</TamanoArchivo>");
                Thread.sleep(2000);

                Log.d(TAG, "Enviamos el fichero");
                byteArrayArchivo=new byte[8192];

                int bytesRead;
                int offsetActual=0;
                int offsetAnterior=0;
                int bytesTotalesEnviados=0;
                while (bytesTotalesEnviados!=Integer.parseInt(tamanoArchivo)) {
                    bytesRead = inputStream.read(byteArrayArchivo, offsetActual, byteArrayArchivo.length - offsetActual);
                    outputStream.write(byteArrayArchivo, offsetActual, bytesRead);
                    offsetActual += bytesRead;
                    bytesTotalesEnviados+=bytesRead;
                    if(offsetActual==byteArrayArchivo.length){
                        byteArrayArchivo=new byte[8192];
                        offsetActual=0;
                    }

                }

                //Log.d(TAG,"Leyendo...");
                //inputStream.read(byteArrayArchivo, 0, byteArrayArchivo.length);
                inputStream.close();
                //Log.d(TAG, "Enviando...");
                //outputStream.write(byteArrayArchivo, 0, byteArrayArchivo.length);
                outputStream.flush();


                Log.d(TAG,"Fichero transmitido");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        return null;
    }



    @Override
    protected synchronized void onProgressUpdate(String... texto) {
        super.onProgressUpdate(texto);

        String palabra="";
        for(int i=0;i<texto.length;i++){
            palabra=palabra+texto[i];
        }

        if(palabra.equals("CHPerdido")){
            Toast.makeText(context, "Hemos perdido la conexión al servidor, reinicia el proceso", Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(Void o) {
        super.onPostExecute(o);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }


}
