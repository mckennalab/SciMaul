package stats

import main.scala.ReadContainer
import recipe.Coordinate
import transforms.ReadPosition
import transforms.ReadPosition.ReadPosition
import utils.ReadUtils

import scala.collection.mutable

class CellStats(coordinates: Coordinate, readsCovered: Array[ReadPosition]) {

  val cellStats = Array[CellStat](new ReadCount(),new ReadLength(), new AverageQual(readsCovered))

  def addRead(read: ReadContainer): Unit = {
    cellStats.foreach{cs => cs.addRead(read)}
  }

  def header(sep: String = "\t"): String = cellStats.flatMap{cs => cs.name}.mkString(sep)

  def stats(sep: String = "\t"): String = cellStats.flatMap{cs => cs.stat}.mkString(sep)
}

trait CellStat {
  def addRead(read: ReadContainer)
  def name: Array[String]
  def stat: Array[String]
}

class ReadCount extends CellStat {
  var readCount = 0

  override def addRead(read: ReadContainer): Unit = {readCount += 1}

  override def name = Array[String]("readCount")

  override def stat= Array[String](readCount.toString)
}

class ReadLength extends CellStat {
  var readLength = 0

  override def addRead(read: ReadContainer): Unit = {readLength += 1}

  override def name = Array[String]("readLength")

  override def stat = Array[String](readLength.toString)
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

  override def name = totalQual.keys.toArray.map{c => "averageQual" + c.toString}

  override def stat = totalQual.map{case(c,v) => (v.toDouble / baseCount(c).toDouble).toString}.toArray
}