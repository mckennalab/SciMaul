package main.scala

import htsjdk.samtools.fastq.FastqRecord
import output.BufferedOutputCell
import transforms.{ReadPosition, TransforedReadAndDimension}
import transforms.ReadPosition.ReadPosition

import scala.collection.mutable

class ReadContainer(r1: Option[FastqRecord], r2: Option[FastqRecord], i1: Option[FastqRecord], i2: Option[FastqRecord]) {

  var read1 = r1
  var read2 = r2
  var index1 = i1
  var index2 = i2

  // a container to map additional metadata to the read set
  var metaData = new mutable.HashMap[String,String]()
  val metaDataString = new mutable.StringBuilder()


  def readFromContainer(position: ReadPosition): Option[FastqRecord] = position match {
    case ReadPosition.Read1 => read1
    case ReadPosition.Read2 => read2
    case ReadPosition.Index1 => index1
    case ReadPosition.Index2 => index2
  }
}

object ReadContainer {

  /**
    * extract a subsequence out of the specified read
    * @param toSlice the fastq record to extract from
    * @param start the starting position
    * @param length the length of the sequence to slice out
    * @param keepSequence should we keep the sequence within the read
    * @return a tuple of fastqrecord, barcode string, and barcode qual
    */
  def sliceFromFastq(toSlice: FastqRecord, start: Int, length: Int, keepSequence: Boolean): Tuple3[FastqRecord,String,String] = {
    val barcode = toSlice.getReadString.slice(start,start + length)
    val barcodeRemaining = toSlice.getReadString.slice(0, start) +
      toSlice.getReadString.slice(start + length,toSlice.getReadString.size)

    val bcQuals = toSlice.getBaseQualityString.slice(start,start + length)
    val bcQualRemaining = toSlice.getBaseQualityString.slice(0, start) +
      toSlice.getBaseQualityString.slice(start + length,toSlice.getReadString.size)


    val replacement =
      if (!keepSequence)
        new FastqRecord(toSlice.getReadHeader,barcodeRemaining,toSlice.getBaseQualityHeader, bcQualRemaining)
      else
        toSlice

    (replacement,barcode,bcQuals)
  }

  /**
    * extract out a sequence from a collection of reads
    * @param read the container to slice from
    * @param start the starting position
    * @param length how long of a sequence to slice out
    * @param name the annotation name for storing the extracted sequence in the metadata storage
    * @param readDim which of the reads to extract
    * @param keepSequence do we keep the sequence within the read
    */
  def sliceAndAnnototate(read: ReadContainer, start: Int, length: Int, name: String, readDim: ReadPosition, keepSequence: Boolean): TransforedReadAndDimension = {
    // we strip out the index from the read and it's quality score
    val toSlice = readDim match {
      case ReadPosition.Read1 =>  ReadContainer.sliceFromFastq(read.read1.get, start, length, !keepSequence)
      case ReadPosition.Read2 =>  ReadContainer.sliceFromFastq(read.read2.get, start, length, !keepSequence)
      case ReadPosition.Index1 => ReadContainer.sliceFromFastq(read.index1.get, start, length, !keepSequence)
      case ReadPosition.Index2 => ReadContainer.sliceFromFastq(read.index2.get, start, length, !keepSequence)
    }

    read.metaData(name) = "=" + toSlice._2 + "," + toSlice._3
    read.metaDataString.append(name + BufferedOutputCell.keyValueSeparator + "{" + toSlice._2 + "},{" + toSlice._3 + "}")

    readDim match {
      case ReadPosition.Read1 => read.read1 = Some(toSlice._1)
      case ReadPosition.Read2 =>  read.read2 = Some(toSlice._1)
      case ReadPosition.Index1 => read.index1 = Some(toSlice._1)
      case ReadPosition.Index2 => read.index2 = Some(toSlice._1)
    }

    TransforedReadAndDimension(read,Some(toSlice._2))
  }

}