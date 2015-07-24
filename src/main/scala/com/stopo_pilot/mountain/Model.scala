package com.stopo_pilot
package mountain

import com.stopo_pilot.Coordinate

case class Location(
    name: String,
    flag: Array[Byte])

object Mountain {
  val volcanoTypes = Set(
      "Caldera",
      "Cinder Cone",
      "Complex volcano",
      "Cryovolcano",
      "Fissure vent",
      "Guyot",
      "Lava cone",
      "Lava dome",
      "Monogenetic volcanic field",
      "Mud volcano",
      "Pancake dome",
      "Polygenetic volcanic field",
      "Pyroclastic cone",
      "Pyroclastic shield",
      "Shield volcano",
      "Stratovolcano",
      "Subaqueous volcano",
      "Subglacial mound",
      "Subglacial volcano",
      "Submarine volcano",
      "Supervolcano",
      "Somma volcano",
      "Volcanic crater",
      "Volcanic field",
      "Volcanic plug"
  )
}
    
case class Mountain(
    name: String,
    location: Location,
    elevation: Int,
    prominence: Int,
    geocoords: Coordinate,
    range: Option[String],
    mountainType: Option[String],
    photo: Option[Array[Byte]],
    photoCaption: Option[String],
    age: Option[String],
    firstAscent: Option[String],
    lastEruption: Option[String],
    easiestRoute: Option[String])
    