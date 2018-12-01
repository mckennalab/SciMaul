name := "SCIMaul"

version := "0.0.7.1"

scalaVersion := "2.12.5"

resolvers += Resolver.sonatypeRepo("public")

unmanagedBase := baseDirectory.value / "project"

libraryDependencies += "org.scalatest" % "scalatest_2.12" % "3.2.0-SNAP4" % "test"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.6.1"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.6.1"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

libraryDependencies += "com.typesafe.scala-logging" % "scala-logging_2.12" % "3.5.0"

libraryDependencies += "info.picocli" % "picocli" % "3.6.1"

libraryDependencies += "commons-io" % "commons-io" % "2.6"

// set the main class for packaging the main jar

mainClass in (Compile, packageBin) := Some("main.scala.Main")

// set the main class for the main 'run' task
mainClass in (Compile, run) := Some("main.scala.Main")

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")
