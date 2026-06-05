package com.vincent.voicedrop.ui.screens

import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.vincent.voicedrop.R
import com.vincent.voicedrop.data.PlaceType
import com.vincent.voicedrop.location.GeocoderHelper
import com.vincent.voicedrop.ui.common.displayName
import com.vincent.voicedrop.ui.theme.VibrantCoral
import com.vincent.voicedrop.ui.viewmodel.MapPickerViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    placeId: Long?,
    onBack: () -> Unit,
    viewModel: MapPickerViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var query by remember { mutableStateOf("") }
    var searchError by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(PlaceType.OVERIGE) }
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    val loaded by viewModel.place.collectAsStateWithLifecycle()

    // Laad bestaande plek bij edit.
    LaunchedEffect(placeId) {
        if (placeId != null) {
            viewModel.load(placeId)
        } else {
            viewModel.clear()
        }
    }
    // Vul naam + map-positie zodra de geladen plek beschikbaar is.
    LaunchedEffect(loaded, mapView) {
        val place = loaded
        if (place != null && place.id == placeId) {
            if (name.isEmpty()) name = place.name
            selectedType = place.type
            mapView?.controller?.setCenter(GeoPoint(place.lat, place.lng))
            mapView?.controller?.setZoom(16.0)
        }
    }

    fun searchAddress() {
        if (query.isBlank()) return
        scope.launch {
            val point = GeocoderHelper.search(context, query)
            if (point != null) {
                searchError = false
                mapView?.controller?.animateTo(point)
                mapView?.controller?.setZoom(17.0)
            } else {
                searchError = true
            }
        }
    }

    fun hasLocation() = ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    fun centerOnLastLocation() {
        if (!hasLocation()) return
        try {
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && loaded == null && placeId == null) {
                    mapView?.controller?.setCenter(GeoPoint(loc.latitude, loc.longitude))
                }
            }
        } catch (e: SecurityException) {
        }
    }

    fun goToCurrentLocation() {
        if (!hasLocation()) return
        try {
            fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        mapView?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                        mapView?.controller?.setZoom(16.0)
                    }
                }
        } catch (e: SecurityException) {
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            centerOnLastLocation()
        }
    }
    LaunchedEffect(Unit) {
        permLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (placeId == null) stringResource(R.string.add_place)
                        else stringResource(R.string.edit_place)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_label))
                    }
                },
                actions = {
                    IconButton(onClick = { goToCurrentLocation() }) {
                        Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.current_location))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                label = { Text(stringResource(R.string.place_name_label)) },
                isError = nameError
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                PlaceType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        shape = SegmentedButtonDefaults.itemShape(index, PlaceType.entries.size),
                        label = { Text(type.displayName(context), maxLines = 1) }
                    )
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; searchError = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.address_example_placeholder)) },
                isError = searchError,
                supportingText = if (searchError) {
                    { Text(stringResource(R.string.address_not_found)) }
                } else null,
                trailingIcon = {
                    IconButton(onClick = { searchAddress() }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_address))
                    }
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        val map = MapView(ctx)
                        map.setTileSource(TileSourceFactory.MAPNIK)
                        map.setMultiTouchControls(true)
                        map.controller.setZoom(15.0)
                        map.controller.setCenter(GeoPoint(52.3676, 4.9041))
                        map.onResume()
                        mapView = map
                        map
                    }
                )
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    tint = VibrantCoral,
                    modifier = Modifier
                        .size(48.dp)
                        .offset(y = (-24).dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        nameError = true
                        return@Button
                    }
                    val center = mapView?.mapCenter ?: return@Button
                    scope.launch {
                        val address = GeocoderHelper.addressOf(
                            context, center.latitude, center.longitude
                        )
                        viewModel.save(placeId, trimmed, center.latitude, center.longitude, address, selectedType, onBack)
                    }
                },
                enabled = mapView != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(stringResource(R.string.save_location))
            }
        }
    }
}
