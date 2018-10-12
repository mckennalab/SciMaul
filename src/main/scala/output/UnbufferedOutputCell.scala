package output

import java.io.{File, PrintWriter}

import com.typesafe.scalalogging.LazyLogging
import main.scala.ReadContainer
import recipe.Coordinate
import stats.CellStats
import transforms.ReadPosition
import transforms.ReadPosition.ReadPosition

class UnbufferedOutputCell(coordinates: Coordinate, path: File, bufferSize: Int, readType: Array[ReadPosition], cellPrefix: String = "cell") extends LazyLogging with OutputCell {

  override def addRead(read: ReadContainer): Unit = {
  }

  override def close(): Unit = ???

  override def collectStats(): CellStats = ???
}
