import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.templ.ThymeleafTemplateEngine
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*

fun main(args: Array<String>) {
//  val vertx = Vertx.vertx()
//  val server = vertx.createHttpServer()
//  val router = Router.router(vertx)
//  val logger = LoggerFactory.getLogger("VertxServer")
//
//  val templateEngine = ThymeleafTemplateEngine.create()
//
//  router.get("/").handler { routingContext ->
//    val response = routingContext.response()
//    response.end("Hello World!")
//  }
//
//  router.get("/home").handler { ctx ->
//    ctx.put("time", SimpleDateFormat().format(Date()))
//    templateEngine.render(ctx, "public/templates/", "index.html") { buf ->
//      if(buf.failed()) {
//        logger.error("Template rendering failed because ", buf.cause())
//      } else {
//        ctx.response().end(buf.result())
//      }
//    }
//  }
//
//  server.requestHandler { router.accept(it) }.listen(8080) { handler ->
//    if(!handler.succeeded()) {
//      logger.error("Failed to listen to port 8080")
//    }
//  }
}