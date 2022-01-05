package com.teamfillin.mapsample

import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
* Created by Nunu
* at 2022.01.04
*
* MapView에 NaverMap 객체를 공급하기 위한 Provider Class
* */
class NaverMapProvider(
    private val trackingLocationSource: LocationSource
) : OnMapReadyCallback {
    private val _naverMap = MutableStateFlow<State>(State.NotReceived)
    val naverMap = _naverMap.asStateFlow()

    override fun onMapReady(naverMap: NaverMap) {
        _naverMap.value = naverMap.apply {
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
        }.toValue()
    }

    private fun NaverMap.toValue() = State.Map(this)

    sealed class State {
        object NotReceived : State()
        data class Map(val value: NaverMap) : State()
    }
}