import foo.Person

object Main {
  @main
  def main() =
    val person = Person("Caleb", "Dunn")
    println(s"Hello, $person !")
}
