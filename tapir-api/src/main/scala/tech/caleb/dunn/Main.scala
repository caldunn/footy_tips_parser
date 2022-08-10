package tech.caleb.dunn
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import sttp.tapir.server.interceptor.cors.{CORSConfig, CORSInterceptor}
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zhttp.http.*
import zhttp.service.server.ServerChannelFactory
import zhttp.service.{EventLoopGroup, Server}
import zio.{Console, Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault}

import scala.util.Try

object Main extends ZIOAppDefault {
  val log: Logger = Logger(LoggerFactory.getLogger(ZioHttpInterpreter.getClass.getName))

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    def errorLog(msg: String, error: Option[Throwable]): Task[Unit] =
      ZIO.succeed {
        error match {
          case None    => log.debug(msg)
          case Some(_) => log.error(msg, error)
        }
      }

    def decodeLog(msg: String, error: Option[Throwable]): Task[Unit] =
      ZIO.succeed {
        error match {
          case None    => log.error(msg)
          case Some(_) => log.error(msg, error)
        }
      }

    val metrics    = Endpoints.prometheusMetrics.metricsInterceptor()
    val corsConfig = CORSConfig.default

    val cors = CORSInterceptor.customOrThrow[Task](corsConfig)
    val serverOptions: ZioHttpServerOptions[Any] =
      ZioHttpServerOptions
        .customiseInterceptors
        .serverLog(
          DefaultServerLog[Task](
            doLogWhenReceived = msg => ZIO.succeed(log.debug(s"$msg")),
            doLogWhenHandled = errorLog,
            doLogAllDecodeFailures = decodeLog,
            doLogExceptions = (msg: String, ex: Throwable) => ZIO.succeed(log.error(msg, ex)),
            noLog = ZIO.succeed(())
          )
        )
        .corsInterceptor(cors)
        .metricsInterceptor(metrics)
        .options
    
    val app = ZioHttpInterpreter(serverOptions)
      .toHttp(Endpoints.all)

    val server = Server(app).withPort(8080).make
    (for {
      serverStart <- server // Server.start(8080, app)
      _           <- Console.printLine("Go to http://localhost:8080/docs to open SwaggerUI.")
      _           <- ZIO.never
    } yield serverStart)
      .provideSomeLayer(EventLoopGroup.auto(0) ++ ServerChannelFactory.auto ++ Scope.default)
      .exitCode
  }
}
