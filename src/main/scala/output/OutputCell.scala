package output

import main.scala.ReadContainer
import stats.CellStats

/**
  *
  */
trait OutputCell {
  def addRead(read: ReadContainer)
  def close()
  def collectStats(): CellStats
}
