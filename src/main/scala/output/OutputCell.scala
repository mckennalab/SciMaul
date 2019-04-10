package output

import main.scala.ReadContainer
import stats.CellStats

trait OutputCell {

  def addRead(read: ReadContainer): Unit

  def close(outputAllReads: Boolean = true): Unit

  def stats(): CellStats
}
