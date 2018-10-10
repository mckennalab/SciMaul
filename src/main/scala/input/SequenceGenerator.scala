package input

import java.io.File

import htsjdk.samtools.fastq.FastqRecord
import main.scala.ReadContainer
import transforms.ReadPosition
import transforms.ReadPosition.ReadPosition

import scala.collection.mutable
import scala.io.Source

class SequenceGenerator(inputs: mutable.LinkedHashMap[ReadPosition, File]) extends Iterator[ReadContainer] {

  val read1 : Iterator[String]#GroupedIterator[String] =
    SequenceGenerator.getLines(inputs(ReadPosition.Read1)).grouped(4)

  val read2 : Option[Iterator[String]#GroupedIterator[String]] =
    if (inputs contains ReadPosition.Read2) Some(SequenceGenerator.getLines(inputs(ReadPosition.Read2)).grouped(4)) else None

  val index1 : Option[Iterator[String]#GroupedIterator[String]] =
    if (inputs contains ReadPosition.Index1) Some(SequenceGenerator.getLines(inputs(ReadPosition.Index1)).grouped(4)) else None

  val index2 : Option[Iterator[String]#GroupedIterator[String]] =
    if (inputs contains ReadPosition.Index2) Some(SequenceGenerator.getLines(inputs(ReadPosition.Index2)).grouped(4)) else None

  private def getNextRead(): ReadContainer = {
    ReadContainer(
      Some(groupToFastqRecord(read1.next)),
      if (read2.isDefined) Some(groupToFastqRecord(read2.get.next)) else None,
      if (index1.isDefined) Some(groupToFastqRecord(index1.get.next)) else None,
      if (index2.isDefined) Some(groupToFastqRecord(index2.get.next)) else None
    )
  }

  def groupToFastqRecord(strings: List[String]): FastqRecord = {
    new FastqRecord(strings(0),strings(1),strings(2),strings(3))
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


