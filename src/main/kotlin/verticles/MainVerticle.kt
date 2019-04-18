package verticles

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariDataSource
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.*
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.ext.web.templ.ThymeleafTemplateEngine
import nl.komponents.kovenant.functional.bind
import nl.komponents.kovenant.functional.map
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import services.DatabaseAuthProvider
import services.MigrationService
import services.SunService
import services.WeatherService
import uy.klutter.vertx.VertxInit

class MainVerticle : AbstractVerticle() {
  private var maybeDataSource: HikariDataSource? = null
  private val logger = LoggerFactory.getLogger(this.javaClass.name)

  override fun start(startFuture: Future<Void>?) {
    logger.info("Starting the server")
    VertxInit.ensure()
    val server = vertx.createHttpServer()
    val router = Router.router(vertx)

    val templateEngine = ThymeleafTemplateEngine.create()

    val weatherService = WeatherService()
    val sunService = SunService()
    val jsonMapper = jacksonObjectMapper()

    val serverConfig = jsonMapper.readValue(config().getJsonObject("server").encode(), ServerConfig::class.java)
    val serverPort = serverConfig.port
    val enableCaching = serverConfig.caching

    val dataSourceConfig = jsonMapper.readValue(config().getJsonObject("datasource").encode(), DataSourceConfig::class.java)
    val dataSource = initDataSource(dataSourceConfig)

    val migrationService = MigrationService(dataSource)
    val migrationResult = migrationService.migrate()
    migrationResult.fold({ exc ->
      logger.error("Exception occurred while performing migration", exc)
      vertx.close()
    }, { _ ->
      logger.debug("Migration successful or not needed")
    })

    val staticHandler = StaticHandler.create().setWebRoot("public").setCachingEnabled(enableCaching)

    val authProvider = DatabaseAuthProvider(dataSource, jsonMapper)

    router.route().handler(CookieHandler.create())
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)))
    router.route().handler(UserSessionHandler.create(authProvider))

    router.route("/hidden/*").handler(RedirectAuthHandler.create(authProvider))
    router.route("/login").handler(BodyHandler.create())
    router.route("/login").handler(FormLoginHandler.create(authProvider))

    router.route("/public/*").handler(staticHandler)

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

    fun renderTemplate(ctx: RoutingContext, template: String) {
      templateEngine.render(ctx, "public/templates/", template) { buf ->
        val response = ctx.response()
        if (buf.failed()) {
          logger.error("Template rendering failed because ", buf.cause())
          response.setStatusCode(500).end()
        } else {
          response.end(buf.result())
        }
      }
    }

    router.get("/hidden/admin").handler { ctx ->
      logger.error("Hello there")
      renderTemplate(ctx.put("username",
          ctx.user().principal().getString("username")),
          "admin.html")
    }

    router.get("/home").handler { ctx ->
      renderTemplate(ctx, "index.html")
    }

    router.get("/loginpage").handler { ctx ->
      logger.error("LOGIN HANDLER")
      renderTemplate(ctx, "login.html")
    }

    router.get("/").handler { routingContext ->
      val response = routingContext.response()
      response.end("Hello my dude!")
    }

    server.requestHandler { router.accept(it) }.listen(serverPort) { handler ->
      if (!handler.succeeded()) {
        logger.error("Failed to listen to port $serverPort")
      }
    }
  }

  private fun initDataSource(config: DataSourceConfig): HikariDataSource {
    val hikariDS = HikariDataSource()
    hikariDS.username = config.user
    hikariDS.password = config.password
    hikariDS.jdbcUrl = config.jdbcUrl
    maybeDataSource = hikariDS
    return hikariDS
  }

  override fun stop(stopFuture: Future<Void>?) {
    maybeDataSource?.close()
  }
}

data class SunInfo(val sunrise: String, val sunset: String)
data class SunWeatherInfo(val sunInfo: SunInfo, val temperature: Double)

data class ServerConfig(val port: Int, val caching: Boolean)
data class DataSourceConfig(val user: String, val password: String, val jdbcUrl: String)