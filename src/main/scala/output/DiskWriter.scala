package output

import java.io.{BufferedWriter, File, FileWriter, PrintWriter}

import htsjdk.samtools.fastq.FastqRecord
import main.scala.ReadContainer
import transforms.ReadPosition.ReadPosition
import utils.FileUtils

import scala.collection.mutable
import scala.io.Source
/*

commented out for now
class DiskWriter extends Actor {
  override def receive: Receive = {
    case e: OutputReads => BufferedOutputCell.writeReadToFastqFile(e.path,e.reads,e.read)
  }
}
*/
object DiskWriter {

  var maxOpenFiles = 1000

  val fileHandleLookup = new scala.collection.mutable.LinkedHashMap[String, BufferedWriter]()
  val lowToHighFileActivity = new collection.mutable.PriorityQueue[QueueOrder]()

  val everWritten = new mutable.HashMap[String,Boolean]()

  /**
    * write the read contents out to the specified file
    * @param out a container with the file to write to, the reads to write, and which position the file is (read1, etc)
    */
  def write(out: OutputReads): Unit = {
    val path = out.path.getAbsolutePath
    if (!(fileHandleLookup contains path)) {
      while (lowToHighFileActivity.size >= maxOpenFiles) {
        val fileToDrop = lowToHighFileActivity.dequeue()
        fileHandleLookup(fileToDrop.path).flush()
        fileHandleLookup(fileToDrop.path).close()
        fileHandleLookup.remove(fileToDrop.path)
      }
      lowToHighFileActivity.enqueue(new QueueOrder(path, out.reads.size))
      fileHandleLookup(path) = new BufferedWriter(new FileWriter(path, true))

    }
    BufferedOutputCell.writeFastqRecords(out.reads, out.read, fileHandleLookup(path))
    everWritten(path) = true
  }

  /**
    * close any and all remaining open files
    */
  def close(): Unit = {
    fileHandleLookup.foreach { case (fl, bw) => bw.flush(); bw.close() }
    // rewriteZipped() -- this is sadly super slow right now -- make a buffered block gzip writer later to fix this
  }

  /**
    * close a specific file (only if we have it open)
    * @param file the file to close, if open
    */
  def rewriteZipped(compressedExtension: String = ".gz"): Unit = {
    println("Rewriting output as compressed files...")
    everWritten.foreach{case(file,bool) => {
      val gzippedVersion = new File(file + compressedExtension)
      DiskWriter.textToGZippedThenDelete(new File(file),gzippedVersion)
    }}

  }

  /**
    * copy the contents of a text file over to a gzipped version AND remove the text version
    * @param input the input file to REMOVE AFTER THE COPY
    * @param output the output file to write
    */
  def textToGZippedThenDelete(input: File, output: File): Unit = {
    val outputWriter = FileUtils.gos(output.getAbsolutePath)

    Source.fromFile(input).getLines().foreach(line => {
      outputWriter.write(line + "\n")
    })

    input.delete()
    outputWriter.close()
  }


}

case class OutputReads(path: File, reads: Array[ReadContainer], read: ReadPosition)
class QueueOrder(mPath: String, mCount: Int) extends Ordering[QueueOrder] {
  val path = mPath
  val count = mCount
  override def compare(x: QueueOrder, y: QueueOrder): Int = y.count - x.count
}
object QueueOrder {
  implicit def QueueOrderOrdering: Ordering[QueueOrder] = Ordering.by(-1 * _.count)
}