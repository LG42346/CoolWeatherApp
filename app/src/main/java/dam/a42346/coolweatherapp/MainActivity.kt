package dam.a42346.coolweatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dam.a42346.coolweatherapp.databinding.ActivityMainBinding

//TODO toggle
private const val day: Boolean = false
private lateinit var fusedLocationClient: FusedLocationProviderClient

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private fun set_theme(){
        println("THEME: $theme")
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> setTheme(if (day) R.style.Theme_Day_Land else R.style.Theme_Night_Land)
            else->setTheme(if (day) R.style.Theme_Day else R.style.Theme_Night)
        }
        println("Current Theme: $theme")
    }
    private fun set_gps(){
        println("set_gps()")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 0)
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Use the location object as you wish
                println("GPS # Latitude: ${location?.latitude}, Longitude: ${location?.longitude}")
                binding?.mainViewModel?.apply {
                    latitudeInput?.value = (location?.latitude ?: lisbon_latitude).toString()
                    longitudeInput?.value = (location?.longitude ?: lisbon_longitude).toString()
                    println("MAIN VIEW MODEL # latitude: ${latitudeInput.value}, longitude: ${longitudeInput.value}")
                    binding.mainViewModel?.updateWeather()
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.mainViewModel = MainViewModel().apply {
            weatherData.observe(this@MainActivity) { weatherData ->
                updateUI(weatherData)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        set_theme()
        set_gps()
    }

    private fun updateUI(request: WeatherData) {
        runOnUiThread {
            val weatherImage: ImageView = findViewById(R.id.weatherImage)
            val pressure: TextView = findViewById(R.id.pressureValue)
            println("pressure: " + pressure.text)
            pressure.text = buildString {
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
}