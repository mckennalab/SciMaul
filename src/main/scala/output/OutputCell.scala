package output

import java.io._

import barcodes.FastBarcode
import htsjdk.samtools.{SAMFileHeader, SAMRecord}
import main.scala.ReadContainer
import transforms.ReadPosition.ReadPosition

import scala.collection.mutable.ArrayBuffer
import java.util.zip._

import htsjdk.samtools.fastq.FastqRecord
import recipe.Coordinate
import transforms.ReadPosition

import scala.collection.mutable


/**
  * manage the reads we see for a single cell, based on the set dimensions
  * @param dimensions the coordinates of this cell in barcode space
  * @param outputPath the directory we should write the cell's reads to
  * @param bufferSize how many reads to buffer before we dump them to disk; saves time and file handles
  * @param readType what type of reads are we expecting to see
  * @param outputType what file type do we write to?
  */
class OutputCell(coordinates: Coordinate, path: File, bufferSize: Int, readType: Array[ReadPosition]) {

  // Our read buffer: to make things as fast as possible we'll allocate an array of the set size, and watch for overflow
  var currentIndex = 0
  val readBuffer = new Array[ReadContainer](bufferSize)


  val readOutput = readType.map{ readType => {
    (readType,new File(path + File.separator + ReadPosition.fileExtension(readType)))
  }}.toMap


  def addRead(read: ReadContainer): Unit = {
    if (currentIndex >= bufferSize) {
      // write the reads out to disk
      readType.foreach{tp =>
        OutputCell.writeReadToFastqFile(readOutput(tp), readBuffer, tp)
      }
      currentIndex = 0
    }
    readBuffer(currentIndex) = read
    currentIndex += 1
  }

  def close(): Unit = {
    if (currentIndex >= bufferSize) {
      // write the reads out to disk -- but just up to the index size
      readType.foreach{tp =>
        OutputCell.writeReadToFastqFile(readOutput(tp), readBuffer.slice(0,currentIndex), tp)
      }
      currentIndex = 0
    }
  }

}

/**
  * static methods on the OutputCell
  */
object OutputCell {

  // read in compressed input and output streams with scala source commands
  def gis(s: String) = new GZIPInputStream(new BufferedInputStream(new FileInputStream(s)))
  def gosAppend(s: String) = new PrintWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(s, true))))

  // the separator
  val nameSeparator = "."
  val keyValueSeparator = "->"

  def writeToSamFile(path: File, reads: ReadContainer, header: SAMFileHeader): Unit = {
    val samRecord = new SAMRecord(header)
    samRecord.setReadBases(reads.read1.getReadString.toCharArray.map{c => c.toByte})
    samRecord.setOriginalBaseQualities(reads.read1.getBaseQualityString.toArray.map{c => c.toByte})
    samRecord.setReadName(reads.read1.getReadHeader)
  }


  def writeReadToFastqFile(path: File, reads: Array[ReadContainer], read: ReadPosition): Unit = {
    val output = gosAppend(path.getAbsolutePath)
    reads.foreach { rd =>
      read match {
        case ReadPosition.Read1 => writeRecordToFastq(output,rd.read1,rd.metaData)
        case ReadPosition.Read2 => writeRecordToFastq(output,rd.read2.get,rd.metaData)
        case ReadPosition.Index1 => writeRecordToFastq(output,rd.index1.get,rd.metaData)
        case ReadPosition.Index2 => writeRecordToFastq(output,rd.index2.get,rd.metaData)
      }
    }
    output.close()

  }

  def writeRecordToFastq(output: PrintWriter, read: FastqRecord, annotations: mutable.HashMap[String,String]): Unit = {

    // create the new read name
    val readName = read.getReadHeader + annotations.map{case(k,v) => k + OutputCell.keyValueSeparator + v}

    output.write(readName + "\n")
    output.write(read.getReadString + "\n")
    output.write(read.getBaseQualityHeader + "\n")
    output.write(read.getBaseQualityString + "\n")
  }
}