@file:Suppress("DEPRECATION")

package com.example.rutacasa

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*

//variables a utilizar para marcar la linea, y sacar la localizacion actual
class MainActivity : AppCompatActivity() {
    //libreria para sacar lalocalizacion actual
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var btnVerRuta: Button
    private var map: MapView? = null
    private var line = Polyline()
    private var start: String = ""
    private var end: String = ""
    private val apiService: ApiService by lazy {
        Directions.apiService
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx: Context = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        setContentView(R.layout.activity_main)

        //Inicializando la variable donde se guardara la localizacion actual
        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this)

        //
        //Inicialoizar el mapa
        map = findViewById<View>(R.id.map) as MapView
        map!!.setTileSource(TileSourceFactory.MAPNIK)


        //MapController
        val mapController = map!!.controller
        mapController.setZoom(10)
        val startPoint =GeoPoint(
            20.028513, -100.721827)
        mapController.setCenter(startPoint)

        //Add markers
        val markerStart = Marker(map)
        markerStart.isDraggable = true
        markerStart.position = GeoPoint(20.028513, -100.721827)
        markerStart.title = "ACAMBARITO"
        map!!.overlays.add(markerStart)

        val markerEnd = Marker(map)
        markerEnd.isDraggable = true
        markerEnd.position = GeoPoint(20.139398208378335, -101.15073143396242)
        markerEnd.title = "ITSUR"
        map?.overlays?.add(markerEnd)


        btnVerRuta = findViewById(R.id.btnCalcularRuta)
        //Bandera para saber si se limpia o se marca la ruta
        var aux = false

        btnVerRuta.setOnClickListener{
            fecthLocation()
            if (!aux) {
                line = Polyline()
                drawRoute(markerStart.position, markerEnd.position)
                btnVerRuta.text = "Quitar Ruta"
                aux=true
            } else {
                btnVerRuta.text = "Mostrar Ruta"
                map?.overlays?.remove(line)
                aux =false
            }
        }

        map?.invalidate()
    }

    //La variable del provider es la que se tare la loaclizacuion y verifica permisos
    private fun fecthLocation() {
        val task= fusedLocationProviderClient.lastLocation
       if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)!=
               PackageManager.PERMISSION_GRANTED &&
           ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION)!=
               PackageManager.PERMISSION_GRANTED){
           ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),101)
           //si no los tene le pide al usuario que los habilite
           return
       }
        //mensaje que muestra la aplicacion actual
        task.addOnSuccessListener {
            if(it!=null){
                Toast.makeText(applicationContext, "${it.latitude} ${it.longitude}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //DATOS DEL RETROFIT que dubja la ruta mediante la info que contiene la apiservice
    private fun drawRoute(startPoint: GeoPoint, endPoint: GeoPoint){
        CoroutineScope(Dispatchers.IO).launch {
            end = "${endPoint.longitude},${endPoint.latitude}"
            start = "${startPoint.longitude},${startPoint.latitude}"

            val points = apiService.getRoute("5b3ce3597851110001cf62488d38aa048bea4519ae3177df424c06de", start, end)
            val features = points.features

            //este bloque se encarga de dibujar la linea
            for (feature in features) {
                val geometry = feature.geometry
                val coordinates = geometry.coordinates
                for (coordinate in coordinates) {
                    val point = GeoPoint(coordinate[1], coordinate[0])
                    line.addPoint(point)
                }
                //se le meta al map que viene siendo el view
                map?.overlays?.add(line)
            }
        }
    }

}