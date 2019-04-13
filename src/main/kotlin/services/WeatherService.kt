package services

import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.double
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.obj
import com.google.gson.JsonParser
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import java.nio.charset.Charset

class WeatherService {
  fun getTemperature(lat: Double, lon: Double): Promise<Double, Exception> = task {
    val url = "http://api.openweathermap.org/data/2.5/weather?appId=58cc58b739454be847bb389852959eed&lat=$lat&lon=$lon&units=metric"
    val (_, response) = url.httpGet().responseString()
    val jsonStr = String(response.data, Charset.forName("UTF-8"))
    val json = JsonParser().parse(jsonStr).obj
    json["main"]["temp"].double
  }
}