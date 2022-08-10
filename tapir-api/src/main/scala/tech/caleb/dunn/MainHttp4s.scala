package tech.caleb.dunn

import cats.syntax.all.*
import com.comcast.ip4s.{ipv4, port}
import com.typesafe.scalalogging.Logger
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.ember.server.*
import org.http4s.server.Router
import org.http4s.server.middleware.Logger as HLogger
import org.slf4j.LoggerFactory
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.http4s.Http4sServerOptions
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.server.interceptor.log.{DefaultServerLog, ServerLog}
import sttp.tapir.ztapir.*
import zio.interop.catz.*
import zio.{RIO, Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault, durationInt}

import scala.io.StdIn

object MainHttp4s extends ZIOAppDefault {

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val log: Logger = Logger(LoggerFactory.getLogger(ZHttp4sServerInterpreter.getClass.getName))

    def errorLog(msg: String, error: Option[Throwable]): Task[Unit] =
      ZIO.succeed {
        error match {
          case None    => log.debug(msg)
          case Some(_) => log.error(msg, error)
        }
      }

    val serverOptions: Http4sServerOptions[Task] =
      Http4sServerOptions
        .customiseInterceptors[Task]
        .metricsInterceptor(Endpoints.prometheusMetrics.metricsInterceptor())
        .serverLog(
          DefaultServerLog[Task](
            doLogWhenReceived = msg => ZIO.succeed(log.warn(msg)),
            doLogWhenHandled = errorLog,
            doLogAllDecodeFailures = errorLog,
            doLogExceptions = (msg: String, ex: Throwable) => ZIO.succeed(log.error(msg, ex)),
            noLog = ZIO.succeed(())
          )
        )
        .options
    val listOfEndpoints = List(Endpoints.pingServerEndpoint, Endpoints.streamServer, Endpoints.metricsEndpoint)
    val kek = ZHttp4sServerInterpreter(serverOptions)
      .from(
        listOfEndpoints
      )
      .toRoutes

    val endpoints = ZHttp4sServerInterpreter(serverOptions).from(Endpoints.all).toRoutes
    val port = 8080
    val main = ZIO.succeedBlocking {
      println(s"Go to http://localhost:$port/docs to open SwaggerUI.")
      // StdIn.readLine()

    }
    // HLogger.

    ZIO.executor.flatMap { executor =>
      BlazeServerBuilder[Task]
        .withExecutionContext(executor.asExecutionContext)
        .bindHttp(port, "localhost")
        .withHttpApp(Router("/" -> endpoints).orNotFound)
        // .resource
        .serve
        .compile
        .drain
//      EmberServerBuilder
//        .default[Task]
//        .withHttp2
//        // .withExecutionContext(executor.asExecutionContext)
//        .withHost(ipv4"0.0.0.0")
//        .withPort(port"8080")
//        .withHttpApp(Router("/" -> (routes)).orNotFound)
//        // .resource
    // .build

//        .use { _ =>
//          main
//            .delay(5.seconds)
//            .forever
//        }
//        .unit
    }

  }
}
