package com.example.bottomnavigationbarcomposeexample

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.*
import android.Manifest
import android.annotation.SuppressLint
import com.google.android.gms.location.LocationRequest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import kotlinx.coroutines.delay
import java.io.File
import java.util.UUID

private const val PERMISSION_REQUEST_CODE = 1
lateinit var api: PolarBleApi
lateinit var context: Context

class MainActivity : ComponentActivity() {

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //START THE MAIN SCREEN
        setContent {
            MainScreen()
        }

        //REQUEST PERMISSIONS
        requestPermissions(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ), PERMISSION_REQUEST_CODE
        )

        //START THE POLAR API
        context = applicationContext
        api = getApi(applicationContext)
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                //SEE IF WE NEED THIS
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("", "adding polar device ${polarDeviceInfo.deviceId}  to home screen")
                if (firstConnectedDeviceFlag) {
                    deviceListForHomeScreen[0] = ConnectedDevices(polarDeviceInfo.name)
                    polarDeviceIdListForConnection[0] = polarDeviceInfo.deviceId
                    firstConnectedDeviceFlag = false
                } else {
                    deviceListForHomeScreen.add(ConnectedDevices(polarDeviceInfo.name))
                    polarDeviceIdListForConnection.add(polarDeviceInfo.deviceId)
                }
                //YOU CAN REPLACE THIS WITH A .INDEXOF, just search for .indexof to see how its doen
                //actually if you wanna do that then deviceId has to be the main
                for (index in deviceListForDeviceScreen.indices){
                    if (deviceListForDeviceScreen[index].deviceId == polarDeviceInfo.deviceId){
                        deviceListForDeviceScreen[index].connected = "Connected"
                        deviceListForDeviceScreen.add(AvailableDevices("none")) //This'll actually work. so adding something works but not changing. supposed to be somewhat fixable but idk. seems chill to me.
                        deviceListForDeviceScreen.remove(AvailableDevices("none")) //https://stackoverflow.com/questions/69718059/android-jetpack-compose-mutablestatelistof-not-doing-recomposition/69718724#69718724
                    }
                }
            }

            override fun bleSdkFeatureReady(identifier: String, feature: PolarBleApi.PolarBleSdkFeature) {

            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                //SEE IF WE NEED THIS
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                //SEE IF WE NEED THIS
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                //SEE IF WE NEED THIS
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                //SEE IF WE NEED THIS
            }

            override fun hrNotificationReceived(identifier: String, data: PolarHrData.PolarHrSample) {
            // //SEE IF WE NEED THIS
            }

        })

        //START THE LOCATION TRACKING
        LocationManager(context, 10L, 1.0F).startLocationTracking()
    }

    //class DeviceViewModel : ViewModel()

    @SuppressLint("MissingPermission")
    class LocationManager(
        context: Context,
        private var timeInterval: Long,
        private var minimalDistance: Float
    ) : LocationCallback() {

        private var request: LocationRequest
        private var locationClient: FusedLocationProviderClient

        init {
            // getting the location client
            locationClient = LocationServices.getFusedLocationProviderClient(context)
            request = createRequest()
        }

        private fun createRequest(): LocationRequest =
            // New builder
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, timeInterval).apply {
                setMinUpdateDistanceMeters(minimalDistance)
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setWaitForAccurateLocation(true)
            }.build()

        fun startLocationTracking() =
            locationClient.requestLocationUpdates(request, this, Looper.getMainLooper())

        fun stopLocationTracking() {
            locationClient.flushLocations()
            locationClient.removeLocationUpdates(this)
        }

        override fun onLocationResult(location: LocationResult) {
            var timeStamp = 0L
            if (firstTimeStamps.size >= 1){
                timeStamp = System.currentTimeMillis() - firstPhoneTimeStamp
            }else{
                timeStamp = 0L
            }
            val fileString = "$timeStamp; ${location.lastLocation!!.latitude}; ${location.lastLocation!!.longitude}; ${location.lastLocation!!.altitude} \n"
            generateAndAppend("LocationData.txt",fileString,"TimeStamp; Latitude; Longitude; Altitude; \n")
        }

        override fun onLocationAvailability(availability: LocationAvailability) {
            Log.d("","Location Availible")
        }

    }

}


fun getApi(context: Context): PolarBleApi {
    val api: PolarBleApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
            context, setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_LED_ANIMATION,
            )
        )
    }
    return api
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                Navigation(navController = navController)
            }
        },
        backgroundColor = colorResource(R.color.colorPrimaryDark) // Set background color to avoid the white flashing when you switch between screens
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen()
}

@Composable
fun Navigation(navController: NavHostController) {
    NavHost(navController, startDestination = NavigationItem.Home.route) {
        composable(NavigationItem.Home.route) {
            HomeScreen()
        }
        composable(NavigationItem.Devices.route) {
            DevicesScreen()
        }
    }
}


@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.Devices,
    )
    BottomNavigation(
        backgroundColor = colorResource(id = R.color.colorPrimary),
        contentColor = Color.White
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            BottomNavigationItem(
                icon = { Icon(painterResource(id = item.icon), contentDescription = item.title) },
                label = { Text(text = item.title) },
                selectedContentColor = Color.White,
                unselectedContentColor = Color.White.copy(0.4f),
                alwaysShowLabel = true,
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun BottomNavigationBarPreview() {
    // BottomNavigationBar()
}