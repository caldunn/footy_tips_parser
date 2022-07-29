/*
package tech.caleb.dunn
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zhttp.http.HttpApp
import zhttp.service.server.ServerChannelFactory
import zhttp.service.{EventLoopGroup, Server}
import zio.{Console, Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault}

// import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter

object MainOld extends ZIOAppDefault {
  val log: Logger = Logger(LoggerFactory.getLogger(ZioHttpInterpreter.getClass.getName))

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    def errorLog(msg: String, error: Option[Throwable]): Task[Unit] =
      ZIO.succeed {
        error match {
          case None    => log.debug(msg)
          case Some(_) => log.debug(msg, error)
        }
      }

    val serverOptions: ZioHttpServerOptions[Any] =
      ZioHttpServerOptions
        .customiseInterceptors
        .serverLog(
          DefaultServerLog[Task](
            doLogWhenReceived = msg => ZIO.succeed(log.debug(s"$msg")),
            doLogWhenHandled = errorLog,
            doLogAllDecodeFailures = errorLog,
            doLogExceptions = (msg: String, ex: Throwable) => ZIO.succeed(log.debug(msg, ex)),
            noLog = ZIO.succeed(())
          )
        )
        .metricsInterceptor(Endpoints.prometheusMetrics.metricsInterceptor())
        .options
    val app: HttpApp[Any, Throwable] = Endpoints.streamServer
    // ZioHttpInterpreter(serverOptions).toHttp(Endpoints.streamServer) // ZioHttpInterpreter(serverOptions).toHttp(list)

    (for {
      serverStart <- Server.start(8080, app) // Server(app).withPort(8080).make
      _           <- Console.printLine("Go to http://localhost:8080/docs to open SwaggerUI. Press ENTER key to exit.")
      _           <- Console.readLine
    } yield serverStart)
      // .provideSomeLayer(EventLoopGroup.auto(0) ++ ServerChannelFactory.auto ++ Scope.default)
      .exitCode
  }
}
 */
