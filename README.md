# Sample Project: NaverMap 맛보기

## Gradle Dependency

```groovy
// App 단위 Gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 이런 식으로 naver maven 설정
        maven { url 'https://naver.jfrog.io/artifactory/maven/' }
    }
}

// 모듈 단위 Gradle
dependencies {
    /* etc */

    // Naver Map Dependency
    implementation 'com.naver.maps:map-sdk:3.13.0'
    // 위치 추적 Dependency 
    implementation 'com.google.android.gms:play-services-location:19.0.0'
}
```

위와 같이 의존성 추가를 한다

## Implementation

### MapView와 NaverMap

[NaverMap 공식문서](https://navermaps.github.io/android-map-sdk/guide-ko/2-1.html)를 참조해보면 MapView에
NaverMap 객체를 끼워넣어야 Map 기능이 정상적으로 작동할 수 있다는 것을 알 수 있다.<br/>
이번 Practice에서는 ``MapFragment``를 사용하지 않았다 .(그런데 이번 프로젝트에서는 이걸 사용할 것 같진않다, 화면에 지도를 넣어야하는 것이어서)

### Usage

#### Activity에 MapView를 선언한다

```xml

<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools" android:layout_width="match_parent"
    android:layout_height="match_parent" tools:context=".MainActivity">

    <!-- TextView 생략 -->

    <com.naver.maps.map.MapView android:id="@+id/map_main" android:layout_width="0dp"
        android:layout_height="360dp" android:layout_marginHorizontal="16dp"
        android:layout_marginTop="24dp" app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@+id/txt_title" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

#### MapView를 Parent의 생명주기에 맞춘다

LifecycleCallback뿐만 아니라 ``onLowMemory``, ``onSavedInstanceState`` 등도 같이 맞추라고 하는데 이걸 한 클래스에서 다 적으면
가독성에 지대한 영향을 줄 것이 뻔하니 ``DefaultLifecycleObserver``를 활용하여 분리할 수 있는 코드는 분리한다.

```kotlin
// MainActivity.kt
override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    binding.mapMain.onSaveInstanceState(outState)
}

override fun onLowMemory() {
    super.onLowMemory()
    binding.mapMain.onLowMemory()
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

// onCreate에서 LifecycleObserver 등록
lifecycle.addObserver(MapViewLifecycleObserver(binding.mapMain, savedInstanceState))
```

#### MapView에 NaverMap 객체를 끼워넣는다

코드를 깊이 살펴보진 못했으나, ``OnMapReadyCallback.onMapReady()``에서 패러미터로 주어지는 NaverMap 객체를 MapView에 넣는 것으로 보인다.
Activity에 ``OnMapReadyCallback``를 구현하는 것이 역할에 안맞는 것 같아서 이 역시 ``NaverMapProvider``라는 클래스로 분리하여 주입한다

```kotlin
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

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    // 여기에 NaverMapProvider 주입
    binding.mapMain.getMapAsync(NaverMapProvider(locationSource, naverMap))
}
```

#### 위치추적 기능: LocationSource

우선 사용자의 위치를 받아오기 위해서는 위치정보 사용권한을 받아와야하는데, API 31부터 위치에 대한 정확한 정보를 안 넘겨줄 수도 있는 권한 항목이 신설, 강제화되어서 이때문에
꼴받고 싶지않다면 targetSdkVersion은 30으로 유지하는 것이 좋을 것 같다.

```kotlin
// 내부 코드 보면 생성자에서 권한 요청함
locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)

// 권한 요청의 결과 받아오기
override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
        // 권한 승인이 나오면 위치추적하고 아니면 일반지도모드
        naverMap?.locationTrackingMode =
            if (!locationSource.isActivated) LocationTrackingMode.None
            else LocationTrackingMode.Follow
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
}

// NaverMap에서 실시간으로 위치를 받아와야하기에 NaverMap 객체에 locationSource 설정
activityNaverMap = naverMap.apply {
    /* etc */
    locationSource = trackingLocationSource
    /* etc */
}
```

##### Contributor

[HyunWoo Lee](https://github.com/l2hyunwoo)
