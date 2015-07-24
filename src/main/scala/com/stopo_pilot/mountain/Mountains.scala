package com.stopo_pilot
package mountain

import scala.collection.mutable.ListBuffer
import Wiki._
import scala.xml._
import scalafx.scene.image.Image
import scala.collection.mutable.LinkedHashMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, ExecutionContext$, Future, Promise, Await }
import scala.concurrent.duration._
import scala.util.Try

object Mountains {

  def getMountainFromInfoBox(title: String): Future[Option[Mountain]] = {
    val paramFuture = getInfoboxParameters("mountain", title)
    
    paramFuture map { params =>
      if (!params.isEmpty) {
        
        var lat = 0.0
        var lon = 0.0
        
        val photoCaption = params.get("photo_caption")
        
        val elev = params.getOrElse("elevation_ft", 
            (params.getOrElse("elevation_m", "0.0").replace(",", "").toDouble * 3.28084).toString.replace(",", "")).replace(",", "").toDouble.toInt
              
        val prominence = params.getOrElse("prominence_ft", 
            (params.getOrElse("prominence_m", "0.0").replace(",", "").toDouble * 3.28084).toString.replace(",", "")).toDouble.toInt
        
        if (params.contains("coordinates")) { // old geohack format
          val parts = params("coordinates").split("\\|")

          if (parts.size >= 3) {
            if (parts(3) == "N" || parts(3) == "S") {
              val isNorth = if (parts(3) == "N") 1.0 else -1.0
              val isEast = if (parts(6) == "E") 1.0 else -1.0
              lat = parts(1).toDouble + parts(2).toDouble / 60.0 * isNorth
              lon = parts(4).toDouble + parts(5).toDouble / 60.0 * isEast
            } else {
              val isNorth = if (parts(4) == "N") 1.0 else -1.0
              val isEast = if (parts(8) == "E") 1.0 else -1.0
              lat = parts(1).toDouble + parts(2).toDouble / 60.0 + parts(3).toDouble / 3600.0 * isNorth
              lon = parts(5).toDouble + parts(6).toDouble / 60.0 + parts(7).toDouble / 3600.0 * isEast
            }
          }
          
        } else {
          lat = params.getOrElse("lat_d", "0.0").toDouble + params.getOrElse("lat_m", "0.0").toDouble / 60.0 + params.getOrElse("lat_s", "0.0").toDouble / 3600.0
          lon = params.getOrElse("long_d", "0.0").toDouble + params.getOrElse("long_m", "0.0").toDouble / 60.0 + params.getOrElse("long_s", "0.0").toDouble / 3600.0
          if (params.contains("lat_NS") && params("lat_NS") == "S") {
            lat *= -1.0
          }
          if (params.contains("long_EW") && params("long_EW") == "W") {
            lon *= -1.0
          }
        }
            
        val latFormatted = f"$lat%5.3f".toDouble
        val lonFormatted = f"$lon%5.3f".toDouble
        
        val imageFileName = params.get("photo")
        
        val imageData = imageFileName.map { fileName =>
          val addFilePrefix = if (fileName.contains("File:") || fileName.contains("Image:")) "" else "File:"
          val imageResponse = for {
            url <- Wiki.getThumbnailUrl(addFilePrefix + fileName.replace(" ", "_"), 227)
            imageXML = XML.loadString(url.entity.asString)
            thumburl = (imageXML \\ "ii" \@ "thumburl")
            image <- getWikiImage(thumburl)
          } yield image
          Await.result(imageResponse, 15.seconds)
        }

        var flagData = Array[Byte]()
        var country: String = ""
        
        try {
          val imageResponse = for {
            (theCountry, state) <- MapQuest.getCountry(lat, lon)
            url <- {
              country = theCountry.split(",").head
              if (country.contains("United")) {
                country = state
              }
              Wiki.getFlagURL(country)
            }
            imageXML = XML.loadString(url)
            thumburl = (imageXML \\ "ii" \@ "thumburl")
            image <- Pilot.getImage(thumburl) 
          } yield image
          
          flagData = Await.result(imageResponse, 5.seconds)
        } catch {
          case _: Throwable => "no image uri found"
        }
        
        if (prominence > 4921.26) {// cutoff for ultras
          Some(Mountain(
            name = params.getOrElse("name", "").split("&lt;").head,
            location = Location(country, flagData),
            range = params.get("range"),
            mountainType = params.get("type").map{_.replace("[", "").replace("]", "")},
            elevation = elev,
            prominence = prominence,
            geocoords = Coordinate(latFormatted, lonFormatted),
            photo = imageData,
            photoCaption = photoCaption,
            age = params.get("age"),
            firstAscent = params.get("first_ascent"),
            lastEruption = params.get("last_eruption"),
            easiestRoute = params.get("easiest_route"))
         )
        } else {
          None
        }
      } else {
        None
      }
    }
  }

  def getUltras(title: String): Future[Seq[Mountain]] = {
    for {
      query <- Wiki.getPageLinks(title)
      linkXML = XML.loadString(query.entity.asString)
      links = (linkXML \\ "pl").map(_ \@ "title")
      templateQuery <- Future.sequence(links.map(link => Wiki.checkForTemplate(link, "Template:Infobox mountain")))
      templateXML = templateQuery.map(x => XML.loadString(x.entity.asString))
      templates = templateXML.flatMap(x => (x \\ "query").map(x => (x \\ "page" \@ "title", x \\ "tl" \@ "title")).filter(x => x._2 != "")).toMap
      withMountainInfo = links.filter(link => templates.contains(link))
      result <- Future.sequence(withMountainInfo.map(title => getMountainFromInfoBox(title)))
    } yield result.flatten
  }
}