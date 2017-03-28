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
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.Manet.AppManet.ManetWifiDirectProyecto.R;

public class ActivityConfiguracion extends Activity {

    public static int puerto=7070;
    public static boolean CHforzado=false;
    public static int tiempoBusquedaDispositivos=20000;
    public static int tiempoBusquedaGrupos=20000;


    CheckBox CHforzadoCheckBox;
    EditText editTextPuerto;
    Button botonCambiarPuerto;

    CheckBox tiempoAlgoritmoRapido;
    CheckBox tiempoAlgoritmoNormal;
    CheckBox tiempoAlgoritmoLento;
    CheckBox tiempoAlgoritmoMuyLento;

    static boolean tRapido=false;
    static boolean tNormal=false;
    static boolean tLento=false;
    static boolean tMuyLento=false;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracion);

        CHforzadoCheckBox=(CheckBox)findViewById(R.id.chechCHforzado);

        tiempoAlgoritmoRapido=(CheckBox)findViewById(R.id.checktiemporapido);
        tiempoAlgoritmoNormal=(CheckBox)findViewById(R.id.checktiemponormal);
        tiempoAlgoritmoLento=(CheckBox)findViewById(R.id.checktiempolento);
        tiempoAlgoritmoMuyLento=(CheckBox)findViewById(R.id.checktiempomuylento);

        editTextPuerto=(EditText)findViewById(R.id.editTextPuerto);
        botonCambiarPuerto=(Button)findViewById(R.id.buttonCambiarPuerto);

        CHforzadoCheckBox.setChecked(CHforzado);
        editTextPuerto.setText(Integer.toString(puerto));

        if(!tRapido && !tNormal && !tLento && !tMuyLento){
            tiempoAlgoritmoLento.setChecked(true);
            tLento=true;
        }


        if(tRapido){
            tiempoAlgoritmoRapido.setChecked(true);
        }
        else if(tNormal){
            tiempoAlgoritmoNormal.setChecked(true);
        }
        else if(tLento){
            tiempoAlgoritmoLento.setChecked(true);
        }
        else if(tMuyLento){
            tiempoAlgoritmoMuyLento.setChecked(true);
        }


        CHforzadoCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CHforzado = CHforzadoCheckBox.isChecked();
            }
        });

        botonCambiarPuerto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                puerto=Integer.parseInt(editTextPuerto.getText().toString());
                Toast.makeText(getApplicationContext(),"Puerto cambiado a :",Toast.LENGTH_SHORT).show();
            }
        });

        tiempoAlgoritmoRapido.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tiempoAlgoritmoRapido.isChecked()){
                    tiempoBusquedaDispositivos=5000;
                    tiempoBusquedaGrupos=10000;


                    tiempoAlgoritmoNormal.setChecked(false);
                    tiempoAlgoritmoLento.setChecked(false);
                    tiempoAlgoritmoMuyLento.setChecked(false);

                    tRapido=true;
                    tNormal=false;
                    tLento=false;
                    tMuyLento=false;
                }else{
                    tiempoAlgoritmoRapido.setChecked(true);
                }

            }
        });

        tiempoAlgoritmoNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tiempoAlgoritmoNormal.isChecked()){
                    tiempoBusquedaDispositivos=10000;
                    tiempoBusquedaGrupos=15000;

                    tiempoAlgoritmoRapido.setChecked(false);
                    tiempoAlgoritmoLento.setChecked(false);
                    tiempoAlgoritmoMuyLento.setChecked(false);

                    tNormal=true;
                    tRapido=false;
                    tLento=false;
                    tMuyLento=false;
                }else{
                    tiempoAlgoritmoNormal.setChecked(true);
                }

            }
        });

        tiempoAlgoritmoLento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tiempoAlgoritmoLento.isChecked()) {
                    tiempoBusquedaDispositivos = 20000;
                    tiempoBusquedaGrupos = 20000;

                    tiempoAlgoritmoNormal.setChecked(false);
                    tiempoAlgoritmoRapido.setChecked(false);
                    tiempoAlgoritmoMuyLento.setChecked(false);

                    tLento=true;
                    tNormal=false;
                    tRapido=false;
                    tMuyLento=false;
                }else{
                    tiempoAlgoritmoLento.setChecked(true);
                }

            }
        });

        tiempoAlgoritmoMuyLento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tiempoAlgoritmoMuyLento.isChecked()){
                    tiempoBusquedaDispositivos=30000;
                    tiempoBusquedaGrupos=30000;

                    tiempoAlgoritmoNormal.setChecked(false);
                    tiempoAlgoritmoLento.setChecked(false);
                    tiempoAlgoritmoRapido.setChecked(false);

                    tMuyLento=true;
                    tNormal=false;
                    tLento=false;
                    tRapido=false;
                }else{
                    tiempoAlgoritmoMuyLento.setChecked(true);
                }

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity_configuracion, menu);
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
