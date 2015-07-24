name := "scala-wikiDB"

version := "1.0"

organization := "com.wiki.db"

scalaVersion := "2.11.6"

resolvers += "spray repo" at "http://repo.spray.io"
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "io.spray" %% "spray-can" % "1.3.3"
libraryDependencies += "io.spray" %% "spray-client" % "1.3.3"
libraryDependencies += "io.spray" %% "spray-caching" % "1.3.3"

libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.40-R8"
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.9"

import com.github.retronym.SbtOneJar._

oneJarSettings