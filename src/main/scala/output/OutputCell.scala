package output

import main.scala.ReadContainer
import stats.CellStats

/**
  *
  */
trait OutputCell {
  def addRead(read: ReadContainer)
  def close(convertToCompressedFile: Boolean = true)
  def collectStats(): CellStats
}
