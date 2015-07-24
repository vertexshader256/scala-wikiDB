package com.stopo_pilot

import scalafx.scene.image.{ Image, ImageView }
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.effect.DropShadow
import scalafx.scene.paint.Color._
import scalafx.scene.paint.{ Stops, LinearGradient }
import scalafx.scene.text.Text
//import scala.xml._
import scalafx.Includes._
import scalafx.scene.Cursor
import scalafx.scene.control.MenuItem._
import scalafx.scene.input.{ MouseEvent, KeyEvent }
import scalafx.event.{ ActionEvent, Event }
import scalafx.scene.input.MouseEvent._
import scalafx.scene.layout.{ Priority, ColumnConstraints, Pane }
import scalafx.scene.control._
import javafx.scene.shape.Rectangle
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import com.sun.javafx.robot.FXRobot
import scalafx.geometry.Point2D
import javafx.application.Platform
import scalafx.scene.paint.Color
import scalafx.collections.ObservableBuffer
import scalafx.beans.property._
import scalafx.scene.control.TableColumn._
import scala.collection.mutable.HashMap
import java.io._
import scala.collection.mutable.ListBuffer
import Network._
import scalafx.scene.control.cell.CheckBoxTableCell
import scalafx.beans.value.ObservableValue
import scala.concurrent._
import scala.concurrent.duration._
import spray.caching.{ LruCache, Cache }
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.LinkedHashMap
import scalafx.scene.web._
import scala.concurrent._
import akka.actor._
import spray.http._
import HttpMethods._
import spray.client.pipelining._
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.regex.Pattern
import scalafx.scene.layout.{ BorderPane, VBox }
import scalafx.geometry.Pos
import scalafx.stage.Screen
import com.stopo_pilot.mountain._


object FlightData {
  var thrust = 0.0;
  var latitude = 36.108
  var longitude = -112.46
  var heading = 0.0
  var altitude = 12000.0
  var pitch = 0.0
  var destination: String = ""
  var counter: Int = 0
}

object Pilot extends JFXApp {

  val degToRadian = 0.0174532925;

  implicit val system: ActorSystem = ActorSystem()
  val cache: Cache[Array[Byte]] = LruCache()
  val flagCache: Cache[String] = LruCache()

  val udpThread = new Thread(new UDP {})
  udpThread.start

  val changeListener: (ObservableValue[MountainRow, MountainRow], MountainRow, MountainRow) => Unit = {
    (_, _, mountainView) => {
      MountainView.centerMap(mountainView.mountain)
      MountainView.setDetails(mountainView.mountain)
      println(mountainView.mountain.photo.getOrElse(Array[Byte](0)).size)
      MountainView.previewPhoto.image = new Image(new ByteArrayInputStream(mountainView.mountain.photo.getOrElse(Array[Byte]())))
      MountainView.photoCaption.text = mountainView.mountain.photoCaption.getOrElse("")
      FlightData.latitude = mountainView.mountain.geocoords.lat
      FlightData.longitude = mountainView.mountain.geocoords.lon
      latField.text = FlightData.latitude.toString
      lonField.text = FlightData.longitude.toString
      FlightData.altitude = mountainView.mountain.elevation + mountainView.mountain.prominence / 3.0
      heightField.text = FlightData.altitude.toString
    }
  }

  val ultras = Mountains.getUltras("List_of_Ultras_of_the_United_States")
  //val ultras = Mountains.getUltras("List_of_Ultras_of_Africa")
  //val ultras = Mountains.getUltras("List_of_European_ultra_prominent_peaks")
  //val ultras = Mountains.getUltras("List_of_Ultras_of_the_United_States")
  //val ultras = Mountains.getUltras("List_of_Ultras_of_the_Philippines")
  //val ultras = Mountains.getUltras("List_of_Ultras_of_Oceania")
  //val ultras = Mountains.getUltras("List_of_Ultras_of_the_Western_Himalayas")
  //val ultras = Mountains.getUltras("List_of_Ultras_of_West_Asia")

  //val ultras = Mountains.getUltras("List_of_Ultras_of_Japan")
  val startTime = System.nanoTime
  val ultraList = Await.result(ultras, 20000.milliseconds)
  println("MOUNTAIN TIME: " + (System.nanoTime - startTime) / 1000000.0)
  //println(ultraList)
  val mountains = ObservableBuffer[Mountain](ultraList)

  val rainier = (46.8529, -121.7604)

  val sampleContextMenu = new ContextMenu {
    items += (
      new MenuItem("MenuItemA") {
        onAction = { e: ActionEvent => println(e.eventType + " occurred on Menu Item A") }
      },
      new MenuItem("MenuItemB") {
        onAction = { e: ActionEvent => println(e.eventType + " occurred on Menu Item B") }
      })
  }

  val latLabel = new Label("Lat:") {
    translateX = 10.0
    translateY = 13.0
  }

  lazy val lonLabel = new Label("Lon:") {
    translateX = 10.0
    translateY = 43.0
  }

  lazy val headingLabel = new Label("Heading:") {
    translateX = 10.0
    translateY = 73.0
  }

  lazy val latField = new TextField {
    prefWidth = 130.0
    translateX = 60.0
    translateY = 10.0
    margin = Insets(3)
    editable = true
    promptText = "Latitude"
  }

  lazy val lonField = new TextField {
    prefWidth = 130.0
    translateX = 60.0
    translateY = 40.0
    margin = Insets(3)
    editable = true
    promptText = "Longitude"
  }

  lazy val headingField = new TextField {
    prefWidth = 130.0
    translateX = 60.0
    translateY = 70.0
    margin = Insets(3)
    editable = true
    promptText = "Heading"
  }

  lazy val heightLabel = new Label("Height:") {
    translateX = 10.0
    translateY = 130.0
  }

  lazy val heightField = new TextField {
    prefWidth = 130.0
    translateX = 60.0
    translateY = 130.0
    margin = Insets(3)
    editable = true
    promptText = "Height"
  }

 

  lazy val destinationField = new TextField {
    prefWidth = 130.0
    translateX = 60.0
    translateY = 100.0
    margin = Insets(3)
    editable = true
    promptText = "Destination"
  }

  val compass = new ImageView {
    val file = new File("compass.png");
    image = new Image(file.toURI().toString());
    fitWidth = 150
    fitHeight = 150
    translateX = 220.0
    translateY = 20.0
    preserveRatio = true
    smooth = true
  }

  def getImage(url: String): Future[Array[Byte]] = cache(url) {
    addHeader("User-Agent", "scala-wikiDB/1.0 (github/bdwashbu)")
    val request = spray.http.HttpRequest(GET, Uri(url))
    val pipeline = sendReceive
    pipeline(request).map { response => response.entity.data.toByteArray }.recover{ case thrown => Array[Byte]() }
  }

  val databaseTabs = new TabPane {
    translateY = 100
      tabs = Seq(
        new Tab {
          text = "Mountains"
          closable = false
          content = MountainView.getMountainTable(mountains)
        },
        new Tab {
          text = "Airports"
          closable = false
        },
        new Tab {
          text = "Cities"
          closable = false
        },
        new Tab {
          text = "Towers"
          closable = false
        })
    }
  
  val pane =    
    new TabPane { tabPane =>
    val robot = com.sun.glass.ui.Application.GetApplication().createRobot()
    
    tabs = Seq(
      new Tab {
        text = "Databases"  
        closable = false
        content = new Pane { pane =>
          children = Seq(MountainView.scrollPane, databaseTabs)
        }
      })
  }      

  val menu = new Menu("File") {
    items = List(
      new MenuItem("Set Proxy") {
        onAction = {
          e: ActionEvent =>
            {
              val result = ProxyDialog.getDialog.showAndWait()
              result match {
                case Some(ProxyDialog.Result(host, port)) => {
                  val file = new File("proxy.txt")
                  val bw = new BufferedWriter(new FileWriter(file))
                  bw.write(s"$host:$port")
                  bw.close()
                  println("host=" + host + ", port=" + port)
                }
                case _ => println("Dialog returned: None")
              }
            }
        }
      },
      new MenuItem("Exit") {
        onAction = {
          e: ActionEvent =>
            {
              Platform.exit()
              System.exit(0)
            }
        }
      })
  }

  val menuBar = new MenuBar {
    useSystemMenuBar = true
    minWidth = 900
    menus.add(menu)
  }

  var myScene = new Scene { selfScene =>
      fill = White
      content = new BorderPane {
        bottom = pane
        top = menuBar
      }
    }

  databaseTabs.prefHeight <== myScene.height - 155
  pane.prefHeight <== myScene.height - 25
  
  stage = new PrimaryStage { stage =>
    title = "scala-wikiDB"
    width = 900
    height = 600
    onCloseRequest = handle { System.exit(0) }
    scene = myScene
  }
}