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

package com.Manet.AppManet.ManetWifiDirectProyecto.Datos;

import java.io.Serializable;

/**
 * Esta clase contiene los datos del dispositivo en cuanto se refiere a identificación, datos del
 * algoritmo K-CONID referidos al dispositivo, etcétera.
 */
public class MisDatos implements Serializable{

    private boolean soyClusterHead;
    private int NID;
    private int numeroUsuariosAlzanzables; /*** Parametro Conectividad ***/
    private String nombreKConid; /***Es el nombre que se establece tras la primera fase,
                                     contiene el Parámetro de Conectividad y el NID
                                       Si tiene mas de 32 caracteres no se cambia***/

    public MisDatos(){
        soyClusterHead=true;
        NID=0;
        numeroUsuariosAlzanzables=0;
        nombreKConid="";
    }


    /**
     * Mediante éste metodo, generaremos un NID, Número de identificación aleatorio.
     *
     * @return
     */
    public void generarNumeroAleatorioNID(){
        NID=-1;

        NID= 1+(int)(9999*Math.random());

    }

    public void setNombreKCONID(String nombreOriginal){
        /**Formato nombre K-Conid = <C>nºUsuariosAlcanzables<I>nºIdentificacion</I>nombreOriginal*/
        nombreKConid="<C>"+numeroUsuariosAlzanzables+"<I>"+NID+"</I>"+nombreOriginal;
    }

    public void aniadirUsuarioAlcanzable(){
        numeroUsuariosAlzanzables++;
    }

    public void resetearNumeroUsuariosAlcanzables(){
        numeroUsuariosAlzanzables=0;
    }

    public int getNumeroUsuariosAlcanzables(){
        return numeroUsuariosAlzanzables;
    }

    public int getNID(){
        return NID;
    }

    public boolean getSoyClusterHead(){
        return soyClusterHead;
    }

    public void setSoyClusterHead(boolean setSoyCH){
        this.soyClusterHead =setSoyCH;
    }

    public String getNombreKCONID(){
        return nombreKConid;
    }




}
