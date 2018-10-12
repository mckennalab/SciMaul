package input

import java.io.File

import htsjdk.samtools.fastq.{FastqReader, FastqRecord}
import main.scala.ReadContainer
import transforms.ReadPosition
import transforms.ReadPosition.ReadPosition

import scala.collection.mutable
import scala.io.Source

class SequenceGenerator(inputs: mutable.LinkedHashMap[ReadPosition, File]) extends Iterator[ReadContainer] {

  val read1 : FastqReader =
    new FastqReader(inputs(ReadPosition.Read1))

  val read2 : Option[FastqReader] =
    if (inputs contains ReadPosition.Read2) Some(new FastqReader(inputs(ReadPosition.Read2))) else None

  val index1 : Option[FastqReader] =
    if (inputs contains ReadPosition.Index1) Some(new FastqReader(inputs(ReadPosition.Index1))) else None

  val index2 : Option[FastqReader] =
    if (inputs contains ReadPosition.Index2) Some(new FastqReader(inputs(ReadPosition.Index2))) else None

  private def getNextRead(): ReadContainer = {
    new ReadContainer(
      Some(read1.next),
      if (read2.isDefined) Some(read2.get.next) else None,
      if (index1.isDefined) Some(index1.get.next) else None,
      if (index2.isDefined) Some(index2.get.next) else None
    )
  }

  // our next read
  private var nextRead: Option[ReadContainer] = Some(getNextRead())

  override def hasNext: Boolean = nextRead.isDefined

  override def next(): ReadContainer = {
    assert(nextRead.isDefined,"Next called on empty iterator")
    val ret = nextRead.get
    if (read1.hasNext)
      nextRead = Some(getNextRead())
    else
      nextRead = None
    ret
  }
}

object SequenceGenerator {
  def getLines(fl: File): Iterator[String] =
    if (fl.getAbsolutePath.endsWith(".gz") || fl.getAbsolutePath.endsWith(".gzip"))
      Source.fromInputStream(utils.FileUtils.gis(fl.getAbsolutePath)).getLines()
    else
      Source.fromFile(fl.getAbsolutePath).getLines()
}


