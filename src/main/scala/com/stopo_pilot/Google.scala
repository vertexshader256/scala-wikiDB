package com.stopo_pilot

import scala.xml._
import spray.can.Http
import spray.http._
import HttpMethods._
import spray.can.Http.{ClientConnectionType, HostConnectorInfo}
import spray.http.HttpHeaders.`User-Agent`
import spray.can.client.{HostConnectorSettings, ClientConnectionSettings}
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.ask
import akka.io.IO
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent._
import spray.http._
import scala.concurrent._
import akka.actor._
import spray.http._
import spray.client.pipelining._
import java.util.regex.Pattern

object Google {
  
  implicit val system: ActorSystem = Pilot.system
  implicit val timeout: Timeout = Timeout(5.seconds)
  import system.dispatcher
  
  val defaultSettings = ClientConnectionSettings(system)
  
  val setup = Http.HostConnectorSetup(
    "maps.googleapis.com/maps/api",
    port = 443,
    sslEncryption = true,
    defaultHeaders = List(`User-Agent`(Seq(ProductVersion("scala-wikiDB", "1.0", "email = github/bdwashbu")))),
    settings = Some(new HostConnectorSettings(
        maxConnections = 10,
        maxRetries = 3,
        maxRedirects = 0,
        pipelining = false,
        idleTimeout = 5.seconds,
        connectionSettings = defaultSettings)),
        connectionType = ClientConnectionType.Proxied("maps.googleapis.com/maps/api", 443)
  )
  
  val Http.HostConnectorInfo(connector, _) = Await.result(IO(Http)(system).ask (setup), 1.second)
  
  def getStaticMap(lat: Double, lon: Double, sizeX: Int, sizeY: Int) = {
     (connector ? Get(
      Uri("/staticmap").
        withQuery(
          "center" -> "0,0",
          "zoom" -> "1",
          "size" -> s"${sizeX}x$sizeY",
          "maptype" -> "satellite",
          "markers" -> s"color:blue|label:R|$lat,$lon"
        )
    )).mapTo[HttpResponse]
  }
  
  def getElevationResult(lat: Double, lon: Double): Future[Int] = {
    (connector ? Get(
      Uri("/elevation/xml").
        withQuery(
          "locations" -> s"$lat, $lon"
        )
    )).mapTo[HttpResponse].map { response =>
    
       val xml = XML.loadString(response.entity.asString)
       val elevation = ((xml \\ "ElevationResponse" \ "result" \ "elevation").text.toFloat * 3.28084).toInt.toString.take(8)
       elevation.toInt
    }
  }
}