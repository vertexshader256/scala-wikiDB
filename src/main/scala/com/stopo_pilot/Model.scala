package com.stopo_pilot

case class Coordinate(lat: Double, lon: Double) {
    override def toString = s"$lat, $lon"
}

case class Runway(
    number: String,
    length: Int,
    surface: String)
    
case class Airport(
    name: String,
    image: String,
    location: String,
    iata: String,
    icao: String,
    faa: String,
    geocoords: Coordinate,
    runways: List[Runway])