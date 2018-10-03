package main.scala.algorithms

import net.sf.picard.fastq.FastqRecord

/**
  * transform reads by trimming out a subsection
  */
object ReadTrimmer {

  def trimRead(inputRead: FastqRecord, trimStart: Int, trimStop: Int): FastqRecord = {
    require(trimStop - trimStart > 0,"The trim stop minus the trim start should be positive")

    val trimmedBases = inputRead.getReadString.substring(0,trimStart) + inputRead.getReadString.substring(trimStop,inputRead.getReadString.size)
    val trimmedQualScore = inputRead.getBaseQualityString.substring(0,trimStart) + inputRead.getBaseQualityString.substring(trimStop,inputRead.getBaseQualityString.size)

    assert(trimmedBases.size == inputRead.getReadString.size - (trimStop - trimStart),"something went wrong in read subtraction")
    new FastqRecord(inputRead.getReadHeader,trimmedBases,inputRead.getBaseQualityHeader,trimmedQualScore)
  }
}
