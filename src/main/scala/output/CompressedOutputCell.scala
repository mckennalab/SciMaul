package output
import java.io.{BufferedWriter, File, FileOutputStream, OutputStreamWriter}
import java.util.zip.GZIPOutputStream

import htsjdk.samtools.fastq.FastqRecord
import main.scala.ReadContainer
import output.BufferedOutputCell.fastqToString
import recipe.Coordinate
import stats.CellStats
import transforms.ReadPosition
import transforms.ReadPosition.ReadPosition

class CompressedOutputCell(coordinates: Coordinate,
                           path: File,
                           bufferSize: Int,
                           readType: Array[ReadPosition],
                           cellPrefix: String = "cell") extends OutputCell {

  // our statistics about the reads
  private val m_stats = new CellStats(coordinates, readType)

  // setup the output files
  val readOutput = readType.map{ readType => {
    val outputFile = new File(path + File.separator + BufferedOutputCell.cellName + BufferedOutputCell.suffixSeparator + ReadPosition.fileExtension(readType, true))

    assert(!outputFile.exists(), "We wont overwrite old data; please remove the existing data file: " + outputFile.getAbsolutePath + " (and probably others)")

    val out = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputFile)), "UTF-8"))

    (readType, out)

  }}.toMap

  val types = readType

  override def addRead(reads: ReadContainer): Unit = {
    stats.addRead(reads)
    val readArray = Array[ReadContainer](reads)
    readOutput.foreach{case(position,writer) => BufferedOutputCell.writeFastqRecords(readArray, position, writer)}
  }

  override def close(outputAllReads: Boolean): Unit = readOutput.foreach{case(t) => t._2.close()}

  override def stats(): CellStats = m_stats
}

object CompressedOutputCell {

  def writeToCompressedOutput(out: GZIPOutputStream, read: FastqRecord) {
    val bytes = read.getReadString.getBytes()
    out.write(bytes)
  }
}