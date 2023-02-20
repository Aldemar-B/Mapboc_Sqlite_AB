package com.example.mapbox_sqlite_ab

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.mapbox_sqlite_ab.dao.DAO
import com.example.mapbox_sqlite_ab.dao.database.DataBase
import com.example.mapbox_sqlite_ab.dao.entity.map_points
import com.example.mapbox_sqlite_ab.databinding.ActivityMainBinding
import com.example.mapbox_sqlite_ab.utils.LocationPermissionHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.gestures.*
import com.mapbox.maps.plugin.locationcomponent.DefaultLocationProvider
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList


class MainActivity : AppCompatActivity(), OnMapClickListener, OnMapLongClickListener, OnPointClickListener {

    var dao: DAO? = null
    var mBound: Boolean? = null

    //base de datos
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var viewAnnotationManager: ViewAnnotationManager
    private lateinit var locationPermissionHelper: LocationPermissionHelper
    private lateinit var binding: ActivityMainBinding
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var User_Point: Point? = null
    private var isButtonPressed = false
    private var favoritebookPressed = false
    private val pointList = CopyOnWriteArrayList<Feature>()
    private var markerId = 0
    private var m_Text = ""
    private var markerWidth = 0
    private var markerHeight = 0
    private val asyncInflater by lazy { AsyncLayoutInflater(this) }
    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        // Jump to the current indicator position
        if (User_Point != it) {
            // Set the flag to true to indicate that the action has been performed
            // Perform the action here
            binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
            binding.mapView.gestures.focalPoint =
                binding.mapView.getMapboxMap().pixelForCoordinate(it)
            User_Point = it
        }

    }
    var Pstyle: String? = null
    var bitmap: Bitmap? = null
    var items_user: ArrayList<map_points>? = null
    var recyclerView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //find ID
        var butonPosition = findViewById<FloatingActionButton>(R.id.Ubication_user)
        var favorite_ubication_user =
            findViewById<FloatingActionButton>(R.id.favorite_allubications_user)
        var menu = findViewById<FloatingActionButton>(R.id.favorite_ubications)
        recyclerView = findViewById<RecyclerView>(R.id.usersRecyclerView)
        mapView = findViewById(R.id.mapView)
        //creacion de base de datos
        val adb = DataBase(this, "mapbox.db")
        adb.createDataBase()

        viewAnnotationManager = binding.mapView.viewAnnotationManager
        bitmap = BitmapFactory.decodeResource(resources, R.drawable.blue_marker_view)
        markerWidth = bitmap!!.width
        markerHeight = bitmap!!.height
        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
        locationPermissionHelper.checkPermissions {
            binding.mapView.apply {
                getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
                    // Disable scroll gesture, since we are updating the camera position based on the indicator location.
                    gestures.scrollEnabled = false
                    gestures.addOnMapClickListener { point ->
                        location
                            .isLocatedAt(point) { isPuckLocatedAtPoint ->
                                if (isPuckLocatedAtPoint) {
                                    Toast.makeText(
                                        context,
                                        "Clicked on location puck",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        true
                    }
                    gestures.addOnMapLongClickListener { point ->
                        location.isLocatedAt(point) { isPuckLocatedAtPoint ->
                            if (isPuckLocatedAtPoint) {
                                Toast.makeText(
                                    context,
                                    "Long-clicked on location puck",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                        true
                    }
                    val locationProvider = location.getLocationProvider() as DefaultLocationProvider
                    locationProvider.addOnCompassCalibrationListener {
                        Toast.makeText(context, "Compass needs to be calibrated", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }

        ResourceOptionsManager.getDefault(this, getString(R.string.mapbox_access_token))
            .update { tileStoreUsageMode(TileStoreUsageMode.READ_ONLY) }
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val locationDialog = MaterialAlertDialogBuilder(this)
            locationDialog.setTitle("Atencion")
            locationDialog.setMessage("Para continuar, activa la ubicacion del dispositivo, que usa el servicio de ubicacion de Google")
            locationDialog.setCancelable(false)
            locationDialog.setPositiveButton("Confirmar") { dialogInterface, i ->
                val intent = Intent(ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            locationDialog.create().show()
        }



        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //aplicacion de estilo y listener
        mapboxMap = binding.mapView.getMapboxMap().apply {
            loadStyle(styleExtension = prepareStyle(Style.MAPBOX_STREETS, bitmap!!)) {
                addOnMapClickListener(this@MainActivity)
                addOnMapLongClickListener(this@MainActivity)
                Toast.makeText(this@MainActivity, STARTUP_TEXT, Toast.LENGTH_LONG).show()
                Pstyle = Style.MAPBOX_STREETS
            }
        }

        butonPosition?.setOnClickListener {
            getLocation()
        }
        favorite_ubication_user?.setOnClickListener {
            isButtonPressed = !isButtonPressed
            if (isButtonPressed) {
                reload_PM(isButtonPressed)
            } else {
                reload_PM(isButtonPressed)
            }
        }
        menu?.setOnClickListener {
            carga_vpoint()
        }

    }

    private fun getLocation() {
        binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().zoom(14.0).center(User_Point).build())
    }

    fun carga_vpoint() {
        favoritebookPressed = !favoritebookPressed
        if (favoritebookPressed) {
            items_user = dao?.search_pm()
            val adapter = UserAdapter(items_user!!)
            recyclerView?.isGone = false
            adapter.setOnPointClickListener(this)
            recyclerView?.adapter = adapter
            recyclerView?.layoutManager = LinearLayoutManager(this@MainActivity)
        } else {
            recyclerView?.isGone = true
        }
    }



    private fun prepareStyle(styleUri: String, bitmap: Bitmap) = style(styleUri) {
        +image(BLUE_ICON_ID) {
            bitmap(bitmap)
        }
        +geoJsonSource(id = "populated_places") {
            url("https://d2ad6b4ur7yvpq.cloudfront.net/naturalearth-3.3.0/ne_50m_populated_places_simple.geojson")
            cluster(false)
        }
        +geoJsonSource(SOURCE_ID) {
            featureCollection(FeatureCollection.fromFeatures(pointList))
        }
        +symbolLayer(LAYER_ID, SOURCE_ID) {
            iconImage(BLUE_ICON_ID)
            iconAnchor(IconAnchor.BOTTOM)
            iconAllowOverlap(false)
        }
        if (styleUri == Style.MAPBOX_STREETS) {
            Pstyle = Style.MAPBOX_STREETS
            +circleLayer(layerId = "populated_placesCircle", sourceId = "populated_places") {
                circleRadius(get { literal("rank_max") }) //importancia de la ciudad
                circleColor(Color.rgb(165, 51, 255))
                circleOpacity(0.2)
                circleStrokeColor(Color.WHITE)
            }
        }
        if (styleUri == Style.OUTDOORS) {
            Pstyle = Style.OUTDOORS
            +circleLayer(layerId = "populated_placesCircle", sourceId = "populated_places") {
                circleRadius(get { literal("rank_max") }) //importancia de la ciudad
                circleColor(Color.rgb(255, 51, 51))
                circleOpacity(0.2)
                circleStrokeColor(Color.WHITE)
            }
        }
        if (styleUri == Style.SATELLITE) {
            Pstyle = Style.SATELLITE
            +circleLayer(layerId = "populated_placesCircle", sourceId = "populated_places") {
                circleRadius(get { literal("rank_max") }) //importancia de la ciudad
                circleColor(Color.rgb(51, 175, 255))
                circleOpacity(0.2)
                circleStrokeColor(Color.WHITE)
            }
        }
        if (styleUri == Style.SATELLITE_STREETS) {
            Pstyle = Style.SATELLITE_STREETS
            +circleLayer(layerId = "populated_placesCircle", sourceId = "populated_places") {
                circleRadius(get { literal("rank_max") }) //importancia de la ciudad
                circleColor(Color.rgb(51, 255, 144))
                circleOpacity(0.2)
                circleStrokeColor(Color.WHITE)
            }
        }
        if (styleUri == Style.LIGHT) {
            Pstyle = Style.LIGHT
            +circleLayer(layerId = "populated_placesCircle", sourceId = "populated_places") {
                circleRadius(get { literal("rank_max") }) //importancia de la ciudad
                circleColor(Color.rgb(14, 102, 149))
                circleOpacity(0.2)
                circleStrokeColor(Color.WHITE)
            }
        }

        if (styleUri == Style.DARK) {
            Pstyle = Style.DARK
            +circleLayer(layerId = "populated_placesCircle", sourceId = "populated_places") {
                circleRadius(get { literal("rank_max") }) //importancia de la ciudad
                circleColor(Color.rgb(221, 240, 38))
                circleOpacity(0.2)
                circleStrokeColor(Color.WHITE)
            }
        }
        if (styleUri == Style.TRAFFIC_DAY) {
            Pstyle = Style.TRAFFIC_DAY
            +circleLayer(layerId = "populated_placesCircle", sourceId = "populated_places") {
                circleRadius(get { literal("rank_max") }) //importancia de la ciudad
                circleColor(Color.rgb(240, 160, 38))
                circleOpacity(0.2)
                circleStrokeColor(Color.WHITE)
            }
        }
        if (styleUri == Style.TRAFFIC_NIGHT) {
            Pstyle = Style.TRAFFIC_NIGHT
            +circleLayer(layerId = "populated_placesCircle", sourceId = "populated_places") {
                circleRadius(get { literal("rank_max") }) //importancia de la ciudad
                circleColor(Color.rgb(157, 38, 240))
                circleOpacity(0.2)
                circleStrokeColor(Color.WHITE)
            }
        }


    }

    override fun onMapLongClick(point: Point): Boolean {
        val markerId = addMarkerAndReturnId(point)
        addViewAnnotation(point, markerId)
        return true
    }

    override fun onMapClick(point: Point): Boolean {
        mapboxMap.queryRenderedFeatures(
            RenderedQueryGeometry(mapboxMap.pixelForCoordinate(point)),
            RenderedQueryOptions(listOf(LAYER_ID), null)
        ) {
            onFeatureClicked(it) { feature ->
                if (feature.id() != null) {
                    viewAnnotationManager.getViewAnnotationByFeatureId(feature.id()!!)
                        ?.toggleViewVisibility()
                }
            }
        }
        return true
    }

    private fun onFeatureClicked(
        expected: Expected<String, List<QueriedFeature>>,
        onFeatureClicked: (Feature) -> Unit
    ) {
        if (expected.isValue && expected.value?.size!! > 0) {
            expected.value?.get(0)?.feature?.let { feature ->
                onFeatureClicked.invoke(feature)
            }
        }
    }

    private fun View.toggleViewVisibility() {
        visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun addMarkerAndReturnId(point: Point): String {
        val currentId = "${MARKER_ID_PREFIX}${(markerId++)}"
        pointList.add(Feature.fromGeometry(point, null, currentId))
        val featureCollection = FeatureCollection.fromFeatures(pointList)
        mapboxMap.getStyle { style ->
            style.getSourceAs<GeoJsonSource>(SOURCE_ID)?.featureCollection(featureCollection)
        }
        return currentId
    }

    private fun reload_PM(isButtonPressed: Boolean) {
        items_user = dao?.search_pm()
        if (!items_user.isNullOrEmpty()) {
            for (i in items_user!!) {
                val Point = Point.fromLngLat(i.Point_longitud, i.Point_latitude)
                val markerId = addMarkerAndReturnId(Point)
                reloadAddViewAnnotation(Point, markerId, i.Name, isButtonPressed)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun addViewAnnotation(point: Point, markerId: String) {
        viewAnnotationManager.addViewAnnotation(
            resId = R.layout.item_callout_view,
            options = viewAnnotationOptions {
                geometry(point)
                associatedFeatureId(markerId)
                anchor(ViewAnnotationAnchor.BOTTOM)
                allowOverlap(false)
            },
            asyncInflater = asyncInflater
        ) { viewAnnotation ->
            viewAnnotation.visibility = View.GONE
            // calculate offsetY manually taking into account icon height only because of bottom anchoring
            viewAnnotationManager.updateViewAnnotation(
                viewAnnotation,
                viewAnnotationOptions {
                    offsetY(markerHeight)
                }
            )

            viewAnnotation.findViewById<TextView>(R.id.textNativeView).text =
                "Punto nuevo".format(point.latitude(), point.longitude())
            viewAnnotation.findViewById<ImageView>(R.id.closeNativeView).setOnClickListener { _ ->
                //ALERTA custom
                SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Punto")
                    .setContentText("¿Esta seguro que desea eliminar el punto?")
                    .setConfirmButtonBackgroundColor(Color.parseColor("#0065b2"))
                    .setCancelButtonBackgroundColor(Color.parseColor("#b24c00"))
                    .setConfirmText("SI")
                    .setConfirmClickListener { sDialog ->
                        sDialog.dismissWithAnimation()
                        dao?.delete_p(point)
                        viewAnnotationManager.removeViewAnnotation(viewAnnotation)
                        pointList.remove(Feature.fromGeometry(point, null, markerId))
                        mapboxMap = binding.mapView.getMapboxMap().apply {
                            loadStyle(styleExtension = prepareStyle(Pstyle!!, bitmap!!)) {
                                addOnMapClickListener(this@MainActivity)
                                addOnMapLongClickListener(this@MainActivity)
                            }
                        }
                    }
                    .setCancelButton("NO") { sDialog -> sDialog.dismissWithAnimation() }
                    .show()
            }
            viewAnnotation.findViewById<Button>(R.id.selectButton).setOnClickListener { b ->
                val button = b as Button
                val isSelected = button.text.toString().equals("Presione para guardar", true)
                val pxDelta =
                    (if (isSelected) SELECTED_ADD_COEF_DP.dpToPx() else -SELECTED_ADD_COEF_DP.dpToPx()).toInt()
                if (isSelected) {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Nombre del punto")
                    val input = EditText(this)
                    input.inputType = InputType.TYPE_CLASS_TEXT
                    builder.setView(input)
                    builder.setPositiveButton("OK") { dialog, which ->
                        m_Text = input.text.toString()
                        viewAnnotation.findViewById<TextView>(R.id.textNativeView).text =
                            m_Text.format(point.latitude(), point.longitude())
                        dao?.save_p(m_Text, point)
                        button.text = "GUARDADO!"
                        items_user = dao?.search_pm()
                    }
                    builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
                    builder.show()
                }
                viewAnnotationManager.updateViewAnnotation(
                    viewAnnotation,
                    viewAnnotationOptions {
                        selected(isSelected)
                    }
                )
                (button.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    bottomMargin += pxDelta
                    rightMargin += pxDelta
                    leftMargin += pxDelta
                }
                button.requestLayout()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun reloadAddViewAnnotation(
        point: Point,
        markerId: String,
        name: String?,
        isButtonPressed: Boolean
    ) {
        if (isButtonPressed) {
            viewAnnotationManager.addViewAnnotation(
                resId = R.layout.item_callout_view,
                options = viewAnnotationOptions {
                    geometry(point)
                    associatedFeatureId(markerId)
                    anchor(ViewAnnotationAnchor.BOTTOM)
                    allowOverlap(false)
                },
                asyncInflater = asyncInflater
            ) { viewAnnotation ->
                viewAnnotation.visibility = View.GONE
                // calculate offsetY manually taking into account icon height only because of bottom anchoring
                viewAnnotationManager.updateViewAnnotation(
                    viewAnnotation,
                    viewAnnotationOptions {
                        offsetY(markerHeight)
                    }
                )

                viewAnnotation.findViewById<TextView>(R.id.textNativeView).text =
                    name!!.format(point.latitude(), point.longitude())
                viewAnnotation.findViewById<ImageView>(R.id.closeNativeView)
                    .setOnClickListener { _ ->
                        //ALERTA custom
                        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("Punto")
                            .setContentText("¿Esta seguro que desea eliminar el punto?")
                            .setConfirmButtonBackgroundColor(Color.parseColor("#0065b2"))
                            .setCancelButtonBackgroundColor(Color.parseColor("#b24c00"))
                            .setConfirmText("SI")
                            .setConfirmClickListener { sDialog ->
                                sDialog.dismissWithAnimation()
                                dao?.delete_p(point)
                                viewAnnotationManager.removeViewAnnotation(viewAnnotation)
                                pointList.remove(Feature.fromGeometry(point, null, markerId))
                                mapboxMap = binding.mapView.getMapboxMap().apply {
                                    loadStyle(styleExtension = prepareStyle(Pstyle!!, bitmap!!)) {
                                        addOnMapClickListener(this@MainActivity)
                                        addOnMapLongClickListener(this@MainActivity)
                                    }
                                }

                            }
                            .setCancelButton("NO") { sDialog -> sDialog.dismissWithAnimation() }
                            .show()
                    }
                var selectButton = viewAnnotation.findViewById<Button>(R.id.selectButton)
                selectButton.visibility = GONE
            }
        } else {
            viewAnnotationManager.removeAllViewAnnotations()
            pointList.removeAll(pointList)
            mapboxMap = binding.mapView.getMapboxMap().apply {
                loadStyle(styleExtension = prepareStyle(Pstyle!!, bitmap!!)) {
                    addOnMapClickListener(this@MainActivity)
                    addOnMapLongClickListener(this@MainActivity)
                }
            }
        }
    }

    private fun Float.dpToPx() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        this@MainActivity.resources.displayMetrics
    )

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Maneja las selecciones del usuario
        when (item.itemId) {
            R.id.MAPBOX_STREETS -> {
                // Cambia el estilo del mapa a Mapbox Streets
                mapboxMap = binding.mapView.getMapboxMap().apply {
                    loadStyle(styleExtension = prepareStyle(Style.MAPBOX_STREETS, bitmap!!)) {
                        addOnMapClickListener(this@MainActivity)
                        addOnMapLongClickListener(this@MainActivity)
                    }
                }
                item.isChecked = true;
                return true
            }
            R.id.OUTDOORS -> {
                mapboxMap = binding.mapView.getMapboxMap().apply {
                    loadStyle(styleExtension = prepareStyle(Style.OUTDOORS, bitmap!!)) {
                        addOnMapClickListener(this@MainActivity)
                        addOnMapLongClickListener(this@MainActivity)
                    }
                }
                item.isChecked = true;
                return true
            }
            R.id.SATELLITE -> {
                mapboxMap = binding.mapView.getMapboxMap().apply {
                    loadStyle(styleExtension = prepareStyle(Style.SATELLITE, bitmap!!)) {
                        addOnMapClickListener(this@MainActivity)
                        addOnMapLongClickListener(this@MainActivity)
                    }
                }
                item.isChecked = true;
                return true
            }
            R.id.SATELLITE_STREETS -> {
                mapboxMap = binding.mapView.getMapboxMap().apply {
                    loadStyle(styleExtension = prepareStyle(Style.SATELLITE_STREETS, bitmap!!)) {
                        addOnMapClickListener(this@MainActivity)
                        addOnMapLongClickListener(this@MainActivity)
                    }
                }
                item.isChecked = true;
                return true
            }
            R.id.LIGHT -> {
                mapboxMap = binding.mapView.getMapboxMap().apply {
                    loadStyle(styleExtension = prepareStyle(Style.LIGHT, bitmap!!)) {
                        addOnMapClickListener(this@MainActivity)
                        addOnMapLongClickListener(this@MainActivity)
                    }
                }
                item.isChecked = true;
                return true
            }
            R.id.DARK -> {
                mapboxMap = binding.mapView.getMapboxMap().apply {
                    loadStyle(styleExtension = prepareStyle(Style.DARK, bitmap!!)) {
                        addOnMapClickListener(this@MainActivity)
                        addOnMapLongClickListener(this@MainActivity)
                    }
                }
                item.isChecked = true;
                return true
            }
            R.id.TRAFFIC_DAY -> {
                mapboxMap = binding.mapView.getMapboxMap().apply {
                    loadStyle(styleExtension = prepareStyle(Style.TRAFFIC_DAY, bitmap!!)) {
                        addOnMapClickListener(this@MainActivity)
                        addOnMapLongClickListener(this@MainActivity)
                    }
                }
                item.isChecked = true;
                return true
            }
            R.id.TRAFFIC_NIGHT -> {
                mapboxMap = binding.mapView.getMapboxMap().apply {
                    loadStyle(styleExtension = prepareStyle(Style.TRAFFIC_NIGHT, bitmap!!)) {
                        addOnMapClickListener(this@MainActivity)
                        addOnMapLongClickListener(this@MainActivity)
                    }
                }
                item.isChecked = true;
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.style_map, menu)
        return true
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (Pstyle.isNullOrEmpty()) {
            Pstyle = Style.MAPBOX_STREETS
        }
        mapboxMap = binding.mapView.getMapboxMap().apply {
            loadStyle(styleExtension = prepareStyle(Pstyle!!, bitmap!!)) {
                addOnMapClickListener(this@MainActivity)
                addOnMapLongClickListener(this@MainActivity)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (Pstyle.isNullOrEmpty()) {
            Pstyle = Style.MAPBOX_STREETS
        }
        mapboxMap = binding.mapView.getMapboxMap().apply {
            loadStyle(styleExtension = prepareStyle(Pstyle!!, bitmap!!)) {
                addOnMapClickListener(this@MainActivity)
                addOnMapLongClickListener(this@MainActivity)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onStart() {
        try {
            super.onStart()
            val intent = Intent(this, DAO::class.java)
            bindService(intent, mConnection, BIND_AUTO_CREATE)
            mapView.onStart()
            binding.mapView.location.addOnIndicatorPositionChangedListener(
                onIndicatorPositionChangedListener
            )
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    override fun onStop() {
        try {
            super.onStop()
            mapView?.onStop()
            binding.mapView.location.removeOnIndicatorPositionChangedListener(
                onIndicatorPositionChangedListener
            )
            if (mBound != null && mBound as Boolean) {
                unbindService(mConnection)
                mBound = false
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }

    }

    //base de datos
    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            try {
                val binder = service as DAO.LocalBinder
                dao = binder.getService()
                mBound = true
                //Genera LogCat de la aplicacion
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            try {
                mBound = false
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }


    private companion object {
        const val BLUE_ICON_ID = "red"
        const val SOURCE_ID = "source_id"
        const val LAYER_ID = "layer_id"
        const val MARKER_ID_PREFIX = "view_annotation_"
        const val SELECTED_ADD_COEF_DP: Float = 8f
        const val STARTUP_TEXT =
            "Haga click sostenido en el mapa para agregar un marcador con etiqueta"
    }

    override fun onPointClick(point: Point) {
        favoritebookPressed = false
        recyclerView?.isGone = true
        reload_PM(true)
        binding.mapView.getMapboxMap()
            .setCamera(CameraOptions.Builder().zoom(14.0).center(point).build())
    }

}