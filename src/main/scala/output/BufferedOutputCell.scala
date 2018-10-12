package output

import java.io._

import barcodes.FastBarcode
import htsjdk.samtools.{SAMFileHeader, SAMRecord}
import main.scala.ReadContainer
import transforms.ReadPosition.ReadPosition

import scala.collection.mutable.ArrayBuffer
import java.util.zip._

import com.typesafe.scalalogging.LazyLogging
import htsjdk.samtools.fastq.FastqRecord
import output.BufferedOutputCell.writeRecordToFastq
import recipe.Coordinate
import stats.CellStats
import transforms.ReadPosition
import org.apache.commons.io.FileUtils
import scala.collection.mutable


/**
  * manage the reads we see for a single cell, based on the set dimensions
  * @param dimensions the coordinates of this cell in barcode space
  * @param outputPath the directory we should write the cell's reads to
  * @param bufferSize how many reads to buffer before we dump them to disk; saves time and file handles
  * @param readType what type of reads are we expecting to see
  * @param outputType what file type do we write to?
  */
class BufferedOutputCell(coordinates: Coordinate, path: File, bufferSize: Int, readType: Array[ReadPosition], cellPrefix: String = "cell") extends LazyLogging with OutputCell {

  // Our read buffer: to make things as fast as possible we'll allocate an array of the set size, and watch for overflow
  var currentIndex = 0
  var readBuffer = new Array[ReadContainer](bufferSize)
  var haveWritten = false

  // our statistics about the reads
  val stats = new CellStats(coordinates, readType)

  val readOutput = readType.map{ readType => {
    new File(path + File.separator + BufferedOutputCell.cellName + BufferedOutputCell.suffixSeparator + ReadPosition.fileExtension(readType))
  }}
  val types = readType

  def addRead(read: ReadContainer): Unit = {
    if (currentIndex >= bufferSize) {
      // write the reads out to disk -- again a while for speed in this inner loop
      var index = 0
      while(index < readOutput.size) {
        DiskWriter.write(OutputReads(readOutput(index), readBuffer.slice(0,currentIndex), types(index)))
        index += 1
      }
      // readBuffer = new Array[ReadContainer](bufferSize)
      // logger.debug("Wrote " + currentIndex + " reads to cell " + path)
      currentIndex = 0
    }
    readBuffer(currentIndex) = read
    currentIndex += 1

    // record any statistics about the read
    stats.addRead(read)
  }

  /**
    * flush out any reads we have in storage -- only if we've written already, otherwise this is
    * a garbage cell (below the threshold) and shouldn't be output
    */
  def close(): Unit = {
    //logger.debug("Current size = " + currentIndex)
    if (haveWritten && currentIndex >= 0) {
      var index = 0
      while(index < readOutput.size) {
        DiskWriter.write(OutputReads(readOutput(index), readBuffer.slice(0,currentIndex), types(index)))
        index += 1
      }
      currentIndex = -1
    }
  }

  def collectStats(): CellStats = stats
}


/**
  * static methods on the OutputCell
  */
object BufferedOutputCell extends LazyLogging {

  // read in compressed input and output streams with scala source commands
  def gis(s: String) = new GZIPInputStream(new BufferedInputStream(new FileInputStream(s)))
  //def gosAppend(s: String) = new PrintWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(s, true))))

  // the separator
  val nameSeparator = "."
  val keyValueSeparator = "->"
  val suffixSeparator = "."
  val cellName = "cell"

  /**
    * write a read to a fastq file -- this function is a bit weird, as we should pass a matched tuple,
    * as we assume the calling function has correctly determined the path
    * @param path the file path to write to
    * @param reads the reads
    * @param read which read type we want to write
    */
  def writeReadToFastqFile(path: File, reads: Array[ReadContainer], read: ReadPosition): Unit = {
    //logger.info("Writing to " + path)
    //val output = gosAppend(path.getAbsolutePath)

    val extractor : ReadContainer => FastqRecord = read match {
      case ReadPosition.Read1 =>   (x: ReadContainer) => x.read1.get
      case ReadPosition.Read2 =>   (x: ReadContainer) => x.read2.get
      case ReadPosition.Index1 =>  (x: ReadContainer) => x.index1.get
      case ReadPosition.Index2 =>  (x: ReadContainer) => x.index2.get
  }

    var index = 0
    val stringBuilder = new mutable.StringBuilder()
    while (index < reads.size) {
      val rd = reads(index)
      val annotationString = rd.metaDataString.toString()
      stringBuilder.append(fastqToString(extractor(rd),annotationString))
      index += 1
    }

    FileUtils.write(path,stringBuilder,true)
  }

  def fastqToString(fastq: FastqRecord, annotations: String): String = {
    val stringBuilder = new mutable.StringBuilder()
    stringBuilder.append(fastq.getReadHeader)
    stringBuilder.append("_")
    stringBuilder.append(annotations)
    stringBuilder.append("\n")
    stringBuilder.append(fastq.getReadString)
    stringBuilder.append("\n")
    stringBuilder.append("+")
    stringBuilder.append("\n")
    stringBuilder.append(fastq.getBaseQualityString)
    stringBuilder.append("\n")
    stringBuilder.toString()
  }

  /**
    * write a read out to disk
    * @param output the output to write to
    * @param read the read
    * @param annotations any annotations that have to be put in the readname
    */
  def writeRecordToFastq(output: PrintWriter, read: FastqRecord, annotations: String): Unit = {

    // create the new read name
    //val readName = read.getReadHeader + annotations.map{case(k,v) => k + OutputCell.keyValueSeparator + v}
    //val annotationString = new mutable.StringBuilder()
    //annotationString.append(read.getReadHeader)
    //annotations.toArray.foreach{case(k,v) => annotationString.append(k + OutputCell.keyValueSeparator + v)}



    output.write(read.getReadHeader + BufferedOutputCell.keyValueSeparator + annotations + "\n")
    output.write(read.getReadString + "\n")
    output.write(read.getBaseQualityHeader + "\n")
    output.write(read.getBaseQualityString + "\n")
  }
}