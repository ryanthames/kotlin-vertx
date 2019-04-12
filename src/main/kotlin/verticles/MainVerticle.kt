package verticles

import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonParser
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.templ.ThymeleafTemplateEngine
import nl.komponents.kovenant.task
import org.slf4j.LoggerFactory
import uy.klutter.vertx.VertxInit
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainVerticle : AbstractVerticle() {
  override fun start(startFuture: Future<Void>?) {
    VertxInit.ensure()
    val server = vertx.createHttpServer()
    val router = Router.router(vertx)
    val logger = LoggerFactory.getLogger("VertxServer")

    val templateEngine = ThymeleafTemplateEngine.create()

    router.get("/").handler { routingContext ->
      val response = routingContext.response()
      response.end("Hello my dude!")
    }

    router.get("/home").handler { ctx ->
      ctx.put("time", SimpleDateFormat().format(Date()))

      // get sunrise and sunset info
      val sunInfoP = task {
        val url = "http://api.sunrise-sunset.org/json?lat=32.7252&lng=-97.3205&formatted=0"
        val (_, response) = url.httpGet().responseString()
        val jsonStr = String(response.data, Charset.forName("UTF-8"))

        val json = JsonParser().parse(jsonStr).obj
        val sunrise = json["results"]["sunrise"].string
        val sunset = json["results"]["sunset"].string

        val sunriseTime = ZonedDateTime.parse(sunrise)
        val sunsetTime = ZonedDateTime.parse(sunset)
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.of("America/Chicago"))

        SunInfo(sunriseTime.format(formatter), sunsetTime.format(formatter))
      }

      sunInfoP.success { sunInfo ->
        ctx.put("sunrise", sunInfo.sunrise)
        ctx.put("sunset", sunInfo.sunset)

        templateEngine.render(ctx, "public/templates/", "index.html") { buf ->
          if(buf.failed()) {
            logger.error("Template rendering failed because ", buf.cause())
          } else {
            ctx.response().end(buf.result())
          }
        }
      }
    }

    server.requestHandler { router.accept(it) }.listen(8080) { handler ->
      if(!handler.succeeded()) {
        logger.error("Failed to listen to port 8080")
      }
    }
  }
}

data class SunInfo(val sunrise: String, val sunset: String)