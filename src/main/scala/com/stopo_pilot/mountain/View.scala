package com.stopo_pilot
package mountain

import scalafx.beans.property._
import scalafx.scene.control.{ TableCell, TableColumn, TableView }
import scalafx.scene.image.{ Image, ImageView }
import scalafx.scene.effect.DropShadow
import scala.concurrent.duration._
import java.io.ByteArrayInputStream
import scala.concurrent._
import scala.xml._
import akka.actor._
import scalafx.scene.paint.Color._
import scalafx.collections.ObservableBuffer
import scalafx.Includes._
import scalafx.scene.control._
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.cell.CheckBoxTableCell
import scalafx.beans.value.ObservableValue
import scalafx.scene.web._
import scalafx.scene.layout.VBox
import scalafx.scene.layout.StackPane
import scalafx.scene.text.TextAlignment

case class MountainRow(mountain: Mountain) {
  val name = new StringProperty(this, "name", mountain.name)
  val location = new ObjectProperty[Location](this, "location", mountain.location)
  val isVolcano = new BooleanProperty(this, "type", mountain.mountainType.map { mType => Mountain.volcanoTypes.map(_.toLowerCase).contains(mType.toLowerCase) }.getOrElse(false))
  val elevation = new ObjectProperty[Int](this, "elevation", mountain.elevation)
  val prominence = new ObjectProperty[Int](this, "prominence", mountain.prominence)
  val geocoords = new ObjectProperty(this, "geocoords", mountain.geocoords)
}

case class MountainDetailRow(prop: String, rawValue: String) {
  val property = new StringProperty(this, "property", prop)
  val value = new StringProperty(this, "value", rawValue)
}

object MountainView {

  implicit val system: ActorSystem = Pilot.system
  import system.dispatcher
  
  //val mountainTable = getMountainTable(mountains)
  val mountainDetailTable = mountainDetailsTable

  def getMountainTable(mountains: ObservableBuffer[Mountain]) = new TableView[MountainRow](mountains.map(MountainRow(_))) { self =>
    selectionModel().selectedItem.onChange(Pilot.changeListener)

    val volcanoColumn = new TableColumn[MountainRow, java.lang.Boolean]() { self =>
      text = "Volcano"
      cellValueFactory = _.value.isVolcano.delegate
      prefWidth = 70
    }

    volcanoColumn.setCellFactory(CheckBoxTableCell.forTableColumn(volcanoColumn))

    columns ++= List(

      new TableColumn[MountainRow, String]() {
        text = "Name"
        cellValueFactory = { _.value.name }
        prefWidth = 120
      },
      new TableColumn[MountainRow, Location]() { self =>
        text = "Location"
        cellValueFactory = { _.value.location }
        prefWidth = 120
        cellFactory = { _ =>
          new TableCell[MountainRow, Location] { s =>
            //val what = s.
            item.onChange { (_, _, loc) =>
              if (loc != null) {
                text = loc.name

                try {
                  graphic = new ImageView {
                    effect = new DropShadow(2, 0, 0, Black)
                    image = new Image(new ByteArrayInputStream(loc.flag))
                  }
                } catch {
                  case _ =>
                }
              }
            }
          }
        }
      },
      new TableColumn[MountainRow, Int]() {
        text = "Elevation (ft)"
        cellValueFactory = { _.value.elevation }
        prefWidth = 90
      },
      new TableColumn[MountainRow, Int]() {
        text = "Prominence (ft)"
        cellValueFactory = { x => x.value.prominence }
        prefWidth = 110
      },
      new TableColumn[MountainRow, Coordinate]() {
        text = "Location"
        cellValueFactory = { _.value.geocoords }
        prefWidth = 100
      },
      volcanoColumn)
  }

  lazy val scrollPane = new ScrollPane { self =>
    prefWidth = 260
    minHeight = 500
    translateX = 620.0
    translateY = 30.0
    vbarPolicy = ScrollBarPolicy.ALWAYS
    content = new VBox {
      spacing = 10
      children = Seq(MountainView.previewPhoto, new StackPane{children = photoCaption}, webView, mountainDetailTable)
    }
  }
  
  lazy val photoCaption = new Label {
    translateY = 15.0
    translateX = 10.0
    maxWidth = 227
    textAlignment = TextAlignment.Center
    wrapText = true
  }
  
  lazy val previewPhoto = new ImageView { self =>
    var counter = 0
    //image = new Image(new ByteArrayInputStream(Http(imgurImageUrls(counter)).asBytes.body))
    fitWidth = 227
    translateX = 10.0
    translateY = 10.0
    preserveRatio = true
    smooth = true
    effect = new DropShadow(3, 2, 2, Black)
    //    onMouseClicked = { e: MouseEvent =>    
    //      
    //      val newImgur = new Image(new ByteArrayInputStream(Http(imgurImageUrls(counter)).asBytes.body))
    //      image_=(newImgur)
    //      
    //      stage.show()
    //      
    //      val scaledWidth = this.boundsInParent.value.width
    //      val scaledHeight = this.boundsInParent.value.height
    //      
    //      val clip = new Rectangle(
    //          scaledWidth - 20,
    //          scaledHeight - 20
    //      );
    //      clip.setArcWidth(20);
    //      clip.setArcHeight(20);
    //   
    //      this.clip_=(clip)
    //
    //      // snapshot the rounded image.
    //      val parameters = new SnapshotParameters();
    //      parameters.setFill(Color.TRANSPARENT);
    //      val image = snapshot(parameters, null);
    //
    //      val shadowClip = new Rectangle(
    //          scaledWidth + 20, scaledHeight + 20
    //      );
    //      // remove the rounding clip so that our effect can show through.
    //      this.clip_=(shadowClip)
    //
    //      // apply a shadow effect.
    //      this.effect_=(new DropShadow(8, 5, 5, Color.BLACK));
    //      image_=(image)
    //      
    //      counter += 1
    //    }
  }
  
  val webEngine: WebEngine = webView.getEngine
  webEngine.setJavaScriptEnabled(true)
  webEngine.load("file:/C:/Scala/Git/stopo_pilot/GoogleMapsV3.html")
  
  def centerMap(mountain: Mountain) = {
    webEngine.executeScript(s"""
      document.map.setCenter(new google.maps.LatLng(${mountain.geocoords}));
      document.marker.setMap(null)
      document.marker = new google.maps.Marker({
          position: new google.maps.LatLng(${mountain.geocoords}),
          map: document.map,
          title: 'Hello World!'
      });
      document.map.setZoom(document.map.getZoom());
      google.maps.event.trigger(document.map, 'resize'); // Can't remember if really helps
      document.map.setZoom( document.map.getZoom() -1 );    
      document.map.setZoom( document.map.getZoom() +1 );
    """)
  }
  
  lazy val webView = new WebView { self =>
//    maxWidth = 400
//    maxHeight = 200
//    translateX = 200.0
//    translateY = 10.0
    maxWidth = 227
    maxHeight = 227
    translateX = 10.0
    translateY = 10.0
    effect = new DropShadow(3, 2, 2, Black)
    
//    onMouseClicked = { e: MouseEvent =>
//      println(webEngine.executeScript("""function getCoord() {
//                                        return document.map.getCenter();
//                                      }
//                                      getCoord();"""))
//    }
  }

 
  
  def setDetails(mountain: Mountain) = {
    mountainDetailsTable.items = ObservableBuffer(
      mountain.mountainType.map { mType => MountainDetailRow("Type", mType) },
      mountain.age.map { age => MountainDetailRow("Age", age) },
      mountain.firstAscent.map { year => MountainDetailRow("First Ascent", year) },
      mountain.lastEruption.map { year => MountainDetailRow("Last Eruption", year) },
      mountain.easiestRoute.map { year => MountainDetailRow("Easiest Route", year) }
    ).flatten
  }
  
  lazy val mountainDetailsTable = new TableView[MountainDetailRow] { self =>

    columns ++= List(

      new TableColumn[MountainDetailRow, String]() {
        text = "Field"
        cellValueFactory = { _.value.property }
        prefWidth = 120
      },
      new TableColumn[MountainDetailRow, String]() {
        text = "Info"
        cellValueFactory = { _.value.value }
        prefWidth = 120
      })
  }
}