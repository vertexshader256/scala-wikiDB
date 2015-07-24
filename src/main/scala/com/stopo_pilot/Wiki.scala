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

object Wiki {
  
  implicit val system: ActorSystem = Pilot.system
  implicit val timeout: Timeout = Timeout(5.seconds)
  import system.dispatcher
   
  val defaultSettings = ClientConnectionSettings(system)
  
  val setup = Http.HostConnectorSetup(
    "en.wikipedia.org",
    port = 443,
    sslEncryption = true,
    defaultHeaders = List(`User-Agent`(Seq(ProductVersion("scala-wikiDB", "1.0", "email = github/bdwashbu")))),
    settings = Some(new HostConnectorSettings(
        maxConnections = 8,
        maxRetries = 3,
        maxRedirects = 0,
        pipelining = false,
        idleTimeout = 30.second,
        connectionSettings = defaultSettings))
  )
  
  val imageSetup = Http.HostConnectorSetup(
    "upload.wikimedia.org",
    port = 443,
    sslEncryption = true,
    defaultHeaders = List(`User-Agent`(Seq(ProductVersion("scala-wikiDB", "1.0", "email = github/bdwashbu")))),
    settings = Some(new HostConnectorSettings(
        maxConnections = 8,
        maxRetries = 3,
        maxRedirects = 0,
        pipelining = false,
        idleTimeout = 30.second,
        connectionSettings = defaultSettings))
  )
  
  val Http.HostConnectorInfo(imageConnector, _) = Await.result(IO(Http)(system).ask (imageSetup), 5.second)
  val Http.HostConnectorInfo(connector, _) = Await.result(IO(Http)(system).ask (setup), 5.second)

  def getWikiImage(url: String): Future[Array[Byte]] = {
    val hostRemoved = url.drop("https://upload.wikimedia.org".length)
    val future = (imageConnector ? Get(
      Uri(hostRemoved))).mapTo[HttpResponse]
    
    
    future.map { response => response.entity.data.toByteArray }.recover{ case thrown => println("----- " + hostRemoved); println("IMAGE ERROR: " + thrown.getMessage); Array[Byte]() }
  }
  
  def GetInfoBox(title: String): Future[HttpResponse] = {
    (connector ? Get(
      Uri("/w/api.php").
        withQuery(
          "format" -> "xml",
          "action" -> "query",
          "prop" -> "revisions",
          "rvprop" -> "content",
          "rvsection" -> "0",
          "titles" -> title,
          "redirects" -> ""
        )
    )).mapTo[HttpResponse]
    
//    val pipeline = sendReceive
//    pipeline(request)
  }
  
     // summary
     // exchars - control number of chars
     // https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=Mount%20Rainier
     
     // all info
     // https://en.wikipedia.org/w/api.php?format=json&action=query&prop=revisions&rvprop=content&format=xml&titles=Mount%20Rainier
     // https://en.wikipedia.org/w/api.php?format=xml&action=query&prop=revisions&rvprop=content&format=xml&titles=List_of_Ultras_of_the_United_States
     
     
     // search
     // https://en.wikipedia.org/w/api.php?action=opensearch&search=mt%20rainier&format=xml
  
   def parseRow(rowText: String): Seq[String] = {

     var i = 0
     var level = 1
     
     val markupGone = rowText.split("\n").map { x =>
       val noHtml = x.replaceAll("(?s)&lt;!--.*?--&gt;", "") // remove html comments
       val noRef = noHtml.replaceAll("(?s)&lt;ref.*?ref&gt;", "")
       noRef.replaceAll("(?s)&lt;ref.*?/&gt;", "")
     }.mkString
     var row = markupGone
     
     do {
         
       val nextTwo = List(row(i), row(i+1)).mkString
       
       if (nextTwo == "{{") {
         level += 1
         i += 1
       } else if (nextTwo == "}}") {
         level -= 1
         i += 1
       } else if (nextTwo == "[[") {
         level += 1
         i += 1
       } else if (nextTwo == "]]") {
         level -= 1
         i += 1
       } else if (row(i) == '|' && level == 1) {
         row = row.patch(i, "^", 1)//(i) = '^'//.substring(0, i) + "^" + cutBeginning.substring(i+1)
       }
       
       i += 1
     } while (level > 0 && i < markupGone.length-1)
       
      row.split('^').toList.tail.map(_.trim)
   }
  
   // returns a seq of tables, each having a seq of rows
   def getTables(wikiText: String, startText: String, terminator: String): Seq[Seq[Seq[String]]] = {
     val rawTables = wikiText.split(Pattern.quote(s"$startText\n")).toSeq.tail    
     
     val rowsPerTable = rawTables.map(_.split(Pattern.quote("|-")).toSeq)
     
     rowsPerTable.map(row => row.map(parseRow))
   }
   
   def getInfoboxParameters(typeOfInfoBox: String, title: String): Future[LinkedHashMap[String, String]] = {
     
     val req = GetInfoBox(title)
     
     req.map { x =>
        val tables = getTables(x.entity.asString, s"{{Infobox $typeOfInfoBox", "}}")
         if (!tables.isEmpty) {
           val infoBox = tables.head.head // assume only 1 infobox per page, and 1 "row" per infobox
           val mapping = infoBox.map{param => param.replace("[", "").replace("]", "").split("=", 2).map(_.trim).filter(!_.isEmpty)}
              .filter(_.size == 2).toList
              .map{apt => apt(0) -> apt(1)}
           
           val results = new LinkedHashMap[String, String]()
           
           println(mapping(0)._2)
           results ++= mapping
           results
         } else {
           new LinkedHashMap()
         }
      }
   }
 
  def checkForTemplate(title: String, template: String): Future[HttpResponse] = {
    (connector ? Get(
      Uri("/w/api.php").
        withQuery(
         "format" -> "xml",
         "action" -> "query",
         "titles" -> title,
         "prop" -> "templates",
         "tllimit" -> "max",
         "tltemplates" -> template
       )
     )).mapTo[HttpResponse]
  }
   
  def getPageLinks(title: String): Future[HttpResponse] = {
    (connector ? Get(
      Uri("/w/api.php").
        withQuery(
         "format" -> "xml",
         "action" -> "query",
         "titles" -> title,
         "prop" -> "links",
         "pllimit" -> "max"
       )
     )).mapTo[HttpResponse]
  }
   
  def getPageContents(title: String): Future[HttpResponse] = {
    (connector ? Get(
      Uri("/w/api.php").
        withQuery(
         "format" -> "xml",
         "action" -> "query",
         "titles" -> title,
         "prop" -> "revisions",
         "rvprop" -> "content"
       )
     )).mapTo[HttpResponse]
  }
 
  def getTemplateImages(templateName: String, param: String): Future[String] = {
    (connector ? Get(
      Uri("/w/api.php").
        withQuery(
          "format" -> "xml",
          "action" -> "parse",
          "prop" -> "images",
          "text" -> s"{{$templateName|$param}}"
        )
    )).mapTo[HttpResponse].map{ xml =>

       val imageXML = XML.loadString(xml.entity.asString)
       if ( (imageXML \\ "img").isEmpty) {
         if (param.indexOf(" ") != -1) {
           val newLocation = param.substring(param.indexOf(" ") + 1)
           val x = getTemplateImages(templateName, newLocation)
           Await.result(x, 5.seconds)
         } else {
           ""
         }
       } else {
         "File: " + (imageXML \\ "img").head.text
       }
    }
  }

  
  def getThumbnailUrl(title: String, size: Int): Future[HttpResponse] = {
    (connector ? Get(
      Uri("/w/api.php").
        withQuery(
          "format" -> "xml",
          "action" -> "query",
          "prop" -> "imageinfo",
          "iiprop" -> "url",
          "iiurlwidth" -> size.toString,
          "titles" -> title
        )
    )).mapTo[HttpResponse]
  }
  
  def getFlagURL(country: String): Future[String] =  {
    for {
      imgs <- Pilot.flagCache(country) {getTemplateImages("Flag", country)}
      url <- getThumbnailUrl(imgs, 23)
    } yield url.entity.asString
  }
  
}