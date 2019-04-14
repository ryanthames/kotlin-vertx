package verticles

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.templ.ThymeleafTemplateEngine
import nl.komponents.kovenant.functional.bind
import nl.komponents.kovenant.functional.map
import org.slf4j.LoggerFactory
import services.SunService
import services.WeatherService
import uy.klutter.vertx.VertxInit

class MainVerticle : AbstractVerticle() {
  override fun start(startFuture: Future<Void>?) {
    VertxInit.ensure()
    val server = vertx.createHttpServer()
    val router = Router.router(vertx)
    val logger = LoggerFactory.getLogger("VertxServer")

    val templateEngine = ThymeleafTemplateEngine.create()

    val staticHandler = StaticHandler.create().setWebRoot("public").setCachingEnabled(false)

    val weatherService = WeatherService()
    val sunService = SunService()

    val jsonMapper = jacksonObjectMapper()

    router.route("/public/*").handler(staticHandler)

    router.get("/").handler { routingContext ->
      val response = routingContext.response()
      response.end("Hello my dude!")
    }

    router.get("/home").handler { ctx ->
      templateEngine.render(ctx, "public/templates/", "index.html") { buf ->
        if (buf.failed()) {
          logger.error("Template rendering failed because ", buf.cause())
        } else {
          ctx.response().end(buf.result())
        }
      }
    }

    router.get("/api/data").handler { ctx ->
      val lat = 32.7252
      val lon = -97.3205
      val sunInfoP = sunService.getSunInfo(lat, lon)
      val temperatureP = weatherService.getTemperature(lat, lon)
      val sunWeatherInfoP = sunInfoP.bind { sunInfo ->
        temperatureP.map { temp -> SunWeatherInfo(sunInfo, temp) }
      }

      sunWeatherInfoP.success { info ->
        val json = jsonMapper.writeValueAsString(info)
        val response = ctx.response()
        response.end(json)
      }
    }

    server.requestHandler { router.accept(it) }.listen(8080) { handler ->
      if (!handler.succeeded()) {
        logger.error("Failed to listen to port 8080")
      }
    }
  }
}

data class SunInfo(val sunrise: String, val sunset: String)
data class SunWeatherInfo(val sunInfo: SunInfo, val temperature: Double)