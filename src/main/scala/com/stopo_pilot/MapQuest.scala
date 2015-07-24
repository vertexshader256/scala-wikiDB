package com.stopo_pilot

import Network._
import scala.xml._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.LinkedHashMap

import scala.concurrent._
import akka.actor._
import spray.http._
import spray.client.pipelining._
import java.util.regex.Pattern

import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.ask
import akka.io.IO

import spray.can.Http
import HttpMethods._
import spray.can.Http.{ClientConnectionType, HostConnectorInfo}
import spray.http.HttpHeaders.`User-Agent`
import spray.can.client.{HostConnectorSettings, ClientConnectionSettings}

object MapQuest {
  implicit val system: ActorSystem = Pilot.system
  implicit val timeout: Timeout = Timeout(5.seconds)
  import system.dispatcher
   
  val defaultSettings = ClientConnectionSettings(system)
  
  
  
  //http://open.mapquestapi.com/nominatim/v1/reverse.php?format=xml&lat=51.521435&lon=1.162714
  
  val setup = Http.HostConnectorSetup(
    "open.mapquestapi.com",
    port = 80,
    sslEncryption = false,
    defaultHeaders = List(`User-Agent`(Seq(ProductVersion("scala-wikiDB", "1.0", "email = github/bdwashbu")))),
    settings = Some(new HostConnectorSettings(
        maxConnections = 8,
        maxRetries = 3,
        maxRedirects = 0,
        pipelining = false,
        idleTimeout = 30.seconds,
        connectionSettings = defaultSettings))
  )
  
  val Http.HostConnectorInfo(mapQuest, _) = Await.result(IO(Http)(system).ask (setup), 5.second)
  
   def getCountry(lat: Double, lon: Double): Future[(String, String)] = {
    val request = (mapQuest ? Get(
      Uri("/nominatim/v1/reverse.php").
        withQuery(
          "format" -> "xml",
          "lat" -> lat.toString,
          "lon" -> lon.toString,
          "accept-language" -> "en_EN"
        )
    )).mapTo[HttpResponse]
    
    request.map{ response =>
      val imageXML = XML.loadString(response.entity.asString)
      ((imageXML \\ "country").text, (imageXML \\ "state").text)
    }
  }
}