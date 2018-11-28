package output

import java.io.{BufferedWriter, File, FileWriter, PrintWriter}

import htsjdk.samtools.fastq.FastqRecord
import main.scala.ReadContainer
import transforms.ReadPosition.ReadPosition
import utils.FileUtils

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
  }

  /**
    * close any and all remaining open files
    */
  def close(): Unit = {
    fileHandleLookup.foreach { case (fl, bw) => bw.flush(); bw.close() }
  }

  /**
    * close a specific file (only if we have it open)
    * @param file the file to close, if open
    */
  def closeFile(file: File): Unit = {
    val path = file.getAbsolutePath
    if (!(fileHandleLookup contains path)) {
      fileHandleLookup(path).flush()
      fileHandleLookup(path).close()
      fileHandleLookup.remove(path)
    }
  }

  /**
    * copy the contents of a text file over to a gzipped version AND remove the text version
    * @param input the input file to REMOVE AFTER THE COPY
    * @param output the output file to write
    */
  def textToGZippedThenDelete(input: File, output: File): Unit = {
    val output = FileUtils.gos(output)

    Source.fromFile(input).getLines().foreach(line => {
      output.write(line + "\n")
    })
    output.close()
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