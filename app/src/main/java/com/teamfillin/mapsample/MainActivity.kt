package com.teamfillin.mapsample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.util.FusedLocationSource
import com.teamfillin.mapsample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationSource: FusedLocationSource
    private var naverMap: NaverMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
        lifecycle.addObserver(MapViewLifecycleObserver(binding.mapMain, savedInstanceState))
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
        binding.mapMain.getMapAsync(NaverMapProvider(locationSource, naverMap))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            naverMap?.locationTrackingMode =
                if (!locationSource.isActivated) LocationTrackingMode.None
                else LocationTrackingMode.Follow
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapMain.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapMain.onLowMemory()
    }

    private class NaverMapProvider(
        private val trackingLocationSource: LocationSource,
        private var activityNaverMap: NaverMap? = null
    ) : OnMapReadyCallback {
        override fun onMapReady(naverMap: NaverMap) {
            activityNaverMap = naverMap.apply {
                mapType = NaverMap.MapType.Navi
                setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRANSIT, false)
                setLayerGroupEnabled(NaverMap.LAYER_GROUP_BUILDING, true)
                isNightModeEnabled = true
                locationSource = trackingLocationSource
                locationTrackingMode = LocationTrackingMode.Follow
                addOnLocationChangeListener { location ->
                    cameraPosition =
                        CameraPosition(LatLng(location.latitude, location.longitude), 16.0)
                }
                uiSettings.run {
                    isCompassEnabled = false
                    isScaleBarEnabled = false
                    isZoomControlEnabled = false
                    isLocationButtonEnabled = true
                }
            }
        }
    }

    private class MapViewLifecycleObserver(
        private val mapView: MapView,
        private val savedInstanceState: Bundle?
    ) : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            super.onCreate(owner)
            mapView.onCreate(savedInstanceState)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            mapView.onDestroy()
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            mapView.onPause()
        }

        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            mapView.onResume()
        }

        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            mapView.onStart()
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            mapView.onStop()
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }
}

