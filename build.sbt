ThisBuild / scalaVersion      := "3.1.3"
ThisBuild / version           := "0.1.0-SNAPSHOT"
ThisBuild / organization      := "tech.caleb-dunn"
ThisBuild / organizationName  := "example"
Global / onChangedBuildSource := ReloadOnSourceChanges

// Versions
val ZIO  = "0.0.0+1-e54a59df-SNAPSHOT"
val STTP = "3.7.1"
lazy val zioMain = Seq(
  fork := true
)

lazy val root = (project in file("."))
  .settings(
    name := "footy_tips_parser",
    welcomeMessage
  )
  .aggregate(cliInterface, tapirApi, webScraper, smithy4s)

// import smithy4s.codegen.Smithy4sCodegenPlugin
lazy val smithy4s = project
  .in(file("smithy4s"))
  .enablePlugins(Smithy4sCodegenPlugin)
  // version for smithy4s-core is sourced from Smithy4sCodegenPlugin
  .settings(
    libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
  ) //"0.14.2")

lazy val webScraper = (project in file("web-scraper"))
  .settings(
    name := "web-scraper",
    zioMain,
    Compile / resourceDirectory := baseDirectory.value / "src/resources",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "MANIFEST.MF")                  => MergeStrategy.discard
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
      case x                                                    => MergeStrategy.first
    },
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"         % ZIO,
      "dev.zio" %% "zio-streams" % ZIO,
      "dev.zio" %% "zio-test"    % ZIO % Test,

//      "com.softwaremill.sttp.client3" %% "zio"      % STTP,

      // Use the %%% operator instead of %% for Scala.js and Scala Native
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.13.37",
      // Use the "provided" scope instead when the "compile-internal" scope is not supported
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.13.37" % "provided",
      "com.typesafe.scala-logging"            %% "scala-logging"         % "3.9.5",

      // Java
      "org.apache.poi"          % "poi"             % "5.2.2",
      "org.seleniumhq.selenium" % "selenium-java"   % "4.3.0",
      "ch.qos.logback"          % "logback-classic" % "1.2.11",
      "org.slf4j"               % "slf4j-api"       % "1.7.36"
    )
  )
  .dependsOn(common)

lazy val cliInterface = (project in file("cli-interface"))
  .settings(
    name := "cli-interface",
    // zioMain,
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "MANIFEST.MF")                  => MergeStrategy.discard
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
      case x                                                    => MergeStrategy.first
    },
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"         % ZIO,
      "dev.zio" %% "zio-streams" % ZIO
    )
  )
  .dependsOn(common, webScraper)

val tapirVersion = "1.0.2"
lazy val tapirApi = (project in file("tapir-api"))
  .settings(
    // zioMain,
    name                := "tapir-api",
    reStart / mainClass := Some("tech.caleb.dunn.Main"),
    reLogTag            := "\u0008", // Sadge... Doesn't get rid of gap.

    Compile / resourceDirectory := baseDirectory.value / "src/resources",
    assembly / assemblyMergeStrategy := {
      //      case PathList("org", "openqa", "selenium", "remote", "http", "netty", xs @ _*) =>
      //        println(s"$xs")
      //        MergeStrategy.first
      case PathList("META-INF", "MANIFEST.MF")                  => MergeStrategy.discard
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
      case x                                                    => MergeStrategy.first
    },
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir"           %% "tapir-http4s-server-zio"  % tapirVersion,
      "com.softwaremill.sttp.tapir"           %% "tapir-http4s-server"      % tapirVersion,
      "org.http4s"                            %% "http4s-ember-server"      % "0.23.12",
      "org.http4s"                            %% "http4s-blaze-server"      % "0.23.12",
      "com.softwaremill.sttp.tapir"           %% "tapir-prometheus-metrics" % tapirVersion,
      "com.softwaremill.sttp.tapir"           %% "tapir-swagger-ui-bundle"  % tapirVersion,
      "com.softwaremill.sttp.tapir"           %% "tapir-jsoniter-scala"     % tapirVersion,
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"      % "2.13.38",
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros"    % "2.13.38",
      "ch.qos.logback"                         % "logback-classic"          % "1.2.11",
      "com.softwaremill.sttp.tapir"           %% "tapir-sttp-stub-server"   % tapirVersion % Test,
      "dev.zio"                               %% "zio-test"                 % "2.0.0"      % Test,
      "dev.zio"                               %% "zio-test-sbt"             % "2.0.0"      % Test,
      "com.softwaremill.sttp.client3"         %% "jsoniter"                 % "3.7.2"      % Test

//      "dev.zio"                               %% "zio"                      % ZIO,
//      "com.softwaremill.sttp.tapir"           %% "tapir-zio-http-server"    % tapirVersion,
//      "com.softwaremill.sttp.shared"          %% "zio"                      % "1.3.6",
//      "com.softwaremill.sttp.tapir"           %% "tapir-http4s-server-zio"  % "1.0.2",
//      "com.softwaremill.sttp.tapir"           %% "tapir-prometheus-metrics" % tapirVersion,
//      "com.softwaremill.sttp.tapir"           %% "tapir-swagger-ui-bundle"  % tapirVersion,
//      "com.softwaremill.sttp.tapir"           %% "tapir-jsoniter-scala"     % tapirVersion,
//      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"      % "2.13.37",
//      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros"    % "2.13.37",
//      "com.typesafe.scala-logging"            %% "scala-logging"            % "3.9.5",
//      "ch.qos.logback"                         % "logback-classic"          % "1.2.11",
//      "com.softwaremill.sttp.tapir"           %% "tapir-sttp-stub-server"   % tapirVersion % Test,
//      "dev.zio"                               %% "zio-test"                 % "2.0.0"      % Test,
//      "dev.zio"                               %% "zio-test-sbt"             % "2.0.0"      % Test,
//      "com.softwaremill.sttp.client3"         %% "jsoniter"                 % "3.7.1"      % Test
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
  .dependsOn(common, webScraper)
lazy val common = project
  .settings(
//    libraryDependencies ++= Seq(
//      "org.apache.poi" % "poi" % "5.2.2"
//    )
  )
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"      % ZIO,
      "dev.zio" %% "zio-test" % ZIO % Test,
      // Use the %%% operator instead of %% for Scala.js and Scala Native
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.13.37",
      // Use the "provided" scope instead when the "compile-internal" scope is not supported
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.13.37" % "provided"
    )
  )

// lazy val webUI = (project in file("web-ui")).settings(name := "web-ui")
def welcomeMessage = onLoadMessage := {
  import scala.Console

  def header(text: String): String = s"${Console.RED}$text${Console.RESET}"

  def item(text: String): String = s"${Console.GREEN}> ${Console.CYAN}$text${Console.RESET}"

  def subItem(text: String): String = s"  ${Console.YELLOW}> ${Console.CYAN}$text${Console.RESET}"

  s"""
     |Welcome!
     |Footy Tips Parser is a small utility to pull results from ESPN's awful website and collate it into something
     |more usable.
     |${header("Projects")}:
     |${item("CLI Interface")} - A CLI interface for pulling footy tips.
     |${item("Tapir API")} - A REST interface for interacting with the webscraper.
     |${item("Web Scraper")} - The engine that scrapes ESPN's footy tipping website with selenium.
     |${item("Common")} - Common Lib. Holds mostly interfaces.
     |${item("Web UI")} - A solid JS browser interface ${header("NOT managed by SBT (YET)")}.
       """.stripMargin
}