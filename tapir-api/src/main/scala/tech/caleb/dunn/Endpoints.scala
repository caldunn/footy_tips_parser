package tech.caleb.dunn

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import common.{Round, arrayRoundCodec}
import org.http4s.HttpRoutes
import sttp.capabilities.zio.ZioStreams
import sttp.model.sse.ServerSentEvent
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.*
import sttp.tapir.server.http4s.ztapir.{ZHttp4sServerInterpreter, serverSentEventsBody}
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.*
import sttp.tapir.{AnyEndpoint, CodecFormat, Endpoint, PublicEndpoint, Schema, endpoint, query, stringBody}
import tech.calebdunn.webscraper.*
import zio.stream.{Stream, ZStream}
import zio.{Schedule, Task, ZIO, durationInt}

object Endpoints {

  implicit val codecSignin: JsonValueCodec[ScrapeRequest] = JsonCodecMaker.make
  val signInEndpoint: PublicEndpoint[ScrapeRequest, Unit, MyStream, ZioStreams] = endpoint
    .post
    .in("signin")
    .in(jsonBody[ScrapeRequest])
    .out(serverSentEventsBody)

  val signinServer: ZServerEndpoint[Any, ZioStreams] =
    signInEndpoint.serverLogicSuccess { v =>
      ZIO.succeed(countTo10)
    }

  type pingEndpointHeaders = (Option[String], Option[Int])
  val pingEndpoint: PublicEndpoint[pingEndpointHeaders, Unit, String, Any] = endpoint
    .get
    .in("ping")
    .in(header[Option[String]]("my-header"))
    .in(header[Option[Int]]("my-age"))
    .out(stringBody)

  val pingServerEndpoint: ZServerEndpoint[Any, Any] =
    pingEndpoint.serverLogicSuccess { v =>
      ZIO.succeed("pong")
    }

  val pingPersonEndpoint: PublicEndpoint[pingEndpointHeaders, Unit, String, Any] = endpoint
    .get
    .in("pingCrash")
    .in(header[Option[String]]("my-header"))
    .in(header[Option[Int]]("my-age"))
    .out(stringBody)

  val pingPersonServerEndpoint: ZServerEndpoint[Any, Any] =
    pingPersonEndpoint.serverLogic { v =>
      v._1 match {
        case None    => ZIO.fail(new Exception("e"))
        case Some(_) => ZIO.succeed(Right("pong"))
      }
    }

  type MyStream = Stream[Throwable, ServerSentEvent]
  val streamPoint: PublicEndpoint[Unit, Unit, MyStream, ZioStreams] = endpoint
    .get
    .in("cd")
    .out(serverSentEventsBody)

  val countTo10: MyStream =
    ZStream
      .fromIterable(1 to 10)
      .map(i => ServerSentEvent(Some(s"round -> $i!"), id = Some(s"$i"), eventType = Some("round-update")))
      .schedule(Schedule.spaced(500.millis))
      .tap(zio.Console.printLine(_)) ++ more

  val more: MyStream =
    ZStream
      .fromIterable(11 to 20)
      .map(i => ServerSentEvent(Some(s"round -> $i!"), id = Some(s"$i"), eventType = Some("round-update")))
      .schedule(Schedule.spaced(500.millis))

  val streamServer: ZServerEndpoint[Any, ZioStreams] =
    streamPoint.zServerLogic(_ => ZIO.succeed(countTo10))

  val results: PublicEndpoint[Unit, Unit, Array[Round], Any] = endpoint
    .get
    .in("results")
    .out(jsonBody[Array[Round]])

  val resultsServerEndpoint: ZServerEndpoint[Any, Any] =
    results.serverLogicSuccess(_ => TempInterface.writtenArray.delay(1.seconds))

  val prometheusMetrics: PrometheusMetrics[Task] = PrometheusMetrics.default[Task]()
  val metricsEndpoint: ZServerEndpoint[Any, Any] = prometheusMetrics.metricsEndpoint

  val forDocs: List[AnyEndpoint] = List(
    metricsEndpoint.endpoint,
    streamServer.endpoint,
    signInEndpoint,
    pingServerEndpoint.endpoint
  )
  val docEndpoints: List[ZServerEndpoint[Any, Any]] =
    SwaggerInterpreter()
      .fromEndpoints[Task](forDocs, "ESPN Footy Tips Scraper", "1.0.0")

  val all: List[ZServerEndpoint[Any, ZioStreams]] =
    List(
      pingServerEndpoint,
      streamServer,
      signinServer,
      resultsServerEndpoint,
      metricsEndpoint
    ) ++ docEndpoints
}
