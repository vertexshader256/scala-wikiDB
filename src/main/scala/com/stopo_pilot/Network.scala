package com.stopo_pilot

import akka.io._
import akka.actor._
import akka.util._
import java.io._
import java.net.{ InetSocketAddress, InetAddress }
import java.nio.ByteBuffer
import java.nio.ByteOrder
import scala.collection.mutable.HashMap

object Network {
  class Flow(remote: InetSocketAddress) extends Actor {
  import context.system
  IO(Udp) ! Udp.SimpleSender

  def receive = {
    case Udp.SimpleSenderReady =>
      context.become(ready(sender()))
  }

  def ready(socket: ActorRef): Receive = {
    case msg: Array[Byte] =>
      socket ! Udp.Send(ByteString(msg), remote)
  }
}

trait UDP extends Runnable {
  def run {
    val system = ActorSystem("mySystem")

    val gpsIP = InetAddress.getByAddress(Array[Byte](224.toByte, 224.toByte, 117.toByte, 249.toByte))
    val gpsPort = 1234
    val localIP = InetAddress.getLoopbackAddress
    val local = new InetSocketAddress(localIP, gpsPort)
    val props = Props(new Flow(new InetSocketAddress(localIP, 1234)))
    val gps = system.actorOf(props, "myactor2") //new Flow(new InetSocketAddress(gpsIP, gpsPort))

    while (true) {
      try {
        val bbuf = ByteBuffer.allocate(40);
        bbuf.order(ByteOrder.LITTLE_ENDIAN)

        bbuf.putDouble(FlightData.latitude)
        bbuf.putDouble(FlightData.longitude)
        bbuf.putDouble(FlightData.altitude)
        bbuf.putDouble(FlightData.heading)
        bbuf.putDouble(FlightData.pitch)

        gps ! bbuf.array
        Thread.sleep(10)
      } catch {

        case _: Throwable => { println("STOPPING?"); gps ! "STOP" }
      }
    }
  }
}
}