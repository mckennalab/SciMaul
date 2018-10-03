name := "SCIMaul"

version := "1.1"

scalaVersion := "2.12.5"

resolvers += Resolver.sonatypeRepo("public")

unmanagedBase := baseDirectory.value / "project"


// https://mvnrepository.com/artifact/com.github.scopt/scopt
libraryDependencies += "com.github.scopt" %% "scopt" % "3.7.0"

libraryDependencies += "org.scalatest" % "scalatest_2.12" % "3.2.0-SNAP4" % "test"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.6.1"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.6.1"

// set the main class for packaging the main jar
// 'run' will still auto-detect and prompt
// change Compile to Test to set it for the test jar
mainClass in (Compile, packageBin) := Some("main.scala.Main")

// set the main class for the main 'run' task
// change Compile to Test to set it for 'test:run'
mainClass in (Compile, run) := Some("main.scala.Main")
