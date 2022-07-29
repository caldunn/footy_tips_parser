package tech.caleb.dunn

import cats.syntax.all.*
import com.comcast.ip4s.{ipv4, port}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.ember.server.*
import org.http4s.server.Router
import sttp.tapir.server.http4s.Http4sServerOptions
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import zio.interop.catz.*
import zio.{Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault, durationInt}

import scala.io.StdIn

object Main extends ZIOAppDefault {

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {

    val serverOptions: Http4sServerOptions[Task] =
      Http4sServerOptions
        .customiseInterceptors[Task]
        .metricsInterceptor(Endpoints.prometheusMetrics.metricsInterceptor())
        .options
    val routes  = ZHttp4sServerInterpreter(serverOptions).from(Endpoints.all /*Endpoints.all*/ ).toRoutes
    val routes2 = Endpoints.routes
    val port    = 8080
    ZIO.executor.flatMap { executor =>
      BlazeServerBuilder[Task]
        .withExecutionContext(executor.asExecutionContext)
        .bindHttp(port, "localhost")
        .withHttpApp(Router("/streams/" -> (routes2)).orNotFound)
        .withHttpApp(Router("/" -> (routes)).orNotFound)
        .resource

//      EmberServerBuilder
//        .default[Task]
//        .withHttp2
//        // .withExecutionContext(executor.asExecutionContext)
//        .withHost(ipv4"0.0.0.0")
//        .withPort(port"8080")
//        .withHttpApp(Router("/" -> (routes)).orNotFound)
//        // .resource
        // .build
        .use { _ =>
          ZIO.succeedBlocking {
            println(s"Go to http://localhost:$port/docs to open SwaggerUI. Press ENTER key to exit.")
            // StdIn.readLine()
          }
            .delay(5.minutes)
            .forever
        }
        .unit
    }

  }
}
