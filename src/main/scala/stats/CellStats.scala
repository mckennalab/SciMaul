package stats

import main.scala.ReadContainer
import recipe.Coordinate
import transforms.ReadPosition
import transforms.ReadPosition.ReadPosition
import utils.ReadUtils

import scala.collection.mutable

class CellStats(coordinates: Coordinate, readsCovered: Array[ReadPosition]) {

  val cellStats = Array[CellStat](new ReadCount())

  def addRead(read: ReadContainer): Unit = {
    var index = 0
    while (index < cellStats.size) {
      cellStats(index).addRead(read)
      index += 1
    }
  }

  def name: String = {
    "Cell" + Coordinate.seperator + coordinates.coordinateString()
  }
}



trait CellStat {
  def addRead(read: ReadContainer)
  def read: ReadPosition
  def name: Array[String]
  def stat: Array[String]
}

class ReadCount extends CellStat {
  var readCount = 0

  override def addRead(read: ReadContainer): Unit = {readCount += 1}

  override def name = Array[String]("readCount")

  override def stat= Array[String](readCount.toString)

  override def read: ReadPosition = ReadPosition.All
}

class ReadLength(reads: Array[ReadPosition]) extends CellStat {
  var totalLen = new mutable.LinkedHashMap[ReadPosition,Long]()
  var readCount = 0

  reads.foreach{rt => {
    totalLen(rt) = 0
  }}

  override def addRead(read: ReadContainer): Unit = {
    reads.foreach{rd => {
      totalLen(rd) += read.readFromContainer(rd).get.getReadString.size
    }}
    readCount += 1
  }

  override def name = totalLen.keys.toArray.map{c => "averageLength" + c.toString}

  override def stat = totalLen.map{case(c,v) => (v.toDouble / readCount).toString}.toArray

  override def read: ReadPosition = ReadPosition.All
}


class AverageQual(reads: Array[ReadPosition]) extends CellStat {
  var totalQual = new mutable.LinkedHashMap[ReadPosition,Long]()
  var baseCount = new mutable.LinkedHashMap[ReadPosition,Long]()

  reads.foreach{rt => {
    totalQual(rt) = 0
    baseCount(rt) = 0
  }}

  override def addRead(read: ReadContainer): Unit = {
    reads.foreach{rd => {
      val scores = read.readFromContainer(rd).get.getBaseQualityString.map{c => ReadUtils.phredToScore(c)}.toArray
      totalQual(rd) += scores.sum
      baseCount(rd) += scores.size
    }}
  }

  override def name = totalQual.keys.toArray.map{c => "averageQuality" + c.toString}

  override def stat = totalQual.map{case(c,v) => (v.toDouble / baseCount(c).toDouble).toString}.toArray

  override def read: ReadPosition = ReadPosition.All
}