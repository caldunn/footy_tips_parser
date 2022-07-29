package tech.caleb.dunn

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import org.http4s.HttpRoutes
import sttp.capabilities.zio.ZioStreams
import sttp.model.sse.ServerSentEvent
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.*
import sttp.tapir.server.http4s.ztapir.{ZHttp4sServerInterpreter, serverSentEventsBody}
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.*
import sttp.tapir.{CodecFormat, PublicEndpoint, Schema, endpoint, query, stringBody}
import tech.caleb.dunn.Library.*
import zio.stream.{Stream, ZStream}
import zio.{Schedule, Task, ZIO, durationInt}

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference

object Endpoints {

  type pingEndpointHeaders = (Option[String], Option[Int])
  val pingEndpoint: PublicEndpoint[pingEndpointHeaders, Unit, String, Any] = endpoint
    .get
    .in("ping")
    .in(header[Option[String]]("my-header"))
    .in(header[Option[Int]]("my-age"))
    .out(header("Access-Control-Allow-Origin", "*"))
    .out(stringBody)

  val pingServerEndpoint: ZServerEndpoint[Any, Any] =
    pingEndpoint.serverLogicSuccess(v => ZIO.succeed(s"pongers $v"))

  type MyStream = Stream[Throwable, ServerSentEvent]
  val streamPoint: PublicEndpoint[Unit, Unit, MyStream, ZioStreams] = endpoint
    .get
    .in("cd")
    .out(header("Access-Control-Allow-Origin", "*"))
    .out(serverSentEventsBody)
  // .out(streamTextBody(ZioStreams)(CodecFormat.TextEventStream(), Some(StandardCharsets.UTF_8)))

  val countTo10: MyStream =
    ZStream
      .fromIterable(1 to 20)
      .map(i => ServerSentEvent(Some(s"round -> $i!")))
      .schedule(Schedule.spaced(500.millis))
      .tap(zio.Console.printLine(_))

  val streamServer: ZServerEndpoint[Any, ZioStreams] =
    streamPoint.zServerLogic(_ => ZIO.succeed(countTo10))

  val routes: HttpRoutes[Task] =
    ZHttp4sServerInterpreter().from {
      streamServer
    }
      // ZIO.succeed(ZStream(ServerSentEvent(Some("data"), None, None, None)))))
      .toRoutes

  implicit val codecBooks: JsonValueCodec[List[Book]] = JsonCodecMaker.make
  val booksListing: PublicEndpoint[Unit, Unit, List[Book], Any] = endpoint
    .get
    .in("books" / "list" / "all")
    .out(jsonBody[List[Book]])

  val booksListingServerEndpoint: ZServerEndpoint[Any, Any] =
    booksListing.serverLogicSuccess(_ => ZIO.succeed(books.get()))

  val prometheusMetrics: PrometheusMetrics[Task] = PrometheusMetrics.default[Task]()
  val metricsEndpoint: ZServerEndpoint[Any, Any] = prometheusMetrics.metricsEndpoint

  val docEndpoints: List[ZServerEndpoint[Any, Any]] =
    SwaggerInterpreter()
      .fromEndpoints[Task](List(metricsEndpoint.endpoint, booksListing), "footy_tips_scraper", "1.0.0")

  val all: List[ZServerEndpoint[Any, Any]] =
    List(
      pingServerEndpoint,
      booksListingServerEndpoint,
      metricsEndpoint
    ) ++ docEndpoints
}

object Library {
  case class Author(name: String)
  case class Book(title: String, year: Int, author: Author)

  val books = new AtomicReference(
    List(
      Book("The Sorrows of Young Werther", 1774, Author("Johann Wolfgang von Goethe")),
      Book("Nad Niemnem", 1888, Author("Eliza Orzeszkowa")),
      Book("The Art of Computer Programming", 1968, Author("Donald Knuth")),
      Book("Pharaoh", 1897, Author("Boleslaw Prus"))
    )
  )
}
