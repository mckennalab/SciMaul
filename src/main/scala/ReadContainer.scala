package main.scala

import htsjdk.samtools.fastq.FastqRecord
import output.BufferedOutputCell
import transforms.ReadPosition
import transforms.ReadPosition.ReadPosition

import scala.collection.mutable

class ReadContainer(r1: Option[FastqRecord], r2: Option[FastqRecord], i1: Option[FastqRecord], i2: Option[FastqRecord]) {

  var read1 = r1
  var read2 = r2
  var index1 = i1
  var index2 = i2

  // a container to map additional metadata to the read set
  var metaDataString = "" // new mutable.StringBuilder(1000)

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
  def sliceAndAnnototate(read: ReadContainer, start: Int, length: Int, name: String, readDim: ReadPosition, keepSequence: Boolean): String = {
    val toSlice = readDim match {
      case ReadPosition.Read1 =>  ReadContainer.sliceFromFastq(read.read1.get, start, length, !keepSequence)
      case ReadPosition.Read2 =>  ReadContainer.sliceFromFastq(read.read2.get, start, length, !keepSequence)
      case ReadPosition.Index1 => ReadContainer.sliceFromFastq(read.index1.get, start, length, !keepSequence)
      case ReadPosition.Index2 => ReadContainer.sliceFromFastq(read.index2.get, start, length, !keepSequence)
    }

    //read.metaDataString = read.metaDataString.concat(name + BufferedOutputCell.keyValueSeparator + "{" + toSlice._2 + "},{" + toSlice._3 + "};")
    read.metaDataString = read.metaDataString.concat(BufferedOutputCell.keyValueSeparator)
    read.metaDataString = read.metaDataString.concat( "{")
    read.metaDataString = read.metaDataString.concat(toSlice._2)
    read.metaDataString = read.metaDataString.concat("},{")
    read.metaDataString = read.metaDataString.concat(toSlice._3)
    read.metaDataString = read.metaDataString.concat("};")

    readDim match {
      case ReadPosition.Read1 => read.read1 = Some(toSlice._1)
      case ReadPosition.Read2 =>  read.read2 = Some(toSlice._1)
      case ReadPosition.Index1 => read.index1 = Some(toSlice._1)
      case ReadPosition.Index2 => read.index2 = Some(toSlice._1)
    }

    toSlice._2
  }


  /**
    * for testing, especially performance testing, generate a fake read (avoiding disk times)
    * @return a fake read container
    */
  def fakeRead(): ReadContainer = {
    new ReadContainer(
      Some(new FastqRecord("@M00296:79:000000000-BJJ8W:1:1102:15663:1332",
        "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGCGGGGGGTGGGGGAAAGGAAAAGGACAGGGGGAGGTGGGGGGGGAGTGGTAGAGCGGTAGATGAGAAGAGGTAGTAGAAATGGGGAGGTTATTGTGAGAAACAGCAAAAGGGAGTAGGTAATAATACGTACAGAGTAGAAATATGATATAATGA",
      "+",
      "BBBBBBBBBB@@?@9;99@;?F=B---;--9;BF?=--;-999-9--9--99--;---;-.....//./.......-9---...9-;-----;9.//////9-...////////9../;9///9;////...9-..:////////////9...:./.;;..9/9/////9;//.//..//9//:///////://:/9///")),
      Some(new FastqRecord("@M00296:79:000000000-BJJ8W:1:1102:15663:1332",
        "AGGAAGGGAGGGGGGGCAAGGAGGGCGGTGTATTGAGGTGGAGAGAGGAGGGTGGGGAGGTAGGGGAGGAGGTGAGGAACGTGGAGAATGGAGAGGCAGTGGAGGGAGAGATGGAAATAGGAGAATGAGTGAAATGGGAATATGAAGAGATGGATAGAGGATGAGTCGTAGAGCAGGAGA",
        "+",
        "1>>111111111A0>---......-.-:--.:00000//.;../9.;....---9---9-9//-9A--;----;/--/-;------/-////9/-----//;/9-------/////9//////--//////9/////--9-//;////--/////////-9//9///-----////---9")),
        Some(new FastqRecord("@M00296:79:000000000-BJJ8W:1:1102:15663:1332",
        "GAGAGGGGGG",
        "+",
        "111111111/")),
          Some(new FastqRecord("@M00296:79:000000000-BJJ8W:1:1102:15663:1332",
        "ACGTGAGGGA",
        "+",
        "111111111>")))
  }
}