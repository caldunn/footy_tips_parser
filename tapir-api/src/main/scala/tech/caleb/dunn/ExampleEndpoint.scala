package tech.caleb.dunn

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.tapir.json.jsoniter.*
import sttp.tapir.generic.auto.*
import sttp.tapir.{PublicEndpoint, endpoint, stringToPath}
import sttp.tapir.ztapir.ZServerEndpoint

import zio.*

import java.util.concurrent.atomic.AtomicReference
object ExampleEndpoint {
  import Library.*
  implicit val codecBooks: JsonValueCodec[List[Book]] = JsonCodecMaker.make
  val booksListing: PublicEndpoint[Unit, Unit, List[Book], Any] = endpoint
    .get
    .in("books" / "list" / "all")
    .out(jsonBody[List[Book]])

  val booksListingServerEndpoint: ZServerEndpoint[Any, Any] =
    booksListing.serverLogicSuccess(_ => ZIO.succeed(books.get()))

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
}
