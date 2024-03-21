package dam.a42346.coolweatherapp

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import java.io.InputStreamReader
import java.net.URL

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat

//TODO change to false
private val day: Boolean = true

private const val lisbon_latitude: Float = 38.076F
private const val lisbon_longitude: Float = -9.12F

private lateinit var fusedLocationClient: FusedLocationProviderClient

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        when (resources.configuration.orientation) {
            //Configuration.ORIENTATION_PORTRAIT -> if (day) setTheme(R.style.Theme_Day)
            //else setTheme(R.style.Theme_Night)
            Configuration.ORIENTATION_LANDSCAPE -> if (day) setTheme(R.style.Theme_Day_Land)
            else setTheme(R.style.Theme_Night_Land)

            else -> if (day) setTheme(R.style.Theme_Day)
            else setTheme(R.style.Theme_Night)
        }
        println("Current Theme: $theme")

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val latitudeV : EditText = findViewById(R.id.latitudeValue)
        latitudeV.hint = lisbon_latitude.toString()
        val longitudeV : EditText = findViewById(R.id.longitudeValue)
        longitudeV.hint = lisbon_longitude.toString()


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions if they are not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 0)
        }
        var lat = lisbon_latitude
        var lon = lisbon_longitude
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Use the location object as you wish
                println("GOT GPS!!!")
                println("Latitude: ${location?.latitude}, Longitude: ${location?.longitude}")
                lat = location?.latitude?.toFloat() ?: lisbon_latitude
                lon = location?.longitude?.toFloat() ?: lisbon_longitude
            }

        fetchWeatherData(lat, lon).start()

    }

    @SuppressLint("MissingPermission")
    /*
    fun getLastKnownLocation() {
        println("getLastKnownLocation()")
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                println("Location: $location")
                if (location != null) {
                    // Use your location object here
                    val gps_lat = location.latitude
                    val gps_lon = location.longitude
                    // Handle the latitude and longitude as needed
                    println("GPS Latitude: $gps_lat")
                    println("GPS Longitude: $gps_lon")

                    fetchWeatherData(gps_lat.toFloat(), gps_lon.toFloat()).start()
                }
            }
            .addOnCanceledListener {
                println("Location request cancelled")
            }
    }

     */

    private fun WeatherAPI_Call(lat: Float, long: Float): WeatherData {
        println("WeatherAPI_Call()")

        val reqString = buildString {
            append("https://api.open-meteo.com/v1/forecast?")
            append("latitude=${lat}&longitude=${long}&")
            append("current_weather=true&")
            append("hourly=temperature_2m,weathercode,pressure_msl,windspeed_10m")
        }
        println("reqString: $reqString")
        //val str = reqString.toString()
        val url = URL(reqString)
        println("url: $url")
        url.openStream().use {
            val json = Gson().fromJson(InputStreamReader(it, "UTF-8"), WeatherData::class.java)
            println("json: $json")
            return json
        }
    }

    private fun fetchWeatherData(lat: Float, long: Float): Thread {
        println("fetchWeatherData()")

        return Thread {
            val weather = WeatherAPI_Call(lat, long)
            println("WEATHER: $weather")
            updateUI(weather)
        }
    }

    private fun updateUI(request: WeatherData) {
        //println("REQUEST: $request")

        runOnUiThread {
            val weatherImage: ImageView = findViewById(R.id.weatherImage)
            val pressure: TextView = findViewById(R.id.pressureValue)
            println("pressure: " + pressure.text)
            pressure.text = buildString {
                //append(request.hourly.pressure_msl.toString())
                append(request.hourly.pressure_msl[12].toString())
                append(" hPa")
            }
            val windSpeed: TextView = findViewById(R.id.speedValue)
            println("windSpeed: " + windSpeed.text)
            windSpeed.text = buildString {
                append(request.current_weather.windspeed.toString())
                append(" km/h")
            }
            val windDirection: TextView = findViewById(R.id.dirValue)
            println("windDirection: " + windDirection.text)
            windDirection.text = buildString {
                append(request.current_weather.winddirection.toString())
                append("°")
            }
            val temperature: TextView = findViewById(R.id.tempValue)
            println("temperature: " + temperature.text)
            temperature.text = buildString {
                //append(request.hourly.temperature_2m.toString())
                append(request.current_weather.temperature.toString())
                append("°C")
            }
            val time = findViewById<TextView>(R.id.timeValue)
            println("time: " + time.text)
            time.text = request.current_weather.time

            val wImage = when (val wCode = getWeatherCodeMap()[request.current_weather.weathercode]) {
                WMO_WeatherCode.CLEAR_SKY,
                WMO_WeatherCode.MAINLY_CLEAR,
                WMO_WeatherCode.PARTLY_CLOUDY -> if (day) wCode.image + "day" else wCode.image + "night"

                else -> wCode?.image
            }
            println("wImage: $wImage")
            val res = getResources()
            println("res: $res")
            weatherImage.setImageResource(R.drawable.fog)
            val resID = res.getIdentifier(wImage, "drawable", packageName)
            print("resID: $resID")
            val drawable = getDrawable(resID)
            println("drawable: $drawable")
            weatherImage.setImageDrawable(drawable);
        }
    }

    fun updateWeather(view: View) {

        val latitudeV : EditText = findViewById(R.id.latitudeValue)
        println("Latitude: " + latitudeV.text)
        val longitudeV : EditText = findViewById(R.id.longitudeValue)
        println("Longitude: " + longitudeV.text)

        var lat : Float
        var lon : Float
        try {
            lat = latitudeV.text.toString().toFloat()
            if(lat < -90 || lat > 90) throw NumberFormatException()
            lon = longitudeV.text.toString().toFloat()
            if(lon < -180 || lon > 180) throw NumberFormatException()
        } catch (e: NumberFormatException) {
            System.err.println("Invalid input. Using default values.")

            lat = lisbon_latitude
            lon = lisbon_longitude

            latitudeV.setText(lisbon_latitude.toString())
            longitudeV.setText(lisbon_longitude.toString())
        }

        fetchWeatherData(lat, lon).start()
    }
}