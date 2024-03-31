package dam.a42346.coolweatherapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.URL

private const val maxAbsLat = 90
private const val maxAbsLon = 180

const val lisbon_latitude: Float = 38.076F
const val lisbon_longitude: Float = -9.12F
class MainViewModel : ViewModel() {
    val weatherData = MutableLiveData<WeatherData>()

    val latitudeInput = MutableLiveData<String>()
    val longitudeInput = MutableLiveData<String>()
    
    val lisbon_lat = lisbon_latitude.toString()
    val lisbon_lon = lisbon_longitude.toString()

    private suspend fun WeatherAPI_Call(lat: Float, long: Float): WeatherData? {
        println("MainViewModel.WeatherAPI_Call()")

        val reqString = buildString {
            append("https://api.open-meteo.com/v1/forecast?")
            append("latitude=${lat}&longitude=${long}&")
            append("current_weather=true&")
            append("hourly=temperature_2m,weathercode,pressure_msl,windspeed_10m")
        }
        println("reqString: $reqString")
        val url = URL(reqString)
        println("url: $url")
        try {
            withContext(Dispatchers.IO) {
                url.openStream()
            }.use {
                val reader = InputStreamReader(it, "UTF-8")
                val json = Gson().fromJson(reader, WeatherData::class.java)
                return json
            }
        } catch (e: Exception) {
            println("An error occurred: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    private fun fetchWeatherData(lat: Float, lon: Float) {
        println("MainViewModel.fetchWeatherData()")
        println("lat: $lat, lon: $lon")
        viewModelScope.launch{
            val weather = WeatherAPI_Call(lat, lon)
            println("WEATHER: $weather")
            if (weather != null) updateLiveData(weather)
        }
    }

    private fun updateLiveData(request: WeatherData) {
        println("MainViewModel.updateLiveData()")
        weatherData.value = request
    }

    fun updateWeather() {
        println("MainViewModel.updateWeather()")
        var lat : Float
        var lon : Float
        try {
            lat = latitudeInput.value?.toFloat() ?: lisbon_latitude
            if(lat < -maxAbsLat || lat > maxAbsLat) throw NumberFormatException()
            lon = longitudeInput.value?.toFloat() ?: lisbon_longitude
            if(lon < -maxAbsLon || lon > maxAbsLon) throw NumberFormatException()
        } catch (e: NumberFormatException) {
            System.err.println("Invalid input. Using default values.")
            lat = lisbon_latitude
            lon = lisbon_longitude
        }
        fetchWeatherData(lat, lon)
    }
}