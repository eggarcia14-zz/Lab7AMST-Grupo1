package com.amst.grupo2;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Registros extends AppCompatActivity {

    public BarChart graficoBarras;
    private RequestQueue ListaRequest = null;
    private LinearLayout contenedorTemperaturas;
    private Map<String, TextView> temperaturasTVs;
    private Map<String, TextView> fechasTVs;
    private Registros contexto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registros);
        setTitle("Grafico de barras");
        temperaturasTVs = new HashMap<String,TextView>();
        fechasTVs = new HashMap<String,TextView>();
        ListaRequest = Volley.newRequestQueue(this);
        contexto = this;
        /* GRAFICO */
        this.iniciarGrafico();
        this.solicitarTemperaturas();
    }

    public void iniciarGrafico() {
        graficoBarras = findViewById(R.id.barChart);
        graficoBarras.getDescription().setEnabled(false);
        graficoBarras.setMaxVisibleValueCount(60);
        graficoBarras.setPinchZoom(false);
        graficoBarras.setDrawBarShadow(false);
        graficoBarras.setDrawGridBackground(false);
        XAxis xAxis = graficoBarras.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        graficoBarras.getAxisLeft().setDrawGridLines(false);
        graficoBarras.animateY(1500);
        graficoBarras.getLegend().setEnabled(false);
    }
    public void solicitarTemperaturas(){
            String url_registros = "https://amstdb-lab.herokuapp.com/db/logTres";
            JsonArrayRequest requestRegistros = new JsonArrayRequest(
                    Request.Method.GET, url_registros, null,
                    response -> {
                        mostrarTemperaturas(response);
                        actualizarGrafico(response);
                    }, error -> System.out.println(error)
            );
            ListaRequest.add(requestRegistros);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void registrarTemperatura(View view) {
        EditText temp = findViewById(R.id.temptxt);
        Double temperatura = Double.parseDouble(temp.getText().toString());
        String postUrl = "https://amstdb-lab.herokuapp.com/db/logTres";
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        JSONObject postData = new JSONObject();
        try {
            String fechaHoy = (java.time.LocalDate.now()).toString();
            postData.put("date_created", fechaHoy);
            postData.put("key", "temperatura");
            postData.put("value", temperatura );

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, postUrl, postData, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                System.out.println(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });

        requestQueue.add(jsonObjectRequest);
    }

    private void mostrarTemperaturas(JSONArray temperaturas){
            String registroId;
            JSONObject registroTemp;
            LinearLayout nuevoRegistro;
            TextView fechaRegistro;
            TextView valorRegistro;
            Button deleteBtn;
            contenedorTemperaturas = findViewById(R.id.cont_temperaturas);
            LinearLayout.LayoutParams parametrosLayout = new
                    LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            try {
                for (int i = 0; i < temperaturas.length(); i++) {
                    registroTemp =temperaturas.getJSONObject(i);
                    registroId = registroTemp.getString("id");
                    if( registroTemp.getString("key").equals("temperatura")){
                        if( temperaturasTVs.containsKey(registroId) &&
                                fechasTVs.containsKey(registroId) ){
                            fechaRegistro = fechasTVs.get(registroId);
                            valorRegistro = temperaturasTVs.get(registroId);
                            fechaRegistro.setText(registroTemp.getString("date_created"));
                            valorRegistro.setText(registroTemp.getString("value") + " C");
                        } else {
                            nuevoRegistro = new LinearLayout(this);
                            nuevoRegistro.setOrientation(LinearLayout.HORIZONTAL);
                            fechaRegistro = new TextView(this);
                            fechaRegistro.setLayoutParams(parametrosLayout);
                            fechaRegistro.setText(registroTemp.getString("date_created"));
                            nuevoRegistro.addView(fechaRegistro);
                            valorRegistro = new TextView(this);
                            valorRegistro.setLayoutParams(parametrosLayout);
                            valorRegistro.setText(registroTemp.getString("value") + " C");
                            nuevoRegistro.addView(valorRegistro);
                            deleteBtn = new Button(this);
                            deleteBtn.setText("X");
                            deleteBtn.setMinWidth(25);
                            deleteBtn.setMaxWidth(25);
                            String finalRegistroId = registroId;
                            deleteBtn.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    borrarRegistro(finalRegistroId);
                                }
                            });
                            nuevoRegistro.addView(deleteBtn);
                            contenedorTemperaturas.addView(nuevoRegistro);
                            fechasTVs.put(registroId, fechaRegistro);
                            temperaturasTVs.put(registroId, valorRegistro);
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                System.out.println("error");
            }
    }

    private void borrarRegistro(String finalRegistroId) {
        String url = "https://amstdb-lab.herokuapp.com/api/logTres/" + finalRegistroId;
        StringRequest dr = new StringRequest(Request.Method.DELETE, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        contenedorTemperaturas.removeAllViewsInLayout();
                        temperaturasTVs = new HashMap<String,TextView>();
                        fechasTVs = new HashMap<String,TextView>();
                        solicitarTemperaturas();
                        Toast.makeText(getApplicationContext(), "Registro Eliminado", Toast.LENGTH_LONG).show();
                    }
                }, new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }
        );
        ListaRequest.add(dr);
    }

    private void actualizarGrafico(JSONArray temperaturas){
        JSONObject registro_temp;
        String temp;
        String date;
        int count = 1;
        float temp_val;
        ArrayList<BarEntry> dato_temp = new ArrayList<>();
        try {
            for (int i = 0; i < temperaturas.length(); i++) {
                registro_temp =temperaturas.getJSONObject(i);
                if( registro_temp.getString("key").equals("temperatura")){
                    temp = registro_temp.getString("value");
                    date = registro_temp.getString("date_created");
                    temp_val = Float.parseFloat(temp);
                    dato_temp.add(new BarEntry(count, temp_val));
                    count++;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            System.out.println("error");
        }
        llenarGrafico(dato_temp);
    }
    private void llenarGrafico(ArrayList<BarEntry> dato_temp){
        BarDataSet temperaturasDataSet;
        if ( graficoBarras.getData() != null &&
                graficoBarras.getData().getDataSetCount() > 0) {
            temperaturasDataSet = (BarDataSet)
                    graficoBarras.getData().getDataSetByIndex(0);
            temperaturasDataSet.setValues(dato_temp);
            graficoBarras.getData().notifyDataChanged();
            graficoBarras.notifyDataSetChanged();
        } else {
            temperaturasDataSet = new BarDataSet(dato_temp, "Data Set");
            temperaturasDataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);
            temperaturasDataSet.setDrawValues(true);
            ArrayList<IBarDataSet> dataSets = new ArrayList<>();
            dataSets.add(temperaturasDataSet);
            BarData data = new BarData(dataSets);
            graficoBarras.setData(data);
            graficoBarras.setFitBars(true);
        }
        graficoBarras.invalidate();
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                solicitarTemperaturas(), 3000);
    }

}