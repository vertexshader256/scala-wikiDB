package com.stopo_pilot

import scala.collection.mutable.ListBuffer

object Utils {
  
  def splitBySeparator[T]( l: Seq[T], sep: T ): Seq[List[T]] = {
    val b = ListBuffer(ListBuffer[T]())
    l foreach { e =>
      if ( e == sep ) {
        if  ( !b.last.isEmpty ) b += ListBuffer[T]()
      }
      else b.last += e
    }
    b.map(_.toList)
  }
}