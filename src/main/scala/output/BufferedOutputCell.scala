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
import recipe.{Coordinate, ResolvedDimension}
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
  * @param compressedExtension what extension should we tack onto the compressed output
  */
class BufferedOutputCell(coordinates: Coordinate, path: File, bufferSize: Int, readType: Array[ReadPosition], cellPrefix: String = "cell", compressedExtension: String = ".gz") extends LazyLogging with OutputCell {

  // Our read buffer: to make things as fast as possible we'll allocate an array of the set size, and watch for overflow
  var currentIndex = 0
  var readBuffer = new Array[ReadContainer](bufferSize)
  var haveWritten = false

  // we're using an rich-get-richer scheme here: everytime the file writes, we make the buffer larger, up until the 'buffersize' max
  var myBufferSize = 25


  // our statistics about the reads
  val stats = new CellStats(coordinates, readType)

  // setup the output files
  val readOutput = readType.map{ readType => {
    val outputFile = new File(path + File.separator + BufferedOutputCell.cellName + BufferedOutputCell.suffixSeparator + ReadPosition.fileExtension(readType))
    assert(!outputFile.exists(), "We wont overwrite old data; please remove the existing data file: " + outputFile.getAbsolutePath + " (and probably others)")
    outputFile
  }}
  val types = readType

  /**
    * add a read to this output. If we overflow our buffer, write the data to disk
    * @param read the read sequence to add
    */
  def addRead(read: ReadContainer): Unit = {
    if (currentIndex >= myBufferSize) {
      haveWritten = true
      // write the reads out to disk -- again a while for speed in this inner loop
      var index = 0
      while(index < readOutput.size) {
        DiskWriter.write(OutputReads(readOutput(index), readBuffer.slice(0,currentIndex), types(index)))
        index += 1
      }
      // grow by doubling, up to the set buffer size
      myBufferSize = math.min(myBufferSize * 2, bufferSize)
      readBuffer = new Array[ReadContainer](bufferSize)
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
  def close(convertToCompressedFile: Boolean = true): Unit = {
    //logger.debug("Current size = " + currentIndex)
    if (haveWritten && currentIndex >= 0) {
      var index = 0
      while (index < readOutput.size) {
        DiskWriter.write(OutputReads(readOutput(index), readBuffer.slice(0, currentIndex), types(index)))
        index += 1
      }
      currentIndex = -1
    }

    if (haveWritten) {
      readOutput.foreach{textOutput => {
        DiskWriter.closeFile(textOutput)

        val gzippedVersion = new File(textOutput.getAbsolutePath + compressedExtension)
        DiskWriter.textToGZippedThenDelete(textOutput,gzippedVersion)
      }}
    }
    }

  /**
    * @return the stats about this run
    */
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
  val keyValueSeparator = "="
  val suffixSeparator = "."
  val cellName = "cell"

  /**
    * convert a set of reads into an output string
    * @param path the file path to write to
    * @param reads the reads
    * @param read which read type we want to write
    */
  def writeFastqRecords(reads: Array[ReadContainer], read: ReadPosition, writer: BufferedWriter) {
    //logger.info("Writing to " + path)
    //val output = gosAppend(path.getAbsolutePath)

    val extractor : ReadContainer => FastqRecord = read match {
      case ReadPosition.Read1 =>   (x: ReadContainer) => x.read1.get
      case ReadPosition.Read2 =>   (x: ReadContainer) => x.read2.get
      case ReadPosition.Index1 =>  (x: ReadContainer) => x.index1.get
      case ReadPosition.Index2 =>  (x: ReadContainer) => x.index2.get
  }

    var index = 0
    while (index < reads.size) {
      val rd = reads(index)
      val annotationString = rd.metaDataString.toString()
      fastqToString(extractor(rd),annotationString,writer)
      index += 1
    }
  }

  /**
    * convert a fastq record to a string representation, with our annotations
    * @param fastq the fastq object
    * @param annotations the annotations to attach to the read name
    * @param writer
    */
  def fastqToString(fastq: FastqRecord, annotations: String, writer: BufferedWriter) {
    writer.append("@" + fastq.getReadHeader.replace(" ","_"))
    writer.append("_")
    writer.append(annotations)
    writer.append("\n")
    writer.append(fastq.getReadString)
    writer.append("\n")
    writer.append("+")
    writer.append("\n")
    writer.append(fastq.getBaseQualityString)
    writer.append("\n")
  }
}
