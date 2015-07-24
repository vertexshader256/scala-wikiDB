package com.stopo_pilot

import scalafx.Includes._
import scalafx.application.{JFXApp, Platform}
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.ButtonBar.ButtonData
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{GridPane, VBox}
import scala.io.Source
import java.io.File

object ProxyDialog {
  case class Result(proxyHost: String, port: String)
  
  def getDialog() = {
   
    var host = ""
    var port = ""
    
    // Create the custom dialog.
    val dialog = new Dialog[Result]() {
      //initOwner(stage)
      title = "Set Proxy Server"
      headerText = "Specify a proxy server"
    }
    
    // Create the username and password labels and fields.
    val hostField = new TextField() {
      promptText = "Host"
      prefWidth = 180.0
    }
    val portField = new TextField() {
      promptText = "Port"
      prefWidth = 180.0
    }
    
    val filename = "proxy.txt"
    if (new File(filename).exists) {
      for (line <- Source.fromFile(filename).getLines()) {
        val proxy = line.split(":")
        host = proxy.head
        port = proxy.last
      }
    }
    
    hostField.text = host
    portField.text = port
     
    // Set the button types.
    val setButtonType = new ButtonType("Set", ButtonData.OKDone)
    dialog.dialogPane().buttonTypes = Seq(setButtonType, ButtonType.Cancel)
     
    val grid = new GridPane() {
      hgap = 10
      vgap = 10
      padding = Insets(20, 100, 10, 10)
     
      add(new Label("Proxy Host:"), 0, 0)
      add(hostField, 1, 0)
      add(new Label("Proxy Port:"), 0, 1)
      add(portField, 1, 1)
    }
     
    // Enable/Disable login button depending on whether a username was entered.
    val setButton = dialog.dialogPane().lookupButton(setButtonType)
    setButton.disable = true
     
    // Do some validation (disable when username is empty).
    hostField.text.onChange { (_, _, newValue) => 
      setButton.disable = newValue == host && port == portField.text()
    }
    
    // Do some validation (disable when username is empty).
    portField.text.onChange { (_, _, newValue) => 
      setButton.disable = newValue == port && host == hostField.text()
    }
     
    dialog.dialogPane().content = grid
     
    // Request focus on the username field by default.
    Platform.runLater(hostField.requestFocus())
     
    // When the login button is clicked, convert the result to a username-password-pair.
    dialog.resultConverter = dialogButton =>
      if (dialogButton == setButtonType) Result(hostField.text(), portField.text())
      else null
      
    dialog
  }
}