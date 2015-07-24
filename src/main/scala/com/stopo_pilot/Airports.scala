package com.stopo_pilot

import scala.xml._
import scala.collection.mutable.ListBuffer

object Airports {
  
//  def getAirportTables: List[String] = {
//
//    var firstLetter = 'A'
//    val airportNames = new ListBuffer[String]()
//    
//    for (i <- 0 to 0) {
//      firstLetter = ('A' + i).toChar
//      val title = s"List_of_airports_by_IATA_code:_$firstLetter"
//      println(s"getting airports starting with $firstLetter")
//      val airportTable = Wiki.extractTableInfo(title)//.tail // skip the first one
//      
//      airportNames ++= airportTable.flatMap{rows => rows.filter(_.size > 2).map{cells => cells(2)}}
//    }
//    
//    airportNames.take(10).foreach{aptName => println(Wiki.getInfoboxParameters("Airport", aptName))}
//    
//    println(airportNames.size)
//    airportNames.toList
//    
//    //val stateAirportInfo = airportTables.map(airport => getWikipediaList(airport).asString.body)
//  }
  
}